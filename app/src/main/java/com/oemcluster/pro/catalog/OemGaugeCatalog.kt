package com.oemcluster.pro.catalog

/**
 * MotoCross — 35+ Global Markanın Tüm Motosikletlerinin Birebir OEM Gösterge Şartnamesi
 * (Scooter ve otomobil kadranları hariç — yalnızca saf motosiklet kataloğu)
 *
 * ALTIN KURAL:
 * Buradaki her model gerçek OEM kadranını birebir çizer (DeclarativeGaugeSpec + Bespoke Renderer).
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
    DUAL_CLASSIC_ANALOG,        // BMW R nineT, Triumph Bonneville/Thruxton, Royal Enfield GT 650, Concours 14
    SINGLE_PUCK_ROUND_TFT_LCD,  // Husqvarna Svartpilen/Vitpilen 401/801 Bauhaus, RE Himalayan 450, Sportster S, Iron 883
    F1_DIGITAL_ARC_BAR,         // KTM Duke Gen1, Yamaha R3 Gen2, GSX-R1000R L7 Negatif LCD, ZX-10R LED Arch
    RALLY_TOWER_VERTICAL_TFT,   // Yamaha Ténéré 700 World Raid, Aprilia Tuareg 660, CRF1100L Africa Twin, Kove 450 Rally
    PANORAMIC_SPLIT_TFT         // BMW R1300GS, Ducati Multistrada V4, Triumph Tiger 1200, CFMoto 800NK, Gold Wing Tour
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
    val modelNumber: Int,               // 1..307
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

object OemGaugeCatalog {

    val allModels: List<DeclarativeGaugeSpec> = listOf(
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

        // =====================================================================
        // GRUP 3: HONDA
        // =====================================================================
        DeclarativeGaugeSpec(
            24, "honda_cbr1000rrr_sp", "Honda", "CBR1000RR-R Fireblade SP (5\" TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            14500.0, 16000.0, true, true, "BORN TO RACE // HRC", listOf("Circuit", "Street", "Mechanic"),
            0xFFF5F5F5, 0xFF0E1015, 0xFF07080C, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            25, "honda_cbr1000rr_sc77", "Honda", "CBR1000RR Fireblade (SC77 TFT)", "2017–2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            13500.0, 15000.0, true, true, "TOTAL CONTROL // CBR", listOf("Street", "Circuit", "Mechanic"),
            0xFFF2F2F2, 0xFF101218, 0xFF080A0F, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            26, "honda_cbr600rr_2024", "Honda", "CBR600RR (5\" Fireblade-Style TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            15000.0, 16500.0, true, true, "AWAKEN THE RACE // 600RR", listOf("Circuit", "Street"),
            0xFFF5F5F5, 0xFF0E1015, 0xFF07080C, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            27, "honda_cbr600rr_pc40", "Honda", "CBR600RR (PC40 Central Analog + LCD)", "2007–2012",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            15000.0, 17000.0, false, true, "CBR600RR // HESD", listOf("Factory Analog"),
            0xFF0C0E14, 0xFFFFFFFF, 0xFF08090D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            28, "honda_cbr650r_lcd", "Honda", "CBR650R / CB650R (Dual Negative LCD)", "2019–2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            12000.0, 14000.0, true, true, "FOUR CYLINDER // 650R", listOf("Negative LCD"),
            0xFF0A0D14, 0xFFF2F5FA, 0xFF06080D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFFFFFFF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            29, "honda_cbr650r_2024_tft", "Honda", "CBR650R / CB650R (5\" E-Clutch TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12000.0, 14000.0, false, true, "HONDA // E-CLUTCH", listOf("Bar", "Circle", "Simple"),
            0xFFF5F5F5, 0xFF0D1016, 0xFF07090D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            30, "honda_cbr250rr_mc51", "Honda", "CBR250RR (MC51 Sport+ Race LCD)", "2017–2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.CONVERGING_5_LEDS,
            14000.0, 16000.0, true, true, "CBR250RR // SPORT+", listOf("Sport+", "Sport", "Comfort"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            31, "honda_cb750_hornet", "Honda", "CB750 Hornet / XL750 Transalp (5\" TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, false, true, "HORNET // TRANSALP", listOf("Type 1 Circle", "Type 2 Bar", "Type 3 Analog", "Type 4 Simple"),
            0xFFF4F6F9, 0xFF0D1016, 0xFF07090E, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            32, "honda_cb1000r_nsc", "Honda", "CB1000R Neo Sports Café", "2021–2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, false, true, "NEO SPORTS CAFÉ", listOf("Ring TFT"),
            0xFFF2F4F8, 0xFF0E1118, 0xFF080A0F, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            33, "honda_africa_twin_1100", "Honda", "CRF1100L Africa Twin (6.5\" MID TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 10000.0, false, true, "TRUE ADVENTURE // CRF", listOf("Gold", "Silver", "Bronze"),
            0xFFF5F5F5, 0xFF10131A, 0xFF080A0E, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 4: KAWASAKI
        // =====================================================================
        DeclarativeGaugeSpec(
            34, "kawasaki_zx10r_2021", "Kawasaki", "Ninja ZX-10R / ZX-10RR (4.3\" TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            14300.0, 16000.0, true, true, "RIVER MARK // ZX-10R", listOf("Type 1 Street", "Type 2 Circuit"),
            0xFFF2F5F2, 0xFF0B0E0B, 0xFF060906, 0xFFF2F7F2, 0xFF64DD17,
            0xFFFFB300, 0xFFFFB300, 0xFF060906
        ),
        DeclarativeGaugeSpec(
            35, "kawasaki_zx10r_led_arch", "Kawasaki", "Ninja ZX-10R (LED Arch Bar + LCD)", "2011–2020",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.KAWASAKI_LED_ARCH_BAR,
            14200.0, 16000.0, true, true, "NINJA // KRT", listOf("Race LED Arch"),
            0xFF0D1014, 0xFFF5F5F5, 0xFF080A0D, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            36, "kawasaki_zx6r_636_analog", "Kawasaki", "Ninja ZX-6R 636 (Analog + Negative LCD)", "2013–2023",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            15500.0, 18000.0, false, true, "NINJA // 636", listOf("Supersport Analog"),
            0xFF0B0E12, 0xFFFFFFFF, 0xFF07090C, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFD600, 0xFFFFD600, 0xFF07090C
        ),
        DeclarativeGaugeSpec(
            37, "kawasaki_zx6r_2024_tft", "Kawasaki", "Ninja ZX-6R (4.3\" Color TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            15000.0, 16500.0, true, true, "NINJA // ZX-6R", listOf("Type 1", "Type 2 Circuit"),
            0xFFF2F5F2, 0xFF0B0E0B, 0xFF060906, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            38, "kawasaki_zx4rr", "Kawasaki", "Ninja ZX-4RR (16,000+ RPM 4.3\" TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            16000.0, 18000.0, true, true, "ZX-4RR // SCREAMER", listOf("Circuit", "Street"),
            0xFFF2F5F2, 0xFF0B0E0B, 0xFF060906, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFFB300, 0xFF060906
        ),
        DeclarativeGaugeSpec(
            39, "kawasaki_ninja_h2", "Kawasaki", "Ninja H2 / H2R (3D Chronograph + Boost TFT)", "2015–2025",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            14000.0, 16000.0, true, true, "SUPERCHARGED // H2R", listOf("Boost Chronograph", "Track Plus"),
            0xFF080A0C, 0xFFFFFFFF, 0xFF050608, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            40, "kawasaki_z900_tft", "Kawasaki", "Z900 (4.3\" TFT Full Bar Invert)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10800.0, 12000.0, true, true, "SUGOMI // Z900", listOf("Type 1", "Type 2"),
            0xFFF2F5F2, 0xFF0C0F0C, 0xFF070907, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFFB300, 0xFF070907
        ),
        DeclarativeGaugeSpec(
            41, "kawasaki_z900_c_ring_lcd", "Kawasaki", "Z900 (C-Ring Negative LCD)", "2017–2019",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10800.0, 12000.0, false, true, "Z // SUGOMI", listOf("Pattern 1", "Pattern 2", "Pattern 3"),
            0xFF0B0E12, 0xFFF0F4F8, 0xFF07090C, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFD600, 0xFFFFFFFF, 0xFF07090C
        ),
        DeclarativeGaugeSpec(
            42, "kawasaki_ninja_650_z650", "Kawasaki", "Ninja 650 / Z650 (4.3\" TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, true, true, "KAWASAKI // 650", listOf("Type 1", "Type 2"),
            0xFFF2F5F2, 0xFF0C0F0C, 0xFF070907, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFFB300, 0xFF070907
        ),
        DeclarativeGaugeSpec(
            43, "kawasaki_ninja_400", "Kawasaki", "Ninja 400 / Ninja 250 (Analog + LCD)", "2018–2023",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            12000.0, 14000.0, false, true, "NINJA // 400", listOf("Factory Hybrid"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF07080B, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            44, "kawasaki_ninja_500_se", "Kawasaki", "Ninja 500 / Z500 SE", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "NINJA // 500 SE", listOf("SE TFT", "High-Contrast LCD"),
            0xFFF2F5F2, 0xFF0C0F0C, 0xFF070907, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFFB300, 0xFF070907
        ),
        DeclarativeGaugeSpec(
            45, "kawasaki_z_h2", "Kawasaki", "Z H2 Supercharged (4.3\" Boost TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12000.0, 14000.0, true, true, "Z H2 // SUPERCHARGED", listOf("Type 1 Boost", "Type 2 Track"),
            0xFFF2F5F2, 0xFF0C0F0C, 0xFF060806, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFFFF1744, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 5: BMW MOTORRAD (10 Model: #51 - #60)
        // =====================================================================
        DeclarativeGaugeSpec(
            46, "bmw_s1000rr_k67", "BMW Motorrad", "M1000RR / S1000RR (K67 6.5\" TFT)", "2019–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            14600.0, 16000.0, true, true, "M POWER // RR", listOf("Core 1", "Core 2 Analog", "Core 3 Lap", "Pure Ride"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFF0099FF, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            47, "bmw_s1000rr_k46", "BMW Motorrad", "S1000RR (K46 Silver Analog + Grey LCD)", "2009–2018",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            14200.0, 16000.0, true, true, "RR // RACE ABS", listOf("K46 Factory Analog"),
            0xFFE8ECF2, 0xFF12161F, 0xFF10141C, 0xFFE8ECF2, 0xFF0066B1,
            0xFFFFFFFF, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            48, "bmw_m1000r", "BMW Motorrad", "S1000R / M1000R Roadster Core TFT", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12500.0, 14000.0, true, true, "M ROADSTER // R", listOf("Core 1", "Core 2", "Pure Ride"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFF0099FF, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            49, "bmw_s1000xr", "BMW Motorrad", "S1000XR / M1000XR Sport-Touring TFT", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12000.0, 14000.0, true, true, "XR // M MOTORSPORT", listOf("Pure Ride", "Core 1", "Core 2"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFF0099FF, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            50, "bmw_r1300gs", "BMW Motorrad", "R1300GS / R1250GS (6.5\" Connectivity TFT)", "2019–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, true, "GS // SPIRIT OF GS", listOf("Pure Ride", "Sport Lean Screen", "Offroad"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            51, "bmw_f900r_xr", "BMW Motorrad", "F900R / F900XR (6.5\" Connectivity TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9200.0, 10500.0, false, true, "BMW MOTORRAD // F900", listOf("Pure Ride", "Sport Core"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            52, "bmw_g310r_gs", "BMW Motorrad", "G310R / G310GS (Monochrome Bar LCD)", "2017–2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            10500.0, 12000.0, false, true, "BMW // G310", listOf("Monochrome LCD"),
            0xFFD8DEE8, 0xFF10141C, 0xFF0B0E14, 0xFFE5ECF5, 0xFF0066B1,
            0xFFE60012, 0xFF10141C, 0xFFD8DEE8
        ),
        DeclarativeGaugeSpec(
            53, "bmw_r_ninet", "BMW Motorrad", "R nineT / R12 nineT (Dual Chrome Analog)", "2014–2024",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, false, "R nineT // BOXER", listOf("Heritage Chrome"),
            0xFF0C0E12, 0xFFF5F5F5, 0xFF08090C, 0xFFFFE0B2, 0xFF0066B1,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            54, "bmw_hp4_race", "BMW Motorrad", "HP4 Race (2D Datalogger Pure Track)", "2017–2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.MOTOGP_MARSHAL_BAR,
            14800.0, 16500.0, true, true, "HP4 RACE // 2D DATALOGGER", listOf("WSBK Track Logger"),
            0xFF06080C, 0xFFFFFFFF, 0xFF040508, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFFD600, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 6: DUCATI (10 Model: #61 - #70)
        // =====================================================================
        DeclarativeGaugeSpec(
            55, "ducati_panigale_v4_evo", "Ducati", "Panigale V4 / V4 S / V4 R (5\" Track Evo TFT)", "2023–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.MOTOGP_MARSHAL_BAR,
            14500.0, 16500.0, true, true, "DUCATI CORSE // V4", listOf("Track Evo", "Road"),
            0xFFF5F5F5, 0xFF0B0B0E, 0xFF060608, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            56, "ducati_panigale_v2", "Ducati", "Panigale V2 / 959 (4.3\" Circular Arc TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            11500.0, 13000.0, true, true, "PANIGALE V2", listOf("Track", "Road"),
            0xFFF5F5F5, 0xFF0B0B0E, 0xFF070709, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            57, "ducati_1199_1299", "Ducati", "1199 / 1299 Panigale Superquadro TFT", "2012–2017",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            11500.0, 12500.0, true, true, "SUPERQUADRO // 1299", listOf("Track", "Road", "Race"),
            0xFF0A0A0E, 0xFFFFFFFF, 0xFF050508, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            58, "ducati_streetfighter_v4s", "Ducati", "Streetfighter V4 S (Naked Track Evo TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.MOTOGP_MARSHAL_BAR,
            14500.0, 16500.0, true, true, "FIGHT FORMULA // V4S", listOf("Track Evo", "Road"),
            0xFFF5F5F5, 0xFF0B0B0E, 0xFF060608, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            59, "ducati_monster_937", "Ducati", "Monster 937 / SP (4.3\" Swept Arc TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10200.0, 12000.0, false, true, "MONSTER // IL MONSTRO", listOf("Sport", "Touring", "Urban"),
            0xFFF5F5F5, 0xFF0C0C10, 0xFF07070A, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            60, "ducati_multistrada_v4", "Ducati", "Multistrada V4 S / Pikes Peak (6.5\" TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, false, true, "MULTISTRADA V4 // GRANTURISMO", listOf("Sport", "Touring", "Enduro"),
            0xFFF5F5F5, 0xFF0C0C10, 0xFF07070A, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            61, "ducati_hypermotard_698", "Ducati", "Hypermotard 950 RVE / 698 Mono", "2020–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            10250.0, 12000.0, true, true, "HYPER // SUPERQUADRO MONO", listOf("Track", "Road"),
            0xFF0B0B0F, 0xFFFFFFFF, 0xFF060609, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            62, "ducati_diavel_v4", "Ducati", "Diavel V4 (5\" Muscle Cruiser TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, false, true, "DIAVEL V4", listOf("Sport", "Touring"),
            0xFFF5F5F5, 0xFF0C0C10, 0xFF060609, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            63, "ducati_scrambler_800", "Ducati", "Scrambler 800 Next-Gen (4.3\" Ellipse TFT)", "2023+",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, false, "LAND OF JOY // SCRAMBLER", listOf("Retro Ellipse"),
            0xFFF5F5F5, 0xFF101014, 0xFF08080B, 0xFFFFFFFF, 0xFFFFC400,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            64, "ducati_desmosedici_gp24", "Ducati", "Desmosedici GP24 (MotoGP Official Prototype)", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.MOTOGP_MARSHAL_BAR,
            17500.0, 19000.0, true, true, "DUCATI LENOVO // GP24", listOf("MotoGP Race", "Qualifying"),
            0xFF050508, 0xFFFFFFFF, 0xFF040406, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFFD600, 0xFFFF0000, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 7: SUZUKI (9 Model: #71 - #79)
        // =====================================================================
        DeclarativeGaugeSpec(
            65, "suzuki_hayabusa_gen3", "Suzuki", "Hayabusa (Gen 3: 5-Dial Analog + Center TFT)", "2021+",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.FIVE_DIAL_HAYABUSA_HYBRID, ShiftLedPattern.DUAL_SIDE_LIGHTNING_LEDS,
            10800.0, 12000.0, true, true, "HAYABUSA // 隼", listOf("SDMS-alpha Active Data"),
            0xFF0A0C12, 0xFFFFFFFF, 0xFF06070B, 0xFFFFFFFF, 0xFFFF9100,
            0xFFFFAB00, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            66, "suzuki_hayabusa_gen2", "Suzuki", "Hayabusa (Gen 2: 4-Analog + Gear LCD)", "2008–2020",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.FIVE_DIAL_HAYABUSA_HYBRID, ShiftLedPattern.SINGLE_UPPER_LED,
            11000.0, 12500.0, false, true, "GSX1300R // 隼", listOf("Gen 2 Factory"),
            0xFF0D1018, 0xFFFFFFFF, 0xFF080A10, 0xFFFFFFFF, 0xFFFF3D00,
            0xFFFFFFFF, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            67, "suzuki_hayabusa_gen1_340", "Suzuki", "Hayabusa (Gen 1: 340 km/h Unfathomed Analog)", "1999–2000",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10800.0, 13000.0, false, false, "HAYABUSA // 340 KM/H", listOf("1999 340 km/h Spec"),
            0xFF14161C, 0xFFF5E6C8, 0xFF0A0C10, 0xFFFFE8B8, 0xFFFF5722,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            68, "suzuki_gsxr1000r_l7", "Suzuki", "GSX-R1000R (L7+ Full Negative Race LCD)", "2017–2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.CONVERGING_5_LEDS,
            14500.0, 16000.0, true, true, "GSX-R // OWN THE RACETRACK", listOf("L7 Negative Race LCD"),
            0xFF080A10, 0xFFFFFFFF, 0xFF05070B, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFFFAB00, 0xFFFFFFFF, 0xFF05070B
        ),
        DeclarativeGaugeSpec(
            69, "suzuki_gsxr1000_k5", "Suzuki", "GSX-R1000 (K5/K6 & K7/K8 Analog + Amber LCD)", "2005–2008",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            13800.0, 16000.0, false, true, "GSX-R1000 // K5", listOf("K5 Amber", "K7 White"),
            0xFF0E1016, 0xFFFFFFFF, 0xFF090A0E, 0xFFFFAB00, 0xFFFF6D00,
            0xFFFFFFFF, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            70, "suzuki_gsxr600_750_l1", "Suzuki", "GSX-R600 / GSX-R750 (L1+ Analog + Gear LCD)", "2011–2024",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            15500.0, 17500.0, false, true, "GSX-R // 750", listOf("Factory Analog"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF07090E, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFFFFFFF, 0xFF00B0FF, 0xFF07090E
        ),
        DeclarativeGaugeSpec(
            71, "suzuki_gsx8s_8r", "Suzuki", "GSX-8S / GSX-8R (5\" Ring-Invert TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, true, true, "SUZUKI // GSX-8", listOf("Day White", "Night Black"),
            0xFFF4F6FA, 0xFF0C1018, 0xFF07090E, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFF9100, 0xFFFFAB00, 0xFF07090E
        ),
        DeclarativeGaugeSpec(
            72, "suzuki_gsxs1000_gt", "Suzuki", "GSX-S1000 / GSX-S1000GT (6.5\" TFT)", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "GSX-S1000 // GT", listOf("GT TFT", "Naked LCD"),
            0xFFF4F6FA, 0xFF0C1018, 0xFF07090E, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            73, "suzuki_vstrom_1050de", "Suzuki", "V-Strom 1050DE (5\" Adventure TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, true, "V-STROM // MASTER OF ADVENTURE", listOf("Adventure", "Gravel"),
            0xFFF5F5F5, 0xFF0D1016, 0xFF07090D, 0xFFFFFFFF, 0xFFFFD600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 8: APRILIA (6 Model: #80 - #85)
        // =====================================================================
        DeclarativeGaugeSpec(
            74, "aprilia_rsv4_factory", "Aprilia", "RSV4 Factory 1100 / Tuono V4 (5\" TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
            13600.0, 15500.0, true, true, "APRILIA RACING // BE A RACER", listOf("Track", "Road"),
            0xFFF5F5F5, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            75, "aprilia_rs660_tuono660", "Aprilia", "RS 660 / Tuono 660 (5\" Auto Day/Night TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
            11500.0, 13000.0, true, true, "RS 660 // APRC", listOf("Road", "Track"),
            0xFFF5F5F5, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            76, "aprilia_rs457", "Aprilia", "RS 457 (5\" Supersport TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
            10500.0, 12000.0, true, true, "RS 457 // BE A RACER", listOf("Sport", "Track"),
            0xFFF5F5F5, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            77, "aprilia_rsv4_analog", "Aprilia", "RSV4 (White-Face Analog + Grey Race LCD)", "2009–2016",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.CONVERGING_5_LEDS,
            14000.0, 16000.0, false, true, "RSV4 // V4 65°", listOf("WSBK Analog"),
            0xFFF0F2F5, 0xFF101216, 0xFF0C0E12, 0xFFF0F2F5, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            78, "aprilia_tuareg_660", "Aprilia", "Tuareg 660 (5\" Rally TFT)", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
            10000.0, 11500.0, false, true, "TUAREG // RALLY", listOf("Offroad", "Explore"),
            0xFFF5F5F5, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 9: TRIUMPH
        // =====================================================================
        DeclarativeGaugeSpec(
            79, "triumph_street_triple_765_rs", "Triumph", "Street Triple 765 RS (5\" Pincer Bar TFT)", "2020–2025",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SYMMETRIC_PINCER_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12650.0, 14000.0, true, true, "TRIUMPH // 765 RS", listOf("Stylus", "High Contrast", "Chronos", "Grid"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            80, "triumph_speed_triple_1200_rs", "Triumph", "Speed Triple 1200 RS / RR (5\" Circular TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11150.0, 12500.0, true, true, "SPEED TRIPLE // 1200 RS", listOf("Theme 1", "Theme 2"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            81, "triumph_daytona_660", "Triumph", "Daytona 660 (TFT + Upper LCD Hybrid)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12650.0, 14000.0, true, true, "DAYTONA // 660", listOf("Sport Hybrid"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            82, "triumph_daytona_675r", "Triumph", "Daytona 675R (Analog + 7 Blue LED Shift-Light)", "2013–2017",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.TRIUMPH_7_BLUE_LEDS,
            14400.0, 16500.0, true, true, "DAYTONA 675R // TRIPLE", listOf("675R Blue LED Analog"),
            0xFF0B0E14, 0xFFFFFFFF, 0xFF07090D, 0xFFFFFFFF, 0xFF2979FF,
            0xFF2979FF, 0xFF2979FF, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            83, "triumph_tiger_900_1200", "Triumph", "Tiger 900 / Tiger 1200 (7\" Panoramic TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SYMMETRIC_PINCER_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, true, "TIGER // FOR THE RIDE", listOf("Style 1", "Style 2", "Off-Road Pro"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            84, "triumph_thruxton_bonneville", "Triumph", "Thruxton RS / Bonneville T120 (Dual Chrome)", "2016–2024",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 9000.0, false, false, "BONNEVILLE // THRUXTON RS", listOf("Classic Twin Clock"),
            0xFF0D0F14, 0xFFF5F0E6, 0xFF08090C, 0xFFFFE4B5, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 10: MV AGUSTA & HUSQVARNA (5 Model: #92 - #96)
        // =====================================================================
        DeclarativeGaugeSpec(
            85, "mv_agusta_brutale_1000rr", "MV Agusta", "Brutale 1000 RR / F3 RR / Superveloce (5.5\" TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            14000.0, 16000.0, true, true, "MV AGUSTA // MOTORCYCLE ART", listOf("Track", "Road"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF06070A, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            86, "husqvarna_svartpilen_2024", "Husqvarna", "Svartpilen / Vitpilen 401 & 801 (5\" Bauhaus TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10200.0, 12000.0, false, true, "HUSQVARNA // PIONEERING SINCE 1903", listOf("Street", "Dynamic"),
            0xFFF5F5F5, 0xFF0C0E12, 0xFF08090C, 0xFFFFFFFF, 0xFFFFEA00,
            0xFFFFEA00, 0xFFFFEA00, 0xFF08090C
        ),
        DeclarativeGaugeSpec(
            87, "husqvarna_svartpilen_puck_lcd", "Husqvarna", "Svartpilen 401 / 701 (Round Puck LCD)", "2018–2023",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            10000.0, 12000.0, false, false, "SVARTPILEN // VITPILEN", listOf("Monochrome Puck"),
            0xFF0D0F14, 0xFFF5F5F5, 0xFF08090C, 0xFFFFEA00, 0xFFFFEA00,
            0xFFFFEA00, 0xFFFFEA00, 0xFF08090C
        ),
        DeclarativeGaugeSpec(
            88, "husqvarna_norden_901", "Husqvarna", "Norden 901 Expedition (Cyan/Yellow TFT)", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, false, true, "NORDEN 901 // EXPEDITION", listOf("Explorer", "Street", "Offroad"),
            0xFFF5F5F5, 0xFF0C0E12, 0xFF07090D, 0xFFFFFFFF, 0xFFFFEA00,
            0xFF00E5FF, 0xFFFFEA00, 0xFF07090D
        ),
        DeclarativeGaugeSpec(
            89, "mv_agusta_f4_rr", "MV Agusta", "F4 RR Corsacorta (Square Race LCD/Analog)", "2013–2018",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            14200.0, 16000.0, true, true, "F4 RR // 312 KM/H", listOf("Corsacorta LCD"),
            0xFFD8E0EC, 0xFF0F1218, 0xFF090B10, 0xFFE8EEF5, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 11: CFMOTO, BENELLI & ROYAL ENFIELD (6 Model: #97 - #102)
        // =====================================================================
        DeclarativeGaugeSpec(
            90, "cfmoto_450sr_675srr", "CFMoto", "450SR / 450SS & 675SR-R (5\" Curved TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "CFMOTO // SR RACING", listOf("Track", "Eco"),
            0xFFF2F5F8, 0xFF0C1018, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            91, "cfmoto_800nk_adv", "CFMoto", "800NK Advanced (8\" Split MMI TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12000.0, false, true, "CFMOTO // 800NK", listOf("Sport", "Street", "Rain"),
            0xFFF2F5F8, 0xFF0C1018, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            92, "cfmoto_250sr_300sr", "CFMoto", "250SR / 300SR (Sport Dual-Mode TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, false, true, "CFMOTO // PLAY TO WIN", listOf("Sport", "Eco"),
            0xFF0A0D14, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            93, "benelli_tnt600i_trk702", "Benelli", "TNT 600i / TRK 702X (5\" Color TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, false, true, "BENELLI // PESARO 1911", listOf("Day", "Night"),
            0xFFF2F4F8, 0xFF0E121A, 0xFF07090E, 0xFFFFFFFF, 0xFF00C853,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            94, "royal_enfield_himalayan_450", "Royal Enfield", "Himalayan 450 (4\" Round Tripper Dash TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, true, "BUILT FOR ALL ROADS // RE", listOf("Analog Style", "Digital Map"),
            0xFFF5F5F5, 0xFF101216, 0xFF08090C, 0xFFFFFFFF, 0xFFFFAB00,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            95, "royal_enfield_gt650", "Royal Enfield", "Continental GT 650 / Interceptor 650", "2018–2025",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 9000.0, false, false, "PURE MOTORCYCLING // 650 TWIN", listOf("Classic Twin Analog"),
            0xFF0E1014, 0xFFF5F0E6, 0xFF08090C, 0xFFFFE4B5, 0xFFFF6D00,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 12: HARLEY-DAVIDSON
        // =====================================================================
        DeclarativeGaugeSpec(
            96, "harley_sportster_s", "Harley-Davidson", "Sportster S (4\" Round Black/Orange TFT)", "2021+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, true, "HARLEY-DAVIDSON // REVOLUTION MAX", listOf("Sport", "Road"),
            0xFF0A0B0E, 0xFFFFFFFF, 0xFF050608, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF6600, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            97, "harley_iron_883_48", "Harley-Davidson", "Iron 883 / Forty-Eight (Single Classic Analog)", "2014–2022",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6000.0, 7500.0, false, false, "MILWAUKEE // EVOLUTION", listOf("Classic Analog + LCD"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFB74D, 0xFFFF6600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 13: TVS (7 Model: #106 - #112)
        // =====================================================================
        DeclarativeGaugeSpec(
            98, "tvs_apache_rtr_200_4v", "TVS", "Apache RTR 200 4V", "2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            10000.0, 11000.0, false, true, "APACHE // RTR 200 READY", listOf("Sport", "Urban / Rain"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFE30612, 0xFFE30612,
            0xFFE30612, 0xFFE30612, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            99, "tvs_apache_rtr_160_4v", "TVS", "Apache RTR 160 4V", "2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            10000.0, 11000.0, false, true, "APACHE // RTR 160 READY", listOf("Sport", "Urban / Rain"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFE30612,
            0xFFFF5500, 0xFFE30612, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            100, "tvs_apache_rtr_180", "TVS", "Apache RTR 180", "2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, true, "APACHE // RTR 180", listOf("Sport", "Urban"),
            0xFF0C0F18, 0xFFFFFFFF, 0xFF080A10, 0xFFFF6A00, 0xFFFF6A00,
            0xFFFF5500, 0xFF1A0900, 0xFFFF6A00
        ),
        DeclarativeGaugeSpec(
            101, "tvs_apache_rr310", "TVS", "Apache RR 310", "2017–2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9700.0, 11000.0, true, true, "APACHE // RACE CYLINDER", listOf("Race", "Urban / Rain"),
            0xFFF5F5F5, 0xFF141414, 0xFF06080D, 0xFFFFFFFF, 0xFFE30612,
            0xFFE30612, 0xFFFFFFFF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            102, "tvs_apache_rtr_310", "TVS", "Apache RTR 310", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9700.0, 11000.0, true, true, "APACHE // RTR 310", listOf("Sport", "Urban / Rain"),
            0xFFF5F5F5, 0xFF141414, 0xFF06080D, 0xFFFFFFFF, 0xFFE30612,
            0xFFE30612, 0xFFFFFFFF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            103, "tvs_ronin_225", "TVS", "Ronin 225", "2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 9000.0, false, true, "RONIN // UNLEASHED", listOf("Gravel", "Street"),
            0xFFF5F5F5, 0xFF141414, 0xFF08090C, 0xFFF2E9D4, 0xFFE30612,
            0xFFFF9100, 0xFFE30612, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            104, "tvs_raider_125", "TVS", "Raider 125", "2022+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            7500.0, 9000.0, false, true, "RAIDER // NEXT LEVEL RACING", listOf("Street", "Eco"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFE30612,
            0xFFE30612, 0xFF1E1A12, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 14: BAJAJ (10 Model: #113 - #122)
        // =====================================================================
        DeclarativeGaugeSpec(
            105, "bajaj_pulsar_ns160", "Bajaj", "Pulsar NS160", "2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            11000.0, 12500.0, true, true, "PULSAR // NS160 SPIRIT", listOf("Sport", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFFF3D00,
            0xFFFF3D00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            106, "bajaj_pulsar_ns200", "Bajaj", "Pulsar NS200", "2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            11500.0, 13000.0, true, true, "PULSAR // NS200 SPIRIT", listOf("Sport", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFF3D00, 0xFFFF3D00,
            0xFFFF3D00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            107, "bajaj_pulsar_rs200", "Bajaj", "Pulsar RS200", "2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "PULSAR // RS200 RACE SPEC", listOf("Race", "Sport"),
            0xFF0A0C12, 0xFFFFFFFF, 0xFF05070B, 0xFFFFFFFF, 0xFFFF3D00,
            0xFFFF3D00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            108, "bajaj_pulsar_n250", "Bajaj", "Pulsar N250", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 10500.0, false, true, "PULSAR // N250 NEO", listOf("Sport", "Urban"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFF00E5FF,
            0xFF00E5FF, 0xFF00E5FF, 0xFF07080A
        ),
        DeclarativeGaugeSpec(
            109, "bajaj_dominar_250", "Bajaj", "Dominar 250", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, true, "DOMINAR // 250 TOURING", listOf("Touring", "Sport"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFF00E5FF,
            0xFF00B0FF, 0xFF00E5FF, 0xFF07080A
        ),
        DeclarativeGaugeSpec(
            110, "bajaj_dominar_400", "Bajaj", "Dominar 400", "2021",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 10500.0, true, true, "DOMINAR // 400 TOURING", listOf("Touring", "Sport"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFF00E5FF,
            0xFF00B0FF, 0xFF00E5FF, 0xFF07080A
        ),
        DeclarativeGaugeSpec(
            111, "bajaj_pulsar_150", "Bajaj", "Pulsar 150", "2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 10500.0, false, true, "PULSAR // 150 SPIRIT", listOf("Street", "Urban"),
            0xFF0C0F18, 0xFFFFFFFF, 0xFF080A10, 0xFFFF6A00, 0xFFFF6A00,
            0xFFFF3D00, 0xFF1F0A00, 0xFFFF6A00
        ),
        DeclarativeGaugeSpec(
            112, "bajaj_avenger_street_220", "Bajaj", "Avenger Street 220", "2020",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, false, "AVENGER // CRUISER", listOf("Cruiser", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFB74D, 0xFFFF6A00,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            113, "bajaj_pulsar_n150", "Bajaj", "Pulsar N150", "2022",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, true, "PULSAR // N150 NEO", listOf("Street", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFF00E5FF, 0xFF00E5FF,
            0xFF00E5FF, 0xFF00E5FF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            114, "bajaj_pulsar_ns400z", "Bajaj", "Pulsar NS400 Z", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8800.0, 10500.0, true, true, "PULSAR // NS400 Z", listOf("Sport", "Track"),
            0xFFF5F5F5, 0xFF0A0A0A, 0xFF060608, 0xFFF5F5F5, 0xFFFF3D00,
            0xFFFF3D00, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 15: HERO (4 Model: #123 - #126)
        // =====================================================================
        DeclarativeGaugeSpec(
            115, "hero_xtreme_160r_4v", "Hero", "Xtreme 160R 4V", "2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            9500.0, 10500.0, true, true, "XTREME // RACE READY", listOf("Sport", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFF6600, 0xFFFF6600,
            0xFFFF6600, 0xFFFF6600, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            116, "hero_xtreme_200s", "Hero", "Xtreme 200S", "2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            8000.0, 9500.0, true, true, "XTREME // 200S RACE", listOf("Sport", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFF6600, 0xFFFF6600,
            0xFFFF9100, 0xFFFF6600, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            117, "hero_xpulse_200_4v", "Hero", "Xpulse 200 4V", "2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, true, "XPULSE // FEARLESS", listOf("Trail", "Rally"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFFEB3B,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            118, "hero_xpulse_210", "Hero", "Xpulse 210", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9250.0, 11000.0, false, true, "XPULSE // 210 RALLY", listOf("Rally", "Trail"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFFEB3B,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 16: QJMOTOR (4 Model: #127 - #130)
        // =====================================================================
        DeclarativeGaugeSpec(
            119, "qjmotor_srk_125", "QJMotor", "SRK 125", "2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, true, "SRK // STREET RACER", listOf("Street", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFF00E5FF, 0xFF00E5FF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            120, "qjmotor_srk_250", "QJMotor", "SRK 250", "2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            9500.0, 10500.0, false, true, "SRK // STREET RACER 250", listOf("Street", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFF00E5FF, 0xFF00E5FF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            121, "qjmotor_srk_400", "QJMotor", "SRK 400", "2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, true, true, "SRK // TWIN THUNDER", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFF00E5FF,
            0xFF00E5FF, 0xFF00E5FF, 0xFF07080A
        ),
        DeclarativeGaugeSpec(
            122, "qjmotor_srk_800", "QJMotor", "SRK 800", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, true, true, "SRK // INLINE-FOUR THUNDER", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFF00E5FF,
            0xFF00E5FF, 0xFF00E5FF, 0xFF07080A
        ),

        // =====================================================================
        // GRUP 17: ZONTES (3 Model: #131 - #133)
        // =====================================================================
        DeclarativeGaugeSpec(
            123, "zontes_zt125x5", "Zontes", "ZT125X5", "2023",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, true, "ZONTES // ZT125X5", listOf("Street", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFF00E5FF, 0xFFFFFFFF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            124, "zontes_310r", "Zontes", "310R", "2022",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11500.0, false, true, "ZONTES // 310R SPIRIT", listOf("Street", "Sport"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFF00E5FF,
            0xFF00E5FF, 0xFF00E5FF, 0xFF07080A
        ),
        DeclarativeGaugeSpec(
            125, "zontes_703f", "Zontes", "703F", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, true, true, "ZONTES // 703 TRIPLE", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFF00E5FF,
            0xFF00E5FF, 0xFF00E5FF, 0xFF07080A
        ),

        // =====================================================================
        // GRUP 18: BENELLI (6 Model: #134 - #139)
        // =====================================================================
        DeclarativeGaugeSpec(
            126, "benelli_trk_502", "Benelli", "TRK 502", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, true, "TRK // 502 ADVENTURE", listOf("Adventure", "Rally"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B4A0,
            0xFF00B4A0, 0xFF00B4A0, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            127, "benelli_leoncino_500", "Benelli", "Leoncino 500", "2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, true, "LEONCINO // LITTLE LION", listOf("Street", "Urban"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B4A0,
            0xFF00B4A0, 0xFF00B4A0, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            128, "benelli_752s", "Benelli", "752S", "2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, true, true, "752 // S STREETFIGHTER", listOf("Street", "Sport"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B4A0,
            0xFF00B4A0, 0xFF00B4A0, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            129, "benelli_imperiale_400", "Benelli", "Imperiale 400", "2019",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            5500.0, 7000.0, false, false, "IMPERIALE // CLASSIC", listOf("Clasic", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF00B4A0,
            0xFF00B4A0, 0xFF00B4A0, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            130, "benelli_502c", "Benelli", "502C", "2021",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, true, "502 // C CUSTOM CRUISER", listOf("Street", "Urban"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B4A0,
            0xFF00B4A0, 0xFF00B4A0, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            131, "benelli_leoncino_125", "Benelli", "Leoncino 125", "2017",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, true, "LEONCINO // 125", listOf("Street", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFF00B4A0, 0xFF00B4A0,
            0xFF00B4A0, 0xFF00B4A0, 0xFF06080D
        ),

        // =====================================================================
        // GRUP 19: HONDA (12 Model: #140 - #151)
        // =====================================================================
        DeclarativeGaugeSpec(
            132, "honda_cb350", "Honda", "CB350 H'ness", "2023",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 8500.0, false, false, "CLASSIC // H'NESS 350", listOf("Classic", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            133, "honda_cb500f", "Honda", "CB500F", "2016",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, true, true, "HONDA // CB500F SPORT", listOf("Street", "Sport"),
            0xFFF5F5F5, 0xFF0A0A0A, 0xFF060608, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            134, "honda_cb500x", "Honda", "CB500X", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, false, true, "CB500X // ADVENTURE", listOf("Adventure", "Touring"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            135, "honda_cb125r", "Honda", "CB125R", "2020",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            11000.0, 12500.0, true, true, "CB // 125 SPORT", listOf("Sport", "Street"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            136, "honda_cb300r", "Honda", "CB300R", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, false, true, "CB // 300 NEO", listOf("Street", "Sport"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFF07080A
        ),
        DeclarativeGaugeSpec(
            137, "honda_nc750x", "Honda", "NC750X", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 8500.0, false, true, "NC // 750X DCT", listOf("Touring", "Urban"),
            0xFFF5F5F5, 0xFF101218, 0xFF080A0F, 0xFFF4F4F6, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            138, "honda_rebel_500", "Honda", "Rebel 500", "2017",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, true, "REBEL // 500 CRUISER", listOf("Cruiser", "Street"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            139, "honda_cbr500r", "Honda", "CBR500R", "2016",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, true, true, "CBR // 500R SPORT", listOf("Sport", "Track"),
            0xFFF5F5F5, 0xFF0A0A0A, 0xFF060608, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            140, "honda_cbr300r", "Honda", "CBR300R", "2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            10500.0, 12000.0, true, true, "CBR // 300R SPORT", listOf("Sport", "Street"),
            0xFFF5F5F5, 0xFF0A0A0A, 0xFF060608, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            141, "honda_crf450r", "Honda", "CRF450R", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12000.0, false, true, "CRF // 450 RACE", listOf("Motocross", "Race"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            142, "honda_crf300l", "Honda", "CRF300L", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 10500.0, false, true, "CRF // 300 DUAL SPORT", listOf("Trail", "Urban"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            143, "honda_crf250l", "Honda", "CRF250L", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 10500.0, false, true, "CRF // 250 DUAL SPORT", listOf("Trail", "Urban"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 20: YAMAHA (14 Model: #152 - #165)
        // =====================================================================
        DeclarativeGaugeSpec(
            144, "yamaha_mt03", "Yamaha", "MT-03", "2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12500.0, 14000.0, true, true, "CP3 // DARK SIDE OF JAPAN", listOf("Street", "Track"),
            0xFFF0F4F8, 0xFF0B0E14, 0xFF06080D, 0xFFF5F7FA, 0xFF00B0FF,
            0xFFFF9100, 0xFFFF1744, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            145, "yamaha_mt15", "Yamaha", "MT-15", "2022",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, true, true, "MT // STREET WARRIOR", listOf("Street", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFF00B0FF, 0xFF00B0FF,
            0xFF00B0FF, 0xFFFFFFFF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            146, "yamaha_xsr125", "Yamaha", "XSR125", "2021",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 12500.0, true, true, "XSR // HERITAGE RACER", listOf("Street", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFE6E6E6, 0xFF00B0FF,
            0xFF00B0FF, 0xFFFFFFFF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            147, "yamaha_xsr700", "Yamaha", "XSR700", "2016",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 11500.0, true, true, "XSR // HERITAGE RACER", listOf("Street", "Sport"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B0FF,
            0xFFFF9100, 0xFF00B0FF, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            148, "yamaha_xsr900", "Yamaha", "XSR900", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 12500.0, true, true, "XSR // CP3 HERITAGE", listOf("Street", "Sport"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B0FF,
            0xFFFF9100, 0xFF00B0FF, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            149, "yamaha_tracer_7", "Yamaha", "Tracer 7", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 11500.0, false, true, "TRACER // SPORT TOURING", listOf("Touring", "Sport"),
            0xFFF5F5F5, 0xFF101218, 0xFF080A0F, 0xFFF4F4F6, 0xFF00B0FF,
            0xFFFF9100, 0xFF00B0FF, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            150, "yamaha_tracer_9_gt", "Yamaha", "Tracer 9 GT", "2021",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 12500.0, true, true, "TRACER // SPORT TOURING GT", listOf("Touring", "Sport"),
            0xFFF5F5F5, 0xFF101218, 0xFF080A0F, 0xFFF4F4F6, 0xFF00B0FF,
            0xFFFF9100, 0xFF00B0FF, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            151, "yamaha_yz450f", "Yamaha", "YZ450F", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12000.0, false, true, "YZ // 450F MOTOCROSS", listOf("Motocross", "Race"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B0FF,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            152, "yamaha_wr450f", "Yamaha", "WR450F", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12000.0, false, true, "WR // 450F ENDURO", listOf("Enduro", "Rally"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B0FF,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            153, "yamaha_bolt_rspec", "Yamaha", "Bolt R-Spec", "2017",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6500.0, 8000.0, false, false, "BOLT // MUSCLE CRUISER", listOf("Cruiser", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF00B0FF,
            0xFF00B0FF, 0xFFFF6A00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            154, "yamaha_xjr1300", "Yamaha", "XJR1300", "2016",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, false, "XJR // 1300 RETRO", listOf("Retro", "Street"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF00B0FF,
            0xFF00B0FF, 0xFFFF6A00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            155, "yamaha_sr400", "Yamaha", "SR400", "2014",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6500.0, 8000.0, false, false, "SR // 400 HERITAGE", listOf("Retro", "Classic"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF00B0FF,
            0xFF00B0FF, 0xFFFF6A00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            156, "yamaha_yz250f", "Yamaha", "YZ250F", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            13500.0, 14500.0, false, true, "YZ // 250F MOTOCROSS", listOf("Motocross", "Race"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B0FF,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            157, "yamaha_tenere_700_xtreme", "Yamaha", "Ténéré 700 Extreme", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, false, true, "TENERE // 700 EXTREME", listOf("Rally", "Adventure"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B0FF,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 21: KAWASAKI (12 Model: #166 - #177)
        // =====================================================================
        DeclarativeGaugeSpec(
            158, "kawasaki_ninja_125", "Kawasaki", "Ninja 125", "2020",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 12500.0, true, true, "NINJA // 125 SPORT", listOf("Sport", "Street"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            159, "kawasaki_vulcan_s", "Kawasaki", "Vulcan S", "2017",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, false, "VULCAN // S CRUISER", listOf("Cruiser", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF64DD17,
            0xFF64DD17, 0xFFFF9100, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            160, "kawasaki_eliminator", "Kawasaki", "Eliminator", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 11500.0, false, true, "ELIMINATOR // MODERN CRUISER", listOf("Cruiser", "Urban"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            161, "kawasaki_versys_650", "Kawasaki", "Versys 650", "2016",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, false, true, "VERSYS // 650 ADVENTURE", listOf("Touring", "Adventure"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            162, "kawasaki_klx230", "Kawasaki", "KLX230", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, true, "KLX // 230 DUAL SPORT", listOf("Trail", "Urban"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            163, "kawasaki_kx450", "Kawasaki", "KX450", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12000.0, false, true, "KX // 450 MOTOCROSS", listOf("Motocross", "Race"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            164, "kawasaki_z400", "Kawasaki", "Z400", "2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11750.0, 13000.0, true, true, "Z // SUGOMI 400", listOf("Street", "Sport"),
            0xFFF5F5F5, 0xFF0B0E0B, 0xFF060906, 0xFFF5F5F5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            165, "kawasaki_z650_rs", "Kawasaki", "Z650 RS", "2022",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, true, true, "Z // 650RS RETRO", listOf("Retro", "Sport"),
            0xFFF5F5F5, 0xFF0B0E0B, 0xFF060906, 0xFFF5F5F5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            166, "kawasaki_ninja_250", "Kawasaki", "Ninja 250", "2018",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            13500.0, 14500.0, true, true, "NINJA // 250 SPORT", listOf("Sport", "Street"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            167, "kawasaki_versys_x300", "Kawasaki", "Versys-X 300", "2017",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12000.0, 13000.0, false, true, "VERSYS // X300 ADVENTURE", listOf("Adventure", "Trail"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            168, "kawasaki_ninja_1000sx", "Kawasaki", "Ninja 1000SX", "2021",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12500.0, 14000.0, true, true, "NINJA // 1000SX SPORT TOURER", listOf("Sport Touring", "Sport"),
            0xFFF5F5F5, 0xFF0B0E0B, 0xFF060906, 0xFFF5F5F5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            169, "kawasaki_versys_1000se", "Kawasaki", "Versys 1000 SE", "2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, false, true, "VERSYS // 1000 SE GRACE", listOf("Touring", "Adventure"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 22: SUZUKI (6 Model: #178 - #183)
        // =====================================================================
        DeclarativeGaugeSpec(
            170, "suzuki_vstrom_650", "Suzuki", "V-Strom 650", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, false, true, "V-STROM // MASTER OF ADVENTURE", listOf("Adventure", "Touring"),
            0xFFF5F5F5, 0xFF0D1016, 0xFF07090D, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            171, "suzuki_vstrom_800de", "Suzuki", "V-Strom 800DE", "2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, false, true, "V-STROM // 800 DE DARE", listOf("Adventure", "Rally"),
            0xFFF5F5F5, 0xFF0D1016, 0xFF07090D, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            172, "suzuki_drz400s", "Suzuki", "DR-Z400S", "2000",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, false, "DR // Z400S LEGEND", listOf("Enduro", "Dual Sport"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF0091EA,
            0xFF0091EA, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            173, "suzuki_gsx250r", "Suzuki", "GSX250R", "2014",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, true, true, "GSX // 250R SPORT", listOf("Sport", "Street"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFF00B0FF, 0xFF0091EA,
            0xFFFFAB00, 0xFF0091EA, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            174, "suzuki_katana_gsxs1000", "Suzuki", "Katana GSX-S1000", "2019",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            13000.0, 14500.0, true, true, "KATANA // GSX-S1000", listOf("Street", "Sport"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFFAB00, 0xFF0091EA, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            175, "suzuki_sv650", "Suzuki", "SV650", "2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, true, true, "SV // 650 GLADIUS", listOf("Sport", "Street"),
            0xFFF5F5F5, 0xFF0D1016, 0xFF07090D, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFF9100, 0xFF0091EA, 0xFF07090D
        ),

        // =====================================================================
        // GRUP 23: BMW MOTORRAD (8 Model: #184 - #191)
        // =====================================================================
        DeclarativeGaugeSpec(
            176, "bmw_f850gs", "BMW Motorrad", "F850GS", "2018",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 9500.0, false, true, "GS // SPIRIT OF ADVENTURE", listOf("Offroad", "Touring"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            177, "bmw_f900gs", "BMW Motorrad", "F900GS", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, true, "GS // 900 GRANITE", listOf("Offroad", "Touring"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            178, "bmw_r1200gs", "BMW Motorrad", "R 1200 GS", "2014–2018",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 9500.0, false, true, "GS // SPIRIT OF ADVENTURE", listOf("Offroad", "Touring"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            179, "bmw_g310rr", "BMW Motorrad", "G310RR", "2021",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12000.0, true, true, "RR // SPORT", listOf("Sport", "Track"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            180, "bmw_m1000xr", "BMW Motorrad", "M 1000 XR", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            14550.0, 16000.0, true, true, "M // 1000 XR M COMPETITION", listOf("Pure Ride", "Core 1", "Core 2", "Core 3"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFF0099FF, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            181, "bmw_r1300gs_adventure", "BMW Motorrad", "R 1300 GS Adventure", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 9500.0, false, true, "GS // RALLY ADVENTURE", listOf("Offroad", "Touring"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            182, "bmw_r1250rt", "BMW Motorrad", "R 1250 RT", "2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, true, "RT // SPORT TOURING", listOf("Touring", "Pure Ride"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            183, "bmw_k1600gt", "BMW Motorrad", "K 1600 GT", "2011",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, true, "K // 1600 GRAND TOURER", listOf("Touring", "Pure Ride"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 24: DUCATI (9 Model: #192 - #200)
        // =====================================================================
        DeclarativeGaugeSpec(
            184, "ducati_monster_821", "Ducati", "Monster 821", "2018",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12000.0, true, true, "MONSTER // IL MOSTRO", listOf("Street", "Sport"),
            0xFFF5F5F5, 0xFF0B0B0E, 0xFF070709, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            185, "ducati_monster_1200", "Ducati", "Monster 1200", "2017",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12000.0, true, true, "MONSTER // IL MOSTRO", listOf("Street", "Sport"),
            0xFFF5F5F5, 0xFF0B0B0E, 0xFF070709, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            186, "ducati_scrambler_1100", "Ducati", "Scrambler 1100", "2018",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 9000.0, false, true, "SCRAMBLER // 1100 LAND OF JOY", listOf("Classic", "Street"),
            0xFFF5F5F5, 0xFF101014, 0xFF08080B, 0xFFFFE4B5, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            187, "ducati_streetfighter_v2", "Ducati", "Streetfighter V2", "2022",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "STREETFIGHTER // V2 FIGHT FORMULA", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF0B0B0E, 0xFF060608, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            188, "ducati_panigale_899", "Ducati", "Panigale 899", "2014",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            11500.0, 13000.0, true, true, "PANIGALE // 899 SUPERQUADRO", listOf("Track", "Road"),
            0xFF0A0A0E, 0xFFFFFFFF, 0xFF050508, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            189, "ducati_desertx", "Ducati", "DesertX", "2022",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, false, true, "DESERTX // RALLY ADVENTURE", listOf("Rally", "Enduro"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            190, "ducati_supersport_950", "Ducati", "Supersport 950", "2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            10500.0, 11500.0, true, true, "SUPERSPORT // 950 GP-STYLE", listOf("Sport", "Track"),
            0xFFF5F5F5, 0xFF0B0B0E, 0xFF060608, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            191, "ducati_multistrada_v2", "Ducati", "Multistrada V2", "2022",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, false, true, "MULTISTRADA // V2 GRANTURISMO", listOf("Touring", "Sport"),
            0xFFF5F5F5, 0xFF0C0C10, 0xFF07070A, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            192, "ducati_monster_696", "Ducati", "Monster 696", "2008",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, true, "MONSTER // 696 ALESSANDRO", listOf("Street", "Sport"),
            0xFFF5F5F5, 0xFF101014, 0xFF08080B, 0xFFFFE4B5, 0xFFFF0000,
            0xFFFF8800, 0xFFFF0000, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 25: APRILIA (3 Model: #201 - #203)
        // =====================================================================
        DeclarativeGaugeSpec(
            193, "aprilia_rs125", "Aprilia", "RS 125", "2020",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            11500.0, 12500.0, true, true, "RS // 125 BE A RACER", listOf("Sport", "Track"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFF1E1E, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            194, "aprilia_dorsoduro_900", "Aprilia", "Dorsoduro 900", "2018",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, false, true, "DORSODURO // 900 SUPER MOTO", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            195, "aprilia_dorsoduro_1200", "Aprilia", "Dorsoduro 1200", "2013",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
            10750.0, 12500.0, true, true, "DORSODURO // 1200 SUPER MOTO", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFFF1E1E,
            0xFFFFAB00, 0xFFFF1E1E, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 26: TRIUMPH (7 Model: #204 - #210)
        // =====================================================================
        DeclarativeGaugeSpec(
            196, "triumph_speed_400", "Triumph", "Speed 400", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            10000.0, 11000.0, false, true, "SPEED // 400 MODERN CLASSIC", listOf("Street", "Sport"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            197, "triumph_scrambler_400x", "Triumph", "Scrambler 400X", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, false, true, "SCRAMBLER // 400X ADVENTURE", listOf("Adventure", "Urban"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            198, "triumph_trident_660", "Triumph", "Trident 660", "2021",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12000.0, 13000.0, true, true, "TRIDENT // 660 MODERN CLASSIC", listOf("Street", "Sport"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            199, "triumph_rocket_3r", "Triumph", "Rocket 3 R", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7000.0, 8500.0, false, true, "ROCKET // 3R ULTIMATE", listOf("Cruiser", "Sport"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            200, "triumph_tiger_850_sport", "Triumph", "Tiger 850 Sport", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, false, true, "TIGER // 850 SPORT", listOf("Adventure", "Touring"),
            0xFFF4F5F7, 0xFF0E1014, 0xFF07080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            201, "triumph_bonneville_t100", "Triumph", "Bonneville T100", "2017",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6000.0, 7500.0, false, false, "BONNEVILLE // T100 ICON", listOf("Classic", "Urban"),
            0xFF0D0F14, 0xFFF5F0E6, 0xFF08090C, 0xFFFFE4B5, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            202, "triumph_street_twin", "Triumph", "Street Twin", "2016",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7000.0, 8000.0, false, false, "BONNEVILLE // STREET TWIN", listOf("Classic", "Street"),
            0xFF0D0F14, 0xFFF5F0E6, 0xFF08090C, 0xFFFFE4B5, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 27: MV AGUSTA (3 Model: #211 - #213)
        // =====================================================================
        DeclarativeGaugeSpec(
            203, "mv_agusta_brutale_800rr", "MV Agusta", "Brutale 800 RR", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            14000.0, 15500.0, true, true, "BRUTALE // 800RR TRIPLE", listOf("Street", "Track"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF06070A, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            204, "mv_agusta_f3_800", "MV Agusta", "F3 800", "2012",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            13500.0, 15000.0, true, true, "F3 // 800 TRIPLE", listOf("Track", "Road"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF06070A, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            205, "mv_agusta_rush_1000", "MV Agusta", "Rush 1000", "2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            14000.0, 15500.0, true, true, "RUSH // 1000 LIMITED", listOf("Track", "Street"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF06070A, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 28: INDIAN MOTORCYCLE (6 Model: #214 - #219)
        // =====================================================================
        DeclarativeGaugeSpec(
            206, "indian_ftr_1200", "Indian Motorcycle", "FTR 1200", "2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, false, true, "FTR // 1200 FLAT TRACK", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFFC9A227,
            0xFFC9A227, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            207, "indian_ftr_1200_s", "Indian Motorcycle", "FTR 1200 S", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11000.0, false, true, "FTR // 1200 S FLAT TRACK", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF101010, 0xFF07080A, 0xFFF5F5F5, 0xFFC9A227,
            0xFFC9A227, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            208, "indian_scout_1200", "Indian Motorcycle", "Scout 1200", "2015",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, false, "SCOUT // 1200 CRUISER", listOf("Cruiser", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFC9A227,
            0xFFC9A227, 0xFFFF9100, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            209, "indian_scout_bobber", "Indian Motorcycle", "Scout Bobber", "2018",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10500.0, false, false, "SCOUT // BOBBER CRUISER", listOf("Cruiser", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFC9A227,
            0xFFC9A227, 0xFFFF9100, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            210, "indian_challenger_1800", "Indian Motorcycle", "Challenger 1800", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6500.0, 7500.0, false, true, "CHALLENGER // 1800 TOURING", listOf("Touring", "Cruiser"),
            0xFFF4F6FA, 0xFF101218, 0xFF080A0F, 0xFFF4F4F6, 0xFFC9A227,
            0xFFC9A227, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            211, "indian_pursuit_1800", "Indian Motorcycle", "Pursuit 1800", "2022",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6500.0, 7500.0, false, true, "PURSUIT // 1800 TOURING", listOf("Touring", "Cruiser"),
            0xFFF4F6FA, 0xFF101218, 0xFF080A0F, 0xFFF4F4F6, 0xFFC9A227,
            0xFFC9A227, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 29: ROYAL ENFIELD (6 Model: #220 - #225)
        // =====================================================================
        DeclarativeGaugeSpec(
            212, "royal_enfield_meteor_350", "Royal Enfield", "Meteor 350", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 8500.0, false, true, "METEOR // 350 MODERN CRUISER", listOf("Cruiser", "Urban"),
            0xFFF5F0E6, 0xFF12151C, 0xFF08090C, 0xFFFFE4B5, 0xFFFFAB00,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            213, "royal_enfield_hunter_350", "Royal Enfield", "Hunter 350", "2022",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 8500.0, false, true, "HUNTER // 350 STREET", listOf("Street", "Urban"),
            0xFFF5F0E6, 0xFF12151C, 0xFF08090C, 0xFFFFE4B5, 0xFFFFAB00,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            214, "royal_enfield_classic_350", "Royal Enfield", "Classic 350", "2021",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 8500.0, false, false, "CLASSIC // 350 HERITAGE", listOf("Classic", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFFFAB00,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            215, "royal_enfield_super_meteor_650", "Royal Enfield", "Super Meteor 650", "2023",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 10000.0, false, false, "SUPER METEOR // 650 CRUISER", listOf("Cruiser", "Touring"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFFFAB00,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            216, "royal_enfield_bullet_350", "Royal Enfield", "Bullet 350", "2022",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 8500.0, false, false, "BULLET // 350 ICONIC", listOf("Classic", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFFFAB00,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            217, "royal_enfield_scramble_411", "Royal Enfield", "Scram 411", "2022",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6000.0, 8000.0, false, true, "SCRAM // 411 RALLY", listOf("Adventure", "Trail"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFFAB00,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 30: HARLEY-DAVIDSON (6 Model: #226 - #231)
        // =====================================================================
        DeclarativeGaugeSpec(
            218, "harley_street_glide", "Harley-Davidson", "Street Glide", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            5500.0, 6500.0, false, true, "STREET GLIDE // FLH 2024", listOf("Touring", "Cruising"),
            0xFF0A0B0E, 0xFFFFFFFF, 0xFF050608, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF6600, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            219, "harley_road_glide", "Harley-Davidson", "Road Glide", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            5500.0, 6500.0, false, true, "ROAD GLIDE // 117", listOf("Touring", "Cruising"),
            0xFF0A0B0E, 0xFFFFFFFF, 0xFF050608, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF6600, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            220, "harley_pan_america_1250", "Harley-Davidson", "Pan America 1250", "2021",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 11500.0, false, true, "PAN AMERICA // 1250 ADVENTURE", listOf("Adventure", "Touring"),
            0xFFF4F6FA, 0xFF0B0E14, 0xFF06080E, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            221, "harley_softail_street_bob", "Harley-Davidson", "Softail Street Bob", "2018",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6000.0, 7000.0, false, false, "SOFTAIL // STREET BOB", listOf("Cruiser", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFFF6600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            222, "harley_fat_boy", "Harley-Davidson", "Fat Boy", "2018",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6000.0, 7000.0, false, false, "FAT BOY // 115", listOf("Cruiser", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFFF6600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            223, "harley_cvo_street_glide", "Harley-Davidson", "CVO Street Glide", "2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            5500.0, 6500.0, false, true, "CVO // 117 STREET GLIDE", listOf("Touring", "Cruising"),
            0xFF0A0B0E, 0xFFFFFFFF, 0xFF050608, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF6600, 0xFFFF1744, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 31: KTM (10 Model: #232 - #241)
        // =====================================================================
        DeclarativeGaugeSpec(
            224, "ktm_390_adventure", "KTM", "390 Adventure", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10200.0, 12000.0, false, true, "ADVENTURE // 390 READY TO EXPLORE", listOf("Adventure", "Street"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            225, "ktm_rc_200", "KTM", "RC 200", "2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            11000.0, 12500.0, false, true, "RC // 200 GP", listOf("Track", "Sport"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFF7700, 0xFFFF6600,
            0xFFFF5500, 0xFFFF7700, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            226, "ktm_rc_125", "KTM", "RC 125", "2024",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            11500.0, 13000.0, false, true, "RC // 125 GP", listOf("Track", "Sport"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFF7700, 0xFFFF6600,
            0xFFFF5500, 0xFFFF7700, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            227, "ktm_790_adventure", "KTM", "790 Adventure", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11500.0, false, true, "ADVENTURE // 790 EXPEDITION", listOf("Adventure", "Offroad"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            228, "ktm_890_adventure", "KTM", "890 Adventure", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11500.0, false, true, "ADVENTURE // 890 EXPEDITION", listOf("Adventure", "Offroad"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            229, "ktm_250_adventure", "KTM", "250 Adventure", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11500.0, false, true, "ADVENTURE // 250 EXPEDITION", listOf("Adventure", "Street"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            230, "ktm_125_duke", "KTM", "125 Duke", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, true, true, "READY TO RACE // 125 DUKE", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF0A0A0A, 0xFF060608, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF6600, 0xFFFF2200, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            231, "ktm_exc_450", "KTM", "EXC 450", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, false, true, "EXC // 450 RALLY RAID", listOf("Rally", "Enduro"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            232, "ktm_690_duke", "KTM", "690 Duke", "2016",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, true, true, "READY TO RACE // 690 DUKE", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF0A0A0A, 0xFF060608, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF6600, 0xFFFF2200, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            233, "ktm_1190_adventure", "KTM", "1190 Adventure", "2013",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, false, true, "ADVENTURE // 1190 EXPEDITION", listOf("Adventure", "Touring"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFF6600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 32: HUSQVARNA (5 Model: #242 - #246)
        // =====================================================================
        DeclarativeGaugeSpec(
            234, "husqvarna_fe_450", "Husqvarna", "FE 450", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11000.0, 12500.0, false, true, "FE // 450 ENDURO", listOf("Enduro", "Rally"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFFEA00,
            0xFFFFEA00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            235, "husqvarna_701_enduro", "Husqvarna", "701 Enduro", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 9000.0, false, true, "701 // ENDURO BUILT TO EXCITE", listOf("Enduro", "Adventure"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFFFEA00,
            0xFFFFEA00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            236, "husqvarna_vitpilen_801", "Husqvarna", "Vitpilen 801", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 11500.0, true, true, "VITPILEN // 801 BAUHAUS", listOf("Street", "Track"),
            0xFFF5F5F5, 0xFF0C0E12, 0xFF08090C, 0xFFFFFFFF, 0xFFFFEA00,
            0xFFFFEA00, 0xFFFFEA00, 0xFF08090C
        ),
        DeclarativeGaugeSpec(
            237, "husqvarna_fc_450", "Husqvarna", "FC 450", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, false, true, "FC // 450 MOTOCROSS", listOf("Motocross", "Race"),
            0xFFF5F5F5, 0xFF0C0E12, 0xFF08090C, 0xFFFFFFFF, 0xFFFFEA00,
            0xFFFFEA00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            238, "husqvarna_701_supermoto", "Husqvarna", "701 Supermoto", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 9000.0, true, true, "701 // SUPERMOTO STREET FIGHT", listOf("Supermoto", "Track"),
            0xFFF5F5F5, 0xFF0C0E12, 0xFF08090C, 0xFFFFFFFF, 0xFFFFEA00,
            0xFFFFEA00, 0xFFFFEA00, 0xFF08090C
        ),

        // =====================================================================
        // GRUP 33: CFMOTO (2 Model: #247 - #248)
        // =====================================================================
        DeclarativeGaugeSpec(
            239, "cfmoto_450nk", "CFMoto", "450NK", "2023",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "CFMOTO // 450NK STREET", listOf("Street", "Sport"),
            0xFFF2F5F8, 0xFF0C1018, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            240, "cfmoto_450ss", "CFMoto", "450SS", "2024",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "CFMOTO // 450SS SPORT", listOf("Sport", "Track"),
            0xFFF2F5F8, 0xFF0C1018, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 34: MOTO GUZZI (3 Model: #249 - #251)
        // =====================================================================
        DeclarativeGaugeSpec(
            241, "moto_guzzi_v85_tt", "Moto Guzzi", "V85 TT", "2019",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 9000.0, false, true, "V85 // TT ADVENTURE", listOf("Adventure", "Touring"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            242, "moto_guzzi_v7_stone", "Moto Guzzi", "V7 Stone", "2018",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7000.0, 8000.0, false, false, "V7 // STONE CLASSIC", listOf("Classic", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFE60012,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            243, "moto_guzzi_v9_bobber", "Moto Guzzi", "V9 Bobber", "2016",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 8500.0, false, false, "V9 // BOBBER", listOf("Cruiser", "Urban"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFE60012,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),

        // =====================================================================
        // GRUP 35: MISC / RETRO LEGENDS (13 Model: #252 - #264)
        // =====================================================================
        DeclarativeGaugeSpec(
            244, "bmw_r18", "BMW Motorrad", "R 18", "2020",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            5000.0, 6000.0, false, false, "R // 18 MIGHTY BOXER", listOf("Cruiser", "Touring"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            245, "honda_cb400_super_four", "Honda", "CB400 Super Four", "1999",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12500.0, 15000.0, false, false, "SUPER FOUR // CB400", listOf("Street", "Sport"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            246, "yamaha_fjr1300", "Yamaha", "FJR1300", "2001",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9000.0, 11000.0, false, true, "FJR // 1300 GRAND TOURER", listOf("Touring", "Sport"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B0FF,
            0xFFFF9100, 0xFF00B0FF, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            247, "kawasaki_z900rs", "Kawasaki", "Z900RS", "2018",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, false, "Z // 900RS RETRO", listOf("Retro", "Sport"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            248, "yamaha_mt25", "Yamaha", "MT-25", "2020",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            13000.0, 14500.0, true, true, "MT // 25 STREET WARRIOR", listOf("Street", "Urban"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFF00B0FF, 0xFF00B0FF,
            0xFF00B0FF, 0xFFFFFFFF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            249, "yamaha_r15v4", "Yamaha", "YZF-R15 v4", "2021",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            13000.0, 14500.0, true, true, "R // 15 VVA MONSTER", listOf("Sport", "Track"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFF00B0FF, 0xFF00B0FF,
            0xFF00B0FF, 0xFFFFFFFF, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            250, "honda_xr650l", "Honda", "XR650L", "2000",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6000.0, 8000.0, false, true, "XR // 650L LEGEND", listOf("Trail", "Dual Sport"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            251, "honda_cbr1000f", "Honda", "CBR1000F", "1987",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 14000.0, false, false, "CBR // 1000F THE ORIGINAL", listOf("Retro", "Sport"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            252, "suzuki_gixxer_sf250", "Suzuki", "Gixxer SF 250", "2020",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            11500.0, 13000.0, true, true, "GIXXER // SF 250 SPORT", listOf("Sport", "Street"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFF00B0FF, 0xFF0091EA,
            0xFFFFAB00, 0xFF0091EA, 0xFF06080D
        ),
        DeclarativeGaugeSpec(
            253, "suzuki_vstrom_250", "Suzuki", "V-Strom 250", "2017",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, true, "V-STROM // 250 ADVENTURE", listOf("Adventure", "Urban"),
            0xFFF5F5F5, 0xFF0D1016, 0xFF07090D, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            254, "yamaha_xt500", "Yamaha", "XT500", "1976",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7000.0, 9000.0, false, false, "XT // 500 LEGEND", listOf("Enduro", "Classic"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF00B0FF,
            0xFFFF9100, 0xFF00B0FF, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            255, "honda_cb750_four", "Honda", "CB750 Four", "1969",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 10500.0, false, false, "CB // 750 FOUR THE ORIGINAL", listOf("Retro", "Classic"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            256, "kawasaki_zephyr_750", "Kawasaki", "Zephyr 750", "1991",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 12500.0, false, false, "ZEPHYR // 750 RETRO", listOf("Retro", "Sport"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            257, "ducati_superleggera_v4", "Ducati", "Superleggera V4", "2020",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.MOTOGP_MARSHAL_BAR,
            15000.0, 16500.0, true, true, "SUPERLEGGERA // V4 THE ULTIMATE", listOf("Track", "Race"),
            0xFF050508, 0xFFFFFFFF, 0xFF040406, 0xFFFFFFFF, 0xFFFF0000,
            0xFFFFD600, 0xFFFF0000, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            258, "mv_agusta_f4_750_oro", "MV Agusta", "F4 750 Oro", "1999",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            13500.0, 14500.0, false, true, "F4 // 750 ORO MASTERPIECE", listOf("Street", "Track"),
            0xFF0B0D12, 0xFFFFFFFF, 0xFF06070A, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            259, "kawasaki_zx12r", "Kawasaki", "Ninja ZX-12R", "2000",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, false, true, "NINJA // ZX-12R THUNDER", listOf("Retro", "Sport"),
            0xFF0C0D10, 0xFFF5E6C8, 0xFF07080A, 0xFFFFE4B5, 0xFF64DD17,
            0xFFFFB300, 0xFF64DD17, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            260, "triumph_trophy_1200", "Triumph", "Trophy 1200", "2002",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 9500.0, false, true, "TROPHY // 1200 GRAND TOURER", listOf("Touring", "Cruiser"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            261, "honda_africa_twin_750", "Honda", "Africa Twin 750", "1993",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 9500.0, false, true, "AFRICA TWIN // 750 LEGEND", listOf("Rally", "Adventure"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            262, "suzuki_tl1000r", "Suzuki", "TL1000R", "1998",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            11500.0, 13500.0, true, true, "TL // 1000R THE MONSTER", listOf("Sport", "Track"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFFAB00, 0xFF0091EA, 0xFF06080D
        ),

        // =====================================================================
        // GRUP 27: GLOBAL FLAGSHIPS, DÜNYA MARKALARI & 2025/2026 YENİ NESİL (45 Model)
        // =====================================================================
        DeclarativeGaugeSpec(
            263, "honda_gold_wing_tour", "Honda", "Gold Wing Tour 1800 DCT (7\" Cockpit + Twin Dials)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            6000.0, 7500.0, false, false, "GOLD WING // TOUR DCT", listOf("Touring Cockpit", "Navigation"),
            0xFF10131C, 0xFFFFFFFF, 0xFF07090E, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            264, "honda_nx500", "Honda", "NX500 (5\" Bonded RoadSync TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, true, "HONDA // NX500", listOf("Circle", "Bar", "Simple"),
            0xFFF4F6F9, 0xFF0D1016, 0xFF07090E, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            265, "honda_cb1000_hornet_sp", "Honda", "CB1000 Hornet SP (5\" Optical TFT)", "2025+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            11500.0, 13000.0, true, true, "HORNET // 1000 SP", listOf("Sport", "Track", "Street"),
            0xFFF2F4F8, 0xFF0B0E14, 0xFF06080D, 0xFFFFFFFF, 0xFFFFAB00,
            0xFFFF6D00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            266, "honda_vfr800f", "Honda", "VFR800F Interceptor (Center Analog + Dual Blue LCD)", "2014–2020",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11750.0, 13500.0, false, false, "HONDA // VFR800F V4 VTEC", listOf("V4 Touring Analog"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFE8F4FF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            267, "yamaha_yzf_r9", "Yamaha", "YZF-R9 (5\" CP3 YRC Track TFT)", "2025+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.BOOMERANG_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            10500.0, 12000.0, true, true, "YAMAHA // YZF-R9", listOf("Theme 1 Track", "Theme 2 Street", "Theme 3 Minimal"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF0055FF,
            0xFFFFAB00, 0xFF0055FF, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            268, "yamaha_tenere_700_2025", "Yamaha", "Ténéré 700 (6.3\" Vertical Rally TFT)", "2025+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11500.0, false, true, "TÉNÉRÉ 700 // 2025", listOf("Street", "Explorer", "Raid"),
            0xFFF5F5F5, 0xFF12151C, 0xFF080A0F, 0xFFF5F5F5, 0xFF00B0FF,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            269, "yamaha_vmax_1700", "Yamaha", "VMAX 1700 (Analog Tach + Tank OLED Console)", "2009–2020",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            9500.0, 11000.0, false, true, "YAMAHA // VMAX V4 1700", listOf("Muscle V4"),
            0xFF090A0D, 0xFFFFFFFF, 0xFF050608, 0xFFFFFFFF, 0xFFFF3D00,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            270, "kawasaki_klr_650", "Kawasaki", "KLR 650 (Adventure Negative LCD)", "2022+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 9000.0, false, false, "KAWASAKI // KLR 650", listOf("Adventure LCD"),
            0xFF0B0E0B, 0xFFF2F7F2, 0xFF060906, 0xFFF2F7F2, 0xFF64DD17,
            0xFFFFAB00, 0xFF64DD17, 0xFF060906
        ),
        DeclarativeGaugeSpec(
            271, "kawasaki_concours_14", "Kawasaki", "Concours 14 / 1400GTR (Twin Analog + LCD)", "2015–2022",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, false, false, "KAWASAKI // 1400GTR", listOf("Sport Tourer"),
            0xFF0C0F14, 0xFFFFFFFF, 0xFF07090D, 0xFFE0F2F1, 0xFF64DD17,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            272, "kawasaki_zx14r", "Kawasaki", "Ninja ZX-14R (Dual Analog + Dot Matrix LCD)", "2012–2023",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.SINGLE_UPPER_LED,
            11000.0, 12500.0, false, true, "NINJA // ZX-14R", listOf("Hyperbike Dual Analog"),
            0xFF0B0D10, 0xFFFFFFFF, 0xFF06070A, 0xFFFFFFFF, 0xFF64DD17,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            273, "suzuki_gsx_s1000gx", "Suzuki", "GSX-S1000GX+ (6.5\" SAES Crossover TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, true, true, "SUZUKI // GSX-S1000GX", listOf("Active", "Touring"),
            0xFFF2F5FA, 0xFF0D111A, 0xFF060910, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFFAB00, 0xFF0091EA, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            274, "suzuki_dr_z4s", "Suzuki", "DR-Z4S (Enduro Inverted LCD)", "2025+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, true, "SUZUKI // DR-Z4S", listOf("Dual-Sport LCD"),
            0xFF0B0E14, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFFFD600,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            275, "suzuki_intruder_m1800r", "Suzuki", "Intruder M1800R / M109R (Cowl Tach + Tank Speedo)", "2014–2024",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            7500.0, 9000.0, false, false, "SUZUKI // BOULEVARD M109R", listOf("Power Cruiser"),
            0xFF0C0E14, 0xFFFFFFFF, 0xFF07080C, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFFFAB00, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            276, "bmw_k1300r", "BMW Motorrad", "K 1300 R (Asymmetric HP Analog + LCD)", "2009–2016",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            11000.0, 12500.0, false, true, "BMW // K1300R HP", listOf("HP Analog"),
            0xFFF0F2F6, 0xFF0D1016, 0xFF0A0D12, 0xFFFFFFFF, 0xFF0066B1,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            277, "bmw_r12_ninet", "BMW Motorrad", "R 12 nineT (Retro Dual Micro-Dial / TFT)", "2024+",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.DUAL_CLASSIC_ANALOG, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, true, "BMW // R 12 nineT", listOf("Heritage Analog", "Micro TFT"),
            0xFF0D0E12, 0xFFF5F5F5, 0xFF08090C, 0xFFFFE0B2, 0xFF0066B1,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            278, "ducati_panigale_v4_2025", "Ducati", "Panigale V4 S (6.9\" 8:3 Wide Bonded Track TFT)", "2025+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            15000.0, 16500.0, true, true, "PANIGALE V4 // 2025", listOf("Track", "Road", "Info"),
            0xFFF6F6F6, 0xFF101012, 0xFF080809, 0xFFFFFFFF, 0xFFE10600,
            0xFFFF8800, 0xFFE10600, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            279, "ducati_multistrada_v4_rally", "Ducati", "Multistrada V4 Rally (6.5\" Radar Map TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11500.0, 13000.0, false, true, "MULTISTRADA // V4 RALLY", listOf("Enduro", "Touring", "Full Map"),
            0xFFF5F5F5, 0xFF0D0F14, 0xFF08090C, 0xFFFFFFFF, 0xFFE10600,
            0xFFFF9100, 0xFFE10600, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            280, "ducati_1098_1198", "Ducati", "1098 / 1198 S (Digitek Corse Race LCD)", "2007–2011",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            10500.0, 12000.0, true, true, "DUCATI // 1198 CORSE", listOf("Digitek Race"),
            0xFF0A0B0E, 0xFFFFFFFF, 0xFF060709, 0xFFFFFFFF, 0xFFE10600,
            0xFFFF9100, 0xFFE10600, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            281, "ktm_990_duke", "KTM", "990 Duke (5\" Bonded Sniper Track TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            10500.0, 12000.0, false, true, "KTM // 990 DUKE", listOf("Track", "Street", "Telemetry"),
            0xFF101012, 0xFFFFFFFF, 0xFF070709, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF6600, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            282, "ktm_1390_super_adv_s_evo", "KTM", "1390 Super Adventure S Evo (8\" Portrait TFT)", "2025+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10200.0, 11500.0, false, true, "1390 SUPER ADV EVO", listOf("Street", "Rally", "Radar"),
            0xFF121318, 0xFFFFFFFF, 0xFF08090D, 0xFFFFFFFF, 0xFFFF6600,
            0xFFFF6600, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            283, "aprilia_rsv_mille_rp", "Aprilia", "RSV Mille 1000 R (White Analog + Twin Green LCD)", "2001–2003",
            GaugeDisplayCategory.ANALOG_HYBRID, GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD, ShiftLedPattern.SINGLE_UPPER_LED,
            10500.0, 12000.0, false, true, "APRILIA // RSV MILLE R", listOf("Green LCD Analog"),
            0xFF0F1410, 0xFFD9FFCC, 0xFF0A0F0B, 0xFF9CFF7A, 0xFFFF1E1E,
            0xFFFF9100, 0xFFFF1E1E, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            284, "moto_guzzi_v100_mandello", "Moto Guzzi", "V100 Mandello S (5\" Adaptive Aero TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, true, "MOTO GUZZI // V100", listOf("Sport", "Touring"),
            0xFFF3F5F8, 0xFF10131A, 0xFF07090D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            285, "triumph_tiger_sport_660", "Triumph", "Tiger Sport 660 (Compact TFT + White LCD)", "2022+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            11250.0, 12500.0, false, false, "TRIUMPH // TIGER SPORT 660", listOf("Road", "Rain"),
            0xFF0B0E14, 0xFFFFFFFF, 0xFF06080C, 0xFFFFFFFF, 0xFFFF3D00,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            286, "triumph_bobber_1200", "Triumph", "Bonneville Bobber (Single Chrome Dial)", "2017+",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7000.0, 8500.0, false, false, "TRIUMPH // BOBBER 1200", listOf("Heritage Single"),
            0xFF0D0E12, 0xFFF5E6C8, 0xFF08090C, 0xFFFFD180, 0xFFFF6D00,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            287, "triumph_speed_twin_1200_rs", "Triumph", "Speed Twin 1200 RS (Oblong Color TFT + LCD)", "2025+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CIRCULAR_RING_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 9500.0, false, true, "SPEED TWIN 1200 RS", listOf("Sport", "Road"),
            0xFF0C0E14, 0xFFFFFFFF, 0xFF07080C, 0xFFFFFFFF, 0xFFFF3D00,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            288, "voge_525dsx", "Voge", "525DSX (7\" Adventure Radar TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, true, "VOGE // 525DSX", listOf("Offroad", "Touring"),
            0xFFF3F5FA, 0xFF10131C, 0xFF07090E, 0xFFFFFFFF, 0xFFFF9100,
            0xFFFFAB00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            289, "voge_rr525", "Voge", "RR 525 (5\" Track Data TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, true, true, "VOGE // RR525", listOf("Sport", "Street"),
            0xFF0B0E16, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            290, "voge_300_rally", "Voge", "300 Rally (Vertical Dakar LCD)", "2023+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, false, "VOGE // 300 RALLY", listOf("Rally LCD"),
            0xFF0B0E14, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFFF9100,
            0xFFFFAB00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            291, "kove_450_rally", "Kove", "450 Rally (Carbon Dakar Tower TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.SINGLE_UPPER_LED,
            10500.0, 12000.0, false, true, "KOVE // 450 RALLY DAKAR", listOf("Dakar Tower", "Roadbook"),
            0xFF0A0D12, 0xFFFFFFFF, 0xFF05070B, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFFAB00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            292, "kove_800x_pro", "Kove", "800X Pro (Vertical Adventure TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10000.0, 11500.0, false, true, "KOVE // 800X PRO", listOf("Adventure", "Sport"),
            0xFFF3F6FA, 0xFF10141E, 0xFF07090F, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            293, "gasgas_sm_700", "GasGas", "SM 700 / ES 700 (Supermoto LCD)", "2022+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            9000.0, 10500.0, false, true, "GASGAS // GET ON THE GAS", listOf("Supermoto LCD"),
            0xFF0C0A0A, 0xFFFFFFFF, 0xFF070505, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF6D00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            294, "beta_rr_300_racing", "Beta", "RR 300 Racing 2T (Enduro Comp LCD)", "2024+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            10500.0, 12000.0, false, false, "BETA // RR 300 2T", listOf("Enduro Race"),
            0xFF0B0E12, 0xFFFFFFFF, 0xFF06080B, 0xFFFFFFFF, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            295, "sherco_300_sef", "Sherco", "300 SEF Factory (Enduro Comp LCD)", "2024+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12500.0, 14000.0, false, false, "SHERCO // 300 SEF", listOf("Factory Enduro"),
            0xFF080D18, 0xFFFFFFFF, 0xFF050810, 0xFFFFFFFF, 0xFF0091EA,
            0xFFFFD600, 0xFF0091EA, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            296, "fantic_caballero_500", "Fantic", "Caballero Scrambler 500 (Round Chrome/LCD)", "2020+",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8500.0, 10000.0, false, false, "FANTIC // CABALLERO 500", listOf("Scrambler Dial"),
            0xFF0B0D12, 0xFFF5E6C8, 0xFF07080C, 0xFFFFD180, 0xFFE60012,
            0xFFFF9100, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            297, "bimota_tesi_h2", "Bimota", "Tesi H2 (Carbon Supercharged TFT)", "2020+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            13500.0, 15000.0, true, true, "BIMOTA // TESI H2", listOf("Boost Track", "Street"),
            0xFF0A0B0F, 0xFFFFFFFF, 0xFF050608, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            298, "norton_v4sv", "Norton", "V4SV (Carbon Superbike 6\" TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT, ShiftLedPattern.CONVERGING_5_LEDS,
            13500.0, 15000.0, true, true, "NORTON // V4SV", listOf("Track", "Road"),
            0xFF0A0B0D, 0xFFFFFFFF, 0xFF050608, 0xFFFFFFFF, 0xFFC0C8D4,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            299, "cfmoto_450mt", "CFMoto", "450MT (5\" Vertical Curved Rally TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, true, "CFMOTO // 450MT", listOf("Offroad", "Touring"),
            0xFF0B0E14, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFFAB00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            300, "cfmoto_800mt_explore", "CFMoto", "800MT Explore (8\" CarPlay Radar MMI)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.PANORAMIC_SPLIT_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            9500.0, 11000.0, false, true, "CFMOTO // 800MT EXPLORE", listOf("CarPlay Split", "Adventure"),
            0xFF0A0E16, 0xFFFFFFFF, 0xFF05080D, 0xFFFFFFFF, 0xFF00B0FF,
            0xFFFF9100, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            301, "cfmoto_675nk", "CFMoto", "675NK (Triple 5\" Curved TFT)", "2025+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.CURVED_SWEEP_BAR_TFT, ShiftLedPattern.NONE_INTERNAL_ONLY,
            12000.0, 13500.0, true, true, "CFMOTO // 675NK TRIPLE", listOf("Sport", "Street"),
            0xFF0B0F18, 0xFFFFFFFF, 0xFF06090F, 0xFFFFFFFF, 0xFF00E5FF,
            0xFFFFAB00, 0xFF00E5FF, 0xFF06090F
        ),
        DeclarativeGaugeSpec(
            302, "hero_mavrick_440", "Hero", "Mavrick 440 (Negative Round-Bar LCD)", "2024+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            6500.0, 8000.0, false, false, "HERO // MAVRICK 440", listOf("Street LCD"),
            0xFF0B0E14, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFE60012,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            303, "hero_karizma_xmr_210", "Hero", "Karizma XMR 210 (Supersport Inverted LCD)", "2024+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.F1_DIGITAL_ARC_BAR, ShiftLedPattern.SINGLE_UPPER_LED,
            10000.0, 11500.0, false, true, "HERO // KARIZMA XMR", listOf("Sport LCD"),
            0xFF0A0D14, 0xFFFFFFFF, 0xFF06080D, 0xFFFFFFFF, 0xFFFFD600,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            304, "royal_enfield_guerrilla_450", "Royal Enfield", "Guerrilla 450 (4\" Round Tripper Dash TFT)", "2024+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 9500.0, false, true, "ROYAL ENFIELD // GRR 450", listOf("Performance Map", "Digital"),
            0xFF0C0E14, 0xFFFFFFFF, 0xFF07080C, 0xFFFFFFFF, 0xFFFFC107,
            0xFFFF6D00, 0xFFFF3D00, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            305, "royal_enfield_shotgun_650", "Royal Enfield", "Shotgun 650 (Chrome Analog + Tripper Pod)", "2024+",
            GaugeDisplayCategory.RETRO_CLASSIC, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 9000.0, false, false, "ROYAL ENFIELD // SHOTGUN 650", listOf("Analog + Tripper"),
            0xFF0B0D10, 0xFFF5E6C8, 0xFF06070A, 0xFFFFD180, 0xFF00B0FF,
            0xFFFFAB00, 0xFFE60012, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            306, "jawa_42_bobber", "Jawa", "42 Bobber (Negative Floating Round LCD)", "2023+",
            GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            8000.0, 9500.0, false, false, "JAWA // 42 BOBBER", listOf("Round Negative LCD"),
            0xFF0A0C12, 0xFFFFFFFF, 0xFF06070A, 0xFFFFFFFF, 0xFFFF3D00,
            0xFFFF9100, 0xFFFF1744, 0xFFFFFFFF
        ),
        DeclarativeGaugeSpec(
            307, "brixton_cromwell_1200", "Brixton", "Cromwell 1200 (Circular Color TFT)", "2023+",
            GaugeDisplayCategory.MODERN_TFT, GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD, ShiftLedPattern.NONE_INTERNAL_ONLY,
            7500.0, 9000.0, false, false, "BRIXTON // CROMWELL 1200", listOf("Sport", "Eco"),
            0xFF0D0F14, 0xFFFFFFFF, 0xFF07090D, 0xFFFFFFFF, 0xFF00E676,
            0xFFFFAB00, 0xFFFF3D00, 0xFFFFFFFF
        ),
    )

    val allBrands: List<String> = allModels.map { it.brand }.distinct()

    fun getBySkinId(skinId: String): DeclarativeGaugeSpec =
        allModels.firstOrNull { it.skinId == skinId } ?: allModels.first()
}

