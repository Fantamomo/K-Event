package com.fantamomo.kevent.manager.config

class DispatchConfig(private val data: Map<DispatchConfigKey<*>, Any?>) {

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: DispatchConfigKey<T>): T? = data[key] as? T?

    fun <T> getOrDefault(key: DispatchConfigKey<T>) = get(key) ?: key.defaultValue

    fun <T> getOrDefault(key: DispatchConfigKey<T>, default: T) = get(key) ?: default

    operator fun <T> contains(key: DispatchConfigKey<T>) = key in data

    operator fun <T> contains(value: T) = value in data.values

    companion object {
        val EMPTY = DispatchConfig(emptyMap())

        operator fun invoke(scope: DispatchConfigScope) = if (scope.data.isEmpty()) EMPTY else DispatchConfig(scope.data)
    }
}