package com.fantamomo.kevent

import com.fantamomo.kevent.EventConfiguration.EventConfigurationImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Type-safe read-only access to the internal key-value data of an event configuration.
 *
 * This property returns the underlying map of key-value pairs from an [EventConfigurationImpl].
 * If the configuration instance is not an [EventConfigurationImpl], an empty map is returned.
 *
 * @receiver The event configuration instance.
 * @return A map of key-value pairs representing the configuration data.
 * @since 1.5-SNAPSHOT
 */
@PublishedApi
internal val EventConfiguration<*>.data: Map<Key<*>, Any?>
    get() = (this as? EventConfigurationImpl)?.data ?: emptyMap()

/**
 * Provides a mutable copy of the current configuration's internal data.
 *
 * The returned map is a copy and modifying it does not affect the original immutable configuration.
 * Intended primarily for internal library use or advanced scenarios requiring direct manipulation.
 *
 * @receiver The event configuration instance.
 * @return A mutable map containing the configuration data.
 * @since 1.5-SNAPSHOT
 */
@PublishedApi
internal inline val EventConfiguration<*>.mutableData: MutableMap<Key<*>, Any?>
    get() = data.toMutableMap()

/**
 * Adds a key-value pair to the configuration only if the key does not already exist.
 *
 * If the key is present, the original configuration is returned unchanged.
 *
 * @param key The key to add.
 * @param value The value to associate with the key.
 * @return A new configuration instance with the key-value pair added, or the original configuration.
 * @since 1.5-SNAPSHOT
 */
fun <E : Dispatchable, T> EventConfiguration<E>.setIfAbsent(key: Key<T>, value: T): EventConfiguration<E> = when {
    key in this -> this
    isEmpty() -> EventConfigurationImpl(mutableMapOf(key to value))
    else -> EventConfigurationImpl(mutableData.apply { put(key, value) })
}

/**
 * Removes the specified key from the configuration.
 *
 * @param key The key to remove.
 * @return A new configuration without the specified key.
 * @since 1.5-SNAPSHOT
 */
fun <E : Dispatchable, T> EventConfiguration<E>.remove(key: Key<T>): EventConfiguration<E> =
    if (key !in this) this
    else EventConfigurationImpl(mutableData.apply { remove(key) })

/**
 * Returns the value associated with a key, or the provided default if the key is absent.
 *
 * @param key The key to retrieve.
 * @param default The default value to return if the key is not present.
 * @return The value associated with the key, or the default.
 * @since 1.5-SNAPSHOT
 */
@Suppress("UNCHECKED_CAST")
fun <E : Dispatchable, T> EventConfiguration<E>.getOrDefault(key: Key<T>, default: T): T =
    (this as? EventConfigurationImpl?)?.data[key] as? T ?: default

/**
 * Updates the value of an existing key using a provided function.
 *
 * If the key does not exist, the configuration is returned unchanged.
 *
 * @param key The key to update.
 * @param updateFn A lambda that computes the new value from the old value.
 * @return A new configuration with the updated value, or the original if key is absent.
 * @since 1.5-SNAPSHOT
 */
@OptIn(ExperimentalContracts::class)
fun <E : Dispatchable, T> EventConfiguration<E>.update(key: Key<T>, updateFn: (T) -> T): EventConfiguration<E> {
    contract { callsInPlace(updateFn, InvocationKind.AT_MOST_ONCE) }
    return if (key !in this) this
    else {
        val any = this[key] ?: return this@update
        EventConfigurationImpl(mutableData.apply {
            @Suppress("UNCHECKED_CAST")
            this[key] = updateFn(any as T)
        })
    }
}

/**
 * Merges another configuration into this one.
 *
 * Existing keys are overwritten with the values from the other configuration.
 *
 * @param other The other configuration to merge.
 * @return A new configuration containing all keys from both configurations.
 * @since 1.5-SNAPSHOT
 */
fun <E : Dispatchable> EventConfiguration<E>.merge(other: EventConfiguration<E>): EventConfiguration<E> =
    when {
        other.isEmpty() -> this
        isEmpty() -> other
        else -> EventConfigurationImpl(mutableData.apply { putAll(other.data) })
    }

/**
 * Removes all entries that match the given predicate.
 *
 * @param predicate A function that returns `true` for key-value pairs to remove.
 * @return A new configuration with entries removed according to the predicate.
 * @since 1.5-SNAPSHOT
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
@OptIn(ExperimentalContracts::class)
inline fun <E : Dispatchable> EventConfiguration<E>.clearIf(crossinline predicate: (Key<*>, Any?) -> Boolean): EventConfiguration<E> {
    contract { callsInPlace(predicate, InvocationKind.UNKNOWN) }
    return if (isEmpty()) this
    else EventConfigurationImpl(mutableData.apply { entries.removeAll { predicate(it.key, it.value) } })
}