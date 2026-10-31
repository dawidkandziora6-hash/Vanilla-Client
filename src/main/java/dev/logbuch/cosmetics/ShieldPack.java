package dev.logbuch.cosmetics;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import dev.logbuch.config.LogbuchConfig;
import net.minecraft.client.Minecraft;

/**
 * Schild-Design als ganz normales Ressourcenpaket ("Logbuch Kosmetik") –
 * der Vanilla-Weg, Texturen zu ändern. Das Design erscheint nur auf dem Schild-Item.
 * Banner-Muster auf dem Schild funktionieren weiterhin.
 */
public final class ShieldPack {
	public static final String FOLDER = "Logbuch Kosmetik";
	public static final String PACK_ID = "file/" + FOLDER;

	/** Mögliche Pfade der Schild-Grundtextur (alle werden geschrieben, überzählige ignoriert das Spiel). */
	private static final List<String> TARGETS = List.of(
			"assets/minecraft/textures/entity/shield_base_nopattern.png",
			"assets/minecraft/textures/entity/shield_base.png",
			"assets/minecraft/textures/entity/shield/shield_base_nopattern.png",
			"assets/minecraft/textures/entity/shield/shield_base.png",
			"assets/minecraft/textures/entity/shield/base_nopattern.png",
			"assets/minecraft/textures/entity/shield/base.png");

	private ShieldPack() {}

	public static Path dir() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(FOLDER);
	}

	public static boolean isActive() {
		return Minecraft.getInstance().options.resourcePacks.contains(PACK_ID);
	}

	/** Schreibt das Paket passend zur Auswahl. Rückgabe: Fehlermeldung oder null. */
	public static String write(String design) {
		Path root = dir();
		try {
			deleteTree(root.resolve("assets"));
			Files.createDirectories(root);
			Files.writeString(root.resolve("pack.mcmeta"), """
					{
					  "pack": {
					    "description": "§6Logbuch§r – Schild-Design",
					    "pack_format": 97,
					    "min_format": 97,
					    "max_format": 97
					  }
					}
					""", StandardCharsets.UTF_8);
			try (InputStream icon = ShieldPack.class.getResourceAsStream("/assets/logbuch/textures/cosmetics/pack_icon.png")) {
				if (icon != null) Files.write(root.resolve("pack.png"), icon.readAllBytes());
			}
			if (design == null || design.equals("none")) {
				return null;
			}
			byte[] png;
			if (design.equals(Cosmetics.CUSTOM)) {
				Path custom = LogbuchConfig.cosmeticsDir().resolve(Cosmetics.CUSTOM_SHIELD_FILE);
				if (!Files.exists(custom)) {
					return "Lege zuerst " + Cosmetics.CUSTOM_SHIELD_FILE + " (64×64) in den Kosmetik-Ordner.";
				}
				png = Files.readAllBytes(custom);
			} else {
				try (InputStream in = ShieldPack.class.getResourceAsStream("/assets/logbuch/textures/cosmetics/shield_" + design + ".png")) {
					if (in == null) return "Design nicht gefunden.";
					png = in.readAllBytes();
				}
			}
			for (String t : TARGETS) {
				Path p = root.resolve(t);
				Files.createDirectories(p.getParent());
				Files.write(p, png);
			}
			return null;
		} catch (IOException e) {
			LogbuchConfig.LOG.warn("Schild-Paket konnte nicht geschrieben werden", e);
			return "Konnte das Ressourcenpaket nicht schreiben.";
		}
	}

	private static void deleteTree(Path p) throws IOException {
		if (!Files.exists(p)) return;
		try (Stream<Path> s = Files.walk(p)) {
			for (Path x : s.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(x);
			}
		}
	}
}
