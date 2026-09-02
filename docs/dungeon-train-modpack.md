# Adding this mod to the Dungeon Train modpack

Dungeon Train's modpack config lives in
[`modpack/modpack.config.json`](https://github.com/bh679/dungeon-train-mc/blob/main/modpack/modpack.config.json).
This page records the entry to add, why it must ship **opt-in**, and why it cannot be
added before this mod is published.

## Why this is urgent

Dungeon Train `0.773.0` removed its own copy of the Vivecraft VR melee fix
([bh679/dungeon-train-mc#1261](https://github.com/bh679/dungeon-train-mc/pull/1261)) —
correctly, because neither bug is Dungeon Train specific and two copies would wrap the same
call. Until this mod is on the platforms and in the pack, **any VR player who updates
Dungeon Train loses working melee.** That regression window is open now.

## It must ship OFF by default (`required: false`)

This is not a preference — it is forced by
[`neoforge.mods.toml`](../src/main/templates/META-INF/neoforge.mods.toml), which declares
`vivecraft` a **required** dependency:

```toml
[[dependencies.vivecraft_sable_compat]]
    modId="vivecraft"
    type="required"
```

NeoForge refuses to start when a required dependency is absent. Almost nobody playing
Dungeon Train has Vivecraft installed, so a mod shipped **enabled** would hard-fail the
game at load for every non-VR player in the pack.

Mind the documented CurseForge inversion — it points the same way here, but for a
different reason than usual:

> ⚠️ In the CurseForge app `required:false` ships a mod **OFF** (opt-in), not on.
> — `dungeon-train-mc/CLAUDE.md`

So `required: false` is what we want on both platforms: CurseForge installs the jar
disabled, and `build-mrpack.py`'s `compute_env` maps it to Modrinth env
`client: optional` (a launcher toggle). Either way the jar is present but inert until a
VR player turns it on — at which point they have Vivecraft, because that is the only
reason to turn it on.

The mod is client-side only, so its Modrinth project must be published with
`server_side: unsupported`; `compute_env` then correctly skips it on dedicated servers.

## The entry

Add to `optional_mods` in `modpack/modpack.config.json`. **Every field is required** —
CI (`modpack-checks` in `build.yml`) fails on a missing `slug` (`check-relations.py`) or a
missing Modrinth pin (`build-mrpack.py --check-config`):

```json
{ "name": "Vivecraft Sable Compat", "slug": "vivecraft-sable-compat", "project_id": <CF_PROJECT_ID>, "file_id": <CF_FILE_ID>, "required": false, "modrinth_project": "vivecraft-sable-compat", "modrinth_version": "<MODRINTH_VERSION_ID>" }
```

And the paired relation in `.github/workflows/release.yml` under
`curseforge-dependencies` — `check-relations.py` fails CI without it:

```yaml
            vivecraft-sable-compat(optional)
```

Note `check-relations.py` is one-directional: an Include *requires* a relation, but a
relation does not require an Include. Both edits should still land in **one PR** — a
relation naming a CurseForge project that does not exist yet is a warning waiting to
happen on the next Dungeon Train release.

## Blocked until this mod is published

Three of the values above only exist once the platforms have accepted an upload:

| Value | Comes from |
|---|---|
| `project_id` | CurseForge project (numeric id, shown on the project page) |
| `file_id` | The CurseForge file id of the uploaded jar |
| `modrinth_version` | The Modrinth version id of the uploaded jar |

The release workflow prints all of these to the run's job summary — see
[`release.yml`](../.github/workflows/release.yml), "Publish summary". Placeholders must not
be committed: they pass `check-relations.py` but produce a `.mrpack` pointing at a file
that does not exist.

## Order of operations

1. Create the Modrinth + CurseForge projects (slug `vivecraft-sable-compat`,
   client-side only, NeoForge 1.21.1). Set `server_side: unsupported` on Modrinth.
2. Add `MODRINTH_TOKEN` / `CURSEFORGE_TOKEN` secrets and
   `MODRINTH_PROJECT_ID` / `CURSEFORGE_PROJECT_ID` variables to this repo.
3. `gh workflow run release.yml -f tag=v0.2.1` — publishes as **beta** (see the README's
   Releasing section) and prints the three ids.
4. Open one Dungeon Train PR with both edits above, filled in with those ids.
5. Ideally: get a VR test in before promoting to `v1.0.0`. See "VR testing" below.

## VR testing

Nothing in CI can confirm that a teleport actually lands on a moving sub-level — that
needs SteamVR, which macOS cannot run. The check is small once hardware is available:

1. Windows or Linux, SteamVR + a headset, NeoForge 1.21.1 with Vivecraft, Sable and
   this mod.
2. Stand on a moving Sable structure (a Dungeon Train carriage).
3. **Teleport** to a spot on the structure — you should land where you aimed, not fall
   into the void.
4. **Melee** a mob standing next to you — it should take the hit, with no errors in the
   log about an oversized query box.

Until that has happened, the mod stays `0.x` / beta and the listing says so.
