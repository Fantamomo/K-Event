package com.fantamomo.kevent

import kotlin.reflect.KClass

/**
 * Type-safe key for storing and retrieving configuration options.
 *
 * The [Key] class is a central part of the event configuration system. It provides
 * a type-safe way to store and retrieve configuration options of any type. Each key
 * has a string identifier, a Kotlin class type, and a default value.
 *
 * The event system includes built-in keys like [PRIORITY], but third-party systems
 * can define their own keys to store custom configuration options.
 *
 * Example of defining a custom key:
 * ```
 * // In a third-party library
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
 *         // Built-in option
 *         priority = Priority.HIGH
 *         disallowSubtypes = true
 *
 *         // Custom option
 *         set(MyKeys.MY_OPTION, "custom value")
 *         set(MyKeys.MY_FLAG, true)
 *     }
 * }
 * ```
 *
 * @param T The type of value associated with this key
 * @property key The string identifier for this key
 * @property type The Kotlin class representing the type T
 * @property defaultValue The default value to use if no value is set
 *
 * @see EventConfigurationScope
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
data class Key<T>(val key: String, val type: KClass<T & Any>, val defaultValue: T) {
    companion object {
        /**
         * Built-in key for setting the priority of an event handler.
         *
         * This key is used with the [priority] extension property on [EventConfigurationScope].
         *
         * @see priority
         */
        val PRIORITY = Key<Priority>("priority", Priority.Standard.NORMAL)
        /**
         * Build-in key for defining if the listener disallow all subtypes of an event.
         *
         * This key is uses with the [disallowSubtypes] extension property on [EventConfigurationScope].
         *
         * @see disallowSubtypes
         */
        val DISALLOW_SUBTYPES = Key<Boolean>("disallowSubtypes", false)

        /**
         * Built-in key for defining if a listener should process events exclusively.
         *
         * If set to `true`, a listener will not be invoked for a new event if it
         * is already actively handling another event. This ensures that a single
         * listener instance processes events one by one, preventing overlapping execution.
         * **Crucially, if a listener is currently processing an event and another
         * event is fired, the listener will NEVER be called for that new event,
         * not even later. The event is effectively ignored for that busy listener.**
         *
         * @see
         */
        val EXCLUSIVE_LISTENER_PROCESSING = Key<Boolean>("exclusiveListenerProcessing", false)

        /**
         * Represents a debug-only key with a name identifier.
         *
         * This key's primary purpose is to serve debugging operations within the system,
         * and it does not currently have an active role in the main functionality.
         * It may potentially be used by other systems in the future for further expansion
         * or feature integration.
         */
        val NAME = Key<String>("name", "")

        /**
         * Convenience factory method for creating keys with reified types.
         *
         * This method simplifies the creation of keys by inferring the type from
         * the reified type parameter, eliminating the need to explicitly specify
         * the class.
         *
         * Example:
         * ```
         * val MY_KEY = Key("myKey", "default value")
         * ```
         *
         * @param key The string identifier for the key
         * @param defaultValue The default value to use if no value is set
         * @return A new Key instance with the specified parameters
         */
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(key: String, defaultValue: T): Key<T> =
            Key(key, T::class as KClass<T & Any>, defaultValue)
    }
}