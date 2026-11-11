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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.oemcluster.pro.catalog.DeclarativeGaugeSpec
import com.oemcluster.pro.catalog.GaugeDisplayCategory
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * MotoCross (OEM-Cluster Pro) — 120 FPS Ultra-Gerçekçi OEM Motosiklet Gösterge Çizim Motoru.
 *
 * Her gösterge mimarisi (12 Geometri Ailesi + Marka Elektronik Matrisleri: KTM MTC, Yamaha YRC,
 * Ducati Corse EVO, BMW M-Sport, Honda HRC, Suzuki Hayabusa 5-Kadran SDMS-a, Kawasaki KTRC & H2 Boost,
 * 7-Segment Polarize LCD Hayalet Rakamları, Dakar Rally Kulesi ve Çift Krom Klasik Analog)
 * sıfır çakışma (zero-overlap) ve gerçek 3B çerçeve derinliğiyle çizilir.
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

        // Ana Arka Plan ve 3B Gösterge Kasası (Outer Bezel)
        drawRect(color = Color(0xFF050608))
        drawOuterInstrumentHousing(
            spec = spec,
            panelBgColor = activeBgColor,
            accentColor = accentColor,
            chromaKey = chromaKeyGreenScreen
        )

        // Kontak Açılış Logosu Evresi (Phase 1)
        if (telemetry.ignitionBootPhase == IgnitionBootPhase.OEM_SPLASH_LOGO) {
            drawOemBootSplashScreen(spec, accentColor, activeFgColor)
            return@Canvas
        }

        // Üst Çerçeve Fiziksel Shift-Light LED Dizisi + OEM İkaz Lambaları (N, ABS, TC, OIL, MIL, GPS)
        drawUpperShiftLedsAndTelltaleBay(
            spec = spec,
            subModeIndex = subModeIndex,
            telemetry = telemetry,
            fgColor = activeFgColor,
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

        val speedVal = if (useMph) {
            (telemetry.displayedSpeedKmh * 0.621371).roundToInt()
        } else {
            telemetry.displayedSpeedKmh.roundToInt()
        }
        val unitStr = if (useMph) "mph" else "km/h"
        val subModeName = spec.availableSubModes.getOrElse(subModeIndex) {
            spec.availableSubModes.firstOrNull() ?: "ROAD"
        }

        // 12 Özgün OEM Gösterge Mimarisi (Her biri kendi hız/vites/telemetri yerleşimine sahiptir)
        when (spec.geometryType) {
            GaugeGeometryType.BOOMERANG_BAR_TFT -> drawKtmBmwBoomerangCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.CURVED_SWEEP_BAR_TFT -> drawYamahaKawasakiSweepCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.SHARP_HORIZONTAL_EVO_TFT -> drawDucatiBmwTrackEvoCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.CIRCULAR_RING_TFT -> drawCircularRingTurbineCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.SYMMETRIC_PINCER_BAR_TFT -> drawTriumphApriliaPincerCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.CENTRAL_ANALOG_HYBRID_LCD -> drawSupersportCentralAnalogHybridCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.FIVE_DIAL_HAYABUSA_HYBRID -> drawHayabusaFiveDialCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.DUAL_CLASSIC_ANALOG -> drawDualClassicChromeAnalogCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.SINGLE_PUCK_ROUND_TFT_LCD -> drawSingleRoundPuckCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.F1_DIGITAL_ARC_BAR -> drawPolarizedSegmentLcdCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.RALLY_TOWER_VERTICAL_TFT -> drawDakarRallyTowerCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
            GaugeGeometryType.PANORAMIC_SPLIT_TFT -> drawTouringPanoramicSplitCockpit(
                spec, telemetry, subModeName, speedVal, unitStr,
                gaugeFillRatio, peakFillRatio, redlineRatio,
                activeFgColor, activeBarColor, accentColor, oledPixelShiftOffset
            )
        }

        // Teker Derece Ölçer & Teker Metre (Wheelie Analyzer Overlay Pill — Üst-Orta Altı / Sağ-Alt)
        val shouldShowWheeliePod = when (wheelieDisplayMode) {
            WheelieGaugeDisplayMode.ALWAYS_VISIBLE -> true
            WheelieGaugeDisplayMode.SMART_AUTO_POPUP -> telemetry.isWheelieSummaryVisible
            WheelieGaugeDisplayMode.HIDDEN_OEM_PURE -> false
        }
        if (shouldShowWheeliePod) {
            drawCompactWheelieAnalyzerBanner(
                telemetry = telemetry,
                fgColor = activeFgColor,
                accentColor = accentColor,
                language = language,
                shiftOffset = oledPixelShiftOffset
            )
        }

        // Alt Durum Çubuğu: Aktif Motor Profili ("Single Source of Truth"), Gerçek RPM, Güç, Eğim & GPS
        drawBottomTelemetryStatusStrip(
            spec = spec,
            telemetry = telemetry,
            fgColor = activeFgColor,
            accentColor = accentColor,
            language = language
        )
    }
}

/**
 * 3B Kalıplanmış Gösterge Dış Çerçevesi (Bezel) ve Yansıma Önleyici Cam Katmanı
 */
private fun DrawScope.drawOuterInstrumentHousing(
    spec: DeclarativeGaugeSpec,
    panelBgColor: Color,
    accentColor: Color,
    chromaKey: Boolean
) {
    val w = size.width
    val h = size.height
    val margin = 12f

    // Dış metalik/karbon çerçeve
    drawRoundRect(
        color = Color(0xFF141821),
        topLeft = Offset(margin * 0.4f, margin * 0.4f),
        size = Size(w - margin * 0.8f, h - margin * 0.8f),
        cornerRadius = CornerRadius(32f, 32f)
    )

    // İç ekran yüzeyi
    drawRoundRect(
        color = panelBgColor,
        topLeft = Offset(margin, margin),
        size = Size(w - margin * 2f, h - margin * 2f),
        cornerRadius = CornerRadius(26f, 26f)
    )

    if (!chromaKey) {
        // Hafif cam parlama gradyanı (Anti-Glare Glass Sheen)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.045f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.14f)
                )
            ),
            topLeft = Offset(margin, margin),
            size = Size(w - margin * 2f, h - margin * 2f),
            cornerRadius = CornerRadius(26f, 26f)
        )
    }

    // Çerçeve vurgusu
    drawRoundRect(
        color = accentColor.copy(alpha = 0.38f),
        topLeft = Offset(margin, margin),
        size = Size(w - margin * 2f, h - margin * 2f),
        cornerRadius = CornerRadius(26f, 26f),
        style = Stroke(width = 2.5f)
    )
}

/**
 * Kontak Açılış (Ignition Boot Phase 1) Marka Splash Ekranı
 */
private fun DrawScope.drawOemBootSplashScreen(
    spec: DeclarativeGaugeSpec,
    accentColor: Color,
    fgColor: Color
) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f

    drawRoundRect(
        color = accentColor.copy(alpha = 0.12f),
        topLeft = Offset(cx - size.width * 0.34f, cy - 95f),
        size = Size(size.width * 0.68f, 190f),
        cornerRadius = CornerRadius(24f, 24f)
    )
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(cx - size.width * 0.34f, cy - 95f),
        size = Size(size.width * 0.68f, 190f),
        cornerRadius = CornerRadius(24f, 24f),
        style = Stroke(width = 3.5f)
    )
    drawNativeText(
        text = spec.brand.uppercase(Locale.ROOT),
        x = cx,
        y = cy - 28f,
        textSizePx = 42f,
        color = accentColor,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = spec.bootSplashLogoText,
        x = cx,
        y = cy + 28f,
        textSizePx = 36f,
        color = fgColor,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = "${spec.modelName} • SYSTEM CHECK OK",
        x = cx,
        y = cy + 68f,
        textSizePx = 18f,
        color = fgColor.copy(alpha = 0.65f),
        align = Paint.Align.CENTER,
        bold = true
    )
}

/**
 * Üst OEM Telltale İkaz Lambaları Yuvası ([N], Sinyaller, Uzun Far, ABS, TC, OIL, MIL) ve Shift-Light LED Dizisi
 */
