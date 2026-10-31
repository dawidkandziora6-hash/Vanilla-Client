package dev.vanillaclient.gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.cosmetics.Cosmetics;
import dev.vanillaclient.crosshair.CrosshairRenderer;
import dev.vanillaclient.images.ImageManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Main menu: sections on the left, settings on the right. */
public final class SettingsScreen extends BaseScreen {
	private enum Tab {
		MODULES("Modules"), DAYS("Days"), TASKS("Tasks"), ARMOR("Armor"), CROSSHAIR("Crosshair"),
		ZOOM("Zoom"), IMAGES("Images"), COSMETICS("Cosmetics"), HELD("Held Items");

		final String label;

		Tab(String label) {
			this.label = label;
		}

		Item icon() {
			return switch (this) {
				case MODULES -> Items.COMPARATOR;
				case DAYS -> Items.CLOCK;
				case TASKS -> Items.WRITABLE_BOOK;
				case ARMOR -> Items.DIAMOND_CHESTPLATE;
				case CROSSHAIR -> Items.TARGET;
				case ZOOM -> Items.SPYGLASS;
				case IMAGES -> Items.PAINTING;
				case COSMETICS -> Items.ELYTRA;
				case HELD -> Items.TOTEM_OF_UNDYING;
			};
		}
	}

	private static Tab tab = Tab.MODULES;
	private static int imagePage;

	private static final int SIDEBAR = 100;
	private static final int CONTENT = 220;
	private static final int TOP = 40;
	private static final int ROW = 24;

	private String status = "";
	private long statusUntil;
	private long tabChangedAt = System.currentTimeMillis();

	public SettingsScreen(Screen parent) {
		super(Component.literal("Vanilla Client"), parent);
	}

	private int left() {
		return this.width / 2 - (SIDEBAR + 10 + CONTENT) / 2;
	}

	private int cx() {
		return left() + SIDEBAR + 10;
	}

	private int rowY(int i) {
		return TOP + i * ROW;
	}

	private int halfW() {
		return (CONTENT - 4) / 2;
	}

	private int rightHalfX() {
		return cx() + halfW() + 4;
	}

	private void say(String msg) {
		status = msg;
		statusUntil = System.currentTimeMillis() + 4000;
	}

	@Override
	protected void init() {
		super.init();
		int y = TOP;
		int tabH = Math.min(22, Math.max(20, (this.height - TOP - 40) / Tab.values().length));
		for (Tab t : Tab.values()) {
			Button b = iconAction(left(), y, SIDEBAR, Icons.stack(t.icon()), "    " + t.label, () -> {
				tab = t;
				tabChangedAt = System.currentTimeMillis();
				rebuildWidgets();
			});
			b.active = t != tab;
			y += tabH;
		}
		switch (tab) {
			case MODULES -> modulesTab();
			case DAYS -> daysTab();
			case TASKS -> tasksTab();
			case ARMOR -> armorTab();
			case CROSSHAIR -> crosshairTab();
			case ZOOM -> zoomTab();
			case IMAGES -> imagesTab();
			case COSMETICS -> cosmeticsTab();
			case HELD -> heldTab();
		}
		action(this.width / 2 - 154, this.height - 28, 150, "✥ Edit HUD …", () -> this.minecraft.setScreenAndShow(new HudEditorScreen(this)));
		action(this.width / 2 + 4, this.height - 28, 150, "Done", this::onClose);
	}

	private Config cfg() {
		return Config.get();
	}

	// ------------------------------------------------------------ sections

