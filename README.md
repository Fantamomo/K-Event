# KEvent – A Type-Safe and Flexible Event System for Kotlin

**KEvent** is a lightweight and powerful event system designed specifically for Kotlin. It provides a clean and type-safe way to decouple parts of your application, using observer-style logic. Perfect for games, plugin frameworks, or any app that needs structured event handling.

---

## 🚀 Quick Example

```kotlin
class PlayerJoinedEvent(val playerId: String, val username: String) : Event()
class PlayerLeftEvent(val playerId: String) : Event()

class GameListener : Listener {

    @Register
    fun onJoin(event: PlayerJoinedEvent?) {
        configuration(event) {
            priority = Priority.HIGH
        }
        println("${event.username} joined the game!")
    }

    @Register
    fun onLeave(event: PlayerLeftEvent?) {
        emptyConfiguration(event)
        println("Goodbye ${event.playerId}!")
    }
}

fun main() {
    val manager = DefaultEventManager()
    manager.register(GameListener())

    manager.dispatch(PlayerJoinedEvent("123", "Fantamomo"))
    manager.dispatch(PlayerLeftEvent("123"))
}
```

---

## 🧠 How It Works

### 🔹 Define Events

All events extend the `Event` base class:

```kotlin
class GameStartedEvent(val gameId: String) : Event()
```

### 🔹 Create Listeners

Listeners implement the `Listener` interface and define methods annotated with `@Register`. Each method takes one nullable event parameter:

```kotlin
@Register
fun onStart(event: GameStartedEvent?) {
    configuration(event) {
        priority = Priority.LOW
    }
    // event is non-null here
}
```

Why nullable? Because KEvent calls the method once with `null` to collect configuration before actual events are dispatched.

### 🔹 Dispatch Events

To trigger an event, just call:

```kotlin
manager.dispatch(GameStartedEvent("game-42"))
```

Listeners with matching event types are called, ordered by priority.

> The `DefaultEventManager` is located in a separate module to keep the `api` module clean.
> This separation ensures the `api` module remains free of any implementation details that could conflict with custom
> event systems.

---

## ⚙️ Configuration

KEvent provides a DSL to configure how each handler behaves:

```kotlin
configuration(event) {
    priority = Priority.HIGH
    priority = Priority.Custom(750) // For fine-tuned order
    disallowSubtypes = true
    exclusiveListenerProcessing = true
    name = "JoinListener"
    set(MyKeys.ASYNC, true)
}
```

> If you don’t need custom config, use `emptyConfiguration(event)` instead.

### Built-in options:

* `priority`: Order in which listeners are executed
* `disallowSubtypes`: Restrict to exact event class
* `exclusiveListenerProcessing`: Avoid recursive or concurrent handling
* `name`: Helpful for debugging
* `set(...)`: Attach custom behavior with keys

---

## 🧩 Custom Keys

You can extend KEvent with your own keys and behaviors:

```kotlin
object MyKeys {
    val ASYNC = Key("async", false)
}

var EventConfigurationScope<*>.async: Boolean
    get() = getOrDefault(MyKeys.ASYNC)
    set(value) = set(MyKeys.ASYNC, value)
```

Use in configuration:

```kotlin
configuration(event) {
    async = true
}
```

You can now interpret that flag however you need (e.g., offload to another thread).

---

## 🛠 Under the Hood

**Registration Process:**

1. `@Register` methods are found via reflection.
2. Each is called once with `null`.
3. The method throws a special exception to return the config.
4. Configuration is stored.

**Dispatching Process:**

1. All handlers for the event are found.
2. Sorted by their priority.
3. Called with the real event instance.

This system is safe, predictable, and has no hidden magic.

---

## ❓ Why Use KEvent?

* Full Kotlin type safety
* Simple and readable DSL
* Easily extendable
* Designed for performance and clarity
* Zero global state

---

Built by **Fantamomo**
Version **1.0-SNAPSHOT**