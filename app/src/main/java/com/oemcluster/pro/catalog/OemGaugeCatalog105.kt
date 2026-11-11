package com.oemcluster.pro.catalog

/**
 * MotoCross — 12 Marka ve 105 Motosiklet Modelinin Birebir OEM Gösterge Şartnamesi (Bölüm 5)
 *
 * ALTIN KURAL:
 * Buradaki 105 model yalnızca görsel arayüz şemasıdır (DeclarativeGaugeSpec / UI Skin).
 * Gösterilen hız, vites, gerçek devir (RPM) ve Shift-Light tetikleme noktaları %100
 * kullanıcının "Garajım" menüsündeki aktif motosiklet profilinden (BikeMechanicalProfile) gelir.
 */
enum class GaugeDisplayCategory {
    MODERN_TFT,
    ANALOG_HYBRID,
    NEGATIVE_POSITIVE_LCD,
    RETRO_CLASSIC
}

enum class GaugeGeometryType {
    BOOMERANG_BAR_TFT,          // KTM 390/790/890/1290 Duke, MT-09 Gen4, BMW Core 1
    CURVED_SWEEP_BAR_TFT,       // Yamaha R1/R1M, ZX-10R Gen6, Z900, CFMoto 450SR
    CIRCULAR_RING_TFT,          // Honda CBR1000RR-R Circuit, Ducati Panigale V2, GSX-8S, Brutale 1000
    SHARP_HORIZONTAL_EVO_TFT,   // Ducati Panigale V4 Track Evo, Streetfighter V4, 1390 Super Duke
    SYMMETRIC_PINCER_BAR_TFT,   // Triumph Street Triple 765 RS, KTM 1290 Super Adventure
    CENTRAL_ANALOG_HYBRID_LCD,  // Yamaha R6 RJ15/RJ27, CBR600RR PC40, S1000RR K46, ZX-6R, GSX-R1000 K5, RSV4 Analog, Daytona 675R
    FIVE_DIAL_HAYABUSA_HYBRID,  // Suzuki Hayabusa Gen 1 / Gen 2 / Gen 3 (5 Kadran + Merkez TFT)
    DUAL_CLASSIC_ANALOG,        // BMW R nineT, Triumph Bonneville/Thruxton, Royal Enfield GT 650, Forza 350
    SINGLE_PUCK_ROUND_TFT_LCD,  // Husqvarna Svartpilen/Vitpilen 401/801 Bauhaus, RE Himalayan 450, Sportster S, Iron 883
    F1_DIGITAL_ARC_BAR,         // Honda S2000 AP1, Yamaha R3 Gen2, GSX-R1000R L7 Negatif LCD, ZX-10R LED Arch
    RALLY_TOWER_VERTICAL_TFT,   // Yamaha Ténéré 700 World Raid, Aprilia Tuareg 660, CRF1100L Africa Twin
    PANORAMIC_SPLIT_TFT         // BMW R1300GS, Ducati Multistrada V4, Triumph Tiger 1200, CFMoto 800NK, PCX/Vespa
}

enum class ShiftLedPattern {
    NONE_INTERNAL_ONLY,
    SINGLE_UPPER_LED,           // KTM Gen1, R6 RJ15 Altıgen Mercek, CBR600RR REV, GSX-R Beyaz Lamba
    CONVERGING_5_LEDS,          // Yamaha R1 / R1M, CBR1000RR-R Fireblade (İki uçtan merkeze birleşen 5 LED)
    SEQUENTIAL_6_WHITE_LEDS,    // Yamaha R6 RJ27, Aprilia RSV4 (6 LED)
    TRIUMPH_7_BLUE_LEDS,        // Triumph Daytona 675R (7 Mavi LED)
    KAWASAKI_LED_ARCH_BAR,      // Kawasaki ZX-10R (2011-2020) Üst Kemer LED Barı
    DUAL_SIDE_LIGHTNING_LEDS,   // Suzuki Hayabusa Gen 3 Çift Yan Shift-Light
    MOTOGP_MARSHAL_BAR          // Ducati Desmosedici GP24 / BMW HP4 Race
}

data class DeclarativeGaugeSpec(
    val modelNumber: Int,               // 1..105
    val skinId: String,
    val brand: String,
    val modelName: String,
    val generationYears: String,
    val displayCategory: GaugeDisplayCategory,
    val geometryType: GaugeGeometryType,
    val shiftLedPattern: ShiftLedPattern,
    val gaugeRedlineRpm: Double,        // Kadran üzerindeki görsel Redline çizgisi
    val gaugeMaxRpm: Double,            // Kadran üzerindeki maksimum devir ölçeği
    val hasPeakHoldTrail: Boolean,      // Vites atarken 1.8 sn asılı kalan tepe devir izi (Yamaha/Suzuki/Kawasaki)
    val supportsLeanAndWheeliePod: Boolean,
    val bootSplashLogoText: String,
    val availableSubModes: List<String>,
    // Renk Paleti (ARGB Hex)
    val dayBgColorHex: Long,
    val dayPrimaryColorHex: Long,
    val nightBgColorHex: Long,
    val nightPrimaryColorHex: Long,
    val accentBrandColorHex: Long,
    val stage1ShiftColorHex: Long,      // Evre 1 Ön Uyarı Rengi (Örn: KTM #FF6600, Ducati #FF8800)
    val stage2InvertBgColorHex: Long,   // Evre 2 (8 Hz) Tam Renk Tersleme Arka Plan Rengi
    val stage2InvertFgColorHex: Long    // Evre 2 (8 Hz) Tam Renk Tersleme Yazı/Bar Rengi
)

object OemGaugeCatalog105 {

