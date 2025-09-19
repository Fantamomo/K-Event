package com.fantamomo.kevent.processor

/**
 * Internal registry interface for storing event handler definitions
 * of a **single listener class**.
 *
 * The **K-Event-Processor** generates one implementation of this interface
 * for each discovered listener class. That generated object contains
 * metadata for all handler methods of that listener.
 *
 * Example (simplified):
 * ```
 * @OptIn(InternalProcessorApi::class)
 * @GeneratedHandlerRegistry(DemoListener::class)
 * object DemoListenerHandlerRegistry : EventHandlerRegistry {
 *     override val listeners: Array<HandlerDefinition> = arrayOf(
 *         HandlerDefinition(
 *             listener = DemoListener::class,
 *             method = DemoListener::testEvent,
 *             event = Dispatchable::class,
 *             args = arrayOf(),
 *             isSuspend = false,
 *             configuration = EventConfiguration.Empty,
 *             isNullable = false
 *         ),
 *         HandlerDefinition(
 *             listener = DemoListener::class,
 *             method = DemoListener::deadEvent,
 *             event = DeadEvent::class,
 *             args = arrayOf(
 *                 ParameterDefinition("manager", EventManager::class),
 *                 ParameterDefinition("isSticky", Boolean::class)
 *             ),
 *             isSuspend = false,
 *             configuration = EventConfiguration.Empty,
 *             isNullable = false
 *         )
 *     )
 * }
 * ```
 *
 * @author Fantamomo
 * @since 1.8-SNAPSHOT
 */
@InternalProcessorApi
interface EventHandlerRegistry {

    /**
     * All event handler definitions for the associated listener class.
     * Each [HandlerDefinition] represents exactly one annotated handler method.
     */
    val listeners: Array<HandlerDefinition>
}