private fun DrawScope.drawUpperShiftLedsAndTelltaleBay(
    spec: DeclarativeGaugeSpec,
    subModeIndex: Int,
    telemetry: UnifiedTelemetryState,
    fgColor: Color,
    accentColor: Color,
    stage1Color: Color
) {
    val w = size.width
    val h = size.height
    val topY = h * 0.068f

    // Sol OEM İkaz Lambaları: Sol Sinyal [◄], Boş Vites [N], Uzun Far [HI], ABS
    val leftLampsX = w * 0.15f
    val isSweepPhase = telemetry.ignitionBootPhase == IgnitionBootPhase.NEEDLE_SWEEP_UP ||
        telemetry.ignitionBootPhase == IgnitionBootPhase.NEEDLE_SWEEP_RETURN
    // [N] Boş Vites Mücevheri
    val nActive = telemetry.isNeutral || isSweepPhase
    drawTelltaleLamp(
        x = leftLampsX,
        y = topY,
        w = 40f,
        h = 26f,
        label = "N",
        active = nActive,
        activeColor = Color(0xFF00E676)
    )
    // [ABS] Lambası (Hız < 5 km/h iken veya Sweep'te yanar, hareket halinde söner)
    val absActive = telemetry.trueSpeedKmh < 4.0 || isSweepPhase
    drawTelltaleLamp(
        x = leftLampsX + 48f,
        y = topY,
        w = 48f,
        h = 26f,
        label = "ABS",
        active = absActive,
        activeColor = Color(0xFFFFAB00)
    )
    // [TC] Traction Control Lambası (Yatış > 32° veya Teker > 5° veya Kesicide göz kırpar)
    val tcActive = telemetry.isWheelieActive ||
        abs(telemetry.leanAngleDeg) > 34.0 ||
        telemetry.shiftLightState == ShiftLightState.OVER_REV_LIMITER ||
        isSweepPhase
    drawTelltaleLamp(
        x = leftLampsX + 102f,
        y = topY,
        w = 42f,
        h = 26f,
        label = "TC",
        active = tcActive,
        activeColor = Color(0xFFFFAB00)
    )

    // Sağ OEM İkaz Lambaları: OIL, MIL, GPS Durumu, Saat
    val rightLampsX = w * 0.72f
    val oilOrMilActive = isSweepPhase
    drawTelltaleLamp(
        x = rightLampsX,
        y = topY,
        w = 46f,
        h = 26f,
        label = "OIL",
        active = oilOrMilActive,
        activeColor = Color(0xFFFF1744)
    )
    val gpsLabel = if (telemetry.hasGpsFix) "GPS ${max(4, telemetry.satellitesInUse)}S" else "NO GPS"
    drawTelltaleLamp(
        x = rightLampsX + 54f,
        y = topY,
        w = 78f,
        h = 26f,
        label = gpsLabel,
        active = true,
        activeColor = if (telemetry.hasGpsFix) Color(0xFF00E676) else Color(0xFFFFAB00)
    )

    // Merkez Shift-Light LED Dizisi
    val ratio = telemetry.normalizedRpmRatio.toFloat().coerceIn(0f, 1.1f)
    when (spec.shiftLedPattern) {
        ShiftLedPattern.NONE_INTERNAL_ONLY -> {
            // İç TFT Shift-Light şeridi (Evre 1 ve 2'de üst orta bar parlar)
            if (telemetry.shiftLightState != ShiftLightState.NORMAL) {
                val flash = telemetry.shiftLightState == ShiftLightState.STAGE_1_APPROACHING || telemetry.shiftFlashPhaseOn
                if (flash) {
                    val c = if (telemetry.shiftLightState == ShiftLightState.OVER_REV_LIMITER) Color(0xFFFF1744) else stage1Color
                    drawRoundRect(
                        color = c,
                        topLeft = Offset(w * 0.36f, topY - 12f),
                        size = Size(w * 0.28f, 24f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawNativeText(
                        text = "▲ SHIFT UP ▲",
                        x = w * 0.5f,
                        y = topY + 6f,
                        textSizePx = 17f,
                        color = Color.Black,
                        align = Paint.Align.CENTER,
                        bold = true
                    )
                }
            }
        }
        ShiftLedPattern.SINGLE_UPPER_LED,
        ShiftLedPattern.DUAL_SIDE_LIGHTNING_LEDS -> {
            val isOn = telemetry.shiftLightState != ShiftLightState.NORMAL &&
                (telemetry.shiftLightState == ShiftLightState.STAGE_1_APPROACHING || telemetry.shiftFlashPhaseOn)
            val ledColor = if (isOn) Color.White else Color(0xFF232832)
            drawCircle(color = ledColor, radius = 13f, center = Offset(w * 0.5f, topY))
            if (isOn) {
                drawCircle(color = stage1Color.copy(alpha = 0.5f), radius = 22f, center = Offset(w * 0.5f, topY))
            }
        }
        ShiftLedPattern.CONVERGING_5_LEDS -> {
            val ledCount = 5
            val spacing = 46f
            val startX = w * 0.5f - (spacing * 2f)
            for (i in 0 until ledCount) {
                val distFromCenter = abs(i - 2)
                val threshold = when (distFromCenter) {
                    2 -> 0.72f
                    1 -> 0.84f
                    else -> 0.94f
                }
                val lit = (ratio >= threshold) &&
                    (telemetry.shiftLightState != ShiftLightState.OVER_REV_LIMITER || telemetry.shiftFlashPhaseOn)
                val c = when {
                    !lit -> Color(0xFF1F242D)
                    distFromCenter == 0 -> Color(0xFFFF1744)
                    distFromCenter == 1 -> Color(0xFFFFAB00)
                    else -> Color(0xFF00E676)
                }
                drawRoundRect(
                    color = c,
                    topLeft = Offset(startX + i * spacing - 18f, topY - 9f),
                    size = Size(36f, 18f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS,
        ShiftLedPattern.TRIUMPH_7_BLUE_LEDS,
        ShiftLedPattern.KAWASAKI_LED_ARCH_BAR,
        ShiftLedPattern.MOTOGP_MARSHAL_BAR -> {
            val count = if (spec.shiftLedPattern == ShiftLedPattern.TRIUMPH_7_BLUE_LEDS) 7 else 9
            val spacing = 34f
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
                    color = if (lit) activeLedColor else Color(0xFF1F242D),
                    topLeft = Offset(startX + i * spacing - 13f, topY - 8f),
                    size = Size(26f, 16f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }
        }
    }
}

private fun DrawScope.drawTelltaleLamp(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    label: String,
    active: Boolean,
    activeColor: Color
) {
    val bg = if (active) activeColor.copy(alpha = 0.22f) else Color(0xFF181C24).copy(alpha = 0.55f)
    val border = if (active) activeColor else Color(0xFF2B3140).copy(alpha = 0.6f)
    val textCol = if (active) activeColor else Color(0xFF566074)
    drawRoundRect(
        color = bg,
        topLeft = Offset(x, y - h * 0.5f),
        size = Size(w, h),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = border,
        topLeft = Offset(x, y - h * 0.5f),
        size = Size(w, h),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 1.5f)
    )
    drawNativeText(
        text = label,
        x = x + w * 0.5f,
        y = y + 5f,
        textSizePx = 13f,
        color = textCol,
        align = Paint.Align.CENTER,
        bold = true
    )
}

// =========================================================================================
// MİMARİ 1: KTM 390/790/890/1290 DUKE, BMW CORE, YAMAHA MT-09 — BOOMERANG BAR TFT COCKPIT
// =========================================================================================
private fun DrawScope.drawKtmBmwBoomerangCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
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
    val totalSegments = 46
    val activeSegments = (fillRatio * totalSegments).roundToInt()
    val peakSegment = (peakRatio * totalSegments).roundToInt()
    val redlineSegment = (redlineRatio * totalSegments).roundToInt()

    val startX = w * 0.06f + shiftOffset.x
    val startY = h * 0.78f + shiftOffset.y
    val kneeX = w * 0.16f + shiftOffset.x
    val kneeY = h * 0.15f + shiftOffset.y
    val endX = w * 0.92f + shiftOffset.x
    val endY = h * 0.15f + shiftOffset.y

    // 1. 3B Açılı Paralelkenar Bumerang Takometre Segmentleri
    for (i in 0 until totalSegments) {
        val t = i.toFloat() / (totalSegments - 1).toFloat()
        val x: Float
        val y: Float
        val segH: Float
        if (t < 0.30f) {
            val localT = t / 0.30f
            x = startX + (kneeX - startX) * localT
            y = startY + (kneeY - startY) * localT
            segH = 26f + 16f * localT
        } else {
            val localT = (t - 0.30f) / 0.70f
            x = kneeX + (endX - kneeX) * localT
            y = kneeY + (endY - kneeY) * localT
            segH = 42f
        }

        val isLit = i < activeSegments
        val isPeak = spec.hasPeakHoldTrail && (i == peakSegment) && (peakSegment > activeSegments)
        val segColor = when {
            isLit && i >= redlineSegment -> Color(0xFFFF1744)
            isLit -> barColor
            isPeak -> Color.White
            i >= redlineSegment -> Color(0xFFFF1744).copy(alpha = 0.22f)
            else -> fgColor.copy(alpha = 0.13f)
        }

        val segW = ((w * 0.78f) / totalSegments) * 0.74f
        drawParallelogramSegment(x, y, segW, segH, skewX = 8f, color = segColor)

        if (i % 8 == 0) {
            val rpmNum = ((t * spec.gaugeMaxRpm) / 1000.0).roundToInt()
            drawNativeText(
                text = rpmNum.toString(),
                x = x + segW * 0.5f,
                y = y + segH + 22f,
                textSizePx = 19f,
                color = if (i >= redlineSegment) Color(0xFFFF1744) else fgColor.copy(alpha = 0.80f),
                align = Paint.Align.CENTER,
                bold = true
            )
        }
    }

    // 2. Sol-Orta: KTM / BMW 4-Hücreli Favori Telemetri Matrisi (COOLANT, BATT, TRIP 1, LEAN)
    val gridLeft = w * 0.19f + shiftOffset.x
    val gridTop = h * 0.34f + shiftOffset.y
    val cellW = w * 0.16f
    val cellH = h * 0.20f

    drawTelemetryInfoCell(gridLeft, gridTop, cellW, cellH, "COOLANT", "${telemetry.simulatedCoolantTempC}°C", fgColor, accentColor)
    drawTelemetryInfoCell(gridLeft + cellW + 10f, gridTop, cellW, cellH, "BATTERY", "14.2 V", fgColor, accentColor)
    drawTelemetryInfoCell(
        gridLeft,
        gridTop + cellH + 10f,
        cellW,
        cellH,
        "LEAN ANGLE",
        "${abs(telemetry.leanAngleDeg).roundToInt()}° ${if (telemetry.leanAngleDeg < -1) "L" else if (telemetry.leanAngleDeg > 1) "R" else ""}",
        fgColor,
        accentColor
    )
    drawTelemetryInfoCell(
        gridLeft + cellW + 10f,
        gridTop + cellH + 10f,
        cellW,
        cellH,
        "WHEEL POWER",
        "${telemetry.estimatedWheelHorsepower.roundToInt()} HP",
        fgColor,
        accentColor
    )

    // 3. Sağ-Merkez: Devasa Hız, Vites Küpü ve Sürüş Modu (STREET / TRACK / MTC)
    drawHeroSpeedAndGearCube(
        centerX = w * 0.68f + shiftOffset.x,
        centerY = h * 0.54f + shiftOffset.y,
        spec = spec,
        subModeName = subModeName,
        speedVal = speedVal,
        unitStr = unitStr,
        telemetry = telemetry,
        fgColor = fgColor,
        accentColor = accentColor
    )
}

// =========================================================================================
// MİMARİ 2: YAMAHA R1M / KAWASAKI ZX-10R / NINJA H2R — CURVED SWEEP + YRC / H2 BOOST COCKPIT
// =========================================================================================
private fun DrawScope.drawYamahaKawasakiSweepCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
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
    val totalSegments = 56
    val activeSegments = (fillRatio * totalSegments).roundToInt()
    val peakSegment = (peakRatio * totalSegments).roundToInt()
    val redlineSegment = (redlineRatio * totalSegments).roundToInt()

    // 1. Kavisli Yarış Süpürme Takometresi (Yamaha R1M / ZX-10R)
    for (i in 0 until totalSegments) {
        val t = i.toFloat() / (totalSegments - 1).toFloat()
        val x = w * (0.06f + 0.88f * t) + shiftOffset.x
        val curveDrop = (1f - sin(t * Math.PI * 0.88).toFloat()) * (h * 0.16f)
        val y = h * 0.13f + curveDrop + shiftOffset.y

        val isLit = i < activeSegments
        val isPeak = spec.hasPeakHoldTrail && (i == peakSegment) && (peakSegment > activeSegments)
        val segColor = when {
            isLit && i >= redlineSegment -> Color(0xFFFF1744)
            isLit -> barColor
            isPeak -> Color.White
            i >= redlineSegment -> Color(0xFFFF1744).copy(alpha = 0.22f)
            else -> fgColor.copy(alpha = 0.13f)
        }

        val segW = (w * 0.88f / totalSegments) * 0.72f
        drawParallelogramSegment(x, y, segW, 38f, skewX = 10f, color = segColor)

        if (i % 9 == 0) {
            val rpmNum = ((t * spec.gaugeMaxRpm) / 1000.0).roundToInt()
            drawNativeText(
                text = rpmNum.toString(),
                x = x + segW * 0.5f,
                y = y + 58f,
                textSizePx = 18f,
                color = if (i >= redlineSegment) Color(0xFFFF1744) else fgColor.copy(alpha = 0.80f),
                align = Paint.Align.CENTER,
                bold = true
            )
        }
    }

    // 2. Sol Alt: Yamaha YRC / Kawasaki KTRC Elektronik Kontrol Matrisi + H2 Supercharger Boost!
    val matrixLeft = w * 0.06f + shiftOffset.x
    val matrixTop = h * 0.44f + shiftOffset.y
    val isH2Supercharged = spec.skinId.contains("h2", ignoreCase = true)

    if (isH2Supercharged) {
        // Kawasaki Ninja H2 / H2R / ZH2 Supercharger Boost & Impeller Göstergesi
        val boostBar = (fillRatio * 2.4f).coerceIn(0f, 2.4f)
        val impellerK = (telemetry.actualEngineRpm * 9.2 / 1000.0).roundToInt()
        drawRoundRect(
            color = accentColor.copy(alpha = 0.14f),
            topLeft = Offset(matrixLeft, matrixTop),
            size = Size(w * 0.28f, h * 0.36f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(matrixLeft, matrixTop),
            size = Size(w * 0.28f, h * 0.36f),
            cornerRadius = CornerRadius(14f, 14f),
            style = Stroke(width = 2f)
        )
        drawNativeText(
            text = "SUPERCHARGER BOOST",
            x = matrixLeft + 16f,
            y = matrixTop + 26f,
            textSizePx = 15f,
            color = accentColor,
            align = Paint.Align.LEFT,
            bold = true
        )
        drawNativeText(
            text = String.format(Locale.US, "+%.2f BAR", boostBar),
            x = matrixLeft + 16f,
            y = matrixTop + 66f,
            textSizePx = 34f,
            color = fgColor,
            align = Paint.Align.LEFT,
            bold = true
        )
        drawNativeText(
            text = "IMPELLER: ${impellerK}k RPM  •  KTRC-1  •  KEBC-L",
            x = matrixLeft + 16f,
            y = matrixTop + 98f,
            textSizePx = 14f,
            color = fgColor.copy(alpha = 0.8f),
            align = Paint.Align.LEFT,
            bold = true
        )
    } else {
        // Yamaha R1M YRC / Kawasaki ZX-10R 6-Hücreli Elektronik Matris (PWR, TCS, SCS, LCS, QSS, ERS)
        val badges = listOf(
            "PWR" to "1",
            "TCS" to "2",
            "SCS" to "1",
            "LCS" to "1",
            "QSS" to "1",
            "ERS" to "A1"
        )
        val bw = w * 0.088f
        val bh = h * 0.15f
        badges.forEachIndexed { idx, (k, v) ->
            val col = idx % 3
            val row = idx / 3
            val bx = matrixLeft + col * (bw + 8f)
            val by = matrixTop + row * (bh + 10f)
            drawRoundRect(
                color = fgColor.copy(alpha = 0.08f),
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = accentColor.copy(alpha = 0.65f),
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 1.8f)
            )
            drawNativeText(
                text = k,
                x = bx + bw * 0.5f,
                y = by + 22f,
                textSizePx = 14f,
                color = accentColor,
                align = Paint.Align.CENTER,
                bold = true
            )
            drawNativeText(
                text = v,
                x = bx + bw * 0.5f,
                y = by + bh - 12f,
                textSizePx = 24f,
                color = fgColor,
                align = Paint.Align.CENTER,
                bold = true
            )
        }
    }

    // 3. Merkez ve Sağ: YRC Hız, Vites ve Fren/G-Kuvveti Barı
    drawHeroSpeedAndGearCube(
        centerX = w * 0.66f + shiftOffset.x,
        centerY = h * 0.55f + shiftOffset.y,
        spec = spec,
        subModeName = subModeName,
        speedVal = speedVal,
        unitStr = unitStr,
        telemetry = telemetry,
        fgColor = fgColor,
        accentColor = accentColor
    )
}

// =========================================================================================
// MİMARİ 3: DUCATI PANIGALE V4 TRACK EVO / BMW M1000RR / KTM 1390 — SHARP HORIZONTAL EVO
// =========================================================================================
private fun DrawScope.drawDucatiBmwTrackEvoCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
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
    val barLeft = w * 0.06f + shiftOffset.x
    val barWidth = w * 0.88f
    val barTop = h * 0.14f + shiftOffset.y
    val barHeight = 46f

    // 1. MotoGP / WSBK Yatay Şerit Takometre
    drawRoundRect(
        color = fgColor.copy(alpha = 0.12f),
        topLeft = Offset(barLeft, barTop),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color(0xFFFF1744).copy(alpha = 0.28f),
        topLeft = Offset(barLeft + barWidth * redlineRatio, barTop),
        size = Size(barWidth * (1f - redlineRatio), barHeight),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = barColor,
        topLeft = Offset(barLeft, barTop),
        size = Size(barWidth * fillRatio, barHeight),
        cornerRadius = CornerRadius(10f, 10f)
    )

    // RPM ölçek rakamları
    val maxK = (spec.gaugeMaxRpm / 1000.0).roundToInt().coerceIn(10, 18)
    for (k in 0..maxK step 2) {
        val ratio = k.toFloat() / maxK.toFloat()
        val tx = barLeft + barWidth * ratio
        drawNativeText(
            text = "$k",
            x = tx,
            y = barTop + barHeight + 22f,
            textSizePx = 18f,
            color = if (ratio >= redlineRatio) Color(0xFFFF1744) else fgColor.copy(alpha = 0.8f),
            align = Paint.Align.CENTER,
            bold = true
        )
    }

    // 2. Sol Kanat: Büyük Yarış Hızı & Lean Angle Kartı
    drawGhostAndActiveDigits(
        text = speedVal.toString(),
        x = w * 0.30f + shiftOffset.x,
        y = h * 0.58f + shiftOffset.y,
        textSizePx = 124f,
        activeColor = fgColor,
        showGhost = false
    )
    drawNativeText(
        text = "$unitStr  •  $subModeName",
        x = w * 0.30f + shiftOffset.x,
        y = h * 0.67f + shiftOffset.y,
        textSizePx = 20f,
        color = accentColor,
        align = Paint.Align.RIGHT,
        bold = true
    )
    drawNativeText(
        text = "LEAN: ${abs(telemetry.leanAngleDeg).roundToInt()}°   •   TRQ: ${telemetry.estimatedWheelTorqueNm.roundToInt()} Nm",
        x = w * 0.07f + shiftOffset.x,
        y = h * 0.78f + shiftOffset.y,
        textSizePx = 18f,
        color = fgColor.copy(alpha = 0.85f),
        align = Paint.Align.LEFT,
        bold = true
    )

    // 3. Merkez: Devasa Ducati Corse / BMW M-Sport Vites Kutusu ([ 3 ])
    val gearBoxX = w * 0.43f + shiftOffset.x
    val gearBoxY = h * 0.35f + shiftOffset.y
    val gearBoxW = w * 0.17f
    val gearBoxH = h * 0.44f
    val gearStr = if (telemetry.isNeutral || telemetry.currentGear == 0) "N" else telemetry.currentGear.toString()
    val gearBg = when {
        telemetry.isNeutral -> Color(0xFF00C853)
        telemetry.shiftLightState != ShiftLightState.NORMAL -> accentColor
        else -> fgColor.copy(alpha = 0.10f)
    }
    drawRoundRect(
        color = gearBg,
        topLeft = Offset(gearBoxX, gearBoxY),
        size = Size(gearBoxW, gearBoxH),
        cornerRadius = CornerRadius(18f, 18f)
    )
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(gearBoxX, gearBoxY),
        size = Size(gearBoxW, gearBoxH),
        cornerRadius = CornerRadius(18f, 18f),
        style = Stroke(width = 3.5f)
    )
    drawNativeText(
        text = "GEAR",
        x = gearBoxX + gearBoxW * 0.5f,
        y = gearBoxY + 28f,
        textSizePx = 16f,
        color = if (telemetry.isNeutral) Color.Black else accentColor,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = gearStr,
        x = gearBoxX + gearBoxW * 0.5f,
        y = gearBoxY + gearBoxH * 0.76f,
        textSizePx = 112f,
        color = if (telemetry.isNeutral) Color.Black else fgColor,
        align = Paint.Align.CENTER,
        bold = true
    )

    // 4. Sağ Kanat: Ducati Corse / BMW M-Sport Elektronik Destek Sütunu (DTC, DWC, DSC, EBC, DQS)
    val stackLeft = w * 0.65f + shiftOffset.x
    val stackTop = h * 0.35f + shiftOffset.y
    val stackItems = listOf(
        "DTC  (TRACTION)" to "LVL 2",
        "DWC  (WHEELIE)" to "LVL 1",
        "DSC  (SLIDE)" to "LVL 1",
        "EBC  (ENGINE BRK)" to "LVL 2",
        "DQS  (QUICKSHIFT)" to "UP/DW"
    )
    stackItems.forEachIndexed { idx, (label, value) ->
        val ry = stackTop + idx * 44f
        drawRoundRect(
            color = fgColor.copy(alpha = 0.08f),
            topLeft = Offset(stackLeft, ry),
            size = Size(w * 0.28f, 36f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawNativeText(
            text = label,
            x = stackLeft + 14f,
            y = ry + 24f,
            textSizePx = 15f,
            color = fgColor.copy(alpha = 0.78f),
            align = Paint.Align.LEFT,
            bold = true
        )
        drawNativeText(
            text = value,
            x = stackLeft + w * 0.28f - 14f,
            y = ry + 24f,
            textSizePx = 16f,
            color = accentColor,
            align = Paint.Align.RIGHT,
            bold = true
        )
    }
}

// =========================================================================================
// MİMARİ 4: HONDA CBR1000RR-R FIREBLADE SP / PANIGALE V2 / GSX-8S — CIRCULAR RING TFT
// =========================================================================================
private fun DrawScope.drawCircularRingTurbineCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
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
    val cx = w * 0.50f + shiftOffset.x
    val cy = h * 0.50f + shiftOffset.y
    val radius = min(w, h) * 0.34f
    val rectTopLeft = Offset(cx - radius, cy - radius)
    val arcSize = Size(radius * 2f, radius * 2f)

    // 1. Dairesel Türbin Halka Takometre
    drawArc(
        color = fgColor.copy(alpha = 0.13f),
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = rectTopLeft,
        size = arcSize,
        style = Stroke(width = 30f, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFFFF1744).copy(alpha = 0.32f),
        startAngle = 135f + 270f * redlineRatio,
        sweepAngle = 270f * (1f - redlineRatio),
        useCenter = false,
        topLeft = rectTopLeft,
        size = arcSize,
        style = Stroke(width = 30f, cap = StrokeCap.Butt)
    )
    drawArc(
        color = barColor,
        startAngle = 135f,
        sweepAngle = 270f * fillRatio,
        useCenter = false,
        topLeft = rectTopLeft,
        size = arcSize,
        style = Stroke(width = 30f, cap = StrokeCap.Round)
    )

    // Halka içi vites ve hız (Sıfır çakışma!)
    val gearStr = if (telemetry.isNeutral || telemetry.currentGear == 0) "N" else telemetry.currentGear.toString()
    drawNativeText(
        text = gearStr,
        x = cx,
        y = cy - 12f,
        textSizePx = 86f,
        color = if (telemetry.isNeutral) Color(0xFF00E676) else accentColor,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = "$speedVal",
        x = cx,
        y = cy + 68f,
        textSizePx = 76f,
        color = fgColor,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = "$unitStr • $subModeName",
        x = cx,
        y = cy + 96f,
        textSizePx = 16f,
        color = fgColor.copy(alpha = 0.72f),
        align = Paint.Align.CENTER,
        bold = true
    )

    // 2. Sol Kanat: Honda HRC / Yarış Parametre Halkaları (P / T / W / EB)
    val leftX = w * 0.14f + shiftOffset.x
    val hrcParams = listOf("P (POWER)" to "1", "T (HSTC)" to "2", "W (WHEELIE)" to "1", "EB (BRAKE)" to "2")
    hrcParams.forEachIndexed { idx, (label, level) ->
        val py = h * 0.28f + idx * 54f + shiftOffset.y
        drawCircle(color = accentColor.copy(alpha = 0.2f), radius = 20f, center = Offset(leftX, py))
        drawCircle(color = accentColor, radius = 20f, center = Offset(leftX, py), style = Stroke(width = 2.5f))
        drawNativeText(text = level, x = leftX, y = py + 7f, textSizePx = 19f, color = fgColor, align = Paint.Align.CENTER, bold = true)
        drawNativeText(text = label, x = leftX + 30f, y = py + 6f, textSizePx = 15f, color = fgColor.copy(alpha = 0.85f), align = Paint.Align.LEFT, bold = true)
    }

    // 3. Sağ Kanat: Telemetri Kartları (Eğim Açısı, Hararet, Güç)
    val rightX = w * 0.72f + shiftOffset.x
    drawTelemetryInfoCell(rightX, h * 0.26f + shiftOffset.y, w * 0.22f, h * 0.17f, "LEAN ANGLE", "${abs(telemetry.leanAngleDeg).roundToInt()}°", fgColor, accentColor)
    drawTelemetryInfoCell(rightX, h * 0.46f + shiftOffset.y, w * 0.22f, h * 0.17f, "COOLANT TEMP", "${telemetry.simulatedCoolantTempC}°C", fgColor, accentColor)
    drawTelemetryInfoCell(rightX, h * 0.66f + shiftOffset.y, w * 0.22f, h * 0.17f, "OUTPUT", "${telemetry.estimatedWheelHorsepower.roundToInt()} HP", fgColor, accentColor)
}

// =========================================================================================
// MİMARİ 5: TRIUMPH STREET TRIPLE 765 RS / APRILIA RSV4 — SYMMETRIC PINCER WING COCKPIT
// =========================================================================================
private fun DrawScope.drawTriumphApriliaPincerCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
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
    val segCount = 26
    val activeSegs = (fillRatio * segCount).roundToInt()
    val redlineSeg = (redlineRatio * segCount).roundToInt()

    // Sol ve Sağdan Merkeze Doğru Kıvrılan Simetrik Kanat Barları
    for (i in 0 until segCount) {
        val t = i.toFloat() / (segCount - 1).toFloat()
        val dx = w * (0.06f + t * 0.38f)
        val dy = h * (0.36f - sin(t * Math.PI * 0.65).toFloat() * 0.19f) + shiftOffset.y
        val lit = i < activeSegs
        val c = when {
            lit && i >= redlineSeg -> Color(0xFFFF1744)
            lit -> barColor
            i >= redlineSeg -> Color(0xFFFF1744).copy(alpha = 0.22f)
            else -> fgColor.copy(alpha = 0.13f)
        }
        // Sol kanat
        drawParallelogramSegment(dx + shiftOffset.x, dy, 16f, 34f, skewX = 10f, color = c)
        // Sağ kanat (simetrik)
        drawParallelogramSegment(w - dx - 16f + shiftOffset.x, dy, 16f, 34f, skewX = -10f, color = c)
    }

    drawHeroSpeedAndGearCube(
        centerX = w * 0.50f + shiftOffset.x,
        centerY = h * 0.57f + shiftOffset.y,
        spec = spec,
        subModeName = subModeName,
        speedVal = speedVal,
        unitStr = unitStr,
        telemetry = telemetry,
        fgColor = fgColor,
        accentColor = accentColor
    )
}

// =========================================================================================
// MİMARİ 6: SUPERSPORT ANALOG + YAN LCD PENCERESİ (YAMAHA R6, CBR600RR, GSX-R, ZX-6R, DAYTONA)
// =========================================================================================
private fun DrawScope.drawSupersportCentralAnalogHybridCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
    fillRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height

    // 1. SOL TARAF (%54): 3B Karbon/Metalik Analog Devir Saati (İbre göbeğinde hiçbir yazı çakışmaz!)
    val dialCx = w * 0.31f + shiftOffset.x
    val dialCy = h * 0.50f + shiftOffset.y
    val radius = min(w * 0.27f, h * 0.37f)

    // Dış metalik halka ve koyu kadran zemini
    drawCircle(color = Color(0xFF252B37), radius = radius * 1.07f, center = Offset(dialCx, dialCy))
    drawCircle(color = Color(0xFF0B0E14), radius = radius, center = Offset(dialCx, dialCy))
    drawCircle(color = accentColor.copy(alpha = 0.55f), radius = radius, center = Offset(dialCx, dialCy), style = Stroke(width = 4f))

    // Redline yay dilimi
    drawArc(
        color = Color(0xFFFF1744).copy(alpha = 0.38f),
        startAngle = 135f + 260f * redlineRatio,
        sweepAngle = 260f * (1f - redlineRatio),
        useCenter = false,
        topLeft = Offset(dialCx - radius * 0.89f, dialCy - radius * 0.89f),
        size = Size(radius * 1.78f, radius * 1.78f),
        style = Stroke(width = 16f)
    )

    // Majör ve Minör Kadran Çizgileri (Ticks)
    val maxRpmK = (spec.gaugeMaxRpm / 1000.0).roundToInt().coerceIn(10, 20)
    val subTicks = maxRpmK * 2
    for (s in 0..subTicks) {
        val t = s.toFloat() / subTicks.toFloat()
        val angleDeg = 135f + 260f * t
        val rad = Math.toRadians(angleDeg.toDouble())
        val isMajor = s % 2 == 0
        val innerR = if (isMajor) radius * 0.80f else radius * 0.87f
        val outerR = radius * 0.95f
        val tickColor = if (t >= redlineRatio) Color(0xFFFF1744) else Color.White
        drawLine(
            color = tickColor,
            start = Offset(dialCx + (innerR * cos(rad)).toFloat(), dialCy + (innerR * sin(rad)).toFloat()),
            end = Offset(dialCx + (outerR * cos(rad)).toFloat(), dialCy + (outerR * sin(rad)).toFloat()),
            strokeWidth = if (isMajor) 4.2f else 2.0f
        )
        if (isMajor) {
            val numR = radius * 0.66f
            val rpmDigit = s / 2
            drawNativeText(
                text = rpmDigit.toString(),
                x = dialCx + (numR * cos(rad)).toFloat(),
                y = dialCy + (numR * sin(rad)).toFloat() + 7f,
                textSizePx = 20f,
                color = tickColor,
                align = Paint.Align.CENTER,
                bold = true
            )
        }
    }

    // Kadran içi marka etiketi ve fiziksel Shift-Light merceği
    drawNativeText(
        text = spec.brand.uppercase(Locale.ROOT),
        x = dialCx,
        y = dialCy - radius * 0.28f,
        textSizePx = 18f,
        color = accentColor,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = "x1000 r/min",
        x = dialCx,
        y = dialCy + radius * 0.36f,
        textSizePx = 14f,
        color = Color.LightGray,
        align = Paint.Align.CENTER,
        bold = true
    )

    // Fiziksel İbre (Tapered Stepper Needle + Counterweight + Center Cap)
    val needleAngleDeg = 135f + 260f * fillRatio
    drawRealisticAnalogNeedle(
        cx = dialCx,
        cy = dialCy,
        length = radius * 0.90f,
        angleDeg = needleAngleDeg,
        needleColor = if (telemetry.shiftLightState != ShiftLightState.NORMAL) Color(0xFFFF1744) else accentColor
    )

    // 2. SAĞ TARAF (%42): Kalıplanmış Dijital LCD Penceresi (888 Hayalet Segmentli Hız, Vites, Hararet & Yakıt)
    val lcdLeft = w * 0.58f + shiftOffset.x
    val lcdTop = h * 0.16f + shiftOffset.y
    val lcdW = w * 0.36f
    val lcdH = h * 0.66f

    // LCD Dış Çerçeve ve Polarize Cam Zemini
    drawRoundRect(
        color = Color(0xFF1A202C),
        topLeft = Offset(lcdLeft - 6f, lcdTop - 6f),
        size = Size(lcdW + 12f, lcdH + 12f),
        cornerRadius = CornerRadius(20f, 20f)
    )
    drawRoundRect(
        color = Color(0xFF0E1722),
        topLeft = Offset(lcdLeft, lcdTop),
        size = Size(lcdW, lcdH),
        cornerRadius = CornerRadius(16f, 16f)
    )
    drawRoundRect(
        color = accentColor.copy(alpha = 0.55f),
        topLeft = Offset(lcdLeft, lcdTop),
        size = Size(lcdW, lcdH),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 2.5f)
    )

    // LCD Üst Bilgi: Model & Sürüş Modu
    drawNativeText(
        text = "${spec.modelName} • $subModeName",
        x = lcdLeft + 18f,
        y = lcdTop + 30f,
        textSizePx = 15f,
        color = accentColor,
        align = Paint.Align.LEFT,
        bold = true
    )

    // LCD Vites Kutusu (Sağ Üst Köşe)
    val gearStr = if (telemetry.isNeutral || telemetry.currentGear == 0) "N" else telemetry.currentGear.toString()
    val gearBoxX = lcdLeft + lcdW - 88f
    val gearBoxY = lcdTop + 16f
    drawRoundRect(
        color = if (telemetry.isNeutral) Color(0xFF00E676) else Color(0xFF1A2636),
        topLeft = Offset(gearBoxX, gearBoxY),
        size = Size(70f, 76f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawNativeText(
        text = "GEAR",
        x = gearBoxX + 35f,
        y = gearBoxY + 18f,
        textSizePx = 12f,
        color = if (telemetry.isNeutral) Color.Black else Color.LightGray,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = gearStr,
        x = gearBoxX + 35f,
        y = gearBoxY + 66f,
        textSizePx = 48f,
        color = if (telemetry.isNeutral) Color.Black else Color.White,
        align = Paint.Align.CENTER,
        bold = true
    )

    // LCD 7-Segment Hız Okuması (888 Hayalet Rakamlarıyla!)
    drawGhostAndActiveDigits(
        text = speedVal.toString(),
        x = lcdLeft + lcdW * 0.64f,
        y = lcdTop + lcdH * 0.58f,
        textSizePx = 104f,
        activeColor = Color(0xFFE8F5FF),
        showGhost = true
    )
    drawNativeText(
        text = unitStr,
        x = lcdLeft + lcdW * 0.66f,
        y = lcdTop + lcdH * 0.58f,
        textSizePx = 22f,
        color = accentColor,
        align = Paint.Align.LEFT,
        bold = true
    )

    // LCD Alt Telemetri: Hararet, Gerçek RPM ve Yakıt Barı
    drawNativeText(
        text = "TEMP: ${telemetry.simulatedCoolantTempC}°C   •   RPM: ${telemetry.actualEngineRpm.roundToInt()}",
        x = lcdLeft + 18f,
        y = lcdTop + lcdH - 46f,
        textSizePx = 16f,
        color = Color(0xFFB0C7DE),
        align = Paint.Align.LEFT,
        bold = true
    )
    drawLcdSegmentedBar(
        x = lcdLeft + 18f,
        y = lcdTop + lcdH - 28f,
        w = lcdW - 36f,
        h = 14f,
        segments = 10,
        fillRatio = (telemetry.simulatedFuelLevelPercent / 100.0).toFloat(),
        activeColor = accentColor
    )
}

// =========================================================================================
// MİMARİ 7: SUZUKI HAYABUSA (GEN 1/2/3) — 5 KADRANLI SİMETRİK HİBRİT KOKPİT
// =========================================================================================
private fun DrawScope.drawHayabusaFiveDialCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
    fillRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height
    val cy = h * 0.50f + shiftOffset.y

    val mainR = min(w * 0.165f, h * 0.30f)
    val sideR = mainR * 0.62f

    // 1. En Sol Küçük Kadran: FUEL (Yakıt)
    val fuelCx = w * 0.095f + shiftOffset.x
    drawMiniAnalogDial(
        cx = fuelCx,
        cy = cy + 20f,
        radius = sideR,
        title = "FUEL",
        leftLabel = "E",
        rightLabel = "F",
        ratio = (telemetry.simulatedFuelLevelPercent / 100.0).toFloat(),
        accentColor = accentColor
    )

    // 2. İç Sol Büyük Kadran: TACHOMETER (x1000 r/min)
    val rpmCx = w * 0.285f + shiftOffset.x
    drawCircle(color = Color(0xFF282218), radius = mainR * 1.07f, center = Offset(rpmCx, cy))
    drawCircle(color = Color(0xFF0A0D12), radius = mainR, center = Offset(rpmCx, cy))
    drawCircle(color = accentColor, radius = mainR, center = Offset(rpmCx, cy), style = Stroke(width = 4f))
    val maxRpmK = (spec.gaugeMaxRpm / 1000.0).roundToInt().coerceIn(10, 14)
    for (i in 0..maxRpmK) {
        val t = i.toFloat() / maxRpmK.toFloat()
        val ang = Math.toRadians((135f + 260f * t).toDouble())
        val tc = if (t >= redlineRatio) Color(0xFFFF1744) else Color.White
        drawLine(
            color = tc,
            start = Offset(rpmCx + (mainR * 0.80f * cos(ang)).toFloat(), cy + (mainR * 0.80f * sin(ang)).toFloat()),
            end = Offset(rpmCx + (mainR * 0.95f * cos(ang)).toFloat(), cy + (mainR * 0.95f * sin(ang)).toFloat()),
            strokeWidth = 3.5f
        )
        drawNativeText(
            text = "$i",
            x = rpmCx + (mainR * 0.65f * cos(ang)).toFloat(),
            y = cy + (mainR * 0.65f * sin(ang)).toFloat() + 6f,
            textSizePx = 16f,
            color = tc,
            align = Paint.Align.CENTER,
            bold = true
        )
    }
    drawNativeText("x1000 r/min", rpmCx, cy + mainR * 0.42f, 12f, Color.LightGray, Paint.Align.CENTER, true)
    drawRealisticAnalogNeedle(rpmCx, cy, mainR * 0.88f, 135f + 260f * fillRatio, barColor)

    // 3. Merkez: Hayabusa SDMS-a TFT Kulesi (Vites, Dijital Hız, Lean Angle, SDMS-a Modları)
    val tftW = w * 0.17f
    val tftH = h * 0.62f
    val tftLeft = w * 0.50f - tftW * 0.5f + shiftOffset.x
    val tftTop = h * 0.18f + shiftOffset.y
    drawRoundRect(
        color = Color(0xFF0E131D),
        topLeft = Offset(tftLeft, tftTop),
        size = Size(tftW, tftH),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(tftLeft, tftTop),
        size = Size(tftW, tftH),
        cornerRadius = CornerRadius(14f, 14f),
        style = Stroke(width = 2.5f)
    )
    drawNativeText("SDMS-α • $subModeName", w * 0.50f + shiftOffset.x, tftTop + 24f, 14f, accentColor, Paint.Align.CENTER, true)
    val gearStr = if (telemetry.isNeutral || telemetry.currentGear == 0) "N" else telemetry.currentGear.toString()
    drawNativeText(
        text = gearStr,
        x = w * 0.50f + shiftOffset.x,
        y = tftTop + 98f,
        textSizePx = 74f,
        color = if (telemetry.isNeutral) Color(0xFF00E676) else Color.White,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = "$speedVal $unitStr",
        x = w * 0.50f + shiftOffset.x,
        y = tftTop + 145f,
        textSizePx = 30f,
        color = accentColor,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = "PW:1  TC:2  LF:1",
        x = w * 0.50f + shiftOffset.x,
        y = tftTop + tftH - 44f,
        textSizePx = 13f,
        color = Color.LightGray,
        align = Paint.Align.CENTER,
        bold = true
    )
    drawNativeText(
        text = "LEAN ${abs(telemetry.leanAngleDeg).roundToInt()}°",
        x = w * 0.50f + shiftOffset.x,
        y = tftTop + tftH - 18f,
        textSizePx = 15f,
        color = Color(0xFF00E5FF),
        align = Paint.Align.CENTER,
        bold = true
    )

    // 4. İç Sağ Büyük Kadran: SPEEDOMETER (0..300 km/h)
    val spdCx = w * 0.715f + shiftOffset.x
    drawCircle(color = Color(0xFF282218), radius = mainR * 1.07f, center = Offset(spdCx, cy))
    drawCircle(color = Color(0xFF0A0D12), radius = mainR, center = Offset(spdCx, cy))
    drawCircle(color = accentColor, radius = mainR, center = Offset(spdCx, cy), style = Stroke(width = 4f))
    for (spdStep in 0..300 step 30) {
        val t = spdStep / 300f
        val ang = Math.toRadians((135f + 260f * t).toDouble())
        drawLine(
            color = Color.White,
            start = Offset(spdCx + (mainR * 0.80f * cos(ang)).toFloat(), cy + (mainR * 0.80f * sin(ang)).toFloat()),
            end = Offset(spdCx + (mainR * 0.95f * cos(ang)).toFloat(), cy + (mainR * 0.95f * sin(ang)).toFloat()),
            strokeWidth = 3.5f
        )
        drawNativeText(
            text = "$spdStep",
            x = spdCx + (mainR * 0.63f * cos(ang)).toFloat(),
            y = cy + (mainR * 0.63f * sin(ang)).toFloat() + 5f,
            textSizePx = 14f,
            color = Color.White,
            align = Paint.Align.CENTER,
            bold = true
        )
    }
    drawNativeText(unitStr, spdCx, cy + mainR * 0.42f, 13f, Color.LightGray, Paint.Align.CENTER, true)
    val spdRatio = (telemetry.displayedSpeedKmh / 300.0).toFloat().coerceIn(0f, 1f)
    drawRealisticAnalogNeedle(spdCx, cy, mainR * 0.88f, 135f + 260f * spdRatio, accentColor)

    // 5. En Sağ Küçük Kadran: COOLANT TEMP (Hararet)
    val tempCx = w * 0.905f + shiftOffset.x
    val tempRatio = ((telemetry.simulatedCoolantTempC - 40.0) / 80.0).toFloat().coerceIn(0f, 1f)
    drawMiniAnalogDial(
        cx = tempCx,
        cy = cy + 20f,
        radius = sideR,
        title = "TEMP",
        leftLabel = "C",
        rightLabel = "H",
        ratio = tempRatio,
        accentColor = accentColor
    )
}

