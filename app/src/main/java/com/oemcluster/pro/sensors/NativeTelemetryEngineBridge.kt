package com.oemcluster.pro.sensors

import com.oemcluster.pro.domain.BikeMechanicalProfile
import com.oemcluster.pro.domain.IgnitionBootPhase
import com.oemcluster.pro.domain.ShiftLightState
import com.oemcluster.pro.domain.SpeedUnit
import com.oemcluster.pro.domain.TransmissionEngagementState
import com.oemcluster.pro.domain.UnifiedTelemetryState
import com.oemcluster.pro.domain.WheelieGaugeDisplayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.max

/**
 * MotoCross — C++20 NDK 400 Hz Sensör Füzyonu (15-Durumlu EKF + PCA Gidon Kalibrasyonu) ve
 * 120 Hz Sanal ECU Fizik & Teker (Wheelie) Motoru JNI Köprüsü.
 */
object NativeTelemetryEngineBridge {

    private var nativeLibraryLoaded = false

    init {
        try {
            System.loadLibrary("oemcluster_native")
            nativeLibraryLoaded = true
        } catch (_: UnsatisfiedLinkError) {
            nativeLibraryLoaded = false
        }
    }

    private val _telemetryState = MutableStateFlow(UnifiedTelemetryState())
    val telemetryState: StateFlow<UnifiedTelemetryState> = _telemetryState.asStateFlow()

    // Sıfır GC (Zero-Allocation) için önceden ayrılmış 39-elemanlı JNI okuma tamponu
    private val nativeReadBuffer = DoubleArray(39)

    // Canlı GPS Uydu & Doğruluk Durumu
    @Volatile var liveHasGpsFix: Boolean = false
    @Volatile var liveSatellitesInUse: Int = 0
    @Volatile var liveSatellitesVisible: Int = 0
    @Volatile var liveGpsAccuracyMeters: Float = 0.0f
    @Volatile var liveRawGpsSpeedKmh: Double = 0.0
    @Volatile var lastValidGpsLocationNanos: Long = 0L

    // Harici C++ JNI Fonksiyon Bildirimleri
    external fun nativeResetEngine()

    external fun nativeConfigureBikeProfile(
        cylinderCount: Int,
        frontSprocketTeeth: Int,
        rearSprocketTeeth: Int,
        primaryReductionRatio: Double,
        gearRatiosArray: DoubleArray,
        rearTireWidthMm: Double,
        rearTireAspectPercent: Double,
        rearTireRimInch: Double,
        rearTireCrownRadiusMm: Double,
        rearTireSquishFactor: Double,
        idleRpm: Double,
        maxTorqueRpm: Double,
        maxTorqueNm: Double,
        maxPowerRpm: Double,
        redlineRpm: Double,
        revLimiterRpm: Double,
        totalMassKg: Double,
        dragCdA: Double,
        torqueRpmPointsArray: DoubleArray,
        torqueNmPointsArray: DoubleArray
    )

    external fun nativeConfigureCalibrationAndShiftLight(
        useOemSpeedBiasMode: Boolean,
        oemSpeedBiasRatio: Double,
        enableJapan299Lock: Boolean,
        autoOptimalShiftMode: Boolean,
        manualStage1Rpm: Double,
        manualStage2Rpm: Double,
        humanReactionTimeSec: Double
    )

    external fun nativeSetGpsFilterParameters(
        stationaryDeadbandKmh: Double,
        imuSmoothingFactor: Double
    )

    external fun nativeSetManualGearOverride(
        forcedGear: Int,
        clutchPulled: Boolean,
        throttleOverrideRpm: Double
    )

    external fun nativeTriggerZeroLeanCalibrationNow()
    external fun nativeTriggerIgnitionSweep(timestampNanos: Long)

    external fun nativeIngestImu400Hz(
        timestampNanos: Long,
        axPhone: Double,
        ayPhone: Double,
        azPhone: Double,
        gxPhone: Double,
        gyPhone: Double,
        gzPhone: Double
    )

    external fun nativeIngestGnssDoppler(
        timestampNanos: Long,
        vNorthMs: Double,
        vEastMs: Double,
        vDownMs: Double,
        speedAccuracyMs: Double,
        gnssLagSec: Double
    )

    external fun nativeIngestMicrophonePcm48kHz(
        pcmBuffer: FloatArray,
        sampleCount: Int
    )

    external fun nativeSetAcousticEnabled(enabled: Boolean)

