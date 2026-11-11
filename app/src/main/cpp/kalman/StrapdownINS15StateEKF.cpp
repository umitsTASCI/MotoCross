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
    const double safeDt = std::clamp(dt, 0.0005, 0.050);

    // Jiroskop bias arındırılmış açısal hızlar [rad/s]
    const double wx = gyroBike.x - state_[12]; // Roll rate (Yatış açısal hızı)
    const double wy = gyroBike.y - state_[13]; // Pitch rate (Y_b sola bakar: +wy = burun aşağı)
    const double wz = gyroBike.z - state_[14]; // Yaw rate (Sapma açısal hızı)

    const double totalAccelNorm = accelBike.norm();
    const double gyroNorm = gyroBike.norm();
    const bool isStationary =
        (state_[3] < 0.30) && (gyroNorm < 0.05) && (std::abs(totalAccelNorm - GRAVITY_MPS2) < 0.30);

    // -------------------------------------------------------------------------
    // Yokuş eğimi (phi_pitch): jiroskop entegrasyonu (burun-yukarı pozitif => -wy)
    // İvmeölçerden mutlak eğim düzeltmesi YALNIZCA hareketsizken yapılır; aksi halde
    // sürekli ileri ivme "yokuş" sanılıp hız entegrasyonundan silinirdi. Hareket
    // halinde eğim, GNSS güncellemesindeki P(v, pitch) çapraz kovaryansı ile düzeltilir.
    // -------------------------------------------------------------------------
    double pitchRad = state_[7] - wy * safeDt;
    if (isStationary) {
        const double accelPitch = std::asin(std::clamp(accelBike.x / GRAVITY_MPS2, -0.7, 0.7));
        pitchRad = 0.98 * pitchRad + 0.02 * accelPitch;
    }
    state_[7] = std::clamp(pitchRad, -1.25, 1.25);

    // -------------------------------------------------------------------------
    // Bölüm 2.2: Yokuş eğimi (phi_pitch) ve sensör sapması (b_ax) arındırılmış
    // gerçek ileri ivme:
    // a_{x,true}(t) = a_{x,bike}(t) - g * sin(phi_pitch(t)) - b_{ax}(t)
    // -------------------------------------------------------------------------
    const double axTrue = accelBike.x - GRAVITY_MPS2 * std::sin(state_[7]) - state_[9];
    const double ayTrue = accelBike.y - state_[10];

    // İvme türevi (Jerk: j_x = da_x / dt) - Bölüm 3.3 Katman 2 vites geçişi için
    const double rawJerkX = (axTrue - prevTrueAxMs2_) / safeDt;
    filteredJerkXMs3_ = 0.78 * filteredJerkXMs3_ + 0.22 * rawJerkX;
    prevTrueAxMs2_ = axTrue;
    lastTrueAxMs2_ = axTrue;
    lastTrueAyMs2_ = ayTrue;

    // -------------------------------------------------------------------------
    // Bölüm 2.2: Anlık gerçek hız her 2.5 ms'de bir entegre edilir:
    // v_{true}(t + dt) = v_{true}(t) + a_{x,true}(t) * dt
    // -------------------------------------------------------------------------
    double vTrue = state_[3] + axTrue * safeDt;

    // Hareketsizlik (ZUPT - Zero Velocity Update): dururken mikro gürültünün hızı sürüklemesini önler
    if (vTrue < 0.25 && std::abs(axTrue) < 0.22 && gyroNorm < 0.04) {
        vTrue = 0.0;
        lastTrueAxMs2_ = 0.0;
        filteredJerkXMs3_ = 0.0;
    }
    state_[3] = std::max(0.0, vTrue);

    // Konum entegrasyonu
    state_[0] += state_[3] * safeDt;

    // -------------------------------------------------------------------------
    // Bölüm 2.4: Gerçek Viraj Yatış Açısı (True Lean Angle - theta_lean)
    // Yatık gövdede dünya düşeyi etrafındaki gerçek sapma hızı gövde Z ve Y eksenlerine bölünür:
    //   w_yaw = w_z * cos(theta) + w_y * sin(theta)
    //   theta_kinematic = arctan(-(v_true * w_yaw) / g)        [işaret: sağa yatış pozitif]
    //   theta_lean(t) = 0.985 * (theta_lean(t - dt) + w_{x,bike} * dt) + 0.015 * theta_kinematic
    // -------------------------------------------------------------------------
    const double leanPrev = state_[6];
    const double yawRateWorld = wz * std::cos(leanPrev) + wy * std::sin(leanPrev);
    state_[8] += yawRateWorld * safeDt; // Yaw açısı

    const double thetaKinematic = std::atan(-(state_[3] * yawRateWorld) / GRAVITY_MPS2);
    const double thetaGyroIntegrated = leanPrev + wx * safeDt;
    double thetaLean = 0.985 * thetaGyroIntegrated + 0.015 * thetaKinematic;

    // Çok düşük hızda (< 1.5 m/s) yerçekiminin yanal bileşeninden yumuşak mutlak düzeltme
    if (state_[3] < 1.5 && std::abs(wz) < 0.08) {
        const double staticRoll = std::atan2(accelBike.y, std::max(1.0, accelBike.z));
        thetaLean = 0.98 * thetaLean + 0.02 * staticRoll;
    }
    state_[6] = std::clamp(thetaLean, -1.22, 1.22); // Maks ±70° fiziksel sınır

    // Kovaryans tahmin güncellemesi (hız / b_ax / pitch alt bloğu): P = F P F^T + Q
    // F: dv/db_ax = -dt, dv/dphi = -g*cos(phi)*dt
    const double qVel = 0.035 * safeDt;
    const double qBiasA = 1e-4 * safeDt;
    const double qPitch = 5e-4 * safeDt;
    const double dVdPitch = -GRAVITY_MPS2 * std::cos(state_[7]) * safeDt;

    P_[3] += (P_[9] * safeDt * safeDt) - (2.0 * P_v_bax_ * safeDt)
           + (P_[7] * dVdPitch * dVdPitch) + (2.0 * P_v_pitch_ * dVdPitch) + qVel;
    P_v_bax_ -= P_[9] * safeDt;
    P_v_pitch_ += P_[7] * dVdPitch;
    P_[7] += qPitch;
    P_[9] += qBiasA;
}

