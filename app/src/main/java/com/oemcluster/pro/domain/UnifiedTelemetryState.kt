package com.oemcluster.pro.domain

import kotlinx.serialization.Serializable

/**
 * Bölüm 4 & 7: Vites Uyarı Durumu (Shift-Light & Renk Tersleme Evreleri)
 */
@Serializable
enum class ShiftLightState(val nativeCode: Int) {
    NORMAL(0),
    STAGE_1_APPROACHING(1),    // Ön Uyarı
    STAGE_2_SHIFT_NOW(2),      // Tam Renk Terslemesi / 8 Hz Flaşör
    OVER_REV_LIMITER(3);       // Kesiciye Girme (12 Hz Flaşör + Kırmızı Alarm + ±150 RPM İbre Kesici Titremesi)

    companion object {
        fun fromNativeCode(code: Int): ShiftLightState =
            entries.firstOrNull { it.nativeCode == code } ?: NORMAL
    }
}

/**
 * Bölüm 3.3: Debriyaj ve Şanzıman Durum Makinesi
 */
@Serializable
enum class TransmissionEngagementState(val nativeCode: Int) {
    NEUTRAL(0),                // Dururken veya boşta: Yeşil 'N' lambası yanar
    ENGAGED_IN_GEAR(1),        // Viteste ve kavrama tam
    UPSHIFT_IN_PROGRESS(2),    // Quickshifter (40-80ms) veya Debriyajlı vites büyütme geçişi
    DOWNSHIFT_IN_PROGRESS(3),  // Ara gazlı (Rev-match blip) veya motor frenli vites küçültme geçişi
    CLUTCH_PULLED_COASTING(4); // Hareket halinde debriyaj çekili, devir hızdan bağımsız rölantiye düşer

    companion object {
        fun fromNativeCode(code: Int): TransmissionEngagementState =
            entries.firstOrNull { it.nativeCode == code } ?: NEUTRAL
    }
}

/**
 * Bölüm 4.1: Kontak Açılış Selamlaması (Ignition Boot & Needle Sweep) Evresi
 */
@Serializable
enum class IgnitionBootPhase(val nativeCode: Int) {
    OFF(0),
    OEM_SPLASH_LOGO(1),        // "READY TO RACE", "M-Power", "R-Spirit" vb. açılış logosu + tüm ikaz lambaları
    NEEDLE_SWEEP_UP(2),        // İbre 0 -> Redline, Hız 888 veya 299
    NEEDLE_SWEEP_RETURN(3),    // İbre Redline -> Idle/0, Hız -> 0
    RUNNING_LIVE(4);           // Gerçek zamanlı 120 FPS telemetri akışı

    companion object {
        fun fromNativeCode(code: Int): IgnitionBootPhase =
            entries.firstOrNull { it.nativeCode == code } ?: RUNNING_LIVE
    }
}

/**
 * Hız Kalibrasyon ve Birim Ayarları
 */
@Serializable
enum class SpeedCalibrationMode {
    PURE_GPS_TRUE_SPEED,       // v_display = v_true (Saf, hatasız gerçek GPS hızı)
    OEM_ECE_R39_FACTORY_BIAS   // v_display = v_true * (1.0 + bias) + 1.5 km/h
}

@Serializable
enum class SpeedUnit(val label: String, val factorFromKmh: Double) {
    KMH("km/h", 1.0),
    MPH("mph", 0.621371)
}

@Serializable
enum class RpmScalingMode {
    MODE_A_PROPORTIONAL_REDLINE, // RPM_ui = RPM_actual * (Gauge_Redline / Bike_Redline)
    MODE_B_NATIVE_1_TO_1         // İbre gerçek devri gösterir; Shift-Light aktif motorun gerçek devrinde çakar
}

@Serializable
enum class GearCalculationMode {
    AUTO_KINEMATIC_ECU,        // GPS Hızı + İvme + Motor Dişli/Lastik Profilinden Otomatik Vites & Devir
    SEMI_AUTO_WITH_BUTTONS,    // Otomatik + Ekranda Hızlı (+/-) Vites Müdahale Tuşları
    MANUAL_GEAR_LOCK           // Sürücü Vitesi (+/-) ile Manuel Seçer, Devir GPS Hızından Hesaplanır
}

/**
 * Teker Derece Ölçer & Teker Metre (Wheelie Analyzer) Görünüm Modu
 */
@Serializable
enum class WheelieGaugeDisplayMode {
    SMART_AUTO_POPUP, // Varsayılan: Normalde OEM saflığını korur; teker kalktığında + indikten sonra 3.2 sn zarifçe açılır
    ALWAYS_VISIBLE,   // Eğimölçer (Lean Angle) ile simetrik olarak kadranda her zaman görünür
    HIDDEN_OEM_PURE   // Kadranda gizli; yalnızca Yol Bilgisayarı & Drag Box rekorlarında tutulur
}

/**
 * Tek Yönlü Veri Akışı (Strict Unidirectional Data Flow) Çıktısı:
 * Sensors (GNSS + 400Hz IMU + Mic) -> Physics & Virtual ECU Engine (User's Active Bike Profile)
 * -> UnifiedTelemetryState -> Active Dashboard Renderer (Any of the 105 OEM Skins).
 */
