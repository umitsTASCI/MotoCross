package com.oemcluster.pro.data

import com.oemcluster.pro.domain.BikeMechanicalProfile
import com.oemcluster.pro.domain.EngineFiringConfig
import com.oemcluster.pro.domain.TireSpecification
import com.oemcluster.pro.domain.TorqueCurvePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * OEM-Cluster Pro - Mekanik Motor Profili Veritabanı ve "Garajım (My Garage)" Yöneticisi
 *
 * ALTIN KURAL (Bölüm 1.2):
 * Buradaki aktif motor profili (activeBikeProfile), uygulamanın TEK GERÇEK VERİ KAYNAĞIDIR.
 * Ekranda hangi gösterge (S1000RR, R1, Panigale V4, Hayabusa, 390 Duke vb.) seçilirse seçilsin,
 * Hız, Vites, Gerçek RPM ve Shift-Light uyarıları %100 bu aktif motor profilinden hesaplanır.
 */
object BikeDatabaseRepository {

    private val factoryPresets: List<BikeMechanicalProfile> = listOf(
        // 1. KTM 390 Duke (Gen 2 / Gen 3) - 373cc / 399cc Tek Silindir
        BikeMechanicalProfile(
            profileId = "ktm_390_duke_gen2",
            brand = "KTM",
            modelYear = 2023,
            modelName = "390 Duke",
            engineConfig = EngineFiringConfig.SINGLE_1_CYL,
            frontSprocketTeeth = 15,
            rearSprocketTeeth = 45,
            primaryReductionRatio = 2.667, // 80/30
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
            totalMassKg = 242.0, // 165kg ıslak + 77kg sürücü
            dragCoefficientAreaCdA = 0.39,
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

        // 2. KTM 1290 Super Duke R Evo - 1301cc 75° V-Twin
        BikeMechanicalProfile(
            profileId = "ktm_1290_super_duke_r",
            brand = "KTM",
            modelYear = 2023,
            modelName = "1290 Super Duke R",
            engineConfig = EngineFiringConfig.V_TWIN_90_75,
            frontSprocketTeeth = 17,
            rearSprocketTeeth = 38,
            primaryReductionRatio = 1.950, // 78/40
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
            torqueCurve = listOf(
                TorqueCurvePoint(1250.0, 78.0),
                TorqueCurvePoint(3000.0, 108.0),
                TorqueCurvePoint(5000.0, 126.0),
                TorqueCurvePoint(6500.0, 135.0),
                TorqueCurvePoint(8000.0, 140.0),
                TorqueCurvePoint(9000.0, 136.0),
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
            modelName = "YZF-R1 / R1M",
            engineConfig = EngineFiringConfig.INLINE_FOUR_CROSSPLANE,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 41,
            primaryReductionRatio = 1.634, // 67/41
            gearRatios = listOf(2.600, 2.176, 1.842, 1.579, 1.381, 1.250),
            rearTire = TireSpecification(190.0, 55.0, 17.0, crownRadiusMm = 99.0, staticSquishFactor = 0.971),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1300.0,
            maxTorqueRpm = 11500.0,
            maxTorqueNm = 113.3,
            maxPowerRpm = 13500.0,
            maxPowerHp = 200.0,
            redlineRpm = 14200.0,
            revLimiterRpm = 14750.0,
            totalMassKg = 278.0,
            dragCoefficientAreaCdA = 0.30,
            torqueCurve = listOf(
                TorqueCurvePoint(1300.0, 48.0),
                TorqueCurvePoint(4000.0, 76.0),
                TorqueCurvePoint(6500.0, 91.0),
                TorqueCurvePoint(8500.0, 104.0),
                TorqueCurvePoint(10000.0, 110.5),
                TorqueCurvePoint(11500.0, 113.3),
                TorqueCurvePoint(13500.0, 105.0),
                TorqueCurvePoint(14200.0, 96.0),
                TorqueCurvePoint(14750.0, 75.0)
            )
        ),

        // 4. Yamaha YZF-R6 (RJ15 / RJ27 - 16,500 RPM Screamer)
        BikeMechanicalProfile(
            profileId = "yamaha_yzf_r6_rj27",
            brand = "Yamaha",
            modelYear = 2020,
            modelName = "YZF-R6",
            engineConfig = EngineFiringConfig.INLINE_FOUR_SCREAMER,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 45,
            primaryReductionRatio = 1.955, // 86/44
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
            dragCoefficientAreaCdA = 0.29,
            torqueCurve = listOf(
                TorqueCurvePoint(1350.0, 26.0),
                TorqueCurvePoint(5000.0, 38.0),
                TorqueCurvePoint(8000.0, 46.5),
                TorqueCurvePoint(10500.0, 61.7),
                TorqueCurvePoint(12500.0, 60.2),
                TorqueCurvePoint(14500.0, 58.0),
                TorqueCurvePoint(16000.0, 50.0),
                TorqueCurvePoint(16500.0, 38.0)
            )
        ),

        // 5. Yamaha MT-07 (CP2 689cc 270° Twin)
        BikeMechanicalProfile(
            profileId = "yamaha_mt07_cp2",
            brand = "Yamaha",
            modelYear = 2024,
            modelName = "MT-07",
            engineConfig = EngineFiringConfig.PARALLEL_TWIN_270,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 43,
            primaryReductionRatio = 1.925, // 77/40
            gearRatios = listOf(2.846, 2.125, 1.632, 1.300, 1.091, 0.964),
            rearTire = TireSpecification(180.0, 55.0, 17.0, crownRadiusMm = 93.6, staticSquishFactor = 0.973),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1300.0,
            maxTorqueRpm = 6500.0,
            maxTorqueNm = 67.0,
            maxPowerRpm = 8750.0,
            maxPowerHp = 73.4,
            redlineRpm = 10000.0,
            revLimiterRpm = 10500.0,
            totalMassKg = 261.0,
            dragCoefficientAreaCdA = 0.38,
            torqueCurve = listOf(
                TorqueCurvePoint(1300.0, 39.0),
                TorqueCurvePoint(3000.0, 54.0),
                TorqueCurvePoint(5000.0, 63.0),
                TorqueCurvePoint(6500.0, 67.0),
                TorqueCurvePoint(8000.0, 63.5),
                TorqueCurvePoint(8750.0, 59.5),
                TorqueCurvePoint(10000.0, 49.0),
                TorqueCurvePoint(10500.0, 36.0)
            )
        ),

        // 6. Yamaha MT-09 / MT-09 SP (CP3 890cc Inline-3)
        BikeMechanicalProfile(
            profileId = "yamaha_mt09_cp3",
            brand = "Yamaha",
            modelYear = 2024,
            modelName = "MT-09 SP",
            engineConfig = EngineFiringConfig.INLINE_TRIPLE_120,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 45,
            primaryReductionRatio = 1.681, // 79/47
            gearRatios = listOf(2.571, 1.947, 1.619, 1.381, 1.190, 1.037),
            rearTire = TireSpecification(180.0, 55.0, 17.0, crownRadiusMm = 93.6, staticSquishFactor = 0.972),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1250.0,
            maxTorqueRpm = 7000.0,
            maxTorqueNm = 93.0,
            maxPowerRpm = 10000.0,
            maxPowerHp = 119.0,
            redlineRpm = 10800.0,
            revLimiterRpm = 11300.0,
            totalMassKg = 270.0,
            dragCoefficientAreaCdA = 0.37,
            torqueCurve = listOf(
                TorqueCurvePoint(1250.0, 55.0),
                TorqueCurvePoint(3500.0, 79.0),
                TorqueCurvePoint(5500.0, 88.5),
                TorqueCurvePoint(7000.0, 93.0),
                TorqueCurvePoint(8500.0, 90.0),
                TorqueCurvePoint(10000.0, 84.5),
                TorqueCurvePoint(10800.0, 75.0),
                TorqueCurvePoint(11300.0, 58.0)
            )
        ),

        // 7. BMW S1000RR / M1000RR (K67 ShiftCam)
        BikeMechanicalProfile(
            profileId = "bmw_s1000rr_k67",
            brand = "BMW Motorrad",
            modelYear = 2024,
            modelName = "S1000RR / M1000RR",
            engineConfig = EngineFiringConfig.INLINE_FOUR_SCREAMER,
            frontSprocketTeeth = 17,
            rearSprocketTeeth = 46,
            primaryReductionRatio = 1.652, // 76/46
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
            dragCoefficientAreaCdA = 0.295,
            torqueCurve = listOf(
                TorqueCurvePoint(1250.0, 52.0),
                TorqueCurvePoint(4500.0, 82.0),
                TorqueCurvePoint(7000.0, 98.0),
                TorqueCurvePoint(9000.0, 108.0),
                TorqueCurvePoint(11000.0, 113.0),
                TorqueCurvePoint(12500.0, 111.5),
                TorqueCurvePoint(13750.0, 108.5),
                TorqueCurvePoint(14600.0, 99.0),
                TorqueCurvePoint(15100.0, 76.0)
            )
        ),

        // 8. Ducati Panigale V4 S / V4 R (Desmosedici Stradale)
        BikeMechanicalProfile(
            profileId = "ducati_panigale_v4s",
            brand = "Ducati",
            modelYear = 2024,
            modelName = "Panigale V4 S",
            engineConfig = EngineFiringConfig.V4_90_TWIN_PULSE,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 41,
            primaryReductionRatio = 1.800, // 72/40
            gearRatios = listOf(2.467, 1.875, 1.556, 1.333, 1.190, 1.091),
            rearTire = TireSpecification(200.0, 60.0, 17.0, crownRadiusMm = 105.0, staticSquishFactor = 0.969),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1400.0,
            maxTorqueRpm = 11000.0,
            maxTorqueNm = 123.6,
            maxPowerRpm = 13000.0,
            maxPowerHp = 215.5,
            redlineRpm = 14500.0,
            revLimiterRpm = 15000.0,
            totalMassKg = 275.0,
            dragCoefficientAreaCdA = 0.295,
            torqueCurve = listOf(
                TorqueCurvePoint(1400.0, 56.0),
                TorqueCurvePoint(4500.0, 90.0),
                TorqueCurvePoint(7500.0, 109.0),
                TorqueCurvePoint(9500.0, 119.0),
                TorqueCurvePoint(11000.0, 123.6),
                TorqueCurvePoint(13000.0, 117.5),
                TorqueCurvePoint(14500.0, 104.0),
                TorqueCurvePoint(15000.0, 80.0)
            )
        ),

        // 9. Honda CBR1000RR-R Fireblade SP (SC82)
        BikeMechanicalProfile(
            profileId = "honda_cbr1000rrr_sp",
            brand = "Honda",
            modelYear = 2024,
            modelName = "CBR1000RR-R Fireblade SP",
            engineConfig = EngineFiringConfig.INLINE_FOUR_SCREAMER,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 43,
            primaryReductionRatio = 1.630, // 75/46
            gearRatios = listOf(2.538, 1.941, 1.579, 1.350, 1.217, 1.115),
            rearTire = TireSpecification(200.0, 55.0, 17.0, crownRadiusMm = 104.0, staticSquishFactor = 0.970),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1300.0,
            maxTorqueRpm = 12000.0,
            maxTorqueNm = 113.0,
            maxPowerRpm = 14000.0,
            maxPowerHp = 217.5,
            redlineRpm = 14800.0,
            revLimiterRpm = 15200.0,
            totalMassKg = 278.0,
            dragCoefficientAreaCdA = 0.285,
            torqueCurve = listOf(
                TorqueCurvePoint(1300.0, 46.0),
                TorqueCurvePoint(5000.0, 74.0),
                TorqueCurvePoint(8000.0, 92.0),
                TorqueCurvePoint(10000.0, 106.0),
                TorqueCurvePoint(12000.0, 113.0),
                TorqueCurvePoint(14000.0, 110.2),
                TorqueCurvePoint(14800.0, 101.0),
                TorqueCurvePoint(15200.0, 79.0)
            )
        ),

        // 10. Kawasaki Ninja ZX-10R / ZX-10RR
        BikeMechanicalProfile(
            profileId = "kawasaki_zx10r",
            brand = "Kawasaki",
            modelYear = 2024,
            modelName = "Ninja ZX-10R",
            engineConfig = EngineFiringConfig.INLINE_FOUR_SCREAMER,
            frontSprocketTeeth = 17,
            rearSprocketTeeth = 41,
            primaryReductionRatio = 1.681, // 79/47
            gearRatios = listOf(2.600, 2.214, 1.944, 1.722, 1.550, 1.391),
            rearTire = TireSpecification(190.0, 55.0, 17.0, crownRadiusMm = 99.0, staticSquishFactor = 0.971),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1250.0,
            maxTorqueRpm = 11400.0,
            maxTorqueNm = 114.9,
            maxPowerRpm = 13200.0,
            maxPowerHp = 203.0,
            redlineRpm = 14300.0,
            revLimiterRpm = 14750.0,
            totalMassKg = 284.0,
            dragCoefficientAreaCdA = 0.295,
            torqueCurve = listOf(
                TorqueCurvePoint(1250.0, 50.0),
                TorqueCurvePoint(5000.0, 81.0),
                TorqueCurvePoint(8000.0, 99.0),
                TorqueCurvePoint(10000.0, 110.0),
                TorqueCurvePoint(11400.0, 114.9),
                TorqueCurvePoint(13200.0, 109.2),
                TorqueCurvePoint(14300.0, 98.0),
                TorqueCurvePoint(14750.0, 75.0)
            )
        ),

        // 11. Kawasaki Ninja H2 / H2R (Supercharged 998cc)
        BikeMechanicalProfile(
            profileId = "kawasaki_ninja_h2",
            brand = "Kawasaki",
            modelYear = 2024,
            modelName = "Ninja H2 Supercharged",
            engineConfig = EngineFiringConfig.INLINE_FOUR_SCREAMER,
            frontSprocketTeeth = 18,
            rearSprocketTeeth = 44,
            primaryReductionRatio = 1.551, // 76/49
            gearRatios = listOf(3.188, 2.526, 2.045, 1.727, 1.524, 1.348),
            rearTire = TireSpecification(200.0, 55.0, 17.0, crownRadiusMm = 104.0, staticSquishFactor = 0.970),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1200.0,
            maxTorqueRpm = 11000.0,
            maxTorqueNm = 141.7,
            maxPowerRpm = 11500.0,
            maxPowerHp = 231.0,
            redlineRpm = 14000.0,
            revLimiterRpm = 14250.0,
            totalMassKg = 315.0,
            dragCoefficientAreaCdA = 0.30,
            torqueCurve = listOf(
                TorqueCurvePoint(1200.0, 62.0),
                TorqueCurvePoint(4500.0, 98.0),
                TorqueCurvePoint(7500.0, 126.0),
                TorqueCurvePoint(9500.0, 138.0),
                TorqueCurvePoint(11000.0, 141.7),
                TorqueCurvePoint(12500.0, 132.0),
                TorqueCurvePoint(14000.0, 114.0),
                TorqueCurvePoint(14250.0, 90.0)
            )
        ),

        // 12. Suzuki Hayabusa (Gen 3: GSX1300RR - 1340cc)
        BikeMechanicalProfile(
            profileId = "suzuki_hayabusa_gen3",
            brand = "Suzuki",
            modelYear = 2024,
            modelName = "Hayabusa GSX1300RR",
            engineConfig = EngineFiringConfig.INLINE_FOUR_SCREAMER,
            frontSprocketTeeth = 18,
            rearSprocketTeeth = 43,
            primaryReductionRatio = 1.596, // 83/52
            gearRatios = listOf(2.615, 1.937, 1.526, 1.285, 1.136, 1.043),
            rearTire = TireSpecification(190.0, 50.0, 17.0, crownRadiusMm = 95.0, staticSquishFactor = 0.971),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1150.0,
            maxTorqueRpm = 7000.0,
            maxTorqueNm = 150.0,
            maxPowerRpm = 9700.0,
            maxPowerHp = 190.0,
            redlineRpm = 10800.0,
            revLimiterRpm = 11250.0,
            totalMassKg = 341.0, // 264kg ıslak + 77kg sürücü
            dragCoefficientAreaCdA = 0.275,
            torqueCurve = listOf(
                TorqueCurvePoint(1150.0, 88.0),
                TorqueCurvePoint(3000.0, 126.0),
                TorqueCurvePoint(5000.0, 142.0),
                TorqueCurvePoint(7000.0, 150.0),
                TorqueCurvePoint(8500.0, 145.0),
                TorqueCurvePoint(9700.0, 139.0),
                TorqueCurvePoint(10800.0, 121.0),
                TorqueCurvePoint(11250.0, 96.0)
            )
        ),

        // 13. Aprilia RSV4 Factory 1100 (1099cc 65° V4)
        BikeMechanicalProfile(
            profileId = "aprilia_rsv4_factory_1100",
            brand = "Aprilia",
            modelYear = 2024,
            modelName = "RSV4 Factory 1100",
            engineConfig = EngineFiringConfig.V4_65_RSV4,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 41,
            primaryReductionRatio = 1.659, // 73/44
            gearRatios = listOf(2.600, 2.063, 1.700, 1.444, 1.296, 1.200),
            rearTire = TireSpecification(200.0, 55.0, 17.0, crownRadiusMm = 104.0, staticSquishFactor = 0.970),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1350.0,
            maxTorqueRpm = 10500.0,
            maxTorqueNm = 125.0,
            maxPowerRpm = 13000.0,
            maxPowerHp = 217.0,
            redlineRpm = 13600.0,
            revLimiterRpm = 14100.0,
            totalMassKg = 279.0,
            dragCoefficientAreaCdA = 0.295,
            torqueCurve = listOf(
                TorqueCurvePoint(1350.0, 62.0),
                TorqueCurvePoint(4500.0, 95.0),
                TorqueCurvePoint(7500.0, 114.0),
                TorqueCurvePoint(10500.0, 125.0),
                TorqueCurvePoint(12000.0, 121.0),
                TorqueCurvePoint(13000.0, 118.5),
                TorqueCurvePoint(13600.0, 108.0),
                TorqueCurvePoint(14100.0, 84.0)
            )
        ),

        // 14. Triumph Street Triple 765 RS (Moto2 Türevi Inline-3)
        BikeMechanicalProfile(
            profileId = "triumph_street_triple_765_rs",
            brand = "Triumph",
            modelYear = 2024,
            modelName = "Street Triple 765 RS",
            engineConfig = EngineFiringConfig.INLINE_TRIPLE_120,
            frontSprocketTeeth = 16,
            rearSprocketTeeth = 48,
            primaryReductionRatio = 1.854, // 76/41
            gearRatios = listOf(2.500, 1.842, 1.500, 1.285, 1.136, 1.043),
            rearTire = TireSpecification(180.0, 55.0, 17.0, crownRadiusMm = 93.6, staticSquishFactor = 0.972),
            frontTire = TireSpecification(120.0, 70.0, 17.0, crownRadiusMm = 62.4, staticSquishFactor = 0.975),
            idleRpm = 1250.0,
            maxTorqueRpm = 9500.0,
            maxTorqueNm = 80.0,
            maxPowerRpm = 12000.0,
            maxPowerHp = 130.0,
            redlineRpm = 12650.0,
            revLimiterRpm = 13100.0,
            totalMassKg = 265.0,
            dragCoefficientAreaCdA = 0.36,
            torqueCurve = listOf(
                TorqueCurvePoint(1250.0, 44.0),
                TorqueCurvePoint(4000.0, 66.0),
                TorqueCurvePoint(7000.0, 75.5),
                TorqueCurvePoint(9500.0, 80.0),
                TorqueCurvePoint(11000.0, 78.5),
                TorqueCurvePoint(12000.0, 76.8),
                TorqueCurvePoint(12650.0, 69.0),
                TorqueCurvePoint(13100.0, 52.0)
            )
        ),

        // 15. Honda CBR250R / CBR250RR (A2 / Giriş Sınıfı Örneği)
        BikeMechanicalProfile(
            profileId = "honda_cbr250rr_mc51",
            brand = "Honda",
            modelYear = 2023,
            modelName = "CBR250RR SP",
            engineConfig = EngineFiringConfig.PARALLEL_TWIN_180,
            frontSprocketTeeth = 14,
            rearSprocketTeeth = 41,
            primaryReductionRatio = 2.818,
            gearRatios = listOf(2.667, 1.933, 1.611, 1.381, 1.217, 1.077),
            rearTire = TireSpecification(140.0, 70.0, 17.0, crownRadiusMm = 72.8, staticSquishFactor = 0.975),
            frontTire = TireSpecification(110.0, 70.0, 17.0, crownRadiusMm = 57.2, staticSquishFactor = 0.976),
            idleRpm = 1400.0,
            maxTorqueRpm = 11000.0,
            maxTorqueNm = 25.0,
            maxPowerRpm = 13000.0,
            maxPowerHp = 42.0,
            redlineRpm = 14000.0,
            revLimiterRpm = 14500.0,
            totalMassKg = 245.0,
            dragCoefficientAreaCdA = 0.31,
            torqueCurve = listOf(
                TorqueCurvePoint(1400.0, 11.5),
                TorqueCurvePoint(4500.0, 16.5),
                TorqueCurvePoint(7500.0, 21.0),
                TorqueCurvePoint(11000.0, 25.0),
                TorqueCurvePoint(13000.0, 23.0),
                TorqueCurvePoint(14000.0, 20.5),
                TorqueCurvePoint(14500.0, 15.0)
            )
        ),

        // 16. CFMoto 450SR (270° Paralel Çift Silindir)
        BikeMechanicalProfile(
            profileId = "cfmoto_450sr",
            brand = "CFMoto",
            modelYear = 2024,
            modelName = "450SR / 450SS",
            engineConfig = EngineFiringConfig.PARALLEL_TWIN_270,
            frontSprocketTeeth = 14,
            rearSprocketTeeth = 41,
            primaryReductionRatio = 2.086,
            gearRatios = listOf(2.846, 2.000, 1.550, 1.273, 1.083, 0.957),
            rearTire = TireSpecification(150.0, 60.0, 17.0, crownRadiusMm = 78.0, staticSquishFactor = 0.974),
            frontTire = TireSpecification(110.0, 70.0, 17.0, crownRadiusMm = 57.2, staticSquishFactor = 0.976),
            idleRpm = 1400.0,
            maxTorqueRpm = 7600.0,
            maxTorqueNm = 39.3,
            maxPowerRpm = 9500.0,
            maxPowerHp = 47.0,
            redlineRpm = 11500.0,
            revLimiterRpm = 12300.0,
            totalMassKg = 256.0,
            dragCoefficientAreaCdA = 0.32,
            torqueCurve = listOf(
                TorqueCurvePoint(1400.0, 21.0),
                TorqueCurvePoint(4000.0, 31.5),
                TorqueCurvePoint(6000.0, 36.8),
                TorqueCurvePoint(7600.0, 39.3),
                TorqueCurvePoint(9500.0, 36.5),
                TorqueCurvePoint(11500.0, 29.0),
                TorqueCurvePoint(12300.0, 20.0)
            )
        )
    )

    private val _userGarageProfiles = MutableStateFlow<List<BikeMechanicalProfile>>(factoryPresets)
    val userGarageProfiles: StateFlow<List<BikeMechanicalProfile>> = _userGarageProfiles.asStateFlow()

    // Varsayılan aktif motor profili: KTM 390 Duke (Kullanıcı istediği an değiştirebilir)
    private val _activeBikeProfile = MutableStateFlow<BikeMechanicalProfile>(factoryPresets.first())
    val activeBikeProfile: StateFlow<BikeMechanicalProfile> = _activeBikeProfile.asStateFlow()

    fun getAllFactoryPresets(): List<BikeMechanicalProfile> = factoryPresets

    fun selectActiveBikeProfile(profileId: String) {
        val found = _userGarageProfiles.value.firstOrNull { it.profileId == profileId }
        if (found != null) {
            _activeBikeProfile.value = found
        }
    }

    /**
     * Kullanıcının kendi motosikletinde yaptığı özel dişli (Z_front, Z_rear),
     * lastik ebadı veya ECU Flash kesici devri değişikliklerini kaydeder ve
     * anında aktif mekanik profil olarak (Single Source of Truth) devreye alır.
     */
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

