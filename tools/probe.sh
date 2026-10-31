#!/usr/bin/env bash
# Developer tool: dumps Minecraft 26.3 class signatures (javap) into probe/.
# Lines in tools/probe-classes.txt ending in " -c" also dump bytecode.
set -u
mkdir -p probe
JAR=$(find ~/.gradle /home/runner/work -name "*.jar" -size +15M 2>/dev/null | while read -r j; do
  if unzip -l "$j" 2>/dev/null | grep -q "net/minecraft/client/Minecraft.class"; then echo "$j"; fi
done | head -1)
echo "jar: $JAR" > probe/info.txt
[ -z "$JAR" ] && exit 0
unzip -p "$JAR" version.json > probe/version.json 2>/dev/null; find ~/.gradle -name "26.3*.json" -path "*version*" 2>/dev/null | head -3 >> probe/info.txt
unzip -l "$JAR" | awk '{print $4}' | grep '\.class$' | sed 's/\.class$//' | sort > probe/classes.txt
while read -r line; do
  [ -z "$line" ] && continue
  cls=${line%% *}; flags=""
  [[ "$line" == *" -c" ]] && flags="-c"
  name=$(echo "$cls" | tr '/' '.')
  CP="$JAR"
  if [[ "$cls" == net/fabricmc/* ]]; then CP="$JAR:$(find ~/.gradle -name 'fabric-client-gametest-api-v1*.jar' | head -1)"; fi
  javap -p $flags -cp "$CP" "$name" > "probe/$(basename "$cls" | tr '$' '_').txt" 2>&1
done < tools/probe-classes.txt
grep -iE "$(paste -sd'|' tools/probe-patterns.txt)" probe/classes.txt > probe/matches.txt || true
unzip -l "$JAR" | awk '{print $4}' | grep -E "totem|textures/gui/sprites/widget/|textures/item/compass|textures/gui/title" > probe/assets.txt || true
mkdir -p probe/assets
while read -r a; do [ -z "$a" ] && continue; unzip -p "$JAR" "$a" > "probe/assets/$(echo $a | tr '/' '_')" 2>/dev/null; done < tools/probe-assets.txt
unzip -l "$JAR" | awk '{print $4}' | grep -E "sprites/hud/(crosshair|locator|experience)" > probe/hud-sprites.txt || true
# who uses ItemActivation / displayItemActivation
mkdir -p /tmp/x && cd /tmp/x && unzip -qo "$JAR" 'net/minecraft/client/*' && grep -rl "ItemActivation" . > "$OLDPWD/probe/itemactivation-users.txt" || true
GT=$(find ~/.gradle -name "fabric-client-gametest-api-v1*.jar" | head -1); [ -n "$GT" ] && unzip -l "$GT" | awk "{print \$4}" | grep "\.class$" > "$OLDPWD/probe/gametest-classes.txt" 2>/dev/null; unzip -l "$GT" | awk "{print \$4}" | grep "\.class$" > probe/gametest-classes.txt 2>/dev/null || true
