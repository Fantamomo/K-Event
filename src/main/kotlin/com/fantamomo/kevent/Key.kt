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
         * Example:
         * ```
         * configuration(event) {
         *     priority = Priority.HIGH
         * }
         * ```
         * 
         * @see com.fantamomo.kevent.priority
         */
        val PRIORITY = Key<Priority>("priority", Priority.Standard.NORMAL)

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