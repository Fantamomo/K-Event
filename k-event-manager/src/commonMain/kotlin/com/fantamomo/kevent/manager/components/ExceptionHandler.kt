package com.fantamomo.kevent.manager.components

import com.fantamomo.kevent.Listener
import com.fantamomo.kevent.SimpleListener
import com.fantamomo.kevent.SimpleSuspendListener
import kotlin.reflect.*

/**
 * A base class for handling exceptions that occur within the event management system.
 * Provides methods to process exceptions raised during event method invocation, registration,
 * or access.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
abstract class ExceptionHandler : EventManagerComponent<ExceptionHandler> {
    override val key: EventManagerComponent.Key<ExceptionHandler>
        get() = Key

    companion object Key : EventManagerComponent.Key<ExceptionHandler> {
        override val clazz = ExceptionHandler::class
    }

    /**
     * Handles exceptions that occur during event dispatching when an event handler throws an exception.
     *
     * @param exception The exception thrown by the event handler during event processing.
     * @param listener The listener instance whose method caused the exception. Can be null if no specific listener is associated.
     * @param methode The method being invoked when the exception occurred. Can be null if the method is unknown or unavailable.
     */
    abstract fun handle(exception: Throwable, listener: Listener?, methode: KFunction<*>?)

    /**
     * Invoked when the method of a listener does not have public visibility.
     * This function is called to handle scenarios where an event listener method
     * is not accessible due to its visibility level.
     *
     * @param listener The listener instance that contains the method.
     *                 Provides context about the listener being processed.
     * @param method The method that does not have public visibility.
     *               This represents the function identified in the listener.
     * @param visibility The visibility level of the method, indicating why it may
     *                   not be accessible. Can be null if the visibility is unknown.
     */
    open fun onMethodNotPublic(listener: Listener, method: KFunction<*>, visibility: KVisibility?) {}

    /**
     * Invoked when a listener method has no parameters. This function handles cases
     * where a listener's method signature does not include any parameters, and
     * takes appropriate action in such scenarios.
     *
     * @param listener The listener instance that contains the method. Provides context
     *                 about the listener being processed.
     * @param method The method within the listener that has no parameters.
     */
    open fun onMethodHasNoParameters(listener: Listener, method: KFunction<*>) {}

    /**
     * Invoked when the first parameter of a method in a listener does not extends [com.fantamomo.kevent.Dispatchable].
     * This function is used to handle cases where the given method cannot participate
     * effectively in event dispatch due to incompatible or missing parameter types.
     *
     * @param listener The instance of the listener containing the method. This provides context about the
     *                 listener being processed.
     * @param method The method within the listener that cannot be dispatched due to its parameter(s).
     * @param type The type information of parameters that are expected to be dispatchable but are not.
     */
    open fun onMethodHasNoDispatchableParameter(listener: Listener, method: KFunction<*>, type: KType) {}

    /**
     * Called when setting `method.isAccessible = true` throws an exception.
     *
     * This function is used to handle cases where changing the accessibility of
     * a method fails due to restrictions or other issues.
     *
     * @param listener The listener instance containing the method that triggered the issue.
     *                 Provides context for identifying the source of the problem.
     * @param method The method that failed to change its accessibility.
     */
    open fun onMethodNotAccessible(listener: Listener, method: KFunction<*>) {}

    /**
     * Invoked when a method in a listener throws an exception during registration,
     * which is not of type [com.fantamomo.kevent.ConfigurationCapturedException].
     *
     * This function handles unexpected exceptions that occur in the registration
     * process of listener methods, providing information about the listener,
     * the method, and the thrown exception.
     *
     * @param listener The listener instance whose method triggered the exception.
     *                 Provides context about the source of the issue.
     * @param method The method within the listener that threw the unexpected exception.
     * @param exception The `Throwable` instance representing the unexpected exception
     *                  that occurred during method registration.
     */
    open fun onMethodThrewUnexpectedException(listener: Listener, method: KFunction<*>, exception: Throwable) {}

    /**
     * Invoked when an unexpected exception occurs during the registration of a listener method.
     * Typically, this handles exceptions such as reflection-related errors that are not explicitly managed.
     *
     * @param listener The listener instance containing the method that triggered the exception.
     *                 This provides context for identifying the source of the problem.
     * @param method The method within the listener being processed when the exception occurred.
     *               This represents the function causing the unexpected issue.
     * @param exception The `Throwable` instance representing the unexpected exception
     *                  that occurred during the registration process.
     */
    open fun onUnexpectedExceptionDuringRegistration(listener: Listener, method: KFunction<*>, exception: Throwable) {}

    /**
     * Invoked when a listener method expected to throw a [com.fantamomo.kevent.ConfigurationCapturedException] does not throw any exception.
     *
     * @param listener The listener instance containing the method that did not throw the expected exception.
     * @param method The method in the listener that was expected to throw a ConfigurationCapturedException but did not.
     */
    open fun onMethodDidNotThrowConfiguredException(listener: Listener, method: KFunction<*>) {}

    /**
     * Invoked when a parameter of a method in a listener does not have a resolver.
     * This function handles cases where a parameter cannot be resolved or matched
     * during the processing of listener methods.
     *
     * Note:
     * 1. `parameter.type` always equals `type`.
     * 2. `parameter.name` does not necessarily equal `name`.
     * 3. `name` corresponds to either `parameter.name` or the value of the
     *    [com.fantamomo.kevent.utils.InjectionName] annotation if present:
     *    `(parameter.findAnnotation<InjectionName>()?.value ?: parameter.name) === name`
     *
     * @param listener The listener instance that contains the method with the parameter
     *                 that has no resolver.
     * @param method The method within the listener whose parameter could not be resolved,
     *               providing context about the function being processed.
     * @param parameter The specific parameter that does not have a resolver.
     * @param name The expected name for the parameter, which is either `parameter.name`
     *             or [com.fantamomo.kevent.utils.InjectionName.value] if the annotation is present.
     * @param type The type of the parameter that does not have a resolver.
     *
     * @since 1.6-SNAPSHOT
     */
    open fun onParameterHasNoResolver(
        listener: Listener,
        method: KFunction<*>,
        parameter: KParameter,
        name: String,
        type: KType,
    ) {}

    /**
     * Called when a parameter required by a [SimpleListener] cannot be resolved.
     *
     * @param listener The [SimpleListener] instance with the unresolved parameter.
     * @param name The expected name of the parameter.
     * @param type The type of the unresolved parameter.
     *
     * @since 1.8-SNAPSHOT
     */
    open fun onParameterHasNoResolver(listener: SimpleListener<*>, name: String, type: KClass<*>) {}

    /**
     * Called when a parameter required by a [SimpleSuspendListener] cannot be resolved.
     *
     * @param listener The [SimpleSuspendListener] instance with the unresolved parameter.
     * @param name The expected name of the parameter.
     * @param type The type of the unresolved parameter.
     *
     * @since 1.8-SNAPSHOT
     */
    open fun onParameterHasNoResolver(listener: SimpleSuspendListener<*>, name: String, type: KClass<*>) {}

    /**
     * Provides a no-operation implementation of the exception handler.
     *
     * The [Empty] object serves as an implementation of the [ExceptionHandler] where no specific
     * action is taken to handle exceptions. This is used as the default exception handler
     * when no custom exception handler is provided.
     *
     * @author Fantamomo
     * @since 1.0-SNAPSHOT
     */
    object Empty : ExceptionHandler() {
        override fun handle(
            exception: Throwable,
            listener: Listener?,
            methode: KFunction<*>?,
        ) {
        }
    }
}