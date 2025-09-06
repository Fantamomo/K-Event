package com.fantamomo.kevent.manager.config

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.EventDsl

/**
 * Provides a scope for configuring event dispatch settings using a type-safe DSL.
 *
 * This class is used in conjunction with [DispatchConfig] and [DispatchConfigKey] to
 * define and manage configuration options for event dispatching in a flexible and
 * type-safe manner. The [DispatchConfigScope] enables storing, retrieving, and modifying
 * configuration values through a set of operations.
 *
 * This class is annotated with the [EventDsl] marker to enforce proper scoping rules
 * when used within a DSL context.
 *
 * @author Fantamomo
 * @since 1.9-SNAPSHOT
 */
@EventDsl
class DispatchConfigScope<E : Dispatchable> @PublishedApi internal constructor(internal val data: MutableMap<DispatchConfigKey<*>, Any?>) {

    /**
     * Creates a new instance of `DispatchConfigScope` with an empty mutable map
     * as the initial configuration data.
     */
    constructor() : this(mutableMapOf())

    /**
     * Sets the value associated with a specified [DispatchConfigKey] in the configuration data.
     *
     * @param key The configuration key for which the value should be set.
     * @param value The value to associate with the specified key.
     * @param T The type of the value associated with the configuration key.
     */
    operator fun <T> set(key: DispatchConfigKey<T>, value: T) {
        data[key] = value
    }

    /**
     * Retrieves the value associated with the specified [DispatchConfigKey] from the configuration data.
     * If the key is not found or the value cannot be cast to the expected type, `null` is returned.
     *
     * @param key The configuration key whose associated value is to be retrieved.
     * @return The value associated with the specified key, or `null` if the key is not present
     *         or the value cannot be cast to the expected type.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: DispatchConfigKey<T>): T? = data[key] as? T?

    /**
     * Retrieves the value associated with the specified [DispatchConfigKey] in the configuration.
     * If the key is not found or its value is null, the provided default value is returned.
     *
     * @param key The configuration key whose associated value is to be retrieved or whose
     * default value should be used if no value is present.
     * @param default The default value to return if the key is not present or its value is null.
     * @return The value associated with the specified key, or the provided default value
     * if no value exists.
     * @param T The type of the value associated with the configuration key.
     */
    fun <T> getOrDefault(key: DispatchConfigKey<T>, default: T) = get(key) ?: default

    /**
     * Retrieves the value associated with the specified configuration key or returns its default value
     * if the key is not present in the configuration or its value is null.
     *
     * @param key The configuration key whose associated value is to be retrieved.
     *            The key includes a default value, which will be returned if no other value is present
     *            or the associated value is null.
     * @return The value associated with the specified key, or the default value if the key's value is
     *         absent or null.
     * @param T The type of the value associated with the configuration key.
     */
    fun <T> getOrDefault(key: DispatchConfigKey<T>) = get(key) ?: key.defaultValue

    /**
     * Checks whether the specified [DispatchConfigKey] is present in the configuration data.
     *
     * @param key The configuration key to check for existence in the configuration data.
     * @return `true` if the key exists in the configuration data, otherwise `false`.
     * @param T The type of the value associated with the configuration key.
     */
    operator fun <T> contains(key: DispatchConfigKey<T>) = key in data

    /**
     * Checks whether the specified value exists in the configuration data.
     *
     * @param value The value to check for existence in the configuration data.
     * @return `true` if the value exists in the configuration data, otherwise `false`.
     */
    operator fun <T> contains(value: T) = value in data.values

    /**
     * Removes the value associated with the specified [DispatchConfigKey] from the configuration data.
     *
     * @param key The configuration key whose associated value should be removed.
     * @param T The type of the value associated with the configuration key.
     */
    fun <T> remove(key: DispatchConfigKey<T>) {
        data.remove(key)
    }
}