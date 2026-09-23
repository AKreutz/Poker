# Poker

An Android app for tracking home poker game sessions: buy-ins, cash-outs, running balances,
and long-term stats/records across a recurring group of players. Data is stored locally and
can optionally be synced across devices via Google Drive.

## Features

- **Sessions** — start a session, add players, log buy-ins and cash-outs, then conclude it.
  Concluding a session shows a summary of results, updated records, and streak changes.
- **Overview** — home screen with the current/most recent session and quick highlights.
- **Stats** — all-time player totals and records: biggest win/loss, longest win/loss streaks
  (including currently active streaks), highest/lowest balance, most sessions played, most
  profitable, and most/least consistent player (by standard deviation of results).
- **Sessions history** — browse past sessions with per-player results.
- **Sync** — sign in with Google to back up and sync players, sessions, and entries to Google
  Drive. Merge strategy is last-write-wins per row (by `updatedAt`); deletes are soft deletes so
  they merge the same way as any other edit.

## Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3) for UI
- **Room** for local persistence (players, sessions, session entries)
- **Coroutines/Flow** for reactive data
- **Navigation Compose** for screen navigation
- **Credential Manager / Google Identity / Google Drive API** for sign-in and cloud sync
- **kotlinx.serialization** for the Drive snapshot format

## Project structure

```
app/src/main/java/com/akreutz/poker/
├── data/
│   ├── local/          Room database, DAOs, entities
│   ├── model/           Derived/computed models (records, totals, session results)
│   ├── repository/      PokerRepository — single entry point over local data
│   └── sync/             SyncManager, Google Drive data source, auth
├── navigation/           App destinations
└── ui/
    ├── home/              Overview screen
    ├── currentsession/    Active session screen
    ├── sessions/          Session history screen
    ├── stats/             Records/stats screen
    └── theme/             Compose theming
```

## Building

Standard Gradle/Android Studio project.

```
./gradlew assembleDebug
```

### Release builds

Release builds (`bundleRelease` / `assembleRelease`) are signed using a keystore referenced from
`local.properties` (git-ignored, never committed). Add the following to your `local.properties`:

```
release.storeFile=/absolute/or/relative/path/to/keystore.jks
release.storePassword=<store password>
release.keyAlias=<key alias>
release.keyPassword=<key password>
```

If `release.storeFile` is not set, release builds are left unsigned — `app/build.gradle.kts`
checks for it before applying the signing config.

To generate a new keystore:

```
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias poker
```

Keep the keystore file and its passwords safe and out of version control. Losing them means the
app can never be updated under the same signing identity on the Play Store.

Signed release artifacts for each shipped version are kept under `releases/vX.Y/`.
