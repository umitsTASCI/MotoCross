package com.oemcluster.pro.renderer

import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.oemcluster.pro.catalog.GaugeDisplayCategory
import com.oemcluster.pro.domain.ShiftLightState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Gerçek OEM Gösterge İmzası ve Alt-Mod (Tema) Motoru.
 *
 * Her motosikletin gerçek fabrika kadranındaki:
 *  - Alt-mod geçişleri (Street <-> Track, Core 1 <-> Core 2 <-> Core 3, Circuit <-> Street <-> Mechanic,
 *    Sport <-> Urban <-> Rain, Theme 1..4)
 *  - Elektronik destek kutucukları (DTC/DWC/DSC/EBC, KTRC/KQS, PWR/TCS/SCS/LIF, SDMS-α, APRC, SmartXonnect)
 *  - Modele özgü çerçeve (bezel), devir ölçeği, ibre/segment formu ve ODO/TRIP/SEANS yerleşimi
 * burada birebir çizilir.
 */
internal fun DrawScope.tryDrawBespokeOemCluster(ctx: GaugeContext): Boolean {
    val id = ctx.spec.skinId
    val brand = ctx.spec.brand
    val sub = ctx.subMode.lowercase()

    // 1. TVS Motor (Apache RTR 200 / RR 310 / RTR 310 / Ronin — portrait + landscape)
    if (brand == "TVS") {
        when {
            id.contains("rr_310") -> drawTvsApacheRr310VerticalTft(ctx)
            id.contains("rtr_310") -> drawTvsApacheRtr310DynamicTft(ctx)
            id.contains("ronin") -> drawTvsRoninAsymmetricPuck(ctx)
            else -> drawTvsApacheRtr200RaceConsole(ctx)
        }
        return true
    }

    // 2. Bajaj Auto (Dominar Dual Console, NS400Z/N250 Infinity, Pulsar Perimeter Hybrid — portrait + landscape)
    if (brand == "Bajaj") {
        when {
            id.contains("dominar") -> drawBajajDominarDualConsole(ctx)
            id.contains("ns400") || id.contains("n250") -> drawBajajInfinityDigitalCluster(ctx)
            else -> drawBajajPulsarPerimeterHybrid(ctx)
        }
        return true
    }

    // 3. BMW Motorrad (Core 1/2/3, Pure Ride, GS Adventure, RT/K1600 10.25" Split)
    if (brand == "BMW Motorrad") {
        if (ctx.spec.displayCategory != GaugeDisplayCategory.MODERN_TFT) return false
        when {
            id.contains("r1250rt") || id.contains("r1300rt") || id.contains("k1600") || id.contains("r18_trans") ->
                drawBmwRt1025SplitTft(ctx)
            id.contains("gs") && !id.contains("g310") -> drawBmwGsAdventureTft(ctx)
            id.contains("s1000") || id.contains("m1000") || id.contains("f900") -> when {
                sub.contains("core 2") || sub.contains("analog") -> drawBmwCore2AnalogTft(ctx)
                sub.contains("core 3") || sub.contains("lap") -> drawBmwCore3TrackLapTft(ctx)
                sub.contains("pure") -> drawBmwPureRideTft(ctx)
                else -> drawBmwCore1MPowerTft(ctx)
            }
            else -> drawBmwPureRideTft(ctx)
        }
        return true
    }

    // 4. Yamaha (R1/R1M, MT-09 Gen4 Piano, Ténéré Raid, Tracer Twin, R7 LCD, R3/R15 M1)
    if (brand == "Yamaha") {
        when {
            id.contains("tenere") -> drawYamahaTenereRaidTft(ctx)
            id.contains("tracer") || id.contains("fjr") -> drawYamahaTracerTwinTft(ctx)
            id.contains("mt09_gen4") || id.contains("mt09_2024") || id.contains("xsr900_gp") ->
                drawYamahaMt09Gen4MultiThemeTft(ctx)
            id.contains("r7") || id.contains("mt07") || id.contains("xsr700") ->
                drawYamahaR7NegativeLcd(ctx)
            id.contains("yzf_r1") || id.contains("mt10") || id.contains("yzf_r9") -> {
                if (sub.contains("track")) drawYamahaR1TrackModeTft(ctx) else drawYamahaR1StreetModeTft(ctx)
            }
            id.contains("r25_r3_gen2") || id.contains("r15") || id.contains("mt25") || id.contains("mt15") -> {
                drawYamahaR3M1RaceLcd(ctx, isTrack = sub.contains("track"))
            }
            else -> return false
        }
        return true
    }

    // 5. Honda (2024 Circle/Bar/Simple, Africa Twin Dual, Gold Wing Cockpit, Fireblade Circuit/Mechanic)
    if (brand == "Honda") {
        if (ctx.spec.displayCategory == GaugeDisplayCategory.RETRO_CLASSIC ||
            ctx.spec.displayCategory == GaugeDisplayCategory.ANALOG_HYBRID
        ) {
            return false
        }
        when {
            id.contains("africa_twin") && !id.contains("750") -> drawHondaAfricaTwinDualTft(ctx)
            id.contains("gold_wing") || id.contains("goldwing") -> drawHondaGoldWingCockpit(ctx)
            id.contains("cbr1000") || id.contains("cbr600rr_2024") || id.contains("cb1000") -> when {
                sub.contains("mechanic") || sub.contains("simple") -> drawHondaMechanicTelemetryTft(ctx)
                sub.contains("bar") || (sub.contains("street") && id.contains("sc77")) -> drawHondaSweptBarTft(ctx)
                else -> drawHondaFirebladeCircuitRingTft(ctx)
            }
            id.contains("cb750") || id.contains("cbr650r_2024") || id.contains("cb650r_2024") ||
                id.contains("transalp") || id.contains("nx500") || id.contains("cb500") ->
                drawHonda2024CircleBarSimpleTft(ctx)
            else -> return false
        }
        return true
    }

    // 6. Kawasaki (H2 Chronograph+Boost, ZX-10R LED Arch, Z900 C-Ring LCD, Type 1/2 TFT)
    if (brand == "Kawasaki") {
        if (ctx.spec.displayCategory == GaugeDisplayCategory.RETRO_CLASSIC) return false
        when {
            id.contains("h2") -> drawKawasakiH2ChronographBoostReal(ctx)
            id.contains("zx10r_2016") || id.contains("zx10r_gen5") -> drawKawasakiZx10rLedArchLcd(ctx)
            id.contains("z900") && !id.contains("tft") && !id.contains("se_2025") -> drawKawasakiZ900CRingLcdReal(ctx)
            ctx.spec.displayCategory == GaugeDisplayCategory.MODERN_TFT && (
                id.contains("zx10r") || id.contains("zx6r_2024") || id.contains("zx4rr") ||
                    id.contains("z900") || id.contains("ninja_500") || id.contains("ninja_650") || id.contains("ninja_1000")
                ) -> {
                if (sub.contains("circuit") || sub.contains("track") || sub.contains("type 2")) {
                    drawKawasakiCircuitModeTft(ctx)
                } else {
                    drawKawasakiStreetType1TftReal(ctx)
                }
            }
            else -> return false
        }
        return true
    }

    // 7. Suzuki (Hayabusa Gen3 SDMS-α, GSX-8S/GX TFT, SV650/DR-Z LCD, GSX-R1000R L7)
    if (brand == "Suzuki") {
        if (ctx.spec.displayCategory == GaugeDisplayCategory.RETRO_CLASSIC) return false
        when {
            id.contains("hayabusa") && ctx.spec.displayCategory == GaugeDisplayCategory.MODERN_TFT ->
                drawSuzukiHayabusaGen3Real(ctx)
            id.contains("gsx_8s") || id.contains("gsx_8r") || id.contains("gsx_s1000gt") ||
                id.contains("gsx_s1000gx") || id.contains("vstrom_800") || id.contains("vstrom_1050") ->
                drawSuzukiGsx8sGtTft(ctx)
            id.contains("drz") || id.contains("sv650") -> drawSuzukiSv650NegativeLcd(ctx)
            id.contains("gsxr1000r_l7") || id.contains("katana") || id.contains("gsxr125") || id.contains("gsxr250") ->
                drawSuzukiGsxrL7NegativeLcd(ctx)
            else -> return false
        }
        return true
    }

    // 8. Ducati (2025 V4 6.9" Wide, 1098 Digitek LCD, Scrambler 800, Road Circular, Track Evo)
    if (brand == "Ducati") {
        if (ctx.spec.displayCategory == GaugeDisplayCategory.RETRO_CLASSIC ||
            ctx.spec.displayCategory == GaugeDisplayCategory.ANALOG_HYBRID
        ) {
            return false
        }
        when {
            id.contains("1098") || id.contains("1198") || id.contains("848") || id.contains("999") ->
                drawDucatiDigitek1098Lcd(ctx)
            id.contains("scrambler") -> drawDucatiScrambler800Tft(ctx)
            id.contains("panigale_v4") || id.contains("streetfighter_v4") || id.contains("gp24") -> {
                if (sub.contains("road") || sub.contains("touring") || sub.contains("urban")) {
                    drawDucatiRoadCircularTft(ctx)
                } else {
                    drawDucatiV4Wide2025Tft(ctx)
                }
            }
            id.contains("panigale") || id.contains("streetfighter") || id.contains("monster") ||
                id.contains("hypermotard") || id.contains("multistrada") || id.contains("desertx") || id.contains("diavel") -> {
                if (sub.contains("road") || sub.contains("touring") || sub.contains("urban")) {
                    drawDucatiRoadCircularTft(ctx)
                } else {
                    drawDucatiTrackEvoBespokeTft(ctx)
                }
            }
            else -> return false
        }
        return true
    }

    // 9. KTM / Husqvarna / GasGas
    if (brand == "KTM") {
        when {
            id.contains("250_duke_2024") -> drawKtm250Duke2024Lcd(ctx)
            id.contains("gen1_lcd") || id.contains("200_duke") || id.contains("rc_200") ||
                id.contains("rc_125") || id.contains("690_smc") -> drawKtmGen1AmberLcd(ctx)
            id.contains("adventure") || id.contains("enduro") -> drawKtmAdventureTowerTft(ctx)
            sub.contains("track") -> drawKtmTrackRaceTft(ctx)
            id.contains("390_duke_gen3") || id.contains("990_duke") || id.contains("1390_super_duke") ->
                drawKtmSniperGen3Tft(ctx)
            id.contains("390_duke_gen2") || id.contains("rc_390") || id.contains("890_duke") || id.contains("1290_super_duke") ->
                drawKtmStreetFavoritesTft(ctx)
            else -> drawKtmSniperGen3Tft(ctx)
        }
        return true
    }
    if (brand == "Husqvarna") {
        if (id.contains("norden")) drawHusqvarnaNorden901Tft(ctx) else drawHusqvarnaBauhaus2024Tft(ctx)
        return true
    }
    if (brand == "GasGas") {
        drawKtmAdventureTowerTft(ctx)
        return true
    }

    // 10. Triumph (4-Tema 765/1200 RS, 660 Split LCD+TFT; klasik çift analoglar genel renderer'a bırakılır)
    if (brand == "Triumph") {
        if (ctx.spec.displayCategory == GaugeDisplayCategory.RETRO_CLASSIC ||
            ctx.spec.displayCategory == GaugeDisplayCategory.ANALOG_HYBRID
        ) {
            return false
        }
        when {
            id.contains("daytona_660") || id.contains("trident_660") || id.contains("tiger_sport_660") -> {
                drawTriumph660SplitLcdTft(ctx)
                return true
            }
            id.contains("765") || id.contains("1200") || id.contains("tiger") || id.contains("rocket") -> {
                drawTriumphMultiStyleTft(ctx)
                return true
            }
            else -> return false
        }
    }

    // 11. MV Agusta & Aprilia
    if (brand == "MV Agusta") {
        if (ctx.spec.displayCategory != GaugeDisplayCategory.MODERN_TFT) return false
        drawMvAgusta55Tft(ctx)
        return true
    }
    if (brand == "Aprilia") {
        if (ctx.spec.displayCategory != GaugeDisplayCategory.MODERN_TFT) return false
        if (id.contains("tuareg")) {
            drawApriliaTuaregRallyTft(ctx)
            return true
        }
        if (id.contains("rsv4_factory") || id.contains("rs660") || id.contains("rs457") || id.contains("tuono")) {
            drawApriliaAprcWingTft(ctx, isTrack = sub.contains("track"))
            return true
        }
        return false
    }

    // 12. Cruiser & Hint & Çin / Küresel OEM Markaları (32 Markanın Tamamı)
    if (brand == "Harley-Davidson") {
        when {
            id.contains("skyline") || id.contains("street_glide") || id.contains("road_glide") || id.contains("cvo") ->
                drawHarleySkyline123Tft(ctx)
            id.contains("sportster_s") || id.contains("nightster") || id.contains("pan_america") ->
                drawHarleySportsterS4Tft(ctx)
            else -> drawHarleyIron883AnalogLcd(ctx)
        }
        return true
    }
    if (brand == "Indian") {
        if (id.contains("challenger") || id.contains("pursuit") || id.contains("chieftain") || id.contains("roadmaster")) {
            drawIndianChallengerSplitTft(ctx)
        } else {
            drawIndianFtrRoundTft(ctx)
        }
        return true
    }
    if (brand == "Royal Enfield") {
        if (id.contains("450") || id.contains("himalayan_450") || id.contains("guerrilla")) {
            drawRoyalEnfieldHimalayan450TripperDash(ctx)
        } else {
            drawRoyalEnfieldTripperSatelliteAndAnalog(ctx)
        }
        return true
    }
    if (brand == "CFMoto") {
        when {
            id.contains("800nk") || id.contains("800mt") || id.contains("ibex_800") -> drawCfmoto800NkVertical8Tft(ctx)
            id.contains("450mt") -> drawCfmoto450MtRallyTft(ctx)
            else -> drawCfmoto450SrCurvedTft(ctx)
        }
        return true
    }
    if (brand == "Hero") {
        drawHeroInvertedSportLcd(ctx)
        return true
    }
    if (brand == "Moto Guzzi") {
        drawMotoGuzziEagleTft(ctx)
        return true
    }
    if (brand == "Kove") {
        if (id.contains("rally") || id.contains("800x")) drawKoveRallyTowerTft(ctx) else drawGlobalSportTft(ctx)
        return true
    }
    if (brand == "QJMotor" || brand == "Voge" || brand == "Zontes" || brand == "Benelli") {
        drawGlobalSportTft(ctx)
        return true
    }
    if (brand == "Norton" || brand == "Bimota" || brand == "Jawa" || brand == "Brixton") {
        drawHeritageBritishItalianDial(ctx)
        return true
    }

    return false
}

