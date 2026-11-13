package com.oemcluster.pro

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.oemcluster.pro.catalog.DeclarativeGaugeSpec
import com.oemcluster.pro.catalog.OemGaugeCatalog
import com.oemcluster.pro.data.AppPreferencesRepository
import com.oemcluster.pro.data.BikeDatabaseRepository
import com.oemcluster.pro.data.ClusterPreferences
import com.oemcluster.pro.domain.BikeMechanicalProfile
import com.oemcluster.pro.domain.IgnitionBootPhase
import com.oemcluster.pro.domain.ShiftLightState
import com.oemcluster.pro.domain.TransmissionEngagementState
import com.oemcluster.pro.domain.UnifiedTelemetryState
import com.oemcluster.pro.i18n.AppLanguage
import com.oemcluster.pro.i18n.LocalizationManager
import com.oemcluster.pro.renderer.OemClusterCanvasRenderer
import com.oemcluster.pro.runtime.DistanceFormatter
import com.oemcluster.pro.runtime.RideSessionState
import com.oemcluster.pro.runtime.RideSessionStateHolder
import com.oemcluster.pro.runtime.ShiftBeeper
import com.oemcluster.pro.runtime.SimulationController
import com.oemcluster.pro.sensors.AndroidHardwareSensorManager
import com.oemcluster.pro.sensors.NativeTelemetryEngineBridge
import com.oemcluster.pro.ui.ControlCenterScreen
import com.oemcluster.pro.ui.LaunchSequenceOverlay
import kotlinx.coroutines.delay
import java.time.LocalTime
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

        AppPreferencesRepository.init(this)
        BikeDatabaseRepository.init(this)

        val startupPrefs = AppPreferencesRepository.state.value
        LocalizationManager.setLanguage(AppLanguage.fromCode(startupPrefs.languageCode))

        // Motosiklet sürüşünde ekran kapanmasın (ayarlardan kapatılabilir); panel 120 Hz'e kilitlenir.
        if (startupPrefs.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (startupPrefs.forceMaxBrightness) {
            window.attributes = window.attributes.apply { screenBrightness = 1.0f }
        }
        request120HzRefreshRate()
        hideSystemUi()

        hardwareSensorManager = AndroidHardwareSensorManager(this, lifecycleScope)
        hardwareSensorManager.setThemeOverrideMode(startupPrefs.themeOverrideMode)
        hardwareSensorManager.updateGpsFilterConfig(
            stationaryDeadbandKmh = startupPrefs.stationaryDeadbandKmh.toDouble(),
            maxAccuracyMeters = startupPrefs.maxAllowedGpsErrorMeters
        )
        ShiftBeeper.setVolume(startupPrefs.shiftBeepVolume)
        requestedOrientation = orientationForMode(startupPrefs.screenOrientationMode)

        val initialBike = BikeDatabaseRepository.activeBikeProfile.value
        NativeTelemetryEngineBridge.applyActiveBikeProfileToNative(initialBike)
        NativeTelemetryEngineBridge.nativeConfigureCalibrationAndShiftLight(
            useOemSpeedBiasMode = startupPrefs.useOemSpeedBias,
            oemSpeedBiasRatio = startupPrefs.oemBiasRatio.toDouble(),
            enableJapan299Lock = startupPrefs.japan299Lock,
            autoOptimalShiftMode = startupPrefs.autoOptimalShift,
            manualStage1Rpm = startupPrefs.manualStage1Rpm.toDouble(),
            manualStage2Rpm = startupPrefs.manualStage2Rpm.toDouble(),
            humanReactionTimeSec = (startupPrefs.reactionTimeMs / 1000f).toDouble()
        )
        if (startupPrefs.ignitionSweepEnabled) {
            NativeTelemetryEngineBridge.nativeTriggerIgnitionSweep(System.nanoTime())
        }

        requestSensorPermissionsIfNeeded()
        hardwareSensorManager.startImuAndLightSensors()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFF6A00),
                    secondary = Color(0xFF00E5FF),
                    background = Color(0xFF000000),
                    surface = Color(0xFF10141C)
                )
            ) {
                Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
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
                        },
                        onUpdateGpsFilter = { deadband, maxErr ->
                            hardwareSensorManager.updateGpsFilterConfig(
                                stationaryDeadbandKmh = deadband.toDouble(),
                                maxAccuracyMeters = maxErr
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        ShiftBeeper.release()
        if (::hardwareSensorManager.isInitialized) hardwareSensorManager.stopAllSensors()
        super.onDestroy()
    }

    override fun onPause() {
        RideSessionStateHolder.current?.pushToPrefs()
        AppPreferencesRepository.flush()
        super.onPause()
    }

    override fun onStop() {
        RideSessionStateHolder.current?.pushToPrefs()
        AppPreferencesRepository.flush()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
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

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun request120HzRefreshRate() {
        val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display ?: @Suppress("DEPRECATION") windowManager.defaultDisplay
        } else {
            @Suppress("DEPRECATION") windowManager.defaultDisplay
        } ?: return
        val bestMode = currentDisplay.supportedModes.maxByOrNull { it.refreshRate }
        if (bestMode != null) {
            val params = window.attributes
            params.preferredDisplayModeId = bestMode.modeId
            window.attributes = params
        }
    }
}

