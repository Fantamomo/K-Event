## K-Event: A Type-Safe Event System for Kotlin Multiplatform

Let’s dive right in — here’s what K-Event looks like in action. We’ll follow it up with a full breakdown, practical design reasoning, and insights into advanced capabilities.

```kotlin
// 1. Define your events
class PlayerJoinedEvent(val playerId: String, val username: String) : Event()
class PlayerLeftEvent(val playerId: String) : Event()

// 2. Create a listener that reacts to events
class GameListener : Listener {

    @Register
    fun onJoin(event: PlayerJoinedEvent?) {
        configuration(event) {
            priority = Priority.HIGH
        }
        println("${event.username} joined the game!")
    }

    @Register
    fun onLeave(event: PlayerLeftEvent) {
        println("Goodbye ${event.playerId}!")
    }
}

// 3. Set up the event system and dispatch events
fun main() {
    val manager = DefaultEventManager()
    manager.register(GameListener())

    manager.dispatch(PlayerJoinedEvent("123", "Fantamomo"))
    manager.dispatch(PlayerLeftEvent("123"))
}
```

---

## 🛠️ Code Walkthrough (Line by Line)

Let’s explore every part of this code so you understand not just **how** to use K-Event, but **why** each part works the way it does. We’ll also connect the lines of code to the architectural ideas behind the system.

---

### ✅ Step 1: Defining Events

```kotlin
class PlayerJoinedEvent(val playerId: String, val username: String) : Event()
```

#### What is an Event?

An `Event` is a class representing an action or occurrence in your application.
In K-Event, all events **must** extend the `Dispatchable` class (`Event` is a subclass of it) to be recognized by the event system.

Why inheritance? It allows:

* Consistent typing across handlers
* Support for subtyping (or restricting it via `disallowSubtypes`)
* Marker-based discovery at runtime

This pattern ensures events are lightweight, fast to dispatch, and predictable.

You can add any number of fields to an event. There are no restrictions on how complex they can be. This design makes K-Event flexible enough for UI events, network callbacks, system messages, and more.

