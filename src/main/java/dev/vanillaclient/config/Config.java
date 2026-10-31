package dev.vanillaclient.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** All settings. Stored in config/vanillaclient/config.json. */
public final class Config {
	public static final Logger LOG = LoggerFactory.getLogger("vanillaclient");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Config instance = new Config();

	// ---------------------------------------------------------------- data

	/** Shared fields of every movable HUD element. */
	public static class Hud {
		public boolean enabled = true;
		/** Position as a fraction of the free space (0 = left/top, 1 = right/bottom). */
		public float x;
		public float y;
		public float scale = 1.0f;
		public int color = 0xFFFFFFFF;
		public boolean shadow = true;
		public boolean background = false;
		public int backgroundColor = 0x90000000;

		Hud at(float x, float y) {
			this.x = x;
			this.y = y;
			return this;
		}
	}

	/** A HUD line with your own label in front ("Day 12", "FPS 144" …). */
	public static class TextHud extends Hud {
		public String label = "";
		public boolean showLabel = true;
		public int labelColor = 0xFFAAAAAA;
		/** Text after the value, e.g. " blocks/s". */
		public String suffix = "";

		TextHud label(String l) {
			this.label = l;
			return this;
		}

		TextHud off() {
			this.enabled = false;
			return this;
		}
	}

	public static class Days extends TextHud {
		public Days() {
			label = "Day";
			labelColor = 0xFFFFFFFF;
		}
	}

	public static class Coords extends Hud {
		public boolean compact = false;
		public boolean showDirection = true;
		/** Show the matching Nether / Overworld position. */
		public boolean showOtherDimension = true;
		public boolean showChunk = false;
		public boolean coloredLabels = true;
		public String labelX = "X", labelY = "Y", labelZ = "Z";
		public String labelFacing = "", labelOther = "", labelChunk = "Chunk";
		public int labelColor = 0xFFAAAAAA;
	}

	/** Item counter: how many of these items you carry. */
	public static class Counter extends TextHud {
		public List<String> items = new ArrayList<>(List.of("totem_of_undying", "golden_apple", "ender_pearl", "arrow"));
		public boolean hideZero = true;
		public boolean vertical = false;
	}

	public static class Keystrokes extends Hud {
		public boolean showMouse = true;
		public boolean showCps = true;
		public boolean showSpace = true;
		public boolean outline = false;
		public int keyColor = 0x80000000;
		public int pressedColor = 0xD0FFFFFF;
		public int pressedTextColor = 0xFF000000;
		public int keySize = 20;
		public int gap = 2;
		public boolean rounded = true;
		public int borderColor = 0x60FFFFFF;
		public boolean textShadow = false;
		public boolean showSneakSprint = false;
		/** 0 = key names, 1 = arrows */
		public int labelStyle = 0;
		/** How fast a key fades back (1 = normal). */
		public float fadeSpeed = 1f;
		public boolean showLabelsOnMouse = true;
	}

	/** Ranks earned with play time. */
	public static class Ranks {
		public boolean enabled = true;
		/** Show your own name tag in third person (F5). */
		public boolean ownNameTag = true;
		/** Rank symbol in front of your name tag. */
		public boolean symbolInName = true;
		/** Rank the owner account wears ("" = Owner). */
		public String display = "";
		// ---- name perks (only work with a rank that unlocks them)
		/** Name color (0 = rank color). */
		public int nameColor = 0;
		/** Second color for a gradient name (0 = off). */
		public int nameColor2 = 0;
		public boolean animatedName = false;
		public boolean boldName = false;
		public boolean italicName = false;
		/** default, uniform, alt (galactic), illageralt */
		public String nameFont = "default";
		/** Badge in front of the name ("" = the rank's badge). */
		public String badge = "";
		/** Also show the rank's title in the name tag. */
		public boolean showRankTitle = false;
		/** Show Owner / Admin … ranks from the online list. */
		public boolean showClientRanks = true;
		/** Download the rank list from GitHub. */
		public boolean onlineRanks = true;
		/** Send your cosmetics to servers / LAN worlds that have Vanilla Client. */
		public boolean shareCosmetics = true;
		/** Show other players' cosmetics. */
		public boolean showOthers = true;
		/** Play time with Vanilla Client in ticks. */
		public long playTicks = 0;
	}