private fun DrawScope.drawMiniAnalogDial(
    cx: Float,
    cy: Float,
    radius: Float,
    title: String,
    leftLabel: String,
    rightLabel: String,
    ratio: Float,
    accentColor: Color
) {
    drawCircle(color = Color(0xFF0B0E14), radius = radius, center = Offset(cx, cy))
    drawCircle(color = accentColor.copy(alpha = 0.65f), radius = radius, center = Offset(cx, cy), style = Stroke(width = 3f))
    drawNativeText(title, cx, cy - radius * 0.35f, 12f, Color.LightGray, Paint.Align.CENTER, true)
    drawNativeText(leftLabel, cx - radius * 0.62f, cy + radius * 0.45f, 13f, Color.White, Paint.Align.CENTER, true)
    drawNativeText(rightLabel, cx + radius * 0.62f, cy + radius * 0.45f, 13f, Color(0xFFFF1744), Paint.Align.CENTER, true)
    val ang = 150f + 240f * ratio.coerceIn(0f, 1f)
    drawRealisticAnalogNeedle(cx, cy, radius * 0.82f, ang, accentColor)
}

// =========================================================================================
// MİMARİ 8: ÇİFT KLASİK KROM ANALOG KUPA (BMW R nineT, BONNEVILLE T120, ROYAL ENFIELD GT 650)
// =========================================================================================
private fun DrawScope.drawDualClassicChromeAnalogCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
    fillRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height
    val cy = h * 0.49f + shiftOffset.y
    val cupR = min(w * 0.22f, h * 0.35f)
    val leftCx = w * 0.28f + shiftOffset.x
    val rightCx = w * 0.72f + shiftOffset.x

    // Sol Kupa: Klasik Hız Kadranı + İç Inset LCD
    drawChromeAnalogCup(
        cx = leftCx,
        cy = cy,
        radius = cupR,
        maxScale = 240,
        step = 20,
        unitLabel = unitStr,
        needleRatio = (telemetry.displayedSpeedKmh / 240.0).toFloat().coerceIn(0f, 1f),
        insetLine1 = "$speedVal $unitStr",
        insetLine2 = "ODO 1420 km",
        accentColor = accentColor
    )

    // Merkez Heritage Rozeti
    drawNativeText(
        text = spec.brand.uppercase(Locale.ROOT),
        x = w * 0.50f + shiftOffset.x,
        y = cy - 18f,
        textSizePx = 22f,
        color = accentColor,
        align = Paint.Align.CENTER,
        bold = true
    )
    val gearStr = if (telemetry.isNeutral || telemetry.currentGear == 0) "N" else telemetry.currentGear.toString()
    drawRoundRect(
        color = if (telemetry.isNeutral) Color(0xFF00C853) else Color(0xFF1A202C),
        topLeft = Offset(w * 0.50f - 34f + shiftOffset.x, cy + 4f),
        size = Size(68f, 68f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawNativeText(
        text = gearStr,
        x = w * 0.50f + shiftOffset.x,
        y = cy + 54f,
        textSizePx = 46f,
        color = if (telemetry.isNeutral) Color.Black else Color.White,
        align = Paint.Align.CENTER,
        bold = true
    )

    // Sağ Kupa: Klasik Devir Kadranı (RPM) + İç Inset LCD
    val maxRpmK = (spec.gaugeMaxRpm / 1000.0).roundToInt().coerceIn(8, 14)
    drawChromeAnalogCup(
        cx = rightCx,
        cy = cy,
        radius = cupR,
        maxScale = maxRpmK,
        step = 1,
        unitLabel = "x1000 r/min",
        needleRatio = fillRatio,
        insetLine1 = "${telemetry.actualEngineRpm.roundToInt()} RPM",
        insetLine2 = "GEAR $gearStr",
        accentColor = barColor
    )
}

private fun DrawScope.drawChromeAnalogCup(
    cx: Float,
    cy: Float,
    radius: Float,
    maxScale: Int,
    step: Int,
    unitLabel: String,
    needleRatio: Float,
    insetLine1: String,
    insetLine2: String,
    accentColor: Color
) {
    // Çift katmanlı fırçalanmış krom çerçeve
    drawCircle(color = Color(0xFF8C96A6), radius = radius * 1.08f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF2B313B), radius = radius * 1.03f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF0E1117), radius = radius, center = Offset(cx, cy))

    var s = 0
    while (s <= maxScale) {
        val t = s.toFloat() / maxScale.toFloat()
        val ang = Math.toRadians((140f + 260f * t).toDouble())
        drawLine(
            color = Color(0xFFEAE6DF),
            start = Offset(cx + (radius * 0.82f * cos(ang)).toFloat(), cy + (radius * 0.82f * sin(ang)).toFloat()),
            end = Offset(cx + (radius * 0.95f * cos(ang)).toFloat(), cy + (radius * 0.95f * sin(ang)).toFloat()),
            strokeWidth = 3f
        )
        drawNativeText(
            text = "$s",
            x = cx + (radius * 0.67f * cos(ang)).toFloat(),
            y = cy + (radius * 0.67f * sin(ang)).toFloat() + 6f,
            textSizePx = 15f,
            color = Color(0xFFEAE6DF),
            align = Paint.Align.CENTER,
            bold = true
        )
        s += step
    }

    drawNativeText(unitLabel, cx, cy - radius * 0.30f, 13f, Color.LightGray, Paint.Align.CENTER, true)

    // Alt Inset LCD Penceresi
    val lcdW = radius * 0.92f
    val lcdH = radius * 0.36f
    drawRoundRect(
        color = Color(0xFF18222D),
        topLeft = Offset(cx - lcdW * 0.5f, cy + radius * 0.25f),
        size = Size(lcdW, lcdH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawNativeText(insetLine1, cx, cy + radius * 0.44f, 16f, Color(0xFFE0F7FA), Paint.Align.CENTER, true)
    drawNativeText(insetLine2, cx, cy + radius * 0.56f, 12f, accentColor, Paint.Align.CENTER, true)

    drawRealisticAnalogNeedle(cx, cy, radius * 0.86f, 140f + 260f * needleRatio, accentColor)
}

// =========================================================================================
// MİMARİ 9: BAUHAUS / SCRAMBLER / CRUISER TEK DAİRESEL PUCK (HUSQVARNA, HIMALAYAN 450, HARLEY)
// =========================================================================================
private fun DrawScope.drawSingleRoundPuckCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
    fillRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f + shiftOffset.x
    val cy = h * 0.49f + shiftOffset.y
    val puckR = min(w, h) * 0.37f

    drawCircle(color = Color(0xFF202530), radius = puckR * 1.08f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF0B0E14), radius = puckR, center = Offset(cx, cy))
    drawCircle(color = accentColor, radius = puckR, center = Offset(cx, cy), style = Stroke(width = 3.5f))

    // Radyal Çevre Segmentleri
    val segs = 40
    val litSegs = (fillRatio * segs).roundToInt()
    val redSeg = (redlineRatio * segs).roundToInt()
    for (i in 0 until segs) {
        val t = i.toFloat() / (segs - 1).toFloat()
        val ang = Math.toRadians((135f + 270f * t).toDouble())
        val c = when {
            i < litSegs && i >= redSeg -> Color(0xFFFF1744)
            i < litSegs -> barColor
            else -> Color.White.copy(alpha = 0.14f)
        }
        drawLine(
            color = c,
            start = Offset(cx + (puckR * 0.82f * cos(ang)).toFloat(), cy + (puckR * 0.82f * sin(ang)).toFloat()),
            end = Offset(cx + (puckR * 0.95f * cos(ang)).toFloat(), cy + (puckR * 0.95f * sin(ang)).toFloat()),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )
    }

    val gearStr = if (telemetry.isNeutral || telemetry.currentGear == 0) "N" else telemetry.currentGear.toString()
    drawNativeText(spec.brand.uppercase(Locale.ROOT), cx, cy - puckR * 0.42f, 18f, accentColor, Paint.Align.CENTER, true)
    drawNativeText("$speedVal", cx, cy + 18f, 92f, Color.White, Paint.Align.CENTER, true)
    drawNativeText("$unitStr  •  GEAR $gearStr", cx, cy + 54f, 20f, accentColor, Paint.Align.CENTER, true)
    drawNativeText("${telemetry.actualEngineRpm.roundToInt()} RPM", cx, cy + 84f, 16f, Color.LightGray, Paint.Align.CENTER, true)
}

