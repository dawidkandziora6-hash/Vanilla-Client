package dev.logbuch.cosmetics;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.NativeImage;
import dev.logbuch.config.LogbuchConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

/**
 * Umhänge und Flügel (Elytra-Design) – nur clientseitig, nur für dich sichtbar.
 * "eigenes" lädt umhang.png / fluegel.png aus config/logbuch/kosmetik.
 */
public final class Cosmetics {
	public static final List<String> CAPES = List.of("none", "rot", "mitternacht", "ender", "wald", "gold", "eigenes");
	public static final List<String> WINGS = List.of("none", "feder", "drache", "phantom", "feuer", "eis", "eigenes");
	public static final List<String> SHIELDS = List.of("none", "eiche", "eisen", "obsidian", "gold", "smaragd", "eigenes");

	public static final String CUSTOM = "eigenes";
	public static final String CUSTOM_CAPE_FILE = "umhang.png";
	public static final String CUSTOM_WINGS_FILE = "fluegel.png";
	public static final String CUSTOM_SHIELD_FILE = "schild.png";

	private static final Map<String, ClientAsset.Texture> CACHE = new HashMap<>();
	private static final Set<String> FAILED = new HashSet<>();
	private static int generation;

	private Cosmetics() {}

	public static String label(String name) {
		if (name == null || name.equals("none")) return "Aus";
		if (name.equals(CUSTOM)) return "Eigenes Bild";
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	public static ClientAsset.Texture capeTexture() {
		String n = LogbuchConfig.get().cosmetics.cape;
		return n == null || n.equals("none") ? null : get("cape", n, CUSTOM_CAPE_FILE);
	}

	public static ClientAsset.Texture wingsTexture() {
		String n = LogbuchConfig.get().cosmetics.wings;
		return n == null || n.equals("none") ? null : get("wings", n, CUSTOM_WINGS_FILE);
	}

	/** Eigene Bilder neu einlesen (nach Austausch der Datei). */
	public static void reloadCustom() {
		generation++;
		CACHE.keySet().removeIf(k -> k.endsWith(":" + CUSTOM));
		FAILED.clear();
	}

	private static ClientAsset.Texture get(String kind, String name, String customFile) {
		String key = kind + ":" + name;
		ClientAsset.Texture cached = CACHE.get(key);
		if (cached != null || FAILED.contains(key)) return cached;
		NativeImage img = null;
		try {
			if (name.equals(CUSTOM)) {
				Path p = LogbuchConfig.cosmeticsDir().resolve(customFile);
				if (Files.exists(p)) {
					try (InputStream in = Files.newInputStream(p)) {
						img = NativeImage.read(in);
					}
				}
			} else {
				try (InputStream in = Cosmetics.class.getResourceAsStream("/assets/logbuch/textures/cosmetics/" + kind + "_" + name + ".png")) {
					if (in != null) img = NativeImage.read(in);
				}
			}
		} catch (Exception e) {
			LogbuchConfig.LOG.warn("Kosmetik {} konnte nicht geladen werden", key, e);
		}
		if (img == null) {
			FAILED.add(key);
			return null;
		}
		String suffix = name.equals(CUSTOM) ? "_" + generation : "";
		Identifier id = Identifier.fromNamespaceAndPath("logbuch", kind + "/" + name + suffix);
		Identifier texPath = Identifier.fromNamespaceAndPath("logbuch", "cosmetic/" + kind + "_" + name + suffix + ".png");
		final String debugName = key;
		Minecraft.getInstance().getTextureManager().register(texPath, new DynamicTexture(() -> debugName, img));
		ClientAsset.Texture tex = new ClientAsset.ResourceTexture(id, texPath);
		CACHE.put(key, tex);
		return tex;
	}
}
