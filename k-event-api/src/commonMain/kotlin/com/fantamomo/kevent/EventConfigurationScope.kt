package com.fantamomo.kevent

/**
 * Scope class for configuring event handlers during initialization.
 *
 * This class provides a type-safe way to store and retrieve configuration options
 * for event handlers. It's used within the configuration block of the [configuration]
 * function to set various options for how an event should be handled.
 *
 * The configuration data is stored as key-value pairs, where the keys are instances
 * of [Key] and the values can be of any type specified by the key. This approach
 * allows for type-safe access to configuration options while still providing flexibility
 * for extension.
 *
 * Example usage:
 * ```
 * configuration(event) {
 *     // 'this' is an EventConfigurationScope<MyEvent>
 *     priority = Priority.HIGH
 *
 *     // Custom configuration options can be set using custom keys
 *     set(MyCustomKey, "custom value")
 * }
 * ```
 *
 * @param E The type of event this configuration applies to
 *
 * @see configuration
 * @see Key
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@EventDsl
class EventConfigurationScope<E : Dispatchable> : EventConfiguration<E> {
    /**
     * Internal storage for configuration data.
     */
    internal val data: MutableMap<Key<*>, Any?> = mutableMapOf()

    /**
     * Gets the value associated with the specified key, or null if no value is set.
     *
     * @param key The key to look up
     * @return The value associated with the key, or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Key<T>): T? = data[key] as? T

    override fun <T> getOrDefault(key: Key<T>): T = get(key) ?: key.defaultValue

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: Key<T>): T = data[key] as T

    /**
     * Sets a configuration option with the specified key and value.
     *
     * @param key The key to associate the value with
     * @param value The value to store
     */
    operator fun <T> set(key: Key<T>, value: T) {
        data[key] = value
    }

    /**
     * Checks whether the specified key is present in the configuration scope.
     *
     * @param key The key to check for presence in the configuration.
     * @return `true` if the key is present, `false` otherwise.
     */
    override fun contains(key: Key<*>) = data.containsKey(key)

    /**
     * Removes a configuration option with the specified key.
     *
     * @param key The key to remove
     * @return The previous value associated with the key, or null if none
     */
    fun remove(key: Key<*>) = data.remove(key)

    override fun isEmpty() = data.isEmpty()

    override val size: Int
        get() = data.size
}