package com.manav.geaper.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "geaper_settings")

class AppPreferences(private val context: Context) {

  companion object {
    val KEY_SAVE_PATH = stringPreferencesKey("save_path")
    val KEY_SEGMENT_MINUTES = intPreferencesKey("segment_minutes")
    val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "system" | "light" | "dark"
    val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    /** Chaturbate affiliate wm token used in the API URL. */
    val KEY_CB_WM_TOKEN = stringPreferencesKey("cb_wm_token")
  }

  val savePath: Flow<String> = context.dataStore.data.map { it[KEY_SAVE_PATH] ?: "" }
  val segmentMinutes: Flow<Int> = context.dataStore.data.map { it[KEY_SEGMENT_MINUTES] ?: 0 }
  val themeMode: Flow<String> = context.dataStore.data.map { it[KEY_THEME_MODE] ?: "system" }
  val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: true }
  /** The wm= token appended to the Chaturbate online-rooms API URL. */
  val cbWmToken: Flow<String> = context.dataStore.data.map { it[KEY_CB_WM_TOKEN] ?: "jeQ1L" }

  suspend fun setSavePath(path: String) = context.dataStore.edit { it[KEY_SAVE_PATH] = path }

  suspend fun setSegmentMinutes(minutes: Int) =
    context.dataStore.edit { it[KEY_SEGMENT_MINUTES] = minutes }

  suspend fun setThemeMode(mode: String) = context.dataStore.edit { it[KEY_THEME_MODE] = mode }

  suspend fun setDynamicColor(enabled: Boolean) =
    context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }

  suspend fun setCbWmToken(token: String) = context.dataStore.edit { it[KEY_CB_WM_TOKEN] = token }
}
