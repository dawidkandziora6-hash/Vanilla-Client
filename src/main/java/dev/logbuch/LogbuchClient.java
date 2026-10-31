package dev.logbuch;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import dev.logbuch.config.LogbuchConfig;
import dev.logbuch.crosshair.CrosshairRenderer;
import dev.logbuch.gui.LogbuchScreen;
import dev.logbuch.hud.HudManager;
import dev.logbuch.images.ImageManager;
import dev.logbuch.zoom.Zoom;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Logbuch – rein clientseitig. Es werden keine Pakete gesendet und keine
 * Spielwerte verändert; alles ist Anzeige oder Kosmetik nur für dich.
 */
public final class LogbuchClient implements ClientModInitializer {
	public static final String MOD_ID = "logbuch";

	private static KeyMapping openMenu;
	private static KeyMapping toggleTodo;

	@Override
	public void onInitializeClient() {
		LogbuchConfig.load();
		ImageManager.scan();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		KeyMapping zoom = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.logbuch.zoom", InputConstants.Type.KEYBOARD, InputConstants.KEY_C, category));
		openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.logbuch.open", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), category));
		toggleTodo = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.logbuch.toggle_todo", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), category));
		Zoom.init(zoom);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenu.consumeClick()) {
				if (client.gui.screen() == null) {
					client.setScreenAndShow(new LogbuchScreen(null));
				}
			}
			while (toggleTodo.consumeClick()) {
				LogbuchConfig.Todo t = LogbuchConfig.get().todo;
				t.enabled = !t.enabled;
				LogbuchConfig.save();
			}
		});

		// Eigene HUD-Elemente liegen unter dem Chat.
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudManager::render);
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, CrosshairRenderer::wrap);

		ScreenEvents.AFTER_INIT.register(LogbuchClient::afterScreenInit);
	}

	private static boolean isPauseScreen(Screen screen) {
		String n = screen.getClass().getSimpleName();
		return n.equals("PauseScreen") || n.equals("GameMenuScreen");
	}

	private static boolean isOptionsButton(AbstractWidget w) {
		return w.getMessage().getContents() instanceof TranslatableContents tc && tc.getKey().equals("menu.options");
	}

	/** Neuer Knopf im Pausemenü, links neben "Optionen…" – wie die kleinen Vanilla-Symbolknöpfe. */
	private static void afterScreenInit(Minecraft client, Screen screen, int w, int h) {
		if (!LogbuchConfig.get().pauseMenuButton || !isPauseScreen(screen)) return;
		List<AbstractWidget> widgets = Screens.getWidgets(screen);
		if (widgets.isEmpty()) return; // Pausemenü per F3+Esc ausgeblendet
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
			button = Button.builder(Component.empty(), b -> client.setScreenAndShow(new LogbuchScreen(screen)))
					.bounds(bx, by, 20, 20)
					.tooltip(Tooltip.create(Component.literal("Logbuch")))
					.build();
			ItemStack icon = new ItemStack(Items.WRITABLE_BOOK);
			ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> g.item(icon, bx + 2, by + 2));
		} else {
			button = Button.builder(Component.literal("Logbuch"), b -> client.setScreenAndShow(new LogbuchScreen(screen)))
					.bounds(4, 4, 80, 20)
					.build();
		}
		widgets.add(button);
	}
}
