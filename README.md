# Minimal Launcher

A text-only Android home screen: pinned apps, user-defined lists, and per-app
daily time limits.

## Status

Scaffold. Written but **never compiled** — there was no Android SDK on this
machine when it was generated. Expect a first-sync round of small fixes.

## Build

```sh
brew install --cask android-studio   # then run the first-launch SDK wizard
open -a "Android Studio" .
```

Android Studio will regenerate the missing `gradle/wrapper/gradle-wrapper.jar`
on first sync, and will offer to upgrade AGP/Kotlin — accept it.

Then: Run ▸ app, and set it as the home app from in-app **Settings ▸ Set as
default home app**.

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
