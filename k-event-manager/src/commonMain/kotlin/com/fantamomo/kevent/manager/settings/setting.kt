package com.fantamomo.kevent.manager.settings

import com.fantamomo.kevent.manager.components.ComponentSet
import com.fantamomo.kevent.manager.components.EventManagerComponent

/**
 * Associates the given value with the current settings entry, creating a [SettingsComponent] instance.
 *
 * @param value The value to associate with this `SettingsEntry`.
 * @return A `SettingsComponent` that pairs the `SettingsEntry` with the provided value.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> SettingsEntry<T>.with(value: T) = SettingsComponent(this, value)

/**
 * Retrieves the setting value associated with the given [SettingsEntry] from the current [ComponentSet].
 * If no associated value is found in the [ComponentSet], the default value of the [SettingsEntry] is returned.
 *
 * @param entry The [SettingsEntry] representing the setting to be retrieved.
 * @return The value associated with the provided [SettingsEntry], or its default value if not present.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@Suppress("UNCHECKED_CAST")
fun <T> ComponentSet.getSetting(entry: SettingsEntry<T>): T =
    (components.find { (it as? SettingsComponent<T>)?.entry === entry } as? SettingsComponent<T>)?.value
        ?: entry.defaultValue

/**
 * Retrieves the value associated with the provided [SettingsEntry] from the current [EventManagerComponent].
 * If the component is a [ComponentSet], it delegates the retrieval to the [ComponentSet.getSetting] method.
 * If the component is a [SettingsComponent] with a matching [SettingsEntry], it retrieves the value of the entry.
 * If no value is found, the default value of the [SettingsEntry] is returned.
 *
 * @param entry The [SettingsEntry] that represents the configuration setting to be retrieved.
 * @return The value associated with the provided [SettingsEntry], or the default value if no matching component is found.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@Suppress("UNCHECKED_CAST")
fun <T> EventManagerComponent<*>.getSetting(entry: SettingsEntry<T>): T =
    if (this is ComponentSet) getSetting(entry)
    else (this as? SettingsComponent<T>)?.takeIf { it.entry === entry }?.value ?: entry.defaultValue