private fun orientationForMode(mode: Int): Int = when (mode) {
    // 0 = Otomatik (sensör), 1 = Sabit yatay, 2 = Sabit dikey.
    // Önceki eşleme "Yatay" seçeneğini sensör-yatay yapıyordu; cihaz ters
    // tutulduğunda kadran 180° dönüyordu. Artık etiketle birebir.
    1 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    2 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    else -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
}

@Composable
fun MotoCrossClusterApp(
    onZeroCalibrateNow: () -> Unit,
    onToggleAcousticMic: (Boolean) -> Unit,
    onThemeModeChanged: (Int) -> Unit,
    onUpdateGpsFilter: (Float, Float) -> Unit
) {
    val prefs by AppPreferencesRepository.state.collectAsState()
    val activeBike by BikeDatabaseRepository.activeBikeProfile.collectAsState()
    val garageBikes by BikeDatabaseRepository.userGarageProfiles.collectAsState()
    val language = remember(prefs.languageCode) { AppLanguage.fromCode(prefs.languageCode) }

    val session = remember {
        RideSessionState().also { it.loadFrom(AppPreferencesRepository.state.value) }
    }
    LaunchedEffect(session) { RideSessionStateHolder.current = session }
    val simulation = remember { SimulationController() }

    var controlCenterOpen by rememberSaveable { mutableStateOf(false) }
    var launchVisible by rememberSaveable { mutableStateOf(prefs.launchAnimationEnabled) }
    var lastControlCenterTab by rememberSaveable { mutableIntStateOf(1) }
    var hintVisible by remember { mutableStateOf(!prefs.onboardingSeen) }
    var gaugeToast by remember { mutableStateOf<String?>(null) }
    var gaugeToastNonce by remember { mutableIntStateOf(0) }
    var rideRevision by remember { mutableIntStateOf(0) }

    val gaugeIndex = prefs.selectedGaugeIndex.coerceIn(0, OemGaugeCatalog.allModels.lastIndex)
    val spec = OemGaugeCatalog.allModels[gaugeIndex]
    val subModeIndex = prefs.selectedSubModeIndex.coerceIn(
        0,
        max(0, spec.availableSubModes.size - 1)
    )

    fun showToast(text: String) {
        gaugeToast = text
        gaugeToastNonce++
    }

    fun selectGauge(index: Int) {
        val count = OemGaugeCatalog.allModels.size
        val next = ((index % count) + count) % count
        // Gösterge değişimi kalıcı ayar yazdırır; sayaçların en güncel değerleri
        // bu yazma ile birlikte gitsin (kadran değişse bile km sayacı korunur).
        session.pushToPrefs()
        AppPreferencesRepository.update {
            it.copy(selectedGaugeIndex = next, selectedSubModeIndex = 0)
        }
        if (AppPreferencesRepository.state.value.ignitionSweepEnabled) {
            NativeTelemetryEngineBridge.nativeTriggerIgnitionSweep(System.nanoTime())
        }
        val nextSpec = OemGaugeCatalog.allModels[next]
        showToast("#%02d  %s %s".format(nextSpec.modelNumber, nextSpec.brand, nextSpec.modelName))
    }

    fun cycleSubMode() {
        val modes = spec.availableSubModes
        if (modes.size <= 1) return
        val next = (subModeIndex + 1) % modes.size
        AppPreferencesRepository.update { it.copy(selectedSubModeIndex = next) }
        showToast("${spec.brand} ${spec.modelName}  ·  ${modes[next]}")
    }

    fun openControlCenter() {
        controlCenterOpen = true
        if (hintVisible) {
            hintVisible = false
            AppPreferencesRepository.update { it.copy(onboardingSeen = true) }
        }
    }

    // Dil, tema, mikrofon ve ekran davranışı ayarlarını çalışma zamanına uygula.
    val context = LocalContext.current
    LaunchedEffect(prefs.languageCode) {
        LocalizationManager.setLanguage(AppLanguage.fromCode(prefs.languageCode))
    }
    LaunchedEffect(prefs.keepScreenOn) {
        val w = (context as? ComponentActivity)?.window ?: return@LaunchedEffect
        if (prefs.keepScreenOn) w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    LaunchedEffect(prefs.forceMaxBrightness) {
        val w = (context as? ComponentActivity)?.window ?: return@LaunchedEffect
        w.attributes = w.attributes.apply {
            screenBrightness = if (prefs.forceMaxBrightness) 1.0f else -1f
        }
    }
    LaunchedEffect(prefs.themeOverrideMode) {
        onThemeModeChanged(prefs.themeOverrideMode)
    }
    LaunchedEffect(prefs.acousticMicEnabled) {
        onToggleAcousticMic(prefs.acousticMicEnabled)
    }
    LaunchedEffect(prefs.stationaryDeadbandKmh, prefs.maxAllowedGpsErrorMeters) {
        onUpdateGpsFilter(prefs.stationaryDeadbandKmh, prefs.maxAllowedGpsErrorMeters)
    }
    LaunchedEffect(prefs.shiftBeepVolume) {
        ShiftBeeper.setVolume(prefs.shiftBeepVolume)
    }
    LaunchedEffect(prefs.screenOrientationMode) {
        (context as? ComponentActivity)?.requestedOrientation =
            orientationForMode(prefs.screenOrientationMode)
    }

    // Aktif motor ve shift-light eşikleri değiştiğinde C++ motorunu yeniden yapılandır.
    LaunchedEffect(
        activeBike,
        prefs.useOemSpeedBias,
        prefs.oemBiasRatio,
        prefs.japan299Lock,
        prefs.autoOptimalShift,
        prefs.manualStage1Rpm,
        prefs.manualStage2Rpm,
        prefs.reactionTimeMs
    ) {
        NativeTelemetryEngineBridge.applyActiveBikeProfileToNative(activeBike)
        NativeTelemetryEngineBridge.nativeConfigureCalibrationAndShiftLight(
            useOemSpeedBiasMode = prefs.useOemSpeedBias,
            oemSpeedBiasRatio = prefs.oemBiasRatio.toDouble(),
            enableJapan299Lock = prefs.japan299Lock,
            autoOptimalShiftMode = prefs.autoOptimalShift,
            manualStage1Rpm = prefs.manualStage1Rpm.toDouble(),
            manualStage2Rpm = prefs.manualStage2Rpm.toDouble(),
            humanReactionTimeSec = (prefs.reactionTimeMs / 1000f).toDouble()
        )
    }

    // Tek çerçeve döngüsü: gerçek sensör hattı, test sürüşü, seans sayaçları ve kayıt.
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        var lastTelemetryNanos = 0L
        var saveAccum = 0.0
        var revisionAccum = 0.0
        var settingsSaveAccum = 0.0
        var lastShiftState = ShiftLightState.NORMAL

        while (true) {
            withFrameNanos { frameNanos ->
                val frameDt = if (lastFrameNanos == 0L) 1.0 / 120.0
                else ((frameNanos - lastFrameNanos) * 1e-9).coerceIn(0.0, 0.25)
                lastFrameNanos = frameNanos

                // Kontrol merkezi açıkken kadran çizilmez; sensör hattı 10 Hz'de
                // canlı önizleme ve GNSS durumu için çalışmaya devam eder.
                val publishNow = !controlCenterOpen ||
                    lastTelemetryNanos == 0L ||
                    (frameNanos - lastTelemetryNanos) >= 100_000_000L

                if (publishNow) {
                    val tickDt = if (lastTelemetryNanos == 0L) frameDt
                    else ((frameNanos - lastTelemetryNanos) * 1e-9).coerceIn(0.001, 0.25)
                    lastTelemetryNanos = frameNanos

                    val profile = BikeDatabaseRepository.activeBikeProfile.value
                    if (simulation.active) {
                        publishSimulationTelemetry(
                            profile = profile,
                            sim = simulation,
                            prefs = AppPreferencesRepository.state.value,
                            timestampNanos = frameNanos,
                            dtSec = tickDt
                        )
                    } else {
                        NativeTelemetryEngineBridge.tick120HzFrame(frameNanos)
                    }

                    session.update(tickDt, NativeTelemetryEngineBridge.telemetryState.value)

                    // Shift Stage 2 / kesici için kask interkomu bip'i (ayarlardan)
                    val shiftStateNow = NativeTelemetryEngineBridge.telemetryState.value.shiftLightState
                    if (shiftStateNow != lastShiftState) {
                        if (AppPreferencesRepository.state.value.shiftBeepEnabled) {
                            ShiftBeeper.onShiftState(shiftStateNow, lastShiftState)
                        }
                        lastShiftState = shiftStateNow
                    }
                }

                saveAccum += frameDt
                if (saveAccum >= 1.0) {
                    saveAccum = 0.0
                    session.pushToPrefs()
                    AppPreferencesRepository.flush()
                }
                if (session.consumeBestChanged()) {
                    session.pushToPrefs()
                    AppPreferencesRepository.flush()
                }

                // Kalıcı ayarları (gösterge, tema, dil, favoriler) periyodik olarak
                // diske yaz. Böylece uygulama sonlandırılsa bile seçim ve km sayacı korunur.
                settingsSaveAccum += frameDt
                if (settingsSaveAccum >= 3.0) {
                    settingsSaveAccum = 0.0
                    AppPreferencesRepository.flush()
                }

                revisionAccum += frameDt
                if (revisionAccum >= 0.5) {
                    revisionAccum = 0.0
                    rideRevision++
                }
            }
        }
    }

    // İlk açılış ipucu kendiliğinden kaybolmaz; kullanıcı okuduğunda kapanır.
    LaunchedEffect(gaugeToastNonce) {
        if (gaugeToastNonce > 0) {
            delay(1800)
            gaugeToast = null
        }
    }

    // Kontrol merkezi açıkken canlı telemetriyi 4 Hz'e seyrelt.
    val controlCenterTelemetry by produceState(
        initialValue = NativeTelemetryEngineBridge.telemetryState.value,
        key1 = controlCenterOpen
    ) {
        while (true) {
            value = NativeTelemetryEngineBridge.telemetryState.value
            delay(if (controlCenterOpen) 250 else 1000)
        }
    }

    BackHandler(enabled = controlCenterOpen && !launchVisible) {
        controlCenterOpen = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (controlCenterOpen) {
            ControlCenterScreen(
                prefs = prefs,
                onPrefsChange = { transform ->
                    // Ayar yazmadan önce canlı sayaçları da gönder; böylece
                    // odometer/trip değerleri hiçbir ayarlama sırasında kaybolmaz.
                    session.pushToPrefs()
                    AppPreferencesRepository.update(transform)
                },
                activeBike = activeBike,
                garageBikes = garageBikes,
                liveTelemetry = controlCenterTelemetry,
                language = language,
                session = session,
                rideRevision = rideRevision,
                simulation = simulation,
                initialTab = lastControlCenterTab,
                onTabChange = { lastControlCenterTab = it },
                onClose = { controlCenterOpen = false },
                onSelectGauge = { index -> selectGauge(index) },
                onSelectBike = { id -> BikeDatabaseRepository.selectActiveBikeProfile(id) },
                onSaveBike = { bike ->
                    BikeDatabaseRepository.upsertBikeProfile(bike, setAsActive = true)
                },
                onDeleteBike = { id -> BikeDatabaseRepository.deleteBikeProfile(id) },
                onRestoreBike = { id -> BikeDatabaseRepository.restoreFactoryPreset(id) },
                onResetGarage = { BikeDatabaseRepository.resetGarageToFactory() },
                onZeroCalibrateNow = onZeroCalibrateNow,
                onReplayLaunch = { launchVisible = true },
                onResetOdometer = { session.resetOdometer() }
            )
        } else {
            LiveClusterHost(
                spec = spec,
                subModeIndex = subModeIndex,
                prefs = prefs,
                activeBike = activeBike,
                session = session,
                language = language,
                onSwipeLeft = { selectGauge(gaugeIndex + 1) },
                onSwipeRight = { selectGauge(gaugeIndex - 1) },
                onDoubleTap = { cycleSubMode() },
                onOpenControlCenter = { openControlCenter() }
            )

            ClusterChrome(
                hintVisible = hintVisible,
                gaugeToast = gaugeToast,
                revision = rideRevision,
                language = language,
                onOpenControlCenter = { openControlCenter() },
                onDismissHint = {
                    hintVisible = false
                    AppPreferencesRepository.update { it.copy(onboardingSeen = true) }
                }
            )
        }

        if (launchVisible) {
            LaunchSequenceOverlay(
                versionLabel = "v1.0.5-PRO  //  USTLABS",
                onFinished = { launchVisible = false }
            )
        }
    }
}

