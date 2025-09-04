package com.fantamomo.kevent.manager

import com.fantamomo.kevent.*
import com.fantamomo.kevent.manager.components.ExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KFunction
import kotlin.test.*

private data class TestEvent(val message: String) : Dispatchable()

private class DeadEventListener : Listener {
    var deadEventCalled = false

    @Register
    fun onDead(event: DeadEvent<*>) {
        deadEventCalled = true
    }
}

private class TestListener : Listener {
    var called = false

    @Register
    fun onTestEvent(event: TestEvent) {
        called = true
    }
}

private data class MyGenericEvent<T : Any>(val data: T) : Dispatchable(), SingleGenericTypedEvent {
    override fun extractGenericType() = data::class
}

private class PriorityListener(val id: Int, val callOrder: MutableList<Int>) : Listener {
    @Register
    fun onEvent(event: TestEvent?) {
        configuration(event) {
            priority = Priority(id)
        }
        callOrder.add(id)
    }
}

private class FailingListener : Listener {
    @Register
    fun onEvent(event: TestEvent) {
        throw RuntimeException("Fail!")
    }
}

private class StringEventListener : Listener {
    var called = false

    @Register
    fun onEvent(event: MyGenericEvent<String>) {
        called = true
    }
}

private class IntEventListener : Listener {
    var called = false

    @Register
    fun onEvent(event: MyGenericEvent<Int>) {
        called = true
    }
}

class DefaultEventManagerTest {

    private lateinit var manager: DefaultEventManager

    @BeforeTest
    fun setup() {
        val components = object : ExceptionHandler() {
            override fun handle(
                exception: Throwable,
                listener: Listener?,
                methode: KFunction<*>?,
            ) {
                println("ExceptionHandler caught: ${exception.message}")
            }
        }

        manager = EventManager(components) as DefaultEventManager
    }

    @Test
    fun `register and dispatch simple event`() {
        val listener = TestListener()
        manager.register(listener)

        manager.dispatch(TestEvent("Hello"))
        assertTrue(listener.called)
    }

    @Test
    fun `unregister should stop listener from being called`() {
        val listener = TestListener()
        manager.register(listener)
        manager.unregister(listener)

        listener.called = false
        manager.dispatch(TestEvent("No call expected"))
        assertFalse(listener.called)
    }

    @Test
    fun `lambda listener should be called`() {
        val flag = AtomicBoolean(false)
        manager.register(TestEvent::class, EventConfiguration.default()) {
            flag.set(true)
        }

        manager.dispatch(TestEvent("Lambda test"))
        assertTrue(flag.get())
    }

    @Test
    fun `suspend lambda listener should be called`() = runBlocking {
        val flag = AtomicBoolean(false)
        manager.registerSuspend(TestEvent::class, EventConfiguration.default()) {
            flag.set(true)
        }

        manager.dispatchSuspend(TestEvent("Suspend lambda test"))
        assertTrue(flag.get())
    }

    @Test
    fun `should dispatch dead event if no listener found`() {
        val deadListener = DeadEventListener()
        manager.register(deadListener)

        manager.dispatch(object : Dispatchable() {}) // unbekanntes Event
        assertTrue(deadListener.deadEventCalled)
    }

    @Test
    fun `should respect listener priority`() {
        val callOrder = mutableListOf<Int>()
        val high = PriorityListener(1, callOrder)
        val low = PriorityListener(2, callOrder)

        val highConfig = createConfigurationScope<TestEvent> {
            priority = Priority(100)
        }
        val lowConfig = EventConfiguration.default<TestEvent>()

        manager.register(TestEvent::class, highConfig) { high.onEvent(it) }
        manager.register(TestEvent::class, lowConfig) { low.onEvent(it) }

        manager.dispatch(TestEvent("Order test"))
        assertEquals(listOf(1, 2), callOrder)
    }

