package com.fantamomo.kevent.manager.components

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.Listener
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * Interface for resolving dynamic or static parameters for listener methods in an event system.
 *
 * Listener methods in the event system can define additional parameters, which are resolved
 * dynamically or statically when the method is invoked. Implementations of this interface
 * provide mechanisms to fulfill these parameters by supplying appropriate values based on
 * context such as the listener, method information, or the event being dispatched.
 *
 * @param T The type of the parameter value resolved by this resolver. Must be a non-nullable type.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface ListenerParameterResolver<T : Any> : EventManagerComponent<ListenerParameterResolver<T>> {
    @Suppress("UNCHECKED_CAST")
    override val key
        get() = Key as EventManagerComponent.Key<ListenerParameterResolver<T>>

    /**
     * Represents the name of a listener parameter.
     * This value is used to identify the parameter and will be used
     * in resolving methods or handling event dispatch in accordance with
     * the configuration or context of the listener.
     */
    val name: String
    /**
     * Represents the expected type resolved for the listener parameter.
     *
     * This property holds the [KClass] reference of the type `T` that the parameter
     * resolver is designed to handle. It is primarily used to verify or enforce type consistency
     * during resolving operations within the containing class.
     */
    val type: KClass<T>
    val valueByConfiguration: T

    object Key : EventManagerComponent.Key<ListenerParameterResolver<Nothing>> {
        @Suppress("UNCHECKED_CAST")
        override val clazz = ListenerParameterResolver::class as KClass<ListenerParameterResolver<Nothing>>
    }

    /**
     * Resolves and processes a value of type `T` based on the provided listener, method, and event.
     *
     * This method uses the `listener` and `methode` to determine the appropriate handling
     * or resolution of an event of type `Dispatchable`. The output value `T` represents the
     * processed or resolved result, which may vary depending on the provided parameters.
     *
     * @param listener The listener instance that potentially contains event-handling methods.
     *                 Can be null if no specific listener is required for resolution.
     * @param methode  The specific method annotated for event handling within the listener.
     *                 Can be null if the resolution does not depend on a method.
     * @param event    The dispatchable event that triggers the resolution process. Must not be null.
     * @return The resolved value of type `T`, calculated based on the provided listener,
     *         method, and event.
     */
    fun resolve(listener: Listener?, methode: KFunction<*>?, event: Dispatchable): T

    /**
     * A dynamic implementation of the `ListenerParameterResolver` interface for resolving
     * listener parameters of a specific type `T`. This resolver allows customization of
     * parameter values based on the given listener, method, and event during event dispatching.
     *
     * This implementation uses a provided value function to dynamically compute the resolved
     * parameter value, enabling flexible and context-aware parameter resolution.
     *
     * @param T The type of parameter that this resolver handles. Must adhere to `Any`.
     * @property name The name identifying this parameter resolver.
     * @property type The `KClass` representation of the type `T` this resolver resolves.
     * @property valueByConfiguration Default value of the parameter, determined by static
     * configuration and used as a baseline in dynamic resolution scenarios.
     * @property valueProvider A function used to dynamically compute the resolved value of type `T`.
     * This function accepts a listener instance, a method reference, and an event of type `Dispatchable`,
     * enabling resolution based on the event-handling context.
     *
     * @author Fantamomo
     * @since 1.0-SNAPSHOT
     */
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

    /**
     * A concrete implementation of [ListenerParameterResolver] that resolves parameters
     * to a static, pre-defined value of type `T`.
     *
     * This resolver always returns a fixed value, specified during its instantiation,
     * whenever its resolution method is invoked. It is particularly useful for scenarios
     * where a constant or shared value is needed for event or listener parameters.
     *
     * @param T The type of the value to be resolved by this resolver. Must be a non-`null` type.
     * @property name The name associated with this parameter resolver.
     * @property type The Kotlin class type of the parameter that this resolver handles.
     * @property valueByConfiguration An additional configuration-related value associated with this resolver.
     * @property value The static value to be used for resolution, returned consistently from the resolver.
     *
     * @author Fantamomo
     * @since 1.0-SNAPSHOT
     */
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
        /**
         * Creates a dynamic listener parameter resolver that dynamically resolves parameters for event handling
         * based on the provided listener, method, and event.
         *
         * This method produces an instance of `DynamicListenerParameterResolver`, which evaluates the parameter value
         * in a context-sensitive manner during event dispatching, relying on the specified value provider function.
         *
         * @param name A unique identifier for the parameter resolver.
         * @param type The `KClass` representation of the type `T` that this resolver handles.
         * @param valueByConfiguration A default value for the parameter, determined statically by configuration.
         * @param valueProvider A lambda function for computing the dynamic value of the parameter. It accepts a listener,
         * a method, and a dispatchable event as parameters to enable context-aware resolution.
         * @return An instance of `ListenerParameterResolver<T>` capable of dynamically resolving the parameter value.
         */
        fun <T : Any> dynamic(
            name: String,
            type: KClass<T>,
            valueByConfiguration: T,
            valueProvider: (listener: Listener?, methode: KFunction<*>?, event: Dispatchable) -> T,
        ): ListenerParameterResolver<T> =
            DynamicListenerParameterResolver(name, type, valueByConfiguration, valueProvider)

        /**
         * Creates a dynamic `ListenerParameterResolver` for resolving parameters of the specified type `T`.
         * This resolver dynamically determines the parameter value using the provided `valueProvider` function.
         *
         * @param name The name identifying the parameter resolver.
         * @param type The `KClass` representation of the type `T` to resolve.
         * @param valueByConfiguration The default or static parameter value used as a base for resolution.
         * @param valueProvider A function that dynamically provides a value of type `T` based on custom logic.
         * @return A `DynamicListenerParameterResolver` instance that resolves parameters dynamically for type `T`.
         */
        fun <T : Any> dynamic(
            name: String,
            type: KClass<T>,
            valueByConfiguration: T,
            valueProvider: () -> T,
        ): ListenerParameterResolver<T> =
            DynamicListenerParameterResolver(name, type, valueByConfiguration) { _, _, _ -> valueProvider() }

        /**
         * Creates and returns a dynamic listener parameter resolver for the specified type `T`.
         * This resolver dynamically determines parameter values at runtime using the provided `valueProvider` function.
         *
         * @param name The name identifying this resolver.
         * @param type The `KClass` representation of the type `T` being resolved.
         * @param valueProvider A function that provides the value of type `T` dynamically when invoked.
         * @return A `ListenerParameterResolver` instance configured to resolve parameters of type `T`.
         */
        fun <T : Any> dynamic(
            name: String,
            type: KClass<T>,
            valueProvider: () -> T,
        ): ListenerParameterResolver<T> =
            DynamicListenerParameterResolver(name, type, valueProvider()) { _, _, _ -> valueProvider() }

        /**
         * Creates a static listener parameter resolver that always resolves a given parameter to
         * a constant, pre-defined value.
         *
         * This method is intended for scenarios where a specific parameter value is always needed,
         * regardless of the event or listener context.
         *
         * @param name The name of the parameter being resolved.
         * @param type The Kotlin class type of the parameter to be resolved.
         * @param value The static value to resolve for the parameter.
         * @return An instance of `StaticListenerParameterResolver` initialized with the given values.
         */
        fun <T : Any> static(
            name: String,
            type: KClass<T>,
            value: T,
        ) = StaticListenerParameterResolver(name, type, value, value)
    }
}