/**
 * Kadranın kendisi. Telemetri akışını yalnızca burada toplar; böylece kontrol
 * merkezi açıkken 120 Hz yeniden çizim yükü oluşmaz.
 */
@Composable
private fun LiveClusterHost(
    spec: DeclarativeGaugeSpec,
    subModeIndex: Int,
    prefs: ClusterPreferences,
    activeBike: BikeMechanicalProfile,
    session: RideSessionState,
    language: AppLanguage,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onDoubleTap: () -> Unit,
    onOpenControlCenter: () -> Unit
) {
    val telemetry by NativeTelemetryEngineBridge.telemetryState.collectAsState()
    val currentSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentSwipeRight by rememberUpdatedState(onSwipeRight)
    val currentDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOpenCenter by rememberUpdatedState(onOpenControlCenter)

    val pixelShiftOffset = if (prefs.oledPixelShiftEnabled) {
        val phase = (telemetry.timestampNanos / 1_000_000_000L) % 60L
        Offset(
            x = sin(phase * 0.1047).toFloat() * 3.0f,
            y = cos(phase * 0.1047).toFloat() * 2.0f
        )
    } else {
        Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { currentOpenCenter() },
                    onDoubleTap = { currentDoubleTap() }
                )
            }
            .pointerInput(Unit) {
                var dragAccum = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (dragAccum < -70f) currentSwipeLeft()
                        else if (dragAccum > 70f) currentSwipeRight()
                    }
                )
            }
    ) {
        OemClusterCanvasRenderer(
            spec = spec,
            subModeIndex = subModeIndex,
            telemetry = telemetry.copy(fuelLevelRatio = prefs.fuelLevelRatio),
            rpmScalingMode = prefs.rpmScalingMode,
            wheelieDisplayMode = prefs.wheelieDisplayMode,
            useMph = prefs.useMph,
            useFahrenheit = prefs.useFahrenheit,
            clock24h = prefs.clock24h,
            fuelUnitCode = prefs.fuelUnitCode,
            chromaKeyGreenScreen = prefs.chromaKeyGreen,
            oledPixelShiftOffset = pixelShiftOffset,
            language = language,
            tripKm = session.tripAKm,
            odometerKm = session.odometerKm,
            sessionKm = session.sessionKm,
            tripBKm = session.tripBKm,
            showStatusRail = prefs.showStatusRail,
            activeBike = activeBike,
            best0to60Sec = session.best0to60Sec,
            best0to100Sec = session.best0to100Sec,
            peakSpeedKmh = session.peakSpeedKmh,
            peakLeanLeftDeg = session.peakLeanLeftDeg,
            peakLeanRightDeg = session.peakLeanRightDeg,
            avgSpeedKmh = session.avgSpeedKmh,
            rideTimeSeconds = session.rideTimeSeconds
        )
    }
}

