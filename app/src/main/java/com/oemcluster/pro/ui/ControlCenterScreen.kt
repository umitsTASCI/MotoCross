package com.oemcluster.pro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oemcluster.pro.catalog.DeclarativeGaugeSpec
import com.oemcluster.pro.catalog.GaugeDisplayCategory
import com.oemcluster.pro.catalog.OemGaugeCatalog
import com.oemcluster.pro.data.BikeDatabaseRepository
import com.oemcluster.pro.data.BikePresetCatalog
import com.oemcluster.pro.data.ClusterPreferences
import com.oemcluster.pro.domain.BikeMechanicalProfile
import com.oemcluster.pro.domain.EngineFiringConfig
import com.oemcluster.pro.domain.FinalDriveType
import com.oemcluster.pro.domain.TransmissionType
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
import com.oemcluster.pro.runtime.DistanceFormatter
import com.oemcluster.pro.runtime.RideSessionState
import com.oemcluster.pro.runtime.SimulationController
import com.oemcluster.pro.sensors.NativeTelemetryEngineBridge
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

/**
 * MotoCross Kontrol Merkezi — garaj, 270 gösterge galerisi, ayarlar,
 * sürüş verileri ve canlı simülatör tek bir kokpit panelinde.
 */
@Composable
fun ControlCenterScreen(
    prefs: ClusterPreferences,
    onPrefsChange: (transform: (ClusterPreferences) -> ClusterPreferences) -> Unit,
    activeBike: BikeMechanicalProfile,
    garageBikes: List<BikeMechanicalProfile>,
    liveTelemetry: UnifiedTelemetryState,
    language: AppLanguage,
    session: RideSessionState,
    rideRevision: Int,
    simulation: SimulationController,
    initialTab: Int,
    onClose: () -> Unit,
    onSelectGauge: (Int) -> Unit,
    onSelectBike: (String) -> Unit,
    onSaveBike: (BikeMechanicalProfile) -> Unit,
    onDeleteBike: (String) -> Unit,
    onRestoreBike: (String) -> Unit,
    onResetGarage: () -> Unit,
    onZeroCalibrateNow: () -> Unit,
    onReplayLaunch: () -> Unit,
    onResetOdometer: () -> Unit
) {
    var tab by remember { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
    val currentSpec = OemGaugeCatalog.allModels[
        prefs.selectedGaugeIndex.coerceIn(0, OemGaugeCatalog.allModels.lastIndex)
    ]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CcColors.bg)
    ) {
        ControlCenterHeader(
            prefs = prefs,
            activeBike = activeBike,
            currentSpec = currentSpec,
            language = language,
            tab = tab,
            onTab = { tab = it },
            onClose = onClose
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            when (tab) {
                0 -> GarageTab(
                    activeBike = activeBike,
                    garageBikes = garageBikes,
                    onSelectBike = onSelectBike,
                    onSaveBike = onSaveBike,
                    onDeleteBike = onDeleteBike,
                    onRestoreBike = onRestoreBike
                )
                1 -> GalleryTab(
                    prefs = prefs,
                    language = language,
                    onSelectGauge = onSelectGauge,
                    onToggleFavorite = { skinId ->
                        onPrefsChange { p ->
                            val favs = p.favoriteSkinIds.toMutableList()
                            if (!favs.remove(skinId)) favs.add(skinId)
                            p.copy(favoriteSkinIds = favs)
                        }
                    }
                )
                2 -> SettingsTab(
                    prefs = prefs,
                    onPrefsChange = onPrefsChange,
                    telemetry = liveTelemetry,
                    language = language,
                    onZeroCalibrateNow = onZeroCalibrateNow,
                    onResetGarage = onResetGarage,
                    onReplayLaunch = onReplayLaunch,
                    onResetOdometer = onResetOdometer
                )
                3 -> RideDataTab(
                    session = session,
                    telemetry = liveTelemetry,
                    rideRevision = rideRevision,
                    activeBike = activeBike,
                    useMph = prefs.useMph,
                    onResetTripA = session::resetTripA,
                    onResetTripB = session::resetTripB,
                    onResetPeaks = session::resetPeaks,
                    onResetDrag = session::resetDragRecords
                )
                else -> SimulatorTab(
                    simulation = simulation,
                    activeBike = activeBike,
                    spec = currentSpec,
                    prefs = prefs
                )
            }
        }
    }
}

