package dev.vanillaclient.cosmetics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.vanillaclient.config.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Totem designs were removed from the wardrobe. This cleans up the totem files an
 * older version put into the "Vanilla Client Textures" pack, so the normal totem is back.
 */
public final class TexturePack {
	private static final String[] OLD_FILES = {
			"assets/minecraft/textures/item/totem_of_undying.png",
			"assets/minecraft/textures/item/totem_of_undying.png.mcmeta",
			"assets/minecraft/models/item/totem_of_undying.json"};

	private TexturePack() {}

	public static void cleanUpOldTotem() {
		Config.Cosmetics c = Config.get().cosmetics;
		Path root = FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve("Vanilla Client Textures");
		boolean changed = false;
		for (String f : OLD_FILES) {
			try {
				changed |= Files.deleteIfExists(root.resolve(f));
			} catch (IOException e) {
				Config.LOG.warn("Could not remove old totem file {}", f, e);
			}
		}
		if (!"none".equals(c.totem)) {
			c.totem = "none";
			Config.save();
		}
		if (changed) Minecraft.getInstance().reloadResourcePacks();
	}
}
