package com.fantamomo.kevent.utils

/**
 * Annotation for overriding the default injection name of a parameter.
 *
 * By default, the parameter’s own name is used as its injection name.
 * This annotation allows specifying a custom name, so the injected value
 * can be bound to a different name than the parameter itself.
 *
 * It is only used for parameter injection of custom values in listener methods.
 *
 * @property value The custom injection name assigned to the parameter.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class InjectionName(val value: String)