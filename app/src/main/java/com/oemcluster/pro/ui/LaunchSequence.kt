package com.oemcluster.pro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * USTlabs imzalı soğuk açılış sekansı.
 *
 * 3.5 saniyelik sinematik bir kontak selamlaması: USTlabs amblemi, motor
 * telemetri hattının canlı sistem kontrolleri ve tam bir iğne süpürme
 * (0 → 299 km/h → 0, 0 → 14.000 RPM → 0) gösterir. Dokunulduğunda atlanır.
 */
@Composable
fun LaunchSequenceOverlay(
    onFinished: () -> Unit,
    versionLabel: String,
    modifier: Modifier = Modifier
) {
    var elapsed by remember { mutableFloatStateOf(0f) }
    var completed by remember { mutableStateOf(false) }

    fun complete() {
        if (!completed) {
            completed = true
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        var startNanos = 0L
        withFrameNanos { startNanos = it }
        while (!completed) {
            withFrameNanos { now ->
                elapsed = ((now - startNanos) * 1e-9f).coerceAtLeast(0f)
            }
            if (elapsed >= SEQUENCE_DURATION_SEC) complete()
        }
    }

    if (completed) return

    val intro = smoothstep(0f, 0.42f, elapsed)
    val outro = 1f - smoothstep(SEQUENCE_DURATION_SEC - 0.55f, SEQUENCE_DURATION_SEC, elapsed)
    val master = min(intro, outro)
    val sweepUp = easeOutCubic(smoothstep(0.58f, 1.72f, elapsed))
    val sweepDown = easeInOutCubic(smoothstep(1.95f, 2.55f, elapsed))
    val revProgress = when {
        elapsed < 0.58f -> 0f
        elapsed < 1.72f -> sweepUp
        elapsed < 1.95f -> 1f
        elapsed < 2.55f -> 1f - sweepDown
        else -> 0f
    }
    val speed = (revProgress * 299f).toInt().coerceIn(0, 299)
    val rpm = (revProgress * 14000f).toInt().coerceIn(0, 14000)
    val systemProgress = smoothstep(0.35f, 2.95f, elapsed)
    val sequence = if (elapsed < 2.55f) "IGNITION SEQUENCE" else "SYSTEM READY"
    val bootDone = elapsed >= 2.95f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF111826),
                        Color(0xFF070A10),
                        Color(0xFF020305)
                    ),
                    center = Offset(0.68f, 0.42f),
                    radius = 2200f
                )
            )
            .pointerInput(Unit) {
                detectTapGestures { complete() }
            }
    ) {
        LaunchBackdrop(elapsed = elapsed, master = master)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 900.dp || maxHeight < 460.dp
            val padH = if (compact) 28.dp else 46.dp
            val padV = if (compact) 16.dp else 28.dp
            val emblemSize = if (compact) 62.dp else 78.dp
            val titleSize = if (compact) 34.sp else 45.sp
            val titleTracking = if (compact) 5.sp else 7.sp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = padH, vertical = padV),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.18f)
                        .fillMaxHeight()
                        .alpha(master),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(if (compact) 22.dp else 26.dp)
                                    .background(LaunchOrange)
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = "USTLABS // MOBILE INSTRUMENT SYSTEMS",
                                color = LaunchMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = versionLabel,
                                color = LaunchMuted.copy(alpha = 0.72f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(Modifier.height(if (compact) 18.dp else 30.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UstlabsAtomMark(
                                modifier = Modifier.size(emblemSize),
                                reveal = smoothstep(0.10f, 0.62f, elapsed)
                            )
                            Spacer(Modifier.width(if (compact) 14.dp else 18.dp))
                            Column {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = LaunchWhite)) { append("UST") }
                                        withStyle(SpanStyle(color = LaunchOrange)) { append("labs") }
                                    },
                                    fontSize = if (compact) 26.sp else 31.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Text(
                                    text = "ENGINEERED IN THE PADDOCK",
                                    color = LaunchMuted,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.2.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(Modifier.height(if (compact) 16.dp else 26.dp))

                        Text(
                            text = "MOTOCROSS",
                            color = LaunchWhite,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Black,
                            letterSpacing = titleTracking,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = titleSize * 1.02f
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "OEM CLUSTER PRO",
                                color = LaunchOrange,
                                fontSize = if (compact) 10.sp else 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = if (compact) 3.sp else 4.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .width(if (compact) 42.dp else 64.dp)
                                    .height(1.dp)
                                    .background(LaunchOrange.copy(alpha = 0.5f))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "USTLABS EDITION",
                                color = LaunchMuted,
                                fontSize = 8.sp,
                                letterSpacing = 1.6.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (!compact) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Gerçek motosiklet göstergesi. Sanal ECU. 400 Hz sensör füzyonu.\n" +
                                    "Kendi motorunuz — istediğiniz OEM kadran.",
                                color = LaunchMuted.copy(alpha = 0.86f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Column {
                        LaunchStatusLine(
                            label = "15-STATE EKF",
                            detail = "GNSS DOPPLER + 400 Hz IMU",
                            activeAt = 0.55f,
                            elapsed = elapsed,
                            dark = bootDone
                        )
                        LaunchStatusLine(
                            label = "VIRTUAL ECU",
                            detail = "TORK EĞRİSİ · VİTES · SHIFT-LIGHT",
                            activeAt = 0.95f,
                            elapsed = elapsed,
                            dark = bootDone
                        )
                        LaunchStatusLine(
                            label = "OEM SKIN LIBRARY",
                            detail = "270 GÖSTERGE · 23 MARKA",
                            activeAt = 1.35f,
                            elapsed = elapsed,
                            dark = bootDone
                        )
                        Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (bootDone) "SYSTEM READY" else "SYSTEM CHECK ${(systemProgress * 100).toInt()}%",
                                color = if (bootDone) LaunchGreen else LaunchMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "TAP TO SKIP",
                                color = LaunchMuted.copy(alpha = 0.62f),
                                fontSize = 8.sp,
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.09f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(systemProgress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(LaunchOrange, LaunchCyan)
                                        )
                                    )
                            )
                        }
                    }
                }

                Spacer(Modifier.width(if (compact) 22.dp else 34.dp))

                Box(
                    modifier = Modifier
                        .weight(0.82f)
                        .fillMaxHeight()
                        .alpha(master),
                    contentAlignment = Alignment.Center
                ) {
                    LaunchTachometer(
                        revProgress = revProgress,
                        speedKmh = speed,
                        rpm = rpm,
                        sequence = sequence,
                        modifier = Modifier
                            .fillMaxHeight(0.94f)
                            .aspectRatio(1f)
                    )
                }
            }
        }

        LaunchScanlines(master = master)
    }
}

