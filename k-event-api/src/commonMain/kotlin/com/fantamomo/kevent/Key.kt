package com.fantamomo.kevent

import com.fantamomo.kevent.Key.Companion.PRIORITY
import kotlin.reflect.KClass

/**
 * Type-safe key for storing and retrieving configuration options.
 *
 * `Key` provides a type-safe way to store and retrieve configuration options of any type.
 * Each key has a string identifier, a Kotlin class type, and a default value.
 *
 * The system includes built-in keys like [PRIORITY], but third-party libraries
 * can define their own keys for custom options.
 *
 * Example of defining custom keys:
 * ```
 * object MyKeys {
 *     val MY_OPTION = Key("myOption", "default value")
 *     val MY_FLAG = Key("myFlag", false)
 * }
 * ```
 *
 * Example of using a custom key:
 * ```
 * @Register
 * fun onMyEvent(event: MyEvent?) {
 *     configuration(event) {
 *         priority = Priority.HIGH
 *         disallowSubtypes = true
 *         set(MyKeys.MY_OPTION, "custom value")
 *         set(MyKeys.MY_FLAG, true)
 *     }
 * }
 * ```
 *
 * @param T The type of value associated with this key.
 * @property key The string identifier for this key.
 * @property type The Kotlin class representing the type `T`.
 * @property defaultValue The default value to use if no value is set.
 *
 * @see EventConfigurationScope
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
data class Key<T>(val key: String, val type: KClass<T & Any>, val defaultValue: T) {
    companion object {
        /**
         * Built-in key for setting an event handler's priority.
         *
         * Used with the [priority] extension property on [EventConfigurationScope].
         * @see priority
         */
        val PRIORITY = Key<Priority>("priority", Priority.Standard.NORMAL)

        /**
         * Built-in key to disallow all subtypes of an event for a listener.
         *
         * Used with the [disallowSubtypes] extension property on [EventConfigurationScope].
         * @see disallowSubtypes
         */
        val DISALLOW_SUBTYPES = Key<Boolean>("disallowSubtypes", false)

        /**
         * Built-in key for exclusive listener processing.
         *
         * If true, a listener will not be invoked for a new event while it is
         * actively processing another event. This ensures sequential handling.
         * Events fired while a listener is busy are ignored completely for that listener.
         *
         * @see exclusiveListenerProcessing
         */
        val EXCLUSIVE_LISTENER_PROCESSING = Key<Boolean>("exclusiveListenerProcessing", false)

        /**
         * Built-in key marking a listener as "silent".
         *
         * Silent listeners are ignored when determining if an event is handled.
         * Events with only silent listeners still trigger a [DeadEvent].
         *
         * @see com.fantamomo.kevent.silent
         */
        val SILENT = Key<Boolean>("silent", false)

        /**
         * Key controlling whether sticky events should be ignored.
         *
         * When true, handlers skip processing previously posted sticky events.
         * Sticky events are retained after dispatch to allow new subscribers
         * to receive the latest event immediately.
         *
         * Default: false (handlers process sticky events by default).
         *
         * @see Key
         * @see EventConfigurationScope.getOrDefault
         * @since 1.3-SNAPSHOT
         */
        val IGNORE_STICKY_EVENTS = Key<Boolean>("ignoreStickyEvents", false)

        /**
         * Debug-only key with a string identifier.
         *
         * Primarily used for debugging purposes, currently inactive in main logic.
         *
         * @see name
         */
        val NAME = Key<String?>("name", null)

        /**
         * Factory method for creating keys with inferred types.
         *
         * Simplifies creation by inferring the type from the reified type parameter.
         *
         * Example:
         * ```
         * val MY_KEY = Key("myKey", "default value")
         * ```
         *
         * @param key The string identifier for the key.
         * @param defaultValue The default value to use if no value is set.
         * @return A new Key instance with the specified parameters.
         */
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(key: String, defaultValue: T): Key<T> =
            Key(key, T::class as KClass<T & Any>, defaultValue)
    }
}
