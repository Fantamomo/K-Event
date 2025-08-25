package com.fantamomo.kevent.manager.debug

/**
 * Configuration options for controlling the behavior of an event recorder.
 *
 * This class defines parameters that customize how the event recorder captures and processes events
 * in an event-driven system. These options include limits on the number of events to record, toggles
 * for recording additional context information, and an overall enable/disable flag for the recorder.
 *
 * @property maxEvents The maximum number of events the recorder should retain in memory. Once the limit
 *                     is reached, older events may be discarded to make space for new ones. Defaults to `1000`.
 * @property recordEventManagerInfo Indicates whether the associated event manager's class and method
 *                                  information should be recorded for each event. Defaults to `true`.
 * @property recordEventDispatcherInfo Indicates whether information about the dispatcher that raised the event,
 *                                     such as its class and method, should be recorded. Defaults to `true`.
 * @property enabled A flag to enable or disable the event recorder. If set to false, no events will be recorded.
 *                   Defaults to `true`.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
data class EventRecorderOptions(
    val maxEvents: Int = 1000,
    val recordEventManagerInfo: Boolean = true,
    val recordEventDispatcherInfo: Boolean = true,
    val enabled: Boolean = true
)