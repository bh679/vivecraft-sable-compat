# VR test — please read, it takes about 5 minutes

Thanks for testing. **The only thing you need to send back is one file: `latest.log`.**
You don't need to describe what happened or take notes — the log records it.

## 1. Install

Into the same Minecraft profile (1.21.1, NeoForge):

- Dungeon Train
- Sable
- Vivecraft
- **Vivecraft Sable Compat** (this mod — the `.jar` you were sent)

## 2. Play

Put the headset on, start a run, and get on a moving train. Then:

1. **Teleport around the train.** Onto the carriage floor, between carriages, up and down.
   This is the main thing being tested.
2. **Teleport off the train** onto normal ground, and teleport around away from the train.
3. **Hit a mob** standing on a moving carriage — a couple of swings is enough.

If anything goes wrong — you end up falling, land somewhere strange, swings don't connect —
**keep playing for a moment and then quit normally.** A failure is a useful result and the log
captures it. There is no need to reproduce it repeatedly.

## 3. Quit properly

**Leave the world and close Minecraft normally** (not Alt-F4, not force-quit). The log writes a
summary as you leave the world, and that summary is the most useful part.

## 4. Send the log

Send `latest.log` from:

| OS | Path |
|---|---|
| Windows | `%appdata%\.minecraft\logs\latest.log` |
| macOS | `~/Library/Application Support/minecraft/logs/latest.log` |
| Linux | `~/.minecraft/logs/latest.log` |

Using a custom launcher (CurseForge, Prism, MultiMC, Modrinth)? It's in that **instance's own**
folder: `<instance>/logs/latest.log`.

> Grab it before launching the game again — the next launch renames `latest.log` and starts a new
> one. If you already relaunched, the previous run is the newest `.gz` file in the same folder;
> send that instead.

That's it. No screenshots or write-up needed, though if something looked wrong, one sentence about
what you saw is always welcome.

---

## What the log will say (for whoever reads it)

Everything this mod prints is tagged `[VSC]`:

```
grep VSC latest.log
```

**First, did the fix load?** There should be a line per mixin:

```
[VSC] mixin applied: TeleportTrackerSubLevelMixin -> org.vivecraft.client_vr.gameplay.trackers.TeleportTracker
[VSC] mixin applied: SwingTrackerSubLevelAabbMixin -> org.vivecraft.client_vr.gameplay.trackers.SwingTracker
```

If either is missing, that fix was **not active** and nothing else in the log means anything —
most likely Vivecraft changed the method being hooked. Re-run
`scripts/verify-mixin-targets.sh` against that Vivecraft build.

**Then, did the bug happen and did we catch it?**

```
[VSC] teleport #3 ON-SUBLEVEL(corrected) correction=20481234.50 raw=(...) used=(...) landed=(...)
[VSC] teleport #4 off-sublevel(unchanged) correction=0.00 raw=(...) used=(...) landed=(...)
```

- `ON-SUBLEVEL(corrected)` with a **huge** `correction` is the bug being caught — this is the fix
  working, and it confirms the diagnosis.
- `off-sublevel(unchanged)` is expected for teleports on normal ground. Untouched behaviour.
- **`off-sublevel(unchanged)` while the tester says they were on the train is the important
  negative result**: the destination was never in a sub-level region, so the root-cause theory
  behind this mod is wrong and the real cause is elsewhere.
- `LANDED OFF TARGET` (a warning) means the correction was computed but something moved the player
  afterwards — the fix is not holding.

**Melee** is summarised, never logged per swing:

```
[VSC] melee: rebuilt 47 oversized swing box(es) in the last 30s (total 312)
```

**The summary on leaving the world** is the fastest read:

```
[VSC] summary: teleports=12 (on-sublevel/corrected=9, off-sublevel=3) meleeClamps=312
```

`teleports=0` means the tester never teleported (or VR wasn't active) — not that the fix failed.
