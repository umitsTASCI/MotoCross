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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.oemcluster.pro.catalog.DeclarativeGaugeSpec
import com.oemcluster.pro.runtime.DistanceFormatter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/* =====================================================================================
 *  1) BOOMERANG BAR TFT — KTM Duke/RC, MT-09 Gen4, 250 Duke
 * ===================================================================================== */
internal fun DrawScope.drawBoomerangTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val variant = skinVariant(ctx.spec.skinId, 3)
    val railH = ctx.h * 0.082f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.4f, ctx.w * 0.34f, ctx.u)

    val rawAnchors: List<Offset> = when {
        !ctx.wide -> listOf(
            Offset(ctx.w * 0.08f, ctx.h * 0.70f),
            Offset(ctx.w * 0.12f, ctx.h * 0.21f),
            Offset(ctx.w * 0.88f, ctx.h * 0.21f)
        )
        variant == 0 -> listOf(
            Offset(ctx.w * 0.065f, ctx.h * 0.72f),
            Offset(ctx.w * 0.115f, ctx.h * 0.22f),
            Offset(ctx.w * 0.91f, ctx.h * 0.22f)
        )
        variant == 1 -> listOf(
            Offset(ctx.w * 0.055f, ctx.h * 0.42f),
            Offset(ctx.w * 0.165f, ctx.h * 0.155f),
            Offset(ctx.w * 0.93f, ctx.h * 0.155f)
        )
        else -> listOf(
            Offset(ctx.w * 0.055f, ctx.h * 0.66f),
            Offset(ctx.w * 0.145f, ctx.h * 0.185f),
            Offset(ctx.w * 0.91f, ctx.h * 0.205f)
        )
    }
    val points = rememberBoomerangCurve(rawAnchors[0], rawAnchors[1], rawAnchors[2])

    val bandWidth = ctx.u * (if (variant == 2) 4.1f else 3.7f) * (1f + skinJitter(ctx.spec.skinId, 11) * 0.14f)

    drawPolylineBar(
        points = points,
        segments = 48,
        filled = ctx.rpmRatio,
        width = bandWidth,
        color = if (t.shiftLightState == com.oemcluster.pro.domain.ShiftLightState.NORMAL) p.accent else p.stage1,
        track = p.track,
        redlineRatio = ctx.redlineRatio,
        peakRatio = ctx.peakRatio,
        u = ctx.u
    )

    // Bar üzerindeki devir rakamları (normal vektörü boyunca dışa/içe temiz ofset)
    val labelCount = if (ctx.maxRpm <= 11000.0) (ctx.maxRpm / 1000.0).roundToInt().coerceIn(5, 10) else 6
    val labelOffsetSign = if (variant == 1) 1f else -1f
    val labelDist = ctx.u * (if (variant == 1) 4.4f else 4.0f)
    for (i in 0..labelCount) {
        val frac = i / labelCount.toFloat()
        val (pt, nrm) = pointAndNormalAt(points, frac)
        val rpm = (ctx.maxRpm * frac / 1000.0).roundToInt()
        val lx = pt.x + nrm.x * labelDist * labelOffsetSign
        val ly = pt.y + nrm.y * labelDist * labelOffsetSign + ctx.u * 0.7f
        gText(
            rpm.toString(),
            lx,
            ly,
            ctx.u * 2.0f,
            if (frac >= ctx.redlineRatio) RL_RED else p.muted,
            Paint.Align.CENTER,
            ctx.type.label,
            true
        )
    }
    gText("x1000 r/min", points.last().x, points.last().y + ctx.u * 5.2f, ctx.u * 1.50f, p.muted, Paint.Align.RIGHT, ctx.type.label, false)

    // Hız + vites kompozisyonu
    when (variant) {
        0 -> {
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.56f, ctx.h * 0.63f, ctx.u * 19.5f, p.fg, p.muted, ctx.type.digits)
            drawGearPod(ctx.w * 0.20f, ctx.h * 0.58f, ctx.u * 12.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
            gText("GEAR", ctx.w * 0.20f, ctx.h * 0.715f, ctx.u * 1.75f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.16f)
        }
        1 -> {
            drawGearPod(ctx.w * 0.80f, ctx.h * 0.55f, ctx.u * 13.5f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.45f, ctx.h * 0.66f, ctx.u * 21.5f, p.fg, p.muted, ctx.type.digits)
            gText("GEAR", ctx.w * 0.80f, ctx.h * 0.68f, ctx.u * 1.8f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.16f)
        }
        else -> {
            drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.48f, ctx.h * 0.64f, ctx.u * 20.5f, p.fg, p.muted, ctx.type.digits)
            val gearX = if (ctx.wide) ctx.w * 0.77f else ctx.w * 0.50f
            val gearY = if (ctx.wide) ctx.h * 0.56f else ctx.h * 0.78f
            drawGearPod(gearX, gearY, ctx.u * 11.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.HEX)
        }
    }

    if (ctx.wide) {
        // Sol alt: OEM entegre yol bilgisayarı (ODO / TRIP A / SEANS)
        val odoCardX = if (variant == 0) ctx.w * 0.045f else ctx.w * 0.045f
        val odoCardY = if (variant == 0) ctx.h * 0.745f else ctx.h * 0.695f
        gText("ODO  ${ctx.formatOdometer()}", odoCardX, odoCardY, ctx.u * 1.85f, p.fg.copy(alpha = 0.92f), Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}   •   SEANS  ${ctx.formatSession()}", odoCardX, odoCardY + ctx.u * 2.5f, ctx.u * 1.50f, p.accent, Paint.Align.LEFT, ctx.type.label, true)

        // Sağ sütun: yakıt/hararet dikey barları
        val bx = ctx.w * 0.945f
        val by = ctx.h * 0.38f
        val bh = ctx.h * 0.28f
        drawLinearBar(bx, by, ctx.u * 1.5f, bh, t.fuelLevelRatio, p.accent, p.track, 0.88f, RL_RED, ctx.u * 0.7f, vertical = true)
        drawLinearBar(bx - ctx.u * 3.2f, by, ctx.u * 1.5f, bh, ((t.coolantTempCelsius - 40) / 80).toFloat().coerceIn(0f, 1f), p.fg, p.track, 0.82f, RL_RED, ctx.u * 0.7f, vertical = true)
        gText("F", bx + ctx.u * 0.75f, by - ctx.u * 1.2f, ctx.u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        gText("E", bx + ctx.u * 0.75f, by + bh + ctx.u * 2.8f, ctx.u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", bx - ctx.u * 2.45f, by + bh + ctx.u * 2.8f, ctx.u * 1.45f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawBrandMark(ctx.spec, p, ctx.w * 0.88f, ctx.h * 0.43f, ctx.u * 5.0f, ctx.u, p.accent.copy(alpha = 0.55f))
        gText(ctx.spec.modelName.substringBefore(" (").uppercase(), ctx.w * 0.90f, ctx.h * 0.50f, ctx.u * 1.3f, p.muted, Paint.Align.RIGHT, ctx.type.label, false)
    } else {
        drawTemperatureAndFuel(ctx, ctx.w * 0.12f, ctx.h * 0.785f, ctx.w * 0.46f, compact = true)
        drawBrandMark(ctx.spec, p, ctx.w * 0.85f, ctx.h * 0.82f, ctx.u * 5.0f, ctx.u, p.accent.copy(alpha = 0.5f))
    }

    drawSubModeChip(ctx, ctx.w * 0.045f, ctx.h - railH - ctx.u * 3.2f, small = true)
    gText(
        "#${ctx.spec.modelNumber}  ${ctx.spec.generationYears}",
        ctx.w * 0.955f,
        ctx.h - railH - ctx.u * 3.2f,
        ctx.u * 1.55f,
        p.muted,
        Paint.Align.RIGHT,
        ctx.type.label,
        false
    )
    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  2) CURVED SWEEP BAR TFT — Yamaha R1/R7/R125, MT-10, CFMoto 450SR
 * ===================================================================================== */
internal fun DrawScope.drawCurvedSweepTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val variant = skinVariant(ctx.spec.skinId, 3)
    val railH = ctx.h * 0.072f

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.4f, ctx.w * 0.30f, ctx.u)

    val jit = skinJitter(ctx.spec.skinId, 21)
    val cx = ctx.w * 0.50f
    val cy = if (ctx.wide) ctx.h * (0.60f + jit * 0.02f) else ctx.h * 0.57f
    val radiusBase = if (ctx.wide) min(ctx.w * 0.38f, ctx.h * 0.48f) else min(ctx.w * 0.45f, ctx.h * 0.34f)
    val radius = radiusBase * (1f + jit * 0.09f)
    val start = if (variant == 1) 196f else 202f + jit * 3.0f
    val sweep = if (variant == 1) 148f else 136f + jit * 4.0f

    drawSegmentedArc(
        cx, cy, radius, start, sweep, 52, ctx.rpmRatio, ctx.u * 4.4f,
        if (t.shiftLightState == com.oemcluster.pro.domain.ShiftLightState.NORMAL) p.accent else p.stage1,
        p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.9f
    )
    // İç ince halka
    drawArcTrack(cx, cy, radius - ctx.u * 4.0f, start, sweep, ctx.u * 0.45f, p.fg.copy(alpha = 0.10f))

    // Devir etiketleri
    val labels = 7
    for (i in 0..labels) {
        val frac = i / labels.toFloat()
        val angle = start + sweep * frac
        val pt = polar(cx, cy, radius - ctx.u * 9.6f, angle)
        val rpm = (ctx.maxRpm * frac / 1000.0).roundToInt()
        gTextCenter(rpm.toString(), pt.x, pt.y, ctx.u * 2.05f, if (frac >= ctx.redlineRatio) RL_RED else p.muted, ctx.type.label)
    }

    // Hız kompozisyonu yayın içinde
    val speedY = if (variant == 1) cy - ctx.u * 2.0f else cy - ctx.u * 8.0f
    drawSpeed(ctx.speedValue, ctx.speedUnit, cx, speedY, if (ctx.wide) ctx.u * 17.5f else ctx.u * 16.0f, p.fg, p.muted, ctx.type.digits)

    if (variant == 2) {
        // çift halka: dış devir, iç hız yüzdesi
        drawArcTrack(cx, cy, radius - ctx.u * 5.2f, start, sweep, ctx.u * 1.1f, p.accent.copy(alpha = 0.25f))
        val spdRatio = (ctx.speedValue.toFloat() / ctx.speedScaleMax).coerceIn(0f, 1f)
        drawArcSegment(cx, cy, radius - ctx.u * 5.2f, start, sweep * spdRatio, ctx.u * 1.1f, p.accent.copy(alpha = 0.9f))
    }

    val gearY = if (ctx.wide) cy + radius * 0.44f else cy + radius * 0.50f
    drawGearPod(cx, gearY, if (ctx.wide) ctx.u * 11.5f else ctx.u * 10.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.HEX)
    gText("GEAR", cx, gearY + ctx.u * 8.6f, ctx.u * 1.6f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)

    if (ctx.wide) {
        // Sol alt: OEM ODO / TRIP / SEANS paneli
        drawPanel(ctx.w * 0.035f, ctx.h * 0.58f, ctx.w * 0.185f, ctx.h * 0.195f, p, accentEdge = true)
        gText("ODO", ctx.w * 0.052f, ctx.h * 0.622f, ctx.u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.14f)
        gText(ctx.formatOdometer(), ctx.w * 0.052f, ctx.h * 0.672f, ctx.u * 3.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.052f, ctx.h * 0.715f, ctx.u * 1.45f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.052f, ctx.h * 0.752f, ctx.u * 1.40f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

        // Sağ alt: güç/tork
        drawPanel(ctx.w * 0.795f, ctx.h * 0.60f, ctx.w * 0.17f, ctx.h * 0.17f, p, accentEdge = true)
        gText("POWER", ctx.w * 0.815f, ctx.h * 0.645f, ctx.u * 1.6f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.14f)
        gText("${t.estimatedWheelHorsepower.roundToInt()}", ctx.w * 0.815f, ctx.h * 0.705f, ctx.u * 4.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("hp  •  ${t.estimatedWheelTorqueNm.roundToInt()} Nm", ctx.w * 0.815f, ctx.h * 0.745f, ctx.u * 1.45f, p.accent, Paint.Align.LEFT, ctx.type.label, true)

        drawBrandMark(ctx.spec, p, ctx.w * 0.50f, ctx.h * 0.115f, ctx.u * 4.6f, ctx.u, p.accent.copy(alpha = 0.7f))
        gText(ctx.spec.modelName.uppercase(), ctx.w * 0.50f, ctx.h * 0.165f, ctx.u * 1.35f, p.muted, Paint.Align.CENTER, ctx.type.label, false)
    } else {
        drawTemperatureAndFuel(ctx, ctx.w * 0.12f, ctx.h * 0.77f, ctx.w * 0.44f, compact = true)
        drawSubModeChip(ctx, ctx.w * 0.55f, ctx.h * 0.795f, alignRight = true, small = true)
    }

    gText(
        "RPM ${ctx.mappedRpm.roundToInt()}",
        ctx.w * 0.955f, ctx.h - railH - ctx.u * 3.6f, ctx.u * 1.6f, p.muted, Paint.Align.RIGHT, ctx.type.label, true
    )
    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  3) CIRCULAR RING TFT — Fireblade, Panigale V2, GSX-8S, Brutale, Speed Triple
 * ===================================================================================== */
internal fun DrawScope.drawCircularRingTft(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val variant = skinVariant(ctx.spec.skinId, 3)
    val railH = ctx.h * 0.082f
    val ringStepRpm = if (ctx.maxRpm <= 11000.0) 1000 else 2000

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.4f, ctx.w * 0.28f, ctx.u)

    if (ctx.wide) {
        when (variant) {
            0 -> {
                // Tek büyük merkez halka
                val cx = ctx.w * 0.50f
                val cy = ctx.h * 0.50f
                val r = min(ctx.w * 0.30f, ctx.h * 0.38f)
                drawSegmentedArc(cx, cy, r, 138f, 264f, 54, ctx.rpmRatio, ctx.u * 4.0f, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.85f)
                drawArcTrack(cx, cy, r - ctx.u * 4.2f, 138f, 264f, ctx.u * 0.5f, p.fg.copy(alpha = 0.10f))
                drawRingRpmLabels(ctx, cx, cy, r - ctx.u * 8.4f, 138f, 264f, ringStepRpm)
                drawBrandMark(ctx.spec, p, cx, cy - r * 0.54f, ctx.u * 4.2f, ctx.u, p.accent.copy(alpha = 0.65f))
                gText("MODE ${ctx.subMode.uppercase()}", cx, cy - r * 0.31f, ctx.u * 1.50f, p.accent, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)
                drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + r * 0.22f, ctx.u * 18.5f, p.fg, p.muted, ctx.type.digits)
                drawGearPod(cx, cy + r * 0.62f, ctx.u * 10.5f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.ROUND)
                drawSideStats(ctx, railH)
            }
            1 -> {
                // İki halka: rpm solda, hız sağda
                val leftCx = ctx.w * 0.27f
                val rightCx = ctx.w * 0.73f
                val cy = ctx.h * 0.51f
                val r = min(ctx.w * 0.20f, ctx.h * 0.33f)
                drawSegmentedArc(leftCx, cy, r, 140f, 260f, 40, ctx.rpmRatio, ctx.u * 3.4f, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.9f)
                drawRingRpmLabels(ctx, leftCx, cy, r - ctx.u * 7.6f, 140f, 260f, ringStepRpm)
                gText("x1000 RPM", leftCx, cy + r * 0.58f, ctx.u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)
                drawGearPod(leftCx, cy + r * 0.20f, ctx.u * 9.5f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.HEX)

                drawArcTrack(rightCx, cy, r, 140f, 260f, ctx.u * 3.4f, p.track)
                val spdRatio = (ctx.speedValue.toFloat() / ctx.speedScaleMax).coerceIn(0f, 1f)
                drawArcSegment(rightCx, cy, r, 140f, 260f * spdRatio, ctx.u * 3.4f, p.accent)
                drawSpeed(ctx.speedValue, ctx.speedUnit, rightCx, cy + r * 0.18f, ctx.u * 15.5f, p.fg, p.muted, ctx.type.digits)
                gText("SPEED", rightCx, cy - r * 0.62f, ctx.u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.14f)
                drawBrandMark(ctx.spec, p, ctx.w * 0.50f, ctx.h * 0.20f, ctx.u * 5.4f, ctx.u, p.accent.copy(alpha = 0.65f))
                // İki halka arası OEM ODO / TRIP kartı
                drawPanel(ctx.w * 0.41f, ctx.h * 0.68f, ctx.w * 0.18f, ctx.h * 0.18f, p, accentEdge = true)
                gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.50f, ctx.h * 0.735f, ctx.u * 1.65f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
                gText("TRIP A ${ctx.formatTrip()}", ctx.w * 0.50f, ctx.h * 0.785f, ctx.u * 1.45f, p.accent, Paint.Align.CENTER, ctx.type.label, true)
                gText("SEANS ${ctx.formatSession()}", ctx.w * 0.50f, ctx.h * 0.830f, ctx.u * 1.35f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
            }
            else -> {
                // Konsantrik halkalar
                val cx = ctx.w * 0.48f
                val cy = ctx.h * 0.50f
                val r = min(ctx.w * 0.27f, ctx.h * 0.38f)
                drawSegmentedArc(cx, cy, r, 130f, 280f, 56, ctx.rpmRatio, ctx.u * 3.0f, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.8f)
                drawArcTrack(cx, cy, r - ctx.u * 5.2f, 130f, 280f, ctx.u * 1.4f, p.fg.copy(alpha = 0.08f))
                val spdRatio = (ctx.speedValue.toFloat() / ctx.speedScaleMax).coerceIn(0f, 1f)
                drawArcSegment(cx, cy, r - ctx.u * 5.2f, 130f, 280f * spdRatio, ctx.u * 1.4f, p.fg.copy(alpha = 0.65f))
                drawRingRpmLabels(ctx, cx, cy, r - ctx.u * 8.6f, 130f, 280f, ringStepRpm)
                drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + ctx.u * 5.0f, ctx.u * 17.0f, p.fg, p.muted, ctx.type.digits)
                drawGearPod(ctx.w * 0.78f, ctx.h * 0.52f, ctx.u * 12.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
                gText("GEAR", ctx.w * 0.78f, ctx.h * 0.64f, ctx.u * 1.65f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.16f)
                drawSideStats(ctx, railH)
            }
        }
    } else {
        val cx = ctx.w * 0.50f
        val cy = ctx.h * 0.44f
        val r = min(ctx.w * 0.40f, ctx.h * 0.29f)
        drawSegmentedArc(cx, cy, r, 138f, 264f, 48, ctx.rpmRatio, ctx.u * 3.8f, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.9f)
        drawRingRpmLabels(ctx, cx, cy, r - ctx.u * 8.0f, 138f, 264f, ringStepRpm)
        drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + ctx.u * 3.5f, ctx.u * 17.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(cx, cy + r * 0.65f, ctx.u * 10.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.ROUND)
        gText("MODE ${ctx.subMode.uppercase()}", cx, cy - r * 0.62f, ctx.u * 1.6f, p.accent, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)
        drawTemperatureAndFuel(ctx, ctx.w * 0.12f, ctx.h * 0.76f, ctx.w * 0.42f, compact = true)
        drawBrandMark(ctx.spec, p, ctx.w * 0.85f, ctx.h * 0.79f, ctx.u * 5.4f, ctx.u, p.accent.copy(alpha = 0.5f))
    }

    drawStatusRail(ctx, RailStyle.TFT)
}