    external fun nativeUpdateAmbientLight(
        ambientLux: Float,
        themeOverrideMode: Int
    )

    external fun nativeStepAndReadTelemetry120Hz(
        timestampNanos: Long,
        outTelemetryBuffer: DoubleArray
    )

    fun applyActiveBikeProfileToNative(profile: BikeMechanicalProfile) {
        if (!nativeLibraryLoaded) return

        val gears = profile.gearRatios.toDoubleArray()
        val trqRpms = profile.torqueCurve.map { it.rpm }.toDoubleArray()
        val trqNms = profile.torqueCurve.map { it.torqueNm }.toDoubleArray()

        nativeConfigureBikeProfile(
            cylinderCount = profile.engineConfig.cylinderCount,
            frontSprocketTeeth = profile.frontSprocketTeeth,
            rearSprocketTeeth = profile.rearSprocketTeeth,
            primaryReductionRatio = profile.primaryReductionRatio,
            gearRatiosArray = gears,
            rearTireWidthMm = profile.rearTire.widthMm,
            rearTireAspectPercent = profile.rearTire.aspectRatioPercent,
            rearTireRimInch = profile.rearTire.rimDiameterInch,
            rearTireCrownRadiusMm = profile.rearTire.crownRadiusMm,
            rearTireSquishFactor = profile.rearTire.staticSquishFactor,
            idleRpm = profile.idleRpm,
            maxTorqueRpm = profile.maxTorqueRpm,
            maxTorqueNm = profile.maxTorqueNm,
            maxPowerRpm = profile.maxPowerRpm,
            redlineRpm = profile.redlineRpm,
            revLimiterRpm = profile.revLimiterRpm,
            totalMassKg = profile.totalMassKg,
            dragCdA = profile.dragCoefficientAreaCdA,
            torqueRpmPointsArray = trqRpms,
            torqueNmPointsArray = trqNms
        )
    }

    fun applyGpsFilterSettings(stationaryDeadbandKmh: Double, imuSmoothingFactor: Double) {
        if (!nativeLibraryLoaded) return
        nativeSetGpsFilterParameters(stationaryDeadbandKmh, imuSmoothingFactor)
    }

    fun applyManualGearOverride(forcedGear: Int, clutchPulled: Boolean, throttleOverrideRpm: Double = -1.0) {
        if (!nativeLibraryLoaded) return
        nativeSetManualGearOverride(forcedGear, clutchPulled, throttleOverrideRpm)
    }

