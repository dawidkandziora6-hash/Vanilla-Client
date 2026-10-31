package dev.logbuch.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Alle Einstellungen der Mod. Liegt in config/logbuch/config.json. */
public final class LogbuchConfig {
	public static final Logger LOG = LoggerFactory.getLogger("logbuch");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static LogbuchConfig instance = new LogbuchConfig();

	// ---------- Daten ----------

	/** Gemeinsame Felder aller verschiebbaren HUD-Elemente. */
	public static class Hud {
		public boolean enabled = true;
		/** Position als Anteil des freien Platzes (0 = links/oben, 1 = rechts/unten). */
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

	public static class Days extends Hud {
		public boolean showTime = true;
		public boolean countFromOne = true;
		public String label = "Tag";
	}

	public static class Todo extends Hud {
		public String title = "Aufgaben";
		public boolean showTitle = true;
		public int maxLines = 6;
		public boolean hideDone = false;
		public boolean showSubtasks = true;
		public boolean showCoords = false;
		/** Leer = alle Kategorien. */
		public String categoryFilter = "";
	}

	public static class Armor extends Hud {
		/** Direkt neben der Hotbar, im Vanilla-Stil. Ausgeschaltet = frei verschiebbar. */
		public boolean attachToHotbar = true;
		public boolean rightSide = true;
		/** 0 = aus, 1 = Prozent, 2 = Restpunkte */
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
	}

	public static class Crosshair {
		public boolean enabled = false;
		public int size = 15;
		/** Zeilenweise '1' = Pixel gesetzt, '0' = leer. Länge size*size. */
		public String pixels = "";
		public int color = 0xFFFFFFFF;
		public float scale = 1.0f;
		public boolean attackIndicator = true;
		public boolean outline = false;
		public int outlineColor = 0xFF000000;
	}

	public static class Cosmetics {
		public String cape = "none";
		public String wings = "none";
		public String shield = "none";
	}

	public static class Image extends Hud {
		public String file = "";
		/** Tönung + Deckkraft (ARGB). */
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
		public String category = "Sonstiges";
		public boolean done;
		/** 0 = normal, 1 = wichtig, 2 = dringend */
		public int priority;
		public String coords = "";
		public List<SubTask> subtasks = new ArrayList<>();

		public TodoItem() {}

		public TodoItem(String text, String category) {
			this.text = text;
			this.category = category;
		}
	}

	public Days days = (Days) new Days().at(0.01f, 0.01f);
	public Todo todo = (Todo) new Todo().at(0.99f, 0.02f);
	public Armor armor = (Armor) new Armor().at(1.0f, 1.0f);
	public Zoom zoom = new Zoom();
	public Crosshair crosshair = new Crosshair();
	public Cosmetics cosmetics = new Cosmetics();
	public List<Image> images = new ArrayList<>();
	public List<TodoItem> todos = new ArrayList<>();
	public List<String> categories = new ArrayList<>(List.of("Farm", "Bau", "Sammeln", "Sonstiges"));
	public boolean pauseMenuButton = true;

	// ---------- Laden / Speichern ----------

	public static LogbuchConfig get() {
		return instance;
	}

	public static Path dir() {
		return FabricLoader.getInstance().getConfigDir().resolve("logbuch");
	}

	public static Path imagesDir() {
		return dir().resolve("bilder");
	}

	public static Path cosmeticsDir() {
		return dir().resolve("kosmetik");
	}

	private static Path file() {
		return dir().resolve("config.json");
	}

	public static void load() {
		try {
			Files.createDirectories(imagesDir());
			Files.createDirectories(cosmeticsDir());
		} catch (IOException e) {
			LOG.warn("Konnte Ordner nicht anlegen", e);
		}
		Path f = file();
		if (Files.exists(f)) {
			try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
				LogbuchConfig loaded = GSON.fromJson(r, LogbuchConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				LOG.warn("config.json ist beschädigt – Standardwerte werden benutzt", e);
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
				LOG.warn("Konnte Einstellungen nicht speichern", e);
			}
		}
	}

	/** Fehlende Felder (z. B. nach Updates) mit Standardwerten auffüllen. */
	private void repair() {
		if (days == null) days = (Days) new Days().at(0.01f, 0.01f);
		if (todo == null) todo = (Todo) new Todo().at(0.99f, 0.02f);
		if (armor == null) armor = (Armor) new Armor().at(1.0f, 1.0f);
		if (zoom == null) zoom = new Zoom();
		if (crosshair == null) crosshair = new Crosshair();
		if (cosmetics == null) cosmetics = new Cosmetics();
		if (images == null) images = new ArrayList<>();
		if (todos == null) todos = new ArrayList<>();
		if (categories == null || categories.isEmpty()) {
			categories = new ArrayList<>(List.of("Farm", "Bau", "Sammeln", "Sonstiges"));
		}
		for (TodoItem t : todos) {
			if (t.subtasks == null) t.subtasks = new ArrayList<>();
			if (t.text == null) t.text = "";
			if (t.coords == null) t.coords = "";
			if (t.category == null) t.category = categories.get(categories.size() - 1);
		}
		zoom.level = clamp(zoom.level, 1.5f, 50f);
		crosshair.size = Math.max(3, Math.min(31, crosshair.size));
		if (crosshair.pixels == null || crosshair.pixels.length() != crosshair.size * crosshair.size) {
			crosshair.size = 15;
			crosshair.pixels = CrosshairPresets.vanilla(15);
		}
	}

	public static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}
