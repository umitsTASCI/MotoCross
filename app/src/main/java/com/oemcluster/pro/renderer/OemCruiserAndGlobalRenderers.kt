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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 1:1 Cruiser, Hint, Çin/Global Adventure & Heritage OEM Gösterge Ailesi:
 * - Harley-Davidson (12.3" Skyline OS, 4" Round Sportster S, Chrome Analog + 6 Saat LCD)
 * - Indian (4" Ride Command Round TFT, Twin Dial + 7" Challenger Split)
 * - Royal Enfield (Himalayan/Guerrilla 450 4" Round Tripper Dash, Classic/Meteor + Sağ Tripper Pod, 650 Twin Clocks)
 * - CFMoto (8" Dikey 800NK/MT Tablet MMI, 5" Kavisli 450SR/675 Yarış TFT, 450MT Rally TFT)
 * - Hero (Inverted Blue-Black LCD)
 * - Moto Guzzi, Kove Rally Tower, QJMotor / Voge / Zontes / Benelli, Norton / Bimota / Jawa / Brixton.
 * Tüm fonksiyonlar hem yatay (wide) hem dikey (portrait) düzende gerçek telemetri ile ölçeklenir.
 */

// =========================================================================
// 1. HARLEY-DAVIDSON — 12.3" Skyline OS, 4" Sportster S Round, Analog LCD
// =========================================================================

internal fun DrawScope.drawHarleySkyline123Tft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val hdOrange = Color(0xFFFF6600)
    val titanium = Color(0xFF9EA6B2)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    // 2024+ Touring 12.3" Geniş cam Skyline OS üst şerit
    val frameLeft = ctx.w * 0.035f
    val frameTop = ctx.h * 0.055f
    val frameW = ctx.w * 0.93f
    val frameH = ctx.h - railH - frameTop - u * 2.5f
    drawPanel(frameLeft, frameTop, frameW, frameH, Color(0xFF0A0C10), hdOrange.copy(alpha = 0.55f), u * 1.8f)

    drawBrandMark(ctx.spec, p, frameLeft + u * 4.5f, frameTop + u * 3.8f, u * 4.2f, u, hdOrange)
    gText("SKYLINE OS  //  ${ctx.subMode.uppercase()}", frameLeft + u * 9.5f, frameTop + u * 4.5f, u * 1.65f, titanium, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.08f)
    gText("${t.gpsSatelliteCount} SAT  •  ${clusterClockText(ctx.clock24h)}", frameLeft + frameW - u * 2.8f, frameTop + u * 4.5f, u * 1.95f, Color.White, Paint.Align.RIGHT, ctx.type.digits, true)

    drawLine(Color(0xFF232833), Offset(frameLeft + u * 2.0f, frameTop + u * 7.2f), Offset(frameLeft + frameW - u * 2.0f, frameTop + u * 7.2f), u * 0.35f)

    if (ctx.wide) {
        // Sol: Dairesel Krom/Turuncu Skyline Hız & Devir Kadranı
        val dialCx = frameLeft + frameW * 0.30f
        val dialCy = frameTop + frameH * 0.55f
        val dialR = min(frameW * 0.22f, frameH * 0.35f)
        drawSegmentedArc(dialCx, dialCy, dialR, 138f, 264f, 40, ctx.rpmRatio, u * 3.6f, hdOrange, Color(0xFF1A1F28), ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
        drawSpeed(ctx.speedValue, ctx.speedUnit, dialCx, dialCy + u * 2.0f, u * 14.5f, Color.White, titanium, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, dialCx, dialCy + dialR * 0.62f, u * 10.5f, hdOrange, Color.White, ctx.type.digits)

        // Orta & Sağ: Skyline Kartlar (Navigasyon Pusulası, Menzil, Eğim & Akü)
        val rightX = frameLeft + frameW * 0.56f
        val rightW = frameW * 0.40f
        val cardTop = frameTop + u * 9.5f
        drawCompassStrip(ctx, cardTop, u * 3.8f)

        val infoTop = cardTop + u * 5.8f
        val infoH = frameH - (infoTop - frameTop) - u * 3.0f
        drawPanel(rightX, infoTop, rightW, infoH, Color(0xFF11151D), Color(0xFF2B3240), u * 1.4f)
        gText("CRUISE  •  RIDE DATA", rightX + u * 2.2f, infoTop + u * 3.6f, u * 1.45f, hdOrange, Paint.Align.LEFT, ctx.type.label, true)
        gText("RANGE    ${ctx.formatRange()}", rightX + u * 2.2f, infoTop + u * 8.6f, u * 2.25f, Color.White, Paint.Align.LEFT, ctx.type.digits, true)
        gText("ODO      ${ctx.formatOdometer()}", rightX + u * 2.2f, infoTop + u * 13.4f, u * 2.0f, titanium, Paint.Align.LEFT, ctx.type.digits, true)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°   •   ${ctx.formatBattery()}   •   ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", rightX + u * 2.2f, infoTop + infoH - u * 3.0f, u * 1.65f, titanium, Paint.Align.LEFT, ctx.type.label, true)
    } else {
        val dialCx = ctx.w * 0.50f
        val dialCy = frameTop + frameH * 0.43f
        val dialR = min(frameW * 0.40f, frameH * 0.30f)
        drawSegmentedArc(dialCx, dialCy, dialR, 138f, 264f, 38, ctx.rpmRatio, u * 3.6f, hdOrange, Color(0xFF1A1F28), ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
        drawSpeed(ctx.speedValue, ctx.speedUnit, dialCx, dialCy + u * 2.0f, u * 15.5f, Color.White, titanium, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, dialCx, dialCy + dialR * 0.62f, u * 11.0f, hdOrange, Color.White, ctx.type.digits)
        gText("RANGE ${ctx.formatRange()}  •  ODO ${ctx.formatOdometer()}", ctx.w * 0.50f, frameTop + frameH * 0.80f, u * 1.9f, Color.White, Paint.Align.CENTER, ctx.type.digits, true)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°  •  ${ctx.formatBattery()}  •  ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", ctx.w * 0.50f, frameTop + frameH * 0.88f, u * 1.65f, titanium, Paint.Align.CENTER, ctx.type.label, true)
    }

    drawSubModeChip(ctx, frameLeft + u * 2.4f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawHarleySportsterS4Tft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val hdOrange = Color(0xFFFF6600)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val cx = ctx.w * (if (ctx.wide) 0.44f else 0.50f)
    val cy = ctx.h * (if (ctx.wide) 0.49f else 0.43f)
    val r = min(ctx.w * (if (ctx.wide) 0.28f else 0.40f), (ctx.h - railH) * 0.43f)

    // 4" Tam Daire Bonded Glass Bezel
    drawCircle(Color(0xFF0A0C10), r * 1.06f, Offset(cx, cy))
    drawCircle(Color(0xFF8C94A0), r * 1.06f, Offset(cx, cy), style = Stroke(width = u * 0.85f))
    drawSegmentedArc(cx, cy, r * 0.90f, 138f, 264f, 40, ctx.rpmRatio, u * 3.8f, hdOrange, Color(0xFF1D222B), ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)

    drawBrandMark(ctx.spec, p, cx, cy - r * 0.50f, u * 4.2f, u, hdOrange)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 1.5f, u * 15.5f, Color.White, Color(0xFF9EA6B2), ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.58f, u * 11.5f, hdOrange, Color.White, ctx.type.digits)

    if (ctx.wide) {
        val sideX = ctx.w * 0.76f
        val sideW = ctx.w * 0.20f
        drawPanel(sideX, ctx.h * 0.20f, sideW, ctx.h * 0.58f, Color(0xFF0D1016), hdOrange.copy(alpha = 0.45f), u * 1.5f)
        gText(ctx.subMode.uppercase(), sideX + u * 1.8f, ctx.h * 0.28f, u * 2.2f, hdOrange, Paint.Align.LEFT, ctx.type.label, true)
        gText("RANGE ${ctx.formatRange()}", sideX + u * 1.8f, ctx.h * 0.40f, u * 1.9f, Color.White, Paint.Align.LEFT, ctx.type.digits, true)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", sideX + u * 1.8f, ctx.h * 0.50f, u * 2.1f, Color.White, Paint.Align.LEFT, ctx.type.digits, true)
        gText(ctx.formatBattery(), sideX + u * 1.8f, ctx.h * 0.62f, u * 1.8f, Color(0xFF9EA6B2), Paint.Align.LEFT, ctx.type.digits, true)
        gText("ODO ${ctx.formatOdometer()}", sideX + u * 1.8f, ctx.h * 0.71f, u * 1.55f, Color(0xFF9EA6B2), Paint.Align.LEFT, ctx.type.label, true)
    } else {
        gText("${ctx.subMode.uppercase()}  •  RANGE ${ctx.formatRange()}  •  LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.50f, ctx.h * 0.77f, u * 1.8f, hdOrange, Paint.Align.CENTER, ctx.type.label, true)
    }
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawHarleyIron883AnalogLcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val hdOrange = Color(0xFFFF7A18)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val cx = ctx.w * 0.50f
    val cy = ctx.h * (if (ctx.wide) 0.48f else 0.43f)
    val r = min(ctx.w * (if (ctx.wide) 0.32f else 0.40f), (ctx.h - railH) * 0.43f)

    val maxScale = max(140, ctx.speedScaleMax)
    val spdRatio = (ctx.speedValue / maxScale.toFloat()).coerceIn(0f, 1f)
    drawNeedleDial(
        cx = cx,
        cy = cy,
        radius = r,
        startAngle = 135f,
        sweepAngle = 270f,
        ratio = spdRatio,
        palette = p,
        u = u,
        needleColor = hdOrange,
        redlineRatio = 0.92f,
        majorCount = 7,
        minorPerMajor = 4,
        labels = true,
        cream = false,
        chrome = true,
        labelFamily = "serif",
        labelFormatter = { idx -> ((idx * maxScale) / 7).toString() }
    )
    drawBrandMark(ctx.spec, p, cx, cy - r * 0.44f, u * 3.8f, u, hdOrange)

    // Saat 6 konumunda ikonik dar amber-arkadan aydınlatmalı LCD pencere
    val lcdW = r * 0.96f
    val lcdH = r * 0.34f
    val lcdLeft = cx - lcdW * 0.5f
    val lcdTop = cy + r * 0.28f
    drawPanel(lcdLeft, lcdTop, lcdW, lcdH, Color(0xFF1A1108), hdOrange.copy(alpha = 0.65f), u * 0.9f)
    gText("${ctx.gearText}  •  ${ctx.mappedRpm.roundToInt()} RPM", cx, lcdTop + lcdH * 0.44f, u * 1.95f, hdOrange, Paint.Align.CENTER, "monospace", true)
    gText("ODO ${ctx.formatOdometer()}  •  TRIP ${ctx.formatTrip()}", cx, lcdTop + lcdH * 0.80f, u * 1.50f, Color(0xFFFFB74D), Paint.Align.CENTER, "monospace", true)

    drawStatusRail(ctx, RailStyle.CLASSIC)
}

// =========================================================================
// 2. INDIAN MOTORCYCLE — 4" Round FTR & Twin Challenger 7" Ride Command
// =========================================================================

internal fun DrawScope.drawIndianFtrRoundTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val indCrimson = Color(0xFF9E1B32)
    val indGold = Color(0xFFD4AF37)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val cx = ctx.w * (if (ctx.wide) 0.44f else 0.50f)
    val cy = ctx.h * (if (ctx.wide) 0.49f else 0.43f)
    val r = min(ctx.w * (if (ctx.wide) 0.28f else 0.40f), (ctx.h - railH) * 0.43f)

    drawCircle(Color(0xFF0C0A0C), r * 1.05f, Offset(cx, cy))
    drawCircle(indGold.copy(alpha = 0.65f), r * 1.05f, Offset(cx, cy), style = Stroke(width = u * 0.8f))
    drawSegmentedArc(cx, cy, r * 0.89f, 138f, 264f, 40, ctx.rpmRatio, u * 3.8f, indCrimson, Color(0xFF211B20), ctx.redlineRatio, RL_RED, ctx.peakRatio, indGold, 1.0f)

    drawBrandMark(ctx.spec, p, cx, cy - r * 0.48f, u * 4.0f, u, indGold)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 2.0f, u * 15.5f, Color.White, Color(0xFFB5A8A0), ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.58f, u * 11.0f, indCrimson, Color.White, ctx.type.digits)

    if (ctx.wide) {
        val px = ctx.w * 0.76f
        drawPanel(px, ctx.h * 0.22f, ctx.w * 0.20f, ctx.h * 0.56f, Color(0xFF120E10), indGold.copy(alpha = 0.45f), u * 1.5f)
        gText("RIDE COMMAND", px + u * 1.8f, ctx.h * 0.28f, u * 1.35f, indGold, Paint.Align.LEFT, ctx.type.label, true)
        gText(ctx.subMode.uppercase(), px + u * 1.8f, ctx.h * 0.36f, u * 2.2f, indCrimson, Paint.Align.LEFT, ctx.type.label, true)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", px + u * 1.8f, ctx.h * 0.46f, u * 2.2f, Color.White, Paint.Align.LEFT, ctx.type.digits, true)
        gText("RANGE ${ctx.formatRange()}", px + u * 1.8f, ctx.h * 0.56f, u * 1.9f, Color.White, Paint.Align.LEFT, ctx.type.digits, true)
        gText("0-60 ${ctx.formatBest0to60()}", px + u * 1.8f, ctx.h * 0.66f, u * 1.75f, indGold, Paint.Align.LEFT, ctx.type.digits, true)
    } else {
        gText("RIDE COMMAND  •  RANGE ${ctx.formatRange()}  •  LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.50f, ctx.h * 0.77f, u * 1.75f, indGold, Paint.Align.CENTER, ctx.type.label, true)
    }
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawIndianChallengerSplitTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val indCrimson = Color(0xFF9E1B32)
    val indGold = Color(0xFFD4AF37)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    if (ctx.wide) {
        val top = ctx.h * 0.10f
        val midH = ctx.h - railH - top - u * 3.0f
        val midX = ctx.w * 0.50f
        val midW = ctx.w * 0.34f
        drawPanel(midX - midW * 0.5f, top, midW, midH, Color(0xFF0E0C10), indGold.copy(alpha = 0.55f), u * 1.6f)
        drawBrandMark(ctx.spec, p, midX, top + u * 4.5f, u * 4.0f, u, indGold)
        drawCompassStrip(ctx, top + u * 8.0f, u * 3.6f)
        drawGearBadge(ctx.gearText, ctx.isNeutral, midX, top + midH * 0.52f, u * 13.0f, indCrimson, Color.White, ctx.type.digits)
        gText("RANGE ${ctx.formatRange()}  •  TRIP ${ctx.formatTrip()}", midX, top + midH * 0.78f, u * 1.75f, Color.White, Paint.Align.CENTER, ctx.type.digits, true)
        gText("${ctx.formatBattery()}  •  ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", midX, top + midH * 0.90f, u * 1.65f, indGold, Paint.Align.CENTER, ctx.type.label, true)

        val dialR = min(ctx.w * 0.145f, midH * 0.44f)
        val spdCy = top + midH * 0.50f
        val spdScale = max(160, ctx.speedScaleMax)
        val spdRatio = (ctx.speedValue / spdScale.toFloat()).coerceIn(0f, 1f)
        drawNeedleDial(ctx.w * 0.17f, spdCy, dialR, 135f, 270f, spdRatio, p, u, indCrimson, 0.95f, 7, 3, true, cream = true, chrome = true, labelFamily = "serif") { (it * spdScale / 7).toString() }
        gText("${ctx.speedValue} ${ctx.speedUnit}", ctx.w * 0.17f, spdCy + dialR * 0.45f, u * 2.2f, Color(0xFF20242A), Paint.Align.CENTER, ctx.type.digits, true)

        val maxK = max(6, (ctx.maxRpm / 1000.0).roundToInt())
        drawNeedleDial(ctx.w * 0.83f, spdCy, dialR, 135f, 270f, ctx.rpmRatio, p, u, indCrimson, ctx.redlineRatio, maxK, 2, true, cream = true, chrome = true, labelFamily = "serif") { it.toString() }
        gText("x1000 RPM", ctx.w * 0.83f, spdCy + dialR * 0.45f, u * 1.55f, Color(0xFF20242A), Paint.Align.CENTER, ctx.type.label, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.40f, u * 20.0f, p.fg, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * 0.50f, ctx.h * 0.58f, u * 14.5f, indCrimson, p.fg, ctx.type.digits)
        gText("RIDE COMMAND  •  RANGE ${ctx.formatRange()}", ctx.w * 0.50f, ctx.h * 0.74f, u * 1.9f, indGold, Paint.Align.CENTER, ctx.type.label, true)
    }
    drawStatusRail(ctx, RailStyle.CLASSIC)
}

// =========================================================================
// 3. ROYAL ENFIELD — Himalayan 450 Tripper Dash, Meteor+Tripper Pod, 650 Twin
// =========================================================================

internal fun DrawScope.drawRoyalEnfieldHimalayan450TripperDash(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val reAmber = Color(0xFFFFB300)
    val reCyan = Color(0xFF29B6F6)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val cx = ctx.w * (if (ctx.wide) 0.43f else 0.50f)
    val cy = ctx.h * (if (ctx.wide) 0.49f else 0.43f)
    val r = min(ctx.w * (if (ctx.wide) 0.28f else 0.40f), (ctx.h - railH) * 0.43f)

    // 4" Tam Yuvarlak Tripper Dash Krom Çerçeve + Devir Çemberi
    drawCircle(Color(0xFF0B0E13), r * 1.05f, Offset(cx, cy))
    drawCircle(Color(0xFFA6ABB5), r * 1.05f, Offset(cx, cy), style = Stroke(width = u * 0.85f))
    drawSegmentedArc(cx, cy, r * 0.89f, 150f, 240f, 36, ctx.rpmRatio, u * 3.8f, reAmber, Color(0xFF1F242D), ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)

    // Üst yarıda navigasyon pusulası & marka
    drawBrandMark(ctx.spec, p, cx, cy - r * 0.54f, u * 3.8f, u, reAmber)
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 1.2f, u * 15.0f, Color.White, Color(0xFF98A3B3), ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, cx - r * 0.28f, cy + r * 0.55f, u * 9.5f, reAmber, Color.White, ctx.type.digits)
    gText("ABS ${if (t.absInterventionActive) "ACT" else "ON"}", cx + r * 0.25f, cy + r * 0.58f, u * 1.85f, reCyan, Paint.Align.CENTER, ctx.type.label, true)

    if (ctx.wide) {
        val cardX = ctx.w * 0.74f
        val cardW = ctx.w * 0.22f
        drawPanel(cardX, ctx.h * 0.18f, cardW, ctx.h * 0.62f, Color(0xFF0E1218), reAmber.copy(alpha = 0.5f), u * 1.6f)
        gText("TRIPPER DASH", cardX + u * 1.8f, ctx.h * 0.25f, u * 1.45f, reAmber, Paint.Align.LEFT, ctx.type.label, true)
        drawCompassStrip(ctx, ctx.h * 0.29f, u * 3.4f)
        gText("MODE ${ctx.subMode.uppercase()}", cardX + u * 1.8f, ctx.h * 0.44f, u * 1.85f, reCyan, Paint.Align.LEFT, ctx.type.label, true)
        gText("RANGE ${ctx.formatRange()}", cardX + u * 1.8f, ctx.h * 0.54f, u * 2.0f, Color.White, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP ${ctx.formatTrip()}", cardX + u * 1.8f, ctx.h * 0.63f, u * 1.8f, Color(0xFF98A3B3), Paint.Align.LEFT, ctx.type.digits, true)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)} • ${ctx.formatBattery()}", cardX + u * 1.8f, ctx.h * 0.72f, u * 1.55f, reAmber, Paint.Align.LEFT, ctx.type.label, true)
    } else {
        gText("TRIPPER DASH  •  RANGE ${ctx.formatRange()}  •  TRIP ${ctx.formatTrip()}", ctx.w * 0.50f, ctx.h * 0.77f, u * 1.75f, reAmber, Paint.Align.CENTER, ctx.type.label, true)
    }

    drawSubModeChip(ctx, ctx.w * 0.055f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawRoyalEnfieldTripperSatelliteAndAnalog(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val reGold = Color(0xFFD4AF37)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val isTwin650 = (ctx.spec.skinId.contains("interceptor") || ctx.spec.skinId.contains("continental")) && !ctx.spec.skinId.contains("meteor")
    val spdScale = max(140, ctx.speedScaleMax)
    val spdRatio = (ctx.speedValue / spdScale.toFloat()).coerceIn(0f, 1f)

    if (isTwin650) {
        // Interceptor 650 / Continental GT 650 İkiz Krom Analog Saat
        val r = min(ctx.w * (if (ctx.wide) 0.22f else 0.34f), (ctx.h - railH) * 0.38f)
        val leftCx = if (ctx.wide) ctx.w * 0.31f else ctx.w * 0.50f
        val leftCy = if (ctx.wide) ctx.h * 0.49f else ctx.h * 0.30f
        val rightCx = if (ctx.wide) ctx.w * 0.69f else ctx.w * 0.50f
        val rightCy = if (ctx.wide) ctx.h * 0.49f else ctx.h * 0.66f

        drawNeedleDial(leftCx, leftCy, r, 135f, 270f, spdRatio, p, u, Color(0xFFE63946), 0.94f, 7, 3, true, cream = true, chrome = true, labelFamily = "serif") { (it * spdScale / 7).toString() }
        drawPanel(leftCx - r * 0.36f, leftCy + r * 0.28f, r * 0.72f, r * 0.24f, Color(0xFF16181C), Color(0xFF424852), u * 0.6f)
        gText("${ctx.speedValue} ${ctx.speedUnit} • ${ctx.gearText}", leftCx, leftCy + r * 0.44f, u * 1.65f, Color.White, Paint.Align.CENTER, "monospace", true)

        val maxK = max(6, (ctx.maxRpm / 1000.0).roundToInt())
        drawNeedleDial(rightCx, rightCy, r, 135f, 270f, ctx.rpmRatio, p, u, Color(0xFFE63946), ctx.redlineRatio, maxK, 2, true, cream = true, chrome = true, labelFamily = "serif") { it.toString() }
        gText("ODO ${ctx.formatOdometer()}", rightCx, rightCy + r * 0.42f, u * 1.55f, Color(0xFF20242A), Paint.Align.CENTER, "serif", true)
    } else {
        // Classic 350 / Super Meteor / Shotgun 650: Büyük Ana Analog Kadran + Sağdaki Yuvarlak Tripper Podu
        val mainR = min(ctx.w * (if (ctx.wide) 0.25f else 0.34f), (ctx.h - railH) * 0.40f)
        val mainCx = if (ctx.wide) ctx.w * 0.40f else ctx.w * 0.42f
        val mainCy = if (ctx.wide) ctx.h * 0.49f else ctx.h * 0.42f

        drawNeedleDial(mainCx, mainCy, mainR, 135f, 270f, spdRatio, p, u, Color(0xFFE63946), 0.94f, 7, 3, true, cream = false, chrome = true, labelFamily = "serif") { (it * spdScale / 7).toString() }
        drawBrandMark(ctx.spec, p, mainCx, mainCy - mainR * 0.42f, u * 3.6f, u, reGold)
        drawPanel(mainCx - mainR * 0.38f, mainCy + mainR * 0.26f, mainR * 0.76f, mainR * 0.28f, Color(0xFF101318), reGold.copy(alpha = 0.55f), u * 0.7f)
        gText("GEAR ${ctx.gearText}  •  ODO ${ctx.formatOdometer()}", mainCx, mainCy + mainR * 0.45f, u * 1.65f, Color.White, Paint.Align.CENTER, "monospace", true)

        // Sağ Uydu Tripper Podu (gerçek Google/Tripper navigasyon tarzı küçük yuvarlak pod)
        val podR = mainR * 0.52f
        val podCx = if (ctx.wide) mainCx + mainR + podR * 1.35f else ctx.w * 0.74f
        val podCy = if (ctx.wide) mainCy + mainR * 0.14f else ctx.h * 0.68f
        drawCircle(Color(0xFF0A0D12), podR, Offset(podCx, podCy))
        drawCircle(Color(0xFFB8BDC6), podR, Offset(podCx, podCy), style = Stroke(width = u * 0.8f))
        gText("TRIPPER", podCx, podCy - podR * 0.42f, u * 1.25f, reGold, Paint.Align.CENTER, ctx.type.label, true)
        gText(ctx.formatTrip(), podCx, podCy + u * 0.8f, u * 2.5f, Color.White, Paint.Align.CENTER, ctx.type.digits, true)
        gText("RNG ${ctx.formatRange()}", podCx, podCy + podR * 0.48f, u * 1.45f, Color(0xFF29B6F6), Paint.Align.CENTER, ctx.type.label, true)
    }

    drawStatusRail(ctx, RailStyle.CLASSIC)
}

// =========================================================================
// 4. CFMOTO — 8" Vertical Tablet MMI, 450SR/675 Curved Race, 450MT Rally
// =========================================================================

internal fun DrawScope.drawCfmoto800NkVertical8Tft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val cfCyan = Color(0xFF00E5FF)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val tabW = if (ctx.wide) ctx.w * 0.44f else ctx.w * 0.88f
    val tabX = ctx.w * 0.50f - tabW * 0.5f
    val tabTop = ctx.h * 0.055f
    val tabH = ctx.h - railH - tabTop - u * 2.4f
    drawPanel(tabX, tabTop, tabW, tabH, Color(0xFF080D14), cfCyan.copy(alpha = 0.55f), u * 2.0f)

    // Tablet Üst Çubuk
    drawBrandMark(ctx.spec, p, tabX + u * 4.0f, tabTop + u * 3.6f, u * 3.6f, u, cfCyan)
    gText("8\" MMI  •  ${ctx.subMode.uppercase()}", tabX + tabW * 0.5f, tabTop + u * 4.3f, u * 1.55f, cfCyan, Paint.Align.CENTER, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), tabX + tabW - u * 2.2f, tabTop + u * 4.3f, u * 1.75f, Color.White, Paint.Align.RIGHT, ctx.type.digits, true)

    // Tablet Devir & Büyük Hız
    drawLinearBar(tabX + u * 2.2f, tabTop + u * 7.2f, tabW - u * 4.4f, u * 3.4f, ctx.rpmRatio, cfCyan, Color(0xFF182230), ctx.redlineRatio, RL_RED, u * 0.7f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, tabX + tabW * 0.36f, tabTop + tabH * 0.36f, u * 14.5f, Color.White, Color(0xFF8C9AAE), ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, tabX + tabW * 0.78f, tabTop + tabH * 0.34f, u * 12.0f, cfCyan, Color.White, ctx.type.digits)

    // Tablet Orta Kart (Eğim + İvme + Menzil)
    val midCardTop = tabTop + tabH * 0.53f
    val midCardH = tabH * 0.25f
    drawPanel(tabX + u * 2.0f, midCardTop, tabW - u * 4.0f, midCardH, Color(0xFF0E1622), Color(0xFF223044), u * 1.2f)
    gText("LEAN  ${abs(t.leanAngleDeg).roundToInt()}°   •   ${t.estimatedWheelHorsepower.roundToInt()} HP", tabX + tabW * 0.5f, midCardTop + midCardH * 0.40f, u * 1.95f, cfCyan, Paint.Align.CENTER, ctx.type.digits, true)
    gText("RANGE ${ctx.formatRange()}   •   0-100 ${ctx.formatBest0to100()}", tabX + tabW * 0.5f, midCardTop + midCardH * 0.76f, u * 1.75f, Color.White, Paint.Align.CENTER, ctx.type.label, true)

    // Tablet Alt Dock Bar
    gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}  •  ${ctx.formatBattery()}  •  ${t.gpsSatelliteCount} SAT", tabX + tabW * 0.5f, tabTop + tabH - u * 2.4f, u * 1.6f, Color(0xFF8C9AAE), Paint.Align.CENTER, ctx.type.digits, true)

    if (ctx.wide) {
        // Tablet yanlarındaki araç kartları
        drawLeanArc(ctx.w * 0.14f, ctx.h * 0.48f, ctx.w * 0.085f, t.leanAngleDeg, cfCyan, p.track, u)
        gText("PEAK L ${ctx.peakLeanLeftDeg.roundToInt()}° / R ${ctx.peakLeanRightDeg.roundToInt()}°", ctx.w * 0.14f, ctx.h * 0.67f, u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.86f, ctx.h * 0.42f, u * 1.9f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        gText("PEAK ${ctx.formatPeakSpeed()}", ctx.w * 0.86f, ctx.h * 0.52f, u * 1.9f, cfCyan, Paint.Align.CENTER, ctx.type.digits, true)
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawCfmoto450SrCurvedTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val cfCyan = Color(0xFF00E5FF)
    val cfRed = Color(0xFFFF2A54)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.30f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.06f, ctx.h * 0.068f, u * 4.0f, u, cfCyan)
    gText("${ctx.spec.modelName.uppercase()}  //  ${ctx.subMode.uppercase()}", ctx.w * 0.135f, ctx.h * 0.075f, u * 1.65f, cfCyan, Paint.Align.LEFT, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, ctx.h * 0.075f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    // 5" Kavisli Kanat Devir Çubuğu
    val pts = List(32) { i ->
        val f = i / 31f
        val x = ctx.w * (0.06f + f * 0.88f)
        val y = ctx.h * (0.32f - sin(f * PI * 0.55).toFloat() * 0.16f)
        Offset(x, y)
    }
    drawPolylineBar(pts, 44, ctx.rpmRatio, u * 4.2f, cfCyan, p.track, ctx.redlineRatio, ctx.peakRatio, u)

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), ctx.h * 0.50f, u * 19.5f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.75f else 0.50f), ctx.h * (if (ctx.wide) 0.49f else 0.67f), u * 16.0f, cfRed, p.fg, ctx.type.digits)
    gText("0-60 ${ctx.formatBest0to60()}  •  0-100 ${ctx.formatBest0to100()}  •  PEAK ${ctx.formatPeakSpeed()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.85f, RL_GREEN, Paint.Align.CENTER, ctx.type.digits, true)

    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawCfmoto450MtRallyTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val cfCyan = Color(0xFF00E5FF)
    val sand = Color(0xFFFFB300)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawBrandMark(ctx.spec, p, ctx.w * 0.06f, ctx.h * 0.065f, u * 4.0f, u, cfCyan)
    gText("450MT // RALLY EXPLORE", ctx.w * 0.135f, ctx.h * 0.072f, u * 1.7f, sand, Paint.Align.LEFT, ctx.type.label, true)
    drawCompassStrip(ctx, ctx.h * 0.10f, u * 3.8f)

    drawLinearBar(ctx.w * 0.06f, ctx.h * 0.18f, ctx.w * 0.88f, u * 3.6f, ctx.rpmRatio, cfCyan, p.track, ctx.redlineRatio, RL_RED, u * 0.7f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.38f else 0.50f), ctx.h * 0.45f, u * 19.0f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.72f else 0.50f), ctx.h * (if (ctx.wide) 0.45f else 0.62f), u * 15.0f, sand, p.fg, ctx.type.digits)
    gText("ABS OFF-RD  •  TRIP ${ctx.formatTrip()}  •  RANGE ${ctx.formatRange()}", ctx.w * 0.50f, ctx.h * 0.76f, u * 1.85f, p.fg, Paint.Align.CENTER, ctx.type.label, true)

    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

