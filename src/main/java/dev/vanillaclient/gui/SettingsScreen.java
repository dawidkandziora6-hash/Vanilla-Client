package dev.vanillaclient.gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.crosshair.CrosshairRenderer;
import dev.vanillaclient.gui.ui.Ui;
import dev.vanillaclient.images.ImageManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Main menu: categorized sidebar on the left, animated settings on the right. */
public final class SettingsScreen extends BaseScreen {
	private enum Page {
		HOME(null, "Home", Items.GRASS_BLOCK),
		DAYS("HUD", "Days", Items.CLOCK),
		COORDS("HUD", "Coordinates", Items.COMPASS),
		ARMOR("HUD", "Armor", Items.DIAMOND_CHESTPLATE),
		TASKLIST("HUD", "Task List", Items.WRITABLE_BOOK),
		KEYS("HUD", "Keystrokes", Items.STONE_BUTTON),
		WAYPOINTS("HUD", "Waypoints", Items.RECOVERY_COMPASS),
		IMAGES("HUD", "Pictures", Items.PAINTING),
		FPS("HUD", "FPS", Items.REDSTONE, 0),
		PING("HUD", "Ping", Items.ENDER_PEARL, 1),
		BIOME("HUD", "Biome", Items.OAK_SAPLING, 2),
		CLOCK("HUD", "Real Time", Items.CLOCK, 3),
		SPEED("HUD", "Speed", Items.FEATHER, 4),
		LIGHT("HUD", "Light Level", Items.TORCH, 5),
		SESSION("HUD", "Session Time", Items.COMPASS, 6),
		EFFECTS("HUD", "Effects", Items.POTION, 7),
		COUNTER("HUD", "Item Counter", Items.TOTEM_OF_UNDYING, 8),
		CROSSHAIR("VISUALS", "Crosshair", Items.TARGET),
		ZOOM("VISUALS", "Zoom", Items.SPYGLASS),
		HELD("VISUALS", "Held Items", Items.TOTEM_OF_UNDYING),
		RANK("ACCOUNT", "Rank", Items.NETHER_STAR),
		CONTROLS("GENERAL", "Controls", Items.LEVER),
		GENERAL("GENERAL", "Settings", Items.COMPARATOR);

		final String group;
		final String label;
		final Item icon;
		/** Index into HudManager.INFO for the small info modules, else -1. */
		final int module;

		Page(String group, String label, Item icon) {
			this(group, label, icon, -1);
		}

		Page(String group, String label, Item icon, int module) {
			this.group = group;
			this.label = label;
			this.icon = icon;
			this.module = module;
		}
	}

	private static Page page = Page.HOME;
	private float sideScroll, sideScrollTarget;

	private static final int SIDEBAR = 106, GAP = 10, HEADER = 12;

	private final Logo logo = new Logo();
	private final Ui.List_ list = new Ui.List_();
	private long pageAt = System.currentTimeMillis();
	private float indicatorY = -1;
	private final float[] entryHover = new float[Page.values().length];
	private String status = "";
	private long statusAt;

	/** Opens the menu on a page by name (e.g. "RANK"). */
	public static SettingsScreen at(Screen parent, String pageName) {
		try {
			page = Page.valueOf(pageName);
		} catch (IllegalArgumentException ignored) {
			page = Page.HOME;
		}
		return new SettingsScreen(parent);
	}

	public SettingsScreen(Screen parent) {
		super(Component.literal("Vanilla Client"), parent);
	}

	// ------------------------------------------------------------ layout

	private int contentW() {
		return Math.max(200, Math.min(270, this.width - SIDEBAR - GAP - 40));
	}

	private int left() {
		return this.width / 2 - (SIDEBAR + GAP + contentW()) / 2;
	}

	private int cx() {
		return left() + SIDEBAR + GAP;
	}

	private int top() {
		return 44;
	}

	private int bottom() {
		return this.height - 34;
	}

	private Config cfg() {
		return Config.get();
	}

	// ------------------------------------------------------------ build

	@Override
	protected void init() {
		super.init();
		list.x = cx();
		list.w = contentW();
		list.top = top();
		list.bottom = bottom();
		list.addWidget = b -> addRenderableWidget(b);
		list.removeWidget = this::removeWidget;
		buildPage();
		action(this.width / 2 - 154, this.height - 26, 150, "✥ Edit HUD", () -> this.minecraft.setScreenAndShow(new HudEditorScreen(this)));
		action(this.width / 2 + 4, this.height - 26, 150, "✔ Done", this::onClose);
	}

	private void go(Page p) {
		if (page == p) return;
		page = p;
		for (Entry e : sidebar()) {
			if (e.page() == p) {
				int vis = bottom() - top();
				float y = e.y() - top() + sideScrollTarget;
				if (y < sideScrollTarget) sideScrollTarget = Math.max(0, y - 14);
				if (y + 16 > sideScrollTarget + vis) sideScrollTarget = y + 30 - vis;
			}
		}
		pageAt = System.currentTimeMillis();
		buildPage();
	}

	private ItemStack i(Item item) {
		return Icons.stack(item);
	}

	private void open(Screen s) {
		this.minecraft.setScreenAndShow(s);
	}

	private void colorPicker(Ui.ColorPick c) {
		open(new ColorPickerScreen(this, c.label(), c.value(), c::apply));
	}

	private void buildPage() {
		list.reset();
		switch (page) {
			case HOME -> home();
			case DAYS -> days();
			case COORDS -> coords();
			case ARMOR -> armor();
			case TASKLIST -> taskList();
			case IMAGES -> images();
			case KEYS -> keystrokes();
			case WAYPOINTS -> waypoints();
			case FPS, PING, BIOME, CLOCK, SPEED, LIGHT, SESSION, EFFECTS, COUNTER -> info(page.module);
			case CROSSHAIR -> crosshair();
			case ZOOM -> zoom();
			case HELD -> held();
			case RANK -> rank();
			case CONTROLS -> controls();
			case GENERAL -> general();
		}
	}

