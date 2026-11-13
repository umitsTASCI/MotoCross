package com.oemcluster.pro.renderer

import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 1:1 Japon OEM Gösterge Ailesi (Yamaha, Honda, Kawasaki, Suzuki):
 * - Yamaha: MT-09 Gen4 (4 Tema dahil Piano Müzik/Harmonik Görselleştirici),
 *   Ténéré 700 Raid Dikey Roadbook, Tracer 9 GT Çift Ekran, R7/MT-07 LCD.
 * - Honda: 2024 5" Circle / Bar / Simple, Africa Twin 1100 Dual TFT + Alt LCD,
 *   Gold Wing 1800 Çift Kadran + 7" Cockpit.
 * - Kawasaki: Type 1 Yol & Type 2 Pist, 2016 ZX-10R LED Kemer, H2 Şarj/Boost,
 *   Z900 C-Ring LCD.
 * - Suzuki: Hayabusa Gen3 SDMS-alpha Center TFT, GSX-8S/GX/1000GT Modern TFT,
 *   SV650 / DR-Z LCD.
 */

// =========================================================================
// 1. YAMAHA — MT-09 Gen4 (4 Tema + Piano), Ténéré Raid, Tracer 9 GT, R7
// =========================================================================

internal fun DrawScope.drawYamahaMt09Gen4MultiThemeTftReal(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val yamBlue = Color(0xFF00B0FF)
    val theme = ctx.subModeIndex % 4
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.30f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.06f, ctx.h * 0.068f, u * 4.0f, u, yamBlue)
    val themeLabel = when (theme) {
        0 -> "THEME 1 // STREET"
        1 -> "THEME 2 // ANALOG RING"
        2 -> "THEME 3 // MINIMAL"
        else -> "THEME 4 // PIANO RHYTHM"
    }
    gText("${ctx.spec.modelName.uppercase()}  •  $themeLabel", ctx.w * 0.125f, ctx.h * 0.076f, u * 1.6f, yamBlue, Paint.Align.LEFT, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, ctx.h * 0.076f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    when (theme) {
        0 -> {
            // Tema 1: Signature CP3 Kıvrımlı Boomerang Bar + Devir + Vites
            val curve = List(28) { i ->
                val f = i / 27f
                val bx = ctx.w * (0.06f + f * 0.88f)
                val by = if (f < 0.25f) ctx.h * (0.48f - (f / 0.25f) * 0.32f) else ctx.h * 0.16f
                Offset(bx, by)
            }
            drawPolylineBar(curve, 44, ctx.rpmRatio, u * 4.2f, yamBlue, p.track, ctx.redlineRatio, ctx.peakRatio, u)
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.44f else 0.50f), ctx.h * 0.50f, u * 20.0f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.76f else 0.50f), ctx.h * (if (ctx.wide) 0.48f else 0.67f), u * 16.0f, yamBlue, p.fg, ctx.type.digits)
        }
        1 -> {
            // Tema 2: Yuvarlak Analog Halka Görünümü
            val cx = ctx.w * 0.50f
            val cy = ctx.h * 0.49f
            val r = min(ctx.w * 0.33f, (ctx.h - railH) * 0.38f)
            drawSegmentedArc(cx, cy, r, 138f, 264f, 44, ctx.rpmRatio, u * 3.8f, yamBlue, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
            drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 2.5f, u * 16.5f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.58f, u * 11.5f, yamBlue, p.fg, ctx.type.digits)
        }
        2 -> {
            // Tema 3: Minimal Pure Odak
            drawLinearBar(ctx.w * 0.10f, ctx.h * 0.17f, ctx.w * 0.80f, u * 2.6f, ctx.rpmRatio, Color.White, p.track, ctx.redlineRatio, RL_RED, u * 0.5f)
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.46f, u * 24.0f, Color.White, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.67f, u * 14.0f, yamBlue, Color.White, ctx.type.digits)
        }
        else -> {
            // Tema 4: 2024+ MT-09 İkonik "PIANO / RHYTHM" Harmonik Bar Görselleştirici
            val barCount = 26
            val startX = ctx.w * 0.08f
            val totalW = ctx.w * 0.84f
            val keyW = (totalW / barCount) * 0.72f
            val baseLine = ctx.h * 0.40f
            val phase = (ctx.mappedRpm / 140.0 + ctx.speedValue * 0.12)
            for (i in 0 until barCount) {
                val norm = i / (barCount - 1).toFloat()
                val active = ctx.rpmRatio >= norm * 0.96f
                val env = (0.35f + 0.65f * abs(sin(phase + i * 0.45).toFloat())) * (0.25f + 0.75f * ctx.rpmRatio.coerceIn(0f, 1f))
                val keyH = (ctx.h * 0.24f * env).coerceAtLeast(u * 1.4f)
                val col = when {
                    !active -> p.track.copy(alpha = 0.45f)
                    norm >= ctx.redlineRatio -> Color(0xFFFF1744)
                    norm >= 0.62f -> Color(0xFF00E5FF)
                    else -> Color(0xFF2979FF)
                }
                val bx = startX + i * (totalW / barCount)
                drawRoundRect(col, Offset(bx, baseLine - keyH), Size(keyW, keyH), CornerRadius(u * 0.4f, u * 0.4f))
            }
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.40f else 0.50f), ctx.h * 0.62f, u * 18.0f, Color.White, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.72f else 0.50f), ctx.h * (if (ctx.wide) 0.62f else 0.78f), u * 13.0f, Color(0xFF00E5FF), Color.White, ctx.type.digits)
        }
    }

    if (theme != 3) {
        gText("YRC ${ctx.subMode.uppercase()}  •  RANGE ${ctx.formatRange()}  •  ${ctx.formatBattery()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.75f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    }
    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawYamahaTenereRaidTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val yamBlue = Color(0xFF00B0FF)
    val amber = Color(0xFFFFAB00)
    val raid = ctx.subMode.contains("Raid", true) || ctx.subMode.contains("Rally", true)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, ctx.h * 0.065f, u * 4.0f, u, yamBlue)
    gText(if (raid) "TÉNÉRÉ // RAID ROADBOOK" else "TÉNÉRÉ // EXPLORER TFT", ctx.w * 0.135f, ctx.h * 0.075f, u * 1.7f, if (raid) amber else yamBlue, Paint.Align.LEFT, ctx.type.label, true)
    drawCompassStrip(ctx, ctx.h * 0.105f, u * 3.8f)

    // Sol veya Üst Dikey Roadbook T1/T2 kutuları + Hız
    drawLinearBar(ctx.w * 0.06f, ctx.h * 0.18f, ctx.w * 0.88f, u * 3.4f, ctx.rpmRatio, yamBlue, p.track, ctx.redlineRatio, RL_RED, u * 0.6f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.34f else 0.50f), ctx.h * 0.45f, u * 18.5f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.58f else 0.50f), ctx.h * (if (ctx.wide) 0.45f else 0.62f), u * 14.0f, yamBlue, p.fg, ctx.type.digits)

    val rbX = if (ctx.wide) ctx.w * 0.68f else ctx.w * 0.08f
    val rbY = if (ctx.wide) ctx.h * 0.27f else ctx.h * 0.69f
    val rbW = if (ctx.wide) ctx.w * 0.26f else ctx.w * 0.84f
    val rbH = if (ctx.wide) ctx.h * 0.46f else ctx.h * 0.14f
    drawPanel(rbX, rbY, rbW, rbH, lerp(p.bg, Color.Black, 0.32f), amber.copy(alpha = 0.55f), u * 1.4f)
    gText("TRIP 1   ${ctx.formatTrip()}", rbX + u * 2.0f, rbY + u * 4.5f, u * 2.2f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("TRIP 2   ${ctx.formatTripB()}", rbX + u * 2.0f, rbY + u * 9.0f, u * 2.0f, amber, Paint.Align.LEFT, ctx.type.digits, true)
    if (ctx.wide) {
        gText("RANGE    ${ctx.formatRange()}", rbX + u * 2.0f, rbY + u * 14.0f, u * 1.9f, yamBlue, Paint.Align.LEFT, ctx.type.label, true)
        gText("ABS OFF  •  ${t.gpsSatelliteCount} SAT", rbX + u * 2.0f, rbY + u * 18.5f, u * 1.65f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    }

    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawYamahaTracerTwinTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val blue = Color(0xFF00B0FF)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    // İkiz 3.5" TFT paneller arası ince fiziksel çerçeve ayrımı
    val gap = u * 1.6f
    val top = ctx.h * 0.07f
    val hBox = ctx.h - railH - top - u * 2.5f
    if (ctx.wide) {
        val halfW = (ctx.w - u * 8.0f - gap) * 0.5f
        val leftX = u * 4.0f
        val rightX = leftX + halfW + gap
        drawPanel(leftX, top, halfW, hBox, lerp(p.bg, Color.Black, 0.26f), blue.copy(alpha = 0.45f), u * 1.6f)
        drawPanel(rightX, top, halfW, hBox, lerp(p.bg, Color.Black, 0.26f), p.fg.copy(alpha = 0.20f), u * 1.6f)

        drawLinearBar(leftX + u * 2.2f, top + u * 3.0f, halfW - u * 4.4f, u * 3.2f, ctx.rpmRatio, blue, p.track, ctx.redlineRatio, RL_RED, u * 0.6f)
        drawSpeed(ctx.speedValue, ctx.speedUnit, leftX + halfW * 0.38f, top + hBox * 0.56f, u * 16.5f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, leftX + halfW * 0.78f, top + hBox * 0.56f, u * 13.5f, blue, p.fg, ctx.type.digits)

        drawBrandMark(ctx.spec, p, rightX + halfW - u * 5.0f, top + u * 4.5f, u * 3.8f, u, blue)
        gText("TOURING TELEMETRY", rightX + u * 2.4f, top + u * 5.2f, u * 1.6f, blue, Paint.Align.LEFT, ctx.type.label, true)
        gText("TRIP A    ${ctx.formatTrip()}", rightX + u * 2.4f, top + u * 12.0f, u * 2.3f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("AVG FUEL  ${ctx.formatAvgFuel()}", rightX + u * 2.4f, top + u * 17.5f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
        gText("RANGE     ${ctx.formatRange()}", rightX + u * 2.4f, top + u * 23.0f, u * 2.3f, RL_GREEN, Paint.Align.LEFT, ctx.type.digits, true)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}  •  ${ctx.formatBattery()}", rightX + u * 2.4f, top + hBox - u * 3.0f, u * 1.85f, p.muted, Paint.Align.LEFT, ctx.type.digits, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.42f, u * 19.5f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.60f, u * 15.0f, blue, p.fg, ctx.type.digits)
        gText("RANGE ${ctx.formatRange()}  •  TRIP ${ctx.formatTrip()}", ctx.w * 0.50f, ctx.h * 0.75f, u * 1.95f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    }
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawYamahaR7NegativeLcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val rCyan = Color(0xFF00E5FF)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val left = ctx.w * 0.055f
    val top = ctx.h * 0.075f
    val pw = ctx.w * 0.89f
    val ph = ctx.h - railH - top - u * 2.5f
    drawPanel(left, top, pw, ph, Color(0xFF080D18), rCyan.copy(alpha = 0.45f), u * 1.6f)
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, top + u * 2.4f, ctx.w * 0.24f, u)

    val barPts = List(34) { i ->
        val f = i / 33f
        val x = left + pw * (0.06f + f * 0.88f)
        val y = if (f < 0.25f) top + ph * (0.42f - (f / 0.25f) * 0.24f) else top + ph * 0.18f
        Offset(x, y)
    }
    drawPolylineBar(barPts, 34, ctx.rpmRatio, u * 3.8f, Color(0xFFEAF6FF), Color(0xFF192234), ctx.redlineRatio, ctx.peakRatio, u)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.44f else 0.50f), top + ph * 0.58f, u * 18.5f, Color(0xFFEAF6FF), Color(0xFF7B8CA8), "monospace")
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.78f else 0.50f), top + ph * (if (ctx.wide) 0.56f else 0.76f), u * 15.0f, rCyan, Color.White, "monospace")
    gText("QS READY  •  ${ctx.formatInstantFuel()}  •  ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", left + u * 2.5f, top + ph - u * 2.4f, u * 1.6f, rCyan, Paint.Align.LEFT, "monospace", true)
    drawStatusRail(ctx, RailStyle.LCD)
}

