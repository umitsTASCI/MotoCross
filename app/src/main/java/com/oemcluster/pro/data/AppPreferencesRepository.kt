package com.oemcluster.pro.data

import android.content.Context
import android.content.SharedPreferences
import com.oemcluster.pro.domain.RpmScalingMode
import com.oemcluster.pro.domain.WheelieGaugeDisplayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Uygulamanın kalıcı ayar ve seans kaydı.
 *
 * Gösterge seçimi, tüm ekran/telemetri ayarları, dil, yol bilgisayarı ve rekorlar
 * tek bir JSON belgesi olarak SharedPreferences'ta saklanır. Böylece uygulama
 * kapansa da garaj, gösterge ve sürüş verileri aynen geri yüklenir.
 */
@Serializable
data class ClusterPreferences(
    val schemaVersion: Int = 2,

    // Gösterge seçimi
    val selectedGaugeIndex: Int = 0,
    val selectedSubModeIndex: Int = 0,
    val favoriteSkinIds: List<String> = listOf(
        "ktm_390_duke_gen2",
        "bmw_s1000rr_k67",
        "yamaha_yzf_r1",
        "ducati_panigale_v4_evo",
        "suzuki_hayabusa_gen3",
        "honda_cbr1000rrr_sp",
        "kawasaki_zx10r_2021"
    ),

    // Ekran ve gösterim
    val useMph: Boolean = false,
    val useOemSpeedBias: Boolean = false,
    val oemBiasRatio: Float = 0.05f,
    val japan299Lock: Boolean = false,
    val rpmScalingMode: RpmScalingMode = RpmScalingMode.MODE_A_PROPORTIONAL_REDLINE,
    val wheelieDisplayMode: WheelieGaugeDisplayMode = WheelieGaugeDisplayMode.SMART_AUTO_POPUP,
    val themeOverrideMode: Int = 2,
    val chromaKeyGreen: Boolean = false,
    val oledPixelShiftEnabled: Boolean = true,

    // Shift-light / ECU
    val autoOptimalShift: Boolean = true,
    val manualStage1Rpm: Float = 8800f,
    val manualStage2Rpm: Float = 9800f,
    val reactionTimeMs: Float = 180f,
    val acousticMicEnabled: Boolean = false,

    // Dil
    val languageCode: String = "tr",

    // USTlabs açılış sekansı
    val launchAnimationEnabled: Boolean = true,

    // Yol bilgisayarı ve rekorlar
    val tripAKm: Double = 0.0,
    val tripBKm: Double = 0.0,
    val odometerKm: Double = 1420.5,
    val peakSpeedKmh: Double = 0.0,
    val peakRpm: Double = 0.0,
    val peakLeanLeftDeg: Double = 0.0,
    val peakLeanRightDeg: Double = 0.0,
    val peakWheelieDeg: Double = 0.0,
    val longestWheelieSec: Double = 0.0,
    val longestWheelieMeters: Double = 0.0,
    val rideTimeSeconds: Double = 0.0,

    // Drag / hızlanma ölçümleri (en iyi dereceler)
    val best0to100Sec: Double = 0.0,
    val best0to200Sec: Double = 0.0,
    val bestQuarterMileSec: Double = 0.0,

    // İlk açılış ipucu
    val onboardingSeen: Boolean = false
)

/**
 * SharedPreferences + kotlinx.serialization tabanlı kalıcı depo.
 * [init] uygulama açılışında bir kez çağrılır; sonrasında tüm ekranlar
 * [state] akışını dinler.
 */
object AppPreferencesRepository {

    private const val PREFS_NAME = "motocross_cluster_prefs_v2"
    private const val KEY_DOCUMENT = "cluster_document"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private lateinit var prefs: SharedPreferences

    private val _state = MutableStateFlow(ClusterPreferences())
    val state: StateFlow<ClusterPreferences> = _state.asStateFlow()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_DOCUMENT, null)
            if (!raw.isNullOrBlank()) {
                runCatching { json.decodeFromString<ClusterPreferences>(raw) }
                    .onSuccess { _state.value = it }
            }
            initialized = true
        }
    }

    /** Ayar değişikliklerinde anında kaydeder (küçük ve seyrek güncellemeler). */
    fun update(transform: (ClusterPreferences) -> ClusterPreferences) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
        write(next)
    }

    /**
     * Yol bilgisayarı gibi saniyede birbirçok kez değişen değerler için:
     * durumu günceller, diske yazmayı çağıran tarafın seyreltmesine bırakır.
     */
    fun updateInMemory(transform: (ClusterPreferences) -> ClusterPreferences) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
    }

    /** Anlık belgeyi diske yazar (periyodik seans kaydı / onPause). */
    fun flush() {
        if (!initialized) return
        write(_state.value)
    }

    private fun write(value: ClusterPreferences) {
        if (!initialized) return
        runCatching {
            prefs.edit().putString(KEY_DOCUMENT, json.encodeToString(ClusterPreferences.serializer(), value)).apply()
        }
    }
}
