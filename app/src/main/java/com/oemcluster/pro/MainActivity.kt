package com.oemcluster.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.oemcluster.pro.catalog.DeclarativeGaugeSpec
import com.oemcluster.pro.catalog.GaugeDisplayCategory
import com.oemcluster.pro.catalog.OemGaugeCatalog105
import com.oemcluster.pro.data.BikeDatabaseRepository
import com.oemcluster.pro.domain.IgnitionBootPhase
import com.oemcluster.pro.domain.RpmScalingMode
import com.oemcluster.pro.domain.ShiftLightState
import com.oemcluster.pro.domain.TireSpecification
import com.oemcluster.pro.domain.TransmissionEngagementState
import com.oemcluster.pro.domain.UnifiedTelemetryState
import com.oemcluster.pro.domain.WheelieGaugeDisplayMode
import com.oemcluster.pro.i18n.AppLanguage
import com.oemcluster.pro.i18n.LocalizationManager
import com.oemcluster.pro.i18n.StringKey
import com.oemcluster.pro.renderer.OemClusterCanvasRenderer
import com.oemcluster.pro.sensors.AndroidHardwareSensorManager
import com.oemcluster.pro.sensors.NativeTelemetryEngineBridge
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var hardwareSensorManager: AndroidHardwareSensorManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            hardwareSensorManager.startGnssUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ekranı her zaman açık tut (WakeLock) ve 120 Hz yenileme hızını talep et
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        request120HzRefreshRate()

        hardwareSensorManager = AndroidHardwareSensorManager(this, lifecycleScope)

        // Aktif motosiklet profilini C++20 NDK motoruna yükle (Altın Kural - Single Source of Truth)
        val initialBike = BikeDatabaseRepository.activeBikeProfile.value
        NativeTelemetryEngineBridge.applyActiveBikeProfileToNative(initialBike)
        NativeTelemetryEngineBridge.nativeTriggerIgnitionSweep(System.nanoTime())

        requestSensorPermissionsIfNeeded()
        hardwareSensorManager.startImuAndLightSensors()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFF6600),
                    secondary = Color(0xFF00E5FF),
                    background = Color(0xFF07080A),
                    surface = Color(0xFF12151C)
                )
            ) {
                MotoCrossClusterApp(
                    onZeroCalibrateNow = {
                        NativeTelemetryEngineBridge.nativeTriggerZeroLeanCalibrationNow()
                    },
                    onToggleAcousticMic = { enabled ->
                        if (enabled) hardwareSensorManager.startAcousticMicrophone48kHz()
                        else hardwareSensorManager.stopAcousticMicrophone()
                    },
                    onThemeModeChanged = { mode ->
                        hardwareSensorManager.setThemeOverrideMode(mode)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        hardwareSensorManager.stopAllSensors()
        super.onDestroy()
    }

    private fun requestSensorPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            hardwareSensorManager.startGnssUpdates()
        }
    }

    private fun request120HzRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = windowManager.defaultDisplay
            val bestMode = display.supportedModes.maxByOrNull { it.refreshRate }
            if (bestMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = bestMode.modeId
                window.attributes = params
            }
        }
    }
}

