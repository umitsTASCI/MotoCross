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
    humanReactionTimeSec_ = std::clamp(humanReactionTimeSec, 0.0, 0.350);
    stage1WarnDeltaRpm_ = std::clamp(stage1WarnDeltaRpm, 300.0, 3000.0);
}

void VirtualEcuPhysicsEngine::triggerIgnitionNeedleSweep(int64_t currentTimestampNanos) noexcept {
    ignitionBootPhaseCode_ = 1; // OEM_SPLASH_LOGO
    ignitionStartNanos_ = currentTimestampNanos;
}

void VirtualEcuPhysicsEngine::updateAmbientLightLux(float ambientLux, int themeOverrideMode) noexcept {
    if (themeOverrideMode == 1) {
        // Her Zaman Gündüz Modu (Beyaz)
        targetNightFactor_ = 0.0f;
    } else if (themeOverrideMode == 2) {
        // Her Zaman Gece Modu (Siyah)
        targetNightFactor_ = 1.0f;
    } else {
        // Otomatik Işık Sensörü (TYPE_LIGHT): < 45 lux ise tünel/gece
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
            peakHoldTimerSec_ = 1.8; // 1.8 sn Peak-Hold
        }
    }
    manualForcedGear_ = forcedGear;
    manualClutchOverride_ = clutchPulled;
    manualThrottleRpm_ = throttleOverrideRpm;
}

void VirtualEcuPhysicsEngine::registerGearChange(int newGear, int transmissionCode) noexcept {
    const int maxGears = std::clamp(bike_.gearCount, 1, 8);
    newGear = std::clamp(newGear, 0, maxGears);
    if (newGear == currentGear_) return;

    // Bölüm 4.6: Vites büyütmede son tepe devir çizgisi 1.8 saniye asılı kalır (Peak-Hold)
    if (newGear > currentGear_) {
        peakHoldRpm_ = std::max(peakHoldRpm_, currentDisplayedRpm_);
        peakHoldTimerSec_ = 1.8;
    }
    currentGear_ = newGear;
    transmissionStateCode_ = transmissionCode;
    gearShiftAnimProgress_ = 0.0f;   // Vites rakamı geçiş animasyonu yeniden başlar
    shiftCooldownSec_ = (bike_.transmissionTypeCode == 1 || bike_.transmissionTypeCode == 2) ? 0.18 : 0.30;
    // UPSHIFT(2)/DOWNSHIFT(3) durumu renderer'ın görebilmesi için 150 ms tutulur
    shiftStateHoldSec_ = (transmissionCode == 2 || transmissionCode == 3) ? 0.15 : 0.0;
}

