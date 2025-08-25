package com.fantamomo.kevent.manager.debug

import com.fantamomo.kevent.Dispatchable
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class RecordedEvent(
    val event: Dispatchable,
    val timestamp: Instant,
    val threadName: String,
    val eventManagerClass: String? = null,
    val eventManagerMethod: String? = null,
    val triggeringClass: String? = null,
    val triggeringMethod: String? = null,
) {
    val eventType: KClass<out Dispatchable> = event::class
}