	private void modulesTab() {
		Config c = cfg();
		int x = cx();
		int w = halfW();
		toggle(x, rowY(0), w, "Days", () -> c.days.enabled, v -> c.days.enabled = v);
		toggle(rightHalfX(), rowY(0), w, "Tasks", () -> c.todo.enabled, v -> c.todo.enabled = v);
		toggle(x, rowY(1), w, "Armor", () -> c.armor.enabled, v -> c.armor.enabled = v);
		toggle(rightHalfX(), rowY(1), w, "Crosshair", () -> c.crosshair.enabled, v -> c.crosshair.enabled = v);
		toggle(x, rowY(2), w, "Zoom", () -> c.zoom.enabled, v -> c.zoom.enabled = v);
		toggle(rightHalfX(), rowY(2), w, "Held Items", () -> c.heldItems.enabled, v -> c.heldItems.enabled = v);
		long shown = c.images.stream().filter(i -> i.enabled).count();
		iconAction(x, rowY(3), w, Icons.stack(Items.PAINTING), "   Images " + shown + "/" + c.images.size(), () -> switchTo(Tab.IMAGES));
		iconAction(rightHalfX(), rowY(3), w, Icons.stack(Items.ELYTRA), "   Wardrobe", () -> this.minecraft.setScreenAndShow(new CosmeticsScreen(this)));
		toggle(x, rowY(4), CONTENT, "Button in pause menu", () -> c.pauseMenuButton, v -> c.pauseMenuButton = v);
		iconAction(x, rowY(5), CONTENT, Icons.stack(Items.WRITABLE_BOOK), "Open task planner …", () -> this.minecraft.setScreenAndShow(new TasksScreen(this)));
	}

	private void switchTo(Tab t) {
		tab = t;
		tabChangedAt = System.currentTimeMillis();
		rebuildWidgets();
	}

	private void hudCommon(Config.Hud s, int firstRow, boolean colors) {
		int x = cx();
		if (colors) {
			color(x, rowY(firstRow), halfW(), "Text color", () -> s.color, v -> s.color = v);
			toggle(rightHalfX(), rowY(firstRow), halfW(), "Shadow", () -> s.shadow, v -> s.shadow = v);
			firstRow++;
		}
		toggle(x, rowY(firstRow), halfW(), "Background", () -> s.background, v -> s.background = v);
		color(rightHalfX(), rowY(firstRow), halfW(), "Backgr. color", () -> s.backgroundColor, v -> s.backgroundColor = v);
		slider(x, rowY(firstRow + 1), CONTENT, 0.5, 3.0, 0.05, () -> s.scale, v -> s.scale = (float) v,
				v -> "Size: " + Math.round(v * 100) + "%");
	}

	private void daysTab() {
		Config.Days s = cfg().days;
		int x = cx();
		toggle(x, rowY(0), halfW(), "Show", () -> s.enabled, v -> s.enabled = v);
		toggle(rightHalfX(), rowY(0), halfW(), "Clock", () -> s.showTime, v -> s.showTime = v);
		toggle(x, rowY(1), halfW(), "Start at 1", () -> s.countFromOne, v -> s.countFromOne = v);
		toggle(rightHalfX(), rowY(1), halfW(), "☀ Icon", () -> s.showIcon, v -> s.showIcon = v);
		EditBox label = new EditBox(this.font, x + 1, rowY(2) + 1, CONTENT - 2, 18, Component.literal("Label"));
		label.setMaxLength(16);
		label.setValue(s.label);
		label.setHint(Component.literal("Label (e.g. Day)"));
		label.setResponder(t -> {
			s.label = t;
			changed();
		});
		addRenderableWidget(label);
		hudCommon(s, 3, true);
	}

	private void tasksTab() {
		Config.Todo s = cfg().todo;
		int x = cx();
		iconAction(x, rowY(0), CONTENT, Icons.stack(Items.WRITABLE_BOOK), "Open task planner …", () -> this.minecraft.setScreenAndShow(new TasksScreen(this)));
		toggle(x, rowY(1), halfW(), "Show", () -> s.enabled, v -> s.enabled = v);
		toggle(rightHalfX(), rowY(1), halfW(), "Hide done", () -> s.hideDone, v -> s.hideDone = v);
		toggle(x, rowY(2), halfW(), "Subtasks", () -> s.showSubtasks, v -> s.showSubtasks = v);
		toggle(rightHalfX(), rowY(2), halfW(), "Coords", () -> s.showCoords, v -> s.showCoords = v);
		List<String> filters = new ArrayList<>();
		filters.add("");
		for (Config.Category c : cfg().taskCategories) filters.add(c.name);
		cycle(x, rowY(3), halfW(), "Show", filters, () -> filters.contains(s.categoryFilter) ? s.categoryFilter : "",
				v -> s.categoryFilter = v, v -> v.isEmpty() ? "All" : v);
		slider(rightHalfX(), rowY(3), halfW(), 1, 20, 1, () -> s.maxLines, v -> s.maxLines = (int) v, v -> "Lines: " + (int) v);
		hudCommon(s, 4, true);
	}

