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
		IMAGES("HUD", "Images", Items.PAINTING),
		WAYPOINTS("HUD", "Waypoints", Items.RECOVERY_COMPASS),
		INFO("HUD", "Info Lines", Items.REDSTONE),
		CROSSHAIR("VISUALS", "Crosshair", Items.TARGET),
		ZOOM("VISUALS", "Zoom", Items.SPYGLASS),
		HELD("VISUALS", "Held Items", Items.TOTEM_OF_UNDYING),
		RANK("GENERAL", "Rank & Name", Items.NETHER_STAR),
		CONTROLS("GENERAL", "Controls", Items.LEVER),
		GENERAL("GENERAL", "Settings", Items.COMPARATOR);

		final String group;
		final String label;
		final Item icon;

		Page(String group, String label, Item icon) {
			this.group = group;
			this.label = label;
			this.icon = icon;
		}
	}

	private static Page page = Page.HOME;
	private static int infoIndex = 0;

	private static final int SIDEBAR = 106, GAP = 10, HEADER = 12;

	private final Logo logo = new Logo();
	private final Ui.List_ list = new Ui.List_();
	private long pageAt = System.currentTimeMillis();
	private float indicatorY = -1;
	private final float[] entryHover = new float[Page.values().length];
	private String status = "";
	private long statusAt;

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
			case WAYPOINTS -> waypoints();
			case INFO -> info();
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
		java.util.List<dev.vanillaclient.hud.HudModule> info = dev.vanillaclient.hud.HudManager.INFO;
		for (int k = 0; k < info.size(); k++) {
			final int idx = k;
			dev.vanillaclient.hud.HudModule m = info.get(k);
			list.add(new Ui.Chip(m.name, m.icon(), () -> m.settings().enabled, v -> m.settings().enabled = v, () -> {
				infoIndex = idx;
				go(Page.INFO);
			}));
		}
		list.add(new Ui.Header("TOOLS"));
		list.add(new Ui.Tile("Task Planner", "Farms, builds, trips", i(Items.WRITABLE_BOOK), () -> open(new TasksScreen(this))));
		list.add(new Ui.Tile("World Map", "Map & waypoints", i(Items.FILLED_MAP), () -> open(new WorldMapScreen(this))));
		list.add(new Ui.Tile("Wardrobe", "Capes, wings, shields", i(Items.ELYTRA), () -> open(new CosmeticsScreen(this))));
		list.add(new Ui.Tile("UHC Guide", "Trades, enchants, brewing", i(Items.ENCHANTED_BOOK), () -> open(new GuideScreen(this))));
		list.add(new Ui.Tile("Advancements", "Every one, what to do", i(Items.GOLDEN_APPLE), () -> open(new AdvancementsChecklistScreen(this))));
		list.add(new Ui.Tile("Statistics", "Your world in numbers", i(Items.BOOK), () -> open(new StatsOverviewScreen(this))));
	}

	private void drawRankBanner(GuiGraphicsExtractor g, int x, int y, int w, float a) {
		dev.vanillaclient.features.Ranks.Rank r = dev.vanillaclient.features.Ranks.current();
		dev.vanillaclient.features.Ranks.Rank next = dev.vanillaclient.features.Ranks.next();
		double h = dev.vanillaclient.features.Ranks.hours();
		g.fill(x, y, x + w, y + 32, Theme.alpha(0x50000000, a));
		g.fill(x, y, x + 2, y + 32, Theme.alpha(r.color(), a));
		g.outline(x, y, w, 32, Theme.alpha(0x40FFFFFF, a));
		String name = this.minecraft.getUser().getName();
		g.pose().pushMatrix();
		g.pose().translate(x + 8, y + 6);
		g.pose().scale(2f, 2f);
		g.text(this.font, r.symbol(), 0, 0, Theme.alpha(r.color(), a), true);
		g.pose().popMatrix();
		g.text(this.font, "§f" + name + " §7· " + r.name(), x + 28, y + 6, Theme.alpha(r.color(), a), false);
		String sub = "§7" + dev.vanillaclient.features.Ranks.formatHours(h) + " played"
				+ (next == null ? " §6· max rank!" : " §8· next: " + next.symbol() + " " + next.name() + " at " + next.hours() + "h");
		g.text(this.font, this.font.plainSubstrByWidth(sub, w - 34), x + 28, y + 17, Theme.alpha(0xFFFFFFFF, a), false);
		if (next != null) {
			double from = r.hours() <= h ? dev.vanillaclient.features.Ranks.best().hours() : 0;
			float f = (float) Math.max(0, Math.min(1, (h - from) / Math.max(0.01, next.hours() - from)));
			g.fill(x + 28, y + 27, x + w - 6, y + 29, Theme.alpha(0x60000000, a));
			g.fill(x + 28, y + 27, x + 28 + Math.round((w - 34) * f), y + 29, Theme.alpha(next.color(), a));
		}
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
		list.add(new Ui.Header("YOUR PICTURES"));
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
			String name = this.font.plainSubstrByWidth(img.file, contentW() - 60);
			list.add(new Ui.Toggle(name, i(Items.PAINTING), () -> img.enabled, v -> img.enabled = v));
			list.add(new Ui.Slider("  Opacity", 0.05, 1.0, 0.05, () -> ((img.tint >>> 24) & 0xFF) / 255.0,
					v -> img.tint = (img.tint & 0x00FFFFFF) | ((int) Math.round(v * 255) << 24), v -> Math.round(v * 100) + "%"));
		}
		list.add(new Ui.Info("Place & resize them in the HUD editor."));
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

	private void info() {
		java.util.List<dev.vanillaclient.hud.HudModule> mods = dev.vanillaclient.hud.HudManager.INFO;
		infoIndex = Math.floorMod(infoIndex, mods.size());
		java.util.List<Integer> idx = new ArrayList<>();
		for (int k = 0; k < mods.size(); k++) idx.add(k);
		list.add(new Ui.Header("INFO LINES"));
		list.add(new Ui.Cycle<>("Module", idx, () -> infoIndex, v -> {
			infoIndex = v;
			buildPage();
		}, v -> mods.get(v).name));
		dev.vanillaclient.hud.HudModule m = mods.get(infoIndex);
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
		list.add(new Ui.Custom(34, (g, x, y, w, h, mx, my, a) -> drawRankBanner(g, x, y, w, a)));
		list.add(new Ui.Header("NAME"));
		list.add(new Ui.Toggle("Show your name in F5 (third person)", i(Items.NAME_TAG), () -> r.ownNameTag, v -> r.ownNameTag = v));
		list.add(new Ui.Toggle("Rank symbol in front of your name", null, () -> r.symbolInName, v -> r.symbolInName = v));
		list.add(new Ui.Toggle("Rank line on screen", i(Items.NETHER_STAR), () -> cfg().rankHud.enabled, v -> cfg().rankHud.enabled = v));
		list.add(new Ui.Header("RANKS  §8· click an unlocked one to wear it"));
		for (dev.vanillaclient.features.Ranks.Rank rk : dev.vanillaclient.features.Ranks.ALL) {
			list.add(new Ui.Custom(20, (g, x, y, w, h, mx, my, a) -> {
				boolean un = dev.vanillaclient.features.Ranks.unlocked(rk);
				boolean cur = dev.vanillaclient.features.Ranks.current() == rk;
				boolean hov = mx >= x && mx < x + w && my >= y && my < y + 18;
				g.fill(x, y, x + w, y + 18, Theme.alpha(cur ? 0x50FFFFFF : hov && un ? 0x30FFFFFF : 0x18FFFFFF, a));
				if (cur) g.fill(x, y, x + 2, y + 18, Theme.alpha(rk.color(), a));
				g.text(this.font, rk.symbol(), x + 7, y + 5, Theme.alpha(un ? rk.color() : 0xFF555555, a), true);
				g.text(this.font, un ? rk.name() : "§8" + rk.name(), x + 20, y + 5, Theme.alpha(un ? rk.color() : 0xFFFFFFFF, a), false);
				String req = un ? (cur ? "§a✔ wearing" : "§7unlocked") : "§8locked · " + rk.hours() + " h";
				g.text(this.font, req, x + w - 6 - this.font.width(req), y + 5, Theme.alpha(0xFFFFFFFF, a), false);
			}, () -> {
				if (dev.vanillaclient.features.Ranks.unlocked(rk)) {
					r.chosen = rk.id();
					Config.save();
				}
			}));
		}
		list.add(new Ui.Info("Hours = play time with Vanilla Client, or the world's", "\"Time Played\" statistic – whichever is higher."));
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

	/** Entry height shrinks on small windows so everything fits. */
	private int entryH() {
		int avail = bottom() - top() - groups() * headerH() - 2;
		return Math.max(9, Math.min(17, avail / Page.values().length));
	}

	private static int groups() {
		java.util.Set<String> g = new java.util.HashSet<>();
		for (Page p : Page.values()) if (p.group != null) g.add(p.group);
		return g.size();
	}

	/** Group headers shrink too when the window is small. */
	private int headerH() {
		int avail = bottom() - top() - 2;
		return avail < Page.values().length * 12 + groups() * HEADER ? 9 : HEADER;
	}

	private List<Entry> sidebar() {
		int ENTRY = entryH();
		List<Entry> out = new ArrayList<>();
		int y = top();
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

	// ------------------------------------------------------------ drawing & input

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
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
				if (e.page() != null && my >= e.y() && my < e.y() + entryH()) {
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