// =========================================================================
// 2. HONDA — 2024 5" Circle / Bar / Simple, Africa Twin, Gold Wing
// =========================================================================

internal fun DrawScope.drawHonda2024CircleBarSimpleTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val hrcRed = Color(0xFFE60012)
    val hrcBlue = Color(0xFF0066CC)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val modeIdx = when {
        ctx.subMode.contains("Bar", true) -> 1
        ctx.subMode.contains("Simple", true) -> 2
        else -> ctx.subModeIndex % 3
    }

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.28f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, ctx.h * 0.068f, u * 4.2f, u, hrcRed)
    gText(ctx.spec.modelName.uppercase(), ctx.w * 0.135f, ctx.h * 0.075f, u * 1.65f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, ctx.h * 0.075f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    when (modeIdx) {
        0 -> {
            // CIRCLE görünümü: Honda temiz dairesel halka + vites
            val cx = ctx.w * (if (ctx.wide) 0.36f else 0.50f)
            val cy = ctx.h * (if (ctx.wide) 0.50f else 0.42f)
            val r = min(ctx.w * (if (ctx.wide) 0.25f else 0.36f), (ctx.h - railH) * 0.39f)
            drawSegmentedArc(cx, cy, r, 138f, 264f, 44, ctx.rpmRatio, u * 3.8f, hrcBlue, p.track, ctx.redlineRatio, hrcRed, ctx.peakRatio, Color.White, 1.0f)
            drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 2.5f, u * 16.0f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.58f, u * 11.0f, hrcRed, p.fg, ctx.type.digits)
            if (ctx.wide) {
                val tx = ctx.w * 0.68f
                drawPanel(tx, ctx.h * 0.22f, ctx.w * 0.26f, ctx.h * 0.56f, lerp(p.bg, Color.Black, 0.24f), hrcBlue.copy(alpha = 0.45f), u * 1.6f)
                gText("ROADSYNC  //  CIRCLE", tx + u * 2.0f, ctx.h * 0.29f, u * 1.45f, hrcRed, Paint.Align.LEFT, ctx.type.label, true)
                gText("P   ${if (t.rpmRateOfChangePerSec > 800) "HIGH" else "STD"}", tx + u * 2.0f, ctx.h * 0.38f, u * 2.0f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
                gText("T   ${if (t.tractionControlInterventionActive) "ACT" else "ON"}", tx + u * 2.0f, ctx.h * 0.46f, u * 2.0f, RL_AMBER, Paint.Align.LEFT, ctx.type.label, true)
                gText("RNG ${ctx.formatRange()}", tx + u * 2.0f, ctx.h * 0.57f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
                gText(ctx.formatInstantFuel(), tx + u * 2.0f, ctx.h * 0.66f, u * 1.8f, p.muted, Paint.Align.LEFT, ctx.type.digits, true)
            }
        }
        1 -> {
            // BAR görünümü: Honda üst şerit bar
            drawLinearBar(ctx.w * 0.06f, ctx.h * 0.16f, ctx.w * 0.88f, u * 4.5f, ctx.rpmRatio, hrcBlue, p.track, ctx.redlineRatio, hrcRed, u * 0.8f)
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.40f else 0.50f), ctx.h * 0.49f, u * 20.0f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.74f else 0.50f), ctx.h * (if (ctx.wide) 0.49f else 0.66f), u * 16.0f, hrcRed, p.fg, ctx.type.digits)
            gText("BAR VIEW  •  RANGE ${ctx.formatRange()}  •  ${ctx.formatBattery()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.85f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        }
        else -> {
            // SIMPLE görünümü: Minimal temiz Honda hız/vites
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.45f, u * 23.0f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.66f, u * 13.5f, hrcRed, p.fg, ctx.type.digits)
            gText("SIMPLE  •  RANGE ${ctx.formatRange()}", ctx.w * 0.50f, ctx.h * 0.79f, u * 1.85f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        }
    }

    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawHondaAfricaTwinDualTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val hrcRed = Color(0xFFE60012)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    // 1) ÜST 6.5" Dokunmatik TFT ekran
    val topH = (ctx.h - railH) * 0.60f
    drawPanel(ctx.w * 0.05f, ctx.h * 0.05f, ctx.w * 0.90f, topH, lerp(p.bg, Color.Black, 0.25f), hrcRed.copy(alpha = 0.55f), u * 1.6f)
    drawBrandMark(ctx.spec, p, ctx.w * 0.10f, ctx.h * 0.105f, u * 3.8f, u, hrcRed)
    gText("CRF1100L // ${ctx.subMode.uppercase()}", ctx.w * 0.16f, ctx.h * 0.112f, u * 1.65f, hrcRed, Paint.Align.LEFT, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.91f, ctx.h * 0.112f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
    drawCompassStrip(ctx, ctx.h * 0.145f, u * 3.8f)
    drawLinearBar(ctx.w * 0.085f, ctx.h * 0.24f, ctx.w * 0.83f, u * 3.6f, ctx.rpmRatio, Color(0xFF00B0FF), p.track, ctx.redlineRatio, hrcRed, u * 0.6f)
    gText("RANGE ${ctx.formatRange()}   •   TRIP A ${ctx.formatTrip()}", ctx.w * 0.085f, ctx.h * 0.42f, u * 2.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("HSTC ${if (t.tractionControlInterventionActive) "ACT" else "3"}   •   WHEELIE ${t.wheelieAngleDeg.roundToInt()}°   •   ${t.gpsSatelliteCount} SAT", ctx.w * 0.085f, ctx.h * 0.52f, u * 1.75f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

    // 2) ALT Bağımsız Negatif LCD Şerit (Africa Twin ikonik hız/vites alt LCD kutusu)
    val lcdTop = ctx.h * 0.675f
    val lcdH = max(u * 10.0f, ctx.h - railH - lcdTop - u * 1.8f)
    drawPanel(ctx.w * 0.14f, lcdTop, ctx.w * 0.72f, lcdH, Color(0xFF070A0F), Color(0xFF2E3848), u * 1.2f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.40f, lcdTop + lcdH * 0.66f, min(u * 11.5f, lcdH * 0.55f), Color.White, Color(0xFF8C96A8), "monospace")
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.73f, lcdTop + lcdH * 0.50f, min(u * 9.5f, lcdH * 0.68f), hrcRed, Color.White, "monospace")
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawHondaGoldWingCockpit(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val hrcRed = Color(0xFFE60012)
    val gold = Color(0xFFFFCA28)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    if (ctx.wide) {
        val midX = ctx.w * 0.50f
        val midW = ctx.w * 0.34f
        val top = ctx.h * 0.14f
        val midH = ctx.h - railH - top - u * 3.0f
        // Merkez 7" Infotainment ekran
        drawPanel(midX - midW * 0.5f, top, midW, midH, Color(0xFF0B0E16), gold.copy(alpha = 0.55f), u * 1.6f)
        drawBrandMark(ctx.spec, p, midX, top + u * 4.5f, u * 4.0f, u, hrcRed)
        drawCompassStrip(ctx, top + u * 8.0f, u * 3.6f)
        drawGearBadge(ctx.gearText, ctx.isNeutral, midX, top + midH * 0.52f, u * 13.0f, gold, Color.White, ctx.type.digits)
        gText("DCT  •  RANGE ${ctx.formatRange()}", midX, top + midH * 0.78f, u * 1.75f, Color.White, Paint.Align.CENTER, ctx.type.label, true)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}  •  ${ctx.formatBattery()}", midX, top + midH * 0.90f, u * 1.65f, p.muted, Paint.Align.CENTER, ctx.type.digits, true)

        // Sol analog hız & sağ analog devir kadranları
        val dialR = min(ctx.w * 0.145f, midH * 0.44f)
        val spdCy = top + midH * 0.50f
        val leftCx = ctx.w * 0.17f
        val rightCx = ctx.w * 0.83f
        val spdRatio = (ctx.speedValue / max(100, ctx.speedScaleMax).toFloat()).coerceIn(0f, 1f)
        drawDialFace(leftCx, spdCy, dialR, p, chrome = true, cream = false, u = u)
        drawTickRing(leftCx, spdCy, dialR - u * 1.4f, 135f, 270f, 7, 3, p.fg, 1.0f, u, labels = true, labelFamily = ctx.type.digits, labelSize = u * 2.0f) {
            (it * (ctx.speedScaleMax / 6)).toString()
        }
        drawNeedle(leftCx, spdCy, 135f + 270f * spdRatio, dialR * 0.84f, u * 1.05f, hrcRed)
        gText("${ctx.speedValue}", leftCx, spdCy + u * 5.0f, u * 4.5f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)

        drawDialFace(rightCx, spdCy, dialR, p, chrome = true, cream = false, u = u)
        drawTickRing(rightCx, spdCy, dialR - u * 1.4f, 135f, 270f, 8, 2, p.fg, ctx.redlineRatio, u, labels = true, labelFamily = ctx.type.digits, labelSize = u * 2.0f) {
            it.toString()
        }
        drawNeedle(rightCx, spdCy, 135f + 270f * ctx.rpmRatio.coerceIn(0f, 1f), dialR * 0.84f, u * 1.05f, hrcRed)
        gText("x1000 RPM", rightCx, spdCy + u * 5.0f, u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.40f, u * 20.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.59f, u * 15.0f, gold, p.fg, ctx.type.digits)
        gText("GOLD WING DCT  •  RANGE ${ctx.formatRange()}", ctx.w * 0.50f, ctx.h * 0.74f, u * 1.85f, gold, Paint.Align.CENTER, ctx.type.label, true)
    }
    drawStatusRail(ctx, RailStyle.CLASSIC)
}

// =========================================================================
// 3. KAWASAKI — Type 1/2 TFT, LED Arch, H2 Boost, Z900 C-Ring
// =========================================================================

internal fun DrawScope.drawKawasakiStreetType1TftReal(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val krtGreen = Color(0xFF64DD17)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.28f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.06f, ctx.h * 0.068f, u * 4.0f, u, krtGreen)
    gText("RIDEOLOGY // ${ctx.subMode.uppercase()}", ctx.w * 0.13f, ctx.h * 0.075f, u * 1.65f, krtGreen, Paint.Align.LEFT, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, ctx.h * 0.075f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    // Type 1: Düzgün kavisli KRT yeşil-beyaz bar
    val pts = List(32) { i ->
        val f = i / 31f
        val x = ctx.w * (0.06f + f * 0.88f)
        val y = ctx.h * (0.30f - sin(f * PI * 0.5).toFloat() * 0.14f)
        Offset(x, y)
    }
    drawPolylineBar(pts, 46, ctx.rpmRatio, u * 4.2f, krtGreen, p.track, ctx.redlineRatio, ctx.peakRatio, u)

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), ctx.h * 0.50f, u * 19.5f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.75f else 0.50f), ctx.h * (if (ctx.wide) 0.48f else 0.67f), u * 16.0f, krtGreen, p.fg, ctx.type.digits)
    gText("KTRC ${if (t.tractionControlInterventionActive) "ACT" else "ON"}  •  LEAN ${abs(t.leanAngleDeg).roundToInt()}°  •  RANGE ${ctx.formatRange()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawKawasakiZx10rLedArchLcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val krtGreen = Color(0xFF64DD17)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    // 2011–2020 ZX-10R ikonik üst kavisli LED kemer
    val segs = 28
    val archCx = ctx.w * 0.50f
    val archCy = ctx.h * 0.72f
    val archR = min(ctx.w * 0.44f, ctx.h * 0.58f)
    for (i in 0 until segs) {
        val f = (i + 1f) / segs
        val ang = Math.toRadians((228.0 + f * 84.0))
        val bx = archCx + cos(ang).toFloat() * archR
        val by = archCy + sin(ang).toFloat() * archR
        val on = ctx.rpmRatio >= f
        val col = when {
            !on -> Color(0xFF1E242C)
            f >= ctx.redlineRatio -> Color(0xFFFF1744)
            f >= 0.70f -> Color(0xFFFFAB00)
            else -> krtGreen
        }
        drawCircle(col, u * 1.45f, Offset(bx, by))
    }

    val boxW = ctx.w * 0.78f
    val boxH = (ctx.h - railH) * 0.46f
    val boxTop = ctx.h * 0.34f
    drawPanel(ctx.w * 0.11f, boxTop, boxW, boxH, Color(0xFF0C1216), krtGreen.copy(alpha = 0.45f), u * 1.6f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.38f, boxTop + boxH * 0.56f, u * 16.0f, Color.White, Color(0xFF8C96A5), "monospace")
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.72f, boxTop + boxH * 0.50f, u * 14.0f, krtGreen, Color.White, "monospace")
    gText("S-KTRC  •  KEBC  •  0-100 ${ctx.formatBest0to100()}", ctx.w * 0.50f, boxTop + boxH - u * 2.2f, u * 1.7f, krtGreen, Paint.Align.CENTER, "monospace", true)
    drawStatusRail(ctx, RailStyle.LCD)
}

