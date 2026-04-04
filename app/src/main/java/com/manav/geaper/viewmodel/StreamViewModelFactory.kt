package com.manav.geaper.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.manav.geaper.data.repository.StreamRepository

class StreamViewModelFactory(
  private val app: Application,
  private val repository: StreamRepository,
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(StreamViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST") return StreamViewModel(app, repository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
