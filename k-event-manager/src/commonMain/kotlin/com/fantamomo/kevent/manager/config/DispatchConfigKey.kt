package com.fantamomo.kevent.manager.config

/**
 * Represents a configuration key used in the event dispatching mechanism.
 *
 * @param T The type of the value associated with the configuration key.
 * @param name The name of the configuration key, uniquely identifying its purpose.
 * @param defaultValue The default value assigned to the configuration key if no other value is specified.
 *
 * This class is used in conjunction with [DispatchConfig] and [DispatchConfigScope] to define and manage
 * configuration options for event dispatching. The key provides a way to define settings with an associated
 * type and default value, ensuring type-safety and clear defaults throughout the framework.
 *
 * @author Fantamomo
 * @since 1.9-SNAPSHOT
 */
data class DispatchConfigKey<T>(val name: String, val defaultValue: T) {
    companion object {
        /**
         * A predefined configuration key that enables or disables the dispatching of [com.fantamomo.kevent.DeadEvent].
         *
         * [com.fantamomo.kevent.DeadEvent] are events that are posted to the event dispatching mechanism but do not
         * have any registered listeners to handle them. When this flag is set to `true`, the
         * system allows dispatching these events, enabling the possibility of catching and
         * analyzing unhandled events.
         *
         * The default value for this configuration key is `true`, which means dispatching
         * dead events is permitted unless explicitly disabled.
         * @see com.fantamomo.kevent.DeadEvent
         */
        val DISPATCH_DEAD_EVENT = key("dispatch_dead_event", true)
        /**
         * A predefined configuration key that determines whether events are dispatched as "sticky."
         *
         * Sticky events remain cached in the event bus and are immediately delivered
         * to new listeners when they register, ensuring that they receive the most recent
         * instance of the event even if it was dispatched prior to their registration.
         *
         * The default value for this configuration key is `false`, meaning events are not
         * cached and only delivered to active listeners at the time of dispatch unless explicitly enabled.
         */
        val STICKY = key("sticky", false)

        private fun <T> key(name: String, defaultValue: T) = DispatchConfigKey(name, defaultValue)
    }
}