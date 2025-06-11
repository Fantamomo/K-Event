# KEvent – A Type-Safe and Flexible Event System for Kotlin

**KEvent** is a lightweight, type-safe event system built for Kotlin. It’s designed to help you decouple parts of your application using clean, simple observer-style logic – with zero compromises on type safety or flexibility.

Whether you're building a game, a plugin framework, or just want to avoid a tangled mess of callbacks, KEvent has your back.

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

All events inherit from a base `Event` class.

```kotlin
class GameStartedEvent(val gameId: String) : Event()
```

### 🔹 Create Listeners

Listeners are just classes that are implementing `Listener` that have methods marked `@Register`. Each method takes exactly one nullable `Event` parameter.
Why nullable? Because KEvent calls it once with `null` during registration to collect configuration.

```kotlin
@Register
fun onStart(event: GameStartedEvent?) {
    configuration(event) {
        priority = Priority.LOW
    }
    
    // ...
}
```

Inside the body, the event is guaranteed to be non-null (thanks to Kotlin contracts).

### 🔹 Dispatch Events

Just call:

```kotlin
manager.dispatch(GameStartedEvent("game-42"))
```

All matching listeners are called, in order of their priority.

---

## ⚙️ Configuration

KEvent lets you configure each handler using a little DSL during registration (when the method is called with `null`).

```kotlin
configuration(event) {
    // Control execution order (HIGHEST, HIGH, NORMAL, LOW, LOWEST, MONITOR)
    priority = Priority.HIGH

    // Or use a custom priority value for fine-grained control
    priority = Priority.Custom(750) // Between HIGH (500) and HIGHEST (1000)

    // Ignore subtypes of this event
    disallowSubtypes = true

    // When set to true, this listener will not be called
    // when it is already handling a event
    // (e.g. to avoid StackOverflow)
    exclusiveListenerProcessing = true

    // Only for debug reason
    name = "My Listener"
    
    set(MyKeys.ASYNC, true)
}
```

> You always have to call `configuration`, if you don’t have to specify settings you can call `emptyConfiguration(event)`.

### Built-in options**:**

* `priority`: Controls call order (`HIGHEST`, `HIGH`, `NORMAL`, etc.)
* `disallowSubtypes`: If true, the handler only fires for the exact class
* `exclusiveListenerProcessing`: If true the listener will only be called for one Event at a time
* `name`: Only for debug reasons
* `set(...)`: Add your own custom behavior via keys

---

## 🧩 Custom Keys

You can define your own config keys to extend KEvent however you want.

```kotlin
// In your library
object MyKeys {
    val ASYNC = Key("async", false)
}

var EventConfigurationScope<*>.async: Boolean
    get() = getOrDefault(MyKeys.ASYNC)
    set(value) = set(MyKeys.ASYNC, value)
```

Use it in config:

```kotlin
configuration(event) {
    async = true
}
```

Now your system can look for that flag and run the handler on another thread, etc.

---

## 🛠 Under the Hood

Here’s what happens when you register a listener:

1. All methods with `@Register` are found via reflection.
2. Each is called once with `null`.
3. A special exception is thrown from your method carrying the config.
4. That config is stored and used when real events are dispatched.

When dispatching:

1. All handlers for the event type are collected.
2. They're sorted by priority.
3. Each is called with the actual event instance.

It's safe, efficient, and lets you write clear event-driven code without magic or mess.

---

## Why KEvent?

* Type-safe from end to end
* Simple, expressive DSL
* Flexible and extendable
* Works great with games, plugins, or tools
* No global state, no singletons

---

Developed by **Fantamomo**

Version **1.0-SNAPSHOT**