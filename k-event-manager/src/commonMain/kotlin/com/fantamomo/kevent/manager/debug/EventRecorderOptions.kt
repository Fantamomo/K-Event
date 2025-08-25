package com.fantamomo.kevent.manager.debug

data class EventRecorderOptions(
    val maxEvents: Int = 1000,
    val recordEventManagerInfo: Boolean = true,
    val recordEventDispatcherInfo: Boolean = true,
    val enabled: Boolean = true
)