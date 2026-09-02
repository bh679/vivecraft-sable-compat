# Vivecraft Sable Compat

[![CI](https://github.com/bh679/vivecraft-sable-compat/actions/workflows/ci.yml/badge.svg)](https://github.com/bh679/vivecraft-sable-compat/actions/workflows/ci.yml)

> ⚠️ **Beta — not yet tested in a headset.** Both fixes are verified against real
> Vivecraft and Sable bytecode and covered by unit tests, but nobody has yet teleported
> in VR on a moving Sable structure. Every 0.x release publishes as **beta** on Modrinth
> and CurseForge for exactly this reason. Please
> [report anything that misbehaves](https://github.com/bh679/vivecraft-sable-compat/issues).

Makes [Vivecraft](https://modrinth.com/mod/vivecraft) VR work properly while you are standing
on a [Sable](https://modrinth.com/mod/sable) sub-level — a moving, physics-driven structure
such as a ship or a train carriage.

NeoForge 1.21.1. **Client-side only** — it works on unmodified servers.

## What it fixes

| Symptom | Cause |
|---|---|
| **VR teleport drops you into the void.** You aim at a spot on the moving structure, teleport, and fall through empty space far away from it. | Vivecraft aims with a block raycast, so on a sub-level it picks a destination in the far-off region where Sable actually *stores* the structure's blocks — then writes that position to you directly. |
| **VR melee hits nothing.** Swinging at a mob standing next to you on the structure does nothing, and the log fills with errors. | Vivecraft's swing search box is built from two different coordinate frames at once, so it spans ~20 million blocks. Sable refuses any query that large and returns no targets at all. |

Both are the same underlying problem: Sable keeps a sub-level's blocks somewhere far from
where it draws them, and Vivecraft's VR code predates Sable and does not know that.

## How it works

Sable already defends the *vanilla* teleport path — it runs destinations through
`SableCompanion.projectOutOfSubLevel`, which maps a position inside a sub-level's stored
region back out to where that structure really is, and leaves any other position untouched.
Vivecraft doesn't use the vanilla path, so it never reaches that guard. This mod applies the
same guard at Vivecraft's own call, and rebuilds the oversized melee query box.

Fixing the client is enough for multiplayer: Vivecraft tells the server where it teleported
*after* moving locally, so correcting the local move corrects what the server is told.

**No Sable internals are touched.** Every mixin targets a Vivecraft class, and the only Sable
code called is one public API method — which is why Sable is declared as a minimum version
rather than pinned exactly, unlike the sibling Sable mods.

## Requirements

- Minecraft 1.21.1 · NeoForge 21.1.228+
- Vivecraft (verified against `1.21.1-1.3.15-neoforge`)
- Sable 2.0.5+

Vivecraft and Sable are **not** bundled — install them yourself. (Sable's PolyForm Shield
licence does not permit redistribution.)

## Development

```bash
./gradlew build   # compile + unit tests
./gradlew test    # unit tests only
./gradlew runClient
```

Vivecraft is not a compile dependency — mixins target it by string name — so a Vivecraft
rename would not break the build, only the game. Guard against that before releasing:

```bash
scripts/verify-mixin-targets.sh
```

It disassembles the pinned Vivecraft jar and asserts every wrapped call still exists in the
method being injected into. It needs a JDK on `PATH` for `javap` (Gradle's own toolchain is
not enough) and network access to Modrinth to fetch the pinned build.

> ⚠️ The one thing unit tests cannot cover is whether the teleport actually lands correctly
> in VR. That needs a headset, Sable and a moving structure.

### Getting it tested

Because the bug only exists in a headset on a moving structure, the mod logs its own diagnosis:
whether each mixin applied, and for every teleport, the destination Vivecraft chose versus the one
that was actually used. A tester therefore only has to **send `latest.log`** — see
**[TESTING.md](TESTING.md)**, which is written to be handed straight to them, and also explains how
to read the result.

Diagnostics ship enabled. They cost a counter on the melee path (which runs many times a second,
so it is aggregated, never logged per swing) and a rate-limited line per teleport; the log-volume
limits are enforced by unit tests rather than left as a promise.
### CI

[`ci.yml`](.github/workflows/ci.yml) runs on every push and PR, as two parallel jobs so a
failure is attributable at a glance:

| Job | What it catches |
|---|---|
| `build` | Compile errors and unit-test regressions (`./gradlew build`). |
| `mixins` | An upstream Vivecraft rename, via `scripts/verify-mixin-targets.sh`. |

The `mixins` job is the one that matters most. Vivecraft is not a compile dependency, so a
rename does not fail `build` — it fails at class load, in a headset, where nobody is
watching. A red `mixins` leg means Vivecraft changed; a red `build` leg means we did.

## Releasing

Dispatch-only — the workflow is the source of tags, never a consumer of them:

```bash
gh workflow run release.yml -f tag=v0.2.1
```

The tag must match `mod_version` in `gradle.properties`, and must not already exist
(pass `-f republish=true` to recover a partial publish). Before building, the workflow
re-runs the mixin-target verification as a **blocking gate** — no release ships with a
mixin that no longer matches the pinned Vivecraft build.

Every `0.x` tag publishes as **beta** and prepends the headset-testing notice above to
the platform changelog automatically. That stops at `v1.0.0` — so do not cut a 1.0 until
the fixes have actually been flown in VR.

Publishing is credential-gated and degrades safely: without the secrets below the GitHub
Release still happens and the platform upload is skipped with a warning.

| Setting | Kind | Purpose |
|---|---|---|
| `MODRINTH_TOKEN` | secret | Modrinth API token (scope: create versions) |
| `CURSEFORGE_TOKEN` | secret | CurseForge API token |
| `MODRINTH_PROJECT_ID` | variable | Modrinth project id/slug |
| `CURSEFORGE_PROJECT_ID` | variable | CurseForge numeric project id |

### Downstream: the Dungeon Train modpack

Dungeon Train bundles this mod, so a release here has a follow-up there — see
[`docs/dungeon-train-modpack.md`](docs/dungeon-train-modpack.md) for the exact entry and
the reason it must ship **opt-in**.

## History

The melee fix originally shipped inside
[dungeon-train-mc](https://github.com/bh679/dungeon-train-mc) and was moved here — neither bug
is Dungeon Train specific. **Dungeon Train must not also apply it**, or both copies would wrap
the same call.

## Licence

PolyForm Shield 1.0.0 — matching the other mods in this family. See [LICENSE](LICENSE).
