package dev.aatracker;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * All Advancements Tracker: pressing L (advancements) opens a tracker with every
 * advancement as an icon grid that ticks itself off, plus checklists for the
 * multi-part ones. Minecraft's own screen is one click away.
 */
public final class AATracker implements ClientModInitializer {
	private static boolean openNext;
	private static final boolean VC = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("vanillaclient");

	@Override
	public void onInitializeClient() {
		net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) ->
				dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("aatracker").executes(c -> {
					openNext = true; // open after the chat screen has closed
					return 1;
				})));
		// with Vanilla Client installed, it already does the chat count + sound
		net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.MODIFY_GAME.register((msg, overlay) -> overlay || VC ? msg : AdvancementProgressChat.modify(msg));
		KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("aatracker", "main"));
		KeyMapping open = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.aatracker.open", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), cat));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (open.consumeClick()) {
				if (client.gui.screen() == null) client.setScreenAndShow(new TrackerScreen(null));
			}
			if (openNext && !(client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen)) {
				openNext = false;
				client.setScreenAndShow(new TrackerScreen(null));
			}
			if (!VC) AdvancementProgressChat.tick(client);
		});
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof AdvancementsScreen)) return;
			if (!TrackerScreen.vanillaWanted) {
				client.execute(() -> {
					if (client.gui.screen() == screen) client.setScreenAndShow(new TrackerScreen(null));
				});
				return;
			}
			ScreenEvents.remove(screen).register(s -> TrackerScreen.vanillaWanted = false);
			Screens.getWidgets(screen).add(Button.builder(Component.literal("All Advancements"),
					b -> {
						TrackerScreen.vanillaWanted = false;
						client.setScreenAndShow(new TrackerScreen(null));
					}).bounds(4, 4, 110, 20).build());
		});
	}
}
