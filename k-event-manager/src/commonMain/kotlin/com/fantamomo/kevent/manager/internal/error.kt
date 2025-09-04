package com.fantamomo.kevent.manager.internal

/**
 * Rethrows the throwable if it is considered a fatal error.
 *
 * This method checks the type of the throwable and rethrows it if it is one of the following (including subtypes):
 * - [VirtualMachineError]
 * - [LinkageError]
 * - [InterruptedException]
 * - [AssertionError]
 *
 * Fatal errors generally represent critical conditions such as JVM-level failures
 * or thread interruptions that should not be suppressed or handled trivially.
 * Non-fatal exceptions are ignored by this method.
 *
 * @author Fantamomo
 * @since 1.4-SNAPSHOT
 * @throws Throwable if the throwable is fatal
 */
@Throws(Throwable::class)
internal fun Throwable.rethrowIfFatal() {
    when (this) {
        is VirtualMachineError,
        is LinkageError,
        is InterruptedException,
        is AssertionError,
            -> throw this
    }
}
