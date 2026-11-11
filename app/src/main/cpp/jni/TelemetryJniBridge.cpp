#include <jni.h>
#include <atomic>
#include <mutex>
#include <algorithm>
#include "../kalman/MountCalibrator.hpp"
#include "../kalman/StrapdownINS15StateEKF.hpp"
#include "../acoustic/AcousticFftRpmDetector.hpp"
#include "../physics/VirtualEcuPhysicsEngine.hpp"

namespace {

constexpr jsize TELEMETRY_PACK_SIZE = 39;

struct NativeEngineInstance {
    std::mutex mutex;
    std::mutex acousticMutex;

    oemcluster::kalman::MountCalibrator mountCalibrator;
    oemcluster::kalman::StrapdownINS15StateEKF ekf15;
    oemcluster::acoustic::AcousticFftRpmDetector acousticDetector;
    oemcluster::physics::VirtualEcuPhysicsEngine virtualEcu;

    std::atomic<double> acousticRpm{0.0};
    std::atomic<double> acousticConfidence{0.0};

    oemcluster::math::Vec3d lastRawPhoneAccel{0.0, 9.5, 2.5};
    int64_t lastImuTimestampNanos{0};
    int64_t lastPhysicsTimestampNanos{0};

    bool useOemSpeedBiasMode{false};
    double oemSpeedBiasRatio{0.05};
    bool enableJapan299Lock{false};
};

NativeEngineInstance g_engine;

} // anonymous namespace

