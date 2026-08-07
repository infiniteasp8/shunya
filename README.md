# Shunya

*शून्य — zero, void, emptiness.*

An Android launcher for people who pick up the phone without having decided to.

---

## The problem

Nobody decides to watch forty reels. The sequence is: unlock the phone for a
reason, see a grid of coloured icons, and arrive somewhere twenty-five minutes
later with no memory of choosing to go there. The interval between the reach and
the decision is where the entire problem lives, and every mainstream home screen
is optimised to make that interval as short as possible.

Shunya makes it longer.

## The mechanism

**Recognition is automatic. Recall is not.**

Seeing a logo is pre-conscious — you have identified Instagram before you know
you are looking at it. Typing `instagram` cannot happen without you deciding to
type it. That asymmetry is the whole design, and everything below follows from
it.

The home screen has no icons. It has a clock, a date, and at most six lowercase
words sitting low in the thumb zone, on true black. Everything else is invisible
and reached by typing.

### Three tiers

| Tier | Cost to reach | For |
|---|---|---|
| **0** | Visible, one tap | Things the world demands: phone, camera, maps |
| **1** | Type 3+ characters, prefix match, enter | Everything ordinary |
| **2** | Type the name in full, **exactly**, then a 10s hold | The handful you open without deciding to |

Tier 2 has no prefix matching and no autocomplete. Typing eight of the nine
letters of `instagram` shows **nothing at all** — not "no results", not a greyed
suggestion. A near-miss that displays *something* still pays out, and a partial
payout keeps the loop alive.

Uniform friction does not work. A launcher that makes everything hard gets
uninstalled inside a fortnight, because sometimes you need the camera in two
seconds. Friction only changes behaviour when it is spent precisely.

### The gate

Reaching a tier-2 app costs three things, in this order:

1. **You see today first.** Opens and minutes, read live from Android, *before*
   you are asked to decide.
2. **You say what you came for.** Not stored, not scored. An intention you
   cannot finish typing is usually not one you had.
3. **You hold for ten seconds.** Release early and it resets to zero.

*Not now* is a wide, easy target. The way in is a small circle you must keep a
thumb on. Any gate that makes leaving as hard as entering trains people to push
through.

### The feed watcher

A launcher alone cannot work, and this is the part most minimal launchers miss.

The home screen only governs `unlock → home → tap`. That is very likely not how
anyone ends up in Reels at midnight — the real doors are a notification, a
shared link, and the recents switcher, and a launcher never sees any of them.

So an `AccessibilityService` watches for the **feed surface specifically**, not
the app:

- Instagram DMs work normally. Reels is gated.
- YouTube search works normally. Shorts is gated.

This is what makes the tool survivable. Block whole apps and you have not made
someone focused, you have just given them a reason to remove the launcher.

It intervenes twice, and the second one is the one that matters:

- **At the door** — arriving on the feed without a grace window throws the gate.
- **Mid-scroll** — after ~20 items the gate returns, even though you were let in
  legitimately. The damage is not done in minute one, when you still had a
  reason. It is done in minute twenty-five.

The service is scoped in both `accessibility_service_config.xml` and at runtime
to the short-video packages only. Android never delivers it an event from a
banking app, a password manager, or a keyboard — it is structurally incapable of
seeing them, which is a stronger guarantee than a policy promising not to look.

## What this deliberately does not do

- **No streaks, no badges, no "great job today".** That is a second engagement
  loop built to fix the first one. This app's job is to be boring.
- **No usage history.** Counts are read live from Android and never copied to
  disk. Uninstalling takes the record with it. A wellbeing app that accumulates
  a behavioural profile has quietly become the thing it was built to oppose.
- **No network permission.** Nothing leaves the phone. There is nowhere for it
  to go.
- **No colour**, except one burnt orange spent entirely on friction.

## Build

Requires JDK 17 and the Android SDK (platform 35).

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then on the phone:

1. **Settings → Apps → Default apps → Home app → Shunya**
2. Long-press the hairline at the bottom of the home screen to reach settings.
3. Grant **usage access** so the gate can show real numbers.
4. Enable the **feed watcher** under Accessibility.

Steps 3 and 4 are prompted in-app and are optional — the launcher works without
them, just blind.

## Status

Working, and in daily use on a Motorola Edge 40. Early — the interaction model
is settled, the details are not.

Known next:

- Reorder tier-0 apps rather than accepting seeded order
- Tune hold duration and feed threshold from settings
- A cooldown on disabling the watcher, so the decision to switch it off is made
  by the same person who set it up rather than by whoever is holding the phone
  at 11pm
- Feed surface IDs are other people's internals and will drift when they
  redesign; they need a maintenance path

## A note on publishing

Google restricts `AccessibilityService` heavily and digital-wellbeing apps have
been pulled for it before. The launcher itself carries no policy risk; the
watcher requires a declaration and a prominent-disclosure flow. The architecture
keeps them separable on purpose.

## Prototype

`docs/prototype/` holds the interaction prototype that preceded the app — a
single self-contained HTML file used to test whether ten seconds of holding
feels like a speed bump or a punishment before any Kotlin was written.

## Licence

MIT
