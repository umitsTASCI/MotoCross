package com.oemcluster.pro.renderer

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/* =====================================================================================
 *  6) CENTRAL ANALOG HYBRID — R6, CBR600RR PC40, S1000RR K46, ZX-6R, GSX-R K5, RSV4
 * ===================================================================================== */
internal fun DrawScope.drawCentralAnalogHybrid(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val variant = skinVariant(ctx.spec.skinId, 3)
    val railH = ctx.h * (if (ctx.spec.displayCategory == com.oemcluster.pro.catalog.GaugeDisplayCategory.RETRO_CLASSIC) 0.088f else 0.082f)
    val stepK = if (ctx.maxRpm <= 11000.0) 1 else 2
    val rpmMajor = max(4, (ctx.maxRpm / (stepK * 1000.0)).roundToInt())
    val rpmFormatter: (Int) -> String = { (it * stepK).toString() }
    val speedMajor = max(4, (ctx.speedScaleMax / 40.0).roundToInt())
    val speedFormatter: (Int) -> String = { (it * 40).toString() }
    val spdRatio = (ctx.speedValue.toFloat() / ctx.speedScaleMax).coerceIn(0f, 1f)

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.2f, ctx.w * 0.26f, ctx.u)

    if (ctx.wide) {
        when (variant) {
            0 -> {
                val cx = ctx.w * 0.375f
                val cy = ctx.h * 0.49f
                val r = min(ctx.h * 0.36f, ctx.w * 0.285f)
                drawSpeedHubLcd(ctx, ctx.w * 0.73f, ctx.h * 0.16f, ctx.w * 0.24f, ctx.h * 0.64f, railH)
                drawNeedleDial(cx, cy, r, 140f, 260f, ctx.rpmRatio, p, ctx.u, p.accent, ctx.redlineRatio, rpmMajor, if (stepK == 1) 2 else 4, true, labelFormatter = rpmFormatter)
                gText("x1000 r/min", cx, cy + r * 0.52f, ctx.u * 1.6f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.08f)
                gText("RPM", cx, cy + r * 0.66f, ctx.u * 1.35f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)
                drawBrandMark(ctx.spec, p, cx, cy - r * 0.58f, ctx.u * 4.2f, ctx.u, p.accent.copy(alpha = 0.55f))
            }
            1 -> {
                val leftCx = ctx.w * 0.27f
                val rightCx = ctx.w * 0.73f
                val cy = ctx.h * 0.48f
                val r = min(ctx.h * 0.31f, ctx.w * 0.21f)
                drawNeedleDial(leftCx, cy, r, 140f, 260f, ctx.rpmRatio, p, ctx.u, p.accent, ctx.redlineRatio, rpmMajor, if (stepK == 1) 2 else 4, true, labelFormatter = rpmFormatter)
                gText("x1000 r/min", leftCx, cy + r * 0.55f, ctx.u * 1.35f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
                drawNeedleDial(rightCx, cy, r, 140f, 260f, spdRatio, p, ctx.u, p.fg, 0.82f, speedMajor, 2, true, labelFormatter = speedFormatter)
                gText(ctx.speedUnit.uppercase(), rightCx, cy + r * 0.55f, ctx.u * 1.35f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)
                drawCenterGearStrip(ctx, ctx.w * 0.50f, ctx.h * 0.80f, ctx.w * 0.26f, railH)
                drawBrandMark(ctx.spec, p, ctx.w * 0.50f, ctx.h * 0.11f, ctx.u * 4.2f, ctx.u, p.accent.copy(alpha = 0.55f))
            }
            else -> {
                val cx = ctx.w * 0.63f
                val cy = ctx.h * 0.49f
                val r = min(ctx.h * 0.35f, ctx.w * 0.275f)
                drawNeedleDial(cx, cy, r, 140f, 260f, ctx.rpmRatio, p, ctx.u, p.accent, ctx.redlineRatio, rpmMajor, if (stepK == 1) 2 else 4, true, labelFormatter = rpmFormatter)
                gText("x1000 r/min", cx, cy + r * 0.52f, ctx.u * 1.55f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
                drawSpeedHubLcd(ctx, ctx.w * 0.03f, ctx.h * 0.16f, ctx.w * 0.24f, ctx.h * 0.56f, railH)
                drawBrandMark(ctx.spec, p, cx, cy - r * 0.58f, ctx.u * 4.2f, ctx.u, p.accent.copy(alpha = 0.55f))
            }
        }
        if (variant != 1) {
            // Dikey kadranın sol alt boşluğuna taşınır; ibre/sahne çakışması olmaz.
            drawTemperatureAndFuel(ctx, ctx.w * 0.03f, ctx.h * 0.76f, ctx.w * 0.18f, compact = true)
        }
    } else {
        val cx = ctx.w * 0.50f
        val cy = ctx.h * 0.34f
        val r = min(ctx.w * 0.40f, ctx.h * 0.23f)
        drawNeedleDial(cx, cy, r, 140f, 260f, ctx.rpmRatio, p, ctx.u, p.accent, ctx.redlineRatio, rpmMajor, if (stepK == 1) 2 else 4, true, labelFormatter = rpmFormatter)
        gText("x1000 r/min", cx, cy + r * 0.58f, ctx.u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawSpeedHubLcd(ctx, ctx.w * 0.08f, ctx.h * 0.53f, ctx.w * 0.84f, ctx.h * 0.24f, railH)
        drawTemperatureAndFuel(ctx, ctx.w * 0.10f, ctx.h * 0.80f, ctx.w * 0.38f, compact = true)
        drawSubModeChip(ctx, ctx.w * 0.90f, ctx.h * 0.825f, alignRight = true, small = true)
    }
    drawStatusRail(ctx, if (ctx.spec.displayCategory == com.oemcluster.pro.catalog.GaugeDisplayCategory.RETRO_CLASSIC) RailStyle.CLASSIC else RailStyle.LCD)
}

private fun DrawScope.drawSpeedHubLcd(
    ctx: GaugeContext,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    railH: Float
) {
    val p = ctx.palette
    val t = ctx.telemetry
    drawPanel(left, top, width, height, p, accentEdge = true)
    // LCD iç negatif çerçeve
    drawRoundRect(
        p.bg.copy(alpha = 0.65f),
        Offset(left + ctx.u * 1.2f, top + ctx.u * 1.2f),
        Size(width - ctx.u * 2.4f, height - ctx.u * 2.4f),
        androidx.compose.ui.geometry.CornerRadius(ctx.u * 1.2f, ctx.u * 1.2f)
    )
    gText("SPEED", left + width * 0.5f, top + height * 0.10f, ctx.u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, left + width * 0.5f, top + height * 0.36f, min(ctx.u * 11.5f, width * 0.40f), p.fg, p.muted, ctx.type.digits)
    val podSize = min(ctx.u * 7.0f, height * 0.14f)
    drawGearPod(left + width * 0.5f, top + height * 0.54f, podSize, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
    gText("GEAR", left + width * 0.5f, top + height * 0.665f, ctx.u * 1.25f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)

    val statsTop = top + height * 0.74f
    gText("${(t.fuelLevelRatio * 100).roundToInt()}%", left + width * 0.22f, statsTop, ctx.u * 1.55f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    gText("FUEL", left + width * 0.22f, statsTop + ctx.u * 1.75f, ctx.u * 1.05f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    gText("${t.coolantTempCelsius.roundToInt()}°", left + width * 0.50f, statsTop, ctx.u * 1.55f, p.fg, Paint.Align.CENTER, ctx.type.label, true)
    gText("TEMP", left + width * 0.50f, statsTop + ctx.u * 1.75f, ctx.u * 1.05f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    gText("${t.gpsSatelliteCount}", left + width * 0.78f, statsTop, ctx.u * 1.55f, p.accent, Paint.Align.CENTER, ctx.type.label, true)
    gText("SAT", left + width * 0.78f, statsTop + ctx.u * 1.75f, ctx.u * 1.05f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    gText("ODO ${ctx.formatOdometer()}", left + width * 0.5f, top + height * 0.885f, ctx.u * 1.35f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
    gText("TRIP ${ctx.formatTrip()}  •  S ${ctx.formatSession()}", left + width * 0.5f, top + height * 0.945f, ctx.u * 1.15f, p.accent, Paint.Align.CENTER, ctx.type.label, true)
}

private fun DrawScope.drawCenterGearStrip(ctx: GaugeContext, cx: Float, cy: Float, width: Float, railH: Float) {
    val p = ctx.palette
    drawPanel(cx - width * 0.5f, cy - ctx.u * 6.5f, width, ctx.u * 13.0f, p, accentEdge = true)
    drawGearPod(cx - width * 0.30f, cy - ctx.u * 0.6f, ctx.u * 7.2f, p, ctx.telemetry, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
    gText("GEAR", cx - width * 0.30f, cy + ctx.u * 5.0f, ctx.u * 1.15f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    gText("ODO ${ctx.formatOdometer()}", cx - width * 0.04f, cy - ctx.u * 2.2f, ctx.u * 1.65f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("TRIP A ${ctx.formatTrip()}", cx - width * 0.04f, cy + ctx.u * 0.8f, ctx.u * 1.45f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
    gText("SEANS ${ctx.formatSession()}", cx - width * 0.04f, cy + ctx.u * 3.6f, ctx.u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
}

/* =====================================================================================
 *  7) FIVE DIAL HAYABUSA HYBRID — Gen 1 / Gen 2 / Gen 3
 * ===================================================================================== */
internal fun DrawScope.drawHayabusaFiveDial(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val railH = ctx.h * 0.072f
    val gen3 = ctx.spec.skinId.contains("gen3")
    val gen1 = ctx.spec.skinId.contains("gen1_340")
    val rpmMajor = max(4, (ctx.maxRpm / 1000.0).roundToInt())
    val rpmFormatter: (Int) -> String = { it.toString() }
    val speedMajor = max(4, (ctx.speedScaleMax / 20.0).roundToInt())
    val speedFormatter: (Int) -> String = { (it * 20).toString() }
    val spdRatio = (ctx.speedValue.toFloat() / ctx.speedScaleMax).coerceIn(0f, 1f)

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.0f, ctx.w * 0.30f, ctx.u)

    if (ctx.wide) {
        val cy = ctx.h * 0.42f
        val speedR = min(ctx.h * 0.22f, ctx.w * 0.14f)
        val tachR = min(ctx.h * 0.25f, ctx.w * 0.16f)
        val auxR = min(ctx.h * 0.18f, ctx.w * 0.11f)

        // Hız kadranı (sol)
        drawNeedleDial(ctx.w * 0.16f, cy, speedR, 140f, 260f, spdRatio, p, ctx.u, p.fg, 0.86f, speedMajor, 2, true, chrome = true, labelFormatter = speedFormatter)
        gText(ctx.speedUnit.uppercase(), ctx.w * 0.16f, cy + speedR * 0.58f, ctx.u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)
        gText("SPEED", ctx.w * 0.16f, cy - speedR * 0.68f, ctx.u * 1.3f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.12f)

        // Devir kadranı (merkez)
        drawNeedleDial(ctx.w * 0.50f, cy, tachR, 140f, 260f, ctx.rpmRatio, p, ctx.u, p.accent, ctx.redlineRatio, rpmMajor, 2, true, chrome = true, labelFormatter = rpmFormatter)
        gText("x1000 r/min", ctx.w * 0.50f, cy + tachR * 0.58f, ctx.u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.08f)
        drawBrandMark(ctx.spec, p, ctx.w * 0.50f, cy - tachR * 0.74f, ctx.u * 4.0f, ctx.u, p.accent.copy(alpha = 0.65f))

        // Sağ: yakıt + hararet
        drawFuelDial(ctx, ctx.w * 0.84f, cy - auxR * 0.82f, auxR, t.fuelLevelRatio)
        drawTempDial(ctx, ctx.w * 0.84f, cy + auxR * 0.92f, auxR, ((t.coolantTempCelsius - 40) / 80).toFloat().coerceIn(0f, 1f), t.coolantTempCelsius)

        // Alt merkez: vites + Hayabusa OEM merkez LCD (ODO / TRIP A / SEANS)
        val lcdW = min(ctx.w * 0.42f, ctx.u * 68f)
        drawPanel(ctx.w * 0.5f - lcdW * 0.5f, ctx.h * 0.71f, lcdW, ctx.h * 0.15f, p, accentEdge = true)
        drawGearPod(ctx.w * 0.5f - lcdW * 0.34f, ctx.h * 0.785f, ctx.u * 7.5f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
        gText("GEAR", ctx.w * 0.5f - lcdW * 0.34f, ctx.h * 0.842f, ctx.u * 1.15f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        gText("${ctx.speedValue} ${ctx.speedUnit}", ctx.w * 0.5f - lcdW * 0.14f, ctx.h * 0.775f, ctx.u * 3.8f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.5f - lcdW * 0.14f, ctx.h * 0.822f, ctx.u * 1.45f, p.fg.copy(alpha = 0.90f), Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}", ctx.w * 0.5f - lcdW * 0.14f, ctx.h * 0.848f, ctx.u * 1.25f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
        gText(if (gen3) "GEN 3  •  ${ctx.subMode.uppercase()}" else if (gen1) "GEN 1  •  340" else "GEN 2  •  ${ctx.subMode.uppercase()}", ctx.w * 0.5f + lcdW * 0.45f, ctx.h * 0.765f, ctx.u * 1.30f, p.accent, Paint.Align.RIGHT, ctx.type.label, true)
    } else {
        val cx = ctx.w * 0.50f
        val cy = ctx.h * 0.31f
        val r = min(ctx.w * 0.30f, ctx.h * 0.20f)
        drawNeedleDial(cx, cy, r, 140f, 260f, ctx.rpmRatio, p, ctx.u, p.accent, ctx.redlineRatio, rpmMajor, 2, true, chrome = true, labelFormatter = rpmFormatter)
        gText("x1000 r/min", cx, cy + r * 0.6f, ctx.u * 1.4f, p.muted, Paint.Align.CENTER, ctx.type.label, true)

        drawNeedleDial(ctx.w * 0.27f, ctx.h * 0.61f, r * 0.72f, 140f, 260f, spdRatio, p, ctx.u, p.fg, 0.86f, speedMajor, 2, true, chrome = true, labelFormatter = speedFormatter)
        drawFuelDial(ctx, ctx.w * 0.73f, ctx.h * 0.56f, r * 0.45f, t.fuelLevelRatio)
        drawTempDial(ctx, ctx.w * 0.73f, ctx.h * 0.68f, r * 0.45f, ((t.coolantTempCelsius - 40) / 80).toFloat().coerceIn(0f, 1f), t.coolantTempCelsius)
        drawPanel(ctx.w * 0.12f, ctx.h * 0.75f, ctx.w * 0.76f, ctx.h * 0.10f, p, accentEdge = true)
        drawGearPod(ctx.w * 0.22f, ctx.h * 0.80f, ctx.u * 6.5f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
        gText("${ctx.speedValue} ${ctx.speedUnit}", ctx.w * 0.34f, ctx.h * 0.805f, ctx.u * 3.6f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.78f, ctx.h * 0.805f, ctx.u * 1.4f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)
    }
    drawStatusRail(ctx, RailStyle.CLASSIC)
}

private fun DrawScope.drawFuelDial(ctx: GaugeContext, cx: Float, cy: Float, r: Float, ratio: Float) {
    val p = ctx.palette
    drawDialFace(cx, cy, r, p, chrome = true, u = ctx.u)
    drawArcTrack(cx, cy, r - ctx.u * 2.0f, 150f, 240f, ctx.u * 1.3f, p.track)
    drawArcSegment(cx, cy, r - ctx.u * 2.0f, 150f, 240f * ratio.coerceIn(0f, 1f), ctx.u * 1.6f, p.accent)
    gText("E", cx - r * 0.52f, cy + r * 0.45f, ctx.u * 1.8f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    gText("F", cx + r * 0.52f, cy + r * 0.45f, ctx.u * 1.8f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    drawFuelIcon(cx, cy - r * 0.05f, r * 0.34f, p.fg.copy(alpha = 0.55f))
    gText("FUEL", cx, cy + r * 0.72f, ctx.u * 1.05f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)
}

private fun DrawScope.drawTempDial(ctx: GaugeContext, cx: Float, cy: Float, r: Float, ratio: Float, tempC: Double) {
    val p = ctx.palette
    drawDialFace(cx, cy, r, p, chrome = true, u = ctx.u)
    drawArcTrack(cx, cy, r - ctx.u * 2.0f, 150f, 240f, ctx.u * 1.3f, p.track)
    drawArcSegment(cx, cy, r - ctx.u * 2.0f, 150f, 240f * ratio.coerceIn(0f, 1f), ctx.u * 1.6f, if (tempC > 105) RL_RED else p.fg)
    gText("C", cx - r * 0.52f, cy + r * 0.45f, ctx.u * 1.8f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    gText("H", cx + r * 0.52f, cy + r * 0.45f, ctx.u * 1.8f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    gText("${tempC.roundToInt()}°", cx, cy + r * 0.18f, ctx.u * 2.2f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
    gText("TEMP", cx, cy + r * 0.72f, ctx.u * 1.05f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)
}

private fun DrawScope.drawFuelIcon(cx: Float, cy: Float, r: Float, color: Color) {
    drawRoundRect(color, Offset(cx - r * 0.62f, cy - r * 0.72f), Size(r * 1.1f, r * 1.45f), androidx.compose.ui.geometry.CornerRadius(r * 0.14f, r * 0.14f), style = Stroke(width = r * 0.22f))
    drawLine(color, Offset(cx + r * 0.48f, cy - r * 0.12f), Offset(cx + r * 0.95f, cy + r * 0.34f), r * 0.22f)
    drawLine(color, Offset(cx + r * 0.95f, cy + r * 0.34f), Offset(cx + r * 0.95f, cy + r * 0.78f), r * 0.22f)
}

/* =====================================================================================
 *  8) DUAL CLASSIC ANALOG — R nineT, Bonneville, Thruxton, GT 650, Forza, TMAX
 * ===================================================================================== */
internal fun DrawScope.drawDualClassicAnalog(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val cream = ctx.spec.brand == "Triumph" || ctx.spec.brand == "Royal Enfield"
    val railH = ctx.h * 0.085f
    val rpmMajor = max(4, (ctx.maxRpm / 1000.0).roundToInt())
    val rpmFormatter: (Int) -> String = { it.toString() }
    val speedMajor = max(4, (ctx.speedScaleMax / 20.0).roundToInt())
    val speedFormatter: (Int) -> String = { (it * 20).toString() }
    val spdRatio = (ctx.speedValue.toFloat() / ctx.speedScaleMax).coerceIn(0f, 1f)
    val labelFamily = if (cream) "serif" else ctx.type.label

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.2f, ctx.w * 0.24f, ctx.u)

    if (ctx.wide) {
        val cy = ctx.h * 0.46f
        val r = min(ctx.h * 0.31f, ctx.w * 0.21f)
        val leftCx = ctx.w * 0.28f
        val rightCx = ctx.w * 0.72f

        drawNeedleDial(leftCx, cy, r, 140f, 260f, spdRatio, p, ctx.u, if (cream) Color(0xFF8B1E1E) else p.fg, 0.86f, speedMajor, 2, true, cream, true, labelFamily, speedFormatter)
        gText(ctx.speedUnit.uppercase(), leftCx, cy + r * 0.58f, ctx.u * 1.30f, if (cream) Color(0xFF3A3126) else p.muted, Paint.Align.CENTER, labelFamily, true, spacing = 0.12f)
        gText("SPEED", leftCx, cy - r * 0.66f, ctx.u * 1.25f, if (cream) Color(0xFF3A3126) else p.muted, Paint.Align.CENTER, labelFamily, true, spacing = 0.16f)
        // Sol kadran içi klasik tambur/LCD ODO penceresi
        drawRoundRect(Color(0xFF14181E).copy(alpha = 0.88f), Offset(leftCx - r * 0.36f, cy + r * 0.24f), Size(r * 0.72f, r * 0.18f), androidx.compose.ui.geometry.CornerRadius(ctx.u * 0.6f, ctx.u * 0.6f))
        gText("ODO ${ctx.formatOdometer()}", leftCx, cy + r * 0.36f, ctx.u * 1.35f, Color(0xFFEAE3D2), Paint.Align.CENTER, "monospace", true)

        drawNeedleDial(rightCx, cy, r, 140f, 260f, ctx.rpmRatio, p, ctx.u, if (cream) Color(0xFF8B1E1E) else p.accent, ctx.redlineRatio, rpmMajor, 2, true, cream, true, labelFamily, rpmFormatter)
        gText("x1000 r/min", rightCx, cy + r * 0.58f, ctx.u * 1.30f, if (cream) Color(0xFF3A3126) else p.muted, Paint.Align.CENTER, labelFamily, true)
        gText("TACHO", rightCx, cy - r * 0.66f, ctx.u * 1.25f, if (cream) Color(0xFF3A3126) else p.muted, Paint.Align.CENTER, labelFamily, true, spacing = 0.16f)
        // Sağ kadran içi klasik LCD TRIP penceresi
        drawRoundRect(Color(0xFF14181E).copy(alpha = 0.88f), Offset(rightCx - r * 0.36f, cy + r * 0.24f), Size(r * 0.72f, r * 0.18f), androidx.compose.ui.geometry.CornerRadius(ctx.u * 0.6f, ctx.u * 0.6f))
        gText("TRIP ${ctx.formatTrip()}", rightCx, cy + r * 0.36f, ctx.u * 1.35f, Color(0xFFEAE3D2), Paint.Align.CENTER, "monospace", true)

        // Orta alt LCD
        val lcdW = ctx.w * 0.26f
        drawPanel(ctx.w * 0.5f - lcdW * 0.5f, ctx.h * 0.73f, lcdW, ctx.h * 0.12f, p, accentEdge = false)
        drawGearPod(ctx.w * 0.5f - lcdW * 0.30f, ctx.h * 0.79f, ctx.u * 5.4f, p, t, ctx.isNeutral, ctx.u, labelFamily, GearPodShape.ROUND)
        gText("${ctx.speedValue} ${ctx.speedUnit}", ctx.w * 0.5f - lcdW * 0.08f, ctx.h * 0.785f, ctx.u * 2.8f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS ${ctx.formatSession()}", ctx.w * 0.5f - lcdW * 0.08f, ctx.h * 0.825f, ctx.u * 1.30f, p.accent, Paint.Align.LEFT, labelFamily, true)

        // Klasik ikaz lambaları
        drawClassicIndicatorLights(ctx, ctx.w * 0.5f, ctx.h * 0.16f, ctx.w * 0.20f)

        // Marka yazısı
        gText(ctx.spec.brand.uppercase(), ctx.w * 0.5f, ctx.h * 0.10f, ctx.u * 2.7f, p.accent.copy(alpha = 0.85f), Paint.Align.CENTER, labelFamily, true, spacing = 0.24f)
        gText(ctx.spec.modelName.uppercase(), ctx.w * 0.5f, ctx.h * 0.132f, ctx.u * 1.25f, p.muted, Paint.Align.CENTER, labelFamily, false, spacing = 0.08f)
    } else {
        val cx = ctx.w * 0.50f
        val topCy = ctx.h * 0.30f
        val botCy = ctx.h * 0.56f
        val r = min(ctx.w * 0.34f, ctx.h * 0.17f)
        drawNeedleDial(cx, topCy, r, 140f, 260f, spdRatio, p, ctx.u, if (cream) Color(0xFF8B1E1E) else p.fg, 0.86f, speedMajor, 2, true, cream, true, labelFamily, speedFormatter)
        gText(ctx.speedUnit.uppercase(), cx, topCy + r * 0.58f, ctx.u * 1.25f, p.muted, Paint.Align.CENTER, labelFamily, true)
        drawNeedleDial(cx, botCy, r, 140f, 260f, ctx.rpmRatio, p, ctx.u, if (cream) Color(0xFF8B1E1E) else p.accent, ctx.redlineRatio, rpmMajor, 2, true, cream, true, labelFamily, rpmFormatter)
        gText("x1000 r/min", cx, botCy + r * 0.58f, ctx.u * 1.25f, p.muted, Paint.Align.CENTER, labelFamily, true)
        drawPanel(ctx.w * 0.12f, ctx.h * 0.78f, ctx.w * 0.43f, ctx.h * 0.10f, p)
        drawGearPod(ctx.w * 0.22f, ctx.h * 0.83f, ctx.u * 5.2f, p, t, ctx.isNeutral, ctx.u, labelFamily, GearPodShape.ROUND)
        gText("${ctx.speedValue} ${ctx.speedUnit}", ctx.w * 0.32f, ctx.h * 0.835f, ctx.u * 3.0f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        drawTemperatureAndFuel(ctx, ctx.w * 0.60f, ctx.h * 0.79f, ctx.w * 0.28f, compact = true)
        drawClassicIndicatorLights(ctx, ctx.w * 0.50f, ctx.h * 0.12f, ctx.w * 0.30f)
    }
    drawStatusRail(ctx, RailStyle.CLASSIC)
}

private fun DrawScope.drawClassicIndicatorLights(ctx: GaugeContext, cx: Float, cy: Float, span: Float) {
    val p = ctx.palette
    val t = ctx.telemetry
    val gap = span / 5f
    val lights = listOf(
        Triple("N", t.isNeutral, RL_GREEN),
        Triple("BEAM", t.nightModeTransitionFactor > 0.5f, Color(0xFF4DA3FF)),
        Triple("OIL", t.actualEngineRpm > 0.0, RL_RED),
        Triple("TC", t.tractionControlInterventionActive, RL_AMBER),
        Triple("ABS", t.absInterventionActive, RL_RED)
    )
    lights.forEachIndexed { i, (label, on, color) ->
        val x = cx + (i - 2) * gap
        drawCircle(if (on) color.copy(alpha = 0.22f) else p.fg.copy(alpha = 0.04f), ctx.u * 1.9f, Offset(x, cy))
        drawCircle(if (on) color else p.fg.copy(alpha = 0.14f), ctx.u * 1.9f, Offset(x, cy), style = Stroke(width = ctx.u * 0.35f))
        gText(label, x, cy + ctx.u * 0.55f, ctx.u * 1.05f, if (on) color else p.fg.copy(alpha = 0.22f), Paint.Align.CENTER, "sans-serif-condensed", true)
    }
}

/* =====================================================================================
 *  9) SINGLE PUCK — Svartpilen/Vitpilen, Himalayan 450, Sportster S, Iron 883
 * ===================================================================================== */
internal fun DrawScope.drawSinglePuck(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val railH = ctx.h * 0.072f
    val analogPuck = ctx.spec.skinId.contains("iron_883")
    val speedMajor = max(4, (ctx.speedScaleMax / 20.0).roundToInt())
    val speedFormatter: (Int) -> String = { (it * 20).toString() }
    val spdRatio = (ctx.speedValue.toFloat() / ctx.speedScaleMax).coerceIn(0f, 1f)

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.2f, ctx.w * 0.24f, ctx.u)

    if (ctx.wide) {
        val cx = ctx.w * 0.33f
        val cy = ctx.h * 0.47f
        val r = min(ctx.h * 0.36f, ctx.w * 0.245f)

        if (analogPuck) {
            drawNeedleDial(cx, cy, r, 140f, 260f, spdRatio, p, ctx.u, p.fg, 0.86f, speedMajor, 2, true, chrome = true, labelFormatter = speedFormatter)
            gText(ctx.speedUnit.uppercase(), cx, cy + r * 0.55f, ctx.u * 1.4f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.12f)
            // küçük mekanik odometer
            drawRoundRect(p.fg.copy(alpha = 0.10f), Offset(cx - r * 0.34f, cy + r * 0.24f), Size(r * 0.68f, r * 0.18f), androidx.compose.ui.geometry.CornerRadius(r * 0.03f, r * 0.03f))
            gText(ctx.formatOdometer(), cx, cy + r * 0.37f, r * 0.12f, p.fg, Paint.Align.CENTER, "monospace", true)
            drawClassicIndicatorLights(ctx, cx, cy - r * 0.72f, r * 0.9f)
        } else {
            // Puck çevresi devir halkası
            drawSegmentedArc(cx, cy, r, 135f, 270f, 38, ctx.rpmRatio, ctx.u * 3.4f, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
            // iç disk
            drawCircle(p.bg.copy(alpha = 0.82f), r - ctx.u * 3.4f, Offset(cx, cy))
            drawCircle(p.fg.copy(alpha = 0.10f), r - ctx.u * 3.4f, Offset(cx, cy), style = Stroke(width = ctx.u * 0.35f))
            drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + ctx.u * 4.0f, ctx.u * 15.0f, p.fg, p.muted, ctx.type.digits)
            drawGearPod(cx, cy + r * 0.48f, ctx.u * 7.2f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.ROUND)
            gText("x1000 RPM", cx, cy - r * 0.48f, ctx.u * 1.35f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)
            // saat yönü işaretçisi
            val tickCount = 12
            for (i in 0 until tickCount) {
                val a = 135f + 270f * i / tickCount
                val pt1 = polar(cx, cy, r + ctx.u * 1.2f, a)
                val pt2 = polar(cx, cy, r + ctx.u * 2.8f, a)
                drawLine(p.fg.copy(alpha = if (i % 3 == 0) 0.45f else 0.18f), pt1, pt2, ctx.u * 0.35f, StrokeCap.Round)
            }
        }

        // Sağ sütun kartları
        val left = ctx.w * 0.65f
        val width = ctx.w * 0.31f
        drawPanel(left, ctx.h * 0.20f, width, ctx.h * 0.19f, p, accentEdge = true)
        gText("RIDE MODE", left + ctx.u * 3.0f, ctx.h * 0.255f, ctx.u * 1.3f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.14f)
        gText(ctx.subMode.uppercase(), left + ctx.u * 3.0f, ctx.h * 0.305f, ctx.u * 3.2f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.04f)
        gText("${ctx.spec.generationYears}  •  ${ctx.spec.modelName.uppercase()}", left + ctx.u * 3.0f, ctx.h * 0.345f, ctx.u * 1.2f, p.muted, Paint.Align.LEFT, ctx.type.label, false)

        drawPanel(left, ctx.h * 0.42f, width, ctx.h * 0.20f, p)
        gText("FUEL", left + ctx.u * 3.0f, ctx.h * 0.475f, ctx.u * 1.3f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.14f)
        gText("${(t.fuelLevelRatio * 100).roundToInt()}%", left + width - ctx.u * 3.0f, ctx.h * 0.49f, ctx.u * 2.8f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
        drawLinearBar(left + ctx.u * 3.0f, ctx.h * 0.50f, width - ctx.u * 6.0f, ctx.u * 1.2f, t.fuelLevelRatio, p.accent, p.track, 0.9f, RL_RED, ctx.u * 0.6f)
        gText("${t.coolantTempCelsius.roundToInt()}°C   ${t.gpsSatelliteCount} SAT", left + ctx.u * 3.0f, ctx.h * 0.575f, ctx.u * 1.5f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

        drawPanel(left, ctx.h * 0.64f, width, ctx.h * 0.21f, p, accentEdge = true)
        gText("ODO  ${ctx.formatOdometer()}", left + ctx.u * 3.0f, ctx.h * 0.695f, ctx.u * 2.2f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", left + ctx.u * 3.0f, ctx.h * 0.755f, ctx.u * 1.9f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS  ${ctx.formatSession()}", left + ctx.u * 3.0f, ctx.h * 0.810f, ctx.u * 1.55f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    } else {
        val cx = ctx.w * 0.50f
        val cy = ctx.h * 0.36f
        val r = min(ctx.w * 0.42f, ctx.h * 0.245f)
        if (analogPuck) {
            drawNeedleDial(cx, cy, r, 140f, 260f, spdRatio, p, ctx.u, p.fg, 0.86f, speedMajor, 2, true, chrome = true, labelFormatter = speedFormatter)
            gText(ctx.speedUnit.uppercase(), cx, cy + r * 0.55f, ctx.u * 1.35f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
            drawClassicIndicatorLights(ctx, cx, cy - r * 0.72f, r * 0.9f)
        } else {
            drawSegmentedArc(cx, cy, r, 135f, 270f, 36, ctx.rpmRatio, ctx.u * 3.0f, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
            drawCircle(p.bg.copy(alpha = 0.82f), r - ctx.u * 3.0f, Offset(cx, cy))
            drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + ctx.u * 3.0f, ctx.u * 14.0f, p.fg, p.muted, ctx.type.digits)
            drawGearPod(cx, cy + r * 0.48f, ctx.u * 6.4f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.ROUND)
            gText("x1000 RPM", cx, cy - r * 0.45f, ctx.u * 1.3f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        }
        drawTemperatureAndFuel(ctx, ctx.w * 0.12f, ctx.h * 0.68f, ctx.w * 0.36f, compact = true)
        drawPanel(ctx.w * 0.56f, ctx.h * 0.66f, ctx.w * 0.31f, ctx.h * 0.15f, p, accentEdge = true)
        gText(ctx.subMode.uppercase(), ctx.w * 0.60f, ctx.h * 0.715f, ctx.u * 2.2f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
        gText("TRIP ${ctx.formatTrip()}", ctx.w * 0.60f, ctx.h * 0.755f, ctx.u * 1.6f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.60f, ctx.h * 0.785f, ctx.u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    }
    drawStatusRail(ctx, if (analogPuck) RailStyle.CLASSIC else RailStyle.TFT)
}
