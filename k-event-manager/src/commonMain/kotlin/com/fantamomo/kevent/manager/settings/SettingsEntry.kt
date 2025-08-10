package com.fantamomo.kevent.manager.settings

import kotlin.reflect.KClass

data class SettingsEntry<T>(
    val name: String,
    val type: KClass<T & Any>,
    val defaultValue: T,
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T> invoke(name: String, defaultValue: T) =
            SettingsEntry(name, T::class as KClass<T & Any>, defaultValue)
    }
}