private fun DrawScope.drawRingRpmLabels(
    ctx: GaugeContext,
    cx: Float,
    cy: Float,
    radius: Float,
    start: Float,
    sweep: Float,
    stepRpm: Int = 2000
) {
    // Gerçek kadranlar gibi eşit aralıklı rakamlar
    var rpm = 0
    val maxRound = ctx.maxRpm.roundToInt()
    while (rpm <= maxRound) {
        val frac = rpm / ctx.maxRpm.toFloat()
        val angle = start + sweep * frac
        val pt = polar(cx, cy, radius, angle)
        gTextCenter(
            (rpm / 1000).toString(), pt.x, pt.y, ctx.u * 2.0f,
            if (frac >= ctx.redlineRatio) RL_RED else ctx.palette.muted, ctx.type.label
        )
        rpm += stepRpm
    }
}

private fun DrawScope.drawSideStats(ctx: GaugeContext, railH: Float) {
    val p = ctx.palette
    val t = ctx.telemetry
    val top = ctx.h * 0.21f
    val w = ctx.w * 0.155f
    val h = ctx.h * 0.175f
    drawPanel(ctx.w * 0.025f, top, w, h, p)
    gText("FUEL", ctx.w * 0.042f, top + h * 0.26f, ctx.u * 1.40f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)
    drawLinearBar(ctx.w * 0.042f, top + h * 0.42f, w * 0.72f, ctx.u * 1.1f, t.fuelLevelRatio, p.accent, p.track, 0.9f, RL_RED, ctx.u * 0.55f)
    gText("${(t.fuelLevelRatio * 100).roundToInt()}%", ctx.w * 0.042f, top + h * 0.85f, ctx.u * 1.85f, p.fg, Paint.Align.LEFT, ctx.type.label, true)

    val rightPanelLeft = ctx.w - w - ctx.w * 0.025f
    val rightTextX = ctx.w - ctx.w * 0.042f
    drawPanel(rightPanelLeft, top, w, h, p)
    gText("COOLANT", rightTextX, top + h * 0.25f, ctx.u * 1.35f, p.muted, Paint.Align.RIGHT, ctx.type.label, true, spacing = 0.10f)
    gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", rightTextX, top + h * 0.64f, ctx.u * 3.0f, if (t.coolantTempCelsius > 105) RL_RED else p.fg, Paint.Align.RIGHT, ctx.type.digits, true)
    gText(if (t.tractionControlInterventionActive) "TC ACTIVE" else "TC READY", rightTextX, top + h * 0.88f, ctx.u * 1.30f, p.accent, Paint.Align.RIGHT, ctx.type.label, true)

    // Sol alt OEM ODO / TRIP / SEANS kartı
    val odoTop = ctx.h * 0.42f
    val odoH = ctx.h * 0.21f
    drawPanel(ctx.w * 0.025f, odoTop, w, odoH, p, accentEdge = true)
    gText("ODOMETER", ctx.w * 0.042f, odoTop + odoH * 0.22f, ctx.u * 1.30f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.10f)
    gText(ctx.formatOdometer(), ctx.w * 0.042f, odoTop + odoH * 0.48f, ctx.u * 2.25f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
    gText("TRIP A ${ctx.formatTrip()}", ctx.w * 0.042f, odoTop + odoH * 0.72f, ctx.u * 1.45f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
    gText("SEANS ${ctx.formatSession()}", ctx.w * 0.042f, odoTop + odoH * 0.91f, ctx.u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

    drawSubModeChip(ctx, ctx.w * 0.965f, ctx.h - railH - ctx.u * 3.8f, alignRight = true, small = true)
}

/* =====================================================================================
 *  4) SHARP HORIZONTAL EVO TFT — Panigale V4, Streetfighter V4, 1390 Super Duke
 *
 *  KTM "Bonded TFT" (390 Duke Gen3 / 1390 SDR) gerçek kadran düzenini kullanır:
 *  solda saat, üstte köşe dönen devir şeridi, sağda dev vites, ortada hız.
 *  Ducati / BMW track kadranları ise tam genişlik yatay devir şeridi kullanır.
 * ===================================================================================== */
internal fun DrawScope.drawSharpHorizontalEvo(ctx: GaugeContext) {
    if (!ctx.wide) {
        drawCompactEvo(ctx)
        drawStatusRail(ctx, RailStyle.TFT)
        return
    }
    if (ctx.spec.brand == "KTM") drawBondedTachEvo(ctx) else drawHorizontalEvo(ctx)
    drawStatusRail(ctx, RailStyle.TFT)
}

/* --- KTM 390 Duke Gen3 / 1390 Super Duke: köşe dönen devir şeridi --------------- */
private fun DrawScope.drawBondedTachEvo(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val barColor = shiftBarColor(ctx, p.fg)

    // Saat — gerçek KTM Bonded TFT'de sol üstte.
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.045f, ctx.h * 0.088f, ctx.u * 4.2f, p.fg,
        Paint.Align.LEFT, ctx.type.digits, true)
    gText(ctx.spec.modelName.substringBefore(" (").uppercase(), ctx.w * 0.48f, ctx.h * 0.068f, ctx.u * 1.55f, p.muted,
        Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)

    // Devir şeridi geometrisi — gerçek 390 Duke'te büyük yarıçaplı bir C kavisi.
    val x0 = ctx.w * 0.140f
    val x1 = ctx.w * 0.805f
    val yTop = ctx.h * 0.155f
    val yBottom = ctx.h * 0.835f
    val corner = ctx.h * 0.345f
    val band = ctx.u * 8.6f
    val geo = BondedTachGeometry(x0, yTop, yBottom, x1, corner)
    val path = geo.path()

    // Şerit gövdesi + redline bölgesi + anlık dolgu (gerçek kadranda dolgu beyazdır)
    strokePathSegment(path, 0f, 1f, band + ctx.u * 0.45f, p.fg.copy(alpha = 0.045f))
    strokePathSegment(path, 0f, 1f, band, p.fg.copy(alpha = 0.075f))
    strokePathSegment(path, ctx.redlineRatio, 1f, band, RL_RED.copy(alpha = 0.16f))
    strokePathSegment(path, 0f, ctx.rpmRatio.coerceIn(0f, 1f), band, barColor)

    // Kadran çentikleri ve rakamları — son rakam ("12") şeridin dışına taşmasın diye [0.02..0.96] içine oturtulur.
    val step = if (ctx.maxRpm >= 14000.0) 2000 else 1000
    var rpm = 0
    while (rpm <= ctx.maxRpm.roundToInt() + 1) {
        val rawFrac = (rpm / ctx.maxRpm).toFloat().coerceIn(0f, 1f)
        val geomFrac = 0.02f + rawFrac * 0.945f
        val (pt, nrm) = geo.pointAt(geomFrac)
        val inRed = rawFrac >= ctx.redlineRatio
        val tickColor = if (inRed) RL_RED.copy(alpha = 0.85f) else p.fg.copy(alpha = 0.38f)
        drawLine(
            tickColor,
            pt + nrm * (band * 0.5f + ctx.u * 0.50f),
            pt + nrm * (band * 0.5f + ctx.u * (if (rpm % 2000 == 0) 2.5f else 1.5f)),
            ctx.u * 0.26f
        )
        if (rpm > 0) {
            val inFill = rawFrac <= ctx.rpmRatio.coerceIn(0f, 1f)
            val labelColor = when {
                inFill -> Color(0xFF0C0E12).copy(alpha = 0.92f)
                inRed -> RL_RED.copy(alpha = 0.82f)
                else -> p.fg.copy(alpha = 0.68f)
            }
            gTextCenter(
                (rpm / 1000).toString(), pt.x, pt.y, ctx.u * 5.8f,
                labelColor, ctx.type.label, true
            )
        }
        rpm += step
    }
    gText("x1000 rpm", x1, yTop + band * 0.5f + ctx.u * 3.0f, ctx.u * 1.50f, p.muted,
        Paint.Align.RIGHT, ctx.type.label, true, spacing = 0.10f)

    // Dev vites — gerçek KTM'de sağ üstte, kutusuz ve saat ile çakışmaz.
    val gearText = if (ctx.isNeutral) "N" else t.gearDisplayString
    val gearColor = when {
        ctx.isNeutral -> RL_GREEN
        t.shiftLightState != com.oemcluster.pro.domain.ShiftLightState.NORMAL && t.shiftFlashPhaseOn -> RL_RED
        else -> p.fg
    }
    gText(gearText, ctx.w * 0.905f, ctx.h * 0.255f, ctx.u * 21.0f, gearColor,
        Paint.Align.CENTER, ctx.type.label, true)
    gText("GEAR", ctx.w * 0.905f, ctx.h * 0.312f, ctx.u * 1.55f, p.muted,
        Paint.Align.CENTER, ctx.type.label, true, spacing = 0.22f)

    // ABS ROAD + MTC hapları — gerçek kadranda hızın hemen altında yanyana iki gri hap.
    val pillH = ctx.u * 6.4f
    val pillW = ctx.u * 17.5f
    val pillY = ctx.h * 0.770f
    val pillPad = ctx.u * 1.1f
    val absX = ctx.w * 0.435f
    val mtcX = absX + pillW + ctx.u * 2.2f
    listOf(absX to "ROAD", mtcX to "OFF").forEach { (x, state) ->
        drawRoundRect(
            color = p.fg.copy(alpha = 0.10f),
            topLeft = Offset(x, pillY),
            size = Size(pillW, pillH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillH * 0.22f, pillH * 0.22f)
        )
    }
    gText("ABS", absX + pillPad * 1.6f, pillY + pillH * 0.68f, ctx.u * 2.6f, p.fg,
        Paint.Align.LEFT, ctx.type.label, true, spacing = 0.04f)
    gText("ROAD", absX + pillW - pillPad * 1.6f, pillY + pillH * 0.68f, ctx.u * 2.0f, p.muted,
        Paint.Align.RIGHT, ctx.type.label, true, spacing = 0.08f)
    gText("MTC", mtcX + pillPad * 1.6f, pillY + pillH * 0.68f, ctx.u * 2.6f, p.fg,
        Paint.Align.LEFT, ctx.type.label, true, spacing = 0.04f)
    gText("OFF", mtcX + pillW - pillPad * 1.6f, pillY + pillH * 0.68f, ctx.u * 2.0f, p.muted,
        Paint.Align.RIGHT, ctx.type.label, true, spacing = 0.08f)

    // Hız — C kavisinin içinde, birim rakamın sağında
    drawSpeed(
        ctx.speedValue, ctx.speedUnit,
        ctx.w * 0.585f, ctx.h * 0.690f, ctx.u * 29.0f,
        p.fg, p.muted, ctx.type.digits
    )

    // Menzil — gerçek KTM'de sol alt blok (MPH modunda birim dönüştürülür)
    val rangeValue = DistanceFormatter.value(t.fuelLevelRatio * 380.0, ctx.useMph).roundToInt()
    gText("RANGE", ctx.w * 0.045f, ctx.h * 0.655f, ctx.u * 1.50f, p.muted,
        Paint.Align.LEFT, ctx.type.label, true, spacing = 0.16f)
    drawSpeed(
        rangeValue, DistanceFormatter.unit(ctx.useMph),
        ctx.w * 0.045f, ctx.h * 0.730f, ctx.u * 4.1f,
        p.fg, p.muted, ctx.type.digits, Paint.Align.LEFT
    )
    gText("FUEL ${(t.fuelLevelRatio * 100).roundToInt()}%", ctx.w * 0.045f, ctx.h * 0.788f,
        ctx.u * 1.45f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.10f)

    // Sürüş modu + KTM OEM entegre ODO / TRIP / SEANS bloğu (sağ orta-alt)
    gText("RIDE MODE", ctx.w * 0.905f, ctx.h * 0.420f, ctx.u * 1.4f, p.muted,
        Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)
    drawSubModeChip(ctx, ctx.w * 0.905f, ctx.h * 0.475f, alignCenter = true, small = false)

    gText("ODO", ctx.w * 0.955f, ctx.h * 0.625f, ctx.u * 1.35f, p.muted,
        Paint.Align.RIGHT, ctx.type.label, true, spacing = 0.16f)
    gText(ctx.formatOdometer(), ctx.w * 0.955f, ctx.h * 0.672f, ctx.u * 2.55f, p.fg,
        Paint.Align.RIGHT, ctx.type.digits, true)
    gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.955f, ctx.h * 0.735f, ctx.u * 1.65f, p.accent,
        Paint.Align.RIGHT, ctx.type.label, true)
    gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.955f, ctx.h * 0.785f, ctx.u * 1.55f, p.muted,
        Paint.Align.RIGHT, ctx.type.label, true)
}

