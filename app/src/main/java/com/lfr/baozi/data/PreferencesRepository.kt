package com.lfr.baozi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferenceKeys {
    val API_KEY = stringPreferencesKey("api_key")
    val BASE_URL = stringPreferencesKey("base_url")
    val MODEL_NAME = stringPreferencesKey("model_name")
    val FLOATING_WINDOW_ENABLED = booleanPreferencesKey("floating_window_enabled")
    val INPUT_MODE = intPreferencesKey("input_mode")
    val IMAGE_COMPRESSION_ENABLED = booleanPreferencesKey("image_compression_enabled")
    val IMAGE_COMPRESSION_LEVEL = intPreferencesKey("image_compression_level")
    val MAX_STEPS = intPreferencesKey("max_steps")
    // App control selection
    val ENABLED_APPS = stringSetPreferencesKey("enabled_apps") // legacy: allow-list (may be large)
    val DISABLED_APPS = stringSetPreferencesKey("disabled_apps") // deny-list (preferred)
    val APPS_FILTER_MODE = intPreferencesKey("apps_filter_mode") // 0: deny-list, 1: allow-list
    val HAS_SHOWN_APPS_PERMISSION_GUIDE =
        booleanPreferencesKey("has_shown_apps_permission_guide")
}

enum class InputMode(val value: Int) {
    SET_TEXT(0),    // 直接设置文本
    PASTE(1),       // 复制粘贴
    IME(2);         // 输入法模拟

    companion object {
        fun fromInt(value: Int) = InputMode.entries.firstOrNull { it.value == value } ?: SET_TEXT
    }
}

enum class AppsFilterMode(val value: Int) {
    DENY_LIST(0),
    ALLOW_LIST(1);

    companion object {
        fun fromIntOrNull(value: Int?): AppsFilterMode? = entries.firstOrNull { it.value == value }
    }
}

class PreferencesRepository(private val context: Context) {

    companion object {
        const val DEFAULT_BASE_URL = "http://47.99.92.117:28100/v1"
        const val DEFAULT_MODEL_NAME = "autoglm-phone-9b"
        const val DEFAULT_MAX_STEPS = 50
    }

