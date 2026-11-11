#pragma once

#include "../math/Matrix3x3.hpp"

namespace oemcluster::kalman {

/**
 * Bölüm 2.1: Otomatik Gidon/Tutucu Açısı Kalibrasyonu (3D Mount Alignment Matrix)
 *
 * Telefonun sensör ekseni (X_p, Y_p, Z_p) ile motosikletin şasi eksenini
 * (X_b [İleri], Y_b [Sol], Z_b [Yukarı]) otomatik hizalar:
 *  1. Statik Yerçekimi Vektörü (v = 0): Z_b = g_p / ||g_p||
 *  2. Dinamik İleri İvme Vektörü (PCA): Yatay düzlemdeki ivme vektörlerine
 *     Temel Bileşenler Analizi (PCA) uygulanarak ileri eksen X_b tespit edilir.
 *  3. Ortonormal Dönüşüm Matrisi: Y_b = Z_b x X_b çapraz çarpımı ile R_phone^bike oluşturulur.
 *     Tüm 400 Hz IMU verileri a_bike = R_phone^bike * a_phone ve w_bike = R_phone^bike * w_phone
 *     olarak şasi eksenine çevrilir.
 */
class MountCalibrator {
public:
    MountCalibrator() noexcept;

    /**
     * Kullanıcı Ayarlar menüsünden "Telefon Duruş Açısını Şimdi Sıfırla (0° Yatış Açısı Kalibrasyonu)"
     * butonuna bastığında anlık ivmeölçer vektörünü yeni dikey eksen (Z_b) olarak kilitler.
     */
    void triggerManualZeroMountCalibration(const math::Vec3d& currentPhoneAccel) noexcept;

    /**
     * 400 Hz hızında gelen ham telefon ivmeölçer (a_phone) ve jiroskop (w_phone) örneklerini
     * işleyerek kalibrasyonu günceller.
     */
    void ingestImuSample(
        const math::Vec3d& accelPhone,
        const math::Vec3d& gyroPhone,
        double currentSpeedMs
    ) noexcept;

    /**
     * Ham telefon ivme vektörünü motosiklet şasi eksenine çevirir:
     * a_bike = R_phone^bike * a_phone
     */
    math::Vec3d transformAccelToBikeFrame(const math::Vec3d& accelPhone) const noexcept;

    /**
     * Ham telefon açısal hız vektörünü motosiklet şasi eksenine çevirir:
     * w_bike = R_phone^bike * w_phone
     */
    math::Vec3d transformGyroToBikeFrame(const math::Vec3d& gyroPhone) const noexcept;

    const math::Mat3d& getRotationPhoneToBike() const noexcept { return R_phone_to_bike_; }
    bool isGravityAligned() const noexcept { return gravityAligned_; }
    bool isForwardPcaAligned() const noexcept { return forwardPcaAligned_; }

private:
    void rebuildOrthonormalMatrix() noexcept;

    math::Vec3d z_bike_in_phone_{0.0, 0.9659, 0.2588}; // Varsayılan ~75° dikey gidon açısı
    math::Vec3d x_bike_in_phone_{0.0, -0.2588, 0.9659};
    math::Vec3d y_bike_in_phone_{-1.0, 0.0, 0.0};

    math::Mat3d R_phone_to_bike_{math::Mat3d::identity()};

    // Statik yerçekimi ortalaması için akümülatör
    math::Vec3d gravityAccumulator_{0.0, 0.0, 0.0};
    int staticSampleCount_{0};
    bool gravityAligned_{false};

    // Dinamik ileri ivme PCA kovaryans matrisi
    math::Mat3d horizontalAccelCovariance_{math::Mat3d::zero()};
    math::Vec3d forwardDirectionSignAccumulator_{0.0, 0.0, 0.0};
    int dynamicPcaSampleCount_{0};
    bool forwardPcaAligned_{false};
};

} // namespace oemcluster::kalman

