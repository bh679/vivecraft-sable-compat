#!/usr/bin/env bash
# Verify every mixin target against a real Vivecraft jar.
#
# Vivecraft is not a compile dependency — mixins target it by string name — so a
# renamed method does not fail the build, it fails at load. Worse, the original
# teleport bug this mod exists to fix is itself invisible without VR hardware, so
# a quietly-broken mixin is easy to ship. This script is the cheap guard: it
# disassembles the Vivecraft jar and asserts each targeted method really exists,
# in the method we inject into.
#
# Usage: scripts/verify-mixin-targets.sh [path/to/vivecraft.jar]
# With no argument it downloads the version pinned in gradle.properties.
set -euo pipefail
cd "$(dirname "$0")/.."

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

JAR="${1:-}"
if [[ -z "$JAR" ]]; then
  VER="$(grep '^vivecraft_dev_version=' gradle.properties | cut -d= -f2)"
  JAR="$WORK/vivecraft.jar"
  URL="https://api.modrinth.com/v2/project/vivecraft/version"
  echo "Resolving Vivecraft $VER from Modrinth…"
  DL="$(curl -sf "$URL" | python3 -c "
import json,sys
ver=sys.argv[1]
for v in json.load(sys.stdin):
    if v['version_number']==ver:
        print(v['files'][0]['url']); break
else:
    sys.exit('version not found on Modrinth: '+ver)
" "$VER")"
  curl -sfL -o "$JAR" "$DL"
fi

[[ -f "$JAR" ]] || { echo "no such jar: $JAR" >&2; exit 1; }

# Preflight javap BEFORE any check runs. Without this, a missing or broken JDK
# makes every javap invocation below fail and the script reports "target class
# missing" for each mixin — i.e. it blames Vivecraft for a local toolchain
# problem. It still exits non-zero either way, but the message sent someone
# hunting an upstream rename that had not happened. Fail with the real reason.
if ! javap -version >/dev/null 2>&1; then
  echo "javap not available (or no JDK on PATH) — cannot disassemble." >&2
  echo "Install a JDK 21 and ensure JAVA_HOME/bin is on PATH, then re-run." >&2
  exit 2
fi

echo "Verifying mixin targets against: $JAR"

unzip -q -o "$JAR" 'org/vivecraft/client_vr/gameplay/trackers/*.class' -d "$WORK/x"

fail=0
# check <owner class> <injected method> <expected call substring> <human label>
check() {
  local cls="$1" method="$2" needle="$3" label="$4"
  local out
  if ! out="$(javap -p -c -classpath "$WORK/x" "$cls" 2>/dev/null)"; then
    echo "  ✗ $label — target class missing: $cls"; fail=1; return
  fi
  # isolate the injected method's disassembly, then look for the wrapped call
  local body
  body="$(printf '%s\n' "$out" | awk "/ $method\(/{f=1} f&&/^  [a-z].*\(/&&!/ $method\(/{f=0} f")"
  if [[ -z "$body" ]]; then
    echo "  ✗ $label — injected method not found: $cls#$method"; fail=1; return
  fi
  local n
  n="$(printf '%s\n' "$body" | grep -c -- "$needle" || true)"
  if [[ "$n" -lt 1 ]]; then
    echo "  ✗ $label — wrapped call not found in $method: $needle"; fail=1; return
  fi
  echo "  ✓ $label — $n call site(s) of $needle in $method"
}

TRACKERS=org.vivecraft.client_vr.gameplay.trackers

# TeleportTrackerSubLevelMixin: NB 'moveTo' is the 1.21.1 name; later Minecraft
# versions renamed it 'snapTo'. A rename here is exactly what this guards.
check "$TRACKERS.TeleportTracker" activeProcess \
  "LocalPlayer.moveTo:(DDD)V" "teleport: LocalPlayer.moveTo(DDD)V"

# SwingTrackerSubLevelAabbMixin
check "$TRACKERS.SwingTracker" activeProcess \
  "ClientLevel.getEntities:(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;" \
  "melee: ClientLevel.getEntities(Entity,AABB)"

if [[ "$fail" -ne 0 ]]; then
  echo
  echo "FAILED — a mixin target no longer matches this Vivecraft build." >&2
  echo "Update the mixin (and its javadoc) before releasing." >&2
  exit 1
fi
echo "All mixin targets OK."