    val apiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.API_KEY]
    }

    val baseUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.BASE_URL] ?: DEFAULT_BASE_URL
    }

    val modelName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.MODEL_NAME] ?: DEFAULT_MODEL_NAME
    }

    val floatingWindowEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.FLOATING_WINDOW_ENABLED] ?: false
    }

    val inputMode: Flow<InputMode> = context.dataStore.data.map { preferences ->
        InputMode.fromInt(preferences[PreferenceKeys.INPUT_MODE] ?: 0)
    }

    val imageCompressionEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.IMAGE_COMPRESSION_ENABLED] ?: false
    }

    val imageCompressionLevel: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.IMAGE_COMPRESSION_LEVEL] ?: 50
    }

    val maxSteps: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.MAX_STEPS] ?: DEFAULT_MAX_STEPS
    }

    val enabledApps: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.ENABLED_APPS] ?: emptySet()
    }

    val disabledApps: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.DISABLED_APPS] ?: emptySet()
    }

    val hasShownAppsPermissionGuide: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.HAS_SHOWN_APPS_PERMISSION_GUIDE] ?: false
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.API_KEY] = apiKey
        }
    }

    suspend fun saveBaseUrl(baseUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.BASE_URL] = baseUrl
        }
    }

    suspend fun saveModelName(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.MODEL_NAME] = modelName
        }
    }

    suspend fun saveFloatingWindowEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.FLOATING_WINDOW_ENABLED] = enabled
        }
    }

    suspend fun saveInputMode(mode: InputMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.INPUT_MODE] = mode.value
        }
    }

    suspend fun saveImageCompressionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.IMAGE_COMPRESSION_ENABLED] = enabled
        }
    }

    suspend fun saveImageCompressionLevel(level: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.IMAGE_COMPRESSION_LEVEL] = level
        }
    }

    suspend fun saveMaxSteps(maxSteps: Int) {
        val clamped = maxSteps.coerceIn(1, 500)
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.MAX_STEPS] = clamped
        }
    }

    suspend fun saveEnabledApps(enabledApps: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.APPS_FILTER_MODE] = AppsFilterMode.ALLOW_LIST.value
            preferences[PreferenceKeys.ENABLED_APPS] = enabledApps
            preferences.remove(PreferenceKeys.DISABLED_APPS)
        }
    }

    suspend fun saveDisabledApps(disabledApps: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.APPS_FILTER_MODE] = AppsFilterMode.DENY_LIST.value
            preferences[PreferenceKeys.DISABLED_APPS] = disabledApps
            preferences.remove(PreferenceKeys.ENABLED_APPS)
        }
    }

    suspend fun saveHasShownAppsPermissionGuide(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.HAS_SHOWN_APPS_PERMISSION_GUIDE] = shown
        }
    }

    suspend fun toggleAppEnabled(packageName: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val mode =
                AppsFilterMode.fromIntOrNull(preferences[PreferenceKeys.APPS_FILTER_MODE])
            val legacyAllowList =
                preferences[PreferenceKeys.ENABLED_APPS]?.toMutableSet() ?: mutableSetOf()
            val denyList =
                preferences[PreferenceKeys.DISABLED_APPS]?.toMutableSet() ?: mutableSetOf()

            val effectiveMode =
                mode
                    ?: if (legacyAllowList.isNotEmpty()) AppsFilterMode.ALLOW_LIST
                    else AppsFilterMode.DENY_LIST

            preferences[PreferenceKeys.APPS_FILTER_MODE] = effectiveMode.value

            when (effectiveMode) {
                AppsFilterMode.DENY_LIST -> {
                    if (enabled) denyList.remove(packageName) else denyList.add(packageName)
                    preferences[PreferenceKeys.DISABLED_APPS] = denyList
                    preferences.remove(PreferenceKeys.ENABLED_APPS)
                }
                AppsFilterMode.ALLOW_LIST -> {
                    if (enabled) legacyAllowList.add(packageName) else legacyAllowList.remove(packageName)
                    preferences[PreferenceKeys.ENABLED_APPS] = legacyAllowList
                    preferences.remove(PreferenceKeys.DISABLED_APPS)
                }
            }
        }
    }

    suspend fun getApiKeySync(): String? {
        return context.dataStore.data.map { it[PreferenceKeys.API_KEY] }.firstOrNull()
    }

    suspend fun getBaseUrlSync(): String {
        return context.dataStore.data.map {
            it[PreferenceKeys.BASE_URL] ?: DEFAULT_BASE_URL
        }.firstOrNull() ?: DEFAULT_BASE_URL
    }

    suspend fun getModelNameSync(): String {
        return context.dataStore.data.map {
            it[PreferenceKeys.MODEL_NAME] ?: DEFAULT_MODEL_NAME
        }.firstOrNull() ?: DEFAULT_MODEL_NAME
    }

    suspend fun getFloatingWindowEnabledSync(): Boolean {
        return context.dataStore.data.map {
            it[PreferenceKeys.FLOATING_WINDOW_ENABLED] ?: false
        }.firstOrNull() ?: false
    }

    suspend fun getInputModeSync(): InputMode {
        return context.dataStore.data.map {
            InputMode.fromInt(it[PreferenceKeys.INPUT_MODE] ?: 0)
        }.firstOrNull() ?: InputMode.SET_TEXT
    }

    suspend fun getImageCompressionEnabledSync(): Boolean {
        return context.dataStore.data.map {
            it[PreferenceKeys.IMAGE_COMPRESSION_ENABLED] ?: false
        }.firstOrNull() ?: false
    }

    suspend fun getImageCompressionLevelSync(): Int {
        return context.dataStore.data.map {
            it[PreferenceKeys.IMAGE_COMPRESSION_LEVEL] ?: 50
        }.firstOrNull() ?: 50
    }

    suspend fun getMaxStepsSync(): Int {
        return context.dataStore.data.map {
            it[PreferenceKeys.MAX_STEPS] ?: DEFAULT_MAX_STEPS
        }.firstOrNull() ?: DEFAULT_MAX_STEPS
    }

    suspend fun getEnabledAppsSync(): Set<String> {
        return context.dataStore.data.map {
            it[PreferenceKeys.ENABLED_APPS] ?: emptySet()
        }.firstOrNull() ?: emptySet()
    }

    suspend fun getDisabledAppsSync(): Set<String> {
        return context.dataStore.data.map {
            it[PreferenceKeys.DISABLED_APPS] ?: emptySet()
        }.firstOrNull() ?: emptySet()
    }

    suspend fun getAppsFilterModeSyncOrNull(): AppsFilterMode? {
        return context.dataStore.data.map { prefs ->
            AppsFilterMode.fromIntOrNull(prefs[PreferenceKeys.APPS_FILTER_MODE])
        }.firstOrNull()
    }

    suspend fun getHasShownAppsPermissionGuideSync(): Boolean {
        return context.dataStore.data.map {
            it[PreferenceKeys.HAS_SHOWN_APPS_PERMISSION_GUIDE] ?: false
        }.firstOrNull() ?: false
    }

    suspend fun isAppEnabled(packageName: String): Boolean {
        return context.dataStore.data.map { prefs ->
            val mode = AppsFilterMode.fromIntOrNull(prefs[PreferenceKeys.APPS_FILTER_MODE])
            val allowList = prefs[PreferenceKeys.ENABLED_APPS] ?: emptySet()
            val denyList = prefs[PreferenceKeys.DISABLED_APPS] ?: emptySet()

            when (mode) {
                AppsFilterMode.DENY_LIST -> !denyList.contains(packageName)
                AppsFilterMode.ALLOW_LIST -> allowList.contains(packageName)
                null -> {
                    // Legacy fallback:
                    // - if allow-list is empty, treat as "allow all" (unconfigured default)
                    // - otherwise, enforce allow-list membership.
                    if (allowList.isEmpty()) true else allowList.contains(packageName)
                }
            }
        }.firstOrNull() ?: true
    }
}