// =========================================================================================
// MİMARİ 10: POLARİZE 7-SEGMENT LCD KOKPİT (MT-07, Z1000, GSX-R1000 L7, KTM GEN1, R25/R3, S2000)
// =========================================================================================
private fun DrawScope.drawPolarizedSegmentLcdCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
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

    // 1. Üst Kavisli Tuğla Segmentli LCD Devir Barı
    val totalBricks = 48
    val litBricks = (fillRatio * totalBricks).roundToInt()
    val peakBrick = (peakRatio * totalBricks).roundToInt()
    val redBrick = (redlineRatio * totalBricks).roundToInt()

    for (i in 0 until totalBricks) {
        val t = i.toFloat() / (totalBricks - 1).toFloat()
        val bx = w * (0.07f + t * 0.86f) + shiftOffset.x
        val by = h * (0.24f - sin(t * Math.PI * 0.85).toFloat() * 0.09f) + shiftOffset.y
        val isLit = i < litBricks || (spec.hasPeakHoldTrail && i == peakBrick && peakBrick > litBricks)
        val c = when {
            isLit && i >= redBrick -> Color(0xFFFF1744)
            isLit -> fgColor
            else -> fgColor.copy(alpha = 0.11f) // LCD Hayalet Segmenti!
        }
        drawRect(
            color = c,
            topLeft = Offset(bx, by),
            size = Size((w * 0.86f / totalBricks) * 0.75f, 34f)
        )
        if (i % 8 == 0) {
            val rpmNum = ((t * spec.gaugeMaxRpm) / 1000.0).roundToInt()
            drawNativeText("$rpmNum", bx + 6f, by + 54f, 17f, fgColor.copy(alpha = 0.85f), Paint.Align.CENTER, true)
        }
    }

    // 2. Sol LCD Yakıt & Hararet Kulesi
    val leftX = w * 0.08f + shiftOffset.x
    drawNativeText("FUEL", leftX, h * 0.44f + shiftOffset.y, 16f, fgColor, Paint.Align.LEFT, true)
    drawLcdSegmentedBar(leftX, h * 0.46f + shiftOffset.y, w * 0.18f, 20f, 8, (telemetry.simulatedFuelLevelPercent / 100.0).toFloat(), fgColor)
    drawNativeText("TEMP ${telemetry.simulatedCoolantTempC}°C", leftX, h * 0.60f + shiftOffset.y, 18f, fgColor, Paint.Align.LEFT, true)
    drawLcdSegmentedBar(leftX, h * 0.62f + shiftOffset.y, w * 0.18f, 20f, 8, 0.62f, fgColor)

    // 3. Merkez 7-Segment Hız Okuması (Gerçek `888` Hayalet Katmanlı!)
    drawGhostAndActiveDigits(
        text = speedVal.toString(),
        x = w * 0.58f + shiftOffset.x,
        y = h * 0.66f + shiftOffset.y,
        textSizePx = 136f,
        activeColor = fgColor,
        showGhost = true
    )
    drawNativeText(unitStr, w * 0.60f + shiftOffset.x, h * 0.66f + shiftOffset.y, 26f, fgColor, Paint.Align.LEFT, true)

    // 4. Sağ LCD Vites Kutusu
    val gearBoxX = w * 0.76f + shiftOffset.x
    val gearBoxY = h * 0.38f + shiftOffset.y
    val gearStr = if (telemetry.isNeutral || telemetry.currentGear == 0) "N" else telemetry.currentGear.toString()
    drawRoundRect(
        color = fgColor.copy(alpha = 0.10f),
        topLeft = Offset(gearBoxX, gearBoxY),
        size = Size(w * 0.15f, h * 0.36f),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = fgColor,
        topLeft = Offset(gearBoxX, gearBoxY),
        size = Size(w * 0.15f, h * 0.36f),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 3f)
    )
    drawNativeText("GEAR", gearBoxX + w * 0.075f, gearBoxY + 26f, 15f, fgColor, Paint.Align.CENTER, true)
    drawNativeText(gearStr, gearBoxX + w * 0.075f, gearBoxY + h * 0.29f, 92f, fgColor, Paint.Align.CENTER, true)
}