@Composable
private fun ControlCenterHeader(
    prefs: ClusterPreferences,
    activeBike: BikeMechanicalProfile,
    currentSpec: DeclarativeGaugeSpec,
    language: AppLanguage,
    tab: Int,
    onTab: (Int) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(CcColors.panel, lerp(CcColors.bg, Color.Black, 0.35f))
                )
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(listOf(CcColors.orange, CcColors.orange.copy(alpha = 0.35f))),
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(Modifier.width(10.dp))
            UstlabsAtomMark(
                modifier = Modifier.size(34.dp),
                reveal = 1f
            )
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MOTOCROSS",
                    color = CcColors.text,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "USTLABS  ·  CONTROL CENTER  ·  270 OEM CLUSTERS  ·  VIRTUAL ECU",
                    color = CcColors.muted,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.sp
                )
            }
            ActiveBikeBadge(activeBike)
            Spacer(Modifier.width(10.dp))
            Surface(
                color = Color(0xFF2A0E12),
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, CcColors.red.copy(alpha = 0.55f)),
                modifier = Modifier.clickable { onClose() }
            ) {
                Text(
                    text = "✕  KADRAJA DÖN",
                    color = Color(0xFFFF8A80),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(11.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                val tabs = listOf(
                    "🏍  " + LocalizationManager.getString(StringKey.MENU_MY_GARAGE, language)
                        .substringBefore(" ("),
                    "🎨  " + LocalizationManager.getString(StringKey.MENU_DASHBOARD_GALLERY, language)
                        .substringBefore(" (") + " · 270",
                    "⚙  " + LocalizationManager.getString(StringKey.MENU_SETTINGS, language),
                    "🏁  " + LocalizationManager.getString(StringKey.MENU_TRIP_TELEMETRY, language)
                        .substringBefore(" &"),
                    "🎛  SİMÜLATÖR"
                )
                itemsIndexed(tabs) { index, title ->
                    val selected = tab == index
                    Surface(
                        color = if (selected) CcColors.orange else CcColors.panel2,
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(
                            1.dp,
                            if (selected) CcColors.orange else CcColors.border
                        ),
                        shadowElevation = if (selected) 4.dp else 0.dp,
                        modifier = Modifier.clickable { onTab(index) }
                    ) {
                        Text(
                            text = title,
                            color = if (selected) Color.Black else CcColors.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBikeBadge(bike: BikeMechanicalProfile) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(CcColors.green.copy(alpha = 0.12f), CcColors.panel2)
                )
            )
            .border(1.dp, CcColors.green.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(CcColors.green, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(7.dp))
        Column {
            Text(
                text = "AKTİF MOTOR",
                color = CcColors.green.copy(alpha = 0.85f),
                fontSize = 7.5.sp,
                letterSpacing = 1.1.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${bike.brand} ${bike.modelName}",
                color = CcColors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 230.dp)
            )
        }
    }
}

// =====================================================================
// GARAJ
// =====================================================================

@Composable
private fun GarageTab(
    activeBike: BikeMechanicalProfile,
    garageBikes: List<BikeMechanicalProfile>,
    onSelectBike: (String) -> Unit,
    onSaveBike: (BikeMechanicalProfile) -> Unit,
    onDeleteBike: (String) -> Unit,
    onRestoreBike: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var editingId by remember(activeBike.profileId) { mutableStateOf(activeBike.profileId) }
    val editing = garageBikes.firstOrNull { it.profileId == editingId } ?: activeBike
    val isFactory = BikeDatabaseRepository.isFactoryPreset(editing.profileId)
    var editSession by remember { mutableIntStateOf(0) }

    var brandText by remember(editing, editSession) { mutableStateOf(editing.brand) }
    var modelText by remember(editing, editSession) { mutableStateOf(editing.modelName) }
    var yearText by remember(editing, editSession) { mutableStateOf(editing.modelYear.toString()) }
    var engineIndex by remember(editing, editSession) {
        mutableIntStateOf(EngineFiringConfig.entries.indexOf(editing.engineConfig).coerceAtLeast(0))
    }
    var idleText by remember(editing, editSession) { mutableStateOf(editing.idleRpm.roundToInt().toString()) }
    var torqueRpmText by remember(editing, editSession) { mutableStateOf(editing.maxTorqueRpm.roundToInt().toString()) }
    var torqueNmText by remember(editing, editSession) { mutableStateOf(editing.maxTorqueNm.roundToInt().toString()) }
    var powerRpmText by remember(editing, editSession) { mutableStateOf(editing.maxPowerRpm.roundToInt().toString()) }
    var powerHpText by remember(editing, editSession) { mutableStateOf(editing.maxPowerHp.roundToInt().toString()) }
    var redlineText by remember(editing, editSession) { mutableStateOf(editing.redlineRpm.roundToInt().toString()) }
    var limiterText by remember(editing, editSession) { mutableStateOf(editing.revLimiterRpm.roundToInt().toString()) }
    var cdaText by remember(editing, editSession) { mutableStateOf(String.format(Locale.US, "%.2f", editing.dragCoefficientAreaCdA)) }
    var frontTeethText by remember(editing, editSession) { mutableStateOf(editing.frontSprocketTeeth.toString()) }
    var rearTeethText by remember(editing, editSession) { mutableStateOf(editing.rearSprocketTeeth.toString()) }
    var primaryText by remember(editing, editSession) { mutableStateOf(String.format(Locale.US, "%.3f", editing.primaryReductionRatio)) }
    var gearCount by remember(editing, editSession) {
        mutableIntStateOf(editing.transmissionGearCount.coerceIn(4, 8))
    }
    var transmissionTypeIndex by remember(editing, editSession) {
        mutableIntStateOf(TransmissionType.entries.indexOf(editing.transmissionType).coerceAtLeast(0))
    }
    var finalDriveIndex by remember(editing, editSession) {
        mutableIntStateOf(FinalDriveType.entries.indexOf(editing.finalDriveType).coerceAtLeast(0))
    }
    var slipperClutch by remember(editing, editSession) { mutableStateOf(editing.hasSlipperClutch) }
    var displacementText by remember(editing, editSession) { mutableStateOf(editing.displacementCc.toString()) }
    var boreText by remember(editing, editSession) { mutableStateOf(String.format(Locale.US, "%.1f", editing.boreMm)) }
    var strokeText by remember(editing, editSession) { mutableStateOf(String.format(Locale.US, "%.1f", editing.strokeMm)) }
    var compressionText by remember(editing, editSession) { mutableStateOf(String.format(Locale.US, "%.1f", editing.compressionRatio)) }
    var wheelbaseText by remember(editing, editSession) { mutableStateOf(editing.wheelbaseMm.roundToInt().toString()) }
    var seatHeightText by remember(editing, editSession) { mutableStateOf(editing.seatHeightMm.roundToInt().toString()) }
    var fuelTankText by remember(editing, editSession) { mutableStateOf(String.format(Locale.US, "%.1f", editing.fuelTankCapacityLiters)) }
    var consumptionText by remember(editing, editSession) { mutableStateOf(String.format(Locale.US, "%.1f", editing.avgFuelConsumptionLPer100Km)) }
    var wetMassText by remember(editing, editSession) { mutableStateOf(editing.bikeWetMassKg.roundToInt().toString()) }
    var riderMassText by remember(editing, editSession) { mutableStateOf(editing.riderAndCargoMassKg.roundToInt().toString()) }
    var gearTexts by remember(editing, editSession) {
        mutableStateOf((0 until 8).map { idx -> String.format(Locale.US, "%.3f", editing.gearRatios.getOrElse(idx) { 1.0 }) })
    }
    var rearTireText by remember(editing, editSession) { mutableStateOf(editing.rearTire.toSpecString()) }
    var frontTireText by remember(editing, editSession) { mutableStateOf(editing.frontTire.toSpecString()) }
    var confirmDelete by remember(editing.profileId) { mutableStateOf(false) }
    var savedFlash by remember { mutableStateOf(false) }

    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(1600)
            savedFlash = false
        }
    }

    fun number(text: String, fallback: Double): Double =
        text.replace(',', '.').trim().toDoubleOrNull() ?: fallback

    fun integer(text: String, fallback: Int): Int =
        text.trim().toIntOrNull() ?: fallback

    fun tire(text: String, fallback: TireSpecification): TireSpecification {
        val looksValid = Regex("""\d{3}\s*/\s*\d{2}\s*[A-Za-z]*\s*\d{2}""").containsMatchIn(text)
        return if (looksValid) {
            TireSpecification.parse(text, fallback.staticSquishFactor)
        } else {
            fallback
        }
    }

    fun buildDraft(): BikeMechanicalProfile {
        val count = gearCount.coerceIn(1, 8)
        val ratios = (0 until count).map { index ->
            number(
                gearTexts.getOrElse(index) { "1.000" },
                editing.gearRatios.getOrElse(index) { 1.0 }
            ).coerceIn(0.30, 4.80)
        }
        val wetMass = number(wetMassText, editing.bikeWetMassKg).coerceIn(65.0, 500.0)
        val riderMass = number(riderMassText, editing.riderAndCargoMassKg).coerceIn(40.0, 220.0)

        val draft = editing.copy(
            brand = brandText.trim().ifEmpty { editing.brand },
            modelName = modelText.trim().ifEmpty { editing.modelName },
            modelYear = integer(yearText, editing.modelYear).coerceIn(1900, 2100),
            engineConfig = EngineFiringConfig.entries[engineIndex.coerceIn(0, EngineFiringConfig.entries.lastIndex)],
            frontSprocketTeeth = integer(frontTeethText, editing.frontSprocketTeeth).coerceIn(8, 80),
            rearSprocketTeeth = integer(rearTeethText, editing.rearSprocketTeeth).coerceIn(8, 90),
            primaryReductionRatio = number(primaryText, editing.primaryReductionRatio).coerceIn(0.8, 3.5),
            gearRatios = ratios,
            transmissionGearCount = count,
            transmissionType = TransmissionType.entries.getOrElse(transmissionTypeIndex) { TransmissionType.MANUAL_SEQUENTIAL },
            finalDriveType = FinalDriveType.entries.getOrElse(finalDriveIndex) { FinalDriveType.CHAIN },
            hasSlipperClutch = slipperClutch,
            displacementCc = integer(displacementText, editing.displacementCc).coerceIn(50, 2500),
            boreMm = number(boreText, editing.boreMm).coerceIn(20.0, 150.0),
            strokeMm = number(strokeText, editing.strokeMm).coerceIn(20.0, 150.0),
            compressionRatio = number(compressionText, editing.compressionRatio).coerceIn(5.0, 25.0),
            wheelbaseMm = number(wheelbaseText, editing.wheelbaseMm).coerceIn(1200.0, 2300.0),
            seatHeightMm = number(seatHeightText, editing.seatHeightMm).coerceIn(600.0, 1050.0),
            fuelTankCapacityLiters = number(fuelTankText, editing.fuelTankCapacityLiters).coerceIn(2.0, 60.0),
            avgFuelConsumptionLPer100Km = number(consumptionText, editing.avgFuelConsumptionLPer100Km).coerceIn(1.0, 30.0),
            bikeWetMassKg = wetMass,
            riderAndCargoMassKg = riderMass,
            totalMassKg = (wetMass + riderMass).coerceIn(80.0, 600.0),
            rearTire = tire(rearTireText, editing.rearTire),
            frontTire = tire(frontTireText, editing.frontTire),
            idleRpm = number(idleText, editing.idleRpm).coerceIn(600.0, 3000.0),
            maxTorqueRpm = number(torqueRpmText, editing.maxTorqueRpm).coerceIn(2000.0, 15000.0),
            maxTorqueNm = number(torqueNmText, editing.maxTorqueNm).coerceIn(10.0, 250.0),
            maxPowerRpm = number(powerRpmText, editing.maxPowerRpm).coerceIn(3000.0, 17000.0),
            maxPowerHp = number(powerHpText, editing.maxPowerHp).coerceIn(5.0, 350.0),
            redlineRpm = number(redlineText, editing.redlineRpm).coerceIn(4000.0, 18000.0),
            revLimiterRpm = number(limiterText, editing.revLimiterRpm).coerceIn(4500.0, 19000.0),
            dragCoefficientAreaCdA = number(cdaText, editing.dragCoefficientAreaCdA).coerceIn(0.20, 0.90)
        )
        return BikePresetCatalog.withRegeneratedTorqueCurve(
            draft.copy(revLimiterRpm = draft.revLimiterRpm.coerceAtLeast(draft.redlineRpm + 150.0))
        )
    }

    val draft = buildDraft()
    val filtered = remember(garageBikes, query) {
        val q = query.trim()
        if (q.isBlank()) garageBikes
        else garageBikes.filter {
            it.brand.contains(q, ignoreCase = true) ||
                it.modelName.contains(q, ignoreCase = true) ||
                it.profileId.contains(q, ignoreCase = true)
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // -----------------------------------------------------------------
        // Sol: garaj listesi
        // -----------------------------------------------------------------
        Column(
            modifier = Modifier
                .weight(0.38f)
                .fillMaxHeight()
        ) {
            CcTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Garajda ara: marka, model, profil kimliği",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filtered.size} motor  ·  ${garageBikes.count { !BikeDatabaseRepository.isFactoryPreset(it.profileId) }} özel profil",
                    color = CcColors.muted,
                    fontSize = 10.sp
                )
                CcSmallButton(
                    text = "+ YENİ PROFİL",
                    onClick = {
                        val newId = "custom_" + System.currentTimeMillis()
                        val fresh = BikePresetCatalog.withRegeneratedTorqueCurve(
                            activeBike.copy(
                                profileId = newId,
                                brand = activeBike.brand,
                                modelName = "Yeni Profil",
                                modelYear = 2026
                            )
                        )
                        onSaveBike(fresh)
                        editingId = newId
                    }
                )
            }
            Spacer(Modifier.height(7.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                lazyItems(filtered, key = { it.profileId }) { bike ->
                    val selectedForEdit = bike.profileId == editing.profileId
                    val isActive = bike.profileId == activeBike.profileId
                    Surface(
                        color = when {
                            selectedForEdit -> Color(0xFF241708)
                            isActive -> Color(0xFF12211A)
                            else -> CcColors.panel
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            if (selectedForEdit) 2.dp else 1.dp,
                            when {
                                selectedForEdit -> CcColors.orange
                                isActive -> CcColors.green.copy(alpha = 0.65f)
                                else -> CcColors.border
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingId = bike.profileId }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${bike.brand} ${bike.modelName}",
                                    color = CcColors.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${bike.modelYear}  ·  ${bike.engineConfig.cylinderCount} sil.  ·  " +
                                        "${bike.frontSprocketTeeth}/${bike.rearSprocketTeeth}  ·  ${bike.gearRatios.size} vites",
                                    color = CcColors.muted,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isActive) {
                                Text(
                                    text = "AKTİF",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CcColors.green)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // Sağ: seçili motorun tam mekanik editörü
        // -----------------------------------------------------------------
        LazyColumn(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                CcPanel(
                    title = "${editing.brand} ${editing.modelName}",
                    subtitle = if (isFactory) {
                        "Fabrika profili  ·  düzenlemeler bu cihazda saklanır  ·  ${editing.profileId}"
                    } else {
                        "Özel profil  ·  ${editing.profileId}"
                    }
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editing.profileId != activeBike.profileId) {
                            CcSmallButton(
                                text = "● AKTİF MOTOR YAP",
                                onClick = { onSelectBike(editing.profileId) }
                            )
                        }
                        Button(
                            onClick = {
                                onSaveBike(buildDraft())
                                savedFlash = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (savedFlash) CcColors.green else CcColors.orange
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (savedFlash) "KAYDEDİLDİ ✓" else "KAYDET VE UYGULA",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        CcSmallButton(
                            text = "DEĞİŞİKLİKLERİ GERİ AL",
                            onClick = { editSession++ }
                        )
                        if (isFactory) {
                            CcSmallButton(
                                text = "FABRİKA DEĞERLERİNE DÖN",
                                onClick = { onRestoreBike(editing.profileId) }
                            )
                        }
                        CcSmallButton(
                            text = "SİL",
                            onClick = { confirmDelete = true },
                            danger = true
                        )
                    }
                    if (confirmDelete) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2A0E12))
                                .padding(9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isFactory) {
                                    "Bu fabrika profili garajdan kaldırılacak. Ayarlar menüsünden fabrika garajına dönebilirsiniz."
                                } else {
                                    "Bu özel profil kalıcı olarak silinecek."
                                },
                                color = Color(0xFFFF8A80),
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            CcSmallButton(
                                text = "EMİNİM, SİL",
                                onClick = {
                                    confirmDelete = false
                                    onDeleteBike(editing.profileId)
                                },
                                danger = true
                            )
                            CcSmallButton(text = "VAZGEÇ", onClick = { confirmDelete = false })
                        }
                    }
                }
            }

            item {
                CcPanel(
                    title = "🏷  KİMLİK VE MOTOR MİMARİSİ",
                    subtitle = "Marka, model, yıl ve ateşleme düzeni akustik devir analizini de belirler"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GarageField("MARKA", brandText, { brandText = it }, Modifier.weight(0.9f))
                        GarageField("MODEL", modelText, { modelText = it }, Modifier.weight(1.5f))
                        GarageField("YIL", yearText, { yearText = it }, Modifier.weight(0.6f))
                    }
                    Spacer(Modifier.height(9.dp))
                    CcChoiceRow(
                        title = "Motor / ateşleme düzeni",
                        options = EngineFiringConfig.entries.map { it.displayName },
                        selectedIndex = engineIndex,
                        onSelect = { engineIndex = it }
                    )
                }
            }

            item {
                CcPanel(
                    title = "🔥  MOTOR EĞRİSİ VE LİMİTLER",
                    subtitle = "Tork eğrisi bu tepe değerlerinden fiziksel P = T·ω bağıntısıyla yeniden üretilir"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GarageField("RÖLANTİ", idleText, { idleText = it }, Modifier.weight(1f))
                        GarageField("MAKS TORK RPM", torqueRpmText, { torqueRpmText = it }, Modifier.weight(1f))
                        GarageField("MAKS TORK Nm", torqueNmText, { torqueNmText = it }, Modifier.weight(1f))
                        GarageField("MAKS GÜÇ RPM", powerRpmText, { powerRpmText = it }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GarageField("MAKS GÜÇ HP", powerHpText, { powerHpText = it }, Modifier.weight(1f))
                        GarageField("REDLINE RPM", redlineText, { redlineText = it }, Modifier.weight(1f))
                        GarageField("KESİCİ RPM", limiterText, { limiterText = it }, Modifier.weight(1f))
                        GarageField("CdA m²", cdaText, { cdaText = it }, Modifier.weight(1f))
                    }
                }
            }

            item {
                CcPanel(
                    title = "🔧  MOTOR BLOĞU",
                    subtitle = "Silindir hacmi ve yanma odası — piston hızı ve tepe güç hesabında kullanılır"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GarageField("HACİM cc", displacementText, { displacementText = it }, Modifier.weight(1f))
                        GarageField("ÇAP mm", boreText, { boreText = it }, Modifier.weight(1f))
                        GarageField("STROK mm", strokeText, { strokeText = it }, Modifier.weight(1f))
                        GarageField("SKIM. ORAN", compressionText, { compressionText = it }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "Piston hızı (redline): ${String.format(Locale.US, "%.1f", draft.meanPistonSpeedMs(draft.redlineRpm))} m/s  ·  " +
                            "Hacim/bore/strok: ${draft.displacementCc} cc · ${String.format(Locale.US, "%.1f", draft.boreMm)}×${String.format(Locale.US, "%.1f", draft.strokeMm)} mm",
                        color = CcColors.muted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            item {
                CcPanel(
                    title = "🏁  ŞANZIMAN VE SON AKTARMA",
                    subtitle = "Vites sayısı, geçiş karakteri ve verim — vites ve akustik senkronizasyonu"
                ) {
                    CcChoiceRow(
                        title = "Vites sayısı",
                        options = listOf("4", "5", "6", "7", "8"),
                        selectedIndex = (gearCount - 4).coerceIn(0, 4),
                        onSelect = { idx -> gearCount = idx + 4 }
                    )
                    Spacer(Modifier.height(8.dp))
                    CcChoiceRow(
                        title = "Şanzıman tipi",
                        options = TransmissionType.entries.map { it.displayName },
                        selectedIndex = transmissionTypeIndex.coerceIn(0, TransmissionType.entries.lastIndex),
                        onSelect = { transmissionTypeIndex = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    CcChoiceRow(
                        title = "Son aktarma",
                        options = FinalDriveType.entries.map { it.displayName },
                        selectedIndex = finalDriveIndex.coerceIn(0, FinalDriveType.entries.lastIndex),
                        onSelect = { finalDriveIndex = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    CcSwitchRow(
                        title = "Kaydırmalı debriyaj (Slipper)",
                        description = "Ani vites küçültmede arka teker stabilitesi — tork kesintisi ve rev-match profili",
                        checked = slipperClutch,
                        onChange = { slipperClutch = it }
                    )
                }
            }

            item {
                CcPanel(
                    title = "⛽  YAKIT DEPOSU VE MENZİL",
                    subtitle = "Depo kapasitesi ve ortalama tüketim — tam depo menzili"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GarageField("DEPO LİTRE", fuelTankText, { fuelTankText = it }, Modifier.weight(1f))
                        GarageField("TÜKETİM L/100", consumptionText, { consumptionText = it }, Modifier.weight(1f))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TAM DEPO MENZİL", color = CcColors.muted, fontSize = 8.sp, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${draft.fullTankRangeKm.roundToInt()} km",
                                color = CcColors.orange,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            item {
                CcPanel(
                    title = "🏗  ŞASİ, AĞIRLIK VE KOLTUK",
                    subtitle = "Aks mesafesi, sele yüksekliği ve yük dağılımı — teker ve yatış fiziği"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GarageField("AKS MM", wheelbaseText, { wheelbaseText = it }, Modifier.weight(1f))
                        GarageField("SELE MM", seatHeightText, { seatHeightText = it }, Modifier.weight(1f))
                        GarageField("MOTOSİKLET kg", wetMassText, { wetMassText = it }, Modifier.weight(1f))
                        GarageField("SÜRÜCÜ kg", riderMassText, { riderMassText = it }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "Toplam (motosiklet+sürücü): ${draft.totalMassKg.roundToInt()} kg  ·  " +
                            "Yük dağılımı ≈ ${(draft.riderAndCargoMassKg / draft.totalMassKg * 100).roundToInt()}% sürücü  ·  " +
                            "Güç/ağırlık: ${String.format(Locale.US, "%.2f", draft.maxPowerHp / draft.totalMassKg)} hp/kg",
                        color = CcColors.muted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            item {
                CcPanel(
                    title = "⚙  AKTARMA, DİŞLİ VE LASTİK",
                    subtitle = "Hız, vites ve devir hesabının tek kaynağı: bu mekanik profil"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GarageField("ÖN DİŞLİ Z", frontTeethText, { frontTeethText = it }, Modifier.weight(1f))
                        GarageField("ARKA DİŞLİ Z", rearTeethText, { rearTeethText = it }, Modifier.weight(1f))
                        GarageField("BİRİNCİL ORAN", primaryText, { primaryText = it }, Modifier.weight(1f))
                        GarageField("ARKA LASTİK", rearTireText, { rearTireText = it }, Modifier.weight(1.5f))
                        GarageField("ÖN LASTİK", frontTireText, { frontTireText = it }, Modifier.weight(1.5f))
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = "VİTES ORANLARI (1 → ${gearCount.coerceIn(4, 8)})",
                        color = CcColors.muted,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        for (i in 0 until gearCount.coerceIn(4, 8)) {
                            GarageField(
                                "${i + 1}. VİTES",
                                gearTexts.getOrElse(i) { "1.000" },
                                { value ->
                                    gearTexts = gearTexts.toMutableList().also { list ->
                                        while (list.size < 8) list.add("1.000")
                                        list[i] = value
                                    }
                                },
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                CcPanel(
                    title = "📊  CANLI HESAP ÖNİZLEMESİ",
                    subtitle = "Kaydetmeden önce yeni değerlerin redline hızları ve optimum vites devirleri"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        val speeds = draft.theoreticalSpeedTableKmh()
                        val shifts = draft.computeAllOptimalShiftRpms()
                        for (i in 0 until draft.effectiveGearCount) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CcColors.panel2)
                                    .padding(horizontal = 5.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${i + 1}. VİTES",
                                    color = CcColors.orange,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(speeds.getOrElse(i) { 0.0 }).roundToInt()}",
                                    color = CcColors.text,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("km/h", color = CcColors.muted, fontSize = 7.sp)
                                Text(
                                    text = "▲ ${(shifts.getOrElse(i) { 0.0 }).roundToInt()}",
                                    color = CcColors.cyan,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "Son hız teorik: ${draft.theoreticalSpeedTableKmh().lastOrNull()?.roundToInt() ?: 0} km/h  ·  " +
                            "Ağırlık/güç: ${String.format(Locale.US, "%.2f", draft.totalMassKg / draft.maxPowerHp.coerceAtLeast(1.0))} kg/hp  ·  " +
                            "Son dişli: ${String.format(Locale.US, "%.2f", draft.finalDriveRatio)}:1",
                        color = CcColors.muted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun GarageField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, color = CcColors.muted, fontSize = 8.sp, letterSpacing = 0.5.sp, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        CcTextField(
            value = value,
            onValueChange = onChange,
            placeholder = label,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


// =====================================================================
// GÖSTERGE GALERİSİ
// =====================================================================

@Composable
private fun GalleryTab(
    prefs: ClusterPreferences,
    language: AppLanguage,
    onSelectGauge: (Int) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var brand by remember { mutableStateOf("ALL") }
    var category by remember { mutableStateOf<GaugeDisplayCategory?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(brand, category, favoritesOnly, query, prefs.favoriteSkinIds) {
        OemGaugeCatalog.allModels.filter { spec ->
            val brandOk = brand == "ALL" || spec.brand.equals(brand, ignoreCase = true)
            val catOk = category == null || spec.displayCategory == category
            val favOk = !favoritesOnly || prefs.favoriteSkinIds.contains(spec.skinId)
            val qOk = query.isBlank() ||
                spec.modelName.contains(query, ignoreCase = true) ||
                spec.brand.contains(query, ignoreCase = true) ||
                spec.modelNumber.toString() == query.trim()
            brandOk && catOk && favOk && qOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CcTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = LocalizationManager.getString(StringKey.SEARCH_MODELS_PLACEHOLDER, language),
                modifier = Modifier.widthIn(min = 180.dp, max = 320.dp).weight(0.35f)
            )
            LazyRow(
                modifier = Modifier.weight(0.65f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    CcChip(
                        text = LocalizationManager.getString(StringKey.GALLERY_ALL_BRANDS, language),
                        selected = brand == "ALL",
                        onClick = { brand = "ALL" }
                    )
                }
                item {
                    CcChip(
                        text = "★ " + LocalizationManager.getString(StringKey.ACTION_FAVORITES_ONLY, language),
                        selected = favoritesOnly,
                        onClick = { favoritesOnly = !favoritesOnly },
                        accent = CcColors.gold
                    )
                }
                lazyItems(OemGaugeCatalog.allBrands) { b ->
                    CcChip(text = b, selected = brand == b, onClick = { brand = b })
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    CcChip(
                        text = "TÜMÜ",
                        selected = category == null,
                        onClick = { category = null }
                    )
                }
                lazyItems(GaugeDisplayCategory.entries) { cat ->
                    CcChip(
                        text = cat.label(),
                        selected = category == cat,
                        onClick = { category = if (category == cat) null else cat }
                    )
                }
            }
            Text(
                text = "${filtered.size} / 270",
                color = CcColors.muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 230.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            gridItems(filtered, key = { it.skinId }) { spec ->
                val selected = spec.modelNumber - 1 == prefs.selectedGaugeIndex
                val favorite = prefs.favoriteSkinIds.contains(spec.skinId)
                GalleryCard(
                    spec = spec,
                    selected = selected,
                    favorite = favorite,
                    onSelect = { onSelectGauge(spec.modelNumber - 1) },
                    onToggleFavorite = { onToggleFavorite(spec.skinId) }
                )
            }
        }
    }
}

@Composable
private fun GalleryCard(
    spec: DeclarativeGaugeSpec,
    selected: Boolean,
    favorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val demo = remember(spec.skinId) { demoTelemetryFor(spec) }
    Surface(
        color = if (selected) Color(0xFF241708) else CcColors.panel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) CcColors.orange else CcColors.border
        ),
        shadowElevation = if (selected) 8.dp else 3.dp,
        modifier = Modifier.clickable { onSelect() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                    .background(Color.Black)
            ) {
                OemClusterCanvasRenderer(
                    spec = spec,
                    subModeIndex = 0,
                    telemetry = demo,
                    rpmScalingMode = RpmScalingMode.MODE_A_PROPORTIONAL_REDLINE,
                    wheelieDisplayMode = WheelieGaugeDisplayMode.ALWAYS_VISIBLE,
                    useMph = false,
                    chromaKeyGreenScreen = false,
                    oledPixelShiftOffset = androidx.compose.ui.geometry.Offset.Zero,
                    language = AppLanguage.TR,
                    tripKm = 181.4,
                    odometerKm = 1420.5,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "#%03d".format(spec.modelNumber),
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.Black.copy(alpha = 0.62f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                    // Yıldız gerçek bir dokunma hedefi: kartı seçmeden favoriye ekler.
                    Text(
                        text = if (favorite) "★" else "☆",
                        color = if (favorite) CcColors.gold else Color.White.copy(alpha = 0.60f),
                        fontSize = 19.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { onToggleFavorite() }
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
                if (selected) {
                    Text(
                        text = "AKTİF",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(CcColors.orange)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "${spec.brand} · ${spec.modelName}",
                    color = CcColors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = spec.generationYears,
                    color = CcColors.muted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = spec.displayCategory.tone().copy(alpha = 0.14f),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, spec.displayCategory.tone().copy(alpha = 0.40f))
                    ) {
                        Text(
                            text = spec.displayCategory.shortLabel(),
                            color = spec.displayCategory.tone(),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${spec.gaugeRedlineRpm.roundToInt()} RPM",
                        color = CcColors.muted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// =====================================================================
// AYARLAR
// =====================================================================

@Composable
private fun SettingsTab(
    prefs: ClusterPreferences,
    onPrefsChange: (transform: (ClusterPreferences) -> ClusterPreferences) -> Unit,
    telemetry: UnifiedTelemetryState,
    language: AppLanguage,
    onZeroCalibrateNow: () -> Unit,
    onResetGarage: () -> Unit,
    onReplayLaunch: () -> Unit,
    onResetOdometer: () -> Unit
) {
    var confirmGarageReset by remember { mutableStateOf(false) }
    var confirmOdometerReset by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // --- Özet şeridi: aktif motor, GNSS, shift eşikleri, tema ---
        settingsItem(visible = matchesQuery(query, "canli durum live hiz hiz kilometre devir rpm vites gear gnss uydu telemetry")) {
            SettingsSearchBar(query = query, onQueryChange = { query = it })
        }

        settingsItem(visible = matchesQuery(query, "canli durum live hiz hiz kilometre devir rpm vites gear gnss uydu telemetry hiz hiz km")) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CcColors.panel)
                    .border(1.dp, CcColors.border, RoundedCornerShape(18.dp))
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, CcColors.orange, Color.Transparent)
                                )
                            )
                    )
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(CcColors.green)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                "CANLI DURUM",
                                color = CcColors.muted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.7.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (prefs.useMph) "mph" else "km/h",
                                color = CcColors.muted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%d",
                                    (telemetry.trueSpeedKmh * if (prefs.useMph) 0.621371 else 1.0).roundToInt()
                                ),
                                color = CcColors.text,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 46.sp
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "VİTES",
                                    color = CcColors.muted,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.4.sp
                                )
                                Text(
                                    telemetry.gearDisplayString,
                                    color = CcColors.cyan,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 30.sp
                                )
                                Text(
                                    "RPM ${telemetry.actualEngineRpm.roundToInt()}",
                                    color = CcColors.muted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "GÜVEN %${(telemetry.gearConfidence * 100).roundToInt()}",
                                    color = CcColors.green,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        settingsItem(visible = matchesQuery(query, "dil language idioma langue sprache lingua turkce english espanol italiano francais deutsch portugais russo chinese japones korean korece")) {
            CcPanel(
                title = "🌍  DİL / LANGUAGE",
                subtitle = "Arayüz ve gösterge etiketleri anında değişir"
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    lazyItems(AppLanguage.entries) { lang ->
                        CcChip(
                            text = "${lang.flagEmoji} ${lang.nativeName}",
                            selected = lang == language,
                            onClick = {
                                LocalizationManager.setLanguage(lang)
                                onPrefsChange { it.copy(languageCode = lang.code) }
                            }
                        )
                    }
                }
            }
        }

        settingsItem(visible = matchesQuery(query, "birim units mph kmh sicaklik temperature fahrenheit celsius f saat clock 12 24 yakit fuel mpg l100 l100km kml")) {
            CcPanel(
                title = "📐  BİRİMLER",
                subtitle = "Hız ve sıcaklık birimleri; gösterge anında güncellenir"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CcSwitchRow(
                        title = "MPH hız birimi",
                        description = "Kadran hızını mil/saat olarak gösterir (0.621371 çarpanı)",
                        checked = prefs.useMph,
                        onChange = { v -> onPrefsChange { it.copy(useMph = v) } }
                    )
                    CcSwitchRow(
                        title = "Sıcaklık °F (Fahrenheit)",
                        description = "Hararet göstergesi ve uyarılar Fahrenheit okunur",
                        checked = prefs.useFahrenheit,
                        onChange = { v -> onPrefsChange { it.copy(useFahrenheit = v) } }
                    )
                    CcSwitchRow(
                        title = "24 saat biçimi",
                        description = "Kapalıyken gösterge saati 12 saat (1:25 PM) biçiminde gösterir",
                        checked = prefs.clock24h,
                        onChange = { v -> onPrefsChange { it.copy(clock24h = v) } }
                    )
                    CcChoiceRow(
                        title = "Yakıt tüketimi birimi",
                        options = listOf("L/100km", "km/L", "MPG"),
                        selectedIndex = when (prefs.fuelUnitCode) {
                            "kml" -> 1
                            "mpg" -> 2
                            else -> 0
                        },
                        onSelect = { idx ->
                            val code = when (idx) {
                                1 -> "kml"
                                2 -> "mpg"
                                else -> "l100km"
                            }
                            onPrefsChange { it.copy(fuelUnitCode = code) }
                        }
                    )
                }
            }
        }

        settingsItem(visible = matchesQuery(query, "ekran display tema theme gunduz gece day night oled rpm yatis teker wheelie parlaklik brightness wakelock keepscreenon kontak ignition sweep tarama pivot")) {
            CcPanel(
                title = "🖥  GÖSTERGE VE EKRAN",
                subtitle = "Tema, birim, RPM ölçeği ve OLED koruması"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CcChoiceRow(
                        title = "Tema",
                        options = listOf("Otomatik", "Gündüz", "Gece"),
                        selectedIndex = prefs.themeOverrideMode.coerceIn(0, 2),
                        onSelect = { mode -> onPrefsChange { it.copy(themeOverrideMode = mode) } }
                    )
                    CcSwitchRow(
                        title = "OLED piksel kaydırma",
                        description = "Sabit görüntü yanmasını önlemek için 1–3 px yumuşak gezinme",
                        checked = prefs.oledPixelShiftEnabled,
                        onChange = { v -> onPrefsChange { it.copy(oledPixelShiftEnabled = v) } }
                    )
                    CcSwitchRow(
                        title = "Chroma Key yeşil ekran",
                        description = "GoPro / OBS kompozisyonu için saf yeşil arka plan",
                        checked = prefs.chromaKeyGreen,
                        onChange = { v -> onPrefsChange { it.copy(chromaKeyGreen = v) } }
                    )
                    CcChoiceRow(
                        title = "RPM ölçekleme",
                        options = listOf("Redline'a oranla", "Gerçek 1:1"),
                        selectedIndex = if (prefs.rpmScalingMode == RpmScalingMode.MODE_A_PROPORTIONAL_REDLINE) 0 else 1,
                        onSelect = { idx ->
                            onPrefsChange {
                                it.copy(
                                    rpmScalingMode = if (idx == 0) RpmScalingMode.MODE_A_PROPORTIONAL_REDLINE
                                    else RpmScalingMode.MODE_B_NATIVE_1_TO_1
                                )
                            }
                        }
                    )
                    CcChoiceRow(
                        title = "Teker göstergesi",
                        options = listOf("Akıllı otomatik", "Her zaman açık", "Gizli / OEM"),
                        selectedIndex = when (prefs.wheelieDisplayMode) {
                            WheelieGaugeDisplayMode.SMART_AUTO_POPUP -> 0
                            WheelieGaugeDisplayMode.ALWAYS_VISIBLE -> 1
                            WheelieGaugeDisplayMode.HIDDEN_OEM_PURE -> 2
                        },
                        onSelect = { idx ->
                            onPrefsChange {
                                it.copy(
                                    wheelieDisplayMode = when (idx) {
                                        0 -> WheelieGaugeDisplayMode.SMART_AUTO_POPUP
                                        1 -> WheelieGaugeDisplayMode.ALWAYS_VISIBLE
                                        else -> WheelieGaugeDisplayMode.HIDDEN_OEM_PURE
                                    }
                                )
                            }
                        }
                    )
                    CcSwitchRow(
                        title = "Ekran her zaman açık (WakeLock)",
                        description = "Sürüş sırasında ekran kapanmaz; yolun ortasında karanlıkta kalınmaz",
                        checked = prefs.keepScreenOn,
                        onChange = { v -> onPrefsChange { it.copy(keepScreenOn = v) } }
                    )
                    CcSwitchRow(
                        title = "Güneş altında maks. parlaklık",
                        description = "Ekran parlaklığını %100'e sabitler; güneş altında kadranı rahat okutur",
                        checked = prefs.forceMaxBrightness,
                        onChange = { v -> onPrefsChange { it.copy(forceMaxBrightness = v) } }
                    )
                    CcSwitchRow(
                        title = "Kontak açılış taraması (0→redline→0)",
                        description = "Her kontak açılışında ibreler tam tarama yapar; kapatılırsa gösterge doğrudan canlı moda geçer",
                        checked = prefs.ignitionSweepEnabled,
                        onChange = { v -> onPrefsChange { it.copy(ignitionSweepEnabled = v) } }
                    )
                }
            }
        }

        settingsItem(visible = matchesQuery(query, "shift light gear vites stage stage1 stage2 evre optimal ecu beep bip ses kask interkom reaksiyon otomatik manual")) {
            CcPanel(
                title = "🚦  SHIFT-LIGHT / SANAL ECU",
                subtitle = "Vites uyarı eşikleri, reaksiyon süresi ve akustik devir"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CcSwitchRow(
                        title = "Optimum vites noktası (otomatik)",
                        description = "Tork eğrisi ile bir üst vitesin teker torku kesişiminden hesaplanır",
                        checked = prefs.autoOptimalShift,
                        onChange = { v -> onPrefsChange { it.copy(autoOptimalShift = v) } }
                    )
                    if (!prefs.autoOptimalShift) {
                        CcSliderRow(
                            title = "Evre 1 ön uyarı",
                            valueLabel = "${prefs.manualStage1Rpm.roundToInt()} RPM",
                            value = prefs.manualStage1Rpm,
                            range = 4000f..14000f,
                            onChange = { v -> onPrefsChange { it.copy(manualStage1Rpm = v) } }
                        )
                        CcSliderRow(
                            title = "Evre 2 vites at",
                            valueLabel = "${prefs.manualStage2Rpm.roundToInt()} RPM",
                            value = prefs.manualStage2Rpm,
                            range = 5000f..16000f,
                            onChange = { v -> onPrefsChange { it.copy(manualStage2Rpm = v) } }
                        )
                    }
                    CcSliderRow(
                        title = "İnsan reaksiyon süresi",
                        valueLabel = "${prefs.reactionTimeMs.roundToInt()} ms",
                        value = prefs.reactionTimeMs,
                        range = 80f..350f,
                        onChange = { v -> onPrefsChange { it.copy(reactionTimeMs = v) } }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CcStatTile("EVRE 1", "${telemetry.stage1ShiftRpm.roundToInt()}", "RPM", CcColors.orange, Modifier.weight(1f))
                        CcStatTile("EVRE 2", "${telemetry.stage2ShiftRpm.roundToInt()}", "RPM", CcColors.red, Modifier.weight(1f))
                        CcStatTile("VİTES GÜVENİ", "${(telemetry.gearConfidence * 100).roundToInt()}", "%", CcColors.green, Modifier.weight(1f))
                    }
                    CcSwitchRow(
                        title = "Akustik mikrofon ile devir",
                        description = "48 kHz FFT/HPS harmonik eşleme; opsiyonel ikinci devir kaynağı",
                        checked = prefs.acousticMicEnabled,
                        onChange = { v -> onPrefsChange { it.copy(acousticMicEnabled = v) } }
                    )
                    CcSwitchRow(
                        title = "Stage 2 kask bip'i",
                        description = "Shift Stage 2'ye veya kesiciye geçildiğinde kaska bip verir; interkom açıkken duyulur",
                        checked = prefs.shiftBeepEnabled,
                        onChange = { v -> onPrefsChange { it.copy(shiftBeepEnabled = v) } }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CcSmallButton("Bip sesini dene", onClick = { com.oemcluster.pro.runtime.ShiftBeeper.test() })
                    }
                }
            }
        }

        settingsItem(visible = matchesQuery(query, "hiz speed kalibrasyon calibration ece r39 oem 299 japan jdm doppler gnss kustas true speed saf")) {
            CcPanel(
                title = "🎯  HIZ KALİBRASYONU",
                subtitle = "Saf GNSS Doppler hızı veya OEM ECE-R39 fabrika sapması"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CcSwitchRow(
                        title = "OEM ECE-R39 hız sapması",
                        description = "Kadran, gerçek hızın %5–8 üzerini gösterir (fabrika yasası)",
                        checked = prefs.useOemSpeedBias,
                        onChange = { v -> onPrefsChange { it.copy(useOemSpeedBias = v) } }
                    )
                    if (prefs.useOemSpeedBias) {
                        CcSliderRow(
                            title = "Sapma oranı",
                            valueLabel = "+${(prefs.oemBiasRatio * 100).roundToInt()}%  +1.5 km/h",
                            value = prefs.oemBiasRatio,
                            range = 0f..0.10f,
                            onChange = { v -> onPrefsChange { it.copy(oemBiasRatio = v) } }
                        )
                    }
                    CcSwitchRow(
                        title = "Japon 299 km/h kilidi",
                        description = "JDM göstergelerde olduğu gibi 299 üzerini sabitler",
                        checked = prefs.japan299Lock,
                        onChange = { v -> onPrefsChange { it.copy(japan299Lock = v) } }
                    )
                }
            }
        }

        settingsItem(visible = matchesQuery(query, "sifirla zero yatis kalibrasyon calibration imu derece")) {
            CcPanel(
                title = "📐  SIFIR YATIŞ KALİBRASYONU",
                subtitle = "Telefonun montajdaki gerçek sürüş pozisyonunu 0° yatış olarak sıfırlar"
            ) {
                Button(
                    onClick = onZeroCalibrateNow,
                    colors = ButtonDefaults.buttonColors(containerColor = CcColors.cyan),
                    shape = RoundedCornerShape(7.dp)
                ) {
                    Text(
                        "🎯  Yatışı şimdi sıfırla (0°)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Motosikleti dik ve düz tutarak basın; yatış açısı, eğim ve teker metrikleri buradan sıfırlanır.",
                    color = CcColors.muted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        settingsItem(visible = matchesQuery(query, "gnss uydu satellite sensor imu kalibrasyon sifirla zero fix constellation")) {
            GnssStatusPanel(telemetry)
        }

        settingsItem(visible = matchesQuery(query, "uygulama app ustlabs ust labs launch acilis acilis animasyon boot oyna replay")) {
            CcPanel(
                title = "🏷  USTLABS / UYGULAMA",
                subtitle = "Açılış sekansı, geliştirici ve sürüm bilgisi"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CcSwitchRow(
                        title = "USTlabs açılış animasyonu",
                        description = "Kontak selamlaması: iğne süpürme + sistem kontrolleri (3,5 sn · dokununca atlanır)",
                        checked = prefs.launchAnimationEnabled,
                        onChange = { v -> onPrefsChange { it.copy(launchAnimationEnabled = v) } }
                    )
                    Button(
                        onClick = onReplayLaunch,
                        colors = ButtonDefaults.buttonColors(containerColor = CcColors.orange),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            "AÇILIŞ ANİMASYONUNU ŞİMDİ OYNAT",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Geliştirici: USTlabs  ·  MotoCross 1.0.5-PRO\n" +
                            "270 OEM gösterge  ·  15 durumlu EKF  ·  C++20 NDK  ·  400 Hz IMU / 120 Hz render",
                        color = CcColors.muted,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        settingsItem(visible = matchesQuery(query, "garaj garage fabrika factory sifirla reset sil delete veri bakim bakim odometer sayac kilometre km")) {
            CcPanel(
                title = "🧰  GARAJ VE VERİ BAKIMI",
                subtitle = "Fabrika profilleri, yol bilgisayarı ve rekorlar"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { confirmGarageReset = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1A10)),
                            border = BorderStroke(1.dp, CcColors.orange.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Text("Garajı fabrika ayarlarına döndür", color = CcColors.orange, fontSize = 11.sp)
                        }
                    }
                    if (confirmGarageReset) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFF2A0E12))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Tüm özel motor profilleri ve düzenlemeler silinecek. Emin misiniz?",
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    confirmGarageReset = false
                                    onResetGarage()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CcColors.red),
                                shape = RoundedCornerShape(6.dp)
                            ) { Text("SİL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = { confirmGarageReset = false },
                                colors = ButtonDefaults.buttonColors(containerColor = CcColors.panel2),
                                shape = RoundedCornerShape(6.dp)
                            ) { Text("VAZGEÇ", color = CcColors.muted, fontSize = 11.sp) }
                        }
                    }
                    Text(
                        text = "USTlabs  ·  MotoCross 1.0.5-PRO  ·  270 OEM gösterge  ·  15 durumlu EKF  ·  C++20 NDK  ·  400 Hz IMU / 120 Hz render",
                        color = CcColors.muted,
                        fontSize = 10.sp
                    )
                }
            }
        }
        settingsItem(visible = matchesQuery(query, "odometer sayac kilometre km sifirla reset kalici total trip")) {
            CcPanel(
                title = "🛣️  KALICI SAYAÇ",
                subtitle = "Toplam kilometre göstergesi (uygulama kapansa bile korunur)"
            ) {
                if (!confirmOdometerReset) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Odometreyi sıfırla",
                                color = CcColors.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Toplam km göstergesi 0 km'e döner; trip ve rekorlar korunur.",
                                color = CcColors.muted,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                        CcSmallButton("SIFIRLA", onClick = { confirmOdometerReset = true }, danger = true)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF2A0E12))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Kalıcı sayaç 0 km'e dönecek. Emin misiniz?",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                confirmOdometerReset = false
                                onResetOdometer()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CcColors.red),
                            shape = RoundedCornerShape(6.dp)
                        ) { Text("SIFIRLA", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = { confirmOdometerReset = false },
                            colors = ButtonDefaults.buttonColors(containerColor = CcColors.panel2),
                            shape = RoundedCornerShape(6.dp)
                        ) { Text("VAZGEÇ", color = CcColors.muted, fontSize = 11.sp) }
                    }
                }
            }
        }

        if (query.isNotBlank() && !settingsSectionVisible(query)) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔍", fontSize = 30.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "'" + query.trim() + "' için ayar bulunamadı",
                        color = CcColors.muted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GnssStatusPanel(telemetry: UnifiedTelemetryState) {
    val hasFix = telemetry.gpsSatelliteCount > 0 && telemetry.gpsSpeedAgeSec < 5.0
    val accuracy = telemetry.gpsHorizontalAccuracyM
    val accColor = when {
        !hasFix -> CcColors.red
        accuracy <= 5.0 -> CcColors.green
        accuracy <= 15.0 -> CcColors.gold
        else -> CcColors.orange
    }
    CcPanel(
        title = "🛰  GNSS / SENSÖR DURUMU",
        subtitle = if (hasFix) "Doppler hızı canlı" else "Uydu bekleniyor — IMU yedeği devrede"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CcStatTile(
                    "UYDU",
                    if (hasFix) "${telemetry.gpsSatelliteCount}" else "—",
                    "adet",
                    if (hasFix) CcColors.green else CcColors.red,
                    Modifier.weight(1f)
                )
                CcStatTile(
                    "YATAY HATA",
                    if (hasFix) String.format(Locale.US, "%.1f", accuracy) else "—",
                    "m",
                    accColor,
                    Modifier.weight(1f)
                )
                CcStatTile(
                    "FIX YAŞI",
                    if (telemetry.gpsSpeedAgeSec > 1000) "—" else String.format(Locale.US, "%.1f", telemetry.gpsSpeedAgeSec),
                    "s",
                    if (hasFix) CcColors.cyan else CcColors.red,
                    Modifier.weight(1f)
                )
                CcStatTile(
                    "GNSS PAYI",
                    "${(telemetry.gpsSpeedSourceBlend * 100).roundToInt()}",
                    "%",
                    CcColors.cyan,
                    Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CcColors.panel2)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (telemetry.isStationary) "● DURUŞ TESPİTİ: AKTİF (ZUPT)" else "● HAREKET: GPS + IMU FÜZYONU",
                    color = if (telemetry.isStationary) CcColors.cyan else CcColors.green,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "HIZ DOĞRULUĞU ${String.format(Locale.US, "%.2f", telemetry.gpsSpeedAccuracyMs)} m/s",
                    color = CcColors.muted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "Duruşta sıfır hız kilidi (ZUPT), 3 s'den eski fix reddi, 60 m üzeri doğruluk reddi ve 4 ardışık aykırı değer kapısı aktiftir.",
                color = CcColors.muted,
                fontSize = 10.sp
            )
        }
    }
}

// =====================================================================
// YOL BİLGİSAYARI & DRAG
// =====================================================================

@Composable
private fun RideDataTab(
    session: RideSessionState,
    telemetry: UnifiedTelemetryState,
    rideRevision: Int,
    activeBike: BikeMechanicalProfile,
    useMph: Boolean,
    onResetTripA: () -> Unit,
    onResetTripB: () -> Unit,
    onResetPeaks: () -> Unit,
    onResetDrag: () -> Unit
) {
    // rideRevision, 2 Hz'lik sayaç yenilemesini bu ekrana bağlar.
    val revision = rideRevision

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            CcPanel(
                title = "📡  CANLI TELEMETRİ",
                subtitle = "${activeBike.brand} ${activeBike.modelName}  ·  ${activeBike.engineConfig.displayName}"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CcStatTile("HIZ", "${telemetry.displayedSpeedKmh.roundToInt()}", "km/h", CcColors.orange, Modifier.weight(1f))
                    CcStatTile("VİTES", telemetry.gearDisplayString, if (telemetry.isClutchPulled) "DEBRİYAJ" else "", CcColors.cyan, Modifier.weight(1f))
                    CcStatTile("DEVİR", "${telemetry.actualEngineRpm.roundToInt()}", "RPM", CcColors.red, Modifier.weight(1f))
                    CcStatTile("YATIŞ", "${telemetry.leanAngleDeg.roundToInt()}", "°", CcColors.green, Modifier.weight(1f))
                    CcStatTile("BEYGİR", "${telemetry.estimatedWheelHorsepower.roundToInt()}", "whp", CcColors.gold, Modifier.weight(1f))
                }
            }
        }

        item {
            CcPanel(
                title = "🧭  YOL BİLGİSAYARI",
                subtitle = "Seans sayaçları uygulama kapansa da korunur"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CcStatTile("TRIP A", DistanceFormatter.numeric(session.tripAKm, useMph, 2), DistanceFormatter.unit(useMph), CcColors.cyan, Modifier.weight(1f))
                    CcStatTile("TRIP B", DistanceFormatter.numeric(session.tripBKm, useMph, 2), DistanceFormatter.unit(useMph), CcColors.cyan, Modifier.weight(1f))
                    CcStatTile("SESA", DistanceFormatter.numeric(session.sessionKm, useMph, 2), DistanceFormatter.unit(useMph), CcColors.orange, Modifier.weight(1f))
                    CcStatTile("ODOMETER", DistanceFormatter.numeric(session.odometerKm, useMph, 1), DistanceFormatter.unit(useMph), CcColors.text, Modifier.weight(1f))
                    CcStatTile("ORT. HIZ", String.format(Locale.US, "%.1f", session.avgSpeedKmh), "km/h", CcColors.green, Modifier.weight(1f))
                    CcStatTile("SÜRE", formatDuration(session.rideTimeSeconds), "", CcColors.muted, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CcSmallButton("TRIP A SIFIRLA", onResetTripA)
                    CcSmallButton("TRIP B SIFIRLA", onResetTripB)
                }
            }
        }

        item {
            CcPanel(
                title = "🏆  TEPE DEĞERLER",
                subtitle = "Bu motosiklet profiliyle kaydedilen en yüksek değerler"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CcStatTile("MAKS HIZ", "${session.peakSpeedKmh.roundToInt()}", "km/h", CcColors.orange, Modifier.weight(1f))
                    CcStatTile("MAKS DEVİR", "${session.peakRpm.roundToInt()}", "RPM", CcColors.red, Modifier.weight(1f))
                    CcStatTile("SOL YATIŞ", "${session.peakLeanLeftDeg.roundToInt()}", "°", CcColors.cyan, Modifier.weight(1f))
                    CcStatTile("SAĞ YATIŞ", "${session.peakLeanRightDeg.roundToInt()}", "°", CcColors.cyan, Modifier.weight(1f))
                    CcStatTile("TEKER AÇI", "${session.peakWheelieDeg.roundToInt()}", "°", CcColors.green, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CcStatTile("EN UZUN TEKER", String.format(Locale.US, "%.2f", session.longestWheelieSec), "s", CcColors.green, Modifier.weight(1f))
                    CcStatTile("TEKER MESAFESİ", String.format(Locale.US, "%.1f", session.longestWheelieMeters), "m", CcColors.green, Modifier.weight(1f))
                    CcStatTile("VİTES GÜVENİ", "${(telemetry.gearConfidence * 100).roundToInt()}", "%", CcColors.gold, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                CcSmallButton("TEPE DEĞERLERİ SIFIRLA", onResetPeaks, danger = true)
            }
        }

        item {
            CcPanel(
                title = "🏁  DRAG BOX",
                subtitle = "Duruştan otomatik ölçüm: 0–100, 0–200 ve çeyrek mil"
            ) {
                if (session.dragActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF10261A))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("● KALKIŞ ÖLÇÜLÜYOR", color = CcColors.green, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(
                            "t = ${String.format(Locale.US, "%.2f", session.dragElapsedSec)} s   ·   ${session.dragDistanceM.roundToInt()} m   ·   ${session.dragSpeedKmh.roundToInt()} km/h",
                            color = CcColors.text,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CcStatTile(
                        "0–100 KM/H",
                        if (session.best0to100Sec > 0) String.format(Locale.US, "%.2f", session.best0to100Sec) else "—",
                        "s",
                        CcColors.orange,
                        Modifier.weight(1f)
                    )
                    CcStatTile(
                        "0–200 KM/H",
                        if (session.best0to200Sec > 0) String.format(Locale.US, "%.2f", session.best0to200Sec) else "—",
                        "s",
                        CcColors.red,
                        Modifier.weight(1f)
                    )
                    CcStatTile(
                        "1/4 MİL",
                        if (session.bestQuarterMileSec > 0) String.format(Locale.US, "%.2f", session.bestQuarterMileSec) else "—",
                        "s",
                        CcColors.gold,
                        Modifier.weight(1f)
                    )
                    CcStatTile(
                        "SON 0–100",
                        if (session.last0to100Sec > 0) String.format(Locale.US, "%.2f", session.last0to100Sec) else "—",
                        "s",
                        CcColors.cyan,
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                CcSmallButton("DRAG REKORLARINI SIFIRLA", onResetDrag, danger = true)
            }
        }

        item {
            CcPanel(
                title = "📐  AKTİF MOTORUN MEKANİK ÖZETİ",
                subtitle = "Tüm hesaplamaların tek kaynağı: garaj profili"
            ) {
                val speedTable = activeBike.theoreticalSpeedTableKmh()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CcStatTile("SON DİŞLİ", "${activeBike.frontSprocketTeeth}/${activeBike.rearSprocketTeeth}", "Z", CcColors.cyan, Modifier.weight(1f))
                    CcStatTile("BİRİNCİL", String.format(Locale.US, "%.3f", activeBike.primaryReductionRatio), ":1", CcColors.text, Modifier.weight(1f))
                    CcStatTile("LASTİK", activeBike.rearTire.toSpecString(), "", CcColors.muted, Modifier.weight(1f))
                    CcStatTile("AĞIRLIK", "${activeBike.totalMassKg.roundToInt()}", "kg", CcColors.text, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Redline'da teorik hız:  " + speedTable.mapIndexed { i, v ->
                        "${i + 1}. ${v.roundToInt()}"
                    }.joinToString("   ") + " km/h",
                    color = CcColors.muted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Optimum vites devirleri:  " + activeBike.computeAllOptimalShiftRpms()
                        .map { it.roundToInt() }
                        .joinToString(" / ") + " RPM",
                    color = CcColors.cyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Son revizyon: $revision  ·  Motor profili: ${activeBike.profileId}",
                    color = CcColors.muted.copy(alpha = 0.65f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// =====================================================================
// SİMÜLATÖR
// =====================================================================

@Composable
private fun SimulatorTab(
    simulation: SimulationController,
    activeBike: BikeMechanicalProfile,
    spec: DeclarativeGaugeSpec,
    prefs: ClusterPreferences
) {
    // Sekme açıkken 10 Hz canlı önizleme; kadranın aynısı, aynı telemetri.
    val preview by produceState(
        initialValue = NativeTelemetryEngineBridge.telemetryState.value,
        key1 = simulation.active
    ) {
        while (true) {
            value = NativeTelemetryEngineBridge.telemetryState.value
            delay(100)
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1.05f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            CcPanel(
                title = "🎛  CANLI TEST SÜRÜŞÜ",
                subtitle = "Simülatör aynı C++ fizik hattını ve aynı göstergeyi besler"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CcSwitchRow(
                        title = "Simülatörü etkinleştir",
                        description = if (simulation.active) "Gerçek sensörler devre dışı; test telemetrisi yayınlanıyor"
                        else "Kapalıyken gerçek GNSS + IMU verisi kullanılır",
                        checked = simulation.active,
                        onChange = { if (it) simulation.start() else simulation.stop() }
                    )
                    CcSliderRow(
                        title = "Hız",
                        valueLabel = "${simulation.speedKmh.roundToInt()} km/h",
                        value = simulation.speedKmh,
                        range = 0f..320f,
                        onChange = { simulation.setSpeed(it) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        for (g in 0..8) {
                            CcGearButton(
                                label = if (g == 0) "N" else "$g",
                                selected = simulation.gear == g,
                                onClick = { simulation.selectGear(g) }
                            )
                        }
                    }
                    CcSwitchRow(
                        title = "Debriyaj çekili",
                        description = "Motor devrini tekerlekten ayırır; coasting durumu",
                        checked = simulation.clutchIn,
                        onChange = { simulation.clutchIn = it; simulation.start() }
                    )
                    CcSliderRow(
                        title = "Viraj yatış açısı",
                        valueLabel = "${simulation.leanDeg.roundToInt()}°",
                        value = simulation.leanDeg,
                        range = -58f..58f,
                        onChange = { simulation.setLean(it) }
                    )
                    CcSliderRow(
                        title = "Teker (wheelie) açısı",
                        valueLabel = "${simulation.wheelieDeg.roundToInt()}°",
                        value = simulation.wheelieDeg,
                        range = 0f..62f,
                        onChange = { simulation.setWheelie(it) }
                    )
                }
            }

            CcPanel(
                title = "⚡  HAZIR SENARYOLAR",
                subtitle = "Tek dokunuşla gerçekçi test koşulları"
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CcChip("0–100 KALKIŞ", false, simulation::launchScenario)
                    CcChip("KANYON 84", false, simulation::canyonScenario)
                    CcChip("TEKER 34°", false, simulation::wheelieScenario)
                    CcChip("299 KM/H", false, simulation::highSpeedScenario)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Mevcut motor: ${activeBike.brand} ${activeBike.modelName}  ·  ${activeBike.gearRatios.size} vites  ·  " +
                        "redline ${activeBike.redlineRpm.roundToInt()} RPM",
                    color = CcColors.muted,
                    fontSize = 10.sp
                )
            }
        }

        Column(
            modifier = Modifier.weight(0.95f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CANLI KADRAN ÖNİZLEMESİ  ·  #${spec.modelNumber} ${spec.brand} ${spec.modelName}",
                color = CcColors.muted,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, CcColors.border, RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                OemClusterCanvasRenderer(
                    spec = spec,
                    subModeIndex = prefs.selectedSubModeIndex,
                    telemetry = preview,
                    rpmScalingMode = prefs.rpmScalingMode,
                    wheelieDisplayMode = WheelieGaugeDisplayMode.ALWAYS_VISIBLE,
                    useMph = prefs.useMph,
                    useFahrenheit = prefs.useFahrenheit,
                    chromaKeyGreenScreen = false,
                    oledPixelShiftOffset = androidx.compose.ui.geometry.Offset.Zero,
                    language = AppLanguage.TR,
                    tripKm = 181.4,
                    odometerKm = 1420.5,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CcStatTile("HIZ", "${preview.displayedSpeedKmh.roundToInt()}", if (prefs.useMph) "mph" else "km/h", CcColors.orange, Modifier.weight(1f))
                CcStatTile("VİTES", preview.gearDisplayString, "", CcColors.cyan, Modifier.weight(1f))
                CcStatTile("DEVİR", "${preview.actualEngineRpm.roundToInt()}", "RPM", CcColors.red, Modifier.weight(1f))
                CcStatTile("SHIFT", preview.shiftLightState.shortLabel(), "", CcColors.green, Modifier.weight(1f))
            }
        }
    }
}

// =====================================================================
// ORTAK BİLEŞENLER
// =====================================================================

@Composable
fun CcPanel(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        color = CcColors.panel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CcColors.border),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Sol vurgu çubuğu — her bölümde marka imzası
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(CcColors.orange, CcColors.orange.copy(alpha = 0.15f))
                        ),
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 13.dp, end = 14.dp, top = 13.dp, bottom = 14.dp)
            ) {
                Text(
                    text = title,
                    color = CcColors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(text = subtitle, color = CcColors.muted, fontSize = 10.sp)
                }
                Spacer(Modifier.height(11.dp))
                content()
            }
        }
    }
}

@Composable
fun CcSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) CcColors.orangeSoft else CcColors.panel2)
            .border(
                1.dp,
                if (checked) CcColors.orange.copy(alpha = 0.35f) else CcColors.border,
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = CcColors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(description, color = CcColors.muted, fontSize = 10.sp, lineHeight = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CcColors.orange,
                uncheckedThumbColor = CcColors.muted,
                uncheckedTrackColor = CcColors.panel3,
                uncheckedBorderColor = CcColors.border
            )
        )
    }
}

@Composable
fun CcSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CcColors.panel2)
            .border(1.dp, CcColors.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = CcColors.text, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Surface(
                color = CcColors.orangeSoft,
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(1.dp, CcColors.orange.copy(alpha = 0.30f))
            ) {
                Text(
                    text = valueLabel,
                    color = CcColors.orange,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = CcColors.orange,
                activeTrackColor = CcColors.orange,
                inactiveTrackColor = CcColors.panel3
            )
        )
    }
}

@Composable
fun CcChoiceRow(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = CcColors.muted, fontSize = 9.5.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEachIndexed { index, label ->
                CcChip(text = label, selected = index == selectedIndex, onClick = { onSelect(index) })
            }
        }
    }
}

@Composable
fun CcChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color = CcColors.orange
) {
    Surface(
        color = if (selected) accent else CcColors.panel2,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) accent else CcColors.border),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else CcColors.muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun CcSmallButton(
    text: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (danger) Color(0xFF2A0E12) else CcColors.panel2
        ),
        border = BorderStroke(1.dp, if (danger) CcColors.red.copy(alpha = 0.6f) else CcColors.border),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = if (danger) Color(0xFFFF8A80) else CcColors.muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CcStatTile(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.verticalGradient(
                    listOf(CcColors.panel2, lerp(CcColors.panel, Color.Black, 0.18f))
                )
            )
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(9.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp)
    ) {
        Text(label, color = CcColors.muted, fontSize = 7.5.sp, letterSpacing = 0.6.sp, maxLines = 1, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = color,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            if (unit.isNotBlank()) {
                Spacer(Modifier.width(3.dp))
                Text(unit, color = CcColors.muted, fontSize = 8.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
fun CcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = CcColors.muted, fontSize = 11.sp) },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = CcColors.text,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        ),
        shape = RoundedCornerShape(7.dp),
        colors = CcTextFieldColors(),
        modifier = modifier.height(46.dp)
    )
}

