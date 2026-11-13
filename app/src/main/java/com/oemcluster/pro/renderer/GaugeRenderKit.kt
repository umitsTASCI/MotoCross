package com.oemcluster.pro.renderer

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.oemcluster.pro.catalog.DeclarativeGaugeSpec
import com.oemcluster.pro.catalog.GaugeDisplayCategory
import com.oemcluster.pro.catalog.ShiftLedPattern
import com.oemcluster.pro.domain.BikeMechanicalProfile
import com.oemcluster.pro.domain.ShiftLightState
import com.oemcluster.pro.domain.UnifiedTelemetryState
import com.oemcluster.pro.runtime.DistanceFormatter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal val RL_RED = Color(0xFFFF2A36)
internal val RL_GREEN = Color(0xFF20E070)
internal val RL_AMBER = Color(0xFFFFB300)
internal val RL_ICE = Color(0xFFEAF2FF)

/** Model-specific sinem — aynı kadranın her açılışta farklı piksel ofseti (OLED shift) değil, statik farklı kadran kimliği. */
internal fun skinTilt(skinId: String, salt: Int, amplitude: Float): Float =
    skinJitter(skinId, salt) * amplitude

internal fun hexColor(value: Long): Color = Color(value)

/**
 * skinId'ten türetilen deterministik değişken seçici.
 * Her model, modelNumber % 2/3 ile aynı 2-3 yerleşime düşmek yerine
 * kendi kimliğine göre farklı bir alt düzen alır — 307 kadran birbirinden
 * yapısal olarak ayrışır.
 */
internal fun skinVariant(skinId: String, mod: Int): Int {
    var h = 1125899906842597L // büyük asal
    for (c in skinId) {
        h = h * 31 + c.code
    }
    return ((h xor (h ushr 32)) and 0x7fffffffL).toInt().mod(mod).let { if (it < 0) it + mod else it }
}

/** Her kadran için -1..1 arası yumuşak, deterministik "el yapımı" sapma (piksel düzeyinde benzersiz). */
internal fun skinJitter(skinId: String, seed: Int): Float {
    var h = 1469598103934665603L
    val mixed = skinId + "#$seed"
    for (c in mixed) {
        h = h xor c.code.toLong()
        h *= 1099511628211L
    }
    val v = ((h xor (h ushr 40)) and 0xffff).toLong()
    return (v / 65535.0).toFloat() * 2f - 1f
}

/** Sıcaklık birimini uygular: 40..120 °C aralığına karşılık 104..248 °F */
internal fun formatTemp(celsius: Double, useFahrenheit: Boolean): String =
    if (useFahrenheit) "${(celsius * 9.0 / 5.0 + 32.0).roundToInt()}°F"
    else "${celsius.roundToInt()}°C"

/** Ortak, her karede yeniden üretilmeyen metin boyacısı. */
private val gaugeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    isSubpixelText = true
    isDither = true
}

private val typefaceCache = HashMap<String, Typeface>()

internal fun gaugeTypeface(family: String, bold: Boolean, italic: Boolean = false): Typeface {
    val style = when {
        bold && italic -> Typeface.BOLD_ITALIC
        bold -> Typeface.BOLD
        italic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }
    val key = "$family/$style"
    return typefaceCache.getOrPut(key) {
        try {
            Typeface.create(family, style)
        } catch (_: Throwable) {
            Typeface.create(Typeface.SANS_SERIF, style)
        }
    }
}

internal data class GaugeType(
    val label: String = "sans-serif-condensed",
    val digits: String = "sans-serif-condensed",
    val labelBold: Boolean = true
)

internal fun typographyFor(spec: DeclarativeGaugeSpec): GaugeType {
    return when (spec.brand) {
        "KTM", "GasGas" -> GaugeType("sans-serif-condensed", "sans-serif-condensed", true)
        "Yamaha" -> GaugeType("sans-serif", "sans-serif-condensed", true)
        "Honda" -> GaugeType("sans-serif", "sans-serif", true)
        "Kawasaki" -> GaugeType("sans-serif-condensed", "sans-serif-condensed", true)
        "BMW Motorrad" -> GaugeType("sans-serif-light", "sans-serif-light", false)
        "Ducati" -> GaugeType("sans-serif-condensed", "sans-serif-condensed", true)
        "Suzuki" -> GaugeType("sans-serif", "sans-serif", true)
        "Aprilia" -> GaugeType("sans-serif-condensed", "sans-serif-condensed", true)
        "Triumph" -> GaugeType("serif", "serif", true)
        "MV Agusta" -> GaugeType("sans-serif-condensed", "sans-serif-condensed", true)
        "Husqvarna" -> GaugeType("sans-serif-condensed", "sans-serif-condensed", true)
        "CFMoto", "QJMotor", "Voge", "Zontes", "Kove" -> GaugeType("sans-serif", "sans-serif-condensed", true)
        "Benelli", "Moto Guzzi" -> GaugeType("serif", "sans-serif-condensed", true)
        "Royal Enfield", "Indian" -> GaugeType("serif", "serif", true)
        "Harley-Davidson" -> GaugeType("serif", "sans-serif-condensed", true)
        "TVS", "Bajaj", "Hero" -> GaugeType("sans-serif-condensed", "sans-serif-condensed", true)
        else -> GaugeType()
    }
}

internal class GaugePalette(
    val bg: Color,
    val fg: Color,
    val accent: Color,
    val stage1: Color,
    val invertBg: Color,
    val invertFg: Color,
    val inverted: Boolean,
    val lcd: Boolean
) {
    val redline: Color get() = RL_RED
    val dim: Color get() = fg.copy(alpha = 0.14f)
    val faint: Color get() = fg.copy(alpha = 0.065f)
    val muted: Color get() = fg.copy(alpha = 0.58f)
    val panel: Color get() = fg.copy(alpha = 0.055f)
    val track: Color get() = fg.copy(alpha = 0.10f)
    val edge: Color get() = fg.copy(alpha = 0.22f)
    val bezelDark: Color get() = lerp(bg, Color.Black, 0.55f)
    val bezelLight: Color get() = lerp(fg, Color.White, 0.25f)
    val glass: Color get() = Color.White.copy(alpha = 0.10f)
}

internal class GaugeContext(
    val spec: DeclarativeGaugeSpec,
    val telemetry: UnifiedTelemetryState,
    val palette: GaugePalette,
    val type: GaugeType,
    val subModeIndex: Int,
    val rpmRatio: Float,
    val peakRatio: Float,
    val redlineRatio: Float,
    val mappedRpm: Double,
    val speedValue: Int,
    val speedUnit: String,
    val useMph: Boolean,
    val useFahrenheit: Boolean = false,
    val clock24h: Boolean = true,
    val fuelUnitCode: String = "l100km",
    val shiftOffset: Offset,
    val tripKm: Double,
    val odometerKm: Double,
    val sessionKm: Double = 0.0,
    val tripBKm: Double = 0.0,
    val showStatusRail: Boolean = true,
    val chromaKey: Boolean = false,
    val activeBike: BikeMechanicalProfile? = null,
    val best0to60Sec: Double = 0.0,
    val best0to100Sec: Double = 0.0,
    val peakSpeedKmh: Double = 0.0,
    val peakLeanLeftDeg: Double = 0.0,
    val peakLeanRightDeg: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val rideTimeSeconds: Double = 0.0,
    val w: Float,
    val h: Float
) {
    val u: Float = min(w, h) / 100f
    val wide: Boolean = w > h * 1.22f
    val maxRpm: Double = max(1000.0, spec.gaugeMaxRpm)
    val redlineRpm: Double = spec.gaugeRedlineRpm
    val gearText: String = telemetry.gearDisplayString
    val isNeutral: Boolean = telemetry.isNeutral || telemetry.currentGear <= 0
    val subMode: String = spec.availableSubModes.getOrElse(subModeIndex) {
        spec.availableSubModes.firstOrNull() ?: "OEM"
    }

    val tankCapacityLiters: Double = (activeBike?.fuelTankCapacityLiters ?: 15.0).coerceAtLeast(5.0)
    val avgFuelL100Km: Double = (activeBike?.avgFuelConsumptionLPer100Km ?: 5.2).coerceIn(1.5, 16.0)
    val fullTankRangeKm: Double = (tankCapacityLiters / avgFuelL100Km) * 100.0
    val computedRangeKm: Double = fullTankRangeKm * telemetry.fuelLevelRatio.toDouble().coerceIn(0.0, 1.0)
    val instantFuelL100Km: Double
        get() {
            if (telemetry.actualEngineRpm < 300.0 || telemetry.trueSpeedKmh < 3.0) return 0.0
            val refPower = max(15.0, activeBike?.maxPowerHp ?: 90.0)
            val powerLoad = (telemetry.estimatedWheelHorsepower / refPower).coerceIn(0.0, 0.85)
            return (avgFuelL100Km * (0.48 + 0.82 * rpmRatio.coerceIn(0f, 1.1f) + powerLoad)).coerceIn(1.5, 19.9)
        }
    val estimatedBatteryVolts: Double
        get() = when {
            telemetry.actualEngineRpm >= 1800.0 -> (13.8 + rpmRatio.coerceIn(0f, 1f) * 0.5).coerceIn(13.8, 14.3)
            telemetry.actualEngineRpm >= 400.0 -> 13.4
            else -> 12.6
        }

    val speedScaleMax: Int = estimateSpeedScale(spec, speedValue)
    val rpmLabelStep: Double = when {
        maxRpm >= 16000 -> 2000.0
        maxRpm >= 12000 -> 2000.0
        maxRpm >= 9000 -> 1000.0
        else -> 1000.0
    }
    val rpmLabelEvery: Int = if (maxRpm >= 15000) 1 else 1

    fun x(f: Float): Float = w * f + shiftOffset.x
    fun y(f: Float): Float = h * f + shiftOffset.y

    /** Kalıcı toplam kilometre (MPH modunda mil olarak gösterilir). */
    fun formatOdometer(): String = DistanceFormatter.format(odometerKm, useMph)

    /** Yol bilgisayarı turu (MPH modunda mil olarak gösterilir). */
    fun formatTrip(): String = DistanceFormatter.format(tripKm, useMph)

    /** İkinci yol bilgisayarı turu (Trip B). */
    fun formatTripB(): String = DistanceFormatter.format(tripBKm, useMph)

    /** Uygulama açıldığından beri biriken seans mesafesi. */
    fun formatSession(): String = DistanceFormatter.format(sessionKm, useMph)

    /** Aktif depo ve yakıt tüketiminden hesaplanan gerçek kalan menzil. */
    fun formatRange(): String {
        val dist = if (useMph) computedRangeKm * DistanceFormatter.KM_TO_MILE else computedRangeKm
        val unit = if (useMph) "MI" else "KM"
        return "${dist.roundToInt()} $unit"
    }

    /** Anlık veya ortalama yakıt tüketimi (seçili birimle). */
    fun formatInstantFuel(): String =
        if (telemetry.actualEngineRpm < 300.0 || telemetry.trueSpeedKmh < 3.0) "--.-"
        else formatFuelConsumption(instantFuelL100Km, fuelUnitCode)

    fun formatAvgFuel(): String = formatFuelConsumption(avgFuelL100Km, fuelUnitCode)

    fun formatBattery(): String = String.format(java.util.Locale.US, "%.1f V", estimatedBatteryVolts)

    fun formatBest0to60(): String =
        if (best0to60Sec > 0.05) String.format(java.util.Locale.US, "%.2f s", best0to60Sec) else "--.-- s"

    fun formatBest0to100(): String =
        if (best0to100Sec > 0.05) String.format(java.util.Locale.US, "%.2f s", best0to100Sec) else "--.-- s"

    fun formatPeakSpeed(): String {
        val v = if (useMph) (peakSpeedKmh * DistanceFormatter.KM_TO_MILE).roundToInt() else peakSpeedKmh.roundToInt()
        return "$v ${speedUnit.uppercase()}"
    }

    fun formatAvgSpeed(): String {
        val v = if (useMph) (avgSpeedKmh * DistanceFormatter.KM_TO_MILE).roundToInt() else avgSpeedKmh.roundToInt()
        return "$v ${speedUnit.uppercase()}"
    }

    fun formatRideTime(): String {
        val mins = (rideTimeSeconds / 60.0).toInt().coerceAtLeast(0)
        val hrs = (mins / 60).coerceAtMost(99)
        val rem = mins % 60
        return String.format(java.util.Locale.US, "%02d:%02d", hrs, rem)
    }
}