// =========================================================================================
// MİMARİ 11: DAKAR / ADVENTURE RALLY TOWER (YAMAHA TÉNÉRÉ 700, AFRICA TWIN, TUAREG 660)
// =========================================================================================
private fun DrawScope.drawDakarRallyTowerCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
    fillRatio: Float,
    redlineRatio: Float,
    fgColor: Color,
    barColor: Color,
    accentColor: Color,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height

    // 1. Üst Dakar Pusula Şeridi (CAP Heading Tape)
    val tapeLeft = w * 0.15f + shiftOffset.x
    val tapeTop = h * 0.13f + shiftOffset.y
    val tapeW = w * 0.70f
    drawRoundRect(
        color = fgColor.copy(alpha = 0.10f),
        topLeft = Offset(tapeLeft, tapeTop),
        size = Size(tapeW, 38f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawNativeText(
        text = "◄ NW  •  330°  •  [ CAP 000° N ]  •  030°  •  NE ►",
        x = w * 0.50f + shiftOffset.x,
        y = tapeTop + 26f,
        textSizePx = 18f,
        color = accentColor,
        align = Paint.Align.CENTER,
        bold = true
    )

    // 2. Sol Roadbook Tripmeter Kartları
    val leftX = w * 0.07f + shiftOffset.x
    drawTelemetryInfoCell(leftX, h * 0.26f + shiftOffset.y, w * 0.24f, h * 0.22f, "RALLY TRIP 1", "142.8 km", fgColor, accentColor)
    drawTelemetryInfoCell(leftX, h * 0.52f + shiftOffset.y, w * 0.24f, h * 0.22f, "INCLINOMETER", "PITCH ${telemetry.pitchAngleDeg.roundToInt()}° / ROLL ${telemetry.leanAngleDeg.roundToInt()}°", fgColor, accentColor)

    // 3. Merkez-Sağ: Rally Hız, Vites & Dikey RPM Barı
    drawHeroSpeedAndGearCube(
        centerX = w * 0.64f + shiftOffset.x,
        centerY = h * 0.52f + shiftOffset.y,
        spec = spec,
        subModeName = subModeName,
        speedVal = speedVal,
        unitStr = unitStr,
        telemetry = telemetry,
        fgColor = fgColor,
        accentColor = accentColor
    )
}

// =========================================================================================
// MİMARİ 12: PANORAMİK BÖLÜNMÜŞ TOURING EKRANI (BMW R1300GS, MULTISTRADA V4, TIGER 1200)
// =========================================================================================
private fun DrawScope.drawTouringPanoramicSplitCockpit(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
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

    // Sol %58: Panoramik Takometre Barı + Büyük Hız & Vites
    val barLeft = w * 0.06f + shiftOffset.x
    val barW = w * 0.52f
    val barTop = h * 0.15f + shiftOffset.y
    drawRoundRect(color = fgColor.copy(alpha = 0.12f), topLeft = Offset(barLeft, barTop), size = Size(barW, 38f), cornerRadius = CornerRadius(8f, 8f))
    drawRoundRect(color = barColor, topLeft = Offset(barLeft, barTop), size = Size(barW * fillRatio, 38f), cornerRadius = CornerRadius(8f, 8f))

    drawHeroSpeedAndGearCube(
        centerX = w * 0.34f + shiftOffset.x,
        centerY = h * 0.54f + shiftOffset.y,
        spec = spec,
        subModeName = subModeName,
        speedVal = speedVal,
        unitStr = unitStr,
        telemetry = telemetry,
        fgColor = fgColor,
        accentColor = accentColor
    )

    // Sağ %36: Touring Yol Bilgisayarı & TPMS Lastik Basıncı Kartı
    val cardLeft = w * 0.62f + shiftOffset.x
    val cardTop = h * 0.15f + shiftOffset.y
    val cardW = w * 0.32f
    val cardH = h * 0.64f
    drawRoundRect(color = fgColor.copy(alpha = 0.07f), topLeft = Offset(cardLeft, cardTop), size = Size(cardW, cardH), cornerRadius = CornerRadius(16f, 16f))
    drawRoundRect(color = accentColor.copy(alpha = 0.5f), topLeft = Offset(cardLeft, cardTop), size = Size(cardW, cardH), cornerRadius = CornerRadius(16f, 16f), style = Stroke(width = 2f))

    drawNativeText("TOURING TELEMETRY", cardLeft + 18f, cardTop + 30f, 16f, accentColor, Paint.Align.LEFT, true)
    drawNativeText("TPMS FRONT: 2.5 BAR  •  REAR: 2.9 BAR", cardLeft + 18f, cardTop + 66f, 15f, fgColor, Paint.Align.LEFT, true)
    drawNativeText("COOLANT: ${telemetry.simulatedCoolantTempC}°C  •  RANGE: 315 KM", cardLeft + 18f, cardTop + 102f, 15f, fgColor, Paint.Align.LEFT, true)
    drawNativeText("ENGINE: ${telemetry.actualEngineRpm.roundToInt()} RPM", cardLeft + 18f, cardTop + 138f, 18f, accentColor, Paint.Align.LEFT, true)
    drawNativeText("POWER: ${telemetry.estimatedWheelHorsepower.roundToInt()} HP / ${telemetry.estimatedWheelTorqueNm.roundToInt()} Nm", cardLeft + 18f, cardTop + 174f, 15f, fgColor, Paint.Align.LEFT, true)
}

// =========================================================================================
// ORTAK YARDIMCI BİLEŞENLER: HERO SPEED & GEAR CUBE, 3B ANALOG İBRE, 888 GHOST, TEKER METRE
// =========================================================================================
private fun DrawScope.drawHeroSpeedAndGearCube(
    centerX: Float,
    centerY: Float,
    spec: DeclarativeGaugeSpec,
    subModeName: String,
    speedVal: Int,
    unitStr: String,
    telemetry: UnifiedTelemetryState,
    fgColor: Color,
    accentColor: Color
) {
    // Hız Okuması
    drawGhostAndActiveDigits(
        text = speedVal.toString(),
        x = centerX - 24f,
        y = centerY + 26f,
        textSizePx = 122f,
        activeColor = fgColor,
        showGhost = false
    )
    drawNativeText(
        text = unitStr + (if (telemetry.is299LockEngaged) " [299]" else ""),
        x = centerX - 24f,
        y = centerY + 62f,
        textSizePx = 22f,
        color = fgColor.copy(alpha = 0.78f),
        align = Paint.Align.RIGHT,
        bold = true
    )

    // Vites Küpü (Sağda)
    val gearBoxX = centerX + 12f
    val gearBoxY = centerY - 78f
    val gearW = 114f
    val gearH = 136f
    val gearBg = when {
        telemetry.isNeutral -> Color(0xFF00C853)
        telemetry.shiftLightState == ShiftLightState.STAGE_2_SHIFT_NOW ||
            telemetry.shiftLightState == ShiftLightState.OVER_REV_LIMITER ->
            if (telemetry.shiftFlashPhaseOn) Color(spec.stage2InvertBgColorHex) else accentColor.copy(alpha = 0.28f)
        telemetry.shiftLightState == ShiftLightState.STAGE_1_APPROACHING ->
            Color(spec.stage1ShiftColorHex)
        else -> fgColor.copy(alpha = 0.11f)
    }
    val gearFg = when {
        telemetry.isNeutral -> Color(0xFF05140A)
        telemetry.shiftLightState != ShiftLightState.NORMAL -> Color.White
        else -> fgColor
    }

    drawRoundRect(
        color = gearBg,
        topLeft = Offset(gearBoxX, gearBoxY),
        size = Size(gearW, gearH),
        cornerRadius = CornerRadius(18f, 18f)
    )
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(gearBoxX, gearBoxY),
        size = Size(gearW, gearH),
        cornerRadius = CornerRadius(18f, 18f),
        style = Stroke(width = 3f)
    )
    drawNativeText("GEAR", gearBoxX + gearW * 0.5f, gearBoxY + 24f, 14f, gearFg.copy(alpha = 0.8f), Paint.Align.CENTER, true)
    val gearText = if (telemetry.isNeutral || telemetry.currentGear == 0) "N" else telemetry.currentGear.toString()
    drawNativeText(gearText, gearBoxX + gearW * 0.5f, gearBoxY + gearH - 22f, 88f, gearFg, Paint.Align.CENTER, true)

    // Sürüş Modu Rozeti
    drawRoundRect(
        color = accentColor.copy(alpha = 0.18f),
        topLeft = Offset(gearBoxX, gearBoxY + gearH + 10f),
        size = Size(gearW, 32f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawNativeText(
        text = subModeName.uppercase(Locale.ROOT),
        x = gearBoxX + gearW * 0.5f,
        y = gearBoxY + gearH + 32f,
        textSizePx = 15f,
        color = accentColor,
        align = Paint.Align.CENTER,
        bold = true
    )
}

private fun DrawScope.drawRealisticAnalogNeedle(
    cx: Float,
    cy: Float,
    length: Float,
    angleDeg: Float,
    needleColor: Color
) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val perpRad = rad + Math.PI * 0.5
    val tipX = cx + (length * cos(rad)).toFloat()
    val tipY = cy + (length * sin(rad)).toFloat()
    val tailX = cx - (length * 0.22f * cos(rad)).toFloat()
    val tailY = cy - (length * 0.22f * sin(rad)).toFloat()
    val halfBase = 7.5f

    val path = Path().apply {
        moveTo(tipX, tipY)
        lineTo(tailX + (halfBase * cos(perpRad)).toFloat(), tailY + (halfBase * sin(perpRad)).toFloat())
        lineTo(tailX - (halfBase * cos(perpRad)).toFloat(), tailY - (halfBase * sin(perpRad)).toFloat())
        close()
    }
    // İbre gölgesi ve gövdesi
    drawPath(path = path, color = needleColor)
    // Merkez fırçalanmış alüminyum göbek kapağı
    drawCircle(color = Color(0xFF2B313D), radius = 19f, center = Offset(cx, cy))
    drawCircle(color = needleColor, radius = 9f, center = Offset(cx, cy))
}

private fun DrawScope.drawParallelogramSegment(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    skewX: Float,
    color: Color
) {
    val p = Path().apply {
        moveTo(x + skewX, y)
        lineTo(x + w + skewX, y)
        lineTo(x + w, y + h)
        lineTo(x, y + h)
        close()
    }
    drawPath(path = p, color = color)
}

private fun DrawScope.drawTelemetryInfoCell(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    title: String,
    value: String,
    fgColor: Color,
    accentColor: Color
) {
    drawRoundRect(
        color = fgColor.copy(alpha = 0.07f),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = accentColor.copy(alpha = 0.35f),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = 1.5f)
    )
    drawNativeText(title, x + 14f, y + 24f, 13f, accentColor, Paint.Align.LEFT, true)
    drawNativeText(value, x + 14f, y + h - 16f, 24f, fgColor, Paint.Align.LEFT, true)
}

