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
     * Bölüm 2.2: Tahmin Adımı (400 Hz / her 2.5 ms'de bir)
     * - a_x,true(t) = a_x,bike(t) - g * sin(phi_pitch(t)) - b_ax(t)
     * - v_true(t + dt) = v_true(t) + a_x,true(t) * dt
     * - Bölüm 2.4: Gerçek Viraj Yatış Açısı (theta_lean) füzyonu
     */
    void predict400Hz(
        int64_t timestampNanos,
        double dtSec,
        const math::Vec3d& accelBike,
        const math::Vec3d& gyroBike
    ) noexcept;

    /**
     * Bölüm 2.2: Güncelleme Adımı (GNSS Geldiğinde - OOSM Ring Buffer)
     * GPS'ten gelen 3D Doppler hızı v_GNSS = sqrt(v_N^2 + v_E^2 + v_D^2),
     * halka tampon (Ring Buffer) içindeki (t - tau_lag) anındaki durumla kıyaslanarak
     * Kalman Kazancı K_k üzerinden v_true, b_ax ve phi_pitch sapmasını düzeltir ve
     * tamponu şu anki zamana (t) yeniden entegre eder.
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
    double getTrueLongitudinalAccelMs2() const noexcept { return lastTrueAxMs2_; }
    double getLateralAccelMs2() const noexcept { return lastTrueAyMs2_; }
    double getLongitudinalJerkMs3() const noexcept { return filteredJerkXMs3_; }
    double getLeanAngleRad() const noexcept { return state_[6]; }
    double getLeanAngleDeg() const noexcept { return state_[6] * (180.0 / 3.14159265358979323846); }
    double getPitchAngleRad() const noexcept { return state_[7]; }
    double getPitchAngleDeg() const noexcept { return state_[7] * (180.0 / 3.14159265358979323846); }
    double getAccelBiasX() const noexcept { return state_[9]; }
    void zeroLeanAndPitchNow() noexcept;

private:
    void propagateSingleStep(
        double dt,
        const math::Vec3d& accelBike,
        const math::Vec3d& gyroBike
    ) noexcept;

    std::array<double, 15> state_{};
    std::array<double, 15> P_{}; // 15-durumlu hata kovaryansı (köşegen + çapraz kuplaj)
    double P_v_bax_{0.0};        // Hız ve ivmeölçer sapması çapraz kovaryansı P(v_x, b_ax)
    double P_v_pitch_{0.0};      // Hız ve yokuş eğimi çapraz kovaryansı P(v_x, phi_pitch)

    double lastTrueAxMs2_{0.0};
    double prevTrueAxMs2_{0.0};
    double lastTrueAyMs2_{0.0};
    double filteredJerkXMs3_{0.0};

    // OOSM Halka Tampon (Ring Buffer)
    std::array<EkfHistorySlot, OOSM_RING_BUFFER_SIZE> ringBuffer_{};
    size_t ringHead_{0};
    size_t ringCount_{0};
};

} // namespace oemcluster::kalman

