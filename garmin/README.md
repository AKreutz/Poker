# Poker watch app (Garmin Forerunner 745)

A remote for the hands-won counters of the open session. The phone owns the counts; the watch shows them and sends
taps. The message format is documented on `WatchProtocol` in the Android app (`data/watch/WatchProtocol.kt`).

| Button | Action |
| --- | --- |
| **UP** / **DOWN** | Select a player |
| **START** | +1 hand won for the selected player |
| **Hold UP** | -1 hand won for the selected player |
| **BACK** | Leave the app |

The count changes when the phone confirms the tap, and the watch buzzes briefly then.

## Build

Needs the Connect IQ SDK (9.2 or newer, with the Forerunner 745 device files), a developer key and Java 11 or newer:

```
cd garmin
monkeyc -d fr745 -f monkey.jungle -o bin/PokerWatch.prg -y <path to developer_key.der> -w -l 2
```

Copy `bin/PokerWatch.prg` into `GARMIN/Apps` on the watch over USB. Keep the developer key out of the repository.

## Connect it to the Android app

Add the app id from `manifest.xml` to the Android project's `local.properties`:

```
garmin.appId=398692007d204cf3b21914fecf0679ae
```

Without it the Android app never starts the watch connection. For the Connect IQ simulator, also set
`garmin.useSimulator=true` and run `adb forward tcp:7381 tcp:7381`.

Open **Poker** on the watch after starting a session on the phone. The watch asks for the state every 5 seconds
until the phone answers.
