package dev.vanillaclient;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vanillaclient.config.Config;
import dev.vanillaclient.cosmetics.Cosmetics;
import dev.vanillaclient.crosshair.CrosshairRenderer;
import dev.vanillaclient.gui.HudEditorScreen;
import dev.vanillaclient.gui.Icons;
import dev.vanillaclient.gui.SettingsScreen;
import dev.vanillaclient.hud.HudManager;
import dev.vanillaclient.images.ImageManager;
import dev.vanillaclient.zoom.Zoom;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/**
 * Vanilla Client – client-side only. No packets are sent and no game values are
 * changed; everything is display or cosmetics just for you.
 */
public final class VanillaClient implements ClientModInitializer {
	public static final String MOD_ID = "vanillaclient";

	@Override
	public void onInitializeClient() {
		Config.load();
		ImageManager.scan();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		KeyMapping zoom = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.zoom", InputConstants.Type.KEYBOARD, InputConstants.KEY_C, category));
		KeyMapping openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.open", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, category));
		KeyMapping hudEditor = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.hud_editor", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), category));
		KeyMapping toggleTodo = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.toggle_todo", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), category));
		KeyMapping mapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.map", InputConstants.Type.KEYBOARD, InputConstants.KEY_M, category));
		Zoom.init(zoom);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenu.consumeClick()) {
				if (client.gui.screen() == null) client.setScreenAndShow(new SettingsScreen(null));
			}
			while (hudEditor.consumeClick()) {
				if (client.gui.screen() == null) client.setScreenAndShow(new HudEditorScreen(null));
			}
			while (toggleTodo.consumeClick()) {
				Config.Todo t = Config.get().todo;
				t.enabled = !t.enabled;
				Config.save();
			}
			while (mapKey.consumeClick()) {
				if (client.gui.screen() == null) client.setScreenAndShow(new dev.vanillaclient.gui.WorldMapScreen(null));
			}
			Cosmetics.tick();
			dev.vanillaclient.map.WorldMap.tick(client);
		});

		// Our HUD elements sit below the chat.
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudManager::render);
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, CrosshairRenderer::wrap);

		ScreenEvents.AFTER_INIT.register(VanillaClient::afterScreenInit);

		// Developer check (CI only): force-apply every mixin, then quit.
		if (Boolean.getBoolean("vanillaclient.audit")) {
			net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
				org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
				Config.LOG.info("VANILLACLIENT AUDIT OK");
				Runtime.getRuntime().halt(0);
			});
		}
	}

	private static boolean isPauseScreen(Screen screen) {
		String n = screen.getClass().getSimpleName();
		return n.equals("PauseScreen") || n.equals("GameMenuScreen");
	}

	private static boolean isOptionsButton(AbstractWidget w) {
		return w.getMessage().getContents() instanceof TranslatableContents tc && tc.getKey().equals("menu.options");
	}

	/** New button in the pause menu, left of "Options…" – like the small vanilla icon buttons. */
	private static void afterScreenInit(Minecraft client, Screen screen, int w, int h) {
		if (screen instanceof net.minecraft.client.gui.screens.options.OptionsScreen) {
			addGuideButton(client, screen);
			return;
		}
		if (!Config.get().pauseMenuButton || !isPauseScreen(screen)) return;
		List<AbstractWidget> widgets = Screens.getWidgets(screen);
		if (widgets.isEmpty()) return; // pause menu hidden with F3+Esc
		AbstractWidget options = null;
		for (AbstractWidget wd : widgets) {
			if (isOptionsButton(wd)) {
				options = wd;
				break;
			}
		}
		Button button;
		if (options != null) {
			int bx = options.getX() - 24;
			int by = options.getY();
			button = Button.builder(Component.empty(), b -> client.setScreenAndShow(new SettingsScreen(screen)))
					.bounds(bx, by, 20, 20)
					.tooltip(Tooltip.create(Component.literal("Vanilla Client")))
					.build();
			ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> g.item(Icons.stack(Items.WRITABLE_BOOK), bx + 2, by + 2));
		} else {
			button = Button.builder(Component.literal("Vanilla Client"), b -> client.setScreenAndShow(new SettingsScreen(screen)))
					.bounds(4, 4, 90, 20)
					.build();
		}
		widgets.add(button);
	}

	/** Small "UHC Guide" button in Minecraft's own Options screen (top left). */
	private static void addGuideButton(Minecraft client, Screen screen) {
		Button button = Button.builder(Component.empty(), b -> client.setScreenAndShow(new dev.vanillaclient.gui.GuideScreen(screen)))
				.bounds(6, 6, 20, 20)
				.tooltip(Tooltip.create(Component.literal("UHC Guide – villagers, enchants, brewing")))
				.build();
		Screens.getWidgets(screen).add(button);
		ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> g.item(Icons.stack(Items.ENCHANTED_BOOK), 8, 8));
	}
}
