# The Debug Adapter

The debug-adapter connects the state machine with the visualizer. When running it shows the current state(s) in real-time.
To make it run you have to connect the debug-adapter with the state machine, and you have to run the jasm-debugger docker image.
The history-example shows how to use the debug-adapter.

## Using the Debug Adapter

Your jasm project needs a dependency to the `jasm-debug-adapter`.
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.frantoso:jasm-debug-adapter:<version>")
}
```

Connect the debug-adapter to your state machine like this:
```
DebugAdapter.of(<your fsm>)
```

From the example project, it would look like this:
```kotlin
val controller = Controller()
DebugAdapter.of(controller.mainFsm.machine)
controller.run()
```

When you run your project now, the debug-adapter will try to connect to a jasm-debugger instance running on `localhost:4000`.

## Running the jasm-debugger

You can run the jasm-debugger using Docker. Just pull the image and run it:
```bash
docker pull frantoso/jasm-debugger
docker run -p 4000:4000 -p 5076:5076 frantoso/jasm-debugger
```

After that, you can open your browser and go to `http://localhost:5076` to see the visualizer.

## Configuration

If the default host and port do not fit your needs, you can configure them. Just change the `docker run` port mappings and configure the debug-adapter accordingly.  
You can configure the debug-adapter using environment variables:
- `JASM_DEBUGGER_HOST`: The host where the jasm-debugger is running. Default is `localhost`.
- `JASM_DEBUGGER_PORT`: The port where the jasm-debugger is running. Default is `4000`.

Also, you can configure the debug-adapter via a config file `debug-settings.json` which is in the applications' directory:
```json
{
    "JasmDebug": {
        "TcpSettings": {
            "Host": "9.8.7.6",
            "Port": 1441
        }
    }
}
```
The config file settings override the environment variables.
