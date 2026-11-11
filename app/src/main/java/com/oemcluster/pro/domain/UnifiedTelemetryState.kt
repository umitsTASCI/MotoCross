package com.oemcluster.pro.domain

import kotlinx.serialization.Serializable

/**
 * Bölüm 4 & 7: Vites Uyarı Durumu (Shift-Light & Renk Tersleme Evreleri)
 */
@Serializable
enum class ShiftLightState(val nativeCode: Int) {
    NORMAL(0),
    STAGE_1_APPROACHING(1),    // Ön Uyarı (Örn: KTM 390 Duke barı #FF6600 Turuncu olur; R1 LED'leri ortaya yürür; Ducati kehribar olur)
    STAGE_2_SHIFT_NOW(2),      // Tam Renk Terslemesi / 8 Hz Flaşör (Örn: KTM Beyaz<->Turuncu/Siyah terslenir; S1000RR Kırmızı/Mavi çakar)
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
    ENGAGED_IN_GEAR(1),        // Viteste ve kavrama tam (Kinematik + Akustik + Tork doğrulamalı)
    UPSHIFT_IN_PROGRESS(2),    // Quickshifter (40-80ms) veya Debriyajlı vites büyütme geçişi
    DOWNSHIFT_IN_PROGRESS(3),  // Ara gazlı (Rev-match blip) veya motor frenli vites küçültme geçişi
    CLUTCH_PULLED_COASTING(4); // Hareket halinde (v > 3 km/h) debriyaj çekili, devir hızdan bağımsız rölantiye düşer

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
 * Bölüm 2.3 & 4.2: Hız ve Devir Ölçekleme Ayarları
 */
@Serializable
enum class SpeedCalibrationMode {
    PURE_GPS_TRUE_SPEED,       // v_display = v_true (Saf, hatasız gerçek hız)
    OEM_ECE_R39_FACTORY_BIAS   // v_display = v_true * (1.0 + bias) + 1.5 km/h
}

@Serializable
enum class RpmScalingMode {
    MODE_A_PROPORTIONAL_REDLINE, // RPM_ui = RPM_actual * (Gauge_Redline / Bike_Redline)
    MODE_B_NATIVE_1_TO_1         // İbre gerçek devri gösterir; Shift-Light aktif motorun gerçek devrinde çakar
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
 * Bölüm 7.1: Tek Yönlü Veri Akışı (Strict Unidirectional Data Flow) Çıktısı:
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

    // Hız Verileri (Bölüm 2.2 & 2.3)
    val trueSpeedMs: Double = 0.0,
    val trueSpeedKmh: Double = 0.0,
    val displayedSpeedKmh: Double = 0.0,
    val is299LockEngaged: Boolean = false,

    // Devir (RPM) ve Shift-Light Verileri (Bölüm 3 & 4)
    val actualEngineRpm: Double = 0.0,          // Kullanıcının motorunun gerçek devri (RPM)
    val normalizedRpmRatio: Double = 0.0,       // [0.0 .. 1.0+] (actualEngineRpm / bikeRedlineRpm)
    val peakHoldRpm: Double = 0.0,              // Vites atmadan önceki 1.8 sn asılı kalan tepe devir izi
    val rpmRateOfChangePerSec: Double = 0.0,    // d(RPM)/dt [RPM/s]
    val stage1ShiftRpm: Double = 8800.0,        // Aktif vites için hesaplanan Evre 1 uyarı devri
    val stage2ShiftRpm: Double = 9800.0,        // Aktif vites için hesaplanan Evre 2 flaşör devri
    val shiftLightState: ShiftLightState = ShiftLightState.NORMAL,
    val shiftFlashPhaseOn: Boolean = false,     // 8 Hz (Stage 2) veya 12 Hz (Over-Rev) anlık flaşör darbesi
    val fullColorInversionActive: Boolean = false, // Evre 2 ve Kesicide tam renk terslemesi aktif mi?

    // Vites ve Debriyaj Durumu (Bölüm 3.3)
    val currentGear: Int = 0,                   // 0 = Neutral (N), 1..6 = Aktif Vites
    val isNeutral: Boolean = true,
    val isClutchPulled: Boolean = false,
    val transmissionState: TransmissionEngagementState = TransmissionEngagementState.NEUTRAL,
    val gearShiftAnimationProgress: Float = 1.0f, // [0.0 .. 1.0] Vites rakamı büyüme/kayma animasyonu

