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
			if (design == null || design.equals(Cosmetics.NONE)) {
				Files.deleteIfExists(target);
			} else {
				byte[] png;
				if (design.equals(Cosmetics.CUSTOM)) {
					Path custom = Config.cosmeticsDir().resolve(Cosmetics.Kind.TOTEM.customFile);
					if (!Files.exists(custom)) return "Put totem.png into the cosmetics folder first";
					png = Files.readAllBytes(custom);
				} else {
					try (InputStream in = TexturePack.class.getResourceAsStream("/assets/vanillaclient/textures/cosmetics/totem_" + design + ".png")) {
						if (in == null) return "Design not found";
						png = in.readAllBytes();
					}
				}
				Files.createDirectories(target.getParent());
				Files.write(target, png);
			}
			enableAndReload();
			return null;
		} catch (IOException e) {
			Config.LOG.warn("Texture pack could not be written", e);
			return "Could not write the texture pack";
		}
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
