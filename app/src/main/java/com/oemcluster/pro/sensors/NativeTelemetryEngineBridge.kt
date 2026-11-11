package com.oemcluster.pro.sensors

import com.oemcluster.pro.data.BikeDatabaseRepository
import com.oemcluster.pro.domain.BikeMechanicalProfile
import com.oemcluster.pro.domain.IgnitionBootPhase
import com.oemcluster.pro.domain.ShiftLightState
import com.oemcluster.pro.domain.TransmissionEngagementState
import com.oemcluster.pro.domain.UnifiedTelemetryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    /**
     * Garajım (My Bike Profile) menüsündeki aktif motosiklet profilini
     * C++ NDK Sanal ECU motoruna anında yükler.
     */
    fun applyActiveBikeProfileToNative(profile: BikeMechanicalProfile) {
        if (!nativeLibraryLoaded) return

        val gears = DoubleArray(6) { idx ->
            profile.gearRatios.getOrElse(idx) { 1.0 }
        }
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

    /**
     * 120 FPS Render döngüsünde C++ NDK motorundan son telemetri durumunu çeker ve
     * UnifiedTelemetryState olarak yayınlar.
     */
    fun tick120HzFrame(timestampNanos: Long) {
        val activeBike = BikeDatabaseRepository.activeBikeProfile.value
        if (nativeLibraryLoaded) {
            nativeStepAndReadTelemetry120Hz(timestampNanos, nativeReadBuffer)
            val b = nativeReadBuffer
            _telemetryState.value = UnifiedTelemetryState(
                timestampNanos = timestampNanos,
                activeBikeProfileId = activeBike.profileId,
                activeBikeDisplayName = "${activeBike.brand} ${activeBike.modelName}",
                bikeIdleRpm = activeBike.idleRpm,
                bikeRedlineRpm = activeBike.redlineRpm,
                bikeRevLimiterRpm = activeBike.revLimiterRpm,
                trueSpeedMs = b[0],
                trueSpeedKmh = b[1],
                displayedSpeedKmh = b[2],
                is299LockEngaged = b[3] > 0.5,
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
                leanAngleDeg = b[18],
                pitchAngleDeg = b[19],
                longitudinalAccelMs2 = b[20],
                longitudinalG = b[20] / 9.80665,
                lateralG = b[21],
                jerkLongitudinalMs3 = b[22],
                effectiveTireRadiusMm = b[23],
                wheelSlipRatioLambda = b[24],
                nightModeTransitionFactor = b[25].toFloat(),
                ignitionBootPhase = IgnitionBootPhase.fromNativeCode(b[26].toInt()),
                ignitionSweepProgress = b[27].toFloat(),
                estimatedWheelHorsepower = b[28],
                estimatedWheelTorqueNm = b[29],
                tractionControlInterventionActive = b[30] > 0.5,
                isWheelieActive = b[31] > 0.5,
                isWheelieSummaryVisible = b[32] > 0.5,
                wheelieAngleDeg = b[33],
                currentWheelieDurationSec = b[34],
                currentWheelieDistanceMeters = b[35],
                maxWheelieAngleDeg = b[36],
                longestWheelieDurationSec = b[37],
                longestWheelieDistanceMeters = b[38]
            )
        }
    }

    /**
     * Kotlin JVM Simülasyon / Test veya Yedek Çalışma Modu için doğrudan state güncelleyici
     */
    fun publishTelemetryState(state: UnifiedTelemetryState) {
        _telemetryState.value = state
    }
}
