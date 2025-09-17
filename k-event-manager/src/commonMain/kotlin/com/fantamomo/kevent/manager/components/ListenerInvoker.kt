package com.fantamomo.kevent.manager.components

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.Listener
import com.fantamomo.kevent.manager.components.invoker.MethodHandlerListenerInvoker
import com.fantamomo.kevent.manager.components.invoker.ReflectionListenerInvoker
import java.lang.invoke.MethodHandles
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * Represents a component responsible for invoking and managing listeners in an event-driven system.
 *
 * ListenerInvoker is used to bind listener methods to dispatchable events and invoke them during
 * event processing. It supports both regular and suspending listener methods. The implementation
 * enables flexibility in choosing how listener methods are invoked, such as through [reflection][ReflectionListenerInvoker]
 * or [method handles][MethodHandlerListenerInvoker].
 *
 * @author Fantamomo
 * @since 1.16-SNAPSHOT
 */
interface ListenerInvoker : EventManagerComponent<ListenerInvoker> {

    override val key: EventManagerComponent.Key<ListenerInvoker>
        get() = Key

    /**
     * Binds a specified listener method to an event with its expected argument types and creates a call handler.
     *
     * This method links a listener's function to dispatchable events by applying the given argument types.
     * The resulting call handler can subsequently invoke the listener's function either synchronously or
     * asynchronously during event processing.
     *
     * @param listener The listener that contains the method to be bound.
     * @param function The function from the listener to be bound to events.
     * @param args An array of `KClass` instances representing the argument types expected by the function.
     * @return A `CallHandler` instance capable of invoking the listener's method with the appropriate arguments
     * when handling a dispatchable event.
     */
    fun <D : Dispatchable> bindListener(listener: Listener, function: KFunction<*>, args: Array<KClass<*>>): CallHandler<D>

    /**
     * Interface defining a handler for invoking operations on dispatchable entities.
     *
     * The `CallHandler` interface provides methods for executing synchronous
     * and asynchronous operations on dispatchable objects. Implementations of
     * this interface define how to handle the invocation process, enabling
     * the linkage of listener methods to events within the dispatchable system.
     *
     * @param D The type parameter representing a class that is a subtype of `Dispatchable`.
     * @author Fantamomo
     * @since 1.16-SNAPSHOT
     */
    interface CallHandler<D : Dispatchable> {
        fun invoke(dispatchable: D, args: Array<Any?>)

        suspend fun invokeSuspend(dispatchable: D, args: Array<Any?>)
    }

    companion object {
        /**
         * Provides an instance of [ListenerInvoker] that uses reflection for invoking listener methods.
         *
         * This method returns an implementation of [ListenerInvoker] designed to invoke listener functions
         * dynamically using Kotlin's reflection capabilities. Its usage allows for the execution of listener
         * methods tied to specific event-handling scenarios.
         *
         * @return A [ListenerInvoker] instance based on the [ReflectionListenerInvoker] implementation.
         */
        fun reflection(): ListenerInvoker = ReflectionListenerInvoker

        /**
         * Creates and returns an instance of [MethodHandlerListenerInvoker], which can be used
         * to bind listener methods to events for invocation handling. This function uses a
         * specific [MethodHandles.Lookup] to resolve method references.
         *
         * @param lookup An optional [MethodHandles.Lookup] instance used to resolve method references.
         * If not provided, [MethodHandles.publicLookup] will be used as the default.
         * @return An instance of [ListenerInvoker] implemented by [MethodHandlerListenerInvoker].
         */
        fun methodHandler(lookup: MethodHandles.Lookup = MethodHandles.publicLookup()): ListenerInvoker = MethodHandlerListenerInvoker(lookup)
    }

    object Key : EventManagerComponent.Key<ListenerInvoker> {
        override val clazz = ListenerInvoker::class
    }
}
