package dev.vanillaclient.cosmetics;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.NativeImage;
import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

/**
 * Capes, wings (elytra skin) and shield skins. Client-side only:
 * the cape hides when you wear an elytra, wings only show with an elytra,
 * the shield skin only shows on plain shields (banners win).
 */
public final class Cosmetics {
	public enum Kind {
		CAPE("cape", "Capes", "cape.png"), WINGS("wings", "Wings", "wings.png"), SHIELD("shield", "Shields", "shield.png"),
		TOTEM("totem", "Totems", "totem.png");

		public final String prefix;
		public final String title;
		public final String customFile;

		Kind(String prefix, String title, String customFile) {
			this.prefix = prefix;
			this.title = title;
			this.customFile = customFile;
		}
	}

	/** One design: id (texture name), display name, short description, accent color. */
	public record Design(String id, String name, String desc, int color) {}

	public static final String NONE = "none";
	public static final String CUSTOM = "custom";

	public static final List<Design> CAPES = List.of(
			new Design(NONE, "None", "No cape", 0xFF555555),
			new Design("crimson", "Crimson Knight", "Gold trim & sword", 0xFFC8202C),
			new Design("midnight", "Midnight", "Moon & stars", 0xFF1E285F),
			new Design("aurora", "Aurora", "✦ Animated", 0xFF3CC8AA),
			new Design("ender", "Ender", "The watching eye", 0xFF1E122D),
			new Design("forest", "Forest", "Pine & hills", 0xFF3C8C3C),
			new Design("royal", "Royal", "Crown & ermine", 0xFF7828A0),
			new Design("ocean", "Ocean", "Rolling waves", 0xFF2896D2),
			new Design("cherry", "Cherry Blossom", "Falling petals", 0xFFFF9EC3),
			new Design("inferno", "Inferno", "Rising flames", 0xFFE04010),
			new Design("diamond", "Diamond", "Shiny gem", 0xFF6EE6E6),
			new Design(CUSTOM, "Your Image", "cosmetics/cape.png", 0xFFFFFFFF));

	public static final List<Design> WINGS = List.of(
			new Design(NONE, "Vanilla", "Normal elytra", 0xFF555555),
			new Design("angel", "Angel", "Soft white feathers", 0xFFF5F5F0),
			new Design("dragon", "Dragon", "Purple scales", 0xFF5F1E82),
			new Design("phoenix", "Phoenix", "Fire feathers", 0xFFFFB030),
			new Design("frost", "Frost", "Icy blue", 0xFF9ACFF0),
			new Design("phantom", "Phantom", "Glowing veins", 0xFF5A6A8C),
			new Design("void", "Void", "Starry night", 0xFF190A28),
			new Design("monarch", "Monarch", "Butterfly", 0xFFFF9620),
			new Design(CUSTOM, "Your Image", "cosmetics/wings.png", 0xFFFFFFFF));

	public static final List<Design> SHIELDS = List.of(
			new Design(NONE, "Vanilla", "Normal shield", 0xFF555555),
			new Design("knight", "Knight", "Red & gold cross", 0xFFC8232D),
			new Design("oak", "Iron Oak", "Planks & iron", 0xFF966E41),
			new Design("royal", "Royal", "Blue crown", 0xFF2846AA),
			new Design("emerald", "Emerald", "Green gem", 0xFF28AA5A),
			new Design("frost", "Frost", "Snowflake", 0xFFC8EBFF),
			new Design("obsidian", "Obsidian", "Purple runes", 0xFF28163C),
			new Design("inferno", "Inferno", "Flames", 0xFFD22A0A),
			new Design(CUSTOM, "Your Image", "cosmetics/shield.png", 0xFFFFFFFF));

	public static final List<Design> TOTEMS = List.of(
			new Design(NONE, "Vanilla", "Normal totem", 0xFF555555),
			new Design("king", "Golden King", "Shiny gold", 0xFFF5C846),
			new Design("diamond", "Diamond", "Ice-cold blue", 0xFF6EE6E6),
			new Design("emerald", "Emerald", "Villager's favorite", 0xFF3CC864),
			new Design("ender", "Ender", "Glowing eyes", 0xFF5A3C8C),
			new Design("blood", "Blood Moon", "Deep red", 0xFFC81E28),
			new Design("frost", "Frost", "Snow white", 0xFFDCF0FF),
			new Design(CUSTOM, "Your Image", "cosmetics/totem.png", 0xFFFFFFFF));

	public record Tex(Identifier id, int width, int height, ClientAsset.Texture asset) {}

	private static final Map<String, Tex> CACHE = new HashMap<>();
	private static final Set<String> FAILED = new HashSet<>();
	private static int generation;

	/** Temporary design shown while hovering in the wardrobe (null = use the saved one). */
	public static String previewCape, previewWings, previewShield;

	// animated aurora cape
	private static DynamicTexture auroraTex;
	private static int[] auroraBase;
	private static int auroraW, auroraH;

	private Cosmetics() {}

	public static List<Design> designs(Kind k) {
		return switch (k) {
			case CAPE -> CAPES;
			case WINGS -> WINGS;
			case SHIELD -> SHIELDS;
			case TOTEM -> TOTEMS;
		};
	}