/**
 * Her kadranın sonunda model kimliğine göre benzersiz OEM çerçeve (bezel),
 * model rozeti ve alt-mod mikro imzasını çizer.
 */
internal fun DrawScope.drawModelUniqueHardwareSignature(ctx: GaugeContext) {
    val p = ctx.palette
    val s = ctx.spec
    val u = ctx.u
    val seed = s.skinId.hashCode()
    val cornerCut = ((abs(seed) % 5) + 2) * u * 0.65f

    // 1. Modele özgü köşe pahları (fiziksel kasa/cam kesimi hissi)
    val frameColor = p.accent.copy(alpha = 0.22f)
    // Sol üst pah
    drawLine(frameColor, Offset(u * 0.8f, u * 0.8f + cornerCut), Offset(u * 0.8f + cornerCut, u * 0.8f), max(1.2f, u * 0.28f))
    // Sağ üst pah
    drawLine(frameColor, Offset(ctx.w - u * 0.8f - cornerCut, u * 0.8f), Offset(ctx.w - u * 0.8f, u * 0.8f + cornerCut), max(1.2f, u * 0.28f))

    drawOemShiftReactionOverlay(ctx)

    // 2. Üst merkez/sol-üst OEM Model & Devir Limiti Mikro Rozeti (Hiçbir kadran aynı kalmaz)
    val redlineK = String.format(java.util.Locale.US, "%.1fk", s.gaugeRedlineRpm / 1000.0)
    val badgeText = "${s.brand.uppercase()} • ${s.modelName.substringBefore(" (").uppercase()} • RL $redlineK"
    val badgeY = ctx.h - ctx.h * 0.082f - u * 1.15f
    gText(
        badgeText,
        ctx.w * 0.50f,
        badgeY,
        u * 1.18f,
        p.muted.copy(alpha = 0.52f),
        Paint.Align.CENTER,
        ctx.type.label,
        true,
        spacing = 0.08f
    )
}

/**
 * Tam ekran inversiyon kullanmayan TFT / Hybrid / Dijital OEM kadranlarda,
 * gerçek fabrika vites değişim reaksiyonunu (kenar parlaması, üst bant darbesi) çizer.
 */
private fun DrawScope.drawOemShiftReactionOverlay(ctx: GaugeContext) {
    val s = ctx.spec
    if (s.displayCategory == GaugeDisplayCategory.RETRO_CLASSIC) return
    if (ctx.palette.inverted) return
    val stage = ctx.telemetry.shiftLightState
    if (stage == ShiftLightState.NORMAL) return

    val u = ctx.u
    val flashOn = ctx.telemetry.shiftFlashPhaseOn
    val isStage1 = stage == ShiftLightState.STAGE_1_APPROACHING
    val isOverRev = stage == ShiftLightState.OVER_REV_LIMITER

    val primaryFlash = when (s.brand) {
        "BMW Motorrad" -> if (isStage1) Color(0xFFFFB300) else Color.White
        "Ducati", "Aprilia", "MV Agusta" -> if (isStage1) Color(0xFFFF7A00) else RL_RED
        "Yamaha" -> if (isStage1) Color(0xFFFFB300) else if (isOverRev) RL_RED else Color(0xFF00E5FF)
        "Kawasaki" -> if (isStage1) Color(0xFF76FF03) else Color.White
        "Honda" -> if (isStage1) Color(0xFFFF9100) else Color(0xFFE60012)
        else -> if (isStage1) ctx.palette.stage1 else if (isOverRev) RL_RED else ctx.palette.accent
    }
    val edgeColor = if (isOverRev && !flashOn) Color.White else primaryFlash
    val strokeAlpha = when {
        isStage1 -> 0.28f
        flashOn -> 0.80f
        else -> 0.22f
    }
    val strokeW = max(1.5f, if (isStage1) u * 0.85f else u * 1.45f)
    val pad = u * 0.7f
    drawRoundRect(
        color = edgeColor.copy(alpha = strokeAlpha),
        topLeft = Offset(pad, pad),
        size = Size(ctx.w - pad * 2f, ctx.h - pad * 2f),
        cornerRadius = CornerRadius(u * 1.4f, u * 1.4f),
        style = Stroke(width = strokeW)
    )
    if (!isStage1 && flashOn) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(edgeColor.copy(alpha = 0.26f), Color.Transparent),
                startY = 0f,
                endY = ctx.h * 0.14f
            ),
            topLeft = Offset.Zero,
            size = Size(ctx.w, ctx.h * 0.14f)
        )
    }
}

/* =====================================================================================
 *  TVS APACHE RTR 200 4V (2024 SmartXonnect Race Edition) — Birebir OEM Kadran
 * ===================================================================================== */
