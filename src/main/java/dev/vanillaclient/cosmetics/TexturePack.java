package dev.vanillaclient.cosmetics;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.vanillaclient.config.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

/**
 * Item textures (custom totem) through a normal resource pack that the mod
 * creates, switches on and reloads by itself – you never have to pick it.
 */
public final class TexturePack {
	public static final String FOLDER = "Vanilla Client Textures";
	public static final String PACK_ID = "file/" + FOLDER;
	private static final String TOTEM = "assets/minecraft/textures/item/totem_of_undying.png";
	private static final String TOTEM_META = TOTEM + ".mcmeta";
	private static final String TOTEM_MODEL = "assets/minecraft/models/item/totem_of_undying.json";

	/** 3D statue: base, body, gem, raised arms and a head with a face on front and back. */
	private static final String STATUE_MODEL = """
			{
			  "textures": {"t": "minecraft:item/totem_of_undying", "particle": "minecraft:item/totem_of_undying"},
			  "elements": [
			    {"from": [4, 0, 5], "to": [12, 2, 11], "faces": {
			      "north": {"uv": [8, 0, 16, 2], "texture": "#t"}, "south": {"uv": [8, 0, 16, 2], "texture": "#t"},
			      "east": {"uv": [8, 2, 14, 4], "texture": "#t"}, "west": {"uv": [8, 2, 14, 4], "texture": "#t"},
			      "up": {"uv": [8, 0, 16, 6], "texture": "#t"}, "down": {"uv": [8, 0, 16, 6], "texture": "#t"}}},
			    {"from": [5.5, 2, 6], "to": [10.5, 10, 10], "faces": {
			      "north": {"uv": [0, 6, 5, 8], "texture": "#t"}, "south": {"uv": [0, 6, 5, 8], "texture": "#t"},
			      "east": {"uv": [0, 6, 4, 8], "texture": "#t"}, "west": {"uv": [0, 6, 4, 8], "texture": "#t"},
			      "up": {"uv": [0, 6, 5, 8], "texture": "#t"}, "down": {"uv": [0, 6, 5, 8], "texture": "#t"}}},
			    {"from": [7, 5, 5.5], "to": [9, 7, 10.5], "faces": {
			      "north": {"uv": [9, 9, 11, 11], "texture": "#t"}, "south": {"uv": [9, 9, 11, 11], "texture": "#t"},
			      "east": {"uv": [9, 9, 11, 11], "texture": "#t"}, "west": {"uv": [9, 9, 11, 11], "texture": "#t"},
			      "up": {"uv": [9, 9, 11, 11], "texture": "#t"}, "down": {"uv": [9, 9, 11, 11], "texture": "#t"}}},
			    {"from": [2, 6, 7], "to": [5.5, 9, 9], "faces": {
			      "north": {"uv": [0, 8, 4, 11], "texture": "#t"}, "south": {"uv": [0, 8, 4, 11], "texture": "#t"},
			      "east": {"uv": [0, 8, 2, 11], "texture": "#t"}, "west": {"uv": [0, 8, 2, 11], "texture": "#t"},
			      "up": {"uv": [0, 8, 4, 10], "texture": "#t"}, "down": {"uv": [0, 8, 4, 10], "texture": "#t"}}},
			    {"from": [10.5, 6, 7], "to": [14, 9, 9], "faces": {
			      "north": {"uv": [0, 8, 4, 11], "texture": "#t"}, "south": {"uv": [0, 8, 4, 11], "texture": "#t"},
			      "east": {"uv": [0, 8, 2, 11], "texture": "#t"}, "west": {"uv": [0, 8, 2, 11], "texture": "#t"},
			      "up": {"uv": [0, 8, 4, 10], "texture": "#t"}, "down": {"uv": [0, 8, 4, 10], "texture": "#t"}}},
			    {"from": [2, 9, 7], "to": [4, 12, 9], "faces": {
			      "north": {"uv": [0, 8, 2, 11], "texture": "#t"}, "south": {"uv": [0, 8, 2, 11], "texture": "#t"},
			      "east": {"uv": [0, 8, 2, 11], "texture": "#t"}, "west": {"uv": [0, 8, 2, 11], "texture": "#t"},
			      "up": {"uv": [0, 8, 2, 10], "texture": "#t"}, "down": {"uv": [0, 8, 2, 10], "texture": "#t"}}},
			    {"from": [12, 9, 7], "to": [14, 12, 9], "faces": {
			      "north": {"uv": [0, 8, 2, 11], "texture": "#t"}, "south": {"uv": [0, 8, 2, 11], "texture": "#t"},
			      "east": {"uv": [0, 8, 2, 11], "texture": "#t"}, "west": {"uv": [0, 8, 2, 11], "texture": "#t"},
			      "up": {"uv": [0, 8, 2, 10], "texture": "#t"}, "down": {"uv": [0, 8, 2, 10], "texture": "#t"}}},
			    {"from": [5, 10, 5.5], "to": [11, 16, 10.5], "faces": {
			      "north": {"uv": [0, 0, 8, 8], "texture": "#t"}, "south": {"uv": [0, 0, 8, 8], "texture": "#t"},
			      "east": {"uv": [0, 6, 5, 8], "texture": "#t"}, "west": {"uv": [0, 6, 5, 8], "texture": "#t"},
			      "up": {"uv": [0, 6, 6, 8], "texture": "#t"}, "down": {"uv": [0, 6, 6, 8], "texture": "#t"}}}
			  ],
			  "display": {
			    "gui": {"rotation": [15, -25, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
			    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
			    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
			    "head": {"rotation": [0, 0, 0], "translation": [0, 10, 0], "scale": [0.8, 0.8, 0.8]},
			    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1.5], "scale": [0.5, 0.5, 0.5]},
			    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2.5, 1.5], "scale": [0.5, 0.5, 0.5]},
			    "firstperson_righthand": {"rotation": [0, -15, 5], "translation": [1, 2.5, 0], "scale": [0.55, 0.55, 0.55]},
			    "firstperson_lefthand": {"rotation": [0, 15, -5], "translation": [1, 2.5, 0], "scale": [0.55, 0.55, 0.55]}
			  }
			}
			""";

