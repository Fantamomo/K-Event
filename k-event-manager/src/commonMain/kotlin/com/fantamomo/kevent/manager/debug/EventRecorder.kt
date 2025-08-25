package com.fantamomo.kevent.manager.debug

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.Listener

/**
 * An interface for managing and recording events within an event-driven system.
 *
 * [EventRecorder] provides mechanisms to collect, filter, and analyze interactions
 * with [Dispatchable] events. It allows for querying recorded events, applying live
 * filters, enabling or disabling the recording process, and clearing stored event data.
 *
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
interface EventRecorder : Listener {

    /**
     * Retrieves all recorded events.
     *
     * @return A list of all recorded events as instances of [RecordedEvent].
     */
    fun getAllEvents(): List<RecordedEvent>

    /**
     * Retrieves the number of recorded events that match the specified event class.
     *
     * @param eventClass The class of the event to count. Must be a subclass of [Dispatchable].
     * @return The count of events that match the specified event class.
     */
    fun getEventCount(eventClass: Class<out Dispatchable>): Int

    /**
     * Retrieves the most recently recorded event of the specified event class.
     *
     * @param eventClass The class of the event to retrieve. Must be a subclass of [Dispatchable].
     * @return The last recorded event of the specified class wrapped in a [RecordedEvent] instance,
     *         or `null` if no such event is found.
     */
    fun getLastEvent(eventClass: Class<out Dispatchable>): RecordedEvent?

    /**
     * Clears all recorded events from the internal storage, removing any previously tracked events.
     * After invoking this method, subsequent calls to event retrieval methods will return empty results
     * until new events are recorded.
     */
    fun clear()

    /**
     * Enables the event recorder, allowing it to start tracking and recording new events.
     * After calling this method, any events processed by the recorder will be saved for
     * future retrieval until the recorder is disabled.
     */
    fun enable()

    /**
     * Disables the event recording functionality, preventing any events from being
     * tracked or recorded until re-enabled. Once disabled, no new events will be
     * captured by the recorder.
     */
    fun disable()

    /**
     * Checks if the event recorder is currently enabled.
     *
     * @return True if the recorder is enabled and actively tracking events, false otherwise.
     */
    fun isEnabled(): Boolean

    /**
     * Retrieves all recorded events that match the specified event class.
     *
     * @param eventClass The class of the event to retrieve. Must be a subclass of [Dispatchable].
     * @return A list of [RecordedEvent] instances that correspond to the specified event class.
     */
    fun getEventsOfType(eventClass: Class<out Dispatchable>): List<RecordedEvent>

    /**
     * Checks if a recorded event of the specified type exists.
     *
     * This method determines whether at least one event that matches the given event class
     * has been recorded.
     *
     * @param eventClass The class of the event to check. Must be a subclass of [Dispatchable].
     * @return True if an event of the specified type has been recorded, false otherwise.
     */
    fun hasEventOfType(eventClass: Class<out Dispatchable>): Boolean

    /**
     * Retrieves a list of recorded events associated with a specific thread.
     *
     * @param threadName The name of the thread for which the recorded events should be retrieved.
     * @return A list of [RecordedEvent] instances associated with the specified thread.
     */
    fun getEventsByThread(threadName: String): List<RecordedEvent>

    /**
     * Retrieves all recorded events that occurred within a specified time range.
     *
     * @param start The start timestamp (inclusive) of the time range, represented in milliseconds since the epoch.
     * @param end The end timestamp (exclusive) of the time range, represented in milliseconds since the epoch.
     * @return A list of [RecordedEvent] instances that occurred within the specified time range.
     */
    fun getEventsInTimeRange(start: Long, end: Long): List<RecordedEvent>

    /**
     * Retrieves the most frequent event types along with their occurrence counts.
     *
     * This method analyzes all recorded events and identifies the event types
     * that have been recorded most frequently. The result is limited to a
     * specified number of top occurrences.
     *
     * @param limit The maximum number of event type-frequency pairs to return. Defaults to 5.
     * @return A list of pairs, where each pair consists of an event class (subclass of [Dispatchable])
     *         and its occurrence count, sorted in descending order by frequency.
     */
    fun getMostFrequentEvents(limit: Int = 5): List<Pair<Class<out Dispatchable>, Int>>

    /**
     * Calculates the average time interval between occurrences of events of the specified class.
     *
     * This method determines the elapsed time between consecutive events of the given type
     * and computes the average interval based on recorded data.
     *
     * @param eventClass The class of the event to analyze. Must be a subclass of [Dispatchable].
     * @return The average interval in milliseconds between occurrences of the specified event type.
     *         Returns 0.0 if there are fewer than two events of the specified type.
     */
    fun getAverageInterval(eventClass: Class<out Dispatchable>): Double

    /**
     * Restricts the event recorder to only record events of the specified types.
     *
     * This method allows filtering of events so that only events of the provided
     * types will be tracked and processed by the event recorder. Any events that
     * do not match the specified types will be ignored.
     *
     * @param types The allowed event types as an array of `Class` objects. Each `Class`
     *        must represent a subclass of [Dispatchable]. Only events matching these types
     *        will be recorded.
     */
    fun allowOnlyEventTypes(vararg types: Class<out Dispatchable>)

    /**
     * Restricts the event recorder to only record events from the specified threads.
     *
     * This method allows filtering of events so that only events generated by the
     * specified threads will be tracked and processed by the event recorder.
     * Any events generated by threads not specified in the parameter will be ignored.
     *
     * @param names The names of the threads that will be allowed to record events.
     */
    fun allowOnlyThreads(vararg names: String)

    /**
     * Clears all live filters applied to the event recorder.
     *
     * Live filters restrict the types of events or threads that the event recorder processes.
     * Invoking this method removes any such restrictions, allowing the recorder to process
     * all events and threads without filters.
     */
    fun clearLiveFilters()
}
