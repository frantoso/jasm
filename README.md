# Just another State Machine

## Introduction

This is an easy to use state machine implementation for Kotlin.

There are a lot of variants known to implement state machines.
Most of them merge the code of the state machines behavior together with the functional code.
A better solution is to strictly separate the code for the state machines logic from
the functional code. An existing state machine component is parametrized to define its behavior.

This readme includes the documentation of an implementation of a ready to use FSM (Finite State Machine).
Using this state machine is very simple. Just define the states, the transitions and the state actions.

## Basic steps to create a State Machine

1. Model your state machine inside a graphical editor e.g. UML tool or any other applicable graphic tool.
1. Create an instance of the FSM class in your code.
1. Transfer all your transitions from the graphic to the code.
1. Register the action handlers for your states.
1. Start the state machine.

## How to ...

- [Get it.](#gradle)
- [Implementing a simple Finite State Machine.](#how-to-create-a-simple-state-machine)

### Gradle

The library jasm is distributed via MavenCentral.

**build.gradle.kts**

```kotlin
repositories {
  mavenCentral()
}

dependencies {
  implementation("io.github.frantoso:jasm:<version>")
}
```

## How to: Create a simple State Machine

This topic shows how to implement a simple Finite State Machine using the StateMachine component.
The example shows the modelling of a single traffic light.                    

### Start with the model of the state machine

![Simple state machine](images/traffic_light_simple.png)  
*A simple traffic light with four states, starting with showing the red light.*

### Create the state machine and the states

```kotlin
// create the state machine
val fsm = FsmSync<Int>("simple traffic light")

// create the states...
val showingRed = State<Int>("ShowingRed")
val showingRedYellow = State<Int>("ShowingRedYellow")
val showingYellow = State<Int>("ShowingYellow")
val showingGreen = State<Int>("ShowingGreen")

// define the transitions...
fsm.initialTransition(showingRed)
showingRed
  .entry { println("x--  $it") }
  .transition(Tick, showingRedYellow)
showingRedYellow
  .entry { println("xx-  $it") }
  .transition(Tick, showingGreen)
showingGreen
  .entry { println("--x  $it") }
  .transition(Tick, showingYellow)
showingYellow
  .entry { println("-x-  $it") }
  .transition(Tick, showingRed)

// start the state machine
fsm.start(1)

assertThat(fsm.isRunning).isTrue

// trigger an event
fsm.trigger(Tick, 1)

assertThat(fsm.currentState).isEqualTo(showingRedYellow)
```

## The classes

### FsmSync<T>

A synchronous (blocking) state machine. The call to trigger is blocking. 

The type parameter defines the type of data to send to the machine when it is triggered or started.

```kotlin
data class MyFsmData(
  val x: Int,
  val y: String,
)

val fsm = FsmSync<MyFsmData>("MyFsm")

// add states and transitions

fsm.start(MyFsmData(42, "test"))
```

### FsmAsync<T>

An asynchronous (non-blocking) state machine. The call to trigger is non-blocking. The events are 
queued and triggered sequentially.

The type parameter defines the type of data to send to the machine when it is triggered or started.

```kotlin
data class MyFsmData(
  val x: Int,
  val y: String,
)

val fsm = FsmAsync<MyFsmData>("MyFsm")

// add states and transitions

fsm.start(MyFsmData(1, "2"))
```