	private void armorTab() {
		Config.Armor s = cfg().armor;
		int x = cx();
		toggle(x, rowY(0), halfW(), "Show", () -> s.enabled, v -> s.enabled = v);
		toggle(rightHalfX(), rowY(0), halfW(), "At hotbar", () -> s.attachToHotbar, v -> s.attachToHotbar = v);
		cycle(x, rowY(1), halfW(), "Side", List.of(true, false), () -> s.rightSide, v -> s.rightSide = v, v -> v ? "Right" : "Left");
		cycle(rightHalfX(), rowY(1), halfW(), "Durab.", List.of(0, 1, 2), () -> s.durabilityText, v -> s.durabilityText = v,
				v -> switch (v) {
					case 1 -> "%";
					case 2 -> "Number";
					default -> "Bar";
				});
		toggle(x, rowY(2), halfW(), "Held item", () -> s.showHeldItem, v -> s.showHeldItem = v);
		toggle(rightHalfX(), rowY(2), halfW(), "Hide if empty", () -> s.hideWhenEmpty, v -> s.hideWhenEmpty = v);
		slider(x, rowY(3), CONTENT, 0.5, 3.0, 0.05, () -> s.scale, v -> s.scale = (float) v,
				v -> "Size (when free): " + Math.round(v * 100) + "%");
	}

	private void crosshairTab() {
		Config.Crosshair s = cfg().crosshair;
		int x = cx();
		toggle(x, rowY(0), halfW(), "Custom", () -> s.enabled, v -> s.enabled = v);
		action(rightHalfX(), rowY(0), halfW(), "✎ Paint …", () -> this.minecraft.setScreenAndShow(new CrosshairEditorScreen(this)));
		color(x, rowY(1), halfW(), "Color", () -> s.color, v -> s.color = v);
		toggle(rightHalfX(), rowY(1), halfW(), "Attack bar", () -> s.attackIndicator, v -> s.attackIndicator = v);
		toggle(x, rowY(2), halfW(), "Outline", () -> s.outline, v -> s.outline = v);
		color(rightHalfX(), rowY(2), halfW(), "Outline col.", () -> s.outlineColor, v -> s.outlineColor = v);
		slider(x, rowY(3), CONTENT, 0.5, 4.0, 0.25, () -> s.scale, v -> s.scale = (float) v, v -> "Size: " + v + "×");
	}

	private void zoomTab() {
		Config.Zoom z = cfg().zoom;
		int x = cx();
		toggle(x, rowY(0), halfW(), "Zoom", () -> z.enabled, v -> z.enabled = v);
		toggle(rightHalfX(), rowY(0), halfW(), "Smooth", () -> z.smoothTransition, v -> z.smoothTransition = v);
		slider(x, rowY(1), CONTENT, 1.5, 30, 0.5, () -> z.level, v -> z.level = (float) v, v -> "Zoom level: " + v + "×");
		toggle(x, rowY(2), halfW(), "Scroll wheel", () -> z.scrollToAdjust, v -> z.scrollToAdjust = v);
		toggle(rightHalfX(), rowY(2), halfW(), "Cinematic", () -> z.cinematicCamera, v -> z.cinematicCamera = v);
		toggle(x, rowY(3), halfW(), "Hide hand", () -> z.hideHand, v -> z.hideHand = v);
		toggle(rightHalfX(), rowY(3), halfW(), "Reset scroll", () -> z.resetLevelOnRelease, v -> z.resetLevelOnRelease = v);
	}

