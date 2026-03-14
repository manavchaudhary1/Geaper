package com.manav.geaper.worker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StatusChecker(val task: suspend () -> Unit) {

  fun start() {

    CoroutineScope(Dispatchers.IO).launch {
      while (true) {

        task()
      }
    }
  }
}