    @Test
    fun `exclusive listener should not be called concurrently`() = runBlocking {
        val counter = AtomicInteger(0)
        val config = createConfigurationScope<TestEvent> {
            exclusiveListenerProcessing = true
        }

        manager.registerSuspend(TestEvent::class, config) {
            val before = counter.incrementAndGet()
            assertEquals(1, before, "Should not be called concurrently")
            delay(50)
            counter.decrementAndGet()
        }

        launch { manager.dispatchSuspend(TestEvent("one")) }
        launch { manager.dispatchSuspend(TestEvent("two")) }
        delay(200)
    }

    @Test
    fun `failing listener should not crash manager`() {
        val fail = FailingListener()
        val ok = TestListener()

        manager.register(fail)
        manager.register(ok)

        manager.dispatch(TestEvent("Test"))
        assertTrue(ok.called, "Other listeners should still run after failure")
    }

    @Test
    fun `closed manager should throw`() {
        manager.close()
        assertFailsWith(IllegalStateException::class) {
            manager.dispatch(TestEvent("Should fail"))
        }
    }

    @Test
    fun `multiple listeners of same class should both be called`() {
        val l1 = TestListener()
        val l2 = TestListener()
        manager.register(l1)
        manager.register(l2)

        manager.dispatch(TestEvent("Hello"))
        assertTrue(l1.called)
        assertTrue(l2.called)
    }

    @Test
    fun `should not call handler when DISALLOW_SUBTYPES is set`() {
        open class ParentEvent : Dispatchable()
        class ChildEvent : ParentEvent()

        var called = false
        val config = createConfigurationScope<ParentEvent> {
            disallowSubtypes = true
        }

        manager.register(ParentEvent::class, config) { called = true }
        manager.dispatch(ChildEvent())

        assertFalse(called, "Handler should not be called for subtype when DISALLOW_SUBTYPES is true")
    }

    @Test
    fun `all dead event listeners should be called`() {
        val d1 = DeadEventListener()
        val d2 = DeadEventListener()
        manager.register(d1)
        manager.register(d2)

        manager.dispatch(object : Dispatchable() {})
        assertTrue(d1.deadEventCalled)
        assertTrue(d2.deadEventCalled)
    }

    @Test
    fun `listener with suspend and non-suspend should both run`() = runBlocking {
        class MixedListener : Listener {
            var normalCalled = false
            var suspendCalled = false

            @Register
            fun normal(event: TestEvent) { normalCalled = true }

            @Register
            suspend fun susp(event: TestEvent) { suspendCalled = true }
        }

        val listener = MixedListener()
        manager.register(listener)

        manager.dispatch(TestEvent("normal"))
        manager.dispatchSuspend(TestEvent("suspend"))

        assertTrue(listener.normalCalled)
        assertTrue(listener.suspendCalled)
    }

    @Test
    fun `close can only be called once`() {
        manager.close()
        assertFailsWith<IllegalStateException> { manager.close() }
    }

    @Test
    fun `generic typed event works with listener classes`() {
        val stringListener = StringEventListener()
        val intListener = IntEventListener()

        manager.register(stringListener)
        manager.register(intListener)

        manager.dispatch(MyGenericEvent("Hello"))
        manager.dispatch(MyGenericEvent(123))

        assertTrue(stringListener.called)
        assertTrue(intListener.called)
    }

    @Test
    fun `generic typed event works with listener classes 2`() {
        val stringListener = StringEventListener()
        val intListener = IntEventListener()

        manager.register(stringListener)
        manager.register(intListener)

        manager.dispatch(MyGenericEvent("Hello"))

        assertTrue(stringListener.called)
        assertFalse(intListener.called)
    }

    @Test
    fun `parameter injection works`() {
        class InjectListener : Listener {
            var receivedManager: EventManager? = null
            var receivedIsWaiting: Boolean? = null

            @Register
            fun onEvent(event: TestEvent?, manager: EventManager, isWaiting: Boolean) {
                emptyConfiguration(event)
                receivedManager = manager
                receivedIsWaiting = isWaiting
            }
        }

        val listener = InjectListener()
        manager.register(listener)
        manager.dispatch(TestEvent("Check Injection"))

        assertSame(manager, listener.receivedManager)
        assertFalse(listener.receivedIsWaiting != false)
    }
}
