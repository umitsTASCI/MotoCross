package com.oemcluster.pro.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.oemcluster.pro.catalog.DeclarativeGaugeSpec
import com.oemcluster.pro.catalog.GaugeDisplayCategory
import com.oemcluster.pro.catalog.GaugeGeometryType
import com.oemcluster.pro.domain.IgnitionBootPhase
import com.oemcluster.pro.domain.RpmScalingMode
import com.oemcluster.pro.domain.ShiftLightState
import com.oemcluster.pro.domain.UnifiedTelemetryState
import com.oemcluster.pro.domain.WheelieGaugeDisplayMode
import com.oemcluster.pro.i18n.AppLanguage
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * MotoCross — 270 OEM gösterge panelinin tek giriş noktası.
 *
 * Tasarım kuralları:
 *  - Tüm ölçüler min(w,h)/100 normalize birimine bağlıdır; hiçbir sabit piksel yoktur.
 *  - Hız/vites/deVir bloğu ortak bir katman değildir; her geometri ailesi kendi
 *    kompozisyonu içinde bu değerleri farklı yerde ve farklı tipografiyle çizer.
 *  - Her model, paleti + geometrisi + varyantı + shift-LED deseni + marka imzası
 *    ile diğer 104 modelden ayrışır.
 */
@Composable
fun OemClusterCanvasRenderer(
    spec: DeclarativeGaugeSpec,
    subModeIndex: Int,
    telemetry: UnifiedTelemetryState,
    rpmScalingMode: RpmScalingMode,
    wheelieDisplayMode: WheelieGaugeDisplayMode,
    useMph: Boolean,
    useFahrenheit: Boolean = false,
    clock24h: Boolean = true,
    fuelUnitCode: String = "l100km",
    chromaKeyGreenScreen: Boolean,
    oledPixelShiftOffset: Offset,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    tripKm: Double = 0.0,
    odometerKm: Double = 0.0,
    sessionKm: Double = 0.0,
    tripBKm: Double = 0.0
) {
    val type = remember(spec.skinId) { typographyFor(spec) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w < 2f || h < 2f) return@Canvas

        val dayBg = hexColor(spec.dayBgColorHex)
        val nightBg = hexColor(spec.nightBgColorHex)
        val dayFg = hexColor(spec.dayPrimaryColorHex)
        val nightFg = hexColor(spec.nightPrimaryColorHex)
        val accent = hexColor(spec.accentBrandColorHex)
        val stage1 = hexColor(spec.stage1ShiftColorHex)
        val invertBg = hexColor(spec.stage2InvertBgColorHex)
        val invertFg = hexColor(spec.stage2InvertFgColorHex)

        val baseBg = lerp(dayBg, nightBg, telemetry.nightModeTransitionFactor.coerceIn(0f, 1f))
        val baseFg = lerp(dayFg, nightFg, telemetry.nightModeTransitionFactor.coerceIn(0f, 1f))
        val inverted = telemetry.fullColorInversionActive &&
            (telemetry.shiftLightState == ShiftLightState.STAGE_2_SHIFT_NOW ||
                telemetry.shiftLightState == ShiftLightState.OVER_REV_LIMITER)

        val activeBg = when {
            chromaKeyGreenScreen -> Color(0xFF00FF00)
            inverted -> invertBg
            else -> baseBg
        }
        val activeFg = if (inverted) invertFg else baseFg

        val palette = GaugePalette(
            bg = activeBg,
            fg = activeFg,
            accent = accent,
            stage1 = stage1,
            invertBg = invertBg,
            invertFg = invertFg,
            inverted = inverted,
            lcd = spec.displayCategory == GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD
        )

        drawRect(activeBg)

        val mappedRpm = telemetry.getMappedRpmForGauge(
            gaugeRedlineRpm = spec.gaugeRedlineRpm,
            gaugeMaxRpm = spec.gaugeMaxRpm,
            scalingMode = rpmScalingMode
        )
        val speedValue = if (useMph) (telemetry.displayedSpeedKmh * 0.621371).roundToInt()
        else telemetry.displayedSpeedKmh.roundToInt()
        val maxSpeed = estimateSpeedScale(spec, speedValue)

        val ctx = GaugeContext(
            spec = spec,
            telemetry = telemetry,
            palette = palette,
            type = type,
            subModeIndex = subModeIndex,
            rpmRatio = (mappedRpm / max(1000.0, spec.gaugeMaxRpm)).toFloat().coerceIn(0f, 1.08f),
            peakRatio = peakRatioFor(telemetry, spec),
            redlineRatio = (spec.gaugeRedlineRpm / max(1000.0, spec.gaugeMaxRpm)).toFloat().coerceIn(0.55f, 0.97f),
            mappedRpm = mappedRpm,
            speedValue = speedValue,
            speedUnit = if (useMph) "mph" else "km/h",
            useMph = useMph,
            useFahrenheit = useFahrenheit,
            clock24h = clock24h,
            fuelUnitCode = fuelUnitCode,
            shiftOffset = if (oledPixelShiftOffset == Offset.Zero) Offset.Zero else oledPixelShiftOffset,
            tripKm = tripKm,
            odometerKm = odometerKm,
            sessionKm = sessionKm,
            tripBKm = tripBKm,
            w = w,
            h = h
        )

        if (telemetry.ignitionBootPhase == IgnitionBootPhase.OEM_SPLASH_LOGO) {
            drawBootSplash(ctx, telemetry.ignitionSweepProgress.coerceIn(0f, 1f))
            return@Canvas
        }

        translate(ctx.shiftOffset.x, ctx.shiftOffset.y) {
            drawBackdrop(ctx)
            if (!tryDrawBespokeOemCluster(ctx)) {
                when (spec.geometryType) {
                    GaugeGeometryType.BOOMERANG_BAR_TFT -> drawBoomerangTft(ctx)
                    GaugeGeometryType.CURVED_SWEEP_BAR_TFT -> drawCurvedSweepTft(ctx)
                    GaugeGeometryType.CIRCULAR_RING_TFT -> drawCircularRingTft(ctx)
                    GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT -> drawSharpHorizontalEvo(ctx)
                    GaugeGeometryType.SYMMETRIC_PINCER_BAR_TFT -> drawSymmetricPincer(ctx)
                    GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD -> drawCentralAnalogHybrid(ctx)
                    GaugeGeometryType.FIVE_DIAL_HAYABUSA_HYBRID -> drawHayabusaFiveDial(ctx)
                    GaugeGeometryType.DUAL_CLASSIC_ANALOG -> drawDualClassicAnalog(ctx)
                    GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD -> drawSinglePuck(ctx)
                    GaugeGeometryType.F1_DIGITAL_ARC_BAR -> drawF1DigitalArc(ctx)
                    GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT -> drawRallyTower(ctx)
                    GaugeGeometryType.PANORAMIC_SPLIT_TFT -> drawPanoramicSplit(ctx)
                }
            }
            drawModelUniqueHardwareSignature(ctx)

            val showWheelie = when (wheelieDisplayMode) {
                WheelieGaugeDisplayMode.ALWAYS_VISIBLE -> true
                WheelieGaugeDisplayMode.SMART_AUTO_POPUP -> telemetry.isWheelieSummaryVisible
                WheelieGaugeDisplayMode.HIDDEN_OEM_PURE -> false
            }
            if (showWheelie && spec.supportsLeanAndWheeliePod) {
                val cardW = min(w * 0.46f, ctx.u * 46f)
                val cardY = h - max(h * 0.072f, min(h * 0.085f, ctx.u * 9f)) - cardW * 0.24f
                drawWheelieCard(ctx, w * 0.5f, cardY, cardW)
            }
        }
    }
}

