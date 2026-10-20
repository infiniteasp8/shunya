# Contributing

Shunya is small on purpose. Before proposing something, read *What this
deliberately does not do* in the README — most feature requests are answered
there, and the answer is usually no. Bugs, on the other hand, are very welcome.

## Running it

- **The prototype** — open `docs/prototype/index.html` in a browser. It is the
  feel test that preceded the app and it mirrors the app's search rules, so
  interaction changes should land there too.
- **The app** — see *Build from source* in the README. You need JDK 17 and the
  Android SDK with platform 35.

## Before opening a pull request

- `./gradlew :app:testReleaseUnitTest :app:assembleRelease` passes.
- The resolver (`app/src/main/kotlin/dev/anantpandey/shunya/data/Resolution.kt`)
  has unit tests. If you change how typing resolves, add the case that made you
  change it.
- One concern per pull request, referencing the issue it closes.
- Match the voice of the code: comments say *why*, nothing shouts, nothing is
  decorated.

## What a change has to respect

- **No icons, no colour except ember, no network, no history.** These are not
  style preferences. They are the product; see the README for why each one.
- **Tier two is never reachable by prefix.** Anything that makes a gated app
  easier to reach by accident is a regression, however convenient it is.
- **Leaving is always easier than entering.** *Not now* stays wide; the way in
  stays small.

## Finding a feed surface id

The feed watcher tells Reels apart from DMs by Android view-id resource names,
which the apps rename when they redesign. To find a current one:

```bash
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml
grep -o 'resource-id="[^"]*"' ui.xml | sort -u
```

with the feed open on the phone. Add it to `SurfaceDetector.kt` with a comment
saying which app version you checked it against.
