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
class EventConfiguration<E : Event>(private val data: Map<Key<*>, Any?>) {
    /**
     * Creates a new configuration from an [EventConfigurationScope].
     * 
     * This constructor is used during the initialization process to convert the
     * mutable configuration scope to an immutable configuration.
     * 
     * @param scope The configuration scope to convert
     */
    constructor(scope: EventConfigurationScope<E>) : this(scope.data.toMap())

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

    companion object {
        /**
         * Default empty configuration used when no custom configuration is provided.
         * 
         * This is used by [emptyConfiguration] to provide a default configuration
         * without requiring a configuration block.
         */
        val DEFAULT = EventConfiguration<Nothing>(emptyMap())
    }
}