private const val SEQUENCE_DURATION_SEC = 3.5f

private val LaunchBg = Color(0xFF04060A)
private val LaunchOrange = Color(0xFFFF6A00)
private val LaunchCyan = Color(0xFF00E5FF)
private val LaunchGreen = Color(0xFF00E676)
private val LaunchMuted = Color(0xFF8B98AA)
private val LaunchWhite = Color(0xFFF4F7FB)

@Composable
private fun LaunchBackdrop(elapsed: Float, master: Float) {
    Canvas(modifier = Modifier.fillMaxSize().alpha(master)) {
        // Karbon dokusu
        val step = size.height / 26f
        var y = -size.width
        while (y < size.height + size.width) {
            drawLine(
                color = Color.White.copy(alpha = 0.018f),
                start = Offset(0f, y),
                end = Offset(size.width, y + size.width * 0.36f),
                strokeWidth = 1f
            )
            y += step
        }

        // Turuncu ve cyan ışık kütleleri
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LaunchOrange.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(size.width * 0.78f, size.height * 0.46f),
                radius = size.height * 0.52f
            ),
            radius = size.height * 0.52f,
            center = Offset(size.width * 0.78f, size.height * 0.46f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LaunchCyan.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.16f, size.height * 0.10f),
                radius = size.height * 0.42f
            ),
            radius = size.height * 0.42f,
            center = Offset(size.width * 0.16f, size.height * 0.10f)
        )

        // Yarış çizgileri
        for (i in 0 until 5) {
            val offset = (i - 2) * size.width * 0.085f
            drawLine(
                color = LaunchOrange.copy(alpha = 0.035f + 0.01f * i),
                start = Offset(size.width * 0.52f + offset, 0f),
                end = Offset(size.width * 0.30f + offset, size.height),
                strokeWidth = size.height * 0.004f
            )
        }

        // Ufuk çizgisi
        drawLine(
            color = LaunchOrange.copy(alpha = 0.12f),
            start = Offset(0f, size.height * 0.965f),
            end = Offset(size.width, size.height * 0.965f),
            strokeWidth = 1.5f
        )
    }
}

