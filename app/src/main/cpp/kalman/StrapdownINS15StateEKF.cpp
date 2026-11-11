#include "StrapdownINS15StateEKF.hpp"
#include <cmath>
#include <algorithm>

namespace oemcluster::kalman {

StrapdownINS15StateEKF::StrapdownINS15StateEKF() noexcept {
    reset();
}

void StrapdownINS15StateEKF::reset() noexcept {
    state_.fill(0.0);
    P_[0] = 1.0;  P_[1] = 1.0;  P_[2] = 1.0;
    P_[3] = 0.25; P_[4] = 0.10; P_[5] = 0.10;
    P_[6] = 0.01; P_[7] = 0.01; P_[8] = 0.05;
    P_[9] = 0.08; P_[10] = 0.08; P_[11] = 0.08;
    P_[12] = 0.002; P_[13] = 0.002; P_[14] = 0.002;

    P_v_bax_ = 0.0;
    P_v_pitch_ = 0.0;
    lastTrueAxMs2_ = 0.0;
    prevTrueAxMs2_ = 0.0;
    fusedLongitudinalAccelMs2_ = 0.0;
    gnssDerivedAccelMs2_ = 0.0;
    lastTrueAyMs2_ = 0.0;
    filteredJerkXMs3_ = 0.0;

    hasValidGnssFix_ = false;
    lastGnssTimestampNanos_ = 0;
    lastGnssSpeedMs_ = 0.0;
    prevGnssSpeedMs_ = 0.0;

    ringHead_ = 0;
    ringCount_ = 0;
}

void StrapdownINS15StateEKF::setGpsFilterParameters(
    double stationaryDeadbandKmh,
    double imuSmoothingFactor
) noexcept {
    stationaryDeadbandMs_ = std::clamp(stationaryDeadbandKmh / 3.6, 0.20, 2.00);
    imuBlendingWeight_ = std::clamp(imuSmoothingFactor, 0.05, 1.0);
}

void StrapdownINS15StateEKF::zeroLeanAndPitchNow() noexcept {
    state_[6] = 0.0;
    state_[7] = 0.0;
}

void StrapdownINS15StateEKF::propagateSingleStep(
    int64_t timestampNanos,
    double dt,
    const math::Vec3d& accelBike,
    const math::Vec3d& gyroBike
) noexcept {
    const double safeDt = std::clamp(dt, 0.0005, 0.050);

    // Jiroskop bias arındırılmış açısal hızlar [rad/s]
    const double wx = gyroBike.x - state_[12];
    const double wy = gyroBike.y - state_[13];
    const double wz = gyroBike.z - state_[14];

    // Son geçerli GPS paketinden bu yana geçen süre [saniye]
    const double timeSinceGnssSec = (lastGnssTimestampNanos_ > 0 && timestampNanos >= lastGnssTimestampNanos_)
        ? static_cast<double>(timestampNanos - lastGnssTimestampNanos_) * 1e-9
        : 999.0;

    const bool gnssFresh = hasValidGnssFix_ && (timeSinceGnssSec <= 2.5);
    const bool gnssReportsMoving = gnssFresh && (lastGnssSpeedMs_ >= stationaryDeadbandMs_);

    // -------------------------------------------------------------------------
    // 1. PITCH (EĞİM / TEKER AÇISI) HESAPLAMASI
    // Dururken veya GPS hareketi yokken ivmeölçerin X bileşeni yerçekimi eğimidir;
    // böylece telefonu elde eğmek asla "ileri ivme" olarak algılanmaz!
    // -------------------------------------------------------------------------
    double pitchRad = state_[7] - wy * safeDt;
    const double accelPitch = std::asin(std::clamp(accelBike.x / GRAVITY_MPS2, -0.85, 0.85));

    if (!gnssReportsMoving) {
        // Dururken eğimi hızla yerçekimi vektörüne kilitle (telefon açısı değişse bile axTrue -> 0 kalır)
        pitchRad = 0.90 * pitchRad + 0.10 * accelPitch;
        // Kalan farkı da ivmeölçer bias'ına (b_ax) emdir
        const double residualAx = accelBike.x - GRAVITY_MPS2 * std::sin(pitchRad);
        state_[9] = 0.92 * state_[9] + 0.08 * std::clamp(residualAx, -3.5, 3.5);
    } else {
        // Hareket halindeyken jiroskop baskın, çok yavaş yerçekimi referansı
        pitchRad = 0.996 * pitchRad + 0.004 * accelPitch;
    }
    state_[7] = std::clamp(pitchRad, -1.25, 1.25);

    // -------------------------------------------------------------------------
    // 2. GERÇEK İLERİ İVME VE JERK
    // -------------------------------------------------------------------------
    const double rawAxTrue = accelBike.x - GRAVITY_MPS2 * std::sin(state_[7]) - state_[9];
    const double ayTrue = accelBike.y - state_[10];

    const double rawJerkX = (rawAxTrue - prevTrueAxMs2_) / safeDt;
    filteredJerkXMs3_ = 0.78 * filteredJerkXMs3_ + 0.22 * rawJerkX;
    prevTrueAxMs2_ = rawAxTrue;
    lastTrueAxMs2_ = rawAxTrue;
    lastTrueAyMs2_ = ayTrue;

    // -------------------------------------------------------------------------
    // 3. KESİN HIZ FÜZYONU (DURDUĞUN YERDE HIZ FIRLAMASINI %100 ÖNLEYEN MİMARİ)
    // -------------------------------------------------------------------------
    if (!gnssFresh) {
        // GPS kilidi yoksa veya 2.5 saniyedir GPS verisi gelmediyse (kapalı alan / duruş):
        // İvmeölçerin kendi kendine hız uydurmasını kesin olarak engelle!
        state_[3] *= std::exp(-safeDt / 0.25);
        if (state_[3] < 0.15) {
            state_[3] = 0.0;
        }
        fusedLongitudinalAccelMs2_ = 0.0;
        filteredJerkXMs3_ = 0.0;
    } else if (!gnssReportsMoving) {
        // GPS kilitli ve GPS hızı duruş eşiğinin (örn. 2.2 km/h) altında:
        // Hızı 0.0 km/h olarak kilitle!
        state_[3] = 0.0;
        fusedLongitudinalAccelMs2_ = 0.0;
    } else {
        // GPS kilitli ve motosiklet gerçekten hareket ediyor (v_GNSS >= deadband):
        // GPS ivmesi ile 400 Hz IMU ivmesini harmanla
        const double blendedAx = (1.0 - imuBlendingWeight_) * gnssDerivedAccelMs2_
                               + imuBlendingWeight_ * std::clamp(rawAxTrue, -12.0, 12.0);
        fusedLongitudinalAccelMs2_ = 0.85 * fusedLongitudinalAccelMs2_ + 0.15 * blendedAx;

        // 400 Hz hız entegrasyonu + GPS hız çapasına yumuşak çekim
        double vTrue = state_[3] + fusedLongitudinalAccelMs2_ * safeDt;
        const double pullAlpha = std::clamp(safeDt / 0.18, 0.01, 0.25);
        vTrue = (1.0 - pullAlpha) * vTrue + pullAlpha * lastGnssSpeedMs_;

        // IMU'nun son gerçek GPS hızından en fazla ±(1.8 m/s *imuWeight) sapmasına izin ver
        const double maxDeviationMs = 1.8 * imuBlendingWeight_;
        const double minAllowedV = std::max(0.0, lastGnssSpeedMs_ - maxDeviationMs);
        const double maxAllowedV = lastGnssSpeedMs_ + maxDeviationMs;
        vTrue = std::clamp(vTrue, minAllowedV, maxAllowedV);

        if (vTrue < stationaryDeadbandMs_ * 0.75) {
            vTrue = 0.0;
        }
        state_[3] = vTrue;
    }

    // Konum entegrasyonu
    state_[0] += state_[3] * safeDt;

    // -------------------------------------------------------------------------
    // 4. GERÇEK VİRAJ YATIŞ AÇISI (TRUE LEAN ANGLE - theta_lean)
    // -------------------------------------------------------------------------
    const double leanPrev = state_[6];
    const double yawRateWorld = wz * std::cos(leanPrev) + wy * std::sin(leanPrev);
    state_[8] += yawRateWorld * safeDt;

    const double thetaKinematic = std::atan(-(state_[3] * yawRateWorld) / GRAVITY_MPS2);
    const double thetaGyroIntegrated = leanPrev + wx * safeDt;
    double thetaLean = 0.985 * thetaGyroIntegrated + 0.015 * thetaKinematic;

    if (state_[3] < 2.0 && std::abs(wz) < 0.12) {
        const double staticRoll = std::atan2(accelBike.y, std::max(1.0, accelBike.z));
        thetaLean = 0.94 * thetaLean + 0.06 * staticRoll;
    }
    state_[6] = std::clamp(thetaLean, -1.22, 1.22);

    // Kovaryans güncellemesi
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
    propagateSingleStep(timestampNanos, dtSec, accelBike, gyroBike);

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
    double vGnss3D = std::sqrt(
        vNorthMs * vNorthMs +
        vEastMs * vEastMs +
        vDownMs * vDownMs
    );

    // Duruş Hız Ölü Bölgesi (Stationary Deadband):
    // GPS hızı deadband altındaysa (örn. < 2.2 km/h) veya doğruluk hatasının içindeyse 0 kabul et
    if (vGnss3D < stationaryDeadbandMs_ ||
        (vGnss3D < 1.25 && speedAccuracyMs > 1.1 && vGnss3D < speedAccuracyMs * 0.95)) {
        vGnss3D = 0.0;
    }

    // GPS hız türevinden gerçek ileri ivme hesabı (a_gps = dv_gps / dt)
    if (hasValidGnssFix_ && lastGnssTimestampNanos_ > 0 && measurementTimestampNanos > lastGnssTimestampNanos_) {
        const double dtGnss = std::clamp(
            static_cast<double>(measurementTimestampNanos - lastGnssTimestampNanos_) * 1e-9,
            0.04,
            2.50
        );
        const double rawGnssAccel = (vGnss3D - lastGnssSpeedMs_) / dtGnss;
        gnssDerivedAccelMs2_ = 0.60 * gnssDerivedAccelMs2_ + 0.40 * std::clamp(rawGnssAccel, -12.0, 12.0);
    } else {
        gnssDerivedAccelMs2_ = 0.0;
    }

    prevGnssSpeedMs_ = lastGnssSpeedMs_;
    lastGnssSpeedMs_ = vGnss3D;
    lastGnssTimestampNanos_ = measurementTimestampNanos;
    hasValidGnssFix_ = true;

    if (vGnss3D == 0.0) {
        state_[3] = 0.0;
        gnssDerivedAccelMs2_ = 0.0;
        fusedLongitudinalAccelMs2_ = 0.0;
        return;
    }

    const double R_meas = std::max(0.02, speedAccuracyMs * speedAccuracyMs);
    const int64_t effectiveTimestampNanos =
        measurementTimestampNanos - static_cast<int64_t>(gnssLagSec * 1e9);

    if (ringCount_ == 0) {
        state_[3] = vGnss3D;
        return;
    }

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

    const double vPredictedAtLag = ringBuffer_[matchedIdx].state[3];
    const double innovation = vGnss3D - vPredictedAtLag;

    const double Pvv = P_[3];
    const double Pvb = P_v_bax_;
    const double Pvp = P_v_pitch_;
    const double S = Pvv + R_meas;
    const double K_v = std::clamp(Pvv / S, 0.35, 0.95);
    const double K_bax = Pvb / S;
    const double K_pitch = Pvp / S;

    state_ = ringBuffer_[matchedIdx].state;
    state_[3] = std::max(0.0, state_[3] + K_v * innovation);
    state_[9] = std::clamp(state_[9] + K_bax * innovation, -2.5, 2.5);
    state_[7] = std::clamp(state_[7] + K_pitch * innovation, -1.25, 1.25);
    ringBuffer_[matchedIdx].state = state_;

    P_[3] = std::max(1e-6, Pvv - K_v * Pvv);
    P_[9] = std::max(1e-6, P_[9] - K_bax * Pvb);
    P_[7] = std::max(1e-7, P_[7] - K_pitch * Pvp);
    P_v_bax_ = Pvb - K_v * Pvb;
    P_v_pitch_ = Pvp - K_v * Pvp;

    const double savedJerk = filteredJerkXMs3_;
    for (size_t step = stepsBack; step > 0; --step) {
        const size_t replayIdx = (ringHead_ + OOSM_RING_BUFFER_SIZE - step) % OOSM_RING_BUFFER_SIZE;
        EkfHistorySlot& slot = ringBuffer_[replayIdx];
        propagateSingleStep(slot.timestampNanos, slot.dtSec, slot.accelBike, slot.gyroBike);
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
    if (vTrueKmh < (stationaryDeadbandMs_ * 3.6 * 0.85)) {
        outIs299Locked = false;
        return 0.0;
    }

    double vDisplayKmh = vTrueKmh;
    if (useOemFactoryBiasMode) {
        const double clampedBias = std::clamp(oemBiasRatio, 0.0, 0.12);
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