	private void home() {
		Config c = cfg();
		list.add(new Ui.Custom(34, (g, x, y, w, h, mx, my, a) -> drawRankBanner(g, x, y, w, a)));
		list.add(new Ui.Header("MODULES  §8· click = settings · right-click = on/off"));
		list.add(new Ui.Chip("Days", i(Items.CLOCK), () -> c.days.enabled, v -> c.days.enabled = v, () -> go(Page.DAYS)));
		list.add(new Ui.Chip("Coordinates", i(Items.COMPASS), () -> c.coords.enabled, v -> c.coords.enabled = v, () -> go(Page.COORDS)));
		list.add(new Ui.Chip("Armor", i(Items.DIAMOND_CHESTPLATE), () -> c.armor.enabled, v -> c.armor.enabled = v, () -> go(Page.ARMOR)));
		list.add(new Ui.Chip("Task List", i(Items.WRITABLE_BOOK), () -> c.todo.enabled, v -> c.todo.enabled = v, () -> go(Page.TASKLIST)));
		list.add(new Ui.Chip("Waypoint Bar", i(Items.RECOVERY_COMPASS), () -> c.waypointHud.mode != 0,
				v -> c.waypointHud.mode = v ? 1 : 0, () -> go(Page.WAYPOINTS)));
		list.add(new Ui.Chip("Crosshair", i(Items.TARGET), () -> c.crosshair.enabled, v -> c.crosshair.enabled = v, () -> go(Page.CROSSHAIR)));
		list.add(new Ui.Chip("Zoom", i(Items.SPYGLASS), () -> c.zoom.enabled, v -> c.zoom.enabled = v, () -> go(Page.ZOOM)));
		list.add(new Ui.Chip("Held Items", i(Items.TOTEM_OF_UNDYING), () -> c.heldItems.enabled, v -> c.heldItems.enabled = v, () -> go(Page.HELD)));
		list.add(new Ui.Chip("World Map", i(Items.FILLED_MAP), () -> c.worldMap.enabled, v -> c.worldMap.enabled = v, () -> go(Page.GENERAL)));
		list.add(new Ui.Chip("Name Tag", i(Items.NAME_TAG), () -> c.ranks.ownNameTag, v -> c.ranks.ownNameTag = v, () -> go(Page.RANK)));
		list.add(new Ui.Chip("Keystrokes", i(Items.STONE_BUTTON), () -> c.keystrokes.enabled, v -> c.keystrokes.enabled = v, () -> go(Page.KEYS)));
		java.util.List<dev.vanillaclient.hud.HudModule> info = dev.vanillaclient.hud.HudManager.INFO;
		for (int k = 0; k < info.size(); k++) {
			final int idx = k;
			dev.vanillaclient.hud.HudModule m = info.get(k);
			list.add(new Ui.Chip(m.name, m.icon(), () -> m.settings().enabled, v -> m.settings().enabled = v, () -> {
				for (Page p : Page.values()) if (p.module == idx) go(p);
			}));
		}
		list.add(new Ui.Header("TOOLS"));
		list.add(new Ui.Tile("Task Planner", "Farms, builds, trips", i(Items.WRITABLE_BOOK), () -> open(new TasksScreen(this))));
		list.add(new Ui.Tile("World Map", "Map & waypoints", i(Items.FILLED_MAP), () -> open(new WorldMapScreen(this))));
		list.add(new Ui.Tile("Wardrobe", "Capes, wings, shields", i(Items.ELYTRA), () -> open(new CosmeticsScreen(this))));
		list.add(new Ui.Tile("UHC Guide", "Trades, enchants, brewing", i(Items.ENCHANTED_BOOK), () -> open(new GuideScreen(this))));
		list.add(new Ui.Tile("Advancements", "Every one, what to do", i(Items.GOLDEN_APPLE), () -> open(new AdvancementsChecklistScreen(this))));
		list.add(new Ui.Tile("Statistics", "Your world in numbers", i(Items.BOOK), () -> open(new StatsOverviewScreen(this))));
		list.add(new Ui.Tile("Gallery", "Screenshots & pictures", i(Items.PAINTING), () -> open(new GalleryScreen(this))));
	}

