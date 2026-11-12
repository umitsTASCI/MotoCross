package com.oemcluster.pro

import com.oemcluster.pro.catalog.OemGaugeCatalog
import com.oemcluster.pro.data.BikePresetCatalog
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
        val forbiddenTerms = listOf("scooter", "tmax", "forza", "pcx", "vespa", "x-adv", "xadv", "c400", "sr gt", "s2000")

        val specs = OemGaugeCatalog.allModels
        val bikes = BikePresetCatalog.all

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
}
