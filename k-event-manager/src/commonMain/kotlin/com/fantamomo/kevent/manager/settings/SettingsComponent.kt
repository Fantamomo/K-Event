package com.fantamomo.kevent.manager.settings

import com.fantamomo.kevent.manager.components.EventManagerComponent
import kotlin.reflect.KClass

data class SettingsComponent<T>(val entry: SettingsEntry<T>, val value: T) : EventManagerComponent<SettingsComponent<T>> {

    @Suppress("UNCHECKED_CAST")
    override val key: EventManagerComponent.Key<SettingsComponent<T>> = Key as EventManagerComponent.Key<SettingsComponent<T>>

    object Key : EventManagerComponent.Key<SettingsComponent<Nothing>> {
        @Suppress("UNCHECKED_CAST")
        override val clazz = SettingsComponent::class as KClass<SettingsComponent<Nothing>>
    }
}