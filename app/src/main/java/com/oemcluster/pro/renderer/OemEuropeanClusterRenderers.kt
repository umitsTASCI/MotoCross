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
 * 1:1 Avrupa OEM Gösterge Ailesi:
 * BMW Motorrad (Pure Ride, Core 1/2/3, GS, RT/K1600),
 * KTM / Husqvarna / GasGas (Gen2, Gen3 Sniper, 250 Duke LCD, Adventure),
 * Ducati (Panigale V4 6.9" 2025, Evo, Circular V2/Monster, 1098 Digitek, Scrambler),
 * Triumph (4-Tema 765/1200 RS, 660 Bölünmüş LCD+TFT),
 * MV Agusta (5.5" Hexagon TFT).
 * Tüm paneller hem yatay (wide) hem dikey (portrait) modda %100 ölçeklenir.
 */

// =========================================================================
// 1. BMW MOTORRAD — 6.5" Connectivity (Pure Ride, Core 3, GS, RT Split)
// =========================================================================

internal fun DrawScope.drawBmwPureRideTftReal(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val bmwBlue = Color(0xFF1C69D4)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.28f, u)

    // Üst BMW Connectivity durum çubuğu (Marka + Sıcaklık + Uydu + Saat)
    val hdrY = ctx.h * 0.065f
    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, hdrY, u * 4.2f, u, bmwBlue)
    gText(
        "PURE RIDE  •  ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}",
        ctx.w * 0.13f,
        hdrY + u * 0.9f,
        u * 1.65f,
        p.muted,
        Paint.Align.LEFT,
        ctx.type.label,
        true
    )
    gText(
        "${t.gpsSatelliteCount} SAT  •  ${clusterClockText(ctx.clock24h)}",
        ctx.w * 0.94f,
        hdrY + u * 0.9f,
        u * 2.0f,
        p.fg,
        Paint.Align.RIGHT,
        ctx.type.digits,
        true
    )

    // BMW Pure Ride signature kavisli ince üst devir yayı
    val barTop = ctx.h * 0.145f
    val barLeft = ctx.w * 0.07f
    val barW = ctx.w * 0.86f
    val barPts = List(28) { i ->
        val f = i / 27f
        val bx = barLeft + barW * f
        val lift = sin(f * PI).toFloat() * u * 3.2f
        Offset(bx, barTop + u * 4.5f - lift)
    }
    for (i in 0 until barPts.lastIndex) {
        val f = i / barPts.lastIndex.toFloat()
        val segOn = f <= ctx.rpmRatio
        val col = when {
            !segOn -> p.track
            f >= ctx.redlineRatio -> RL_RED
            f >= ctx.redlineRatio * 0.85f -> Color(0xFFFF6D00)
            else -> p.fg
        }
        drawLine(col, barPts[i], barPts[i + 1], u * 1.9f, StrokeCap.Butt)
    }

    if (ctx.wide) {
        // Büyük hız merkez-solda, BMW karakteristik sağ vites karesi
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.38f, ctx.h * 0.56f, u * 21.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.77f, ctx.h * 0.45f, u * 20.0f, bmwBlue, p.fg, ctx.type.digits)
        gText(
            "ODO ${ctx.formatOdometer()}  •  TRIP ${ctx.formatTrip()}",
            ctx.w * 0.07f,
            ctx.h * 0.76f,
            u * 1.95f,
            p.fg.copy(alpha = 0.84f),
            Paint.Align.LEFT,
            ctx.type.label,
            true
        )
        gText(
            "RANGE ${ctx.formatRange()}  •  ${ctx.formatBattery()}",
            ctx.w * 0.77f,
            ctx.h * 0.65f,
            u * 1.75f,
            p.muted,
            Paint.Align.CENTER,
            ctx.type.label,
            true
        )
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.42f, u * 23.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.62f, u * 18.0f, bmwBlue, p.fg, ctx.type.digits)
        gText(
            "ODO ${ctx.formatOdometer()}  •  RANGE ${ctx.formatRange()}",
            ctx.w * 0.50f,
            ctx.h * 0.77f,
            u * 2.1f,
            p.fg.copy(alpha = 0.86f),
            Paint.Align.CENTER,
            ctx.type.label,
            true
        )
    }

    drawSubModeChip(ctx, ctx.w * 0.07f, ctx.h - railH - u * 3.4f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawBmwCore3TrackLapTftReal(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val mBlue = Color(0xFF0099FF)
    val mPurple = Color(0xFF512DA8)
    val mRed = Color(0xFFE60012)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.34f, u)

    // M Sport üst şerit + Core 3 başlık
    val topY = ctx.h * 0.068f
    drawRect(mBlue, Offset(ctx.w * 0.045f, topY - u * 1.8f), Size(u * 2.6f, u * 0.9f))
    drawRect(mPurple, Offset(ctx.w * 0.045f + u * 2.9f, topY - u * 1.8f), Size(u * 2.6f, u * 0.9f))
    drawRect(mRed, Offset(ctx.w * 0.045f + u * 5.8f, topY - u * 1.8f), Size(u * 2.6f, u * 0.9f))
    gText("M CORE 3  //  TRACK", ctx.w * 0.045f + u * 9.8f, topY - u * 0.9f, u * 1.75f, p.fg, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.08f)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.955f, topY - u * 0.9f, u * 2.1f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    // BMW Core 3 karakteristik 4.000 RPM'den başlayan yatay yarış devir barı
    val minTrackRpm = if (ctx.maxRpm > 10000.0) 4000.0 else 2000.0
    val coreRatio = ((ctx.mappedRpm - minTrackRpm) / max(2000.0, ctx.maxRpm - minTrackRpm)).toFloat().coerceIn(0f, 1.05f)
    val barLeft = ctx.w * 0.055f
    val barW = ctx.w * 0.89f
    val barTop = ctx.h * 0.145f
    val barH = u * 4.6f
    val segs = 44
    val segW = barW / segs
    for (i in 0 until segs) {
        val f = (i + 1f) / segs
        val on = coreRatio >= f
        val col = when {
            !on -> p.track
            f >= 0.85f -> mRed
            f >= 0.68f -> Color(0xFFFF6D00)
            f >= 0.45f -> mBlue
            else -> Color(0xFF00E5FF)
        }
        drawRoundRect(
            col,
            Offset(barLeft + i * segW, barTop),
            Size(segW * 0.76f, barH),
            CornerRadius(u * 0.35f, u * 0.35f)
        )
    }
    val stepK = if (ctx.maxRpm >= 14000) 2 else 1
    var k = (minTrackRpm / 1000.0).roundToInt()
    val maxK = (ctx.maxRpm / 1000.0).roundToInt()
    while (k <= maxK) {
        val f = ((k * 1000.0 - minTrackRpm) / max(2000.0, ctx.maxRpm - minTrackRpm)).toFloat().coerceIn(0f, 1f)
        gText(
            k.toString(),
            barLeft + barW * f,
            barTop + barH + u * 3.1f,
            u * 1.65f,
            if (k * 1000.0 >= ctx.redlineRpm) mRed else p.fg.copy(alpha = 0.85f),
            Paint.Align.CENTER,
            ctx.type.digits,
            true
        )
        k += stepK
    }

    if (ctx.wide) {
        // Sol: Canlı Lean + Peak Lean göstergesi
        val leanPanelW = ctx.w * 0.25f
        val leanTop = ctx.h * 0.30f
        drawPanel(ctx.w * 0.055f, leanTop, leanPanelW, ctx.h * 0.30f, lerp(p.bg, Color.Black, 0.3f), mBlue.copy(alpha = 0.45f), u * 1.6f)
        gText("LEAN ANGLE", ctx.w * 0.055f + leanPanelW * 0.5f, leanTop + u * 3.2f, u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        val leanDir = if (t.leanAngleDeg < -1.0) "L" else if (t.leanAngleDeg > 1.0) "R" else "CTR"
        gText("$leanDir ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.055f + leanPanelW * 0.5f, leanTop + u * 10.5f, u * 5.2f, mBlue, Paint.Align.CENTER, ctx.type.digits, true)
        gText(
            "PEAK L ${ctx.peakLeanLeftDeg.roundToInt()}° / R ${ctx.peakLeanRightDeg.roundToInt()}°",
            ctx.w * 0.055f + leanPanelW * 0.5f,
            leanTop + u * 15.2f,
            u * 1.35f,
            p.fg.copy(alpha = 0.78f),
            Paint.Align.CENTER,
            ctx.type.label,
            true
        )

        // Orta: Vites ve Hız
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.48f, ctx.h * 0.49f, u * 18.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.68f, ctx.h * 0.45f, u * 16.0f, mRed, p.fg, ctx.type.digits)

        // Sağ: 0-100 & Peak Speed Gerçek Track Kartı
        val rightX = ctx.w * 0.78f
        val rightW = ctx.w * 0.165f
        drawPanel(rightX, leanTop, rightW, ctx.h * 0.30f, lerp(p.bg, Color.Black, 0.3f), mRed.copy(alpha = 0.45f), u * 1.6f)
        gText("0-100 BEST", rightX + rightW * 0.5f, leanTop + u * 3.2f, u * 1.4f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        gText(ctx.formatBest0to100(), rightX + rightW * 0.5f, leanTop + u * 8.2f, u * 2.8f, RL_GREEN, Paint.Align.CENTER, ctx.type.digits, true)
        gText("PEAK ${ctx.formatPeakSpeed()}", rightX + rightW * 0.5f, leanTop + u * 14.2f, u * 1.5f, p.fg, Paint.Align.CENTER, ctx.type.label, true)

        // Alt: BMW Core 3 imzası — DTC ve BRAKE/G canlı çubukları
        val mBarTop = ctx.h * 0.675f
        val halfBarW = ctx.w * 0.415f
        gText("DTC SLIP / INTERVENTION", ctx.w * 0.055f, mBarTop - u * 1.2f, u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        val dtcRatio = if (t.tractionControlInterventionActive) 0.92f else (t.wheelSlipRatioLambda.toFloat() * 4f).coerceIn(0.06f, 0.75f)
        drawLinearBar(ctx.w * 0.055f, mBarTop, halfBarW, u * 2.4f, dtcRatio, Color(0xFFFF9100), p.track, 0.75f, mRed, u * 0.6f)

        val brkRatio = ((-t.longitudinalG).coerceAtLeast(0.0) / 1.25).toFloat().coerceIn(0f, 1f)
        gText("DECEL / BRAKE ${String.format(java.util.Locale.US, "%.2f", abs(t.longitudinalG))} G", ctx.w * 0.53f, mBarTop - u * 1.2f, u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        drawLinearBar(ctx.w * 0.53f, mBarTop, halfBarW, u * 2.4f, brkRatio, mRed, p.track, 0.70f, mRed, u * 0.6f)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.40f, u * 21.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.57f, u * 16.0f, mRed, p.fg, ctx.type.digits)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°   •   0-100 ${ctx.formatBest0to100()}", ctx.w * 0.50f, ctx.h * 0.71f, u * 2.2f, mBlue, Paint.Align.CENTER, ctx.type.digits, true)
        gText("DECEL ${String.format(java.util.Locale.US, "%.2f", abs(t.longitudinalG))}G   •   PEAK ${ctx.formatPeakSpeed()}", ctx.w * 0.50f, ctx.h * 0.77f, u * 1.9f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawBmwGsAdventureTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val gsBlue = Color(0xFF0066B1)
    val sand = Color(0xFFFFB300)
    val offroad = ctx.subMode.contains("Offroad", true) || ctx.subMode.contains("Enduro", true)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.28f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.06f, ctx.h * 0.065f, u * 4.0f, u, gsBlue)
    gText(
        if (offroad) "GS // ENDURO PRO" else "GS // ROAD ADVENTURE",
        ctx.w * 0.125f,
        ctx.h * 0.075f,
        u * 1.85f,
        if (offroad) sand else gsBlue,
        Paint.Align.LEFT,
        ctx.type.label,
        true
    )
    gText("${t.gpsSatelliteCount} SAT  •  ${clusterClockText(ctx.clock24h)}", ctx.w * 0.945f, ctx.h * 0.075f, u * 1.95f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    drawCompassStrip(ctx, ctx.h * 0.105f, u * 4.0f)

    // Yatay mavi GS devir barı
    val barY = ctx.h * 0.185f
    drawLinearBar(ctx.w * 0.06f, barY, ctx.w * 0.88f, u * 3.0f, ctx.rpmRatio, if (offroad) sand else gsBlue, p.track, ctx.redlineRatio, RL_RED, u * 0.6f)

    if (ctx.wide) {
        val colW = ctx.w * 0.26f
        val topBox = ctx.h * 0.28f
        drawPanel(ctx.w * 0.055f, topBox, colW, ctx.h * 0.44f, lerp(p.bg, Color.Black, 0.28f), gsBlue.copy(alpha = 0.4f), u * 1.6f)
        gText("VEHICLE TELEMETRY", ctx.w * 0.075f, topBox + u * 3.4f, u * 1.45f, sand, Paint.Align.LEFT, ctx.type.label, true)
        gText("RANGE ${ctx.formatRange()}", ctx.w * 0.075f, topBox + u * 8.2f, u * 2.7f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("AVG ${ctx.formatAvgFuel()}", ctx.w * 0.075f, topBox + u * 13.0f, u * 2.1f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("TEMP ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", ctx.w * 0.075f, topBox + u * 17.5f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)

        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.48f, u * 19.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.68f, u * 13.5f, gsBlue, p.fg, ctx.type.digits)

        drawPanel(ctx.w * 0.685f, topBox, colW, ctx.h * 0.44f, lerp(p.bg, Color.Black, 0.28f), gsBlue.copy(alpha = 0.4f), u * 1.6f)
        gText("TRIP COMPUTER", ctx.w * 0.705f, topBox + u * 3.4f, u * 1.45f, gsBlue, Paint.Align.LEFT, ctx.type.label, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.705f, topBox + u * 8.2f, u * 2.3f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("PITCH  ${t.pitchAngleDeg.roundToInt()}°", ctx.w * 0.705f, topBox + u * 13.0f, u * 2.2f, sand, Paint.Align.LEFT, ctx.type.digits, true)
        gText("LEAN   ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.705f, topBox + u * 17.5f, u * 2.2f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.42f, u * 21.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.58f, u * 14.5f, gsBlue, p.fg, ctx.type.digits)
        gText("RANGE ${ctx.formatRange()}  •  TRIP ${ctx.formatTrip()}", ctx.w * 0.50f, ctx.h * 0.71f, u * 2.1f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°  •  PITCH ${t.pitchAngleDeg.roundToInt()}°", ctx.w * 0.50f, ctx.h * 0.77f, u * 2.0f, sand, Paint.Align.CENTER, ctx.type.digits, true)
    }

    drawSubModeChip(ctx, ctx.w * 0.055f, ctx.h - railH - u * 3.2f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawBmwRt1025SplitTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val blue = Color(0xFF1C69D4)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawBrandMark(ctx.spec, p, ctx.w * 0.05f, ctx.h * 0.065f, u * 4.0f, u, blue)
    gText("10.25\" TOURING COCKPIT", ctx.w * 0.11f, ctx.h * 0.073f, u * 1.75f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.95f, ctx.h * 0.073f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    val dialCx = if (ctx.wide) ctx.w * 0.28f else ctx.w * 0.50f
    val dialCy = if (ctx.wide) ctx.h * 0.50f else ctx.h * 0.36f
    val dialR = min(ctx.w * (if (ctx.wide) 0.22f else 0.34f), ctx.h * 0.34f)
    drawDialFace(dialCx, dialCy, dialR, p, chrome = true, cream = false, u = u)
    drawTickRing(
        cx = dialCx,
        cy = dialCy,
        radius = dialR - u * 1.4f,
        startAngle = 135f,
        sweepAngle = 270f,
        majorCount = 10,
        minorPerMajor = 4,
        color = p.fg,
        redlineRatio = 0.85f,
        u = u,
        labels = true,
        labelFamily = ctx.type.digits,
        labelSize = u * 2.2f,
        labelFormatter = { (it * (ctx.maxRpm / 10000.0)).roundToInt().toString() }
    )
    drawNeedle(dialCx, dialCy, 135f + 270f * ctx.rpmRatio.coerceIn(0f, 1f), dialR * 0.84f, u * 1.15f, blue)
    drawSpeed(ctx.speedValue, ctx.speedUnit, dialCx, dialCy + u * 3.0f, u * 10.5f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, dialCx + dialR * 0.65f, dialCy + dialR * 0.55f, u * 9.5f, blue, p.fg, ctx.type.digits)

    // Sağ (veya Alt) 10.25" Split Kart
    val cardX = if (ctx.wide) ctx.w * 0.54f else ctx.w * 0.07f
    val cardY = if (ctx.wide) ctx.h * 0.18f else ctx.h * 0.62f
    val cardW = if (ctx.wide) ctx.w * 0.41f else ctx.w * 0.86f
    val cardH = if (ctx.wide) ctx.h * 0.64f else ctx.h * 0.22f
    drawPanel(cardX, cardY, cardW, cardH, lerp(p.bg, Color.Black, 0.30f), blue.copy(alpha = 0.45f), u * 1.8f)
    gText("NAVIGATION & VEHICLE INFO", cardX + u * 2.4f, cardY + u * 3.5f, u * 1.55f, blue, Paint.Align.LEFT, ctx.type.label, true)
    gText("RANGE  ${ctx.formatRange()}   •   ${ctx.formatAvgFuel()}", cardX + u * 2.4f, cardY + u * 8.5f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("ODO ${ctx.formatOdometer()}   •   ${t.gpsSatelliteCount} SAT", cardX + u * 2.4f, cardY + u * 13.0f, u * 1.9f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    if (ctx.wide) {
        gText("AVG ${ctx.formatAvgSpeed()}   •   TIME ${ctx.formatRideTime()}", cardX + u * 2.4f, cardY + u * 17.8f, u * 1.9f, p.fg.copy(alpha = 0.86f), Paint.Align.LEFT, ctx.type.label, true)
    }

    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

// =========================================================================
// 2. KTM & HUSQVARNA — Sniper Gen3, 250 Duke Bonded LCD, Track, Bauhaus
// =========================================================================

internal fun DrawScope.drawKtmSniperGen3Tft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val ktmOrange = Color(0xFFFF6600)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.4f, ctx.w * 0.30f, u)

    // 5" bonding TFT çerçevesi (fotoğraftaki köşeleri kırpılmış panel)
    val panelL = ctx.w * 0.025f
    val panelT = ctx.h * 0.035f
    val panelW = ctx.w * 0.95f
    val panelH = ctx.h - railH - panelT - u * 1.6f
    drawRoundRect(
        Color(0xFF07080A),
        Offset(panelL, panelT), Size(panelW, panelH),
        androidx.compose.ui.geometry.CornerRadius(u * 2.2f, u * 2.2f)
    )
    drawRoundRect(
        ktmOrange.copy(alpha = 0.20f),
        Offset(panelL, panelT), Size(panelW, panelH),
        androidx.compose.ui.geometry.CornerRadius(u * 2.2f, u * 2.2f),
        style = Stroke(width = max(1f, u * 0.22f))
    )

    if (ctx.wide) {
        // --- Sol üst: vites + saat + sıcaklık (gerçek Gen 3 düzeni) ---
        gText(
            ctx.gearText,
            ctx.w * 0.075f, ctx.h * 0.245f, u * 17.0f,
            if (ctx.isNeutral) RL_GREEN else p.fg,
            Paint.Align.LEFT, ctx.type.digits, true
        )

        gText(clusterClockText(ctx.clock24h), ctx.w * 0.155f, ctx.h * 0.130f, u * 2.3f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText(
            formatTemp(t.coolantTempCelsius, ctx.useFahrenheit),
            ctx.w * 0.155f, ctx.h * 0.185f, u * 1.9f, p.muted,
            Paint.Align.LEFT, ctx.type.digits, true
        )

        // --- Çapraz devir çizgisi: sol alttan sağ üste (fotoğraftaki gibi ince) ---
        val tachStart = Offset(ctx.w * 0.205f, ctx.h * 0.575f)
        val tachEnd = Offset(ctx.w * 0.775f, ctx.h * 0.205f)
        drawLine(p.track, tachStart, tachEnd, u * 0.85f, StrokeCap.Round)
        val redStart = Offset(
            tachStart.x + (tachEnd.x - tachStart.x) * ctx.redlineRatio,
            tachStart.y + (tachEnd.y - tachStart.y) * ctx.redlineRatio
        )
        drawLine(RL_RED.copy(alpha = 0.55f), redStart, tachEnd, u * 1.15f, StrokeCap.Round)
        val fillEnd = Offset(
            tachStart.x + (tachEnd.x - tachStart.x) * ctx.rpmRatio.coerceIn(0f, 1f),
            tachStart.y + (tachEnd.y - tachStart.y) * ctx.rpmRatio.coerceIn(0f, 1f)
        )
        drawLine(Color.White, tachStart, fillEnd, u * 1.15f, StrokeCap.Round)
        // Başlangıç işareti (küçük beyaz üçgen)
        val marker = Path().apply {
            moveTo(tachStart.x - u * 0.6f, tachStart.y + u * 1.5f)
            lineTo(tachStart.x + u * 0.9f, tachStart.y + u * 1.5f)
            lineTo(tachStart.x + u * 0.15f, tachStart.y - u * 0.5f)
            close()
        }
        drawPath(marker, Color.White.copy(alpha = 0.90f))
        gText("10", ctx.w * 0.79f, ctx.h * 0.185f, u * 1.8f, RL_RED, Paint.Align.LEFT, ctx.type.digits, true)

        // --- Merkez: hız ---
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.615f, u * 15.0f, p.fg, p.muted, ctx.type.digits)

        // --- Sağ üst: sürüş modu karesi (Z ikonu) ---
        val boxS = u * 9.5f
        val boxL = ctx.w * 0.865f - boxS * 0.5f
        val boxT = ctx.h * 0.155f
        drawRoundRect(
            p.fg.copy(alpha = 0.85f),
            Offset(boxL, boxT), Size(boxS, boxS),
            androidx.compose.ui.geometry.CornerRadius(u * 1.2f, u * 1.2f),
            style = Stroke(width = max(1f, u * 0.30f))
        )
        val z = Path().apply {
            moveTo(boxL + boxS * 0.24f, boxT + boxS * 0.27f)
            lineTo(boxL + boxS * 0.76f, boxT + boxS * 0.27f)
            lineTo(boxL + boxS * 0.24f, boxT + boxS * 0.73f)
            lineTo(boxL + boxS * 0.76f, boxT + boxS * 0.73f)
        }
        drawPath(z, p.fg, style = Stroke(width = u * 0.55f))

        // --- Alt ikon/değer satırı (KTM LAUNCH | ABS | MTC | TIMER | DTC) ---
        drawKtmGen3StatusRow(ctx, panelL, panelW, ctx.h * 0.735f, railH, ktmOrange)
    } else {
        // Dikey: aynı düzen sıkıştırılmış
        gText(
            if (ctx.isNeutral) "N" else ctx.gearText,
            ctx.w * 0.09f, ctx.h * 0.22f, u * 15.0f,
            if (ctx.isNeutral) RL_GREEN else p.fg,
            Paint.Align.LEFT, ctx.type.digits, true
        )
        gText(clusterClockText(ctx.clock24h), ctx.w * 0.20f, ctx.h * 0.13f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText(formatTemp(t.coolantTempCelsius, ctx.useFahrenheit), ctx.w * 0.20f, ctx.h * 0.18f, u * 1.8f, p.muted, Paint.Align.LEFT, ctx.type.digits, true)

        val tachStart = Offset(ctx.w * 0.14f, ctx.h * 0.50f)
        val tachEnd = Offset(ctx.w * 0.86f, ctx.h * 0.20f)
        drawLine(p.track, tachStart, tachEnd, u * 0.8f, StrokeCap.Round)
        val redStart = Offset(
            tachStart.x + (tachEnd.x - tachStart.x) * ctx.redlineRatio,
            tachStart.y + (tachEnd.y - tachStart.y) * ctx.redlineRatio
        )
        drawLine(RL_RED.copy(alpha = 0.55f), redStart, tachEnd, u * 1.1f, StrokeCap.Round)
        val fillEnd = Offset(
            tachStart.x + (tachEnd.x - tachStart.x) * ctx.rpmRatio.coerceIn(0f, 1f),
            tachStart.y + (tachEnd.y - tachStart.y) * ctx.rpmRatio.coerceIn(0f, 1f)
        )
        drawLine(Color.White, tachStart, fillEnd, u * 1.1f, StrokeCap.Round)

        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.60f, u * 16.0f, p.fg, p.muted, ctx.type.digits)
        gText("MTC ${if (t.tractionControlInterventionActive) "ACT" else "ON"}   •   ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", ctx.w * 0.50f, ctx.h * 0.74f, u * 1.85f, ktmOrange, Paint.Align.CENTER, ctx.type.label, true)
    }

    drawSubModeChip(ctx, ctx.w * 0.075f, ctx.h - railH - u * 3.2f)
    drawStatusRail(ctx, RailStyle.TFT)
}

/** KTM Gen 3 gerçek alt satırı: ince ayırıcılar + ikon, değer ve etiket. */
private fun DrawScope.drawKtmGen3StatusRow(
    ctx: GaugeContext,
    panelLeft: Float,
    panelWidth: Float,
    top: Float,
    railH: Float,
    orange: Color
) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val left = panelLeft + panelWidth * 0.035f
    val width = panelWidth * 0.93f
    val cell = width / 5f
    val sepTop = top - u * 0.8f
    val sepBottom = min(ctx.h - railH - u * 1.4f, top + u * 8.2f)

    val timerSec = ctx.rideTimeSeconds.coerceAtLeast(0.0)
    val timerText = "%02d:%02d.%03d".format(
        java.util.Locale.US,
        (timerSec / 60).toInt(),
        (timerSec % 60).toInt(),
        ((timerSec * 1000).toLong() % 1000).toInt()
    )
    val values = listOf(
        "3",
        if (ctx.subMode.contains("Super", ignoreCase = true)) "Supermoto" else "Road",
        if (t.tractionControlInterventionActive) "ACT" else "ON",
        timerText,
        "6"
    )
    val labels = listOf("KTM LAUNCH", "ABS", "MTC", "TIMER", "DTC")

    for (i in 0 until 5) {
        val cx = left + cell * (i + 0.5f)
        if (i > 0) {
            drawLine(
                p.fg.copy(alpha = 0.14f),
                Offset(left + cell * i, sepTop),
                Offset(left + cell * i, sepBottom),
                max(1f, u * 0.12f)
            )
        }
        // Küçük ikon
        when (i) {
            0 -> {
                val tri = Path().apply {
                    moveTo(cx, top - u * 1.9f)
                    lineTo(cx + u * 1.1f, top - u * 0.1f)
                    lineTo(cx - u * 1.1f, top - u * 0.1f)
                    close()
                }
                drawPath(tri, p.muted.copy(alpha = 0.75f), style = Stroke(width = max(1f, u * 0.18f)))
            }
            1 -> drawCircle(p.muted.copy(alpha = 0.55f), u * 0.95f, Offset(cx, top - u * 1.0f), style = Stroke(width = max(1f, u * 0.18f)))
            2 -> gText("MTC", cx, top - u * 0.4f, u * 0.95f, p.muted.copy(alpha = 0.75f), Paint.Align.CENTER, ctx.type.label, true)
            3 -> {
                drawCircle(p.muted.copy(alpha = 0.55f), u * 0.90f, Offset(cx, top - u * 1.0f), style = Stroke(width = max(1f, u * 0.18f)))
                drawLine(p.muted.copy(alpha = 0.75f), Offset(cx, top - u * 2.1f), Offset(cx, top - u * 1.7f), max(1f, u * 0.18f))
            }
            else -> for (dx in -1..1) for (dy in -1..1) {
                drawCircle(p.muted.copy(alpha = 0.65f), u * 0.14f, Offset(cx + dx * u * 0.55f, top - u * 1.0f + dy * u * 0.55f))
            }
        }
        val valueSize = if (i == 3) u * 1.75f else if (i == 1) u * 1.95f else u * 2.25f
        gText(values[i], cx, top + u * 2.8f, valueSize, if (i == 0) orange else p.fg, Paint.Align.CENTER, ctx.type.digits, i != 1)
        gText(labels[i], cx, top + u * 5.5f, u * 1.05f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.06f)
    }
}

internal fun DrawScope.drawKtm250Duke2024Lcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val orange = Color(0xFFFF6600)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    // Bonded koyu LCD plaka hissi
    val cardLeft = ctx.w * 0.045f
    val cardTop = ctx.h * 0.06f
    val cardW = ctx.w * 0.91f
    val cardH = ctx.h - railH - cardTop - u * 2.5f
    drawPanel(cardLeft, cardTop, cardW, cardH, Color(0xFF0A0D13), orange.copy(alpha = 0.55f), u * 1.8f)

    // 250 Duke 2024+ 5" Bonded LCD karakteristik üst-sol yükselip sağa düzleşen segment bar
    val segPts = List(32) { i ->
        val f = i / 31f
        val bx = cardLeft + cardW * (0.06f + f * 0.88f)
        val by = if (f < 0.28f) {
            cardTop + cardH * (0.38f - (f / 0.28f) * 0.24f)
        } else {
            cardTop + cardH * 0.14f
        }
        Offset(bx, by)
    }
    drawPolylineBar(segPts, 32, ctx.rpmRatio, u * 3.8f, Color.White, Color(0xFF202632), ctx.redlineRatio, ctx.peakRatio, u)

    gText("KTM 250 DUKE", cardLeft + u * 2.6f, cardTop + cardH * 0.22f, u * 1.55f, orange, Paint.Align.LEFT, "monospace", true)
    gText(clusterClockText(ctx.clock24h), cardLeft + cardW - u * 2.6f, cardTop + cardH * 0.22f, u * 2.0f, Color.White, Paint.Align.RIGHT, "monospace", true)

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.44f else 0.50f), cardTop + cardH * 0.54f, u * 19.0f, Color.White, Color(0xFF8C96A8), "monospace")
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.77f else 0.50f), cardTop + cardH * (if (ctx.wide) 0.52f else 0.74f), u * 16.0f, orange, Color.White, "monospace")

    gText(
        "RANGE ${ctx.formatRange()}   •   ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}",
        cardLeft + u * 2.6f,
        cardTop + cardH - u * 2.4f,
        u * 1.65f,
        Color(0xFFB0BAC8),
        Paint.Align.LEFT,
        "monospace",
        true
    )
    drawStatusRail(ctx, RailStyle.LCD)
}

internal fun DrawScope.drawKtmTrackRaceTftReal(ctx: GaugeContext) {
    // KTM Track Mode: tam yarış çerçevesi, geniş üst turuncu-beyaz bar, orta vites, gerçek 0-100 & peak
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val orange = Color(0xFFFF6600)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.34f, u)
    gText("KTM TRACK RACE", ctx.w * 0.055f, ctx.h * 0.072f, u * 2.0f, orange, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)
    gText("RPM ${ctx.mappedRpm.roundToInt()}", ctx.w * 0.945f, ctx.h * 0.072f, u * 2.2f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    val barLeft = ctx.w * 0.055f
    val barW = ctx.w * 0.89f
    val barTop = ctx.h * 0.135f
    drawLinearBar(barLeft, barTop, barW, u * 5.2f, ctx.rpmRatio, orange, p.track, ctx.redlineRatio, RL_RED, u * 0.7f)

    if (ctx.wide) {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.29f, ctx.h * 0.53f, u * 18.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.56f, ctx.h * 0.51f, u * 19.0f, orange, p.fg, ctx.type.digits)

        val panelX = ctx.w * 0.70f
        drawPanel(panelX, ctx.h * 0.28f, ctx.w * 0.245f, ctx.h * 0.45f, lerp(p.bg, Color.Black, 0.32f), orange.copy(alpha = 0.5f), u * 1.6f)
        gText("0-60 BEST", panelX + u * 2.0f, ctx.h * 0.34f, u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText(ctx.formatBest0to60(), panelX + u * 2.0f, ctx.h * 0.405f, u * 2.9f, RL_GREEN, Paint.Align.LEFT, ctx.type.digits, true)
        gText("0-100 BEST", panelX + u * 2.0f, ctx.h * 0.485f, u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText(ctx.formatBest0to100(), panelX + u * 2.0f, ctx.h * 0.55f, u * 2.9f, orange, Paint.Align.LEFT, ctx.type.digits, true)
        gText("PEAK ${ctx.formatPeakSpeed()}", panelX + u * 2.0f, ctx.h * 0.65f, u * 1.75f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.40f, u * 21.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.59f, u * 17.0f, orange, p.fg, ctx.type.digits)
        gText("0-100 ${ctx.formatBest0to100()}   •   PEAK ${ctx.formatPeakSpeed()}", ctx.w * 0.50f, ctx.h * 0.75f, u * 2.0f, RL_GREEN, Paint.Align.CENTER, ctx.type.digits, true)
    }

    drawSubModeChip(ctx, ctx.w * 0.055f, ctx.h - railH - u * 3.2f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawKtmAdventureTowerTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val orange = Color(0xFFFF6600)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawBrandMark(ctx.spec, p, ctx.w * 0.07f, ctx.h * 0.065f, u * 4.0f, u, orange)
    gText(ctx.spec.modelName.uppercase(), ctx.w * 0.14f, ctx.h * 0.072f, u * 1.65f, orange, Paint.Align.LEFT, ctx.type.label, true)
    drawCompassStrip(ctx, ctx.h * 0.10f, u * 3.8f)

    // Dikey / kule görünümlü Rally/Adventure merkezi
    drawLinearBar(ctx.w * 0.08f, ctx.h * 0.175f, ctx.w * 0.84f, u * 3.2f, ctx.rpmRatio, orange, p.track, ctx.redlineRatio, RL_RED, u * 0.6f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.36f else 0.50f), ctx.h * 0.44f, u * 19.0f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.68f else 0.50f), ctx.h * (if (ctx.wide) 0.44f else 0.61f), u * 15.0f, orange, p.fg, ctx.type.digits)

    val infoY = ctx.h * 0.72f
    gText("TRIP A  ${ctx.formatTrip()}   •   RANGE  ${ctx.formatRange()}", ctx.w * 0.50f, infoY, u * 2.0f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
    gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°   •   ${t.gpsSatelliteCount} SAT   •   ${ctx.formatBattery()}", ctx.w * 0.50f, infoY + u * 4.2f, u * 1.75f, orange, Paint.Align.CENTER, ctx.type.label, true)
    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawHusqvarnaBauhaus2024Tft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val voltYellow = Color(0xFFD6FF00)
    val iceCyan = Color(0xFF00E5FF)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val cx = ctx.w * (if (ctx.wide) 0.36f else 0.50f)
    val cy = ctx.h * (if (ctx.wide) 0.48f else 0.41f)
    val r = min(ctx.w * (if (ctx.wide) 0.30f else 0.38f), (ctx.h - railH) * 0.42f)

    // İskandinav Bauhaus halka tasarımı
    drawCircle(Color(0xFF0B0D10), r * 1.06f, Offset(cx, cy))
    drawCircle(Color(0xFF282E38), r * 1.06f, Offset(cx, cy), style = Stroke(width = u * 0.8f))
    drawSegmentedArc(cx, cy, r * 0.90f, 140f, 260f, 44, ctx.rpmRatio, u * 3.8f, iceCyan, Color(0xFF1A2029), ctx.redlineRatio, voltYellow, ctx.peakRatio, Color.White, 1.0f)
    drawBrandMark(ctx.spec, p, cx, cy - r * 0.48f, u * 4.2f, u, voltYellow)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 2.5f, u * 16.0f, Color.White, Color(0xFF909AA8), ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.62f, u * 11.5f, voltYellow, Color.White, ctx.type.digits)

    if (ctx.wide) {
        val cardX = ctx.w * 0.66f
        drawPanel(cardX, ctx.h * 0.18f, ctx.w * 0.28f, ctx.h * 0.62f, Color(0xFF0C0F14), voltYellow.copy(alpha = 0.45f), u * 1.6f)
        gText("SVARTPILEN / VITPILEN", cardX + u * 2.0f, ctx.h * 0.24f, u * 1.45f, voltYellow, Paint.Align.LEFT, ctx.type.label, true)
        gText("MODE: ${ctx.subMode.uppercase()}", cardX + u * 2.0f, ctx.h * 0.32f, u * 2.1f, iceCyan, Paint.Align.LEFT, ctx.type.label, true)
        gText("RANGE: ${ctx.formatRange()}", cardX + u * 2.0f, ctx.h * 0.42f, u * 2.1f, Color.White, Paint.Align.LEFT, ctx.type.digits, true)
        gText("LEAN: ${abs(t.leanAngleDeg).roundToInt()}°", cardX + u * 2.0f, ctx.h * 0.51f, u * 2.1f, Color.White, Paint.Align.LEFT, ctx.type.digits, true)
        gText("ODO: ${ctx.formatOdometer()}", cardX + u * 2.0f, ctx.h * 0.61f, u * 1.75f, Color(0xFF909AA8), Paint.Align.LEFT, ctx.type.label, true)
    } else {
        gText("RANGE ${ctx.formatRange()}   •   ODO ${ctx.formatOdometer()}", ctx.w * 0.50f, ctx.h * 0.77f, u * 1.95f, voltYellow, Paint.Align.CENTER, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawHusqvarnaNorden901Tft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val yellow = Color(0xFFFFD600)
    val cyan = Color(0xFF00B8D4)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawBrandMark(ctx.spec, p, ctx.w * 0.07f, ctx.h * 0.068f, u * 4.0f, u, yellow)
    gText("NORDEN 901 // EXPEDITION", ctx.w * 0.14f, ctx.h * 0.076f, u * 1.75f, cyan, Paint.Align.LEFT, ctx.type.label, true)
    drawLinearBar(ctx.w * 0.06f, ctx.h * 0.145f, ctx.w * 0.88f, u * 3.8f, ctx.rpmRatio, cyan, p.track, ctx.redlineRatio, yellow, u * 0.6f)

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), ctx.h * 0.46f, u * 19.0f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.72f else 0.50f), ctx.h * (if (ctx.wide) 0.44f else 0.64f), u * 16.0f, yellow, p.fg, ctx.type.digits)
    gText("TRIP A ${ctx.formatTrip()}  •  TRIP B ${ctx.formatTripB()}  •  ${ctx.formatRange()}", ctx.w * 0.50f, ctx.h - railH - u * 4.0f, u * 1.8f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    drawStatusRail(ctx, RailStyle.TFT)
}

// =========================================================================
// 3. DUCATI — 2025 Panigale V4 6.9" 8:3 Wide, Circular V2/Monster, 1098, Scrambler
// =========================================================================

internal fun DrawScope.drawDucatiV4Wide2025Tft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val ducRed = Color(0xFFE10600)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.6f, ctx.w * 0.38f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.07f, ctx.h * 0.072f, u * 4.2f, u, ducRed)
    gText("PANIGALE V4  //  ${ctx.subMode.uppercase()}", ctx.w * 0.145f, ctx.h * 0.082f, u * 1.75f, p.fg, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.08f)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.94f, ctx.h * 0.082f, u * 2.1f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    // 2025 8:3 Geniş şerit devir bandı (sarı -> kırmızı shift noktası + beyaz üst çizgi)
    val pts = ArrayList<Offset>(36)
    for (i in 0 until 36) {
        val f = i / 35f
        val x = ctx.w * (0.055f + f * 0.89f)
        val y = if (f < 0.22f) ctx.h * (0.29f - (f / 0.22f) * 0.14f) else ctx.h * 0.15f
        pts.add(Offset(x, y))
    }
    drawPolylineBar(pts, 48, ctx.rpmRatio, u * 4.6f, Color(0xFF4CD964), p.track, ctx.redlineRatio, ctx.peakRatio, u)

    val infoMode = ctx.subMode.contains("Info", true) || ctx.subMode.contains("Track", true)
    if (ctx.wide) {
        // Sol: Ducati Altıgen Vites Çerçevesi
        val gearCx = ctx.w * 0.165f
        val gearCy = ctx.h * 0.53f
        val hex = Path().apply {
            val rad = u * 12.5f
            for (k in 0 until 6) {
                val a = Math.toRadians(k * 60.0 + 30.0)
                val px = gearCx + cos(a).toFloat() * rad
                val py = gearCy + sin(a).toFloat() * rad
                if (k == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(hex, lerp(p.bg, Color.Black, 0.45f))
        drawPath(hex, ducRed, style = Stroke(width = u * 0.7f))
        gText(ctx.gearText, gearCx, gearCy + u * 5.2f, u * 15.0f, if (ctx.isNeutral) RL_GREEN else Color.White, Paint.Align.CENTER, ctx.type.digits, true)

        // Merkez: Dev hız + Ducati Power/Torque canlı çubukları
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.45f, ctx.h * 0.50f, u * 21.0f, p.fg, p.muted, ctx.type.digits)
        val pwrPct = ((t.estimatedWheelHorsepower / max(40.0, ctx.activeBike?.maxPowerHp ?: 215.0)) * 100.0).coerceIn(0.0, 100.0)
        gText("WHEEL POWER ${t.estimatedWheelHorsepower.roundToInt()} HP (${pwrPct.roundToInt()}%)", ctx.w * 0.45f, ctx.h * 0.68f, u * 1.75f, ducRed, Paint.Align.CENTER, ctx.type.label, true)

        // Sağ: G-Meter dairesi & CANLI DTC / DWC / ABS
        val gx = ctx.w * 0.80f
        val gy = ctx.h * 0.46f
        val gr = u * 10.5f
        drawCircle(lerp(p.bg, Color.Black, 0.35f), gr, Offset(gx, gy))
        drawCircle(p.fg.copy(alpha = 0.25f), gr, Offset(gx, gy), style = Stroke(width = u * 0.35f))
        drawLine(p.fg.copy(alpha = 0.16f), Offset(gx - gr, gy), Offset(gx + gr, gy), 1f)
        drawLine(p.fg.copy(alpha = 0.16f), Offset(gx, gy - gr), Offset(gx, gy + gr), 1f)
        val dotX = (gx + (t.lateralG.toFloat() / 1.5f).coerceIn(-1f, 1f) * gr * 0.85f)
        val dotY = (gy - (t.longitudinalG.toFloat() / 1.5f).coerceIn(-1f, 1f) * gr * 0.85f)
        drawCircle(ducRed, u * 1.5f, Offset(dotX, dotY))
        gText(if (infoMode) "0-100 ${ctx.formatBest0to100()}" else "LEAN ${abs(t.leanAngleDeg).roundToInt()}°", gx, gy + gr + u * 4.2f, u * 1.95f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.42f, u * 21.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.60f, u * 17.0f, ducRed, p.fg, ctx.type.digits)
        gText("${t.estimatedWheelHorsepower.roundToInt()} HP  •  LEAN ${abs(t.leanAngleDeg).roundToInt()}°  •  0-100 ${ctx.formatBest0to100()}", ctx.w * 0.50f, ctx.h * 0.74f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    }

    drawSubModeChip(ctx, ctx.w * 0.055f, ctx.h - railH - u * 3.2f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawDucatiRoadCircularTftReal(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val ducRed = Color(0xFFE10600)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.28f, u)

    val cx = ctx.w * (if (ctx.wide) 0.44f else 0.50f)
    val cy = ctx.h * (if (ctx.wide) 0.49f else 0.42f)
    val r = min(ctx.w * (if (ctx.wide) 0.26f else 0.36f), (ctx.h - railH) * 0.40f)

    drawSegmentedArc(cx, cy, r, 138f, 264f, 48, ctx.rpmRatio, u * 4.0f, p.fg, p.track, ctx.redlineRatio, ducRed, ctx.peakRatio, Color.White, 1.0f)
    drawArcTrack(cx, cy, r - u * 4.4f, 138f, 264f, u * 0.5f, ducRed.copy(alpha = 0.28f))
    drawBrandMark(ctx.spec, p, cx, cy - r * 0.52f, u * 4.0f, u, ducRed)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 2.5f, u * 16.5f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.58f, u * 11.5f, ducRed, p.fg, ctx.type.digits)

    if (ctx.wide) {
        val leftX = ctx.w * 0.045f
        drawPanel(leftX, ctx.h * 0.24f, ctx.w * 0.16f, ctx.h * 0.42f, lerp(p.bg, Color.Black, 0.3f), ducRed.copy(alpha = 0.45f), u * 1.4f)
        gText("MODE", leftX + u * 1.6f, ctx.h * 0.31f, u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText(ctx.subMode.uppercase(), leftX + u * 1.6f, ctx.h * 0.38f, u * 2.6f, ducRed, Paint.Align.LEFT, ctx.type.label, true)
        gText("RANGE", leftX + u * 1.6f, ctx.h * 0.48f, u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText(ctx.formatRange(), leftX + u * 1.6f, ctx.h * 0.55f, u * 2.2f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)

        val rx = ctx.w * 0.72f
        val rw = ctx.w * 0.235f
        drawPanel(rx, ctx.h * 0.24f, rw, ctx.h * 0.46f, lerp(p.bg, Color.Black, 0.3f), ducRed.copy(alpha = 0.45f), u * 1.4f)
        gText("ELECTRONICS", rx + u * 1.8f, ctx.h * 0.31f, u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("DTC  ${if (t.tractionControlInterventionActive) "ACT" else "ON"}", rx + u * 1.8f, ctx.h * 0.38f, u * 2.0f, if (t.tractionControlInterventionActive) RL_AMBER else Color(0xFF00E676), Paint.Align.LEFT, ctx.type.label, true)
        gText("DWC  ${if (t.isWheelieActive) "${t.wheelieAngleDeg.roundToInt()}°" else "ARM"}", rx + u * 1.8f, ctx.h * 0.45f, u * 2.0f, Color(0xFFFF9100), Paint.Align.LEFT, ctx.type.label, true)
        gText("ABS  ${if (t.absInterventionActive) "ACT" else "CORN"}", rx + u * 1.8f, ctx.h * 0.52f, u * 2.0f, Color(0xFF29B6F6), Paint.Align.LEFT, ctx.type.label, true)
        gText("${ctx.formatBattery()}  •  ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", rx + u * 1.8f, ctx.h * 0.62f, u * 1.55f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    } else {
        gText("MODE ${ctx.subMode.uppercase()}  •  RANGE ${ctx.formatRange()}  •  ${ctx.formatBattery()}", ctx.w * 0.50f, ctx.h * 0.77f, u * 1.85f, ducRed, Paint.Align.CENTER, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawDucatiDigitek1098Lcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val ducRed = Color(0xFFE10600)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val left = ctx.w * 0.06f
    val top = ctx.h * 0.08f
    val pw = ctx.w * 0.88f
    val ph = ctx.h - railH - top - u * 3.0f
    drawPanel(left, top, pw, ph, Color(0xFF101316), Color(0xFF39424D), u * 1.6f)
    gText("DUCATI  //  DIGITEK CORSE", left + u * 2.4f, top + u * 3.5f, u * 1.65f, ducRed, Paint.Align.LEFT, "monospace", true)

    // 1098/1198 ikonik soldan sağa yükselen LCD merdiven takometre
    val segs = 36
    val barX = left + u * 2.4f
    val barW = pw - u * 4.8f
    val segW = barW / segs
    for (i in 0 until segs) {
        val f = (i + 1f) / segs
        val on = ctx.rpmRatio >= f
        val col = if (!on) Color(0xFF20252C) else if (f >= ctx.redlineRatio) ducRed else Color(0xFFEAF2FF)
        val hScale = 0.28f + 0.72f * (i / segs.toFloat())
        val sh = ph * 0.24f * hScale
        drawRect(col, Offset(barX + i * segW, top + ph * 0.34f - sh), Size(segW * 0.72f, sh))
    }

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.42f, top + ph * 0.64f, u * 18.0f, Color(0xFFEAF2FF), Color(0xFF7D8A99), "monospace")
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.78f, top + ph * 0.64f, u * 15.0f, ducRed, Color.White, "monospace")
    gText("ODO ${ctx.formatOdometer()}  •  ${ctx.formatBattery()}  •  ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", left + u * 2.4f, top + ph - u * 2.4f, u * 1.6f, Color(0xFF9FB0C2), Paint.Align.LEFT, "monospace", true)
    drawStatusRail(ctx, RailStyle.LCD)
}

internal fun DrawScope.drawDucatiScrambler800Tft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val scamYellow = Color(0xFFFFC107)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val wBox = ctx.w * 0.88f
    val hBox = (ctx.h - railH) * 0.74f
    val left = ctx.w * 0.06f
    val top = ctx.h * 0.08f
    // Asimetrik retro-oval Scrambler ekran çerçevesi
    drawRoundRect(Color(0xFF0E1014), Offset(left, top), Size(wBox, hBox), CornerRadius(hBox * 0.46f, hBox * 0.46f))
    drawRoundRect(scamYellow.copy(alpha = 0.75f), Offset(left, top), Size(wBox, hBox), CornerRadius(hBox * 0.46f, hBox * 0.46f), style = Stroke(width = u * 0.85f))

    val arcCx = left + hBox * 0.48f
    val arcCy = top + hBox * 0.50f
    drawSegmentedArc(arcCx, arcCy, hBox * 0.39f, 115f, 150f, 34, ctx.rpmRatio, u * 3.6f, scamYellow, Color(0xFF232830), ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, left + wBox * 0.56f, arcCy + u * 2.5f, u * 18.0f, Color.White, Color(0xFF9AA4B2), ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, left + wBox * 0.84f, arcCy, u * 13.0f, scamYellow, Color.White, ctx.type.digits)
    gText("SCRAMBLER  •  ${ctx.formatRange()}  •  ${clusterClockText(ctx.clock24h)}", left + wBox * 0.50f, top + hBox - u * 2.6f, u * 1.7f, scamYellow, Paint.Align.CENTER, ctx.type.label, true)
    drawStatusRail(ctx, RailStyle.TFT)
}

// =========================================================================
// 4. TRIUMPH, MV AGUSTA & APRILIA
// =========================================================================

internal fun DrawScope.drawTriumphMultiStyleTftReal(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val theme = ctx.subModeIndex % 4
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f
    val primaryCol = when (theme) {
        1 -> Color(0xFF00E5FF)
        2 -> Color(0xFFFFAB00)
        3 -> Color(0xFFFF3D00)
        else -> p.accent
    }

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.30f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, ctx.h * 0.068f, u * 4.0f, u, primaryCol)
    gText("TRIUMPH  //  STYLE ${theme + 1}", ctx.w * 0.13f, ctx.h * 0.076f, u * 1.75f, primaryCol, Paint.Align.LEFT, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, ctx.h * 0.076f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    when (theme) {
        0, 1 -> {
            // İki taraftan ortaya birleşen imza "Pincer / Wings" devir çubukları
            val segs = 22
            val wingW = ctx.w * 0.38f
            val yTop = ctx.h * 0.20f
            for (i in 0 until segs) {
                val f = (i + 1f) / segs
                val on = ctx.rpmRatio >= f
                val col = if (!on) p.track else if (f >= ctx.redlineRatio) RL_RED else primaryCol
                val segW = (wingW / segs) * 0.74f
                val rise = sin(f * PI * 0.5).toFloat() * u * 3.0f
                val lx = ctx.w * 0.06f + (i / segs.toFloat()) * wingW
                val rx = ctx.w * 0.94f - (i / segs.toFloat()) * wingW - segW
                drawRoundRect(col, Offset(lx, yTop - rise), Size(segW, u * 4.2f), CornerRadius(u * 0.4f, u * 0.4f))
                drawRoundRect(col, Offset(rx, yTop - rise), Size(segW, u * 4.2f), CornerRadius(u * 0.4f, u * 0.4f))
            }
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.49f, u * 20.0f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.78f else 0.50f), ctx.h * (if (ctx.wide) 0.50f else 0.68f), u * 15.0f, primaryCol, p.fg, ctx.type.digits)
        }
        2 -> {
            // Tema 3: Radial Chrono büyük tek halka + büyük orta vites
            val cx = ctx.w * 0.50f
            val cy = ctx.h * 0.49f
            val r = min(ctx.w * 0.34f, (ctx.h - railH) * 0.38f)
            drawSegmentedArc(cx, cy, r, 140f, 260f, 44, ctx.rpmRatio, u * 3.8f, primaryCol, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
            drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 2.5f, u * 16.5f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.62f, u * 11.5f, primaryCol, p.fg, ctx.type.digits)
        }
        else -> {
            // Tema 4: Track Linear
            drawLinearBar(ctx.w * 0.06f, ctx.h * 0.16f, ctx.w * 0.88f, u * 5.0f, ctx.rpmRatio, primaryCol, p.track, ctx.redlineRatio, RL_RED, u * 0.8f)
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.34f else 0.50f), ctx.h * 0.49f, u * 19.5f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.68f else 0.50f), ctx.h * (if (ctx.wide) 0.49f else 0.66f), u * 16.5f, primaryCol, p.fg, ctx.type.digits)
            gText("0-100 ${ctx.formatBest0to100()}   •   PEAK ${ctx.formatPeakSpeed()}", ctx.w * 0.50f, ctx.h * 0.76f, u * 1.9f, RL_GREEN, Paint.Align.CENTER, ctx.type.digits, true)
        }
    }

    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.2f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawTriumph660SplitLcdTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f
    val cx = ctx.w * 0.50f
    val cy = ctx.h * (if (ctx.wide) 0.46f else 0.43f)
    val r = min(ctx.w * 0.36f, (ctx.h - railH) * 0.42f)

    // Trident 660 / Tiger Sport 660 yuvarlak kasa + üst yarı LCD + alt yarı küçük renkli TFT penceresi
    drawCircle(Color(0xFF0A0C10), r, Offset(cx, cy))
    drawCircle(Color(0xFF323946), r, Offset(cx, cy), style = Stroke(width = u * 0.8f))

    // Üst yarı negatif LCD takometre yayı + hız
    drawSegmentedArc(cx, cy - r * 0.05f, r * 0.84f, 190f, 160f, 28, ctx.rpmRatio, u * 3.4f, Color.White, Color(0xFF1E242E), ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy - r * 0.06f, u * 13.5f, Color.White, Color(0xFF8C96A5), "monospace")

    // Yatay ayırıcı çizgi & alt yarı küçük renkli TFT kutusu
    drawLine(Color(0xFF424B5C), Offset(cx - r * 0.82f, cy + r * 0.14f), Offset(cx + r * 0.82f, cy + r * 0.14f), u * 0.45f)
    val tftRect = Size(r * 1.18f, r * 0.56f)
    drawPanel(cx - tftRect.width * 0.5f, cy + r * 0.22f, tftRect.width, tftRect.height, Color(0xFF101726), Color(0xFFFF6D00).copy(alpha = 0.6f), u * 1.2f)
    drawGearBadge(ctx.gearText, ctx.isNeutral, cx - r * 0.26f, cy + r * 0.50f, u * 9.5f, Color(0xFFFF6D00), Color.White, ctx.type.digits)
    gText(ctx.subMode.uppercase(), cx + r * 0.20f, cy + r * 0.44f, u * 2.1f, Color(0xFF00E5FF), Paint.Align.CENTER, ctx.type.label, true)
    gText(ctx.formatRange(), cx + r * 0.20f, cy + r * 0.65f, u * 1.85f, Color.White, Paint.Align.CENTER, ctx.type.digits, true)

    drawStatusRail(ctx, RailStyle.LCD)
}

internal fun DrawScope.drawMvAgusta55Tft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val mvRed = Color(0xFFE60012)
    val mvGold = Color(0xFFFFD54F)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.30f, u)
    // İtalya bayrağı şeridi
    val flagW = u * 3.2f
    drawRect(Color(0xFF009246), Offset(ctx.w * 0.055f, ctx.h * 0.055f), Size(flagW, u * 0.8f))
    drawRect(Color.White, Offset(ctx.w * 0.055f + flagW, ctx.h * 0.055f), Size(flagW, u * 0.8f))
    drawRect(Color(0xFFCE2B37), Offset(ctx.w * 0.055f + flagW * 2f, ctx.h * 0.055f), Size(flagW, u * 0.8f))
    drawBrandMark(ctx.spec, p, ctx.w * 0.14f, ctx.h * 0.080f, u * 3.8f, u, Color.White)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, ctx.h * 0.080f, u * 2.0f, Color.White, Paint.Align.RIGHT, ctx.type.digits, true)

    val cx = ctx.w * (if (ctx.wide) 0.46f else 0.50f)
    val cy = ctx.h * (if (ctx.wide) 0.50f else 0.42f)
    val r = min(ctx.w * (if (ctx.wide) 0.27f else 0.36f), (ctx.h - railH) * 0.40f)
    drawSegmentedArc(cx, cy, r, 138f, 264f, 52, ctx.rpmRatio, u * 4.0f, mvRed, p.track, ctx.redlineRatio, mvGold, ctx.peakRatio, Color.White, 1.0f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 2.5f, u * 16.5f, Color.White, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.58f, u * 11.5f, mvRed, Color.White, ctx.type.digits)

    if (ctx.wide) {
        gText("MVICS  •  TC ${if (t.tractionControlInterventionActive) "ACT" else "ON"}", ctx.w * 0.77f, ctx.h * 0.38f, u * 1.9f, mvGold, Paint.Align.LEFT, ctx.type.label, true)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.77f, ctx.h * 0.47f, u * 2.6f, Color.White, Paint.Align.LEFT, ctx.type.digits, true)
        gText("RANGE ${ctx.formatRange()}", ctx.w * 0.77f, ctx.h * 0.56f, u * 1.9f, p.muted, Paint.Align.LEFT, ctx.type.digits, true)
    }

    drawSubModeChip(ctx, ctx.w * 0.055f, ctx.h - railH - u * 3.2f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawApriliaTuaregRallyTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val aprRed = Color(0xFFFF1E1E)
    val cyan = Color(0xFF00E5FF)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.6f, ctx.w * 0.28f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, ctx.h * 0.07f, u * 4.0f, u, aprRed)
    gText("TUAREG 660  //  APRC RALLY", ctx.w * 0.14f, ctx.h * 0.078f, u * 1.7f, cyan, Paint.Align.LEFT, ctx.type.label, true)

    drawLinearBar(ctx.w * 0.06f, ctx.h * 0.14f, ctx.w * 0.88f, u * 3.8f, ctx.rpmRatio, cyan, p.track, ctx.redlineRatio, aprRed, u * 0.6f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.40f else 0.50f), ctx.h * 0.44f, u * 19.0f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.74f else 0.50f), ctx.h * (if (ctx.wide) 0.44f else 0.62f), u * 15.0f, aprRed, p.fg, ctx.type.digits)
    gText("ATCX  •  ABS OFF-RD  •  TRIP ${ctx.formatTrip()}  •  ${ctx.formatRange()}", ctx.w * 0.50f, ctx.h * 0.75f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}
