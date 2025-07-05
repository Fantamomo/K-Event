package com.fantamomo.kevent

/**
 * Utility extension function to get a configuration value or its default.
 * 
 * This function retrieves the value associated with the specified key from the
 * configuration scope, or returns the key's default value if no value is set.
 * 
 * It's used internally by other parts of the event system, such as the [priority]
 * extension property, to provide default values for configuration options.
 * 
 * @param key The key to look up
 * @return The value associated with the key, or the default value if not found
 * 
 * @see EventConfigurationScope.get
 * @see Key.defaultValue
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun <T> EventConfigurationScope<*>.getOrDefault(key: Key<T>): T = get(key) ?: key.defaultValue