private fun peakRatioFor(telemetry: UnifiedTelemetryState, spec: DeclarativeGaugeSpec): Float {
    if (!spec.hasPeakHoldTrail || telemetry.peakHoldRpm <= 0.0) return -1f
    val mapped = telemetry.peakHoldRpm * (spec.gaugeRedlineRpm / max(1000.0, telemetry.bikeRedlineRpm))
    return (mapped / max(1000.0, spec.gaugeMaxRpm)).toFloat().coerceIn(0f, 1f)
}

private fun DrawScope.drawBackdrop(ctx: GaugeContext) {
    val p = ctx.palette
    when (ctx.spec.displayCategory) {
        GaugeDisplayCategory.ANALOG_HYBRID, GaugeDisplayCategory.RETRO_CLASSIC -> {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(lerp(p.bg, Color(0xFF2A2E36), 0.28f), p.bg, lerp(p.bg, Color.Black, 0.55f)),
                    center = Offset(ctx.w * 0.5f, ctx.h * 0.42f),
                    radius = max(ctx.w, ctx.h) * 0.72f
                )
            )
            // fırçalanmış metal dokusu
            var y = 0f
            while (y < ctx.h) {
                drawLine(p.fg.copy(alpha = 0.018f), Offset(0f, y), Offset(ctx.w, y), 1f)
                y += ctx.u * 0.7f
            }
        }
        GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD -> {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(lerp(p.bg, Color.Black, 0.28f), p.bg, lerp(p.bg, p.fg, 0.03f))
                )
            )
            var x = 0f
            while (x < ctx.w) {
                var yy = 0f
                while (yy < ctx.h) {
                    drawCircle(p.fg.copy(alpha = 0.035f), max(0.4f, ctx.u * 0.13f), Offset(x, yy))
                    yy += ctx.u * 2.2f
                }
                x += ctx.u * 2.2f
            }
        }
        GaugeDisplayCategory.MODERN_TFT -> {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(lerp(p.bg, Color.White, 0.035f), p.bg, lerp(p.bg, Color.Black, 0.30f)),
                    start = Offset(0f, 0f),
                    end = Offset(ctx.w, ctx.h)
                )
            )
            drawCrosshairTexture(p, ctx.w, ctx.h, ctx.u * 5.5f)
            // köşe vinyet
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, p.bg.copy(alpha = 0.75f)),
                    center = Offset(ctx.w * 0.5f, ctx.h * 0.5f),
                    radius = max(ctx.w, ctx.h) * 0.68f
                )
            )
        }
    }
    // İnce çerçeve — fiziksel ekran hissi
    if (ctx.spec.displayCategory != GaugeDisplayCategory.RETRO_CLASSIC) {
        drawRect(
            color = p.fg.copy(alpha = 0.10f),
            topLeft = Offset(ctx.u * 0.5f, ctx.u * 0.5f),
            size = androidx.compose.ui.geometry.Size(ctx.w - ctx.u, ctx.h - ctx.u),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = max(1f, ctx.u * 0.18f))
        )
    }
}
