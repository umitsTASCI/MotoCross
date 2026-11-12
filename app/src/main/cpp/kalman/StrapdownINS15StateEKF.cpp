#include "StrapdownINS15StateEKF.hpp"
#include <cmath>
#include <algorithm>

namespace oemcluster::kalman {

/*
 * EKSEN VE İŞARET KURALLARI (şasi ekseni: X_b ileri, Y_b sol, Z_b yukarı, sağ-el kuralı)
 *  - Pitch (phi_pitch): burun YUKARI pozitif. Sağ-el kuralına göre +w_y burnu AŞAĞI çevirir,
 *    bu yüzden d(phi_pitch)/dt = -w_y.
 *  - Lean (theta_lean): SAĞA yatış pozitif (+w_x sol tarafı kaldırır = sağa yatış).
 *  - Yaw: +w_z sola dönüş. Sola dönüşte motosiklet sola yatar (negatif theta), dolayısıyla
 *    kinematik yatış = arctan(-(v * w_yaw) / g). (Şartnamedeki formül işaret kuralından bağımsız
 *    büyüklüğü verir; burada işaret bu eksen tanımına göre uygulanır.)
 *  - İvmeölçer özgül kuvvet ölçer: dururken burun-yukarı phi eğiminde a_x = +g*sin(phi).
 */

StrapdownINS15StateEKF::StrapdownINS15StateEKF() noexcept {
    reset();
}

void StrapdownINS15StateEKF::reset() noexcept {
    state_.fill(0.0);
    // Başlangıç kovaryans değerleri
    P_[0] = 1.0;  P_[1] = 1.0;  P_[2] = 1.0;        // Konum
    P_[3] = 0.25; P_[4] = 0.10; P_[5] = 0.10;       // Hız
    P_[6] = 0.01; P_[7] = 0.01; P_[8] = 0.05;       // Roll (Lean), Pitch, Yaw
    P_[9] = 0.08; P_[10] = 0.08; P_[11] = 0.08;     // İvmeölçer Bias
    P_[12] = 0.002; P_[13] = 0.002; P_[14] = 0.002; // Jiroskop Bias

    P_v_bax_ = 0.0;
    P_v_pitch_ = 0.0;
    lastTrueAxMs2_ = 0.0;
    prevTrueAxMs2_ = 0.0;
    lastTrueAyMs2_ = 0.0;
    filteredJerkXMs3_ = 0.0;
    ringHead_ = 0;
    ringCount_ = 0;

    gnssSpeedMs_ = 0.0;
    gnssSpeedAccuracyMs_ = 0.0;
    lastPredictTimestampNanos_ = 0;
    lastGnssArrivalNanos_ = 0;
    lastGnssMeasurementNanos_ = 0;
    stationaryTimerSec_ = 0.0;
    stationary_ = true;
    lastAcceptedGnssMs_ = 0.0;
    filteredGnssAccelMs2_ = 0.0;
    outputSpeedMs_ = 0.0;
}

double StrapdownINS15StateEKF::getGnssAgeSec() const noexcept {
    if (lastGnssArrivalNanos_ <= 0 || lastPredictTimestampNanos_ <= 0) return 1.0e9;
    const double age = static_cast<double>(lastPredictTimestampNanos_ - lastGnssArrivalNanos_) * 1e-9;
    return std::max(0.0, age);
}

double StrapdownINS15StateEKF::getSpeedSourceBlend() const noexcept {
    const double age = getGnssAgeSec();
    if (age <= 0.30) return 1.0;
    if (age >= 2.00) return 0.0;
    return 1.0 - ((age - 0.30) / 1.70);
}

double StrapdownINS15StateEKF::getTrueSpeedMs() const noexcept {
    return std::clamp(outputSpeedMs_, 0.0, SPEED_HARD_LIMIT_MS);
}

double StrapdownINS15StateEKF::computeRawTrueSpeedMs() const noexcept {
    const double ekfSpeed = std::clamp(state_[3], 0.0, SPEED_HARD_LIMIT_MS);

    if (lastGnssArrivalNanos_ <= 0) {
        return stationary_ ? 0.0 : ekfSpeed;
    }

    const double age = getGnssAgeSec();
    if (age <= 0.30) {
        // Taze Doppler: EKF ile hafifçe harmanla. Saf GNSS'i olduğu gibi
        // ibreye vermek 10 Hz'de merdiven basamağı ve kalan gürültüyü ekrana
        // yansıtır; bu harmanlama duyarlılığı korurken zıplamayı gizler.
        const double blended = 0.62 * gnssSpeedMs_ + 0.38 * ekfSpeed;
        return std::clamp(blended, 0.0, SPEED_HARD_LIMIT_MS);
    }
    if (age < 2.00) {
        const double blend = (age - 0.30) / 1.70;
        return std::clamp((1.0 - blend) * gnssSpeedMs_ + blend * ekfSpeed,
                          0.0, SPEED_HARD_LIMIT_MS);
    }

    // GNSS uzun süredir yok: ivme entegrasyonunun sürüklenmesini fiziksel olarak
    // makul bir üst sınırla kısıtla. Bu, tünel/orman gibi yerlerde hızın gökyüzüne
    // fırlamasını engeller.
    const double plausible = lastAcceptedGnssMs_ + 5.0 * std::min(age, 4.0);
    return std::clamp(std::min(ekfSpeed, plausible), 0.0, SPEED_HARD_LIMIT_MS);
}

void StrapdownINS15StateEKF::zeroLeanAndPitchNow() noexcept {
    state_[6] = 0.0;
    state_[7] = 0.0;
}

void StrapdownINS15StateEKF::propagateSingleStep(
    double dt,
    const math::Vec3d& accelBike,
    const math::Vec3d& gyroBike
) noexcept {
    // 400 Hz'lik IMU akışında zaman damgası boşlukları (ör. GC/thread pause) hızın
    // tek adımda fırlamasına yol açmasın. 20 ms'den büyük boşluklar alt adımlara
    // bölünmüş gibi kabul edilir.
    const double safeDt = std::clamp(dt, 0.0005, 0.020);

    // Jiroskop bias arındırılmış açısal hızlar [rad/s]
    const double wx = gyroBike.x - state_[12]; // Roll rate (Yatış açısal hızı)
    const double wy = gyroBike.y - state_[13]; // Pitch rate (Y_b sola bakar: +wy = burun aşağı)
    const double wz = gyroBike.z - state_[14]; // Yaw rate (Sapma açısal hızı)

    const double totalAccelNorm = accelBike.norm();
    const double gyroNorm = gyroBike.norm();
    const double accelDeviation = std::abs(totalAccelNorm - GRAVITY_MPS2);
    const double fusedSpeed = std::clamp(state_[3], 0.0, SPEED_HARD_LIMIT_MS);

    // Duruyor muyuz? GNSS taze ise ona güven; değilse IMU titreşimine bak.
    const bool gnssFresh = getGnssAgeSec() < 1.0;
    const bool imuQuiet = (gyroNorm < 0.045) && (accelDeviation < 0.22);
    const bool slowEnough = gnssFresh ? (gnssSpeedMs_ < 0.75) : (fusedSpeed < 0.30);
    const bool isStationary = slowEnough && imuQuiet;

    if (isStationary) {
        stationaryTimerSec_ += safeDt;
    } else if (fusedSpeed > 0.9 || gyroNorm > 0.08 || accelDeviation > 0.35) {
        stationaryTimerSec_ = 0.0;
    }
    // 450 ms boyunca kesintisiz sakinlik varsa gerçekten duruyoruz.
    stationary_ = (stationaryTimerSec_ > 0.45 && fusedSpeed < 1.0);

    // -------------------------------------------------------------------------
    // Yokuş eğimi (phi_pitch): jiroskop entegrasyonu (burun-yukarı pozitif => -wy)
    // İvmeölçerden mutlak eğim düzeltmesi YALNIZCA hareketsizken yapılır.
    // -------------------------------------------------------------------------
    double pitchRad = state_[7] - wy * safeDt;
    if (isStationary) {
        const double accelPitch = std::asin(std::clamp(accelBike.x / GRAVITY_MPS2, -0.7, 0.7));
        pitchRad = 0.985 * pitchRad + 0.015 * accelPitch;
    }
    state_[7] = std::clamp(pitchRad, -1.20, 1.20);

    // -------------------------------------------------------------------------
    // Bölüm 2.2: Yokuş eğimi (phi_pitch) ve sensör sapması (b_ax) arındırılmış
    // gerçek ileri ivme. Motorsiklet için fiziksel ivme sınırı ±5 m/s²'dir;
    // bunun üzerindeki değerler sensör darbesi/kötü montaj olarak kırpılır.
    // -------------------------------------------------------------------------
    double axTrue = accelBike.x - GRAVITY_MPS2 * std::sin(state_[7]) - state_[9];
    axTrue = std::clamp(axTrue, -5.0, 5.0);
    const double ayTrue = std::clamp(accelBike.y - state_[10], -8.0, 8.0);

    if (stationary_) {
        axTrue = 0.0;
    }

    // İvme türevi (Jerk) - vites geçişi ve ani fren tespiti için
    const double rawJerkX = (axTrue - prevTrueAxMs2_) / safeDt;
    filteredJerkXMs3_ = 0.82 * filteredJerkXMs3_ + 0.18 * std::clamp(rawJerkX, -80.0, 80.0);
    prevTrueAxMs2_ = axTrue;
    lastTrueAxMs2_ = axTrue;
    lastTrueAyMs2_ = ayTrue;

    // -------------------------------------------------------------------------
    // Bölüm 2.2: Anlık gerçek hız entegrasyonu.
    // -------------------------------------------------------------------------
    double vTrue = fusedSpeed + axTrue * safeDt;

    // Sıfır Hız Güncellemesi (ZUPT): dururken mikro gürültü hızı sürüklemesin.
    if (isStationary && std::abs(axTrue) < 0.28 && gyroNorm < 0.055) {
        vTrue = 0.0;
        lastTrueAxMs2_ = 0.0;
        filteredJerkXMs3_ = 0.0;
    }

    // Geri vites yok; motorsiklet hızı negatif olamaz ve fiziksel üst sınırı aşamaz.
    state_[3] = std::clamp(vTrue, 0.0, SPEED_HARD_LIMIT_MS);
    state_[0] += state_[3] * safeDt;

    // -------------------------------------------------------------------------
    // Bölüm 2.4: Gerçek Viraj Yatış Açısı (True Lean Angle - theta_lean)
    // -------------------------------------------------------------------------
    const double leanPrev = state_[6];
    const double yawRateWorld = wz * std::cos(leanPrev) + wy * std::sin(leanPrev);
    state_[8] += yawRateWorld * safeDt;

    const double thetaKinematic = std::atan(-(state_[3] * yawRateWorld) / GRAVITY_MPS2);
    const double thetaGyroIntegrated = leanPrev + wx * safeDt;
    double thetaLean = 0.985 * thetaGyroIntegrated + 0.015 * thetaKinematic;

    if (state_[3] < 1.5 && std::abs(wz) < 0.08) {
        const double staticRoll = std::atan2(accelBike.y, std::max(1.0, accelBike.z));
        thetaLean = 0.98 * thetaLean + 0.02 * staticRoll;
    }
    state_[6] = std::clamp(thetaLean, -1.22, 1.22);

    // Kovaryans tahmini (hız / b_ax / pitch alt bloğu): P = F P F^T + Q
    const double qVel = 0.030 * safeDt;
    const double qBiasA = 1e-4 * safeDt;
    const double qPitch = 5e-4 * safeDt;
    const double dVdPitch = -GRAVITY_MPS2 * std::cos(state_[7]) * safeDt;

    P_[3] += (P_[9] * safeDt * safeDt) - (2.0 * P_v_bax_ * safeDt)
           + (P_[7] * dVdPitch * dVdPitch) + (2.0 * P_v_pitch_ * dVdPitch) + qVel;
    P_v_bax_ -= P_[9] * safeDt;
    P_v_pitch_ += P_[7] * dVdPitch;
    P_[7] += qPitch;
    P_[9] += qBiasA;

    // ---------------------------------------------------------------------
    // İbre/ekran hız çıktısı: sabit zaman sabitli birinci dereceden alçak geçiş.
    // 10 Hz Doppler'un merdiven basamaklarını ve EKF düzeltmelerinin kalan
    // gürültüsünü yumuşatır; hız 0.12 s gecikmeyle gerçeğe yumuşak yaklaşır.
    // ---------------------------------------------------------------------
    const double rawSpeed = computeRawTrueSpeedMs();
    const double alpha = 1.0 - std::exp(-safeDt / 0.12);
    outputSpeedMs_ += (rawSpeed - outputSpeedMs_) * alpha;
}

void StrapdownINS15StateEKF::predict400Hz(
    int64_t timestampNanos,
    double dtSec,
    const math::Vec3d& accelBike,
    const math::Vec3d& gyroBike
) noexcept {
    lastPredictTimestampNanos_ = std::max(lastPredictTimestampNanos_, timestampNanos);
    propagateSingleStep(dtSec, accelBike, gyroBike);

    // OOSM Halka Tamponuna (Ring Buffer) kaydet
    EkfHistorySlot& slot = ringBuffer_[ringHead_];
    slot.timestampNanos = timestampNanos;
    slot.dtSec = dtSec;
    slot.accelBike = accelBike;
    slot.gyroBike = gyroBike;
    slot.state = state_;
    slot.diagP = P_;

    ringHead_ = (ringHead_ + 1) % OOSM_RING_BUFFER_SIZE;
    if (ringCount_ < OOSM_RING_BUFFER_SIZE) {
        ringCount_++;
    }
}

void StrapdownINS15StateEKF::updateGnssDopplerOosm(
    int64_t measurementTimestampNanos,
    double vNorthMs,
    double vEastMs,
    double vDownMs,
    double speedAccuracyMs,
    double gnssLagSec
) noexcept {
    // -------------------------------------------------------------------------
    // 1) Ölçüm kapısı (gating): bozuk/absürt GNSS paketleri hızı asla kirletmesin.
    //    Yatay Doppler büyüklüğü kullanılır; düşey bileşen (vDown) tek başına
    //    GPS yükseklik gürültüsü nedeniyle hız sıçraması üretir.
    // -------------------------------------------------------------------------
    if (!std::isfinite(vNorthMs) || !std::isfinite(vEastMs) || !std::isfinite(speedAccuracyMs)) {
        return;
    }
    const double vGnss = std::sqrt(vNorthMs * vNorthMs + vEastMs * vEastMs);
    if (!std::isfinite(vGnss) || vGnss > SPEED_HARD_LIMIT_MS) {
        return;
    }
    const double acc = std::clamp(
        std::isfinite(speedAccuracyMs) ? speedAccuracyMs : 2.0,
        0.05,
        8.0
    );

    lastGnssMeasurementNanos_ = measurementTimestampNanos;
    lastGnssArrivalNanos_ = std::max(lastPredictTimestampNanos_, measurementTimestampNanos);
    lastAcceptedGnssMs_ = vGnss;
    gnssSpeedAccuracyMs_ = acc;

    // -------------------------------------------------------------------------
    // 2) Hareketsizlik histerezisi: 0.8 m/s altındaki Doppler gürültüsü ekranda
    //    1-2 km/h titreme yapmasın. 0.7 s boyunca sakin kalırsa hız tam 0'a çekilir.
    // -------------------------------------------------------------------------
    // İvmeölçer sapmasını uzlaştırıcı öğrenmenin tek zaman kaynağı 400 Hz IMU
    // döngüsüdür (orası gerçek dt ile artırır); burada ek +0.10 puan eklemek
    // sayacı iki kat hızlı şişirip 0.70 s kapısını ~0.35 s'de tetikliyordu.
    if (vGnss > 1.60) {
        stationaryTimerSec_ = 0.0;
        stationary_ = false;
    }
    if (stationaryTimerSec_ > 0.70) {
        stationary_ = true;
        gnssSpeedMs_ = 0.0;
        state_[3] = 0.0;
        // Dururken ivmeölçer sapmasını yavaşça öğren (mount açısı hatasını azaltır).
        // axTrue > 0 iken sapma tahminini artırmak, g sin(phi) arındırmasının kalan
        // ofsetini sıfıra çeker; işaret pozitif yöndedir.
        state_[9] = std::clamp(state_[9] + lastTrueAxMs2_ * 0.002, -1.5, 1.5);
        return;
    }

    gnssSpeedMs_ = vGnss;

    if (ringCount_ == 0) {
        state_[3] = vGnss;
        return;
    }

    // -------------------------------------------------------------------------
    // 3) OOSM: ölçümün gerçek zamanındaki (t - tau_lag) tahmini bul, yeniliği
    //    (innovation) hesapla. Geçmiş durumu geri yükleyip tüm tamponu yeniden
    //    entegre etmek yerine düzeltme güncel duruma uygulanır; böylece GNSS
    //    paketi geldiğinde ibre/hız sıçraması oluşmaz.
    // -------------------------------------------------------------------------
    const int64_t effectiveTimestampNanos =
        measurementTimestampNanos - static_cast<int64_t>(std::max(0.0, gnssLagSec) * 1e9);

    size_t matchedIdx = (ringHead_ + OOSM_RING_BUFFER_SIZE - 1) % OOSM_RING_BUFFER_SIZE;
    for (size_t i = 0; i < ringCount_; ++i) {
        const size_t idx = (ringHead_ + OOSM_RING_BUFFER_SIZE - 1 - i) % OOSM_RING_BUFFER_SIZE;
        matchedIdx = idx;
        if (ringBuffer_[idx].timestampNanos <= effectiveTimestampNanos) {
            break;
        }
    }

    const double vPredictedAtLag = std::clamp(ringBuffer_[matchedIdx].state[3], 0.0, SPEED_HARD_LIMIT_MS);
    const double rawInnovation = vGnss - vPredictedAtLag;

    // 3-sigma + 1.5 m/s kapısı: tek bir GPS sıçraması 15-20 km/h zıplama yapamaz.
    const double innovationLimit = std::max(1.5, 3.0 * acc + 0.75);
    const double innovation = std::clamp(rawInnovation, -innovationLimit, innovationLimit);

    // Kalman kazancı (hız kanalı) + ölçüm doğruluğuna bağlı tamamlayıcı füzyon.
    const double S = std::max(1e-6, P_[3] + acc * acc);
    const double K_v = std::clamp(P_[3] / S, 0.05, 0.65);
    const double quality = std::clamp(1.0 - (acc / 3.0), 0.0, 1.0);
    const double measurementWeight = std::clamp(0.28 + 0.52 * quality, 0.28, 0.80);

    const double kalmanCorrected = state_[3] + K_v * innovation;
    double fused = (1.0 - measurementWeight) * kalmanCorrected + measurementWeight * vGnss;

    // GNSS hızı ile EKF tahmini arasındaki fark ani ise (kötü fix) daha yavaş yaklaş.
    const double delta = std::abs(vGnss - state_[3]);
    if (delta > 6.0 && acc > 0.6) {
        fused = 0.85 * state_[3] + 0.15 * vGnss;
    }

    state_[3] = std::clamp(fused, 0.0, SPEED_HARD_LIMIT_MS);

    // Sapma/eğim düzeltmeleri: yeniliğin küçük bir kısmı, uzun vadeli sürüklenmeyi
    // düzeltir ama tek bir gürültülü fix'in duruş açısını bozmasına izin vermez.
    state_[9] = std::clamp(state_[9] + 0.04 * K_v * innovation, -2.0, 2.0);
    state_[7] = std::clamp(state_[7] + 0.015 * K_v * innovation, -1.20, 1.20);

    P_[3] = std::max(1e-6, P_[3] * (1.0 - K_v * 0.65));
    P_[9] = std::max(1e-6, P_[9] * 0.999);
    P_v_bax_ *= 0.90;
    P_v_pitch_ *= 0.90;
}

double StrapdownINS15StateEKF::computeDisplayedSpeedKmh(
    bool useOemFactoryBiasMode,
    double oemBiasRatio,
    bool enableJapan299Lock,
    bool& outIs299Locked
) const noexcept {
    const double vTrueKmh = getTrueSpeedMs() * 3.6;
    if (vTrueKmh < 1.0) {
        outIs299Locked = false;
        return 0.0;
    }

    double vDisplayKmh = vTrueKmh;
    if (useOemFactoryBiasMode) {
        // Bölüm 2.3.2: OEM Fabrika Kalibrasyonu Modu (ECE-R39 Standardı)
        // v_display = v_true * (1.0 + bias) + 1.5 km/s (Varsayılan +%5 + 1.5 km/s)
        const double clampedBias = std::clamp(oemBiasRatio, 0.0, 0.10);
        vDisplayKmh = vTrueKmh * (1.0 + clampedBias) + 1.5;
    }

    if (enableJapan299Lock && vDisplayKmh >= 299.0) {
        outIs299Locked = true;
        return 299.0;
    }

    outIs299Locked = false;
    return vDisplayKmh;
}

} // namespace oemcluster::kalman