internal fun DrawScope.drawKawasakiH2ChronographBoostReal(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val h2Green = Color(0xFF00E676)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val dialCx = ctx.w * (if (ctx.wide) 0.34f else 0.50f)
    val dialCy = ctx.h * (if (ctx.wide) 0.48f else 0.38f)
    val dialR = min(ctx.w * (if (ctx.wide) 0.25f else 0.35f), (ctx.h - railH) * 0.39f)
    drawCircle(Color(0xFF0A0C0F), dialR * 1.08f, Offset(dialCx, dialCy))
    drawCircle(Color(0xFF707888), dialR * 1.08f, Offset(dialCx, dialCy), style = Stroke(width = u * 0.9f))
    drawDialFace(dialCx, dialCy, dialR * 0.94f, p, chrome = true, cream = false, u = u)
    drawTickRing(dialCx, dialCy, dialR * 0.94f - u * 1.4f, 135f, 270f, 15, 2, p.fg, ctx.redlineRatio, u, labels = true, labelFamily = ctx.type.digits, labelSize = u * 2.1f) {
        it.toString()
    }
    drawNeedle(dialCx, dialCy, 135f + 270f * ctx.rpmRatio.coerceIn(0f, 1f), dialR * 0.82f, u * 1.15f, h2Green)
    drawBrandMark(ctx.spec, p, dialCx, dialCy - dialR * 0.42f, u * 3.8f, u, h2Green)
    gText("SUPERCHARGED", dialCx, dialCy + dialR * 0.48f, u * 1.45f, h2Green, Paint.Align.CENTER, ctx.type.label, true)

    // Sağ veya Alt H2 TFT: Hız, Vites ve Fiziksel Güç Yükünden Türetilmiş "BOOST INDEX" barı
    val panelX = if (ctx.wide) ctx.w * 0.63f else ctx.w * 0.08f
    val panelY = if (ctx.wide) ctx.h * 0.16f else ctx.h * 0.62f
    val panelW = if (ctx.wide) ctx.w * 0.32f else ctx.w * 0.84f
    val panelH = if (ctx.wide) ctx.h * 0.66f else ctx.h * 0.22f
    drawPanel(panelX, panelY, panelW, panelH, Color(0xFF0B0E14), h2Green.copy(alpha = 0.5f), u * 1.6f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, panelX + panelW * 0.38f, panelY + panelH * 0.44f, u * 13.5f, Color.White, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, panelX + panelW * 0.80f, panelY + panelH * 0.38f, u * 11.5f, h2Green, Color.White, ctx.type.digits)

    val boostRatio = ((ctx.rpmRatio * 0.72f + (t.wheelieAngleDeg / 35.0).toFloat() * 0.28f)).coerceIn(0f, 1f)
    gText("SUPERCHARGER LOAD", panelX + u * 2.0f, panelY + panelH * 0.72f, u * 1.45f, h2Green, Paint.Align.LEFT, ctx.type.label, true)
    drawLinearBar(panelX + u * 2.0f, panelY + panelH * 0.77f, panelW - u * 4.0f, u * 2.6f, boostRatio, h2Green, p.track, 0.85f, RL_RED, u * 0.6f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawKawasakiZ900CRingLcdReal(ctx: GaugeContext) {
    val p = ctx.palette
    val u = ctx.u
    val zGreen = Color(0xFF64DD17)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f
    val cx = ctx.w * 0.50f
    val cy = ctx.h * (if (ctx.wide) 0.48f else 0.42f)
    val r = min(ctx.w * 0.36f, (ctx.h - railH) * 0.40f)

    // Z900 ikonik C-Ring parçalı LCD halkası
    drawSegmentedArc(cx, cy, r, 150f, 240f, 36, ctx.rpmRatio, u * 4.4f, zGreen, Color(0xFF1D2428), ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 1.5f, u * 17.5f, Color.White, Color(0xFF90A4AE), "monospace")
    drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.58f, u * 12.5f, zGreen, Color.White, "monospace")
    gText("RANGE ${ctx.formatRange()}  •  ${ctx.formatBattery()}", cx, ctx.h - railH - u * 3.5f, u * 1.8f, p.muted, Paint.Align.CENTER, "monospace", true)
    drawStatusRail(ctx, RailStyle.LCD)
}

// =========================================================================
// 4. SUZUKI — Hayabusa Gen3 SDMS-α, GSX-8S/GX TFT, SV650/DR-Z LCD
// =========================================================================

internal fun DrawScope.drawSuzukiHayabusaGen3Real(ctx: GaugeContext) {
    // AnalogClusterRenderers içindeki 5-Kadran Hayabusa düzenini baz alıp
    // Gen 3 merkez SDMS-alpha Aktif Veri TFT bilgisini gerçek telemetriyle güçlendiriyoruz:
    drawHayabusaFiveDial(ctx)
    val t = ctx.telemetry
    val u = ctx.u
    val cx = ctx.w * 0.50f
    val cy = if (ctx.wide) ctx.h * 0.54f else ctx.h * 0.49f
    gText("SDMS-α  •  LEAN ${abs(t.leanAngleDeg).roundToInt()}°", cx, cy + u * 10.5f, u * 1.55f, ctx.palette.accent, Paint.Align.CENTER, ctx.type.label, true)
}

internal fun DrawScope.drawSuzukiGsx8sGtTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val suzBlue = Color(0xFF0091EA)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.28f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.06f, ctx.h * 0.068f, u * 4.0f, u, suzBlue)
    gText("SDMS ${ctx.subMode.uppercase()}  •  QS", ctx.w * 0.13f, ctx.h * 0.075f, u * 1.65f, suzBlue, Paint.Align.LEFT, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, ctx.h * 0.075f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    drawLinearBar(ctx.w * 0.06f, ctx.h * 0.155f, ctx.w * 0.88f, u * 4.0f, ctx.rpmRatio, suzBlue, p.track, ctx.redlineRatio, RL_RED, u * 0.7f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), ctx.h * 0.49f, u * 19.5f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.74f else 0.50f), ctx.h * (if (ctx.wide) 0.49f else 0.67f), u * 15.5f, suzBlue, p.fg, ctx.type.digits)
    gText("STCS ${if (t.tractionControlInterventionActive) "ACT" else "ON"}  •  RANGE ${ctx.formatRange()}  •  ${ctx.formatAvgFuel()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.8f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawSuzukiSv650NegativeLcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val left = ctx.w * 0.07f
    val top = ctx.h * 0.09f
    val pw = ctx.w * 0.86f
    val ph = ctx.h - railH - top - u * 3.0f
    drawPanel(left, top, pw, ph, p.bg, p.fg.copy(alpha = 0.35f), u * 1.6f)
    drawLinearBar(left + u * 2.5f, top + u * 3.5f, pw - u * 5.0f, u * 3.6f, ctx.rpmRatio, p.fg, p.track, ctx.redlineRatio, RL_RED, u * 0.5f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), top + ph * 0.56f, u * 18.0f, p.fg, p.muted, "monospace")
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.74f else 0.50f), top + ph * (if (ctx.wide) 0.56f else 0.76f), u * 14.0f, p.accent, p.fg, "monospace")
    gText("ODO ${ctx.formatOdometer()}  •  ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", left + u * 2.5f, top + ph - u * 2.4f, u * 1.6f, p.muted, Paint.Align.LEFT, "monospace", true)
    drawStatusRail(ctx, RailStyle.LCD)
}
