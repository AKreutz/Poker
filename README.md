# Poker

## Release signing

Release builds (`bundleRelease`/`assembleRelease`) are signed using a keystore referenced from
`local.properties` (which is git-ignored and never committed). Add the following properties to
your `local.properties`:

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