/* --- Ducati / BMW track: tam genişlik yatay devir şeridi ------------------------ */
private fun DrawScope.drawHorizontalEvo(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val barColor = shiftBarColor(ctx)

    drawBrandMark(ctx.spec, p, ctx.w * 0.065f, ctx.h * 0.058f, ctx.u * 4.0f, ctx.u, p.accent)
    gText(
        ctx.spec.modelName.substringBefore(" (").uppercase(),
        ctx.w * 0.105f, ctx.h * 0.068f, ctx.u * 1.85f, p.muted,
        Paint.Align.LEFT, ctx.type.label, true, spacing = 0.08f
    )
    gText(clusterClockText(ctx.clock24h), ctx.w * 0.955f, ctx.h * 0.072f, ctx.u * 3.0f, p.fg,
        Paint.Align.RIGHT, ctx.type.digits, true)

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.h * 0.112f, ctx.w * 0.28f, ctx.u)

    // Yatay devir şeridi
    val left = ctx.w * 0.045f
    val width = ctx.w * 0.910f
    val barTop = ctx.h * 0.152f
    val barH = ctx.u * 7.2f
    val radius = barH * 0.22f
    drawRoundRect(p.fg.copy(alpha = 0.065f), Offset(left - ctx.u * 0.35f, barTop - ctx.u * 0.35f),
        Size(width + ctx.u * 0.7f, barH + ctx.u * 0.7f),
        androidx.compose.ui.geometry.CornerRadius(radius + ctx.u * 0.35f, radius + ctx.u * 0.35f))
    drawRoundRect(p.fg.copy(alpha = 0.10f), Offset(left, barTop), Size(width, barH),
        androidx.compose.ui.geometry.CornerRadius(radius, radius))
    val redStart = left + width * ctx.redlineRatio
    drawRoundRect(RL_RED.copy(alpha = 0.20f), Offset(redStart, barTop), Size(left + width - redStart, barH),
        androidx.compose.ui.geometry.CornerRadius(radius, radius))
    val fillW = width * ctx.rpmRatio.coerceIn(0f, 1f)
    if (fillW > 1f) {
        drawRoundRect(barColor, Offset(left, barTop), Size(fillW, barH),
            androidx.compose.ui.geometry.CornerRadius(radius, radius))
    }
    // Segment ayırıcıları
    for (i in 1 until 30) {
        val x = left + width * i / 30f
        drawLine(p.bg.copy(alpha = 0.42f), Offset(x, barTop), Offset(x, barTop + barH), ctx.u * 0.26f)
    }
    // Anlık devir imleci
    val cursorX = left + fillW
    if (fillW > ctx.u * 0.4f && fillW < width - ctx.u * 0.4f) {
        drawLine(Color.White.copy(alpha = 0.90f), Offset(cursorX, barTop - ctx.u * 1.1f),
            Offset(cursorX, barTop + barH + ctx.u * 1.1f), ctx.u * 0.55f)
    }
    if (ctx.peakRatio > 0f) {
        val peakX = left + width * ctx.peakRatio
        drawLine(Color.White.copy(alpha = 0.55f), Offset(peakX, barTop - ctx.u * 0.8f),
            Offset(peakX, barTop + barH + ctx.u * 0.8f), ctx.u * 0.35f)
    }

    // Devir rakamları — şeridin altında, her 2 bin devirde
    var rpm = 0
    while (rpm <= ctx.maxRpm.roundToInt() + 1) {
        val frac = (rpm / ctx.maxRpm).toFloat().coerceIn(0f, 1f)
        val x = left + width * frac
        val inRed = frac >= ctx.redlineRatio
        gText(
            (rpm / 1000).toString(), x, barTop + barH + ctx.u * 3.6f,
            ctx.u * 2.05f, if (inRed) RL_RED.copy(alpha = 0.82f) else p.muted,
            Paint.Align.CENTER, ctx.type.label, true
        )
        rpm += if (ctx.maxRpm >= 15000.0) 2000 else 2000
    }
    gText("x1000 r/min", left + width, barTop + barH + ctx.u * 6.6f, ctx.u * 1.45f, p.muted,
        Paint.Align.RIGHT, ctx.type.label, true, spacing = 0.10f)

    // Sol kolon — Ducati / BMW Track Info Mode entegre ODO / TRIP / SEANS ve sektör bloğu
    val lapX = ctx.w * 0.050f
    gText("ODOMETER", lapX, ctx.h * 0.375f, ctx.u * 1.40f, p.muted,
        Paint.Align.LEFT, ctx.type.label, true, spacing = 0.18f)
    gText(ctx.formatOdometer(), lapX, ctx.h * 0.448f, ctx.u * 4.40f, p.fg,
        Paint.Align.LEFT, ctx.type.digits, true)
    gText("TRIP A  ${ctx.formatTrip()}", lapX, ctx.h * 0.525f, ctx.u * 2.15f, p.accent,
        Paint.Align.LEFT, ctx.type.digits, true)
    gText("SEANS  ${ctx.formatSession()}", lapX, ctx.h * 0.585f, ctx.u * 1.85f, p.fg.copy(alpha = 0.72f),
        Paint.Align.LEFT, ctx.type.digits, true)
    // Üç sektör ikonu: beyaz / kırmızı / turuncu (MotoGP renk mantığı).
    val splitColors = listOf(p.fg.copy(alpha = 0.45f), RL_RED.copy(alpha = 0.45f), p.stage1.copy(alpha = 0.45f))
    splitColors.forEachIndexed { i, c ->
        val bx = lapX + ctx.u * (0.2f + i * 4.3f)
        drawRoundRect(c.copy(alpha = 0.20f), Offset(bx, ctx.h * 0.642f),
            Size(ctx.u * 3.2f, ctx.u * 3.0f),
            androidx.compose.ui.geometry.CornerRadius(ctx.u * 0.40f, ctx.u * 0.40f))
        gTextCenter("S${i + 1}", bx + ctx.u * 1.6f, ctx.h * 0.642f + ctx.u * 2.2f,
            ctx.u * 1.55f, c.copy(alpha = 0.9f), ctx.type.label, true)
    }

    // Hız — merkez
    drawSpeed(
        ctx.speedValue, ctx.speedUnit,
        ctx.w * 0.470f, ctx.h * 0.668f, ctx.u * 31.0f,
        p.fg, p.muted, ctx.type.digits
    )
    val rangeText = "${DistanceFormatter.value(t.fuelLevelRatio * 380.0, ctx.useMph).roundToInt()} ${DistanceFormatter.unit(ctx.useMph)}"
    gText("RANGE $rangeText", ctx.w * 0.470f, ctx.h * 0.748f, ctx.u * 2.0f, p.accent,
        Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)

    // Vites — gerçek Panigale track ekranında kutusuz dev rakam.
    val gearText = if (ctx.isNeutral) "N" else t.gearDisplayString
    val gearColor = when {
        ctx.isNeutral -> RL_GREEN
        t.shiftLightState != com.oemcluster.pro.domain.ShiftLightState.NORMAL && t.shiftFlashPhaseOn -> RL_RED
        else -> p.fg
    }
    gText(gearText, ctx.w * 0.800f, ctx.h * 0.540f, ctx.u * 21.0f, gearColor,
        Paint.Align.CENTER, ctx.type.label, true)
    gText("GEAR", ctx.w * 0.800f, ctx.h * 0.606f, ctx.u * 1.50f, p.muted,
        Paint.Align.CENTER, ctx.type.label, true, spacing = 0.22f)

    // Sağ kolon — sürüş elektroniği seviyeleri (Ducati track görünümü).
    gText("DTC 3   DWC 2   ABS 2", ctx.w * 0.800f, ctx.h * 0.678f, ctx.u * 1.70f, p.fg.copy(alpha = 0.75f),
        Paint.Align.CENTER, ctx.type.label, true, spacing = 0.06f)
    gText("RIDE MODE", ctx.w * 0.800f, ctx.h * 0.748f, ctx.u * 1.35f, p.muted,
        Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)
    gText(ctx.subMode.uppercase(), ctx.w * 0.800f, ctx.h * 0.800f, ctx.u * 2.15f, p.accent,
        Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)
}

