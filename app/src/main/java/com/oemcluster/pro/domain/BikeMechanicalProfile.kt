package com.oemcluster.pro.domain

import kotlinx.serialization.Serializable
import kotlin.math.PI
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
    SINGLE_1_CYL("Single Cylinder (1-Cyl 4T)", 1, 0.5),
    PARALLEL_TWIN_180("Parallel Twin 180°", 2, 1.0),
    PARALLEL_TWIN_270("Parallel Twin 270° (CP2)", 2, 1.0),
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
    val crownRadiusMm: Double = widthMm * 0.52,
    val staticSquishFactor: Double = 0.972
) {
    val unloadedRadiusMm: Double
        get() = ((rimDiameterInch * 25.4) / 2.0) + (widthMm * (aspectRatioPercent / 100.0))

    val loadedRadiusMeters: Double
        get() = (unloadedRadiusMm * staticSquishFactor) / 1000.0

    fun toSpecString(): String =
        "${widthMm.toInt()}/${aspectRatioPercent.toInt()} ZR${rimDiameterInch.toInt()}"

    companion object {
        val COMMON_REAR_TIRES = listOf(
            TireSpecification(130.0, 70.0, 17.0),
            TireSpecification(140.0, 70.0, 17.0),
            TireSpecification(150.0, 60.0, 17.0),
            TireSpecification(160.0, 60.0, 17.0),
            TireSpecification(180.0, 55.0, 17.0),
            TireSpecification(190.0, 55.0, 17.0),
            TireSpecification(200.0, 55.0, 17.0),
            TireSpecification(200.0, 60.0, 17.0),
            TireSpecification(240.0, 45.0, 17.0),
            TireSpecification(150.0, 70.0, 18.0),
            TireSpecification(130.0, 70.0, 12.0)
        )

        fun parse(spec: String, defaultSquish: Double = 0.972): TireSpecification {
            val regex = Regex("""(\d{3})\s*/\s*(\d{2})\s*[A-Za-z]*\s*(\d{2})""")
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
 * Motor Tork Eğrisi Noktası (RPM -> Krank Torku [Nm])
 */
@Serializable
data class TorqueCurvePoint(
    val rpm: Double,
    val torqueNm: Double
)

/**
 * ALTIN KURAL:
 * Kullanıcının "Garajım (My Bike Profile)" menüsünde ayarladığı motosiklet özellikleri
 * uygulamanın TEK GERÇEK VERİ KAYNAĞIDIR (Single Source of Truth).
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
    val gearRatios: List<Double>,       // i_gear[1..N]
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
    val dragCoefficientAreaCdA: Double = 0.34,
    val torqueCurve: List<TorqueCurvePoint>,
    val displacementCc: Int = 600,
    val fuelTankLiters: Double = 15.0
) {
    val id: String
        get() = profileId

    val gearCount: Int
        get() = gearRatios.size

    val peakPowerHp: Double
        get() = maxPowerHp

    val peakTorqueNm: Double
        get() = maxTorqueNm

    val peakTorqueRpm: Double
        get() = maxTorqueRpm

    val finalDriveRatio: Double
        get() = rearSprocketTeeth.toDouble() / max(1, frontSprocketTeeth).toDouble()

    /**
     * Verilen vites (1..6) ve motor devri (RPM) için gerçek motosiklet hızını (km/h) hesaplar.
     */
    fun computeSpeedKmhAtRpm(gearOneBased: Int, rpm: Double): Double {
        if (gearRatios.isEmpty()) return 0.0
        val idx = (gearOneBased - 1).coerceIn(0, gearRatios.size - 1)
        val gRatio = gearRatios[idx]
        val totalReduction = primaryReductionRatio * finalDriveRatio * gRatio
        if (totalReduction <= 0.001) return 0.0
        val wheelRpm = rpm / totalReduction
        val wheelCircumferenceMeters = 2.0 * PI * rearTire.loadedRadiusMeters
        val speedMs = (wheelRpm * wheelCircumferenceMeters) / 60.0
        return speedMs * 3.6
    }

    /**
     * Verilen vites (1..6) ve hız (km/h) için gerçek motor devrini (RPM) hesaplar.
     */
    fun computeRpmAtSpeedKmh(gearOneBased: Int, speedKmh: Double): Double {
        if (gearRatios.isEmpty() || speedKmh <= 0.1) return idleRpm
        val idx = (gearOneBased - 1).coerceIn(0, gearRatios.size - 1)
        val gRatio = gearRatios[idx]
        val totalReduction = primaryReductionRatio * finalDriveRatio * gRatio
        val wheelCircumferenceMeters = 2.0 * PI * rearTire.loadedRadiusMeters
        val speedMs = speedKmh / 3.6
        val wheelRpm = (speedMs * 60.0) / max(0.5, wheelCircumferenceMeters)
        return wheelRpm * totalReduction
    }

    /**
     * Kullanıcının girdiği "Bu viteste Redline/Kesici devrinde ulaşılan hız (km/h)" değerinden
     * şanzıman dişli oranını (i_gear) tersine mühendislikle otomatik hesaplar!
     */
    fun computeGearRatioFromTopSpeedKmh(targetSpeedKmh: Double, atRpm: Double = redlineRpm): Double {
        val safeSpeedMs = max(3.0, targetSpeedKmh / 3.6)
        val wheelCircumferenceMeters = 2.0 * PI * rearTire.loadedRadiusMeters
        val wheelRpm = (safeSpeedMs * 60.0) / max(0.5, wheelCircumferenceMeters)
        val baseDrive = max(0.1, primaryReductionRatio * finalDriveRatio)
        return (atRpm / max(1.0, wheelRpm * baseDrive)).coerceIn(0.45, 4.50)
    }

    fun interpolateEngineTorqueNm(rpm: Double): Double {
        if (torqueCurve.isEmpty()) return maxTorqueNm * 0.85
        val sorted = torqueCurve.sortedBy { it.rpm }
        if (rpm <= sorted.first().rpm) return sorted.first().torqueNm
        if (rpm >= sorted.last().rpm) {
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

    fun computeOptimalShiftRpmForGear(gearIndexOneBased: Int): Double {
        val gearCount = gearRatios.size
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
            redlineRpm
        }
    }

    fun computeAllOptimalShiftRpms(): DoubleArray {
        return DoubleArray(gearRatios.size) { idx ->
            computeOptimalShiftRpmForGear(idx + 1)
        }
    }
}
