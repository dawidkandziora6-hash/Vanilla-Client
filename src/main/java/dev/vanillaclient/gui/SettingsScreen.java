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
		CROSSHAIR("VISUALS", "Crosshair", Items.TARGET),
		ZOOM("VISUALS", "Zoom", Items.SPYGLASS),
		HELD("VISUALS", "Held Items", Items.TOTEM_OF_UNDYING),
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
			case CROSSHAIR -> crosshair();
			case ZOOM -> zoom();
			case HELD -> held();
			case GENERAL -> general();
		}
	}

	private void home() {
		Config c = cfg();
		list.add(new Ui.Header("MODULES"));
		list.add(new Ui.ModuleCard("Days", "Day & clock", i(Items.CLOCK), () -> c.days.enabled, v -> c.days.enabled = v, () -> go(Page.DAYS)));
		list.add(new Ui.ModuleCard("Coordinates", "XYZ & facing", i(Items.COMPASS), () -> c.coords.enabled, v -> c.coords.enabled = v, () -> go(Page.COORDS)));
		list.add(new Ui.ModuleCard("Armor", "Next to hotbar", i(Items.DIAMOND_CHESTPLATE), () -> c.armor.enabled, v -> c.armor.enabled = v, () -> go(Page.ARMOR)));
		list.add(new Ui.ModuleCard("Task List", "On screen", i(Items.WRITABLE_BOOK), () -> c.todo.enabled, v -> c.todo.enabled = v, () -> go(Page.TASKLIST)));
		list.add(new Ui.ModuleCard("Crosshair", "Your own pixels", i(Items.TARGET), () -> c.crosshair.enabled, v -> c.crosshair.enabled = v, () -> go(Page.CROSSHAIR)));
		list.add(new Ui.ModuleCard("Zoom", "Hold C", i(Items.SPYGLASS), () -> c.zoom.enabled, v -> c.zoom.enabled = v, () -> go(Page.ZOOM)));
		list.add(new Ui.ModuleCard("Held Items", "Smaller shield/totem", i(Items.TOTEM_OF_UNDYING), () -> c.heldItems.enabled, v -> c.heldItems.enabled = v, () -> go(Page.HELD)));
		list.add(new Ui.ModuleCard("World Map", "Records explored land", i(Items.FILLED_MAP), () -> c.worldMap.enabled, v -> c.worldMap.enabled = v, () -> go(Page.GENERAL)));
		list.add(new Ui.Header("TOOLS"));
		list.add(new Ui.Tile("Task Planner", "Farms, builds, trips", i(Items.WRITABLE_BOOK), () -> open(new TasksScreen(this))));
		list.add(new Ui.Tile("World Map", "Where you've been", i(Items.FILLED_MAP), () -> open(new WorldMapScreen(this))));
		list.add(new Ui.Tile("UHC Guide", "Villagers, enchants", i(Items.ENCHANTED_BOOK), () -> open(new GuideScreen(this))));
		list.add(new Ui.Tile("Wardrobe", "Capes, wings, totems", i(Items.ELYTRA), () -> open(new CosmeticsScreen(this))));
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
		list.add(new Ui.Toggle("Show clock time", null, () -> s.showTime, v -> s.showTime = v));
		list.add(new Ui.Toggle("Start counting at day 1", null, () -> s.countFromOne, v -> s.countFromOne = v));
		list.add(new Ui.Toggle("Sun / moon icon", null, () -> s.showIcon, v -> s.showIcon = v));
		list.add(new Ui.Cycle<>("Label", List.of("Day", "Survival Day", "Hardcore Day", "UHC Day", "Day #"), () -> s.label, v -> s.label = v, v -> v));
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
		list.add(new Ui.Toggle("Gray labels", null, () -> s.coloredLabels, v -> s.coloredLabels = v));
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
		list.add(new Ui.Toggle("Outline", null, () -> s.outline, v -> s.outline = v));
		list.add(new Ui.ColorPick("Outline color", () -> s.outlineColor, v -> s.outlineColor = v, this::colorPicker));
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
		list.add(new Ui.Info("Key: C (change it in Options › Controls)."));
	}

	private void held() {
		Config.HeldItems h = cfg().heldItems;
		list.add(new Ui.Header("HELD ITEMS"));
		list.add(new Ui.Toggle("Change sizes", i(Items.TOTEM_OF_UNDYING), () -> h.enabled, v -> h.enabled = v));
		list.add(new Ui.Slider("Shield", 0.3, 1.5, 0.05, () -> h.shieldScale, v -> h.shieldScale = (float) v, v -> Math.round(v * 100) + "%"));
		list.add(new Ui.Slider("Totem in hand", 0.3, 1.5, 0.05, () -> h.totemScale, v -> h.totemScale = (float) v, v -> Math.round(v * 100) + "%"));
		list.add(new Ui.Slider("Totem pop", 0.2, 1.5, 0.05, () -> h.totemPopScale, v -> h.totemPopScale = (float) v, v -> Math.round(v * 100) + "%"));
		list.add(new Ui.Custom(56, (g, x, y, w, hh, mx, my, a) -> {
			float bob = (float) Math.sin(Theme.time() * 2.0) * 2f;
			float s1 = 2f * (h.enabled ? h.shieldScale : 1f);
			float s2 = 2f * (h.enabled ? h.totemScale : 1f);
			Icons.scaled(g, i(Items.SHIELD), x + w / 4f - 8 * s1, y + 26 - 8 * s1 + bob, s1);
			Icons.scaled(g, i(Items.TOTEM_OF_UNDYING), x + w * 3 / 4f - 8 * s2, y + 26 - 8 * s2 - bob, s2);
		}));
		list.add(new Ui.Info("Only changes how they look in your hands."));
	}

	private void general() {
		Config c = cfg();
		list.add(new Ui.Header("MENU"));
		list.add(new Ui.Toggle("Button in the pause menu", i(Items.WRITABLE_BOOK), () -> c.pauseMenuButton, v -> c.pauseMenuButton = v));
		list.add(new Ui.Header("WORLD MAP"));
		list.add(new Ui.Toggle("Record explored land", i(Items.FILLED_MAP), () -> c.worldMap.enabled, v -> c.worldMap.enabled = v));
		list.add(new Ui.Slider("Map radius", 2, 16, 1, () -> c.worldMap.radius, v -> c.worldMap.radius = (int) v, v -> (int) v + " chunks"));
		list.add(new Ui.Toggle("Show tasks on the map", null, () -> c.worldMap.showTasks, v -> c.worldMap.showTasks = v));
		list.add(new Ui.Button("Open the world map", i(Items.FILLED_MAP), () -> open(new WorldMapScreen(this))));
		list.add(new Ui.Header("FILES"));
		list.add(new Ui.Button("Open settings folder", i(Items.CHEST), () -> Platform.open(Config.dir())));
		list.add(new Ui.Header("KEYS"));
		list.add(new Ui.Info("Right Shift  ·  open this menu", "M  ·  world map", "C  ·  zoom (hold)", "Change keys in Options › Controls"));
	}

	private void say(String s) {
		status = s;
		statusAt = System.currentTimeMillis();
	}

	// ------------------------------------------------------------ sidebar

	private record Entry(Page page, String header, int y) {}

	/** Entry height shrinks on small windows so everything fits. */
	private int entryH() {
		int avail = bottom() - top() - 3 * HEADER;
		return Math.max(12, Math.min(17, avail / Page.values().length));
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
				y += HEADER;
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