/* --- Dikey / kompakt ekran ------------------------------------------------------- */
private fun DrawScope.drawCompactEvo(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val barColor = shiftBarColor(ctx)

    gText(clusterClockText(ctx.clock24h), ctx.w * 0.06f, ctx.h * 0.075f, ctx.u * 4.2f, p.fg,
        Paint.Align.LEFT, ctx.type.digits, true)
    drawBrandMark(ctx.spec, p, ctx.w * 0.80f, ctx.h * 0.062f, ctx.u * 4.4f, ctx.u, p.accent)

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.h * 0.105f, ctx.w * 0.40f, ctx.u)

    val left = ctx.w * 0.08f
    val width = ctx.w * 0.84f
    val barTop = ctx.h * 0.145f
    val barH = ctx.u * 4.6f
    drawRoundRect(p.fg.copy(alpha = 0.10f), Offset(left, barTop), Size(width, barH),
        androidx.compose.ui.geometry.CornerRadius(barH * 0.3f, barH * 0.3f))
    val redStart = left + width * ctx.redlineRatio
    drawRoundRect(RL_RED.copy(alpha = 0.20f), Offset(redStart, barTop), Size(left + width - redStart, barH),
        androidx.compose.ui.geometry.CornerRadius(barH * 0.3f, barH * 0.3f))
    val fillW = width * ctx.rpmRatio.coerceIn(0f, 1f)
    if (fillW > 1f) {
        drawRoundRect(barColor, Offset(left, barTop), Size(fillW, barH),
            androidx.compose.ui.geometry.CornerRadius(barH * 0.3f, barH * 0.3f))
    }
    var rpm = 0
    while (rpm <= ctx.maxRpm.roundToInt() + 1) {
        val frac = (rpm / ctx.maxRpm).toFloat().coerceIn(0f, 1f)
        gText((rpm / 1000).toString(), left + width * frac, barTop + barH + ctx.u * 3.2f,
            ctx.u * 1.8f, if (frac >= ctx.redlineRatio) RL_RED else p.muted,
            Paint.Align.CENTER, ctx.type.label, true)
        rpm += 2000
    }
    gText("x1000 r/min", left + width, barTop + barH + ctx.u * 5.8f, ctx.u * 1.3f, p.muted,
        Paint.Align.RIGHT, ctx.type.label, true, spacing = 0.08f)

    drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.5f, ctx.h * 0.435f, ctx.u * 19.0f,
        p.fg, p.muted, ctx.type.digits)
    drawGearPod(ctx.w * 0.5f, ctx.h * 0.575f, ctx.u * 10.5f, p, t, ctx.isNeutral, ctx.u,
        ctx.type.label, GearPodShape.SQUARE)
    gText("GEAR", ctx.w * 0.5f, ctx.h * 0.675f, ctx.u * 1.5f, p.muted,
        Paint.Align.CENTER, ctx.type.label, true, spacing = 0.20f)

    val rangeText = "${DistanceFormatter.value(t.fuelLevelRatio * 380.0, ctx.useMph).roundToInt()} ${DistanceFormatter.unit(ctx.useMph)}"
    gText("RANGE $rangeText   •   ${ctx.subMode.uppercase()}", ctx.w * 0.5f, ctx.h * 0.725f,
        ctx.u * 1.75f, p.accent, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.08f)
    gText("FUEL ${(t.fuelLevelRatio * 100).roundToInt()}%   •   ${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}",
        ctx.w * 0.5f, ctx.h * 0.765f, ctx.u * 1.55f, p.muted,
        Paint.Align.CENTER, ctx.type.label, true)
    gText("ODO ${ctx.formatOdometer()}  •  TRIP ${ctx.formatTrip()}  •  SEANS ${ctx.formatSession()}",
        ctx.w * 0.5f, ctx.h * 0.810f, ctx.u * 1.48f, p.fg.copy(alpha = 0.88f),
        Paint.Align.CENTER, ctx.type.digits, true)
}

