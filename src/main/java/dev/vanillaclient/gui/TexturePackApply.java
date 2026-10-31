package dev.vanillaclient.gui;

import dev.vanillaclient.cosmetics.TexturePack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Applies the totem texture and tells you if something went wrong. */
final class TexturePackApply {
	private TexturePackApply() {}

	static void run(String design) {
		String err = TexturePack.applyTotem(design);
		if (err != null && Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.sendSystemMessage(Component.literal("§c[Vanilla Client] " + err));
		}
	}
}
