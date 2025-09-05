package com.fantamomo.kevent.manager.config

/**
 * Represents a configuration for event dispatching.
 *
 * @constructor Creates a DispatchConfig instance with a defined set of configuration data.
 * @param data A map of configuration keys and their corresponding values.
 *
 * @author Fantamomo
 * @since 1.9-SNAPSHOT
 */
class DispatchConfig(private val data: Map<DispatchConfigKey<*>, Any?>) {

    /**
     * Retrieves the value associated with the specified [DispatchConfigKey] in the configuration.
     * If the key is not found or the value cannot be cast to the expected type, `null` is returned.
     *
     * @param key The configuration key whose associated value is to be retrieved.
     * @return The value associated with the key, or `null` if the key is not present or type conversion fails.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: DispatchConfigKey<T>): T? = data[key] as? T?

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
    operator fun <T> contains(key: DispatchConfigKey<T>) = key in data

    /**
     * Checks whether the specified value exists in the configuration data.
     *
     * @param value The value to check for existence in the configuration data.
     * @return `true` if the value exists in the configuration data, otherwise `false`.
     */
    operator fun <T> contains(value: T) = value in data.values

    companion object {
        /**
         * Represents an empty [DispatchConfig] instance.
         *
         * This constant is intended for usage where a default or empty configuration
         * is required in dispatching events. It is initialized with an empty data map,
         * ensuring no configuration values are preset.
         */
        val EMPTY = DispatchConfig(emptyMap())

        /**
         * Produces a `DispatchConfig` instance based on the provided [scope].
         * If the [scope]'s data is empty, a predefined empty configuration is returned.
         *
         * @param scope The [DispatchConfigScope] containing configuration data used to
         *              initialize a new `DispatchConfig` instance.
         */
        operator fun invoke(scope: DispatchConfigScope) = if (scope.data.isEmpty()) EMPTY else DispatchConfig(scope.data)
    }
}