@Composable
private fun LaunchScanlines(master: Float) {
    Canvas(modifier = Modifier.fillMaxSize().alpha(master * 0.5f)) {
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.10f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += 3f
        }
    }
}

@Composable
fun UstlabsAtomMark(modifier: Modifier, reveal: Float) {
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height)
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val center = Offset(cx, cy)
        val orbitRx = s * 0.44f
        val orbitRy = s * 0.165f

        // Dış halka — enstrüman/gösterge mirası
        drawCircle(
            color = LaunchWhite.copy(alpha = 0.10f * reveal),
            radius = s * 0.49f,
            center = center,
            style = Stroke(width = s * 0.012f)
        )

        // Çekirdek parıltısı
        drawCircle(
            color = LaunchOrange.copy(alpha = 0.16f * reveal),
            radius = s * 0.24f,
            center = center
        )

        // Üç eliptik yörünge ve elektronlar
        for (i in 0 until 3) {
            rotate(degrees = i * 60f, pivot = center) {
                drawOval(
                    color = LaunchWhite.copy(alpha = 0.62f * reveal),
                    topLeft = Offset(cx - orbitRx, cy - orbitRy),
                    size = Size(orbitRx * 2f, orbitRy * 2f),
                    style = Stroke(width = s * 0.020f)
                )
                val theta = -0.95f + i * 1.9f
                val ex = cx + cos(theta) * orbitRx
                val ey = cy + sin(theta) * orbitRy
                drawCircle(
                    color = LaunchCyan.copy(alpha = 0.20f * reveal),
                    radius = s * 0.075f,
                    center = Offset(ex, ey)
                )
                drawCircle(
                    color = LaunchCyan.copy(alpha = reveal),
                    radius = s * 0.032f,
                    center = Offset(ex, ey)
                )
            }
        }

        // Çekirdek (nükleus)
        drawCircle(
            color = LaunchOrange.copy(alpha = 0.30f * reveal),
            radius = s * 0.105f,
            center = center
        )
        drawCircle(
            color = LaunchOrange.copy(alpha = reveal),
            radius = s * 0.056f,
            center = center
        )
        drawCircle(
            color = LaunchWhite.copy(alpha = 0.92f * reveal),
            radius = s * 0.022f,
            center = center
        )
    }
}

