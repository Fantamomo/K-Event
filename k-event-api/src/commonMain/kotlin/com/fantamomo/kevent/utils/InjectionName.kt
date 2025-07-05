package com.fantamomo.kevent.utils


/**
 * This annotation allows overriding the injection name of a parameter when applied.
 * By default, the parameter name is used as the injection name.
 * This provides the ability to assign
 * a different name to the injected value than the parameter name.
 *
 * It is solely used for parameter injection of custom values within listener
 * methods.
 *
 * @property value The custom injection name to be used for the parameter.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class InjectionName(val value: String)