// =========================================================================
// 5. HERO MOTOCORP — Inverted Blue-Black Negative Sport LCD
// =========================================================================

internal fun DrawScope.drawHeroInvertedSportLcd(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val heroRed = Color(0xFFE31837)
    val iceCyan = Color(0xFF40C4FF)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val left = ctx.w * 0.06f
    val top = ctx.h * 0.08f
    val pw = ctx.w * 0.88f
    val ph = ctx.h - railH - top - u * 2.5f
    drawPanel(left, top, pw, ph, Color(0xFF060E1B), iceCyan.copy(alpha = 0.45f), u * 1.6f)

    drawBrandMark(ctx.spec, p, left + u * 4.0f, top + u * 3.6f, u * 3.8f, u, heroRed)
    gText(ctx.spec.modelName.uppercase(), left + u * 8.5f, top + u * 4.3f, u * 1.65f, iceCyan, Paint.Align.LEFT, "monospace", true)
    gText(clusterClockText(ctx.clock24h), left + pw - u * 2.2f, top + u * 4.3f, u * 1.9f, Color.White, Paint.Align.RIGHT, "monospace", true)

    // Üst kademeli devir barı
    drawLinearBar(left + u * 2.4f, top + u * 7.2f, pw - u * 4.8f, u * 3.6f, ctx.rpmRatio, iceCyan, Color(0xFF132238), ctx.redlineRatio, heroRed, u * 0.5f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), top + ph * 0.55f, u * 18.0f, Color.White, Color(0xFF7D95B5), "monospace")
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.74f else 0.50f), top + ph * (if (ctx.wide) 0.54f else 0.74f), u * 14.0f, heroRed, Color.White, "monospace")
    gText("RANGE ${ctx.formatRange()}  •  AVG ${ctx.formatAvgFuel()}  •  ODO ${ctx.formatOdometer()}", ctx.w * 0.50f, top + ph - u * 2.5f, u * 1.55f, iceCyan, Paint.Align.CENTER, "monospace", true)
    drawStatusRail(ctx, RailStyle.LCD)
}

