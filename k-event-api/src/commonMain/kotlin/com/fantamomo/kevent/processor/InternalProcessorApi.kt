package com.fantamomo.kevent.processor

/**
 * Marks a class as part of the internal API of the **K-Event-Processor** module.
 *
 * The K-Event-Processor uses **KSP (Kotlin Symbol Processing)** to generate code
 * that makes the K-Event system more efficient when registering listeners.
 *
 * Even though these classes may reside in the public API module, they are
 * strictly for internal use by the processor and **must not be used directly
 * outside it**.
 *
 * Applying this annotation ensures that accidental external usage is flagged
 * as a **compile-time error**.
 *
 * @author Fantamomo
 * @since 1.8-SNAPSHOT
 */
@RequiresOptIn(
    message = "This API is internal to the event processor module and must not be used outside of it.",
    level = RequiresOptIn.Level.ERROR
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class InternalProcessorApi
