package com.fantamomo.kevent.processor

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * Internal data model that describes a listener method ("handler") for the K-Event system.
 *
 * This class is used by the **K-Event-Processor** during code generation to capture
 * all necessary metadata about a listener method, so that event dispatch can be
 * optimized at compile time.
 *
 * The information stored here allows the processor to:
 * - Identify the listener class and its method
 * - Determine the event type handled
 * - Track method parameters, their configuration, and nullability
 * - Distinguish between regular and `suspend` handlers
 *
 * Marked with [InternalProcessorApi] because this data class is **only intended
 * for use inside the processor** and must not be accessed by external consumers.
 *
 * @author Fantamomo
 * @since 1.8-SNAPSHOT
 */
@InternalProcessorApi
data class HandlerDefinition(
    /** The class that contains the event listener method. */
    val listener: KClass<*>,

    /** The method in the listener class that handles the event. */
    val method: KFunction<*>,

    /** The type of event that this handler listens to. */
    val event: KClass<*>,

    /** The parameters of the handler method, wrapped in [ParameterDefinition]. */
    val args: Array<ParameterDefinition>,

    /** Whether the handler method is a `suspend` function. */
    val isSuspend: Boolean,

    /** Whether the event parameter is nullable. */
    val isNullable: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HandlerDefinition) return false

        if (isSuspend != other.isSuspend) return false
        if (isNullable != other.isNullable) return false
        if (listener != other.listener) return false
        if (method != other.method) return false
        if (event != other.event) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isSuspend.hashCode()
        result = 31 * result + isNullable.hashCode()
        result = 31 * result + listener.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + event.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }
}
