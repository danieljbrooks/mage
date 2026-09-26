#!/bin/bash
# build-generalist-bundle.sh — the DraftZero "generalist" XMage bundle: Will's MageZero release
# bundle with the three jars this branch changes rebuilt from source. Every other file in the
# bundle stays byte for byte Will's release.
#
#   ./build-generalist-bundle.sh <unzipped release dir containing xmage/> <out dir> [JAVA_HOME]
#
# The three modules are compiled with javac against the release's own lib/ jars, instead of a
# full Maven build of XMage, which would recompile every card set. Order matters: mage-player-ai
# (ActionEncoder.ACTION_DIM) first, then -rl and mage-magezero against the new jar.
set -euo pipefail
REL="$(cd "$1" && pwd)"; OUT="$2"; JH="${3:-${JAVA_HOME:-}}"
JAVAC="${JH:+$JH/bin/}javac"; JAR="${JH:+$JH/bin/}jar"
SRC="$(cd "$(dirname "$0")" && pwd)"
V=1.4.58

test -d "$REL/xmage/lib" || { echo "!! $REL/xmage/lib not found"; exit 1; }
rm -rf "$OUT"; mkdir -p "$OUT"; cp -R "$REL/xmage" "$OUT/xmage"
LIB="$OUT/xmage/lib"
WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT

build() {   # artifact, source dir
  local art="$1" dir="$2" cls="$WORK/$1"
  mkdir -p "$cls"
  # compile against every jar in the bundle except the one being replaced
  local cp; cp=$(ls "$LIB"/*.jar | grep -v "/$art-$V.jar$" | tr '\n' ':')
  find "$dir" -name '*.java' > "$WORK/$art.srcs"
  "$JAVAC" -nowarn -encoding UTF-8 -source 8 -target 8 -Xlint:-options -cp "$cp" -d "$cls" @"$WORK/$art.srcs"
  # keep the release jar's META-INF (manifest and Maven metadata)
  (cd "$cls" && unzip -q -o "$REL/xmage/lib/$art-$V.jar" 'META-INF/*')
  rm -f "$LIB/$art-$V.jar"
  (cd "$cls" && "$JAR" cfm "$LIB/$art-$V.jar" META-INF/MANIFEST.MF .)
  echo "== $art: $(find "$cls" -name '*.class' | wc -l | tr -d ' ') classes"
}

build mage-player-ai    "$SRC/Mage.Server.Plugins/Mage.Player.AI/src/main/java"
build mage-player-ai-rl "$SRC/Mage.Server.Plugins/Mage.Player.AI.RL/src"
build mage-magezero     "$SRC/Mage.MageZero/src/main/java"

echo "$(git -C "$SRC" rev-parse HEAD)" > "$OUT/xmage/GENERALIST_SOURCE_COMMIT"
echo "== bundle at $OUT/xmage (source $(git -C "$SRC" rev-parse --short HEAD))"
