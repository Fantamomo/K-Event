package com.fantamomo.kevent

/**
 * Immutable container for event handler configuration data.
 *
 * This class holds the final configuration for an event handler after it has been
 * registered. It provides type-safe access to the configuration options that were
 * set during initialization.
 *
 * Unlike [EventConfigurationScope], which is mutable and used during configuration,
 * this class is immutable and used during event handling.
 *
 * @param E The type of event this configuration applies to
 * @property data The internal map of configuration data
 *
 * @see EventConfigurationScope
 * @see Key
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
class EventConfiguration<E : Dispatchable>(private val data: Map<Key<*>, Any?>) {

    /**
     * Gets the value associated with the specified key, or null if no value is set.
     *
     * @param key The key to look up
     * @return The value associated with the key, or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: Key<T>) = data[key] as? T

    /**
     * Gets the value associated with the specified key, or the key's default value
     * if no value is set.
     *
     * @param key The key to look up
     * @return The value associated with the key, or the default value if not found
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getOrDefault(key: Key<T>) = data[key] as? T ?: key.defaultValue

    /**
     * Gets the value associated with the specified key, assuming it exists and is non-null.
     *
     * This method should only be used when you are certain that the key has a non-null value.
     * Otherwise, use [get] or [getOrDefault].
     *
     * @param key The key to look up
     * @return The non-null value associated with the key
     * @throws ClassCastException if the value is null or of the wrong type
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValue(key: Key<T>): T = data[key] as T

    /**
     * Checks if the specified key exists in the configuration data.
     *
     * @param key The key to check for existence in the configuration.
     * @return `true` if the key exists, `false` otherwise.
     */
    operator fun contains(key: Key<*>) = data.containsKey(key)

    /**
     * Checks whether the internal configuration data is empty.
     *
     * This method determines if there are no key-value pairs stored in the
     * configuration data.
     *
     * @return `true` if the configuration data is empty, `false` otherwise.
     */
    fun isEmpty(): Boolean = data.isEmpty()

    /**
     * Represents the size of the internal configuration data.
     *
     * This property provides the number of key-value pairs currently stored
     * within the configuration data. It corresponds to the size of the
     * underlying data structure used for storing configuration options.
     *
     * @return The total count of entries in the configuration data.
     */
    val size: Int
        get() = data.size

    companion object {
        /**
         * Default empty configuration used when no custom configuration is provided.
         *
         * This is used by [emptyConfiguration] to provide a default configuration
         * without requiring a configuration block.
         */
        val DEFAULT = EventConfiguration<Nothing>(emptyMap())

        /**
         * Returns a default [EventConfiguration] instance for the specified event type [E].
         *
         * @param E The event type for which the default configuration is returned.
         * @return The default [EventConfiguration] instance for the specified event type [E].
         */
        @Suppress("UNCHECKED_CAST")
        fun <E : Dispatchable> default() = DEFAULT as EventConfiguration<E>

        /**
         * Invokes the event configuration, returning either the default configuration or a new
         * event configuration based on the provided scope.
         *
         * @param scope The event configuration scope used to define custom configuration options.
         *              If the scope is empty, the default event configuration is returned.
         *
         * @since 1.1-SNAPSHOT
         */
        operator fun <D : Dispatchable> invoke(scope: EventConfigurationScope<D>) = if (scope.isEmpty()) default<D>() else EventConfiguration(scope.data.toMap())
    }
}