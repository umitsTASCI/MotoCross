package com.oemcluster.pro.domain

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Motor Bloğu ve Ateşleme Düzeni (Akustik FFT/HPS Harmonik Çarpım Spektrumu için)
 */
@Serializable
enum class EngineFiringConfig(
    val displayName: String,
    val cylinderCount: Int,
    val firingPulsesPerCrankRev: Double
) {
    SINGLE_1_CYL("Single Cylinder (Tek Silindir 4T)", 1, 0.5),
    PARALLEL_TWIN_180("Parallel Twin 180°", 2, 1.0),
    PARALLEL_TWIN_270("Parallel Twin 270° (Crossplane Twin)", 2, 1.0),
    PARALLEL_TWIN_360("Parallel Twin 360°", 2, 1.0),
    V_TWIN_90_75("V-Twin / L-Twin (75°/90°)", 2, 1.0),
    V_TWIN_45_60("V-Twin (45°/60° Cruiser)", 2, 1.0),
    BOXER_TWIN("Boxer Twin 180°", 2, 1.0),
    INLINE_TRIPLE_120("Inline-3 120° / T-Plane", 3, 1.5),
    INLINE_FOUR_SCREAMER("Inline-4 180° Screamer", 4, 2.0),
    INLINE_FOUR_CROSSPLANE("Inline-4 90° Crossplane (CP4)", 4, 2.0),
    V4_90_TWIN_PULSE("V4 90° Desmosedici / Twin-Pulse", 4, 2.0),
    V4_65_RSV4("V4 65° Narrow-Angle", 4, 2.0),
    FLAT_SIX("Flat-6 / Inline-6", 6, 3.0)
}

/**
 * Toroidal Dinamik Lastik Ebadı Modeli (Bölüm 3.1)
 * Örn: 150/60 R17, 180/55 ZR17, 190/55 ZR17, 200/55 ZR17
 */
@Serializable
data class TireSpecification(
    val widthMm: Double,            // W_mm (Örn: 150.0, 180.0, 190.0, 200.0)
    val aspectRatioPercent: Double, // AspectRatio_% (Örn: 60.0, 55.0)
    val rimDiameterInch: Double,    // D_rim_inch (Örn: 17.0)
    val crownRadiusMm: Double = widthMm * 0.52, // Toroidal sırt eğrilik yarıçapı r_crown [mm]
    val staticSquishFactor: Double = 0.972      // Statik yük ezilme katsayısı k_squish
) {
    /**
     * 3.1.1. Yüksüz Yarıçap (R_unloaded) [mm]:
     * R_unloaded = ((D_rim_inch * 25.4) / 2.0) + (W_mm * (AspectRatio_% / 100.0))
     */
    val unloadedRadiusMm: Double
        get() = ((rimDiameterInch * 25.4) / 2.0) + (widthMm * (aspectRatioPercent / 100.0))

    fun toSpecString(): String =
        "${widthMm.toInt()}/${aspectRatioPercent.toInt()} ZR${rimDiameterInch.toInt()}"

    companion object {
        fun parse(spec: String, defaultSquish: Double = 0.972): TireSpecification {
            // Örn: "90/90 R17", "150/60 R17" veya "190/55 ZR17"
            val regex = Regex("""(\d{2,3})\s*/\s*(\d{2,3})\s*[A-Za-z-]*\s*(\d{2})""")
            val match = regex.find(spec.trim())
            return if (match != null) {
                val w = match.groupValues[1].toDouble()
                val ar = match.groupValues[2].toDouble()
                val rim = match.groupValues[3].toDouble()
                TireSpecification(
                    widthMm = w,
                    aspectRatioPercent = ar,
                    rimDiameterInch = rim,
                    crownRadiusMm = w * 0.52,
                    staticSquishFactor = defaultSquish
                )
            } else {
                TireSpecification(180.0, 55.0, 17.0, 93.6, defaultSquish)
            }
        }
    }
}

/**
 * Şanzıman Tipi (Vites geçiş karakteristiği ve CVT varyatör fiziği için)
 */
@Serializable
enum class TransmissionType(
    val displayName: String,
    val shiftConfirmSec: Double
) {
    MANUAL_SEQUENTIAL("Manuel Sıralı Şanzıman", 0.12),
    QUICKSHIFTER_BIDIRECTIONAL("Quickshifter + Auto-Blipper", 0.065),
    DCT_DUAL_CLUTCH("DCT Çift Kavrama", 0.055),
    CVT_VARIATOR("CVT Varyatör (Otomatik)", 0.04)
}

/**
 * Son Aktarma Tipi (Tekerlek beygir gücü ve tork kaybı hesabı için)
 */