extern "C" {

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeResetEngine(
    JNIEnv* /*env*/,
    jobject /*thiz*/
) {
    std::scoped_lock lock(g_engine.mutex);
    g_engine.ekf15.reset();
    g_engine.lastImuTimestampNanos = 0;
    g_engine.lastPhysicsTimestampNanos = 0;
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeConfigureBikeProfile(
    JNIEnv* env,
    jobject /*thiz*/,
    jint cylinderCount,
    jint frontSprocketTeeth,
    jint rearSprocketTeeth,
    jdouble primaryReductionRatio,
    jdoubleArray gearRatiosArray,
    jdouble rearTireWidthMm,
    jdouble rearTireAspectPercent,
    jdouble rearTireRimInch,
    jdouble rearTireCrownRadiusMm,
    jdouble rearTireSquishFactor,
    jdouble idleRpm,
    jdouble maxTorqueRpm,
    jdouble maxTorqueNm,
    jdouble maxPowerRpm,
    jdouble redlineRpm,
    jdouble revLimiterRpm,
    jdouble totalMassKg,
    jdouble dragCdA,
    jdoubleArray torqueRpmPointsArray,
    jdoubleArray torqueNmPointsArray
) {
    oemcluster::physics::NativeBikeMechanicalConfig cfg{};
    cfg.cylinderCount = cylinderCount;
    cfg.frontSprocketTeeth = frontSprocketTeeth;
    cfg.rearSprocketTeeth = rearSprocketTeeth;
    cfg.primaryReductionRatio = primaryReductionRatio;

    if (gearRatiosArray != nullptr) {
        const jsize len = std::min<jsize>(6, env->GetArrayLength(gearRatiosArray));
        cfg.gearCount = std::max<int>(1, static_cast<int>(len));
        jdouble* elems = env->GetDoubleArrayElements(gearRatiosArray, nullptr);
        for (jsize i = 0; i < len; ++i) {
            cfg.gearRatios[i] = elems[i];
        }
        for (jsize i = len; i < 6; ++i) {
            cfg.gearRatios[i] = (len > 0) ? elems[len - 1] : 1.0;
        }
        env->ReleaseDoubleArrayElements(gearRatiosArray, elems, JNI_ABORT);
    }

    cfg.rearTireWidthMm = rearTireWidthMm;
    cfg.rearTireAspectPercent = rearTireAspectPercent;
    cfg.rearTireRimInch = rearTireRimInch;
    cfg.rearTireCrownRadiusMm = rearTireCrownRadiusMm;
    cfg.rearTireSquishFactor = rearTireSquishFactor;

    cfg.idleRpm = idleRpm;
    cfg.maxTorqueRpm = maxTorqueRpm;
    cfg.maxTorqueNm = maxTorqueNm;
    cfg.maxPowerRpm = maxPowerRpm;
    cfg.redlineRpm = redlineRpm;
    cfg.revLimiterRpm = revLimiterRpm;
    cfg.totalMassKg = totalMassKg;
    cfg.dragCdA = dragCdA;

    if (torqueRpmPointsArray != nullptr && torqueNmPointsArray != nullptr) {
        const jsize count = std::min<jsize>(
            16,
            std::min(env->GetArrayLength(torqueRpmPointsArray), env->GetArrayLength(torqueNmPointsArray))
        );
        cfg.torqueCurveCount = static_cast<int>(count);
        jdouble* rpms = env->GetDoubleArrayElements(torqueRpmPointsArray, nullptr);
        jdouble* trqs = env->GetDoubleArrayElements(torqueNmPointsArray, nullptr);
        for (jsize i = 0; i < count; ++i) {
            cfg.torqueRpmPoints[i] = rpms[i];
            cfg.torqueNmPoints[i] = trqs[i];
        }
        env->ReleaseDoubleArrayElements(torqueRpmPointsArray, rpms, JNI_ABORT);
        env->ReleaseDoubleArrayElements(torqueNmPointsArray, trqs, JNI_ABORT);
    }

    {
        std::scoped_lock lock(g_engine.mutex);
        g_engine.virtualEcu.setBikeMechanicalConfig(cfg);
    }
    {
        std::scoped_lock lock(g_engine.acousticMutex);
        g_engine.acousticDetector.setEngineConfiguration(cylinderCount, idleRpm, revLimiterRpm);
    }
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeConfigureCalibrationAndShiftLight(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean useOemSpeedBiasMode,
    jdouble oemSpeedBiasRatio,
    jboolean enableJapan299Lock,
    jboolean autoOptimalShiftMode,
    jdouble manualStage1Rpm,
    jdouble manualStage2Rpm,
    jdouble humanReactionTimeSec
) {
    std::scoped_lock lock(g_engine.mutex);
    g_engine.useOemSpeedBiasMode = static_cast<bool>(useOemSpeedBiasMode);
    g_engine.oemSpeedBiasRatio = oemSpeedBiasRatio;
    g_engine.enableJapan299Lock = static_cast<bool>(enableJapan299Lock);

    g_engine.virtualEcu.setShiftLightSettings(
        static_cast<bool>(autoOptimalShiftMode),
        manualStage1Rpm,
        manualStage2Rpm,
        humanReactionTimeSec,
        1000.0
    );
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeSetGpsFilterParameters(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jdouble stationaryDeadbandKmh,
    jdouble imuSmoothingFactor
) {
    std::scoped_lock lock(g_engine.mutex);
    g_engine.ekf15.setGpsFilterParameters(stationaryDeadbandKmh, imuSmoothingFactor);
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeSetManualGearOverride(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jint forcedGear,
    jboolean clutchPulled,
    jdouble throttleOverrideRpm
) {
    std::scoped_lock lock(g_engine.mutex);
    g_engine.virtualEcu.forceManualGearOrClutch(
        forcedGear,
        static_cast<bool>(clutchPulled),
        throttleOverrideRpm
    );
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeTriggerZeroLeanCalibrationNow(
    JNIEnv* /*env*/,
    jobject /*thiz*/
) {
    std::scoped_lock lock(g_engine.mutex);
    g_engine.mountCalibrator.triggerManualZeroMountCalibration(g_engine.lastRawPhoneAccel);
    g_engine.ekf15.zeroLeanAndPitchNow();
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeTriggerIgnitionSweep(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong timestampNanos
) {
    std::scoped_lock lock(g_engine.mutex);
    g_engine.virtualEcu.triggerIgnitionNeedleSweep(timestampNanos);
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeIngestImu400Hz(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong timestampNanos,
    jdouble axPhone,
    jdouble ayPhone,
    jdouble azPhone,
    jdouble gxPhone,
    jdouble gyPhone,
    jdouble gzPhone
) {
    std::scoped_lock lock(g_engine.mutex);
    const oemcluster::math::Vec3d accelPhone{axPhone, ayPhone, azPhone};
    const oemcluster::math::Vec3d gyroPhone{gxPhone, gyPhone, gzPhone};
    g_engine.lastRawPhoneAccel = accelPhone;

    double dtSec = 0.0025;
    if (g_engine.lastImuTimestampNanos > 0 && timestampNanos > g_engine.lastImuTimestampNanos) {
        dtSec = static_cast<double>(timestampNanos - g_engine.lastImuTimestampNanos) * 1e-9;
    }
    g_engine.lastImuTimestampNanos = timestampNanos;

    g_engine.mountCalibrator.ingestImuSample(
        accelPhone,
        gyroPhone,
        g_engine.ekf15.getTrueSpeedMs()
    );
    const oemcluster::math::Vec3d accelBike =
        g_engine.mountCalibrator.transformAccelToBikeFrame(accelPhone);
    const oemcluster::math::Vec3d gyroBike =
        g_engine.mountCalibrator.transformGyroToBikeFrame(gyroPhone);

    g_engine.ekf15.predict400Hz(timestampNanos, dtSec, accelBike, gyroBike);
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeIngestGnssDoppler(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jlong timestampNanos,
    jdouble vNorthMs,
    jdouble vEastMs,
    jdouble vDownMs,
    jdouble speedAccuracyMs,
    jdouble gnssLagSec
) {
    std::scoped_lock lock(g_engine.mutex);
    g_engine.ekf15.updateGnssDopplerOosm(
        timestampNanos,
        vNorthMs,
        vEastMs,
        vDownMs,
        speedAccuracyMs,
        gnssLagSec
    );
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeIngestMicrophonePcm48kHz(
    JNIEnv* env,
    jobject /*thiz*/,
    jfloatArray pcmBuffer,
    jint sampleCount
) {
    if (pcmBuffer == nullptr || sampleCount <= 0) return;
    const jsize safeCount = std::min<jsize>(sampleCount, env->GetArrayLength(pcmBuffer));
    jfloat* samples = env->GetFloatArrayElements(pcmBuffer, nullptr);
    {
        std::scoped_lock lock(g_engine.acousticMutex);
        g_engine.acousticDetector.processAudioSamples(samples, static_cast<size_t>(safeCount));
        g_engine.acousticRpm.store(g_engine.acousticDetector.getDetectedAcousticRpm(), std::memory_order_relaxed);
        g_engine.acousticConfidence.store(g_engine.acousticDetector.getSignalConfidence(), std::memory_order_relaxed);
    }
    env->ReleaseFloatArrayElements(pcmBuffer, samples, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeSetAcousticEnabled(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jboolean enabled
) {
    if (!enabled) {
        g_engine.acousticRpm.store(0.0, std::memory_order_relaxed);
        g_engine.acousticConfidence.store(0.0, std::memory_order_relaxed);
    }
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeUpdateAmbientLight(
    JNIEnv* /*env*/,
    jobject /*thiz*/,
    jfloat ambientLux,
    jint themeOverrideMode
) {
    std::scoped_lock lock(g_engine.mutex);
    g_engine.virtualEcu.updateAmbientLightLux(ambientLux, themeOverrideMode);
}

JNIEXPORT void JNICALL
Java_com_oemcluster_pro_sensors_NativeTelemetryEngineBridge_nativeStepAndReadTelemetry120Hz(
    JNIEnv* env,
    jobject /*thiz*/,
    jlong timestampNanos,
    jdoubleArray outTelemetryBuffer
) {
    if (outTelemetryBuffer == nullptr || env->GetArrayLength(outTelemetryBuffer) < TELEMETRY_PACK_SIZE) return;
    std::scoped_lock lock(g_engine.mutex);

    double dtSec = 1.0 / 120.0;
    if (g_engine.lastPhysicsTimestampNanos > 0 && timestampNanos > g_engine.lastPhysicsTimestampNanos) {
        dtSec = static_cast<double>(timestampNanos - g_engine.lastPhysicsTimestampNanos) * 1e-9;
    }
    g_engine.lastPhysicsTimestampNanos = timestampNanos;

    const double trueSpeedMs = g_engine.ekf15.getTrueSpeedMs();
    const double axTrueMs2 = g_engine.ekf15.getTrueLongitudinalAccelMs2();
    const double ayTrueMs2 = g_engine.ekf15.getLateralAccelMs2();
    const double jerkXMs3 = g_engine.ekf15.getLongitudinalJerkMs3();
    const double leanRad = g_engine.ekf15.getLeanAngleRad();
    const double pitchRad = g_engine.ekf15.getPitchAngleRad();

    const auto ecuOut = g_engine.virtualEcu.stepPhysics120Hz(
        timestampNanos,
        dtSec,
        trueSpeedMs,
        axTrueMs2,
        jerkXMs3,
        leanRad,
        pitchRad,
        g_engine.acousticRpm.load(std::memory_order_relaxed),
        g_engine.acousticConfidence.load(std::memory_order_relaxed)
    );

    bool is299Locked = false;
    const double displayedSpeedKmh = g_engine.ekf15.computeDisplayedSpeedKmh(
        g_engine.useOemSpeedBiasMode,
        g_engine.oemSpeedBiasRatio,
        g_engine.enableJapan299Lock,
        is299Locked
    );

    jdouble pack[TELEMETRY_PACK_SIZE];
    pack[0]  = trueSpeedMs;
    pack[1]  = trueSpeedMs * 3.6;
    pack[2]  = (ecuOut.ignitionBootPhaseCode == 2) ? 299.0 : displayedSpeedKmh;
    pack[3]  = is299Locked ? 1.0 : 0.0;
    pack[4]  = ecuOut.actualEngineRpm;
    pack[5]  = ecuOut.normalizedRpmRatio;
    pack[6]  = ecuOut.peakHoldRpm;
    pack[7]  = ecuOut.rpmDerivativePerSec;
    pack[8]  = ecuOut.stage1ShiftRpm;
    pack[9]  = ecuOut.stage2ShiftRpm;
    pack[10] = static_cast<double>(ecuOut.shiftLightStateCode);
    pack[11] = ecuOut.shiftFlashPhaseOn ? 1.0 : 0.0;
    pack[12] = ecuOut.fullColorInversionActive ? 1.0 : 0.0;
    pack[13] = static_cast<double>(ecuOut.currentGear);
    pack[14] = ecuOut.isNeutral ? 1.0 : 0.0;
    pack[15] = ecuOut.isClutchPulled ? 1.0 : 0.0;
    pack[16] = static_cast<double>(ecuOut.transmissionStateCode);
    pack[17] = static_cast<double>(ecuOut.gearShiftAnimProgress);
    pack[18] = g_engine.ekf15.getLeanAngleDeg();
    pack[19] = g_engine.ekf15.getPitchAngleDeg();
    pack[20] = axTrueMs2;
    pack[21] = ayTrueMs2 / 9.80665;
    pack[22] = jerkXMs3;
    pack[23] = ecuOut.effectiveTireRadiusMm;
    pack[24] = ecuOut.slipRatioLambda;
    pack[25] = static_cast<double>(ecuOut.nightModeFactor);
    pack[26] = static_cast<double>(ecuOut.ignitionBootPhaseCode);
    pack[27] = static_cast<double>(ecuOut.ignitionSweepProgress);
    pack[28] = ecuOut.estimatedWheelHp;
    pack[29] = ecuOut.estimatedWheelTorqueNm;
    pack[30] = ecuOut.tractionControlActive ? 1.0 : 0.0;
    pack[31] = ecuOut.isWheelieActive ? 1.0 : 0.0;
    pack[32] = ecuOut.isWheelieSummaryVisible ? 1.0 : 0.0;
    pack[33] = ecuOut.wheelieAngleDeg;
    pack[34] = ecuOut.currentWheelieDurationSec;
    pack[35] = ecuOut.currentWheelieDistanceMeters;
    pack[36] = ecuOut.maxWheelieAngleDeg;
    pack[37] = ecuOut.longestWheelieDurationSec;
    pack[38] = ecuOut.longestWheelieDistanceMeters;

    env->SetDoubleArrayRegion(outTelemetryBuffer, 0, TELEMETRY_PACK_SIZE, pack);
}

} // extern "C"