	private void drawRankBanner(GuiGraphicsExtractor g, int x, int y, int w, float a) {
		var user = this.minecraft.getUser();
		dev.vanillaclient.features.ClientRanks.Rank cr = dev.vanillaclient.features.ClientRanks.mine();
		int col = cr == null ? 0xFF808080 : 0xFF000000 | cr.from();
		float pulse = 0.5f + 0.5f * (float) Math.sin(Theme.time() * 2.5);
		g.fillGradient(x, y, x + w, y + 32, Theme.alpha(Theme.lerpColor(col, 0xFF000000, 0.55f - pulse * 0.1f), a), Theme.alpha(0x60000000, a));
		g.fill(x, y, x + 2, y + 32, Theme.alpha(col, a));
		g.outline(x, y, w, 32, Theme.alpha(0x40FFFFFF, a));
		// your head
		try {
			var tex = this.minecraft.player != null ? this.minecraft.player.getSkin().body().texturePath() : null;
			if (tex != null) {
				g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, x + 7, y + 6, 8f, 8f, 20, 20, 8, 8, 64, 64);
				g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, x + 7, y + 6, 40f, 8f, 20, 20, 8, 8, 64, 64);
			}
		} catch (RuntimeException ignored) {
			// no skin yet
		}
		net.minecraft.network.chat.MutableComponent line = cr != null ? dev.vanillaclient.features.NameStyle.mine().build(user.getName(), cr, (float) Theme.time())
				: Component.literal(user.getName()).withColor(0xFFFFFF);
		g.text(this.font, line, x + 33, y + 7, Theme.alpha(0xFFFFFFFF, a), true);
		String sub = cr == null ? "§7No rank yet · ranks are not available yet"
				: "§7" + dev.vanillaclient.features.Ranks.formatHours(dev.vanillaclient.features.Ranks.hours()) + " played with Vanilla Client";
		g.text(this.font, this.font.plainSubstrByWidth(sub, w - 40), x + 33, y + 19, Theme.alpha(0xFFFFFFFF, a), false);
	}

	private void labelOptions(Config.TextHud s, String hint) {
		list.add(new Ui.Header("LABEL"));
		list.add(new Ui.TextField("Label text", () -> s.label, v -> s.label = v, 24));
		list.add(new Ui.Toggle("Show label", null, () -> s.showLabel, v -> s.showLabel = v));
		list.add(new Ui.ColorPick("Label color", () -> s.labelColor, v -> s.labelColor = v, this::colorPicker));
		list.add(new Ui.TextField("Text after value", () -> s.suffix, v -> s.suffix = v, 16));
		if (hint != null) list.add(new Ui.Info(hint));
	}

	private void style(Config.Hud s, boolean text) {
		list.add(new Ui.Header("STYLE"));
		if (text) {
			list.add(new Ui.ColorPick("Text color", () -> s.color, v -> s.color = v, this::colorPicker));
			list.add(new Ui.Toggle("Text shadow", null, () -> s.shadow, v -> s.shadow = v));
		}
		list.add(new Ui.Toggle("Background", null, () -> s.background, v -> s.background = v));
		list.add(new Ui.ColorPick("Background color", () -> s.backgroundColor, v -> s.backgroundColor = v, this::colorPicker));
		list.add(new Ui.Slider("Size", 0.5, 3.0, 0.05, () -> s.scale, v -> s.scale = (float) v, v -> Math.round(v * 100) + "%"));
		list.add(new Ui.Button("Move it in the HUD editor", i(Items.ITEM_FRAME), () -> open(new HudEditorScreen(this))));
	}

	private void days() {
		Config.Days s = cfg().days;
		list.add(new Ui.Header("DAYS"));
		list.add(new Ui.Toggle("Show on screen", i(Items.CLOCK), () -> s.enabled, v -> s.enabled = v));
		list.add(new Ui.Info("Counts from day 1, exactly like your world's days."));
		labelOptions(s, "e.g. \"Day\", \"UHC Day\", \"Tag\" …");
		style(s, true);
	}

	private void coords() {
		Config.Coords s = cfg().coords;
		list.add(new Ui.Header("COORDINATES"));
		list.add(new Ui.Toggle("Show on screen", i(Items.COMPASS), () -> s.enabled, v -> s.enabled = v));
		list.add(new Ui.Toggle("One line (compact)", null, () -> s.compact, v -> s.compact = v));
		list.add(new Ui.Toggle("Facing direction", null, () -> s.showDirection, v -> s.showDirection = v));
		list.add(new Ui.Toggle("Nether ⇄ Overworld position", null, () -> s.showOtherDimension, v -> s.showOtherDimension = v));
		list.add(new Ui.Toggle("Chunk position", null, () -> s.showChunk, v -> s.showChunk = v));
		list.add(new Ui.Header("LABELS"));
		list.add(new Ui.Toggle("Own label color", null, () -> s.coloredLabels, v -> s.coloredLabels = v));
		list.add(new Ui.ColorPick("Label color", () -> s.labelColor, v -> s.labelColor = v, this::colorPicker));
		list.add(new Ui.TextField("X label", () -> s.labelX, v -> s.labelX = v, 12));
		list.add(new Ui.TextField("Y label", () -> s.labelY, v -> s.labelY = v, 12));
		list.add(new Ui.TextField("Z label", () -> s.labelZ, v -> s.labelZ = v, 12));
		list.add(new Ui.TextField("Facing label", () -> s.labelFacing, v -> s.labelFacing = v, 16));
		list.add(new Ui.TextField("Nether/Overworld label", () -> s.labelOther, v -> s.labelOther = v, 16));
		list.add(new Ui.TextField("Chunk label", () -> s.labelChunk, v -> s.labelChunk = v, 16));
		list.add(new Ui.Info("Leave a field empty for the default text."));
		style(s, true);
	}

	private void armor() {
		Config.Armor s = cfg().armor;
		list.add(new Ui.Header("ARMOR"));
		list.add(new Ui.Toggle("Show on screen", i(Items.DIAMOND_CHESTPLATE), () -> s.enabled, v -> s.enabled = v));
		list.add(new Ui.Toggle("Dock next to the hotbar", null, () -> s.attachToHotbar, v -> s.attachToHotbar = v));
		list.add(new Ui.Cycle<>("Side", List.of(true, false), () -> s.rightSide, v -> s.rightSide = v, v -> v ? "Right" : "Left"));
		list.add(new Ui.Cycle<>("Durability", List.of(0, 1, 2), () -> s.durabilityText, v -> s.durabilityText = v,
				v -> switch (v) {
					case 1 -> "Percent";
					case 2 -> "Number";
					default -> "Bar only";
				}));
		list.add(new Ui.Toggle("Also show held item", null, () -> s.showHeldItem, v -> s.showHeldItem = v));
		list.add(new Ui.Toggle("Hide without armor", null, () -> s.hideWhenEmpty, v -> s.hideWhenEmpty = v));
		list.add(new Ui.Slider("Size (undocked)", 0.5, 3.0, 0.05, () -> s.scale, v -> s.scale = (float) v, v -> Math.round(v * 100) + "%"));
		list.add(new Ui.Info("Undock it to move it freely in the HUD editor."));
	}

	private void taskList() {
		Config.Todo s = cfg().todo;
		list.add(new Ui.Button("Open the Task Planner", i(Items.WRITABLE_BOOK), () -> open(new TasksScreen(this)), 22));
		list.add(new Ui.Header("ON-SCREEN LIST"));
		list.add(new Ui.Toggle("Show on screen", i(Items.WRITABLE_BOOK), () -> s.enabled, v -> s.enabled = v));
		list.add(new Ui.Toggle("Hide finished tasks", null, () -> s.hideDone, v -> s.hideDone = v));
		list.add(new Ui.Toggle("Show subtasks", null, () -> s.showSubtasks, v -> s.showSubtasks = v));
		list.add(new Ui.Toggle("Show coordinates", null, () -> s.showCoords, v -> s.showCoords = v));
		list.add(new Ui.Toggle("Show title", null, () -> s.showTitle, v -> s.showTitle = v));
		List<String> filters = new ArrayList<>();
		filters.add("");
		for (Config.Category c : cfg().taskCategories) filters.add(c.name);
		list.add(new Ui.Cycle<>("Category", filters, () -> filters.contains(s.categoryFilter) ? s.categoryFilter : "",
				v -> s.categoryFilter = v, v -> v.isEmpty() ? "All" : v));
		list.add(new Ui.Slider("Max lines", 1, 20, 1, () -> s.maxLines, v -> s.maxLines = (int) v, v -> String.valueOf((int) v)));
		style(s, true);
	}

	private void images() {
		Config c = cfg();
		list.add(new Ui.Button("Open the gallery (screenshots + pictures)", i(Items.PAINTING), () -> open(new GalleryScreen(this)), 22));
		list.add(new Ui.Header("SCREENSHOTS"));
		list.add(new Ui.Toggle("Nicer chat message with [View] [Open]", null, () -> c.screenshotMessage, v -> c.screenshotMessage = v));
		list.add(new Ui.Toggle("Preview popup after F2", null, () -> c.screenshotPopup, v -> c.screenshotPopup = v));
		java.util.List<java.nio.file.Path> shots = dev.vanillaclient.images.Screenshots.all();
		if (!shots.isEmpty()) {
			list.add(new Ui.Custom(46, (g, x, y, w, h, mx, my, a) -> {
				int n = Math.min(shots.size(), Math.max(1, (w + 4) / 74));
				for (int k = 0; k < n; k++) thumb(g, shots.get(k), x + k * 74, y, 70, 42, 160);
			}, () -> open(new GalleryScreen(this))));
		}
		list.add(new Ui.Header("HUD PICTURES  §8· drop PNG / JPG files into this window"));
		list.add(new Ui.Button("Open folder", i(Items.CHEST), () -> Platform.open(Config.imagesDir())));
		list.add(new Ui.Button("Reload pictures", i(Items.CLOCK), () -> {
			ImageManager.reload();
			say("✔ " + c.images.size() + " picture(s) found");
			buildPage();
		}));
		if (c.images.isEmpty()) {
			list.add(new Ui.Info("No pictures yet.", "Drag PNG / JPG files into this window!"));
		}
		for (Config.Image img : c.images) {
			java.nio.file.Path f = Config.imagesDir().resolve(img.file);
			list.add(new Ui.Custom(40, (g, x, y, w, h, mx, my, a) -> {
				g.fill(x, y, x + w, y + 38, Theme.alpha(img.enabled ? 0x30306030 : 0x20000000, a));
				thumb(g, f, x + 3, y + 3, 56, 32, 128);
				g.text(this.font, "§f" + this.font.plainSubstrByWidth(img.file, w - 72), x + 64, y + 8, Theme.alpha(0xFFFFFFFF, a), false);
				g.text(this.font, img.enabled ? "§a● on screen" : "§8○ hidden · click to show", x + 64, y + 22, Theme.alpha(0xFFFFFFFF, a), false);
			}, () -> {
				img.enabled = !img.enabled;
				Config.save();
			}));
			list.add(new Ui.Slider("  Opacity", 0.05, 1.0, 0.05, () -> ((img.tint >>> 24) & 0xFF) / 255.0,
					v -> img.tint = (img.tint & 0x00FFFFFF) | ((int) Math.round(v * 255) << 24), v -> Math.round(v * 100) + "%"));
		}
		list.add(new Ui.Info("Place & resize them in the HUD editor."));
	}

	private void thumb(GuiGraphicsExtractor g, java.nio.file.Path f, int x, int y, int w, int h, int max) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
		dev.vanillaclient.images.Thumbs.Thumb t = dev.vanillaclient.images.Thumbs.get(f, max);
		if (t == null) {
			g.fill(x, y, x + w, y + h, 0xFF1A1A1A);
			return;
		}
		float k = Math.min(w / (float) t.w(), h / (float) t.h());
		int dw = Math.max(1, Math.round(t.w() * k)), dh = Math.max(1, Math.round(t.h() * k));
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, t.id(), x + (w - dw) / 2, y + (h - dh) / 2, 0f, 0f, dw, dh, t.w(), t.h(), t.w(), t.h());
	}

	private void keystrokes() {
		Config.Keystrokes k = cfg().keystrokes;
		list.add(new Ui.Header("KEYSTROKES"));
		list.add(new Ui.Toggle("Show on screen", i(Items.STONE_BUTTON), () -> k.enabled, v -> k.enabled = v));
		list.add(new Ui.Custom(118, (g, x, y, w, h, mx, my, a) -> {
			dev.vanillaclient.hud.KeystrokesModule m = dev.vanillaclient.hud.HudManager.KEYSTROKES;
			float sc = Math.min(1f, 112f / Math.max(1, m.height(true)));
			g.pose().pushMatrix();
			g.pose().translate(x + w / 2f - m.width(true) * sc / 2f, y + 3);
			g.pose().scale(sc, sc);
			m.draw(g, true);
			g.pose().popMatrix();
		}));
		list.add(new Ui.Header("PARTS"));
		list.add(new Ui.Toggle("Mouse buttons", null, () -> k.showMouse, v -> k.showMouse = v));
		list.add(new Ui.Toggle("Clicks per second", null, () -> k.showCps, v -> k.showCps = v));
		list.add(new Ui.Toggle("LMB / RMB text", null, () -> k.showLabelsOnMouse, v -> k.showLabelsOnMouse = v));
		list.add(new Ui.Toggle("Sneak & sprint", null, () -> k.showSneakSprint, v -> k.showSneakSprint = v));
		list.add(new Ui.Toggle("Space bar", null, () -> k.showSpace, v -> k.showSpace = v));
		list.add(new Ui.Cycle<>("Labels", java.util.List.of(0, 1), () -> k.labelStyle, v -> k.labelStyle = v, v -> v == 1 ? "Arrows ▲" : "Key names"));
		list.add(new Ui.Header("SHAPE"));
		list.add(new Ui.Slider("Key size", 12, 32, 1, () -> k.keySize, v -> k.keySize = (int) v, v -> (int) v + " px"));
		list.add(new Ui.Slider("Gap", 0, 8, 1, () -> k.gap, v -> k.gap = (int) v, v -> (int) v + " px"));
		list.add(new Ui.Toggle("Rounded corners", null, () -> k.rounded, v -> k.rounded = v));
		list.add(new Ui.Toggle("Border", null, () -> k.outline, v -> k.outline = v));
		list.add(new Ui.Slider("Fade speed", 0.3, 3.0, 0.1, () -> k.fadeSpeed, v -> k.fadeSpeed = (float) v, v -> Math.round(v * 100) + "%"));
		list.add(new Ui.Slider("Size", 0.5, 3.0, 0.05, () -> k.scale, v -> k.scale = (float) v, v -> Math.round(v * 100) + "%"));
		list.add(new Ui.Header("COLORS"));
		list.add(new Ui.ColorPick("Key", () -> k.keyColor, v -> k.keyColor = v, this::colorPicker));
		list.add(new Ui.ColorPick("Key pressed", () -> k.pressedColor, v -> k.pressedColor = v, this::colorPicker));
		list.add(new Ui.ColorPick("Text", () -> k.color, v -> k.color = v, this::colorPicker));
		list.add(new Ui.ColorPick("Text pressed", () -> k.pressedTextColor, v -> k.pressedTextColor = v, this::colorPicker));
		list.add(new Ui.ColorPick("Border", () -> k.borderColor, v -> k.borderColor = v, this::colorPicker));
		list.add(new Ui.Toggle("Text shadow", null, () -> k.textShadow, v -> k.textShadow = v));
		list.add(new Ui.Header("PRESETS"));
		list.add(new Ui.Button("Classic (dark, white pressed)", null, () -> {
			k.keyColor = 0x80000000; k.pressedColor = 0xD0FFFFFF; k.color = 0xFFFFFFFF; k.pressedTextColor = 0xFF000000; k.rounded = false; k.outline = false;
		}));
		list.add(new Ui.Button("Neon (outline, cyan glow)", null, () -> {
			k.keyColor = 0x60000000; k.pressedColor = 0xC000E5FF; k.color = 0xFF7FF4FF; k.pressedTextColor = 0xFF001018; k.rounded = true; k.outline = true; k.borderColor = 0xA000E5FF;
		}));
		list.add(new Ui.Button("Gold", null, () -> {
			k.keyColor = 0x70201A08; k.pressedColor = 0xE0F2C440; k.color = 0xFFF6D77A; k.pressedTextColor = 0xFF2A1A00; k.rounded = true; k.outline = true; k.borderColor = 0x80F2C440;
		}));
		list.add(new Ui.Button("Move it in the HUD editor", i(Items.ITEM_FRAME), () -> open(new HudEditorScreen(this))));
	}

	private void waypoints() {
		Config.WaypointHud w = cfg().waypointHud;
		list.add(new Ui.Header("WAYPOINTS"));
		list.add(new Ui.Cycle<>("Show waypoints", List.of(1, 2, 0), () -> w.mode, v -> w.mode = v,
				v -> switch (v) {
					case 1 -> "On the XP bar";
					case 2 -> "Compass at the top";
					default -> "Off";
				}));
		list.add(new Ui.Toggle("Show distance", null, () -> w.showDistance, v -> w.showDistance = v));
		list.add(new Ui.Slider("Hide farther than", 0, 5000, 100, () -> w.maxDistance, v -> w.maxDistance = (int) v,
				v -> v <= 0 ? "never" : (int) v + " m"));
		list.add(new Ui.Button("Open the world map", i(Items.FILLED_MAP), () -> open(new WorldMapScreen(this))));
		list.add(new Ui.Info("Press B in game to save a waypoint where you stand.",
				"Right-click the world map to place one anywhere.",
				"Look towards a waypoint to see its name and distance."));
		java.util.List<Config.Waypoint> here = dev.vanillaclient.map.Waypoints.here();
		if (!here.isEmpty()) list.add(new Ui.Header("IN THIS WORLD"));
		var pl = this.minecraft.player;
		for (Config.Waypoint wp : here) {
			String dist = pl == null ? "" : " §7" + Math.round(Math.hypot(wp.x - pl.getX(), wp.z - pl.getZ())) + "m";
			ItemStack ic = dev.vanillaclient.map.Waypoints.iconStack(wp);
			list.add(new Ui.Toggle("§f" + wp.name + dist + " §8" + wp.x + " " + wp.y + " " + wp.z,
					ic.isEmpty() ? i(Items.RECOVERY_COMPASS) : ic, () -> wp.visible, v -> wp.visible = v));
		}
		if (!here.isEmpty()) list.add(new Ui.Info("Click a waypoint on the world map to rename it,", "give it an icon or a color, or drag it."));
	}

	private void crosshair() {
		Config.Crosshair s = cfg().crosshair;
		list.add(new Ui.Header("CROSSHAIR"));
		list.add(new Ui.Toggle("Use my crosshair", i(Items.TARGET), () -> s.enabled, v -> s.enabled = v));
		list.add(new Ui.Button("Paint it …", i(Items.FEATHER), () -> open(new CrosshairEditorScreen(this))));
		list.add(new Ui.Custom(64, (g, x, y, w, h, mx, my, a) -> {
			int px = x + w / 2, py = y + h / 2;
			g.fill(x, y, x + w, y + h / 2, Theme.alpha(0xFF7BA4E0, a));
			g.fill(x, y + h / 2, x + w, y + h, Theme.alpha(0xFF5E8C3A, a));
			g.outline(x, y, w, h, Theme.alpha(0x60FFFFFF, a));
			CrosshairRenderer.draw(g, s, px, py, Math.max(1, Math.round(s.scale)));
		}));
		list.add(new Ui.ColorPick("Color", () -> s.color, v -> s.color = v, this::colorPicker));
		list.add(new Ui.Slider("Size", 1, 5, 1, () -> s.scale, v -> s.scale = (float) v, v -> (int) v + "×"));
		list.add(new Ui.Toggle("Vanilla blending (inverts colors)", null, () -> s.vanillaBlend, v -> s.vanillaBlend = v));
		list.add(new Ui.Header("OUTLINE"));
		list.add(new Ui.Toggle("Outline", null, () -> s.outline, v -> s.outline = v));
		list.add(new Ui.Slider("Thickness", 1, 3, 1, () -> s.outlineThickness, v -> s.outlineThickness = (int) v, v -> (int) v + " px"));
		list.add(new Ui.Slider("Opacity", 0.1, 1.0, 0.05, () -> s.outlineOpacity, v -> s.outlineOpacity = (float) v, v -> Math.round(v * 100) + "%"));
		list.add(new Ui.ColorPick("Outline color", () -> s.outlineColor, v -> s.outlineColor = v, this::colorPicker));
		list.add(new Ui.Header("EXTRA"));
		list.add(new Ui.Toggle("Other color on mobs/players", null, () -> s.dynamicColor, v -> s.dynamicColor = v));
		list.add(new Ui.ColorPick("Color on target", () -> s.targetColor, v -> s.targetColor = v, this::colorPicker));
		list.add(new Ui.Toggle("Attack cooldown bar", null, () -> s.attackIndicator, v -> s.attackIndicator = v));
		list.add(new Ui.Toggle("Also in third person", null, () -> s.thirdPerson, v -> s.thirdPerson = v));
		list.add(new Ui.Info("Tip: the color's opacity makes it see-through."));
	}

	private void zoom() {
		Config.Zoom z = cfg().zoom;
		list.add(new Ui.Header("ZOOM"));
		list.add(new Ui.Toggle("Zoom enabled", i(Items.SPYGLASS), () -> z.enabled, v -> z.enabled = v));
		list.add(new Ui.Slider("Zoom level", 1.5, 30, 0.5, () -> z.level, v -> z.level = (float) v, v -> v + "×"));
		list.add(new Ui.Toggle("Smooth transition", null, () -> z.smoothTransition, v -> z.smoothTransition = v));
		list.add(new Ui.Toggle("Scroll to adjust", null, () -> z.scrollToAdjust, v -> z.scrollToAdjust = v));
		list.add(new Ui.Toggle("Reset scroll when released", null, () -> z.resetLevelOnRelease, v -> z.resetLevelOnRelease = v));
		list.add(new Ui.Toggle("Cinematic camera", null, () -> z.cinematicCamera, v -> z.cinematicCamera = v));
		list.add(new Ui.Toggle("Hide hand while zooming", null, () -> z.hideHand, v -> z.hideHand = v));
		list.add(new Ui.Button("Change the zoom key", i(Items.LEVER), () -> go(Page.CONTROLS)));
	}

	private void held() {
		Config.HeldItems h = cfg().heldItems;
		list.add(new Ui.Header("HELD ITEMS"));
		list.add(new Ui.Toggle("Change sizes", i(Items.TOTEM_OF_UNDYING), () -> h.enabled, v -> h.enabled = v));
		list.add(new Ui.Custom(56, (g, x, y, w, hh, mx, my, a) -> {
			float bob = (float) Math.sin(Theme.time() * 2.0) * 2f;
			Item[] demo = {Items.SHIELD, Items.DIAMOND_SWORD, Items.COOKED_BEEF, Items.GRASS_BLOCK, Items.TOTEM_OF_UNDYING};
			for (int k = 0; k < demo.length; k++) {
				ItemStack st = i(demo[k]);
				float sc = 1.6f * (h.enabled && !st.isEmpty() ? dev.vanillaclient.held.HeldItems.scaleFor(st) : 1f);
				float cxp = x + w * (k + 0.5f) / demo.length;
				Icons.scaled(g, st, cxp - 8 * sc, y + 26 - 8 * sc + (k % 2 == 0 ? bob : -bob), sc);
			}
		}));
		list.add(new Ui.Header("BY CATEGORY  §8· right-click = 100%"));
		for (dev.vanillaclient.held.HeldItems.Category c : dev.vanillaclient.held.HeldItems.CATEGORIES) {
			list.add(new Ui.Slider(c.name(), 0.2, 1.6, 0.05, () -> dev.vanillaclient.held.HeldItems.category(c.id()),
					v -> dev.vanillaclient.held.HeldItems.setCategory(c.id(), (float) v), v -> Math.round(v * 100) + "%")
					.icon(i(c.icon())).onReset(() -> dev.vanillaclient.held.HeldItems.setCategory(c.id(), 1f)));
		}
		list.add(new Ui.Button("Every item one by one … §8(" + h.items.size() + " changed)", i(Items.CHEST), () -> open(new HeldItemsScreen(this)), 22));
		list.add(new Ui.Header("TOTEM POP"));
		list.add(new Ui.Slider("Totem pop animation", 0.2, 1.5, 0.05, () -> h.totemPopScale, v -> h.totemPopScale = (float) v, v -> Math.round(v * 100) + "%"));
		list.add(new Ui.Info("Only changes how items look in your own hands."));
	}

	private void info(int index) {
		dev.vanillaclient.hud.HudModule m = dev.vanillaclient.hud.HudManager.INFO.get(index);
		list.add(new Ui.Header(m.name.toUpperCase(java.util.Locale.ROOT)));
		Config.Hud hs = m.settings();
		list.add(new Ui.Toggle("Show on screen", m.icon(), () -> hs.enabled, v -> hs.enabled = v));
		if (hs instanceof Config.Counter c) {
			list.add(new Ui.Toggle("Hide items you don't have", null, () -> c.hideZero, v -> c.hideZero = v));
			list.add(new Ui.Toggle("Vertical", null, () -> c.vertical, v -> c.vertical = v));
			list.add(new Ui.Header("COUNT THESE"));
			String[][] opts = {{"totem_of_undying", "Totems"}, {"golden_apple", "Golden apples"}, {"enchanted_golden_apple", "Enchanted apples"},
					{"ender_pearl", "Ender pearls"}, {"arrow", "Arrows"}, {"spectral_arrow", "Spectral arrows"}, {"cobweb", "Cobwebs"},
					{"water_bucket", "Water buckets"}, {"lava_bucket", "Lava buckets"}, {"experience_bottle", "XP bottles"},
					{"cobblestone", "Cobblestone"}, {"oak_planks", "Oak planks"}, {"obsidian", "Obsidian"}, {"tnt", "TNT"},
					{"wind_charge", "Wind charges"}, {"firework_rocket", "Rockets"}};
			for (String[] o : opts) {
				Item it = dev.vanillaclient.hud.InfoModules.Counter.item(o[0]);
				if (it == Items.AIR) continue;
				list.add(new Ui.Toggle(o[1], i(it), () -> c.items.contains(o[0]), v -> {
					if (v && !c.items.contains(o[0])) c.items.add(o[0]);
					if (!v) c.items.remove(o[0]);
				}));
			}
			style(c, true);
			return;
		}
		if (hs instanceof Config.TextHud t && !(m instanceof dev.vanillaclient.hud.InfoModules.Effects)) {
			labelOptions(t, null);
		} else if (hs instanceof Config.TextHud t) {
			list.add(new Ui.ColorPick("Time color", () -> t.labelColor, v -> t.labelColor = v, this::colorPicker));
		}
		style(hs, true);
	}

	private void rank() {
		Config.Ranks r = cfg().ranks;
		var me = this.minecraft.getUser();
		boolean owner = dev.vanillaclient.features.ClientRanks.isOwner(me.getName(), me.getProfileId());
		list.add(new Ui.Custom(34, (g, x, y, w, h, mx, my, a) -> drawRankBanner(g, x, y, w, a)));
		perks(r, owner);
		list.add(new Ui.Header(owner ? "RANKS  §8· owner account: click a rank to wear it" : "RANKS  §8· not available yet"));
		java.util.List<dev.vanillaclient.features.ClientRanks.Rank> all = dev.vanillaclient.features.ClientRanks.ALL;
		for (int k = all.size() - 1; k >= 0; k--) {
			dev.vanillaclient.features.ClientRanks.Rank rk = all.get(k);
			Ui.Custom card = new Ui.Custom(24, (g, x, y, w, h, mx, my, a) -> {
				dev.vanillaclient.features.ClientRanks.Rank cur = dev.vanillaclient.features.ClientRanks.mine();
				boolean wearing = cur == rk;
				boolean hov = mx >= x && mx < x + w && my >= y && my < y + 22;
				int c = 0xFF000000 | rk.from();
				g.fillGradient(x, y, x + w, y + 22, Theme.alpha(Theme.lerpColor(c, 0xFF101010, wearing ? 0.45f : hov && owner ? 0.6f : 0.8f), a), Theme.alpha(0x50000000, a));
				if (wearing) g.outline(x, y, w, 22, Theme.alpha(c, a));
				net.minecraft.network.chat.MutableComponent t = dev.vanillaclient.features.ClientRanks.tag(rk);
				g.text(this.font, t, x + 6, y + 7, Theme.alpha(owner ? 0xFFFFFFFF : 0x90FFFFFF, a), true);
				int pc = dev.vanillaclient.features.ClientRanks.perkCount(rk);
				String st = wearing ? "§a✔ wearing" : owner && hov ? "§eWear" : (owner ? "§7" : "§8locked · ") + pc + " perk" + (pc == 1 ? "" : "s");
				g.text(this.font, st, x + w - 6 - this.font.width(st), y + 7, Theme.alpha(0xFFFFFFFF, a), false);
			}, () -> {
				if (owner) {
					r.display = rk.id().equals("owner") ? "" : rk.id();
					Config.save();
				}
			});
			card.span = 2;
			list.add(card);
		}
		list.add(new Ui.Header("NAME TAG"));
		list.add(new Ui.Toggle("Show your name in F5 (third person)", i(Items.NAME_TAG), () -> r.ownNameTag, v -> r.ownNameTag = v));
		list.add(new Ui.Toggle("Rank in front of your name", null, () -> r.symbolInName, v -> r.symbolInName = v));
		list.add(new Ui.Header("OTHER PLAYERS"));
		list.add(new Ui.Toggle("Show ranks of other players", null, () -> r.showClientRanks, v -> r.showClientRanks = v));
		list.add(new Ui.Toggle("Share my cosmetics with other players", null, () -> r.shareCosmetics, v -> r.shareCosmetics = v));
		list.add(new Ui.Toggle("Show other players' cosmetics", null, () -> r.showOthers, v -> r.showOthers = v));
		list.add(new Ui.Toggle("Load the rank list from GitHub", null, () -> r.onlineRanks, v -> r.onlineRanks = v));
		list.add(new Ui.Info("Ranks can't be bought or earned yet.",
				"Cosmetics are shared on LAN worlds and servers with Vanilla Client."));
	}

	private void perks(Config.Ranks r, boolean owner) {
		dev.vanillaclient.features.ClientRanks.Rank mine = dev.vanillaclient.features.ClientRanks.mine();
		list.add(new Ui.Header("RANK PERKS  §8· " + (mine == null ? "get a rank to unlock" : "unlocked by " + mine.tag())));
		// live preview of your name tag
		Ui.Custom preview = new Ui.Custom(30, (g, x, y, w, h, mx, my, a) -> {
			g.fillGradient(x, y, x + w, y + 28, Theme.alpha(0xC0181820, a), Theme.alpha(0x80000000, a));
			g.outline(x, y, w, 28, Theme.alpha(0x30FFFFFF, a));
			dev.vanillaclient.features.ClientRanks.Rank rk = dev.vanillaclient.features.ClientRanks.mine();
			net.minecraft.network.chat.Component name = rk == null ? Component.literal(this.minecraft.getUser().getName())
					: dev.vanillaclient.features.NameStyle.mine().build(this.minecraft.getUser().getName(), rk, (float) Theme.time());
			int nw = this.font.width(name) + 8;
			int nx = x + w / 2 - nw / 2;
			g.fill(nx, y + 9, nx + nw, y + 21, Theme.alpha(0x60000000, a));
			g.text(this.font, name, nx + 4, y + 11, Theme.alpha(0xFFFFFFFF, a), false);
			g.text(this.font, "§8preview", x + 5, y + 3, Theme.alpha(0xFFFFFFFF, a), false);
		});
		preview.span = 2;
		list.add(preview);
		for (dev.vanillaclient.features.ClientRanks.Perk p : dev.vanillaclient.features.ClientRanks.Perk.values()) {
			if (!dev.vanillaclient.features.ClientRanks.has(mine, p)) {
				dev.vanillaclient.features.ClientRanks.Rank need = dev.vanillaclient.features.ClientRanks.ALL.get(p.tier);
				list.add(new Ui.Custom(20, (g, x, y, w, h, mx, my, a) -> {
					g.fill(x, y, x + w, y + 18, Theme.alpha(0x40000000, a));
					g.text(this.font, "§8✖ §7" + p.label, x + 6, y + 5, Theme.alpha(0xFFFFFFFF, a), false);
					net.minecraft.network.chat.MutableComponent t = Component.literal("§8needs ").append(dev.vanillaclient.features.ClientRanks.tag(need));
					g.text(this.font, t, x + w - 6 - this.font.width(t), y + 5, Theme.alpha(0xFFFFFFFF, a), false);
				}));
				continue;
			}
			switch (p) {
				case COLOR -> list.add(new Ui.ColorPick("Name color", () -> r.nameColor == 0 ? 0xFF000000 | mine.nameColor() : r.nameColor,
						v -> r.nameColor = v, this::colorPicker));
				case BOLD -> {
					list.add(new Ui.Toggle("Bold name", null, () -> r.boldName, v -> r.boldName = v));
					list.add(new Ui.Toggle("Italic name", null, () -> r.italicName, v -> r.italicName = v));
				}
				case GRADIENT -> list.add(new Ui.ColorPick("Gradient to (second color)", () -> r.nameColor2 == 0 ? 0xFF000000 | mine.to() : r.nameColor2,
						v -> r.nameColor2 = v, this::colorPicker));
				case ANIMATED -> list.add(new Ui.Toggle("Animated gradient", null, () -> r.animatedName, v -> r.animatedName = v));
				case FONT -> list.add(new Ui.Cycle<>("Name font", java.util.List.of(dev.vanillaclient.features.ClientRanks.FONTS), () -> r.nameFont,
						v -> r.nameFont = v, dev.vanillaclient.features.ClientRanks::fontName));
				case BADGE -> {
					java.util.List<String> b = new java.util.ArrayList<>();
					b.add("");
					b.addAll(java.util.List.of(dev.vanillaclient.features.ClientRanks.BADGES));
					list.add(new Ui.Cycle<>("Badge in front of your name", b, () -> r.badge, v -> r.badge = v, v -> v.isEmpty() ? "Rank symbol" : v));
				}
			}
		}
		if (mine != null) list.add(new Ui.Toggle("Show rank title (e.g. OWNER) in the name", null, () -> r.showRankTitle, v -> r.showRankTitle = v));
		if (mine != null) list.add(new Ui.Button("Reset name look", i(Items.BARRIER), () -> {
			r.nameColor = r.nameColor2 = 0;
			r.animatedName = r.boldName = r.italicName = false;
			r.nameFont = "default";
			r.badge = "";
			Config.save();
		}));
	}

	private void controls() {
		list.add(new Ui.Header("VANILLA CLIENT  §8· click, then press a key · Esc = none · right-click = default"));
		String[][] ours = {{"key.vanillaclient.open", "Open this menu"}, {"key.vanillaclient.zoom", "Zoom (hold)"},
				{"key.vanillaclient.map", "World map"}, {"key.vanillaclient.waypoint", "Save a waypoint"},
				{"key.vanillaclient.hud_editor", "Edit HUD"}, {"key.vanillaclient.toggle_todo", "Show / hide task list"},
				{"key.vanillaclient.cosmetics", "Wardrobe"}, {"key.vanillaclient.tasks", "Task planner"},
				{"key.vanillaclient.sort", "Sort open chest"}};
		for (String[] k : ours) {
			net.minecraft.client.KeyMapping km = net.minecraft.client.KeyMapping.get(k[0]);
			if (km != null) list.add(new Ui.KeyBind(k[1], km));
		}
		list.add(new Ui.Header("MINECRAFT"));
		for (net.minecraft.client.KeyMapping km : this.minecraft.options.keyMappings) {
			if (km.getName().startsWith("key.vanillaclient")) continue;
			list.add(new Ui.KeyBind(Component.translatable(km.getName()).getString(), km));
		}
	}

	private void general() {
		Config c = cfg();
		list.add(new Ui.Header("MENU"));
		list.add(new Ui.Toggle("Vanilla Client & Wardrobe in the pause menu", i(Items.WRITABLE_BOOK), () -> c.pauseMenuButton, v -> c.pauseMenuButton = v));
		list.add(new Ui.Toggle("Advancement & stats helper buttons", i(Items.GOLDEN_APPLE), () -> c.helperButtons, v -> c.helperButtons = v));
		list.add(new Ui.Header("WORLD MAP"));
		list.add(new Ui.Toggle("Record explored land", i(Items.FILLED_MAP), () -> c.worldMap.enabled, v -> c.worldMap.enabled = v));
		list.add(new Ui.Slider("Map radius", 0, 32, 1, () -> c.worldMap.radius, v -> c.worldMap.radius = (int) v,
				v -> v <= 0 ? "Auto (render distance)" : (int) v + " chunks"));
		list.add(new Ui.Toggle("Show tasks on the map", null, () -> c.worldMap.showTasks, v -> c.worldMap.showTasks = v));
		list.add(new Ui.Button("Open the world map", i(Items.FILLED_MAP), () -> open(new WorldMapScreen(this))));
		list.add(new Ui.Header("FILES"));
		list.add(new Ui.Button("Open settings folder", i(Items.CHEST), () -> Platform.open(Config.dir())));
		list.add(new Ui.Header("CHESTS"));
		list.add(new Ui.Toggle("Sort & Store buttons on chests", i(Items.CHEST), () -> c.chestButtons, v -> c.chestButtons = v));
		list.add(new Ui.Button("Change keys", i(Items.LEVER), () -> go(Page.CONTROLS)));
	}

	private void say(String s) {
		status = s;
		statusAt = System.currentTimeMillis();
	}

	// ------------------------------------------------------------ sidebar

	private record Entry(Page page, String header, int y) {}

	private int entryH() {
		return 14;
	}

	private int headerH() {
		return HEADER;
	}

	private int sidebarContentH() {
		int groups = 0;
		String g = null;
		for (Page p : Page.values()) {
			if (p.group != null && !p.group.equals(g)) {
				g = p.group;
				groups++;
			}
		}
		return Page.values().length * entryH() + groups * headerH() + 4;
	}

	private List<Entry> sidebar() {
		int ENTRY = entryH();
		List<Entry> out = new ArrayList<>();
		int y = top() - Math.round(sideScroll);
		String group = null;
		for (Page p : Page.values()) {
			if (p.group != null && !p.group.equals(group)) {
				group = p.group;
				out.add(new Entry(null, group, y));
				y += headerH();
			}
			out.add(new Entry(p, null, y));
			y += ENTRY;
		}
		return out;
	}

	private void drawSidebar(GuiGraphicsExtractor g, int mx, int my) {
		int ENTRY = entryH();
		int x = left();
		Theme.panel(g, x - 4, top() - 4, SIDEBAR + 8, bottom() - top() + 6);
		int maxScroll = Math.max(0, sidebarContentH() - (bottom() - top()));
		sideScrollTarget = Math.max(0, Math.min(maxScroll, sideScrollTarget));
		sideScroll += (sideScrollTarget - sideScroll) * 0.3f;
		g.enableScissor(x - 4, top() - 2, x + SIDEBAR + 4, bottom());
		float targetY = 0;
		for (Entry e : sidebar()) if (e.page() == page) targetY = e.y();
		if (indicatorY < 0) indicatorY = targetY;
		indicatorY += (targetY - indicatorY) * 0.25f;
		g.fill(x - 2, Math.round(indicatorY), x + SIDEBAR + 2, Math.round(indicatorY) + ENTRY - 1, 0x40FFFFFF);
		g.fill(x - 2, Math.round(indicatorY), x, Math.round(indicatorY) + ENTRY - 1, Theme.ACCENT);
		long now = System.currentTimeMillis();
		int idx = 0;
		for (Entry e : sidebar()) {
			float t = Theme.easeOut((now - openedAt - idx * 25L) / 250f);
			idx++;
			int off = Math.round((1f - t) * -12f);
			if (e.header() != null) {
				g.text(this.font, e.header(), x + 2 + off, e.y() + 2, Theme.alpha(0xFF808080, t), false);
				continue;
			}
			Page p = e.page();
			boolean hover = mx >= x && mx < x + SIDEBAR && my >= e.y() && my < e.y() + ENTRY;
			entryHover[p.ordinal()] += ((hover ? 1f : 0f) - entryHover[p.ordinal()]) * 0.3f;
			int slide = Math.round(entryHover[p.ordinal()] * 3f) + off;
			int ty = e.y() + (ENTRY - 8) / 2;
			Icons.small(g, Icons.stack(p.icon), x + 4 + slide, ty);
			int col = p == page ? Theme.ACCENT : Theme.lerpColor(0xFFC8C8C8, 0xFFFFFFFF, entryHover[p.ordinal()]);
			g.text(this.font, p.label, x + 16 + slide, ty, Theme.alpha(col, t), false);
		}
	}

	private void sidebarScrollbar(GuiGraphicsExtractor g) {
		g.disableScissor();
		int viewH = bottom() - top();
		int contentH = sidebarContentH();
		if (contentH > viewH) {
			int x = left() + SIDEBAR + 1;
			int barH = Math.max(12, viewH * viewH / contentH);
			int barY = top() + Math.round((viewH - barH) * (sideScroll / Math.max(1, contentH - viewH)));
			g.fill(x, barY, x + 1, barY + barH, 0x90FFFFFF);
		}
	}

	// ------------------------------------------------------------ drawing & input

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		// slow floating light specks behind the panels
		float t = Theme.time();
		for (int k = 0; k < 36; k++) {
			float seed = k * 12.9898f;
			float fx = (float) ((Math.sin(seed) * 43758.5453) % 1 + 1) % 1;
			float speed = 4f + (k % 5) * 2.5f;
			float fy = 1f - ((t * speed / Math.max(1, this.height) + k * 0.137f) % 1f);
			int x = Math.round(fx * this.width + (float) Math.sin(t * 0.6 + k) * 6f);
			int y = Math.round(fy * this.height);
			float tw = 0.35f + 0.35f * (float) Math.sin(t * 2 + k);
			int c = k % 3 == 0 ? Theme.ACCENT : 0xFFFFFFFF;
			g.fill(x, y, x + (k % 4 == 0 ? 2 : 1), y + (k % 4 == 0 ? 2 : 1), Theme.alpha(c, tw * 0.6f));
		}
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		float in = intro();
		logo.draw(g, this.font, this.width / 2, 6, 1f, in);
		// breadcrumb with typewriter animation
		String crumb = (page.group == null ? "" : page.group.charAt(0) + page.group.substring(1).toLowerCase() + " › ") + page.label;
		int chars = (int) Math.min(crumb.length(), (System.currentTimeMillis() - pageAt) / 18);
		String shown = crumb.substring(0, chars) + (chars < crumb.length() ? "_" : "");
		g.centeredText(this.font, "§7" + shown, this.width / 2, 29, 0xFFFFFFFF);

		drawSidebar(g, mouseX, mouseY);
		sidebarScrollbar(g);
		Theme.panel(g, cx() - 4, top() - 4, contentW() + 10, bottom() - top() + 6);
		list.render(g, this.font, mouseX, mouseY);

		if (System.currentTimeMillis() - statusAt < 4000) {
			float a = 1f - Math.max(0f, (System.currentTimeMillis() - statusAt - 3500) / 500f);
			g.centeredText(this.font, status, this.width / 2, this.height - 40, Theme.alpha(Theme.ACCENT, a));
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		double mx = event.x(), my = event.y();
		if (mx >= left() && mx < left() + SIDEBAR) {
			for (Entry e : sidebar()) {
				if (e.page() != null && my >= e.y() && my < e.y() + entryH() && my >= top() && my < bottom()) {
					Ui.click();
					go(e.page());
					return true;
				}
			}
		}
		return list.mouseDown(mx, my, event.button());
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (Ui.KeyBind.keyPressed(event)) return true;
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		list.mouseUp();
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		if (mx >= left() - 4 && mx < left() + SIDEBAR + 4 && my >= top() && my < bottom()) {
			sideScrollTarget -= (float) v * 28f;
			return true;
		}
		if (list.scroll(mx, my, v)) return true;
		return super.mouseScrolled(mx, my, h, v);
	}

	/** Import pictures by drag & drop from the file explorer. */
	@Override
	public void onFilesDrop(List<Path> paths) {
		int n = ImageManager.importFiles(paths);
		if (n > 0) {
			go(Page.IMAGES);
			buildPage();
			say("✔ " + n + " picture(s) added – place them in the HUD editor");
		} else {
			say("§cOnly PNG or JPG files");
		}
	}
}