@Composable
private fun LaunchStatusLine(
    label: String,
    detail: String,
    activeAt: Float,
    elapsed: Float,
    dark: Boolean
) {
    val reveal = smoothstep(activeAt, activeAt + 0.25f, elapsed)
    val active = elapsed >= activeAt + 0.10f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .alpha(0.35f + 0.65f * reveal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    when {
                        active && dark -> LaunchGreen
                        active -> LaunchOrange
                        else -> LaunchMuted.copy(alpha = 0.35f)
                    }
                )
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = label,
            color = if (active && dark) LaunchGreen else if (active) LaunchWhite else LaunchMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = detail,
            color = LaunchMuted.copy(alpha = 0.72f),
            fontSize = 8.sp,
            letterSpacing = 0.7.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

@Composable
private fun LaunchTachometer(
    revProgress: Float,
    speedKmh: Int,
    rpm: Int,
    sequence: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val d = min(size.width, size.height)
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val radius = d * 0.415f
            val startAngle = 140f
            val sweepAngle = 260f
            val u = d / 100f

            // Dış halkalar
            drawCircle(
                color = Color.White.copy(alpha = 0.055f),
                radius = radius * 1.17f,
                center = Offset(cx, cy),
                style = Stroke(width = u * 0.22f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = radius,
                center = Offset(cx, cy),
                style = Stroke(width = u * 0.10f)
            )
            drawCircle(
                color = LaunchOrange.copy(alpha = 0.12f),
                radius = radius * 0.83f,
                center = Offset(cx, cy),
                style = Stroke(width = u * 0.09f)
            )

            // Kadran yayı
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = u * 0.85f, cap = StrokeCap.Round)
            )
            // Redline bölgesi
            drawArc(
                color = Color(0xFFFF1744).copy(alpha = 0.55f),
                startAngle = startAngle + sweepAngle * 0.86f,
                sweepAngle = sweepAngle * 0.14f,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = u * 0.85f, cap = StrokeCap.Butt)
            )
            // Aktif yay
            if (revProgress > 0.003f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(LaunchCyan, LaunchOrange, Color(0xFFFF1744)),
                        center = Offset(cx, cy)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * revProgress,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = u * 0.85f, cap = StrokeCap.Round)
                )
            }

            // Tikler ve rakamlar
            for (i in 0..28) {
                val fraction = i / 28f
                val angleRad = (startAngle + sweepAngle * fraction) * PI / 180.0
                val major = i % 2 == 0
                val inner = radius * if (major) 0.86f else 0.90f
                val outer = radius * 0.955f
                drawLine(
                    color = when {
                        fraction >= 0.86f -> Color(0xFFFF1744).copy(alpha = 0.85f)
                        fraction <= revProgress + 0.002f -> LaunchOrange
                        else -> Color.White.copy(alpha = 0.34f)
                    },
                    start = Offset(
                        cx + cos(angleRad).toFloat() * inner,
                        cy + sin(angleRad).toFloat() * inner
                    ),
                    end = Offset(
                        cx + cos(angleRad).toFloat() * outer,
                        cy + sin(angleRad).toFloat() * outer
                    ),
                    strokeWidth = if (major) u * 0.28f else u * 0.14f,
                    cap = StrokeCap.Round
                )
                if (major && i % 4 == 0) {
                    val labelR = radius * 0.75f
                    val lx = cx + cos(angleRad).toFloat() * labelR
                    val ly = cy + sin(angleRad).toFloat() * labelR
                    drawCircle(
                        color = Color.White.copy(alpha = 0.42f),
                        radius = u * 0.42f,
                        center = Offset(lx, ly)
                    )
                }
            }

            // İğne
            val needleAngle = (startAngle + sweepAngle * revProgress) * PI / 180.0
            val needleLength = radius * 0.74f
            val needleStart = Offset(
                cx - cos(needleAngle).toFloat() * u * 2.4f,
                cy - sin(needleAngle).toFloat() * u * 2.4f
            )
            val needleEnd = Offset(
                cx + cos(needleAngle).toFloat() * needleLength,
                cy + sin(needleAngle).toFloat() * needleLength
            )
            drawLine(
                color = LaunchOrange.copy(alpha = 0.22f),
                start = needleStart,
                end = needleEnd,
                strokeWidth = u * 2.3f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = LaunchOrange,
                start = needleStart,
                end = needleEnd,
                strokeWidth = u * 0.72f,
                cap = StrokeCap.Round
            )
            drawCircle(color = LaunchWhite, radius = u * 2.6f, center = Offset(cx, cy))
            drawCircle(color = LaunchOrange, radius = u * 1.35f, center = Offset(cx, cy))
            drawCircle(
                color = LaunchOrange.copy(alpha = 0.32f),
                radius = u * 5.2f,
                center = Offset(cx, cy),
                style = Stroke(width = u * 0.25f)
            )

            // Hız kadranı halkası (iç)
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius * 0.50f,
                center = Offset(cx, cy),
                style = Stroke(width = u * 0.16f)
            )

            // RPM iğnesi yönünde turuncu işaret
            val markerAngle = (startAngle + sweepAngle * revProgress) * PI / 180.0
            drawCircle(
                color = LaunchOrange,
                radius = u * 1.0f,
                center = Offset(
                    cx + cos(markerAngle).toFloat() * radius * 0.965f,
                    cy + sin(markerAngle).toFloat() * radius * 0.965f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "%03d".format(speedKmh),
                color = LaunchWhite,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-2).sp,
                lineHeight = 52.sp
            )
            Text(
                text = "km/h",
                color = LaunchOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(LaunchOrange.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "N",
                        color = LaunchOrange,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "RPM ${"%05d".format(rpm)}",
                    color = LaunchMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = sequence,
            color = if (sequence == "SYSTEM READY") LaunchGreen else LaunchMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        )
    }
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    if (edge1 <= edge0) return if (x >= edge1) 1f else 0f
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun easeOutCubic(x: Float): Float {
    val v = 1f - x.coerceIn(0f, 1f)
    return 1f - v * v * v
}

private fun easeInOutCubic(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f
}
