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

Listeners in K-Event are just regular Kotlin classes that implement the `Listener` interface — a **marker interface**.  
This means it doesn’t declare any methods; its sole purpose is to indicate that a class contains event handlers.

#### Why use a marker interface?

- Makes it easy to detect valid listeners via reflection
- Prevents accidental registration of unrelated classes
- Keeps the API surface clean and intuitive

This approach aligns with Kotlin’s idiomatic style and avoids the need for verbose base classes.

> A listener can be any Kotlin type (except annotations), but we recommend using a `class` or `object`.

---

### ⚠️ Handler Methods: Signature Requirements

Event handler methods must follow a strict but straightforward signature:

```kotlin
@Register
fun onJoin(event: PlayerJoinedEvent?)
````

#### Requirements

* Must be annotated with `@Register`
* Must have exactly **one** parameter (additional parameters are allowed only when using [parameter injection](#-parameter-injection))
* The parameter must be **nullable** (for custom configuration) and extend `Dispatchable`
* Must **not** be annotated with `@JvmStatic`

#### Why nullable?

K-Event invokes each handler once during registration with `null` to extract configuration.
If the parameter is non-nullable, it behaves as if using `emptyConfiguration`.

This design allows per-handler configuration without needing a separate config function or builder.

> Although listener methods can be open, it is strongly discouraged as it may lead to unexpected errors.  
> Default parameter values are technically allowed, but they are ignored by the DefaultEventManager.  
> Using `@JvmOverloads` can lead to unexpected errors when registering handlers.
---

### ⚖️ Configuration DSL

The configuration DSL is invoked only when the event argument is `null` (i.e. during registration).

```kotlin
configuration(event) {
    priority = Priority.HIGH
    disallowSubtypes = true
    exclusiveListenerProcessing = true
    silent = true
    ignoreStickyEvents = true
    name = "JoinHandler"
}
```

> It is `inline`, so no lambda generation at runtime

#### Options

- **`priority`**: Determines handler execution order. Higher values run first.
- **`disallowSubtypes`**: If `true`, only matches this exact event class.
- **`exclusiveListenerProcessing`**: Prevents this handler from running concurrently. Applies per `SharedExclusiveExecution` instance.
- **`silent`**: If `true`, the handler will not prevent a `DeadEvent` from being emitted.
- **`ignoreStickyEvents`**: If `true`, the handler will not receive sticky events.
- **`name`**: Optional debug label for this method.

The configuration is stored inside the manager’s registry and applied every time this event is dispatched.

Using `emptyConfiguration(event)` is equivalent to using the default configuration (e.g., `priority = 0`, etc.).

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

K-Event supports both **regular** and **`suspend` handlers**. The event system automatically detects whether a handler is `suspend` and invokes it appropriately.

#### Default behavior with `dispatch(...)`

When you call:

```kotlin
manager.dispatch(event)
```

the system:

1. Collects all handlers applicable to the event.
2. Sorts them by their configured priority (highest first).
3. Invokes each handler in order:
   - **Regular (non-suspend) handlers** are called immediately in the current thread.
   - **`suspend` handlers** are launched in a new coroutine using `Dispatchers.Unconfined`.  
     This lets the handler start immediately on the current thread, potentially modifying the event early, without blocking the overall dispatch process.

This approach allows suspend handlers to begin execution quickly while the dispatch itself does not wait for them to finish.

---

#### Controlled execution with `dispatchSuspend(...)`

If you need all handlers — including `suspend` ones — to **complete before proceeding**, use:

```kotlin
manager.dispatchSuspend(MyEvent(...))
```

In this mode:

1. **All handlers** (regular and `suspend`) run in **priority order**.
2. The manager **waits** for each `suspend` handler to finish before moving to the next handler.
3. Handlers execute **sequentially**, ensuring deterministic behavior.

---

#### `isWaiting`

Handlers can optionally receive an `isWaiting: Boolean` parameter.  

For none `suspend` handlers, `isWaiting` is always `true`. 

For `suspend` handlers, `isWaiting` is:
- `false` when using `dispatch`  
- `true` when using `dispatchSuspend`

This lets handlers know whether the caller is waiting for completion.

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

There are 5 default injectable parameter:

- `manager: EventManager`: The instance of the EventManager is passed, which calls the handler.
- `logger: Logger`: A instance for logging, all `DefaultEventManager` have the same.
- `scope: CoroutineScope`: It can be used to launch new coroutines.
- `isWaiting: Boolean`: For non suspend handler it will always be `true`, for suspend handler:
  - when called with `dispatch` it is `false`
  - when called with `dispatchSuspend` it is `true`
- `config: EventConfiguration<*>`: The configuration of the handler.
- `isSticky: Boolean`: If the event is sticky. See [Sticky Events](#sticky-events).

The following example disables `scope` and `logger`:

```kotlin
EventManager(Settings.DISABLE_SCOPE_INJECTION.with(true) + Settings.DISABLE_LOGGER_INJECTION.with(true))
```

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

The `DefaultEventManager` **cannot** check generic types at runtime.  
As a result, both listeners will be triggered for an event like `MyGenericEvent<Int>`.

This is problematic because `onMyEventString` expects an event with `String`, but it receives one with `Int`.

To address this, K-Event introduces two new interfaces:

- `GenericTypedEvent`
- `SingleGenericTypedEvent`

> Listeners can specify type parameters using `*`, `out T`, `T`, or `in T`.


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

> **Warning:**  
> If an event is generic **and** inherits from another generic event, and you listen to the parent event while a child event is fired,  
> the event manager currently **cannot** verify the generic type in this inheritance case.  
> This means the listener for the parent event might still be called even if the generic type does not match.  
> A solution for this is being worked on.

---

## Sticky Events

A **sticky event** is an event stored in the manager and automatically delivered to all **newly registered** listeners.

To mark an event as *sticky*, set the `sticky` property in the `DispatchConfig` class to `true`:

```kotlin
manager.dispatch(MyEvent(...)) {
    sticky = true
}

