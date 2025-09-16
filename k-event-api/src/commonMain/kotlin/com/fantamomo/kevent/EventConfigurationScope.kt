package com.fantamomo.kevent

/**
 * Scope for configuring event handlers during initialization.
 *
 * Provides a type-safe way to store and retrieve configuration options for event handlers.
 * Used within the configuration block of the [configuration] function to define how
 * an event should be handled.
 *
 * Configuration data is stored as key-value pairs, where keys are instances of [Key]
 * and values can be of any type specified by the key. This ensures type-safe access
 * while allowing flexible extension.
 *
 * Example usage:
 * ```
 * configuration(event) {
 *     // 'this' is an EventConfigurationScope<MyEvent>
 *     priority = Priority.HIGH
 *
 *     // Custom options can be set using custom keys
 *     set(MyCustomKey, "custom value")
 * }
 * ```
 *
 * @param E The type of event this configuration applies to.
 *
 * @see configuration
 * @see Key
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@EventDsl
class EventConfigurationScope<E : KEventElement> : EventConfiguration<E> {
    /**
     * Internal storage for configuration data as key-value pairs.
     */
    internal val data: MutableMap<Key<*>, Any?> = mutableMapOf()

    /**
     * Returns the value associated with the specified key, or `null` if not set.
     *
     * @param key The key to look up.
     * @return The value associated with the key, or `null` if not found.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Key<T>): T? = data[key] as? T

    override fun <T> getOrDefault(key: Key<T>): T = get(key) ?: key.defaultValue

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: Key<T>): T = data[key] as T

    /**
     * Sets a configuration option for the given key and value.
     *
     * @param key The key to associate with the value.
     * @param value The value to store.
     */
    operator fun <T> set(key: Key<T>, value: T) {
        data[key] = value
    }

    /**
     * Checks if the specified key exists in the configuration scope.
     *
     * @param key The key to check.
     * @return `true` if the key exists, `false` otherwise.
     */
    override fun contains(key: Key<*>) = data.containsKey(key)

    /**
     * Removes a configuration option for the specified key.
     *
     * @param key The key to remove.
     * @return The previous value associated with the key, or `null` if none.
     */
    fun remove(key: Key<*>) = data.remove(key)

    override fun isEmpty() = data.isEmpty()

    override val size: Int
        get() = data.size
}