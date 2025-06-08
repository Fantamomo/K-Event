package com.fantamomo.kevent

class EventConfiguration(private val data: Map<Key<*>, Any?>) {
    constructor(scope: EventConfigurationScope) : this(scope.data.toMap())

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: Key<T>) = data[key] as? T

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrDefault(key: Key<T>) = data[key] as? T ?: key.defaultValue

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getValue(key: Key<T>): T = data[key] as T

    companion object {
        val DEFAULT = EventConfiguration(emptyMap())
    }
}