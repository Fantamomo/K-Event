package com.fantamomo.kevent

/**
 * DSL marker annotation for the event configuration DSL.
 *
 * This annotation is used to mark the event configuration DSL, which helps the Kotlin
 * compiler enforce proper scoping rules for the DSL. It prevents implicit access to
 * outer receiver scopes, making the DSL safer and more predictable.
 *
 * When a function parameter is marked with this annotation (like the [block] parameter
 * in the [configuration] function), the Kotlin compiler ensures that within that block,
 * only members of the immediate receiver are accessible without explicit qualification.
 *
 * This is particularly important for nested DSL blocks, where it prevents accidental
 * access to members of outer scopes.
 *
 * @see configuration
 * @see EventConfigurationScope
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@DslMarker
annotation class EventDsl
