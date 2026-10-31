#!/usr/bin/env bash
# Developer tool: lists Minecraft 26.3 class signatures (javap) so the mod code
# can be written against the real API. Output goes to probe/.
set -u
mkdir -p probe
JAR=$(find ~/.gradle /home/runner/work -name "*.jar" -size +15M 2>/dev/null | while read -r j; do
  if unzip -l "$j" 2>/dev/null | grep -q "net/minecraft/client/Minecraft.class"; then echo "$j"; fi
done | head -1)
echo "jar: $JAR" > probe/info.txt
[ -z "$JAR" ] && exit 0
unzip -l "$JAR" | awk '{print $4}' | grep '\.class$' | grep -v '\$' | sed 's/\.class$//' | sort > probe/classes.txt
while read -r cls; do
  [ -z "$cls" ] && continue
  name=$(echo "$cls" | tr '/' '.')
  javap -p -cp "$JAR" "$name" > "probe/$(basename "$cls").txt" 2>&1
done < tools/probe-classes.txt
grep -iE "$(paste -sd'|' tools/probe-patterns.txt)" probe/classes.txt > probe/matches.txt || true
