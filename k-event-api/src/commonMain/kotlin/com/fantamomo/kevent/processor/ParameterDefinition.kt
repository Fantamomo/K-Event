package com.fantamomo.kevent.processor

import kotlin.reflect.KClass

/**
 * Internal representation of a single parameter of an event handler method.
 *
 * Used by the **K-Event-Processor** during code generation to capture
 * the name and type of a method parameter, so that the processor can
 * correctly generate optimized event dispatch code.
 *
 * The parameter name is determined as follows:
 * - By default, it is the parameter's declared name.
 * - If the parameter is annotated with `@InjectionName`, the value of
 *   `InjectionName#value` is used instead.
 *
 * @author Fantamomo
 * @since 1.8-SNAPSHOT
 */
@InternalProcessorApi
data class ParameterDefinition(
    /**
     * The logical parameter name:
     * - Either the declared method parameter name
     * - Or, if annotated with `@InjectionName`, the annotation's value
     */
    val name: String,

    /** The parameter type (class reference) of the handler method parameter. */
    val type: KClass<*>
)
