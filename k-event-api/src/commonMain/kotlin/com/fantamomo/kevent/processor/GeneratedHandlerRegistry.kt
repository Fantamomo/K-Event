package com.fantamomo.kevent.processor

import com.fantamomo.kevent.Listener
import kotlin.reflect.KClass

/**
 * Annotation that marks a generated [EventHandlerRegistry].
 *
 * For every [Listener] class discovered by the **K-Event-Processor**, an
 * implementation of [EventHandlerRegistry] is generated. That implementation
 * is annotated with this annotation to establish the link between the
 * registry and its corresponding listener class.
 *
 * The annotation is retained at runtime so that the K-Event runtime can
 * locate and load the correct registry for a given listener type.
 *
 * Example:
 * ```
 * @OptIn(InternalProcessorApi::class)
 * @GeneratedHandlerRegistry(DemoListener::class)
 * object DemoListenerHandlerRegistry : EventHandlerRegistry {
 *     override val listeners: Array<HandlerDefinition> = ...
 * }
 * ```
 *
 * @property value the [Listener] class for which this registry was generated
 * @author Fantamomo
 * @since 1.8-SNAPSHOT
 */
@InternalProcessorApi
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GeneratedHandlerRegistry(
    val value: KClass<out Listener>
)