    /**
     * 120 FPS Render döngüsünde C++ NDK motorundan son telemetri durumunu çeker ve
     * UnifiedTelemetryState olarak yayınlar.
     */
    fun pollFrame120Hz(
        timestampNanos: Long,
        activeProfile: BikeMechanicalProfile,
        wheelieDisplayMode: WheelieGaugeDisplayMode = WheelieGaugeDisplayMode.SMART_AUTO_POPUP,
        speedUnit: SpeedUnit = SpeedUnit.KMH,
        rideModeLabel: String = "TRACK"
    ): UnifiedTelemetryState {
        // 3 saniyedir geçerli GPS paketi gelmediyse GPS kilidi düştü kabul et
        if (lastValidGpsLocationNanos > 0L && (timestampNanos - lastValidGpsLocationNanos) > 3_000_000_000L) {
            liveHasGpsFix = false
            liveRawGpsSpeedKmh = 0.0
        }

        if (!nativeLibraryLoaded) {
            return _telemetryState.value.copy(
                activeBikeProfileId = activeProfile.id,
                activeBikeDisplayName = "${activeProfile.brand} ${activeProfile.modelName}",
                bikeIdleRpm = activeProfile.idleRpm,
                bikeRedlineRpm = activeProfile.redlineRpm,
                bikeRevLimiterRpm = activeProfile.revLimiterRpm,
                wheelieGaugeDisplayMode = wheelieDisplayMode,
                speedUnit = speedUnit,
                selectedRideModeLabel = rideModeLabel,
                hasGpsFix = liveHasGpsFix,
                satellitesInUse = liveSatellitesInUse,
                satellitesVisible = liveSatellitesVisible,
                gpsAccuracyMeters = liveGpsAccuracyMeters,
                rawGpsSpeedKmh = liveRawGpsSpeedKmh
            )
        }

        nativeStepAndReadTelemetry120Hz(timestampNanos, nativeReadBuffer)
        val b = nativeReadBuffer
        val prev = _telemetryState.value

        val dtHours = if (prev.timestampNanos > 0L && timestampNanos > prev.timestampNanos) {
            ((timestampNanos - prev.timestampNanos) * 1e-9) / 3600.0
        } else {
            0.0
        }

        val trueSpeedKmh = b[1]
        val deltaKm = if (trueSpeedKmh >= 1.5) trueSpeedKmh * dtHours else 0.0
        val leanDeg = b[18]

        val nextState = UnifiedTelemetryState(
            timestampNanos = timestampNanos,
            activeBikeProfileId = activeProfile.id,
            activeBikeDisplayName = "${activeProfile.brand} ${activeProfile.modelName}",
            bikeIdleRpm = activeProfile.idleRpm,
            bikeRedlineRpm = activeProfile.redlineRpm,
            bikeRevLimiterRpm = activeProfile.revLimiterRpm,
            trueSpeedMs = b[0],
            trueSpeedKmh = trueSpeedKmh,
            displayedSpeedKmh = b[2],
            is299LockEngaged = b[3] > 0.5,
            speedUnit = speedUnit,
            hasGpsFix = liveHasGpsFix,
            satellitesInUse = liveSatellitesInUse,
            satellitesVisible = liveSatellitesVisible,
            gpsAccuracyMeters = liveGpsAccuracyMeters,
            rawGpsSpeedKmh = liveRawGpsSpeedKmh,
            actualEngineRpm = b[4],
            normalizedRpmRatio = b[5],
            peakHoldRpm = b[6],
            rpmRateOfChangePerSec = b[7],
            stage1ShiftRpm = b[8],
            stage2ShiftRpm = b[9],
            shiftLightState = ShiftLightState.fromNativeCode(b[10].toInt()),
            shiftFlashPhaseOn = b[11] > 0.5,
            fullColorInversionActive = b[12] > 0.5,
            currentGear = b[13].toInt(),
            isNeutral = b[14] > 0.5,
            isClutchPulled = b[15] > 0.5,
            transmissionState = TransmissionEngagementState.fromNativeCode(b[16].toInt()),
            gearShiftAnimationProgress = b[17].toFloat(),
            leanAngleDeg = leanDeg,
            maxLeftLeanDeg = if (leanDeg < 0) max(prev.maxLeftLeanDeg, abs(leanDeg)) else prev.maxLeftLeanDeg,
            maxRightLeanDeg = if (leanDeg > 0) max(prev.maxRightLeanDeg, leanDeg) else prev.maxRightLeanDeg,
            pitchAngleDeg = b[19],
            longitudinalAccelMs2 = b[20],
            longitudinalGForce = b[20] / 9.80665,
            lateralGForce = b[21],
            longitudinalJerkMs3 = b[22],
            effectiveTireRadiusMm = b[23],
            slipRatioLambda = b[24],
            nightModeBlendFactor = b[25].toFloat(),
            ignitionBootPhase = IgnitionBootPhase.fromNativeCode(b[26].toInt()),
            ignitionSweepProgress = b[27].toFloat(),
            estimatedWheelHorsepower = b[28],
            estimatedWheelTorqueNm = b[29],
            tractionControlIntervening = b[30] > 0.5,
            selectedRideModeLabel = rideModeLabel,
            isWheelieActive = b[31] > 0.5,
            isWheelieSummaryVisible = b[32] > 0.5,
            wheelieAngleDeg = b[33],
            currentWheelieDurationSec = b[34],
            currentWheelieDistanceMeters = b[35],
            maxWheelieAngleDeg = max(prev.maxWheelieAngleDeg, b[36]),
            longestWheelieDurationSec = max(prev.longestWheelieDurationSec, b[37]),
            longestWheelieDistanceMeters = max(prev.longestWheelieDistanceMeters, b[38]),
            wheelieGaugeDisplayMode = wheelieDisplayMode,
            tripDistanceKm = prev.tripDistanceKm + deltaKm,
            odometerKm = prev.odometerKm + deltaKm,
            maxSpeedSessionKmh = max(prev.maxSpeedSessionKmh, trueSpeedKmh),
            maxRpmSession = max(prev.maxRpmSession, b[4]),
            best0To100KmhSec = prev.best0To100KmhSec,
            best100To200KmhSec = prev.best100To200KmhSec,
            best400mDragSec = prev.best400mDragSec
        )

        _telemetryState.value = nextState
        return nextState
    }

    fun updateDirectSimulatorState(state: UnifiedTelemetryState) {
        _telemetryState.value = state
    }
}
