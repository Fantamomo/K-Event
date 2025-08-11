package com.fantamomo.kevent.manager.settings

import kotlin.reflect.KClass

/**
 * Singleton object containing configurable settings for event dispatching behavior.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
object Settings {
    /**
     * A setting that disables the injection of the `isWaiting:` [Boolean] state into parameter resolution during
     * event dispatching.
     *
     * By default, the value is `false`, meaning the `isWaiting` injection is enabled. Setting this to
     * `true` will turn off this behavior.
     */
    val DISABLE_IS_WAITING_INJECTION by setting(false)
    /**
     * A setting that disables the injection of the `manager:` [com.fantamomo.kevent.manager.EventManager] state into parameter resolution during
     * event dispatching.
     *
     * By default, the value is `false`, meaning the `manager` injection is enabled. Setting this to
     * `true` will turn off this behavior.
     */
    val DISABLE_EVENTMANAGER_INJECTION by setting(false)
    /**
     * A setting that disables the injection of the `scope:` [kotlinx.coroutines.CoroutineScope] state into parameter resolution during
     * event dispatching.
     *
     * By default, the value is `false`, meaning the `scope` injection is enabled. Setting this to
     * `true` will turn off this behavior.
     */
    val DISABLE_SCOPE_INJECTION by setting(false)


    private inline fun <reified T> setting(name: String, defaultValue: T) = SettingsEntry<T>(name, defaultValue)
    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> setting(defaultValue: T) = LazySettingsEntry(T::class as KClass<T & Any>, defaultValue)
}