// =========================================================================
// 6. GLOBAL BRANDS: MOTO GUZZI, KOVE RALLY, QJ/VOGE/ZONTES/BENELLI, HERITAGE
// =========================================================================

internal fun DrawScope.drawMotoGuzziEagleTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val guzziGold = Color(0xFFD4AF37)
    val guzziRed = Color(0xFFC8102E)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.28f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.07f, ctx.h * 0.068f, u * 4.2f, u, guzziGold)
    gText("MANDELLO DEL LARIO // ${ctx.subMode.uppercase()}", ctx.w * 0.145f, ctx.h * 0.076f, u * 1.65f, guzziGold, Paint.Align.LEFT, "serif", true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.94f, ctx.h * 0.076f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    // Kartal kanadı simetrik yatay devir barı
    val midX = ctx.w * 0.50f
    val halfW = ctx.w * 0.42f
    val barY = ctx.h * 0.16f
    drawLinearBar(midX - halfW, barY, halfW - u * 0.8f, u * 3.2f, ctx.rpmRatio, guzziGold, p.track, ctx.redlineRatio, guzziRed, u * 0.5f)
    drawLinearBar(midX + u * 0.8f, barY, halfW - u * 0.8f, u * 3.2f, ctx.rpmRatio, guzziGold, p.track, ctx.redlineRatio, guzziRed, u * 0.5f)

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), ctx.h * 0.48f, u * 19.5f, p.fg, p.muted, ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.74f else 0.50f), ctx.h * (if (ctx.wide) 0.48f else 0.65f), u * 15.0f, guzziRed, p.fg, ctx.type.digits)
    gText("MGCT ${if (t.tractionControlInterventionActive) "ACT" else "ON"}  •  LEAN ${abs(t.leanAngleDeg).roundToInt()}°  •  RANGE ${ctx.formatRange()}", ctx.w * 0.50f, ctx.h * 0.77f, u * 1.8f, p.fg, Paint.Align.CENTER, "serif", true)

    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawKoveRallyTowerTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val koveCyan = Color(0xFF00E5FF)
    val koveAmber = Color(0xFFFFAB00)
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    // Paris-Dakar Dikey Roadbook Kulesi
    val towerW = if (ctx.wide) ctx.w * 0.48f else ctx.w * 0.90f
    val towerX = ctx.w * 0.50f - towerW * 0.5f
    val towerTop = ctx.h * 0.055f
    val towerH = ctx.h - railH - towerTop - u * 2.5f
    drawPanel(towerX, towerTop, towerW, towerH, Color(0xFF090C11), koveCyan.copy(alpha = 0.5f), u * 1.6f)

    drawBrandMark(ctx.spec, p, towerX + u * 4.0f, towerTop + u * 3.5f, u * 3.8f, u, koveCyan)
    gText("DAKAR RALLY TOWER", towerX + towerW * 0.5f, towerTop + u * 4.2f, u * 1.55f, koveAmber, Paint.Align.CENTER, ctx.type.label, true)
    drawCompassStrip(ctx, towerTop + u * 6.5f, u * 3.4f)

    drawLinearBar(towerX + u * 2.0f, towerTop + u * 11.4f, towerW - u * 4.0f, u * 2.8f, ctx.rpmRatio, koveCyan, Color(0xFF18222F), ctx.redlineRatio, RL_RED, u * 0.5f)
    drawSpeed(ctx.speedValue, ctx.speedUnit, towerX + towerW * 0.36f, towerTop + towerH * 0.48f, u * 15.5f, Color.White, Color(0xFF90A0B2), ctx.type.digits)
    drawGearBadge(ctx.gearText, ctx.isNeutral, towerX + towerW * 0.78f, towerTop + towerH * 0.46f, u * 12.5f, koveAmber, Color.White, ctx.type.digits)

    gText("T1  ${ctx.formatTrip()}   •   T2  ${ctx.formatTripB()}", towerX + towerW * 0.5f, towerTop + towerH * 0.74f, u * 2.1f, Color.White, Paint.Align.CENTER, ctx.type.digits, true)
    gText("SAT ${t.gpsSatelliteCount}   •   RNG ${ctx.formatRange()}   •   ${ctx.formatBattery()}", towerX + towerW * 0.5f, towerTop + towerH * 0.88f, u * 1.6f, koveCyan, Paint.Align.CENTER, ctx.type.label, true)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawGlobalSportTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val u = ctx.u
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    val brandAccent = when (ctx.spec.brand) {
        "Benelli" -> Color(0xFF39D353)
        "Voge" -> Color(0xFF00C2FF)
        "Zontes" -> Color(0xFFB2FF59)
        "QJMotor" -> Color(0xFFFF3B30)
        else -> p.accent
    }
    val variant = skinVariant(ctx.spec.skinId, 3)

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.30f, u)
    drawBrandMark(ctx.spec, p, ctx.w * 0.06f, ctx.h * 0.068f, u * 4.0f, u, brandAccent)
    gText("${ctx.spec.modelName.uppercase()}  //  ${ctx.subMode.uppercase()}", ctx.w * 0.135f, ctx.h * 0.075f, u * 1.6f, brandAccent, Paint.Align.LEFT, ctx.type.label, true)
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.945f, ctx.h * 0.075f, u * 2.0f, p.fg, Paint.Align.RIGHT, ctx.type.digits, true)

    when (variant) {
        0 -> {
            // Dairesel spor halka + sağda canlı eğim
            val cx = ctx.w * (if (ctx.wide) 0.42f else 0.50f)
            val cy = ctx.h * (if (ctx.wide) 0.50f else 0.42f)
            val r = min(ctx.w * (if (ctx.wide) 0.25f else 0.36f), (ctx.h - railH) * 0.39f)
            drawSegmentedArc(cx, cy, r, 138f, 264f, 42, ctx.rpmRatio, u * 3.8f, brandAccent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
            drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + u * 2.5f, u * 16.0f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, cx, cy + r * 0.58f, u * 11.5f, brandAccent, p.fg, ctx.type.digits)
            if (ctx.wide) {
                gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.78f, ctx.h * 0.42f, u * 3.2f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
                gText("RANGE ${ctx.formatRange()}", ctx.w * 0.78f, ctx.h * 0.53f, u * 2.1f, brandAccent, Paint.Align.LEFT, ctx.type.digits, true)
                gText(ctx.formatBattery(), ctx.w * 0.78f, ctx.h * 0.62f, u * 1.9f, p.muted, Paint.Align.LEFT, ctx.type.digits, true)
            }
        }
        1 -> {
            // Çapraz kanat poliline bar
            val pts = List(30) { i ->
                val f = i / 29f
                Offset(ctx.w * (0.06f + f * 0.88f), ctx.h * (0.30f - sin(f * PI * 0.5).toFloat() * 0.14f))
            }
            drawPolylineBar(pts, 44, ctx.rpmRatio, u * 4.2f, brandAccent, p.track, ctx.redlineRatio, ctx.peakRatio, u)
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), ctx.h * 0.50f, u * 19.5f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.74f else 0.50f), ctx.h * (if (ctx.wide) 0.50f else 0.66f), u * 15.5f, brandAccent, p.fg, ctx.type.digits)
            gText("RANGE ${ctx.formatRange()}  •  ${ctx.formatAvgFuel()}  •  ODO ${ctx.formatOdometer()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.8f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        }
        else -> {
            // Temiz geniş yatay bar
            drawLinearBar(ctx.w * 0.06f, ctx.h * 0.16f, ctx.w * 0.88f, u * 4.2f, ctx.rpmRatio, brandAccent, p.track, ctx.redlineRatio, RL_RED, u * 0.7f)
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), ctx.h * 0.49f, u * 19.5f, p.fg, p.muted, ctx.type.digits)
            drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.74f else 0.50f), ctx.h * (if (ctx.wide) 0.49f else 0.66f), u * 15.5f, brandAccent, p.fg, ctx.type.digits)
            gText("0-100 ${ctx.formatBest0to100()}  •  PEAK ${ctx.formatPeakSpeed()}  •  ${ctx.formatBattery()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.85f, brandAccent, Paint.Align.CENTER, ctx.type.digits, true)
        }
    }

    drawSubModeChip(ctx, ctx.w * 0.06f, ctx.h - railH - u * 3.0f)
    drawStatusRail(ctx, RailStyle.TFT)
}

