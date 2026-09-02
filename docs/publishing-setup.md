# Publishing setup — Modrinth + CurseForge

One-time setup to get this mod onto the platforms, with every value you need to paste.
After this, releasing is one command (see the README's *Releasing* section).

Everything here is done by a human: creating projects and minting API tokens are account
actions. Once the four repo settings in **Step 4** exist, the release workflow does the
rest on every dispatch.

**Why the hurry:** Dungeon Train `0.773.0` removed its own copy of the Vivecraft VR melee
fix ([dungeon-train-mc#1261](https://github.com/bh679/dungeon-train-mc/pull/1261)). Until
this mod is published, any VR player who updates Dungeon Train loses working melee.

---

## Order matters

Modrinth will not let you submit a project for review until it has a version, and the
release workflow is what uploads that version. So the project is created as a **draft**,
its id is wired into this repo, the release runs, and only then do you submit for review.

```
1. Create Modrinth project (draft)   ──┐
2. Create CurseForge project           ├─→ 4. Wire ids + tokens into GitHub
3. Mint both API tokens              ──┘        │
                                                ▼
                                   5. gh workflow run release.yml
                                                │
                        ┌───────────────────────┼───────────────────────┐
                        ▼                       ▼                       ▼
              6. Submit Modrinth      7. CurseForge file        8. DT modpack PR
                 for review              approval                  (needs the ids)
```

---

## Shared values

Both listings use the same identity. **The slug must be exactly `vivecraft-sable-compat`
on both platforms** — Dungeon Train's `check-relations.py` matches the modpack entry to
the mod relation by that one string.

| Field | Value |
|---|---|
| Name | `Vivecraft Sable Compat` |
| Slug / URL | `vivecraft-sable-compat` |
| Summary | `Makes Vivecraft VR work while standing on a Sable sub-level. VR teleport lands where you aimed instead of dropping you into the void, and VR melee hits the mobs standing next to you.` |
| Minecraft | `1.21.1` |
| Loader | NeoForge only |
| Environment | **Client required, server unsupported** |
| Licence | PolyForm Shield 1.0.0 |
| Licence URL | `https://github.com/bh679/vivecraft-sable-compat/blob/main/LICENSE` |
| Source | `https://github.com/bh679/vivecraft-sable-compat` |
| Issues | `https://github.com/bh679/vivecraft-sable-compat/issues` |

The summary is `mod_description` from `gradle.properties`, 181 characters — inside
Modrinth's 256 limit.

### Why client/server matters more than it looks

This is not cosmetic metadata. Dungeon Train's `build-mrpack.py` reads it:

```
required:false + server_side:unsupported  →  {'client': 'optional', 'server': 'unsupported'}
```

That is exactly what the pack needs — a client-side launcher toggle, skipped on dedicated
servers. Get `server_side` wrong and the pack ships a VR mod to every server.

---

## Step 1 — Modrinth project

Go to **[modrinth.com/dashboard/projects](https://modrinth.com/dashboard/projects)** →
*Create a project*. It starts as a **draft**; that is correct, leave it there for now.

| Setting | Value |
|---|---|
| Name | `Vivecraft Sable Compat` |
| URL / slug | `vivecraft-sable-compat` |
| Summary | (from the table above) |
| Client side | **Required** |
| Server side | **Unsupported** |
| Category | `Utility` |
| Licence | Custom → SPDX id `LicenseRef-PolyForm-Shield-License-1.0.0`, URL as above |

That licence id is not invented — it is exactly what `sable`,
`interactive-player-mobs` and `ender-chest-persistence` already carry on Modrinth, so this
mod matches the family.

Modrinth has no VR category, and `Utility` is the honest fit for a compat fix. Discovery
here comes from the word *Vivecraft* in the title and summary, not from the category —
VR players search the mod name.

Leave **Dependencies** alone. The release workflow declares `vivecraft(required)` and
`sable(required)` per version, which is where Modrinth actually stores them.

Then paste the body from [Listing description](#listing-description) below, and grab the
project id: project page → *Settings*, or the `⋮` menu → *Copy ID* (8 characters, like
`AANobbMI`).

> The slug also works as `MODRINTH_PROJECT_ID`, but prefer the id — it survives a rename.

## Step 2 — CurseForge project

Go to **[authors.curseforge.com](https://authors.curseforge.com)** → *Projects* →
*Create Project*.

| Setting | Value |
|---|---|
| Game | Minecraft |
| Project type | Mods |
| Name | `Vivecraft Sable Compat` |
| URL / slug | `vivecraft-sable-compat` |
| Summary | (from the table above) |
| Categories | `Miscellaneous` (+ `Utility & QoL` if offered) |
| Licence | Custom → PolyForm Shield 1.0.0, pointing at the LICENSE URL |

CurseForge has no VR category either; the category is not load-bearing, so pick the
closest and move on.

The project id is the number in the URL of the project's overview page. That is the value
Dungeon Train's modpack config calls `project_id`.

> **CurseForge moderates.** New projects and uploaded files are reviewed by staff before
> they go live — usually hours, sometimes longer. Plan for it: the Modrinth release will
> be live well before the CurseForge one, and Dungeon Train's `release-modpack.yml` waits
> on CurseForge file approval by design.

## Step 3 — API tokens

**Modrinth** — [modrinth.com/settings/pats](https://modrinth.com/settings/pats) →
*Create a PAT*. Name it `vivecraft-sable-compat CI`. Scopes:

- ✅ **Create versions** — the one the upload actually requires (`VERSION_CREATE`)
- ✅ **Read versions**
- ✅ **Write versions**

Nothing else. No project-write, no user scopes. Set an expiry you will actually notice,
and note that a release will fail with a `401` when it lapses.

**CurseForge** — [authors-old.curseforge.com/account/api-tokens](https://authors-old.curseforge.com/account/api-tokens)
(also reachable from the author portal: *Settings → My API Tokens*). Generate one and give
it a descriptive name. CurseForge tokens are not scoped.

Copy both somewhere temporary. You paste them once, in the next step, and never again.

## Step 4 — Wire into this repo

Two secrets and two variables. Run these from anywhere:

```bash
gh secret set MODRINTH_TOKEN -R bh679/vivecraft-sable-compat
```

```bash
gh secret set CURSEFORGE_TOKEN -R bh679/vivecraft-sable-compat
```

Both prompt for the value — paste at the prompt. Do **not** pass `--body "token"`: that
puts the token in your shell history.

```bash
gh variable set MODRINTH_PROJECT_ID -R bh679/vivecraft-sable-compat --body "<8-char id from Step 1>"
```

```bash
gh variable set CURSEFORGE_PROJECT_ID -R bh679/vivecraft-sable-compat --body "<numeric id from Step 2>"
```

Ids are variables, not secrets — they are public information and appear in the run log.

Check all four landed:

```bash
gh secret list -R bh679/vivecraft-sable-compat && gh variable list -R bh679/vivecraft-sable-compat
```

The workflow gates on each platform having **both** its token and its id. Miss one and
that platform is skipped with a `::warning::` rather than failing the release — so a
missing secret looks like a successful run that published nothing. Read the job summary.

## Step 5 — Release

```bash
gh workflow run release.yml -f tag=v0.2.2 -R bh679/vivecraft-sable-compat
```

The tag must equal `mod_version` in `gradle.properties` (`0.2.2` today) and must not
already exist. What happens:

1. Verifies every mixin target against the pinned Vivecraft jar — **blocking**
2. Builds and runs the tests
3. Creates the GitHub Release, marked pre-release
4. Publishes to both platforms as **beta**, with the "not yet tested in a headset" banner
   prepended automatically
5. Prints the three ids the Dungeon Train modpack needs

Read them from the run summary:

```bash
gh run view --repo bh679/vivecraft-sable-compat --log 2>/dev/null | tail -40
```

or just open the run's summary page. You want:

| Value | Used as |
|---|---|
| Modrinth version id | `modrinth_version` in `modpack.config.json` |
| CurseForge file id | `file_id` in `modpack.config.json` |
| CurseForge project id (from Step 2) | `project_id` in `modpack.config.json` |

## Step 6 — Submit Modrinth for review

Now that the project has a version, the draft can go public: project page → *Submit for
review*. Modrinth's queue is usually quick. Nothing else depends on this except players
being able to find it.

## Step 7 — Dungeon Train modpack

With those three ids, the modpack entry goes in — see
[dungeon-train-modpack.md](dungeon-train-modpack.md) for the exact JSON and the paired
`release.yml` relation, and for why it must ship `required: false`.

## Step 8 — The VR test

Still outstanding, and the only thing here no automation can cover. See the end of
[dungeon-train-modpack.md](dungeon-train-modpack.md). Until someone has flown it in a
headset, the mod stays `0.x` / beta.

---

## Listing description

Paste into both listings' description body. CurseForge's editor accepts Markdown via its
source view; if it fights you, its rich-text editor renders the same structure fine.

````markdown
> ⚠️ **Beta — not yet tested in a headset.** Both fixes are verified against real Vivecraft
> and Sable bytecode and covered by unit tests, but nobody has yet teleported in VR on a
> moving Sable structure. Please report anything that misbehaves on the
> [issue tracker](https://github.com/bh679/vivecraft-sable-compat/issues).

Makes [Vivecraft](https://modrinth.com/mod/vivecraft) VR work properly while you are
standing on a [Sable](https://modrinth.com/mod/sable) sub-level — a moving,
physics-driven structure such as a ship or a train carriage.

**Client-side only.** It works on unmodified servers.

## What it fixes

**VR teleport drops you into the void.** You aim at a spot on the moving structure,
teleport, and fall through empty space far away from it. Vivecraft aims with a block
raycast, so on a sub-level it picks a destination in the far-off region where Sable
actually *stores* the structure's blocks — then writes that position to you directly.

**VR melee hits nothing.** Swinging at a mob standing next to you does nothing, and the
log fills with errors. Vivecraft's swing search box is built from two different coordinate
frames at once, so it spans ~20 million blocks. Sable refuses any query that large and
returns no targets at all.

Both are the same underlying problem: Sable keeps a sub-level's blocks somewhere far from
where it draws them, and Vivecraft's VR code predates Sable and does not know that.

## Requirements

- Minecraft 1.21.1 · NeoForge 21.1.228+
- [Vivecraft](https://modrinth.com/mod/vivecraft) (verified against `1.21.1-1.3.15-neoforge`)
- [Sable](https://modrinth.com/mod/sable) 2.0.5+

Neither is bundled — install them yourself. Sable's PolyForm Shield licence does not
permit redistribution.

## Reporting a problem

The mod logs its own diagnosis: whether each fix loaded, and for every teleport, the
destination Vivecraft chose versus the one actually used. Play, quit normally, and send
`logs/latest.log` — that one file is usually enough. See
[TESTING.md](https://github.com/bh679/vivecraft-sable-compat/blob/main/TESTING.md).

[Source](https://github.com/bh679/vivecraft-sable-compat) · PolyForm Shield 1.0.0
````

---

## If something goes wrong

| Symptom | Cause |
|---|---|
| Run is green but nothing published | A token or id is missing — the platform was skipped with a warning. Check the job summary. |
| `401` from Modrinth | PAT expired, or missing the *Create versions* scope. |
| CurseForge upload rejected | Project not yet approved by staff, or the game version is not enabled on the project. |
| `Input tag vX does not match mod_version` | Bump `gradle.properties`, or dispatch with the tag that matches it. |
| Mixin verification fails | Vivecraft renamed a wrapped call. Fix the mixin before releasing — do not bypass it. |
| Tag already exists | Delete it, or re-dispatch with `-f republish=true` to update the existing release. |
