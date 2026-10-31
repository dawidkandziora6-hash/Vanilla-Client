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
		CAPE("cape", "Capes", "cape.png"), WINGS("wings", "Wings", "wings.png"), SHIELD("shield", "Shields", "shield.png");

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
			new Design("crimson", "Crimson Knight", "Sword & crown", 0xFFC8202C),
			new Design("midnight", "Midnight", "✦ Twinkling stars", 0xFF1E285F),
			new Design("aurora", "Aurora", "✦ Dancing lights", 0xFF3CC8AA),
			new Design("inferno", "Inferno", "✦ Living flames", 0xFFE04010),
			new Design("galaxy", "Galaxy", "✦ Spinning spiral", 0xFF6A3CD0),
			new Design("ocean", "Ocean", "✦ Waves & trident", 0xFF2896D2),
			new Design("ender", "Ender", "✦ Watching eye", 0xFF2A1240),
			new Design("storm", "Thunderstorm", "✦ Lightning flashes", 0xFF4A5266),
			new Design("magma", "Magma", "✦ Glowing cracks", 0xFFFF7A1A),
			new Design("frost", "Frostbite", "✦ Snow & icicles", 0xFF9CC6E8),
			new Design("cherry", "Cherry Blossom", "✦ Falling petals", 0xFFFF9EC3),
			new Design("diamond", "Diamond", "✦ Sparkling gem", 0xFF6EE6E6),
			new Design("sun", "Solar", "✦ Turning rays", 0xFFFFB020),
			new Design("toxic", "Toxic", "✦ Bubbling goo", 0xFF7AFF2A),
			new Design("redstone", "Redstone", "✦ Pulsing circuit", 0xFFFF2A1A),
			new Design("rift", "Void Rift", "✦ Pulsing crack", 0xFF8C3CDC),
			new Design("phantom", "Phantom", "✦ Glowing eyes", 0xFF5A6A90),
			new Design("dragon", "Dragon Scale", "✦ Shifting colors", 0xFF5A1A7A),
			new Design("prism", "Prism", "✦ Rainbow flow", 0xFFE070E0),
			new Design("forest", "Forest", "✦ Fireflies", 0xFF3C8C3C),
			new Design("royal", "Royal", "Crown & ermine", 0xFF7828A0),
			new Design("creeper", "Creeper", "Sss…", 0xFF3A8A2A),
			new Design("honey", "Honey", "Dripping comb", 0xFFFFC03A),
			new Design("wanderer", "Wanderer", "Torn & patched", 0xFF7A6046),
			new Design(CUSTOM, "Your Image", "cosmetics/cape.png", 0xFFFFFFFF));

	public static final List<Design> WINGS = List.of(
			new Design(NONE, "Vanilla", "Normal elytra", 0xFF555555),
			new Design("angel", "Angel", "Soft feathers", 0xFFF5F5F0),
			new Design("archangel", "Archangel", "✦ Golden glow", 0xFFF6C945),
			new Design("phoenix", "Phoenix", "✦ Burning feathers", 0xFFFFB030),
			new Design("enderdragon", "Ender Dragon", "✦ Purple energy", 0xFFC050FF),
			new Design("demon", "Demon", "✦ Lava veins", 0xFFA0140A),
			new Design("crystal", "Crystal", "✦ Amethyst shards", 0xFFC89AFF),
			new Design("frost", "Frost", "✦ Icicle edge", 0xFF9ACFF0),
			new Design("storm", "Storm", "✦ Lightning", 0xFF5A6276),
			new Design("fairy", "Fairy", "✦ Shimmering", 0xFFE0B0FF),
			new Design("phantom", "Phantom", "✦ Glowing veins", 0xFF5A6A8C),
			new Design("void", "Void", "✦ Starry night", 0xFF24103A),
			new Design("mech", "Mech", "✦ Redstone core", 0xFFA8ACB4),
			new Design("prism", "Prism", "✦ Rainbow", 0xFFE070E0),
			new Design("dragon", "Dragon", "Bony membrane", 0xFF7A2AA8),
			new Design("bat", "Bat", "Leather wings", 0xFF4A4250),
			new Design("monarch", "Monarch", "Butterfly", 0xFFFF9620),
			new Design("vine", "Overgrown", "Leaves & vines", 0xFF5AA040),
			new Design(CUSTOM, "Your Image", "cosmetics/wings.png", 0xFFFFFFFF));

	public static final List<Design> SHIELDS = List.of(
			new Design(NONE, "Vanilla", "Normal shield", 0xFF555555),
			new Design("knight", "Knight", "Heater & cross", 0xFFC8232D),
			new Design("netherite", "Netherite", "✦ Gold-rimmed", 0xFF4A4246),
			new Design("inferno", "Inferno", "✦ Flame crest", 0xFFD22A0A),
			new Design("endereye", "Ender Eye", "✦ Pulsing eye", 0xFF1D8F74),
			new Design("galaxy", "Galaxy", "✦ Round spiral", 0xFF5A3CD0),
			new Design("sun", "Solar", "✦ Turning rays", 0xFFFFB020),
			new Design("emerald", "Emerald", "✦ Glinting gem", 0xFF28AA5A),
			new Design("frost", "Frost", "✦ Ice crystal", 0xFFC8EBFF),
			new Design("obsidian", "Obsidian", "✦ Glowing runes", 0xFF3A2050),
			new Design("ocean", "Ocean", "✦ Waves & trident", 0xFF2896D2),
			new Design("amethyst", "Amethyst", "✦ Crystal spade", 0xFFC89AFF),
			new Design("royal", "Royal", "Kite & crown", 0xFF2846AA),
			new Design("spartan", "Spartan", "Bronze lambda", 0xFFE0A040),
			new Design("viking", "Viking", "Round & painted", 0xFFC83A2A),
			new Design("buckler", "Buckler", "Wood & iron", 0xFFA8783E),
			new Design("dragon", "Dragon", "Scaled", 0xFF2A8A3A),
			new Design("tower", "Tower", "Iron-banded", 0xFF78787F),
			new Design("creeper", "Creeper", "Sss…", 0xFF3A8A2A),
			new Design(CUSTOM, "Your Image", "cosmetics/shield.png", 0xFFFFFFFF));

	/** A registered design. width/height are one frame; animated designs are vertical strips. */
	public record Tex(Identifier id, int width, int height, int frames, ClientAsset.Texture asset) {}

	/** Animated texture: all frames kept in memory, the current one is copied into the GPU texture. */
	private static final class Anim {
		final DynamicTexture tex;
		int[] strip;
		final int w, h, frames;
		long lastUsed = System.currentTimeMillis();

		Anim(DynamicTexture tex, int[] strip, int w, int h, int frames) {
			this.tex = tex;
			this.strip = strip;
			this.w = w;
			this.h = h;
			this.frames = frames;
		}
	}

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
		};
	}

	public static String selected(Kind k) {
		Config.Cosmetics c = Config.get().cosmetics;
		return switch (k) {
			case CAPE -> c.cape;
			case WINGS -> c.wings;
			case SHIELD -> c.shield;
		};
	}

	public static void select(Kind k, String id) {
		Config.Cosmetics c = Config.get().cosmetics;
		switch (k) {
			case CAPE -> c.cape = id;
			case WINGS -> c.wings = id;
			case SHIELD -> c.shield = id;
		}
		Config.save();
	}

	private static String effective(Kind k) {
		String p = switch (k) {
			case CAPE -> previewCape;
			case WINGS -> previewWings;
			case SHIELD -> previewShield;
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

	/** Built-in design texture for another player (never their own custom PNG). */
	public static ClientAsset.Texture assetOf(Kind k, String id) {
		if (id == null || id.equals(NONE) || id.equals(CUSTOM)) return null;
		Tex t = texture(k, id);
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
		if (cached != null) {
			Anim an = ANIMS.get(key);
			if (an != null) {
				an.lastUsed = System.currentTimeMillis();
				if (an.strip == null) an.strip = reloadStrip(kind, id, an);
			}
			return cached;
		}
		if (FAILED.contains(key)) return null;
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

	/** Called every client tick: designs in use advance at 10 frames per second; unused ones sleep. */
	public static void tick() {
		if (++ticks % 2 != 0 || ANIMS.isEmpty()) return;
		int step = ticks / 2;
		long now = System.currentTimeMillis();
		for (Anim a : ANIMS.values()) {
			if (a.strip == null) continue;
			if (now - a.lastUsed > 3000) {
				a.strip = null; // free the frames, keep the current picture
				continue;
			}
			NativeImage px = a.tex.getPixels();
			if (px == null) continue;
			int base = (step % a.frames) * a.h * a.w;
			for (int y = 0; y < a.h; y++) {
				for (int x = 0; x < a.w; x++) px.setPixel(x, y, a.strip[base + y * a.w + x]);
			}
			a.tex.upload();
		}
	}

	private static int[] reloadStrip(Kind kind, String id, Anim a) {
		try (InputStream in = open(kind, id)) {
			if (in == null) return null;
			try (NativeImage img = NativeImage.read(in)) {
				if (img.getWidth() != a.w || img.getHeight() != a.h * a.frames) return null;
				int[] strip = new int[a.w * a.h * a.frames];
				for (int y = 0; y < img.getHeight(); y++) {
					for (int x = 0; x < a.w; x++) strip[y * a.w + x] = img.getPixel(x, y);
				}
				return strip;
			}
		} catch (Exception e) {
			return null;
		}
	}

	private static InputStream open(Kind kind, String id) throws java.io.IOException {
		if (id.equals(CUSTOM)) {
			Path p = Config.cosmeticsDir().resolve(kind.customFile);
			return Files.exists(p) ? Files.newInputStream(p) : null;
		}
		return Cosmetics.class.getResourceAsStream("/assets/vanillaclient/textures/cosmetics/" + kind.prefix + "_" + id + ".png");
	}

	/** Vanilla elytra texture (used when you wear a cape design but "Vanilla" wings). */
	public static final ClientAsset.Texture VANILLA_ELYTRA = new ClientAsset.ResourceTexture(
			Identifier.withDefaultNamespace("entity/equipment/wings/elytra"),
			Identifier.withDefaultNamespace("textures/entity/equipment/wings/elytra.png"));

	/** True for textures this mod made (they may have see-through, shaped edges). */
	public static boolean isOurs(Identifier tex) {
		return tex != null && "vanillaclient".equals(tex.getNamespace());
	}
}
