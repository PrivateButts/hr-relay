# HR Relay

Wear OS app that reads live heart rate data and relays it to a
configurable HTTP server on the local network.

## Features

- Real-time heart rate monitoring via Health Services API
- Foreground service with wakelock — relays continuously
- Scrollable Wear OS UI with Start/Stop toggle and live BPM
- Configurable server URL via ADB deep link
- Automatically handles API-level permissions
  (`BODY_SENSORS` for API ≤35,
  `health.READ_HEART_RATE` for API 36+)

## Server Protocol

See [SERVER-PROTOCOL.md](SERVER-PROTOCOL.md) for the HTTP contract.

## Building

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Setting the Server URL

After deployment, configure the relay target:

```bash
adb shell am start \
  -a dev.privatebutts.hrrelay.SET_URL \
  -e url "http://192.168.1.100:8080/hr" \
  dev.privatebutts.hrrelay
```

Then tap **Start** in the app.

## Permissions

The app requests the following at runtime:
- Body sensors / heart rate read access
- Notifications (for the foreground service)

## License

MIT
