package com.fantamomo.kevent.manager.config

class DispatchConfigScope {
    internal val data = mutableMapOf<DispatchConfigKey<*>, Any?>()

    operator fun <T> set(key: DispatchConfigKey<T>, value: T) {
        data[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: DispatchConfigKey<T>): T? = data[key] as? T?

    fun <T> getOrDefault(key: DispatchConfigKey<T>) = get(key) ?: key.defaultValue

    operator fun <T> contains(key: DispatchConfigKey<T>) = key in data

    operator fun <T> contains(value: T) = value in data.values

    fun <T> remove(key: DispatchConfigKey<T>) {
        data.remove(key)
    }
}