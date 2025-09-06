package com.fantamomo.kevent.manager.config

import com.fantamomo.kevent.Dispatchable

/**
 * Represents the configuration storage used within the dispatch system.
 *
 * The [DispatchConfig] interface defines the contract for accessing and managing configuration
 * data specific to dispatchable events. It ensures type-safe retrieval of configuration values
 * and provides utility methods for common operations like checking for key existence and
 * determining if the configuration is empty.
 *
 * The configuration keys are represented by instances of [DispatchConfigKey] associated with
 * specific types. This allows for strongly-typed lookups and default value management.
 *
 * @param E The type of [Dispatchable] events associated with this configuration.
 *
 * @author Fantamomo
 * @since 1.9-SNAPSHOT
 */
sealed interface DispatchConfig<E : Dispatchable> {
    /**
     * Represents the number of entries in the configuration data map.
     *
     * This property indicates the total count of key-value pairs currently held
     * within the configuration. It provides a way to monitor or validate the
     * amount of configuration data managed by the [DispatchConfig].
     *
     * @since 1.10-SNAPSHOT
     */
    val size: Int

    /**
     * Retrieves the value associated with the specified [DispatchConfigKey] in the configuration.
     * If the key is not found or the value cannot be cast to the expected type, `null` is returned.
     *
     * @param key The configuration key whose associated value is to be retrieved.
     * @return The value associated with the key, or `null` if the key is not present or type conversion fails.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: DispatchConfigKey<T>): T?

    /**
     * Retrieves the value associated with the specified [DispatchConfigKey] in the configuration.
     * Returns the key's default value if the key is not present or its value is null.
     *
     * @param key The configuration key whose associated value is to be retrieved or whose default value should be used.
     * @return The value associated with the key, or the key's default value if no value exists.
     */
    fun <T> getOrDefault(key: DispatchConfigKey<T>) = get(key) ?: key.defaultValue

    /**
     * Retrieves the value associated with the specified key in the configuration.
     * If the key is not found or its value is null, the provided default value is returned.
     *
     * @param key The configuration key whose associated value is to be retrieved.
     * @param default The value to return if the key is not present or its value is null.
     * @return The value associated with the key, or the provided default value if no value exists.
     */
    fun <T> getOrDefault(key: DispatchConfigKey<T>, default: T) = get(key) ?: default

    /**
     * Checks whether the specified configuration key is present in the data map.
     *
     * @param key The [DispatchConfigKey] to check for existence in the configuration data.
     * @return `true` if the key exists in the configuration data, otherwise `false`.
     */
    operator fun <T> contains(key: DispatchConfigKey<T>): Boolean

    /**
     * Checks whether the specified value exists in the configuration data.
     *
     * @param value The value to check for existence in the configuration data.
     * @return `true` if the value exists in the configuration data, otherwise `false`.
     */
    operator fun <T> contains(value: T): Boolean

    /**
     * Determines if the configuration is empty.
     *
     * This method checks whether the configuration data contains any entries.
     *
     * @return `true` if the configuration has no entries, `false` otherwise.
     *
     * @since 1.10-SNAPSHOT
     */
    fun isEmpty(): Boolean

    companion object Empty : DispatchConfig<Nothing> {

        /**
         * Produces a `DispatchConfig` instance based on the provided [scope].
         * If the [scope]'s data is empty, a predefined empty configuration is returned.
         *
         * @param scope The [DispatchConfigScope] containing configuration data used to
         *              initialize a new `DispatchConfig` instance.
         */
        operator fun <E : Dispatchable> invoke(scope: DispatchConfigScope<E>): DispatchConfig<E> =
            (if (scope.data.isEmpty()) empty() else DispatchConfigImpl(scope.data))

        /**
         * Returns the current instance of `DispatchConfig` casted to a version parameterized with the
         * generic type [E].
         *
         * @param E The type of `Dispatchable` event associated with the configuration.
         *
         * @since 1.10-SNAPSHOT
         */
        @Suppress("UNCHECKED_CAST")
        fun <E : Dispatchable> empty() = this as DispatchConfig<E>

        override val size: Int = 0

        override fun <T> get(key: DispatchConfigKey<T>) = null

        override fun <T> getOrDefault(key: DispatchConfigKey<T>) = key.defaultValue

        override fun <T> getOrDefault(key: DispatchConfigKey<T>, default: T) = default

        override fun <T> contains(key: DispatchConfigKey<T>) = false

        override fun <T> contains(value: T) = false
        override fun isEmpty() = true
    }

    /**
     * Implementation of the [DispatchConfig] interface that provides a mechanism to store
     * and retrieve configuration data for dispatchable events.
     *
     * This class encapsulates a map of configuration keys and their associated values,
     * ensuring type safety and efficient access to specific configurations.
     *
     * @param E The type of dispatchable entity this configuration applies to.
     * @constructor Internal constructor for creating an instance of [DispatchConfigImpl].
     * @property data The internal map storing configuration keys and their associated values.
     *
     * @since 1.10-SNAPSHOT
     * @author Fantamomo
     */
    class DispatchConfigImpl<E : Dispatchable> internal constructor(private val data: Map<DispatchConfigKey<*>, Any?>) :
        DispatchConfig<E> {
        override val size: Int
            get() = data.size

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: DispatchConfigKey<T>): T? = data[key] as? T

        override fun <T> contains(key: DispatchConfigKey<T>) = data.containsKey(key)

        override fun <T> contains(value: T) = data.containsValue(value)

        override fun isEmpty() = data.isEmpty()

        /**
         * Converts the internal immutable configuration data map into a mutable map.
         *
         * This method is intended for internal use when modifications to the configuration
         * data are necessary. It creates and returns a mutable copy of the existing data map,
         * ensuring that the original immutable map remains unchanged.
         *
         * @return A mutable map containing the same entries as the internal immutable data map.
         */
        @PublishedApi
        internal fun dataAsMutable() = data.toMutableMap()
    }
}