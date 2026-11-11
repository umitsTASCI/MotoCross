package com.oemcluster.pro.renderer

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.oemcluster.pro.catalog.DeclarativeGaugeSpec
import com.oemcluster.pro.catalog.GaugeGeometryType
import com.oemcluster.pro.catalog.ShiftLedPattern
import com.oemcluster.pro.domain.IgnitionBootPhase
import com.oemcluster.pro.domain.RpmScalingMode
import com.oemcluster.pro.domain.ShiftLightState
import com.oemcluster.pro.domain.UnifiedTelemetryState
import com.oemcluster.pro.domain.WheelieGaugeDisplayMode
import com.oemcluster.pro.i18n.AppLanguage
import com.oemcluster.pro.i18n.LocalizationManager
import com.oemcluster.pro.i18n.StringKey
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * MotoCross — 120 FPS OEM Gösterge Paneli, Renk Tersleme (Full Color Inversion),
 * Eğimölçer (Lean Angle) ve Estetik Teker Derece Ölçer / Teker Metre (Wheelie Analyzer) Çizim Motoru.
 */
@Composable
fun OemClusterCanvasRenderer(
    spec: DeclarativeGaugeSpec,
    subModeIndex: Int,
    telemetry: UnifiedTelemetryState,
    rpmScalingMode: RpmScalingMode,
    wheelieDisplayMode: WheelieGaugeDisplayMode,
    useMph: Boolean,
    chromaKeyGreenScreen: Boolean,
    oledPixelShiftOffset: Offset,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Gündüz/Gece 500ms Yumuşak Renk İnterpolasyonu ve Evre 2 (8 Hz) Tam Renk Terslemesi
        val dayBg = Color(spec.dayBgColorHex)
        val nightBg = Color(spec.nightBgColorHex)
        val dayFg = Color(spec.dayPrimaryColorHex)
        val nightFg = Color(spec.nightPrimaryColorHex)
        val accentColor = Color(spec.accentBrandColorHex)
        val stage1Color = Color(spec.stage1ShiftColorHex)
        val invertBg = Color(spec.stage2InvertBgColorHex)
        val invertFg = Color(spec.stage2InvertFgColorHex)

        val baseBg = lerp(dayBg, nightBg, telemetry.nightModeTransitionFactor)
        val baseFg = lerp(dayFg, nightFg, telemetry.nightModeTransitionFactor)

        val isStage2OrOverRevInverted =
            telemetry.fullColorInversionActive &&
                (telemetry.shiftLightState == ShiftLightState.STAGE_2_SHIFT_NOW ||
                    telemetry.shiftLightState == ShiftLightState.OVER_REV_LIMITER)

        val activeBgColor = when {
            chromaKeyGreenScreen -> Color(0xFF00FF00)
            isStage2OrOverRevInverted -> invertBg
            else -> baseBg
        }

        val activeFgColor = if (isStage2OrOverRevInverted) invertFg else baseFg

        val activeBarColor = when (telemetry.shiftLightState) {
            ShiftLightState.OVER_REV_LIMITER ->
                if (telemetry.shiftFlashPhaseOn) Color(0xFFFF1744) else invertFg
            ShiftLightState.STAGE_2_SHIFT_NOW ->
                if (isStage2OrOverRevInverted) invertFg else stage1Color
            ShiftLightState.STAGE_1_APPROACHING -> stage1Color
            ShiftLightState.NORMAL -> accentColor
        }

        // Arka planı çiz
        drawRect(color = activeBgColor)

        // Kontak Açılış Logosu Evresi (Phase 1)
        if (telemetry.ignitionBootPhase == IgnitionBootPhase.OEM_SPLASH_LOGO) {
            drawOemBootSplashScreen(spec, accentColor, activeFgColor)
            return@Canvas
        }

        // Üst Çerçeve Fiziksel Shift-Light LED Dizisi
        drawUpperShiftLightLeds(
            spec = spec,
            telemetry = telemetry,
            accentColor = accentColor,
            stage1Color = stage1Color
        )

        // Aktif Motorun Devrini Seçilen Göstergenin Kadranına Haritala (Altın Kural + Bölüm 4.2)
        val mappedGaugeRpm = telemetry.getMappedRpmForGauge(
            gaugeRedlineRpm = spec.gaugeRedlineRpm,
            gaugeMaxRpm = spec.gaugeMaxRpm,
            scalingMode = rpmScalingMode
        )
        val gaugeFillRatio = (mappedGaugeRpm / max(1000.0, spec.gaugeMaxRpm)).toFloat().coerceIn(0f, 1f)
        val peakFillRatio = ((telemetry.peakHoldRpm / max(1000.0, telemetry.bikeRedlineRpm)) *
            (spec.gaugeRedlineRpm / max(1000.0, spec.gaugeMaxRpm))).toFloat().coerceIn(0f, 1f)
        val redlineRatio = (spec.gaugeRedlineRpm / max(1000.0, spec.gaugeMaxRpm)).toFloat().coerceIn(0.65f, 0.95f)

        // Gösterge Geometrisini Çiz (105 Modelin 12 Geometri Ailesi)
        when (spec.geometryType) {
            GaugeGeometryType.BOOMERANG_BAR_TFT -> drawBoomerangTftGauge(
                spec, telemetry, gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.CURVED_SWEEP_BAR_TFT,
            GaugeGeometryType.F1_DIGITAL_ARC_BAR -> drawCurvedSweepTftGauge(
                spec, telemetry, gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT,
            GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT,
            GaugeGeometryType.PANORAMIC_SPLIT_TFT -> drawSharpHorizontalEvoGauge(
                spec, telemetry, gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.SYMMETRIC_PINCER_BAR_TFT -> drawSymmetricPincerGauge(
                spec, telemetry, gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.CIRCULAR_RING_TFT,
            GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD -> drawCircularRingTftGauge(
                spec, telemetry, gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD -> drawCentralAnalogHybridGauge(
                spec, telemetry, gaugeFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.FIVE_DIAL_HAYABUSA_HYBRID,
            GaugeGeometryType.DUAL_CLASSIC_ANALOG -> drawHayabusaAndDualAnalogGauge(
                spec, telemetry, gaugeFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
        }

        // Merkezi Hız, Vites (N / 1..6), Aktif Motor Bilgisi ve Alt Mod Çizimi
        drawCentralSpeedAndGearBlock(
            spec = spec,
            subModeIndex = subModeIndex,
            telemetry = telemetry,
            useMph = useMph,
            fgColor = activeFgColor,
            barColor = activeBarColor,
            accentColor = accentColor,
            isInverted = isStage2OrOverRevInverted,
            shiftOffset = oledPixelShiftOffset,
            language = language
        )

        // Sol Kanat: Estetik Eğimölçer (Lean Angle °L / °R + G-Kuvveti)
        drawAestheticLeanAnglePod(
            telemetry = telemetry,
            fgColor = activeFgColor,
            accentColor = accentColor,
            language = language,
            shiftOffset = oledPixelShiftOffset
        )

        // Sağ Kanat: Estetik Teker Derece Ölçer & Teker Metre (Wheelie Angle °, Süre sn, Mesafe m)
        val shouldShowWheeliePod = when (wheelieDisplayMode) {
            WheelieGaugeDisplayMode.ALWAYS_VISIBLE -> true
            WheelieGaugeDisplayMode.SMART_AUTO_POPUP -> telemetry.isWheelieSummaryVisible
            WheelieGaugeDisplayMode.HIDDEN_OEM_PURE -> false
        }
        if (shouldShowWheeliePod) {
            drawAestheticWheelieMeterPod(
                telemetry = telemetry,
                fgColor = activeFgColor,
                accentColor = accentColor,
                language = language,
                shiftOffset = oledPixelShiftOffset
            )
        }

        // Alt Durum Çubuğu: Aktif Motor Profili ("Single Source of Truth"), Hararet, Yakıt, TC, Debriyaj
        drawBottomTelemetryStatusStrip(
            spec = spec,
            telemetry = telemetry,
            fgColor = activeFgColor,
            accentColor = accentColor,
            language = language
        )
    }
}

private fun DrawScope.drawOemBootSplashScreen(
    spec: DeclarativeGaugeSpec,
    accentColor: Color,
    fgColor: Color
) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    drawRoundRect(
        color = accentColor.copy(alpha = 0.18f),
        topLeft = Offset(cx - size.width * 0.32f, cy - 70f),
        size = Size(size.width * 0.64f, 140f),
        cornerRadius = CornerRadius(20f, 20f),
        style = Stroke(width = 4f)
    )
    drawNativeText(
        text = spec.brand.uppercase(Locale.ROOT),
        x = cx,
        y = cy - 18f,
        textSizePx = 34f,
        color = accentColor,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = spec.bootSplashLogoText,
        x = cx,
        y = cy + 32f,
        textSizePx = 42f,
        color = fgColor,
        align = Paint.Align.CENTER,
        bold = true
    )
}

private fun DrawScope.drawUpperShiftLightLeds(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    accentColor: Color,
    stage1Color: Color
) {
    if (spec.shiftLedPattern == ShiftLedPattern.NONE_INTERNAL_ONLY) return

    val w = size.width
    val topY = 26f
    val ratio = telemetry.normalizedRpmRatio.toFloat().coerceIn(0f, 1.1f)

    when (spec.shiftLedPattern) {
        ShiftLedPattern.NONE_INTERNAL_ONLY -> Unit
        ShiftLedPattern.SINGLE_UPPER_LED,
        ShiftLedPattern.DUAL_SIDE_LIGHTNING_LEDS -> {
            val isOn = telemetry.shiftLightState != ShiftLightState.NORMAL && telemetry.shiftFlashPhaseOn
            val ledColor = if (isOn) Color(0xFFFFFFFF) else Color(0xFF262A32)
            drawCircle(
                color = ledColor,
                radius = 14f,
                center = Offset(w * 0.5f, topY)
            )
            if (isOn) {
                drawCircle(
                    color = stage1Color.copy(alpha = 0.45f),
                    radius = 24f,
                    center = Offset(w * 0.5f, topY)
                )
            }
        }
        ShiftLedPattern.CONVERGING_5_LEDS -> {
            // Yamaha R1 / Fireblade: İki uçtan merkeze doğru birleşen 5 LED
            val ledCount = 5
            val spacing = 54f
            val startX = w * 0.5f - (spacing * 2f)
            for (i in 0 until ledCount) {
                val distFromCenter = abs(i - 2) // 2, 1, 0, 1, 2
                val threshold = when (distFromCenter) {
                    2 -> 0.72f
                    1 -> 0.84f
                    else -> 0.94f
                }
                val lit = (ratio >= threshold) &&
                    (telemetry.shiftLightState != ShiftLightState.OVER_REV_LIMITER || telemetry.shiftFlashPhaseOn)
                val c = when {
                    !lit -> Color(0xFF22262E)
                    distFromCenter == 0 -> Color(0xFFFF1744)
                    distFromCenter == 1 -> Color(0xFFFFAB00)
                    else -> Color(0xFF00E676)
                }
                drawRoundRect(
                    color = c,
                    topLeft = Offset(startX + i * spacing - 18f, topY - 8f),
                    size = Size(36f, 16f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
        ShiftLedPattern.TRIUMPH_7_BLUE_LEDS,
        ShiftLedPattern.KAWASAKI_LED_ARCH_BAR,
        ShiftLedPattern.MOTOGP_MARSHAL_BAR -> {
            val count = if (spec.shiftLedPattern == ShiftLedPattern.TRIUMPH_7_BLUE_LEDS) 7 else 9
            val spacing = 42f
            val startX = w * 0.5f - ((count - 1) * spacing * 0.5f)
            for (i in 0 until count) {
                val thresh = 0.65f + (i.toFloat() / count.toFloat()) * 0.33f
                val lit = (ratio >= thresh) &&
                    (telemetry.shiftLightState == ShiftLightState.NORMAL ||
                        telemetry.shiftLightState == ShiftLightState.STAGE_1_APPROACHING ||
                        telemetry.shiftFlashPhaseOn)
                val activeLedColor = when {
                    spec.shiftLedPattern == ShiftLedPattern.TRIUMPH_7_BLUE_LEDS -> Color(0xFF2979FF)
                    i >= count - 2 -> Color(0xFFFF1744)
                    i >= count - 5 -> stage1Color
                    else -> accentColor
                }
                drawRoundRect(
                    color = if (lit) activeLedColor else Color(0xFF22262E),
                    topLeft = Offset(startX + i * spacing - 15f, topY - 7f),
                    size = Size(30f, 14f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }
        }
    }
}

/**
 * 1. KTM 390/790/890/1290 Duke, MT-09 Gen4, BMW Core 1: Sol kenardan üst yatay eksene kıvrılan Bumerang Bar
 */
private fun DrawScope.drawBoomerangTftGauge(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    fillRatio: Float,
    peakRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height
    val totalSegments = 44
    val activeSegments = (fillRatio * totalSegments).roundToInt()
    val peakSegment = (peakRatio * totalSegments).roundToInt()
    val redlineSegment = (redlineRatio * totalSegments).roundToInt()

    val startX = w * 0.08f + shiftOffset.x
    val startY = h * 0.72f + shiftOffset.y
    val kneeX = w * 0.16f + shiftOffset.x
    val kneeY = h * 0.16f + shiftOffset.y
    val endX = w * 0.90f + shiftOffset.x
    val endY = h * 0.16f + shiftOffset.y

    for (i in 0 until totalSegments) {
        val t = i.toFloat() / (totalSegments - 1).toFloat()
        val x: Float
        val y: Float
        val barHeight: Float
        if (t < 0.32f) {
            val localT = t / 0.32f
            x = startX + (kneeX - startX) * localT
            y = startY + (kneeY - startY) * localT
            barHeight = 28f + 12f * localT
        } else {
            val localT = (t - 0.32f) / 0.68f
            x = kneeX + (endX - kneeX) * localT
            y = kneeY + (endY - kneeY) * localT
            barHeight = 40f
        }

        val isLit = i < activeSegments
        val isPeak = spec.hasPeakHoldTrail && (i == peakSegment) && (peakSegment > activeSegments)
        val segColor = when {
            isLit && i >= redlineSegment -> Color(0xFFFF1744)
            isLit -> barColor
            isPeak -> accentColor
            i >= redlineSegment -> Color(0xFFFF1744).copy(alpha = 0.22f)
            else -> fgColor.copy(alpha = 0.14f)
        }

        val segWidth = ((w * 0.78f) / totalSegments) * 0.74f
        drawRoundRect(
            color = segColor,
            topLeft = Offset(x, y),
            size = Size(segWidth, barHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Her 2000 RPM'de bir ölçek rakamı
        if (i % 8 == 0) {
            val rpmNum = ((t * spec.gaugeMaxRpm) / 1000.0).roundToInt()
            drawNativeText(
                text = rpmNum.toString(),
                x = x + segWidth * 0.5f,
                y = y + barHeight + 22f,
                textSizePx = 19f,
                color = if (i >= redlineSegment) Color(0xFFFF1744) else fgColor.copy(alpha = 0.75f),
                align = Paint.Align.CENTER,
                bold = true
            )
        }
    }
}

/**
 * 2. Yamaha R1 / R1M, Kawasaki ZX-10R / Z900, Honda S2000 F1, CFMoto 450SR: Kavisli Süpürme Barı
 */
private fun DrawScope.drawCurvedSweepTftGauge(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    fillRatio: Float,
    peakRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height
    val totalSegments = 52
    val activeSegments = (fillRatio * totalSegments).roundToInt()
    val peakSegment = (peakRatio * totalSegments).roundToInt()
    val redlineSegment = (redlineRatio * totalSegments).roundToInt()

    for (i in 0 until totalSegments) {
        val t = i.toFloat() / (totalSegments - 1).toFloat()
        val x = w * (0.08f + 0.84f * t) + shiftOffset.x
        val curveDrop = (1f - sin(t * Math.PI * 0.85).toFloat()) * (h * 0.18f)
        val y = h * 0.14f + curveDrop + shiftOffset.y

        val isLit = i < activeSegments
        val isPeak = spec.hasPeakHoldTrail && (i == peakSegment) && (peakSegment > activeSegments)
        val segColor = when {
            isLit && i >= redlineSegment -> Color(0xFFFF1744)
            isLit -> barColor
            isPeak -> Color.White
            i >= redlineSegment -> Color(0xFFFF1744).copy(alpha = 0.22f)
            else -> fgColor.copy(alpha = 0.14f)
        }

        drawRoundRect(
            color = segColor,
            topLeft = Offset(x, y),
            size = Size((w * 0.84f / totalSegments) * 0.72f, 38f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

/**
 * 3. Ducati Panigale V4 Track Evo, KTM 1390 Super Duke R, BMW HP4 Race: Keskin Yatay Yarış Barı
 */
private fun DrawScope.drawSharpHorizontalEvoGauge(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    fillRatio: Float,
    peakRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height
    val barLeft = w * 0.07f + shiftOffset.x
    val barWidth = w * 0.86f
    val barTop = h * 0.13f + shiftOffset.y
    val barHeight = 42f

    // Arka kanal
    drawRoundRect(
        color = fgColor.copy(alpha = 0.12f),
        topLeft = Offset(barLeft, barTop),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Redline bölgesi arka planı
    drawRoundRect(
        color = Color(0xFFFF1744).copy(alpha = 0.25f),
        topLeft = Offset(barLeft + barWidth * redlineRatio, barTop),
        size = Size(barWidth * (1f - redlineRatio), barHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Aktif doluluk
    drawRoundRect(
        color = barColor,
        topLeft = Offset(barLeft, barTop),
        size = Size(barWidth * fillRatio, barHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Peak-Hold çizgisi (1.8 sn asılı kalır)
    if (spec.hasPeakHoldTrail && peakRatio > fillRatio + 0.01f) {
        val px = barLeft + barWidth * peakRatio
        drawLine(
            color = accentColor,
            start = Offset(px, barTop - 4f),
            end = Offset(px, barTop + barHeight + 4f),
            strokeWidth = 6f
        )
    }
}

/**
 * 4. Triumph Street Triple 765 RS, KTM 1290 Super Adventure: İki Yandan Merkeze Kıskaç (Pincer) Bar
 */
private fun DrawScope.drawSymmetricPincerGauge(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    fillRatio: Float,
    peakRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height
    val halfWidth = w * 0.42f
    val topY = h * 0.14f + shiftOffset.y
    val leftStart = w * 0.06f + shiftOffset.x
    val rightEnd = w * 0.94f + shiftOffset.x

    // Sol ve sağ arka bar
    drawRoundRect(
        color = fgColor.copy(alpha = 0.13f),
        topLeft = Offset(leftStart, topY),
        size = Size(halfWidth, 38f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = fgColor.copy(alpha = 0.13f),
        topLeft = Offset(rightEnd - halfWidth, topY),
        size = Size(halfWidth, 38f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Sol ve sağdan merkeze doğru uzayan aktif kıskaç barı
    val activeLen = halfWidth * fillRatio
    drawRoundRect(
        color = barColor,
        topLeft = Offset(leftStart, topY),
        size = Size(activeLen, 38f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = barColor,
        topLeft = Offset(rightEnd - activeLen, topY),
        size = Size(activeLen, 38f),
        cornerRadius = CornerRadius(8f, 8f)
    )
}

/**
 * 5. Honda CBR1000RR-R Circuit, Ducati Panigale V2, Suzuki GSX-8S, Husqvarna Bauhaus: Dairesel Halka TFT
 */
private fun DrawScope.drawCircularRingTftGauge(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    fillRatio: Float,
    peakRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val cx = size.width * 0.5f + shiftOffset.x
    val cy = size.height * 0.48f + shiftOffset.y
    val radius = min(size.width, size.height) * 0.35f
    val rectTopLeft = Offset(cx - radius, cy - radius)
    val arcSize = Size(radius * 2f, radius * 2f)

    // Arka halka (240 derece yay)
    drawArc(
        color = fgColor.copy(alpha = 0.14f),
        startAngle = 150f,
        sweepAngle = 240f,
        useCenter = false,
        topLeft = rectTopLeft,
        size = arcSize,
        style = Stroke(width = 28f, cap = StrokeCap.Round)
    )
    // Redline yay dilimi
    drawArc(
        color = Color(0xFFFF1744).copy(alpha = 0.30f),
        startAngle = 150f + 240f * redlineRatio,
        sweepAngle = 240f * (1f - redlineRatio),
        useCenter = false,
        topLeft = rectTopLeft,
        size = arcSize,
        style = Stroke(width = 28f, cap = StrokeCap.Butt)
    )
    // Aktif devir yay dilimi
    drawArc(
        color = barColor,
        startAngle = 150f,
        sweepAngle = 240f * fillRatio,
        useCenter = false,
        topLeft = rectTopLeft,
        size = arcSize,
        style = Stroke(width = 28f, cap = StrokeCap.Round)
    )
}

/**
 * 6. Yamaha R6, Honda CBR600RR PC40, BMW S1000RR K46, Kawasaki H2/ZX-6R, Triumph Daytona 675R:
 *    Merkezi Analog Kadran + Fiziksel İbre
 */
private fun DrawScope.drawCentralAnalogHybridGauge(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    fillRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val cx = size.width * 0.5f + shiftOffset.x
    val cy = size.height * 0.48f + shiftOffset.y
    val radius = min(size.width, size.height) * 0.36f

    drawCircle(
        color = fgColor.copy(alpha = 0.20f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 6f)
    )

    // Kadran çizgileri (Ticks)
    val tickCount = (spec.gaugeMaxRpm / 1000.0).roundToInt().coerceIn(8, 20)
    for (i in 0..tickCount) {
        val t = i.toFloat() / tickCount.toFloat()
        val angleDeg = 140f + 260f * t
        val rad = Math.toRadians(angleDeg.toDouble())
        val innerR = radius * 0.82f
        val outerR = radius * 0.96f
        val tickColor = if (t >= redlineRatio) Color(0xFFFF1744) else fgColor
        drawLine(
            color = tickColor,
            start = Offset(cx + (innerR * cos(rad)).toFloat(), cy + (innerR * sin(rad)).toFloat()),
            end = Offset(cx + (outerR * cos(rad)).toFloat(), cy + (outerR * sin(rad)).toFloat()),
            strokeWidth = 4.5f
        )
        val numR = radius * 0.69f
        drawNativeText(
            text = i.toString(),
            x = cx + (numR * cos(rad)).toFloat(),
            y = cy + (numR * sin(rad)).toFloat() + 8f,
            textSizePx = 22f,
            color = tickColor,
            align = Paint.Align.CENTER,
            bold = true
        )
    }

    // Analog İbre (Yay-Damper Fiziğiyle Hareket Eden İbre)
    val needleAngleDeg = 140f + 260f * fillRatio
    val needleRad = Math.toRadians(needleAngleDeg.toDouble())
    val needleEnd = Offset(
        cx + (radius * 0.90f * cos(needleRad)).toFloat(),
        cy + (radius * 0.90f * sin(needleRad)).toFloat()
    )
    drawLine(
        color = barColor,
        start = Offset(cx, cy),
        end = needleEnd,
        strokeWidth = 9f,
        cap = StrokeCap.Round
    )
    drawCircle(color = accentColor, radius = 16f, center = Offset(cx, cy))
}

/**
 * 7. Suzuki Hayabusa (Gen 1/2/3 5-Kadranlı Hibrit) & Çift Klasik Analog Kadranlar (R nineT, Bonneville, GT 650)
 */
private fun DrawScope.drawHayabusaAndDualAnalogGauge(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    fillRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height
    val dialRadius = min(w, h) * 0.25f
    val leftCx = w * 0.26f + shiftOffset.x
    val rightCx = w * 0.74f + shiftOffset.x
    val cy = h * 0.48f + shiftOffset.y

    // Sol Kadran: Analog Devir (RPM)
    drawCircle(color = fgColor.copy(alpha = 0.22f), radius = dialRadius, center = Offset(leftCx, cy), style = Stroke(width = 5f))
    val rpmAngle = Math.toRadians((140f + 250f * fillRatio).toDouble())
    drawLine(
        color = barColor,
        start = Offset(leftCx, cy),
        end = Offset(leftCx + (dialRadius * 0.88f * cos(rpmAngle)).toFloat(), cy + (dialRadius * 0.88f * sin(rpmAngle)).toFloat()),
        strokeWidth = 7f,
        cap = StrokeCap.Round
    )
    drawCircle(color = accentColor, radius = 12f, center = Offset(leftCx, cy))

    // Sağ Kadran: Analog Hız (km/h)
    drawCircle(color = fgColor.copy(alpha = 0.22f), radius = dialRadius, center = Offset(rightCx, cy), style = Stroke(width = 5f))
    val maxSpeedScale = if (spec.skinId.contains("hayabusa")) 340.0 else 260.0
    val speedRatio = (telemetry.displayedSpeedKmh / maxSpeedScale).toFloat().coerceIn(0f, 1f)
    val spdAngle = Math.toRadians((140f + 250f * speedRatio).toDouble())
    drawLine(
        color = accentColor,
        start = Offset(rightCx, cy),
        end = Offset(rightCx + (dialRadius * 0.88f * cos(spdAngle)).toFloat(), cy + (dialRadius * 0.88f * sin(spdAngle)).toFloat()),
        strokeWidth = 7f,
        cap = StrokeCap.Round
    )
    drawCircle(color = accentColor, radius = 12f, center = Offset(rightCx, cy))
}

/**
 * Merkezi Dijital Hız, Devasa Vites Kutusu (N / 1..6) ve Gerçek RPM Okuması
 */
private fun DrawScope.drawCentralSpeedAndGearBlock(
    spec: DeclarativeGaugeSpec,
    subModeIndex: Int,
    telemetry: UnifiedTelemetryState,
    useMph: Boolean,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    isInverted: Boolean,
    shiftOffset: Offset,
    language: AppLanguage
) {
    val cx = size.width * 0.5f + shiftOffset.x
    val cy = size.height * 0.50f + shiftOffset.y

    val subModeName = spec.availableSubModes.getOrElse(subModeIndex) { spec.availableSubModes.firstOrNull() ?: "OEM" }
    drawNativeText(
        text = "#${spec.modelNumber} ${spec.brand.uppercase(Locale.ROOT)} ${spec.modelName}  •  $subModeName",
        x = cx,
        y = size.height * 0.085f + shiftOffset.y,
        textSizePx = 20f,
        color = fgColor.copy(alpha = 0.80f),
        align = Paint.Align.CENTER,
        bold = true
    )

    // Hız Değeri (Kontak Sweep sırasında 299/888)
    val speedVal = if (useMph) (telemetry.displayedSpeedKmh * 0.621371).roundToInt() else telemetry.displayedSpeedKmh.roundToInt()
    val unitStr = if (useMph) "mph" else "km/h"

    drawNativeText(
        text = speedVal.toString(),
        x = cx - 48f,
        y = cy + 28f,
        textSizePx = 112f,
        color = fgColor,
        align = Paint.Align.RIGHT,
        bold = true
    )
    drawNativeText(
        text = unitStr + (if (telemetry.is299LockEngaged) " [299 LOCK]" else ""),
        x = cx - 44f,
        y = cy + 62f,
        textSizePx = 22f,
        color = fgColor.copy(alpha = 0.72f),
        align = Paint.Align.RIGHT,
        bold = true
    )

    // Vites Kutusu (Evre 1 ve Evre 2'de KTM/BMW/Ducati orijinal vites kutusu renk terslemesi!)
    val gearBoxBg = when {
        telemetry.isNeutral -> Color(0xFF00C853)
        telemetry.shiftLightState == ShiftLightState.STAGE_2_SHIFT_NOW ||
            telemetry.shiftLightState == ShiftLightState.OVER_REV_LIMITER ->
            if (telemetry.shiftFlashPhaseOn) Color(spec.stage2InvertBgColorHex) else accentColor.copy(alpha = 0.25f)
        telemetry.shiftLightState == ShiftLightState.STAGE_1_APPROACHING ->
            Color(spec.stage1ShiftColorHex)
        else -> fgColor.copy(alpha = 0.12f)
    }

    val gearTextColor = when {
        telemetry.isNeutral -> Color(0xFF06150A)
        telemetry.shiftLightState != ShiftLightState.NORMAL -> Color.White
        else -> fgColor
    }

    val scaleAnim = 0.85f + 0.15f * telemetry.gearShiftAnimationProgress
    val boxW = 110f * scaleAnim
    val boxH = 126f * scaleAnim
    val boxLeft = cx + 18f
    val boxTop = cy - boxH * 0.55f

    drawRoundRect(
        color = gearBoxBg,
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(18f, 18f)
    )
    drawNativeText(
        text = telemetry.gearDisplayString,
        x = boxLeft + boxW * 0.5f,
        y = boxTop + boxH * 0.74f,
        textSizePx = 88f * scaleAnim,
        color = gearTextColor,
        align = Paint.Align.CENTER,
        bold = true
    )

    // Aktif Motorun Gerçek Devri (RPM) & Shift Durumu Etiketi
    val rpmText = "${telemetry.actualEngineRpm.roundToInt()} RPM"
    val shiftLabel = when (telemetry.shiftLightState) {
        ShiftLightState.OVER_REV_LIMITER -> "  •  " + LocalizationManager.getString(StringKey.GAUGE_REV_LIMITER, language)
        ShiftLightState.STAGE_2_SHIFT_NOW -> "  •  " + LocalizationManager.getString(StringKey.GAUGE_SHIFT_NOW, language)
        ShiftLightState.STAGE_1_APPROACHING -> "  •  SHIFT STAGE 1"
        ShiftLightState.NORMAL -> if (telemetry.isClutchPulled) {
            "  •  " + LocalizationManager.getString(StringKey.GAUGE_CLUTCH_IN, language)
        } else ""
    }

    drawNativeText(
        text = rpmText + shiftLabel,
        x = cx,
        y = cy + 114f,
        textSizePx = 25f,
        color = if (telemetry.shiftLightState != ShiftLightState.NORMAL) barColor else fgColor.copy(alpha = 0.85f),
        align = Paint.Align.CENTER,
        bold = true
    )
}

/**
 * Sol Telemetri Kanadı: Estetik Eğimölçer (Lean Angle °L / °R + İvme/Fren G-Bar)
 */
private fun DrawScope.drawAestheticLeanAnglePod(
    telemetry: UnifiedTelemetryState,
    fgColor: Color,
    accentColor: Color,
    language: AppLanguage,
    shiftOffset: Offset
) {
    val cardLeft = size.width * 0.04f + shiftOffset.x
    val cardTop = size.height * 0.55f + shiftOffset.y
    val cardW = size.width * 0.22f
    val cardH = size.height * 0.28f

    drawRoundRect(
        color = fgColor.copy(alpha = 0.08f),
        topLeft = Offset(cardLeft, cardTop),
        size = Size(cardW, cardH),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 2f)
    )

    val centerX = cardLeft + cardW * 0.5f
    val arcCenterY = cardTop + cardH * 0.54f
    val arcRadius = min(cardW, cardH) * 0.36f

    drawNativeText(
        text = LocalizationManager.getString(StringKey.GAUGE_LEAN_ANGLE, language),
        x = centerX,
        y = cardTop + 26f,
        textSizePx = 16f,
        color = fgColor.copy(alpha = 0.72f),
        align = Paint.Align.CENTER,
        bold = true
    )

    // -60°..+60° Yatış Yayı
    drawArc(
        color = fgColor.copy(alpha = 0.18f),
        startAngle = 210f,
        sweepAngle = 120f,
        useCenter = false,
        topLeft = Offset(centerX - arcRadius, arcCenterY - arcRadius),
        size = Size(arcRadius * 2f, arcRadius * 2f),
        style = Stroke(width = 12f, cap = StrokeCap.Round)
    )

    val clampedLean = telemetry.leanAngleDeg.toFloat().coerceIn(-60f, 60f)
    val leanSweep = (clampedLean / 60f) * 60f
    val leanColor = when {
        abs(clampedLean) >= 48f -> Color(0xFFFF1744)
        abs(clampedLean) >= 35f -> Color(0xFFFF9100)
        else -> accentColor
    }
    drawArc(
        color = leanColor,
        startAngle = 270f,
        sweepAngle = leanSweep,
        useCenter = false,
        topLeft = Offset(centerX - arcRadius, arcCenterY - arcRadius),
        size = Size(arcRadius * 2f, arcRadius * 2f),
        style = Stroke(width = 12f, cap = StrokeCap.Round)
    )

    val sidePrefix = when {
        clampedLean < -1.0f -> "L "
        clampedLean > 1.0f -> "R "
        else -> ""
    }
    drawNativeText(
        text = "$sidePrefix${abs(clampedLean).roundToInt()}°",
        x = centerX,
        y = arcCenterY + 12f,
        textSizePx = 32f,
        color = fgColor,
        align = Paint.Align.CENTER,
        bold = true
    )

    // Altında anlık boyuna G-kuvveti (+G ivme / -G fren)
    val gStr = String.format(Locale.US, "%+.2f G", telemetry.longitudinalG)
    drawNativeText(
        text = "${LocalizationManager.getString(StringKey.GAUGE_G_FORCE, language)}: $gStr",
        x = centerX,
        y = cardTop + cardH - 14f,
        textSizePx = 16f,
        color = fgColor.copy(alpha = 0.78f),
        align = Paint.Align.CENTER,
        bold = false
    )
}

/**
 * Sağ Telemetri Kanadı: Estetik Teker Derece Ölçer & Teker Metre (Wheelie Angle °, Süre sn, Mesafe m)
 *
 * Eğimölçer (Lean Angle) ile %100 simetrik, OEM estetiğinde tasarlanmıştır:
 *  - Kavisli Teker Açısı Barı (0°..65°) + Yeşil/Turkuaz Denge Noktası (Balance Point: 36°..46°) Bölgesi
 *  - Anlık Teker Açısı (°), Teker Süresi (sn) ve Teker Mesafesi (m)
 *  - Dinamik Ön Teker Kalkış Silüet Çizgisi
 */
private fun DrawScope.drawAestheticWheelieMeterPod(
    telemetry: UnifiedTelemetryState,
    fgColor: Color,
    accentColor: Color,
    language: AppLanguage,
    shiftOffset: Offset
) {
    val cardW = size.width * 0.22f
    val cardH = size.height * 0.28f
    val cardLeft = size.width * 0.74f + shiftOffset.x
    val cardTop = size.height * 0.55f + shiftOffset.y

    val angleDeg = telemetry.wheelieAngleDeg.toFloat().coerceIn(0f, 65f)
    val isInBalancePoint = angleDeg in 35f..47f
    val isOverPitchDanger = angleDeg > 52f

    val podHighlightColor = when {
        isOverPitchDanger -> Color(0xFFFF1744)
        isInBalancePoint -> Color(0xFF00E676)
        telemetry.isWheelieActive -> accentColor
        else -> fgColor.copy(alpha = 0.35f)
    }

    // Dış çerçeve (Teker aktifken parlak OEM vurgu çerçevesi)
    drawRoundRect(
        color = if (telemetry.isWheelieActive) podHighlightColor.copy(alpha = 0.16f) else fgColor.copy(alpha = 0.08f),
        topLeft = Offset(cardLeft, cardTop),
        size = Size(cardW, cardH),
        cornerRadius = CornerRadius(16f, 16f)
    )
    drawRoundRect(
        color = podHighlightColor,
        topLeft = Offset(cardLeft, cardTop),
        size = Size(cardW, cardH),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = if (telemetry.isWheelieActive) 3.2f else 2f)
    )

    val centerX = cardLeft + cardW * 0.5f
    drawNativeText(
        text = if (isInBalancePoint) {
            "★ " + LocalizationManager.getString(StringKey.GAUGE_WHEELIE_BALANCE_POINT, language)
        } else {
            LocalizationManager.getString(StringKey.GAUGE_WHEELIE_ANGLE, language)
        },
        x = centerX,
        y = cardTop + 26f,
        textSizePx = 16f,
        color = if (isInBalancePoint) Color(0xFF00E676) else fgColor.copy(alpha = 0.80f),
        align = Paint.Align.CENTER,
        bold = true
    )

    // Sol tarafta Kavisli Teker Derece Ölçer Yayı (0° yataydan 65° yukarıya)
    val arcCx = cardLeft + cardW * 0.34f
    val arcCy = cardTop + cardH * 0.62f
    val arcR = min(cardW, cardH) * 0.34f

    // Arka yay (0°..65° -> -65° süpürme)
    drawArc(
        color = fgColor.copy(alpha = 0.18f),
        startAngle = 360f - 65f,
        sweepAngle = 65f,
        useCenter = false,
        topLeft = Offset(arcCx - arcR, arcCy - arcR),
        size = Size(arcR * 2f, arcR * 2f),
        style = Stroke(width = 12f, cap = StrokeCap.Round)
    )
    // Denge Noktası (Balance Point 35°..47°) Yeşil/Turkuaz Hedef Dilimi
    drawArc(
        color = Color(0xFF00E676).copy(alpha = 0.42f),
        startAngle = 360f - 47f,
        sweepAngle = 12f,
        useCenter = false,
        topLeft = Offset(arcCx - arcR, arcCy - arcR),
        size = Size(arcR * 2f, arcR * 2f),
        style = Stroke(width = 12f, cap = StrokeCap.Butt)
    )
    // Aktif Teker Açısı Yayı
    val activeSweep = angleDeg.coerceIn(0f, 65f)
    if (activeSweep > 0.5f) {
        drawArc(
            color = podHighlightColor,
            startAngle = 360f - activeSweep,
            sweepAngle = activeSweep,
            useCenter = false,
            topLeft = Offset(arcCx - arcR, arcCy - arcR),
            size = Size(arcR * 2f, arcR * 2f),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )
    }

    // Dinamik Şasi Eğim Çizgisi (Arka teker merkezinden ön tekere uzanan vektör)
    val bikeRad = Math.toRadians(-angleDeg.toDouble())
    val chassisLen = arcR * 0.78f
    val frontWheelPos = Offset(
        arcCx + (chassisLen * cos(bikeRad)).toFloat(),
        arcCy + (chassisLen * sin(bikeRad)).toFloat()
    )
    drawLine(
        color = podHighlightColor,
        start = Offset(arcCx, arcCy),
        end = frontWheelPos,
        strokeWidth = 4.5f,
        cap = StrokeCap.Round
    )
    drawCircle(color = fgColor, radius = 6f, center = Offset(arcCx, arcCy))
    drawCircle(color = podHighlightColor, radius = 6f, center = frontWheelPos)

    // Sağ tarafta: Teker Derecesi (°), Teker Süresi (sn) ve Teker Mesafesi (m)
    val readoutX = cardLeft + cardW * 0.72f
    drawNativeText(
        text = "${angleDeg.roundToInt()}°",
        x = readoutX,
        y = cardTop + cardH * 0.48f,
        textSizePx = 36f,
        color = if (telemetry.isWheelieActive) podHighlightColor else fgColor,
        align = Paint.Align.CENTER,
        bold = true
    )

    val timeStr = String.format(Locale.US, "%.2f s", telemetry.currentWheelieDurationSec)
    val distStr = String.format(Locale.US, "%.1f m", telemetry.currentWheelieDistanceMeters)

    drawNativeText(
        text = "⏱ $timeStr",
        x = readoutX,
        y = cardTop + cardH * 0.70f,
        textSizePx = 20f,
        color = fgColor,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = "📏 $distStr",
        x = readoutX,
        y = cardTop + cardH * 0.89f,
        textSizePx = 18f,
        color = fgColor.copy(alpha = 0.82f),
        align = Paint.Align.CENTER,
        bold = false
    )
}

private fun DrawScope.drawBottomTelemetryStatusStrip(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    fgColor: Color,
    accentColor: Color,
    language: AppLanguage
) {
    val w = size.width
    val bottomY = size.height * 0.93f

    // Sol Alt: Aktif Mekanik Motor Profili (Single Source of Truth) + Dinamik Lastik Yarıçapı
    val bikeInfo = "🏍️ ${telemetry.activeBikeDisplayName} (Redline: ${telemetry.bikeRedlineRpm.roundToInt()} RPM | R_eff: ${telemetry.effectiveTireRadiusMm.roundToInt()} mm)"
    drawNativeText(
        text = bikeInfo,
        x = w * 0.04f,
        y = bottomY,
        textSizePx = 17f,
        color = fgColor.copy(alpha = 0.78f),
        align = Paint.Align.LEFT,
        bold = true
    )

    // Orta Alt: Sanal Dyno Beygir & Tork + TC
    val dynoInfo = String.format(
        Locale.US,
        "DYNO: %.1f HP  |  %.0f Nm  |  %s: %.0f°C  |  %s: %.0f%%",
        telemetry.estimatedWheelHorsepower,
        telemetry.estimatedWheelTorqueNm,
        LocalizationManager.getString(StringKey.GAUGE_COOLANT_TEMP, language),
        telemetry.coolantTempCelsius,
        LocalizationManager.getString(StringKey.GAUGE_FUEL_LEVEL, language),
        telemetry.fuelLevelRatio * 100f
    )
    drawNativeText(
        text = dynoInfo,
        x = w * 0.96f,
        y = bottomY,
        textSizePx = 17f,
        color = fgColor.copy(alpha = 0.78f),
        align = Paint.Align.RIGHT,
        bold = false
    )
}

private fun DrawScope.drawNativeText(
    text: String,
    x: Float,
    y: Float,
    textSizePx: Float,
    color: Color,
    align: Paint.Align,
    bold: Boolean
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        this.textSize = textSizePx
        this.textAlign = align
        this.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