internal fun estimateSpeedScale(spec: DeclarativeGaugeSpec, currentSpeed: Int): Int {
    val base = when {
        spec.skinId.contains("hayabusa_gen1") -> 340
        spec.skinId.contains("hayabusa") || spec.skinId.contains("zzr1400") -> 320
        spec.skinId.contains("h2") || spec.skinId.contains("zh2") -> 300
        spec.skinId.contains("s1000") || spec.skinId.contains("m1000") -> 300
        spec.skinId.contains("zx10r") || spec.skinId.contains("zx-10") -> 300
        spec.skinId.contains("panigale_v4") || spec.skinId.contains("rsv4") || spec.skinId.contains("gp24") -> 300
        spec.skinId.contains("rtr_160") || spec.skinId.contains("rtr_200") ||
            spec.skinId.contains("ns125") || spec.skinId.contains("ns160") ||
            spec.skinId.contains("xpulse") || spec.skinId.contains("125") -> 160
        spec.skinId.contains("himalayan") || spec.skinId.contains("tenere") ||
            spec.skinId.contains("tuareg") || spec.skinId.contains("africa") ||
            spec.skinId.contains("vstrom") || spec.skinId.contains("norden") ||
            spec.skinId.contains("450mt") || spec.skinId.contains("525dsx") -> 200
        spec.skinId.contains("sportster") || spec.skinId.contains("iron") ||
            spec.skinId.contains("diavel") || spec.skinId.contains("ftr") ||
            spec.skinId.contains("scout") || spec.skinId.contains("fat_boy") -> 240
        spec.skinId.contains("classic") || spec.skinId.contains("bonneville") ||
            spec.skinId.contains("gt650") || spec.skinId.contains("thruxton") ||
            spec.skinId.contains("v7") || spec.skinId.contains("meteor") -> 200
        else -> when {
            spec.gaugeMaxRpm >= 15000 -> 299
            spec.gaugeMaxRpm >= 12000 -> 260
            spec.gaugeMaxRpm >= 10500 -> 240
            spec.gaugeMaxRpm >= 9000 -> 220
            else -> 180
        }
    }
    val needed = ceil(currentSpeed / 20.0).toInt() * 20
    return max(base, max(120, needed))
}

internal fun DrawScope.gText(
    text: String,
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    align: Paint.Align = Paint.Align.LEFT,
    family: String = "sans-serif-condensed",
    bold: Boolean = true,
    italic: Boolean = false,
    spacing: Float = 0.0f
) {
    if (text.isEmpty() || size <= 0.1f) return
    gaugeTextPaint.reset()
    gaugeTextPaint.isAntiAlias = true
    gaugeTextPaint.isSubpixelText = true
    gaugeTextPaint.color = color.toArgb()
    gaugeTextPaint.textSize = size
    gaugeTextPaint.textAlign = align
    gaugeTextPaint.typeface = gaugeTypeface(family, bold, italic)
    gaugeTextPaint.letterSpacing = spacing
    drawContext.canvas.nativeCanvas.drawText(text, x, y, gaugeTextPaint)
}

internal fun DrawScope.gTextWidth(
    text: String,
    size: Float,
    family: String = "sans-serif-condensed",
    bold: Boolean = true,
    spacing: Float = 0f
): Float {
    gaugeTextPaint.reset()
    gaugeTextPaint.isAntiAlias = true
    gaugeTextPaint.textSize = size
    gaugeTextPaint.typeface = gaugeTypeface(family, bold, false)
    gaugeTextPaint.letterSpacing = spacing
    return gaugeTextPaint.measureText(text)
}

internal fun DrawScope.gTextCenter(
    text: String,
    x: Float,
    centerY: Float,
    size: Float,
    color: Color,
    family: String = "sans-serif-condensed",
    bold: Boolean = true,
    spacing: Float = 0f
) {
    gText(text, x, centerY + size * 0.35f, size, color, Paint.Align.CENTER, family, bold, false, spacing)
}

internal fun DrawScope.polar(cx: Float, cy: Float, radius: Float, angleDeg: Float): Offset {
    val rad = angleDeg * PI / 180.0
    return Offset(cx + (radius * cos(rad)).toFloat(), cy + (radius * sin(rad)).toFloat())
}

internal fun DrawScope.drawArcTrack(
    cx: Float,
    cy: Float,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    width: Float,
    color: Color,
    cap: StrokeCap = StrokeCap.Butt
) {
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = width, cap = cap)
    )
}

internal fun DrawScope.drawArcSegment(
    cx: Float,
    cy: Float,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    width: Float,
    color: Color,
    cap: StrokeCap = StrokeCap.Round
) {
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = width, cap = cap)
    )
}

internal fun DrawScope.drawSegmentedArc(
    cx: Float,
    cy: Float,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    segments: Int,
    filledRatio: Float,
    width: Float,
    color: Color,
    trackColor: Color,
    redlineRatio: Float = 1.0f,
    redlineColor: Color = RL_RED,
    peakRatio: Float = -1f,
    peakColor: Color = Color.White,
    gapDegrees: Float = 1.0f,
    cap: StrokeCap = StrokeCap.Butt
) {
    val seg = sweepAngle / segments
    val filled = (filledRatio.coerceIn(0f, 1f) * segments).roundToInt()
    val peak = if (peakRatio >= 0f) (peakRatio.coerceIn(0f, 1f) * segments).roundToInt() else -1
    val redSeg = (redlineRatio.coerceIn(0f, 1f) * segments).roundToInt()
    for (i in 0 until segments) {
        val s = startAngle + i * seg
        val inRed = i >= redSeg
        val c = when {
            i < filled && inRed -> if (color == RL_RED) color else redlineColor
            i < filled -> color
            i == peak && peak > filled -> peakColor
            inRed -> redlineColor.copy(alpha = 0.24f)
            else -> trackColor
        }
        drawArcSegment(cx, cy, radius, s, max(0.12f, seg - gapDegrees), width, c, cap)
    }
}

internal fun DrawScope.drawNeedle(
    cx: Float,
    cy: Float,
    angleDeg: Float,
    length: Float,
    width: Float,
    color: Color,
    tail: Float = 0.18f
) {
    val tip = polar(cx, cy, length, angleDeg)
    val back = polar(cx, cy, -length * tail, angleDeg)
    drawLine(color = color, start = back, end = tip, strokeWidth = width, cap = StrokeCap.Round)
    drawLine(
        color = Color.White.copy(alpha = 0.28f),
        start = polar(cx, cy, -length * tail * 0.9f, angleDeg),
        end = polar(cx, cy, length * 0.72f, angleDeg),
        strokeWidth = max(1f, width * 0.22f),
        cap = StrokeCap.Round
    )
}