private fun DrawScope.drawTvsApacheRtr200RaceConsole(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val sub = ctx.subMode.uppercase() // SPORT, URBAN, RAIN

    // Mod rengi: SPORT -> Kırmızı/Turuncu, URBAN -> Cyan/Mavi, RAIN -> Mavi/Yeşil
    val modeColor = when {
        sub.contains("URBAN") -> Color(0xFF00E5FF)
        sub.contains("RAIN") -> Color(0xFF2979FF)
        else -> Color(0xFFFF2A00)
    }

    // Üstte TVS Racing SmartXonnect başlığı ve Shift-Light LED'i
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.0f, ctx.w * 0.28f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.075f, ctx.h * 0.085f, u * 4.6f, u, modeColor)
    gText("SMARTXONNECT // RACE EDITION 4V", ctx.w * 0.125f, ctx.h * 0.092f, u * 1.55f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.10f)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, ctx.h * 0.095f, u * 3.2f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    // Gerçek RTR 200 4V üst kavisli 60-segment LCD devir barı (Soldan sağa yükselip düzleşen yarış eğrisi)
    val pts = rememberTvsRtrCurve(
        Offset(ctx.w * 0.055f, ctx.h * 0.34f),
        Offset(ctx.w * 0.18f, ctx.h * 0.165f),
        Offset(ctx.w * 0.92f, ctx.h * 0.165f)
    )
    drawTvsSegmentedBar(pts, 56, ctx.rpmRatio, u * 4.2f, modeColor, p.track, ctx.redlineRatio, ctx.peakRatio)

    // Devir rakamları: 0..12 (197.75cc 4V 9000 RPM peak power, 9500 redline, 12000 skala)
    val maxK = (ctx.maxRpm / 1000.0).roundToInt().coerceAtLeast(10)
    for (k in 0..maxK) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        val inRed = frac >= ctx.redlineRatio
        gText(
            k.toString(),
            pt.x,
            pt.y + u * 5.2f,
            u * 2.1f,
            if (inRed) RL_RED else p.fg.copy(alpha = 0.82f),
            Paint.Align.CENTER,
            ctx.type.digits,
            true
        )
    }
    gText("x1000 RPM", ctx.w * 0.92f, ctx.h * 0.255f, u * 1.35f, p.muted, Paint.Align.RIGHT, ctx.type.label, true, spacing = 0.08f)

    if (ctx.wide) {
        // Sol blok: Vites kutusu + SmartXonnect Gerçek Telemetri (0-60 sn, Top Speed, G-Force)
        drawPanel(ctx.w * 0.045f, ctx.h * 0.40f, ctx.w * 0.22f, ctx.h * 0.42f, p, accentEdge = true)
        gText("GEAR", ctx.w * 0.155f, ctx.h * 0.455f, u * 1.55f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)
        drawGearPod(ctx.w * 0.155f, ctx.h * 0.565f, u * 12.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        gText("PEAK  ${ctx.formatPeakSpeed()}", ctx.w * 0.065f, ctx.h * 0.705f, u * 1.50f, modeColor, Paint.Align.LEFT, ctx.type.digits, true)
        gText("0-60  ${ctx.formatBest0to60()}", ctx.w * 0.065f, ctx.h * 0.752f, u * 1.50f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("RT-Fi  •  5-SPEED SLIPPER", ctx.w * 0.065f, ctx.h * 0.795f, u * 1.25f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

        // Merkez: Dev Dijital Hız + Sürüş Modu (SPORT / URBAN / RAIN)
        drawRoundRect(
            modeColor.copy(alpha = 0.18f),
            Offset(ctx.w * 0.40f, ctx.h * 0.305f),
            Size(ctx.w * 0.22f, u * 4.8f),
            CornerRadius(u * 0.9f, u * 0.9f)
        )
        drawRoundRect(
            modeColor,
            Offset(ctx.w * 0.40f, ctx.h * 0.305f),
            Size(ctx.w * 0.22f, u * 4.8f),
            CornerRadius(u * 0.9f, u * 0.9f),
            style = Stroke(width = u * 0.30f)
        )
        gText("MODE: $sub", ctx.w * 0.51f, ctx.h * 0.305f + u * 3.3f, u * 2.25f, modeColor, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.12f)

        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.51f, ctx.h * 0.655f, u * 27.5f, p.fg, p.muted, ctx.type.digits)

        // Merkez alt: ODO / TRIP 1 / TRIP 2 / SEANS (Orijinal RTR 200 LCD alt satırı)
        drawPanel(ctx.w * 0.30f, ctx.h * 0.705f, ctx.w * 0.42f, ctx.h * 0.115f, p)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.325f, ctx.h * 0.752f, u * 2.0f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP 1 ${ctx.formatTrip()}", ctx.w * 0.695f, ctx.h * 0.752f, u * 1.9f, modeColor, Paint.Align.RIGHT, ctx.type.digits, true)
        gText("SEANS ${ctx.formatSession()}", ctx.w * 0.325f, ctx.h * 0.798f, u * 1.55f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("TRIP 2 ${ctx.formatTripB()}", ctx.w * 0.695f, ctx.h * 0.798f, u * 1.55f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)

        // Sağ blok: Gerçek Sürüş Süresi + Lean/G-Force + Hesaplanan Menzil
        drawPanel(ctx.w * 0.75f, ctx.h * 0.34f, ctx.w * 0.205f, ctx.h * 0.48f, p, accentEdge = true)
        gText("RIDE TIME & DRAG", ctx.w * 0.77f, ctx.h * 0.39f, u * 1.35f, modeColor, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.10f)
        gText(ctx.formatRideTime(), ctx.w * 0.77f, ctx.h * 0.455f, u * 3.2f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("0-100  ${ctx.formatBest0to100()}", ctx.w * 0.77f, ctx.h * 0.500f, u * 1.45f, RL_GREEN, Paint.Align.LEFT, ctx.type.digits, true)
        gText("LEAN  ${abs(t.leanAngleDeg).roundToInt()}°   G  ${String.format(java.util.Locale.US, "%.2f", t.longitudinalG)}", ctx.w * 0.77f, ctx.h * 0.565f, u * 1.60f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("PEAK  ${t.estimatedWheelHorsepower.roundToInt()} PS  •  ${t.estimatedWheelTorqueNm.roundToInt()} Nm", ctx.w * 0.77f, ctx.h * 0.618f, u * 1.40f, modeColor, Paint.Align.LEFT, ctx.type.label, true)
        gText("FUEL LEVEL", ctx.w * 0.77f, ctx.h * 0.685f, u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        drawLinearBar(ctx.w * 0.77f, ctx.h * 0.71f, ctx.w * 0.165f, u * 1.8f, t.fuelLevelRatio, modeColor, p.track, 0.9f, RL_RED, u * 0.5f)
        gText("RNG ${ctx.formatRange()}  •  ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", ctx.w * 0.77f, ctx.h * 0.785f, u * 1.45f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
    } else {
        // Dikey RTR 200 SmartXonnect Yerleşimi
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.47f, u * 22.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.25f, ctx.h * 0.66f, u * 12.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        gText("MODE $sub", ctx.w * 0.66f, ctx.h * 0.63f, u * 2.1f, modeColor, Paint.Align.CENTER, ctx.type.label, true)
        gText("0-60 ${ctx.formatBest0to60()}", ctx.w * 0.66f, ctx.h * 0.69f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        gText("ODO ${ctx.formatOdometer()}  •  RNG ${ctx.formatRange()}", ctx.w * 0.50f, ctx.h * 0.80f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.LCD)
}

private fun DrawScope.drawTvsApacheRr310VerticalTft(ctx: GaugeContext) {
    // Gerçek 1:1 TVS Apache RR 310 5" Dikey Bi-LED Race TFT
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val sub = ctx.subMode.uppercase()
    val modeCol = when {
        sub.contains("TRACK") -> Color(0xFFFF1E1E)
        sub.contains("SPORT") -> Color(0xFFFF6D00)
        sub.contains("RAIN") -> Color(0xFF2979FF)
        else -> Color(0xFF00E5FF)
    }
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    // Dikey Racing Frame (Landscape'te ortalanmış dikey 5" TFT kasası; Portrait'te tam ekran)
    val boxW = if (ctx.wide) ctx.w * 0.46f else ctx.w * 0.90f
    val boxX = ctx.w * 0.50f - boxW * 0.5f
    val boxTop = ctx.h * 0.055f
    val boxH = ctx.h - railH - boxTop - u * 2.4f
    drawPanel(boxX, boxTop, boxW, boxH, Color(0xFF090C12), modeCol.copy(alpha = 0.62f), u * 1.8f)

    // TVS Racing damalı bayrak üst şeridi
    for (sq in 0 until 8) {
        val c = if (sq % 2 == 0) Color.White else modeCol
        drawRect(c.copy(alpha = 0.78f), Offset(boxX + u * 2.2f + sq * u * 1.5f, boxTop + u * 2.0f), Size(u * 1.3f, u * 1.2f))
    }
    drawBrandMark(ctx.spec, p, boxX + boxW - u * 5.2f, boxTop + u * 3.0f, u * 3.6f, u, modeCol)
    gText("RR 310 // $sub", boxX + boxW * 0.5f, boxTop + u * 6.4f, u * 1.65f, modeCol, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.08f)

    // Üst yüksek çözünürlüklü segmentli devir barı
    drawLinearBar(boxX + u * 2.2f, boxTop + u * 8.4f, boxW - u * 4.4f, u * 3.6f, ctx.rpmRatio, modeCol, Color(0xFF1A2230), ctx.redlineRatio, RL_RED, u * 0.7f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, boxX + boxW * 0.36f, boxTop + boxH * 0.44f, u * 16.5f, Color.White, Color(0xFF8C9AAE), ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, boxX + boxW * 0.76f, boxTop + boxH * 0.42f, u * 13.5f, modeCol, Color.White, ctx.type.digits)

    // Alt Race Kartları (0-60 / Top Speed / Coolant / Range — sıfır sahte veri)
    gText("0-60 ${ctx.formatBest0to60()}   •   PEAK ${ctx.formatPeakSpeed()}", boxX + boxW * 0.5f, boxTop + boxH * 0.68f, u * 1.8f, RL_GREEN, Paint.Align.CENTER, ctx.type.digits, true)
    gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°   •   RANGE ${ctx.formatRange()}   •   ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", boxX + boxW * 0.5f, boxTop + boxH * 0.80f, u * 1.55f, Color.White, Paint.Align.CENTER, ctx.type.label, true)
    gText("ODO ${ctx.formatOdometer()}  •  TRIP ${ctx.formatTrip()}", boxX + boxW * 0.5f, boxTop + boxH - u * 2.4f, u * 1.45f, Color(0xFF8C9AAE), Paint.Align.CENTER, ctx.type.digits, true)

    if (ctx.wide) {
        drawLeanArc(ctx.w * 0.14f, ctx.h * 0.48f, ctx.w * 0.085f, t.leanAngleDeg, modeCol, p.track, u)
        gText("PEAK L ${ctx.peakLeanLeftDeg.roundToInt()}° / R ${ctx.peakLeanRightDeg.roundToInt()}°", ctx.w * 0.14f, ctx.h * 0.67f, u * 1.40f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        gText("0-100 ${ctx.formatBest0to100()}", ctx.w * 0.86f, ctx.h * 0.44f, u * 1.95f, RL_GREEN, Paint.Align.CENTER, ctx.type.digits, true)
        gText("${t.estimatedWheelHorsepower.roundToInt()} PS  •  ${ctx.formatBattery()}", ctx.w * 0.86f, ctx.h * 0.54f, u * 1.65f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

private fun DrawScope.drawTvsApacheRtr310DynamicTft(ctx: GaugeContext) {
    // Gerçek 1:1 TVS Apache RTR 310 Yatay 5" Dynamic TFT
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val cyan = Color(0xFF00E5FF)
    val tvsRed = Color(0xFFFF1E1E)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.30f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.06f, ctx.h * 0.068f, u * 4.2f, u, tvsRed)
    gText("RTR 310 // ${ctx.subMode.uppercase()}", ctx.w * 0.13f, ctx.h * 0.075f, u * 1.65f, cyan, Paint.Align.LEFT, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, ctx.h * 0.075f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    // İkonik Dinamik Çapraz Tach Bar
    val curve = List(30) { i ->
        val f = i / 29f
        val bx = ctx.w * (0.06f + f * 0.88f)
        val by = ctx.h * (0.31f - sin(f * PI * 0.5).toFloat() * 0.15f)
        Offset(bx, by)
    }
    drawPolylineBar(curve, 44, ctx.rpmRatio, u * 4.4f, cyan, p.track, ctx.redlineRatio, ctx.peakRatio, u)

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), ctx.h * 0.50f, u * 19.5f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.74f else 0.50f), ctx.h * (if (ctx.wide) 0.49f else 0.66f), u * 15.5f, tvsRed, p.fg, ctx.type.digits)
    gText("0-60 ${ctx.formatBest0to60()}   •   0-100 ${ctx.formatBest0to100()}   •   LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.50f, ctx.h * 0.77f, u * 1.8f, cyan, Paint.Align.CENTER, ctx.type.digits, true)

    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

private fun DrawScope.drawTvsRoninAsymmetricPuck(ctx: GaugeContext) {
    // Gerçek 1:1 TVS Ronin Asimetrik Krom Yuvarlak SmartXonnect LCD Podu
    val p = ctx.palette
    val u = ctx.u
    val roninCyan = Color(0xFF00E5FF)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val cx = if (ctx.wide) ctx.w * 0.42f else ctx.w * 0.50f
    val cy = if (ctx.wide) ctx.h * 0.49f else ctx.h * 0.43f
    val r = min(ctx.w * (if (ctx.wide) 0.28f else 0.38f), (ctx.h - railH) * 0.42f)

    drawCircle(Color(0xFF070C14), r * 1.05f, Offset(cx, cy))
    drawCircle(Color(0xFF9EA6B2), r * 1.05f, Offset(cx, cy), style = Stroke(width = u * 0.85f))
    drawSegmentedArc(cx, cy, r * 0.88f, 150f, 240f, 34, ctx.rpmRatio, u * 3.8f, roninCyan, Color(0xFF172230), ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
    drawBrandMark(ctx.spec, p, cx, cy - r * 0.46f, u * 3.6f, u, roninCyan)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 1.5f, u * 15.0f, Color.White, Color(0xFF7D92AC), "monospace")
    drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.56f, u * 10.5f, roninCyan, Color.White, "monospace")

    if (ctx.wide) {
        val sx = ctx.w * 0.74f
        drawPanel(sx, ctx.h * 0.25f, ctx.w * 0.21f, ctx.h * 0.48f, Color(0xFF0B111A), roninCyan.copy(alpha = 0.4f), u * 1.4f)
        gText("RONIN // ${ctx.subMode.uppercase()}", sx + u * 1.8f, ctx.h * 0.32f, u * 1.55f, roninCyan, Paint.Align.LEFT, ctx.type.label, true)
        gText("DTE  ${ctx.formatRange()}", sx + u * 1.8f, ctx.h * 0.43f, u * 2.1f, Color.White, Paint.Align.LEFT, "monospace", true)
        gText("ODO  ${ctx.formatOdometer()}", sx + u * 1.8f, ctx.h * 0.53f, u * 1.8f, p.muted, Paint.Align.LEFT, "monospace", true)
        gText("TRIP ${ctx.formatTrip()}", sx + u * 1.8f, ctx.h * 0.63f, u * 1.8f, roninCyan, Paint.Align.LEFT, "monospace", true)
    } else {
        gText("DTE ${ctx.formatRange()}  •  ODO ${ctx.formatOdometer()}", ctx.w * 0.50f, ctx.h * 0.77f, u * 1.8f, roninCyan, Paint.Align.CENTER, "monospace", true)
    }
    drawStatusRail(ctx, RailStyle.LCD)
}

/* =====================================================================================
 *  BAJAJ PULSAR RS200 / NS200 / NS400Z / DOMINAR 400 — Birebir OEM Kadranlar
 * ===================================================================================== */
private fun DrawScope.drawBajajPulsarPerimeterHybrid(ctx: GaugeContext) {
    // Pulsar RS200 / NS200: Karbon desenli merkez/sol Analog Devir Saati + Sağ/Alt Mavi-Beyaz Dijital Hız LCD
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val lcdBlue = Color(0xFF40C4FF)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val maxK = max(8, (ctx.maxRpm / 1000.0).roundToInt())
    if (ctx.wide) {
        val dialCx = ctx.w * 0.30f
        val dialCy = ctx.h * 0.49f
        val dialR = min(ctx.w * 0.24f, (ctx.h - railH) * 0.41f)
        drawNeedleDial(dialCx, dialCy, dialR, 135f, 270f, ctx.rpmRatio, p, u, Color(0xFFFF3D00), ctx.redlineRatio, maxK, 2, true, cream = false, chrome = false, labelFamily = ctx.type.digits) { it.toString() }
        drawBrandMark(ctx.spec, p, dialCx, dialCy - dialR * 0.40f, u * 3.6f, u, lcdBlue)

        val lcdX = ctx.w * 0.57f
        val lcdY = ctx.h * 0.19f
        val lcdW = ctx.w * 0.38f
        val lcdH = ctx.h * 0.61f
        drawPanel(lcdX, lcdY, lcdW, lcdH, Color(0xFF0A1626), lcdBlue.copy(alpha = 0.55f), u * 1.6f)
        drawSpeed(ctx.speedValue, ctx.speedUnit, lcdX + lcdW * 0.40f, lcdY + lcdH * 0.48f, u * 16.5f, Color.White, lcdBlue, "monospace")
        drawGearBadge(ctx.gearText, ctx.isNeutral, lcdX + lcdW * 0.82f, lcdY + lcdH * 0.42f, u * 11.5f, lcdBlue, Color.White, "monospace")
        gText("ODO ${ctx.formatOdometer()}  •  TRIP ${ctx.formatTrip()}", lcdX + u * 2.2f, lcdY + lcdH * 0.78f, u * 1.65f, Color.White, Paint.Align.LEFT, "monospace", true)
        gText("FUEL ${(t.fuelLevelRatio * 100).roundToInt()}%  •  RNG ${ctx.formatRange()}", lcdX + u * 2.2f, lcdY + lcdH * 0.90f, u * 1.55f, lcdBlue, Paint.Align.LEFT, "monospace", true)
    } else {
        val dialCx = ctx.w * 0.50f
        val dialCy = ctx.h * 0.34f
        val dialR = min(ctx.w * 0.36f, ctx.h * 0.25f)
        drawNeedleDial(dialCx, dialCy, dialR, 135f, 270f, ctx.rpmRatio, p, u, Color(0xFFFF3D00), ctx.redlineRatio, maxK, 2, true, cream = false, chrome = false, labelFamily = ctx.type.digits) { it.toString() }
        val lcdY = ctx.h * 0.62f
        val lcdH = ctx.h - railH - lcdY - u * 2.5f
        drawPanel(ctx.w * 0.08f, lcdY, ctx.w * 0.84f, lcdH, Color(0xFF0A1626), lcdBlue.copy(alpha = 0.55f), u * 1.6f)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.40f, lcdY + lcdH * 0.56f, u * 14.5f, Color.White, lcdBlue, "monospace")
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.78f, lcdY + lcdH * 0.50f, u * 11.0f, lcdBlue, Color.White, "monospace")
    }
    drawStatusRail(ctx, RailStyle.LCD)
}

private fun DrawScope.drawBajajInfinityDigitalCluster(ctx: GaugeContext) {
    // Pulsar NS400Z / N250 Infinity Display
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.0f, ctx.w * 0.30f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.07f, ctx.h * 0.085f, u * 4.4f, u, p.accent)
    gText("${ctx.spec.modelName.uppercase()} // INFINITY CONSOLE", ctx.w * 0.12f, ctx.h * 0.092f, u * 1.6f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

    val pts = rememberTvsRtrCurve(
        Offset(ctx.w * 0.07f, ctx.h * 0.36f),
        Offset(ctx.w * 0.22f, ctx.h * 0.16f),
        Offset(ctx.w * 0.93f, ctx.h * 0.16f)
    )
    drawTvsSegmentedBar(pts, 48, ctx.rpmRatio, u * 4.0f, p.accent, p.track, ctx.redlineRatio, ctx.peakRatio)
    val maxK = (ctx.maxRpm / 1000.0).roundToInt().coerceAtLeast(10)
    for (k in 0..maxK) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + u * 5.0f, u * 2.0f, if (frac >= ctx.redlineRatio) RL_RED else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
    }

    if (ctx.wide) {
        drawGearPod(ctx.w * 0.18f, ctx.h * 0.56f, u * 13.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
        gText("GEAR", ctx.w * 0.18f, ctx.h * 0.68f, u * 1.6f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.64f, u * 26.0f, p.fg, p.muted, ctx.type.digits)
        drawSubModeChip(ctx, ctx.w * 0.50f, ctx.h * 0.74f, alignCenter = true)

        drawPanel(ctx.w * 0.73f, ctx.h * 0.36f, ctx.w * 0.22f, ctx.h * 0.44f, p, accentEdge = true)
        gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.755f, ctx.h * 0.43f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP 1  ${ctx.formatTrip()}", ctx.w * 0.755f, ctx.h * 0.50f, u * 1.85f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.755f, ctx.h * 0.56f, u * 1.65f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("DFE  ${ctx.formatInstantFuel()}", ctx.w * 0.755f, ctx.h * 0.63f, u * 1.55f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
        drawLinearBar(ctx.w * 0.755f, ctx.h * 0.70f, ctx.w * 0.17f, u * 1.6f, t.fuelLevelRatio, p.accent, p.track, 0.9f, RL_RED, u * 0.6f)
        gText("RNG ${ctx.formatRange()}  •  ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", ctx.w * 0.755f, ctx.h * 0.76f, u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.49f, u * 22.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.26f, ctx.h * 0.68f, u * 12.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
        gText("DFE ${ctx.formatInstantFuel()}", ctx.w * 0.66f, ctx.h * 0.65f, u * 1.85f, p.accent, Paint.Align.CENTER, ctx.type.label, true)
        gText("ODO ${ctx.formatOdometer()}  •  RNG ${ctx.formatRange()}", ctx.w * 0.50f, ctx.h * 0.80f, u * 1.8f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.LCD)
}

private fun DrawScope.drawBajajDominarDualConsole(ctx: GaugeContext) {
    // Dominar 400 UG: Gerçek 2-Katmanlı Konsol (Üst Ana Gidon Reverse LCD + Alt Yakıt Deposu İkinci LCD Pod)
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val domCyan = Color(0xFF00E5FF)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    // 1. Üst Gidon Birincil Ters LCD
    val topBoxTop = ctx.h * 0.07f
    val topBoxH = (ctx.h - railH) * 0.52f
    drawPanel(ctx.w * 0.07f, topBoxTop, ctx.w * 0.86f, topBoxH, Color(0xFF080E16), domCyan.copy(alpha = 0.55f), u * 1.6f)
    drawBrandMark(ctx.spec, p, ctx.w * 0.12f, topBoxTop + u * 3.5f, u * 3.6f, u, domCyan)
    gText("DOMINAR // PRIMARY LCD", ctx.w * 0.18f, topBoxTop + u * 4.2f, u * 1.55f, domCyan, Paint.Align.LEFT, "monospace", true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.89f, topBoxTop + u * 4.2f, u * 1.85f, Color.White, Paint.Align.RIGHT, "monospace", true)
    drawLinearBar(ctx.w * 0.10f, topBoxTop + u * 7.0f, ctx.w * 0.80f, u * 3.6f, ctx.rpmRatio, domCyan, Color(0xFF152230), ctx.redlineRatio, RL_RED, u * 0.6f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, topBoxTop + topBoxH * 0.72f, u * 18.0f, Color.White, Color(0xFF889BB0), "monospace")

    // 2. Alt Yakıt Deposu İkincil Ekran (Vites + ODO + Trip + Ortalama Tüketim + Menzil)
    val tankTop = topBoxTop + topBoxH + u * 2.0f
    val tankH = max(u * 10.5f, ctx.h - railH - tankTop - u * 2.0f)
    drawPanel(ctx.w * 0.14f, tankTop, ctx.w * 0.72f, tankH, Color(0xFF060A10), Color(0xFF394658), u * 1.5f)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.25f, tankTop + tankH * 0.50f, min(u * 10.5f, tankH * 0.68f), domCyan, Color.White, "monospace")
    gText("TANK DISPLAY  •  ODO ${ctx.formatOdometer()}", ctx.w * 0.58f, tankTop + tankH * 0.38f, u * 1.75f, Color.White, Paint.Align.CENTER, "monospace", true)
    gText("TRIP ${ctx.formatTrip()}  •  AVG ${ctx.formatAvgFuel()}  •  RNG ${ctx.formatRange()}", ctx.w * 0.58f, tankTop + tankH * 0.74f, u * 1.55f, domCyan, Paint.Align.CENTER, "monospace", true)

    drawStatusRail(ctx, RailStyle.LCD)
}

/* =====================================================================================
 *  BMW MOTORRAD 6.5" CONNECTIVITY TFT — Core 1 / Core 2 / Core 3 / Pure Ride
 * ===================================================================================== */
private fun DrawScope.drawBmwCore1MPowerTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.6f, ctx.w * (if (ctx.wide) 0.28f else 0.42f), u)
    // BMW M-Motorsport üç renk şeridi (Açık Mavi / Lacivert / Kırmızı)
    val headerY = ctx.h * (if (ctx.wide) 0.055f else 0.048f)
    drawRect(Color(0xFF0099FF), Offset(ctx.w * 0.04f, headerY), Size(u * 2.2f, u * 1.4f))
    drawRect(Color(0xFF003380), Offset(ctx.w * 0.04f + u * 2.5f, headerY), Size(u * 2.2f, u * 1.4f))
    drawRect(Color(0xFFE60012), Offset(ctx.w * 0.04f + u * 5.0f, headerY), Size(u * 2.2f, u * 1.4f))
    gText("M MOTORSPORT // CORE 1", ctx.w * 0.04f + u * 8.5f, headerY + u * 1.5f, u * 1.75f, p.fg, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.95f, headerY + u * 1.8f, u * 2.8f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    val pts = if (ctx.wide) {
        rememberBoomerangCurve(
            Offset(ctx.w * 0.06f, ctx.h * 0.68f),
            Offset(ctx.w * 0.13f, ctx.h * 0.19f),
            Offset(ctx.w * 0.93f, ctx.h * 0.19f)
        )
    } else {
        rememberBoomerangCurve(
            Offset(ctx.w * 0.06f, ctx.h * 0.28f),
            Offset(ctx.w * 0.15f, ctx.h * 0.135f),
            Offset(ctx.w * 0.94f, ctx.h * 0.135f)
        )
    }
    drawPolylineBar(pts, 52, ctx.rpmRatio, u * 4.2f, p.accent, p.track, ctx.redlineRatio, ctx.peakRatio, u)
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    val step = if (maxK >= 14) 2 else 1
    var k = 0
    while (k <= maxK) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + u * 4.8f, u * 2.0f, if (frac >= ctx.redlineRatio) RL_RED else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        k += step
    }

    if (ctx.wide) {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.44f, ctx.h * 0.62f, u * 25.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.78f, ctx.h * 0.52f, u * 14.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        gText("RACE PRO 1", ctx.w * 0.78f, ctx.h * 0.665f, u * 2.0f, Color(0xFFE60012), Paint.Align.CENTER, ctx.type.label, true, spacing = 0.12f)

        // Sol alt: DTC + Lean + ODO/TRIP
        drawPanel(ctx.w * 0.14f, ctx.h * 0.69f, ctx.w * 0.78f, ctx.h * 0.14f, p, accentEdge = true)
        gText("LEAN  L ${abs(min(0.0, t.leanAngleDeg)).roundToInt()}° / R ${max(0.0, t.leanAngleDeg).roundToInt()}°", ctx.w * 0.165f, ctx.h * 0.745f, u * 1.85f, Color(0xFF0099FF), Paint.Align.LEFT, ctx.type.digits, true)
        gText("DTC +2   •   ABS PRO   •   DBC ${String.format(java.util.Locale.US, "%.1f", t.longitudinalG)}G", ctx.w * 0.165f, ctx.h * 0.798f, u * 1.50f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.895f, ctx.h * 0.745f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
        gText("TRIP A ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}", ctx.w * 0.895f, ctx.h * 0.798f, u * 1.50f, p.accent, Paint.Align.RIGHT, ctx.type.label, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.36f, ctx.h * 0.44f, u * 23.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.78f, ctx.h * 0.40f, u * 14.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        gText("RACE PRO 1", ctx.w * 0.78f, ctx.h * 0.50f, u * 2.0f, Color(0xFFE60012), Paint.Align.CENTER, ctx.type.label, true, spacing = 0.12f)

        drawLeanArc(ctx.w * 0.50f, ctx.h * 0.59f, u * 11.0f, t.leanAngleDeg, Color(0xFF0099FF), p.track, u)
        drawPanel(ctx.w * 0.06f, ctx.h * 0.65f, ctx.w * 0.88f, ctx.h * 0.17f, p, accentEdge = true)
        gText("LEAN L ${abs(min(0.0, t.leanAngleDeg)).roundToInt()}° / R ${max(0.0, t.leanAngleDeg).roundToInt()}°", ctx.w * 0.095f, ctx.h * 0.715f, u * 1.85f, Color(0xFF0099FF), Paint.Align.LEFT, ctx.type.digits, true)
        gText("DTC +2  •  ABS PRO  •  DBC ${String.format(java.util.Locale.US, "%.1f", t.longitudinalG)}G", ctx.w * 0.095f, ctx.h * 0.78f, u * 1.55f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.905f, ctx.h * 0.715f, u * 1.95f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
        gText("TRIP A ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}", ctx.w * 0.905f, ctx.h * 0.78f, u * 1.50f, p.accent, Paint.Align.RIGHT, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  KTM GEN 1 / NG TURUNCU ARKA IŞIKLI LCD — 125 / 200 / 390 Duke Gen 1 & RC 200
 * ===================================================================================== */
private fun DrawScope.drawKtmGen1AmberLcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * (if (ctx.wide) 0.24f else 0.42f), u)

    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    if (ctx.wide) {
        // Sol dikey 8-segment yakıt barı + Sağ dikey 8-segment hararet barı (Orijinal KTM Gen 1 LCD)
        val barTop = ctx.h * 0.18f
        val barH = ctx.h * 0.58f
        drawLinearBar(ctx.w * 0.055f, barTop, u * 3.0f, barH, t.fuelLevelRatio, p.fg, p.track, 0.90f, RL_RED, u * 0.4f, vertical = true)
        gText("FUEL", ctx.w * 0.055f + u * 1.5f, barTop - u * 1.5f, u * 1.45f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
        val tempRatio = ((t.coolantTempCelsius - 40.0) / 80.0).toFloat().coerceIn(0f, 1f)
        drawLinearBar(ctx.w * 0.945f - u * 3.0f, barTop, u * 3.0f, barH, tempRatio, p.fg, p.track, 0.85f, RL_RED, u * 0.4f, vertical = true)
        gText("TEMP", ctx.w * 0.945f - u * 1.5f, barTop - u * 1.5f, u * 1.45f, p.fg, Paint.Align.CENTER, ctx.type.label, true)

        // Üst yatay-kavisli 44-segment LCD bar
        val pts = rememberTvsRtrCurve(
            Offset(ctx.w * 0.13f, ctx.h * 0.30f),
            Offset(ctx.w * 0.25f, ctx.h * 0.16f),
            Offset(ctx.w * 0.87f, ctx.h * 0.16f)
        )
        drawTvsSegmentedBar(pts, 44, ctx.rpmRatio, u * 4.2f, p.fg, p.track, ctx.redlineRatio, ctx.peakRatio)
        for (k in 0..maxK step 2) {
            val frac = k / maxK.toFloat()
            val pt = pointOnCurve(pts, frac)
            gText(k.toString(), pt.x, pt.y + u * 4.8f, u * 1.95f, p.fg, Paint.Align.CENTER, "monospace", true)
        }

        drawGearPod(ctx.w * 0.24f, ctx.h * 0.52f, u * 12.5f, p, t, ctx.isNeutral, u, "monospace", GearPodShape.SQUARE)
        gText("GEAR", ctx.w * 0.24f, ctx.h * 0.65f, u * 1.55f, p.fg, Paint.Align.CENTER, "monospace", true)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.56f, ctx.h * 0.60f, u * 25.0f, p.fg, p.muted, "monospace")

        gText("ODO ${ctx.formatOdometer()}   •   TRIP 1 ${ctx.formatTrip()}   •   SEANS ${ctx.formatSession()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.85f, p.fg, Paint.Align.CENTER, "monospace", true)
    } else {
        // Portre KTM Gen 1 Amber LCD: üst kavisli devir barı, yanlarda dikey FUEL/TEMP, ortada hız + altta vites & ODO
        val pts = rememberTvsRtrCurve(
            Offset(ctx.w * 0.10f, ctx.h * 0.22f),
            Offset(ctx.w * 0.24f, ctx.h * 0.13f),
            Offset(ctx.w * 0.90f, ctx.h * 0.13f)
        )
        drawTvsSegmentedBar(pts, 44, ctx.rpmRatio, u * 4.0f, p.fg, p.track, ctx.redlineRatio, ctx.peakRatio)
        for (k in 0..maxK step 2) {
            val frac = k / maxK.toFloat()
            val pt = pointOnCurve(pts, frac)
            gText(k.toString(), pt.x, pt.y + u * 4.5f, u * 1.9f, p.fg, Paint.Align.CENTER, "monospace", true)
        }

        val barTop = ctx.h * 0.30f
        val barH = ctx.h * 0.28f
        drawLinearBar(ctx.w * 0.065f, barTop, u * 3.4f, barH, t.fuelLevelRatio, p.fg, p.track, 0.90f, RL_RED, u * 0.4f, vertical = true)
        gText("FUEL", ctx.w * 0.065f + u * 1.7f, barTop - u * 1.6f, u * 1.45f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
        val tempRatio = ((t.coolantTempCelsius - 40.0) / 80.0).toFloat().coerceIn(0f, 1f)
        drawLinearBar(ctx.w * 0.935f - u * 3.4f, barTop, u * 3.4f, barH, tempRatio, p.fg, p.track, 0.85f, RL_RED, u * 0.4f, vertical = true)
        gText("TEMP", ctx.w * 0.935f - u * 1.7f, barTop - u * 1.6f, u * 1.45f, p.fg, Paint.Align.CENTER, ctx.type.label, true)

        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.44f, u * 26.5f, p.fg, p.muted, "monospace")

        drawGearPod(ctx.w * 0.23f, ctx.h * 0.68f, u * 14.0f, p, t, ctx.isNeutral, u, "monospace", GearPodShape.SQUARE)
        gText("GEAR", ctx.w * 0.23f, ctx.h * 0.78f, u * 1.6f, p.fg, Paint.Align.CENTER, "monospace", true)
        drawPanel(ctx.w * 0.40f, ctx.h * 0.61f, ctx.w * 0.535f, ctx.h * 0.19f, p, accentEdge = true)
        gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.435f, ctx.h * 0.665f, u * 2.15f, p.fg, Paint.Align.LEFT, "monospace", true)
        gText("TRIP 1  ${ctx.formatTrip()}", ctx.w * 0.435f, ctx.h * 0.72f, u * 2.0f, p.accent, Paint.Align.LEFT, "monospace", true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.435f, ctx.h * 0.775f, u * 1.95f, p.fg, Paint.Align.LEFT, "monospace", true)
    }
    drawStatusRail(ctx, RailStyle.LCD)
}

/* =====================================================================================
 *  YAMAHA YZR-M1 / R25 / R3 GEN 2 / R15M V4 — Dikdörtgen Yarış LCD
 * ===================================================================================== */
private fun DrawScope.drawYamahaR3M1RaceLcd(ctx: GaugeContext, isTrack: Boolean) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.0f, ctx.w * (if (ctx.wide) 0.30f else 0.42f), u)

    // Üst tam genişlik düz M1 yarış devir barı
    val left = ctx.w * 0.06f
    val width = ctx.w * 0.88f
    val top = ctx.h * (if (ctx.wide) 0.145f else 0.115f)
    val barH = u * (if (ctx.wide) 5.2f else 4.6f)
    val segs = 48
    val segW = width / segs
    val filled = (ctx.rpmRatio * segs).roundToInt()
    for (i in 0 until segs) {
        val inRed = i.toFloat() / segs >= ctx.redlineRatio
        val c = when {
            i < filled && inRed -> RL_RED
            i < filled -> p.fg
            inRed -> RL_RED.copy(alpha = 0.22f)
            else -> p.track
        }
        drawRect(c, Offset(left + i * segW, top), Size(segW * 0.78f, barH))
    }
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    for (k in 0..maxK) {
        val x = left + width * (k / maxK.toFloat())
        gText(k.toString(), x, top + barH + u * 3.4f, u * 1.85f, if (k * 1000 >= ctx.redlineRpm) RL_RED else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
    }

    if (ctx.wide) {
        // Sol büyük Vites kutusu + Merkez Hız + Sağ VVA / Yakıt / ODO
        drawGearPod(ctx.w * 0.17f, ctx.h * 0.54f, u * 14.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        gText("GEAR", ctx.w * 0.17f, ctx.h * 0.68f, u * 1.6f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.48f, ctx.h * 0.64f, u * 26.5f, p.fg, p.muted, ctx.type.digits)
        if (ctx.spec.skinId.contains("r15") || ctx.spec.skinId.contains("mt15")) {
            val vvaActive = ctx.mappedRpm >= 7400.0
            drawRoundRect(if (vvaActive) p.accent else p.track, Offset(ctx.w * 0.42f, ctx.h * 0.70f), Size(ctx.w * 0.12f, u * 3.6f), CornerRadius(u * 0.6f, u * 0.6f))
            gText("VVA", ctx.w * 0.48f, ctx.h * 0.70f + u * 2.5f, u * 1.75f, if (vvaActive) Color.Black else p.muted, Paint.Align.CENTER, ctx.type.label, true)
        }

        drawPanel(ctx.w * 0.71f, ctx.h * 0.35f, ctx.w * 0.23f, ctx.h * 0.45f, p, accentEdge = true)
        gText(if (isTrack) "PEAK ${ctx.formatPeakSpeed()}" else "ODO ${ctx.formatOdometer()}", ctx.w * 0.735f, ctx.h * 0.43f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.735f, ctx.h * 0.50f, u * 1.85f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("0-100  ${ctx.formatBest0to100()}", ctx.w * 0.735f, ctx.h * 0.565f, u * 1.60f, RL_GREEN, Paint.Align.LEFT, ctx.type.label, true)
        drawTemperatureAndFuel(ctx, ctx.w * 0.735f, ctx.h * 0.64f, ctx.w * 0.16f, compact = true)
    } else {
        // Portre Yamaha YZR-M1 / R25 / R3 / R15M: üstte devir, ortada büyük Vites + Hız + VVA, altta tam genişlik bilgi paneli
        drawGearPod(ctx.w * 0.22f, ctx.h * 0.34f, u * 15.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        gText("GEAR", ctx.w * 0.22f, ctx.h * 0.43f, u * 1.65f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.61f, ctx.h * 0.39f, u * 27.5f, p.fg, p.muted, ctx.type.digits)
        if (ctx.spec.skinId.contains("r15") || ctx.spec.skinId.contains("mt15")) {
            val vvaActive = ctx.mappedRpm >= 7400.0
            drawRoundRect(if (vvaActive) p.accent else p.track, Offset(ctx.w * 0.49f, ctx.h * 0.44f), Size(ctx.w * 0.24f, u * 3.8f), CornerRadius(u * 0.6f, u * 0.6f))
            gText("VVA", ctx.w * 0.61f, ctx.h * 0.44f + u * 2.65f, u * 1.85f, if (vvaActive) Color.Black else p.muted, Paint.Align.CENTER, ctx.type.label, true)
        }

        drawPanel(ctx.w * 0.06f, ctx.h * 0.52f, ctx.w * 0.88f, ctx.h * 0.27f, p, accentEdge = true)
        gText(if (isTrack) "PEAK  ${ctx.formatPeakSpeed()}" else "ODO  ${ctx.formatOdometer()}", ctx.w * 0.10f, ctx.h * 0.585f, u * 2.25f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.10f, ctx.h * 0.655f, u * 2.0f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("0-100  ${ctx.formatBest0to100()}", ctx.w * 0.10f, ctx.h * 0.725f, u * 1.85f, RL_GREEN, Paint.Align.LEFT, ctx.type.label, true)
        drawTemperatureAndFuel(ctx, ctx.w * 0.56f, ctx.h * 0.59f, ctx.w * 0.34f, compact = true)
    }

    drawStatusRail(ctx, RailStyle.LCD)
}

/* =====================================================================================
 *  SUZUKI GSX-R1000R L7 / KATANA / SV650 — Negatif S-DMS Yarış LCD
 * ===================================================================================== */
private fun DrawScope.drawSuzukiGsxrL7NegativeLcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.0f, ctx.w * (if (ctx.wide) 0.30f else 0.42f), u)

    val pts = if (ctx.wide) {
        rememberTvsRtrCurve(
            Offset(ctx.w * 0.06f, ctx.h * 0.31f),
            Offset(ctx.w * 0.24f, ctx.h * 0.145f),
            Offset(ctx.w * 0.94f, ctx.h * 0.145f)
        )
    } else {
        rememberTvsRtrCurve(
            Offset(ctx.w * 0.06f, ctx.h * 0.22f),
            Offset(ctx.w * 0.24f, ctx.h * 0.12f),
            Offset(ctx.w * 0.94f, ctx.h * 0.12f)
        )
    }
    drawTvsSegmentedBar(pts, 54, ctx.rpmRatio, u * 4.4f, p.accent, p.track, ctx.redlineRatio, ctx.peakRatio)
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    var k = 0
    while (k <= maxK) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + u * 4.8f, u * 1.95f, if (frac >= ctx.redlineRatio) RL_RED else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        k += if (maxK >= 13) 2 else 1
    }

    if (ctx.wide) {
        // Sol: S-DMS ve TC Durum kutuları
        drawPanel(ctx.w * 0.055f, ctx.h * 0.40f, ctx.w * 0.17f, ctx.h * 0.38f, p, accentEdge = true)
        gText("S-DMS", ctx.w * 0.14f, ctx.h * 0.46f, u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        gText(ctx.subMode.uppercase().take(3), ctx.w * 0.14f, ctx.h * 0.56f, u * 5.4f, p.accent, Paint.Align.CENTER, ctx.type.digits, true)
        gText(if (t.tractionControlInterventionActive) "TC ACTIVE" else "TC READY", ctx.w * 0.14f, ctx.h * 0.66f, u * 1.75f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
        gText("RNG ${ctx.formatRange()}", ctx.w * 0.14f, ctx.h * 0.73f, u * 1.50f, RL_GREEN, Paint.Align.CENTER, ctx.type.label, true)

        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.47f, ctx.h * 0.63f, u * 27.0f, p.fg, p.muted, ctx.type.digits)
        gText("ODO ${ctx.formatOdometer()}   •   TRIP A ${ctx.formatTrip()}   •   SEANS ${ctx.formatSession()}", ctx.w * 0.47f, ctx.h * 0.76f, u * 1.70f, p.accent, Paint.Align.CENTER, ctx.type.digits, true)

        drawGearPod(ctx.w * 0.82f, ctx.h * 0.52f, u * 15.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.NONE)
        gText("GEAR", ctx.w * 0.82f, ctx.h * 0.66f, u * 1.55f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.20f)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.36f, ctx.h * 0.41f, u * 24.5f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.79f, ctx.h * 0.37f, u * 15.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.NONE)
        gText("GEAR", ctx.w * 0.79f, ctx.h * 0.46f, u * 1.55f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.20f)

        drawPanel(ctx.w * 0.06f, ctx.h * 0.54f, ctx.w * 0.88f, ctx.h * 0.165f, p, accentEdge = true)
        gText("S-DMS", ctx.w * 0.18f, ctx.h * 0.595f, u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        gText(ctx.subMode.uppercase().take(3), ctx.w * 0.18f, ctx.h * 0.675f, u * 4.8f, p.accent, Paint.Align.CENTER, ctx.type.digits, true)
        gText(if (t.tractionControlInterventionActive) "TC ACTIVE" else "TC READY", ctx.w * 0.54f, ctx.h * 0.61f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
        gText("RNG ${ctx.formatRange()}", ctx.w * 0.54f, ctx.h * 0.675f, u * 1.75f, RL_GREEN, Paint.Align.CENTER, ctx.type.digits, true)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", ctx.w * 0.84f, ctx.h * 0.645f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)

        gText("ODO ${ctx.formatOdometer()}  •  TRIP A ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.75f, p.accent, Paint.Align.CENTER, ctx.type.digits, true)
    }

    drawStatusRail(ctx, RailStyle.LCD)
}

/* =====================================================================================
 *  TRIUMPH 765 RS / TIGER — Stylus / High Contrast / Chronos / Grid + Trident/Daytona
 * ===================================================================================== */
private fun DrawScope.drawTriumphMultiStyleTft(ctx: GaugeContext) {
    drawTriumphMultiStyleTftReal(ctx)
}

private fun DrawScope.drawTriumphTridentDaytonaHybrid(ctx: GaugeContext) {
    // Trident 660 / Daytona 660: Üst yarı monokrom beyaz segmentli yay + Alt yarı kare renkli TFT
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val cx = ctx.w * 0.50f
    val cy = ctx.h * 0.52f
    val r = min(ctx.w * 0.34f, ctx.h * 0.40f)
    drawSegmentedArc(cx, cy, r, 195f, 150f, 42, ctx.rpmRatio, u * 4.2f, Color.White, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
    drawGearPod(ctx.w * 0.25f, ctx.h * 0.46f, u * 12.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.ROUND)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, ctx.h * 0.50f, u * 21.0f, p.fg, p.muted, ctx.type.digits)

    drawPanel(ctx.w * 0.26f, ctx.h * 0.58f, ctx.w * 0.48f, ctx.h * 0.22f, p, accentEdge = true)
    gText("TRIUMPH // ${ctx.subMode.uppercase()}", cx, ctx.h * 0.635f, u * 1.65f, p.accent, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.12f)
    gText("ODO ${ctx.formatOdometer()}   •   TRIP A ${ctx.formatTrip()}", cx, ctx.h * 0.705f, u * 2.0f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
    gText("SEANS ${ctx.formatSession()}   •   ${t.coolantTempCelsius.roundToInt()}°C   •   FUEL ${(t.fuelLevelRatio * 100).roundToInt()}%", cx, ctx.h * 0.765f, u * 1.50f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  CFMOTO / QJMOTOR / VOGE / ZONTES / KOVE — TPMS (Lastik Basıncı & Sıcaklığı) + Çift Mod TFT
 * ===================================================================================== */
private fun DrawScope.drawTpmsSupersportTft(ctx: GaugeContext) {
    val sub = ctx.subMode.lowercase()
    if (sub.contains("eco") || sub.contains("street") || sub.contains("normal")) {
        drawCircularRingTft(ctx)
    } else {
        drawCurvedSweepTft(ctx)
    }
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val tpmsW = if (ctx.wide) ctx.w * 0.195f else ctx.w * 0.46f
    val tpmsH = if (ctx.wide) ctx.h * 0.155f else ctx.h * 0.088f
    val left = if (ctx.wide) ctx.w * 0.035f else ctx.w * 0.27f
    val top = if (ctx.wide) ctx.h * 0.20f else ctx.h * 0.095f
    drawPanel(left, top, tpmsW, tpmsH, p, accentEdge = true)
    gText("LIVE DRAG & LEAN", left + u * 1.6f, top + tpmsH * 0.30f, u * 1.30f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.08f)
    gText("0-100  ${ctx.formatBest0to100()}", left + u * 1.6f, top + tpmsH * 0.60f, u * 1.55f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°  •  ${ctx.formatBattery()}", left + u * 1.6f, top + tpmsH * 0.88f, u * 1.50f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
}

private fun DrawScope.drawBmwCore2AnalogTft(ctx: GaugeContext) {
    // BMW Core 2: Merkezde dev yuvarlak M halkası + Sol/Sağ yatış barları + Sağda/Altta dev Vites
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.6f, ctx.w * (if (ctx.wide) 0.28f else 0.42f), u)
    val cx = if (ctx.wide) ctx.w * 0.42f else ctx.w * 0.50f
    val cy = if (ctx.wide) ctx.h * 0.49f else ctx.h * 0.36f
    val r = min(ctx.w * (if (ctx.wide) 0.28f else 0.36f), ctx.h * (if (ctx.wide) 0.36f else 0.25f))
    drawSegmentedArc(cx, cy, r, 135f, 270f, 54, ctx.rpmRatio, u * 4.2f, Color(0xFF0099FF), p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.85f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 4.5f, u * 19.0f, p.fg, p.muted, ctx.type.digits)
    gText("CORE 2 // M ANALOG", cx, cy - r * 0.45f, u * 1.6f, Color(0xFF0099FF), Paint.Align.CENTER, ctx.type.label, true)
    drawLeanArc(cx, cy, r + u * 4.2f, t.leanAngleDeg, Color(0xFFE60012), p.track, u)

    if (ctx.wide) {
        drawGearPod(ctx.w * 0.80f, ctx.h * 0.38f, u * 14.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        drawPanel(ctx.w * 0.68f, ctx.h * 0.56f, ctx.w * 0.26f, ctx.h * 0.25f, p, accentEdge = true)
        gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.705f, ctx.h * 0.625f, u * 2.0f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.705f, ctx.h * 0.685f, u * 1.8f, Color(0xFF0099FF), Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.705f, ctx.h * 0.740f, u * 1.55f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("DTC INTERVENTION  ${String.format(java.util.Locale.US, "%.1f", t.wheelSlipRatioLambda * 100)}%", ctx.w * 0.705f, ctx.h * 0.785f, u * 1.35f, Color(0xFFE60012), Paint.Align.LEFT, ctx.type.label, true)
    } else {
        drawGearPod(ctx.w * 0.20f, ctx.h * 0.72f, u * 13.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        drawPanel(ctx.w * 0.35f, ctx.h * 0.63f, ctx.w * 0.59f, ctx.h * 0.19f, p, accentEdge = true)
        gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.385f, ctx.h * 0.68f, u * 1.95f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.385f, ctx.h * 0.73f, u * 1.75f, Color(0xFF0099FF), Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.385f, ctx.h * 0.775f, u * 1.5f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("DTC ${String.format(java.util.Locale.US, "%.1f", t.wheelSlipRatioLambda * 100)}%", ctx.w * 0.91f, ctx.h * 0.775f, u * 1.5f, Color(0xFFE60012), Paint.Align.RIGHT, ctx.type.label, true)
    }
    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  KTM GEN 1 / NG TURUNCU ARKA IŞIKLI LCD — 125 / 200 / 390 Duke Gen 1 & RC 200
 * ===================================================================================== */

/* =====================================================================================
 *  YAMAHA YZR-M1 / R25 / R3 GEN 2 / R15M V4 — Dikdörtgen Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  SUZUKI GSX-R1000R L7 / KATANA / SV650 — Negatif S-DMS Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  TRIUMPH 765 RS / TIGER — Stylus / High Contrast / Chronos / Grid + Trident/Daytona
 * ===================================================================================== */


/* =====================================================================================
 *  CFMOTO / QJMOTOR / VOGE / ZONTES / KOVE — TPMS (Lastik Basıncı & Sıcaklığı) + Çift Mod TFT
 * ===================================================================================== */

private fun DrawScope.drawBmwCore3TrackLapTft(ctx: GaugeContext) {
    drawBmwCore3TrackLapTftReal(ctx)
}

private fun DrawScope.drawBmwPureRideTft(ctx: GaugeContext) {
    drawBmwPureRideTftReal(ctx)
}

/* =====================================================================================
 *  YAMAHA YZF-R1 / R1M / MT-10 — Street Mode vs Track Zoom Mode + MT-09 Gen 4
 * ===================================================================================== */
private fun DrawScope.drawYamahaR1StreetModeTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.2f, ctx.w * (if (ctx.wide) 0.32f else 0.42f), u)
    val headerY = ctx.h * (if (ctx.wide) 0.088f else 0.072f)
    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, headerY - u * 0.6f, u * 4.2f, u, p.accent)
    gText("${ctx.spec.modelName.substringBefore(" (").uppercase()} // STREET MODE", ctx.w * 0.12f, headerY, u * 1.65f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

    // Yamaha R1 imza yatay-kavisli devir barı (Yeşil -> Kehribar -> Kırmızı)
    val pts = if (ctx.wide) {
        rememberTvsRtrCurve(
            Offset(ctx.w * 0.055f, ctx.h * 0.34f),
            Offset(ctx.w * 0.20f, ctx.h * 0.16f),
            Offset(ctx.w * 0.94f, ctx.h * 0.16f)
        )
    } else {
        rememberTvsRtrCurve(
            Offset(ctx.w * 0.06f, ctx.h * 0.22f),
            Offset(ctx.w * 0.22f, ctx.h * 0.125f),
            Offset(ctx.w * 0.94f, ctx.h * 0.125f)
        )
    }
    val r1BarColor = when {
        ctx.rpmRatio >= ctx.redlineRatio -> RL_RED
        ctx.rpmRatio >= 0.68f -> RL_AMBER
        else -> Color(0xFF00C853)
    }
    drawTvsSegmentedBar(pts, 56, ctx.rpmRatio, u * 4.4f, r1BarColor, p.track, ctx.redlineRatio, ctx.peakRatio)
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    var k = 0
    while (k <= maxK) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + u * 5.0f, u * 1.95f, if (frac >= ctx.redlineRatio) RL_RED else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        k += 2
    }

    val yrc = listOf(
        ctx.subMode.uppercase().take(7),
        "TCS ${if (t.tractionControlInterventionActive) "ACT" else "ON"}",
        "LIF ${if (t.isWheelieActive) "${t.wheelieAngleDeg.roundToInt()}°" else "ARM"}",
        "ABS ${if (t.absInterventionActive) "ACT" else "ON"}",
        ctx.formatBattery(),
        formatTemp(t.coolantTempCelsius, ctx.useFahrenheit),
        "RNG ${ctx.formatRange()}"
    )

    if (ctx.wide) {
        // Sol: Hız, Orta-Sağ: Dev Vites Kutusu
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.34f, ctx.h * 0.60f, u * 25.5f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.68f, ctx.h * 0.49f, u * 14.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        gText("GEAR", ctx.w * 0.68f, ctx.h * 0.625f, u * 1.55f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)

        // Sağ: ODO / TRIP / SEANS
        drawPanel(ctx.w * 0.77f, ctx.h * 0.34f, ctx.w * 0.19f, ctx.h * 0.30f, p, accentEdge = true)
        gText("ODO", ctx.w * 0.79f, ctx.h * 0.39f, u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText(ctx.formatOdometer(), ctx.w * 0.79f, ctx.h * 0.45f, u * 2.5f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.79f, ctx.h * 0.52f, u * 1.65f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.79f, ctx.h * 0.58f, u * 1.50f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

        // Alt Yamaha YRC Canlı Telemetri Kartları (7 tek sıra)
        val boxW = (ctx.w * 0.90f) / yrc.size
        val boxTop = ctx.h * 0.70f
        yrc.forEachIndexed { idx, tag ->
            val bx = ctx.w * 0.05f + idx * boxW
            drawRoundRect(p.panel, Offset(bx + u * 0.4f, boxTop), Size(boxW - u * 0.8f, ctx.h * 0.11f), CornerRadius(u * 0.7f, u * 0.7f))
            drawRoundRect(p.accent.copy(alpha = 0.45f), Offset(bx + u * 0.4f, boxTop), Size(boxW - u * 0.8f, ctx.h * 0.11f), CornerRadius(u * 0.7f, u * 0.7f), style = Stroke(width = u * 0.2f))
            gText(tag, bx + boxW * 0.5f, boxTop + ctx.h * 0.070f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
        }
    } else {
        // Portre R1 / MT-10 / R9 Street Mode: Hız + Vites yan yana, altta ODO/TRIP/SEANS şeridi ve 2 sıralı YRC ızgarası
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.36f, ctx.h * 0.375f, u * 24.5f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.78f, ctx.h * 0.33f, u * 14.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        gText("GEAR", ctx.w * 0.78f, ctx.h * 0.42f, u * 1.55f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)

        drawPanel(ctx.w * 0.06f, ctx.h * 0.47f, ctx.w * 0.88f, ctx.h * 0.115f, p, accentEdge = true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.095f, ctx.h * 0.525f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.095f, ctx.h * 0.565f, u * 1.75f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.905f, ctx.h * 0.55f, u * 1.75f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)

        val row1 = yrc.take(4)
        val row2 = yrc.drop(4)
        val w1 = (ctx.w * 0.88f) / row1.size
        val top1 = ctx.h * 0.625f
        val bh = ctx.h * 0.075f
        row1.forEachIndexed { idx, tag ->
            val bx = ctx.w * 0.06f + idx * w1
            drawRoundRect(p.panel, Offset(bx + u * 0.45f, top1), Size(w1 - u * 0.9f, bh), CornerRadius(u * 0.7f, u * 0.7f))
            drawRoundRect(p.accent.copy(alpha = 0.45f), Offset(bx + u * 0.45f, top1), Size(w1 - u * 0.9f, bh), CornerRadius(u * 0.7f, u * 0.7f), style = Stroke(width = u * 0.2f))
            gText(tag, bx + w1 * 0.5f, top1 + bh * 0.63f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
        }
        val w2 = (ctx.w * 0.88f) / row2.size
        val top2 = ctx.h * 0.72f
        row2.forEachIndexed { idx, tag ->
            val bx = ctx.w * 0.06f + idx * w2
            drawRoundRect(p.panel, Offset(bx + u * 0.45f, top2), Size(w2 - u * 0.9f, bh), CornerRadius(u * 0.7f, u * 0.7f))
            drawRoundRect(p.accent.copy(alpha = 0.45f), Offset(bx + u * 0.45f, top2), Size(w2 - u * 0.9f, bh), CornerRadius(u * 0.7f, u * 0.7f), style = Stroke(width = u * 0.2f))
            gText(tag, bx + w2 * 0.5f, top2 + bh * 0.63f, u * 1.95f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
        }
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  KTM GEN 1 / NG TURUNCU ARKA IŞIKLI LCD — 125 / 200 / 390 Duke Gen 1 & RC 200
 * ===================================================================================== */

/* =====================================================================================
 *  YAMAHA YZR-M1 / R25 / R3 GEN 2 / R15M V4 — Dikdörtgen Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  SUZUKI GSX-R1000R L7 / KATANA / SV650 — Negatif S-DMS Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  TRIUMPH 765 RS / TIGER — Stylus / High Contrast / Chronos / Grid + Trident/Daytona
 * ===================================================================================== */


/* =====================================================================================
 *  CFMOTO / QJMOTOR / VOGE / ZONTES / KOVE — TPMS (Lastik Basıncı & Sıcaklığı) + Çift Mod TFT
 * ===================================================================================== */

private fun DrawScope.drawYamahaR1TrackModeTft(ctx: GaugeContext) {
    // Yamaha R1 Track Mode: Devir barı 8.000 RPM'den başlar (Zoomed Race Tach), merkezde LAP/0-100 + Vites!
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.2f, ctx.w * (if (ctx.wide) 0.34f else 0.42f), u)
    gText("YRC TRACK MODE // ZOOM 8-16k RPM", ctx.w * 0.055f, ctx.h * (if (ctx.wide) 0.085f else 0.072f), u * 1.75f, Color(0xFF00E5FF), Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)

    val pts = if (ctx.wide) {
        rememberTvsRtrCurve(
            Offset(ctx.w * 0.055f, ctx.h * 0.28f),
            Offset(ctx.w * 0.18f, ctx.h * 0.15f),
            Offset(ctx.w * 0.945f, ctx.h * 0.15f)
        )
    } else {
        rememberTvsRtrCurve(
            Offset(ctx.w * 0.06f, ctx.h * 0.205f),
            Offset(ctx.w * 0.20f, ctx.h * 0.12f),
            Offset(ctx.w * 0.94f, ctx.h * 0.12f)
        )
    }
    drawTvsSegmentedBar(pts, 60, ctx.rpmRatio, u * 4.8f, Color(0xFF00E5FF), p.track, ctx.redlineRatio, ctx.peakRatio)
    val startK = 8
    val endK = (ctx.maxRpm / 1000.0).roundToInt().coerceAtLeast(14)
    for (k in startK..endK) {
        val frac = (k - startK) / (endK - startK).toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + u * 5.2f, u * 2.15f, if (k * 1000 >= ctx.redlineRpm) RL_RED else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
    }

    if (ctx.wide) {
        // Merkezde Dev Vites + 0-100 & Peak Telemetry
        drawGearPod(ctx.w * 0.22f, ctx.h * 0.56f, u * 17.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.54f, ctx.h * 0.54f, u * 22.0f, p.fg, p.muted, ctx.type.digits)
        gText("0-100  ${ctx.formatBest0to100()}   •   ${ctx.formatRideTime()}", ctx.w * 0.54f, ctx.h * 0.68f, u * 3.1f, Color(0xFF00C853), Paint.Align.CENTER, ctx.type.digits, true)
        gText("ODO ${ctx.formatOdometer()}  •  TRIP ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}", ctx.w * 0.54f, ctx.h * 0.77f, u * 1.75f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)

        drawPanel(ctx.w * 0.78f, ctx.h * 0.34f, ctx.w * 0.17f, ctx.h * 0.46f, p, accentEdge = true)
        gText("BRAKE", ctx.w * 0.865f, ctx.h * 0.40f, u * 1.4f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawLinearBar(ctx.w * 0.81f, ctx.h * 0.43f, ctx.w * 0.11f, u * 1.8f, abs(min(0.0, t.longitudinalG)).toFloat().coerceIn(0f, 1f), RL_RED, p.track, 0.9f, RL_RED, u * 0.5f)
        gText("ACCEL", ctx.w * 0.865f, ctx.h * 0.53f, u * 1.4f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawLinearBar(ctx.w * 0.81f, ctx.h * 0.56f, ctx.w * 0.11f, u * 1.8f, max(0.0, t.longitudinalG).toFloat().coerceIn(0f, 1f), Color(0xFF00C853), p.track, 0.9f, RL_RED, u * 0.5f)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.865f, ctx.h * 0.70f, u * 2.2f, Color(0xFF00E5FF), Paint.Align.CENTER, ctx.type.digits, true)
    } else {
        // Portre R1 Track Mode: Vites + Hız üst-ortada, Drag & ODO ortada, altta yatay G-Force / Lean telemetri paneli
        drawGearPod(ctx.w * 0.23f, ctx.h * 0.34f, u * 16.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.62f, ctx.h * 0.39f, u * 24.5f, p.fg, p.muted, ctx.type.digits)
        gText("0-100  ${ctx.formatBest0to100()}   •   ${ctx.formatRideTime()}", ctx.w * 0.50f, ctx.h * 0.495f, u * 2.75f, Color(0xFF00C853), Paint.Align.CENTER, ctx.type.digits, true)
        gText("ODO ${ctx.formatOdometer()}  •  TRIP ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}", ctx.w * 0.50f, ctx.h * 0.55f, u * 1.75f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)

        drawPanel(ctx.w * 0.06f, ctx.h * 0.60f, ctx.w * 0.88f, ctx.h * 0.20f, p, accentEdge = true)
        gText("BRAKE", ctx.w * 0.22f, ctx.h * 0.655f, u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawLinearBar(ctx.w * 0.10f, ctx.h * 0.685f, ctx.w * 0.24f, u * 2.1f, abs(min(0.0, t.longitudinalG)).toFloat().coerceIn(0f, 1f), RL_RED, p.track, 0.9f, RL_RED, u * 0.5f)
        gText("ACCEL", ctx.w * 0.52f, ctx.h * 0.655f, u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawLinearBar(ctx.w * 0.40f, ctx.h * 0.685f, ctx.w * 0.24f, u * 2.1f, max(0.0, t.longitudinalG).toFloat().coerceIn(0f, 1f), Color(0xFF00C853), p.track, 0.9f, RL_RED, u * 0.5f)
        gText("LEAN", ctx.w * 0.80f, ctx.h * 0.655f, u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        gText("${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.80f, ctx.h * 0.725f, u * 3.0f, Color(0xFF00E5FF), Paint.Align.CENTER, ctx.type.digits, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  KTM GEN 1 / NG TURUNCU ARKA IŞIKLI LCD — 125 / 200 / 390 Duke Gen 1 & RC 200
 * ===================================================================================== */

/* =====================================================================================
 *  YAMAHA YZR-M1 / R25 / R3 GEN 2 / R15M V4 — Dikdörtgen Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  SUZUKI GSX-R1000R L7 / KATANA / SV650 — Negatif S-DMS Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  TRIUMPH 765 RS / TIGER — Stylus / High Contrast / Chronos / Grid + Trident/Daytona
 * ===================================================================================== */


/* =====================================================================================
 *  CFMOTO / QJMOTOR / VOGE / ZONTES / KOVE — TPMS (Lastik Basıncı & Sıcaklığı) + Çift Mod TFT
 * ===================================================================================== */

private fun DrawScope.drawYamahaMt09Gen4MultiThemeTft(ctx: GaugeContext) {
    drawYamahaMt09Gen4MultiThemeTftReal(ctx)
}

/* =====================================================================================
 *  HONDA FIREBLADE / CBR600RR / CB750 HORNET — Circuit Ring vs Swept Bar vs Mechanic
 * ===================================================================================== */
private fun DrawScope.drawHondaFirebladeCircuitRingTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.2f, ctx.w * (if (ctx.wide) 0.30f else 0.42f), u)

    val cx = ctx.w * 0.50f
    val cy = if (ctx.wide) ctx.h * 0.48f else ctx.h * 0.36f
    val r = min(ctx.w * (if (ctx.wide) 0.28f else 0.36f), ctx.h * (if (ctx.wide) 0.36f else 0.25f))
    val stepRpm = if (ctx.maxRpm <= 11500.0) 1000 else 2000
    drawSegmentedArc(cx, cy, r, 135f, 270f, 56, ctx.rpmRatio, u * 4.1f, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.8f)

    var rpm = 0
    while (rpm <= ctx.maxRpm.roundToInt()) {
        val frac = rpm / ctx.maxRpm.toFloat()
        val pt = polar(cx, cy, r - u * 8.0f, 135f + 270f * frac)
        gTextCenter((rpm / 1000).toString(), pt.x, pt.y, u * 1.95f, if (frac >= ctx.redlineRatio) RL_RED else p.muted, ctx.type.label)
        rpm += stepRpm
    }

    drawGearPod(cx, cy - r * 0.22f, u * 11.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.ROUND)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + r * 0.42f, u * 18.0f, p.fg, p.muted, ctx.type.digits)

    val hrc = listOf("P" to "1", "T" to "2", "W" to "1", "EB" to "2")
    if (ctx.wide) {
        // Sol sütun: HRC P / T / W / EB / Öhlins S1 halkaları
        hrc.forEachIndexed { idx, (k, v) ->
            val y = ctx.h * (0.24f + idx * 0.145f)
            val x = ctx.w * 0.11f
            drawCircle(p.accent.copy(alpha = 0.16f), u * 4.2f, Offset(x, y))
            drawCircle(p.accent, u * 4.2f, Offset(x, y), style = Stroke(width = u * 0.45f))
            gText("$k $v", x, y + u * 0.8f, u * 2.0f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
        }

        // Sağ sütun: HRC Lap + ODO / TRIP / SEANS
        drawPanel(ctx.w * 0.74f, ctx.h * 0.22f, ctx.w * 0.22f, ctx.h * 0.56f, p, accentEdge = true)
        gText("HRC CIRCUIT", ctx.w * 0.765f, ctx.h * 0.28f, u * 1.6f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)
        gText("ODO", ctx.w * 0.765f, ctx.h * 0.35f, u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText(ctx.formatOdometer(), ctx.w * 0.765f, ctx.h * 0.42f, u * 2.8f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.765f, ctx.h * 0.495f, u * 1.75f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.765f, ctx.h * 0.555f, u * 1.55f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("COOLANT  ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", ctx.w * 0.765f, ctx.h * 0.635f, u * 1.65f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
        gText("MODE  ${ctx.subMode.uppercase()}", ctx.w * 0.765f, ctx.h * 0.715f, u * 1.75f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
    } else {
        // Portre Fireblade Circuit: Alt HRC P/T/W/EB dairesel hapları + Telemetri paneli
        hrc.forEachIndexed { idx, (k, v) ->
            val x = ctx.w * (0.17f + idx * 0.22f)
            val y = ctx.h * 0.58f
            drawCircle(p.accent.copy(alpha = 0.16f), u * 4.5f, Offset(x, y))
            drawCircle(p.accent, u * 4.5f, Offset(x, y), style = Stroke(width = u * 0.45f))
            gText("$k $v", x, y + u * 0.8f, u * 2.1f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
        }
        drawPanel(ctx.w * 0.06f, ctx.h * 0.65f, ctx.w * 0.88f, ctx.h * 0.165f, p, accentEdge = true)
        gText("HRC CIRCUIT // ${ctx.subMode.uppercase()}", ctx.w * 0.095f, ctx.h * 0.705f, u * 1.55f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.10f)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.095f, ctx.h * 0.765f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP ${ctx.formatTrip()}", ctx.w * 0.905f, ctx.h * 0.705f, u * 1.8f, p.accent, Paint.Align.RIGHT, ctx.type.digits, true)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}  •  ${ctx.formatSession()}", ctx.w * 0.905f, ctx.h * 0.765f, u * 1.55f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  KTM GEN 1 / NG TURUNCU ARKA IŞIKLI LCD — 125 / 200 / 390 Duke Gen 1 & RC 200
 * ===================================================================================== */

/* =====================================================================================
 *  YAMAHA YZR-M1 / R25 / R3 GEN 2 / R15M V4 — Dikdörtgen Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  SUZUKI GSX-R1000R L7 / KATANA / SV650 — Negatif S-DMS Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  TRIUMPH 765 RS / TIGER — Stylus / High Contrast / Chronos / Grid + Trident/Daytona
 * ===================================================================================== */


/* =====================================================================================
 *  CFMOTO / QJMOTOR / VOGE / ZONTES / KOVE — TPMS (Lastik Basıncı & Sıcaklığı) + Çift Mod TFT
 * ===================================================================================== */

private fun DrawScope.drawHondaSweptBarTft(ctx: GaugeContext) {
    // Honda Fireblade / CBR600RR / Hornet SP Bar Modu: Kavisli HRC üst bar + P/T/W/EB parametre şeridi
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.0f, ctx.w * (if (ctx.wide) 0.30f else 0.42f), u)

    val headerY = ctx.h * (if (ctx.wide) 0.085f else 0.068f)
    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, headerY - u * 0.5f, u * 4.0f, u, p.accent)
    gText("HRC // ${ctx.subMode.uppercase()} BAR", ctx.w * 0.13f, headerY, u * 1.65f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.10f)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, headerY, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    val pts = if (ctx.wide) {
        rememberTvsRtrCurve(
            Offset(ctx.w * 0.055f, ctx.h * 0.36f),
            Offset(ctx.w * 0.18f, ctx.h * 0.16f),
            Offset(ctx.w * 0.945f, ctx.h * 0.16f)
        )
    } else {
        rememberTvsRtrCurve(
            Offset(ctx.w * 0.06f, ctx.h * 0.22f),
            Offset(ctx.w * 0.20f, ctx.h * 0.125f),
            Offset(ctx.w * 0.94f, ctx.h * 0.125f)
        )
    }
    val barColor = when {
        ctx.rpmRatio >= ctx.redlineRatio -> RL_RED
        ctx.rpmRatio >= 0.72f -> Color.White
        else -> p.accent
    }
    drawTvsSegmentedBar(pts, 54, ctx.rpmRatio, u * 4.4f, barColor, p.track, ctx.redlineRatio, ctx.peakRatio)
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    val step = if (maxK >= 12) 2 else 1
    var k = 0
    while (k <= maxK) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + u * 5.0f, u * 1.95f, if (frac >= ctx.redlineRatio) RL_RED else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        k += step
    }

    if (ctx.wide) {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.34f, ctx.h * 0.56f, u * 24.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.65f, ctx.h * 0.48f, u * 14.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.ROUND)

        drawPanel(ctx.w * 0.76f, ctx.h * 0.31f, ctx.w * 0.19f, ctx.h * 0.48f, p, accentEdge = true)
        gText("P 1   T 2", ctx.w * 0.855f, ctx.h * 0.385f, u * 2.1f, p.accent, Paint.Align.CENTER, ctx.type.digits, true)
        gText("W 1  EB 2", ctx.w * 0.855f, ctx.h * 0.455f, u * 2.1f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.855f, ctx.h * 0.545f, u * 1.85f, Color(0xFF00E5FF), Paint.Align.CENTER, ctx.type.digits, true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.855f, ctx.h * 0.64f, u * 1.65f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        gText("TRIP ${ctx.formatTrip()}", ctx.w * 0.855f, ctx.h * 0.715f, u * 1.55f, p.muted, Paint.Align.CENTER, ctx.type.label, true)

        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}   •   ${ctx.formatBattery()}   •   SEANS ${ctx.formatSession()}", ctx.w * 0.38f, ctx.h * 0.76f, u * 1.65f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.36f, ctx.h * 0.40f, u * 23.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.78f, ctx.h * 0.37f, u * 14.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.ROUND)

        val pills = listOf("P 1", "T 2", "W 1", "EB 2")
        pills.forEachIndexed { idx, txt ->
            val pw = ctx.w * 0.19f
            val px = ctx.w * 0.06f + idx * (ctx.w * 0.23f)
            val py = ctx.h * 0.525f
            drawPanel(px, py, pw, ctx.h * 0.075f, p, accentEdge = idx == 0)
            gText(txt, px + pw * 0.5f, py + ctx.h * 0.050f, u * 2.1f, if (idx == 0) p.accent else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        }

        drawPanel(ctx.w * 0.06f, ctx.h * 0.635f, ctx.w * 0.88f, ctx.h * 0.17f, p, accentEdge = true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.095f, ctx.h * 0.695f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A ${ctx.formatTrip()}", ctx.w * 0.095f, ctx.h * 0.76f, u * 1.75f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.905f, ctx.h * 0.695f, u * 2.0f, Color(0xFF00E5FF), Paint.Align.RIGHT, ctx.type.digits, true)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}  •  ${ctx.formatBattery()}", ctx.w * 0.905f, ctx.h * 0.76f, u * 1.55f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

private fun DrawScope.drawHondaMechanicTelemetryTft(ctx: GaugeContext) {
    // Honda Mechanic Mode: Tüm sayısal sensör verileri 6 kutuda + üst düz devir barı
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * (if (ctx.wide) 0.28f else 0.42f), u)
    gText("HRC MECHANIC DIAGNOSTIC VIEW", ctx.w * 0.05f, ctx.h * (if (ctx.wide) 0.085f else 0.068f), u * 1.8f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.14f)
    drawLinearBar(ctx.w * 0.05f, ctx.h * (if (ctx.wide) 0.13f else 0.105f), ctx.w * 0.90f, u * 4.5f, ctx.rpmRatio, p.accent, p.track, ctx.redlineRatio, RL_RED, u * 0.8f)

    if (ctx.wide) {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.25f, ctx.h * 0.48f, u * 21.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.52f, ctx.h * 0.42f, u * 12.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.36f, ctx.h * 0.29f, u * 20.5f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.78f, ctx.h * 0.26f, u * 13.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
    }

    val cells = listOf(
        "ENGINE RPM" to "${ctx.mappedRpm.roundToInt()} r/min",
        "WATER TEMP" to formatTemp(t.coolantTempCelsius, ctx.useFahrenheit),
        "BATTERY / VOLT" to ctx.formatBattery(),
        "WHEEL POWER" to "${t.estimatedWheelHorsepower.roundToInt()} HP / ${t.estimatedWheelTorqueNm.roundToInt()} Nm",
        "LEAN / PITCH" to "${t.leanAngleDeg.roundToInt()}° / ${t.pitchAngleDeg.roundToInt()}°",
        "ODO / TRIP A" to "${ctx.formatOdometer()} / ${ctx.formatTrip()}"
    )
    if (ctx.wide) {
        val cw = ctx.w * 0.28f
        val ch = ctx.h * 0.14f
        cells.forEachIndexed { i, (title, value) ->
            val col = i % 3
            val row = i / 3
            val x = ctx.w * 0.05f + col * (cw + ctx.w * 0.03f)
            val y = ctx.h * 0.56f + row * (ch + ctx.h * 0.025f)
            drawPanel(x, y, cw, ch, p, accentEdge = i == 5)
            gText(title, x + u * 1.8f, y + ch * 0.36f, u * 1.3f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
            gText(value, x + u * 1.8f, y + ch * 0.80f, u * 2.2f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        }
    } else {
        val cw = ctx.w * 0.425f
        val ch = ctx.h * 0.115f
        cells.forEachIndexed { i, (title, value) ->
            val col = i % 2
            val row = i / 2
            val x = ctx.w * 0.06f + col * (cw + ctx.w * 0.03f)
            val y = ctx.h * 0.415f + row * (ch + ctx.h * 0.018f)
            drawPanel(x, y, cw, ch, p, accentEdge = i == 5)
            gText(title, x + u * 1.6f, y + ch * 0.35f, u * 1.3f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
            gText(value, x + u * 1.6f, y + ch * 0.78f, u * 1.95f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        }
    }
    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  KTM GEN 1 / NG TURUNCU ARKA IŞIKLI LCD — 125 / 200 / 390 Duke Gen 1 & RC 200
 * ===================================================================================== */

/* =====================================================================================
 *  YAMAHA YZR-M1 / R25 / R3 GEN 2 / R15M V4 — Dikdörtgen Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  SUZUKI GSX-R1000R L7 / KATANA / SV650 — Negatif S-DMS Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  TRIUMPH 765 RS / TIGER — Stylus / High Contrast / Chronos / Grid + Trident/Daytona
 * ===================================================================================== */


/* =====================================================================================
 *  CFMOTO / QJMOTOR / VOGE / ZONTES / KOVE — TPMS (Lastik Basıncı & Sıcaklığı) + Çift Mod TFT
 * ===================================================================================== */

/* =====================================================================================
 *  KAWASAKI RIDEOLOGY TFT — Type 1 Street vs Type 2 Circuit (KTRC + Lean Hold + Boost)
 * ===================================================================================== */
private fun DrawScope.drawKawasakiStreetModeTft(ctx: GaugeContext) {
    drawKawasakiStreetType1TftReal(ctx)
}

private fun DrawScope.drawKawasakiCircuitModeTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.0f, ctx.w * (if (ctx.wide) 0.30f else 0.42f), u)
    val headerY = ctx.h * (if (ctx.wide) 0.088f else 0.068f)
    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, headerY - u * 0.6f, u * 4.4f, u, p.accent)
    gText("${ctx.spec.modelName.substringBefore(" (").uppercase()} // KRT CIRCUIT TYPE 2", ctx.w * 0.125f, headerY, u * 1.65f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.10f)

    // Kawasaki üst bumerang-swept yeşil/kehribar/kırmızı bar
    val pts = if (ctx.wide) {
        rememberTvsRtrCurve(
            Offset(ctx.w * 0.06f, ctx.h * 0.36f),
            Offset(ctx.w * 0.20f, ctx.h * 0.16f),
            Offset(ctx.w * 0.94f, ctx.h * 0.16f)
        )
    } else {
        rememberTvsRtrCurve(
            Offset(ctx.w * 0.06f, ctx.h * 0.225f),
            Offset(ctx.w * 0.20f, ctx.h * 0.125f),
            Offset(ctx.w * 0.94f, ctx.h * 0.125f)
        )
    }
    val krtColor = if (t.shiftLightState == ShiftLightState.NORMAL) p.accent else p.stage1
    drawTvsSegmentedBar(pts, 54, ctx.rpmRatio, u * 4.5f, krtColor, p.track, ctx.redlineRatio, ctx.peakRatio)
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    var k = 0
    while (k <= maxK) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + u * 5.0f, u * 2.0f, if (frac >= ctx.redlineRatio) RL_RED else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        k += 2
    }

    if (ctx.wide) {
        // Merkezde Dev Vites (Yeşil KRT çerçeveli) + Sağda Hız + Solda KRT Lean Angle Grafik
        drawLeanArc(ctx.w * 0.18f, ctx.h * 0.58f, u * 13.0f, t.leanAngleDeg, p.accent, p.track, u)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.18f, ctx.h * 0.60f, u * 2.4f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        gText("${if (t.tractionControlInterventionActive) "KTRC ACT" else "KTRC ON"}  •  ${ctx.subMode.uppercase()}", ctx.w * 0.18f, ctx.h * 0.77f, u * 1.50f, p.accent, Paint.Align.CENTER, ctx.type.label, true)

        drawGearPod(ctx.w * 0.48f, ctx.h * 0.52f, u * 16.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
        gText("GEAR", ctx.w * 0.48f, ctx.h * 0.675f, u * 1.6f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.20f)

        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.79f, ctx.h * 0.56f, u * 22.0f, p.fg, p.muted, ctx.type.digits)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.94f, ctx.h * 0.71f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
        gText("TRIP A ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}", ctx.w * 0.94f, ctx.h * 0.77f, u * 1.55f, p.accent, Paint.Align.RIGHT, ctx.type.label, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.35f, ctx.h * 0.40f, u * 23.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.78f, ctx.h * 0.37f, u * 15.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
        gText("GEAR", ctx.w * 0.78f, ctx.h * 0.46f, u * 1.6f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.20f)

        drawPanel(ctx.w * 0.06f, ctx.h * 0.535f, ctx.w * 0.88f, ctx.h * 0.26f, p, accentEdge = true)
        drawLeanArc(ctx.w * 0.25f, ctx.h * 0.65f, u * 12.0f, t.leanAngleDeg, p.accent, p.track, u)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.25f, ctx.h * 0.665f, u * 2.2f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        gText(if (t.tractionControlInterventionActive) "KTRC ACT" else "KTRC ON", ctx.w * 0.25f, ctx.h * 0.745f, u * 1.6f, p.accent, Paint.Align.CENTER, ctx.type.label, true)

        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.90f, ctx.h * 0.605f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
        gText("TRIP A ${ctx.formatTrip()}", ctx.w * 0.90f, ctx.h * 0.668f, u * 1.85f, p.accent, Paint.Align.RIGHT, ctx.type.digits, true)
        gText("SEANS ${ctx.formatSession()}", ctx.w * 0.90f, ctx.h * 0.732f, u * 1.65f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  KTM GEN 1 / NG TURUNCU ARKA IŞIKLI LCD — 125 / 200 / 390 Duke Gen 1 & RC 200
 * ===================================================================================== */

/* =====================================================================================
 *  YAMAHA YZR-M1 / R25 / R3 GEN 2 / R15M V4 — Dikdörtgen Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  SUZUKI GSX-R1000R L7 / KATANA / SV650 — Negatif S-DMS Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  TRIUMPH 765 RS / TIGER — Stylus / High Contrast / Chronos / Grid + Trident/Daytona
 * ===================================================================================== */


/* =====================================================================================
 *  CFMOTO / QJMOTOR / VOGE / ZONTES / KOVE — TPMS (Lastik Basıncı & Sıcaklığı) + Çift Mod TFT
 * ===================================================================================== */

/* =====================================================================================
 *  DUCATI CORSE TRACK EVO vs ROAD CIRCULAR — Renkli Elektronik Hapları (DTC/DWC/DSC/EBC)
 * ===================================================================================== */
private fun DrawScope.drawDucatiTrackEvoBespokeTft(ctx: GaugeContext) {
    drawSharpHorizontalEvo(ctx)
    // Sağ alt köşeye gerçek canlı elektronik durumu (DTC / DWC / ABS / Akü)
    val t = ctx.telemetry
    val u = ctx.u
    val pills = listOf(
        Triple("DTC", if (t.tractionControlInterventionActive) "ACT" else "ON", if (t.tractionControlInterventionActive) RL_AMBER else Color(0xFF00E676)),
        Triple("DWC", if (t.isWheelieActive) "${t.wheelieAngleDeg.roundToInt()}°" else "ARM", Color(0xFFFF9100)),
        Triple("ABS", if (t.absInterventionActive) "ACT" else "ON", Color(0xFF00E5FF)),
        Triple("BAT", ctx.formatBattery().substringBefore(" "), Color(0xFFFFEA00))
    )
    val startX = if (ctx.wide) ctx.w * 0.67f else ctx.w * 0.06f
    val pillW = if (ctx.wide) ctx.w * 0.068f else (ctx.w * 0.88f - u * 2.4f) / pills.size.toFloat()
    val y = if (ctx.wide) ctx.h * 0.825f else ctx.h * 0.835f
    pills.forEachIndexed { idx, (tag, lvl, col) ->
        val px = startX + idx * (pillW + u * 0.8f)
        drawRoundRect(col.copy(alpha = 0.20f), Offset(px, y - u * 2.8f), Size(pillW, u * 3.8f), CornerRadius(u * 0.6f, u * 0.6f))
        drawRoundRect(col, Offset(px, y - u * 2.8f), Size(pillW, u * 3.8f), CornerRadius(u * 0.6f, u * 0.6f), style = Stroke(width = u * 0.22f))
        gText("$tag $lvl", px + pillW * 0.5f, y - u * 0.3f, u * (if (ctx.wide) 1.45f else 1.55f), col, Paint.Align.CENTER, ctx.type.label, true)
    }
}

private fun DrawScope.drawDucatiRoadCircularTft(ctx: GaugeContext) {
    drawDucatiRoadCircularTftReal(ctx)
}

/* =====================================================================================
 *  KTM STREET (4-Kutulu Favoriler) vs KTM TRACK (Lap & Sector Yarış Ekranı)
 * ===================================================================================== */
private fun DrawScope.drawKtmStreetFavoritesTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.2f, ctx.w * (if (ctx.wide) 0.32f else 0.42f), u)

    val pts = if (ctx.wide) {
        rememberBoomerangCurve(
            Offset(ctx.w * 0.44f, ctx.h * 0.72f),
            Offset(ctx.w * 0.49f, ctx.h * 0.19f),
            Offset(ctx.w * 0.94f, ctx.h * 0.19f)
        )
    } else {
        rememberBoomerangCurve(
            Offset(ctx.w * 0.06f, ctx.h * 0.24f),
            Offset(ctx.w * 0.16f, ctx.h * 0.125f),
            Offset(ctx.w * 0.94f, ctx.h * 0.125f)
        )
    }
    drawPolylineBar(pts, 46, ctx.rpmRatio, u * 3.9f, p.accent, p.track, ctx.redlineRatio, ctx.peakRatio, u)
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    for (k in 0..maxK step 2) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + (if (ctx.wide) -u * 2.8f else u * 4.8f), u * 1.85f, if (frac >= ctx.redlineRatio) RL_RED else p.muted, Paint.Align.CENTER, ctx.type.digits, true)
    }

    val favs = listOf(
        "ODOMETER" to ctx.formatOdometer(),
        "TRIP 1" to ctx.formatTrip(),
        "SEANS" to ctx.formatSession(),
        "COOLANT / RANGE" to "${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}  •  ${ctx.formatRange()}"
    )
    if (ctx.wide) {
        // Sol yarı: Gerçek KTM 390/790/890/1290 Duke "Favorites" 4 Satırlı Bilgi Paneli!
        favs.forEachIndexed { idx, (label, value) ->
            val top = ctx.h * (0.15f + idx * 0.17f)
            drawPanel(ctx.w * 0.04f, top, ctx.w * 0.36f, ctx.h * 0.145f, p, accentEdge = idx == 0)
            gText(label, ctx.w * 0.06f, top + ctx.h * 0.052f, u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.10f)
            gText(value, ctx.w * 0.38f, top + ctx.h * 0.115f, u * 2.55f, if (idx == 1) p.accent else p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
        }

        // Sağ yarı (bumerangın içi): Hız + Vites + Sürüş Modu
        drawGearPod(ctx.w * 0.58f, ctx.h * 0.48f, u * 13.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.80f, ctx.h * 0.56f, u * 22.0f, p.fg, p.muted, ctx.type.digits)
        drawSubModeChip(ctx, ctx.w * 0.72f, ctx.h * 0.74f, alignCenter = true)
    } else {
        drawGearPod(ctx.w * 0.22f, ctx.h * 0.37f, u * 13.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.65f, ctx.h * 0.40f, u * 22.5f, p.fg, p.muted, ctx.type.digits)
        drawSubModeChip(ctx, ctx.w * 0.50f, ctx.h * 0.495f, alignCenter = true)

        val cw = ctx.w * 0.425f
        val ch = ctx.h * 0.11f
        favs.forEachIndexed { idx, (label, value) ->
            val col = idx % 2
            val row = idx / 2
            val left = ctx.w * 0.06f + col * (cw + ctx.w * 0.03f)
            val top = ctx.h * 0.55f + row * (ch + ctx.h * 0.02f)
            drawPanel(left, top, cw, ch, p, accentEdge = idx == 0)
            gText(label, left + u * 1.6f, top + ch * 0.36f, u * 1.3f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.08f)
            gText(value, left + cw - u * 1.6f, top + ch * 0.80f, u * 2.0f, if (idx == 1) p.accent else p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
        }
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  KTM GEN 1 / NG TURUNCU ARKA IŞIKLI LCD — 125 / 200 / 390 Duke Gen 1 & RC 200
 * ===================================================================================== */

/* =====================================================================================
 *  YAMAHA YZR-M1 / R25 / R3 GEN 2 / R15M V4 — Dikdörtgen Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  SUZUKI GSX-R1000R L7 / KATANA / SV650 — Negatif S-DMS Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  TRIUMPH 765 RS / TIGER — Stylus / High Contrast / Chronos / Grid + Trident/Daytona
 * ===================================================================================== */


/* =====================================================================================
 *  CFMOTO / QJMOTOR / VOGE / ZONTES / KOVE — TPMS (Lastik Basıncı & Sıcaklığı) + Çift Mod TFT
 * ===================================================================================== */

private fun DrawScope.drawKtmTrackRaceTft(ctx: GaugeContext) {
    drawKtmTrackRaceTftReal(ctx)
}

/* =====================================================================================
 *  APRILIA APRC WING TFT — RSV4 Factory / RS 660 / RS 457
 * ===================================================================================== */
private fun DrawScope.drawApriliaAprcWingTft(ctx: GaugeContext, isTrack: Boolean) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.2f, ctx.w * (if (ctx.wide) 0.30f else 0.42f), u)

    val cx = if (ctx.wide) (if (isTrack) ctx.w * 0.42f else ctx.w * 0.36f) else ctx.w * 0.50f
    val cy = if (ctx.wide) ctx.h * 0.50f else ctx.h * 0.34f
    val r = min(ctx.w * (if (ctx.wide) 0.25f else 0.34f), ctx.h * (if (ctx.wide) 0.35f else 0.23f))
    drawSegmentedArc(cx, cy, r, 135f, 270f, 52, ctx.rpmRatio, u * 4.0f, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.85f)
    var rpm = 0
    val step = if (ctx.maxRpm <= 11500.0) 1000 else 2000
    while (rpm <= ctx.maxRpm.roundToInt()) {
        val frac = rpm / ctx.maxRpm.toFloat()
        val pt = polar(cx, cy, r - u * 7.6f, 135f + 270f * frac)
        gTextCenter((rpm / 1000).toString(), pt.x, pt.y, u * 1.9f, if (frac >= ctx.redlineRatio) RL_RED else p.muted, ctx.type.label)
        rpm += step
    }

    val aprc = listOf(
        "ATC (Traction)" to if (t.tractionControlInterventionActive) "ACT" else "ON",
        "AWC (Wheelie)" to if (t.isWheelieActive) "${t.wheelieAngleDeg.roundToInt()}°" else "ARM",
        "ABS (Cornering)" to if (t.absInterventionActive) "ACT" else "ON",
        "LEAN / BAT" to "${abs(t.leanAngleDeg).roundToInt()}° / ${ctx.formatBattery()}"
    )
    if (ctx.wide) {
        if (isTrack) {
            drawGearPod(cx, cy + u * 1.0f, u * 14.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.74f, ctx.h * 0.38f, u * 19.0f, p.fg, p.muted, ctx.type.digits)
        } else {
            drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 3.5f, u * 18.0f, p.fg, p.muted, ctx.type.digits)
            drawGearPod(cx, cy + r * 0.58f, u * 10.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
        }

        // Sağ kanat: Aprilia APRC (Canlı Telemetri Durumu + ODO / TRIP / SEANS)
        drawPanel(ctx.w * 0.64f, ctx.h * 0.18f, ctx.w * 0.31f, ctx.h * 0.64f, p, accentEdge = true)
        gText("APRILIA APRC // ${ctx.subMode.uppercase()}", ctx.w * 0.665f, ctx.h * 0.24f, u * 1.65f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
        aprc.forEachIndexed { i, (name, lvl) ->
            val y = ctx.h * (0.31f + i * 0.075f)
            gText(name, ctx.w * 0.665f, y, u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
            gText(lvl, ctx.w * 0.925f, y, u * 1.55f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
        }
        gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.665f, ctx.h * 0.65f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.665f, ctx.h * 0.715f, u * 1.8f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.665f, ctx.h * 0.775f, u * 1.55f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    } else {
        if (isTrack) {
            drawGearPod(cx, cy - r * 0.08f, u * 13.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
            drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + r * 0.45f, u * 16.5f, p.fg, p.muted, ctx.type.digits)
        } else {
            drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 2.5f, u * 17.5f, p.fg, p.muted, ctx.type.digits)
            drawGearPod(cx, cy + r * 0.56f, u * 10.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
        }
        drawPanel(ctx.w * 0.06f, ctx.h * 0.57f, ctx.w * 0.88f, ctx.h * 0.25f, p, accentEdge = true)
        gText("APRILIA APRC // ${ctx.subMode.uppercase()}", ctx.w * 0.095f, ctx.h * 0.618f, u * 1.6f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
        aprc.forEachIndexed { i, (name, lvl) ->
            val y = ctx.h * (0.658f + i * 0.042f)
            gText(name, ctx.w * 0.095f, y, u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
            gText(lvl, ctx.w * 0.54f, y, u * 1.5f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
        }
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.905f, ctx.h * 0.665f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
        gText("TRIP ${ctx.formatTrip()}", ctx.w * 0.905f, ctx.h * 0.725f, u * 1.75f, p.accent, Paint.Align.RIGHT, ctx.type.digits, true)
        gText("SEANS ${ctx.formatSession()}", ctx.w * 0.905f, ctx.h * 0.78f, u * 1.5f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  KTM GEN 1 / NG TURUNCU ARKA IŞIKLI LCD — 125 / 200 / 390 Duke Gen 1 & RC 200
 * ===================================================================================== */

/* =====================================================================================
 *  YAMAHA YZR-M1 / R25 / R3 GEN 2 / R15M V4 — Dikdörtgen Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  SUZUKI GSX-R1000R L7 / KATANA / SV650 — Negatif S-DMS Yarış LCD
 * ===================================================================================== */

/* =====================================================================================
 *  TRIUMPH 765 RS / TIGER — Stylus / High Contrast / Chronos / Grid + Trident/Daytona
 * ===================================================================================== */


/* =====================================================================================
 *  CFMOTO / QJMOTOR / VOGE / ZONTES / KOVE — TPMS (Lastik Basıncı & Sıcaklığı) + Çift Mod TFT
 * ===================================================================================== */

/* --- Yardımcı Eğri Fonksiyonları --- */
private fun rememberTvsRtrCurve(p0: Offset, knee: Offset, p2: Offset): List<Offset> {
    val out = ArrayList<Offset>(28)
    for (i in 0..24) {
        val t = i / 24f
        val omt = 1f - t
        val x = omt * omt * p0.x + 2f * omt * t * knee.x + t * t * p2.x
        val y = omt * omt * p0.y + 2f * omt * t * knee.y + t * t * p2.y
        out.add(Offset(x, y))
    }
    return out
}

private fun pointOnCurve(pts: List<Offset>, frac: Float): Offset = pointAt(pts, frac)

private fun DrawScope.drawTvsSegmentedBar(
    points: List<Offset>,
    segments: Int,
    filledRatio: Float,
    width: Float,
    color: Color,
    track: Color,
    redlineRatio: Float,
    peakRatio: Float
) {
    val filledSeg = (filledRatio.coerceIn(0f, 1f) * segments).roundToInt()
    val redSeg = (redlineRatio.coerceIn(0f, 1f) * segments).roundToInt()
    val peakSeg = if (peakRatio >= 0f) (peakRatio.coerceIn(0f, 1f) * segments).roundToInt() else -1
    for (i in 0 until segments) {
        val pA = pointOnCurve(points, i / segments.toFloat())
        val pB = pointOnCurve(points, (i + 0.76f) / segments.toFloat())
        val c = when {
            i < filledSeg && i >= redSeg -> RL_RED
            i < filledSeg -> color
            i == peakSeg && peakSeg > filledSeg -> Color.White
            i >= redSeg -> RL_RED.copy(alpha = 0.22f)
            else -> track
        }
        drawLine(c, pA, pB, width, StrokeCap.Butt)
    }
}