double VirtualEcuPhysicsEngine::interpolateEngineTorque(double rpm) const noexcept {
    if (bike_.torqueCurveCount <= 0) {
        return bike_.maxTorqueNm * 0.85;
    }
    const int n = std::min(bike_.torqueCurveCount, 16);
    if (rpm <= bike_.torqueRpmPoints[0]) {
        return bike_.torqueNmPoints[0];
    }
    if (rpm >= bike_.torqueRpmPoints[n - 1]) {
        const double over = rpm - bike_.torqueRpmPoints[n - 1];
        return std::max(0.0, bike_.torqueNmPoints[n - 1] * (1.0 - over / 800.0));
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
    // -------------------------------------------------------------------------
    // Bölüm 3.4.1: Matematiksel Optimum Vites Noktası (RPM_opt[n])
    // T_engine(RPM_opt[n]) * i_gear[n] =
    //     T_engine(RPM_opt[n] * (i_gear[n+1] / i_gear[n])) * i_gear[n+1]
    // -------------------------------------------------------------------------
    const int maxGears = std::clamp(bike_.gearCount, 1, 8);
    for (int g = 0; g < maxGears - 1; ++g) {
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
    for (int g = maxGears - 1; g < 8; ++g) {
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

    // -------------------------------------------------------------------------
    // Bölüm 4.7: Otomatik Gündüz / Gece (Day / Night) 500ms Yumuşak Geçiş
    // -------------------------------------------------------------------------
    const float nightStep = static_cast<float>(safeDt / 0.500); // 500ms geçiş süresi
    if (currentNightFactor_ < targetNightFactor_) {
        currentNightFactor_ = std::min(targetNightFactor_, currentNightFactor_ + nightStep);
    } else if (currentNightFactor_ > targetNightFactor_) {
        currentNightFactor_ = std::max(targetNightFactor_, currentNightFactor_ - nightStep);
    }
    out.nightModeFactor = currentNightFactor_;

    // -------------------------------------------------------------------------
    // Bölüm 4.1: Kontak Açılış Selamlaması (Ignition Boot & Needle Sweep)
    //   0.00s - 0.55s: OEM Açılış Logosu & İkaz Işıkları (Phase 1)
    //   0.55s - 1.35s: İbre 0 -> Redline, Hız 888/299 (Phase 2)
    //   1.35s - 2.10s: İbre Redline -> Rölanti/0 (Phase 3)
    //   > 2.10s      : Canlı Telemetri (Phase 4)
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Bölüm 3.1: Toroidal Dinamik Lastik Yarıçapı Modeli (R_eff)
    //  1. Yüksüz Yarıçap:
    //     R_unloaded = ((D_rim_inch * 25.4) / 2) + (W_mm * (AspectRatio_% / 100))
    //  2. Yük ve İvme Ezilmesi:
    //     R_loaded = R_unloaded * (k_squish - 0.004 * (a_{x,true} / g))
    //  3. Viraj Yatış Açısı Yarıçap Küçülmesi:
    //     R_lean(theta_lean) = R_loaded - r_crown * (1 - cos(|theta_lean|))
    //  4. Merkezkaç Lastik Büyümesi:
    //     R_eff = R_lean(theta_lean) * (1 + 1.8e-6 * v_true^2)
    // -------------------------------------------------------------------------
    const double rUnloadedMm =
        ((bike_.rearTireRimInch * 25.4) * 0.5) +
        (bike_.rearTireWidthMm * (bike_.rearTireAspectPercent / 100.0));

    const double rLoadedMm = rUnloadedMm *
        (bike_.rearTireSquishFactor - 0.004 * (longitudinalAccelMs2 / GRAVITY));

    const double absLeanRad = std::clamp(std::abs(leanAngleRad), 0.0, 1.20);
    const double rLeanMm = rLoadedMm - bike_.rearTireCrownRadiusMm * (1.0 - std::cos(absLeanRad));

    const double rEffMm = std::max(
        180.0,
        rLeanMm * (1.0 + 1.8e-6 * (trueSpeedMs * trueSpeedMs))
    );
    out.effectiveTireRadiusMm = rEffMm;

    // -------------------------------------------------------------------------
    // Bölüm 3.2: Tekerlek Kayması (Slip Ratio - lambda) ve Kinematik RPM Denklemi
    //  lambda = max(0, (a_{x,true} / g) * (0.035 / cos(theta_lean)))
    //  RPM_calc[n] = ((v_true * (1 + lambda) * 60) / (2 * pi * (R_eff / 1000)))
    //                * i_primary * (Z_rear / Z_front) * i_gear[n]
    // -------------------------------------------------------------------------
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

    const int maxGears = std::clamp(bike_.gearCount, 1, 8);
    for (int n = 0; n < 8; ++n) {
        const double gRatio = (n < maxGears) ? bike_.gearRatios[n] : bike_.gearRatios[maxGears - 1];
        out.kinematicRpmPerGear[n] =
            wheelRpm * bike_.primaryReductionRatio * finalDriveRatio * gRatio;
    }

    // -------------------------------------------------------------------------
    // Bölüm 3.3: Gerçek Zamanlı Vites, Vites Artırma/Düşürme ve Debriyaj Durum Makinesi
    // (GNSS hızı + şanzıman kinematiği + akustik doğrulama)
    //
    // Temel fikir: OBD/CAN yoksa vites, hız ve motor devri arasındaki kinematik
    // bağıntıdan başka bir yolla güvenilir biçimde ölçülemez. Bu yüzden:
    //   1. Her vites için RPM_kin = f(v_true) hesaplanır.
    //   2. Sürüş yönüne göre planlanmış vites değiştirme devirleri (RPM_opt)
    //      eşik olarak kullanılır: üst devirde büyüt, rölantiye yaklaşınca küçült.
    //   3. Akustik RPM varsa kinematik eşleşme ile doğrulanır ve düzeltilir.
    //   4. Sabit hızda (cruise) kısa vitesleme tespiti için ek histerezis uygulanır.
    // -------------------------------------------------------------------------
    if (shiftCooldownSec_ > 0.0) {
        shiftCooldownSec_ = std::max(0.0, shiftCooldownSec_ - safeDt);
    }
    if (shiftStateHoldSec_ > 0.0) {
        shiftStateHoldSec_ = std::max(0.0, shiftStateHoldSec_ - safeDt);
    }

    // Hava direnci ivmesi: a_drag = -(0.5 * rho * CdA * v^2) / m
    const double aeroDragForceN = 0.5 * 1.225 * bike_.dragCdA * trueSpeedMs * trueSpeedMs;
    const double rollingDragForceN = 0.015 * bike_.totalMassKg * GRAVITY;
    const double coastingDecelMs2 = -((aeroDragForceN + rollingDragForceN) / std::max(80.0, bike_.totalMassKg));

    const bool acousticValid =
        acousticConfidence > 0.50 &&
        acousticRpm > bike_.idleRpm * 0.80 &&
        acousticRpm < bike_.revLimiterRpm * 1.10;

    double targetRpm = bike_.idleRpm;
    int resolvedGear = currentGear_;
    double gearConfidence = 0.55;

    if (manualForcedGear_ >= 0) {
        // Kullanıcı / simülatör kontrol modu
        resolvedGear = std::clamp(manualForcedGear_, 0, maxGears);
        currentGear_ = resolvedGear;
        isClutchPulled_ = manualClutchOverride_;
        gearConfidence = 1.0;
        if (resolvedGear == 0) {
            transmissionStateCode_ = 0;
            targetRpm = (manualThrottleRpm_ > 0.0) ? manualThrottleRpm_ : bike_.idleRpm;
        } else if (isClutchPulled_) {
            transmissionStateCode_ = 4;
            targetRpm = (manualThrottleRpm_ > 0.0) ? manualThrottleRpm_ : bike_.idleRpm;
        } else {
            transmissionStateCode_ = 1;
            targetRpm = (manualThrottleRpm_ > 0.0)
                ? manualThrottleRpm_
                : std::max(bike_.idleRpm, out.kinematicRpmPerGear[resolvedGear - 1]);
        }
    } else if (trueSpeedMs < 0.75) {
        // Dururken: N (Neutral). Debriyaj çekiliyse de motor rölantide kalır.
        currentGear_ = 0;
        resolvedGear = 0;
        isClutchPulled_ = false;
        transmissionStateCode_ = 0;
        targetRpm = (acousticValid && acousticRpm > bike_.idleRpm) ? acousticRpm : bike_.idleRpm;
        gearConfidence = 1.0;
        cruiseTimerSec_ = 0.0;
        shiftConfirmSec_ = 0.0;
        pendingGear_ = 0;
    } else if (bike_.transmissionTypeCode == 3) {
        // CVT Varyatörlü Otomatik (Maxi-Scooter): Sürekli değişken kasnak oranı
        isClutchPulled_ = false;
        currentGear_ = 1;
        resolvedGear = 1;
        transmissionStateCode_ = 1;
        const double throttleEffort = std::clamp((longitudinalAccelMs2 + 0.25) / 2.60, 0.0, 1.0);
        const double lowRatio = std::max(0.5, bike_.gearRatios[0]);
        const double highRatio = std::max(0.4, bike_.gearRatios[maxGears - 1]);
        const double speedNorm = std::clamp(trueSpeedMs / 30.0, 0.0, 1.0);
        const double effRatio = lowRatio + (highRatio - lowRatio) * std::clamp(speedNorm - throttleEffort * 0.28, 0.0, 1.0);
        const double cvtKinRpm = wheelRpm * bike_.primaryReductionRatio * finalDriveRatio * effRatio;
        const double cvtBandRpm = bike_.idleRpm + 1600.0 + (bike_.maxPowerRpm - (bike_.idleRpm + 1600.0)) * throttleEffort;
        targetRpm = std::clamp(0.55 * cvtKinRpm + 0.45 * cvtBandRpm, bike_.idleRpm + 900.0, bike_.redlineRpm);
        gearConfidence = 0.96;
    } else {
        // ---------------------------------------------------------------------
        // Debriyaj çekili (coasting) algılama: motor freni yerine sadece hava/
        // yuvarlanma direnciyle yavaşlama + akustik devrin rölantiye düşmesi.
        // ---------------------------------------------------------------------
        const bool isCoastingWithoutEngineBrake =
            (trueSpeedMs > 1.2) &&
            (std::abs(longitudinalAccelMs2 - coastingDecelMs2) < 0.32) &&
            acousticValid && (acousticRpm < bike_.idleRpm * 1.45);

        if (isCoastingWithoutEngineBrake) {
            isClutchPulled_ = true;
            transmissionStateCode_ = 4;
            targetRpm = std::clamp(acousticRpm, bike_.idleRpm, bike_.revLimiterRpm * 0.5);
            gearConfidence = 0.55;
        } else {
            isClutchPulled_ = false;
            if (resolvedGear <= 0) {
                resolvedGear = 1;
                registerGearChange(1, 1);
            }
            resolvedGear = std::clamp(resolvedGear, 1, maxGears);

            // Sabit hız süresi: kısa vitesleme/cruise tespiti
            if (std::abs(longitudinalAccelMs2) < 0.20 && trueSpeedMs > 5.0) {
                cruiseTimerSec_ += safeDt;
            } else {
                cruiseTimerSec_ = 0.0;
            }

            const double usableRpmSpan = std::max(2000.0, bike_.redlineRpm - bike_.idleRpm);
            const double throttleEffort = std::clamp((longitudinalAccelMs2 - 0.15) / 3.10, 0.0, 1.0);
            const double brakingEffort = std::clamp((-longitudinalAccelMs2 - 0.80) / 3.50, 0.0, 1.0);

            const auto shiftUpRpmFor = [&](int g) -> double {
                if (g < 1 || g >= maxGears) return bike_.redlineRpm;
                const double peakShiftRpm = autoOptimalShiftMode_
                    ? bike_.optimalShiftRpmPerGear[g - 1]
                    : manualStage2Rpm_;
                const double streetShiftRpm = bike_.idleRpm + usableRpmSpan * 0.43;
                const double cruiseAdjust = (cruiseTimerSec_ > 1.6) ? (-usableRpmSpan * 0.05) : 0.0;
                const double adaptiveUp = streetShiftRpm +
                    (peakShiftRpm - streetShiftRpm) * std::pow(throttleEffort, 0.78) +
                    cruiseAdjust;
                return std::clamp(adaptiveUp, bike_.idleRpm + 1400.0, peakShiftRpm);
            };

            const auto shiftDownRpmFor = [&](int g) -> double {
                if (g <= 1 || g > maxGears) return bike_.idleRpm;
                const double iPrev = std::max(0.1, bike_.gearRatios[g - 2]);
                const double iCurr = std::max(0.1, bike_.gearRatios[g - 1]);
                const double stepRatio = iCurr / iPrev;
                const double hysteresisCeiling = shiftUpRpmFor(g - 1) * stepRatio * 0.84;
                const double baseDown = bike_.idleRpm +
                    usableRpmSpan * (0.15 + 0.09 * throttleEffort + 0.10 * brakingEffort);
                return std::clamp(
                    std::min(hysteresisCeiling, baseDown),
                    bike_.idleRpm * 1.12,
                    bike_.redlineRpm * 0.65
                );
            };

            // Akustik RPM ile en iyi kinematik vites eşleşmesi (varsa)
            int acousticGear = resolvedGear;
            double acousticBestCost = 1.0e9;
            if (acousticValid) {
                for (int g = 1; g <= maxGears; ++g) {
                    const double kin = out.kinematicRpmPerGear[g - 1];
                    if (kin < bike_.idleRpm * 0.55 || kin > bike_.revLimiterRpm * 1.08) continue;
                    const double cost = std::abs(kin - acousticRpm) / std::max(1500.0, acousticRpm);
                    if (cost < acousticBestCost) {
                        acousticBestCost = cost;
                        acousticGear = g;
                    }
                }
            }

            const double curKin = out.kinematicRpmPerGear[std::clamp(resolvedGear - 1, 0, maxGears - 1)];
            const double curUpRpm = shiftUpRpmFor(resolvedGear);
            const double downRpm = shiftDownRpmFor(resolvedGear);

            // Öncelik sırası: aşırı devir koruması > akustik doğrulama > ivme-adaptif kinematik vites modeli
            int desiredGear = resolvedGear;

            if (curKin > bike_.revLimiterRpm * 0.96 && resolvedGear < maxGears) {
                for (int g = resolvedGear + 1; g <= maxGears; ++g) {
                    desiredGear = g;
                    if (out.kinematicRpmPerGear[g - 1] <= shiftUpRpmFor(g) * 0.92) {
                        break;
                    }
                }
            } else if (acousticValid && shiftCooldownSec_ <= 0.0 && acousticGear != resolvedGear) {
                const double currentCost =
                    std::abs(curKin - acousticRpm) / std::max(1500.0, acousticRpm);
                if (acousticBestCost + 0.06 < currentCost) {
                    desiredGear = acousticGear;
                }
            }

            if (desiredGear == resolvedGear && shiftCooldownSec_ <= 0.0) {
                const bool notHardBraking = longitudinalAccelMs2 > -0.65;
                if (resolvedGear < maxGears && notHardBraking && curKin >= curUpRpm) {
                    desiredGear = resolvedGear + 1;
                } else if (resolvedGear > 1 && curKin <= downRpm) {
                    desiredGear = resolvedGear - 1;
                }
            }

            // Vites teyit süresi: Quickshifter/DCT ise 65 ms, manuel sıralı ise 115 ms
            const double reqConfirmSec = (bike_.transmissionTypeCode == 1 || bike_.transmissionTypeCode == 2) ? 0.065 : 0.115;
            if (desiredGear != resolvedGear) {
                if (pendingGear_ == desiredGear) {
                    shiftConfirmSec_ += safeDt;
                } else {
                    pendingGear_ = desiredGear;
                    shiftConfirmSec_ = 0.0;
                }
                if (shiftConfirmSec_ >= reqConfirmSec) {
                    registerGearChange(desiredGear, desiredGear > resolvedGear ? 2 : 3);
                    resolvedGear = currentGear_;
                    pendingGear_ = 0;
                    shiftConfirmSec_ = 0.0;
                    cruiseTimerSec_ = 0.0;
                }
            } else {
                pendingGear_ = 0;
                shiftConfirmSec_ = 0.0;
            }

            currentGear_ = std::clamp(resolvedGear, 0, maxGears);
            if (currentGear_ <= 0) {
                currentGear_ = 1;
            }

            const double finalKin = out.kinematicRpmPerGear[currentGear_ - 1];
            const double finalUpRpm = shiftUpRpmFor(currentGear_);

            if (acousticValid) {
                // Kinematik tahmin ile gerçek motor sesini harmanla: ibre ne yapay
                // ne de GPS gürültüsüyle zıplasın.
                targetRpm = 0.62 * finalKin + 0.38 * acousticRpm;
                const double relErr = std::abs(finalKin - acousticRpm) / std::max(1200.0, acousticRpm);
                gearConfidence = std::clamp(1.0 - relErr * 1.8, 0.35, 0.99);
            } else {
                const double lower = std::max(downRpm, bike_.idleRpm);
                const double margin = std::min(
                    std::abs(finalKin - lower),
                    std::abs(finalUpRpm - finalKin)
                ) / std::max(1000.0, finalUpRpm);
                gearConfidence = std::clamp(0.42 + margin * 0.9, 0.40, 0.92);
                if (cruiseTimerSec_ > 2.0) gearConfidence *= 0.88;
                targetRpm = finalKin;
            }

            targetRpm = std::clamp(targetRpm, bike_.idleRpm, bike_.revLimiterRpm * 1.02);

            if (shiftStateHoldSec_ <= 0.0) {
                transmissionStateCode_ = 1; // ENGAGED_IN_GEAR
            }
        }
    }
    gearConfidence_ = gearConfidence;
    prevAccelMs2_ = longitudinalAccelMs2;

    // -------------------------------------------------------------------------
    // Bölüm 4.5: Fazla Devir / Kesiciye Girme (OVER_REV / REV_LIMITER)
    // İbre gerçek ateşleme kesicisi gibi ±150 RPM aralığında kesici titremesi (Rev-Limiter Bounce) yapar!
    // -------------------------------------------------------------------------
    bool isHittingRevLimiter = false;
    if (targetRpm >= bike_.revLimiterRpm) {
        isHittingRevLimiter = true;
        const double bounceWave = std::sin(static_cast<double>(timestampNanos) * 1e-9 * 2.0 * PI * 14.0);
        targetRpm = bike_.revLimiterRpm + (150.0 * bounceWave);
    }

    // -------------------------------------------------------------------------
    // İkinci Dereceden Yay-Damper İbre Fiziği (120 Hz Gerçekçi İbre/Bar Yanıtı)
    // -------------------------------------------------------------------------
    const double prevRpm = currentDisplayedRpm_;
    const double omegaN = 42.0; // Doğal frekans [rad/s] - çok hızlı yarış takometresi tepkisi
    const double zeta = 0.88;   // Sönüm oranı
    // Yarı-örtük Euler + 2 ms alt adımlar: kare süresi uzasa bile (örn. 50 ms takılma)
    // omegaN * h << 2 kalır ve ibre sayısal olarak kararsızlaşıp salınmaz.
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

    // Vites Geçiş Animasyonu İlerlemesi (Bölüm 4.6)
    if (gearShiftAnimProgress_ < 1.0f) {
        gearShiftAnimProgress_ = std::min(1.0f, gearShiftAnimProgress_ + static_cast<float>(safeDt / 0.160));
    }

    // -------------------------------------------------------------------------
    // Bölüm 3.4: Optimum Vites Değiştirme (Shift-Light) Matematiği ve
    // İnsan Reaksiyon Süresi Kompanzasyonu (tau_human ≈ 0.18 s):
    //   RPM_stage2[n] = RPM_opt[n] - (d(RPM)/dt * tau_human)
    //   RPM_stage1[n] = RPM_stage2[n] - Delta_RPM_warn (Varsayılan: 1000 RPM önce)
    // -------------------------------------------------------------------------
    const int activeGearIdx = std::clamp(currentGear_ - 1, 0, maxGears - 1);
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

    // -------------------------------------------------------------------------
    // Bölüm 4.3, 4.4, 4.5: Shift-Light Evreleri ve Renk Terslemesi (Full Color Inversion)
    //   - Evre 1 (STAGE_1_APPROACHING): Ön uyarı
    //   - Evre 2 (STAGE_2_SHIFT_NOW): 8 Hz Flaşör + Tam Renk Terslemesi (Inversion)
    //   - Kesici (OVER_REV_LIMITER): 12 Hz Flaşör + Tam Kırmızı Alarm + ±150 RPM Bounce
    // -------------------------------------------------------------------------
    const double timeSec = static_cast<double>(timestampNanos) * 1e-9;
    int shiftStateCode = 0;
    bool flashOn = false;
    bool colorInvert = false;

    if (isHittingRevLimiter || currentDisplayedRpm_ >= bike_.revLimiterRpm - 80.0) {
        shiftStateCode = 3; // OVER_REV_LIMITER
        // 12 Hz Flaşör
        flashOn = (std::fmod(timeSec * 12.0, 1.0) < 0.5);
        colorInvert = flashOn;
    } else if (currentDisplayedRpm_ >= dynamicStage2Rpm) {
        shiftStateCode = 2; // STAGE_2_SHIFT_NOW
        // 8 Hz Flaşör ve Tam Renk Terslemesi (Full Color Inversion)
        flashOn = (std::fmod(timeSec * 8.0, 1.0) < 0.5);
        colorInvert = flashOn;
    } else if (currentDisplayedRpm_ >= dynamicStage1Rpm) {
        shiftStateCode = 1; // STAGE_1_APPROACHING
        flashOn = true;
        colorInvert = false;
    }

    // Sanal Dyno Tekerlek Torku ve Beygir Gücü Hesabı (Gerçek aktarma tipi verimiyle)
    const double currentEngineTrq = interpolateEngineTorque(currentDisplayedRpm_);
    const double currentGearRatio = (currentGear_ >= 1 && currentGear_ <= maxGears)
        ? bike_.gearRatios[currentGear_ - 1]
        : 0.0;
    const double eff = std::clamp(bike_.drivetrainEfficiency, 0.75, 0.98);
    const double wheelTorqueNm = (!isClutchPulled_ && currentGear_ > 0)
        ? (currentEngineTrq * bike_.primaryReductionRatio * finalDriveRatio * currentGearRatio * eff)
        : 0.0;
    const double enginePowerKw = (currentEngineTrq * currentDisplayedRpm_ * 2.0 * PI) / (60.0 * 1000.0);
    const double wheelHorsepower = (!isClutchPulled_ && currentGear_ > 0)
        ? (enginePowerKw * 1.34102209 * eff)
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
    out.gearConfidence = gearConfidence_;

    // -------------------------------------------------------------------------
    // Bosch 6D IMU Tarzı Teker Derece Ölçer & Teker Metre (Wheelie Analyzer)
    // Yokuş eğimini (roadSlopeBaselineRad_) gerçek ön teker kalkışından ayırır:
    //  - Teker aktif değilken ve hızlı burun kalkışı yokken (< 12°/s) yol eğimi
    //    yavaşça (tau = 2.5s) referans alınır.
    //  - Net teker açısı: wheelieAngleDeg = max(0°, (pitch - roadBaseline) * 180/pi)
    //  - >= 5.0° (başlangıç) / >= 4.0° (sürdürme) ve hız > 1.8 m/s iken süre (sn)
    //    ve mesafe (m = integral(v_true * dt)) gerçek zamanlı sayılır.
    // -------------------------------------------------------------------------
    const double pitchRateRadSec = (pitchAngleRad - prevPitchRad_) / safeDt;
    prevPitchRad_ = pitchAngleRad;

    if (!wheelieActive_ && std::abs(pitchRateRadSec) < 0.21) { // < ~12 deg/s
        const double alphaSlope = std::clamp(safeDt / 2.5, 0.0, 0.05);
        roadSlopeBaselineRad_ = (1.0 - alphaSlope) * roadSlopeBaselineRad_ + alphaSlope * pitchAngleRad;
    }

    const double rawWheelieDeg = (pitchAngleRad - roadSlopeBaselineRad_) * (180.0 / PI);
    const double cleanWheelieDeg = std::clamp(rawWheelieDeg, 0.0, 72.0);
    const double absLeanDeg = std::abs(leanAngleRad) * (180.0 / PI);

    const double enterThresholdDeg = 5.0;
    const double exitThresholdDeg = 3.8;
    const bool canWheelie = (trueSpeedMs > 1.8) && (absLeanDeg < 22.0);

    if (!wheelieActive_) {
        if (canWheelie && cleanWheelieDeg >= enterThresholdDeg) {
            wheelieActive_ = true;
            currentWheelieDurationSec_ = safeDt;
            currentWheelieDistanceMeters_ = trueSpeedMs * safeDt;
            currentWheeliePeakAngleDeg_ = cleanWheelieDeg;
            wheelieSummaryHoldSec_ = 3.2; // Teker bittikten sonra 3.2 sn özet kartı görünür kalır
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

