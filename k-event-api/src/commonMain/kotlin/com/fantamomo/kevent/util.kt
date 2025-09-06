package com.fantamomo.kevent

import com.fantamomo.kevent.EventConfiguration.EventConfigurationImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PublishedApi
internal val EventConfiguration<*>.data: Map<Key<*>, Any?>
    get() = (this as? EventConfigurationImpl)?.data ?: emptyMap()

@PublishedApi
internal val EventConfiguration<*>.mutableData: MutableMap<Key<*>, Any?>
    get() = (this as? EventConfigurationImpl)?.data?.toMutableMap() ?: mutableMapOf()

/**
 * Adds the specified `key` and `value` to the event configuration only if the key is not already present.
 * If the key exists, the original configuration is returned unchanged.
 *
 * @param key The key to associate with the value in the event configuration.
 * @param value The value to associate with the key if the key is not already present.
 * @return A new `EventConfiguration` instance with the specified key-value pair added, or the original configuration
 * if the key already exists.
 *
 * @author Fantamomo
 * @since 1.5-SNAPSHOT
 */
fun <E : Dispatchable, T> EventConfiguration<E>.setIfAbsent(key: Key<T>, value: T): EventConfiguration<E> = when {
    key in this -> this
    isEmpty() -> EventConfigurationImpl(mutableMapOf(key to value))
    else -> EventConfigurationImpl(mutableData.apply { put(key, value) })
}

/**
 * Removes the specified `key` from the event configuration.
 *
 * @param key The key to remove.
 * @return A new `EventConfiguration` without the specified key.
 * @author Fantamomo
 * @since 1.5-SNAPSHOT
 */
fun <E : Dispatchable, T> EventConfiguration<E>.remove(key: Key<T>): EventConfiguration<E> =
    if (key !in this) this
    else EventConfigurationImpl(mutableData.apply { remove(key) })


/**
 * Returns the value associated with the key, or the provided default if the key is not present.
 *
 * @param key The key to retrieve the value for.
 * @param default The default value to return if the key is absent.
 * @author Fantamomo
 * @since 1.5-SNAPSHOT
 */
@Suppress("UNCHECKED_CAST")
fun <E : Dispatchable, T> EventConfiguration<E>.getOrDefault(key: Key<T>, default: T): T =
    (this as? EventConfigurationImpl?)?.data[key] as? T ?: default


/**
 * Updates the value of the specified key using the provided lambda function.
 * If the key does not exist, the configuration is returned unchanged.
 *
 * @param key The key to update.
 * @param updateFn The function to compute the new value from the old value.
 * @author Fantamomo
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
 * Merges another configuration into this one. Existing keys are overwritten.
 *
 * @param other The other configuration to merge.
 * @author Fantamomo
 * @since 1.5-SNAPSHOT
 */
fun <E : Dispatchable> EventConfiguration<E>.merge(other: EventConfiguration<E>): EventConfiguration<E> =
    when {
        other.isEmpty() -> this
        isEmpty() -> other
        else -> EventConfigurationImpl(mutableData.apply {
            putAll(other.data)
        })
    }


/**
 * Removes all entries that match the given predicate.
 *
 * @param predicate The function that determines whether a key-value pair should be removed.
 * @author Fantamomo
 * @since 1.5-SNAPSHOT
 */
@Suppress("LEAKED_IN_PLACE_LAMBDA")
@OptIn(ExperimentalContracts::class)
inline fun <E : Dispatchable> EventConfiguration<E>.clearIf(crossinline predicate: (Key<*>, Any?) -> Boolean): EventConfiguration<E> {
    contract { callsInPlace(predicate, InvocationKind.UNKNOWN) }
    return if (isEmpty()) this
    else EventConfigurationImpl(mutableData.apply {
        entries.removeAll { predicate(it.key, it.value) }
    })
}