	public static class WorldMap {
		public boolean enabled = true;
		/** Chunks around you that get mapped (0 = your render distance). */
		public int radius = 0;
		public boolean showTasks = true;
	}

	public static class Waypoint {
		public String name = "Waypoint";
		public int x, y, z;
		/** World id + dimension, e.g. "sp_myworld/overworld". */
		public String world = "";
		public int color = 0xFFFFD34D;
		public boolean visible = true;
		/** Item id used as icon ("" = colored diamond). */
		public String icon = "";
		public long created = 0;

		public Waypoint() {}

		public Waypoint(String name, int x, int y, int z, String world, int color) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.world = world;
			this.color = color;
		}
	}

	public static class WaypointHud {
		/** 0 = off, 1 = dots on the XP bar, 2 = compass bar at the top */
		public int mode = 1;
		public boolean showDistance = true;
		/** Hide waypoints farther away than this (0 = never). */
		public int maxDistance = 0;
	}

	public static class Todo extends Hud {
		public String title = "Tasks";
		public boolean showTitle = true;
		public int maxLines = 6;
		public boolean hideDone = false;
		public boolean showSubtasks = true;
		public boolean showCoords = false;
		/** Empty = all categories. */
		public String categoryFilter = "";
	}

	public static class Armor extends Hud {
		/** Right next to the hotbar in the vanilla style. Off = freely movable. */
		public boolean attachToHotbar = true;
		public boolean rightSide = true;
		/** 0 = bar only, 1 = percent, 2 = points left */
		public int durabilityText = 0;
		public boolean showHeldItem = false;
		public boolean hideWhenEmpty = true;
	}

	public static class Zoom {
		public boolean enabled = true;
		public float level = 4.0f;
		public boolean smoothTransition = true;
		public boolean scrollToAdjust = true;
		public boolean cinematicCamera = true;
		public boolean resetLevelOnRelease = true;
		public boolean hideHand = true;
	}

	public static class Crosshair {
		public boolean enabled = false;
		public int size = 15;
		/** Row by row, '1' = pixel set. Length size*size. */
		public String pixels = "";
		public int color = 0xFFFFFFFF;
		public float scale = 1.0f;
		public boolean attackIndicator = true;
		public boolean outline = false;
		public int outlineColor = 0xFF000000;
		/** CS / Valorant style outline: 1-3 px, own opacity. */
		public int outlineThickness = 1;
		public float outlineOpacity = 0.8f;
		/** Inverts the colors behind it like the vanilla crosshair. */
		public boolean vanillaBlend = false;
		public boolean thirdPerson = false;
		/** Hide while a menu or the inventory is open? (always hidden by vanilla) */
		public boolean dynamicColor = false;
		public int targetColor = 0xFFFF4040;
	}

	public static class Cosmetics {
		public String cape = "none";
		public String wings = "none";
		public String shield = "none";
		public String totem = "none";
		/** Wings only appear while you wear an elytra (they replace its look). */
		public boolean wingsOnlyWithElytra = false;
		/** With an elytra on: show the wings instead of the elytra. */
		public boolean wingsReplaceElytra = true;
	}

	/** Smaller (or bigger) items in your own first-person hands. Purely visual. */
	public static class HeldItems {
		public boolean enabled = true;
		public float shieldScale = 1.0f;
		public float totemScale = 1.0f;
		/** Size of the big totem animation when a totem saves you. */
		public float totemPopScale = 1.0f;
		/** Size per category (swords, food, blocks …). */
		public Map<String, Float> categories = new java.util.LinkedHashMap<>();
		/** Size per item id – wins over the category. */
		public Map<String, Float> items = new java.util.LinkedHashMap<>();
	}

	public static class Image extends Hud {
		public String file = "";
		/** Tint + opacity (ARGB). */
		public int tint = 0xFFFFFFFF;
	}

	public static class SubTask {
		public String text = "";
		public boolean done;

		public SubTask() {}

		public SubTask(String text) {
			this.text = text;
		}
	}

	public static class TodoItem {
		public String text = "";
		public String category = "Other";
		public boolean done;
		/** 0 = normal, 1 = important, 2 = urgent */
		public int priority;
		public String coords = "";
		public String note = "";
		public List<SubTask> subtasks = new ArrayList<>();
		/** Picture file names from the images folder. */
		public List<String> images = new ArrayList<>();
		/** Item id shown as the task icon ("" = category icon). */
		public String icon = "";
		/** Due day in the world (0 = none). */
		public int dueDay = 0;

		public TodoItem() {}

		public TodoItem(String text, String category) {
			this.text = text;
			this.category = category;
		}
	}

	public static class Category {
		public String name = "";
		/** Index into {@link dev.vanillaclient.gui.Icons#CATEGORY_ICONS}. */
		public int icon;

		public Category() {}

		public Category(String name, int icon) {
			this.name = name;
			this.icon = icon;
		}
	}

	public Days days = (Days) new Days().at(0.01f, 0.01f);
	public TextHud fps, ping, biome, clock, speed, light, session, rankHud, effects;
	public Counter counter;
	public Ranks ranks = new Ranks();
	public Keystrokes keystrokes;
	/** Coins and bought cosmetics, per Minecraft account. */
	public Map<String, Wallet> wallets = new java.util.HashMap<>();

	public static class Wallet {
		public long coins;
		public List<String> owned = new ArrayList<>();
		/** Coins already given by ranks.json. */
		public long granted;
	}
	public Coords coords = (Coords) new Coords().at(0.01f, 0.08f);
	public WorldMap worldMap = new WorldMap();
	public Todo todo = (Todo) new Todo().at(0.99f, 0.02f);
	public Armor armor = (Armor) new Armor().at(1.0f, 1.0f);
	public Zoom zoom = new Zoom();
	public Crosshair crosshair = new Crosshair();
	public Cosmetics cosmetics = new Cosmetics();
	public HeldItems heldItems = new HeldItems();
	public List<Image> images = new ArrayList<>();
	public List<Waypoint> waypoints = new ArrayList<>();
	public WaypointHud waypointHud = new WaypointHud();
	public List<TodoItem> todos = new ArrayList<>();
	public List<Category> taskCategories;
	/** Old format (plain names) – only read for migration. */
	public List<String> categories;
	public boolean pauseMenuButton = true;
	public boolean chestButtons = true;
	public boolean screenshotMessage = true;
	public boolean screenshotPopup = true;
	public boolean helperButtons = true;

	public static List<Category> defaultCategories() {
		return new ArrayList<>(List.of(new Category("Farms", 0), new Category("Builds", 1),
				new Category("Gathering", 2), new Category("Other", 3)));
	}

	// ---------------------------------------------------------------- load / save

	public static Config get() {
		return instance;
	}

	public static Path dir() {
		return FabricLoader.getInstance().getConfigDir().resolve("vanillaclient");
	}

	public static Path imagesDir() {
		return dir().resolve("images");
	}

	public static Path cosmeticsDir() {
		return dir().resolve("cosmetics");
	}

	private static Path file() {
		return dir().resolve("config.json");
	}

	public static void load() {
		migrateOldFolder();
		try {
			Files.createDirectories(imagesDir());
			Files.createDirectories(cosmeticsDir());
		} catch (IOException e) {
			LOG.warn("Could not create folders", e);
		}
		Path f = file();
		if (Files.exists(f)) {
			try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
				Config loaded = GSON.fromJson(r, Config.class);
				if (loaded != null) instance = loaded;
			} catch (Exception e) {
				LOG.warn("config.json is broken – using defaults", e);
			}
		}
		instance.repair();
		save();
	}

	public static void save() {
		Path f = file();
		Path tmp = f.resolveSibling("config.json.tmp");
		try {
			Files.createDirectories(f.getParent());
			try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, w);
			}
			Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			try {
				Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
				LOG.warn("Could not save settings", e);
			}
		}
	}

	/** Takes over settings, images and cosmetics from the first (German) version. */
	private static void migrateOldFolder() {
		Path old = FabricLoader.getInstance().getConfigDir().resolve("vanillaclient");
		if (Files.exists(dir()) || !Files.isDirectory(old)) return;
		try {
			Files.createDirectories(dir());
			copyTree(old.resolve("bilder"), imagesDir());
			copyTree(old.resolve("kosmetik"), cosmeticsDir());
			Path cfg = old.resolve("config.json");
			if (Files.exists(cfg)) Files.copy(cfg, file());
			Map<String, String> renames = Map.of("umhang.png", "cape.png", "fluegel.png", "wings.png", "schild.png", "shield.png");
			for (var e : renames.entrySet()) {
				Path p = cosmeticsDir().resolve(e.getKey());
				if (Files.exists(p)) Files.move(p, cosmeticsDir().resolve(e.getValue()), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOG.warn("Could not migrate old settings", e);
		}
	}

	private static void copyTree(Path from, Path to) throws IOException {
		if (!Files.isDirectory(from)) return;
		Files.createDirectories(to);
		try (Stream<Path> s = Files.list(from)) {
			for (Path p : s.toList()) {
				if (Files.isRegularFile(p)) Files.copy(p, to.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	/** Fill missing fields (e.g. after updates) with defaults. */
	private void repair() {
		if (days == null) days = (Days) new Days().at(0.01f, 0.01f);
		if (todo == null) todo = (Todo) new Todo().at(0.99f, 0.02f);
		if (coords == null) coords = (Coords) new Coords().at(0.01f, 0.08f);
		if (worldMap == null) worldMap = new WorldMap();
		if (armor == null) armor = (Armor) new Armor().at(1.0f, 1.0f);
		if (zoom == null) zoom = new Zoom();
		if (crosshair == null) crosshair = new Crosshair();
		if (cosmetics == null) cosmetics = new Cosmetics();
		if (heldItems == null) heldItems = new HeldItems();
		if (images == null) images = new ArrayList<>();
		if (waypoints == null) waypoints = new ArrayList<>();
		if (waypointHud == null) waypointHud = new WaypointHud();
		if (todos == null) todos = new ArrayList<>();
		if (ranks == null) ranks = new Ranks();
		if (wallets == null) wallets = new java.util.HashMap<>();
		if (ranks.nameFont == null) ranks.nameFont = "default";
		if (ranks.badge == null) ranks.badge = "";
		if (keystrokes == null) {
			keystrokes = new Keystrokes();
			keystrokes.enabled = false;
			keystrokes.at(0.99f, 0.97f);
		}
		if (fps == null) fps = (TextHud) new TextHud().label("FPS").off().at(0.01f, 0.30f);
		if (ping == null) ping = (TextHud) new TextHud().label("Ping").off().at(0.01f, 0.34f);
		if (biome == null) biome = (TextHud) new TextHud().label("Biome").off().at(0.01f, 0.38f);
		if (clock == null) clock = (TextHud) new TextHud().label("Time").off().at(0.01f, 0.42f);
		if (speed == null) speed = (TextHud) new TextHud().label("Speed").off().at(0.01f, 0.46f);
		if (light == null) light = (TextHud) new TextHud().label("Light").off().at(0.01f, 0.50f);
		if (session == null) session = (TextHud) new TextHud().label("Session").off().at(0.01f, 0.54f);
		if (rankHud == null) rankHud = (TextHud) new TextHud().label("Rank").off().at(0.01f, 0.58f);
		if (effects == null) effects = (TextHud) new TextHud().label("").off().at(0.99f, 0.40f);
		if (counter == null) {
			counter = new Counter();
			counter.label = "";
			counter.enabled = false;
			counter.at(0.99f, 0.60f);
		}
		if (days.label == null) days.label = "Day";
		if (heldItems.categories == null) heldItems.categories = new java.util.LinkedHashMap<>();
		if (heldItems.items == null) heldItems.items = new java.util.LinkedHashMap<>();
		// old single sliders -> categories
		if (heldItems.shieldScale != 1f && !heldItems.categories.containsKey("shields")) heldItems.categories.put("shields", heldItems.shieldScale);
		if (heldItems.totemScale != 1f && !heldItems.categories.containsKey("totems")) heldItems.categories.put("totems", heldItems.totemScale);
		heldItems.shieldScale = 1f;
		heldItems.totemScale = 1f;
		for (TodoItem t : todos) {
			if (t.images == null) t.images = new ArrayList<>();
			if (t.icon == null) t.icon = "";
		}
		for (Waypoint w : waypoints) if (w.icon == null) w.icon = "";
		if (coords.labelX == null) coords.labelX = "X";
		if (coords.labelY == null) coords.labelY = "Y";
		if (coords.labelZ == null) coords.labelZ = "Z";
		if (coords.labelFacing == null) coords.labelFacing = "";
		if (coords.labelOther == null) coords.labelOther = "";
		if (coords.labelChunk == null) coords.labelChunk = "Chunk";

		// German defaults from the first version -> English
		if ("Tag".equals(days.label) || days.label == null) days.label = "Day";
		if ("Aufgaben".equals(todo.title) || todo.title == null) todo.title = "Tasks";
		Map<String, String> catNames = Map.of("Farm", "Farms", "Bau", "Builds", "Sammeln", "Gathering", "Sonstiges", "Other");

		if (taskCategories == null || taskCategories.isEmpty()) {
			if (categories != null && !categories.isEmpty()) {
				taskCategories = new ArrayList<>();
				int i = 0;
				for (String c : categories) {
					taskCategories.add(new Category(catNames.getOrDefault(c, c), Math.min(i++, 3)));
				}
			} else {
				taskCategories = defaultCategories();
			}
		}
		categories = null;
		todo.categoryFilter = catNames.getOrDefault(todo.categoryFilter, todo.categoryFilter == null ? "" : todo.categoryFilter);
		for (TodoItem t : todos) {
			if (t.subtasks == null) t.subtasks = new ArrayList<>();
			if (t.text == null) t.text = "";
			if (t.coords == null) t.coords = "";
			if (t.note == null) t.note = "";
			t.category = catNames.getOrDefault(t.category, t.category);
			if (t.category == null || category(t.category) == null) t.category = taskCategories.get(taskCategories.size() - 1).name;
		}
		zoom.level = clamp(zoom.level, 1.5f, 50f);
		heldItems.shieldScale = clamp(heldItems.shieldScale, 0.3f, 1.5f);
		heldItems.totemScale = clamp(heldItems.totemScale, 0.3f, 1.5f);
		heldItems.totemPopScale = clamp(heldItems.totemPopScale, 0.2f, 1.5f);
		crosshair.size = Math.max(3, Math.min(31, crosshair.size));
		crosshair.scale = Math.max(1, Math.min(5, Math.round(crosshair.scale)));
		if (cosmetics.totem == null) cosmetics.totem = "none";
		if (crosshair.pixels == null || crosshair.pixels.length() != crosshair.size * crosshair.size) {
			crosshair.size = 15;
			crosshair.pixels = CrosshairPresets.vanilla(15);
		}
	}

	public Category category(String name) {
		for (Category c : taskCategories) {
			if (c.name.equals(name)) return c;
		}
		return null;
	}

	public static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}