internal fun DrawScope.drawDialFace(
    cx: Float,
    cy: Float,
    radius: Float,
    palette: GaugePalette,
    chrome: Boolean = false,
    cream: Boolean = false,
    u: Float
) {
    val faceOuter = if (cream) Color(0xFFF2E9D4) else lerp(palette.bg, Color.Black, 0.25f)
    val faceInner = if (cream) Color(0xFFE2D5B8) else lerp(palette.bg, Color.Black, 0.55f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                if (cream) Color(0xFFFFFDF6) else lerp(faceOuter, Color.White, 0.16f),
                faceOuter,
                faceInner
            ),
            center = Offset(cx - radius * 0.18f, cy - radius * 0.22f),
            radius = radius * 1.35f
        ),
        radius = radius,
        center = Offset(cx, cy)
    )
    // Dış krom/bilezik
    drawCircle(
        color = if (chrome) Color(0xFFB8BDC6) else palette.edge,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = max(1.2f, u * 1.15f))
    )
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(
                if (chrome) Color(0xFFFFFFFF) else palette.fg.copy(alpha = 0.34f),
                if (chrome) Color(0xFF5E646E) else palette.fg.copy(alpha = 0.08f)
            ),
            start = Offset(cx - radius, cy - radius),
            end = Offset(cx + radius, cy + radius)
        ),
        radius = radius - u * 0.45f,
        center = Offset(cx, cy),
        style = Stroke(width = max(1f, u * 0.7f))
    )
    // Cam parlaması
    drawArc(
        color = Color.White.copy(alpha = 0.09f),
        startAngle = 200f,
        sweepAngle = 65f,
        useCenter = false,
        topLeft = Offset(cx - radius * 0.92f, cy - radius * 0.92f),
        size = Size(radius * 1.84f, radius * 1.84f),
        style = Stroke(width = max(2f, u * 2.2f), cap = StrokeCap.Round)
    )
}

internal fun DrawScope.drawTickRing(
    cx: Float,
    cy: Float,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    majorCount: Int,
    minorPerMajor: Int,
    color: Color,
    redlineRatio: Float,
    u: Float,
    labels: Boolean = true,
    labelFamily: String = "sans-serif-condensed",
    labelSize: Float = u * 3.1f,
    outward: Boolean = true,
    labelRadiusOverride: Float? = null,
    labelFormatter: ((Int) -> String)? = null
) {
    val total = max(1, majorCount * max(1, minorPerMajor))
    val tickOuter = radius
    val tickInnerMajor = radius - u * (if (labels) 4.2f else 3.2f)
    val tickInnerMinor = radius - u * 2.0f
    for (i in 0..total) {
        val ratio = i.toFloat() / total
        val angle = startAngle + sweepAngle * ratio
        val isMajor = i % max(1, minorPerMajor) == 0
        val inner = if (isMajor) tickInnerMajor else tickInnerMinor
        val tickColor = when {
            ratio >= redlineRatio -> RL_RED
            ratio >= redlineRatio - 0.06f -> lerp(color, RL_RED, 0.45f)
            else -> color.copy(alpha = if (isMajor) 0.92f else 0.48f)
        }
        drawLine(
            color = tickColor,
            start = polar(cx, cy, inner, angle),
            end = polar(cx, cy, tickOuter, angle),
            strokeWidth = if (isMajor) u * 0.72f else u * 0.34f,
            cap = StrokeCap.Round
        )
        if (labels && isMajor && i <= total) {
            val majorIndex = i / max(1, minorPerMajor)
            val labelRadius = labelRadiusOverride ?: (radius - u * 10.2f)
            val p = polar(cx, cy, labelRadius, angle)
            val txt = labelFormatter?.invoke(majorIndex) ?: majorIndex.toString()
            gTextCenter(txt, p.x, p.y, labelSize, tickColor, labelFamily, true)
        }
    }
}

internal fun DrawScope.drawNeedleDial(
    cx: Float,
    cy: Float,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    ratio: Float,
    palette: GaugePalette,
    u: Float,
    needleColor: Color,
    redlineRatio: Float,
    majorCount: Int,
    minorPerMajor: Int,
    labels: Boolean = true,
    cream: Boolean = false,
    chrome: Boolean = false,
    labelFamily: String = "sans-serif-condensed",
    labelFormatter: ((Int) -> String)? = null
) {
    drawDialFace(cx, cy, radius, palette, chrome, cream, u)
    drawTickRing(
        cx, cy, radius - u * 1.4f, startAngle, sweepAngle,
        majorCount, minorPerMajor,
        if (cream) Color(0xFF20242A) else palette.fg,
        redlineRatio, u, labels,
        labelFamily = labelFamily,
        labelSize = u * 2.75f,
        labelRadiusOverride = radius - u * 9.4f,
        labelFormatter = labelFormatter
    )
    val angle = startAngle + sweepAngle * ratio.coerceIn(0f, 1f)
    drawNeedle(cx, cy, angle, radius * 0.80f, u * 1.65f, needleColor, 0.22f)
    drawCircle(color = needleColor, radius = u * 2.35f, center = Offset(cx, cy))
    drawCircle(color = lerp(needleColor, Color.Black, 0.45f), radius = u * 1.25f, center = Offset(cx, cy))
    drawCircle(color = Color.White.copy(alpha = 0.55f), radius = u * 0.42f, center = Offset(cx - u * 0.5f, cy - u * 0.5f))
}