@Composable
fun CcTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CcColors.orange,
    unfocusedBorderColor = CcColors.border,
    focusedContainerColor = CcColors.panel2,
    unfocusedContainerColor = CcColors.panel2,
    cursorColor = CcColors.orange
)

@Composable
private fun CcGearButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) CcColors.cyan else CcColors.panel2,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (selected) CcColors.cyan else CcColors.border),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else CcColors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}

// =====================================================================
// YARDIMCILAR
// =====================================================================

object CcColors {
    val bg = Color(0xFF06080D)
    val panel = Color(0xFF11161F)
    val panel2 = Color(0xFF181E29)
    val panel3 = Color(0xFF202837)
    val border = Color(0xFF273041)
    val borderAccent = Color(0x55FF6A00)
    val orange = Color(0xFFFF6A00)
    val orangeSoft = Color(0x26FF6A00)
    val cyan = Color(0xFF00E5FF)
    val green = Color(0xFF00E676)
    val red = Color(0xFFFF3D2E)
    val gold = Color(0xFFFFC400)
    val text = Color(0xFFF2F5FA)
    val muted = Color(0xFF97A5B9)
}

private fun GaugeDisplayCategory.label(): String = when (this) {
    GaugeDisplayCategory.MODERN_TFT -> "TFT"
    GaugeDisplayCategory.ANALOG_HYBRID -> "ANALOG + LCD"
    GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD -> "LCD"
    GaugeDisplayCategory.RETRO_CLASSIC -> "RETRO"
}