@Composable
fun MotoCrossClusterApp(
    onZeroCalibrateNow: () -> Unit,
    onToggleAcousticMic: (Boolean) -> Unit,
    onThemeModeChanged: (Int) -> Unit
) {
    val language by LocalizationManager.currentLanguage.collectAsState()
    val activeBike by BikeDatabaseRepository.activeBikeProfile.collectAsState()
    val allGarageBikes by BikeDatabaseRepository.userGarageProfiles.collectAsState()
    val nativeTelemetry by NativeTelemetryEngineBridge.telemetryState.collectAsState()

    // Seçili Gösterge (105 OEM Model içinden) ve Alt Modu
    var selectedGaugeIndex by remember { mutableIntStateOf(0) } // Varsayılan: #1 KTM 390 Duke
    var selectedSubModeIndex by remember { mutableIntStateOf(0) }
    val favoriteSkinIds = remember {
        mutableStateListOf(
            "ktm_390_duke_gen2",
            "bmw_s1000rr_k67",
            "yamaha_yzf_r1",
            "ducati_panigale_v4_evo",
            "suzuki_hayabusa_gen3",
            "honda_cbr1000rrr_sp",
            "kawasaki_zx10r_2021"
        )
    }

    // Kontrol Merkezi Modal Durumu
    var isControlCenterOpen by remember { mutableStateOf(false) }
    var activeMenuTab by remember { mutableIntStateOf(0) } // 0: Garajım, 1: 105 Galeri, 2: Ayarlar, 3: Yol Bilgisayarı & Drag

    // Kalibrasyon, Shift-Light, Teker Ölçer ve Ekran Ayarları
    var useMph by remember { mutableStateOf(false) }
    var useOemSpeedBias by remember { mutableStateOf(false) }
    var oemBiasRatio by remember { mutableFloatStateOf(0.05f) }
    var japan299Lock by remember { mutableStateOf(false) }
    var rpmScalingMode by remember { mutableStateOf(RpmScalingMode.MODE_A_PROPORTIONAL_REDLINE) }
    var wheelieDisplayMode by remember { mutableStateOf(WheelieGaugeDisplayMode.SMART_AUTO_POPUP) }
    var autoOptimalShift by remember { mutableStateOf(true) }
    var manualStage1Rpm by remember { mutableFloatStateOf(8800f) }
    var manualStage2Rpm by remember { mutableFloatStateOf(9800f) }
    var reactionTimeMs by remember { mutableFloatStateOf(180f) }
    var acousticMicEnabled by remember { mutableStateOf(false) }
    var themeOverrideMode by remember { mutableIntStateOf(2) } // 0: Auto, 1: Gündüz, 2: Gece
    var chromaKeyGreen by remember { mutableStateOf(false) }
    var oledPixelShiftEnabled by remember { mutableStateOf(true) }

    // Masaüstü / Emülatör Testi için Canlı Sürüş & Teker Simülatörü (İsteğe bağlı açılır çubuk)
    var simDrawerOpen by remember { mutableStateOf(false) }
    var simActive by remember { mutableStateOf(false) }
    var simSpeedKmh by remember { mutableFloatStateOf(0f) }
    var simGear by remember { mutableIntStateOf(1) }
    var simClutchIn by remember { mutableStateOf(false) }
    var simLeanDeg by remember { mutableFloatStateOf(0f) }
    var simWheelieDeg by remember { mutableFloatStateOf(0f) }

    // Yol Bilgisayarı, Drag Box ve Rekor Kayıtları
    var tripAKm by remember { mutableDoubleStateOf(0.0) }
    var tripBKm by remember { mutableDoubleStateOf(0.0) }
    var odometerKm by remember { mutableDoubleStateOf(1420.5) }
    var peakSpeedKmh by remember { mutableDoubleStateOf(0.0) }
    var peakRpm by remember { mutableDoubleStateOf(0.0) }
    var peakLeanLeftDeg by remember { mutableDoubleStateOf(0.0) }
    var peakLeanRightDeg by remember { mutableDoubleStateOf(0.0) }
    var peakWheelieDeg by remember { mutableDoubleStateOf(0.0) }
    var longestWheelieSec by remember { mutableDoubleStateOf(0.0) }
    var longestWheelieMeters by remember { mutableDoubleStateOf(0.0) }
    var simCurrentWheelieSec by remember { mutableDoubleStateOf(0.0) }
    var simCurrentWheelieMeters by remember { mutableDoubleStateOf(0.0) }
    var simWheelieHoldSec by remember { mutableDoubleStateOf(0.0) }
    var lastFrameNanos by remember { mutableLongStateOf(0L) }

    // 120 Hz Render ve Telemetri Döngüsü
    LaunchedEffect(
        activeBike,
        useOemSpeedBias,
        oemBiasRatio,
        japan299Lock,
        autoOptimalShift,
        manualStage1Rpm,
        manualStage2Rpm,
        reactionTimeMs
    ) {
        NativeTelemetryEngineBridge.applyActiveBikeProfileToNative(activeBike)
        NativeTelemetryEngineBridge.nativeConfigureCalibrationAndShiftLight(
            useOemSpeedBiasMode = useOemSpeedBias,
            oemSpeedBiasRatio = oemBiasRatio.toDouble(),
            enableJapan299Lock = japan299Lock,
            autoOptimalShiftMode = autoOptimalShift,
            manualStage1Rpm = manualStage1Rpm.toDouble(),
            manualStage2Rpm = manualStage2Rpm.toDouble(),
            humanReactionTimeSec = (reactionTimeMs / 1000f).toDouble()
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameNanos ->
                val dtSec = if (lastFrameNanos == 0L) (1.0 / 120.0)
                else ((frameNanos - lastFrameNanos) * 1e-9).coerceIn(0.001, 0.050)
                lastFrameNanos = frameNanos

                if (!simActive) {
                    NativeTelemetryEngineBridge.tick120HzFrame(frameNanos)
                } else {
                    // Canlı Test / Masaüstü Simülatör Modu: Aktif Motorun Gerçek Dişli ve Toroidal Lastik Fiziğini Çalıştırır
                    val trueSpdMs = simSpeedKmh / 3.6
                    val rUnloaded = activeBike.rearTire.unloadedRadiusMm
                    val rLean = rUnloaded * activeBike.rearTire.staticSquishFactor -
                        activeBike.rearTire.crownRadiusMm * (1.0 - cos(Math.toRadians(abs(simLeanDeg.toDouble()))))
                    val rEffMm = rLean * (1.0 + 1.8e-6 * trueSpdMs * trueSpdMs)

                    val wheelRpm = (trueSpdMs * 60.0) / (2.0 * PI * (rEffMm / 1000.0))
                    val gearRatio = activeBike.gearRatios.getOrElse((simGear - 1).coerceIn(0, 5)) { 1.0 }
                    val calcRpm = if (simClutchIn || simSpeedKmh < 1f) {
                        activeBike.idleRpm
                    } else {
                        (wheelRpm * activeBike.primaryReductionRatio * activeBike.finalDriveRatio * gearRatio)
                            .coerceIn(activeBike.idleRpm, activeBike.revLimiterRpm)
                    }

                    val optStage2 = if (autoOptimalShift) {
                        activeBike.computeOptimalShiftRpmForGear(simGear)
                    } else {
                        manualStage2Rpm.toDouble()
                    }
                    val optStage1 = if (autoOptimalShift) optStage2 - 1000.0 else manualStage1Rpm.toDouble()

                    val timeSec = frameNanos * 1e-9
                    val shiftState = when {
                        calcRpm >= activeBike.revLimiterRpm - 80.0 -> ShiftLightState.OVER_REV_LIMITER
                        calcRpm >= optStage2 -> ShiftLightState.STAGE_2_SHIFT_NOW
                        calcRpm >= optStage1 -> ShiftLightState.STAGE_1_APPROACHING
                        else -> ShiftLightState.NORMAL
                    }
                    val flashOn = when (shiftState) {
                        ShiftLightState.OVER_REV_LIMITER -> (timeSec * 12.0) % 1.0 < 0.5
                        ShiftLightState.STAGE_2_SHIFT_NOW -> (timeSec * 8.0) % 1.0 < 0.5
                        ShiftLightState.STAGE_1_APPROACHING -> true
                        ShiftLightState.NORMAL -> false
                    }
                    val bounceRpm = if (shiftState == ShiftLightState.OVER_REV_LIMITER) {
                        activeBike.revLimiterRpm + 150.0 * sin(timeSec * 2.0 * PI * 14.0)
                    } else calcRpm

                    // Teker Derece Ölçer & Teker Metre Simülasyonu
                    val isWheelieNow = simWheelieDeg >= 5.0f && simSpeedKmh > 6f
                    if (isWheelieNow) {
                        simCurrentWheelieSec += dtSec
                        simCurrentWheelieMeters += trueSpdMs * dtSec
                        simWheelieHoldSec = 3.2
                        peakWheelieDeg = max(peakWheelieDeg, simWheelieDeg.toDouble())
                        longestWheelieSec = max(longestWheelieSec, simCurrentWheelieSec)
                        longestWheelieMeters = max(longestWheelieMeters, simCurrentWheelieMeters)
                    } else if (simWheelieHoldSec > 0.0) {
                        simWheelieHoldSec = max(0.0, simWheelieHoldSec - dtSec)
                    } else {
                        simCurrentWheelieSec = 0.0
                        simCurrentWheelieMeters = 0.0
                    }

                    val rawDisplaySpd = if (useOemSpeedBias && simSpeedKmh > 1f) {
                        simSpeedKmh * (1.0 + oemBiasRatio) + 1.5
                    } else simSpeedKmh.toDouble()
                    val locked299 = japan299Lock && rawDisplaySpd >= 299.0
                    val finalDisplaySpd = if (locked299) 299.0 else rawDisplaySpd

                    val trqNm = activeBike.interpolateEngineTorqueNm(bounceRpm)
                    val wheelHp = ((trqNm * bounceRpm * 2.0 * PI) / 60000.0) * 1.341 * 0.90

                    NativeTelemetryEngineBridge.publishTelemetryState(
                        UnifiedTelemetryState(
                            timestampNanos = frameNanos,
                            activeBikeProfileId = activeBike.profileId,
                            activeBikeDisplayName = "${activeBike.brand} ${activeBike.modelName}",
                            bikeIdleRpm = activeBike.idleRpm,
                            bikeRedlineRpm = activeBike.redlineRpm,
                            bikeRevLimiterRpm = activeBike.revLimiterRpm,
                            trueSpeedMs = trueSpdMs,
                            trueSpeedKmh = simSpeedKmh.toDouble(),
                            displayedSpeedKmh = finalDisplaySpd,
                            is299LockEngaged = locked299,
                            actualEngineRpm = bounceRpm,
                            normalizedRpmRatio = bounceRpm / max(1000.0, activeBike.redlineRpm),
                            peakHoldRpm = bounceRpm,
                            stage1ShiftRpm = optStage1,
                            stage2ShiftRpm = optStage2,
                            shiftLightState = shiftState,
                            shiftFlashPhaseOn = flashOn,
                            fullColorInversionActive = (shiftState == ShiftLightState.STAGE_2_SHIFT_NOW ||
                                shiftState == ShiftLightState.OVER_REV_LIMITER) && flashOn,
                            currentGear = if (simSpeedKmh < 1f && !simClutchIn) 0 else simGear,
                            isNeutral = simSpeedKmh < 1f && !simClutchIn,
                            isClutchPulled = simClutchIn,
                            transmissionState = if (simClutchIn) TransmissionEngagementState.CLUTCH_PULLED_COASTING
                            else TransmissionEngagementState.ENGAGED_IN_GEAR,
                            leanAngleDeg = simLeanDeg.toDouble(),
                            pitchAngleDeg = simWheelieDeg.toDouble(),
                            effectiveTireRadiusMm = rEffMm,
                            estimatedWheelHorsepower = wheelHp,
                            estimatedWheelTorqueNm = trqNm * activeBike.primaryReductionRatio * activeBike.finalDriveRatio * gearRatio * 0.92,
                            nightModeTransitionFactor = if (themeOverrideMode == 1) 0f else 1f,
                            ignitionBootPhase = IgnitionBootPhase.RUNNING_LIVE,
                            isWheelieActive = isWheelieNow,
                            isWheelieSummaryVisible = isWheelieNow || simWheelieHoldSec > 0.0,
                            wheelieAngleDeg = simWheelieDeg.toDouble(),
                            currentWheelieDurationSec = simCurrentWheelieSec,
                            currentWheelieDistanceMeters = simCurrentWheelieMeters,
                            maxWheelieAngleDeg = peakWheelieDeg,
                            longestWheelieDurationSec = longestWheelieSec,
                            longestWheelieDistanceMeters = longestWheelieMeters
                        )
                    )
                }

                // Mesafe ve Tepe Değer Kayıtları
                val cur = NativeTelemetryEngineBridge.telemetryState.value
                val dKm = (cur.trueSpeedMs * dtSec) / 1000.0
                tripAKm += dKm
                tripBKm += dKm
                odometerKm += dKm
                peakSpeedKmh = max(peakSpeedKmh, cur.trueSpeedKmh)
                peakRpm = max(peakRpm, cur.actualEngineRpm)
                if (cur.leanAngleDeg < 0) peakLeanLeftDeg = max(peakLeanLeftDeg, abs(cur.leanAngleDeg))
                if (cur.leanAngleDeg > 0) peakLeanRightDeg = max(peakLeanRightDeg, cur.leanAngleDeg)
                peakWheelieDeg = max(peakWheelieDeg, cur.maxWheelieAngleDeg)
                longestWheelieSec = max(longestWheelieSec, cur.longestWheelieDurationSec)
                longestWheelieMeters = max(longestWheelieMeters, cur.longestWheelieDistanceMeters)
            }
        }
    }

    val currentSpec = OemGaugeCatalog105.all105Models[selectedGaugeIndex.coerceIn(0, OemGaugeCatalog105.all105Models.lastIndex)]
    val pixelShiftOffset = if (oledPixelShiftEnabled) {
        val phase = (nativeTelemetry.timestampNanos / 1_000_000_000L) % 60L
        Offset(
            x = sin(phase * 0.1047).toFloat() * 3.0f,
            y = cos(phase * 0.1047).toFloat() * 2.0f
        )
    } else Offset.Zero

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { isControlCenterOpen = true },
                    onDoubleTap = {
                        // Çift dokunuş: Aktif göstergenin alt modları (Street / Track / Core 1-2-3) arasında geçiş
                        val modeCount = currentSpec.availableSubModes.size
                        if (modeCount > 1) {
                            selectedSubModeIndex = (selectedSubModeIndex + 1) % modeCount
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var dragAccum = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (dragAccum < -90f) {
                            selectedGaugeIndex = (selectedGaugeIndex + 1) % OemGaugeCatalog105.all105Models.size
                            selectedSubModeIndex = 0
                        } else if (dragAccum > 90f) {
                            selectedGaugeIndex = (selectedGaugeIndex - 1 + OemGaugeCatalog105.all105Models.size) %
                                OemGaugeCatalog105.all105Models.size
                            selectedSubModeIndex = 0
                        }
                    }
                )
            }
    ) {
        // 1. 120 FPS OEM Gösterge Çizim Katmanı
        OemClusterCanvasRenderer(
            spec = currentSpec,
            subModeIndex = selectedSubModeIndex,
            telemetry = nativeTelemetry,
            rpmScalingMode = rpmScalingMode,
            wheelieDisplayMode = wheelieDisplayMode,
            useMph = useMph,
            chromaKeyGreenScreen = chromaKeyGreen,
            oledPixelShiftOffset = pixelShiftOffset,
            language = language
        )

        // 2. Üst Köşe Hızlı Erişim Butonları (Eldiven Dostu Menü + Canlı Test Çekmecesi)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xAA10141C),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.clickable { isControlCenterOpen = true }
            ) {
                Text(
                    text = "☰ ${LocalizationManager.getString(StringKey.CONTROL_CENTER_TITLE, language)} (${activeBike.brand} ${activeBike.modelName})",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = if (simActive) Color(0xFFFF6600) else Color(0xAA10141C),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable { simDrawerOpen = !simDrawerOpen }
                ) {
                    Text(
                        text = if (simActive) "⚡ TEST SİMÜLATÖRÜ AKTİF" else "🎛️ CANLI TEST / TEKER SİMÜLATÖRÜ",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 3. Masaüstü / Emülatör Canlı Sürüş & Teker Derece/Metre Test Paneli
        if (simDrawerOpen) {
            Surface(
                color = Color(0xEE0D1017),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 52.dp, end = 16.dp)
                    .width(360.dp)
                    .border(1.dp, Color(0xFFFF6600), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Canlı Sürüş & Teker Simülatörü", color = Color.White, fontWeight = FontWeight.Bold)
                        Switch(checked = simActive, onCheckedChange = { simActive = it })
                    }
                    Text(
                        text = "Hız: ${simSpeedKmh.roundToInt()} km/h  |  Vites: $simGear",
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp
                    )
                    Slider(
                        value = simSpeedKmh,
                        onValueChange = {
                            simActive = true
                            simSpeedKmh = it
                        },
                        valueRange = 0f..299f
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (g in 1..6) {
                            Button(
                                onClick = {
                                    simActive = true
                                    simGear = g
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (simGear == g) Color(0xFFFF6600) else Color(0xFF222834)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$g", fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Teker Açısı (Wheelie): ${simWheelieDeg.roundToInt()}° (≥5° Teker Sayacı Başlar)",
                        color = Color(0xFF00E676),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = simWheelieDeg,
                        onValueChange = {
                            simActive = true
                            if (simSpeedKmh < 35f) simSpeedKmh = 68f
                            simWheelieDeg = it
                        },
                        valueRange = 0f..58f
                    )
                    Text(
                        text = "Viraj Yatış Açısı (Lean): ${simLeanDeg.roundToInt()}°",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = simLeanDeg,
                        onValueChange = {
                            simActive = true
                            simLeanDeg = it
                        },
                        valueRange = -55f..55f
                    )
                }
            }
        }

        // 4. Ana Kontrol Merkezi (Garajım, 105 Gösterge Galerisi, Gelişmiş Ayarlar, Yol Bilgisayarı & Drag Box)
        if (isControlCenterOpen) {
            Surface(
                color = Color(0xF50A0D12),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                    // Üst Başlık ve Sekmeler
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏍️ " + LocalizationManager.getString(StringKey.APP_TITLE, language),
                            color = Color(0xFFFF6600),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { isControlCenterOpen = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                        ) {
                            Text("✕ KADRANA DÖN", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val tabs = listOf(
                        "🏠 " + LocalizationManager.getString(StringKey.MENU_MY_GARAGE, language),
                        "🎨 " + LocalizationManager.getString(StringKey.MENU_DASHBOARD_GALLERY, language),
                        "⚙️ " + LocalizationManager.getString(StringKey.MENU_SETTINGS, language),
                        "🏁 " + LocalizationManager.getString(StringKey.MENU_TRIP_TELEMETRY, language)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEachIndexed { idx, title ->
                            Button(
                                onClick = { activeMenuTab = idx },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeMenuTab == idx) Color(0xFFFF6600) else Color(0xFF1A1F29)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(title, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sekme İçerikleri
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (activeMenuTab) {
                            0 -> GarageProfileTab(
                                activeBike = activeBike,
                                allBikes = allGarageBikes,
                                language = language
                            )
                            1 -> DashboardGallery105Tab(
                                selectedGaugeIndex = selectedGaugeIndex,
                                favoriteSkinIds = favoriteSkinIds,
                                language = language,
                                onSelectGauge = { idx ->
                                    selectedGaugeIndex = idx
                                    selectedSubModeIndex = 0
                                    NativeTelemetryEngineBridge.nativeTriggerIgnitionSweep(System.nanoTime())
                                    isControlCenterOpen = false
                                }
                            )
                            2 -> AdvancedSettingsTab(
                                language = language,
                                useMph = useMph,
                                onUseMphChange = { useMph = it },
                                useOemSpeedBias = useOemSpeedBias,
                                onUseOemSpeedBiasChange = { useOemSpeedBias = it },
                                oemBiasRatio = oemBiasRatio,
                                onOemBiasRatioChange = { oemBiasRatio = it },
                                japan299Lock = japan299Lock,
                                onJapan299LockChange = { japan299Lock = it },
                                rpmScalingMode = rpmScalingMode,
                                onRpmScalingModeChange = { rpmScalingMode = it },
                                wheelieDisplayMode = wheelieDisplayMode,
                                onWheelieDisplayModeChange = { wheelieDisplayMode = it },
                                autoOptimalShift = autoOptimalShift,
                                onAutoOptimalShiftChange = { autoOptimalShift = it },
                                reactionTimeMs = reactionTimeMs,
                                onReactionTimeMsChange = { reactionTimeMs = it },
                                acousticMicEnabled = acousticMicEnabled,
                                onAcousticMicChange = {
                                    acousticMicEnabled = it
                                    onToggleAcousticMic(it)
                                },
                                themeOverrideMode = themeOverrideMode,
                                onThemeOverrideChange = {
                                    themeOverrideMode = it
                                    onThemeModeChanged(it)
                                },
                                chromaKeyGreen = chromaKeyGreen,
                                onChromaKeyChange = { chromaKeyGreen = it },
                                onZeroCalibrateNow = onZeroCalibrateNow
                            )
                            3 -> TripDragAndWheelieRecordsTab(
                                language = language,
                                tripAKm = tripAKm,
                                tripBKm = tripBKm,
                                odometerKm = odometerKm,
                                peakSpeedKmh = peakSpeedKmh,
                                peakRpm = peakRpm,
                                peakLeanLeftDeg = peakLeanLeftDeg,
                                peakLeanRightDeg = peakLeanRightDeg,
                                peakWheelieDeg = peakWheelieDeg,
                                longestWheelieSec = longestWheelieSec,
                                longestWheelieMeters = longestWheelieMeters,
                                onResetTripAndPeaks = {
                                    tripAKm = 0.0
                                    peakSpeedKmh = 0.0
                                    peakRpm = 0.0
                                    peakLeanLeftDeg = 0.0
                                    peakLeanRightDeg = 0.0
                                    peakWheelieDeg = 0.0
                                    longestWheelieSec = 0.0
                                    longestWheelieMeters = 0.0
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GarageProfileTab(
    activeBike: com.oemcluster.pro.domain.BikeMechanicalProfile,
    allBikes: List<com.oemcluster.pro.domain.BikeMechanicalProfile>,
    language: AppLanguage
) {
    var frontTeethText by remember(activeBike) { mutableStateOf(activeBike.frontSprocketTeeth.toString()) }
    var rearTeethText by remember(activeBike) { mutableStateOf(activeBike.rearSprocketTeeth.toString()) }
    var rearTireSpecText by remember(activeBike) { mutableStateOf(activeBike.rearTire.toSpecString()) }
    var redlineRpmText by remember(activeBike) { mutableStateOf(activeBike.redlineRpm.roundToInt().toString()) }
    var limiterRpmText by remember(activeBike) { mutableStateOf(activeBike.revLimiterRpm.roundToInt().toString()) }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Sol Sütun: Fabrika Veritabanı Motor Seçimi
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = LocalizationManager.getString(StringKey.GARAGE_ACTIVE_PROFILE_BADGE, language),
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = LocalizationManager.getString(StringKey.GARAGE_SINGLE_SOURCE_WARNING, language),
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            items(allBikes) { bike ->
                val isSelected = bike.profileId == activeBike.profileId
                Surface(
                    color = if (isSelected) Color(0xFF28180D) else Color(0xFF151922),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFFFF6600) else Color(0xFF282E3D),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            BikeDatabaseRepository.selectActiveBikeProfile(bike.profileId)
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${bike.brand} ${bike.modelName} (${bike.modelYear})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Dişli: ${bike.frontSprocketTeeth}/${bike.rearSprocketTeeth}  •  Lastik: ${bike.rearTire.toSpecString()}  •  Redline: ${bike.redlineRpm.roundToInt()} RPM",
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Sağ Sütun: Dişli, Lastik Ebadı ve ECU Flash Özelleştirme (Custom Tuning)
        Surface(
            color = Color(0xFF141820),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            LazyColumn(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "🔧 ${LocalizationManager.getString(StringKey.GARAGE_CUSTOM_TUNING_HEADER, language)}: ${activeBike.brand} ${activeBike.modelName}",
                        color = Color(0xFFFF6600),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = frontTeethText,
                            onValueChange = { frontTeethText = it },
                            label = { Text(LocalizationManager.getString(StringKey.PARAM_FRONT_SPROCKET, language)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = rearTeethText,
                            onValueChange = { rearTeethText = it },
                            label = { Text(LocalizationManager.getString(StringKey.PARAM_REAR_SPROCKET, language)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = rearTireSpecText,
                        onValueChange = { rearTireSpecText = it },
                        label = { Text(LocalizationManager.getString(StringKey.PARAM_REAR_TIRE_SPEC, language)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = redlineRpmText,
                            onValueChange = { redlineRpmText = it },
                            label = { Text(LocalizationManager.getString(StringKey.PARAM_REDLINE_RPM, language)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = limiterRpmText,
                            onValueChange = { limiterRpmText = it },
                            label = { Text(LocalizationManager.getString(StringKey.PARAM_REV_LIMITER_RPM, language)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    val optRpms = activeBike.computeAllOptimalShiftRpms()
                    Text(
                        text = "Hesaplanan Optimum Vites Devirleri (RPM_opt[1..5]): " +
                            optRpms.take(5).joinToString(" / ") { "${it.roundToInt()}" },
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp
                    )
                }
                item {
                    Button(
                        onClick = {
                            val updated = activeBike.copy(
                                frontSprocketTeeth = frontTeethText.toIntOrNull() ?: activeBike.frontSprocketTeeth,
                                rearSprocketTeeth = rearTeethText.toIntOrNull() ?: activeBike.rearSprocketTeeth,
                                rearTire = TireSpecification.parse(rearTireSpecText, activeBike.rearTire.staticSquishFactor),
                                redlineRpm = redlineRpmText.toDoubleOrNull() ?: activeBike.redlineRpm,
                                revLimiterRpm = limiterRpmText.toDoubleOrNull() ?: activeBike.revLimiterRpm
                            )
                            BikeDatabaseRepository.upsertBikeProfile(updated, setAsActive = true)
                            NativeTelemetryEngineBridge.applyActiveBikeProfileToNative(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ " + LocalizationManager.getString(StringKey.ACTION_SAVE, language),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardGallery105Tab(
    selectedGaugeIndex: Int,
    favoriteSkinIds: MutableList<String>,
    language: AppLanguage,
    onSelectGauge: (Int) -> Unit
) {
    var selectedBrand by remember { mutableStateOf("ALL") }
    var selectedCategory by remember { mutableStateOf<GaugeDisplayCategory?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredModels = remember(selectedBrand, selectedCategory, favoritesOnly, searchQuery, favoriteSkinIds.size) {
        OemGaugeCatalog105.all105Models.filter { spec ->
            val brandOk = selectedBrand == "ALL" || spec.brand.equals(selectedBrand, ignoreCase = true)
            val catOk = selectedCategory == null || spec.displayCategory == selectedCategory
            val favOk = !favoritesOnly || favoriteSkinIds.contains(spec.skinId)
            val qOk = searchQuery.isBlank() ||
                spec.modelName.contains(searchQuery, ignoreCase = true) ||
                spec.brand.contains(searchQuery, ignoreCase = true) ||
                spec.modelNumber.toString() == searchQuery.trim()
            brandOk && catOk && favOk && qOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = LocalizationManager.getString(StringKey.GALLERY_SKIN_INDEPENDENT_NOTE, language),
            color = Color(0xFF00E5FF),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Marka Filtre Çubuğu (12 Marka)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChipButton(
                    label = LocalizationManager.getString(StringKey.GALLERY_ALL_BRANDS, language),
                    selected = selectedBrand == "ALL",
                    onClick = { selectedBrand = "ALL" }
                )
            }
            item {
                FilterChipButton(
                    label = "⭐ " + LocalizationManager.getString(StringKey.ACTION_FAVORITES_ONLY, language),
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly }
                )
            }
            items(OemGaugeCatalog105.allBrands) { brand ->
                FilterChipButton(
                    label = brand,
                    selected = selectedBrand == brand,
                    onClick = { selectedBrand = brand }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 105 Gösterge Listesi
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredModels) { spec ->
                val globalIdx = spec.modelNumber - 1
                val isSelected = globalIdx == selectedGaugeIndex
                val isFav = favoriteSkinIds.contains(spec.skinId)
                Surface(
                    color = if (isSelected) Color(0xFF28180D) else Color(0xFF141821),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFFFF6600) else Color(0xFF252B38),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectGauge(globalIdx) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${spec.modelNumber}  ${spec.brand} — ${spec.modelName} (${spec.generationYears})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Kadran Redline: ${spec.gaugeRedlineRpm.roundToInt()} RPM  •  Alt Modlar: ${spec.availableSubModes.joinToString(", ")}",
                                color = Color(0xFFB0BEC5),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = if (isFav) "★" else "☆",
                            color = if (isFav) Color(0xFFFFD600) else Color.Gray,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .clickable {
                                    if (isFav) favoriteSkinIds.remove(spec.skinId)
                                    else favoriteSkinIds.add(spec.skinId)
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingsTab(
    language: AppLanguage,
    useMph: Boolean,
    onUseMphChange: (Boolean) -> Unit,
    useOemSpeedBias: Boolean,
    onUseOemSpeedBiasChange: (Boolean) -> Unit,
    oemBiasRatio: Float,
    onOemBiasRatioChange: (Float) -> Unit,
    japan299Lock: Boolean,
    onJapan299LockChange: (Boolean) -> Unit,
    rpmScalingMode: RpmScalingMode,
    onRpmScalingModeChange: (RpmScalingMode) -> Unit,
    wheelieDisplayMode: WheelieGaugeDisplayMode,
    onWheelieDisplayModeChange: (WheelieGaugeDisplayMode) -> Unit,
    autoOptimalShift: Boolean,
    onAutoOptimalShiftChange: (Boolean) -> Unit,
    reactionTimeMs: Float,
    onReactionTimeMsChange: (Float) -> Unit,
    acousticMicEnabled: Boolean,
    onAcousticMicChange: (Boolean) -> Unit,
    themeOverrideMode: Int,
    onThemeOverrideChange: (Int) -> Unit,
    chromaKeyGreen: Boolean,
    onChromaKeyChange: (Boolean) -> Unit,
    onZeroCalibrateNow: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 9 Dil Seçimi (Anında Değişim)
        item {
            Text(
                text = "🌍 " + LocalizationManager.getString(StringKey.SETTINGS_LANGUAGE, language),
                color = Color(0xFFFF6600),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppLanguage.entries) { lang ->
                    FilterChipButton(
                        label = "${lang.flagEmoji} ${lang.nativeName}",
                        selected = lang == language,
                        onClick = { LocalizationManager.setLanguage(lang) }
                    )
                }
            }
        }

        // 2. Teker Derece Ölçer & Teker Metre Görünüm Ayarı
        item {
            Text(
                text = "🎯 " + LocalizationManager.getString(StringKey.SETTINGS_WHEELIE_MODE, language),
                color = Color(0xFF00E676),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipButton(
                    label = LocalizationManager.getString(StringKey.SETTINGS_WHEELIE_SMART_AUTO, language),
                    selected = wheelieDisplayMode == WheelieGaugeDisplayMode.SMART_AUTO_POPUP,
                    onClick = { onWheelieDisplayModeChange(WheelieGaugeDisplayMode.SMART_AUTO_POPUP) }
                )
                FilterChipButton(
                    label = LocalizationManager.getString(StringKey.SETTINGS_WHEELIE_ALWAYS_ON, language),
                    selected = wheelieDisplayMode == WheelieGaugeDisplayMode.ALWAYS_VISIBLE,
                    onClick = { onWheelieDisplayModeChange(WheelieGaugeDisplayMode.ALWAYS_VISIBLE) }
                )
                FilterChipButton(
                    label = LocalizationManager.getString(StringKey.SETTINGS_WHEELIE_HIDDEN, language),
                    selected = wheelieDisplayMode == WheelieGaugeDisplayMode.HIDDEN_OEM_PURE,
                    onClick = { onWheelieDisplayModeChange(WheelieGaugeDisplayMode.HIDDEN_OEM_PURE) }
                )
            }
        }

        // 3. Saf GPS Hızı vs OEM ECE-R39 Hız Sapması ve 299 Kilidi
        item {
            Text(
                text = "⚡ " + LocalizationManager.getString(StringKey.SETTINGS_CALIBRATION_SECTION, language),
                color = Color(0xFFFF6600),
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(LocalizationManager.getString(StringKey.SETTINGS_OEM_ECE_R39_SPEED, language), color = Color.White)
                Switch(checked = useOemSpeedBias, onCheckedChange = onUseOemSpeedBiasChange)
                Text("Japon 299 km/h Kilidi", color = Color.White)
                Switch(checked = japan299Lock, onCheckedChange = onJapan299LockChange)
                Text("MPH Birimi", color = Color.White)
                Switch(checked = useMph, onCheckedChange = onUseMphChange)
            }
            if (useOemSpeedBias) {
                Text("OEM Sapma Oranı: +${(oemBiasRatio * 100).roundToInt()}% + 1.5 km/h", color = Color.LightGray, fontSize = 12.sp)
                Slider(value = oemBiasRatio, onValueChange = onOemBiasRatioChange, valueRange = 0f..0.10f)
            }
        }

        // 4. Devir (RPM) Kadran Ölçekleme Modu (Mod A vs Mod B)
        item {
            Text(
                text = LocalizationManager.getString(StringKey.SETTINGS_RPM_SCALING_MODE, language),
                color = Color(0xFFFF6600),
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipButton(
                    label = LocalizationManager.getString(StringKey.SETTINGS_RPM_MODE_PROPORTIONAL, language),
                    selected = rpmScalingMode == RpmScalingMode.MODE_A_PROPORTIONAL_REDLINE,
                    onClick = { onRpmScalingModeChange(RpmScalingMode.MODE_A_PROPORTIONAL_REDLINE) }
                )
                FilterChipButton(
                    label = LocalizationManager.getString(StringKey.SETTINGS_RPM_MODE_NATIVE_1_TO_1, language),
                    selected = rpmScalingMode == RpmScalingMode.MODE_B_NATIVE_1_TO_1,
                    onClick = { onRpmScalingModeChange(RpmScalingMode.MODE_B_NATIVE_1_TO_1) }
                )
            }
        }

        // 5. Shift-Light Reaksiyon Süresi ve Sensör Kalibrasyonu
        item {
            Text(
                text = "${LocalizationManager.getString(StringKey.SETTINGS_REACTION_TIME_MS, language)}: ${reactionTimeMs.roundToInt()} ms",
                color = Color.White
            )
            Slider(value = reactionTimeMs, onValueChange = onReactionTimeMsChange, valueRange = 0f..350f)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onZeroCalibrateNow,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
                ) {
                    Text(LocalizationManager.getString(StringKey.SETTINGS_ZERO_LEAN_CALIBRATE_NOW, language), color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Text(LocalizationManager.getString(StringKey.SETTINGS_ACOUSTIC_MIC_RPM, language), color = Color.White, fontSize = 12.sp)
                Switch(checked = acousticMicEnabled, onCheckedChange = onAcousticMicChange)
            }
        }

        // 6. Gündüz / Gece Teması ve GoPro Yeşil Ekran (Chroma Key)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChipButton(
                    label = LocalizationManager.getString(StringKey.SETTINGS_THEME_AUTO_SENSOR, language),
                    selected = themeOverrideMode == 0,
                    onClick = { onThemeOverrideChange(0) }
                )
                FilterChipButton(
                    label = LocalizationManager.getString(StringKey.SETTINGS_THEME_ALWAYS_DAY, language),
                    selected = themeOverrideMode == 1,
                    onClick = { onThemeOverrideChange(1) }
                )
                FilterChipButton(
                    label = LocalizationManager.getString(StringKey.SETTINGS_THEME_ALWAYS_NIGHT, language),
                    selected = themeOverrideMode == 2,
                    onClick = { onThemeOverrideChange(2) }
                )
                FilterChipButton(
                    label = "🎬 Chroma Key",
                    selected = chromaKeyGreen,
                    onClick = { onChromaKeyChange(!chromaKeyGreen) }
                )
            }
        }
    }
}

@Composable
private fun TripDragAndWheelieRecordsTab(
    language: AppLanguage,
    tripAKm: Double,
    tripBKm: Double,
    odometerKm: Double,
    peakSpeedKmh: Double,
    peakRpm: Double,
    peakLeanLeftDeg: Double,
    peakLeanRightDeg: Double,
    peakWheelieDeg: Double,
    longestWheelieSec: Double,
    longestWheelieMeters: Double,
    onResetTripAndPeaks: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TelemetryStatCard("TRIP A", String.format(Locale.US, "%.2f km", tripAKm), Color(0xFF00E5FF), Modifier.weight(1f))
            TelemetryStatCard("TRIP B", String.format(Locale.US, "%.2f km", tripBKm), Color(0xFF00E5FF), Modifier.weight(1f))
            TelemetryStatCard("ODOMETER", String.format(Locale.US, "%.1f km", odometerKm), Color.White, Modifier.weight(1f))
        }

        Text(
            text = "🏆 " + LocalizationManager.getString(StringKey.PEAK_VALUES_HEADER, language),
            color = Color(0xFFFF6600),
            fontWeight = FontWeight.Bold
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TelemetryStatCard(
                LocalizationManager.getString(StringKey.PEAK_MAX_SPEED, language),
                "${peakSpeedKmh.roundToInt()} km/h",
                Color(0xFFFF6600),
                Modifier.weight(1f)
            )
            TelemetryStatCard(
                LocalizationManager.getString(StringKey.PEAK_MAX_RPM, language),
                "${peakRpm.roundToInt()} RPM",
                Color(0xFFFF1744),
                Modifier.weight(1f)
            )
            TelemetryStatCard(
                "MAX LEAN (L / R)",
                "${peakLeanLeftDeg.roundToInt()}° L  /  ${peakLeanRightDeg.roundToInt()}° R",
                Color(0xFF00E5FF),
                Modifier.weight(1f)
            )
        }

        // Teker Derece Ölçer & Teker Metre Rekorları
        Text(
            text = "🔥 TEKER (WHEELIE) REKORLARI — DERECE, SÜRE (SN) & MESAFE (M)",
            color = Color(0xFF00E676),
            fontWeight = FontWeight.Bold
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TelemetryStatCard(
                LocalizationManager.getString(StringKey.PEAK_MAX_WHEELIE_ANGLE, language),
                "${peakWheelieDeg.roundToInt()}°",
                Color(0xFF00E676),
                Modifier.weight(1f)
            )
            TelemetryStatCard(
                LocalizationManager.getString(StringKey.PEAK_LONGEST_WHEELIE_TIME, language),
                String.format(Locale.US, "%.2f sn", longestWheelieSec),
                Color(0xFF00E676),
                Modifier.weight(1f)
            )
            TelemetryStatCard(
                LocalizationManager.getString(StringKey.PEAK_LONGEST_WHEELIE_DIST, language),
                String.format(Locale.US, "%.1f m", longestWheelieMeters),
                Color(0xFF00E676),
                Modifier.weight(1f)
            )
        }

        Button(
            onClick = onResetTripAndPeaks,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
        ) {
            Text(LocalizationManager.getString(StringKey.ACTION_RESET, language), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TelemetryStatCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF151922),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, Color(0xFF262D3D), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = Color.LightGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Color(0xFFFF6600) else Color(0xFF1A1F2B),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