// or

val config = createDispatchConfig {
    sticky = true
}

manager.dispatch(MyEvent(...), config)
```

When a new listener is registered, it will immediately receive the stored event.

> Only the **most recent** sticky event of a given type is kept.

There is also a new injectable parameter `isSticky: Boolean` that can be used to check whether an event is sticky.

---

## Listener Invoker

The **ListenerInvoker** is a core component of the event manager.
It is responsible for binding `Listener` methods to events and invoking them during event processing.

Listener methods can be invoked in two different ways:

### 🔍 ReflectionListenerInvoker

* Uses **Kotlin reflection** to dynamically call listener methods.
* Supports both regular and `suspend` functions.
* Acts as a **fallback** if another invoker fails during binding.
* Easy to use, but generally slower compared to method handles.
* The default invoker.

```kotlin
val invoker = ListenerInvoker.reflection()
```

### ⚡ MethodHandlerListenerInvoker

* Uses the **Java MethodHandle API** for high-performance invocation.
* Supports both regular and `suspend` listener methods.
* By default relies on `MethodHandles.publicLookup()`.
  Since listener methods must always be `public`, there is no need to create a private lookup in most cases.
* **Limitations:** Cannot handle classes declared inside functions or anonymous classes when using the default `publicLookup()`.
  In these cases, the system automatically falls back to the `ReflectionListenerInvoker`.
  Private classes **can** be handled if a proper `MethodHandles.Lookup` is provided instead of the default.
* Faster than reflection and recommended if performance is important.

```kotlin
val invoker = ListenerInvoker.methodHandler()
```

---

## Create EventManagers

The default way to create a `DefaultEventManager` is via the `EventManager` function. However, you can customize it with additional components.

By default, the manager ignores errors thrown in listeners. If you want to handle or log these errors, you can provide an `ExceptionHandler`:

```kotlin
object SysOutExceptionHandler : ExceptionHandler() {
    override fun handle(exception: Throwable, listener: Listener?, method: KFunction<*>?) {
        exception.printStackTrace()
    }
}

val manager = EventManager(SysOutExceptionHandler)
```

### Custom Parameters

You can also inject custom parameters into listeners:

```kotlin
val server: Server = ...

val staticParameter = ListenerParameterResolver.static("server", Server::class, server)
val dynamicParameter = ListenerParameterResolver.dynamic("time", Instant::class) { Clock.System.now() }

val manager = EventManager(staticParameter + dynamicParameter)
```

There are two types of `ListenerParameterResolver`:

* **static**: Value does not change.
* **dynamic**: Value can change (e.g., depending on runtime conditions).  
  A dynamic resolver **must** provide a default value (e.g., `0`, empty, etc.) for listener registration.

Example usage in a listener:

```kotlin
class ServerListener : Listener {
    @Register
    fun onServerReload(event: ServerReloadEvent?, server: Server) {
        ...
    }
}
```

You can add multiple `ListenerParameterResolver`s, but only **one** `ExceptionHandler` per manager.

---

### Shared Exclusive Execution

The `SharedExclusiveExecution` component can prevent concurrent execution of handlers when using `exclusiveListenerProcessing`.  
Each `EventManager` has its own `SharedExclusiveExecution` instance, but you can override it when adding it to the manager using the `+` operator.

---

### Combining Components

You can combine multiple components when creating a manager:

```kotlin
val sharedExecution = SharedExclusiveExecution()
val invoker = ListenerInvoker.methodHandler()

val manager = EventManager(
    SysOutExceptionHandler +
    staticParameter +
    dynamicParameter +
    sharedExecution +
    invoker
)
```

For more on `ListenerInvoker`, see [Listener Invoker](#listener-invoker).

## 🎓 How to Add to Your Project

> Note: K-Event is currently not published to Maven Central.

To get started:

1. Clone the GitHub repository
2. Run `./gradlew publishToMavenLocal`
3. Add this to your `build.gradle.kts`:

```kotlin
// Required API module
implementation("com.fantamomo:k-event-api:${api-version}")

// Optional manager (JVM only for now)
implementation("com.fantamomo:k-event-manager:${manager-version}")
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

API-Version: **1.6-SNAPSHOT**
Manager-Version: **1.17-SNAPSHOT**

Happy event handling!
