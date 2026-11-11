#pragma once

#include <array>
#include <cstdint>

namespace oemcluster::physics {

struct NativeBikeMechanicalConfig {
    int cylinderCount{1};
    int gearCount{6};                     // 4, 5 veya 6 Vites
    int frontSprocketTeeth{15};           // Z_front
    int rearSprocketTeeth{45};            // Z_rear
    double primaryReductionRatio{2.667};  // i_primary
    std::array<double, 6> gearRatios{2.667, 1.857, 1.421, 1.143, 0.956, 0.840}; // i_gear[1..6]

    // Arka Lastik Parametreleri (Bölüm 3.1)
    double rearTireWidthMm{150.0};        // W_mm
    double rearTireAspectPercent{60.0};   // AspectRatio_%
    double rearTireRimInch{17.0};         // D_rim_inch
    double rearTireCrownRadiusMm{78.0};   // r_crown
    double rearTireSquishFactor{0.974};   // k_squish

    // Motor Devir & Tork Parametreleri
    double idleRpm{1450.0};
    double maxTorqueRpm{7000.0};
    double maxTorqueNm{37.0};
    double maxPowerRpm{9000.0};
    double redlineRpm{10000.0};
    double revLimiterRpm{10500.0};
    double totalMassKg{242.0};
    double dragCdA{0.39};

    // Tork Eğrisi Tablosu (16 nokta)
    int torqueCurveCount{0};
    std::array<double, 16> torqueRpmPoints{};
    std::array<double, 16> torqueNmPoints{};

    // Her vites (1..6) için önceden veya dinamik hesaplanan RPM_opt[n]
    std::array<double, 6> optimalShiftRpmPerGear{9800.0, 9900.0, 10000.0, 10000.0, 10000.0, 10000.0};
};

struct VirtualEcuTelemetryOutput {
    double effectiveTireRadiusMm{306.9};
    double slipRatioLambda{0.0};
    std::array<double, 6> kinematicRpmPerGear{};

    int currentGear{0};          // 0 = Neutral (N), 1..6 = Vites
    bool isNeutral{true};
    bool isClutchPulled{false};
    int transmissionStateCode{0}; // 0:Neutral, 1:Engaged, 2:Upshift, 3:Downshift, 4:ClutchIn

    double actualEngineRpm{1450.0};
    double normalizedRpmRatio{0.145};
    double peakHoldRpm{1450.0};
    double rpmDerivativePerSec{0.0};

    double stage1ShiftRpm{8800.0};
    double stage2ShiftRpm{9800.0};
    int shiftLightStateCode{0};   // 0:Normal, 1:Stage1, 2:Stage2_Invert(8Hz), 3:OverRev(12Hz)
    bool shiftFlashPhaseOn{false};
    bool fullColorInversionActive{false};

    float gearShiftAnimProgress{1.0f};
    int ignitionBootPhaseCode{4}; // 1:Logo, 2:SweepUp, 3:SweepDown, 4:Live
    float ignitionSweepProgress{1.0f};
    float nightModeFactor{1.0f};  // 0.0 = Gündüz, 1.0 = Gece (500ms yumuşak geçiş)

    double estimatedWheelHp{0.0};
    double estimatedWheelTorqueNm{0.0};
    bool tractionControlActive{false};

    // Teker Derece Ölçer & Teker Metre (Wheelie Analyzer & Timer/Meter)
    bool isWheelieActive{false};
    bool isWheelieSummaryVisible{false};      // Teker indikten sonra 3.2 sn özet gösterimi
    double wheelieAngleDeg{0.0};              // Anlık teker açısı (°) [Yokuş eğimi arındırılmış]
    double currentWheelieDurationSec{0.0};    // Anlık/Son teker süresi (sn)
    double currentWheelieDistanceMeters{0.0}; // Anlık/Son teker mesafesi (m)
    double maxWheelieAngleDeg{0.0};           // Sürüş rekoru: Maksimum teker açısı (°)
    double longestWheelieDurationSec{0.0};    // Sürüş rekoru: En uzun teker süresi (sn)
    double longestWheelieDistanceMeters{0.0}; // Sürüş rekoru: En uzun teker mesafesi (m)
};

class VirtualEcuPhysicsEngine {
public:
    static constexpr double GRAVITY = 9.80665;
    static constexpr double PI = 3.14159265358979323846;

