package com.fantamomo.kevent.manager.components

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Executes the provided block of code while ensuring that the specified handler is locked
 * during the execution. Only one execution for the given handler is allowed at a time.
 * If the lock for the handler cannot be acquired, the block is not executed.
 *
 * @param handler The unique identifier of the handler to lock during the execution.
 * @param block The code block to execute while the handler is locked.
 * @author Fantamomo
 * @since 1.15-SNAPSHOT
 */
@OptIn(ExperimentalContracts::class)
inline fun SharedExclusiveExecution.withLock(handler: String, block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    if (tryAcquire(handler)) {
        try {
            block()
        } finally {
            release(handler)
        }
    }
}