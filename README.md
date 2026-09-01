# Minimal Launcher

A text-only Android home screen: pinned apps, user-defined lists, and per-app
daily time limits.

## Status

Compiles and packages. **Not yet run on a device** — no behaviour has been
verified beyond the compiler accepting it.

## Build

Requires a JDK (17+) and the Android SDK with platform 35 and build-tools 35.
Without Android Studio:

```sh
brew install --cask android-commandlinetools
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Point the project at the SDK in `local.properties` (not committed):

```properties
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

Then:

```sh
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew installDebug
```

Set it as the home app from in-app **Settings ▸ Set as default home app**, and
grant usage access (needed for daily limits):

```sh
adb shell appops set com.gabriel.minimal GET_USAGE_STATS allow
```

### If it crashes while it is your default home

The phone is not bricked — reboot to safe mode, or:

```sh
adb shell cmd package set-home-activity com.google.android.apps.nexuslauncher/.NexusLauncherActivity
```

(component varies by OEM; Samsung is
`com.sec.android.app.launcher/.activities.LauncherActivity`)

## The three features

| Ask | Where |
|---|---|
| Apps organised per list | `data/LauncherConfig.kt` (`AppList`), `ui/DrawerScreen.kt` |
| Editable home screen | `LauncherViewModel.toggleHome` / `moveHomeApp`, `ui/HomeScreen.kt` |
| Daily app time limits | `data/UsageTracker.kt`, `ui/LimitReachedDialog.kt` |

## How limits work, and what they do not do

`UsageTracker` reconstructs today's foreground time per package from
`UsageStatsManager.queryEvents` — no polling, no background service, no battery
cost. It needs **Usage access**, granted in Settings ▸ Special app access.

Enforcement happens at the launch site: `LauncherViewModel.requestLaunch` checks
the limit and shows a friction dialog instead of launching.

This deliberately only covers launches **from this launcher**. Opening an app
from a notification, from Recents, or from another app's link is not
intercepted. Doing that needs either an `AccessibilityService` (rejected by Play
for wellbeing apps) or a `SYSTEM_ALERT_WINDOW` overlay driven by a polling loop
(battery cost, ~200-500 ms of visible feed before the overlay lands).

The launch-site-only approach is the right v1: zero policy risk, zero battery
cost, and for a minimalist launcher it catches most launches anyway.

## Not implemented yet

- Reordering pinned apps by drag (`moveHomeApp` exists, no UI calls it)
- Swipe-up gesture to open the drawer (there is a button)
- Widgets (`AppWidgetHost`), notification badges
- Per-list limits, schedules, "focus mode"