internal fun DrawScope.drawLinearBar(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    ratio: Float,
    color: Color,
    trackColor: Color,
    redlineRatio: Float = 1f,
    redlineColor: Color = RL_RED,
    radius: Float,
    vertical: Boolean = false
) {
    if (vertical) {
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        val redStart = top + height * redlineRatio
        drawRoundRect(
            color = redlineColor.copy(alpha = 0.28f),
            topLeft = Offset(left, redStart),
            size = Size(width, height * (1f - redlineRatio)),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        val fill = height * ratio.coerceIn(0f, 1f)
        if (fill > 0.5f) {
            drawRoundRect(
                color = if (ratio > redlineRatio) redlineColor else color,
                topLeft = Offset(left, top + height - fill),
                size = Size(width, fill),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
        }
    } else {
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        val redStart = left + width * redlineRatio
        drawRoundRect(
            color = redlineColor.copy(alpha = 0.28f),
            topLeft = Offset(redStart, top),
            size = Size(width * (1f - redlineRatio), height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
        )
        val fill = width * ratio.coerceIn(0f, 1f)
        if (fill > 0.5f) {
            drawRoundRect(
                color = if (ratio > redlineRatio) redlineColor else color,
                topLeft = Offset(left, top),
                size = Size(fill, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
            )
        }
    }
}

internal fun DrawScope.drawGearPod(
    cx: Float,
    cy: Float,
    size: Float,
    palette: GaugePalette,
    telemetry: UnifiedTelemetryState,
    neutral: Boolean,
    u: Float,
    family: String = "sans-serif-condensed",
    shape: GearPodShape = GearPodShape.SQUARE
) {
    val bg = when {
        // Gerçek OEM kadranlarında "N" parlak yeşil bir kutu değil,
        // koyu zemin üzerinde yeşil rakam + yeşil çerçevedir.
        neutral -> RL_GREEN.copy(alpha = 0.15f)
        telemetry.shiftLightState == ShiftLightState.STAGE_2_SHIFT_NOW ||
            telemetry.shiftLightState == ShiftLightState.OVER_REV_LIMITER ->
            if (telemetry.shiftFlashPhaseOn) palette.invertFg else palette.accent
        telemetry.shiftLightState == ShiftLightState.STAGE_1_APPROACHING -> palette.stage1
        else -> palette.fg.copy(alpha = 0.10f)
    }
    val fg = when {
        neutral -> RL_GREEN
        telemetry.shiftLightState != ShiftLightState.NORMAL ->
            if (bg.luminance() > 0.55f) Color(0xFF090C12) else Color.White
        else -> palette.fg
    }
    val edge = when {
        neutral -> RL_GREEN.copy(alpha = 0.55f)
        telemetry.shiftLightState != ShiftLightState.NORMAL -> Color.White.copy(alpha = 0.35f)
        else -> palette.fg.copy(alpha = 0.20f)
    }
    when (shape) {
        GearPodShape.NONE -> Unit
        GearPodShape.ROUND -> {
            drawCircle(color = bg, radius = size * 0.58f, center = Offset(cx, cy))
            drawCircle(color = edge, radius = size * 0.58f, center = Offset(cx, cy), style = Stroke(width = u * 0.5f))
        }
        GearPodShape.HEX -> {
            val p = Path()
            for (i in 0 until 6) {
                val a = (60.0 * i - 90.0) * PI / 180.0
                val x = cx + (size * 0.62f * cos(a)).toFloat()
                val y = cy + (size * 0.62f * sin(a)).toFloat()
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close()
            drawPath(p, bg)
            drawPath(p, edge, style = Stroke(width = u * 0.55f))
        }
        GearPodShape.SQUARE -> {
            drawRoundRect(
                color = bg,
                topLeft = Offset(cx - size * 0.55f, cy - size * 0.52f),
                size = Size(size * 1.10f, size * 1.04f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.12f, size * 0.12f)
            )
            drawRoundRect(
                color = edge,
                topLeft = Offset(cx - size * 0.55f, cy - size * 0.52f),
                size = Size(size * 1.10f, size * 1.04f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.12f, size * 0.12f),
                style = Stroke(width = u * 0.45f)
            )
        }
    }
    gText(
        text = if (neutral) "N" else telemetry.gearDisplayString,
        x = cx,
        y = cy + size * 0.36f,
        size = size,
        color = fg,
        align = Paint.Align.CENTER,
        family = family,
        bold = true
    )
}

internal enum class GearPodShape { SQUARE, ROUND, HEX, NONE }

internal fun DrawScope.drawSpeed(
    value: Int,
    unit: String,
    x: Float,
    baseline: Float,
    size: Float,
    color: Color,
    unitColor: Color,
    family: String,
    align: Paint.Align = Paint.Align.CENTER
) {
    // OEM kuralı: birim, rakamların sağında ve taban çizgisinde durur.
    // (Altına ortalanmış "km/h" amatör görünür; tüm OEM kadranlarda bu düzeltildi.)
    val text = value.toString()
    val unitSize = size * 0.20f
    val numberWidth = gTextWidth(text, size, family, true, -0.02f)
    val unitText = unit.uppercase()
    val unitWidth = gTextWidth(unitText, unitSize, "sans-serif-condensed", true, 0.07f)
    val gap = size * 0.055f
    val groupWidth = numberWidth + gap + unitWidth
    val left = when (align) {
        Paint.Align.CENTER -> x - groupWidth * 0.5f
        Paint.Align.LEFT -> x
        Paint.Align.RIGHT -> x - groupWidth
    }
    gText(
        text = text,
        x = left,
        y = baseline,
        size = size,
        color = color,
        align = Paint.Align.LEFT,
        family = family,
        bold = true,
        spacing = -0.02f
    )
    gText(
        text = unitText,
        x = left + numberWidth + gap,
        y = baseline,
        size = unitSize,
        color = unitColor,
        align = Paint.Align.LEFT,
        family = "sans-serif-condensed",
        bold = true,
        spacing = 0.07f
    )
}

internal fun DrawScope.drawBrandMark(
    spec: DeclarativeGaugeSpec,
    palette: GaugePalette,
    cx: Float,
    cy: Float,
    size: Float,
    u: Float,
    tint: Color = palette.accent
) {
    when (spec.brand) {
        "KTM" -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(cx - size * 0.62f, cy - size * 0.34f),
                size = Size(size * 1.24f, size * 0.68f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.06f, size * 0.06f)
            )
            gText("KTM", cx, cy + size * 0.17f, size * 0.46f, Color.Black, Paint.Align.CENTER, "sans-serif", true, spacing = 0.06f)
        }
        "Yamaha" -> {
            drawCircle(color = tint.copy(alpha = 0.16f), radius = size * 0.62f, center = Offset(cx, cy), style = Stroke(width = u * 0.5f))
            drawLine(tint, Offset(cx - size * 0.44f, cy - size * 0.38f), Offset(cx + size * 0.44f, cy + size * 0.38f), u * 0.9f)
            drawLine(tint, Offset(cx + size * 0.44f, cy - size * 0.38f), Offset(cx - size * 0.44f, cy + size * 0.38f), u * 0.9f)
            drawCircle(tint, radius = size * 0.13f, center = Offset(cx, cy), style = Stroke(width = u * 0.55f))
        }
        "Honda" -> {
            val p = Path()
            p.moveTo(cx - size * 0.58f, cy + size * 0.10f)
            p.quadraticTo(cx - size * 0.22f, cy - size * 0.42f, cx + size * 0.12f, cy - size * 0.20f)
            p.quadraticTo(cx + size * 0.42f, cy - size * 0.02f, cx + size * 0.60f, cy - size * 0.26f)
            p.quadraticTo(cx + size * 0.36f, cy + size * 0.34f, cx - size * 0.06f, cy + size * 0.22f)
            p.quadraticTo(cx - size * 0.36f, cy + size * 0.14f, cx - size * 0.58f, cy + size * 0.10f)
            p.close()
            drawPath(p, tint)
        }
        "Kawasaki" -> {
            gText("K", cx, cy + size * 0.34f, size * 1.06f, tint, Paint.Align.CENTER, "sans-serif", true, italic = true)
            drawLine(tint, Offset(cx - size * 0.5f, cy + size * 0.42f), Offset(cx + size * 0.46f, cy + size * 0.42f), u * 0.7f)
        }
        "BMW Motorrad" -> {
            drawCircle(Color(0xFF1C69D4), radius = size * 0.55f, center = Offset(cx, cy))
            drawArc(
                color = Color.White,
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(cx - size * 0.55f, cy - size * 0.55f),
                size = Size(size * 1.10f, size * 1.10f)
            )
            drawArc(
                color = Color.White,
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = true,
                topLeft = Offset(cx - size * 0.55f, cy - size * 0.55f),
                size = Size(size * 1.10f, size * 1.10f)
            )
            drawCircle(tint, radius = size * 0.55f, center = Offset(cx, cy), style = Stroke(width = u * 0.55f))
        }
        "Ducati" -> {
            drawRoundRect(
                color = Color(0xFFCC0000),
                topLeft = Offset(cx - size * 0.66f, cy - size * 0.26f),
                size = Size(size * 1.32f, size * 0.52f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.05f, size * 0.05f)
            )
            gText("DUCATI", cx, cy + size * 0.15f, size * 0.34f, Color.White, Paint.Align.CENTER, "sans-serif-condensed", true, spacing = 0.14f)
        }
        "Suzuki" -> {
            val p = Path()
            p.moveTo(cx - size * 0.46f, cy - size * 0.42f)
            p.quadraticTo(cx + size * 0.22f, cy - size * 0.34f, cx + size * 0.32f, cy - size * 0.02f)
            p.quadraticTo(cx + size * 0.40f, cy + size * 0.34f, cx - size * 0.08f, cy + size * 0.40f)
            p.quadraticTo(cx - size * 0.50f, cy + size * 0.44f, cx - size * 0.52f, cy + size * 0.10f)
            p.quadraticTo(cx - size * 0.52f, cy - size * 0.10f, cx - size * 0.20f, cy - size * 0.06f)
            drawPath(p, tint, style = Stroke(width = u * 1.25f, cap = StrokeCap.Round))
        }
        "Aprilia" -> {
            gText("a", cx, cy + size * 0.38f, size * 1.16f, tint, Paint.Align.CENTER, "sans-serif", true, italic = true)
            drawRoundRect(
                color = tint,
                topLeft = Offset(cx - size * 0.62f, cy - size * 0.48f),
                size = Size(size * 1.24f, size * 0.12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.04f, size * 0.04f)
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(cx - size * 0.62f, cy + size * 0.34f),
                size = Size(size * 1.24f, size * 0.12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.04f, size * 0.04f)
            )
        }
        "Triumph" -> {
            val p = Path()
            p.moveTo(cx, cy - size * 0.52f)
            p.lineTo(cx + size * 0.48f, cy + size * 0.34f)
            p.lineTo(cx - size * 0.48f, cy + size * 0.34f)
            p.close()
            drawPath(p, tint, style = Stroke(width = u * 0.95f))
            drawCircle(tint, radius = size * 0.10f, center = Offset(cx, cy + size * 0.06f))
        }
        "MV Agusta" -> {
            gText("MV", cx, cy + size * 0.20f, size * 0.52f, tint, Paint.Align.CENTER, "sans-serif", true, spacing = 0.04f)
            drawLine(tint, Offset(cx - size * 0.46f, cy + size * 0.30f), Offset(cx + size * 0.46f, cy + size * 0.30f), u * 0.65f)
            drawLine(tint, Offset(cx - size * 0.30f, cy - size * 0.34f), Offset(cx + size * 0.30f, cy - size * 0.34f), u * 0.65f)
        }
        "Husqvarna" -> {
            val p = Path()
            p.moveTo(cx - size * 0.52f, cy + size * 0.26f)
            p.lineTo(cx - size * 0.52f, cy - size * 0.18f)
            p.lineTo(cx - size * 0.18f, cy + size * 0.02f)
            p.lineTo(cx, cy - size * 0.42f)
            p.lineTo(cx + size * 0.18f, cy + size * 0.02f)
            p.lineTo(cx + size * 0.52f, cy - size * 0.18f)
            p.lineTo(cx + size * 0.52f, cy + size * 0.26f)
            p.close()
            drawPath(p, tint, style = Stroke(width = u * 0.9f))
        }
        "CFMoto" -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(cx - size * 0.58f, cy - size * 0.34f),
                size = Size(size * 1.16f, size * 0.68f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.1f, size * 0.1f),
                style = Stroke(width = u * 0.72f)
            )
            gText("CF", cx, cy + size * 0.18f, size * 0.48f, tint, Paint.Align.CENTER, "sans-serif", true)
        }
        "Benelli" -> {
            drawCircle(tint, radius = size * 0.46f, center = Offset(cx, cy), style = Stroke(width = u * 0.8f))
            gText("B", cx, cy + size * 0.22f, size * 0.52f, tint, Paint.Align.CENTER, "serif", true)
        }
        "Royal Enfield" -> {
            drawCircle(tint, radius = size * 0.5f, center = Offset(cx, cy), style = Stroke(width = u * 0.65f))
            gText("RE", cx, cy + size * 0.18f, size * 0.40f, tint, Paint.Align.CENTER, "serif", true, spacing = 0.04f)
        }
        "Harley-Davidson" -> {
            val p = Path()
            p.moveTo(cx - size * 0.52f, cy - size * 0.34f)
            p.lineTo(cx + size * 0.52f, cy - size * 0.34f)
            p.lineTo(cx + size * 0.36f, cy + size * 0.40f)
            p.lineTo(cx - size * 0.36f, cy + size * 0.40f)
            p.close()
            drawPath(p, tint, style = Stroke(width = u * 0.85f))
            gText("HD", cx, cy + size * 0.16f, size * 0.38f, tint, Paint.Align.CENTER, "sans-serif-condensed", true)
        }
        "TVS" -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(cx - size * 0.66f, cy - size * 0.30f),
                size = Size(size * 1.32f, size * 0.60f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.08f, size * 0.08f),
                style = Stroke(width = u * 0.72f)
            )
            gText("TVS", cx, cy + size * 0.16f, size * 0.44f, tint, Paint.Align.CENTER, "sans-serif-condensed", true, italic = true, spacing = 0.08f)
        }
        "Bajaj" -> {
            val p = Path()
            p.moveTo(cx, cy - size * 0.50f)
            p.lineTo(cx + size * 0.50f, cy)
            p.lineTo(cx, cy + size * 0.50f)
            p.lineTo(cx - size * 0.50f, cy)
            p.close()
            drawPath(p, tint, style = Stroke(width = u * 0.75f))
            gText("B", cx, cy + size * 0.18f, size * 0.46f, tint, Paint.Align.CENTER, "sans-serif-condensed", true)
        }
        "Hero" -> {
            drawLine(tint, Offset(cx - size * 0.36f, cy - size * 0.42f), Offset(cx - size * 0.12f, cy + size * 0.42f), u * 0.95f)
            drawLine(tint, Offset(cx + size * 0.12f, cy - size * 0.42f), Offset(cx + size * 0.36f, cy + size * 0.42f), u * 0.95f)
            drawLine(tint, Offset(cx - size * 0.28f, cy), Offset(cx + size * 0.28f, cy), u * 0.8f)
        }
        "QJMotor" -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(cx - size * 0.62f, cy - size * 0.32f),
                size = Size(size * 1.24f, size * 0.64f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.10f, size * 0.10f),
                style = Stroke(width = u * 0.70f)
            )
            gText("QJ", cx, cy + size * 0.17f, size * 0.44f, tint, Paint.Align.CENTER, "sans-serif-condensed", true)
        }
        "Voge" -> {
            val p = Path()
            p.moveTo(cx - size * 0.52f, cy - size * 0.36f)
            p.lineTo(cx, cy + size * 0.42f)
            p.lineTo(cx + size * 0.52f, cy - size * 0.36f)
            drawPath(p, tint, style = Stroke(width = u * 0.95f, cap = StrokeCap.Round))
        }
        "Zontes" -> {
            gText("ZT", cx, cy + size * 0.20f, size * 0.52f, tint, Paint.Align.CENTER, "sans-serif-condensed", true, italic = true)
            drawLine(tint, Offset(cx - size * 0.48f, cy + size * 0.34f), Offset(cx + size * 0.48f, cy + size * 0.34f), u * 0.6f)
        }
        "Kove" -> {
            gText("KOVE", cx, cy + size * 0.16f, size * 0.38f, tint, Paint.Align.CENTER, "sans-serif-condensed", true, italic = true, spacing = 0.06f)
        }
        "Moto Guzzi" -> {
            drawLine(tint, Offset(cx - size * 0.68f, cy - size * 0.10f), Offset(cx + size * 0.68f, cy - size * 0.10f), u * 0.85f)
            drawLine(tint, Offset(cx - size * 0.48f, cy + size * 0.08f), Offset(cx + size * 0.48f, cy + size * 0.08f), u * 0.65f)
            drawCircle(tint, radius = size * 0.14f, center = Offset(cx, cy - size * 0.10f))
        }
        "GasGas" -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(cx - size * 0.66f, cy - size * 0.28f),
                size = Size(size * 1.32f, size * 0.56f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.06f, size * 0.06f)
            )
            gText("GASGAS", cx, cy + size * 0.14f, size * 0.30f, Color.White, Paint.Align.CENTER, "sans-serif-condensed", true, spacing = 0.06f)
        }
        "Indian" -> {
            drawCircle(tint, radius = size * 0.48f, center = Offset(cx, cy), style = Stroke(width = u * 0.65f))
            gText("IND", cx, cy + size * 0.16f, size * 0.36f, tint, Paint.Align.CENTER, "serif", true, italic = true)
        }
        else -> gText(spec.brand.uppercase(), cx, cy + size * 0.2f, size * 0.42f, tint, Paint.Align.CENTER, "sans-serif", true)
    }
}