private fun DrawScope.drawGhostAndActiveDigits(
    text: String,
    x: Float,
    y: Float,
    textSizePx: Float,
    activeColor: Color,
    showGhost: Boolean
) {
    if (showGhost) {
        drawNativeText(
            text = "888",
            x = x,
            y = y,
            textSizePx = textSizePx,
            color = activeColor.copy(alpha = 0.09f),
            align = Paint.Align.RIGHT,
            bold = true
        )
    }
    drawNativeText(
        text = text,
        x = x,
        y = y,
        textSizePx = textSizePx,
        color = activeColor,
        align = Paint.Align.RIGHT,
        bold = true
    )
}

private fun DrawScope.drawLcdSegmentedBar(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    segments: Int,
    fillRatio: Float,
    activeColor: Color
) {
    val lit = (fillRatio.coerceIn(0f, 1f) * segments).roundToInt()
    val segW = (w / segments) * 0.78f
    val step = w / segments
    for (i in 0 until segments) {
        drawRect(
            color = if (i < lit) activeColor else activeColor.copy(alpha = 0.12f),
            topLeft = Offset(x + i * step, y),
            size = Size(segW, h)
        )
    }
}

/**
 * Kompakt Teker Derece Ölçer & Teker Metre (Wheelie Analyzer Banner)
 */