internal fun DrawScope.drawHeritageBritishItalianDial(ctx: GaugeContext) {
    val p = ctx.palette
    val u = ctx.u
    val isBimota = ctx.spec.brand == "Bimota"
    val railH = if (ctx.showStatusRail) ctx.h * 0.082f else 0f

    if (isBimota) {
        val bimRed = Color(0xFFE10600)
        drawShiftLeds(ctx.spec, ctx.telemetry, p, ctx.w * 0.5f, u * 3.8f, ctx.w * 0.30f, u)
        drawBrandMark(ctx.spec, p, ctx.w * 0.065f, ctx.h * 0.07f, u * 4.0f, u, bimRed)
        gText("${ctx.spec.modelName.uppercase()}  //  RIMINI", ctx.w * 0.14f, ctx.h * 0.078f, u * 1.65f, Color.White, Paint.Align.LEFT, ctx.type.label, true)
        drawLinearBar(ctx.w * 0.06f, ctx.h * 0.16f, ctx.w * 0.88f, u * 4.2f, ctx.rpmRatio, bimRed, p.track, ctx.redlineRatio, Color.White, u * 0.7f)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * (if (ctx.wide) 0.42f else 0.50f), ctx.h * 0.50f, u * 20.0f, Color.White, p.muted, ctx.type.digits)
        drawGearBadge(ctx.gearText, ctx.isNeutral, ctx.w * (if (ctx.wide) 0.75f else 0.50f), ctx.h * (if (ctx.wide) 0.50f else 0.67f), u * 16.0f, bimRed, Color.White, ctx.type.digits)
        gText("0-100 ${ctx.formatBest0to100()}  •  PEAK ${ctx.formatPeakSpeed()}", ctx.w * 0.50f, ctx.h * 0.78f, u * 1.85f, bimRed, Paint.Align.CENTER, ctx.type.digits, true)
        drawStatusRail(ctx, RailStyle.TFT)
    } else {
        // Norton / Jawa / Brixton Billet Krom Analog Kadran
        val accentGold = if (ctx.spec.brand == "Norton") Color(0xFFD4AF37) else Color(0xFFE63946)
        val cx = ctx.w * 0.50f
        val cy = ctx.h * (if (ctx.wide) 0.48f else 0.43f)
        val r = min(ctx.w * 0.36f, (ctx.h - railH) * 0.42f)
        val maxScale = max(140, ctx.speedScaleMax)
        val spdRatio = (ctx.speedValue / maxScale.toFloat()).coerceIn(0f, 1f)

        drawNeedleDial(cx, cy, r, 135f, 270f, spdRatio, p, u, accentGold, 0.94f, 7, 3, true, cream = ctx.spec.brand == "Norton", chrome = true, labelFamily = "serif") { (it * maxScale / 7).toString() }
        drawBrandMark(ctx.spec, p, cx, cy - r * 0.42f, u * 3.8f, u, accentGold)
        drawPanel(cx - r * 0.40f, cy + r * 0.25f, r * 0.80f, r * 0.28f, Color(0xFF14161A), accentGold.copy(alpha = 0.55f), u * 0.7f)
        gText("${ctx.gearText}  •  ${ctx.mappedRpm.roundToInt()} RPM  •  ${ctx.formatOdometer()}", cx, cy + r * 0.43f, u * 1.55f, Color.White, Paint.Align.CENTER, "monospace", true)
        drawStatusRail(ctx, RailStyle.CLASSIC)
    }
}
