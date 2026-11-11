#include "MountCalibrator.hpp"
#include <cmath>

namespace oemcluster::kalman {

MountCalibrator::MountCalibrator() noexcept {
    rebuildOrthonormalMatrix();
}

void MountCalibrator::triggerManualZeroMountCalibration(const math::Vec3d& currentPhoneAccel) noexcept {
    const double n = currentPhoneAccel.norm();
    if (n > 5.0 && n < 15.0) {
        z_bike_in_phone_ = currentPhoneAccel.normalized();
        gravityAligned_ = true;
        rebuildOrthonormalMatrix();
    }
}

void MountCalibrator::ingestImuSample(
    const math::Vec3d& accelPhone,
    const math::Vec3d& gyroPhone,
    double currentSpeedMs
) noexcept {
    const double accelNorm = accelPhone.norm();
    const double gyroNorm = gyroPhone.norm();

    // 1. Statik Yerçekimi Vektörü (v == 0 iken): Z_b = g_p / ||g_p||
    if (currentSpeedMs < 0.35 && gyroNorm < 0.06 && std::abs(accelNorm - 9.80665) < 0.45) {
        gravityAccumulator_ += accelPhone;
        staticSampleCount_++;

        if (staticSampleCount_ >= 80) { // 400 Hz'de 200 ms hareketsiz pencere
            const math::Vec3d avgG = gravityAccumulator_.normalized();
            if (!gravityAligned_) {
                z_bike_in_phone_ = avgG;
                gravityAligned_ = true;
            } else {
                // Yumuşak üstel güncelleme (süspansiyon çökmesi telafisi)
                z_bike_in_phone_ = (z_bike_in_phone_ * 0.96 + avgG * 0.04).normalized();
            }
            gravityAccumulator_ = {0.0, 0.0, 0.0};
            staticSampleCount_ = 0;
            rebuildOrthonormalMatrix();
        }
    }

    // 2. Dinamik İleri İvme Vektörü (PCA):
    // İlk ileri harekette yatay düzlemdeki ivme vektörlerine PCA uygulanarak X_b tespit edilir.
    if (gravityAligned_ && currentSpeedMs > 1.2 && gyroNorm < 0.18) {
        // Yatay düzleme izdüşüm: a_horiz = a_p - (a_p . Z_b) * Z_b
        const double verticalComponent = accelPhone.dot(z_bike_in_phone_);
        const math::Vec3d aHoriz = accelPhone - (z_bike_in_phone_ * verticalComponent);
        const double horizMag = aHoriz.norm();

        // Belirgin ileri ivmelenme veya frenleme anlarını PCA kovaryansına ekle (> 1.1 m/s^2)
        if (horizMag > 1.1 && horizMag < 14.0) {
            horizontalAccelCovariance_.addScaledOuterProduct(aHoriz, 1.0);
            dynamicPcaSampleCount_++;

            // Hız artarken ivme vektörü ileri yönü (+X_b) işaret eder
            if (horizMag > 1.5) {
                forwardDirectionSignAccumulator_ += aHoriz;
            }

            if (dynamicPcaSampleCount_ >= 60) {
                math::Vec3d principalAxis = horizontalAccelCovariance_.largestEigenvectorSymmetric(25);
                if (principalAxis.normSquared() > 0.5) {
                    // İşaret belirsizliğini çöz (İleri ivme yönü +X_b olmalı)
                    if (forwardDirectionSignAccumulator_.normSquared() > 0.1) {
                        if (principalAxis.dot(forwardDirectionSignAccumulator_) < 0.0) {
                            principalAxis = principalAxis * -1.0;
                        }
                    } else if (principalAxis.dot(x_bike_in_phone_) < 0.0) {
                        principalAxis = principalAxis * -1.0;
                    }

                    x_bike_in_phone_ = principalAxis.normalized();
                    forwardPcaAligned_ = true;
                    rebuildOrthonormalMatrix();
                }
            }
        }
    }
}

void MountCalibrator::rebuildOrthonormalMatrix() noexcept {
    z_bike_in_phone_ = z_bike_in_phone_.normalized();
    if (z_bike_in_phone_.normSquared() < 0.5) {
        z_bike_in_phone_ = {0.0, 0.9659, 0.2588};
    }

    // X_b'yi Z_b'ye dikleştir (Gram-Schmidt)
    math::Vec3d xOrtho = x_bike_in_phone_ - (z_bike_in_phone_ * x_bike_in_phone_.dot(z_bike_in_phone_));
    if (xOrtho.normSquared() < 1e-6) {
        // Z_b ile paralel düşerse yedek dik eksen seç
        const math::Vec3d fallback = (std::abs(z_bike_in_phone_.z) < 0.8)
            ? math::Vec3d{0.0, 0.0, 1.0}
            : math::Vec3d{1.0, 0.0, 0.0};
        xOrtho = fallback - (z_bike_in_phone_ * fallback.dot(z_bike_in_phone_));
    }
    x_bike_in_phone_ = xOrtho.normalized();

    // 3. Ortonormal Dönüşüm Matrisi: Y_b = Z_b x X_b
    y_bike_in_phone_ = z_bike_in_phone_.cross(x_bike_in_phone_).normalized();
    x_bike_in_phone_ = y_bike_in_phone_.cross(z_bike_in_phone_).normalized();

    // R_phone^bike matrisinin satırları: [X_b^T; Y_b^T; Z_b^T]
    R_phone_to_bike_ = math::Mat3d::fromRows(
        x_bike_in_phone_,
        y_bike_in_phone_,
        z_bike_in_phone_
    );
}

math::Vec3d MountCalibrator::transformAccelToBikeFrame(const math::Vec3d& accelPhone) const noexcept {
    return R_phone_to_bike_.multiply(accelPhone);
}

math::Vec3d MountCalibrator::transformGyroToBikeFrame(const math::Vec3d& gyroPhone) const noexcept {
    return R_phone_to_bike_.multiply(gyroPhone);
}

} // namespace oemcluster::kalman

