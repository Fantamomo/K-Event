package com.fantamomo.kevent.manager.components

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.Listener
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface ListenerParameterResolver<T : Any> : EventManagerComponent<ListenerParameterResolver<T>> {
    @Suppress("UNCHECKED_CAST")
    override val key
        get() = Key as EventManagerComponent.Key<ListenerParameterResolver<T>>

    val name: String
    val type: KClass<T>
    val valueByConfiguration: T

    object Key : EventManagerComponent.Key<ListenerParameterResolver<Nothing>> {
        @Suppress("UNCHECKED_CAST")
        override val clazz = ListenerParameterResolver::class as KClass<ListenerParameterResolver<Nothing>>
    }

    fun resolve(listener: Listener?, methode: KFunction<*>?, event: Dispatchable): T

    class DynamicListenerParameterResolver<T : Any>(
        override val name: String,
        override val type: KClass<T>,
        override val valueByConfiguration: T,
        val valueProvider: (listener: Listener?, methode: KFunction<*>?, event: Dispatchable) -> T,
    ) : ListenerParameterResolver<T> {
        override fun resolve(
            listener: Listener?,
            methode: KFunction<*>?,
            event: Dispatchable,
        ) = valueProvider(listener, methode, event)
    }

    class StaticListenerParameterResolver<T : Any>(
        override val name: String,
        override val type: KClass<T>,
        override val valueByConfiguration: T,
        val value: T,
    ) : ListenerParameterResolver<T> {
        override fun resolve(
            listener: Listener?,
            methode: KFunction<*>?,
            event: Dispatchable,
        ) = value
    }

    companion object {
        fun <T : Any> dynamic(
            name: String,
            type: KClass<T>,
            valueByConfiguration: T,
            valueProvider: (listener: Listener?, methode: KFunction<*>?, event: Dispatchable) -> T,
        ): ListenerParameterResolver<T> =
            DynamicListenerParameterResolver(name, type, valueByConfiguration, valueProvider)

        fun <T : Any> dynamic(
            name: String,
            type: KClass<T>,
            valueByConfiguration: T,
            valueProvider: () -> T,
        ): ListenerParameterResolver<T> =
            DynamicListenerParameterResolver(name, type, valueByConfiguration, { _, _, _ -> valueProvider() })

        fun <T : Any> static(
            name: String,
            type: KClass<T>,
            value: T,
        ) = StaticListenerParameterResolver(name, type, value, value)
    }
}