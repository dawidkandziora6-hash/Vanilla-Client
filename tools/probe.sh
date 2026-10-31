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
unzip -l "$JAR" | awk '{print $4}' | grep '\.class$' | sed 's/\.class$//' | sort > probe/classes.txt
while read -r line; do
  [ -z "$line" ] && continue
  cls=${line%% *}; flags=""
  [[ "$line" == *" -c" ]] && flags="-c"
  name=$(echo "$cls" | tr '/' '.')
  javap -p $flags -cp "$JAR" "$name" > "probe/$(basename "$cls" | tr '$' '_').txt" 2>&1
done < tools/probe-classes.txt
grep -iE "$(paste -sd'|' tools/probe-patterns.txt)" probe/classes.txt > probe/matches.txt || true
# who uses ItemActivation / displayItemActivation
mkdir -p /tmp/x && cd /tmp/x && unzip -qo "$JAR" 'net/minecraft/client/*' && grep -rl "ItemActivation" . > "$OLDPWD/probe/itemactivation-users.txt" || true
