package com.fantamomo.kevent

/**
 * Immutable container for event handler configuration data.
 *
 * Defines the API for accessing event configuration.
 * This interface is sealed.
 *
 * @param E The type of event this configuration applies to.
 *
 * @see EventConfigurationScope
 * @see Key
 */
sealed interface EventConfiguration<E : Dispatchable> {

    /**
     * Returns the value associated with the specified key, or `null` if no value is set.
     */
    operator fun <T> get(key: Key<T>): T?

    /**
     * Returns the value associated with the specified key, or the key's default value if none is set.
     */
    fun <T> getOrDefault(key: Key<T>): T

    /**
     * Returns the non-null value associated with the specified key.
     *
     * @throws IllegalStateException if no value is set for the key
     */
    fun <T : Any> getValue(key: Key<T>): T

    /**
     * Checks if the configuration contains a value for the specified key.
     */
    operator fun contains(key: Key<*>): Boolean

    /**
     * Returns `true` if the configuration contains no entries.
     */
    fun isEmpty(): Boolean

    /**
     * Returns the number of entries in the configuration.
     */
    val size: Int

    companion object Empty : EventConfiguration<Nothing> {

        @Suppress("UNCHECKED_CAST")
        fun <E : Dispatchable> default(): EventConfiguration<E> =
            this as EventConfiguration<E>

        operator fun <D : Dispatchable> invoke(scope: EventConfigurationScope<D>): EventConfiguration<D> =
            if (scope.isEmpty()) default() else EventConfigurationImpl(scope.data.toMap())

        override fun <T> get(key: Key<T>) = null

        override fun <T> getOrDefault(key: Key<T>) = key.defaultValue

        override fun <T : Any> getValue(key: Key<T>) =
            throw IllegalStateException("Empty configuration has no value for key $key")

        override fun contains(key: Key<*>) = false

        override fun isEmpty() = true

        override val size: Int = 0
    }

    /**
     * Default immutable implementation of [EventConfiguration].
     *
     * Stores configuration entries in a map and provides access methods.
     *
     * @author Fantamomo
     * @since 1.4-SNAPSHOT
     */
    class EventConfigurationImpl<E : Dispatchable> @PublishedApi internal constructor(
        internal val data: Map<Key<*>, Any?>
    ) : EventConfiguration<E> {

        @Suppress("UNCHECKED_CAST")
        override operator fun <T> get(key: Key<T>): T? =
            data[key] as? T

        @Suppress("UNCHECKED_CAST")
        override fun <T> getOrDefault(key: Key<T>): T =
            data[key] as? T ?: key.defaultValue

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> getValue(key: Key<T>): T =
            data[key] as T

        override operator fun contains(key: Key<*>) =
            data.containsKey(key)

        override fun isEmpty(): Boolean =
            data.isEmpty()

        override val size: Int
            get() = data.size
    }

    fun toScope(): EventConfigurationScope<E> {
        if (isEmpty()) return EventConfigurationScope()
        return EventConfigurationScope(data.toMutableMap())
    }
}
