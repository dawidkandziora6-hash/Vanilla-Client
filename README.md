# Vanilla Client – Fabric mod for Minecraft 26.3

A clean, **client-side** helper for your Hardcore/UHC world. It sends nothing to the server and
changes no game mechanics – it only shows things. Cosmetics are only visible to you.

## Features

| Module | What it does |
|---|---|
| **Days** | "☀ Day 37 · 14:20" on screen (sun turns into a moon at night) |
| **Coordinates** | XYZ, facing direction, matching Nether/Overworld position, chunk |
| **World Map** | Press **M** – biome-colored map of everything loaded/explored, saved per world; tasks show as markers |
| **Waypoints** | Press **B** (or right-click the map) – shown on the world map and as dots in the XP bar or a compass bar at the top |
| **UHC Guide** | Exact villager trades (what you pay → what you get) for every level, best enchantments, brewing table with ingredients – also in Minecraft's Options |
| **Tasks** | Task planner with categories + icons, priorities, coordinates ("Here" button), subtasks / material lists, progress bars |
| **Armor** | Armor + durability in the real hotbar look, right next to the hotbar (or free to move) |
| **Crosshair** | Paint your own pixel crosshair (pencil, eraser, line, fill, mirror, undo, presets), color, crisp 1–5× size, outline |
| **Zoom** | Hold **C**. Zoom level, smooth transition, scroll to adjust, cinematic camera, hide hand |
| **Images** | PNG/JPG from your PC as overlays (e.g. build plans). Drag them into the menu |
| **Wardrobe** | 14 capes, 10 wings, 10 shields, 8 totems – custom shapes (V-cut, rags, scallops, round/kite shields), animated designs (✦) and a 3D statue totem; plus your own PNG (just drop it in) |
| **Held Items** | Make the shield / totem in your hand and the totem pop animation smaller |

**Menu:** In the pause menu, left of "Options…", there's a new button with a book icon (or press **Right Shift**).
**Edit HUD:** drag everything with the mouse, scroll to resize, right-click to reset, snaps to edges and center.

## Install

Fabric Loader ≥ 0.19.5 + Fabric API 0.161.0+26.3 + the jar from
[Releases → latest](../../releases/tag/latest) into your `mods` folder.

## Build

JDK 25, then `./gradlew build` → `build/libs/`. Every push to `main` is built by GitHub Actions and
published as the `latest` release.

## Folders (`.minecraft/config/vanillaclient/`)

- `config.json` – all settings and tasks
- `images/` – your overlay pictures
- `cosmetics/` – your own cosmetics: `cape.png` / `wings.png` (Minecraft cape layout, 64×32 or 128×64), `shield.png` (shield layout, 64×64 or 128×128), `totem.png` (16×16 or 32×32). Transparent pixels cut the shape; a tall image (frames stacked vertically) is animated at 10 fps
