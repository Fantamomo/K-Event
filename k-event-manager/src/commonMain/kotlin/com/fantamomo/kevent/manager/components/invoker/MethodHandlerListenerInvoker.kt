package com.fantamomo.kevent.manager.components.invoker

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.Listener
import com.fantamomo.kevent.manager.components.ListenerInvoker
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

/**
 * A concrete implementation of [ListenerInvoker] that uses method handles for invoking listener methods.
 *
 * This class allows binding of listener methods and their invocation through a method handle mechanism
 * provided by the **Java MethodHandle API**. It supports both regular and suspend functions in listener classes.
 *
 * @constructor Creates an instance of [MethodHandlerListenerInvoker] with a given `MethodHandles.Lookup`.
 * @param lookup The [MethodHandles.Lookup] instance used for resolving method references during listener binding.
 * @author Fantamomo
 * @since 1.16-SNAPSHOT
 */
class MethodHandlerListenerInvoker(
    private val lookup: MethodHandles.Lookup
) : ListenerInvoker {
    override fun <D : Dispatchable> bindListener(
        listener: Listener,
        function: KFunction<*>,
        args: Array<KClass<*>>,
    ): ListenerInvoker.CallHandler<D> {
        val javaMethod = function.javaMethod
            ?: throw IllegalArgumentException("Function must be backed by a Java method")

        val paramTypes = args.map { it.java }.toMutableList()

        if (function.isSuspend) {
            paramTypes += Continuation::class.java
        }

        val methodType = MethodType.methodType(
            javaMethod.returnType,
            paramTypes.toTypedArray()
        )

        val handle = lookup.findVirtual(
            javaMethod.declaringClass,
            javaMethod.name,
            methodType
        )

        return MethodHandlerCallHandler(listener, handle, function.isSuspend)
    }

    /**
     * A concrete implementation of the [ListenerInvoker.CallHandler] interface, using method handles to invoke
     * listener methods tied to dispatchable events.
     *
     * This handler supports invoking both regular and suspending listener methods, with appropriate safeguards to ensure
     * that suspending methods are not invoked in a blocking context.
     *
     * @param D The type parameter representing a class that is a subtype of [Dispatchable].
     * @param listener The listener instance containing the method to be invoked during event processing.
     * @param handle A method handle representing the target method to be invoked.
     * @param suspend A boolean indicating whether the target method is a suspending function.
     * @author Fantamomo
     * @since 1.16-SNAPSHOT
     */
    class MethodHandlerCallHandler<D : Dispatchable>(
        private val listener: Listener,
        private val handle: MethodHandle,
        private val suspend: Boolean
    ) : ListenerInvoker.CallHandler<D> {
        override fun invoke(dispatchable: D, args: Array<Any?>) {
            if (suspend) {
                throw IllegalStateException(
                    "Cannot call suspend listener method in blocking invoke(); use invokeSuspend()"
                )
            }
            try {
                handle.invokeWithArguments(listener, dispatchable, *args)
            } catch (e: Throwable) {
                throw RuntimeException("Failed to invoke listener method", e)
            }
        }

        override suspend fun invokeSuspend(dispatchable: D, args: Array<Any?>) {
            if (!suspend) {
                invoke(dispatchable, args)
                return
            }
            suspendCoroutineUninterceptedOrReturn<Any?> { cont ->
                try {
                    handle.invokeWithArguments(listener, dispatchable, *args, cont)
                } catch (t: Throwable) {
                    throw RuntimeException("Failed to invoke suspend listener method", t)
                }
            }
        }
    }
}
