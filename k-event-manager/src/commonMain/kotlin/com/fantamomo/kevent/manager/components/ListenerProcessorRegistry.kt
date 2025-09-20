package com.fantamomo.kevent.manager.components

import com.fantamomo.kevent.Listener
import com.fantamomo.kevent.processor.EventHandlerRegistry
import com.fantamomo.kevent.processor.GeneratedHandlerRegistry
import com.fantamomo.kevent.processor.InternalProcessorApi
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.streams.asSequence

@OptIn(InternalProcessorApi::class)
sealed interface ListenerProcessorRegistry : EventManagerComponent<ListenerProcessorRegistry> {

    override val key get() = Key

    fun load()
    fun getRegistry(listenerClass: KClass<out Listener>): EventHandlerRegistry?
    fun init(listenerClass: KClass<out Listener>)
    fun fullInit()

    operator fun contains(listenerClass: KClass<out Listener>): Boolean

    companion object Key : EventManagerComponent.Key<ListenerProcessorRegistry> {
        override val clazz = ListenerProcessorRegistry::class

        fun create(): ListenerProcessorRegistry = Default()
    }

    object Empty : ListenerProcessorRegistry {
        override fun load() {}
        override fun getRegistry(listenerClass: KClass<out Listener>): EventHandlerRegistry? = null
        override fun init(listenerClass: KClass<out Listener>) {}
        override fun fullInit() {}
        override fun contains(listenerClass: KClass<out Listener>) = false
    }

    class Default internal constructor() : ListenerProcessorRegistry {
        private val serviceLoader = ServiceLoader.load(EventHandlerRegistry::class.java)
        private val lock = ReentrantLock()

        @Volatile
        private var providedListeners: Map<KClass<out Listener>, KClass<out EventHandlerRegistry>>? = null

        private val registries: MutableMap<KClass<out Listener>, EventHandlerRegistry> = ConcurrentHashMap()

        @Volatile
        private var finished = false

        override fun load() {
            if (finished) return
            if (providedListeners != null) return

            lock.withLock {
                if (providedListeners != null) return

                val providers = mutableMapOf<KClass<out Listener>, KClass<out EventHandlerRegistry>>()
                serviceLoader.stream().asSequence().forEach { provider ->
                    val clazz = provider.type().kotlin
                    val annotation = clazz.findAnnotation<GeneratedHandlerRegistry>()
                    if (annotation != null) {
                        providers.putIfAbsent(annotation.value, clazz)
                    }
                }
                providedListeners = providers
            }
        }

        override fun getRegistry(listenerClass: KClass<out Listener>): EventHandlerRegistry? {
            init(listenerClass)
            return registries[listenerClass]
        }

        override fun init(listenerClass: KClass<out Listener>) {
            load()
            val localProvided = providedListeners ?: return
            if (!localProvided.containsKey(listenerClass)) return
            if (registries.containsKey(listenerClass)) return

            lock.withLock {
                if (!registries.containsKey(listenerClass)) {
                    val registryClass = localProvided[listenerClass] ?: return
                    val registry = registryClass.objectInstance
                        ?: throw IllegalArgumentException("Class ${registryClass.qualifiedName} must be an object")
                    registries[listenerClass] = registry
                }
                if (registries.size == localProvided.size) finished = true
            }
        }

        override fun fullInit() {
            load()
            val localProvided = providedListeners ?: return
            localProvided.keys.forEach { init(it) }
            finished = true
        }

        override fun contains(listenerClass: KClass<out Listener>): Boolean {
            load()
            return providedListeners?.containsKey(listenerClass) ?: false
        }
    }
}
