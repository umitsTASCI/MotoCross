package com.oemcluster.pro.data

import com.oemcluster.pro.catalog.DeclarativeGaugeSpec
import com.oemcluster.pro.catalog.OemGaugeCatalog105
import com.oemcluster.pro.domain.BikeMechanicalProfile
import com.oemcluster.pro.domain.EngineFiringConfig
import com.oemcluster.pro.domain.TireSpecification
import com.oemcluster.pro.domain.TorqueCurvePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * MotoCross (OEM-Cluster Pro) — 105 Model Tam Mekanik Motor Veritabanı ve "Garajım" Yöneticisi
 *
 * ALTIN KURAL:
 * Buradaki aktif motor profili (activeBikeProfile), uygulamanın TEK GERÇEK VERİ KAYNAĞIDIR.
 * Ekranda hangi gösterge seçilirse seçilsin, Hız -> Vites, Gerçek RPM ve Shift-Light uyarıları
 * %100 bu aktif motor profilinden hesaplanır.
 */
object BikeDatabaseRepository {

    private val coreReferencePresets: List<BikeMechanicalProfile> = listOf(
        // 1. KTM 390 Duke (Gen 2 / Gen 3) - 373cc Tek Silindir
        BikeMechanicalProfile(
            profileId = "ktm_390_duke_gen2",
            brand = "KTM",
            modelYear = 2023,
            modelName = "390 Duke (Gen 2)",
            engineConfig = EngineFiringConfig.SINGLE_1_CYL,
            frontSprocketTeeth = 15,
            rearSprocketTeeth = 45,
            primaryReductionRatio = 2.667,
            gearRatios = listOf(2.667, 1.857, 1.421, 1.143, 0.956, 0.840),
            rearTire = TireSpecification(150.0, 60.0, 17.0, crownRadiusMm = 78.0, staticSquishFactor = 0.974),
            frontTire = TireSpecification(110.0, 70.0, 17.0, crownRadiusMm = 57.2, staticSquishFactor = 0.976),
            idleRpm = 1450.0,
            maxTorqueRpm = 7000.0,
            maxTorqueNm = 37.0,
            maxPowerRpm = 9000.0,
            maxPowerHp = 43.5,
            redlineRpm = 10000.0,
            revLimiterRpm = 10500.0,
            totalMassKg = 242.0,
            dragCoefficientAreaCdA = 0.39,
            displacementCc = 373,
            fuelTankLiters = 13.4,
            torqueCurve = listOf(
                TorqueCurvePoint(1450.0, 18.0),
                TorqueCurvePoint(3000.0, 24.5),
                TorqueCurvePoint(4500.0, 29.8),
                TorqueCurvePoint(6000.0, 34.8),
                TorqueCurvePoint(7000.0, 37.0),
                TorqueCurvePoint(8000.0, 36.1),
                TorqueCurvePoint(9000.0, 34.0),
                TorqueCurvePoint(10000.0, 29.5),
                TorqueCurvePoint(10500.0, 22.0)
            )
        ),

        // 2. KTM 1290 Super Duke R Evo - 1301cc V-Twin
        BikeMechanicalProfile(
            profileId = "ktm_1290_super_duke_r",
            brand = "KTM",
            modelYear = 2023,
            modelName = "1290 Super Duke R EVO",
            engineConfig = EngineFiringConfig.V_TWIN_90_75,
            frontSprocketTeeth = 17,
            rearSprocketTeeth = 38,
            primaryReductionRatio = 1.950,
            gearRatios = listOf(2.467, 1.857, 1.476, 1.227, 1.042, 0.920),
            rearTire = TireSpecification(200.0, 55.0, 17.0, crownRadiusMm = 104.0, staticSquishFactor = 0.970),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1250.0,
            maxTorqueRpm = 8000.0,
            maxTorqueNm = 140.0,
            maxPowerRpm = 9500.0,
            maxPowerHp = 180.0,
            redlineRpm = 10200.0,
            revLimiterRpm = 10600.0,
            totalMassKg = 282.0,
            dragCoefficientAreaCdA = 0.38,
            displacementCc = 1301,
            fuelTankLiters = 16.0,
            torqueCurve = listOf(
                TorqueCurvePoint(1250.0, 78.0),
                TorqueCurvePoint(3000.0, 108.0),
                TorqueCurvePoint(5000.0, 126.0),
                TorqueCurvePoint(6500.0, 135.0),
                TorqueCurvePoint(8000.0, 140.0),
                TorqueCurvePoint(9500.0, 132.5),
                TorqueCurvePoint(10200.0, 118.0),
                TorqueCurvePoint(10600.0, 92.0)
            )
        ),

        // 3. Yamaha YZF-R1 / R1M (CP4 Crossplane)
        BikeMechanicalProfile(
            profileId = "yamaha_yzf_r1_cp4",
            brand = "Yamaha",
            modelYear = 2024,
            modelName = "YZF-R1 / R1M (CP4)",
            engineConfig = EngineFiringConfig.INLINE_FOUR_CROSSPLANE,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 41,
            primaryReductionRatio = 1.634,
            gearRatios = listOf(2.600, 2.176, 1.842, 1.579, 1.381, 1.250),
            rearTire = TireSpecification(200.0, 55.0, 17.0, crownRadiusMm = 104.0, staticSquishFactor = 0.970),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1300.0,
            maxTorqueRpm = 11500.0,
            maxTorqueNm = 113.3,
            maxPowerRpm = 13500.0,
            maxPowerHp = 200.0,
            redlineRpm = 14000.0,
            revLimiterRpm = 14500.0,
            totalMassKg = 278.0,
            dragCoefficientAreaCdA = 0.29,
            displacementCc = 998,
            fuelTankLiters = 17.0,
            torqueCurve = listOf(
                TorqueCurvePoint(1300.0, 52.0),
                TorqueCurvePoint(4000.0, 76.0),
                TorqueCurvePoint(7000.0, 92.0),
                TorqueCurvePoint(9500.0, 106.0),
                TorqueCurvePoint(11500.0, 113.3),
                TorqueCurvePoint(13500.0, 105.0),
                TorqueCurvePoint(14000.0, 96.0),
                TorqueCurvePoint(14500.0, 74.0)
            )
        ),

        // 4. Yamaha YZF-R6 (RJ15 / RJ27 16,500 RPM Screamer)
        BikeMechanicalProfile(
            profileId = "yamaha_yzf_r6_rj27",
            brand = "Yamaha",
            modelYear = 2020,
            modelName = "YZF-R6 (RJ27 Screamer)",
            engineConfig = EngineFiringConfig.INLINE_FOUR_SCREAMER,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 45,
            primaryReductionRatio = 1.955,
            gearRatios = listOf(2.583, 2.000, 1.667, 1.444, 1.286, 1.150),
            rearTire = TireSpecification(180.0, 55.0, 17.0, crownRadiusMm = 93.6, staticSquishFactor = 0.972),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1350.0,
            maxTorqueRpm = 10500.0,
            maxTorqueNm = 61.7,
            maxPowerRpm = 14500.0,
            maxPowerHp = 118.4,
            redlineRpm = 16000.0,
            revLimiterRpm = 16500.0,
            totalMassKg = 267.0,
            dragCoefficientAreaCdA = 0.28,
            displacementCc = 599,
            fuelTankLiters = 17.0,
            torqueCurve = listOf(
                TorqueCurvePoint(1350.0, 26.0),
                TorqueCurvePoint(5000.0, 41.0),
                TorqueCurvePoint(8000.0, 49.5),
                TorqueCurvePoint(10500.0, 61.7),
                TorqueCurvePoint(13000.0, 60.2),
                TorqueCurvePoint(14500.0, 58.0),
                TorqueCurvePoint(16000.0, 49.0),
                TorqueCurvePoint(16500.0, 35.0)
            )
        ),

        // 5. Yamaha MT-07 (CP2 689cc)
        BikeMechanicalProfile(
            profileId = "yamaha_mt07_cp2",
            brand = "Yamaha",
            modelYear = 2024,
            modelName = "MT-07 (CP2)",
            engineConfig = EngineFiringConfig.PARALLEL_TWIN_270,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 43,
            primaryReductionRatio = 1.925,
            gearRatios = listOf(2.846, 2.125, 1.632, 1.300, 1.091, 0.964),
            rearTire = TireSpecification(180.0, 55.0, 17.0, crownRadiusMm = 93.6, staticSquishFactor = 0.972),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1250.0,
            maxTorqueRpm = 6500.0,
            maxTorqueNm = 67.0,
            maxPowerRpm = 8750.0,
            maxPowerHp = 73.4,
            redlineRpm = 10000.0,
            revLimiterRpm = 10500.0,
            totalMassKg = 261.0,
            dragCoefficientAreaCdA = 0.39,
            displacementCc = 689,
            fuelTankLiters = 14.0,
            torqueCurve = listOf(
                TorqueCurvePoint(1250.0, 42.0),
                TorqueCurvePoint(3500.0, 58.0),
                TorqueCurvePoint(6500.0, 67.0),
                TorqueCurvePoint(8750.0, 59.8),
                TorqueCurvePoint(10000.0, 48.0),
                TorqueCurvePoint(10500.0, 36.0)
            )
        ),

        // 6. BMW S1000RR (K67 ShiftCam)
        BikeMechanicalProfile(
            profileId = "bmw_s1000rr_k67",
            brand = "BMW Motorrad",
            modelYear = 2024,
            modelName = "S1000RR (K67 M-Package)",
            engineConfig = EngineFiringConfig.INLINE_FOUR_SCREAMER,
            frontSprocketTeeth = 17,
            rearSprocketTeeth = 45,
            primaryReductionRatio = 1.652,
            gearRatios = listOf(2.647, 2.091, 1.727, 1.500, 1.360, 1.261),
            rearTire = TireSpecification(200.0, 55.0, 17.0, crownRadiusMm = 104.0, staticSquishFactor = 0.970),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1250.0,
            maxTorqueRpm = 11000.0,
            maxTorqueNm = 113.0,
            maxPowerRpm = 13750.0,
            maxPowerHp = 210.0,
            redlineRpm = 14600.0,
            revLimiterRpm = 15100.0,
            totalMassKg = 274.0,
            dragCoefficientAreaCdA = 0.285,
            displacementCc = 999,
            fuelTankLiters = 16.5,
            torqueCurve = listOf(
                TorqueCurvePoint(1250.0, 58.0),
                TorqueCurvePoint(5500.0, 92.0),
                TorqueCurvePoint(9000.0, 108.0),
                TorqueCurvePoint(11000.0, 113.0),
                TorqueCurvePoint(13750.0, 108.5),
                TorqueCurvePoint(14600.0, 98.0),
                TorqueCurvePoint(15100.0, 72.0)
            )
        ),

        // 7. Ducati Panigale V4 R (Desmosedici Stradale R 16,500 RPM)
        BikeMechanicalProfile(
            profileId = "ducati_panigale_v4r",
            brand = "Ducati",
            modelYear = 2024,
            modelName = "Panigale V4 R (Desmosedici)",
            engineConfig = EngineFiringConfig.V4_90_TWIN_PULSE,
            frontSprocketTeeth = 15,
            rearSprocketTeeth = 42,
            primaryReductionRatio = 1.800,
            gearRatios = listOf(2.467, 1.941, 1.600, 1.364, 1.208, 1.096),
            rearTire = TireSpecification(200.0, 60.0, 17.0, crownRadiusMm = 106.0, staticSquishFactor = 0.969),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1400.0,
            maxTorqueRpm = 12000.0,
            maxTorqueNm = 111.3,
            maxPowerRpm = 15500.0,
            maxPowerHp = 240.5,
            redlineRpm = 16000.0,
            revLimiterRpm = 16500.0,
            totalMassKg = 269.0,
            dragCoefficientAreaCdA = 0.28,
            displacementCc = 998,
            fuelTankLiters = 17.0,
            torqueCurve = listOf(
                TorqueCurvePoint(1400.0, 52.0),
                TorqueCurvePoint(6000.0, 82.0),
                TorqueCurvePoint(10000.0, 104.0),
                TorqueCurvePoint(12000.0, 111.3),
                TorqueCurvePoint(15500.0, 108.5),
                TorqueCurvePoint(16500.0, 80.0)
            )
        ),

        // 8. Suzuki Hayabusa Gen 3 (GSX1300RR)
        BikeMechanicalProfile(
            profileId = "suzuki_hayabusa_gen3",
            brand = "Suzuki",
            modelYear = 2024,
            modelName = "Hayabusa GSX1300RR (Gen 3)",
            engineConfig = EngineFiringConfig.INLINE_FOUR_SCREAMER,
            frontSprocketTeeth = 18,
            rearSprocketTeeth = 43,
            primaryReductionRatio = 1.596,
            gearRatios = listOf(2.615, 1.937, 1.526, 1.285, 1.136, 1.043),
            rearTire = TireSpecification(190.0, 50.0, 17.0, crownRadiusMm = 96.0, staticSquishFactor = 0.968),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.974),
            idleRpm = 1150.0,
            maxTorqueRpm = 7000.0,
            maxTorqueNm = 150.0,
            maxPowerRpm = 9700.0,
            maxPowerHp = 190.0,
            redlineRpm = 10800.0,
            revLimiterRpm = 11300.0,
            totalMassKg = 341.0,
            dragCoefficientAreaCdA = 0.265,
            displacementCc = 1340,
            fuelTankLiters = 20.0,
            torqueCurve = listOf(
                TorqueCurvePoint(1150.0, 95.0),
                TorqueCurvePoint(4000.0, 136.0),
                TorqueCurvePoint(7000.0, 150.0),
                TorqueCurvePoint(9700.0, 137.5),
                TorqueCurvePoint(10800.0, 118.0),
                TorqueCurvePoint(11300.0, 88.0)
            )
        ),