private fun DrawScope.drawCompactWheelieAnalyzerBanner(
    telemetry: UnifiedTelemetryState,
    fgColor: Color,
    accentColor: Color,
    language: AppLanguage,
    shiftOffset: Offset
) {
    val w = size.width
    val h = size.height
    val boxW = w * 0.42f
    val boxH = 54f
    val left = w * 0.50f - boxW * 0.5f + shiftOffset.x
    val top = h * 0.80f + shiftOffset.y

    val statusColor = when {
        telemetry.wheelieAngleDeg >= 42.0 -> Color(0xFFFF1744)
        telemetry.isWheelieActive -> Color(0xFF00E676)
        else -> accentColor
    }

    drawRoundRect(
        color = Color(0xE60B0F17),
        topLeft = Offset(left, top),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = statusColor,
        topLeft = Offset(left, top),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(14f, 14f),
        style = Stroke(width = 2.2f)
    )

    val summary = String.format(
        Locale.US,
        "WHEELIE  %.1f° UP   •   %.1f s   •   %.1f m   (MAX %.0f°)",
        max(0.0, telemetry.wheelieAngleDeg),
        telemetry.currentWheelieDurationSec,
        telemetry.currentWheelieDistanceMeters,
        telemetry.maxWheelieAngleDeg
    )
    drawNativeText(
        text = summary,
        x = w * 0.50f + shiftOffset.x,
        y = top + 34f,
        textSizePx = 17f,
        color = statusColor,
        align = Paint.Align.CENTER,
        bold = true
    )
}