internal fun DrawScope.drawShiftLeds(
    spec: DeclarativeGaugeSpec,
    telemetry: UnifiedTelemetryState,
    palette: GaugePalette,
    cx: Float,
    cy: Float,
    span: Float,
    u: Float,
    verticalSides: Boolean = false
) {
    if (spec.shiftLedPattern == ShiftLedPattern.NONE_INTERNAL_ONLY) return
    val ratio = (telemetry.normalizedRpmRatio).toFloat().coerceIn(0f, 1.12f)
    val flashOn = telemetry.shiftFlashPhaseOn
    val stage = telemetry.shiftLightState

    fun litAt(threshold: Float): Boolean {
        if (ratio < threshold) return false
        return when (stage) {
            ShiftLightState.OVER_REV_LIMITER -> flashOn
            ShiftLightState.STAGE_2_SHIFT_NOW -> flashOn || spec.shiftLedPattern == ShiftLedPattern.CONVERGING_5_LEDS
            else -> true
        }
    }

    when (spec.shiftLedPattern) {
        ShiftLedPattern.NONE_INTERNAL_ONLY -> Unit
        ShiftLedPattern.SINGLE_UPPER_LED -> {
            val isStage1 = stage == ShiftLightState.STAGE_1_APPROACHING
            val isStage2 = stage == ShiftLightState.STAGE_2_SHIFT_NOW
            val isOverRev = stage == ShiftLightState.OVER_REV_LIMITER
            val on = isStage1 || ((isStage2 || isOverRev) && flashOn)
            val glowColor = when {
                isOverRev -> RL_RED
                isStage2 -> Color.White
                else -> palette.stage1
            }
            val coreColor = when {
                isOverRev -> if (flashOn) Color.White else RL_RED.copy(alpha = 0.25f)
                isStage2 -> if (flashOn) Color.White else palette.stage1.copy(alpha = 0.22f)
                isStage1 -> palette.stage1
                else -> palette.fg.copy(alpha = 0.13f)
            }
            if (on) drawCircle(glowColor.copy(alpha = if (isStage1) 0.22f else 0.34f), u * 3.4f, Offset(cx, cy))
            drawCircle(coreColor, u * 1.55f, Offset(cx, cy))
            if (on) drawCircle(Color.White.copy(alpha = if (isStage1) 0.55f else 0.90f), u * 0.72f, Offset(cx, cy))
        }
        ShiftLedPattern.CONVERGING_5_LEDS -> {
            val gap = span / 6f
            for (i in 0 until 5) {
                val dist = abs(i - 2)
                val threshold = when (dist) { 2 -> 0.70f; 1 -> 0.82f; else -> 0.93f }
                val on = litAt(threshold)
                val c = when {
                    !on -> palette.fg.copy(alpha = 0.10f)
                    dist == 0 -> RL_RED
                    dist == 1 -> RL_AMBER
                    else -> RL_GREEN
                }
                val x = cx + (i - 2) * gap
                if (on) drawRoundRect(c.copy(alpha = 0.18f), Offset(x - u * 1.7f, cy - u * 1.5f), Size(u * 3.4f, u * 3.0f), androidx.compose.ui.geometry.CornerRadius(u * 0.6f, u * 0.6f))
                drawRoundRect(c, Offset(x - u * 1.25f, cy - u * 0.62f), Size(u * 2.5f, u * 1.24f), androidx.compose.ui.geometry.CornerRadius(u * 0.3f, u * 0.3f))
            }
        }
        ShiftLedPattern.SEQUENTIAL_6_WHITE_LEDS -> {
            val gap = span / 6f
            for (i in 0 until 6) {
                val on = litAt(0.64f + i * 0.055f)
                val c = when {
                    !on -> palette.fg.copy(alpha = 0.10f)
                    i >= 5 -> RL_RED
                    i >= 4 -> RL_AMBER
                    else -> Color.White
                }
                val x = cx + (i - 2.5f) * gap
                if (on) drawCircle(c.copy(alpha = 0.20f), u * 2.0f, Offset(x, cy))
                drawCircle(c, u * 0.85f, Offset(x, cy))
            }
        }
        ShiftLedPattern.TRIUMPH_7_BLUE_LEDS -> {
            val gap = span / 7f
            for (i in 0 until 7) {
                val on = litAt(0.62f + i * 0.048f)
                val x = cx + (i - 3f) * gap
                val c = when {
                    !on -> palette.fg.copy(alpha = 0.10f)
                    i >= 6 -> Color(0xFFFF3B30)
                    else -> Color(0xFF2A7BFF)
                }
                if (on) drawCircle(c.copy(alpha = 0.22f), u * 1.9f, Offset(x, cy))
                drawCircle(c, u * 0.80f, Offset(x, cy))
            }
        }
        ShiftLedPattern.KAWASAKI_LED_ARCH_BAR -> {
            val count = 11
            val radius = span * 0.55f
            for (i in 0 until count) {
                val t = i.toFloat() / (count - 1)
                val angle = 200f + 140f * t
                val p = polar(cx, cy + radius * 0.55f, radius, angle)
                val on = litAt(0.58f + t * 0.36f)
                val c = when {
                    !on -> palette.fg.copy(alpha = 0.09f)
                    t >= 0.82f -> RL_RED
                    t >= 0.64f -> RL_AMBER
                    else -> Color(0xFF35E36B)
                }
                if (on) drawCircle(c.copy(alpha = 0.18f), u * 1.6f, p)
                drawCircle(c, u * 0.72f, p)
            }
        }
        ShiftLedPattern.DUAL_SIDE_LIGHTNING_LEDS -> {
            val on = stage != ShiftLightState.NORMAL && (flashOn || stage == ShiftLightState.STAGE_1_APPROACHING)
            val color = if (stage == ShiftLightState.OVER_REV_LIMITER) RL_RED else Color(0xFFFFD54A)
            val dx = if (verticalSides) span * 0.5f else span * 0.46f
            for (side in listOf(-1f, 1f)) {
                val baseX = cx + side * dx
                if (on) {
                    drawLine(color.copy(alpha = 0.22f), Offset(baseX, cy - u * 2.4f), Offset(baseX, cy + u * 2.4f), u * 2.2f, StrokeCap.Round)
                }
                drawLine(if (on) color else palette.fg.copy(alpha = 0.10f), Offset(baseX, cy - u * 1.6f), Offset(baseX, cy + u * 1.6f), u * 0.8f, StrokeCap.Round)
            }
        }
        ShiftLedPattern.MOTOGP_MARSHAL_BAR -> {
            val count = 8
            val gap = span / count
            for (i in 0 until count) {
                val on = litAt(0.66f + i * 0.038f)
                val c = when {
                    !on -> palette.fg.copy(alpha = 0.08f)
                    i >= 6 -> RL_RED
                    i >= 4 -> RL_AMBER
                    else -> RL_GREEN
                }
                val x = cx + (i - (count - 1) / 2f) * gap
                drawRoundRect(c, Offset(x - gap * 0.36f, cy - u * 0.8f), Size(gap * 0.72f, u * 1.6f), androidx.compose.ui.geometry.CornerRadius(u * 0.25f, u * 0.25f))
            }
        }
    }
}