        // 9. Kawasaki Ninja H2R (Supercharged 310 HP)
        BikeMechanicalProfile(
            profileId = "kawasaki_ninja_h2r",
            brand = "Kawasaki",
            modelYear = 2024,
            modelName = "Ninja H2 / H2R (Supercharged)",
            engineConfig = EngineFiringConfig.INLINE_FOUR_SCREAMER,
            frontSprocketTeeth = 18,
            rearSprocketTeeth = 42,
            primaryReductionRatio = 1.551,
            gearRatios = listOf(3.188, 2.526, 2.045, 1.727, 1.524, 1.348),
            rearTire = TireSpecification(200.0, 55.0, 17.0, crownRadiusMm = 104.0, staticSquishFactor = 0.968),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.974),
            idleRpm = 1300.0,
            maxTorqueRpm = 12500.0,
            maxTorqueNm = 165.0,
            maxPowerRpm = 14000.0,
            maxPowerHp = 310.0,
            redlineRpm = 14200.0,
            revLimiterRpm = 14700.0,
            totalMassKg = 293.0,
            dragCoefficientAreaCdA = 0.275,
            displacementCc = 998,
            fuelTankLiters = 17.0,
            torqueCurve = listOf(
                TorqueCurvePoint(1300.0, 68.0),
                TorqueCurvePoint(6000.0, 118.0),
                TorqueCurvePoint(10000.0, 152.0),
                TorqueCurvePoint(12500.0, 165.0),
                TorqueCurvePoint(14000.0, 156.0),
                TorqueCurvePoint(14700.0, 110.0)
            )
        )
    )

    /**
     * 105 Gösterge Kataloğundaki her bir motosiklet modeli için gerçekçi fabrika mekanik profilini
     * üretir veya referans tablodan döndürür. Böylece Garajım'da 105 motorun tamamı seçilebilir!
     */
    private val all105FactoryProfiles: List<BikeMechanicalProfile> by lazy {
        val coreMap = coreReferencePresets.associateBy { it.profileId }
        OemGaugeCatalog105.all105Models.map { spec ->
            coreMap[spec.skinId] ?: synthesizeFactoryProfileFromGaugeSpec(spec)
        }
    }

    private fun synthesizeFactoryProfileFromGaugeSpec(spec: DeclarativeGaugeSpec): BikeMechanicalProfile {
        val nameLower = spec.modelName.lowercase()
        val brandLower = spec.brand.lowercase()

        // Model adından motor hacmi (cc) ve silindir/kasa tipi çıkarımı
        val displacementCc = when {
            "2500" in nameLower || "rocket 3" in nameLower -> 2458
            "1800" in nameLower || "gold wing" in nameLower -> 1833
            "1390" in nameLower -> 1350
            "1300" in nameLower || "hayabusa" in nameLower -> 1340
            "1290" in nameLower -> 1301
            "1250" in nameLower -> 1254
            "1200" in nameLower || "t120" in nameLower || "speed twin" in nameLower -> 1200
            "1100" in nameLower || "rsv4" in nameLower || "tuono v4" in nameLower || "africa twin" in nameLower -> 1099
            "1050" in nameLower -> 1037
            "1000" in nameLower || "r1" in nameLower || "zx-10" in nameLower || "h2" in nameLower || "s1000" in nameLower || "m1000" in nameLower || "panigale v4" in nameLower || "streetfighter v4" in nameLower || "fireblade" in nameLower || "katana" in nameLower || "mt-10" in nameLower -> 998
            "950" in nameLower || "937" in nameLower || "desertx" in nameLower || "panigale v2" in nameLower -> 937
            "900" in nameLower || "901" in nameLower || "mt-09" in nameLower || "xsr900" in nameLower || "tracer 9" in nameLower || "z900" in nameLower || "f900" in nameLower -> 890
            "890" in nameLower -> 889
            "883" in nameLower -> 883
            "850" in nameLower -> 853
            "821" in nameLower -> 821
            "800" in nameLower || "801" in nameLower || "gsx-8" in nameLower || "f3 800" in nameLower || "superveloce" in nameLower -> 798
            "790" in nameLower -> 799
            "765" in nameLower -> 765
            "750" in nameLower || "gsx-r750" in nameLower || "forza 750" in nameLower || "nc750" in nameLower -> 749
            "700" in nameLower || "701" in nameLower || "702" in nameLower || "mt-07" in nameLower || "tenere" in nameLower || "ténéré" in nameLower || "r7" in nameLower -> 689
            "675" in nameLower -> 675
            "660" in nameLower -> 659
            "650" in nameLower || "sv650" in nameLower || "z650" in nameLower || "ninja 650" in nameLower || "cbr650" in nameLower || "cb650" in nameLower || "interceptor" in nameLower -> 649
            "636" in nameLower || "zx-6r" in nameLower -> 636
            "600" in nameLower || "r6" in nameLower || "cbr600" in nameLower || "gsx-r600" in nameLower || "tnt 600" in nameLower -> 599
            "560" in nameLower || "tmax" in nameLower -> 562
            "500" in nameLower || "502" in nameLower -> 471
            "457" in nameLower -> 457
            "450" in nameLower -> 449
            "400" in nameLower || "401" in nameLower || "zx-4rr" in nameLower -> 399
            "390" in nameLower -> 373
            "350" in nameLower -> 349
            "310" in nameLower -> 313
            "300" in nameLower || "r3" in nameLower -> 321
            "250" in nameLower || "r25" in nameLower -> 249
            "200" in nameLower -> 199
            "155" in nameLower || "150" in nameLower || "125" in nameLower || "grom" in nameLower || "pcx" in nameLower -> 125
            else -> 650
        }

        val redline = spec.gaugeRedlineRpm
        val revLimiter = minOf(spec.gaugeMaxRpm - 100.0, redline + 500.0)
        val idle = when {
            redline >= 14500.0 -> 1350.0
            displacementCc >= 1100 -> 1150.0
            displacementCc <= 300 -> 1500.0
            else -> 1300.0
        }

        val engineConfig = when {
            "v4" in nameLower || ("ducati" in brandLower && displacementCc >= 990) -> EngineFiringConfig.V4_90_TWIN_PULSE
            "rsv4" in nameLower || "tuono v4" in nameLower -> EngineFiringConfig.V4_65_RSV4
            "r1" in nameLower || "mt-10" in nameLower -> EngineFiringConfig.INLINE_FOUR_CROSSPLANE
            redline >= 13000.0 || "cbr650" in nameLower || "cb650" in nameLower || "z900" in nameLower || "z1000" in nameLower || "gsx-s1000" in nameLower || "hayabusa" in nameLower || "tnt 600" in nameLower -> EngineFiringConfig.INLINE_FOUR_SCREAMER
            "mt-09" in nameLower || "xsr900" in nameLower || "tracer 9" in nameLower || "765" in nameLower || "daytona" in nameLower || "trident" in nameLower || "tiger" in nameLower || "rocket 3" in nameLower || "f3" in nameLower || "superveloce" in nameLower -> EngineFiringConfig.INLINE_TRIPLE_120
            "r1250" in nameLower || "r1300" in nameLower || "r ninet" in nameLower -> EngineFiringConfig.BOXER_TWIN
            "harley" in brandLower || "indian" in brandLower -> EngineFiringConfig.V_TWIN_45_60
            "ducati" in brandLower || "super duke" in nameLower || "sv650" in nameLower || "dorsoduro" in nameLower -> EngineFiringConfig.V_TWIN_90_75
            displacementCc <= 401 && "zx-4" !in nameLower && "cbr250rr" !in nameLower && "r3" !in nameLower && "r25" !in nameLower && "ninja 400" !in nameLower -> EngineFiringConfig.SINGLE_1_CYL
            else -> EngineFiringConfig.PARALLEL_TWIN_270
        }

        // Gerçekçi HP / Tork / Dişli / Lastik ölçeklemesi
        val maxHp = when {
            "h2r" in nameLower -> 310.0
            "v4" in nameLower && "ducati" in brandLower -> 215.0
            displacementCc >= 998 && redline >= 13000.0 -> 202.0
            displacementCc >= 1200 -> 165.0
            displacementCc in 850..997 -> 122.0
            displacementCc in 630..849 && redline >= 13500.0 -> 124.0
            displacementCc in 630..849 -> 78.0
            displacementCc in 370..500 -> 45.0
            displacementCc in 240..369 -> 36.0
            else -> 15.0
        }

        val maxTorqueNm = (displacementCc * 0.105).coerceIn(11.5, 221.0)
        val maxPowerRpm = (redline * 0.91).roundToInt().toDouble()
        val maxTorqueRpm = (redline * 0.72).roundToInt().toDouble()

        val rearTire = when {
            displacementCc >= 998 && maxHp >= 170.0 -> TireSpecification(200.0, 55.0, 17.0)
            displacementCc >= 600 -> TireSpecification(180.0, 55.0, 17.0)
            displacementCc >= 300 -> TireSpecification(150.0, 60.0, 17.0)
            else -> TireSpecification(130.0, 70.0, 17.0)
        }
        val frontTire = if (displacementCc >= 600) {
            TireSpecification(120.0, 70.0, 17.0)
        } else {
            TireSpecification(110.0, 70.0, 17.0)
        }

        // Hedef son hızdan (km/h) birincil ve son dişli oranlarını kalibre et
        val targetTopSpeedKmh = when {
            maxHp >= 190.0 -> 299.0
            maxHp >= 140.0 -> 272.0
            maxHp >= 110.0 -> 252.0
            maxHp >= 70.0 -> 214.0
            maxHp >= 42.0 -> 176.0
            maxHp >= 30.0 -> 158.0
            else -> 122.0
        }

        val gearRatios = listOf(2.667, 1.947, 1.524, 1.273, 1.096, 0.964)
        val frontTeeth = if (displacementCc >= 600) 16 else 15
        val rearTeeth = if (displacementCc >= 600) 42 else 45
        val finalRatio = rearTeeth.toDouble() / frontTeeth.toDouble()
        val wheelCircM = 2.0 * Math.PI * rearTire.loadedRadiusMeters
        val wheelRpmAtTop = ((targetTopSpeedKmh / 3.6) * 60.0) / max(0.5, wheelCircM)
        val primaryRatio = (redline / max(1.0, wheelRpmAtTop * finalRatio * gearRatios.last())).coerceIn(1.45, 3.10)

        return BikeMechanicalProfile(
            profileId = spec.skinId,
            brand = spec.brand,
            modelYear = 2024,
            modelName = spec.modelName,
            engineConfig = engineConfig,
            frontSprocketTeeth = frontTeeth,
            rearSprocketTeeth = rearTeeth,
            primaryReductionRatio = (primaryRatio * 1000.0).roundToInt() / 1000.0,
            gearRatios = gearRatios,
            rearTire = rearTire,
            frontTire = frontTire,
            idleRpm = idle,
            maxTorqueRpm = maxTorqueRpm,
            maxTorqueNm = (maxTorqueNm * 10.0).roundToInt() / 10.0,
            maxPowerRpm = maxPowerRpm,
            maxPowerHp = (maxHp * 10.0).roundToInt() / 10.0,
            redlineRpm = redline,
            revLimiterRpm = revLimiter,
            totalMassKg = (160.0 + displacementCc * 0.045 + 75.0).roundToInt().toDouble(),
            dragCoefficientAreaCdA = if (maxHp >= 150.0) 0.29 else 0.36,
            displacementCc = displacementCc,
            fuelTankLiters = if (displacementCc >= 600) 16.5 else 13.5,
            torqueCurve = listOf(
                TorqueCurvePoint(idle, maxTorqueNm * 0.52),
                TorqueCurvePoint(idle + (maxTorqueRpm - idle) * 0.45, maxTorqueNm * 0.78),
                TorqueCurvePoint(maxTorqueRpm * 0.82, maxTorqueNm * 0.93),
                TorqueCurvePoint(maxTorqueRpm, maxTorqueNm),
                TorqueCurvePoint(maxPowerRpm, maxTorqueNm * 0.92),
                TorqueCurvePoint(redline, maxTorqueNm * 0.80),
                TorqueCurvePoint(revLimiter, maxTorqueNm * 0.58)
            )
        )
    }

    private val _userGarageProfiles = MutableStateFlow<List<BikeMechanicalProfile>>(all105FactoryProfiles)
    val userGarageProfiles: StateFlow<List<BikeMechanicalProfile>> = _userGarageProfiles.asStateFlow()

    private val _activeBikeProfile = MutableStateFlow<BikeMechanicalProfile>(all105FactoryProfiles.first())
    val activeBikeProfile: StateFlow<BikeMechanicalProfile> = _activeBikeProfile.asStateFlow()

    fun getAllFactoryPresets(): List<BikeMechanicalProfile> = all105FactoryProfiles

    fun selectActiveBikeProfile(profileId: String) {
        val found = _userGarageProfiles.value.firstOrNull { it.profileId == profileId }
            ?: all105FactoryProfiles.firstOrNull { it.profileId == profileId }
        if (found != null) {
            _activeBikeProfile.value = found
        }
    }

    fun upsertBikeProfile(updatedProfile: BikeMechanicalProfile, setAsActive: Boolean = true) {
        val currentList = _userGarageProfiles.value.toMutableList()
        val existingIdx = currentList.indexOfFirst { it.profileId == updatedProfile.profileId }
        if (existingIdx >= 0) {
            currentList[existingIdx] = updatedProfile
        } else {
            currentList.add(0, updatedProfile)
        }
        _userGarageProfiles.value = currentList
        if (setAsActive || _activeBikeProfile.value.profileId == updatedProfile.profileId) {
            _activeBikeProfile.value = updatedProfile
        }
    }
}