@Serializable
data class UnifiedTelemetryState(
    val timestampNanos: Long = 0L,

    // Aktif Mekanik Motor Profili Bilgisi (Single Source of Truth)
    val activeBikeProfileId: String = "ktm_390_duke_gen2",
    val activeBikeDisplayName: String = "KTM 390 Duke",
    val bikeIdleRpm: Double = 1450.0,
    val bikeRedlineRpm: Double = 10200.0,
    val bikeRevLimiterRpm: Double = 10500.0,

    // Hız Verileri
    val trueSpeedMs: Double = 0.0,
    val trueSpeedKmh: Double = 0.0,
    val displayedSpeedKmh: Double = 0.0,
    val is299LockEngaged: Boolean = false,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,

    // Canlı GNSS / GPS Uydu ve Doğruluk Durumu
    val hasGpsFix: Boolean = false,
    val satellitesInUse: Int = 0,
    val satellitesVisible: Int = 0,
    val gpsAccuracyMeters: Float = 0.0f,
    val rawGpsSpeedKmh: Double = 0.0,

    // Devir (RPM) ve Shift-Light Verileri
    val actualEngineRpm: Double = 1450.0,       // Kullanıcının motorunun gerçek devri (RPM)
    val normalizedRpmRatio: Double = 0.14,      // [0.0 .. 1.0+] (actualEngineRpm / bikeRedlineRpm)
    val peakHoldRpm: Double = 1450.0,           // Vites atmadan önceki 1.8 sn asılı kalan tepe devir izi
    val rpmRateOfChangePerSec: Double = 0.0,    // d(RPM)/dt [RPM/s]
    val stage1ShiftRpm: Double = 8800.0,        // Aktif vites için hesaplanan Evre 1 uyarı devri
    val stage2ShiftRpm: Double = 9800.0,        // Aktif vites için hesaplanan Evre 2 flaşör devri
    val shiftLightState: ShiftLightState = ShiftLightState.NORMAL,
    val shiftFlashPhaseOn: Boolean = false,     // 8 Hz (Stage 2) veya 12 Hz (Over-Rev) anlık flaşör darbesi
    val fullColorInversionActive: Boolean = false, // Evre 2 ve Kesicide tam renk terslemesi aktif mi?

    // Vites ve Debriyaj Durumu
    val currentGear: Int = 0,                   // 0 = Neutral (N), 1..6 = Aktif Vites
    val isNeutral: Boolean = true,
    val isClutchPulled: Boolean = false,
    val transmissionState: TransmissionEngagementState = TransmissionEngagementState.NEUTRAL,
    val gearShiftAnimationProgress: Float = 1.0f,

    // Eğim, İvme ve Dinamik Lastik Fiziği
    val leanAngleDeg: Double = 0.0,             // -Sol .. +Sağ viraj yatış açısı (derece)
    val maxLeftLeanDeg: Double = 0.0,           // Sürüş maksimum sol yatış rekoru
    val maxRightLeanDeg: Double = 0.0,          // Sürüş maksimum sağ yatış rekoru
    val pitchAngleDeg: Double = 0.0,            // Yokuş / Şaha kalkma eğimi (derece)
    val longitudinalAccelMs2: Double = 0.0,     // a_{x,true} (m/s^2)
    val longitudinalGForce: Double = 0.0,       // a_{x,true} / g (G)
    val lateralGForce: Double = 0.0,            // a_y / g (G)
    val longitudinalJerkMs3: Double = 0.0,      // j_x (m/s^3)
    val effectiveTireRadiusMm: Double = 306.9,  // R_eff (mm)
    val slipRatioLambda: Double = 0.0,          // Tekerlek kayma oranı lambda
    val tractionControlIntervening: Boolean = false,

    // Sanal Dyno ve Motor Telemetrisi
    val estimatedWheelHorsepower: Double = 0.0,
    val estimatedWheelTorqueNm: Double = 0.0,
    val simulatedCoolantTempC: Int = 88,
    val simulatedAmbientTempC: Int = 24,
    val simulatedBatteryVolts: Double = 14.2,
    val simulatedFuelLevelRatio: Float = 0.78f,
    val selectedRideModeLabel: String = "TRACK",

    // Kontak Selamlama ve Gece/Gündüz Tema Durumu
    val ignitionBootPhase: IgnitionBootPhase = IgnitionBootPhase.RUNNING_LIVE,
    val ignitionSweepProgress: Float = 1.0f,
    val nightModeBlendFactor: Float = 1.0f,     // 0.0 = Gündüz, 1.0 = Gece

    // Teker Derece Ölçer & Teker Metre (Wheelie Angle & Wheelie Meter)
    val isWheelieActive: Boolean = false,
    val isWheelieSummaryVisible: Boolean = false,
    val wheelieAngleDeg: Double = 0.0,               // Anlık teker şaha kalkma açısı (°)
    val currentWheelieDurationSec: Double = 0.0,     // Anlık/Son teker süresi (sn)
    val currentWheelieDistanceMeters: Double = 0.0,  // Anlık/Son teker mesafesi (m)
    val maxWheelieAngleDeg: Double = 0.0,            // Sürüş rekoru: Maksimum teker açısı (°)
    val longestWheelieDurationSec: Double = 0.0,     // Sürüş rekoru: En uzun teker süresi (sn)
    val longestWheelieDistanceMeters: Double = 0.0,  // Sürüş rekoru: En uzun teker mesafesi (m)
    val wheelieGaugeDisplayMode: WheelieGaugeDisplayMode = WheelieGaugeDisplayMode.SMART_AUTO_POPUP,

    // Yol Bilgisayarı & Drag Sayacı
    val tripDistanceKm: Double = 0.0,
    val odometerKm: Double = 12480.0,
    val maxSpeedSessionKmh: Double = 0.0,
    val maxRpmSession: Double = 0.0,
    val best0To100KmhSec: Double? = null,
    val best100To200KmhSec: Double? = null,
    val best400mDragSec: Double? = null
)
