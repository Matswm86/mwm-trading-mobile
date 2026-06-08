# MWM Trading Mobile (Android)

Native Kotlin / Jetpack Compose client over the MWM trading platform API
(`https://trading.mwmai.no`). See [PLAN.md](PLAN.md) for architecture and roadmap.

This is its own standalone repo (like the Godot game repos), repo root = the
Gradle project.

## Build the APK

**CI:** every push runs `.github/workflows/build-android.yml`, which builds a
debug APK and uploads the artifact `mwm-trading-mobile-debug-apk`. Trigger
manually via Actions → **build-android** → *Run workflow*. Download the artifact
and sideload (allow install from unknown sources).

**Locally** (a self-contained toolchain is installed on the dev box under
`~/.jdks/temurin-17`, `~/Android/Sdk`, `~/gradle`):

```bash
export JAVA_HOME=~/.jdks/temurin-17
export ANDROID_HOME=~/Android/Sdk
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` (gitignored) points Gradle at the SDK via `sdk.dir`.

## v1 status

Dashboard only: one `GET /api/observability` call renders the account balances
and every cell as a card (PF, net P&L, max DD, win rate, Ironclad status, live
heartbeat health). Pull down to refresh. All reads are public; no login needed.