@Composable
private fun ClusterChrome(
    hintVisible: Boolean,
    gaugeToast: String?,
    revision: Int,
    language: com.oemcluster.pro.i18n.AppLanguage,
    onOpenControlCenter: () -> Unit,
    onDismissHint: () -> Unit
) {
    // revision, sayaçların bu katmana canlı yansımasını sağlar (0,5 sn'de bir artar).
    @Suppress("UNUSED_EXPRESSION") revision
    Box(modifier = Modifier.fillMaxSize()) {
        // Sağ kenar sekmesi: panelin üstünde duran tek fiziksel kontrol.
        Surface(
            color = Color(0xD90A0D12),
            shape = RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0x66FF6A00)
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(34.dp)
                .height(108.dp)
                .alpha(if (hintVisible) 1f else 0.58f)
                .clickable { onOpenControlCenter() }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "☰",
                    color = Color(0xFFFF6A00),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${OemGaugeCatalog.allModels.size}",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(Color(0xFFFF6A00).copy(alpha = 0.45f))
                )
            }
        }

        // Kaydırma / alt mod geri bildirimi.
        if (gaugeToast != null) {
            Surface(
                color = Color(0xD90B0E14),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0x33FFFFFF)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = gaugeToast.orEmpty(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                )
            }
        }

        if (hintVisible) {
            Surface(
                color = Color(0xF211151C),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0x99FF6A00)
                ),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp, bottom = 22.dp)
                    .widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${OemGaugeCatalog.allModels.size} OEM GÖSTERGE",
                            color = Color(0xFFFF6A00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "✕",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onDismissHint() }
                        )
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = com.oemcluster.pro.i18n.LocalizationManager.getString(
                            com.oemcluster.pro.i18n.StringKey.ONBOARDING_HINT,
                            language
                        ),
                        color = Color(0xFFB8C2D0),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(9.dp))
                    Button(
                        onClick = onDismissHint,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        )
                    ) {
                        Text(
                            text = com.oemcluster.pro.i18n.LocalizationManager.getString(
                                com.oemcluster.pro.i18n.StringKey.ACTION_GOT_IT,
                                language
                            ),
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simülatör aynı telemetri sözleşmesini besler: C++ fizik motoru yerine
 * kullanıcı girdisi kullanılır, fakat gösterge ve shift-light yolu değişmez.
 */
private fun publishSimulationTelemetry(
    profile: BikeMechanicalProfile,
    sim: SimulationController,
    prefs: ClusterPreferences,
    timestampNanos: Long,
    dtSec: Double
) {
    val speedKmh = sim.speedKmh.toDouble()
    val trueSpdMs = speedKmh / 3.6
    val gear = sim.gear.coerceIn(0, 8)
    val clutch = sim.clutchIn
    val isNeutral = gear == 0
    val isMoving = speedKmh > 1.0
    val isWheelieNow = sim.wheelieDeg >= 5.0f && speedKmh > 6.0

    if (isWheelieNow) {
        sim.currentWheelieSec += dtSec.toFloat()
        sim.currentWheelieMeters += (trueSpdMs * dtSec).toFloat()
        sim.wheelieHoldSec = 3.2f
        sim.maxWheelieDeg = max(sim.maxWheelieDeg, sim.wheelieDeg.toDouble())
        sim.longestWheelieSec = max(sim.longestWheelieSec, sim.currentWheelieSec.toDouble())
        sim.longestWheelieMeters = max(sim.longestWheelieMeters, sim.currentWheelieMeters.toDouble())
    } else if (sim.wheelieHoldSec > 0f) {
        sim.wheelieHoldSec = (sim.wheelieHoldSec - dtSec.toFloat()).coerceAtLeast(0f)
    } else {
        sim.currentWheelieSec = 0f
        sim.currentWheelieMeters = 0f
    }

    val safeDt = dtSec.coerceIn(0.001, 0.25)
    val rUnloaded = profile.rearTire.unloadedRadiusMm
    val rLean = rUnloaded * profile.rearTire.staticSquishFactor -
        profile.rearTire.crownRadiusMm * (1.0 - cos(Math.toRadians(abs(sim.leanDeg.toDouble()))))
    val rEffMm = rLean * (1.0 + 1.8e-6 * trueSpdMs * trueSpdMs)

    val wheelRpm = (trueSpdMs * 60.0) / (2.0 * PI * (rEffMm / 1000.0))
    val gearRatio = profile.gearRatios.getOrElse(
        (gear - 1).coerceIn(0, profile.gearRatios.size - 1)
    ) { 1.0 }
    val engaged = !clutch && !isNeutral && isMoving
    val calcRpm = if (!engaged) {
        profile.idleRpm
    } else {
        (wheelRpm * profile.primaryReductionRatio * profile.finalDriveRatio * gearRatio)
            .coerceIn(profile.idleRpm, profile.revLimiterRpm)
    }

    if (sim.lastRpm <= 0.0) {
        sim.lastRpm = calcRpm
        sim.peakHoldRpm = calcRpm
        sim.peakHoldTimerSec = 1.8
    }
    val instRpmRate = (calcRpm - sim.lastRpm) / safeDt
    sim.lastRpm = calcRpm
    sim.filteredRpmRatePerSec = 0.82 * sim.filteredRpmRatePerSec + 0.18 * instRpmRate

    val baseOptStage2 = if (prefs.autoOptimalShift) {
        profile.computeOptimalShiftRpmForGear(if (gear <= 0) 1 else gear)
    } else {
        prefs.manualStage2Rpm.toDouble()
    }
    // Reaksiyon telafisi: VirtualEcuPhysicsEngine ile aynı sınır (en fazla 1400 RPM).
    // Sınırsız bırakılırsa ani devir sıçramalarında (ateşleme selamlaması, vites atlama)
    // shift-light erken yanıp gerçek sürüşteki eşikten sapıyordu.
    val reactionLead = min(
        1400.0,
        max(0.0, sim.filteredRpmRatePerSec) *
            (prefs.reactionTimeMs.toDouble().coerceIn(0.0, 350.0) / 1000.0)
    )
    val optStage2: Double
    val optStage1: Double
    if (prefs.autoOptimalShift) {
        optStage2 = max(profile.idleRpm + 2000.0, baseOptStage2 - reactionLead)
            .coerceAtMost(profile.redlineRpm)
        optStage1 = max(profile.idleRpm + 1200.0, optStage2 - 1000.0)
    } else {
        optStage2 = prefs.manualStage2Rpm.toDouble()
            .coerceIn(profile.idleRpm + 800.0, profile.redlineRpm)
        optStage1 = prefs.manualStage1Rpm.toDouble()
            .coerceIn(profile.idleRpm + 400.0, optStage2 - 150.0)
    }

    val timeSec = timestampNanos * 1e-9

    // İbre yay-damper fiziği: VirtualEcuPhysicsEngine ile aynı katsayılar
    // (ωn = 42 rad/s, ζ = 0.88, 2 ms alt adım). Simülasyonda da kadran ibresi
    // gerçek sürüşteki gibi yumuşak tepki verir; hız sürgüsü anında zıplatmaz.
    val needleTarget = if (calcRpm >= profile.revLimiterRpm - 80.0) {
        profile.revLimiterRpm + 150.0 * sin(timeSec * 2.0 * PI * 14.0)
    } else {
        calcRpm
    }
    if (sim.displayedRpm <= 0.0) {
        sim.displayedRpm = needleTarget
        sim.rpmVelocity = 0.0
    }
    val needleSteps = max(1, (safeDt / 0.002).toInt())
    val needleH = safeDt / needleSteps
    repeat(needleSteps) {
        val rpmError = needleTarget - sim.displayedRpm
        val rpmAccel = (42.0 * 42.0 * rpmError) - (2.0 * 0.88 * 42.0 * sim.rpmVelocity)
        sim.rpmVelocity += rpmAccel * needleH
        sim.displayedRpm += sim.rpmVelocity * needleH
    }
    val displayedEngineRpm = max(0.0, sim.displayedRpm)

    val shiftState = when {
        displayedEngineRpm >= profile.revLimiterRpm - 80.0 -> ShiftLightState.OVER_REV_LIMITER
        displayedEngineRpm >= optStage2 -> ShiftLightState.STAGE_2_SHIFT_NOW
        displayedEngineRpm >= optStage1 -> ShiftLightState.STAGE_1_APPROACHING
        else -> ShiftLightState.NORMAL
    }
    val flashOn = when (shiftState) {
        ShiftLightState.OVER_REV_LIMITER -> (timeSec * 12.0) % 1.0 < 0.5
        ShiftLightState.STAGE_2_SHIFT_NOW -> (timeSec * 8.0) % 1.0 < 0.5
        ShiftLightState.STAGE_1_APPROACHING -> true
        ShiftLightState.NORMAL -> false
    }

    if (displayedEngineRpm >= sim.peakHoldRpm) {
        sim.peakHoldRpm = displayedEngineRpm
        sim.peakHoldTimerSec = 1.8
    } else if (sim.peakHoldTimerSec > 0.0) {
        sim.peakHoldTimerSec -= safeDt
    } else {
        sim.peakHoldRpm = max(displayedEngineRpm, sim.peakHoldRpm - 2800.0 * safeDt)
    }

    val rawDisplaySpd = if (prefs.useOemSpeedBias && speedKmh > 1.0) {
        speedKmh * (1.0 + prefs.oemBiasRatio) + 1.5
    } else {
        speedKmh
    }
    val locked299 = prefs.japan299Lock && rawDisplaySpd >= 299.0
    val finalDisplaySpd = if (locked299) 299.0 else rawDisplaySpd

    val trqNm = profile.interpolateEngineTorqueNm(displayedEngineRpm)
    val driveEff = profile.finalDriveType.mechanicalEfficiency.coerceIn(0.75, 0.98)
    val wheelHp = if (engaged) ((trqNm * displayedEngineRpm * 2.0 * PI) / 60000.0) * 1.34102209 * driveEff else 0.0
    val wheelTorqueNm = if (engaged) (trqNm * profile.primaryReductionRatio * profile.finalDriveRatio * gearRatio * driveEff) else 0.0

    val autoNight = LocalTime.now().hour.let { it < 7 || it >= 19 }
    val nightFactor = when (prefs.themeOverrideMode) {
        1 -> 0f
        2 -> 1f
        else -> if (autoNight) 1f else 0f
    }

    NativeTelemetryEngineBridge.publishTelemetryState(
        UnifiedTelemetryState(
            timestampNanos = timestampNanos,
            activeBikeProfileId = profile.profileId,
            activeBikeDisplayName = "${profile.brand} ${profile.modelName}",
            bikeIdleRpm = profile.idleRpm,
            bikeRedlineRpm = profile.redlineRpm,
            bikeRevLimiterRpm = profile.revLimiterRpm,
            trueSpeedMs = trueSpdMs,
            trueSpeedKmh = speedKmh,
            displayedSpeedKmh = finalDisplaySpd,
            is299LockEngaged = locked299,
            actualEngineRpm = displayedEngineRpm,
            normalizedRpmRatio = displayedEngineRpm / max(1000.0, profile.redlineRpm),
            peakHoldRpm = sim.peakHoldRpm,
            rpmRateOfChangePerSec = sim.filteredRpmRatePerSec,
            stage1ShiftRpm = optStage1,
            stage2ShiftRpm = optStage2,
            shiftLightState = shiftState,
            shiftFlashPhaseOn = flashOn,
            fullColorInversionActive = (shiftState == ShiftLightState.STAGE_2_SHIFT_NOW ||
                shiftState == ShiftLightState.OVER_REV_LIMITER) && flashOn,
            currentGear = gear,
            isNeutral = isNeutral,
            isClutchPulled = clutch,
            transmissionState = when {
                clutch -> TransmissionEngagementState.CLUTCH_PULLED_COASTING
                isNeutral -> TransmissionEngagementState.NEUTRAL
                else -> TransmissionEngagementState.ENGAGED_IN_GEAR
            },
            leanAngleDeg = sim.leanDeg.toDouble(),
            pitchAngleDeg = sim.wheelieDeg.toDouble(),
            effectiveTireRadiusMm = rEffMm,
            estimatedWheelHorsepower = wheelHp,
            estimatedWheelTorqueNm = wheelTorqueNm,
            fuelLevelRatio = prefs.fuelLevelRatio,
            nightModeTransitionFactor = nightFactor,
            ignitionBootPhase = IgnitionBootPhase.RUNNING_LIVE,
            isWheelieActive = isWheelieNow,
            isWheelieSummaryVisible = isWheelieNow || sim.wheelieHoldSec > 0f,
            wheelieAngleDeg = sim.wheelieDeg.toDouble(),
            currentWheelieDurationSec = sim.currentWheelieSec.toDouble(),
            currentWheelieDistanceMeters = sim.currentWheelieMeters.toDouble(),
            maxWheelieAngleDeg = sim.maxWheelieDeg,
            longestWheelieDurationSec = sim.longestWheelieSec,
            longestWheelieDistanceMeters = sim.longestWheelieMeters,
            gearConfidence = if (isNeutral || clutch) 1.0 else 0.98,
            gpsSpeedAgeSec = 1.0e9,
            gpsSpeedAccuracyMs = 0.0,
            isStationary = speedKmh < 0.5,
            gpsSpeedSourceBlend = 0.0,
            gpsSatelliteCount = 0,
            gpsHorizontalAccuracyM = 99.0
        )
    )
}