/**
 * Alt Durum Çubuğu: Aktif Motor Profili ("Single Source of Truth"), Gerçek RPM, Kadran Modeli & Saat
 */
private fun DrawScope.drawBottomTelemetryStatusStrip(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    fgColor: Color,
    accentColor: Color,
    language: AppLanguage
) {
    val w = size.width
    val h = size.height
    val stripY = h * 0.935f
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    // Sol: Aktif Mekanik Motor Profili ve Gerçek Motor Devri
    drawNativeText(
        text = "ECU: ${telemetry.activeBikeDisplayName}  •  ${telemetry.actualEngineRpm.roundToInt()} / ${telemetry.bikeRedlineRpm.roundToInt()} RPM",
        x = w * 0.04f,
        y = stripY,
        textSizePx = 15f,
        color = accentColor,
        align = Paint.Align.LEFT,
        bold = true
    )

    // Orta: Seçili Görsel Gösterge Modeli (#1..#105)
    drawNativeText(
        text = "SKIN #${spec.modelNumber}/105: ${spec.brand} ${spec.modelName}",
        x = w * 0.50f,
        y = stripY,
        textSizePx = 15f,
        color = fgColor.copy(alpha = 0.78f),
        align = Paint.Align.CENTER,
        bold = true
    )

    // Sağ: Eğim Açısı, Hararet ve Saat
    drawNativeText(
        text = "LEAN ${abs(telemetry.leanAngleDeg).roundToInt()}°  •  ${telemetry.simulatedCoolantTempC}°C  •  $timeStr",
        x = w * 0.96f,
        y = stripY,
        textSizePx = 15f,
        color = fgColor.copy(alpha = 0.85f),
        align = Paint.Align.RIGHT,
        bold = true
    )
}

private fun DrawScope.drawNativeText(
    text: String,
    x: Float,
    y: Float,
    textSizePx: Float,
    color: Color,
    align: Paint.Align = Paint.Align.CENTER,
    bold: Boolean = true
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        this.textSize = textSizePx
        this.textAlign = align
        this.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}