private fun GaugeDisplayCategory.shortLabel(): String = when (this) {
    GaugeDisplayCategory.MODERN_TFT -> "TFT"
    GaugeDisplayCategory.ANALOG_HYBRID -> "ANALOG"
    GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD -> "LCD"
    GaugeDisplayCategory.RETRO_CLASSIC -> "RETRO"
}

private fun GaugeDisplayCategory.tone(): Color = when (this) {
    GaugeDisplayCategory.MODERN_TFT -> CcColors.cyan
    GaugeDisplayCategory.ANALOG_HYBRID -> CcColors.orange
    GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD -> CcColors.green
    GaugeDisplayCategory.RETRO_CLASSIC -> CcColors.gold
}

private fun ShiftLightState.shortLabel(): String = when (this) {
    ShiftLightState.NORMAL -> "NORMAL"
    ShiftLightState.STAGE_1_APPROACHING -> "STAGE 1"
    ShiftLightState.STAGE_2_SHIFT_NOW -> "SHIFT"
    ShiftLightState.OVER_REV_LIMITER -> "LIMITER"
}

private fun formatDuration(seconds: Double): String {
    val s = seconds.toLong().coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(Locale.US, h, m, sec)
    else "%d:%02d".format(Locale.US, m, sec)
}

/** Galeri kartları için sabit, canlı görünümlü örnek telemetri. */
fun demoTelemetryFor(spec: DeclarativeGaugeSpec): UnifiedTelemetryState {
    val speed = when (spec.displayCategory) {
        GaugeDisplayCategory.MODERN_TFT -> 128.0
        GaugeDisplayCategory.ANALOG_HYBRID -> 112.0
        GaugeDisplayCategory.NEGATIVE_POSITIVE_LCD -> 96.0
        GaugeDisplayCategory.RETRO_CLASSIC -> 84.0
    }
    val rpm = spec.gaugeRedlineRpm * 0.82
    return UnifiedTelemetryState(
        timestampNanos = 10_000_000_000L,
        activeBikeProfileId = spec.skinId,
        activeBikeDisplayName = "${spec.brand} ${spec.modelName}",
        bikeIdleRpm = 1250.0,
        bikeRedlineRpm = spec.gaugeRedlineRpm,
        bikeRevLimiterRpm = spec.gaugeMaxRpm,
        trueSpeedMs = speed / 3.6,
        trueSpeedKmh = speed,
        displayedSpeedKmh = speed,
        actualEngineRpm = rpm,
        normalizedRpmRatio = 0.82,
        peakHoldRpm = rpm * 0.94,
        stage1ShiftRpm = spec.gaugeRedlineRpm * 0.92,
        stage2ShiftRpm = spec.gaugeRedlineRpm * 0.98,
        shiftLightState = ShiftLightState.NORMAL,
        shiftFlashPhaseOn = false,
        fullColorInversionActive = false,
        currentGear = 4,
        isNeutral = false,
        isClutchPulled = false,
        transmissionState = TransmissionEngagementState.ENGAGED_IN_GEAR,
        leanAngleDeg = if (spec.supportsLeanAndWheeliePod) 24.0 else 0.0,
        pitchAngleDeg = 0.0,
        effectiveTireRadiusMm = 306.9,
        estimatedWheelHorsepower = 128.0,
        estimatedWheelTorqueNm = 98.0,
        coolantTempCelsius = 86.0,
        fuelLevelRatio = 0.72f,
        ignitionBootPhase = com.oemcluster.pro.domain.IgnitionBootPhase.RUNNING_LIVE,
        ignitionSweepProgress = 1f,
        nightModeTransitionFactor = 1f,
        isWheelieActive = false,
        isWheelieSummaryVisible = false,
        gearConfidence = 1.0,
        gpsSpeedAgeSec = 0.2,
        gpsSpeedAccuracyMs = 0.35,
        isStationary = false,
        gpsSpeedSourceBlend = 1.0,
        gpsSatelliteCount = 14,
        gpsHorizontalAccuracyM = 2.4
    )
}


