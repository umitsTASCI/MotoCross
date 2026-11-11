package com.oemcluster.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.oemcluster.pro.domain.BikeMechanicalProfile
import com.oemcluster.pro.domain.GearCalculationMode
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
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var hardwareSensorManager: AndroidHardwareSensorManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
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
                    },
                    onGpsFilterChanged = { deadbandKmh, maxAccuracyM, imuWeight ->
                        hardwareSensorManager.updateGpsFilterConfig(
                            stationaryDeadbandKmh = deadbandKmh.toDouble(),
                            maxAccuracyMeters = maxAccuracyM,
                            imuBlendingWeight = imuWeight.toDouble()
                        )
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
    onThemeModeChanged: (Int) -> Unit,
    onGpsFilterChanged: (Float, Float, Float) -> Unit
) {
    val language by LocalizationManager.currentLanguage.collectAsState()
    val activeBike by BikeDatabaseRepository.activeBikeProfile.collectAsState()
    val allGarageBikes by BikeDatabaseRepository.userGarageProfiles.collectAsState()
    val nativeTelemetry by NativeTelemetryEngineBridge.telemetryState.collectAsState()

    // Seçili Gösterge (105 OEM Model içinden) ve Alt Modu
    var selectedGaugeIndex by remember { mutableIntStateOf(0) } // #1 KTM 390 Duke
    var selectedSubModeIndex by remember { mutableIntStateOf(0) }
    val favoriteSkinIds = remember {
        mutableStateListOf(
            "ktm_390_duke_gen2",
            "bmw_s1000rr_k67",
            "yamaha_yzf_r1",
            "ducati_panigale_v4_evo",
            "suzuki_hayabusa_gen3",
            "honda_cbr1000rrr_sp",
            "kawasaki_zx10r_2021",
            "yamaha_yzf_r6_rj27"
        )
    }

    // Kontrol Merkezi Modal Durumu ve Otomatik Gizlenen Üst Kokpit Çubuğu
    var isControlCenterOpen by remember { mutableStateOf(false) }
    var activeMenuTab by remember { mutableIntStateOf(0) } // 0: Garajım, 1: 105 Galeri, 2: Ayarlar, 3: Yol Bilgisayarı & Drag
    var autoHideCockpitHud by remember { mutableStateOf(true) }
    var lastUserInteractionNanos by remember { mutableLongStateOf(System.nanoTime()) }
    var hudVisible by remember { mutableStateOf(true) }

    // Altın Kural Kilidi vs Kadranla Motoru Eşleştirme
    var syncBikeProfileWithGauge by remember { mutableStateOf(false) }

    // GPS, ECU, Vites ve Kalibrasyon Ayarları
    var useMph by remember { mutableStateOf(false) }
    var useOemSpeedBias by remember { mutableStateOf(false) }
    var oemBiasRatio by remember { mutableFloatStateOf(0.05f) }
    var japan299Lock by remember { mutableStateOf(false) }
    var stationaryDeadbandKmh by remember { mutableFloatStateOf(2.2f) }
    var maxGpsAccuracyMeters by remember { mutableFloatStateOf(22f) }
    var imuBlendingWeight by remember { mutableFloatStateOf(0.35f) }

    var gearCalculationMode by remember { mutableStateOf(GearCalculationMode.AUTO_KINEMATIC_ECU) }
    var manualGearSelection by remember { mutableIntStateOf(1) }

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

    // Canlı Sürüş & Teker Simülatörü (İsteğe Bağlı)
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

    // Aktif motor ve kalibrasyon ayarlarını C++20 NDK motoruna senkronize et
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

    // GPS filtre parametrelerini güncelle
    LaunchedEffect(stationaryDeadbandKmh, maxGpsAccuracyMeters, imuBlendingWeight) {
        onGpsFilterChanged(stationaryDeadbandKmh, maxGpsAccuracyMeters, imuBlendingWeight)
    }

    // Vites hesaplama modunu C++20 NDK motoruna gönder
    LaunchedEffect(gearCalculationMode, manualGearSelection, simClutchIn) {
        val overrideGear = when (gearCalculationMode) {
            GearCalculationMode.AUTO_KINEMATIC_ECU -> -1
            GearCalculationMode.SEMI_AUTO_WITH_BUTTONS,
            GearCalculationMode.MANUAL_GEAR_LOCK -> manualGearSelection
        }
        NativeTelemetryEngineBridge.applyManualGearOverride(
            forcedGear = overrideGear,
            clutchPulled = simClutchIn,
            throttleOverrideRpm = -1.0
        )
    }

    // 120 Hz Render ve Telemetri Döngüsü
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameNanos ->
                val dtSec = if (lastFrameNanos == 0L) (1.0 / 120.0)
                else ((frameNanos - lastFrameNanos) * 1e-9).coerceIn(0.001, 0.050)
                lastFrameNanos = frameNanos

                // Üst HUD çubuğunu 6 saniye dokunulmadığında otomatik gizle
                hudVisible = !autoHideCockpitHud ||
                    simDrawerOpen ||
                    ((frameNanos - lastUserInteractionNanos) < 6_000_000_000L)

                if (!simActive) {
                    NativeTelemetryEngineBridge.pollFrame120Hz(
                        timestampNanos = frameNanos,
                        activeProfile = activeBike,
                        wheelieDisplayMode = wheelieDisplayMode
                    )
                } else {
                    // Canlı Sürüş Simülatörü: Hızdan otomatik vites ve gerçek devir hesaplar
                    val trueSpdMs = simSpeedKmh / 3.6
                    val rUnloaded = activeBike.rearTire.unloadedRadiusMm
                    val rLean = rUnloaded * activeBike.rearTire.staticSquishFactor -
                        activeBike.rearTire.crownRadiusMm * (1.0 - cos(Math.toRadians(abs(simLeanDeg.toDouble()))))
                    val rEffMm = rLean * (1.0 + 1.8e-6 * trueSpdMs * trueSpdMs)

                    // Otomatik vites modundaysa hız arttıkça vitesi otomatik yükselt/düşür
                    if (gearCalculationMode == GearCalculationMode.AUTO_KINEMATIC_ECU && simSpeedKmh >= 2f) {
                        var bestG = 1
                        for (g in 1..activeBike.gearCount) {
                            val rpmAtG = activeBike.computeRpmAtSpeedKmh(g, simSpeedKmh.toDouble())
                            bestG = g
                            if (rpmAtG <= activeBike.redlineRpm * 0.86) break
                        }
                        simGear = bestG
                    }

                    val wheelRpm = (trueSpdMs * 60.0) / (2.0 * PI * (rEffMm / 1000.0))
                    val gearRatio = activeBike.gearRatios.getOrElse((simGear - 1).coerceIn(0, 5)) { 1.0 }
                    val calcRpm = if (simClutchIn || simSpeedKmh < 1.5f) {
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

                    val rawDisplaySpd = if (useOemSpeedBias && simSpeedKmh > 1.5f) {
                        simSpeedKmh * (1.0 + oemBiasRatio) + 1.5
                    } else simSpeedKmh.toDouble()
                    val locked299 = japan299Lock && rawDisplaySpd >= 299.0
                    val finalDisplaySpd = if (locked299) 299.0 else rawDisplaySpd

                    val trqNm = activeBike.interpolateEngineTorqueNm(bounceRpm)
                    val wheelHp = ((trqNm * bounceRpm * 2.0 * PI) / 60000.0) * 1.341 * 0.90

                    NativeTelemetryEngineBridge.updateDirectSimulatorState(
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
                            hasGpsFix = true,
                            satellitesInUse = 14,
                            satellitesVisible = 18,
                            gpsAccuracyMeters = 1.4f,
                            rawGpsSpeedKmh = simSpeedKmh.toDouble(),
                            actualEngineRpm = bounceRpm,
                            normalizedRpmRatio = bounceRpm / max(1000.0, activeBike.redlineRpm),
                            peakHoldRpm = bounceRpm,
                            stage1ShiftRpm = optStage1,
                            stage2ShiftRpm = optStage2,
                            shiftLightState = shiftState,
                            shiftFlashPhaseOn = flashOn,
                            fullColorInversionActive = (shiftState == ShiftLightState.STAGE_2_SHIFT_NOW ||
                                shiftState == ShiftLightState.OVER_REV_LIMITER) && flashOn,
                            currentGear = if (simSpeedKmh < 1.5f && !simClutchIn) 0 else simGear,
                            isNeutral = simSpeedKmh < 1.5f && !simClutchIn,
                            isClutchPulled = simClutchIn,
                            transmissionState = if (simClutchIn) TransmissionEngagementState.CLUTCH_PULLED_COASTING
                            else TransmissionEngagementState.ENGAGED_IN_GEAR,
                            leanAngleDeg = simLeanDeg.toDouble(),
                            pitchAngleDeg = simWheelieDeg.toDouble(),
                            effectiveTireRadiusMm = rEffMm,
                            estimatedWheelHorsepower = wheelHp,
                            estimatedWheelTorqueNm = trqNm * activeBike.primaryReductionRatio * activeBike.finalDriveRatio * gearRatio * 0.92,
                            nightModeBlendFactor = if (themeOverrideMode == 1) 0f else 1f,
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
            x = sin(phase * 0.1047).toFloat() * 2.5f,
            y = cos(phase * 0.1047).toFloat() * 1.8f
        )
    } else Offset.Zero

    fun selectGaugeAndSyncIfEnabled(newIdx: Int) {
        val wrapped = (newIdx + OemGaugeCatalog105.all105Models.size) % OemGaugeCatalog105.all105Models.size
        selectedGaugeIndex = wrapped
        selectedSubModeIndex = 0
        lastUserInteractionNanos = System.nanoTime()
        if (syncBikeProfileWithGauge) {
            val spec = OemGaugeCatalog105.all105Models[wrapped]
            BikeDatabaseRepository.selectActiveBikeProfile(spec.skinId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(syncBikeProfileWithGauge) {
                detectTapGestures(
                    onTap = {
                        lastUserInteractionNanos = System.nanoTime()
                        hudVisible = true
                    },
                    onLongPress = {
                        lastUserInteractionNanos = System.nanoTime()
                        isControlCenterOpen = true
                    },
                    onDoubleTap = {
                        lastUserInteractionNanos = System.nanoTime()
                        val modeCount = currentSpec.availableSubModes.size
                        if (modeCount > 1) {
                            selectedSubModeIndex = (selectedSubModeIndex + 1) % modeCount
                        }
                    }
                )
            }
            .pointerInput(syncBikeProfileWithGauge) {
                var dragAccum = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragAccum = 0f
                        lastUserInteractionNanos = System.nanoTime()
                    },
                    onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (dragAccum < -85f) {
                            selectGaugeAndSyncIfEnabled(selectedGaugeIndex + 1)
                        } else if (dragAccum > 85f) {
                            selectGaugeAndSyncIfEnabled(selectedGaugeIndex - 1)
                        }
                    }
                )
            }
    ) {
        // 1. 120 FPS Ultra-Gerçekçi OEM Gösterge Çizim Katmanı
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

        // 2. Otomatik Gizlenen Şık Yüzer Kokpit Hapı (Floating Cockpit Pill)
        AnimatedVisibility(
            visible = hudVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol Hap: Garaj / Kontrol Merkezi + Aktif Motor & GPS Durumu
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xCC0D1119),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .border(1.dp, Color(0xFFFF6600), RoundedCornerShape(20.dp))
                            .clickable {
                                lastUserInteractionNanos = System.nanoTime()
                                isControlCenterOpen = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "☰ GARAJ & AYARLAR",
                                color = Color(0xFFFF6600),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• ${activeBike.brand} ${activeBike.modelName}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Canlı GPS Durum Rozeti
                    val gpsLocked = nativeTelemetry.hasGpsFix
                    Surface(
                        color = Color(0xCC0D1119),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(
                            1.dp,
                            if (gpsLocked) Color(0xFF00E676) else Color(0xFFFFAB00),
                            RoundedCornerShape(20.dp)
                        )
                    ) {
                        Text(
                            text = if (gpsLocked) {
                                "🛰️ GPS ${max(4, nativeTelemetry.satellitesInUse)} SAT (±${nativeTelemetry.gpsAccuracyMeters.roundToInt()}m)"
                            } else {
                                "🛰️ GPS BEKLENİYOR (0.0 km/h KİLİTLİ)"
                            },
                            color = if (gpsLocked) Color(0xFF00E676) else Color(0xFFFFAB00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        )
                    }
                }

                // Sağ Hap: Hızlı Kadran Değiştirici, 0° Kalibrasyon, Kontak Sweep ve Simülatör
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickHudButton("◀") { selectGaugeAndSyncIfEnabled(selectedGaugeIndex - 1) }
                    QuickHudButton("#${currentSpec.modelNumber} ${currentSpec.brand}") {
                        activeMenuTab = 1
                        isControlCenterOpen = true
                    }
                    QuickHudButton("▶") { selectGaugeAndSyncIfEnabled(selectedGaugeIndex + 1) }
                    QuickHudButton("0°") { onZeroCalibrateNow() }
                    QuickHudButton("🔑") {
                        NativeTelemetryEngineBridge.nativeTriggerIgnitionSweep(System.nanoTime())
                    }
                    Surface(
                        color = if (simActive) Color(0xFFFF6600) else Color(0xCC0D1119),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .border(1.dp, Color(0xFF374151), RoundedCornerShape(18.dp))
                            .clickable {
                                lastUserInteractionNanos = System.nanoTime()
                                simDrawerOpen = !simDrawerOpen
                            }
                    ) {
                        Text(
                            text = if (simActive) "⚡ SİM AÇIK" else "🎛️ SİM",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 3. Yarı-Otomatik / Manuel Vites Modunda Eldiven Dostu Ekranda Vites Kulakçıkları (- / +)
        if (gearCalculationMode != GearCalculationMode.AUTO_KINEMATIC_ECU) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 34.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        manualGearSelection = (manualGearSelection - 1).coerceAtLeast(0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC1E2532))
                ) {
                    Text("▼ VİTES DÜŞÜR (-)", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Button(
                    onClick = {
                        manualGearSelection = (manualGearSelection + 1).coerceAtMost(activeBike.gearCount)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xCCFF6600))
                ) {
                    Text("▲ VİTES ARTIR (+)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // 4. Canlı Sürüş & Teker Simülatörü Çekmecesi
        if (simDrawerOpen) {
            Surface(
                color = Color(0xF00B0E15),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 54.dp, end = 16.dp)
                    .width(370.dp)
                    .border(1.5.dp, Color(0xFFFF6600), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎛️ Canlı Sürüş & Teker Simülatörü", color = Color.White, fontWeight = FontWeight.Bold)
                        Switch(checked = simActive, onCheckedChange = { simActive = it })
                    }
                    Text(
                        text = "Hız: ${simSpeedKmh.roundToInt()} km/h  •  Vites: ${if (simSpeedKmh < 1.5f) "N" else simGear}  •  Devir: ${nativeTelemetry.actualEngineRpm.roundToInt()} RPM",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
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
                        for (g in 1..activeBike.gearCount) {
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
                        text = "Teker Açısı (Wheelie): ${simWheelieDeg.roundToInt()}° (≥5° Teker Metre Başlar)",
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

        // 5. Tam Kapsamlı Kontrol Merkezi (Garajım & Motor Stüdyosu, 105 Gösterge Galerisi, Pro Ayarlar, Yol Bilgisayarı)
        if (isControlCenterOpen) {
            Surface(
                color = Color(0xFA080B10),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🏍️ MotoCross — OEM-Cluster Pro Studio",
                                color = Color(0xFFFF6600),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Aktif Motor: ${activeBike.brand} ${activeBike.modelName} (${activeBike.redlineRpm.roundToInt()} RPM)  •  Seçili Kadran: #${currentSpec.modelNumber} ${currentSpec.brand} ${currentSpec.modelName}",
                                color = Color(0xFFB0BEC5),
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = {
                                lastUserInteractionNanos = System.nanoTime()
                                isControlCenterOpen = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                        ) {
                            Text("✕ KADRANA DÖN", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val tabs = listOf(
                        "🔧 Garajım & Motor Bilgileri (105 Profil)",
                        "🎨 105 OEM Gösterge Galerisi",
                        "⚙️ GPS, Vites & ECU Ayarları",
                        "🏁 Yol Bilgisayarı & Teker Rekorları"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEachIndexed { idx, title ->
                            Button(
                                onClick = { activeMenuTab = idx },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeMenuTab == idx) Color(0xFFFF6600) else Color(0xFF171C26)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(title, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (activeMenuTab) {
                            0 -> GarageProfileStudioTab(
                                activeBike = activeBike,
                                allBikes = allGarageBikes,
                                syncBikeWithGauge = syncBikeProfileWithGauge,
                                onSyncBikeWithGaugeChange = { syncBikeProfileWithGauge = it }
                            )
                            1 -> DashboardGallery105Tab(
                                selectedGaugeIndex = selectedGaugeIndex,
                                favoriteSkinIds = favoriteSkinIds,
                                syncBikeWithGauge = syncBikeProfileWithGauge,
                                onSyncBikeWithGaugeChange = { syncBikeProfileWithGauge = it },
                                onSelectGauge = { idx ->
                                    selectGaugeAndSyncIfEnabled(idx)
                                    NativeTelemetryEngineBridge.nativeTriggerIgnitionSweep(System.nanoTime())
                                    isControlCenterOpen = false
                                }
                            )
                            2 -> AdvancedSettingsTab(
                                language = language,
                                telemetry = nativeTelemetry,
                                useMph = useMph,
                                onUseMphChange = { useMph = it },
                                useOemSpeedBias = useOemSpeedBias,
                                onUseOemSpeedBiasChange = { useOemSpeedBias = it },
                                oemBiasRatio = oemBiasRatio,
                                onOemBiasRatioChange = { oemBiasRatio = it },
                                japan299Lock = japan299Lock,
                                onJapan299LockChange = { japan299Lock = it },
                                stationaryDeadbandKmh = stationaryDeadbandKmh,
                                onStationaryDeadbandChange = { stationaryDeadbandKmh = it },
                                maxGpsAccuracyMeters = maxGpsAccuracyMeters,
                                onMaxGpsAccuracyChange = { maxGpsAccuracyMeters = it },
                                imuBlendingWeight = imuBlendingWeight,
                                onImuBlendingWeightChange = { imuBlendingWeight = it },
                                gearCalculationMode = gearCalculationMode,
                                onGearCalculationModeChange = { gearCalculationMode = it },
                                syncBikeWithGauge = syncBikeProfileWithGauge,
                                onSyncBikeWithGaugeChange = { syncBikeProfileWithGauge = it },
                                rpmScalingMode = rpmScalingMode,
                                onRpmScalingModeChange = { rpmScalingMode = it },
                                wheelieDisplayMode = wheelieDisplayMode,
                                onWheelieDisplayModeChange = { wheelieDisplayMode = it },
                                autoOptimalShift = autoOptimalShift,
                                onAutoOptimalShiftChange = { autoOptimalShift = it },
                                manualStage1Rpm = manualStage1Rpm,
                                onManualStage1RpmChange = { manualStage1Rpm = it },
                                manualStage2Rpm = manualStage2Rpm,
                                onManualStage2RpmChange = { manualStage2Rpm = it },
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
                                autoHideCockpitHud = autoHideCockpitHud,
                                onAutoHideCockpitHudChange = { autoHideCockpitHud = it },
                                oledPixelShiftEnabled = oledPixelShiftEnabled,
                                onOledPixelShiftChange = { oledPixelShiftEnabled = it },
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
private fun QuickHudButton(label: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xCC0D1119),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .border(1.dp, Color(0xFF2E3646), RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
        )
    }
}

// =========================================================================================
// SEKME 1: PRO GARAJIM & MOTOR MEKANİK STÜDYOSU (105 FABRİKA PROFİLİ + 2 MODLU ÖZEL EDİTÖR)
// =========================================================================================
@Composable
private fun GarageProfileStudioTab(
    activeBike: BikeMechanicalProfile,
    allBikes: List<BikeMechanicalProfile>,
    syncBikeWithGauge: Boolean,
    onSyncBikeWithGaugeChange: (Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf("Tümü") }
    var editorMode by remember { mutableIntStateOf(0) } // 0: Kolay Hız-Vites Modu, 1: Mühendislik Dişli/Şanzıman Modu
    var statusBanner by remember { mutableStateOf("") }

    // Ortak Alanlar
    var customBrand by remember(activeBike) { mutableStateOf(activeBike.brand) }
    var customModel by remember(activeBike) { mutableStateOf(activeBike.modelName) }
    var customCc by remember(activeBike) { mutableStateOf(activeBike.displacementCc.toString()) }
    var customGearCount by remember(activeBike) { mutableIntStateOf(activeBike.gearCount) }
    var idleRpmText by remember(activeBike) { mutableStateOf(activeBike.idleRpm.roundToInt().toString()) }
    var redlineRpmText by remember(activeBike) { mutableStateOf(activeBike.redlineRpm.roundToInt().toString()) }
    var limiterRpmText by remember(activeBike) { mutableStateOf(activeBike.revLimiterRpm.roundToInt().toString()) }
    var peakHpText by remember(activeBike) { mutableStateOf(activeBike.peakPowerHp.roundToInt().toString()) }
    var peakTrqText by remember(activeBike) { mutableStateOf(activeBike.peakTorqueNm.roundToInt().toString()) }
    var rearTireSpecText by remember(activeBike) { mutableStateOf(activeBike.rearTire.toSpecString()) }

    // Kolay Mod: 1..6. Vites Maksimum Hızları (km/h)
    var gear1MaxKmh by remember(activeBike) {
        mutableStateOf(activeBike.computeSpeedKmhAtRpm(1, activeBike.redlineRpm).roundToInt().toString())
    }
    var gear2MaxKmh by remember(activeBike) {
        mutableStateOf(activeBike.computeSpeedKmhAtRpm(2, activeBike.redlineRpm).roundToInt().toString())
    }
    var gear3MaxKmh by remember(activeBike) {
        mutableStateOf(activeBike.computeSpeedKmhAtRpm(3, activeBike.redlineRpm).roundToInt().toString())
    }
    var gear4MaxKmh by remember(activeBike) {
        mutableStateOf(activeBike.computeSpeedKmhAtRpm(4, activeBike.redlineRpm).roundToInt().toString())
    }
    var gear5MaxKmh by remember(activeBike) {
        mutableStateOf(activeBike.computeSpeedKmhAtRpm(5, activeBike.redlineRpm).roundToInt().toString())
    }
    var gear6MaxKmh by remember(activeBike) {
        mutableStateOf(activeBike.computeSpeedKmhAtRpm(6, activeBike.redlineRpm).roundToInt().toString())
    }

    // Mühendislik Modu: Ön/Arka Dişli, Primer Oran ve 1..6. Vites Şanzıman Oranları
    var frontTeethText by remember(activeBike) { mutableStateOf(activeBike.frontSprocketTeeth.toString()) }
    var rearTeethText by remember(activeBike) { mutableStateOf(activeBike.rearSprocketTeeth.toString()) }
    var primaryRatioText by remember(activeBike) { mutableStateOf(String.format(Locale.US, "%.3f", activeBike.primaryReductionRatio)) }
    var g1RatioText by remember(activeBike) { mutableStateOf(String.format(Locale.US, "%.3f", activeBike.gearRatios.getOrElse(0) { 2.800 })) }
    var g2RatioText by remember(activeBike) { mutableStateOf(String.format(Locale.US, "%.3f", activeBike.gearRatios.getOrElse(1) { 2.000 })) }
    var g3RatioText by remember(activeBike) { mutableStateOf(String.format(Locale.US, "%.3f", activeBike.gearRatios.getOrElse(2) { 1.550 })) }
    var g4RatioText by remember(activeBike) { mutableStateOf(String.format(Locale.US, "%.3f", activeBike.gearRatios.getOrElse(3) { 1.280 })) }
    var g5RatioText by remember(activeBike) { mutableStateOf(String.format(Locale.US, "%.3f", activeBike.gearRatios.getOrElse(4) { 1.100 })) }
    var g6RatioText by remember(activeBike) { mutableStateOf(String.format(Locale.US, "%.3f", activeBike.gearRatios.getOrElse(5) { 0.960 })) }

    val brands = listOf("Tümü", "KTM", "Yamaha", "Honda", "Kawasaki", "Suzuki", "BMW", "Ducati", "Triumph", "Aprilia", "MV Agusta", "Husqvarna")
    val filteredBikes = allBikes.filter { bike ->
        val matchesBrand = selectedBrand == "Tümü" || bike.brand.equals(selectedBrand, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() ||
            bike.modelName.contains(searchQuery, ignoreCase = true) ||
            bike.brand.contains(searchQuery, ignoreCase = true)
        matchesBrand && matchesQuery
    }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        // SOL SÜTUN (%44): 105 Motosiklet Fabrika Veritabanı Seçici
        Column(modifier = Modifier.weight(0.44f).fillMaxHeight()) {
            // Altın Kural vs Otomatik Eşleştirme Anahtarı
            Surface(
                color = Color(0xFF141923),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (syncBikeWithGauge) "🔗 Kadran Değişince Motoru da Otomatik Eşleştir"
                            else "🔒 Altın Kural: Kendi Motorumu Tüm Kadranlarda Sabitle",
                            color = if (syncBikeWithGauge) Color(0xFF00E5FF) else Color(0xFF00E676),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Aktif: ${activeBike.brand} ${activeBike.modelName}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(checked = syncBikeWithGauge, onCheckedChange = onSyncBikeWithGaugeChange)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("105 Motor İçinde Ara (Örn: R6, MT-07, 390 Duke, Hayabusa)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(brands) { b ->
                    Surface(
                        color = if (selectedBrand == b) Color(0xFFFF6600) else Color(0xFF1B202C),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.clickable { selectedBrand = b }
                    ) {
                        Text(
                            text = b,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredBikes) { bike ->
                    val isSelected = bike.profileId == activeBike.profileId
                    Surface(
                        color = if (isSelected) Color(0xFF28180D) else Color(0xFF131720),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFFFF6600) else Color(0xFF252C3B),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                BikeDatabaseRepository.selectActiveBikeProfile(bike.profileId)
                                statusBanner = "✅ ${bike.brand} ${bike.modelName} mekanik motor profili yüklendi!"
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${bike.brand} ${bike.modelName}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${bike.redlineRpm.roundToInt()} RPM",
                                    color = Color(0xFFFF6600),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "${bike.displacementCc} cc  •  Dişli: ${bike.frontSprocketTeeth}/${bike.rearSprocketTeeth}T  •  Lastik: ${bike.rearTire.toSpecString()}  •  6.Vites: ${bike.computeSpeedKmhAtRpm(bike.gearCount, bike.redlineRpm).roundToInt()} km/h",
                                color = Color(0xFF90A4AE),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // SAĞ SÜTUN (%56): 2-Modlu Profesyonel Motor & Şanzıman Kalibrasyon Stüdyosu + Canlı Vites-Hız Tablosu
        Surface(
            color = Color(0xFF12161F),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(0.56f).fillMaxHeight()
        ) {
            LazyColumn(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "🛠️ Motor Bilgileri, Vites-Hız & Dişli Stüdyosu: ${activeBike.brand} ${activeBike.modelName}",
                        color = Color(0xFFFF6600),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (statusBanner.isNotEmpty()) {
                        Text(
                            text = statusBanner,
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Mod Seçici: Kolay Hız Modu vs Mühendislik Dişli Modu
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { editorMode = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (editorMode == 0) Color(0xFFFF6600) else Color(0xFF1D2432)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚡ KOLAY MOD: Vites Hızlarını Gir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { editorMode = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (editorMode == 1) Color(0xFFFF6600) else Color(0xFF1D2432)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚙️ MÜHENDİSLİK MODU: Dişli & Şanzıman Oranı", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Temel Motor Kimliği ve Devir Limitleri
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customBrand,
                            onValueChange = { customBrand = it },
                            label = { Text("Marka") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customModel,
                            onValueChange = { customModel = it },
                            label = { Text("Model Adı") },
                            singleLine = true,
                            modifier = Modifier.weight(1.4f)
                        )
                        OutlinedTextField(
                            value = customCc,
                            onValueChange = { customCc = it },
                            label = { Text("Hacim (cc)") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = idleRpmText,
                            onValueChange = { idleRpmText = it },
                            label = { Text("Rölanti RPM") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = redlineRpmText,
                            onValueChange = { redlineRpmText = it },
                            label = { Text("Redline RPM") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = limiterRpmText,
                            onValueChange = { limiterRpmText = it },
                            label = { Text("Kesici RPM") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = peakHpText,
                            onValueChange = { peakHpText = it },
                            label = { Text("Güç (HP)") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                // Arka Lastik Ebadı Hızlı Seçici + Giriş
                item {
                    Text(
                        text = "Arka Lastik Ebadı (Gerçek Teker Çevresi & Hız/Devir Hesabı İçin):",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TireSpecification.COMMON_REAR_TIRES) { tireObj ->
                            val preset = tireObj.toSpecString()
                            Surface(
                                color = if (rearTireSpecText == preset) Color(0xFFFF6600) else Color(0xFF1E2533),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { rearTireSpecText = preset }
                            ) {
                                Text(
                                    text = preset,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                if (editorMode == 0) {
                    // KOLAY MOD: 1..6. Vites Maksimum Hızlarını (km/h) Girerek Otomatik Şanzıman Oranı Hesaplama
                    item {
                        Surface(
                            color = Color(0xFF192130),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "⚡ Her Viteste Redline (${redlineRpmText} RPM) Hızınızı (km/h) Girin — Oranlar Otomatik Hesaplanır!",
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = gear1MaxKmh,
                                        onValueChange = { gear1MaxKmh = it },
                                        label = { Text("1. Vites (km/h)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = gear2MaxKmh,
                                        onValueChange = { gear2MaxKmh = it },
                                        label = { Text("2. Vites (km/h)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = gear3MaxKmh,
                                        onValueChange = { gear3MaxKmh = it },
                                        label = { Text("3. Vites (km/h)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = gear4MaxKmh,
                                        onValueChange = { gear4MaxKmh = it },
                                        label = { Text("4. Vites (km/h)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = gear5MaxKmh,
                                        onValueChange = { gear5MaxKmh = it },
                                        label = { Text("5. Vites (km/h)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = gear6MaxKmh,
                                        onValueChange = { gear6MaxKmh = it },
                                        label = { Text("6. Vites (km/h)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        val redRpm = redlineRpmText.toDoubleOrNull()?.coerceIn(4000.0, 20000.0) ?: activeBike.redlineRpm
                                        val limRpm = limiterRpmText.toDoubleOrNull()?.coerceIn(redRpm, 21000.0) ?: (redRpm + 500.0)
                                        val idleRpm = idleRpmText.toDoubleOrNull()?.coerceIn(800.0, 3000.0) ?: activeBike.idleRpm
                                        val tire = TireSpecification.parse(rearTireSpecText)
                                        val tempBike = activeBike.copy(rearTire = tire, redlineRpm = redRpm)
                                        val speeds = listOf(
                                            gear1MaxKmh.toDoubleOrNull() ?: 60.0,
                                            gear2MaxKmh.toDoubleOrNull() ?: 90.0,
                                            gear3MaxKmh.toDoubleOrNull() ?: 120.0,
                                            gear4MaxKmh.toDoubleOrNull() ?: 148.0,
                                            gear5MaxKmh.toDoubleOrNull() ?: 172.0,
                                            gear6MaxKmh.toDoubleOrNull() ?: 195.0
                                        )
                                        val computedRatios = speeds.map { spd ->
                                            tempBike.computeGearRatioFromTopSpeedKmh(
                                                targetSpeedKmh = spd,
                                                atRpm = redRpm
                                            )
                                        }
                                        val updated = activeBike.copy(
                                            brand = customBrand.ifBlank { activeBike.brand },
                                            modelName = customModel.ifBlank { activeBike.modelName },
                                            displacementCc = customCc.toIntOrNull() ?: activeBike.displacementCc,
                                            gearRatios = computedRatios,
                                            rearTire = tire,
                                            idleRpm = idleRpm,
                                            redlineRpm = redRpm,
                                            revLimiterRpm = limRpm,
                                            maxPowerHp = peakHpText.toDoubleOrNull() ?: activeBike.maxPowerHp,
                                            maxTorqueNm = peakTrqText.toDoubleOrNull() ?: activeBike.maxTorqueNm
                                        )
                                        BikeDatabaseRepository.upsertBikeProfile(updated, setAsActive = true)
                                        NativeTelemetryEngineBridge.applyActiveBikeProfileToNative(updated)
                                        statusBanner = "✅ Vites hızlarından şanzıman oranları hesaplandı ve C++20 ECU'ya kaydedildi!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "💾 VİTES HIZLARINDAN OTOMATİK ŞANZIMAN HESAPLA & UYGULA",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // MÜHENDİSLİK MODU: Ön/Arka Dişli, Primer Oran ve 1..6. Vites Oranları
                    item {
                        Surface(
                            color = Color(0xFF192130),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "⚙️ Ön/Arka Dişli (Sprocket), Primer Redüksiyon ve 1–6. Vites Şanzıman Oranları",
                                    color = Color(0xFFFFAB00),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = frontTeethText,
                                        onValueChange = { frontTeethText = it },
                                        label = { Text("Ön Dişli (T)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = rearTeethText,
                                        onValueChange = { rearTeethText = it },
                                        label = { Text("Arka Dişli (T)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = primaryRatioText,
                                        onValueChange = { primaryRatioText = it },
                                        label = { Text("Primer Oran") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = g1RatioText, onValueChange = { g1RatioText = it }, label = { Text("1.Vites Oranı") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = g2RatioText, onValueChange = { g2RatioText = it }, label = { Text("2.Vites Oranı") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = g3RatioText, onValueChange = { g3RatioText = it }, label = { Text("3.Vites Oranı") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = g4RatioText, onValueChange = { g4RatioText = it }, label = { Text("4.Vites Oranı") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = g5RatioText, onValueChange = { g5RatioText = it }, label = { Text("5.Vites Oranı") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = g6RatioText, onValueChange = { g6RatioText = it }, label = { Text("6.Vites Oranı") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val fTeeth = frontTeethText.toIntOrNull()?.coerceIn(10, 22) ?: activeBike.frontSprocketTeeth
                                            val rTeeth = rearTeethText.toIntOrNull()?.coerceIn(30, 62) ?: activeBike.rearSprocketTeeth
                                            val prim = primaryRatioText.toDoubleOrNull()?.coerceIn(1.0, 4.0) ?: activeBike.primaryReductionRatio
                                            val ratios = listOf(
                                                g1RatioText.toDoubleOrNull() ?: activeBike.gearRatios[0],
                                                g2RatioText.toDoubleOrNull() ?: activeBike.gearRatios[1],
                                                g3RatioText.toDoubleOrNull() ?: activeBike.gearRatios[2],
                                                g4RatioText.toDoubleOrNull() ?: activeBike.gearRatios[3],
                                                g5RatioText.toDoubleOrNull() ?: activeBike.gearRatios[4],
                                                g6RatioText.toDoubleOrNull() ?: activeBike.gearRatios[5]
                                            )
                                            val updated = activeBike.copy(
                                                brand = customBrand.ifBlank { activeBike.brand },
                                                modelName = customModel.ifBlank { activeBike.modelName },
                                                displacementCc = customCc.toIntOrNull() ?: activeBike.displacementCc,
                                                frontSprocketTeeth = fTeeth,
                                                rearSprocketTeeth = rTeeth,
                                                primaryReductionRatio = prim,
                                                gearRatios = ratios,
                                                rearTire = TireSpecification.parse(rearTireSpecText),
                                                idleRpm = idleRpmText.toDoubleOrNull() ?: activeBike.idleRpm,
                                                redlineRpm = redlineRpmText.toDoubleOrNull() ?: activeBike.redlineRpm,
                                                revLimiterRpm = limiterRpmText.toDoubleOrNull() ?: activeBike.revLimiterRpm
                                            )
                                            BikeDatabaseRepository.upsertBikeProfile(updated, setAsActive = true)
                                            NativeTelemetryEngineBridge.applyActiveBikeProfileToNative(updated)
                                            statusBanner = "✅ Dişli ve şanzıman oranları C++20 ECU'ya kaydedildi!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6600)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("💾 PROFİLİ GÜNCELLE", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val newId = "custom_${System.currentTimeMillis()}"
                                            val newProfile = activeBike.copy(
                                                profileId = newId,
                                                brand = customBrand.ifBlank { "Özel" },
                                                modelName = customModel.ifBlank { "Custom Bike" },
                                                displacementCc = customCc.toIntOrNull() ?: activeBike.displacementCc,
                                                frontSprocketTeeth = frontTeethText.toIntOrNull() ?: activeBike.frontSprocketTeeth,
                                                rearSprocketTeeth = rearTeethText.toIntOrNull() ?: activeBike.rearSprocketTeeth,
                                                rearTire = TireSpecification.parse(rearTireSpecText),
                                                redlineRpm = redlineRpmText.toDoubleOrNull() ?: activeBike.redlineRpm,
                                                revLimiterRpm = limiterRpmText.toDoubleOrNull() ?: activeBike.revLimiterRpm
                                            )
                                            BikeDatabaseRepository.upsertBikeProfile(newProfile, setAsActive = true)
                                            NativeTelemetryEngineBridge.applyActiveBikeProfileToNative(newProfile)
                                            statusBanner = "✅ '${newProfile.brand} ${newProfile.modelName}' garaja yeni motor olarak eklendi!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("➕ YENİ MOTOR OLARAK EKLE", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // CANLI VİTES – HIZ – DEVİR TABLOSU (1..6. Vites)
                item {
                    Surface(
                        color = Color(0xFF0D1118),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF283244), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "📊 Canlı Şanzıman Vites–Hız–Devir Tablosu (${activeBike.brand} ${activeBike.modelName})",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            for (g in 1..activeBike.gearCount) {
                                val spd5k = activeBike.computeSpeedKmhAtRpm(g, 5000.0).roundToInt()
                                val spdTrq = activeBike.computeSpeedKmhAtRpm(g, activeBike.peakTorqueRpm).roundToInt()
                                val spdRed = activeBike.computeSpeedKmhAtRpm(g, activeBike.redlineRpm).roundToInt()
                                val ratioStr = String.format(Locale.US, "%.3f", activeBike.gearRatios.getOrElse(g - 1) { 1.0 })
                                Text(
                                    text = "$g. Vites (Oran $ratioStr):   5000 RPM → $spd5k km/h   •   Max Tork → $spdTrq km/h   •   Redline → $spdRed km/h",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================================
// SEKME 2: 105 OEM GÖSTERGE GALERİSİ (MARKA & KATEGORİ FİLTRELİ GÖRSEL ÖNİZLEME KARTLARI)
// =========================================================================================
@Composable
private fun DashboardGallery105Tab(
    selectedGaugeIndex: Int,
    favoriteSkinIds: MutableList<String>,
    syncBikeWithGauge: Boolean,
    onSyncBikeWithGaugeChange: (Boolean) -> Unit,
    onSelectGauge: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<GaugeDisplayCategory?>(null) }
    var selectedBrand by remember { mutableStateOf("Tümü") }
    var showFavoritesOnly by remember { mutableStateOf(false) }

    val brands = listOf("Tümü", "KTM", "Yamaha", "Honda", "Kawasaki", "Suzuki", "BMW", "Ducati", "Triumph", "Aprilia", "MV Agusta", "Husqvarna")

    val filteredModels = OemGaugeCatalog105.all105Models.mapIndexedNotNull { idx, spec ->
        val matchesFav = !showFavoritesOnly || favoriteSkinIds.contains(spec.skinId)
        val matchesCat = selectedCategory == null || spec.displayCategory == selectedCategory
        val matchesBrand = selectedBrand == "Tümü" || spec.brand.equals(selectedBrand, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
            spec.brand.contains(searchQuery, ignoreCase = true) ||
            spec.modelName.contains(searchQuery, ignoreCase = true) ||
            spec.modelNumber.toString() == searchQuery.trim()
        if (matchesFav && matchesCat && matchesBrand && matchesSearch) idx to spec else null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("105 Gösterge Modeli Ara (Örn: S1000RR, R1M, Hayabusa, R6, Panigale)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { showFavoritesOnly = !showFavoritesOnly },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showFavoritesOnly) Color(0xFFFFAB00) else Color(0xFF1D222D)
                )
            ) {
                Text("⭐ Favoriler (${favoriteSkinIds.size})", color = Color.White, fontSize = 12.sp)
            }
            Button(
                onClick = { selectedCategory = null; selectedBrand = "Tümü" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedCategory == null && selectedBrand == "Tümü") Color(0xFFFF6600) else Color(0xFF1D222D)
                )
            ) {
                Text("Tümü (105)", fontSize = 12.sp)
            }
            Button(
                onClick = { selectedCategory = GaugeDisplayCategory.MODERN_TFT },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedCategory == GaugeDisplayCategory.MODERN_TFT) Color(0xFFFF6600) else Color(0xFF1D222D)
                )
            ) {
                Text("TFT", fontSize = 12.sp)
            }
            Button(
                onClick = { selectedCategory = GaugeDisplayCategory.ANALOG_HYBRID },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedCategory == GaugeDisplayCategory.ANALOG_HYBRID) Color(0xFFFF6600) else Color(0xFF1D222D)
                )
            ) {
                Text("Analog Hibrit", fontSize = 12.sp)
            }
            Button(
                onClick = { selectedCategory = GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedCategory == GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD) Color(0xFFFF6600) else Color(0xFF1D222D)
                )
            ) {
                Text("LCD", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(brands) { b ->
                Surface(
                    color = if (selectedBrand == b) Color(0xFFFF6600) else Color(0xFF1B202C),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable { selectedBrand = b }
                ) {
                    Text(
                        text = b,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredModels) { (index, spec) ->
                val isSelected = index == selectedGaugeIndex
                val isFav = favoriteSkinIds.contains(spec.skinId)
                val accent = Color(spec.accentBrandColorHex)
                Surface(
                    color = if (isSelected) Color(0xFF241810) else Color(0xFF131720),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) accent else Color(0xFF262D3D),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectGauge(index) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Renk Paleti Önizleme Rozeti
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(spec.nightBgColorHex), RoundedCornerShape(10.dp))
                                    .border(2.dp, accent, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${spec.modelNumber}",
                                    color = accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "${spec.brand} ${spec.modelName} (${spec.generationYears})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Mimari: ${spec.geometryType.name}  •  Kadran: ${spec.gaugeRedlineRpm.roundToInt()}/${spec.gaugeMaxRpm.roundToInt()} RPM  •  Modlar: ${spec.availableSubModes.joinToString("/")}",
                                    color = Color(0xFF90A4AE),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (isFav) favoriteSkinIds.remove(spec.skinId)
                                else favoriteSkinIds.add(spec.skinId)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFav) Color(0xFFFFAB00) else Color(0xFF232936)
                            )
                        ) {
                            Text(if (isFav) "★ Favori" else "☆ Ekle", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================================
// SEKME 3: PRO GPS, SANAL ECU, VİTES VE GÖRÜNÜM AYARLARI
// =========================================================================================
@Composable
private fun AdvancedSettingsTab(
    language: AppLanguage,
    telemetry: UnifiedTelemetryState,
    useMph: Boolean,
    onUseMphChange: (Boolean) -> Unit,
    useOemSpeedBias: Boolean,
    onUseOemSpeedBiasChange: (Boolean) -> Unit,
    oemBiasRatio: Float,
    onOemBiasRatioChange: (Float) -> Unit,
    japan299Lock: Boolean,
    onJapan299LockChange: (Boolean) -> Unit,
    stationaryDeadbandKmh: Float,
    onStationaryDeadbandChange: (Float) -> Unit,
    maxGpsAccuracyMeters: Float,
    onMaxGpsAccuracyChange: (Float) -> Unit,
    imuBlendingWeight: Float,
    onImuBlendingWeightChange: (Float) -> Unit,
    gearCalculationMode: GearCalculationMode,
    onGearCalculationModeChange: (GearCalculationMode) -> Unit,
    syncBikeWithGauge: Boolean,
    onSyncBikeWithGaugeChange: (Boolean) -> Unit,
    rpmScalingMode: RpmScalingMode,
    onRpmScalingModeChange: (RpmScalingMode) -> Unit,
    wheelieDisplayMode: WheelieGaugeDisplayMode,
    onWheelieDisplayModeChange: (WheelieGaugeDisplayMode) -> Unit,
    autoOptimalShift: Boolean,
    onAutoOptimalShiftChange: (Boolean) -> Unit,
    manualStage1Rpm: Float,
    onManualStage1RpmChange: (Float) -> Unit,
    manualStage2Rpm: Float,
    onManualStage2RpmChange: (Float) -> Unit,
    reactionTimeMs: Float,
    onReactionTimeMsChange: (Float) -> Unit,
    acousticMicEnabled: Boolean,
    onAcousticMicChange: (Boolean) -> Unit,
    themeOverrideMode: Int,
    onThemeOverrideChange: (Int) -> Unit,
    autoHideCockpitHud: Boolean,
    onAutoHideCockpitHudChange: (Boolean) -> Unit,
    oledPixelShiftEnabled: Boolean,
    onOledPixelShiftChange: (Boolean) -> Unit,
    chromaKeyGreen: Boolean,
    onChromaKeyChange: (Boolean) -> Unit,
    onZeroCalibrateNow: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. CANLI GPS & SENSÖR FÜZYONU TANI VE KALİBRASYON KARTI
        item {
            SettingSectionCard("🛰️ 1. Hassas GPS Hız Filtresi & Dururken Hız Fırlamasını Önleme (Zero-Drift EKF)") {
                Text(
                    text = "Canlı GPS Durumu: ${if (telemetry.hasGpsFix) "KİLİTLİ (${telemetry.satellitesInUse} Uydu, ±${telemetry.gpsAccuracyMeters.roundToInt()}m)" else "UYDU ARANIYOR — Hız 0.0 km/h Kilitli"}  •  Ham GPS Hızı: ${String.format(Locale.US, "%.1f", telemetry.rawGpsSpeedKmh)} km/h",
                    color = if (telemetry.hasGpsFix) Color(0xFF00E676) else Color(0xFFFFAB00),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Durma Hız Eşiği (Stationary Deadband): ${String.format(Locale.US, "%.1f", stationaryDeadbandKmh)} km/h (Bu değerin altında hız 0.0 km/h kilitlenir)",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Slider(
                    value = stationaryDeadbandKmh,
                    onValueChange = onStationaryDeadbandChange,
                    valueRange = 1.0f..5.0f
                )
                Text(
                    text = "Maksimum GPS Sapma Filtresi (Accuracy Gate): ±${maxGpsAccuracyMeters.roundToInt()} metre",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Slider(
                    value = maxGpsAccuracyMeters,
                    onValueChange = onMaxGpsAccuracyChange,
                    valueRange = 8f..40f
                )
                Text(
                    text = "IMU İvmeölçer Karıştırma Oranı: %${(imuBlendingWeight * 100).roundToInt()} (%0 = Saf GPS Hızı)",
                    color = Color.White,
                    fontSize = 12.sp
                )
                Slider(
                    value = imuBlendingWeight,
                    onValueChange = onImuBlendingWeightChange,
                    valueRange = 0f..1f
                )
                Button(
                    onClick = onZeroCalibrateNow,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Text(
                        text = "📐 Şu Anki Telefon/Gidon Açısını 0° (Dik Konum) Olarak Kalibre Et",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. VİTES HESAPLAMA MODU & ALTIN KURAL
        item {
            SettingSectionCard("⚙️ 2. Hızdan Vites & Devir Hesaplama Motoru (Virtual ECU)") {
                Text("Vites Hesaplama Yöntemi:", color = Color.LightGray, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onGearCalculationModeChange(GearCalculationMode.AUTO_KINEMATIC_ECU) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gearCalculationMode == GearCalculationMode.AUTO_KINEMATIC_ECU) Color(0xFFFF6600) else Color(0xFF232936)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Otomatik (Hız + İvme + Dişli)", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onGearCalculationModeChange(GearCalculationMode.SEMI_AUTO_WITH_BUTTONS) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (gearCalculationMode == GearCalculationMode.SEMI_AUTO_WITH_BUTTONS) Color(0xFFFF6600) else Color(0xFF232936)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Yarı-Otomatik (+ / - Butonlu)", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kadran Değişince Motor Profilini de Otomatik Değiştir", color = Color.White, fontSize = 13.sp)
                    Switch(checked = syncBikeWithGauge, onCheckedChange = onSyncBikeWithGaugeChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kadran Devir Ölçekleme: Oransal Redline (Mod A) / Mutlak 1:1 (Mod B)", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = rpmScalingMode == RpmScalingMode.MODE_A_PROPORTIONAL_REDLINE,
                        onCheckedChange = {
                            onRpmScalingModeChange(
                                if (it) RpmScalingMode.MODE_A_PROPORTIONAL_REDLINE
                                else RpmScalingMode.MODE_B_NATIVE_1_TO_1
                            )
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("48 kHz Akustik Mikrofon DSP (Boşta Gaz & Debriyaj Tespiti)", color = Color.White, fontSize = 13.sp)
                    Switch(checked = acousticMicEnabled, onCheckedChange = onAcousticMicChange)
                }
            }
        }

        // 3. HIZ BİRİMİ, ECE-R39 SAPMASI & SHIFT-LIGHT
        item {
            SettingSectionCard("🚨 3. Hız Birimi, Fabrika Kadran Sapması & Shift-Light") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onUseMphChange(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (!useMph) Color(0xFFFF6600) else Color(0xFF232936)),
                        modifier = Modifier.weight(1f)
                    ) { Text("KM/H (Kilometre/Saat)") }
                    Button(
                        onClick = { onUseMphChange(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (useMph) Color(0xFFFF6600) else Color(0xFF232936)),
                        modifier = Modifier.weight(1f)
                    ) { Text("MPH (Mil/Saat)") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fabrika OEM Kadran Sapması (ECE-R39 +%${(oemBiasRatio * 100).roundToInt()})", color = Color.White, fontSize = 13.sp)
                    Switch(checked = useOemSpeedBias, onCheckedChange = onUseOemSpeedBiasChange)
                }
                if (useOemSpeedBias) {
                    Slider(value = oemBiasRatio, onValueChange = onOemBiasRatioChange, valueRange = 0.01f..0.10f)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Japon Centilmenlik Anlaşması: 299 km/h Dijital Kilit", color = Color.White, fontSize = 13.sp)
                    Switch(checked = japan299Lock, onCheckedChange = onJapan299LockChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dinamik Optimal Shift-Light (Tork Eğrisi + Tepki Süresi: ${reactionTimeMs.roundToInt()} ms)", color = Color.White, fontSize = 13.sp)
                    Switch(checked = autoOptimalShift, onCheckedChange = onAutoOptimalShiftChange)
                }
                if (!autoOptimalShift) {
                    Text("Manuel Evre 1 RPM: ${manualStage1Rpm.roundToInt()}  •  Evre 2 RPM: ${manualStage2Rpm.roundToInt()}", color = Color.LightGray, fontSize = 12.sp)
                    Slider(value = manualStage1Rpm, onValueChange = onManualStage1RpmChange, valueRange = 4000f..16000f)
                    Slider(value = manualStage2Rpm, onValueChange = onManualStage2RpmChange, valueRange = 4500f..17500f)
                }
            }
        }

        // 4. TEKER DERECE ÖLÇER, GÖRÜNÜM VE DİL SEÇİMİ
        item {
            SettingSectionCard("🎨 4. Teker Ölçer, Tema, Ekran ve 9 Dil Seçeneği") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onWheelieDisplayModeChange(WheelieGaugeDisplayMode.SMART_AUTO_POPUP) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wheelieDisplayMode == WheelieGaugeDisplayMode.SMART_AUTO_POPUP) Color(0xFF00E676) else Color(0xFF232936)
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Teker: Akıllı Otomatik", fontSize = 11.sp) }
                    Button(
                        onClick = { onWheelieDisplayModeChange(WheelieGaugeDisplayMode.ALWAYS_VISIBLE) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wheelieDisplayMode == WheelieGaugeDisplayMode.ALWAYS_VISIBLE) Color(0xFF00E676) else Color(0xFF232936)
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Teker: Her Zaman Açık", fontSize = 11.sp) }
                    Button(
                        onClick = { onWheelieDisplayModeChange(WheelieGaugeDisplayMode.HIDDEN_OEM_PURE) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wheelieDisplayMode == WheelieGaugeDisplayMode.HIDDEN_OEM_PURE) Color(0xFFFF6600) else Color(0xFF232936)
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Teker: Gizli (Saf OEM)", fontSize = 11.sp) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Otomatik (Lüks)", 1 to "Gündüz Modu", 2 to "Gece Modu").forEach { (mode, title) ->
                        Button(
                            onClick = { onThemeOverrideChange(mode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (themeOverrideMode == mode) Color(0xFFFF6600) else Color(0xFF232936)
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text(title, fontSize = 12.sp) }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Üst Kontrol Çubuğunu Sürüşte Otomatik Gizle (Ekrana Dokununca Açılır)", color = Color.White, fontSize = 13.sp)
                    Switch(checked = autoHideCockpitHud, onCheckedChange = onAutoHideCockpitHudChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("OLED Yanık Koruması (Pixel Shift) & Chroma Key (#00FF00)", color = Color.White, fontSize = 13.sp)
                    Switch(checked = chromaKeyGreen, onCheckedChange = onChromaKeyChange)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppLanguage.entries) { lang ->
                        Button(
                            onClick = { LocalizationManager.setLanguage(lang) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (language == lang) Color(0xFFFF6600) else Color(0xFF232936)
                            )
                        ) {
                            Text("${lang.flagEmoji} ${lang.code.uppercase(Locale.ROOT)} • ${lang.nativeName}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================================
// SEKME 4: YOL BİLGİSAYARI, DRAG BOX VE TEKER REKORLARI
// =========================================================================================
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryStatCard("TRIP A", String.format(Locale.US, "%.2f km", tripAKm), Modifier.weight(1f))
            TelemetryStatCard("TRIP B", String.format(Locale.US, "%.2f km", tripBKm), Modifier.weight(1f))
            TelemetryStatCard("ODOMETER", String.format(Locale.US, "%.1f km", odometerKm), Modifier.weight(1f))
            TelemetryStatCard("MAX SPEED", "${peakSpeedKmh.roundToInt()} km/h", Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryStatCard("PEAK RPM", "${peakRpm.roundToInt()} RPM", Modifier.weight(1f))
            TelemetryStatCard("MAX LEAN L/R", "${peakLeanLeftDeg.roundToInt()}° L / ${peakLeanRightDeg.roundToInt()}° R", Modifier.weight(1f))
            TelemetryStatCard("MAX WHEELIE AÇISI", String.format(Locale.US, "%.1f°", peakWheelieDeg), Modifier.weight(1f))
            TelemetryStatCard(
                "EN UZUN TEKER",
                String.format(Locale.US, "%.1f sn  •  %.1f m", longestWheelieSec, longestWheelieMeters),
                Modifier.weight(1f)
            )
        }

        Button(
            onClick = onResetTripAndPeaks,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
        ) {
            Text("🔄 TRIP A VE TÜM TEPE REKORLARINI SIFIRLA", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingSectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        color = Color(0xFF141820),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Color(0xFFFF6600), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            content()
        }
    }
}

@Composable
private fun TelemetryStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF151A24),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, Color(0xFF283040), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, color = Color(0xFF90A4AE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
