#include "VirtualEcuPhysicsEngine.hpp"
#include <cmath>
#include <algorithm>

namespace oemcluster::physics {

VirtualEcuPhysicsEngine::VirtualEcuPhysicsEngine() noexcept {
    recomputeOptimalShiftRpms();
}

void VirtualEcuPhysicsEngine::setBikeMechanicalConfig(
    const NativeBikeMechanicalConfig& config
) noexcept {
    bike_ = config;
    bike_.gearCount = std::clamp(bike_.gearCount, 1, 6);
    recomputeOptimalShiftRpms();
    if (currentDisplayedRpm_ < bike_.idleRpm) {
        currentDisplayedRpm_ = bike_.idleRpm;
    }
}

void VirtualEcuPhysicsEngine::setShiftLightSettings(
    bool autoOptimalMode,
    double manualStage1Rpm,
    double manualStage2Rpm,
    double humanReactionTimeSec,
    double stage1WarnDeltaRpm
) noexcept {
    autoOptimalShiftMode_ = autoOptimalMode;
    manualStage1Rpm_ = manualStage1Rpm;
    manualStage2Rpm_ = manualStage2Rpm;
    humanReactionTimeSec_ = std::clamp(humanReactionTimeSec, 0.0, 0.450);
    stage1WarnDeltaRpm_ = std::clamp(stage1WarnDeltaRpm, 300.0, 3500.0);
}

void VirtualEcuPhysicsEngine::triggerIgnitionNeedleSweep(int64_t currentTimestampNanos) noexcept {
    ignitionBootPhaseCode_ = 1;
    ignitionStartNanos_ = currentTimestampNanos;
}

void VirtualEcuPhysicsEngine::updateAmbientLightLux(float ambientLux, int themeOverrideMode) noexcept {
    if (themeOverrideMode == 1) {
        targetNightFactor_ = 0.0f;
    } else if (themeOverrideMode == 2) {
        targetNightFactor_ = 1.0f;
    } else {
        targetNightFactor_ = (ambientLux < 45.0f) ? 1.0f : 0.0f;
    }
}

void VirtualEcuPhysicsEngine::forceManualGearOrClutch(
    int forcedGear,
    bool clutchPulled,
    double throttleOverrideRpm
) noexcept {
    if (forcedGear != manualForcedGear_ && forcedGear >= 0) {
        gearShiftAnimProgress_ = 0.0f;
        if (currentDisplayedRpm_ > peakHoldRpm_) {
            peakHoldRpm_ = currentDisplayedRpm_;
            peakHoldTimerSec_ = 1.8;
        }
    }
    manualForcedGear_ = forcedGear;
    manualClutchOverride_ = clutchPulled;
    manualThrottleRpm_ = throttleOverrideRpm;
}

void VirtualEcuPhysicsEngine::registerGearChange(int newGear, int transmissionCode) noexcept {
    const int maxG = std::clamp(bike_.gearCount, 1, 6);
    newGear = std::clamp(newGear, 0, maxG);
    if (newGear == currentGear_) return;

    if (newGear > currentGear_) {
        peakHoldRpm_ = std::max(peakHoldRpm_, currentDisplayedRpm_);
        peakHoldTimerSec_ = 1.8;
    }
    currentGear_ = newGear;
    transmissionStateCode_ = transmissionCode;
    gearShiftAnimProgress_ = 0.0f;
    shiftCooldownSec_ = 0.28;
    shiftStateHoldSec_ = (transmissionCode == 2 || transmissionCode == 3) ? 0.16 : 0.0;
}

double VirtualEcuPhysicsEngine::interpolateEngineTorque(double rpm) const noexcept {
    if (bike_.torqueCurveCount <= 0) {
        return bike_.maxTorqueNm * 0.88;
    }
    const int n = std::min(bike_.torqueCurveCount, 16);
    if (rpm <= bike_.torqueRpmPoints[0]) {
        return bike_.torqueNmPoints[0];
    }
    if (rpm >= bike_.torqueRpmPoints[n - 1]) {
        const double over = rpm - bike_.torqueRpmPoints[n - 1];
        return std::max(0.0, bike_.torqueNmPoints[n - 1] * (1.0 - over / 900.0));
    }
    for (int i = 0; i < n - 1; ++i) {
        const double r0 = bike_.torqueRpmPoints[i];
        const double r1 = bike_.torqueRpmPoints[i + 1];
        if (rpm >= r0 && rpm <= r1) {
            const double span = std::max(1.0, r1 - r0);
            const double t = (rpm - r0) / span;
            return bike_.torqueNmPoints[i] + t * (bike_.torqueNmPoints[i + 1] - bike_.torqueNmPoints[i]);
        }
    }
    return bike_.maxTorqueNm;
}

void VirtualEcuPhysicsEngine::recomputeOptimalShiftRpms() noexcept {
    const int maxG = std::clamp(bike_.gearCount, 1, 6);
    for (int g = 0; g < maxG - 1; ++g) {
        const double iCurr = bike_.gearRatios[g];
        const double iNext = bike_.gearRatios[g + 1];
        if (iCurr <= 0.0 || iNext <= 0.0) {
            bike_.optimalShiftRpmPerGear[g] = bike_.redlineRpm;
            continue;
        }
        const double ratioStep = iNext / iCurr;
        const double startRpm = std::max(bike_.maxTorqueRpm, bike_.maxPowerRpm * 0.88);
        double optRpm = bike_.redlineRpm;

        for (double testRpm = startRpm; testRpm <= bike_.revLimiterRpm; testRpm += 25.0) {
            const double trqCurr = interpolateEngineTorque(testRpm) * iCurr;
            const double trqNext = interpolateEngineTorque(testRpm * ratioStep) * iNext;
            if (trqCurr <= trqNext) {
                optRpm = std::min(testRpm, bike_.redlineRpm);
                break;
            }
        }
        bike_.optimalShiftRpmPerGear[g] = optRpm;
    }
    for (int g = maxG - 1; g < 6; ++g) {
        bike_.optimalShiftRpmPerGear[g] = bike_.redlineRpm;
    }
}

VirtualEcuTelemetryOutput VirtualEcuPhysicsEngine::stepPhysics120Hz(
    int64_t timestampNanos,
    double dtSec,
    double trueSpeedMs,
    double longitudinalAccelMs2,
    double longitudinalJerkMs3,
    double leanAngleRad,
    double pitchAngleRad,
    double acousticRpm,
    double acousticConfidence
) noexcept {
    VirtualEcuTelemetryOutput out{};
    const double safeDt = std::clamp(dtSec, 0.001, 0.050);
    const double timeSec = static_cast<double>(timestampNanos) * 1e-9;
    const int maxG = std::clamp(bike_.gearCount, 1, 6);

    // 1. Otomatik Gündüz / Gece 500ms Yumuşak Geçiş
    const float nightStep = static_cast<float>(safeDt / 0.500);
    if (currentNightFactor_ < targetNightFactor_) {
        currentNightFactor_ = std::min(targetNightFactor_, currentNightFactor_ + nightStep);
    } else if (currentNightFactor_ > targetNightFactor_) {
        currentNightFactor_ = std::max(targetNightFactor_, currentNightFactor_ - nightStep);
    }
    out.nightModeFactor = currentNightFactor_;

    // 2. Kontak Açılış Selamlaması (Ignition Boot & Needle Sweep)
    if (ignitionBootPhaseCode_ != 4) {
        const double elapsedBootSec =
            static_cast<double>(timestampNanos - ignitionStartNanos_) * 1e-9;
        if (elapsedBootSec < 0.55) {
            ignitionBootPhaseCode_ = 1;
            out.ignitionBootPhaseCode = 1;
            out.ignitionSweepProgress = 0.0f;
            out.actualEngineRpm = 0.0;
            out.normalizedRpmRatio = 0.0;
            return out;
        } else if (elapsedBootSec < 1.35) {
            ignitionBootPhaseCode_ = 2;
            const float sweepUp = static_cast<float>((elapsedBootSec - 0.55) / 0.80);
            out.ignitionBootPhaseCode = 2;
            out.ignitionSweepProgress = std::clamp(sweepUp, 0.0f, 1.0f);
            out.actualEngineRpm = bike_.revLimiterRpm * static_cast<double>(out.ignitionSweepProgress);
            out.normalizedRpmRatio = out.actualEngineRpm / std::max(1000.0, bike_.redlineRpm);
            out.shiftLightStateCode = (out.ignitionSweepProgress > 0.85f) ? 3 : 1;
            out.shiftFlashPhaseOn = true;
            return out;
        } else if (elapsedBootSec < 2.10) {
            ignitionBootPhaseCode_ = 3;
            const float sweepDown = 1.0f - static_cast<float>((elapsedBootSec - 1.35) / 0.75);
            out.ignitionBootPhaseCode = 3;
            out.ignitionSweepProgress = std::clamp(sweepDown, 0.0f, 1.0f);
            out.actualEngineRpm = bike_.idleRpm +
                (bike_.revLimiterRpm - bike_.idleRpm) * static_cast<double>(out.ignitionSweepProgress);
            out.normalizedRpmRatio = out.actualEngineRpm / std::max(1000.0, bike_.redlineRpm);
            return out;
        } else {
            ignitionBootPhaseCode_ = 4;
        }
    }
    out.ignitionBootPhaseCode = 4;
    out.ignitionSweepProgress = 1.0f;

    // 3. Toroidal Dinamik Lastik Yarıçapı Modeli (R_eff)
    const double rUnloadedMm =
        ((bike_.rearTireRimInch * 25.4) * 0.5) +
        (bike_.rearTireWidthMm * (bike_.rearTireAspectPercent / 100.0));

    const double rLoadedMm = rUnloadedMm *
        (bike_.rearTireSquishFactor - 0.004 * (longitudinalAccelMs2 / GRAVITY));

    const double absLeanRad = std::clamp(std::abs(leanAngleRad), 0.0, 1.20);
    const double rLeanMm = rLoadedMm - bike_.rearTireCrownRadiusMm * (1.0 - std::cos(absLeanRad));

    const double rEffMm = std::max(
        160.0,
        rLeanMm * (1.0 + 1.8e-6 * (trueSpeedMs * trueSpeedMs))
    );
    out.effectiveTireRadiusMm = rEffMm;

    // 4. Tekerlek Kayması (Slip Ratio) ve Her Vites İçin Kinematik RPM Denklemi
    const double cosLean = std::max(0.30, std::cos(absLeanRad));
    const double slipLambda = std::max(
        0.0,
        (longitudinalAccelMs2 / GRAVITY) * (0.035 / cosLean)
    );
    out.slipRatioLambda = slipLambda;
    out.tractionControlActive = (slipLambda > 0.045);

    const double wheelRpm =
        (trueSpeedMs * (1.0 + slipLambda) * 60.0) /
        (2.0 * PI * (rEffMm / 1000.0));

    const double finalDriveRatio =
        static_cast<double>(bike_.rearSprocketTeeth) /
        static_cast<double>(std::max(1, bike_.frontSprocketTeeth));

    for (int n = 0; n < 6; ++n) {
        const double gRatio = (n < maxG) ? bike_.gearRatios[n] : bike_.gearRatios[maxG - 1];
        out.kinematicRpmPerGear[n] =
            wheelRpm * bike_.primaryReductionRatio * finalDriveRatio * gRatio;
    }

    // 5. Gerçek Zamanlı Vites, Vites Artırma/Düşürme ve Debriyaj Durum Makinesi
    if (shiftCooldownSec_ > 0.0) {
        shiftCooldownSec_ = std::max(0.0, shiftCooldownSec_ - safeDt);
    }
    if (shiftStateHoldSec_ > 0.0) {
        shiftStateHoldSec_ = std::max(0.0, shiftStateHoldSec_ - safeDt);
    }

    smoothedAccelMs2_ = 0.88 * smoothedAccelMs2_ + 0.12 * longitudinalAccelMs2;

    const double aeroDragForceN = 0.5 * 1.225 * bike_.dragCdA * trueSpeedMs * trueSpeedMs;
    const double rollingDragForceN = 0.015 * bike_.totalMassKg * GRAVITY;
    const double coastingDecelMs2 = -((aeroDragForceN + rollingDragForceN) / std::max(80.0, bike_.totalMassKg));

    // Gerçekçi rölanti mikro-salınımı (±16 RPM organik motor rölantisi)
    const double idlePulseRpm = bike_.idleRpm + 16.0 * std::sin(timeSec * 2.0 * PI * 3.2);
    double targetRpm = idlePulseRpm;

    if (manualForcedGear_ >= 0) {
        // Manuel / Yarı-Otomatik Vites veya Simülatör Modu
        currentGear_ = std::clamp(manualForcedGear_, 0, maxG);
        isClutchPulled_ = manualClutchOverride_;
        if (currentGear_ == 0) {
            transmissionStateCode_ = 0; // NEUTRAL
            targetRpm = (manualThrottleRpm_ > 0.0) ? manualThrottleRpm_ : idlePulseRpm;
        } else if (isClutchPulled_) {
            transmissionStateCode_ = 4; // CLUTCH_PULLED_COASTING
            targetRpm = (manualThrottleRpm_ > 0.0) ? manualThrottleRpm_ : idlePulseRpm;
        } else {
            transmissionStateCode_ = 1; // ENGAGED_IN_GEAR
            targetRpm = (manualThrottleRpm_ > 0.0)
                ? manualThrottleRpm_
                : std::clamp(out.kinematicRpmPerGear[currentGear_ - 1], idlePulseRpm, bike_.revLimiterRpm * 1.02);
        }
    } else if (trueSpeedMs < 0.58) {
        // Dururken (v < 2.1 km/h): Yeşil 'N' (Neutral), devir = rölanti (veya mikrofondan gaz)
        currentGear_ = 0;
        isClutchPulled_ = false;
        transmissionStateCode_ = 0; // NEUTRAL
        targetRpm = (acousticConfidence > 0.45 && acousticRpm > bike_.idleRpm * 1.15)
            ? acousticRpm
            : idlePulseRpm;
    } else {
        // Hareket Halinde (GPS hızı + Motor Mekanik Profilinden Akıllı Vites & Devir Hesabı):
        const bool isCoastingWithoutEngineBrake =
            (trueSpeedMs > 1.2) &&
            (std::abs(longitudinalAccelMs2 - coastingDecelMs2) < 0.35) &&
            (acousticConfidence > 0.50 && acousticRpm < bike_.idleRpm * 1.38);

        if (isCoastingWithoutEngineBrake) {
            isClutchPulled_ = true;
            transmissionStateCode_ = 4; // CLUTCH_PULLED_COASTING
            targetRpm = std::max(idlePulseRpm, acousticRpm);
        } else {
            isClutchPulled_ = false;
            if (currentGear_ == 0) {
                registerGearChange(1, 1);
            }

            // Sürüş agresiflik katsayısı alpha [0.0 = sakin sabit sürüş .. 1.0 = tam gaz ivmelenme]
            const double alphaAggression = std::clamp((smoothedAccelMs2_ - 0.15) / 2.20, 0.0, 1.0);

            // Mevcut viteste dinamik vites büyütme (Upshift) ve küçültme (Downshift) eşikleri
            const int gIdx = std::clamp(currentGear_ - 1, 0, maxG - 1);
            const double optGearRpm = bike_.optimalShiftRpmPerGear[gIdx];
            const double rpmRange = std::max(2000.0, optGearRpm - bike_.idleRpm);

            // Sakin sürüşte %56 devirde, orta gazda %75 devirde, tam gazda %96 (Shift-Light) devrinde vites büyütür
            const double dynamicUpshiftRpm =
                bike_.idleRpm + (0.56 + 0.40 * alphaAggression) * rpmRange;

            // Yavaşlarken devir %28-%40 altına indiğinde (ve alt vites kesiciye girmeyecekse) vites küçültür
            const double dynamicDownshiftRpm =
                bike_.idleRpm + (0.25 + 0.14 * alphaAggression) * (bike_.redlineRpm - bike_.idleRpm);

            if (shiftCooldownSec_ <= 0.0) {
                const double curRpm = out.kinematicRpmPerGear[gIdx];

                // 1) Akustik mikrofon yüksek güvenle bağlıysa ve komşu vitesle eşleşiyorsa
                if (acousticConfidence > 0.55) {
                    int bestAcousticGear = currentGear_;
                    double minDiff = std::abs(curRpm - acousticRpm);
                    for (int g = 1; g <= maxG; ++g) {
                        const double candRpm = out.kinematicRpmPerGear[g - 1];
                        if (candRpm < bike_.idleRpm * 0.85 || candRpm > bike_.revLimiterRpm * 1.02) continue;
                        const double diff = std::abs(candRpm - acousticRpm) + std::abs(g - currentGear_) * 220.0;
                        if (diff + 350.0 < minDiff) {
                            minDiff = diff;
                            bestAcousticGear = g;
                        }
                    }
                    if (bestAcousticGear != currentGear_) {
                        registerGearChange(bestAcousticGear, (bestAcousticGear > currentGear_) ? 2 : 3);
                    }
                }

                // 2) Hız + Dişli Oranları + İvme Tabanlı Sıralı Şanzıman Geçişi
                if (shiftCooldownSec_ <= 0.0) {
                    if (currentGear_ < maxG && curRpm >= dynamicUpshiftRpm) {
                        // Vites Büyüt (Upshift: 1->2->3->4->5->6)
                        registerGearChange(currentGear_ + 1, 2);
                    } else if (currentGear_ > 1 && curRpm <= dynamicDownshiftRpm) {
                        // Alt vitesteki karşılık devrini kontrol et (Motoru patlatmayacak / kesiciye sokmayacak ise düşür)
                        const double lowerGearRpm = out.kinematicRpmPerGear[currentGear_ - 2];
                        if (lowerGearRpm <= bike_.redlineRpm * 0.84) {
                            registerGearChange(currentGear_ - 1, 3);
                        }
                    }
                }
            }

            if (shiftStateHoldSec_ <= 0.0) {
                transmissionStateCode_ = 1; // ENGAGED_IN_GEAR
            }
            targetRpm = std::clamp(
                out.kinematicRpmPerGear[std::clamp(currentGear_ - 1, 0, maxG - 1)],
                idlePulseRpm,
                bike_.revLimiterRpm * 1.02
            );
        }
    }
    prevAccelMs2_ = longitudinalAccelMs2;

    // 6. Fazla Devir / Kesiciye Girme (OVER_REV / REV_LIMITER)
    bool isHittingRevLimiter = false;
    if (targetRpm >= bike_.revLimiterRpm) {
        isHittingRevLimiter = true;
        const double bounceWave = std::sin(timeSec * 2.0 * PI * 14.0);
        targetRpm = bike_.revLimiterRpm + (150.0 * bounceWave);
    }

    // 7. İkinci Dereceden Yay-Damper İbre Fiziği (Sub-stepped 2ms)
    const double prevRpm = currentDisplayedRpm_;
    const double omegaN = 45.0;
    const double zeta = 0.88;
    const int needleSubsteps = std::max(1, static_cast<int>(std::ceil(safeDt / 0.002)));
    const double h = safeDt / static_cast<double>(needleSubsteps);
    for (int s = 0; s < needleSubsteps; ++s) {
        const double rpmError = targetRpm - currentDisplayedRpm_;
        const double rpmAccel = (omegaN * omegaN * rpmError) - (2.0 * zeta * omegaN * currentRpmVelocity_);
        currentRpmVelocity_ += rpmAccel * h;
        currentDisplayedRpm_ += currentRpmVelocity_ * h;
    }
    currentDisplayedRpm_ = std::max(0.0, currentDisplayedRpm_);

    const double instantRpmDeriv = (currentDisplayedRpm_ - prevRpm) / safeDt;
    filteredRpmDerivative_ = 0.82 * filteredRpmDerivative_ + 0.18 * instantRpmDeriv;

    // Peak-Hold Tepe Devir İzi Güncellemesi (1.8 saniye asılı kalır)
    if (currentDisplayedRpm_ >= peakHoldRpm_) {
        peakHoldRpm_ = currentDisplayedRpm_;
        peakHoldTimerSec_ = 1.8;
    } else if (peakHoldTimerSec_ > 0.0) {
        peakHoldTimerSec_ -= safeDt;
    } else {
        peakHoldRpm_ = std::max(currentDisplayedRpm_, peakHoldRpm_ - 2800.0 * safeDt);
    }

    if (gearShiftAnimProgress_ < 1.0f) {
        gearShiftAnimProgress_ = std::min(1.0f, gearShiftAnimProgress_ + static_cast<float>(safeDt / 0.160));
    }

    // 8. Optimum Shift-Light Matematiği ve İnsan Reaksiyon Süresi Kompanzasyonu
    const int activeGearIdx = std::clamp(currentGear_ - 1, 0, maxG - 1);
    const double baseOptRpm = autoOptimalShiftMode_
        ? bike_.optimalShiftRpmPerGear[activeGearIdx]
        : manualStage2Rpm_;

    const double positiveRpmRate = std::max(0.0, filteredRpmDerivative_);
    const double reactionLeadRpm = std::min(1400.0, positiveRpmRate * humanReactionTimeSec_);

    const double dynamicStage2Rpm = autoOptimalShiftMode_
        ? std::max(bike_.idleRpm + 2000.0, baseOptRpm - reactionLeadRpm)
        : manualStage2Rpm_;

    const double dynamicStage1Rpm = autoOptimalShiftMode_
        ? std::max(bike_.idleRpm + 1200.0, dynamicStage2Rpm - stage1WarnDeltaRpm_)
        : manualStage1Rpm_;

    int shiftStateCode = 0;
    bool flashOn = false;
    bool colorInvert = false;

    if (isHittingRevLimiter || currentDisplayedRpm_ >= bike_.revLimiterRpm - 80.0) {
        shiftStateCode = 3; // OVER_REV_LIMITER (12 Hz)
        flashOn = (std::fmod(timeSec * 12.0, 1.0) < 0.5);
        colorInvert = flashOn;
    } else if (currentDisplayedRpm_ >= dynamicStage2Rpm) {
        shiftStateCode = 2; // STAGE_2_SHIFT_NOW (8 Hz)
        flashOn = (std::fmod(timeSec * 8.0, 1.0) < 0.5);
        colorInvert = flashOn;
    } else if (currentDisplayedRpm_ >= dynamicStage1Rpm) {
        shiftStateCode = 1; // STAGE_1_APPROACHING
        flashOn = true;
        colorInvert = false;
    }

    // 9. Sanal Dyno Tekerlek Torku ve Beygir Gücü Hesabı
    const double currentEngineTrq = interpolateEngineTorque(currentDisplayedRpm_);
    const double currentGearRatio = (currentGear_ >= 1 && currentGear_ <= maxG)
        ? bike_.gearRatios[currentGear_ - 1]
        : 0.0;
    const double throttleLoadFactor = (trueSpeedMs < 0.58)
        ? 0.0
        : std::clamp(0.22 + (smoothedAccelMs2_ + 0.5) / 4.5, 0.10, 1.0);

    const double wheelTorqueNm = (!isClutchPulled_ && currentGear_ > 0)
        ? (currentEngineTrq * throttleLoadFactor * bike_.primaryReductionRatio * finalDriveRatio * currentGearRatio * 0.92)
        : 0.0;
    const double enginePowerKw = (currentEngineTrq * throttleLoadFactor * currentDisplayedRpm_ * 2.0 * PI) / (60.0 * 1000.0);
    const double wheelHorsepower = (!isClutchPulled_ && currentGear_ > 0)
        ? (enginePowerKw * 1.34102209 * 0.90)
        : 0.0;

    out.currentGear = currentGear_;
    out.isNeutral = (currentGear_ == 0);
    out.isClutchPulled = isClutchPulled_;
    out.transmissionStateCode = transmissionStateCode_;
    out.actualEngineRpm = currentDisplayedRpm_;
    out.normalizedRpmRatio = currentDisplayedRpm_ / std::max(1000.0, bike_.redlineRpm);
    out.peakHoldRpm = peakHoldRpm_;
    out.rpmDerivativePerSec = filteredRpmDerivative_;
    out.stage1ShiftRpm = dynamicStage1Rpm;
    out.stage2ShiftRpm = dynamicStage2Rpm;
    out.shiftLightStateCode = shiftStateCode;
    out.shiftFlashPhaseOn = flashOn;
    out.fullColorInversionActive = colorInvert;
    out.gearShiftAnimProgress = gearShiftAnimProgress_;
    out.estimatedWheelHp = wheelHorsepower;
    out.estimatedWheelTorqueNm = wheelTorqueNm;

    // 10. Bosch 6D IMU Tarzı Teker Derece Ölçer & Teker Metre (Wheelie Analyzer)
    const double pitchRateRadSec = (pitchAngleRad - prevPitchRad_) / safeDt;
    prevPitchRad_ = pitchAngleRad;

    if (!wheelieActive_ && std::abs(pitchRateRadSec) < 0.21) {
        const double alphaSlope = std::clamp(safeDt / 2.5, 0.0, 0.05);
        roadSlopeBaselineRad_ = (1.0 - alphaSlope) * roadSlopeBaselineRad_ + alphaSlope * pitchAngleRad;
    }

    const double rawWheelieDeg = (pitchAngleRad - roadSlopeBaselineRad_) * (180.0 / PI);
    const double cleanWheelieDeg = std::clamp(rawWheelieDeg, 0.0, 72.0);
    const double absLeanDeg = std::abs(leanAngleRad) * (180.0 / PI);

    const double enterThresholdDeg = 5.5;
    const double exitThresholdDeg = 4.0;
    const bool canWheelie = (trueSpeedMs > 1.8) && (absLeanDeg < 22.0);

    if (!wheelieActive_) {
        if (canWheelie && cleanWheelieDeg >= enterThresholdDeg) {
            wheelieActive_ = true;
            currentWheelieDurationSec_ = safeDt;
            currentWheelieDistanceMeters_ = trueSpeedMs * safeDt;
            currentWheeliePeakAngleDeg_ = cleanWheelieDeg;
            wheelieSummaryHoldSec_ = 3.2;
        } else if (wheelieSummaryHoldSec_ > 0.0) {
            wheelieSummaryHoldSec_ = std::max(0.0, wheelieSummaryHoldSec_ - safeDt);
        }
    } else {
        if (canWheelie && cleanWheelieDeg >= exitThresholdDeg) {
            currentWheelieDurationSec_ += safeDt;
            currentWheelieDistanceMeters_ += trueSpeedMs * safeDt;
            currentWheeliePeakAngleDeg_ = std::max(currentWheeliePeakAngleDeg_, cleanWheelieDeg);

            maxWheelieAngleDeg_ = std::max(maxWheelieAngleDeg_, cleanWheelieDeg);
            longestWheelieDurationSec_ = std::max(longestWheelieDurationSec_, currentWheelieDurationSec_);
            longestWheelieDistanceMeters_ = std::max(longestWheelieDistanceMeters_, currentWheelieDistanceMeters_);
            wheelieSummaryHoldSec_ = 3.2;
        } else {
            wheelieActive_ = false;
        }
    }

    out.isWheelieActive = wheelieActive_;
    out.isWheelieSummaryVisible = wheelieActive_ || (wheelieSummaryHoldSec_ > 0.0);
    out.wheelieAngleDeg = wheelieActive_
        ? cleanWheelieDeg
        : ((wheelieSummaryHoldSec_ > 0.0) ? currentWheeliePeakAngleDeg_ : 0.0);
    out.currentWheelieDurationSec = currentWheelieDurationSec_;
    out.currentWheelieDistanceMeters = currentWheelieDistanceMeters_;
    out.maxWheelieAngleDeg = maxWheelieAngleDeg_;
    out.longestWheelieDurationSec = longestWheelieDurationSec_;
    out.longestWheelieDistanceMeters = longestWheelieDistanceMeters_;

    return out;
}

} // namespace oemcluster::physics