private fun shiftBarColor(ctx: GaugeContext, idleColor: Color = ctx.palette.accent): Color {
    val t = ctx.telemetry
    return when (t.shiftLightState) {
        com.oemcluster.pro.domain.ShiftLightState.NORMAL -> idleColor
        com.oemcluster.pro.domain.ShiftLightState.STAGE_1_APPROACHING -> ctx.palette.stage1
        else -> if (t.shiftFlashPhaseOn) RL_RED else ctx.palette.stage1
    }
}

/** KTM "bonded" devir şeridi: dikey başlar, 90° döner ve üstte yatay biter. */
private class BondedTachGeometry(
    private val x0: Float,
    private val yTop: Float,
    private val yBottom: Float,
    private val x1: Float,
    private val corner: Float
) {
    private val verticalLen = (yBottom - (yTop + corner)).coerceAtLeast(0f)
    private val arcLen = (PI.toFloat() * 0.5f) * corner
    private val horizontalLen = (x1 - (x0 + corner)).coerceAtLeast(0f)
    val totalLength: Float = verticalLen + arcLen + horizontalLen

    fun path(): Path = Path().apply {
        moveTo(x0, yBottom)
        lineTo(x0, yTop + corner)
        arcTo(
            androidx.compose.ui.geometry.Rect(x0, yTop, x0 + corner * 2f, yTop + corner * 2f),
            180f, 90f, false
        )
        lineTo(x1, yTop)
    }

    /** Nokta ve dışa bakan normal. */
    fun pointAt(ratio: Float): Pair<Offset, Offset> {
        val d = ratio.coerceIn(0f, 1f) * totalLength
        return when {
            d <= verticalLen -> Offset(x0, yBottom - d) to Offset(-1f, 0f)
            d <= verticalLen + arcLen -> {
                val a = (d - verticalLen) / arcLen
                val theta = (PI * (1.0 + 0.5 * a)).toFloat()
                val cx = x0 + corner
                val cy = yTop + corner
                Offset(cx + corner * cos(theta), cy + corner * sin(theta)) to
                    Offset(cos(theta), sin(theta))
            }
            else -> {
                val s = d - verticalLen - arcLen
                Offset(x0 + corner + s, yTop) to Offset(0f, -1f)
            }
        }
    }
}

/** Path'in [startRatio, endRatio] aralığını kalın bir şerit olarak boyar. */
private fun DrawScope.strokePathSegment(
    path: Path,
    startRatio: Float,
    endRatio: Float,
    width: Float,
    color: Color
) {
    if (endRatio - startRatio <= 0.0005f) return
    val measure = androidx.compose.ui.graphics.PathMeasure()
    measure.setPath(path, false)
    val len = measure.length
    if (len <= 0f) return
    val dst = Path()
    measure.getSegment(
        len * startRatio.coerceIn(0f, 1f),
        len * endRatio.coerceIn(0f, 1f),
        dst,
        true
    )
    drawPath(dst, color, style = Stroke(width = width, cap = StrokeCap.Butt))
}

/* =====================================================================================
 *  5) SYMMETRIC PINCER BAR — Street Triple 765 RS, 1290 Super Adventure
 * ===================================================================================== */
