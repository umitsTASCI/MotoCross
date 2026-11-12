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
    if (!ctx.wide) return false
    val id = ctx.spec.skinId
    val brand = ctx.spec.brand
    val sub = ctx.subMode.lowercase()

    // 1. TVS Apache RTR 200 4V (2024 SmartXonnect) / RTR 160 4V / Ronin / RR 310 / RTR 310
    if (brand == "TVS") {
        when {
            id.contains("rr_310") -> drawTvsApacheRr310VerticalTft(ctx)
            id.contains("rtr_310") -> drawTvsApacheRtr310DynamicTft(ctx)
            id.contains("ronin") -> drawTvsRoninAsymmetricPuck(ctx)
            else -> drawTvsApacheRtr200RaceConsole(ctx)
        }
        return true
    }

    // 2. Bajaj Pulsar RS200 / NS200 / N250 / NS400Z / Dominar 400
    if (brand == "Bajaj") {
        when {
            id.contains("dominar") -> drawBajajDominarDualConsole(ctx)
            id.contains("ns400") || id.contains("n250") -> drawBajajInfinityDigitalCluster(ctx)
            else -> drawBajajPulsarPerimeterHybrid(ctx)
        }
        return true
    }

    // 3. BMW Motorrad Core 1 / Core 2 / Core 3 / Pure Ride
    if (brand == "BMW Motorrad" && (id.contains("s1000") || id.contains("m1000") || id.contains("f900"))) {
        when {
            sub.contains("core 2") || sub.contains("analog") -> drawBmwCore2AnalogTft(ctx)
            sub.contains("core 3") || sub.contains("lap") -> drawBmwCore3TrackLapTft(ctx)
            sub.contains("pure") -> drawBmwPureRideTft(ctx)
            else -> drawBmwCore1MPowerTft(ctx)
        }
        return true
    }

    // 4. Yamaha R1 / R1M / MT-10 / R7 / R125 & MT-09 Gen 4
    if (brand == "Yamaha") {
        if (id.contains("yzf_r1") || id.contains("mt10") || id.contains("yzf_r9")) {
            if (sub.contains("track")) drawYamahaR1TrackModeTft(ctx) else drawYamahaR1StreetModeTft(ctx)
            return true
        }
        if (id.contains("mt09_gen4") || id.contains("tracer_9")) {
            drawYamahaMt09Gen4MultiThemeTft(ctx)
            return true
        }
    }

    // 5. Honda CBR1000RR-R Fireblade / CBR600RR / CB750 Hornet / CBR650R TFT (Circuit / Street / Bar / Mechanic)
    if (brand == "Honda" && (id.contains("cbr1000") || id.contains("cbr600rr_2024") || id.contains("cb750") || id.contains("cbr650r_2024") || id.contains("cb1000"))) {
        when {
            sub.contains("mechanic") || sub.contains("simple") -> drawHondaMechanicTelemetryTft(ctx)
            sub.contains("bar") || sub.contains("street") && id.contains("sc77") -> drawHondaSweptBarTft(ctx)
            else -> drawHondaFirebladeCircuitRingTft(ctx)
        }
        return true
    }

    // 6. Kawasaki Ninja ZX-10R / ZX-6R / ZX-4RR / Z900 / Z H2 (Type 1 Street vs Type 2 Circuit)
    if (brand == "Kawasaki" && (id.contains("zx10r_2021") || id.contains("zx6r_2024") || id.contains("zx4rr") || id.contains("z900_tft") || id.contains("z_h2") || id.contains("ninja_500") || id.contains("ninja_650") || id.contains("ninja_1000"))) {
        if (sub.contains("circuit") || sub.contains("track") || sub.contains("type 2")) {
            drawKawasakiCircuitModeTft(ctx)
        } else {
            drawKawasakiStreetModeTft(ctx)
        }
        return true
    }

    // 7. Ducati Panigale V4 / Streetfighter V4 / Panigale V2 / Monster 937 (Track Evo vs Road)
    if (brand == "Ducati" && (id.contains("panigale") || id.contains("streetfighter") || id.contains("monster") || id.contains("hypermotard") || id.contains("gp24"))) {
        if (sub.contains("road") || sub.contains("touring") || sub.contains("urban")) {
            drawDucatiRoadCircularTft(ctx)
        } else {
            drawDucatiTrackEvoBespokeTft(ctx)
        }
        return true
    }

    // 8. KTM 390 / 790 / 890 / 1290 Duke & RC + Gen 1 Amber LCD
    if (brand == "KTM") {
        if (id.contains("gen1_lcd") || id.contains("200_duke") || id.contains("rc_200") || id.contains("rc_125") || id.contains("690_smc")) {
            drawKtmGen1AmberLcd(ctx)
            return true
        }
        if (id.contains("390_duke_gen2") || id.contains("rc_390") || id.contains("890_duke") || id.contains("1290_super_duke")) {
            if (sub.contains("track")) drawKtmTrackRaceTft(ctx) else drawKtmStreetFavoritesTft(ctx)
            return true
        }
    }

    // 9. Yamaha R25 / R3 Gen 2 / R15M V4 / MT-25 / MT-15 (YZR-M1 Race LCD)
    if (brand == "Yamaha" && (id.contains("r25_r3_gen2") || id.contains("r15") || id.contains("mt25") || id.contains("mt15"))) {
        drawYamahaR3M1RaceLcd(ctx, isTrack = sub.contains("track"))
        return true
    }

    // 10. Suzuki GSX-R1000R L7 / Katana / SV650 / GSX-R125 (Negative S-DMS Race LCD)
    if (brand == "Suzuki" && (id.contains("gsxr1000r_l7") || id.contains("katana") || id.contains("sv650") || id.contains("gsxr125") || id.contains("gsxr250"))) {
        drawSuzukiGsxrL7NegativeLcd(ctx)
        return true
    }

    // 11. Triumph Street Triple 765 RS / Tiger (Stylus / High Contrast / Chronos / Grid) & Daytona/Trident Hybrid
    if (brand == "Triumph") {
        if (id.contains("daytona_660") || id.contains("trident_660")) {
            drawTriumphTridentDaytonaHybrid(ctx)
            return true
        }
        if (id.contains("765_rs") || id.contains("tiger")) {
            drawTriumphMultiStyleTft(ctx)
            return true
        }
    }

    // 12. CFMoto / QJMotor / Voge / Zontes / Kove / Benelli (TPMS Lastik Basınçlı + Çift Modlu TFT)
    if (brand == "CFMoto" || brand == "QJMotor" || brand == "Voge" || brand == "Zontes" || brand == "Kove") {
        if (!id.contains("800nk") && !id.contains("450mt") && !id.contains("450clc") && !id.contains("700clx") && !id.contains("450_rally")) {
            drawTpmsSupersportTft(ctx)
            return true
        }
    }

    // 13. Aprilia RSV4 / RS 660 / RS 457 (APRC Road vs Track)
    if (brand == "Aprilia" && (id.contains("rsv4_factory") || id.contains("rs660") || id.contains("rs457") || id.contains("tuono"))) {
        drawApriliaAprcWingTft(ctx, isTrack = sub.contains("track"))
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

    // Sol blok: Vites kutusu + SmartXonnect Telemetri (0-60 sn, Top Speed, G-Force)
    drawPanel(ctx.w * 0.045f, ctx.h * 0.40f, ctx.w * 0.22f, ctx.h * 0.42f, p, accentEdge = true)
    gText("GEAR", ctx.w * 0.155f, ctx.h * 0.455f, u * 1.55f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)
    drawGearPod(ctx.w * 0.155f, ctx.h * 0.565f, u * 12.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
    val topSpd = max(ctx.speedValue, (t.trueSpeedKmh * 1.04).roundToInt())
    gText("HIGH SPD  $topSpd ${ctx.speedUnit}", ctx.w * 0.065f, ctx.h * 0.705f, u * 1.55f, modeColor, Paint.Align.LEFT, ctx.type.digits, true)
    gText("0-60 KM/H  3.9 s", ctx.w * 0.065f, ctx.h * 0.752f, u * 1.50f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
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

    // Sağ blok: Lap Timer + Lean/G-Force + Yakıt Segmentleri
    drawPanel(ctx.w * 0.75f, ctx.h * 0.34f, ctx.w * 0.205f, ctx.h * 0.48f, p, accentEdge = true)
    gText("LAP TIMER", ctx.w * 0.77f, ctx.h * 0.39f, u * 1.45f, modeColor, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)
    gText("01:18.42", ctx.w * 0.77f, ctx.h * 0.455f, u * 3.3f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("BEST 01:17.85", ctx.w * 0.77f, ctx.h * 0.498f, u * 1.45f, p.muted, Paint.Align.LEFT, ctx.type.digits, true)
    gText("LEAN  ${abs(t.leanAngleDeg).roundToInt()}°   G  ${String.format(java.util.Locale.US, "%.2f", t.longitudinalG)}", ctx.w * 0.77f, ctx.h * 0.565f, u * 1.65f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("PEAK  ${t.estimatedWheelHorsepower.roundToInt()} PS  •  ${t.estimatedWheelTorqueNm.roundToInt()} Nm", ctx.w * 0.77f, ctx.h * 0.618f, u * 1.45f, modeColor, Paint.Align.LEFT, ctx.type.label, true)
    gText("FUEL LEVEL", ctx.w * 0.77f, ctx.h * 0.685f, u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    drawLinearBar(ctx.w * 0.77f, ctx.h * 0.71f, ctx.w * 0.165f, u * 1.8f, t.fuelLevelRatio, modeColor, p.track, 0.9f, RL_RED, u * 0.5f)
    gText("RANGE ${(t.fuelLevelRatio * 390).roundToInt()} km  •  ${t.coolantTempCelsius.roundToInt()}°C", ctx.w * 0.77f, ctx.h * 0.785f, u * 1.45f, p.fg, Paint.Align.LEFT, ctx.type.label, true)

    drawStatusRail(ctx, RailStyle.LCD)
}

private fun DrawScope.drawTvsApacheRr310VerticalTft(ctx: GaugeContext) {
    // RR 310 dikey 5" Bi-LED Race TFT şeması
    drawRallyTower(ctx)
}

private fun DrawScope.drawTvsApacheRtr310DynamicTft(ctx: GaugeContext) {
    drawDucatiTrackEvoBespokeTft(ctx)
}

private fun DrawScope.drawTvsRoninAsymmetricPuck(ctx: GaugeContext) {
    drawSinglePuck(ctx)
}

/* =====================================================================================
 *  BAJAJ PULSAR RS200 / NS200 / NS400Z / DOMINAR 400 — Birebir OEM Kadranlar
 * ===================================================================================== */
private fun DrawScope.drawBajajPulsarPerimeterHybrid(ctx: GaugeContext) {
    // Pulsar RS200 / NS200: Merkez karbon çerçeveli analog devir saati + Sağ mavi/beyaz dijital hız & ODO + Üst kırmızı Shift LED
    drawCentralAnalogHybrid(ctx)
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

    drawGearPod(ctx.w * 0.18f, ctx.h * 0.56f, u * 13.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
    gText("GEAR", ctx.w * 0.18f, ctx.h * 0.68f, u * 1.6f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.64f, u * 26.0f, p.fg, p.muted, ctx.type.digits)
    drawSubModeChip(ctx, ctx.w * 0.50f, ctx.h * 0.74f, alignCenter = true)

    drawPanel(ctx.w * 0.73f, ctx.h * 0.36f, ctx.w * 0.22f, ctx.h * 0.44f, p, accentEdge = true)
    gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.755f, ctx.h * 0.43f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("TRIP 1  ${ctx.formatTrip()}", ctx.w * 0.755f, ctx.h * 0.50f, u * 1.85f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
    gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.755f, ctx.h * 0.56f, u * 1.65f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    gText("DFE  ${formatFuelConsumption(3.4, ctx.fuelUnitCode)}", ctx.w * 0.755f, ctx.h * 0.63f, u * 1.55f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
    drawLinearBar(ctx.w * 0.755f, ctx.h * 0.70f, ctx.w * 0.17f, u * 1.6f, t.fuelLevelRatio, p.accent, p.track, 0.9f, RL_RED, u * 0.6f)
    gText("FUEL ${(t.fuelLevelRatio * 100).roundToInt()}%  •  ${t.coolantTempCelsius.roundToInt()}°C", ctx.w * 0.755f, ctx.h * 0.76f, u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

    drawStatusRail(ctx, RailStyle.LCD)
}

private fun DrawScope.drawBajajDominarDualConsole(ctx: GaugeContext) {
    // Dominar 400 UG: İkiz ekran (Ana gidon LCD + Depo üstü ikinci bilgi ekranı)
    drawPanoramicSplit(ctx)
}

/* =====================================================================================
 *  BMW MOTORRAD 6.5" CONNECTIVITY TFT — Core 1 / Core 2 / Core 3 / Pure Ride
 * ===================================================================================== */
private fun DrawScope.drawBmwCore1MPowerTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    // BMW M-Motorsport üç renk şeridi (Açık Mavi / Lacivert / Kırmızı)
    drawRect(Color(0xFF0099FF), Offset(ctx.w * 0.04f, ctx.h * 0.055f), Size(u * 2.2f, u * 1.4f))
    drawRect(Color(0xFF003380), Offset(ctx.w * 0.04f + u * 2.5f, ctx.h * 0.055f), Size(u * 2.2f, u * 1.4f))
    drawRect(Color(0xFFE60012), Offset(ctx.w * 0.04f + u * 5.0f, ctx.h * 0.055f), Size(u * 2.2f, u * 1.4f))
    gText("M MOTORSPORT // CORE 1", ctx.w * 0.04f + u * 8.5f, ctx.h * 0.072f, u * 1.75f, p.fg, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.95f, ctx.h * 0.075f, u * 2.8f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    val pts = rememberBoomerangCurve(
        Offset(ctx.w * 0.06f, ctx.h * 0.68f),
        Offset(ctx.w * 0.13f, ctx.h * 0.19f),
        Offset(ctx.w * 0.93f, ctx.h * 0.19f)
    )
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

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.44f, ctx.h * 0.62f, u * 25.0f, p.fg, p.muted, ctx.type.digits)
    drawGearPod(ctx.w * 0.78f, ctx.h * 0.52f, u * 14.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
    gText("RACE PRO 1", ctx.w * 0.78f, ctx.h * 0.665f, u * 2.0f, Color(0xFFE60012), Paint.Align.CENTER, ctx.type.label, true, spacing = 0.12f)

    // Sol alt: DTC + Lean + ODO/TRIP
    drawPanel(ctx.w * 0.14f, ctx.h * 0.69f, ctx.w * 0.78f, ctx.h * 0.14f, p, accentEdge = true)
    gText("LEAN  L ${abs(min(0.0, t.leanAngleDeg)).roundToInt()}° / R ${max(0.0, t.leanAngleDeg).roundToInt()}°", ctx.w * 0.165f, ctx.h * 0.745f, u * 1.85f, Color(0xFF0099FF), Paint.Align.LEFT, ctx.type.digits, true)
    gText("DTC +2   •   ABS PRO   •   DBC ${String.format(java.util.Locale.US, "%.1f", t.longitudinalG)}G", ctx.w * 0.165f, ctx.h * 0.798f, u * 1.50f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.895f, ctx.h * 0.745f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
    gText("TRIP A ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}", ctx.w * 0.895f, ctx.h * 0.798f, u * 1.50f, p.accent, Paint.Align.RIGHT, ctx.type.label, true)

    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  KTM GEN 1 / NG TURUNCU ARKA IŞIKLI LCD — 125 / 200 / 390 Duke Gen 1 & RC 200
 * ===================================================================================== */
private fun DrawScope.drawKtmGen1AmberLcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.24f, u)

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
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    for (k in 0..maxK step 2) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + u * 4.8f, u * 1.95f, p.fg, Paint.Align.CENTER, "monospace", true)
    }

    drawGearPod(ctx.w * 0.24f, ctx.h * 0.52f, u * 12.5f, p, t, ctx.isNeutral, u, "monospace", GearPodShape.SQUARE)
    gText("GEAR", ctx.w * 0.24f, ctx.h * 0.65f, u * 1.55f, p.fg, Paint.Align.CENTER, "monospace", true)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.56f, ctx.h * 0.60f, u * 25.0f, p.fg, p.muted, "monospace")

    gText("ODO ${ctx.formatOdometer()}   •   TRIP 1 ${ctx.formatTrip()}   •   SEANS ${ctx.formatSession()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.85f, p.fg, Paint.Align.CENTER, "monospace", true)
    drawStatusRail(ctx, RailStyle.LCD)
}

/* =====================================================================================
 *  YAMAHA YZR-M1 / R25 / R3 GEN 2 / R15M V4 — Dikdörtgen Yarış LCD
 * ===================================================================================== */
private fun DrawScope.drawYamahaR3M1RaceLcd(ctx: GaugeContext, isTrack: Boolean) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.0f, ctx.w * 0.30f, u)

    // Üst tam genişlik düz/hafif eğimli M1 bar
    val left = ctx.w * 0.06f
    val width = ctx.w * 0.88f
    val top = ctx.h * 0.145f
    val barH = u * 5.2f
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
    gText(if (isTrack) "LAP 01:41.20" else "ODO ${ctx.formatOdometer()}", ctx.w * 0.735f, ctx.h * 0.43f, u * 2.2f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.735f, ctx.h * 0.50f, u * 1.85f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
    gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.735f, ctx.h * 0.565f, u * 1.60f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    drawTemperatureAndFuel(ctx, ctx.w * 0.735f, ctx.h * 0.64f, ctx.w * 0.16f, compact = true)

    drawStatusRail(ctx, RailStyle.LCD)
}

/* =====================================================================================
 *  SUZUKI GSX-R1000R L7 / KATANA / SV650 — Negatif S-DMS Yarış LCD
 * ===================================================================================== */
private fun DrawScope.drawSuzukiGsxrL7NegativeLcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.0f, ctx.w * 0.30f, u)

    val pts = rememberTvsRtrCurve(
        Offset(ctx.w * 0.06f, ctx.h * 0.31f),
        Offset(ctx.w * 0.24f, ctx.h * 0.145f),
        Offset(ctx.w * 0.94f, ctx.h * 0.145f)
    )
    drawTvsSegmentedBar(pts, 54, ctx.rpmRatio, u * 4.4f, p.accent, p.track, ctx.redlineRatio, ctx.peakRatio)
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    var k = 0
    while (k <= maxK) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + u * 4.8f, u * 1.95f, if (frac >= ctx.redlineRatio) RL_RED else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        k += if (maxK >= 13) 2 else 1
    }

    // Sol: S-DMS (A) ve TC (3) kutuları
    drawPanel(ctx.w * 0.055f, ctx.h * 0.40f, ctx.w * 0.17f, ctx.h * 0.38f, p, accentEdge = true)
    gText("S-DMS", ctx.w * 0.14f, ctx.h * 0.46f, u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    gText("A", ctx.w * 0.14f, ctx.h * 0.56f, u * 6.2f, p.accent, Paint.Align.CENTER, ctx.type.digits, true)
    gText("TC MODE 2", ctx.w * 0.14f, ctx.h * 0.66f, u * 1.75f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    gText("QS ON", ctx.w * 0.14f, ctx.h * 0.73f, u * 1.50f, RL_GREEN, Paint.Align.CENTER, ctx.type.label, true)

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.47f, ctx.h * 0.63f, u * 27.0f, p.fg, p.muted, ctx.type.digits)
    gText("ODO ${ctx.formatOdometer()}   •   TRIP A ${ctx.formatTrip()}   •   SEANS ${ctx.formatSession()}", ctx.w * 0.47f, ctx.h * 0.76f, u * 1.70f, p.accent, Paint.Align.CENTER, ctx.type.digits, true)

    drawGearPod(ctx.w * 0.82f, ctx.h * 0.52f, u * 15.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.NONE)
    gText("GEAR", ctx.w * 0.82f, ctx.h * 0.66f, u * 1.55f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.20f)

    drawStatusRail(ctx, RailStyle.LCD)
}

/* =====================================================================================
 *  TRIUMPH 765 RS / TIGER — Stylus / High Contrast / Chronos / Grid + Trident/Daytona
 * ===================================================================================== */
private fun DrawScope.drawTriumphMultiStyleTft(ctx: GaugeContext) {
    when (ctx.subModeIndex % 4) {
        1 -> drawCircularRingTft(ctx)
        2 -> drawDualClassicAnalog(ctx)
        3 -> drawSharpHorizontalEvo(ctx)
        else -> drawSymmetricPincer(ctx)
    }
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
    // Gerçek CFMoto / Zontes / QJMotor / Voge ekranlarındaki imza TPMS (Ön/Arka Bar & °C) kutusu
    val p = ctx.palette
    val u = ctx.u
    val tpmsW = ctx.w * 0.185f
    val tpmsH = ctx.h * 0.155f
    val left = ctx.w * 0.035f
    val top = ctx.h * 0.20f
    drawPanel(left, top, tpmsW, tpmsH, p, accentEdge = true)
    gText("TPMS TIRE INFO", left + u * 1.6f, top + tpmsH * 0.28f, u * 1.30f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.08f)
    gText("FR  2.4 BAR  •  34°C", left + u * 1.6f, top + tpmsH * 0.58f, u * 1.55f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("RR  2.7 BAR  •  39°C", left + u * 1.6f, top + tpmsH * 0.86f, u * 1.55f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
}

private fun DrawScope.drawBmwCore2AnalogTft(ctx: GaugeContext) {
    // BMW Core 2: Merkezde dev yuvarlak M halkası + Sol/Sağ yatış barları + Sağda dev Vites
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val cx = ctx.w * 0.42f
    val cy = ctx.h * 0.49f
    val r = min(ctx.w * 0.28f, ctx.h * 0.36f)
    drawSegmentedArc(cx, cy, r, 135f, 270f, 54, ctx.rpmRatio, u * 4.2f, Color(0xFF0099FF), p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.85f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 4.5f, u * 19.0f, p.fg, p.muted, ctx.type.digits)
    gText("CORE 2 // M ANALOG", cx, cy - r * 0.45f, u * 1.6f, Color(0xFF0099FF), Paint.Align.CENTER, ctx.type.label, true)
    drawLeanArc(cx, cy, r + u * 4.2f, t.leanAngleDeg, Color(0xFFE60012), p.track, u)

    drawGearPod(ctx.w * 0.80f, ctx.h * 0.38f, u * 14.0f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
    drawPanel(ctx.w * 0.68f, ctx.h * 0.56f, ctx.w * 0.26f, ctx.h * 0.25f, p, accentEdge = true)
    gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.705f, ctx.h * 0.625f, u * 2.0f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.705f, ctx.h * 0.685f, u * 1.8f, Color(0xFF0099FF), Paint.Align.LEFT, ctx.type.digits, true)
    gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.705f, ctx.h * 0.740f, u * 1.55f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    gText("DTC INTERVENTION  ${String.format(java.util.Locale.US, "%.1f", t.wheelSlipRatioLambda * 100)}%", ctx.w * 0.705f, ctx.h * 0.785f, u * 1.35f, Color(0xFFE60012), Paint.Align.LEFT, ctx.type.label, true)
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
    // BMW Core 3: Üst yatay bar + Sol Yatış Kadranı + Sağ Fren/DTC G-Force + Merkez Lap & Speed
    drawDucatiTrackEvoBespokeTft(ctx)
}

private fun DrawScope.drawBmwPureRideTft(ctx: GaugeContext) {
    drawPanoramicSplit(ctx)
}

/* =====================================================================================
 *  YAMAHA YZF-R1 / R1M / MT-10 — Street Mode vs Track Zoom Mode + MT-09 Gen 4
 * ===================================================================================== */
private fun DrawScope.drawYamahaR1StreetModeTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.2f, ctx.w * 0.32f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, ctx.h * 0.08f, u * 4.2f, u, p.accent)
    gText("${ctx.spec.modelName.substringBefore(" (").uppercase()} // STREET MODE", ctx.w * 0.11f, ctx.h * 0.088f, u * 1.65f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

    // Yamaha R1 imza yatay-kavisli devir barı (Yeşil -> Kehribar -> Kırmızı)
    val pts = rememberTvsRtrCurve(
        Offset(ctx.w * 0.055f, ctx.h * 0.34f),
        Offset(ctx.w * 0.20f, ctx.h * 0.16f),
        Offset(ctx.w * 0.94f, ctx.h * 0.16f)
    )
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

    // Alt Yamaha YRC Elektronik Izgarası (PWR / TCS / SCS / LIF / LCS / QSS / EBM)
    val yrc = listOf("PWR 1", "TCS 2", "SCS 1", "LIF 1", "QSS ▲▼", "EBM 1", "YRC-A")
    val boxW = (ctx.w * 0.90f) / yrc.size
    val boxTop = ctx.h * 0.70f
    yrc.forEachIndexed { idx, tag ->
        val bx = ctx.w * 0.05f + idx * boxW
        drawRoundRect(p.panel, Offset(bx + u * 0.4f, boxTop), Size(boxW - u * 0.8f, ctx.h * 0.11f), CornerRadius(u * 0.7f, u * 0.7f))
        drawRoundRect(p.accent.copy(alpha = 0.45f), Offset(bx + u * 0.4f, boxTop), Size(boxW - u * 0.8f, ctx.h * 0.11f), CornerRadius(u * 0.7f, u * 0.7f), style = Stroke(width = u * 0.2f))
        gText(tag, bx + boxW * 0.5f, boxTop + ctx.h * 0.070f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
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
    // Yamaha R1 Track Mode: Devir barı 8.000 RPM'den başlar (Zoomed Race Tach), merkezde LAP + Vites!
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.2f, ctx.w * 0.34f, u)
    gText("YRC TRACK MODE // ZOOM 8-16k RPM", ctx.w * 0.055f, ctx.h * 0.085f, u * 1.75f, Color(0xFF00E5FF), Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)

    val pts = rememberTvsRtrCurve(
        Offset(ctx.w * 0.055f, ctx.h * 0.28f),
        Offset(ctx.w * 0.18f, ctx.h * 0.15f),
        Offset(ctx.w * 0.945f, ctx.h * 0.15f)
    )
    drawTvsSegmentedBar(pts, 60, ctx.rpmRatio, u * 4.8f, Color(0xFF00E5FF), p.track, ctx.redlineRatio, ctx.peakRatio)
    val startK = 8
    val endK = (ctx.maxRpm / 1000.0).roundToInt().coerceAtLeast(14)
    for (k in startK..endK) {
        val frac = (k - startK) / (endK - startK).toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y + u * 5.2f, u * 2.15f, if (k * 1000 >= ctx.redlineRpm) RL_RED else p.fg, Paint.Align.CENTER, ctx.type.digits, true)
    }

    // Merkezde Dev Vites + Lap Time, Sağ üstte Hız
    drawGearPod(ctx.w * 0.22f, ctx.h * 0.56f, u * 17.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.54f, ctx.h * 0.54f, u * 22.0f, p.fg, p.muted, ctx.type.digits)
    gText("LAP 04   01:32.18", ctx.w * 0.54f, ctx.h * 0.68f, u * 3.4f, Color(0xFF00C853), Paint.Align.CENTER, ctx.type.digits, true)
    gText("ODO ${ctx.formatOdometer()}  •  TRIP ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}", ctx.w * 0.54f, ctx.h * 0.77f, u * 1.75f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)

    drawPanel(ctx.w * 0.78f, ctx.h * 0.34f, ctx.w * 0.17f, ctx.h * 0.46f, p, accentEdge = true)
    gText("BRAKE", ctx.w * 0.865f, ctx.h * 0.40f, u * 1.4f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    drawLinearBar(ctx.w * 0.81f, ctx.h * 0.43f, ctx.w * 0.11f, u * 1.8f, abs(min(0.0, t.longitudinalG)).toFloat().coerceIn(0f, 1f), RL_RED, p.track, 0.9f, RL_RED, u * 0.5f)
    gText("ACCEL", ctx.w * 0.865f, ctx.h * 0.53f, u * 1.4f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    drawLinearBar(ctx.w * 0.81f, ctx.h * 0.56f, ctx.w * 0.11f, u * 1.8f, max(0.0, t.longitudinalG).toFloat().coerceIn(0f, 1f), Color(0xFF00C853), p.track, 0.9f, RL_RED, u * 0.5f)
    gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.865f, ctx.h * 0.70f, u * 2.2f, Color(0xFF00E5FF), Paint.Align.CENTER, ctx.type.digits, true)

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
    when (ctx.subModeIndex % 4) {
        1 -> drawYamahaR1TrackModeTft(ctx)
        2 -> drawSymmetricPincer(ctx)
        3 -> drawCircularRingTft(ctx)
        else -> drawBoomerangTft(ctx)
    }
}

/* =====================================================================================
 *  HONDA FIREBLADE / CBR600RR / CB750 HORNET — Circuit Ring vs Swept Bar vs Mechanic
 * ===================================================================================== */
private fun DrawScope.drawHondaFirebladeCircuitRingTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.2f, ctx.w * 0.30f, u)

    val cx = ctx.w * 0.50f
    val cy = ctx.h * 0.48f
    val r = min(ctx.w * 0.28f, ctx.h * 0.36f)
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

    // Sol sütun: HRC P / T / W / EB / Öhlins S1 halkaları
    val hrc = listOf("P" to "1", "T" to "2", "W" to "1", "EB" to "2")
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
    gText("COOLANT  ${t.coolantTempCelsius.roundToInt()}°C", ctx.w * 0.765f, ctx.h * 0.635f, u * 1.65f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
    gText("MODE  ${ctx.subMode.uppercase()}", ctx.w * 0.765f, ctx.h * 0.715f, u * 1.75f, p.accent, Paint.Align.LEFT, ctx.type.label, true)

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
    drawCurvedSweepTft(ctx)
}

private fun DrawScope.drawHondaMechanicTelemetryTft(ctx: GaugeContext) {
    // Honda Mechanic Mode: Tüm sayısal sensör verileri 6 kutuda + üst düz devir barı
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    gText("HRC MECHANIC DIAGNOSTIC VIEW", ctx.w * 0.05f, ctx.h * 0.085f, u * 1.8f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.14f)
    drawLinearBar(ctx.w * 0.05f, ctx.h * 0.13f, ctx.w * 0.90f, u * 4.5f, ctx.rpmRatio, p.accent, p.track, ctx.redlineRatio, RL_RED, u * 0.8f)

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.25f, ctx.h * 0.48f, u * 21.0f, p.fg, p.muted, ctx.type.digits)
    drawGearPod(ctx.w * 0.52f, ctx.h * 0.42f, u * 12.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.SQUARE)

    val cells = listOf(
        "ENGINE RPM" to "${ctx.mappedRpm.roundToInt()} r/min",
        "WATER TEMP" to "${t.coolantTempCelsius.roundToInt()}°C",
        "BATTERY / VOLT" to "14.2 V",
        "WHEEL POWER" to "${t.estimatedWheelHorsepower.roundToInt()} HP / ${t.estimatedWheelTorqueNm.roundToInt()} Nm",
        "LEAN / PITCH" to "${t.leanAngleDeg.roundToInt()}° / ${t.pitchAngleDeg.roundToInt()}°",
        "ODO / TRIP A" to "${ctx.formatOdometer()} / ${ctx.formatTrip()}"
    )
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
    drawCurvedSweepTft(ctx)
}

private fun DrawScope.drawKawasakiCircuitModeTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, ctx.h * 0.08f, u * 4.4f, u, p.accent)
    gText("${ctx.spec.modelName.uppercase()} // KRT CIRCUIT TYPE 2", ctx.w * 0.115f, ctx.h * 0.088f, u * 1.65f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.10f)

    // Kawasaki üst bumerang-swept yeşil/kehribar/kırmızı bar
    val pts = rememberTvsRtrCurve(
        Offset(ctx.w * 0.06f, ctx.h * 0.36f),
        Offset(ctx.w * 0.20f, ctx.h * 0.16f),
        Offset(ctx.w * 0.94f, ctx.h * 0.16f)
    )
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

    // Merkezde Dev Vites (Yeşil KRT çerçeveli) + Sağda Hız + Solda KRT Lean Angle Grafik
    drawLeanArc(ctx.w * 0.18f, ctx.h * 0.58f, u * 13.0f, t.leanAngleDeg, p.accent, p.track, u)
    gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.18f, ctx.h * 0.60f, u * 2.4f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
    gText("KTRC 1  •  PWR F  •  KQS", ctx.w * 0.18f, ctx.h * 0.77f, u * 1.55f, p.accent, Paint.Align.CENTER, ctx.type.label, true)

    drawGearPod(ctx.w * 0.48f, ctx.h * 0.52f, u * 16.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
    gText("GEAR", ctx.w * 0.48f, ctx.h * 0.675f, u * 1.6f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.20f)

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.79f, ctx.h * 0.56f, u * 22.0f, p.fg, p.muted, ctx.type.digits)
    gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.94f, ctx.h * 0.71f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
    gText("TRIP A ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}", ctx.w * 0.94f, ctx.h * 0.77f, u * 1.55f, p.accent, Paint.Align.RIGHT, ctx.type.label, true)

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
    // Sağ alt köşeye gerçek Ducati Panigale V4 renkli elektronik haplarını (DTC/DWC/DSC/EBC) ekle
    val u = ctx.u
    val pills = listOf(
        Triple("DTC", "2", Color(0xFF00E676)),
        Triple("DWC", "1", Color(0xFFFF9100)),
        Triple("DSC", "2", Color(0xFF00E5FF)),
        Triple("EBC", "1", Color(0xFFFFEA00))
    )
    val startX = ctx.w * 0.67f
    val pillW = ctx.w * 0.068f
    val y = ctx.h * 0.825f
    pills.forEachIndexed { idx, (tag, lvl, col) ->
        val px = startX + idx * (pillW + u * 0.8f)
        drawRoundRect(col.copy(alpha = 0.20f), Offset(px, y - u * 2.8f), Size(pillW, u * 3.8f), CornerRadius(u * 0.6f, u * 0.6f))
        drawRoundRect(col, Offset(px, y - u * 2.8f), Size(pillW, u * 3.8f), CornerRadius(u * 0.6f, u * 0.6f), style = Stroke(width = u * 0.22f))
        gText("$tag $lvl", px + pillW * 0.5f, y - u * 0.3f, u * 1.50f, col, Paint.Align.CENTER, ctx.type.label, true)
    }
}

private fun DrawScope.drawDucatiRoadCircularTft(ctx: GaugeContext) {
    drawCircularRingTft(ctx)
}

/* =====================================================================================
 *  KTM STREET (4-Kutulu Favoriler) vs KTM TRACK (Lap & Sector Yarış Ekranı)
 * ===================================================================================== */
private fun DrawScope.drawKtmStreetFavoritesTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.2f, ctx.w * 0.32f, u)

    val pts = rememberBoomerangCurve(
        Offset(ctx.w * 0.44f, ctx.h * 0.72f),
        Offset(ctx.w * 0.49f, ctx.h * 0.19f),
        Offset(ctx.w * 0.94f, ctx.h * 0.19f)
    )
    drawPolylineBar(pts, 46, ctx.rpmRatio, u * 3.9f, p.accent, p.track, ctx.redlineRatio, ctx.peakRatio, u)
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    for (k in 0..maxK step 2) {
        val frac = k / maxK.toFloat()
        val pt = pointOnCurve(pts, frac)
        gText(k.toString(), pt.x, pt.y - u * 2.8f, u * 1.85f, if (frac >= ctx.redlineRatio) RL_RED else p.muted, Paint.Align.CENTER, ctx.type.digits, true)
    }

    // Sol yarı: Gerçek KTM 390/790/890/1290 Duke "Favorites" 4 Satırlı Bilgi Paneli!
    val favs = listOf(
        "ODOMETER" to "${ctx.formatOdometer()} ${if (ctx.useMph) "mi" else "km"}",
        "TRIP 1" to "${ctx.formatTrip()} ${if (ctx.useMph) "mi" else "km"}",
        "SEANS MESAFE" to "${ctx.formatSession()} ${if (ctx.useMph) "mi" else "km"}",
        "COOLANT / RANGE" to "${t.coolantTempCelsius.roundToInt()}°C  •  ${(t.fuelLevelRatio * 340).roundToInt()} km"
    )
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
    drawBoomerangTft(ctx)
}

/* =====================================================================================
 *  APRILIA APRC WING TFT — RSV4 Factory / RS 660 / RS 457
 * ===================================================================================== */
private fun DrawScope.drawApriliaAprcWingTft(ctx: GaugeContext, isTrack: Boolean) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 4.2f, ctx.w * 0.30f, u)

    val cx = if (isTrack) ctx.w * 0.42f else ctx.w * 0.36f
    val cy = ctx.h * 0.50f
    val r = min(ctx.w * 0.25f, ctx.h * 0.35f)
    drawSegmentedArc(cx, cy, r, 135f, 270f, 52, ctx.rpmRatio, u * 4.0f, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.85f)
    var rpm = 0
    val step = if (ctx.maxRpm <= 11500.0) 1000 else 2000
    while (rpm <= ctx.maxRpm.roundToInt()) {
        val frac = rpm / ctx.maxRpm.toFloat()
        val pt = polar(cx, cy, r - u * 7.6f, 135f + 270f * frac)
        gTextCenter((rpm / 1000).toString(), pt.x, pt.y, u * 1.9f, if (frac >= ctx.redlineRatio) RL_RED else p.muted, ctx.type.label)
        rpm += step
    }

    if (isTrack) {
        drawGearPod(cx, cy + u * 1.0f, u * 14.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.74f, ctx.h * 0.38f, u * 19.0f, p.fg, p.muted, ctx.type.digits)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 3.5f, u * 18.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(cx, cy + r * 0.58f, u * 10.5f, p, t, ctx.isNeutral, u, ctx.type.digits, GearPodShape.HEX)
    }

    // Sağ kanat: Aprilia APRC (ATC / AWC / AEB / AEM / ABS) + ODO / TRIP / SEANS
    drawPanel(ctx.w * 0.64f, ctx.h * 0.18f, ctx.w * 0.31f, ctx.h * 0.64f, p, accentEdge = true)
    gText("APRILIA APRC // ${ctx.subMode.uppercase()}", ctx.w * 0.665f, ctx.h * 0.24f, u * 1.65f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
    val aprc = listOf("ATC (Traction)" to 3, "AWC (Wheelie)" to 1, "AEB (Engine Brake)" to 2, "AEM (Engine Map)" to 1)
    aprc.forEachIndexed { i, (name, lvl) ->
        val y = ctx.h * (0.31f + i * 0.075f)
        gText(name, ctx.w * 0.665f, y, u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("LVL $lvl", ctx.w * 0.925f, y, u * 1.6f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
    }
    gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.665f, ctx.h * 0.65f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.665f, ctx.h * 0.715f, u * 1.8f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
    gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.665f, ctx.h * 0.775f, u * 1.55f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

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

private fun pointOnCurve(pts: List<Offset>, frac: Float): Offset {
    if (pts.isEmpty()) return Offset.Zero
    val idx = (frac.coerceIn(0f, 1f) * (pts.size - 1)).toInt().coerceIn(0, pts.lastIndex)
    return pts[idx]
}

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
