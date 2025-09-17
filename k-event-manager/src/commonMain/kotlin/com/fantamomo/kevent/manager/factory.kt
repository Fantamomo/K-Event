@file:JvmName("EventManagerFactory")

package com.fantamomo.kevent.manager

import com.fantamomo.kevent.manager.components.*

private val DEFAULT_COMPONENTS = ComponentSet.of(ExceptionHandler.Empty, SharedExclusiveExecution(), ListenerInvoker.reflection())

/**
 * Creates a default instance of [EventManager] with a predefined empty component set.
 *
 * This function initializes an [EventManager] implementation using an empty [ComponentSet],
 * enabling the management of event listeners and event dispatching without any pre-configured components.
 *
 * @return A default instance of [EventManager] with an empty component set.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun EventManager(): EventManager = EventManager(ComponentSet.EMPTY)

/**
 * Constructs a new instance of an `EventManager` using the provided components.
 * Ensures that the required [ExceptionHandler.Empty] component is present by adding
 * it to the given components if it is not already included.
 *
 * @param components The initial set of components used to construct the `EventManager`.
 *                   If `ExceptionHandler.Empty` is not present, it will be added.
 * @return A newly constructed [EventManager] implementation with the provided and
 *         required components.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun EventManager(components: EventManagerComponent<*>): EventManager {
    if (components === ComponentSet.EMPTY) return DefaultEventManager(DEFAULT_COMPONENTS)
    val component = components
        .addIfAbsent(ExceptionHandler.Empty)
        .addIfAbsent(SharedExclusiveExecution())
        .addIfAbsent(ListenerInvoker.reflection())
    return DefaultEventManager(component)
}