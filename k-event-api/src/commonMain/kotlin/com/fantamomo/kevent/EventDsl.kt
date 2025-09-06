package com.fantamomo.kevent

/**
 * DSL marker for the event configuration DSL.
 *
 * Marks the event configuration DSL to help the Kotlin compiler enforce proper scoping rules.
 * This prevents implicit access to outer receiver scopes, making the DSL safer and more predictable.
 *
 * When applied to a function parameter (e.g., the `block` parameter in the [configuration] function),
 * the compiler ensures that only members of the immediate receiver are accessible within that block
 * without explicit qualification.
 *
 * This is especially useful for nested DSL blocks, preventing accidental access to members of outer scopes.
 *
 * @see configuration
 * @see EventConfigurationScope
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@DslMarker
annotation class EventDsl