	private void imagesTab() {
		Config c = cfg();
		int x = cx();
		action(x, rowY(0), halfW(), "Open folder", () -> Platform.open(Config.imagesDir()));
		action(rightHalfX(), rowY(0), halfW(), "↻ Reload", () -> {
			ImageManager.reload();
			say(c.images.size() + " image(s) found");
			rebuildWidgets();
		});
		int perPage = 4;
		int pages = Math.max(1, (c.images.size() + perPage - 1) / perPage);
		imagePage = Math.max(0, Math.min(imagePage, pages - 1));
		for (int i = 0; i < perPage; i++) {
			int idx = imagePage * perPage + i;
			if (idx >= c.images.size()) break;
			Config.Image img = c.images.get(idx);
			String name = this.font.plainSubstrByWidth(img.file, 70);
			if (!name.equals(img.file)) name += "…";
			toggle(x, rowY(1 + i), 110, name, () -> img.enabled, v -> img.enabled = v);
			slider(x + 114, rowY(1 + i), CONTENT - 114, 0.05, 1.0, 0.05, () -> ((img.tint >>> 24) & 0xFF) / 255.0,
					v -> img.tint = (img.tint & 0x00FFFFFF) | ((int) Math.round(v * 255) << 24),
					v -> "Opacity " + Math.round(v * 100) + "%");
		}
		if (pages > 1) {
			action(x, rowY(5), 30, "◀", () -> {
				imagePage--;
				rebuildWidgets();
			}).active = imagePage > 0;
			action(x + CONTENT - 30, rowY(5), 30, "▶", () -> {
				imagePage++;
				rebuildWidgets();
			}).active = imagePage < pages - 1;
		}
	}

	private void cosmeticsTab() {
		int x = cx();
		iconAction(x, rowY(0), CONTENT, Icons.stack(Items.ELYTRA), "✦ Open Wardrobe …", () -> this.minecraft.setScreenAndShow(new CosmeticsScreen(this)));
		action(x, rowY(1), halfW(), "Open folder", () -> Platform.open(Config.cosmeticsDir()));
		action(rightHalfX(), rowY(1), halfW(), "↻ Reload own", () -> {
			Cosmetics.reloadCustom();
			say("Your images were reloaded");
		});
	}

	private void heldTab() {
		Config.HeldItems h = cfg().heldItems;
		int x = cx();
		toggle(x, rowY(0), CONTENT, "Change held item size", () -> h.enabled, v -> h.enabled = v);
		slider(x, rowY(1), CONTENT, 0.3, 1.5, 0.05, () -> h.shieldScale, v -> h.shieldScale = (float) v,
				v -> "⛨ Shield: " + Math.round(v * 100) + "%");
		slider(x, rowY(2), CONTENT, 0.3, 1.5, 0.05, () -> h.totemScale, v -> h.totemScale = (float) v,
				v -> "❤ Totem in hand: " + Math.round(v * 100) + "%");
		slider(x, rowY(3), CONTENT, 0.2, 1.5, 0.05, () -> h.totemPopScale, v -> h.totemPopScale = (float) v,
				v -> "✦ Totem pop animation: " + Math.round(v * 100) + "%");
	}

	// ------------------------------------------------------------ drawing

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		float in = intro();
		int titleY = 10 + Math.round((1 - in) * -6);
		g.centeredText(this.font, "§l✦ Vanilla Client ✦", this.width / 2, titleY, Theme.alpha(Theme.ACCENT, in));
		g.centeredText(this.font, "§7" + tab.label + " · everything stays vanilla", this.width / 2, titleY + 12, 0xFFFFFFFF);
		// animated underline below the active tab
		float t = Theme.easeOut((System.currentTimeMillis() - tabChangedAt) / 200f);
		int x = cx();
		g.fill(x, TOP - 4, x + Math.round(CONTENT * t), TOP - 3, Theme.alpha(Theme.ACCENT, 0.8f));