internal fun DrawScope.drawBootSplash(
    ctx: GaugeContext,
    progress: Float
) {
    val p = ctx.palette
    drawRect(p.bg)
    // ince grid
    val step = ctx.u * 8f
    var gx = 0f
    while (gx < ctx.w) {
        drawLine(p.fg.copy(alpha = 0.035f), Offset(gx, 0f), Offset(gx, ctx.h), 1f)
        gx += step
    }
    var gy = 0f
    while (gy < ctx.h) {
        drawLine(p.fg.copy(alpha = 0.035f), Offset(0f, gy), Offset(ctx.w, gy), 1f)
        gy += step
    }
    val cx = ctx.w * 0.5f
    val cy = ctx.h * 0.44f
    drawCircle(p.accent.copy(alpha = 0.08f), ctx.u * 24f, Offset(cx, cy))
    drawCircle(p.accent.copy(alpha = 0.22f), ctx.u * 24f, Offset(cx, cy), style = Stroke(width = ctx.u * 0.6f))
    drawBrandMark(ctx.spec, p, cx, cy - ctx.u * 2f, ctx.u * 15f, ctx.u)
    gText(
        ctx.spec.bootSplashLogoText,
        cx, cy + ctx.u * 17.5f, ctx.u * 3.5f, p.fg,
        Paint.Align.CENTER, ctx.type.label, true, spacing = 0.08f
    )
    gText(
        "${ctx.spec.brand.uppercase()}  //  ${ctx.spec.modelName.uppercase()}",
        cx, cy + ctx.u * 22.5f, ctx.u * 2.2f, p.muted,
        Paint.Align.CENTER, ctx.type.label, false, spacing = 0.04f
    )

    // Self-check ikonları
    val icons = listOf("ABS", "TC", "OIL", "BAT", "GPS", "ECU")
    val iconY = ctx.h * 0.78f
    val gap = min(ctx.w * 0.13f, ctx.u * 15f)
    val startX = cx - (icons.size - 1) * gap * 0.5f
    icons.forEachIndexed { i, label ->
        val threshold = (i + 1) / (icons.size + 1f)
        val on = progress >= threshold || progress >= 1f
        val x = startX + i * gap
        drawCircle(if (on) p.accent.copy(alpha = 0.16f) else p.fg.copy(alpha = 0.05f), ctx.u * 4.2f, Offset(x, iconY))
        drawCircle(if (on) p.accent else p.fg.copy(alpha = 0.18f), ctx.u * 4.2f, Offset(x, iconY), style = Stroke(width = ctx.u * 0.5f))
        gText(label, x, iconY + ctx.u * 1.0f, ctx.u * 1.5f, if (on) p.accent else p.fg.copy(alpha = 0.25f), Paint.Align.CENTER, "sans-serif-condensed", true, spacing = 0.04f)
    }
    val barLeft = cx - ctx.w * 0.22f
    val barTop = ctx.h * 0.885f
    drawRoundRect(p.fg.copy(alpha = 0.10f), Offset(barLeft, barTop), Size(ctx.w * 0.44f, ctx.u * 0.9f), androidx.compose.ui.geometry.CornerRadius(ctx.u, ctx.u))
    drawRoundRect(p.accent, Offset(barLeft, barTop), Size(ctx.w * 0.44f * progress.coerceIn(0f, 1f), ctx.u * 0.9f), androidx.compose.ui.geometry.CornerRadius(ctx.u, ctx.u))
    gText(
        if (progress > 0.98f) "SYSTEM READY" else "SYSTEM CHECK ${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%",
        cx, barTop + ctx.u * 3.4f, ctx.u * 1.7f, p.muted, Paint.Align.CENTER, "sans-serif-condensed", true, spacing = 0.12f
    )
}

internal fun DrawScope.drawStatusRail(
    ctx: GaugeContext,
    style: RailStyle = RailStyle.TFT
) {
    if (!ctx.showStatusRail) return
    val p = ctx.palette
    val t = ctx.telemetry
    val railH = if (style == RailStyle.CLASSIC) ctx.h * 0.088f else ctx.h * 0.082f
    val top = ctx.h - railH
    when (style) {
        RailStyle.TFT, RailStyle.LCD -> {
            drawRect(lerp(p.bg, Color.Black, 0.38f), Offset(0f, top), Size(ctx.w, railH))
            drawRect(p.fg.copy(alpha = 0.045f), Offset(0f, top), Size(ctx.w, railH))
            drawLine(p.fg.copy(alpha = 0.16f), Offset(0f, top), Offset(ctx.w, top), max(1f, ctx.u * 0.18f))
        }
        RailStyle.CLASSIC -> {
            drawRect(lerp(p.bg, Color.Black, 0.45f), Offset(0f, top), Size(ctx.w, railH))
            drawLine(p.accent.copy(alpha = 0.38f), Offset(0f, top), Offset(ctx.w, top), max(1f, ctx.u * 0.22f))
        }
    }

    val cy = top + railH * 0.50f
    val yTop = top + railH * 0.41f
    val yBot = top + railH * 0.81f
    val textSize = min(ctx.u * 2.25f, railH * 0.36f)
    val small = textSize * 0.76f
    val family = ctx.type.label
    val pad = ctx.w * 0.025f

    // 1. Sol blok: OEM ikaz lambaları (Sinyal, Uzun Far, Yağ, TC, ABS)
    var iconX = pad + ctx.u * 0.8f
    val iconR = min(ctx.u * 1.35f, railH * 0.22f)
    drawTurnIcon(iconX, cy, iconR, p.fg.copy(alpha = 0.32f), false)
    iconX += iconR * 2.6f
    drawTurnIcon(iconX, cy, iconR, p.fg.copy(alpha = 0.32f), true)
    iconX += iconR * 2.2f
    drawHeadlightIcon(iconX, cy, iconR, if (t.nightModeTransitionFactor > 0.5f) Color(0xFF4DA3FF) else p.fg.copy(alpha = 0.36f))
    iconX += iconR * 2.2f
    drawOilIcon(iconX, cy, iconR, if (t.actualEngineRpm in 1.0..450.0) RL_AMBER else p.fg.copy(alpha = 0.30f))
    iconX += iconR * 2.3f

    if (t.tractionControlInterventionActive) {
        drawRoundRect(RL_AMBER, Offset(iconX - iconR, cy - iconR * 0.85f), Size(iconR * 2.6f, iconR * 1.7f), androidx.compose.ui.geometry.CornerRadius(iconR * 0.3f, iconR * 0.3f))
        gText("TC", iconX + iconR * 0.3f, cy + small * 0.35f, small, Color.Black, Paint.Align.CENTER, family, true)
    } else {
        gText("TC", iconX + iconR * 0.3f, cy + small * 0.35f, small, p.fg.copy(alpha = 0.34f), Paint.Align.CENTER, family, true)
    }
    iconX += iconR * 2.6f
    gText("ABS", iconX + iconR * 0.5f, cy + small * 0.35f, small, if (t.absInterventionActive) RL_RED else p.fg.copy(alpha = 0.34f), Paint.Align.CENTER, family, true)

    val divTop = top + railH * 0.16f
    val divBot = top + railH * 0.84f
    fun drawDivider(x: Float) {
        drawLine(p.fg.copy(alpha = 0.12f), Offset(x, divTop), Offset(x, divBot), max(1f, ctx.u * 0.12f))
    }

    // 2. Yakıt bloğu
    drawDivider(ctx.w * 0.195f)
    val fuelX = ctx.w * 0.212f
    drawFuelIcon(fuelX, top + railH * 0.36f, iconR * 0.92f, p.fg.copy(alpha = 0.65f))
    val barW = ctx.w * 0.072f
    val barH = max(2f, railH * 0.15f)
    drawLinearBar(fuelX + iconR * 1.7f, top + railH * 0.28f, barW, barH, t.fuelLevelRatio, p.accent, p.track, 0.88f, RL_RED, barH * 0.45f)
    gText("FUEL ${(t.fuelLevelRatio * 100).roundToInt()}%", fuelX + iconR * 1.7f, yBot, small, p.muted, Paint.Align.LEFT, family, true)

    // 3. Hararet bloğu
    drawDivider(ctx.w * 0.328f)
    val tempX = ctx.w * 0.345f
    drawTempIcon(tempX, top + railH * 0.36f, iconR * 0.92f, if (t.coolantTempCelsius > 108.0) RL_RED else p.fg.copy(alpha = 0.65f))
    val tempRatio = ((t.coolantTempCelsius - 40.0) / 80.0).toFloat().coerceIn(0f, 1f)
    drawLinearBar(tempX + iconR * 1.7f, top + railH * 0.28f, barW, barH, tempRatio, if (t.coolantTempCelsius > 108.0) RL_RED else p.fg, p.track, 0.82f, RL_RED, barH * 0.45f)
    gText("TEMP ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", tempX + iconR * 1.7f, yBot, small, p.muted, Paint.Align.LEFT, family, true)

    // 4. GNSS Uydu bloğu
    drawDivider(ctx.w * 0.462f)
    val gpsX = ctx.w * 0.478f
    val sat = t.gpsSatelliteCount.coerceIn(0, 24)
    val satColor = when {
        sat >= 6 -> RL_GREEN
        sat >= 3 -> RL_AMBER
        else -> p.fg.copy(alpha = 0.45f)
    }
    drawGpsIcon(gpsX, top + railH * 0.36f, iconR * 0.90f, satColor)
    for (i in 0 until 4) {
        val bh = railH * (0.12f + i * 0.065f)
        val on = sat >= (i + 1) * 2
        drawRoundRect(
            if (on) satColor else p.fg.copy(alpha = 0.14f),
            Offset(gpsX + iconR * 1.55f + i * iconR * 0.56f, top + railH * 0.50f - bh),
            Size(iconR * 0.36f, bh),
            androidx.compose.ui.geometry.CornerRadius(iconR * 0.12f, iconR * 0.12f)
        )
    }
    gText("$sat SAT", gpsX + iconR * 1.55f, yBot, small, p.muted, Paint.Align.LEFT, family, true)

    // 5. Yatış (Lean Angle) bloğu
    drawDivider(ctx.w * 0.558f)
    val lean = t.leanAngleDeg
    val leanTxt = "${if (lean < 0) "L" else "R"} ${abs(lean).roundToInt()}°"
    val leanCenterX = ctx.w * 0.598f
    gText(leanTxt, leanCenterX, yTop, textSize, p.accent, Paint.Align.CENTER, family, true)
    gText("LEAN", leanCenterX, yBot, small, p.muted, Paint.Align.CENTER, family, true)

    // 6. Kalıcı Sabit KM / Mil Sayacı Bloğu (Tüm kadranlarda aynı koordinatta, üstünü kapatmaz)
    drawDivider(ctx.w * 0.638f)
    val tripColX = ctx.w * 0.652f
    gText("SEANS ${ctx.formatSession()}", tripColX, yTop, small * 1.06f, p.accent, Paint.Align.LEFT, family, true)
    gText("TRIP A ${ctx.formatTrip()}", tripColX, yBot, small, p.fg.copy(alpha = 0.78f), Paint.Align.LEFT, family, true)

    drawDivider(ctx.w * 0.805f)
    val right = ctx.w - pad
    gText("ODO ${ctx.formatOdometer()}", right, yTop, textSize, p.fg.copy(alpha = 0.94f), Paint.Align.RIGHT, family, true)
    gText("TRIP B ${ctx.formatTripB()}", right, yBot, small, p.muted, Paint.Align.RIGHT, family, true)
}

