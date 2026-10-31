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
			new Design("crimson", "Crimson Knight", "V-cut, gold trim", 0xFFC8202C),
			new Design("midnight", "Midnight", "✦ Twinkling stars", 0xFF1E285F),
			new Design("aurora", "Aurora", "✦ Flowing lights", 0xFF3CC8AA),
			new Design("inferno", "Inferno", "✦ Burning hem", 0xFFE04010),
			new Design("ocean", "Ocean", "✦ Rolling waves", 0xFF2896D2),
			new Design("ender", "Ender", "✦ Ragged, drifting", 0xFF1E122D),
			new Design("cherry", "Cherry Blossom", "✦ Falling petals", 0xFFFF9EC3),
			new Design("diamond", "Diamond", "✦ Sparkling point", 0xFF6EE6E6),
			new Design("prism", "Prism", "✦ Rainbow scroll", 0xFFE070E0),
			new Design("rift", "Void Rift", "✦ Pulsing crack", 0xFF8C3CDC),
			new Design("royal", "Royal", "Rounded, ermine", 0xFF7828A0),
			new Design("forest", "Forest", "Leafy hem", 0xFF3C8C3C),
			new Design("wanderer", "Wanderer", "Torn & patched", 0xFF7A6046),
			new Design(CUSTOM, "Your Image", "cosmetics/cape.png", 0xFFFFFFFF));

	public static final List<Design> WINGS = List.of(
			new Design(NONE, "Vanilla", "Normal elytra", 0xFF555555),
			new Design("angel", "Angel", "Feather tips", 0xFFF5F5F0),
			new Design("phoenix", "Phoenix", "✦ Flickering fire", 0xFFFFB030),
			new Design("dragon", "Dragon", "Bony membrane", 0xFF5F1E82),
			new Design("bat", "Bat", "Scalloped leather", 0xFF34303A),
			new Design("monarch", "Monarch", "Butterfly shape", 0xFFFF9620),
			new Design("fairy", "Fairy", "✦ Shimmering", 0xFFE0B0FF),
			new Design("frost", "Frost", "✦ Icicle edge", 0xFF9ACFF0),
			new Design("phantom", "Phantom", "✦ Glowing veins", 0xFF5A6A8C),
			new Design("void", "Void", "✦ Starry night", 0xFF190A28),
			new Design(CUSTOM, "Your Image", "cosmetics/wings.png", 0xFFFFFFFF));

	public static final List<Design> SHIELDS = List.of(
			new Design(NONE, "Vanilla", "Normal shield", 0xFF555555),
			new Design("knight", "Knight", "Heater shape", 0xFFC8232D),
			new Design("buckler", "Buckler", "Round & studded", 0xFF966E41),
			new Design("royal", "Royal", "Kite shape", 0xFF2846AA),
			new Design("emerald", "Emerald", "✦ Glinting gem", 0xFF28AA5A),
			new Design("frost", "Frost", "✦ Ice crystal", 0xFFC8EBFF),
			new Design("obsidian", "Obsidian", "✦ Pulsing runes", 0xFF28163C),
			new Design("inferno", "Inferno", "✦ Flame crest", 0xFFD22A0A),
			new Design("dragon", "Dragon", "Scaled heater", 0xFF46964A),
			new Design("tower", "Tower", "Iron-banded", 0xFF78787F),
			new Design(CUSTOM, "Your Image", "cosmetics/shield.png", 0xFFFFFFFF));

	public static final List<Design> TOTEMS = List.of(
			new Design(NONE, "Vanilla", "Normal totem", 0xFF555555),
			new Design("statue", "3D Statue", "◆ Real 3D golden model", 0xFFF5C846),
			new Design("king", "Golden King", "✦ Gem pulse", 0xFFF5C846),
			new Design("diamond", "Diamond", "✦ Glint", 0xFF6EE6E6),
			new Design("emerald", "Emerald", "✦ Glint", 0xFF3CC864),
			new Design("ender", "Ender", "✦ Glowing eyes", 0xFF5A3C8C),
			new Design("blood", "Blood Moon", "✦ Glowing eyes", 0xFFC81E28),
			new Design("frost", "Frost", "✦ Glint", 0xFFDCF0FF),
			new Design(CUSTOM, "Your Image", "cosmetics/totem.png", 0xFFFFFFFF));

	/** A registered design. width/height are one frame; animated designs are vertical strips. */
	public record Tex(Identifier id, int width, int height, int frames, ClientAsset.Texture asset) {}

	/** Animated texture: all frames kept in memory, the current one is copied into the GPU texture. */
	private record Anim(DynamicTexture tex, int[] strip, int w, int h, int frames) {}

	private static final Map<String, Anim> ANIMS = new HashMap<>();
	private static int ticks;

	private static final Map<String, Tex> CACHE = new HashMap<>();
	private static final Set<String> FAILED = new HashSet<>();
	private static int generation;

	/** Temporary design shown while hovering in the wardrobe (null = use the saved one). */
	public static String previewCape, previewWings, previewShield;

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
		ANIMS.keySet().removeIf(key -> key.endsWith(":" + CUSTOM));
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
		int frameH = (kind == Kind.CAPE || kind == Kind.WINGS) ? w / 2 : w;
		int frames = frameH > 0 && h > frameH && h % frameH == 0 ? h / frameH : 1;
		DynamicTexture dyn;
		if (frames > 1) {
			int[] strip = new int[w * h];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) strip[y * w + x] = img.getPixel(x, y);
			}
			NativeImage first = new NativeImage(w, frameH, false);
			for (int y = 0; y < frameH; y++) {
				for (int x = 0; x < w; x++) first.setPixel(x, y, strip[y * w + x]);
			}
			img.close();
			dyn = new DynamicTexture(() -> key, first);
			ANIMS.put(key, new Anim(dyn, strip, w, frameH, frames));
		} else {
			frameH = h;
			dyn = new DynamicTexture(() -> key, img);
		}
		Minecraft.getInstance().getTextureManager().register(texPath, dyn);
		Tex t = new Tex(texPath, w, frameH, frames, new ClientAsset.ResourceTexture(assetId, texPath));
		CACHE.put(key, t);
		return t;
	}

	/** Called every client tick: animated designs advance at 10 frames per second. */
	public static void tick() {
		if (++ticks % 2 != 0 || ANIMS.isEmpty()) return;
		int step = ticks / 2;
		for (Anim a : ANIMS.values()) {
			NativeImage px = a.tex().getPixels();
			if (px == null) continue;
			int base = (step % a.frames()) * a.h() * a.w();
			for (int y = 0; y < a.h(); y++) {
				for (int x = 0; x < a.w(); x++) px.setPixel(x, y, a.strip()[base + y * a.w() + x]);
			}
			a.tex().upload();
		}
	}

	/** True for textures this mod made (they may have see-through, shaped edges). */
	public static boolean isOurs(Identifier tex) {
		return tex != null && "vanillaclient".equals(tex.getNamespace());
	}
}
