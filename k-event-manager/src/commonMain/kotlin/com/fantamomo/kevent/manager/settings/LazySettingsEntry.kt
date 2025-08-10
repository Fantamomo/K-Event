package com.fantamomo.kevent.manager.settings

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal class LazySettingsEntry<T>(val type: KClass<T & Any>, val defaultValue: T) : ReadOnlyProperty<Settings, SettingsEntry<T>> {
    private lateinit var entry: SettingsEntry<T>

    override fun getValue(
        thisRef: Settings,
        property: KProperty<*>,
    ): SettingsEntry<T> {
        if (::entry.isInitialized) return entry
        entry = SettingsEntry(property.name, type, defaultValue)
        return entry
    }
}