internal enum class RailStyle { TFT, LCD, CLASSIC }

/** OEM kadran saati: dakikada bir güncellenir, her karede tarih biçimlendirmez. */
private var clusterClockMinute: Long = -1L
private var clusterClockValue: String = "00:00"
private var clusterClock24h: Boolean = true

internal fun clusterClockText(clock24h: Boolean = true): String {
    val minute = System.currentTimeMillis() / 60_000L
    if (minute != clusterClockMinute || clock24h != clusterClock24h) {
        clusterClockMinute = minute
        clusterClock24h = clock24h
        val pattern = if (clock24h) "HH:mm" else "h:mm a"
        clusterClockValue = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
            .format(java.util.Date(minute * 60_000L))
    }
    return clusterClockValue
}

/** Yakıt tüketimi birimi: "l100km" | "kml" | "mpg". Girdi L/100km'dir. */
internal fun formatFuelConsumption(lPer100Km: Double, unitCode: String): String {
    val safe = if (lPer100Km > 0.0) lPer100Km else 4.8
    return when (unitCode.lowercase()) {
        "kml" -> String.format(java.util.Locale.US, "%.1f km/L", 100.0 / safe)
        "mpg" -> String.format(java.util.Locale.US, "%.1f mpg", 235.214583 / safe)
        else -> String.format(java.util.Locale.US, "%.1f L/100", safe)
    }
}

internal fun formatKm(value: Double): String {
    if (!value.isFinite() || value < 0.0) return "0.0"
    return if (value >= 10000.0) String.format(java.util.Locale.US, "%.0f", value)
    else String.format(java.util.Locale.US, "%.1f", value)
}

private fun DrawScope.drawTurnIcon(x: Float, y: Float, r: Float, color: Color, right: Boolean) {
    val dir = if (right) 1f else -1f
    val p = Path()
    p.moveTo(x + dir * r, y)
    p.lineTo(x - dir * r * 0.25f, y - r * 0.78f)
    p.lineTo(x - dir * r * 0.25f, y - r * 0.26f)
    p.lineTo(x - dir * r * 0.9f, y - r * 0.26f)
    p.lineTo(x - dir * r * 0.9f, y + r * 0.26f)
    p.lineTo(x - dir * r * 0.25f, y + r * 0.26f)
    p.lineTo(x - dir * r * 0.25f, y + r * 0.78f)
    p.close()
    drawPath(p, color)
}

private fun DrawScope.drawHeadlightIcon(x: Float, y: Float, r: Float, color: Color) {
    drawCircle(color, r * 0.68f, Offset(x, y), style = Stroke(width = r * 0.24f))
    drawLine(color, Offset(x - r * 1.1f, y - r * 0.55f), Offset(x - r * 0.55f, y - r * 0.3f), r * 0.22f)
    drawLine(color, Offset(x - r * 1.1f, y + r * 0.55f), Offset(x - r * 0.55f, y + r * 0.3f), r * 0.22f)
    drawLine(color, Offset(x - r * 1.1f, y), Offset(x - r * 0.62f, y), r * 0.22f)
}

private fun DrawScope.drawOilIcon(x: Float, y: Float, r: Float, color: Color) {
    val p = Path()
    p.moveTo(x - r * 0.15f, y - r * 0.95f)
    p.quadraticTo(x + r * 0.95f, y + r * 0.1f, x + r * 0.5f, y + r * 0.75f)
    p.quadraticTo(x, y + r * 1.2f, x - r * 0.5f, y + r * 0.75f)
    p.quadraticTo(x - r * 0.95f, y + r * 0.1f, x - r * 0.15f, y - r * 0.95f)
    p.close()
    drawPath(p, color, style = Stroke(width = r * 0.24f))
}

private fun DrawScope.drawFuelIcon(x: Float, y: Float, r: Float, color: Color) {
    drawRoundRect(color, Offset(x - r * 0.7f, y - r * 0.8f), Size(r * 1.25f, r * 1.6f), androidx.compose.ui.geometry.CornerRadius(r * 0.16f, r * 0.16f), style = Stroke(width = r * 0.2f))
    drawLine(color, Offset(x + r * 0.55f, y - r * 0.15f), Offset(x + r * 1.05f, y + r * 0.35f), r * 0.2f)
    drawLine(color, Offset(x + r * 1.05f, y + r * 0.35f), Offset(x + r * 1.05f, y + r * 0.85f), r * 0.2f)
    drawLine(color, Offset(x - r * 0.45f, y - r * 0.3f), Offset(x + r * 0.15f, y - r * 0.3f), r * 0.18f)
}

private fun DrawScope.drawTempIcon(x: Float, y: Float, r: Float, color: Color) {
    drawRoundRect(color, Offset(x - r * 0.22f, y - r * 0.9f), Size(r * 0.44f, r * 1.35f), androidx.compose.ui.geometry.CornerRadius(r * 0.22f, r * 0.22f), style = Stroke(width = r * 0.19f))
    drawCircle(color, r * 0.42f, Offset(x, y + r * 0.62f), style = Stroke(width = r * 0.19f))
    drawLine(color, Offset(x, y - r * 0.35f), Offset(x, y + r * 0.55f), r * 0.16f)
}

private fun DrawScope.drawGpsIcon(x: Float, y: Float, r: Float, color: Color) {
    drawCircle(color, r * 0.42f, Offset(x, y), style = Stroke(width = r * 0.18f))
    drawLine(color, Offset(x - r * 0.9f, y), Offset(x + r * 0.9f, y), r * 0.14f)
    drawLine(color, Offset(x, y - r * 0.9f), Offset(x, y + r * 0.9f), r * 0.14f)
    drawCircle(color, r * 0.13f, Offset(x, y))
}

