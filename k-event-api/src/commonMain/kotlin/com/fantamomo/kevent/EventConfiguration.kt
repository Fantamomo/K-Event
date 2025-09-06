package com.fantamomo.kevent

/**
 * Immutable container for event handler configuration data.
 *
 * This interface defines the API for accessing event configuration.
 * It is sealed so all implementations are known at compile time.
 *
 * @param E The type of event this configuration applies to
 *
 * @see EventConfigurationScope
 * @see Key
 */
sealed interface EventConfiguration<E : Dispatchable> {

    /**
     * Gets the value associated with the specified key, or null if no value is set.
     */
    operator fun <T> get(key: Key<T>): T?

    /**
     * Gets the value associated with the specified key, or the key's default value if none is set.
     */
    fun <T> getOrDefault(key: Key<T>): T

    /**
     * Gets the non-null value associated with the specified key.
     */
    fun <T : Any> getValue(key: Key<T>): T

    /**
     * Checks if the key exists in the configuration.
     */
    operator fun contains(key: Key<*>): Boolean

    /**
     * Whether the configuration contains no entries.
     */
    fun isEmpty(): Boolean

    /**
     * Number of entries in the configuration.
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

        override fun <T : Any> getValue(key: Key<T>) = throw IllegalStateException("Empty configuration has no value for key $key")

        override fun contains(key: Key<*>) = false

        override fun isEmpty() = true

        override val size: Int = 0
    }

    /**
     * Default immutable implementation of [EventConfiguration].
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
}