private fun matchesQuery(query: String, keywords: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim().lowercase(Locale.ROOT)
    return keywords.lowercase(Locale.ROOT).contains(q)
}

private fun settingsSectionVisible(query: String): Boolean =
    listOf(
        "canli durum live hiz hiz kilometre devir rpm vites gear gnss uydu telemetry",
        "dil language idioma langue sprache lingua turkce english espanol italiano francais deutsch portugais russo chinese",
        "birim units mph kmh sicaklik temperature fahrenheit celsius saat clock 12 24 yakit fuel mpg l100 l100km kml",
        "ekran display tema theme gunduz gece day night oled rpm yatis teker wheelie parlaklik",
        "shift light gear vites stage evre optimal ecu beep bip",
        "hiz speed kalibrasyon calibration ece r39 oem 299 japan",
        "sifirla zero yatis kalibrasyon imu",
        "gnss uydu satellite sensor imu",
        "uygulama app ustlabs ust labs launch acilis animasyon",
        "garaj garage fabrika factory sifirla reset",
        "odometer sayac kilometre km sifirla reset kalici"
    ).any { matchesQuery(query, it) }

private fun LazyListScope.settingsItem(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    if (visible) item { content() }
}

@Composable
private fun SettingsSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        placeholder = {
            Text("Ayarlar içinde ara…", color = CcColors.muted, fontSize = 12.sp)
        },
        leadingIcon = { Text("🔍", fontSize = 14.sp) },
        trailingIcon = {
            if (query.isNotBlank()) {
                Text(
                    "×",
                    modifier = Modifier
                        .clickable { onQueryChange("") }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = CcColors.muted,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CcColors.orange,
            unfocusedBorderColor = CcColors.border,
            focusedContainerColor = CcColors.panel2,
            unfocusedContainerColor = CcColors.panel2,
            cursorColor = CcColors.orange,
            focusedTextColor = CcColors.text,
            unfocusedTextColor = CcColors.text,
            focusedPlaceholderColor = CcColors.muted,
            unfocusedPlaceholderColor = CcColors.muted
        )
    )
}