internal fun DrawScope.drawSymmetricPincer(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val variant = skinVariant(ctx.spec.skinId, 2)
    val railH = ctx.h * 0.072f
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.3f, ctx.w * 0.30f, ctx.u)

    if (ctx.wide) {
        val topY = ctx.h * 0.18f
        val barH = ctx.u * 4.0f
        val leftStart = ctx.w * 0.06f
        val rightEnd = ctx.w * 0.94f
        val half = ctx.w * 0.40f
        val gap = ctx.w * 0.04f
        // sol bar dıştan içe
        drawPincerBar(leftStart, topY, half - gap, barH, ctx.rpmRatio, true, p, ctx.redlineRatio, ctx.peakRatio, ctx.u)
        drawPincerBar(rightEnd, topY, half - gap, barH, ctx.rpmRatio, false, p, ctx.redlineRatio, ctx.peakRatio, ctx.u)
        // orta elmas vites
        drawGearPod(ctx.w * 0.5f, topY + barH * 0.5f, ctx.u * 9.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.HEX)
        // devir uçları
        gText("0", leftStart, topY + barH + ctx.u * 3.6f, ctx.u * 1.8f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText((ctx.maxRpm / 1000).roundToInt().toString(), leftStart + half - gap, topY + barH + ctx.u * 3.6f, ctx.u * 1.8f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)
        gText("0", rightEnd, topY + barH + ctx.u * 3.6f, ctx.u * 1.8f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)
        gText((ctx.maxRpm / 1000).roundToInt().toString(), rightEnd - half + gap, topY + barH + ctx.u * 3.6f, ctx.u * 1.8f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("x1000 RPM", ctx.w * 0.5f, topY + barH + ctx.u * 3.8f, ctx.u * 1.5f, p.accent, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.12f)

        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.62f, ctx.u * 21.0f, p.fg, p.muted, ctx.type.digits)
        if (variant == 0) {
            drawGearPod(ctx.w * 0.50f, ctx.h * 0.73f, ctx.u * 10.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.ROUND)
            gText("GEAR", ctx.w * 0.50f, ctx.h * 0.79f, ctx.u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)
        }

        // yan paneller
        drawPanel(ctx.w * 0.035f, ctx.h * 0.40f, ctx.w * 0.19f, ctx.h * 0.245f, p, accentEdge = true)
        gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.052f, ctx.h * 0.452f, ctx.u * 1.75f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.052f, ctx.h * 0.512f, ctx.u * 2.4f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.052f, ctx.h * 0.562f, ctx.u * 1.50f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText("MODE ${ctx.subMode.uppercase()}", ctx.w * 0.052f, ctx.h * 0.612f, ctx.u * 1.35f, p.fg.copy(alpha = 0.78f), Paint.Align.LEFT, ctx.type.label, true)

        drawPanel(ctx.w * 0.775f, ctx.h * 0.42f, ctx.w * 0.19f, ctx.h * 0.22f, p)
        gText("FUEL", ctx.w * 0.795f, ctx.h * 0.465f, ctx.u * 1.6f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.14f)
        drawLinearBar(ctx.w * 0.795f, ctx.h * 0.50f, ctx.w * 0.145f, ctx.u * 1.3f, t.fuelLevelRatio, p.accent, p.track, 0.9f, RL_RED, ctx.u * 0.6f)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}  •  ${t.gpsSatelliteCount} SAT", ctx.w * 0.795f, ctx.h * 0.58f, ctx.u * 1.6f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
        gText(if (t.tractionControlInterventionActive) "TC ACTIVE" else "TC READY", ctx.w * 0.795f, ctx.h * 0.62f, ctx.u * 1.35f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        drawBrandMark(ctx.spec, p, ctx.w * 0.50f, ctx.h * 0.10f, ctx.u * 4.8f, ctx.u, p.accent.copy(alpha = 0.7f))
    } else {
        // Dikey kıskaç: iki yan bar hızın etrafında
        val cx = ctx.w * 0.50f
        val cy = ctx.h * 0.46f
        val barH = ctx.h * 0.30f
        val barW = ctx.u * 2.8f
        val leftX = ctx.w * 0.10f
        val rightX = ctx.w * 0.90f - barW
        drawLinearBar(leftX, cy - barH * 0.5f, barW, barH, ctx.rpmRatio, p.accent, p.track, ctx.redlineRatio, RL_RED, barW * 0.5f, vertical = true)
        drawLinearBar(rightX, cy - barH * 0.5f, barW, barH, ctx.rpmRatio, p.accent, p.track, ctx.redlineRatio, RL_RED, barW * 0.5f, vertical = true)
        gText("0", leftX + barW * 0.5f, cy + barH * 0.5f + ctx.u * 2.8f, ctx.u * 1.6f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        gText("MAX", leftX + barW * 0.5f, cy - barH * 0.5f - ctx.u * 1.2f, ctx.u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawSpeed(ctx.speedValue, ctx.speedUnit, cx, cy + ctx.u * 4.0f, ctx.u * 17.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(cx, cy - ctx.u * 9.0f, ctx.u * 8.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.HEX)
        drawTemperatureAndFuel(ctx, ctx.w * 0.14f, ctx.h * 0.80f, ctx.w * 0.40f, compact = true)
        drawSubModeChip(ctx, ctx.w * 0.86f, ctx.h * 0.82f, alignRight = true, small = true)
    }
    drawStatusRail(ctx, RailStyle.TFT)
}

private fun DrawScope.drawPincerBar(
    outerX: Float,
    y: Float,
    length: Float,
    height: Float,
    ratio: Float,
    leftSide: Boolean,
    p: GaugePalette,
    redlineRatio: Float,
    peakRatio: Float,
    u: Float
) {
    val dir = if (leftSide) 1f else -1f
    drawRoundRect(p.track, Offset(min(outerX, outerX + dir * length), y), Size(length, height), androidx.compose.ui.geometry.CornerRadius(height * 0.4f, height * 0.4f))
    val fill = length * ratio.coerceIn(0f, 1f)
    val fillLeft = if (leftSide) outerX else outerX - fill
    drawRoundRect(p.accent, Offset(fillLeft, y), Size(fill, height), androidx.compose.ui.geometry.CornerRadius(height * 0.4f, height * 0.4f))
    // redline kısmı iç uçta
    val redLen = length * (1f - redlineRatio)
    val redLeft = if (leftSide) outerX + length - redLen else outerX - length
    drawRoundRect(RL_RED.copy(alpha = 0.35f), Offset(redLeft, y), Size(redLen, height), androidx.compose.ui.geometry.CornerRadius(height * 0.4f, height * 0.4f))
    if (peakRatio > 0f) {
        val px = if (leftSide) outerX + length * peakRatio else outerX - length * peakRatio
        drawLine(Color.White, Offset(px, y - u * 1.1f), Offset(px, y + height + u * 1.1f), u * 0.6f)
    }
}

/* =====================================================================================
 * 10) F1 DIGITAL ARC BAR — Duke Gen1, 690 SMC, R3 Gen2, GSX-R L7, MT-09/07 LCD
 * ===================================================================================== */
internal fun DrawScope.drawF1DigitalArc(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val railH = ctx.h * 0.072f
    val straight = ctx.spec.skinId.contains("gsxr1000r_l7") || ctx.spec.skinId.contains("katana")
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.0f, ctx.w * 0.24f, ctx.u)

    // Segment ark / düz bar
    val segments = if (straight) 36 else 24
    if (straight) {
        val left = ctx.w * 0.06f
        val right = ctx.w * 0.94f
        val top = ctx.h * 0.13f
        val h = ctx.u * 5.2f
        val segW = (right - left) / segments
        val filled = (ctx.rpmRatio * segments).roundToInt()
        for (i in 0 until segments) {
            val inRed = i.toFloat() / segments >= ctx.redlineRatio
            val c = when {
                i < filled && inRed -> RL_RED
                i < filled -> p.accent
                inRed -> RL_RED.copy(alpha = 0.20f)
                else -> p.track
            }
            drawRoundRect(c, Offset(left + i * segW, top), Size(segW * 0.78f, h), androidx.compose.ui.geometry.CornerRadius(ctx.u * 0.25f, ctx.u * 0.25f))
        }
        var rpmLabel = 0
        while (rpmLabel <= ctx.maxRpm.roundToInt()) {
            val frac = rpmLabel / ctx.maxRpm.toFloat()
            val x = left + (right - left) * frac
            gText(
                (rpmLabel / 1000).toString(), x, top + h + ctx.u * 3.4f, ctx.u * 1.8f,
                if (frac >= ctx.redlineRatio) RL_RED else p.muted,
                Paint.Align.CENTER, ctx.type.label, true
            )
            rpmLabel += 2000
        }
    } else {
        // Gerçek racing LCD: ekranın üstünde tam genişlik, hafif kavisli segment bar.
        // Yay Yarıçapı sabit bir yükseklikten türetilir; böylece geniş ekranlarda
        // segmentler yuvarlak makara gibi değil, farklı bloklar olarak görünür.
        val cx = ctx.w * 0.5f
        val arcW = ctx.w * 0.86f
        val sag = ctx.h * 0.205f
        val radius = (arcW * arcW / 4f + sag * sag) / (2f * sag)
        val half = kotlin.math.asin((arcW / 2f) / radius)
        val sweep = (2f * half * 180f / PI.toFloat())
        val cy = ctx.h * 0.145f + radius
        drawSegmentedArc(
            cx, cy, radius, 270f - sweep * 0.5f, sweep, segments,
            ctx.rpmRatio, ctx.u * 3.15f, p.accent, p.track,
            ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 0.32f,
            cap = StrokeCap.Butt
        )
        var rpmLabel = 0
        while (rpmLabel <= ctx.maxRpm.roundToInt()) {
            val frac = rpmLabel / ctx.maxRpm.toFloat()
            val a = 270f - sweep * 0.5f + sweep * frac
            val pt = polar(cx, cy, radius - ctx.u * 5.4f, a)
            gTextCenter(
                (rpmLabel / 1000).toString(),
                pt.x, pt.y, ctx.u * 1.9f,
                if (frac >= ctx.redlineRatio) RL_RED else p.muted,
                ctx.type.label
            )
            rpmLabel += 2000
        }
    }

    if (ctx.wide) {
        // Marka / model — ince üst satır
        gText(
            "${ctx.spec.brand.uppercase()}  //  ${ctx.spec.modelName.substringBefore(" (").uppercase()}",
            ctx.w * 0.5f, ctx.h * 0.310f, ctx.u * 1.45f, p.muted,
            Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f
        )

        // Hız — merkez
        drawSpeed(
            ctx.speedValue, ctx.speedUnit,
            ctx.w * 0.415f, ctx.h * 0.665f, ctx.u * 28.0f,
            p.fg, p.muted, ctx.type.digits
        )
        drawSubModeChip(ctx, ctx.w * 0.415f, ctx.h * 0.735f, small = true, alignCenter = true)

        // Vites — sağda, kutusuz dev rakam (gerçek GSX-R / ZX-10R düzeni)
        val gearText = if (ctx.isNeutral) "N" else t.gearDisplayString
        val gearColor = when {
            ctx.isNeutral -> RL_GREEN
            t.shiftLightState != com.oemcluster.pro.domain.ShiftLightState.NORMAL && t.shiftFlashPhaseOn -> RL_RED
            else -> p.fg
        }
        gText(gearText, ctx.w * 0.805f, ctx.h * 0.590f, ctx.u * 15.5f, gearColor,
            Paint.Align.CENTER, ctx.type.label, true)
        gText("GEAR", ctx.w * 0.805f, ctx.h * 0.655f, ctx.u * 1.45f, p.muted,
            Paint.Align.CENTER, ctx.type.label, true, spacing = 0.22f)
        gText(ctx.mappedRpm.roundToInt().toString(), ctx.w * 0.805f, ctx.h * 0.738f, ctx.u * 3.0f, p.accent,
            Paint.Align.CENTER, ctx.type.digits, true)
        gText("r/min", ctx.w * 0.805f, ctx.h * 0.772f, ctx.u * 1.3f, p.muted,
            Paint.Align.CENTER, ctx.type.label, true)

        // Yakıt / hararet — sol alt, panel yok
        drawTemperatureAndFuel(ctx, ctx.w * 0.060f, ctx.h * 0.812f, ctx.w * 0.215f, compact = true)

        // Trip / ODO / SEANS — sağ alt OEM dijital LCD bloğu
        gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.945f, ctx.h * 0.802f, ctx.u * 1.85f, p.fg,
            Paint.Align.RIGHT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.945f, ctx.h * 0.840f, ctx.u * 1.60f, p.accent,
            Paint.Align.RIGHT, ctx.type.label, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.945f, ctx.h * 0.875f, ctx.u * 1.45f, p.muted,
            Paint.Align.RIGHT, ctx.type.label, true)
    } else {
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.52f, ctx.u * 22.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.50f, ctx.h * 0.67f, ctx.u * 9.5f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
        gText("RPM ${ctx.mappedRpm.roundToInt()}", ctx.w * 0.50f, ctx.h * 0.76f, ctx.u * 1.8f, p.accent, Paint.Align.CENTER, ctx.type.label, true)
        drawTemperatureAndFuel(ctx, ctx.w * 0.12f, ctx.h * 0.81f, ctx.w * 0.42f, compact = true)
    }
    drawStatusRail(ctx, RailStyle.LCD)
}

/* =====================================================================================
 * 11) RALLY TOWER VERTICAL TFT — Ténéré 700, Tuareg 660, Africa Twin
 * ===================================================================================== */
internal fun DrawScope.drawRallyTower(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val railH = ctx.h * 0.072f
    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.0f, ctx.w * 0.24f, ctx.u)

    if (ctx.wide) {
        val towerW = min(ctx.w * 0.34f, ctx.u * 62f)
        val towerLeft = ctx.w * 0.5f - towerW * 0.5f
        val towerTop = ctx.h * 0.10f
        val towerBottom = ctx.h * 0.90f
        drawCompassStrip(ctx, towerTop, ctx.h * 0.065f)
        drawRect(p.fg.copy(alpha = 0.03f), Offset(towerLeft, towerTop + ctx.h * 0.065f), Size(towerW, towerBottom - towerTop - ctx.h * 0.065f))
        drawLine(p.accent.copy(alpha = 0.5f), Offset(towerLeft, towerTop + ctx.h * 0.065f), Offset(towerLeft, towerBottom), ctx.u * 0.25f)
        drawLine(p.accent.copy(alpha = 0.5f), Offset(towerLeft + towerW, towerTop + ctx.h * 0.065f), Offset(towerLeft + towerW, towerBottom), ctx.u * 0.25f)

        drawGearPod(ctx.w * 0.5f, towerTop + ctx.h * 0.16f, ctx.u * 10.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.5f, towerTop + ctx.h * 0.43f, ctx.u * 22.0f, p.fg, p.muted, ctx.type.digits)
        gText("RALLY MODE", ctx.w * 0.5f, towerTop + ctx.h * 0.48f, ctx.u * 1.5f, p.accent, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)

        // Dikey devir barları tower'ın iki yanında
        val barH = ctx.h * 0.40f
        val barW = ctx.u * 2.4f
        val barTop = towerTop + ctx.h * 0.10f
        drawLinearBar(towerLeft - ctx.u * 5.0f, barTop, barW, barH, ctx.rpmRatio, p.accent, p.track, ctx.redlineRatio, RL_RED, barW * 0.5f, vertical = true)
        drawLinearBar(towerLeft + towerW + ctx.u * 2.6f, barTop, barW, barH, ctx.rpmRatio, p.accent, p.track, ctx.redlineRatio, RL_RED, barW * 0.5f, vertical = true)
        for (i in 0..4) {
            val yy = barTop + barH * i / 4f
            gText(((ctx.maxRpm - ctx.maxRpm * i / 4) / 1000).roundToInt().toString(), towerLeft - ctx.u * 6.4f, yy + ctx.u * 0.6f, ctx.u * 1.4f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)
            gText(((ctx.maxRpm - ctx.maxRpm * i / 4) / 1000).roundToInt().toString(), towerLeft + towerW + ctx.u * 6.2f, yy + ctx.u * 0.6f, ctx.u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        }

        // Sol panel: yakıt / hararet / menzil
        val leftPanelX = ctx.w * 0.035f
        val panelW = ctx.w * 0.17f
        drawPanel(leftPanelX, ctx.h * 0.18f, panelW, ctx.h * 0.42f, p, accentEdge = true)
        gText("FUEL", leftPanelX + panelW * 0.12f, ctx.h * 0.23f, ctx.u * 1.45f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)
        gText("${(t.fuelLevelRatio * 100).roundToInt()}%", leftPanelX + panelW * 0.12f, ctx.h * 0.30f, ctx.u * 4.0f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        drawLinearBar(leftPanelX + panelW * 0.12f, ctx.h * 0.32f, panelW * 0.70f, ctx.u * 1.2f, t.fuelLevelRatio, p.accent, p.track, 0.9f, RL_RED, ctx.u * 0.6f)
        gText("RANGE", leftPanelX + panelW * 0.12f, ctx.h * 0.39f, ctx.u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)
        gText("${DistanceFormatter.value(t.fuelLevelRatio * 380.0, ctx.useMph).roundToInt()} ${DistanceFormatter.unit(ctx.useMph)}", leftPanelX + panelW * 0.12f, ctx.h * 0.45f, ctx.u * 3.1f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}", leftPanelX + panelW * 0.12f, ctx.h * 0.52f, ctx.u * 2.6f, p.accent, Paint.Align.LEFT, ctx.type.digits, true)
        gText("${t.gpsSatelliteCount} SAT  •  ${t.gpsHorizontalAccuracyM.roundToInt()} m", leftPanelX + panelW * 0.12f, ctx.h * 0.57f, ctx.u * 1.3f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

        // Sağ panel: trip / yol kitabı
        val rightPanelX = ctx.w - ctx.w * 0.035f - panelW
        drawPanel(rightPanelX, ctx.h * 0.18f, panelW, ctx.h * 0.42f, p, accentEdge = true)
        gText("TRIP A", rightPanelX + panelW * 0.12f, ctx.h * 0.23f, ctx.u * 1.45f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)
        gText(ctx.formatTrip(), rightPanelX + panelW * 0.12f, ctx.h * 0.30f, ctx.u * 3.6f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("SEANS ${ctx.formatSession()}", rightPanelX + panelW * 0.12f, ctx.h * 0.34f, ctx.u * 1.35f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
        gText("ODO", rightPanelX + panelW * 0.12f, ctx.h * 0.39f, ctx.u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.12f)
        gText(ctx.formatOdometer(), rightPanelX + panelW * 0.12f, ctx.h * 0.45f, ctx.u * 3.0f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("MODE ${ctx.subMode.uppercase()}", rightPanelX + panelW * 0.12f, ctx.h * 0.52f, ctx.u * 1.35f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", rightPanelX + panelW * 0.12f, ctx.h * 0.57f, ctx.u * 1.35f, p.fg, Paint.Align.LEFT, ctx.type.label, true)

        drawBrandMark(ctx.spec, p, ctx.w * 0.5f, ctx.h * 0.875f, ctx.u * 3.8f, ctx.u, p.accent.copy(alpha = 0.55f))
    } else {
        drawCompassStrip(ctx, ctx.h * 0.075f, ctx.h * 0.06f)
        drawGearPod(ctx.w * 0.50f, ctx.h * 0.20f, ctx.u * 9.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.37f, ctx.u * 20.0f, p.fg, p.muted, ctx.type.digits)
        val barH = ctx.h * 0.18f
        drawLinearBar(ctx.w * 0.12f, ctx.h * 0.44f, ctx.w * 0.72f, ctx.u * 2.0f, ctx.rpmRatio, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.u, vertical = false)
        for (i in 0..4) {
            val x = ctx.w * 0.12f + ctx.w * 0.72f * i / 4f
            gText(((ctx.maxRpm * i / 4) / 1000).roundToInt().toString(), x, ctx.h * 0.44f + ctx.u * 4.6f, ctx.u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        }
        drawTemperatureAndFuel(ctx, ctx.w * 0.12f, ctx.h * 0.54f, ctx.w * 0.36f, compact = false)
        drawPanel(ctx.w * 0.58f, ctx.h * 0.54f, ctx.w * 0.30f, ctx.h * 0.16f, p, accentEdge = true)
        gText("TRIP", ctx.w * 0.61f, ctx.h * 0.585f, ctx.u * 1.5f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        gText(ctx.formatTrip(), ctx.w * 0.61f, ctx.h * 0.635f, ctx.u * 3.6f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.61f, ctx.h * 0.675f, ctx.u * 1.4f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        drawCompassStrip(ctx, ctx.h * 0.75f, ctx.h * 0.05f)
        drawSubModeChip(ctx, ctx.w * 0.12f, ctx.h * 0.86f, small = true)
        drawBrandMark(ctx.spec, p, ctx.w * 0.86f, ctx.h * 0.85f, ctx.u * 5.0f, ctx.u, p.accent.copy(alpha = 0.55f))
    }
    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 * 12) PANORAMIC SPLIT TFT — R1300GS, Multistrada V4, Tiger 1200, CFMoto 800NK, Tourer
 * ===================================================================================== */
internal fun DrawScope.drawPanoramicSplit(ctx: GaugeContext) {
    val p = ctx.palette
    val t = ctx.telemetry
    val railH = ctx.h * 0.072f
    val adventure = ctx.spec.skinId.contains("r1300gs") || ctx.spec.skinId.contains("multistrada") ||
        ctx.spec.skinId.contains("tiger") || ctx.spec.skinId.contains("africa") ||
        ctx.spec.skinId.contains("norden") || ctx.spec.skinId.contains("800nk") ||
        ctx.spec.skinId.contains("transalp") || ctx.spec.skinId.contains("vstrom")

    drawShiftLeds(ctx.spec, t, p, ctx.w * 0.5f, ctx.u * 4.0f, ctx.w * 0.24f, ctx.u)

    if (ctx.wide) {
        // Üst durum şeridi
        drawRect(p.fg.copy(alpha = 0.04f), Offset(0f, ctx.h * 0.095f), Size(ctx.w, ctx.h * 0.055f))
        gText("MODE  ${ctx.subMode.uppercase()}", ctx.w * 0.035f, ctx.h * 0.132f, ctx.u * 1.75f, p.accent, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.10f)
        gText(if (adventure) "OFF-ROAD READY" else "TOURING", ctx.w * 0.50f, ctx.h * 0.132f, ctx.u * 1.6f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.14f)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}   ${(t.fuelLevelRatio * 100).roundToInt()}%   ${t.gpsSatelliteCount} SAT", ctx.w * 0.965f, ctx.h * 0.132f, ctx.u * 1.55f, p.fg, Paint.Align.RIGHT, ctx.type.label, true)

        val top = ctx.h * 0.19f
        val bottom = ctx.h * 0.88f
        val panelH = bottom - top
        val leftW = ctx.w * 0.24f
        val rightW = ctx.w * 0.23f
        val centerW = ctx.w - leftW - rightW - ctx.w * 0.06f

        // Sol kart: harita / yol bilgisayarı
        drawPanel(ctx.w * 0.025f, top, leftW, panelH, p)
        gText(if (adventure) "NAVIGATION" else "TRIP", ctx.w * 0.045f, top + ctx.u * 4.0f, ctx.u * 1.6f, p.muted, Paint.Align.LEFT, ctx.type.label, true, spacing = 0.14f)
        // minik harita ızgarası
        val mapTop = top + ctx.u * 6.0f
        val mapH = panelH * 0.44f
        drawRoundRect(p.fg.copy(alpha = 0.035f), Offset(ctx.w * 0.04f, mapTop), Size(leftW * 0.82f, mapH), androidx.compose.ui.geometry.CornerRadius(ctx.u * 1.2f, ctx.u * 1.2f))
        for (i in 0..3) {
            drawLine(p.fg.copy(alpha = 0.10f), Offset(ctx.w * 0.04f, mapTop + mapH * i / 3f), Offset(ctx.w * 0.04f + leftW * 0.82f, mapTop + mapH * i / 3f), ctx.u * 0.12f)
            drawLine(p.fg.copy(alpha = 0.10f), Offset(ctx.w * 0.04f + leftW * 0.82f * i / 3f, mapTop), Offset(ctx.w * 0.04f + leftW * 0.82f * i / 3f, mapTop + mapH), ctx.u * 0.12f)
        }
        val route = Path().apply {
            moveTo(ctx.w * 0.065f, mapTop + mapH * 0.86f)
            cubicTo(ctx.w * 0.09f, mapTop + mapH * 0.55f, ctx.w * 0.14f, mapTop + mapH * 0.72f, ctx.w * 0.17f, mapTop + mapH * 0.35f)
            cubicTo(ctx.w * 0.19f, mapTop + mapH * 0.12f, ctx.w * 0.21f, mapTop + mapH * 0.28f, ctx.w * 0.235f, mapTop + mapH * 0.10f)
        }
        drawPath(route, p.accent, style = Stroke(width = ctx.u * 0.9f, cap = StrokeCap.Round))
        drawCircle(p.accent, ctx.u * 1.0f, Offset(ctx.w * 0.235f, mapTop + mapH * 0.10f))
        gText(if (adventure) "TET SECTOR 4" else "ALPS ROUTE", ctx.w * 0.045f, mapTop + mapH + ctx.u * 3.0f, ctx.u * 1.6f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
        gText("ODO  ${ctx.formatOdometer()}", ctx.w * 0.045f, mapTop + mapH + ctx.u * 6.0f, ctx.u * 1.85f, p.fg, Paint.Align.LEFT, ctx.type.digits, true)
        gText("TRIP A  ${ctx.formatTrip()}", ctx.w * 0.045f, mapTop + mapH + ctx.u * 8.8f, ctx.u * 1.65f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
        gText("SEANS  ${ctx.formatSession()}", ctx.w * 0.045f, mapTop + mapH + ctx.u * 11.4f, ctx.u * 1.50f, p.muted, Paint.Align.LEFT, ctx.type.label, true)

        // Orta: hız + vites
        val centerX = ctx.w * 0.5f
        drawSpeed(ctx.speedValue, ctx.speedUnit, centerX, ctx.h * 0.57f, ctx.u * 23.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(centerX, ctx.h * 0.72f, ctx.u * 10.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
        gText("GEAR", centerX, ctx.h * 0.785f, ctx.u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.18f)
        drawBrandMark(ctx.spec, p, centerX, ctx.h * 0.24f, ctx.u * 4.6f, ctx.u, p.accent.copy(alpha = 0.55f))
        gText(ctx.spec.modelName.uppercase(), centerX, ctx.h * 0.295f, ctx.u * 1.3f, p.muted, Paint.Align.CENTER, ctx.type.label, false)

        // Sağ kart: devir yayı + durum
        drawPanel(ctx.w - rightW - ctx.w * 0.025f, top, rightW, panelH, p)
        val ringCx = ctx.w - rightW * 0.5f - ctx.w * 0.025f
        val ringCy = top + panelH * 0.34f
        val ringR = min(rightW * 0.34f, panelH * 0.24f)
        drawSegmentedArc(ringCx, ringCy, ringR, 135f, 270f, 30, ctx.rpmRatio, ctx.u * 2.8f, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.peakRatio, Color.White, 1.0f)
        gText((ctx.mappedRpm / 1000).roundToInt().toString(), ringCx, ringCy + ctx.u * 1.8f, ctx.u * 4.2f, p.fg, Paint.Align.CENTER, ctx.type.digits, true)
        gText("x1000 RPM", ringCx, ringCy + ctx.u * 4.6f, ctx.u * 1.3f, p.muted, Paint.Align.CENTER, ctx.type.label, true, spacing = 0.10f)

        val statY = top + panelH * 0.72f
        gText("TC ${if (t.tractionControlInterventionActive) "ACT" else "STBY"}", ctx.w - rightW * 0.82f, statY, ctx.u * 1.5f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
        gText("ABS ${if (t.absInterventionActive) "ACT" else "STBY"}", ctx.w - rightW * 0.82f, statY + ctx.u * 3.2f, ctx.u * 1.5f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
        gText("LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w - rightW * 0.82f, statY + ctx.u * 6.4f, ctx.u * 1.5f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
        gText("SUS ${if (adventure) "AUTO" else "ROAD"}", ctx.w - rightW * 0.82f, statY + ctx.u * 9.6f, ctx.u * 1.5f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
    } else {
        drawRect(p.fg.copy(alpha = 0.04f), Offset(0f, ctx.h * 0.075f), Size(ctx.w, ctx.h * 0.05f))
        gText("MODE  ${ctx.subMode.uppercase()}", ctx.w * 0.06f, ctx.h * 0.108f, ctx.u * 1.6f, p.accent, Paint.Align.LEFT, ctx.type.label, true)
        gText("${(t.fuelLevelRatio * 100).roundToInt()}%  ${t.gpsSatelliteCount} SAT", ctx.w * 0.94f, ctx.h * 0.108f, ctx.u * 1.5f, p.muted, Paint.Align.RIGHT, ctx.type.label, true)
        drawSpeed(ctx.speedValue, ctx.speedUnit, ctx.w * 0.50f, ctx.h * 0.40f, ctx.u * 21.0f, p.fg, p.muted, ctx.type.digits)
        drawGearPod(ctx.w * 0.50f, ctx.h * 0.55f, ctx.u * 10.0f, p, t, ctx.isNeutral, ctx.u, ctx.type.label, GearPodShape.SQUARE)
        drawLinearBar(ctx.w * 0.14f, ctx.h * 0.63f, ctx.w * 0.72f, ctx.u * 1.7f, ctx.rpmRatio, p.accent, p.track, ctx.redlineRatio, RL_RED, ctx.u)
        gText("RPM ${ctx.mappedRpm.roundToInt()}", ctx.w * 0.50f, ctx.h * 0.665f, ctx.u * 1.5f, p.muted, Paint.Align.CENTER, ctx.type.label, true)
        drawPanel(ctx.w * 0.06f, ctx.h * 0.71f, ctx.w * 0.40f, ctx.h * 0.13f, p)
        gText("TRIP ${ctx.formatTrip()}", ctx.w * 0.09f, ctx.h * 0.755f, ctx.u * 1.9f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
        gText("ODO ${ctx.formatOdometer()}", ctx.w * 0.09f, ctx.h * 0.795f, ctx.u * 1.6f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        drawPanel(ctx.w * 0.54f, ctx.h * 0.71f, ctx.w * 0.40f, ctx.h * 0.13f, p)
        gText("${formatTemp(t.coolantTempCelsius, ctx.useFahrenheit)}  •  LEAN ${abs(t.leanAngleDeg).roundToInt()}°", ctx.w * 0.57f, ctx.h * 0.755f, ctx.u * 1.8f, p.fg, Paint.Align.LEFT, ctx.type.label, true)
        gText("TC ${if (t.tractionControlInterventionActive) "ACT" else "STBY"}  ABS ${if (t.absInterventionActive) "ACT" else "STBY"}", ctx.w * 0.57f, ctx.h * 0.795f, ctx.u * 1.5f, p.muted, Paint.Align.LEFT, ctx.type.label, true)
        drawBrandMark(ctx.spec, p, ctx.w * 0.86f, ctx.h * 0.90f, ctx.u * 4.0f, ctx.u, p.accent.copy(alpha = 0.5f))
    }
    drawStatusRail(ctx, RailStyle.TFT)
}

/* =====================================================================================
 *  Polyline & Bumerang Bar Yardımcıları (Yuvarlak inci izi olmadan keskin OEM segmentler)
 * ===================================================================================== */
internal fun rememberBoomerangCurve(
    p0: Offset,
    knee: Offset,
    p2: Offset
): List<Offset> {
    // 3 noktalı keskin dirseği (p0 -> knee -> p2) yumuşak kuadratik Bezier omuzlu OEM eğrisine dönüştürür
    val result = ArrayList<Offset>(28)
    val legMix = 0.34f
    val topMix = 0.24f
    val kStart = Offset(
        p0.x + (knee.x - p0.x) * (1f - legMix),
        p0.y + (knee.y - p0.y) * (1f - legMix)
    )
    val kEnd = Offset(
        knee.x + (p2.x - knee.x) * topMix,
        knee.y + (p2.y - knee.y) * topMix
    )
    result.add(p0)
    val cornerSteps = 16
    for (i in 0..cornerSteps) {
        val t = i / cornerSteps.toFloat()
        val omt = 1f - t
        val x = omt * omt * kStart.x + 2f * omt * t * knee.x + t * t * kEnd.x
        val y = omt * omt * kStart.y + 2f * omt * t * knee.y + t * t * kEnd.y
        result.add(Offset(x, y))
    }
    result.add(p2)
    return result
}

internal fun DrawScope.drawPolylineBar(
    points: List<Offset>,
    segments: Int,
    filled: Float,
    width: Float,
    color: Color,
    track: Color,
    redlineRatio: Float,
    peakRatio: Float,
    u: Float
) {
    val filledSeg = (filled.coerceIn(0f, 1f) * segments).roundToInt()
    val peakSeg = if (peakRatio >= 0f) (peakRatio.coerceIn(0f, 1f) * segments).roundToInt() else -1
    val redSeg = (redlineRatio.coerceIn(0f, 1f) * segments).roundToInt()
    val halfW = width * 0.5f
    val segPath = Path()

    for (i in 0 until segments) {
        val gap = 0.14f / segments
        val t0 = (i / segments.toFloat()) + gap
        val t1 = ((i + 1) / segments.toFloat()) - gap
        if (t1 <= t0) continue
        val (a, nA) = pointAndNormalAt(points, t0)
        val (b, nB) = pointAndNormalAt(points, t1)
        val c = when {
            i < filledSeg && i >= redSeg -> RL_RED
            i < filledSeg -> color
            i == peakSeg && peakSeg > filledSeg -> Color.White
            i >= redSeg -> RL_RED.copy(alpha = 0.22f)
            else -> track
        }
        segPath.reset()
        segPath.moveTo(a.x + nA.x * halfW, a.y + nA.y * halfW)
        segPath.lineTo(b.x + nB.x * halfW, b.y + nB.y * halfW)
        segPath.lineTo(b.x - nB.x * halfW, b.y - nB.y * halfW)
        segPath.lineTo(a.x - nA.x * halfW, a.y - nA.y * halfW)
        segPath.close()
        drawPath(segPath, c)
    }
}

private fun pointAndNormalAt(points: List<Offset>, t: Float): Pair<Offset, Offset> {
    if (points.size < 2) return (points.firstOrNull() ?: Offset.Zero) to Offset(0f, -1f)
    val lengths = FloatArray(points.size - 1)
    var total = 0f
    for (i in 0 until points.size - 1) {
        val dx = points[i + 1].x - points[i].x
        val dy = points[i + 1].y - points[i].y
        lengths[i] = kotlin.math.sqrt(dx * dx + dy * dy)
        total += lengths[i]
    }
    if (total <= 0f) return points[0] to Offset(0f, -1f)
    var target = (t.coerceIn(0f, 1f)) * total
    for (i in lengths.indices) {
        val segLen = lengths[i]
        if (target <= segLen || i == lengths.lastIndex) {
            val f = if (segLen > 0f) (target / segLen).coerceIn(0f, 1f) else 0f
            val dx = points[i + 1].x - points[i].x
            val dy = points[i + 1].y - points[i].y
            val pt = Offset(points[i].x + dx * f, points[i].y + dy * f)
            val inv = if (segLen > 1e-4f) 1f / segLen else 0f
            val tx = dx * inv
            val ty = dy * inv
            // Sol/üst dış normal vektörü: (ty, -tx)
            return pt to Offset(ty, -tx)
        }
        target -= segLen
    }
    return points.last() to Offset(0f, -1f)
}

private fun pointAt(points: List<Offset>, t: Float): Offset = pointAndNormalAt(points, t).first
