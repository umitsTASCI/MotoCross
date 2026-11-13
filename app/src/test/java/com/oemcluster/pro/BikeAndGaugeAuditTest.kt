package com.oemcluster.pro

import com.oemcluster.pro.catalog.OemGaugeCatalog
import com.oemcluster.pro.data.BikePresetCatalog
import com.oemcluster.pro.domain.IgnitionBootPhase
import com.oemcluster.pro.domain.ShiftLightState
import com.oemcluster.pro.domain.TransmissionEngagementState
import com.oemcluster.pro.domain.UnifiedTelemetryState
import com.oemcluster.pro.i18n.AppLanguage
import com.oemcluster.pro.i18n.LocalizationManager
import com.oemcluster.pro.i18n.StringKey
import com.oemcluster.pro.runtime.DistanceFormatter
import com.oemcluster.pro.runtime.RideSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BikeAndGaugeAuditTest {

    @Test
    fun auditCatalogsAndMechanicalProfiles() {
        val issues = mutableListOf<String>()
        val forbiddenIds = setOf(
            "yamaha_tmax_560", "honda_xadv_750", "honda_forza_350",
            "honda_pcx_160", "honda_s2000_ap1_f1", "bmw_c400x_gt",
            "aprilia_sr_gt_200", "vespa_gts_300_supertech"
        )
        val forbiddenTerms = listOf(
            "scooter", "tmax", "forza", "pcx", "vespa", "x-adv", "xadv",
            "c400", "sr gt", "s2000", "burgman", "nmax", "aerox"
        )

        val specs = OemGaugeCatalog.allModels
        val bikes = BikePresetCatalog.all
        assertEquals("Expected 307 pure-motorcycle gauge models", 307, specs.size)
        assertEquals("Expected 32 OEM brands", 32, OemGaugeCatalog.allBrands.size)

        val seenSkinIds = HashSet<String>()
        specs.forEachIndexed { idx, spec ->
            if (spec.modelNumber != idx + 1) {
                issues.add("Gauge #${spec.modelNumber} (${spec.skinId}) expected modelNumber=${idx + 1}")
            }
            if (!seenSkinIds.add(spec.skinId)) {
                issues.add("Duplicate gauge skinId: ${spec.skinId}")
            }
            if (spec.skinId in forbiddenIds || forbiddenTerms.any {
                    spec.skinId.contains(it, ignoreCase = true) ||
                        spec.modelName.contains(it, ignoreCase = true)
                }
            ) {
                issues.add("Forbidden scooter/car in OemGaugeCatalog: ${spec.skinId} (${spec.brand} ${spec.modelName})")
            }
            if (spec.availableSubModes.isEmpty()) {
                issues.add("Empty availableSubModes on ${spec.skinId}")
            }
            if (spec.gaugeRedlineRpm <= 2000.0 || spec.gaugeRedlineRpm >= spec.gaugeMaxRpm) {
                issues.add("Invalid gauge RPM range on ${spec.skinId}: redline=${spec.gaugeRedlineRpm}, max=${spec.gaugeMaxRpm}")
            }
        }

        val seenBikeIds = HashSet<String>()
        bikes.forEach { bike ->
            if (!seenBikeIds.add(bike.profileId)) {
                issues.add("Duplicate bike profileId: ${bike.profileId}")
            }
            if (bike.profileId in forbiddenIds || forbiddenTerms.any {
                    bike.profileId.contains(it, ignoreCase = true) ||
                        bike.modelName.contains(it, ignoreCase = true)
                }
            ) {
                issues.add("Forbidden scooter/car in BikePresetCatalog: ${bike.profileId}")
            }
            if (bike.idleRpm <= 500.0 ||
                bike.maxTorqueRpm <= bike.idleRpm ||
                bike.maxPowerRpm < bike.maxTorqueRpm ||
                bike.maxPowerRpm >= bike.redlineRpm ||
                bike.redlineRpm >= bike.revLimiterRpm
            ) {
                issues.add(
                    "Invalid RPM curve on ${bike.profileId}: idle=${bike.idleRpm}, trq=${bike.maxTorqueRpm}, pwr=${bike.maxPowerRpm}, red=${bike.redlineRpm}, lim=${bike.revLimiterRpm}"
                )
            }
            val nGears = bike.effectiveGearCount
            if (nGears !in 4..8 || bike.gearRatios.size < nGears) {
                issues.add("Invalid gears on ${bike.profileId}: count=${bike.transmissionGearCount}, list=${bike.gearRatios.size}")
            } else {
                for (i in 0 until nGears) {
                    if (bike.gearRatios[i] <= 0.3) {
                        issues.add("Zero/negative gear ratio on ${bike.profileId} gear ${i + 1}")
                    }
                    if (i > 0 && bike.gearRatios[i] >= bike.gearRatios[i - 1]) {
                        issues.add("Non-decreasing gear ratio on ${bike.profileId}: g$i=${bike.gearRatios[i - 1]} vs g${i + 1}=${bike.gearRatios[i]}")
                    }
                }
            }
            if (bike.frontTire.widthMm <= 0 || bike.frontTire.rimDiameterInch <= 0) {
                issues.add("Invalid front tire on ${bike.profileId}: ${bike.frontTire}")
            }
            if (bike.rearTire.widthMm <= 0 || bike.rearTire.rimDiameterInch <= 0) {
                issues.add("Invalid rear tire on ${bike.profileId}: ${bike.rearTire}")
            }
            if (bike.displacementCc <= 45 || bike.boreMm <= 20.0 || bike.strokeMm <= 20.0 ||
                bike.compressionRatio < 6.0 || bike.wheelbaseMm < 1000 || bike.seatHeightMm < 550 ||
                bike.fuelTankCapacityLiters <= 2.0 || bike.avgFuelConsumptionLPer100Km <= 1.0 ||
                bike.bikeWetMassKg < 75.0
            ) {
                issues.add("Invalid physical spec on ${bike.profileId}")
            }
            if (bike.torqueCurve.size < 4) {
                issues.add("Short torque curve on ${bike.profileId}")
            }
        }

        val skinSet = specs.map { it.skinId }.toSet()
        val bikeSet = bikes.map { it.profileId }.toSet()
        val missingBikes = skinSet - bikeSet
        val extraBikes = bikeSet - skinSet
        if (missingBikes.isNotEmpty()) {
            issues.add("Missing presets for gauge skinIds (${missingBikes.size}): ${missingBikes.sorted()}")
        }
        if (extraBikes.isNotEmpty()) {
            issues.add("Extra presets without gauge (${extraBikes.size}): ${extraBikes.sorted()}")
        }

        assertTrue("Audit found ${issues.size} issues:\n" + issues.joinToString("\n"), issues.isEmpty())
    }

    @Test
    fun distanceFormatterAndSessionLifecycleWorkCorrectly() {
        assertEquals("100.0", DistanceFormatter.numeric(100.0, useMph = false, decimals = 1))
        assertEquals("km", DistanceFormatter.unit(useMph = false))
        assertEquals("mi", DistanceFormatter.unit(useMph = true))
        assertTrue(DistanceFormatter.numeric(100.0, useMph = true, decimals = 1).startsWith("62.1"))

        val session = RideSessionState()
        assertEquals(0.0, session.odometerKm, 1e-6)
        assertEquals(0.0, session.tripAKm, 1e-6)
        assertEquals(0.0, session.tripBKm, 1e-6)
        assertEquals(0.0, session.best0to60Sec, 1e-6)
        assertEquals(0.0, session.best0to100Sec, 1e-6)

        // Warm-up stationary ticks then acceleration 0 -> 108 km/h
        val speeds = listOf(0.0, 0.0, 4.0, 18.0, 38.0, 64.0, 86.0, 108.0)
        var tNs = 1_000_000_000L
        for (spd in speeds) {
            tNs += 250_000_000L
            session.update(
                dtSec = 0.25,
                t = UnifiedTelemetryState(
                    timestampNanos = tNs,
                    activeBikeProfileId = "ktm_390_duke_gen3",
                    activeBikeDisplayName = "KTM 390 Duke",
                    bikeIdleRpm = 1400.0,
                    bikeRedlineRpm = 10000.0,
                    bikeRevLimiterRpm = 10500.0,
                    trueSpeedMs = spd / 3.6,
                    trueSpeedKmh = spd,
                    displayedSpeedKmh = spd,
                    is299LockEngaged = false,
                    actualEngineRpm = if (spd < 1.0) 1400.0 else 7500.0,
                    normalizedRpmRatio = if (spd < 1.0) 0.14 else 0.75,
                    peakHoldRpm = 7500.0,
                    stage1ShiftRpm = 9000.0,
                    stage2ShiftRpm = 9700.0,
                    shiftLightState = ShiftLightState.NORMAL,
                    shiftFlashPhaseOn = false,
                    fullColorInversionActive = false,
                    currentGear = if (spd < 1.0) 0 else 2,
                    isNeutral = spd < 1.0,
                    isClutchPulled = false,
                    transmissionState = if (spd < 1.0) TransmissionEngagementState.NEUTRAL
                    else TransmissionEngagementState.ENGAGED_IN_GEAR,
                    leanAngleDeg = 18.0,
                    pitchAngleDeg = 0.0,
                    effectiveTireRadiusMm = 300.0,
                    estimatedWheelHorsepower = 42.0,
                    estimatedWheelTorqueNm = 36.0,
                    fuelLevelRatio = 0.78f,
                    nightModeTransitionFactor = 1f,
                    ignitionBootPhase = IgnitionBootPhase.RUNNING_LIVE,
                    gearConfidence = 0.99,
                    gpsSpeedAgeSec = 0.1,
                    gpsSpeedAccuracyMs = 0.2,
                    isStationary = spd < 1.0,
                    gpsSpeedSourceBlend = 1.0,
                    gpsSatelliteCount = 12,
                    gpsHorizontalAccuracyM = 3.0
                )
            )
        }

        assertTrue("Expected best0to60Sec > 0, got ${session.best0to60Sec}", session.best0to60Sec > 0.0)
        assertTrue(
            "Expected best0to100Sec (${session.best0to100Sec}) >= best0to60Sec (${session.best0to60Sec})",
            session.best0to100Sec >= session.best0to60Sec
        )
        assertTrue("Expected positive odometer distance", session.odometerKm > 0.0)

        session.setOdometerKm(1234.5)
        assertEquals(1234.5, session.odometerKm, 1e-6)

        session.resetTripA()
        assertEquals(0.0, session.tripAKm, 1e-6)
        session.resetTripB()
        assertEquals(0.0, session.tripBKm, 1e-6)
        session.resetPeaks()
        assertEquals(0.0, session.peakSpeedKmh, 1e-6)
        session.resetDragRecords()
        assertEquals(0.0, session.best0to60Sec, 1e-6)
        assertEquals(0.0, session.best0to100Sec, 1e-6)
        session.resetOdometer()
        assertEquals(0.0, session.odometerKm, 1e-6)
    }

    @Test
    fun localizationContainsNoStale270Count() {
        for (lang in AppLanguage.entries) {
            for (key in StringKey.entries) {
                val text = LocalizationManager.getString(key, lang)
                assertFalse("Found stale '270' in $lang/$key: $text", text.contains("270"))
            }
        }
    }

    @Test
    fun dragBestChangedNotLostWhen60And100HitSameFrameAndMovingStartSafe() {
        fun frame(spdKmh: Double, tNs: Long) = UnifiedTelemetryState(
            timestampNanos = tNs,
            trueSpeedMs = spdKmh / 3.6,
            trueSpeedKmh = spdKmh,
            displayedSpeedKmh = spdKmh,
            actualEngineRpm = if (spdKmh < 1.0) 1400.0 else 8000.0
        )

        // 1. Seed initial slow 0-60 (1.0s) and fast 0-100 (2.0s)
        val session = RideSessionState()
        session.update(0.1, frame(0.0, 100_000_000L))
        session.update(0.1, frame(15.0, 200_000_000L)) // > 8 km/h launch spike must start timer
        assertTrue("Expected drag run to arm & start after >8 km/h jump from stop", session.dragActive)
        session.update(0.1, frame(65.0, 300_000_000L))
        assertTrue(session.consumeBestChanged())

        // 2. Next run beats 0-60 AND is slower than best on 0-100 on the same frame
        val session2 = RideSessionState().apply {
            best0to60Sec = 2.0
            best0to100Sec = 0.05
        }
        session2.update(0.1, frame(0.0, 100_000_000L))
        session2.update(0.1, frame(12.0, 200_000_000L))
        session2.update(0.1, frame(105.0, 300_000_000L))
        assertEquals(0.1, session2.best0to60Sec, 1e-6)
        assertEquals(0.05, session2.best0to100Sec, 1e-6)
        assertTrue("bestChanged must remain true if 0-60 improved on same frame as non-record 0-100", session2.consumeBestChanged())

        // 3. Running at 120 km/h after timeout must not re-arm drag without stopping first
        val session3 = RideSessionState()
        for (i in 1..700) {
            session3.update(0.1, frame(120.0, i * 100_000_000L))
        }
        assertFalse("Must not re-arm drag run at 120 km/h without stopping", session3.dragActive)
    }

    @Test
    fun shiftBeeperMuteAndMechanicalShiftCurvesValid() {
        com.oemcluster.pro.runtime.ShiftBeeper.setVolume(0)
        com.oemcluster.pro.runtime.ShiftBeeper.test()
        com.oemcluster.pro.runtime.ShiftBeeper.setVolume(80)

        BikePresetCatalog.all.forEach { bike ->
            val v1 = bike.theoreticalSpeedKmh(1, bike.redlineRpm)
            val vTop = bike.theoreticalSpeedKmh(bike.effectiveGearCount, bike.redlineRpm)
            assertTrue("Expected positive tire radius on ${bike.profileId}", bike.rearTire.unloadedRadiusMm > 200.0)
            assertTrue("Expected top gear faster than 1st gear on ${bike.profileId}: v1=$v1 vTop=$vTop", vTop > v1)
            for (g in 1 until bike.effectiveGearCount) {
                val opt = bike.computeOptimalShiftRpmForGear(g)
                assertTrue(
                    "Invalid optimal shift RPM $opt on ${bike.profileId} gear $g",
                    opt >= bike.idleRpm && opt <= bike.revLimiterRpm
                )
            }
        }
    }

    @Test
    fun auditOemRedlineScaleAndSubModeFidelity() {
        val bikeById = BikePresetCatalog.all.associateBy { it.profileId }
        val mismatches = mutableListOf<String>()

        OemGaugeCatalog.allModels.forEach { spec ->
            val bike = bikeById[spec.skinId] ?: return@forEach
            val redDiff = kotlin.math.abs(spec.gaugeRedlineRpm - bike.redlineRpm)
            if (redDiff > 50.0) {
                mismatches.add("REDLINE_DIFF ${spec.skinId}: gauge=${spec.gaugeRedlineRpm} vs bike=${bike.redlineRpm} (lim=${bike.revLimiterRpm})")
            }
            if (spec.gaugeRedlineRpm > bike.revLimiterRpm) {
                mismatches.add("REDLINE_ABOVE_LIMITER ${spec.skinId}: gaugeRed=${spec.gaugeRedlineRpm} > lim=${bike.revLimiterRpm}")
            }
            if (spec.gaugeMaxRpm < bike.revLimiterRpm) {
                mismatches.add("SCALE_BELOW_LIMITER ${spec.skinId}: gaugeMax=${spec.gaugeMaxRpm} < lim=${bike.revLimiterRpm}")
            }
            if (spec.availableSubModes.size != spec.availableSubModes.distinct().size) {
                mismatches.add("DUP_SUBMODE ${spec.skinId}: ${spec.availableSubModes}")
            }
        }

        assertTrue(
            "OEM Gauge vs Bike fidelity mismatches (${mismatches.size}):\n" + mismatches.joinToString("\n"),
            mismatches.isEmpty()
        )
    }

    @Test
    fun localizationCompletenessAcrossAllLanguages() {
        val problems = mutableListOf<String>()
        for (lang in com.oemcluster.pro.i18n.AppLanguage.entries) {
            val missing = com.oemcluster.pro.i18n.LocalizationManager.missingKeysFor(lang)
            if (missing.isNotEmpty()) {
                problems.add("${lang.code} missing keys (${missing.size}): $missing")
            }
            for (key in com.oemcluster.pro.i18n.StringKey.entries) {
                val text = com.oemcluster.pro.i18n.LocalizationManager.getString(key, lang)
                if (text.isBlank() || text == key.name) {
                    problems.add("${lang.code}.${key.name} invalid value '$text'")
                }
            }
        }
        assertTrue("Localization problems (${problems.size}):\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
