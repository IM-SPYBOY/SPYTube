package com.spytube.app.models

import kotlinx.coroutines.flow.MutableStateFlow

object GlobalNavState {
    val currentTab = MutableStateFlow(0)
}