    // Eğim, İvme ve Dinamik Lastik Fiziği (Bölüm 2.1, 2.4, 3.1, 3.2)
    val leanAngleDeg: Double = 0.0,             // Negatif: Sol Yatış (-°), Pozitif: Sağ Yatış (+°)
    val pitchAngleDeg: Double = 0.0,            // Yokuş eğimi / Ön kaldırma (Wheelie) açısı
    val longitudinalAccelMs2: Double = 0.0,     // Gerçek ileri/fren ivmesi a_x,true [m/s^2]
    val longitudinalG: Double = 0.0,            // İleri/Fren G kuvveti (+G ivme, -G fren)
    val lateralG: Double = 0.0,                 // Yanal G kuvveti
    val jerkLongitudinalMs3: Double = 0.0,      // İvme türevi j_x = da_x/dt [m/s^3]
    val effectiveTireRadiusMm: Double = 306.9,  // Dinamik toroidal efektif lastik yarıçapı R_eff [mm]
    val wheelSlipRatioLambda: Double = 0.0,     // Anlık tekerlek kayması (Slip Ratio λ)

    // Sanal Dyno ve Yardımcı Sistemler
    val estimatedWheelHorsepower: Double = 0.0,
    val estimatedWheelTorqueNm: Double = 0.0,
    val tractionControlInterventionActive: Boolean = false,
    val absInterventionActive: Boolean = false,
    val coolantTempCelsius: Double = 88.0,
    val fuelLevelRatio: Float = 0.78f,

    // Gündüz/Gece ve Kontak Durumu (Bölüm 4.1 & 4.7)
    val ignitionBootPhase: IgnitionBootPhase = IgnitionBootPhase.RUNNING_LIVE,
    val ignitionSweepProgress: Float = 1.0f,
    val nightModeTransitionFactor: Float = 1.0f, // 0.0 = Tam Gündüz (Beyaz), 1.0 = Tam Gece (Siyah) (500ms yumuşak geçiş)
    val mountCalibrated: Boolean = true,

    // Bosch 6D IMU Tarzı Teker Derece Ölçer & Teker Metre (Wheelie Angle, Timer & Distance)
    val isWheelieActive: Boolean = false,
    val isWheelieSummaryVisible: Boolean = false,      // Teker indikten sonra 3.2 sn özet görünürlüğü
    val wheelieAngleDeg: Double = 0.0,                 // Anlık teker açısı (°) [Yokuş eğimi arındırılmış]
    val currentWheelieDurationSec: Double = 0.0,       // Anlık/Son teker süresi (sn)
    val currentWheelieDistanceMeters: Double = 0.0,    // Anlık/Son teker mesafesi (m)
    val maxWheelieAngleDeg: Double = 0.0,              // Sürüş rekoru: Maksimum teker açısı (°)
    val longestWheelieDurationSec: Double = 0.0,       // Sürüş rekoru: En uzun teker süresi (sn)
    val longestWheelieDistanceMeters: Double = 0.0     // Sürüş rekoru: En uzun teker mesafesi (m)
) {
    /**
     * Bölüm 4.2: Seçilen göstergenin kadran üst sınırına (gaugeRedlineRpm ve gaugeMaxRpm) göre
     * ekranda çizilecek devir değerini hesaplar.
     */
    fun getMappedRpmForGauge(
        gaugeRedlineRpm: Double,
        gaugeMaxRpm: Double,
        scalingMode: RpmScalingMode
    ): Double {
        return when (scalingMode) {
            RpmScalingMode.MODE_A_PROPORTIONAL_REDLINE -> {
                val safeBikeRedline = if (bikeRedlineRpm > 1000.0) bikeRedlineRpm else 10000.0
                val mapped = actualEngineRpm * (gaugeRedlineRpm / safeBikeRedline)
                mapped.coerceIn(0.0, gaugeMaxRpm)
            }
            RpmScalingMode.MODE_B_NATIVE_1_TO_1 -> {
                actualEngineRpm.coerceIn(0.0, gaugeMaxRpm)
            }
        }
    }

    /**
     * Seçilen göstergede vites rakamını metin olarak döndürür ("N", "1", "2", "3", "4", "5", "6").
     */
    val gearDisplayString: String
        get() = if (isNeutral || currentGear <= 0) "N" else currentGear.toString()
}