	public static String selected(Kind k) {
		Config.Cosmetics c = Config.get().cosmetics;
		return switch (k) {
			case CAPE -> c.cape;
			case WINGS -> c.wings;
			case SHIELD -> c.shield;
			case TOTEM -> c.totem;
		};
	}

	public static void select(Kind k, String id) {
		Config.Cosmetics c = Config.get().cosmetics;
		switch (k) {
			case CAPE -> c.cape = id;
			case WINGS -> c.wings = id;
			case SHIELD -> c.shield = id;
			case TOTEM -> c.totem = id;
		}
		Config.save();
	}

	private static String effective(Kind k) {
		String p = switch (k) {
			case CAPE -> previewCape;
			case WINGS -> previewWings;
			case SHIELD -> previewShield;
			case TOTEM -> null;
		};
		String id = p != null ? p : selected(k);
		return id == null ? NONE : id;
	}

	public static ClientAsset.Texture capeTexture() {
		Tex t = texture(Kind.CAPE, effective(Kind.CAPE));
		return t == null ? null : t.asset();
	}

	public static ClientAsset.Texture wingsTexture() {
		Tex t = texture(Kind.WINGS, effective(Kind.WINGS));
		return t == null ? null : t.asset();
	}

	public static Identifier shieldTexture() {
		Tex t = texture(Kind.SHIELD, effective(Kind.SHIELD));
		return t == null ? null : t.id();
	}

	public static boolean customExists(Kind k) {
		return Files.exists(Config.cosmeticsDir().resolve(k.customFile));
	}

	/** Re-read your own images (after replacing the file). */
	public static void reloadCustom() {
		generation++;
		CACHE.keySet().removeIf(key -> key.endsWith(":" + CUSTOM));
		FAILED.removeIf(key -> key.endsWith(":" + CUSTOM));
	}

	/** Texture of a design (registered on first use). Render thread only. */
	public static Tex texture(Kind kind, String id) {
		if (id == null || id.equals(NONE)) return null;
		String key = kind.prefix + ":" + id;
		Tex cached = CACHE.get(key);
		if (cached != null || FAILED.contains(key)) return cached;
		NativeImage img = null;
		try {
			if (id.equals(CUSTOM)) {
				Path p = Config.cosmeticsDir().resolve(kind.customFile);
				if (Files.exists(p)) {
					try (InputStream in = Files.newInputStream(p)) {
						img = NativeImage.read(in);
					}
				}
			} else {
				try (InputStream in = Cosmetics.class.getResourceAsStream("/assets/vanillaclient/textures/cosmetics/" + kind.prefix + "_" + id + ".png")) {
					if (in != null) img = NativeImage.read(in);
				}
			}
		} catch (Exception e) {
			Config.LOG.warn("Cosmetic {} could not be loaded", key, e);
		}
		if (img == null) {
			FAILED.add(key);
			return null;
		}
		String suffix = id.equals(CUSTOM) ? "_" + generation : "";
		Identifier assetId = Identifier.fromNamespaceAndPath("vanillaclient", kind.prefix + "/" + id + suffix);
		Identifier texPath = Identifier.fromNamespaceAndPath("vanillaclient", "cosmetic/" + kind.prefix + "_" + id + suffix + ".png");
		int w = img.getWidth();
		int h = img.getHeight();
		if (kind == Kind.CAPE && id.equals("aurora")) {
			auroraW = w;
			auroraH = h;
			auroraBase = new int[w * h];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) auroraBase[y * w + x] = img.getPixel(x, y);
			}
		}
		DynamicTexture dyn = new DynamicTexture(() -> key, img);
		Minecraft.getInstance().getTextureManager().register(texPath, dyn);
		if (kind == Kind.CAPE && id.equals("aurora")) auroraTex = dyn;
		Tex t = new Tex(texPath, w, h, new ClientAsset.ResourceTexture(assetId, texPath));
		CACHE.put(key, t);
		return t;
	}

	/** Called every client tick: animates the aurora cape (hue drift). */
	public static void tick() {
		if (auroraTex == null || auroraBase == null) return;
		if (!"aurora".equals(effective(Kind.CAPE))) return;
		NativeImage px = auroraTex.getPixels();
		if (px == null) return;
		float t = (System.nanoTime() / 1_000_000_000f) * 0.25f;
		for (int y = 0; y < auroraH; y++) {
			for (int x = 0; x < auroraW; x++) {
				int c = auroraBase[y * auroraW + x];
				int a = (c >>> 24) & 0xFF;
				if (a == 0) continue;
				px.setPixel(x, y, shiftHue(c, t + y * 0.01f));
			}
		}
		auroraTex.upload();
	}

	private static int shiftHue(int argb, float amount) {
		float r = ((argb >> 16) & 0xFF) / 255f;
		float g = ((argb >> 8) & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float d = max - min;
		float h = 0f;
		if (d > 0f) {
			if (max == r) h = ((g - b) / d) % 6f;
			else if (max == g) h = (b - r) / d + 2f;
			else h = (r - g) / d + 4f;
			h /= 6f;
		}
		float s = max == 0f ? 0f : d / max;
		h = (h + amount) % 1f;
		if (h < 0) h += 1f;
		int rgb = java.awt.Color.HSBtoRGB(h, s, max);
		return (argb & 0xFF000000) | (rgb & 0x00FFFFFF);
	}
}
