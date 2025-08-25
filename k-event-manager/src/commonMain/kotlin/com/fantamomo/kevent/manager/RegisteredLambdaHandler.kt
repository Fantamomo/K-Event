package com.fantamomo.kevent.manager

/**
 * Represents a functional interface for managing the lifecycle of a registered lambda handler.
 *
 * This interface is used as a callback mechanism to allow the caller
 * to unregister a previously registered event-handling lambda.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
fun interface RegisteredLambdaHandler {
    fun unregister()
}