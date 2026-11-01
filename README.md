# Vanilla Client – Fabric mod for Minecraft 26.3

A clean, **client-side** helper for your Hardcore/UHC world. It sends nothing to the server and
changes no game mechanics – it only shows things. Cosmetics are only visible to you.

## Features

| Module | What it does |
|---|---|
| **Days** | "Day 37" on screen, counted from day 1, your own label |
| **Coordinates** | XYZ, facing, Nether/Overworld position, chunk – every label editable |
| **More HUD modules** | FPS, ping, biome, real time, speed, light level, session time, rank, potion effects, item counter (totems, gapples, pearls …) |
| **Ranks** | Client tiers Plus, Pro, Ultra, Ultra+, Elite, Legend, Creator, Partner, Staff, Dev, Owner – given by name in [`ranks.json`](ranks.json) (not obtainable yet). Perks: name color, bold/italic, gradient, animated gradient, name font, own badge |
| **Shared cosmetics** | On LAN worlds and servers with Vanilla Client everyone sees each other's capes, wings, shields and ranks |
| **PvP** | Hitboxes (colors, fill, aim/hurt highlight, eye line, look direction, per entity type), Fullbright, low fire, target HUD (health, armor, distance), reach display, combo counter, hurt-shake/FOV-effect/bobbing/attack-indicator shortcuts – toggle keys in Controls |
| **Scoreboard** | Sidebar as a movable module: scale, hide numbers, number color, max lines, background/title colors – or Minecraft's own |
| **Performance** | One-click FPS boost (with undo), hide far mobs/items, all important video settings in one page |
| **Keystrokes** | W A S D, mouse with CPS, sneak/sprint, space – size, gap, rounded, colors, presets |
| **Gallery** | Screenshot popup + nicer chat message ([View] [Open] [Folder]), in-game screenshot viewer, HUD pictures with previews |
| **Chests** | Small sort + store icons in the top right corner of chests and shulker boxes (+ optional sort key) |
| **Advancements** | Every advancement in one checklist (also hidden ones), what's still missing, short hints |
| **Statistics** | Overview cards + most mined / killed / killed by / crafted |
| **World Map** | Press **M** – biome-colored map of everything loaded/explored, saved per world; tasks show as markers |
| **Waypoints** | Press **B** (or right-click the map) – icons, any color, drag to move; dots on the XP bar or a compass bar |
| **UHC Guide** | Exact villager trades (what you pay → what you get) for every level, best enchantments, brewing table with ingredients – also in Minecraft's Options |
| **Tasks** | Task planner with categories + icons, priorities, coordinates ("Here" button), subtasks / material lists, progress bars |
| **Armor** | Armor + durability in the real hotbar look, right next to the hotbar (or free to move) |
| **Crosshair** | Paint your own pixel crosshair, crisp 1–5× size, CS/Valorant-style outline (1–3 px, opacity), vanilla inverting blend, color on targets |
| **Zoom** | Hold **C**. Zoom level, smooth transition, scroll to adjust, cinematic camera, hide hand |
| **Images** | PNG/JPG from your PC as overlays (e.g. build plans). Drag them into the menu |
| **Wardrobe & Shop** | Collections (Inferno, Celestial, Void): animated capes, 3D flapping wings, round shields – bought with coins (coins come from `ranks.json`), plus your own PNG. Wings optionally only with an elytra |
| **Held Items** | Size of items in your hand per category (swords, food, blocks …) or per single item; totem pop size |

**Menu:** In the pause menu next to the small icon row: "Vanilla Client" (left) and "Wardrobe" (right) – or press **Right Shift**. All keys can be changed in Vanilla Client › Controls.
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
- `cosmetics/` – your own cosmetics: `cape.png` / `wings.png` (Minecraft cape layout, 64×32 or 128×64), `shield.png` (shield layout, 64×64 or 128×128), Transparent pixels cut the shape; a tall image (frames stacked vertically) is animated at 10 fps

## Ranks

Edit [`ranks.json`](ranks.json) on GitHub to give someone a rank (`vip`, `vip+`, `mvp`, `mvp+`, `elite`, `legend`, `media`, `partner`, `mod`, `admin`, `owner`; owners can wear any rank).
Every Vanilla Client downloads the list at start and every 10 minutes. Optional `cape` / `wings` / `shield` entries show
those designs to everyone, even on servers without the mod.
