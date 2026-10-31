# Vanilla Client – Fabric mod for Minecraft 26.3

A clean, **client-side** helper for your Hardcore/UHC world. It sends nothing to the server and
changes no game mechanics – it only shows things. Cosmetics are only visible to you.

## Features

| Module | What it does |
|---|---|
| **Days** | "☀ Day 37 · 14:20" on screen (sun turns into a moon at night) |
| **Tasks** | Task planner with categories + icons, priorities, coordinates ("Here" button), subtasks / material lists, progress bars |
| **Armor** | Armor + durability in the real hotbar look, right next to the hotbar (or free to move) |
| **Crosshair** | Paint your own pixel crosshair (paint, erase, mirror, presets), color, size, outline |
| **Zoom** | Hold **C**. Zoom level, smooth transition, scroll to adjust, cinematic camera, hide hand |
| **Images** | PNG/JPG from your PC as overlays (e.g. build plans). Drag them into the menu |
| **Wardrobe** | 10 capes (one animated), 7 wings (shown with an elytra), 7 shield skins – with a live player preview |
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
- `cosmetics/` – your own cosmetics: `cape.png` / `wings.png` (Minecraft cape layout, 64×32 or 128×64), `shield.png` (shield layout, 64×64 or 128×128)