		switch (tab) {
			case MODULES -> hint(g, x, rowY(6), "Tip: \"Edit HUD\" lets you drag everything,", "scroll to resize. Right Shift opens this menu.");
			case ARMOR -> hint(g, x, rowY(4), "\"At hotbar\" off = free to move", "in the HUD editor.");
			case CROSSHAIR -> crosshairPreview(g, x);
			case ZOOM -> hint(g, x, rowY(4), "Key: §eC§r§7 (change in Controls).", "While zooming: scroll = closer / further.");
			case IMAGES -> hint(g, x, rowY(cfg().images.size() > 4 ? 6 : 1 + Math.max(1, cfg().images.size())),
					"Drag PNG/JPG files into this window.", "Place them in the HUD editor.");
			case COSMETICS -> cosmeticsSummary(g, x);
			case HELD -> heldPreview(g, x);
			default -> {}
		}
		if (System.currentTimeMillis() < statusUntil) {
			g.centeredText(this.font, status, this.width / 2, this.height - 42, Theme.ACCENT);
		}
	}

	private void crosshairPreview(GuiGraphicsExtractor g, int x) {
		int px = x + CONTENT / 2;
		int py = rowY(4) + 34;
		g.fill(px - 40, py - 28, px + 40, py + 28, 0xFF7BA4E0);
		g.fill(px - 40, py + 6, px + 40, py + 28, 0xFF5E8C3A);
		g.outline(px - 40, py - 28, 80, 56, 0x60FFFFFF);
		CrosshairRenderer.draw(g, cfg().crosshair, px, py, cfg().crosshair.scale);
	}

	private void cosmeticsSummary(GuiGraphicsExtractor g, int x) {
		int y = rowY(2) + 6;
		for (Cosmetics.Kind k : Cosmetics.Kind.values()) {
			String id = Cosmetics.selected(k);
			Cosmetics.Design d = Cosmetics.designs(k).stream().filter(e -> e.id().equals(id)).findFirst().orElse(Cosmetics.designs(k).get(0));
			g.fill(x, y + 1, x + 6, y + 7, d.color());
			g.text(this.font, "§7" + k.title + ": §f" + d.name(), x + 10, y, 0xFFFFFFFF, false);
			y += 12;
		}
		hint(g, x, rowY(4) + 8, "Only you can see them.", "Cape hides with elytra, wings need an elytra.");
	}

	private void heldPreview(GuiGraphicsExtractor g, int x) {
		Config.HeldItems h = cfg().heldItems;
		int y = rowY(4) + 8;
		float bob = (float) Math.sin(Theme.time() * 2.0) * 2f;
		float s1 = 2f * (h.enabled ? h.shieldScale : 1f);
		float s2 = 2f * (h.enabled ? h.totemScale : 1f);
		int cx1 = x + CONTENT / 4;
		int cx2 = x + CONTENT * 3 / 4;
		Icons.scaled(g, Icons.stack(Items.SHIELD), cx1 - 8 * s1, y + 20 - 8 * s1 + bob, s1);
		Icons.scaled(g, Icons.stack(Items.TOTEM_OF_UNDYING), cx2 - 8 * s2, y + 20 - 8 * s2 - bob, s2);
		g.centeredText(this.font, "§7Only in your own hands", x + CONTENT / 2, y + 48, 0xFFFFFFFF);
	}

	private void hint(GuiGraphicsExtractor g, int x, int y, String a, String b) {
		g.text(this.font, "§7" + a, x, y + 4, 0xFFFFFFFF, false);
		if (!b.isEmpty()) g.text(this.font, "§7" + b, x, y + 15, 0xFFFFFFFF, false);
	}

	/** Import pictures by drag & drop from the file explorer. */
	@Override
	public void onFilesDrop(List<Path> paths) {
		int n = ImageManager.importFiles(paths);
		if (n > 0) {
			tab = Tab.IMAGES;
			say("✔ " + n + " image(s) added – place them in the HUD editor");
			rebuildWidgets();
		} else {
			say("§cOnly PNG or JPG files");
		}
	}
}