internal fun DrawScope.drawLeanArc(
    cx: Float,
    cy: Float,
    radius: Float,
    leanDeg: Double,
    color: Color,
    track: Color,
    u: Float
) {
    drawArcTrack(cx, cy, radius, 150f, 240f, u * 1.1f, track)
    val center = 270f
    val sweep = (leanDeg / 60.0 * 120.0).toFloat().coerceIn(-120f, 120f)
    drawArcSegment(cx, cy, radius, center, sweep, u * 1.4f, color, StrokeCap.Round)
    val tip = polar(cx, cy, radius, center + sweep)
    drawCircle(color, u * 0.9f, tip)
}

internal fun DrawScope.drawWheelieCard(
    ctx: GaugeContext,
    cx: Float,
    cy: Float,
    width: Float
) {
    val t = ctx.telemetry
    val p = ctx.palette
    val height = width * 0.42f
    drawRoundRect(p.bg.copy(alpha = 0.90f), Offset(cx - width * 0.5f, cy - height * 0.5f), Size(width, height), androidx.compose.ui.geometry.CornerRadius(width * 0.045f, width * 0.045f))
    drawRoundRect(p.accent, Offset(cx - width * 0.5f, cy - height * 0.5f), Size(width, height), androidx.compose.ui.geometry.CornerRadius(width * 0.045f, width * 0.045f), style = Stroke(width = ctx.u * 0.4f))
    gText("WHEELIE", cx - width * 0.43f, cy - height * 0.22f, height * 0.20f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.14f)
    gText("${t.wheelieAngleDeg.roundToInt()}°", cx - width * 0.43f, cy + height * 0.16f, height * 0.46f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("${String.format(java.util.Locale.US, "%.1f", t.currentWheelieDurationSec)} s", cx + width * 0.02f, cy + height * 0.08f, height * 0.22f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
    gText("${t.currentWheelieDistanceMeters.roundToInt()} m", cx + width * 0.02f, cy + height * 0.32f, height * 0.18f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    // mini tepe çizgisi
    val bx = cx + width * 0.22f
    val bw = width * 0.22f
    drawLinearBar(bx, cy - height * 0.22f, bw, height * 0.08f, (t.wheelieAngleDeg / 60.0).toFloat().coerceIn(0f, 1f), p.accent, p.track, 0.85f, RL_RED, height * 0.04f)
    gText("MAX ${t.maxWheelieAngleDeg.roundToInt()}°", bx + bw * 0.5f, cy + height * 0.02f, height * 0.13f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    gText("${String.format(java.util.Locale.US, "%.1f", t.longestWheelieDurationSec)} s / ${t.longestWheelieDistanceMeters.roundToInt()} m", bx + bw * 0.5f, cy + height * 0.24f, height * 0.12f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
}

internal fun DrawScope.drawSubModeChip(
    ctx: GaugeContext,
    x: Float,
    y: Float,
    alignRight: Boolean = false,
    small: Boolean = false,
    alignCenter: Boolean = false
) {
    val text = ctx.subMode.uppercase()
    val size = if (small) ctx.u * 1.8f else ctx.u * 2.1f
    val padX = size * 0.7f
    val width = gTextWidth(text, size, ctx.type.label, true, 0.08f) + padX * 2f
    val height = size * 2.1f
    val left = when {
        alignCenter -> x - width * 0.5f
        alignRight -> x - width
        else -> x
    }
    drawRoundRect(
        ctx.palette.accent.copy(alpha = 0.14f),
        Offset(left, y - height * 0.5f),
        Size(width, height),
        androidx.compose.ui.geometry.CornerRadius(height * 0.22f, height * 0.22f)
    )
    drawRoundRect(
        ctx.palette.accent.copy(alpha = 0.45f),
        Offset(left, y - height * 0.5f),
        Size(width, height),
        androidx.compose.ui.geometry.CornerRadius(height * 0.22f, height * 0.22f),
        style = Stroke(width = max(1f, ctx.u * 0.15f))
    )
    gText(text, left + width * 0.5f, y + size * 0.35f, size, ctx.palette.accent, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.08f)
}

internal fun DrawScope.drawPanel(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    palette: GaugePalette,
    accentEdge: Boolean = false,
    radius: Float = 0f
) {
    val r = if (radius > 0f) radius else min(width, height) * 0.06f
    drawRoundRect(
        palette.panel,
        Offset(left, top),
        Size(width, height),
        androidx.compose.ui.geometry.CornerRadius(r, r)
    )
    drawRoundRect(
        if (accentEdge) palette.accent.copy(alpha = 0.35f) else palette.fg.copy(alpha = 0.10f),
        Offset(left, top),
        Size(width, height),
        androidx.compose.ui.geometry.CornerRadius(r, r),
        style = Stroke(width = max(1f, r * 0.08f))
    )
}

internal fun DrawScope.drawPanel(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    fillColor: Color,
    borderColor: Color,
    radius: Float = 0f
) {
    val r = if (radius > 0f) radius else min(width, height) * 0.06f
    drawRoundRect(
        fillColor,
        Offset(left, top),
        Size(width, height),
        androidx.compose.ui.geometry.CornerRadius(r, r)
    )
    drawRoundRect(
        borderColor,
        Offset(left, top),
        Size(width, height),
        androidx.compose.ui.geometry.CornerRadius(r, r),
        style = Stroke(width = max(1f, r * 0.09f))
    )
}

internal fun DrawScope.drawGearBadge(
    gearText: String,
    isNeutral: Boolean,
    cx: Float,
    cy: Float,
    size: Float,
    accent: Color,
    fg: Color,
    family: String = "sans-serif-condensed"
) {
    val bg = if (isNeutral) RL_GREEN.copy(alpha = 0.16f) else accent.copy(alpha = 0.14f)
    val edge = if (isNeutral) RL_GREEN.copy(alpha = 0.72f) else accent.copy(alpha = 0.60f)
    val textCol = if (isNeutral) RL_GREEN else fg
    val boxW = size * 1.12f
    val boxH = size * 1.06f
    val rad = size * 0.14f
    drawRoundRect(
        bg,
        Offset(cx - boxW * 0.5f, cy - boxH * 0.5f),
        Size(boxW, boxH),
        androidx.compose.ui.geometry.CornerRadius(rad, rad)
    )
    drawRoundRect(
        edge,
        Offset(cx - boxW * 0.5f, cy - boxH * 0.5f),
        Size(boxW, boxH),
        androidx.compose.ui.geometry.CornerRadius(rad, rad),
        style = Stroke(width = max(1.2f, size * 0.045f))
    )
    gText(
        text = if (isNeutral) "N" else gearText,
        x = cx,
        y = cy + size * 0.35f,
        size = size * 0.86f,
        color = textCol,
        align = Paint.Align.CENTER,
        family = family,
        bold = true
    )
}

internal fun DrawScope.drawCrosshairTexture(palette: GaugePalette, w: Float, h: Float, spacing: Float) {
    var x = 0f
    while (x <= w) {
        drawLine(palette.fg.copy(alpha = 0.028f), Offset(x, 0f), Offset(x, h), 1f)
        x += spacing
    }
    var y = 0f
    while (y <= h) {
        drawLine(palette.fg.copy(alpha = 0.028f), Offset(0f, y), Offset(w, y), 1f)
        y += spacing
    }
}

internal fun DrawScope.drawCompassStrip(ctx: GaugeContext, top: Float, height: Float) {
    val p = ctx.palette
    val w = ctx.w
    drawRect(p.fg.copy(alpha = 0.035f), Offset(0f, top), Size(w, height))
    val labels = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val center = w * 0.5f
    for (i in -6..6) {
        val idx = ((i % 8) + 8) % 8
        val x = center + i * w * 0.075f
        if (x < -w * 0.08f || x > w * 1.08f) continue
        val isMajor = i % 2 == 0
        drawLine(
            p.fg.copy(alpha = if (isMajor) 0.35f else 0.18f),
            Offset(x, top + height * if (isMajor) 0.25f else 0.42f),
            Offset(x, top + height * 0.92f),
            max(1f, ctx.u * 0.18f)
        )
        if (i == 0) {
            gText("N", x, top + height * 0.62f, height * 0.36f, p.accent, Paint.Align.CENTER, ctx.type.label, true)
        } else if (isMajor) {
            gText(labels[idx], x, top + height * 0.62f, height * 0.30f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        }
    }
    drawLine(p.accent, Offset(center, top + height * 0.05f), Offset(center, top + height * 0.20f), max(1f, ctx.u * 0.35f))
}

internal fun DrawScope.drawTemperatureAndFuel(
    ctx: GaugeContext,
    x: Float,
    y: Float,
    width: Float,
    compact: Boolean = false
) {
    val p = ctx.palette
    val t = ctx.telemetry
    val h = if (compact) ctx.u * 0.9f else ctx.u * 1.4f
    val gap = ctx.u * 2.4f
    drawLinearBar(x, y, width, h, t.fuelLevelRatio, p.accent, p.track, 0.9f, RL_RED, h * 0.5f)
    gText("E", x - ctx.u * 1.2f, y + h * 0.9f, ctx.u * 1.5f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    gText("F", x + width + ctx.u * 0.5f, y + h * 0.9f, ctx.u * 1.5f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    drawLinearBar(x, y + gap, width, h, ((t.coolantTempCelsius - 40) / 80).toFloat().coerceIn(0f, 1f), if (t.coolantTempCelsius > 108.0) RL_RED else p.fg, p.track, 0.85f, RL_RED, h * 0.5f)
    gText("C", x - ctx.u * 1.2f, y + gap + h * 0.9f, ctx.u * 1.5f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    gText("H", x + width + ctx.u * 0.5f, y + gap + h * 0.9f, ctx.u * 1.5f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    if (!compact) {
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", x + width * 0.5f, y + gap + h + ctx.u * 2.1f, ctx.u * 1.7f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
    }
}
