package com.fantamomo.kevent

class EventConfigurationScope {
    internal val data: MutableMap<Key<*>, Any?> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: Key<T>): T? = data[key] as? T

    operator fun <T> set(key: Key<T>, value: T) {
        data[key] = value
    }

    fun remove(key: Key<*>) = data.remove(key)
}