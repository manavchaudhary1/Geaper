package com.manav.geaper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.manav.geaper.data.prefs.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val prefs: AppPreferences) : ViewModel() {

  val savePath = prefs.savePath.stateIn(viewModelScope, SharingStarted.Lazily, "")
  val segmentMinutes = prefs.segmentMinutes.stateIn(viewModelScope, SharingStarted.Lazily, 0)
  val themeMode = prefs.themeMode.stateIn(viewModelScope, SharingStarted.Lazily, "system")
  val dynamicColor = prefs.dynamicColor.stateIn(viewModelScope, SharingStarted.Lazily, true)
  val cbWmToken = prefs.cbWmToken.stateIn(viewModelScope, SharingStarted.Lazily, "jeQ1L")

  fun setSavePath(path: String) {
    viewModelScope.launch { prefs.setSavePath(path) }
  }

  fun setSegmentMinutes(minutes: Int) {
    viewModelScope.launch { prefs.setSegmentMinutes(minutes) }
  }

  fun setThemeMode(mode: String) {
    viewModelScope.launch { prefs.setThemeMode(mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { prefs.setDynamicColor(enabled) }
  }

  fun setCbWmToken(token: String) {
    viewModelScope.launch { prefs.setCbWmToken(token) }
  }
}

class SettingsViewModelFactory(private val prefs: AppPreferences) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST") return SettingsViewModel(prefs) as T
    }
    throw IllegalArgumentException("Unknown ViewModel")
  }
}
