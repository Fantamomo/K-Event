package com.fantamomo.kevent.manager.debug

import com.fantamomo.kevent.Dispatchable
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents an event that has been recorded in an event-driven system.
 *
 * This class is used to encapsulate details about an event, including the type of the event,
 * the time at which it occurred, the thread that triggered it, and optional metadata such as
 * the class and method associated with the event's generation or management.
 *
 * @property event The instance of the dispatchable event that was recorded.
 * @property timestamp The time at which the event occurred, represented as an [Instant].
 * @property threadName The name of the thread that triggered the event.
 * @property eventManagerClass Optional: The fully qualified name of the class responsible for managing the event.
 * @property eventManagerMethod Optional: The method of the event manager that processed or dispatched the event.
 * @property triggeringClass Optional: The fully qualified name of the class that triggered the event.
 * @property triggeringMethod Optional: The method of the triggering class that caused the event to be dispatched.
 * @property eventType The Kotlin class of the event, derived from the event's runtime type.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
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