    val all105Models: List<DeclarativeGaugeSpec> = listOf(
        // =====================================================================
        // GRUP 1: KTM (10 Model: #1 - #10)
        // =====================================================================
        DeclarativeGaugeSpec(
            1, "ktm_390_duke_gen2", "KTM", "390 Duke (Gen 2)", "2017–2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, false, true, "READY TO RACE", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF0A0A0A, 0xFF080808, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF6600, 0xFFFF6600, 0xFF080808
        ),
        DeclarativeGaugeSpec(
            2, "ktm_390_duke_gen3", "KTM", "390 Duke (Gen 3 Bonded TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10200.0, 12000.0, true, true, "READY TO RACE", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF0C0C0C, 0xFF060608, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF6600, 0xFFFF6600, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            3, "ktm_rc_390_2022", "KTM", "RC 390 GP Edition", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10200.0, 12000.0, true, true, "READY TO RACE // RC", listOf("Track", "Street"),
            0xFFF2F2F2, 0xFF101010, 0xFF070709, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF6600, 0xFFFF6600, 0xFF050505
        ),
        DeclarativeGaugeSpec(
            4, "ktm_duke_gen1_lcd", "KTM", "125 / 250 / 390 Duke (Gen 1)", "2011–2016",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            10000.0, 12000.0, false, true, "READY TO RACE", listOf("Standard LCD"),
            0xFFFF6A00, 0xFF1A0900, 0xFF1F0A00, 0xFFFF7700, 0xFFFF5500,
            0xFFFF2200, 0xFF1A0900, 0xFFFF7700
        ),
        DeclarativeGaugeSpec(
            5, "ktm_890_duke_r", "KTM", "790 / 890 Duke R", "2020–2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, true, true, "READY TO RACE // R", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF0A0A0A, 0xFF080808, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF6600, 0xFFFF2200, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            6, "ktm_1290_super_duke_r", "KTM", "1290 Super Duke R (Gen 3)", "2020–2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10200.0, 12000.0, true, true, "THE BEAST // 1290", listOf("Street", "Track"),
            0xFFF4F4F4, 0xFF0A0A0A, 0xFF060608, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF6600, 0xFFFF1A00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            7, "ktm_1390_super_duke_r", "KTM", "1390 Super Duke R Evo", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, true, true, "THE BEAST // 1390", listOf("Track", "Street"),
            0xFFF5F5F5, 0xFF0A0A0A, 0xFF050507, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF6600, 0xFFFF5500, 0xFF050507
        ),
        DeclarativeGaugeSpec(
            8, "ktm_1290_super_adv_s", "KTM", "1290 Super Adventure S (7\" TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SYMMETRIC_PINCER_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, false, true, "READY TO RACE // ADV", listOf("Split Adventure", "Offroad"),
            0xFFF5F5F5, 0xFF101010, 0xFF08090C, 0xFFF0F0F0, 0xFFFF6600,
            0xFFFF6600, 0xFFFF6600, 0xFF08090C
        ),
        DeclarativeGaugeSpec(
            9, "ktm_250_duke_2024", "KTM", "250 Duke (5\" Segmented LCD)", "2024+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, false, true, "READY TO RACE", listOf("Negative LCD"),
            0xFF0C0E12, 0xFFF5F5F5, 0xFF07080A, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF6600, 0xFFFF6600, 0xFF07080A
        ),
        DeclarativeGaugeSpec(
            10, "ktm_690_smc_r", "KTM", "690 SMC R / Enduro R", "2019–2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            9000.0, 10500.0, false, true, "SMC-R // LC4", listOf("Supermoto Race"),
            0xFFFF6600, 0xFF120700, 0xFF120700, 0xFFFF7700, 0xFFFF6600,
            0xFFFF3300, 0xFFFF6600, 0xFF120700
        ),

        // =====================================================================
        // GRUP 2: YAMAHA (14 Model: #11 - #24)
        // =====================================================================
        DeclarativeGaugeSpec(
            11, "yamaha_yzf_r1", "Yamaha", "YZF-R1 / R1M (4.2\" TFT)", "2015–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            14000.0, 16000.0, true, true, "R1 // CROSSPLANE", listOf("Street", "Track"),
            0xFFF2F4F8, 0xFF0D1117, 0xFF06080C, 0xFFEFF4FA, 0xFF00C853,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            12, "yamaha_yzf_r6_rj15", "Yamaha", "YZF-R6 (RJ11 / RJ15 Analog + Red LCD)", "2006–2016",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            16500.0, 18000.0, false, true, "R6 // REVS YOUR HEART", listOf("Factory Analog"),
            0xFF0D0F14, 0xFFFFFFFF, 0xFF090A0E, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFFFFF, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            13, "yamaha_yzf_r6_rj27", "Yamaha", "YZF-R6 (RJ27 Analog + White LCD)", "2017–2020",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
            16000.0, 18000.0, false, true, "YZF-R6", listOf("Factory Analog"),
            0xFF0B0D12, 0xFFF5F7FA, 0xFF07080C, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFE0F7FA, 0xFF00B0FF, 0xFF07080C
        ),
        DeclarativeGaugeSpec(
            14, "yamaha_yzf_r7", "Yamaha", "YZF-R7 (Negative LCD)", "2022+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            10000.0, 12000.0, true, true, "R7 // R-WORLD", listOf("R-World LCD"),
            0xFF0A0D14, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFF00E5FF, 0xFFFFFFFF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            15, "yamaha_r25_r3_gen1", "Yamaha", "YZF-R25 / R3 (Gen 1 Analog + LCD)", "2015–2018",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            12500.0, 14000.0, false, true, "HI BUDDY // R25-R3", listOf("Sport Analog"),
            0xFF0D1017, 0xFFFFFFFF, 0xFF080A10, 0xFFFFFFFF, 0xFF2979FF,
            0xFFFFFFFF, 0xFF2979FF, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            16, "yamaha_r25_r3_gen2", "Yamaha", "YZF-R25 / R3 (Gen 2 M1-Style LCD)", "2019–2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            12500.0, 14000.0, true, true, "YZR-M1 SPIRIT", listOf("MotoGP LCD"),
            0xFFDCE2EA, 0xFF10141C, 0xFF0B0E15, 0xFFF0F4FA, 0xFF2979FF,
            0xFFFFAB00, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            17, "yamaha_yzf_r125_2023", "Yamaha", "YZF-R125 (5\" R1-Style TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            10500.0, 12000.0, true, true, "R125 // R-SERIES", listOf("Street", "Track"),
            0xFFF2F4F8, 0xFF0E121A, 0xFF07090F, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            18, "yamaha_mt09_gen4", "Yamaha", "MT-09 / MT-09 SP (Gen 4 5\" TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, true, true, "DARK SIDE OF JAPAN", listOf("Theme 1 Cyan", "Theme 2 Track", "Theme 3 Street", "Theme 4 Eco"),
            0xFFF0F4F8, 0xFF0B0E14, 0xFF06080D, 0xFFF5F7FA, 0xFF00E5FF,
            0xFFFF6D00, 0xFFFF1744, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            19, "yamaha_mt09_gen1_lcd", "Yamaha", "MT-09 (Gen 1/2 Asymmetric LCD)", "2014–2020",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, true, true, "MT-09 // CP3", listOf("Negative LCD"),
            0xFF0C1018, 0xFFE8EEF5, 0xFF070A10, 0xFFFFFFFF, 0xFF00E5FF,
            0xFF00E5FF, 0xFFFFFFFF, 0xFF070A10
        ),
        DeclarativeGaugeSpec(
            20, "yamaha_mt07_classic_lcd", "Yamaha", "MT-07 (Classic Central LCD)", "2014–2022",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, true, true, "MT-07 // CP2", listOf("Positive LCD", "Negative Mod"),
            0xFFD8E0EC, 0xFF0F131A, 0xFF090C12, 0xFFE5ECF5, 0xFF00B0FF,
            0xFFFF9100, 0xFF0F131A, 0xFFD8E0EC
        ),
        DeclarativeGaugeSpec(
            21, "yamaha_mt07_2023_tft", "Yamaha", "MT-07 (5\" Color TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, true, true, "DARK SIDE // MT-07", listOf("Street", "Touring"),
            0xFFF2F5F8, 0xFF0D1017, 0xFF07090E, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            22, "yamaha_mt10_sp", "Yamaha", "MT-10 / MT-10 SP (4.2\" R1-Derived TFT)", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            11500.0, 13000.0, true, true, "MASTER OF TORQUE // CP4", listOf("Street", "Track"),
            0xFFF0F4F8, 0xFF0C1018, 0xFF06080C, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            23, "yamaha_tenere_700_raid", "Yamaha", "Ténéré 700 World Raid (5\" Rally Tower)", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, false, true, "NEXT HORIZON // T7", listOf("Explorer", "Street", "Raid Roadbook"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B0FF,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            24, "yamaha_tmax_560", "Yamaha", "TMAX 560 Tech MAX (7\" Dual Circular TFT)", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8200.0, 9500.0, false, true, "MAX EVOLUTION", listOf("Sport", "Intelligent", "Authentic"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFFFAB00, 0xFFFF1744, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 3: HONDA (14 Model: #25 - #38)
        // =====================================================================
        DeclarativeGaugeSpec(
            25, "honda_cbr1000rrr_sp", "Honda", "CBR1000RR-R Fireblade SP (5\" TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            14500.0, 16000.0, true, true, "BORN TO RACE // HRC", listOf("Circuit", "Street", "Mechanic"),
            0xFFF5F5F5, 0xFF0E1015, 0xFF07080C, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            26, "honda_cbr1000rr_sc77", "Honda", "CBR1000RR Fireblade (SC77 TFT)", "2017–2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            13000.0, 15000.0, true, true, "TOTAL CONTROL // CBR", listOf("Street", "Circuit", "Mechanic"),
            0xFFF2F2F2, 0xFF101218, 0xFF080A0F, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            27, "honda_cbr600rr_2024", "Honda", "CBR600RR (5\" Fireblade-Style TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            15000.0, 16500.0, true, true, "AWAKEN THE RACE // 600RR", listOf("Circuit", "Street"),
            0xFFF5F5F5, 0xFF0E1015, 0xFF07080C, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            28, "honda_cbr600rr_pc40", "Honda", "CBR600RR (PC40 Central Analog + LCD)", "2007–2012",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            15000.0, 17000.0, false, true, "CBR600RR // HESD", listOf("Factory Analog"),
            0xFF0C0E14, 0xFFFFFFFF, 0xFF08090D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            29, "honda_cbr650r_lcd", "Honda", "CBR650R / CB650R (Dual Negative LCD)", "2019–2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            12000.0, 14000.0, true, true, "FOUR CYLINDER // 650R", listOf("Negative LCD"),
            0xFF0A0D14, 0xFFF2F5FA, 0xFF06080D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFFFFFFF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            30, "honda_cbr650r_2024_tft", "Honda", "CBR650R / CB650R (5\" E-Clutch TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12000.0, 14000.0, false, true, "HONDA // E-CLUTCH", listOf("Bar", "Circle", "Simple"),
            0xFFF5F5F5, 0xFF0D1016, 0xFF07090D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            31, "honda_cbr250rr_mc51", "Honda", "CBR250RR (MC51 Sport+ Race LCD)", "2017–2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.CONVERGING_5_LEDS,
            14000.0, 16000.0, true, true, "CBR250RR // SPORT+", listOf("Sport+", "Sport", "Comfort"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            32, "honda_cb750_hornet", "Honda", "CB750 Hornet / XL750 Transalp (5\" TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, false, true, "HORNET // TRANSALP", listOf("Type 1 Circle", "Type 2 Bar", "Type 3 Analog", "Type 4 Simple"),
            0xFFF4F6F9, 0xFF0D1016, 0xFF07090E, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            33, "honda_cb1000r_nsc", "Honda", "CB1000R Neo Sports Café", "2021–2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, false, true, "NEO SPORTS CAFÉ", listOf("Ring TFT"),
            0xFFF2F4F8, 0xFF0E1118, 0xFF080A0F, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            34, "honda_africa_twin_1100", "Honda", "CRF1100L Africa Twin (6.5\" MID TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 10000.0, false, true, "TRUE ADVENTURE // CRF", listOf("Gold", "Silver", "Bronze"),
            0xFFF5F5F5, 0xFF10131A, 0xFF080A0E, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            35, "honda_xadv_750", "Honda", "X-ADV 750 / Forza 750 (5\" Octagon TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6500.0, 8000.0, false, true, "X-ADV // FORZA", listOf("Standard", "Sport", "Gravel"),
            0xFFF4F4F6, 0xFF101218, 0xFF080A0E, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            36, "honda_forza_350", "Honda", "Forza 250 / 350 (Dual Analog + TFT)", "2021+",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, false, "FORZA // GT", listOf("Symmetrical GT"),
            0xFF0C0F18, 0xFFFFFFFF, 0xFF07090F, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            37, "honda_pcx_160", "Honda", "PCX 125 / 160 (Panoramic Blue/White LCD)", "2021+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, false, "PCX // eSP+", listOf("Panoramic Blue LCD"),
            0xFF071428, 0xFFE8F4FF, 0xFF050E1C, 0xFFFFFFFF, 0xFF00B0FF,
            0xFF00E5FF, 0xFFE8F4FF, 0xFF050E1C
        ),
        DeclarativeGaugeSpec(
            38, "honda_s2000_ap1_f1", "Honda", "S2000 (AP1 F1-Spec 9000 RPM Digital Arc)", "1999–2003",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8800.0, 9500.0, true, true, "VTEC // 9000 RPM F1", listOf("AP1 F1 Arc"),
            0xFF0A0604, 0xFFFF8C00, 0xFF070402, 0xFFFF9900, 0xFFFF2200,
            0xFFFF5500, 0xFFFF1E00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 4: KAWASAKI (12 Model: #39 - #50)
        // =====================================================================
        DeclarativeGaugeSpec(
            39, "kawasaki_zx10r_2021", "Kawasaki", "Ninja ZX-10R / ZX-10RR (4.3\" TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            14300.0, 16000.0, true, true, "RIVER MARK // ZX-10R", listOf("Type 1 Street", "Type 2 Circuit"),
            0xFFF2F5F2, 0xFF0B0E0B, 0xFF060906, 0xFFF2F7F2, 0xFF64DD17,
            0xFFFFB300, 0xFFFFB300, 0xFF060906
        ),
        DeclarativeGaugeSpec(
            40, "kawasaki_zx10r_led_arch", "Kawasaki", "Ninja ZX-10R (LED Arch Bar + LCD)", "2011–2020",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.KAWASAKI_LED_ARCH_BAR,
            14200.0, 16000.0, true, true, "NINJA // KRT", listOf("Race LED Arch"),
            0xFF0D1014, 0xFFF5F5F5, 0xFF080A0D, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            41, "kawasaki_zx6r_636_analog", "Kawasaki", "Ninja ZX-6R 636 (Analog + Negative LCD)", "2013–2023",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            15500.0, 18000.0, false, true, "NINJA // 636", listOf("Supersport Analog"),
            0xFF0B0E12, 0xFFFFFFFF, 0xFF07090C, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFD600, 0xFFFFD600, 0xFF07090C
        ),
        DeclarativeGaugeSpec(
            42, "kawasaki_zx6r_2024_tft", "Kawasaki", "Ninja ZX-6R (4.3\" Color TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            15000.0, 16500.0, true, true, "NINJA // ZX-6R", listOf("Type 1", "Type 2 Circuit"),
            0xFFF2F5F2, 0xFF0B0E0B, 0xFF060906, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            43, "kawasaki_zx4rr", "Kawasaki", "Ninja ZX-4RR (16,000+ RPM 4.3\" TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            16000.0, 18000.0, true, true, "ZX-4RR // SCREAMER", listOf("Circuit", "Street"),
            0xFFF2F5F2, 0xFF0B0E0B, 0xFF060906, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFFB300, 0xFF060906
        ),
        DeclarativeGaugeSpec(
            44, "kawasaki_ninja_h2", "Kawasaki", "Ninja H2 / H2R (3D Chronograph + Boost TFT)", "2015–2025",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            14000.0, 16000.0, true, true, "SUPERCHARGED // H2R", listOf("Boost Chronograph", "Track Plus"),
            0xFF080A0C, 0xFFFFFFFF, 0xFF050608, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            45, "kawasaki_z900_tft", "Kawasaki", "Z900 (4.3\" TFT Full Bar Invert)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10800.0, 12000.0, true, true, "SUGOMI // Z900", listOf("Type 1", "Type 2"),
            0xFFF2F5F2, 0xFF0C0F0C, 0xFF070907, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFFB300, 0xFF070907
        ),
        DeclarativeGaugeSpec(
            46, "kawasaki_z900_c_ring_lcd", "Kawasaki", "Z900 (C-Ring Negative LCD)", "2017–2019",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10800.0, 12000.0, false, true, "Z // SUGOMI", listOf("Pattern 1", "Pattern 2", "Pattern 3"),
            0xFF0B0E12, 0xFFF0F4F8, 0xFF07090C, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFD600, 0xFFFFFFFF, 0xFF07090C
        ),
        DeclarativeGaugeSpec(
            47, "kawasaki_ninja_650_z650", "Kawasaki", "Ninja 650 / Z650 (4.3\" TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, true, true, "KAWASAKI // 650", listOf("Type 1", "Type 2"),
            0xFFF2F5F2, 0xFF0C0F0C, 0xFF070907, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFFB300, 0xFF070907
        ),
        DeclarativeGaugeSpec(
            48, "kawasaki_ninja_400", "Kawasaki", "Ninja 400 / Ninja 250 (Analog + LCD)", "2018–2023",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            12000.0, 14000.0, false, true, "NINJA // 400", listOf("Factory Hybrid"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF07080B, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            49, "kawasaki_ninja_500_se", "Kawasaki", "Ninja 500 / Z500 SE", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "NINJA // 500 SE", listOf("SE TFT", "High-Contrast LCD"),
            0xFFF2F5F2, 0xFF0C0F0C, 0xFF070907, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFFB300, 0xFF070907
        ),
        DeclarativeGaugeSpec(
            50, "kawasaki_z_h2", "Kawasaki", "Z H2 Supercharged (4.3\" Boost TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12000.0, 14000.0, true, true, "Z H2 // SUPERCHARGED", listOf("Type 1 Boost", "Type 2 Track"),
            0xFFF2F5F2, 0xFF0C0F0C, 0xFF060806, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFF1744, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 5: BMW MOTORRAD (10 Model: #51 - #60)
        // =====================================================================
        DeclarativeGaugeSpec(
            51, "bmw_s1000rr_k67", "BMW Motorrad", "M1000RR / S1000RR (K67 6.5\" TFT)", "2019–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            14600.0, 16000.0, true, true, "M POWER // RR", listOf("Core 1", "Core 2 Analog", "Core 3 Lap", "Pure Ride"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFF0099FF, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            52, "bmw_s1000rr_k46", "BMW Motorrad", "S1000RR (K46 Silver Analog + Grey LCD)", "2009–2018",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            14200.0, 16000.0, true, true, "RR // RACE ABS", listOf("K46 Factory Analog"),
            0xFFE8ECF2, 0xFF12161F, 0xFF10141C, 0xFFE8ECF2, 0xFF0066B1,
            0xFFFFFFFF, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            53, "bmw_m1000r", "BMW Motorrad", "S1000R / M1000R Roadster Core TFT", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12500.0, 14000.0, true, true, "M ROADSTER // R", listOf("Core 1", "Core 2", "Pure Ride"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFF0099FF, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            54, "bmw_s1000xr", "BMW Motorrad", "S1000XR / M1000XR Sport-Touring TFT", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12000.0, 14000.0, true, true, "XR // M MOTORSPORT", listOf("Pure Ride", "Core 1", "Core 2"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFF0099FF, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            55, "bmw_r1300gs", "BMW Motorrad", "R1300GS / R1250GS (6.5\" Connectivity TFT)", "2019–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, true, "GS // SPIRIT OF GS", listOf("Pure Ride", "Sport Lean Screen", "Offroad"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            56, "bmw_f900r_xr", "BMW Motorrad", "F900R / F900XR (6.5\" Connectivity TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9200.0, 10500.0, false, true, "BMW MOTORRAD // F900", listOf("Pure Ride", "Sport Core"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            57, "bmw_g310r_gs", "BMW Motorrad", "G310R / G310GS (Monochrome Bar LCD)", "2017–2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            10500.0, 12000.0, false, true, "BMW // G310", listOf("Monochrome LCD"),
            0xFFD8DEE8, 0xFF10141C, 0xFF0B0E14, 0xFFE5ECF5, 0xFF0066B1,
            0xFFE60012, 0xFF10141C, 0xFFD8DEE8
        ),
        DeclarativeGaugeSpec(
            58, "bmw_r_ninet", "BMW Motorrad", "R nineT / R12 nineT (Dual Chrome Analog)", "2014–2024",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, false, "R nineT // BOXER", listOf("Heritage Chrome"),
            0xFF0C0E12, 0xFFF5F5F5, 0xFF08090C, 0xFFFFE0B2, 0xFF0066B1,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            59, "bmw_c400x_gt", "BMW Motorrad", "C400X / C400GT Connectivity TFT", "2019+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, false, "URBAN MOBILITY // C400", listOf("Pure Ride"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            60, "bmw_hp4_race", "BMW Motorrad", "HP4 Race (2D Datalogger Pure Track)", "2017–2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.MOTOGP_MARSHAL_BAR,
            14800.0, 16500.0, true, true, "HP4 RACE // 2D DATALOGGER", listOf("WSBK Track Logger"),
            0xFF06080C, 0xFFFFFFFF, 0xFF040508, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFFD600, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 6: DUCATI (10 Model: #61 - #70)
        // =====================================================================
        DeclarativeGaugeSpec(
            61, "ducati_panigale_v4_evo", "Ducati", "Panigale V4 / V4 S / V4 R (5\" Track Evo TFT)", "2023–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.MOTOGP_MARSHAL_BAR,
            14500.0, 16500.0, true, true, "DUCATI CORSE // V4", listOf("Track Evo", "Road"),
            0xFFF5F5F5, 0xFF0B0B0E, 0xFF060608, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            62, "ducati_panigale_v2", "Ducati", "Panigale V2 / 959 (4.3\" Circular Arc TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            11500.0, 13000.0, true, true, "PANIGALE V2", listOf("Track", "Road"),
            0xFFF5F5F5, 0xFF0B0B0E, 0xFF070709, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            63, "ducati_1199_1299", "Ducati", "1199 / 1299 Panigale Superquadro TFT", "2012–2017",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            11500.0, 12500.0, true, true, "SUPERQUADRO // 1299", listOf("Track", "Road", "Race"),
            0xFF0A0A0E, 0xFFFFFFFF, 0xFF050508, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            64, "ducati_streetfighter_v4s", "Ducati", "Streetfighter V4 S (Naked Track Evo TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.MOTOGP_MARSHAL_BAR,
            14500.0, 16500.0, true, true, "FIGHT FORMULA // V4S", listOf("Track Evo", "Road"),
            0xFFF5F5F5, 0xFF0B0B0E, 0xFF060608, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            65, "ducati_monster_937", "Ducati", "Monster 937 / SP (4.3\" Swept Arc TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10200.0, 12000.0, false, true, "MONSTER // IL MONSTRO", listOf("Sport", "Touring", "Urban"),
            0xFFF5F5F5, 0xFF0C0C10, 0xFF07070A, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            66, "ducati_multistrada_v4", "Ducati", "Multistrada V4 S / Pikes Peak (6.5\" TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, false, true, "MULTISTRADA V4 // GRANTURISMO", listOf("Sport", "Touring", "Enduro"),
            0xFFF5F5F5, 0xFF0C0C10, 0xFF07070A, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            67, "ducati_hypermotard_698", "Ducati", "Hypermotard 950 RVE / 698 Mono", "2020–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            10250.0, 12000.0, true, true, "HYPER // SUPERQUADRO MONO", listOf("Track", "Road"),
            0xFF0B0B0F, 0xFFFFFFFF, 0xFF060609, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            68, "ducati_diavel_v4", "Ducati", "Diavel V4 (5\" Muscle Cruiser TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, false, true, "DIAVEL V4", listOf("Sport", "Touring"),
            0xFFF5F5F5, 0xFF0C0C10, 0xFF060609, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            69, "ducati_scrambler_800", "Ducati", "Scrambler 800 Next-Gen (4.3\" Ellipse TFT)", "2023+",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, false, "LAND OF JOY // SCRAMBLER", listOf("Retro Ellipse"),
            0xFFF5F5F5, 0xFF101014, 0xFF08080B, 0xFFFFFFFF, 0xFFFFC400,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            70, "ducati_desmosedici_gp24", "Ducati", "Desmosedici GP24 (MotoGP Official Prototype)", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.MOTOGP_MARSHAL_BAR,
            17500.0, 19000.0, true, true, "DUCATI LENOVO // GP24", listOf("MotoGP Race", "Qualifying"),
            0xFF050508, 0xFFFFFFFF, 0xFF040406, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFFD600, 0xFFFF0000, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 7: SUZUKI (9 Model: #71 - #79)
        // =====================================================================
        DeclarativeGaugeSpec(
            71, "suzuki_hayabusa_gen3", "Suzuki", "Hayabusa (Gen 3: 5-Dial Analog + Center TFT)", "2021+",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.FIVE_DIAL_HAYABUSA_HYBRID, ShiftLedPattern.DUAL_SIDE_LIGHTNING_LEDS,
            10800.0, 12000.0, true, true, "HAYABUSA // 隼", listOf("SDMS-alpha Active Data"),
            0xFF0A0C12, 0xFFFFFFFF, 0xFF06070B, 0xFFFFFFFF, 0xFFFF9100,
            0xFFFFAB00, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            72, "suzuki_hayabusa_gen2", "Suzuki", "Hayabusa (Gen 2: 4-Analog + Gear LCD)", "2008–2020",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.FIVE_DIAL_HAYABUSA_HYBRID, ShiftLedPattern.SINGLE_UPPER_LED,
            11000.0, 12500.0, false, true, "GSX1300R // 隼", listOf("Gen 2 Factory"),
            0xFF0D1018, 0xFFFFFFFF, 0xFF080A10, 0xFFFFFFFF, 0xFFFF3D00,
            0xFFFFFFFF, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            73, "suzuki_hayabusa_gen1_340", "Suzuki", "Hayabusa (Gen 1: 340 km/h Unfathomed Analog)", "1999–2000",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10800.0, 13000.0, false, false, "HAYABUSA // 340 KM/H", listOf("1999 340 km/h Spec"),
            0xFF14161C, 0xFFF5E6C8, 0xFF0A0C10, 0xFFFFE8B8, 0xFFFF5722,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            74, "suzuki_gsxr1000r_l7", "Suzuki", "GSX-R1000R (L7+ Full Negative Race LCD)", "2017–2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.CONVERGING_5_LEDS,
            14500.0, 16000.0, true, true, "GSX-R // OWN THE RACETRACK", listOf("L7 Negative Race LCD"),
            0xFF080A10, 0xFFFFFFFF, 0xFF05070B, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFFFAB00, 0xFFFFFFFF, 0xFF05070B
        ),
        DeclarativeGaugeSpec(
            75, "suzuki_gsxr1000_k5", "Suzuki", "GSX-R1000 (K5/K6 & K7/K8 Analog + Amber LCD)", "2005–2008",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            13800.0, 16000.0, false, true, "GSX-R1000 // K5", listOf("K5 Amber", "K7 White"),
            0xFF0E1016, 0xFFFFFFFF, 0xFF090A0E, 0xFFFFAB00, 0xFFFF6D00,
            0xFFFFFFFF, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            76, "suzuki_gsxr600_750_l1", "Suzuki", "GSX-R600 / GSX-R750 (L1+ Analog + Gear LCD)", "2011–2024",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            15500.0, 17500.0, false, true, "GSX-R // 750", listOf("Factory Analog"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF07090E, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFFFFFFF, 0xFF00B0FF, 0xFF07090E
        ),
        DeclarativeGaugeSpec(
            77, "suzuki_gsx8s_8r", "Suzuki", "GSX-8S / GSX-8R (5\" Ring-Invert TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, true, true, "SUZUKI // GSX-8", listOf("Day White", "Night Black"),
            0xFFF4F6FA, 0xFF0C1018, 0xFF07090E, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFF9100, 0xFFFFAB00, 0xFF07090E
        ),
        DeclarativeGaugeSpec(
            78, "suzuki_gsxs1000_gt", "Suzuki", "GSX-S1000 / GSX-S1000GT (6.5\" TFT)", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "GSX-S1000 // GT", listOf("GT TFT", "Naked LCD"),
            0xFFF4F6FA, 0xFF0C1018, 0xFF07090E, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            79, "suzuki_vstrom_1050de", "Suzuki", "V-Strom 1050DE (5\" Adventure TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, true, "V-STROM // MASTER OF ADVENTURE", listOf("Adventure", "Gravel"),
            0xFFF5F5F5, 0xFF0D1016, 0xFF07090D, 0xFFFFFFFF, 0xFFFFD600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 8: APRILIA (6 Model: #80 - #85)
        // =====================================================================
        DeclarativeGaugeSpec(
            80, "aprilia_rsv4_factory", "Aprilia", "RSV4 Factory 1100 / Tuono V4 (5\" TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
            13600.0, 15500.0, true, true, "APRILIA RACING // BE A RACER", listOf("Track", "Road"),
            0xFFF5F5F5, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            81, "aprilia_rs660_tuono660", "Aprilia", "RS 660 / Tuono 660 (5\" Auto Day/Night TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
            11500.0, 13000.0, true, true, "RS 660 // APRC", listOf("Road", "Track"),
            0xFFF5F5F5, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            82, "aprilia_rs457", "Aprilia", "RS 457 (5\" Supersport TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
            10500.0, 12000.0, true, true, "RS 457 // BE A RACER", listOf("Sport", "Track"),
            0xFFF5F5F5, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            83, "aprilia_rsv4_analog", "Aprilia", "RSV4 (White-Face Analog + Grey Race LCD)", "2009–2016",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.CONVERGING_5_LEDS,
            14000.0, 16000.0, false, true, "RSV4 // V4 65°", listOf("WSBK Analog"),
            0xFFF0F2F5, 0xFF101216, 0xFF0C0E12, 0xFFF0F2F5, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            84, "aprilia_tuareg_660", "Aprilia", "Tuareg 660 (5\" Rally TFT)", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
            10000.0, 11500.0, false, true, "TUAREG // RALLY", listOf("Offroad", "Explore"),
            0xFFF5F5F5, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            85, "aprilia_sr_gt_200", "Aprilia", "SR GT 200 (Negative Sport-Scooter LCD)", "2022+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, false, "SR GT // APRILIA", listOf("Sport LCD"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFFFFFF, 0xFF07080B
        ),

        // =====================================================================
        // GRUP 9: TRIUMPH (6 Model: #86 - #91)
        // =====================================================================
        DeclarativeGaugeSpec(
            86, "triumph_street_triple_765_rs", "Triumph", "Street Triple 765 RS (5\" Pincer Bar TFT)", "2020–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SYMMETRIC_PINCER_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12650.0, 14000.0, true, true, "TRIUMPH // 765 RS", listOf("Stylus", "High Contrast", "Chronos", "Grid"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            87, "triumph_speed_triple_1200_rs", "Triumph", "Speed Triple 1200 RS / RR (5\" Circular TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11150.0, 12500.0, true, true, "SPEED TRIPLE // 1200 RS", listOf("Theme 1", "Theme 2"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            88, "triumph_daytona_660", "Triumph", "Daytona 660 (TFT + Upper LCD Hybrid)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12650.0, 14000.0, true, true, "DAYTONA // 660", listOf("Sport Hybrid"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            89, "triumph_daytona_675r", "Triumph", "Daytona 675R (Analog + 7 Blue LED Shift-Light)", "2013–2017",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.TRIUMPH_7_BLUE_LEDS,
            14400.0, 16500.0, true, true, "DAYTONA 675R // TRIPLE", listOf("675R Blue LED Analog"),
            0xFF0B0E14, 0xFFFFFFFF, 0xFF07090D, 0xFFFFFFFF, 0xFF2979FF,
            0xFF2979FF, 0xFF2979FF, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            90, "triumph_tiger_900_1200", "Triumph", "Tiger 900 / Tiger 1200 (7\" Panoramic TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SYMMETRIC_PINCER_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, true, "TIGER // FOR THE RIDE", listOf("Style 1", "Style 2", "Off-Road Pro"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            91, "triumph_thruxton_bonneville", "Triumph", "Thruxton RS / Bonneville T120 (Dual Chrome)", "2016–2024",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 9000.0, false, false, "BONNEVILLE // THRUXTON RS", listOf("Classic Twin Clock"),
            0xFF0D0F14, 0xFFF5F0E6, 0xFF08090C, 0xFFFFE4B5, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 10: MV AGUSTA & HUSQVARNA (5 Model: #92 - #96)
        // =====================================================================
        DeclarativeGaugeSpec(
            92, "mv_agusta_brutale_1000rr", "MV Agusta", "Brutale 1000 RR / F3 RR / Superveloce (5.5\" TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            14000.0, 16000.0, true, true, "MV AGUSTA // MOTORCYCLE ART", listOf("Track", "Road"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF06070A, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            93, "husqvarna_svartpilen_2024", "Husqvarna", "Svartpilen / Vitpilen 401 & 801 (5\" Bauhaus TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10200.0, 12000.0, false, true, "HUSQVARNA // PIONEERING SINCE 1903", listOf("Street", "Dynamic"),
            0xFFF5F5F5, 0xFF0C0E12, 0xFF08090C, 0xFFFFFFFF, 0xFFFFEA00,
            0xFFFFEA00, 0xFFFFEA00, 0xFF08090C
        ),
        DeclarativeGaugeSpec(
            94, "husqvarna_svartpilen_puck_lcd", "Husqvarna", "Svartpilen 401 / 701 (Round Puck LCD)", "2018–2023",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            10000.0, 12000.0, false, false, "SVARTPILEN // VITPILEN", listOf("Monochrome Puck"),
            0xFF0D0F14, 0xFFF5F5F5, 0xFF08090C, 0xFFFFEA00, 0xFFFFEA00,
            0xFFFFEA00, 0xFFFFEA00, 0xFF08090C
        ),
        DeclarativeGaugeSpec(
            95, "husqvarna_norden_901", "Husqvarna", "Norden 901 Expedition (Cyan/Yellow TFT)", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, false, true, "NORDEN 901 // EXPEDITION", listOf("Explorer", "Street", "Offroad"),
            0xFFF5F5F5, 0xFF0C0E12, 0xFF07090D, 0xFFFFFFFF, 0xFFFFEA00,
            0xFF00E5FF, 0xFFFFEA00, 0xFF07090D
        ),
        DeclarativeGaugeSpec(
            96, "mv_agusta_f4_rr", "MV Agusta", "F4 RR Corsacorta (Square Race LCD/Analog)", "2013–2018",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            14200.0, 16000.0, true, true, "F4 RR // 312 KM/H", listOf("Corsacorta LCD"),
            0xFFD8E0EC, 0xFF0F1218, 0xFF090B10, 0xFFE8EEF5, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 11: CFMOTO, BENELLI & ROYAL ENFIELD (6 Model: #97 - #102)
        // =====================================================================
        DeclarativeGaugeSpec(
            97, "cfmoto_450sr_675srr", "CFMoto", "450SR / 450SS & 675SR-R (5\" Curved TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "CFMOTO // SR RACING", listOf("Track", "Eco"),
            0xFFF2F5F8, 0xFF0C1018, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            98, "cfmoto_800nk_adv", "CFMoto", "800NK Advanced (8\" Split MMI TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, false, true, "CFMOTO // 800NK", listOf("Sport", "Street", "Rain"),
            0xFFF2F5F8, 0xFF0C1018, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            99, "cfmoto_250sr_300sr", "CFMoto", "250SR / 300SR (Sport Dual-Mode TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, false, true, "CFMOTO // PLAY TO WIN", listOf("Sport", "Eco"),
            0xFF0A0D14, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            100, "benelli_tnt600i_trk702", "Benelli", "TNT 600i / TRK 702X (5\" Color TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, false, true, "BENELLI // PESARO 1911", listOf("Day", "Night"),
            0xFFF2F4F8, 0xFF0E121A, 0xFF07090E, 0xFFFFFFFF, 0xFF00C853,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            101, "royal_enfield_himalayan_450", "Royal Enfield", "Himalayan 450 (4\" Round Tripper Dash TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, true, "BUILT FOR ALL ROADS // RE", listOf("Analog Style", "Digital Map"),
            0xFFF5F5F5, 0xFF101216, 0xFF08090C, 0xFFFFFFFF, 0xFFFFAB00,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            102, "royal_enfield_gt650", "Royal Enfield", "Continental GT 650 / Interceptor 650", "2018–2025",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 9000.0, false, false, "PURE MOTORCYCLING // 650 TWIN", listOf("Classic Twin Analog"),
            0xFF0E1014, 0xFFF5F0E6, 0xFF08090C, 0xFFFFE4B5, 0xFFFF6D00,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 12: HARLEY-DAVIDSON, INDIAN & VESPA (3 Model: #103 - #105)
        // =====================================================================
        DeclarativeGaugeSpec(
            103, "harley_sportster_s", "Harley-Davidson", "Sportster S (4\" Round Black/Orange TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, true, "HARLEY-DAVIDSON // REVOLUTION MAX", listOf("Sport", "Road"),
            0xFF0A0B0E, 0xFFFFFFFF, 0xFF050608, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF6600, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            104, "harley_iron_883_48", "Harley-Davidson", "Iron 883 / Forty-Eight (Single Classic Analog)", "2014–2022",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6000.0, 7500.0, false, false, "MILWAUKEE // EVOLUTION", listOf("Classic Analog + LCD"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFB74D, 0xFFFF6600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            105, "vespa_gts_300_supertech", "Vespa", "GTS 300 SuperTech (4.3\" Italian TFT & Retro Fan)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, false, "VESPA // SUPERTECH HPE", listOf("Italian TFT", "Retro Fan"),
            0xFFF5F7FA, 0xFF0E141C, 0xFF070A10, 0xFFFFFFFF, 0xFF00E676,
            0xFFFFAB00, 0xFFFF1744, 0xFFFFFFFF
        )
    )

    val allBrands: List<String> = all105Models.map { it.brand }.distinct()

    fun getBySkinId(skinId: String): DeclarativeGaugeSpec =
        all105Models.firstOrNull { it.skinId == skinId } ?: all105Models.first()
}

