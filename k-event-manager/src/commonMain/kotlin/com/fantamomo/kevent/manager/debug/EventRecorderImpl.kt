package com.fantamomo.kevent.manager.debug

import com.fantamomo.kevent.Dispatchable
import com.fantamomo.kevent.Register
import com.fantamomo.kevent.configuration
import com.fantamomo.kevent.manager.EventManager
import com.fantamomo.kevent.silent
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * [EventRecorderImpl] is an implementation of the [EventRecorder] interface.
 * It is used to record and manage events in an application, with various options
 * for filtering and managing recorded events.
 *
 * The class internally uses a concurrent deque to store the recorded events
 * and provides thread-safe operations for event handling. It supports filtering
 * events by type, thread, and time range, as well as additional configuration
 * options for managing how events are recorded.
 *
 * @property options The configuration options for the event recorder, such as
 *                   maximum event storage, recording manager/dispatcher information, and enabling/disabling recording.
 * @constructor Creates an instance of [EventRecorderImpl] with the given options.
 * @author Fantamomo
 * @since 1.0-SNAPSHOT
 */
@OptIn(ExperimentalTime::class)
class EventRecorderImpl(private val options: EventRecorderOptions = EventRecorderOptions()) : EventRecorder {

    private val events = ConcurrentLinkedDeque<RecordedEvent>()
    private var enabled = options.enabled

    private val liveTypeFilter = mutableSetOf<Class<out Dispatchable>>()
    private val liveThreadFilter = mutableSetOf<String>()

    private val stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)

    /**
     * Handles any dispatchable event, capturing event information and recording it if enabled.
     *
     * This method should never be called directly; it is intended to be invoked exclusively
     * through the EventManager system. It processes an event, applies configurations, and
     * ensures compatibility with various filters (like type and thread filters). If configured,
     * debugging information such as the dispatcher's and manager's class/method names will also
     * be recorded.
     *
     * @param event The dispatchable event to be handled. It can be null during configuration phases.
     */
    @Register
    fun onAnyEvent(event: Dispatchable?) {
        configuration(event) {
            silent = true // Prevent interfering with `DeadEvents`
        }

        if (!enabled) return

        if (liveTypeFilter.isNotEmpty() && event::class.java !in liveTypeFilter) return
        if (liveThreadFilter.isNotEmpty() && Thread.currentThread().name !in liveThreadFilter) return


        var managerClass: String? = null
        var managerMethod: String? = null
        var triggeringClass: String? = null
        var triggeringMethod: String? = null

        if (options.recordEventDispatcherInfo || options.recordEventManagerInfo) {
            val frames = stackWalker.walk { stream ->
                stream.skip(1)
                    .dropWhile {
                        !it.declaringClass.isAssignableFrom(EventManager::class.java)
                    }
                    .limit(if (options.recordEventDispatcherInfo) 2 else 1)
                    .toList()
            }

            if (options.recordEventManagerInfo) {
                val frame = frames.first()
                managerClass = frame.declaringClass.name
                managerMethod = frame.methodName
            }
            if (options.recordEventDispatcherInfo) {
                val frame = frames.last()
                triggeringClass = frame.declaringClass.name
                triggeringMethod = frame.methodName
            }
        }

        val recorded = RecordedEvent(
            event = event,
            timestamp = Clock.System.now(),
            threadName = Thread.currentThread().name,
            eventManagerClass = managerClass,
            eventManagerMethod = managerMethod,
            triggeringClass = triggeringClass,
            triggeringMethod = triggeringMethod,
        )

        events.add(recorded)

        while (events.size > options.maxEvents) {
            events.pollFirst()
        }
    }

    override fun getAllEvents(): List<RecordedEvent> = events.toList()

    override fun getEventCount(eventClass: Class<out Dispatchable>): Int =
        events.count { it.event::class.java == eventClass }

    override fun getLastEvent(eventClass: Class<out Dispatchable>): RecordedEvent? =
        events.lastOrNull { it.event::class.java == eventClass }

    override fun clear() = events.clear()

    override fun enable() { enabled = true }

    override fun disable() { enabled = false }

    override fun isEnabled(): Boolean = enabled

    override fun getEventsOfType(eventClass: Class<out Dispatchable>): List<RecordedEvent> =
        events.filter { it.event::class.java == eventClass }

    override fun hasEventOfType(eventClass: Class<out Dispatchable>): Boolean =
        events.any { it.event::class.java == eventClass }

    override fun getEventsByThread(threadName: String): List<RecordedEvent> =
        events.filter { it.threadName == threadName }

    override fun getEventsInTimeRange(start: Long, end: Long): List<RecordedEvent> =
        events.filter { it.timestamp.toEpochMilliseconds() in start..end }

    override fun getMostFrequentEvents(limit: Int): List<Pair<Class<out Dispatchable>, Int>> =
        events.groupingBy { it.event::class.java }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }

    override fun getAverageInterval(eventClass: Class<out Dispatchable>): Double {
        val times = events.filter { it.event::class.java == eventClass }
            .map { it.timestamp.toEpochMilliseconds() }
        if (times.size < 2) return 0.0
        return times.zipWithNext { a, b -> (b - a).toDouble() }.average()
    }

    override fun allowOnlyEventTypes(vararg types: Class<out Dispatchable>) {
        liveTypeFilter.clear()
        liveTypeFilter.addAll(types)
    }

    override fun allowOnlyThreads(vararg names: String) {
        liveThreadFilter.clear()
        liveThreadFilter.addAll(names)
    }

    override fun clearLiveFilters() {
        liveTypeFilter.clear()
        liveThreadFilter.clear()
    }
}
