package com.oemcluster.pro.data

import android.content.Context
import android.content.SharedPreferences
import com.oemcluster.pro.domain.BikeMechanicalProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * OEM-Cluster Pro — Mekanik motor profili veritabanı ve "Garajım" yöneticisi.
 *
 * ALTIN KURAL: Aktif motor profili uygulamanın TEK GERÇEK VERİ KAYNAĞIDIR.
 * Gösterge hangi OEM skin olursa olsun hız, vites, RPM ve shift-light değerleri
 * bu profilden hesaplanır.
 *
 * 307+ fabrika profili [BikePresetCatalog] içinde tanımlıdır. Kullanıcı
 * düzenlemeleri ve kendi eklediği motorlar SharedPreferences'ta kalıcıdır.
 */
object BikeDatabaseRepository {

    private const val PREFS_NAME = "motocross_garage_v2"
    private const val KEY_CUSTOM_PROFILES = "custom_profiles_json"
    private const val KEY_HIDDEN_FACTORY = "hidden_factory_ids_json"
    private const val KEY_ACTIVE_PROFILE = "active_profile_id"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private lateinit var prefs: SharedPreferences

    private val factoryPresets: List<BikeMechanicalProfile> = BikePresetCatalog.all
    private val factoryById: Map<String, BikeMechanicalProfile> = factoryPresets.associateBy { it.profileId }

    /** Kullanıcının değiştirdiği fabrika profilleri + tamamen kendi ekledikleri. */
    private var overrides: MutableMap<String, BikeMechanicalProfile> = linkedMapOf()

    /** Kullanıcının sildiği fabrika profilleri (uygulama güncellemelerinde geri gelmesin). */
    private var hiddenFactoryIds: MutableSet<String> = linkedSetOf()

    private val merged = MutableStateFlow(factoryPresets)
    val userGarageProfiles: StateFlow<List<BikeMechanicalProfile>> = merged.asStateFlow()

    private val _activeBikeProfile = MutableStateFlow(factoryPresets.first())
    val activeBikeProfile: StateFlow<BikeMechanicalProfile> = _activeBikeProfile.asStateFlow()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_CUSTOM_PROFILES, null)?.let { raw ->
                runCatching {
                    json.decodeFromString<List<BikeMechanicalProfile>>(raw)
                }.onSuccess { list ->
                    overrides = list.associateByTo(linkedMapOf()) { it.profileId }
                }
            }
            prefs.getString(KEY_HIDDEN_FACTORY, null)?.let { raw ->
                runCatching {
                    json.decodeFromString<List<String>>(raw)
                }.onSuccess { hiddenFactoryIds = it.toMutableSet() }
            }
            rebuildMerged()
            val activeId = prefs.getString(KEY_ACTIVE_PROFILE, null)
            _activeBikeProfile.value = merged.value.firstOrNull { it.profileId == activeId }
                ?: merged.value.first()
            initialized = true
        }
    }

    fun getAllFactoryPresets(): List<BikeMechanicalProfile> = factoryPresets

    fun getById(profileId: String): BikeMechanicalProfile? =
        merged.value.firstOrNull { it.profileId == profileId }

    fun isFactoryPreset(profileId: String): Boolean = factoryById.containsKey(profileId)

    fun selectActiveBikeProfile(profileId: String) {
        val found = merged.value.firstOrNull { it.profileId == profileId } ?: return
        _activeBikeProfile.value = found
        persistActive()
    }

    /**
     * Kullanıcının dişli, lastik, redline, tork eğrisi, ağırlık gibi
     * değişikliklerini kaydeder ve istenirse aktif profil yapar.
     */
    fun upsertBikeProfile(updatedProfile: BikeMechanicalProfile, setAsActive: Boolean = true) {
        overrides[updatedProfile.profileId] = updatedProfile
        hiddenFactoryIds.remove(updatedProfile.profileId)
        rebuildMerged()
        if (setAsActive) {
            _activeBikeProfile.value = updatedProfile
            persistActive()
        } else if (_activeBikeProfile.value.profileId == updatedProfile.profileId) {
            _activeBikeProfile.value = updatedProfile
        }
        persistOverrides()
    }

    fun duplicateBikeProfile(sourceId: String, newProfileId: String, newName: String): BikeMechanicalProfile? {
        val source = getById(sourceId) ?: return null
        val copy = source.copy(
            profileId = newProfileId,
            modelName = newName,
            modelYear = source.modelYear
        )
        overrides[copy.profileId] = copy
        rebuildMerged()
        persistOverrides()
        return copy
    }

    /**
     * Özel profili tamamen siler; fabrika profili ise fabrika değerlerine döndürür.
     */
    fun deleteBikeProfile(profileId: String) {
        if (factoryById.containsKey(profileId)) {
            overrides.remove(profileId)
            hiddenFactoryIds.add(profileId)
        } else {
            overrides.remove(profileId)
        }
        rebuildMerged()
        if (_activeBikeProfile.value.profileId == profileId) {
            _activeBikeProfile.value = merged.value.first()
            persistActive()
        }
        persistOverrides()
    }

    /** Bir fabrika profilindeki tüm kullanıcı değişikliklerini geri alır. */
    fun restoreFactoryPreset(profileId: String) {
        val factory = factoryById[profileId] ?: return
        overrides.remove(profileId)
        hiddenFactoryIds.remove(profileId)
        rebuildMerged()
        if (_activeBikeProfile.value.profileId == profileId) {
            _activeBikeProfile.value = factory
            persistActive()
        }
        persistOverrides()
    }

    /** Tüm garajı fabrika haline döndürür. */
    fun resetGarageToFactory() {
        overrides.clear()
        hiddenFactoryIds.clear()
        rebuildMerged()
        _activeBikeProfile.value = merged.value.first()
        persistOverrides()
        persistActive()
    }

    private fun rebuildMerged() {
        val list = ArrayList<BikeMechanicalProfile>(factoryPresets.size + overrides.size)
        for (factory in factoryPresets) {
            if (hiddenFactoryIds.contains(factory.profileId)) continue
            list.add(overrides[factory.profileId] ?: factory)
        }
        for ((id, profile) in overrides) {
            if (!factoryById.containsKey(id)) list.add(profile)
        }
        merged.value = list
        val activeId = _activeBikeProfile.value.profileId
        merged.value.firstOrNull { it.profileId == activeId }?.let { _activeBikeProfile.value = it }
    }

    private fun persistOverrides() {
        if (!initialized) return
        runCatching {
            val payload = overrides.values.toList()
            prefs.edit()
                .putString(
                    KEY_CUSTOM_PROFILES,
                    json.encodeToString(
                        kotlinx.serialization.builtins.ListSerializer(BikeMechanicalProfile.serializer()),
                        payload
                    )
                )
                .putString(
                    KEY_HIDDEN_FACTORY,
                    json.encodeToString(
                        ListSerializer(String.serializer()),
                        hiddenFactoryIds.toList()
                    )
                )
                .apply()
        }
    }

    private fun persistActive() {
        if (!initialized) return
        prefs.edit().putString(KEY_ACTIVE_PROFILE, _activeBikeProfile.value.profileId).apply()
    }
}
