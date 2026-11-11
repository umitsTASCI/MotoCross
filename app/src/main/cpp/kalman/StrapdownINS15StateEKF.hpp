#pragma once

#include "../math/Matrix3x3.hpp"
#include <array>
#include <cstdint>

namespace oemcluster::kalman {

/**
 * 15-Durumlu EKF (15-State Extended Kalman Filter) Durum İndeksleri:
 *  [0..2]   : Konum hatası / yerel yerdeğiştirme [p_x, p_y, p_z] (m)
 *  [3..5]   : Şasi/NED Hız vektörü [v_x (v_true), v_y, v_z] (m/s)
 *  [6..8]   : Duruş (Attitude) açıları [roll (theta_lean), pitch (phi_pitch), yaw (psi)] (rad)
 *  [9..11]  : İvmeölçer sapması (Accelerometer Bias) [b_ax, b_ay, b_az] (m/s^2)
 *  [12..14] : Jiroskop sapması (Gyroscope Bias) [b_gx, b_gy, b_gz] (rad/s)
 */
struct EkfHistorySlot {
    int64_t timestampNanos{0};
    double dtSec{0.0025};
    math::Vec3d accelBike{0.0, 0.0, 0.0};
    math::Vec3d gyroBike{0.0, 0.0, 0.0};
    std::array<double, 15> state{};
    std::array<double, 15> diagP{};
};

class StrapdownINS15StateEKF {
public:
    static constexpr double GRAVITY_MPS2 = 9.80665;
    static constexpr size_t OOSM_RING_BUFFER_SIZE = 512; // 400 Hz'de 1.28 saniye geçmiş tamponu

    StrapdownINS15StateEKF() noexcept;

    void reset() noexcept;

    /**
     * Duruş (Stationary) hız ölü bölgesi (Deadband) ve GPS doğruluk eşiklerini yapılandırır.
     */
    void setGpsFilterParameters(
        double stationaryDeadbandKmh,
        double imuSmoothingFactor
    ) noexcept;

    /**
     * Bölüm 2.2: Tahmin Adımı (400 Hz / her 2.5 ms'de bir)
     * - GPS kilidi yoksa veya duruyorsa ivmeölçer sapmasının hızı kendi kendine artırmasını %100 engeller.
     * - Hareket halindeyken GPS hız çapası etrafında sıfır gecikmeli füzyon yapar.
     */
    void predict400Hz(
        int64_t timestampNanos,
        double dtSec,
        const math::Vec3d& accelBike,
        const math::Vec3d& gyroBike
    ) noexcept;

    /**
     * Bölüm 2.2: Güncelleme Adımı (GNSS Geldiğinde - OOSM Ring Buffer)
     */
    void updateGnssDopplerOosm(
        int64_t measurementTimestampNanos,
        double vNorthMs,
        double vEastMs,
        double vDownMs,
        double speedAccuracyMs,
        double gnssLagSec = 0.085
    ) noexcept;

    /**
     * Bölüm 2.3: "Saf GPS Hızı" vs. "OEM Fabrikasyon Gösterge Hızı" (ECE-R39)
     * ve Japon Superbike 299 km/s Kilidi hesaplaması.
     */
    double computeDisplayedSpeedKmh(
        bool useOemFactoryBiasMode,
        double oemBiasRatio,
        bool enableJapan299Lock,
        bool& outIs299Locked
    ) const noexcept;

    // Anlık Çıktı Erişimcileri
    double getTrueSpeedMs() const noexcept { return state_[3]; }
    double getTrueSpeedKmh() const noexcept { return state_[3] * 3.6; }
    double getTrueLongitudinalAccelMs2() const noexcept { return fusedLongitudinalAccelMs2_; }
    double getLateralAccelMs2() const noexcept { return lastTrueAyMs2_; }
    double getLongitudinalJerkMs3() const noexcept { return filteredJerkXMs3_; }
    double getLeanAngleRad() const noexcept { return state_[6]; }
    double getLeanAngleDeg() const noexcept { return state_[6] * (180.0 / 3.14159265358979323846); }
    double getPitchAngleRad() const noexcept { return state_[7]; }
    double getPitchAngleDeg() const noexcept { return state_[7] * (180.0 / 3.14159265358979323846); }
    double getAccelBiasX() const noexcept { return state_[9]; }
    bool hasActiveGnssLock() const noexcept { return hasValidGnssFix_; }
    double getLatestRawGnssSpeedKmh() const noexcept { return lastGnssSpeedMs_ * 3.6; }
    void zeroLeanAndPitchNow() noexcept;

private:
    void propagateSingleStep(
        int64_t timestampNanos,
        double dt,
        const math::Vec3d& accelBike,
        const math::Vec3d& gyroBike
    ) noexcept;

    std::array<double, 15> state_{};
    std::array<double, 15> P_{};
    double P_v_bax_{0.0};
    double P_v_pitch_{0.0};

    double lastTrueAxMs2_{0.0};
    double prevTrueAxMs2_{0.0};
    double fusedLongitudinalAccelMs2_{0.0};
    double gnssDerivedAccelMs2_{0.0};
    double lastTrueAyMs2_{0.0};
    double filteredJerkXMs3_{0.0};

    // GPS Çapası ve Hareketsizlik (Stationary ZUPT) Koruma Durumu
    bool hasValidGnssFix_{false};
    int64_t lastGnssTimestampNanos_{0};
    double lastGnssSpeedMs_{0.0};
    double prevGnssSpeedMs_{0.0};
    double stationaryDeadbandMs_{0.61}; // Varsayılan 2.2 km/h (0.61 m/s) altı = 0 km/h
    double imuBlendingWeight_{0.35};    // 0.0 = Saf GPS, 1.0 = Agresif IMU

    // OOSM Halka Tampon (Ring Buffer)
    std::array<EkfHistorySlot, OOSM_RING_BUFFER_SIZE> ringBuffer_{};
    size_t ringHead_{0};
    size_t ringCount_{0};
};

} // namespace oemcluster::kalman
