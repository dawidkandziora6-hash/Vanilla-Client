package dev.vanillaclient;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import dev.vanillaclient.config.Config;
import dev.vanillaclient.cosmetics.Cosmetics;
import dev.vanillaclient.crosshair.CrosshairRenderer;
import dev.vanillaclient.gui.HudEditorScreen;
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

/**
 * Vanilla Client – client-side only. No packets are sent and no game values are
 * changed; everything is display or cosmetics just for you.
 */
public final class VanillaClient implements ClientModInitializer {
	public static final String MOD_ID = "vanillaclient";
	/** Screen to open on the next tick (commands run while the chat is still closing). */
	private static volatile Runnable pending;

	@Override
	public void onInitializeClient() {
		Config.load();
		ImageManager.scan();
		dev.vanillaclient.features.ClientRanks.init();
		dev.vanillaclient.net.ClientSync.init();
		// /vcgallery [file] – opened from the screenshot chat message
		net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> {
			dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("vctracker").executes(c -> {
				pending = () -> Minecraft.getInstance().setScreenAndShow(new dev.vanillaclient.gui.AdvancementsChecklistScreen(null));
				return 1;
			}));
			dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("vcgallery")
					.executes(c -> {
						pending = () -> Minecraft.getInstance().setScreenAndShow(new dev.vanillaclient.gui.GalleryScreen(null));
						return 1;
					})
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("file", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
							.executes(c -> {
								String f = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "file");
								pending = () -> Minecraft.getInstance().setScreenAndShow(dev.vanillaclient.gui.GalleryScreen.showing(null, f));
								return 1;
							})));
		});

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
		KeyMapping waypointKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.waypoint", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, category));
		KeyMapping wardrobeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.cosmetics", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), category));
		KeyMapping tasksKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.tasks", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), category));
		KeyMapping sortKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.sort", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), category));
		KeyMapping hitboxKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.hitboxes", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), category));
		KeyMapping fullbrightKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vanillaclient.fullbright", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), category));
		Zoom.init(zoom);
		dev.vanillaclient.pvp.Combat.init();
		net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.MODIFY_GAME.register((msg, overlay) -> overlay ? msg : dev.vanillaclient.features.AdvancementProgressChat.modify(msg));
		net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback.EVENT.register(dev.vanillaclient.features.ContainerPreview::client);

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
			while (waypointKey.consumeClick()) {
				if (client.gui.screen() == null) dev.vanillaclient.map.Waypoints.addHere();
			}
			while (wardrobeKey.consumeClick()) {
				if (client.gui.screen() == null) client.setScreenAndShow(new dev.vanillaclient.gui.CosmeticsScreen(null));
			}
			while (hitboxKey.consumeClick()) {
				boolean nowOn = !dev.vanillaclient.pvp.Hitboxes.on();
				dev.vanillaclient.pvp.Hitboxes.set(nowOn);
				client.gui.hud.setOverlayMessage(Component.literal("Hitboxes " + (nowOn ? "§aon" : "§coff")), false);
			}
			while (fullbrightKey.consumeClick()) {
				Config.Pvp pv = Config.get().pvp;
				pv.fullbright = !pv.fullbright;
				Config.save();
				client.gui.hud.setOverlayMessage(Component.literal("Fullbright " + (pv.fullbright ? "§aon" : "§coff")), false);
			}
			while (tasksKey.consumeClick()) {
				if (client.gui.screen() == null) client.setScreenAndShow(new dev.vanillaclient.gui.TasksScreen(null));
			}
			if (pending != null && !(client.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen)) {
				Runnable r = pending;
				pending = null;
				r.run();
			}
			Cosmetics.tick();
			dev.vanillaclient.net.ClientSync.tick(client);
			dev.vanillaclient.map.WorldMap.tick(client);
			dev.vanillaclient.features.Ranks.tick(client);
			dev.vanillaclient.hud.InfoModules.tick(client);
			dev.vanillaclient.pvp.Combat.tick(client);
			dev.vanillaclient.visual.Glint.tick(client);
			dev.vanillaclient.features.AdvancementProgressChat.enabled = Config.get().advancementChat;
			dev.vanillaclient.features.AdvancementProgressChat.tick(client);
			dev.vanillaclient.features.EnderChestCache.tick(client);
		});
		// sort key also works while a chest is open (screens swallow normal key mappings)
		ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
			if (dev.vanillaclient.features.ChestTools.supported(screen)) {
				net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.afterKeyPress(screen).register((s, ev) -> {
					if (sortKey.matches(ev)) dev.vanillaclient.features.ChestTools.sortOpen();
				});
			}
		});
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(
				c -> dev.vanillaclient.cosmetics.TexturePack.cleanUpOldTotem());

		// Our HUD elements sit below the chat.
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudManager::render);
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, CrosshairRenderer::wrap);
		HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, dev.vanillaclient.hud.ScoreboardModule::wrap);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "waypoints"), dev.vanillaclient.hud.WaypointBar::render);
		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "screenshot"), dev.vanillaclient.images.Screenshots::renderPopup);

		ScreenEvents.AFTER_INIT.register(VanillaClient::afterScreenInit);

		// Developer check (CI only): force-apply every mixin, then quit.
		if (Boolean.getBoolean("vanillaclient.audit")) {
			net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
				org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
				Config.LOG.info("VANILLACLIENT AUDIT MIXINS OK");
			});
			// then open every menu for a moment and render it
			java.util.List<java.util.function.Supplier<Screen>> tour = java.util.List.of(
					() -> new SettingsScreen(null), () -> new HudEditorScreen(null),
					() -> new dev.vanillaclient.gui.TasksScreen(null),
					() -> dev.vanillaclient.gui.CosmeticsScreen.onTab(null, Cosmetics.Kind.CAPE),
					() -> dev.vanillaclient.gui.CosmeticsScreen.onTab(null, Cosmetics.Kind.WINGS),
					() -> dev.vanillaclient.gui.CosmeticsScreen.onTab(null, Cosmetics.Kind.SHIELD),
					() -> new dev.vanillaclient.gui.WorldMapScreen(null), () -> new dev.vanillaclient.gui.GuideScreen(null),
					() -> new dev.vanillaclient.gui.HeldItemsScreen(null),
					() -> new dev.vanillaclient.gui.AdvancementsChecklistScreen(null),
					() -> new dev.vanillaclient.gui.StatsOverviewScreen(null),
					() -> new dev.vanillaclient.gui.GalleryScreen(null),
					() -> new dev.vanillaclient.gui.CrosshairEditorScreen(null),
					() -> new dev.vanillaclient.gui.ColorPickerScreen(null, "Test", 0xFFFF0000, c -> {}));
			int[] step = {0, -100};
			ClientTickEvents.END_CLIENT_TICK.register(client -> {
				if (++step[1] == 35 && step[0] > 0) {
					try {
						net.minecraft.client.Screenshot.grab(client, false);
					} catch (Throwable t) {
						Config.LOG.warn("VANILLACLIENT AUDIT screenshot failed", t);
					}
				}
				if (step[1] < 40) return;
				step[1] = 0;
				if (step[0] < tour.size()) {
					Config.LOG.info("VANILLACLIENT AUDIT opening screen {}", step[0]);
					client.setScreenAndShow(tour.get(step[0]++).get());
				} else {
					Config.LOG.info("VANILLACLIENT AUDIT OK");
					Runtime.getRuntime().halt(0);
				}
			});
		}
	}

	private static boolean isPauseScreen(Screen screen) {
		String n = screen.getClass().getSimpleName();
		return n.equals("PauseScreen") || n.equals("GameMenuScreen");
	}

	private static boolean hasKey(AbstractWidget w, String key) {
		return w.getMessage().getContents() instanceof TranslatableContents tc && tc.getKey().equals(key);
	}

	private static void afterScreenInit(Minecraft client, Screen screen, int w, int h) {
		// L (advancements) opens the Vanilla Client tracker
		if (screen instanceof net.minecraft.client.gui.screens.advancements.AdvancementsScreen && Config.get().ownAdvancementsScreen) {
			if (!dev.vanillaclient.gui.AdvancementsChecklistScreen.vanillaWanted) {
				client.execute(() -> {
					if (client.gui.screen() == screen) client.setScreenAndShow(new dev.vanillaclient.gui.AdvancementsChecklistScreen(null));
				});
				return;
			}
			ScreenEvents.remove(screen).register(s -> dev.vanillaclient.gui.AdvancementsChecklistScreen.vanillaWanted = false);
		}
		List<AbstractWidget> widgets = Screens.getWidgets(screen);
		if (dev.vanillaclient.features.ChestTools.supported(screen)) {
			dev.vanillaclient.features.ChestTools.addButtons(screen, widgets);
			return;
		}
		if (Config.get().helperButtons && screen instanceof net.minecraft.client.gui.screens.advancements.AdvancementsScreen) {
			widgets.add(Button.builder(Component.literal("✦ Full checklist"), b -> client.setScreenAndShow(new dev.vanillaclient.gui.AdvancementsChecklistScreen(screen)))
					.bounds(4, 4, 100, 20).tooltip(Tooltip.create(Component.literal("Every advancement, what's missing and how to get it"))).build());
			return;
		}
		if (Config.get().helperButtons && screen instanceof net.minecraft.client.gui.screens.achievement.StatsScreen) {
			widgets.add(Button.builder(Component.literal("✦ Overview"), b -> client.setScreenAndShow(new dev.vanillaclient.gui.StatsOverviewScreen(screen)))
					.bounds(4, 4, 90, 20).build());
			return;
		}
		if (!Config.get().pauseMenuButton || !isPauseScreen(screen) || widgets.isEmpty()) return;
		// The small icon row (feedback, bugs, friends …) gets "Vanilla Client" on its left
		// and "Wardrobe" on its right – same width as the menu.
		AbstractWidget feedback = null, back = null;
		for (AbstractWidget wd : widgets) {
			if (hasKey(wd, "menu.sendFeedback") || hasKey(wd, "menu.reportBugs")) feedback = feedback == null ? wd : feedback;
			if (hasKey(wd, "menu.returnToGame")) back = wd;
		}
		if (feedback != null && back != null) {
			int rowY = feedback.getY();
			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
			for (AbstractWidget wd : widgets) {
				if (wd.getY() == rowY && wd.visible) {
					minX = Math.min(minX, wd.getX());
					maxX = Math.max(maxX, wd.getX() + wd.getWidth());
				}
			}
			int left = back.getX(), right = back.getX() + back.getWidth();
			int lw = minX - 4 - left, rw = right - (maxX + 4);
			if (lw >= 40 && rw >= 40) {
				var font = client.font;
				String l = font.width("Vanilla Client") + 8 <= lw ? "Vanilla Client" : "✦ Client";
				String r = font.width("Cosmetics") + 8 <= rw ? "Cosmetics" : "Wardrobe";
				widgets.add(Button.builder(Component.literal(l), b -> client.setScreenAndShow(new SettingsScreen(screen)))
						.bounds(left, rowY, lw, feedback.getHeight())
						.tooltip(Tooltip.create(Component.literal("Vanilla Client – modules, HUD, map, guide …"))).build());
				widgets.add(Button.builder(Component.literal(r), b -> client.setScreenAndShow(new dev.vanillaclient.gui.CosmeticsScreen(screen)))
						.bounds(maxX + 4, rowY, rw, feedback.getHeight())
						.tooltip(Tooltip.create(Component.literal("Wardrobe – capes, wings, shields"))).build());
				return;
			}
		}
		widgets.add(Button.builder(Component.literal("Vanilla Client"), b -> client.setScreenAndShow(new SettingsScreen(screen)))
				.bounds(4, 4, 100, 20).build());
		widgets.add(Button.builder(Component.literal("Wardrobe"), b -> client.setScreenAndShow(new dev.vanillaclient.gui.CosmeticsScreen(screen)))
				.bounds(4, 28, 100, 20).build());
	}
}
