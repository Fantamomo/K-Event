package com.fantamomo.kevent.manager.components.invoker

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.Listener
import com.fantamomo.kevent.manager.components.ListenerInvoker
import com.fantamomo.kevent.manager.components.ListenerInvoker.CallHandler
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend

/**
 * Implementation of the ListenerInvoker interface using reflection to bind and invoke listener methods.
 *
 * ReflectionListenerInvoker provides mechanisms for dynamically binding listener functions to events and invoking
 * them during event dispatch. It employs Kotlin's reflection capabilities to achieve runtime method invocation, simplifying
 * dynamic event listener management in the system.
 *
 * This implementation creates a [ReflectionCallHandler] for each bound listener function, which handles the invocation
 * logic, allowing for both synchronous and suspending function calls.
 *
 * This implementation is used by default by the [com.fantamomo.kevent.manager.DefaultEventManager].
 * If an implementation of [ListenerInvoker] throws an exception during binding,
 * the [com.fantamomo.kevent.manager.DefaultEventManager] falls back to use this implementation.
 *
 * @author Fantamomo
 * @since 1.16-SNAPSHOT
 */
object ReflectionListenerInvoker : ListenerInvoker {
    override fun <D : Dispatchable> bindListener(
        listener: Listener,
        function: KFunction<*>,
        args: () -> Array<KClass<*>>,
    ) = ReflectionCallHandler<D>(listener, function)

    /**
     * A call handler implementation that uses reflection to invoke listener methods.
     *
     * ReflectionCallHandler is responsible for invoking event listener methods using
     * Kotlin reflection. It implements the [CallHandler] interface, providing synchronous and
     * suspendable invocation mechanisms. The handler delegates the invocation process
     * to the provided [function] using reflection.
     *
     * @param D The type of dispatchable event handled by this call handler.
     * @param listener The instance of the listener object containing the target function.
     * @param function The reflection object representing the listener method to be invoked.
     * @author Fantamomo
     * @since 1.16-SNAPSHOT
     */
    class ReflectionCallHandler<D : Dispatchable>(
        private val listener: Listener,
        private val function: KFunction<*>
    ) : CallHandler<D> {
        override fun invoke(dispatchable: D, args: Array<Any?>) {
            function.call(listener, dispatchable, *args)
        }

        override suspend fun invokeSuspend(dispatchable: D, args: Array<Any?>) {
            function.callSuspend(listener, dispatchable, *args)
        }
    }
}