void StrapdownINS15StateEKF::predict400Hz(
    int64_t timestampNanos,
    double dtSec,
    const math::Vec3d& accelBike,
    const math::Vec3d& gyroBike
) noexcept {
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
    // 3D Doppler hızı: v_GNSS = sqrt(v_N^2 + v_E^2 + v_D^2)
    const double vGnss3D = std::sqrt(
        vNorthMs * vNorthMs +
        vEastMs * vEastMs +
        vDownMs * vDownMs
    );

    const double R_meas = std::max(0.02, speedAccuracyMs * speedAccuracyMs);
    const int64_t effectiveTimestampNanos =
        measurementTimestampNanos - static_cast<int64_t>(gnssLagSec * 1e9);

    if (ringCount_ == 0) {
        state_[3] = vGnss3D;
        return;
    }

    // Ring Buffer içinde (t - tau_lag) anına eşit/önceki en yeni durumu bul
    size_t matchedIdx = (ringHead_ + OOSM_RING_BUFFER_SIZE - 1) % OOSM_RING_BUFFER_SIZE;
    size_t stepsBack = 0;
    for (size_t i = 0; i < ringCount_; ++i) {
        const size_t idx = (ringHead_ + OOSM_RING_BUFFER_SIZE - 1 - i) % OOSM_RING_BUFFER_SIZE;
        matchedIdx = idx;
        stepsBack = i;
        if (ringBuffer_[idx].timestampNanos <= effectiveTimestampNanos) {
            break;
        }
    }

    // (t - tau_lag) anındaki tahmin ile GNSS Doppler hızı arasındaki yenilik (Innovation)
    const double vPredictedAtLag = ringBuffer_[matchedIdx].state[3];
    const double innovation = vGnss3D - vPredictedAtLag;

    // Kalman Kazancı K_k = P H^T (H P H^T + R)^-1, H = [.. 1 (v_x) ..]
    const double Pvv = P_[3];
    const double Pvb = P_v_bax_;
    const double Pvp = P_v_pitch_;
    const double S = Pvv + R_meas;
    const double K_v = Pvv / S;
    const double K_bax = Pvb / S;
    const double K_pitch = Pvp / S;

    // (t - tau_lag) anındaki durumu düzelt
    state_ = ringBuffer_[matchedIdx].state;
    state_[3] = std::max(0.0, state_[3] + K_v * innovation);
    state_[9] = std::clamp(state_[9] + K_bax * innovation, -2.5, 2.5);     // b_ax düzeltmesi
    state_[7] = std::clamp(state_[7] + K_pitch * innovation, -1.25, 1.25); // pitch düzeltmesi
    ringBuffer_[matchedIdx].state = state_;

    // Kovaryans güncellemesi: P = (I - K H) P
    P_[3] = std::max(1e-6, Pvv - K_v * Pvv);
    P_[9] = std::max(1e-6, P_[9] - K_bax * Pvb);
    P_[7] = std::max(1e-7, P_[7] - K_pitch * Pvp);
    P_v_bax_ = Pvb - K_v * Pvb;
    P_v_pitch_ = Pvp - K_v * Pvp;

    // (t - tau_lag) anından şimdiye (t) kadar tamponlanan 400 Hz IMU verilerini yeniden entegre et.
    // Düzeltilmiş durumlar tampona geri yazılır; aksi halde sonraki GNSS ölçümü eski
    // (düzeltilmemiş) geçmişe dönerdi. Jerk filtresi tekrar oynatmadan etkilenmemesi için korunur.
    const double savedJerk = filteredJerkXMs3_;
    for (size_t step = stepsBack; step > 0; --step) {
        const size_t replayIdx = (ringHead_ + OOSM_RING_BUFFER_SIZE - step) % OOSM_RING_BUFFER_SIZE;
        EkfHistorySlot& slot = ringBuffer_[replayIdx];
        propagateSingleStep(slot.dtSec, slot.accelBike, slot.gyroBike);
        slot.state = state_;
    }
    filteredJerkXMs3_ = savedJerk;
    prevTrueAxMs2_ = lastTrueAxMs2_;
}

double StrapdownINS15StateEKF::computeDisplayedSpeedKmh(
    bool useOemFactoryBiasMode,
    double oemBiasRatio,
    bool enableJapan299Lock,
    bool& outIs299Locked
) const noexcept {
    const double vTrueKmh = state_[3] * 3.6;
    if (vTrueKmh < 0.5) {
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