> Events **can** be generic see [Generic Events](#generic-events)

> For more infos about `Event` and `Dispatchable`: [link](#-dispatchable-vs-event)

---

### ✅ Step 2: Creating a Listener

```kotlin
class GameListener : Listener
```

Listeners are just normal Kotlin classes, but they implement the `Listener` interface, which is a **marker interface**.
That means it doesn’t declare any methods — it’s only used to indicate that a class contains event handlers.

Why use a marker interface?

* Makes it trivial to detect valid listeners via reflection
* Prevents accidental registration of irrelevant classes
* Keeps API surface clean and intuitive

This fits Kotlin’s idiomatic style and avoids verbose base classes.

> A Listener could be any Kotlin type, but we recommend `class` or `object`

---

### ⚠️ Handler Methods: Signature Requirements

Handlers must follow a strict but simple signature:

```kotlin
@Register
fun onJoin(event: PlayerJoinedEvent?)
```

* Must be annotated with `@Register`
* Must have exactly **one** parameter (it could have more when using [parameter injection](#-parameter-injection))
* The parameter must be nullable (when using custom configuration) and extend `Dispatchable`
* Must not have the `@JvmStatic` annotation

#### Why nullable?

K-Event calls each handler one time in registration with `null`, to extract the configuration.

If the parameter is non-nullable, it behaves the same as using `emptyConfiguration`.

This enables per-handler configuration without requiring a separate config function or builder.

> Although listener methods can be open, we strongly advise against it as it may lead to unexpected errors.

---

### ⚖️ Configuration DSL

The configuration DSL is invoked only when the event argument is `null` (i.e. during registration).

```kotlin
configuration(event) {
    priority = Priority.HIGH
    disallowSubtypes = true
    exclusiveListenerProcessing = true
    name = "JoinHandler"
}
```

> It is `inline`, so no lambda generation at runtime

#### Options:

* `priority`: Determines handler execution order. Higher runs first.
* `disallowSubtypes`: If `true`, only matches this exact event class.
* `exclusiveListenerProcessing`: Prevents this handler from running concurrently. 
This applies per EventManager instance — multiple managers may still invoke the handler concurrently.
* `name`: Optional debug label for this method.

The configuration is stored inside the manager’s registry and used every time this event is dispatched.

Using `emptyConfiguration(event)` is equivalent to using the default configuration (priority = 0, etc.).

---

### ✅ Step 3: Registering Listeners

```kotlin
val manager = EventManager()
manager.register(GameListener())
```

`EventManager` is a factory methode and comes from the optional `k-event-manager` module. It returns a `DefaultEventManager` instance. It provides:

* Listener scanning and registration
* Dispatch pipeline with priority sorting
* Optional injection (advanced use) see [this](#-parameter-injection)
* Dead event monitoring

Calling `register()` triggers:

* Discovery of `@Register`-annotated methods
* Execution of those methods with `null`
* Internal configuration capture

You can register as many listeners as needed, at any point in time.

---

### ⚛️ Step 4: Dispatching Events

```kotlin
manager.dispatch(PlayerJoinedEvent("123", "Fantamomo"))
```

The dispatch mechanism follows these steps:

1. Locate all handlers for the event type (and optionally subtypes)
2. Sort them by priority (higher first)
3. Call each method with the real event
4. If no handlers match, emit a `DeadEvent`

Dispatch is synchronous by default — the method will return only after all handlers finish.

---

## 💡 Advanced Features

### ⏳ Suspend Handlers and `dispatchSuspend`

K-Event supports not only regular handlers but also **`suspend` handlers**.  
The event system automatically detects whether a method is `suspend` and invokes it accordingly.

#### Default behavior with `dispatch(...)`

When `manager.dispatch(event)` is called, the system:

1. Collects all handlers that should be invoked for the event.
2. Sorts them by their configured priority (highest first).
3. Iterates through each handler in order:
    - If the handler is **regular (non-suspend)**, it is invoked immediately in the current thread.
    - If the handler is **`suspend`**, it is launched in a new coroutine starting on `Dispatchers.Unconfined`.
   This allows the handler to begin execution immediately on the current thread—enabling it to modify the event before any further processing—then it.

This design lets suspend handlers start quickly and potentially mutate the event early, while the overall dispatch does not block on their completion.

---

#### Controlled execution with `dispatchSuspend(...)`

Sometimes you need all handlers — including `suspend` ones — to **fully complete** before continuing.  
That’s what:

```kotlin
manager.dispatchSuspend(MyEvent(...))
````

is for.

In this mode:

1. **All handlers** (regular & `suspend`) are executed in the **configured priority order**.
2. If a `suspend` handler is encountered, the manager **waits** for it to complete before moving on.
3. Only then will the next handler be invoked.

The result: a deterministic sequence where each handler finishes before the next one starts.

#### `isWaiting`

Handlers can receive an injected `isWaiting: Boolean` parameter that indicates whether the caller is waiting for the handler to complete.
This value is `false` when using `dispatch` and `true` when using `dispatchSuspend`.

---


### ✨ Custom Configuration Keys

You can define arbitrary keys to extend the configuration system:

```kotlin
interface Cancellable {
    var cancelled: Boolean
}

object MyKeys {
    val ASYNC = Key("async", false)
    val TIMEOUT = Key("timeout", 1000L)
    val IGNORE_CANCELLED = Key("ignoreCancelled", false)
}

var <T> EventConfigurationScope<T>.ignoreCancelled: Boolean where T : Dispatchable, T : Cancellable
    get() = getOrDefault(MyKeys.IGNORE_CANCELLED)
    set(value) = set(MyKeys.IGNORE_CANCELLED, value)
```

Usage:

```kotlin
configuration(event) {
    ignoreCancelled = true // But only when `event` is a instance of `Cancellable`
    set(MyKeys.ASYNC, true)
}
```

Use these keys in your manager or plugin logic to modify behavior dynamically.

---

### 🔧 Lambda Handlers

K-Event supports lambda-based registration:

```kotlin
manager.register(
    PlayerJoinedEvent::class,
    createConfigurationScope { priority = Priority.HIGH },
) { event ->
    println("Welcome ${event.username}")
}
```

This feature is perfect for:

* Dynamic plugins
* Unit tests
* Simple app-specific logic

No boilerplate listener classes needed.

---

### ⚠️ Dead Events

If an event is dispatched but no handlers exist, a `DeadEvent` is emitted instead.

```kotlin
class DeadEventLogger : Listener {
    @Register
    fun onDead(event: DeadEvent<*>?) {
        emptyConfiguration(event)
        println("Unhandled event: ${event.event::class.simpleName}")
    }
}
```

This lets you debug missing listeners, misconfigurations, or build fallback logic.

---

## 🔍 Additional Architecture Details

### 🔁 Reflection-Based Dispatching

All handler invocations in K-Event use **reflection** under the hood (except lambda-based handlers). That means:

* Handler methods are not compiled as function references.
* Dispatch uses reflection to invoke the correct method at runtime.
* This makes registration lightweight, but dispatch slightly slower compared to inlined call sites.

If you want **zero-reflection dispatch**, use lambda-based handler registration:

```kotlin
manager.register(MyEvent::class) { event ->
    println("Handled $event")
}
```

This avoids reflection entirely.

---

### 📦 Dispatchable vs Event

K-Event defines a base class called `Dispatchable`. The `Event` class — which you typically extend — is a subclass of `Dispatchable`.

This structure exists to:

* Allow more abstract or special types of messages (e.g. `DeadEvent`) to participate in the dispatch system.
* Enable listeners to register for all dispatched content using `Dispatchable`.

However, this also has a side effect:

> A listener registered for `Event` will **not** receive `DeadEvent`, because `DeadEvent` does not inherit from `Event`, only from `Dispatchable`.

Furthermore:

> If any listener is registered for `Dispatchable`, then **no `DeadEvent` will ever be dispatched**.

This is an intentional design decision: if you're listening to absolutely everything, there's no such thing as a "dead" event.

---

### 💉 Parameter Injection

K-Event supports optional **parameter injection** in the `k-event-manager` module. This means that beyond the first `Event?` parameter, additional parameters may be injected automatically.

Example:

```kotlin
class AdvancedListener : Listener {
    @Register
    fun onEvent(
        event: MyEvent?,
        manager: EventManager, // Injected automatically
        logger: Logger,        // Injected if supported
        @InjectionName("config") myConfig: MyConfig // Custom injection
    ) {
        emptyConfiguration(event)
        logger.info("Event received: $event")
    }
}
```

You can extend the manager with your own parameter resolvers to inject services, configurations, or contextual data.

Every Injection has a name and a type, for example `manager` is the name and `EventManager` is the type, only if the type and the name agree,
the listener can be registered.

Sometimes you need in the function another variable name instead of the injection name,
if that happened you can use `@InjectionName`, where the name in brackets is the name that is uses in the system.

---

## Generic Events

K-Event support generic events.

This means that an event that is generic (like DeadEvent) can be handled by listeners.

Example:

```kotlin
data class MyGenericEvent<T : Any>(val value: T) : Event()

class MyListener : Listener {
    
    @Register
    fun onMyEvent(event: MyGenericEvent<*>?) {
        //...
    }
    
    @Register
    fun onMyEventString(event: MyGenericEvent<String>?) {
        //...
    }
}
```

The `DefaultEventManager` **can not** check the generic type at runtime.
In this case both of the listeners will be called when a event like `MyGenericEvent<Int>` is called.
That is a problem because in `onMyEventString` we want the event with `String` but get it with `Int`.

K-Event adds two new interfaces `GenericTypedEvent` and `SingleGenericTypedEvent`.

> Listeners can use `*`, `out T`, `T` and `in T`


```kotlin
data class MyGenericEvent<T : Any>(val value: T) : Event(), SingleGenericTypedEvent {
    override fun extractGenericType(): KClass<*> = value::class
}

class MyListener : Listener {
    
    @Register
    fun onMyEvent(event: MyGenericEvent<*>?) {
        //...
    }
    
    @Register
    fun onMyEventString(event: MyGenericEvent<String>?) {
        //...
    }
}
```

With `SingleGenericTypedEvent` the event manager **can** check the type at runtime and when `MyGenericEvent<Int>` is dispatched,
only `onMyEvent` is called. 

---

## Create EventManagers

The default function for creating a `DefaultEventManager` is `EventManager`, but there is more.

The `EventManager` function takes Components, for example the default manager will ignore errors thrown in listeners,
if you want to log this errors you can use a `ExceptionHandler`.

```kotlin
object SysOutExceptionHandler : ExceptionHandler() {
    override fun handle(exception: Throwable, listener: Listener?, methode: KFunction<*>?) {
        exception.printStackTrace()
    }
}

val manager = EventManager(SysOutExceptionHandler)
```

But there is more, do you want a custom injectable parameter.

```kotlin
val server: Server = ...

val parameter = ListenerParameterResolver.static("server", Server::class, server)
val dynamicParameter = ListenerParameterResolver.dynamic("time", Instant::class) { Clock.System.now() }

val manager = EventManager(parameter)
```

There are two types of `ListenerParameterResolver`:

* `static`: The type does not change.
* `dynamic`: The type can change (e.g. because of listeners).
Note that a dynamic type **must** provide a default value (e.g. `0`, empty, ...), which is uses when registering a listener, so that the signatur is completed.

```kotlin
class ServerListener : Listener {
    @Register
    fun onServerReload(event: ServerReloadEvent?, server: Server) {
        ...
    }
}
```

If you want to combine some components:

```kotlin
val manager = EventManager(SysOutExceptionHandler + parameter + dynamicParameter)
```

You can add as many `ListenerParameterResolver` as you want, but only one `ExceptionHandler`.

## 🎓 How to Add to Your Project

> Note: K-Event is currently not published to Maven Central.

To get started:

1. Clone the GitHub repository
2. Run `./gradlew publishToMavenLocal`
3. Add this to your `build.gradle.kts`:

```kotlin
// Required API module
implementation("com.fantamomo:k-event-api:1.0-SNAPSHOT")

// Optional manager (JVM only for now)
implementation("com.fantamomo:k-event-manager:1.0-SNAPSHOT")
```

You can also include it as a source module directly in your project if needed.

---

## 🌐 Multiplatform Support

K-Event is designed from the ground up for multiplatform development:

| Module            | Platforms          | Notes                                        |
|-------------------|--------------------|----------------------------------------------|
| `k-event-api`     | JVM, JS, Native    | Lightweight, zero dependencies               |
| `k-event-manager` | JVM only (for now) | Future versions will expand platform support |

You can use the `api` module even in shared/common codebases.

---

## 🌎 API Summary

| Element                      | Description                                  |
|------------------------------|----------------------------------------------|
| `Event`                      | Base class for custom events                 |
| `Listener`                   | Marker interface for event receiver classes  |
| `@Register`                  | Marks a handler method                       |
| `EventManager`               | Interface for event dispatch & registration  |
| `DefaultEventManager`        | Built-in manager implementation              |
| `configuration(event)`       | Registers config during handler registration |
| `Priority`                   | Enum & factory for handler execution order   |
| `Key<T>`                     | Typed config extension mechanism             |
| `DeadEvent`                  | Wraps unhandled events                       |
| `createConfigurationScope()` | Used with lambda handler registration        |

---

## 🚀 Why Use K-Event?

* ✅ Kotlin-first, Kotlin-only design
* ✅ Type-safe with minimal reflection
* ✅ Works across JVM, JS, and Native (core module)
* ✅ Easy to extend with custom behavior
* ✅ Declarative configuration DSL
* ✅ Fully isolated — no global state or static handlers
* ✅ Production-ready performance

---

## Built by Fantamomo

Version: **1.0-SNAPSHOT**

Happy event handling!