    VirtualEcuPhysicsEngine() noexcept;

    void setBikeMechanicalConfig(const NativeBikeMechanicalConfig& config) noexcept;
    void setShiftLightSettings(
        bool autoOptimalMode,
        double manualStage1Rpm,
        double manualStage2Rpm,
        double humanReactionTimeSec,
        double stage1WarnDeltaRpm = 1000.0
    ) noexcept;

    void triggerIgnitionNeedleSweep(int64_t currentTimestampNanos) noexcept;
    void updateAmbientLightLux(float ambientLux, int themeOverrideMode) noexcept;

    VirtualEcuTelemetryOutput stepPhysics120Hz(
        int64_t timestampNanos,
        double dtSec,
        double trueSpeedMs,
        double longitudinalAccelMs2,
        double longitudinalJerkMs3,
        double leanAngleRad,
        double pitchAngleRad,
        double acousticRpm,
        double acousticConfidence
    ) noexcept;

    void forceManualGearOrClutch(int forcedGear, bool clutchPulled, double throttleOverrideRpm) noexcept;

private:
    double interpolateEngineTorque(double rpm) const noexcept;
    void recomputeOptimalShiftRpms() noexcept;
    void registerGearChange(int newGear, int transmissionCode) noexcept;

    NativeBikeMechanicalConfig bike_{};

    // Shift-Light Ayarları
    bool autoOptimalShiftMode_{true};
    double manualStage1Rpm_{8800.0};
    double manualStage2Rpm_{9800.0};
    double humanReactionTimeSec_{0.18};
    double stage1WarnDeltaRpm_{1000.0};

    // Şanzıman ve İbre Fizik Durumu
    int currentGear_{0};
    int transmissionStateCode_{0};
    bool isClutchPulled_{false};

    // İkinci dereceden sönümlü yay-kütle ibre fiziği
    double currentDisplayedRpm_{1450.0};
    double currentRpmVelocity_{0.0};
    double filteredRpmDerivative_{0.0};

    // Vites geçiş algılayıcı
    bool upshiftDipDetected_{false};
    double upshiftDipElapsedSec_{0.0};
    double shiftCooldownSec_{0.0};
    double shiftStateHoldSec_{0.0};
    double prevAccelMs2_{0.0};
    double smoothedAccelMs2_{0.0};
    float gearShiftAnimProgress_{1.0f};

    // Peak-Hold Tepe Devir İzi
    double peakHoldRpm_{1450.0};
    double peakHoldTimerSec_{0.0};

    // Kontak Açılış Selamlaması
    int ignitionBootPhaseCode_{4};
    int64_t ignitionStartNanos_{0};

    // Gündüz / Gece 500ms geçişi
    float targetNightFactor_{1.0f};
    float currentNightFactor_{1.0f};

    // Manuel/Simülatör geçersiz kılma bayrakları
    int manualForcedGear_{-1};
    bool manualClutchOverride_{false};
    double manualThrottleRpm_{-1.0};

    // Bosch 6D IMU Tarzı Teker (Wheelie) Durum Makinesi
    double roadSlopeBaselineRad_{0.0};
    double prevPitchRad_{0.0};
    bool wheelieActive_{false};
    double currentWheelieDurationSec_{0.0};
    double currentWheelieDistanceMeters_{0.0};
    double currentWheeliePeakAngleDeg_{0.0};
    double maxWheelieAngleDeg_{0.0};
    double longestWheelieDurationSec_{0.0};
    double longestWheelieDistanceMeters_{0.0};
    double wheelieSummaryHoldSec_{0.0};
};

} // namespace oemcluster::physics