	private TexturePack() {}

	private static Path dir() {
		return FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(FOLDER);
	}

	/** Writes the totem texture (or removes it for "none") and reloads. Returns an error text or null. */
	public static String applyTotem(String design) {
		try {
			Path root = dir();
			Files.createDirectories(root);
			Files.writeString(root.resolve("pack.mcmeta"), """
					{
					  "pack": {
					    "description": "§6Vanilla Client§r – custom item textures",
					    "min_format": 97,
					    "max_format": 97
					  }
					}
					""", StandardCharsets.UTF_8);
			try (InputStream icon = TexturePack.class.getResourceAsStream("/assets/vanillaclient/icon.png")) {
				if (icon != null) Files.write(root.resolve("pack.png"), icon.readAllBytes());
			}
			Path target = root.resolve(TOTEM);
			Path meta = root.resolve(TOTEM_META);
			Path model = root.resolve(TOTEM_MODEL);
			Files.deleteIfExists(meta);
			Files.deleteIfExists(model);
			if (design == null || design.equals(Cosmetics.NONE)) {
				Files.deleteIfExists(target);
			} else {
				byte[] png;
				if (design.equals(Cosmetics.CUSTOM)) {
					Path custom = Config.cosmeticsDir().resolve(Cosmetics.Kind.TOTEM.customFile);
					if (!Files.exists(custom)) return "Put totem.png into the cosmetics folder first";
					png = Files.readAllBytes(custom);
				} else {
					String file = design.equals("statue") ? "totem_statue_model" : "totem_" + design;
					try (InputStream in = TexturePack.class.getResourceAsStream("/assets/vanillaclient/textures/cosmetics/" + file + ".png")) {
						if (in == null) return "Design not found";
						png = in.readAllBytes();
					}
				}
				Files.createDirectories(target.getParent());
				Files.write(target, png);
				// tall strips (height = n × width) become animated textures
				int[] size = pngSize(png);
				if (size != null && size[0] > 0 && size[1] > size[0] && size[1] % size[0] == 0) {
					Files.writeString(meta, "{\"animation\": {\"frametime\": 2}}", StandardCharsets.UTF_8);
				}
				if (design.equals("statue")) {
					Files.createDirectories(model.getParent());
					Files.writeString(model, STATUE_MODEL, StandardCharsets.UTF_8);
				}
			}
			enableAndReload();
			return null;
		} catch (IOException e) {
			Config.LOG.warn("Texture pack could not be written", e);
			return "Could not write the texture pack";
		}
	}

	/** Width and height from a PNG header (IHDR), or null. */
	private static int[] pngSize(byte[] png) {
		if (png.length < 24) return null;
		java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(png);
		return new int[] {b.getInt(16), b.getInt(20)};
	}

	private static void enableAndReload() {
		Minecraft mc = Minecraft.getInstance();
		PackRepository repo = mc.getResourcePackRepository();
		repo.reload();
		List<String> selected = new ArrayList<>(repo.getSelectedIds());
		if (!selected.contains(PACK_ID) && repo.isAvailable(PACK_ID)) {
			selected.add(PACK_ID); // last = highest priority
			repo.setSelected(selected);
			mc.options.updateResourcePacks(repo);
		} else {
			mc.reloadResourcePacks();
		}
	}
}