@Serializable
enum class FinalDriveType(
    val displayName: String,
    val mechanicalEfficiency: Double
) {
    CHAIN("Zincir Aktarma (%93 Verim)", 0.93),
    BELT("Karbon Kayış Aktarma (%90 Verim)", 0.90),
    SHAFT("Şaft / Kardan Aktarma (%86 Verim)", 0.86)
}

/**
 * Motor Tork Eğrisi Noktası (RPM -> Krank Torku [Nm])
 */
@Serializable
data class TorqueCurvePoint(
    val rpm: Double,
    val torqueNm: Double
)

/**
 * ALTIN KURAL (Bölüm 1.2):
 * Kullanıcının "Garajım (My Bike Profile)" menüsünde ayarladığı motosiklet özellikleri
 * uygulamanın TEK GERÇEK VERİ KAYNAĞIDIR (Single Source of Truth).
 * Ekranda seçilen 270 gösterge modeli tamamen görsel birer arayüzdür (UI Skin).
 */
@Serializable
data class BikeMechanicalProfile(
    val profileId: String,
    val brand: String,
    val modelYear: Int,
    val modelName: String,
    val engineConfig: EngineFiringConfig,
    val frontSprocketTeeth: Int,        // Z_front
    val rearSprocketTeeth: Int,         // Z_rear
    val primaryReductionRatio: Double,  // i_primary
    val gearRatios: List<Double>,       // i_gear[1..N] (4..8 vites destekli)
    val rearTire: TireSpecification,
    val frontTire: TireSpecification,
    val idleRpm: Double,
    val maxTorqueRpm: Double,
    val maxTorqueNm: Double,
    val maxPowerRpm: Double,
    val maxPowerHp: Double,
    val redlineRpm: Double,
    val revLimiterRpm: Double,
    val totalMassKg: Double,            // Motosiklet + Sürücü ağırlığı (kg)
    val dragCoefficientAreaCdA: Double = 0.34, // Aerodinamik direnç katsayısı * Ön Alan (m^2)
    val torqueCurve: List<TorqueCurvePoint>,
    // --- Detaylı Mühendislik & Şasi Parametreleri ---
    val transmissionGearCount: Int = gearRatios.size.coerceIn(1, 8),
    val transmissionType: TransmissionType = TransmissionType.MANUAL_SEQUENTIAL,
    val finalDriveType: FinalDriveType = FinalDriveType.CHAIN,
    val displacementCc: Int = (maxTorqueNm * 9.6).toInt().coerceIn(50, 2500),
    val boreMm: Double = 78.0,
    val strokeMm: Double = 52.0,
    val compressionRatio: Double = 11.8,
    val bikeWetMassKg: Double = (totalMassKg - 78.0).coerceAtLeast(65.0),
    val riderAndCargoMassKg: Double = 78.0,
    val wheelbaseMm: Double = 1410.0,
    val seatHeightMm: Double = 815.0,
    val fuelTankCapacityLiters: Double = 16.0,
    val avgFuelConsumptionLPer100Km: Double = 4.8,
    val hasSlipperClutch: Boolean = true
) {
    /**
     * İkincil (Zincir/Kayış/Şaft) redüksiyon oranı: Z_rear / Z_front
     */
    val finalDriveRatio: Double
        get() = rearSprocketTeeth.toDouble() / max(1, frontSprocketTeeth).toDouble()

    /**
     * Etkin vites sayısı (1..8).
     */
    val effectiveGearCount: Int
        get() = transmissionGearCount.coerceIn(1, min(8, max(1, gearRatios.size)))

    /**
     * Belirli bir devirde ortalama piston hızı (m/s): V_piston = 2 * (stroke_m) * (RPM / 60)
     */
    fun meanPistonSpeedMs(rpm: Double = redlineRpm): Double =
        2.0 * (strokeMm / 1000.0) * (rpm / 60.0)

    /**
     * Tam depo teorik menzil (km)
     */
    val fullTankRangeKm: Double
        get() = (fuelTankCapacityLiters / max(1.2, avgFuelConsumptionLPer100Km)) * 100.0

    /**
     * Verilen herhangi bir RPM değerinde motor krank torkunu (Nm)
     * parçalı doğrusal / Hermite interpolasyon ile döndürür.
     */
    fun interpolateEngineTorqueNm(rpm: Double): Double {
        if (torqueCurve.isEmpty()) return maxTorqueNm * 0.8
        val sorted = torqueCurve.sortedBy { it.rpm }
        if (rpm <= sorted.first().rpm) return sorted.first().torqueNm
        if (rpm >= sorted.last().rpm) {
            // Kesici üzerinde ateşleme kesintisi nedeniyle tork hızla düşer
            val excess = rpm - sorted.last().rpm
            return max(0.0, sorted.last().torqueNm * (1.0 - (excess / 800.0)))
        }
        for (i in 0 until sorted.size - 1) {
            val p0 = sorted[i]
            val p1 = sorted[i + 1]
            if (rpm in p0.rpm..p1.rpm) {
                val span = max(1.0, p1.rpm - p0.rpm)
                val t = (rpm - p0.rpm) / span
                return p0.torqueNm + t * (p1.torqueNm - p0.torqueNm)
            }
        }
        return maxTorqueNm
    }

    /**
     * Bölüm 3.4.1: Matematiksel Optimum Vites Değiştirme Noktası (RPM_opt[n])
     * Mevcut vitesteki tekerlek itme kuvvetinin, bir üst vitesteki tekerlek itme kuvvetine
     * eşitlendiği (veya kesiciye / redline'a ulaştığı) devir:
     * T_engine(RPM_opt[n]) * i_gear[n] = T_engine(RPM_opt[n] * (i_gear[n+1] / i_gear[n])) * i_gear[n+1]
     */
    fun computeOptimalShiftRpmForGear(gearIndexOneBased: Int): Double {
        val gearCount = effectiveGearCount
        if (gearIndexOneBased < 1 || gearIndexOneBased >= gearCount) {
            return redlineRpm
        }
        val iCurrent = gearRatios[gearIndexOneBased - 1]
        val iNext = gearRatios[gearIndexOneBased]
        if (iCurrent <= 0.0 || iNext <= 0.0) return redlineRpm

        val gearStepRatio = iNext / iCurrent
        val searchStartRpm = max(maxTorqueRpm, maxPowerRpm * 0.88)
        val searchEndRpm = revLimiterRpm

        var bestRpm = redlineRpm
        var minAbsDiff = Double.MAX_VALUE
        var crossoverFound = false

        var testRpm = searchStartRpm
        while (testRpm <= searchEndRpm) {
            val wheelTorqueCurrent = interpolateEngineTorqueNm(testRpm) * iCurrent
            val rpmAfterUpshift = testRpm * gearStepRatio
            val wheelTorqueNext = interpolateEngineTorqueNm(rpmAfterUpshift) * iNext

            // Mevcut vitesteki tekerlek torku bir üst vitesin altına düştüğü ilk devir veya kesişim noktası
            val diff = wheelTorqueCurrent - wheelTorqueNext
            if (diff <= 0.0) {
                bestRpm = testRpm
                crossoverFound = true
                break
            }
            if (abs(diff) < minAbsDiff) {
                minAbsDiff = abs(diff)
            }
            testRpm += 25.0
        }

        return if (crossoverFound) {
            min(bestRpm, redlineRpm)
        } else {
            // Eğer mevcut viteste tork eğrisi redline'a kadar üst vitesten yüksek kalıyorsa,
            // optimum vites noktası Redline devridir.
            redlineRpm
        }
    }

    /**
     * Tüm etkin vitesler (1..N) için matematiksel optimum vites değiştirme devirlerini döndürür.
     */
    fun computeAllOptimalShiftRpms(): DoubleArray {
        return DoubleArray(effectiveGearCount) { idx ->
            computeOptimalShiftRpmForGear(idx + 1)
        }
    }

    /**
     * Belirli bir viteste, redline devrinde ulaşılabilecek teorik tekerlek hızı.
     * Zincir son dişli oranı, birincil redüksiyon ve yüklü lastik yarıçapı
     * birlikte kullanılır. Drag/son hız tahminleri ve garaj bilgi paneli için.
     */
    fun theoreticalSpeedKmh(
        gearIndexOneBased: Int,
        rpm: Double = redlineRpm,
        includeSquish: Boolean = true
    ): Double {
        val gear = gearRatios.getOrElse(gearIndexOneBased - 1) { 1.0 }
        if (gear <= 0.0 || primaryReductionRatio <= 0.0 || finalDriveRatio <= 0.0) return 0.0
        val radiusMm = rearTire.unloadedRadiusMm *
            (if (includeSquish) rearTire.staticSquishFactor else 1.0)
        val wheelRpm = rpm / (primaryReductionRatio * finalDriveRatio * gear)
        val circumferenceM = 2.0 * Math.PI * radiusMm / 1000.0
        return wheelRpm * circumferenceM * 60.0 / 1000.0
    }

    /** 1..N etkin viteslerin redline'daki teorik hızları (km/h). */
    fun theoreticalSpeedTableKmh(): List<Double> =
        (0 until effectiveGearCount).map { theoreticalSpeedKmh(it + 1) }
}

