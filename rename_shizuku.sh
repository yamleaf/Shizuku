#!/usr/bin/env bash
# Rename Shizuku -> custom package (anti-detection). Linux/macOS companion to rename_shizuku.ps1.
# Version 1.0 (2026-09-03):
#   - mirrors rename_shizuku.ps1 v2.2 logic (copy-excl / content-replace w/ protocol protection /
#     BinderContainer.java skip / dir move / self-test)
# Usage:
#   bash rename_shizuku.sh <Src> <Dst> <NewPkg> [NewServer] [NewProc]
set -euo pipefail

SRC="${1:?usage: rename_shizuku.sh <Src> <Dst> <NewPkg> [NewServer] [NewProc]}"
DST="${2:?missing Dst}"
NEWPKG="${3:?missing NewPkg}"
NEWSERVER="${4:-}"
NEWPROC="${5:-}"

if [[ ! -d "$SRC" ]]; then echo "Source dir not found: $SRC" >&2; exit 1; fi
if ! [[ "$NEWPKG" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]]; then
  echo "NewPkg must be a valid package, e.g. com.abc.helper" >&2; exit 1
fi
if [[ -e "$DST" ]] && [[ -n "$(ls -A "$DST" 2>/dev/null)" ]]; then echo "Dest dir not empty: $DST" >&2; exit 1; fi
mkdir -p "$DST"
[[ -z "$NEWSERVER" ]] && NEWSERVER="$NEWPKG.server"
[[ -z "$NEWPROC" ]]   && NEWPROC="${NEWPKG##*.}_server"

echo "==> rename_shizuku.sh v1.0 | target pkg: $NEWPKG (server $NEWSERVER, proc $NEWPROC)"

# ---- 0. Copy project: exclude .git and build-product dirs ----
echo "==> copy $SRC -> $DST (excl. build/out/.gradle/.idea/.kotlin)"
tar -C "$SRC" --exclude='.git' --exclude='build' --exclude='out' \
    --exclude='.gradle' --exclude='.idea' --exclude='.kotlin' -cf - . | tar -C "$DST" -xf -

OLDPKG="moe.shizuku"
OLDSERVER="moe.shizuku.server"
OLDMANAGER="moe.shizuku.privileged.api"
OLDPROC="shizuku_server"
OLDSTARTER="moe.shizuku.starter"
OLDAPI="moe.shizuku.api"
OLDMANAGERPKG="moe.shizuku.manager"

echo "==> rename $OLDPKG -> $NEWPKG"

# ---- 1. Protected protocol values (must stay unchanged) ----
# Same list as ps1: tokenize before global replace, restore after.
PROTECTED=(
  'moe.shizuku.api.action.BINDER_RECEIVED'
  'moe.shizuku.privileged.api.intent.extra.BINDER'
  'moe.shizuku.client.V3_SUPPORT'
  'moe.shizuku.client.V3_REQUIRES_ROOT'
  'moe.shizuku.api.BinderContainer'
)

# ---- 2. Replace file contents ----
count=0
while IFS= read -r -d '' f; do
  name="$(basename "$f")"
  # v2.1: BinderContainer.java keeps its moe.shizuku.api package declaration untouched.
  if [[ "$name" == "BinderContainer.java" ]]; then continue; fi
  content="$(cat "$f")" || continue
  # gate: skip files that don't mention anything to rename
  if [[ "$content" != *"$OLDPKG"* && "$content" != *"$OLDPROC"* && "$content" != *"moe/shizuku"* ]]; then continue; fi
  new="$content"
  # protect: token first so the sequence replace below won't touch protocol values
  for i in "${!PROTECTED[@]}"; do
    p="${PROTECTED[$i]}"
    token="__PROTECTED_${i}__"
    new="${new//$p/$token}"
  done
  new="${new//$OLDSERVER/$NEWSERVER}"
  new="${new//$OLDMANAGER/$NEWPKG}"
  new="${new//$OLDSTARTER/$NEWPKG.starter}"
  new="${new//$OLDAPI/$NEWPKG.api}"
  new="${new//$OLDMANAGERPKG/$NEWPKG.manager}"
  new="${new//$OLDPKG/$NEWPKG}"
  # native (jni .cpp/.h) JNI class paths are slash-separated
  new="${new//moe\/shizuku/${NEWPKG//./\/}}"
  new="${new//$OLDPROC/$NEWPROC}"
  new="${new//shizuku.library.path/${NEWPKG##*.}.library.path}"
  # restore protected protocol values
  for i in "${!PROTECTED[@]}"; do
    token="__PROTECTED_${i}__"
    p="${PROTECTED[$i]}"
    new="${new//$token/$p}"
  done
  if [[ "$new" != "$content" ]]; then
    printf '%s' "$new" > "$f"
    count=$((count + 1))
  fi
done < <(find "$DST" -type f \
  \( -name '*.java' -o -name '*.kt' -o -name '*.aidl' -o -name '*.xml' \
     -o -name '*.gradle' -o -name '*.gradle.kts' -o -name '*.pro' \
     -o -name '*.cpp' -o -name '*.h' -o -name '*.md' -o -name '*.txt' \) \
  -print0)
echo "==> replaced $count files (protected ${#PROTECTED[@]} protocol values)"

# ---- 3. Move directories moe/shizuku/** -> NewPkg/** ----
SRCDIRS=(
  'api/aidl/src/main/aidl'
  'api/provider/src/main/java'
  'api/shared/src/main/java'
  'starter/src/main/java'
  'server/src/main/java'
  'manager/src/main/java'
  'common/src/main/java'
)
# BinderContainer must stay at moe/shizuku/api (protected FQN). Keep it aside during dir move.
BC_REL='api/provider/src/main/java/moe/shizuku/api/BinderContainer.java'
BC_PATH="$DST/$BC_REL"
BC_TMP="$DST/BinderContainer.java.__keep"
if [[ -f "$BC_PATH" ]]; then mv "$BC_PATH" "$BC_TMP"; fi

NEW_SLASH="${NEWPKG//./\/}"
for d in "${SRCDIRS[@]}"; do
  old="$DST/$d/moe/shizuku"
  if [[ -d "$old" ]]; then
    dest="$DST/$d/$NEW_SLASH"
    mkdir -p "$dest"
    cp -a "$old/." "$dest/"
    rm -rf "$old"
    echo "==> moved moe/shizuku -> $NEWPKG (under $d)"
  fi
done

# Restore BinderContainer.java to its original (unrenamed) package location
if [[ -f "$BC_TMP" ]]; then
  mkdir -p "$(dirname "$BC_PATH")"
  mv "$BC_TMP" "$BC_PATH"
  echo "==> kept moe/shizuku/api/BinderContainer.java (unrenamed)"
fi

# ---- 4. Self-test: scan leftover moe.shizuku mentions ----
# Expected hits: protected protocol values (review output manually).
leftover="$(grep -rl -E 'moe\.shizuku|moe/shizuku|shizuku_server' \
  --include='*.java' --include='*.kt' --include='*.aidl' --include='*.xml' \
  --include='*.cpp' --include='*.h' --include='*.pro' "$DST" 2>/dev/null || true)"
if [[ -n "$leftover" ]]; then
  echo "==> self-test: leftover moe.shizuku mentions (protected protocol values expected):"
  echo "$leftover" | sed "s|$DST|.|g"
else
  echo "==> self-test OK: no moe.shizuku leftover"
fi

echo "==> done. Output: $DST"