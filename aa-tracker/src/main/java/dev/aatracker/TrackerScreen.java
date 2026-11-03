package dev.aatracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * All advancements at one glance, in Minecraft's own look: every tab as a grid of
 * advancement frames that turn gold when done, the XP bar as progress bar, and
 * checklists for the multi-part advancements.
 */
public final class TrackerScreen extends Screen {
	private record Entry(AdvancementHolder holder, DisplayInfo display, AdvancementProgress progress, String parentId) {}

	private record Tab(DisplayInfo display, List<Entry> entries) {}

	private record Crit(String name, boolean done, ItemStack icon) {}

	private record Tracker(String id, String title, Item icon, List<Crit> crits) {}

	private static final String[][] TRACKED = {
			{"adventure/adventuring_time", "Adventuring Time", "biome"},
			{"adventure/kill_all_mobs", "Monsters Hunted", "mob"},
			{"husbandry/balanced_diet", "A Balanced Diet", "item"},
			{"husbandry/bred_all_animals", "Two by Two", "mob"},
			{"husbandry/complete_catalogue", "A Complete Catalogue", "none"},
			{"husbandry/whole_pack", "The Whole Pack", "none"},
			{"nether/explore_nether", "Hot Tourist Destinations", "biome"}};

	private static final Identifier XP_BG = Identifier.withDefaultNamespace("hud/experience_bar_background");
	private static final Identifier XP_FILL = Identifier.withDefaultNamespace("hud/experience_bar_progress");
	private static final Identifier TITLE_BOX = Identifier.withDefaultNamespace("advancements/title_box");
	private static final int TILE = 28, TILE_NAMES_W = 54, TILE_NAMES_H = 42;

	public static boolean names;
	/** Set when you ask for Minecraft's own screen, so L doesn't send you back here. */
	public static boolean vanillaWanted;

	private final Screen parent;
	private final List<Tab> tabs = new ArrayList<>();
	private final List<Tracker> trackers = new ArrayList<>();
	private final Map<String, Boolean> doneById = new HashMap<>();
	private final Map<String, Long> flash = new HashMap<>();
	private Set<String> lastDone;
	private String source = "";
	private int scroll, contentH;
	private long loadedAt;

	public TrackerScreen(Screen parent) {
		super(Component.literal("All Advancements"));
		this.parent = parent;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(parent);
	}

	private int top() {
		return 36;
	}

	private int bottom() {
		return this.height - 30;
	}

	private int tileW() {
		return names ? TILE_NAMES_W : TILE;
	}

	private int tileH() {
		return names ? TILE_NAMES_H : TILE;
	}

	@Override
	protected void init() {
		load();
		int y = this.height - 25;
		addRenderableWidget(Button.builder(Component.literal(names ? "Names: ON" : "Names: OFF"), b -> {
			names = !names;
			this.rebuildWidgets();
		}).bounds(this.width / 2 - 154, y, 100, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.advancements"), b -> {
			if (this.minecraft.getConnection() != null) {
				vanillaWanted = true;
				this.minecraft.setScreenAndShow(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(this.minecraft.getConnection().getAdvancements()));
			}
		}).bounds(this.width / 2 - 50, y, 100, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(this.width / 2 + 54, y, 100, 20).build());
	}

	// ------------------------------------------------------------ data

	private void load() {
		tabs.clear();
		trackers.clear();
		doneById.clear();
		loadedAt = System.currentTimeMillis();
		var mc = this.minecraft;
		AdvancementTree tree = null;
		Function<AdvancementHolder, AdvancementProgress> prog = h -> null;
		try {
			var srv = mc.getSingleplayerServer();
			if (srv != null && mc.player != null) {
				var sp = srv.getPlayerList().getPlayer(mc.player.getUUID());
				if (sp != null) {
					tree = srv.getAdvancements().tree();
					var pa = sp.getAdvancements();
					prog = pa::getOrStartProgress;
					source = "§aSingleplayer: every advancement";
				}
			}
		} catch (RuntimeException e) {
			tree = null;
		}
		if (tree == null && mc.getConnection() != null) {
			var ca = mc.getConnection().getAdvancements();
			tree = ca.tree();
			Map<AdvancementHolder, AdvancementProgress> map = ca.progress();
			prog = map::get;
			source = "§7Server: only what the server shows you";
		}
		if (tree == null) {
			source = "§7Join a world first";
			return;
		}
		Map<String, Entry> byPath = new HashMap<>();
		for (AdvancementNode root : tree.roots()) {
			if (root.advancement().display().isEmpty()) continue;
			List<Entry> list = new ArrayList<>();
			collect(root, null, list, prog);
			tabs.add(new Tab(root.advancement().display().get(), list));
			for (Entry e : list) {
				byPath.put(e.holder().id().getPath(), e);
				doneById.put(e.holder().id().toString(), done(e));
			}
		}
		tabs.sort(java.util.Comparator.comparingInt(t -> order(t)));
		// newly finished while the screen is open -> flash
		Set<String> nowDone = new HashSet<>();
		for (var en : doneById.entrySet()) if (en.getValue()) nowDone.add(en.getKey());
		if (lastDone != null) for (String id : nowDone) if (!lastDone.contains(id)) flash.put(id, System.currentTimeMillis());
		lastDone = nowDone;
		for (String[] t : TRACKED) {
			Entry e = byPath.get(t[0]);
			if (e == null) continue;
			List<Crit> crits = new ArrayList<>();
			for (String c : e.holder().value().criteria().keySet()) {
				boolean d = e.progress() != null && e.progress().getCriterion(c) != null && e.progress().getCriterion(c).isDone();
				crits.add(new Crit(pretty(c), d, critIcon(c, t[2])));
			}
			crits.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
			trackers.add(new Tracker(t[0], t[1], icon(e.display()).getItem(), crits));
		}
	}

	/** Story, Nether, End, Adventure, Husbandry – like the speedrun trackers. */
	private static int order(Tab t) {
		if (t.entries().isEmpty()) return 99;
		String p = t.entries().get(0).holder().id().getPath();
		String[] o = {"story", "nether", "end", "adventure", "husbandry"};
		for (int i = 0; i < o.length; i++) if (p.startsWith(o[i] + "/")) return i;
		return 50;
	}

	private static ItemStack critIcon(String crit, String kind) {
		try {
			Identifier id = Identifier.tryParse(crit);
			if (id == null) return ItemStack.EMPTY;
			if (kind.equals("item")) {
				Item it = BuiltInRegistries.ITEM.getValue(id);
				return it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it);
			}
			if (kind.equals("mob")) {
				Item it = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_spawn_egg"));
				return it == Items.AIR ? ItemStack.EMPTY : new ItemStack(it);
			}
		} catch (RuntimeException ignored) {
			// unknown id
		}
		return ItemStack.EMPTY;
	}

	private void collect(AdvancementNode node, String parent, List<Entry> out, Function<AdvancementHolder, AdvancementProgress> prog) {
		var d = node.advancement().display();
		String me = parent;
		if (d.isPresent()) {
			out.add(new Entry(node.holder(), d.get(), prog.apply(node.holder()), parent));
			me = node.holder().id().toString();
		}
		for (AdvancementNode c : node.children()) collect(c, me, out, prog);
	}

	private static boolean done(Entry e) {
		return e.progress() != null && e.progress().isDone();
	}

	/** The one before it is done (or there is none): you can go for it now. */
	private boolean ready(Entry e) {
		return e.parentId() == null || doneById.getOrDefault(e.parentId(), false);
	}

	private static String pretty(String criterion) {
		String s = criterion;
		int c = s.indexOf(':');
		if (c >= 0) s = s.substring(c + 1);
		int slash = s.lastIndexOf('/');
		if (slash >= 0) s = s.substring(slash + 1);
		StringBuilder b = new StringBuilder();
		for (String part : s.split("_")) {
			if (part.isEmpty()) continue;
			if (b.length() > 0) b.append(' ');
			b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return b.toString();
	}

	private static List<String> remaining(Entry e) {
		List<String> out = new ArrayList<>();
		if (e.progress() == null) return out;
		for (String s : e.progress().getRemainingCriteria()) out.add(pretty(s));
		out.sort(String::compareToIgnoreCase);
		return out;
	}

	private static ItemStack icon(DisplayInfo d) {
		try {
			return d.icon().create();
		} catch (RuntimeException e) {
			return ItemStack.EMPTY;
		}
	}

	// ------------------------------------------------------------ layout

	private int rows() {
		int w = this.width - 16;
		for (int r = 1; r < 200; r++) {
			int sum = 0;
			for (Tab t : tabs) sum += ((t.entries().size() + r - 1) / r) * tileW() + 8;
			sum += (tabs.size() - 1) * 6;
			if (sum <= w) return r;
		}
		return 200;
	}

	private static final int TRACK_LINES = 12, COL_W = 92;

	private int trackerW(Tracker t) {
		int cols = Math.max(1, (t.crits().size() + TRACK_LINES - 1) / TRACK_LINES);
		return Math.max(118, cols * COL_W + 8);
	}

	private int trackerH(Tracker t) {
		return 24 + Math.min(TRACK_LINES, t.crits().size()) * 9 + 4;
	}

	// ------------------------------------------------------------ drawing

	private void small(GuiGraphicsExtractor g, String text, float x, float y, int color, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(this.font, text, 0, 0, color, false);
		g.pose().popMatrix();
	}

	/** Text with a black outline, like the level number above the XP bar. */
	private void outlined(GuiGraphicsExtractor g, String s, int cx, int y, int color) {
		int x = cx - this.font.width(s) / 2;
		g.text(this.font, s, x + 1, y, 0xFF000000, false);
		g.text(this.font, s, x - 1, y, 0xFF000000, false);
		g.text(this.font, s, x, y + 1, 0xFF000000, false);
		g.text(this.font, s, x, y - 1, 0xFF000000, false);
		g.text(this.font, s, x, y, color, false);
	}

	/** Panel with a vanilla title box on top. */
	private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, ItemStack icon, String title, String count, boolean complete) {
		g.fill(x, y + 10, x + w, y + h, 0xB0101010);
		g.outline(x, y + 10, w, h - 10, complete ? 0xFF7FB238 : 0xFF000000);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX, x, y, w, 20);
		if (!icon.isEmpty()) Icons.scaled(g, icon, x + 3, y + 3, 0.875f);
		int room = w - 22 - this.font.width(count) - 4;
		String t = this.font.width(title) <= room ? title : this.font.plainSubstrByWidth(title, room - this.font.width("…")) + "…";
		g.text(this.font, t, x + 20, y + 6, 0xFFFFFFFF, true);
		g.text(this.font, count, x + w - 4 - this.font.width(count), y + 6, complete ? 0xFF80FF20 : 0xFFFFFFFF, true);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		if (System.currentTimeMillis() - loadedAt > 1000) load();
		int doneAll = 0, total = 0;
		for (Tab t : tabs) for (Entry e : t.entries()) {
			total++;
			if (done(e)) doneAll++;
		}
		// header: title, level-style number, XP bar
		g.centeredText(this.font, this.title, this.width / 2, 5, 0xFFFFFFFF);
		outlined(g, doneAll + " / " + total, this.width / 2, 16, 0xFF80FF20);
		int bx = this.width / 2 - 91;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, XP_BG, bx, 27, 182, 5);
		int fill = total == 0 ? 0 : Math.round(182f * doneAll / total);
		if (fill > 0) g.blitSprite(RenderPipelines.GUI_TEXTURED, XP_FILL, 182, 5, 0, 0, bx, 27, fill, 5);
		String igt = playTime();
		if (igt != null) g.text(this.font, igt, this.width - 8 - this.font.width(igt), 5, 0xFFAAAAAA, true);
		if (tabs.isEmpty()) {
			g.centeredText(this.font, source, this.width / 2, this.height / 2, 0xFFAAAAAA);
			return;
		}

		Entry hovered = null;
		int top = top(), bottom = bottom();
		boolean inside = mouseY >= top && mouseY < bottom;
		g.enableScissor(0, top, this.width, bottom);
		int y = top + 2 - scroll;

		// ---- one panel per tab
		int rows = rows();
		int totalW = 0;
		for (Tab t : tabs) totalW += ((t.entries().size() + rows - 1) / rows) * tileW() + 8 + 6;
		int x = Math.max(8, (this.width - totalW + 6) / 2);
		for (Tab t : tabs) {
			int n = t.entries().size();
			int cols = (n + rows - 1) / rows;
			int pw = cols * tileW() + 8, ph = rows * tileH() + 26;
			int d = 0;
			for (Entry e : t.entries()) if (done(e)) d++;
			panel(g, x, y, pw, ph, icon(t.display()), t.display().title().getString(), d + "/" + n, d == n);
			for (int i = 0; i < n; i++) {
				Entry e = t.entries().get(i);
				int tx = x + 4 + (i / rows) * tileW(), ty = y + 22 + (i % rows) * tileH();
				boolean hov = inside && mouseX >= tx && mouseX < tx + tileW() && mouseY >= ty && mouseY < ty + tileH();
				if (hov) hovered = e;
				drawTile(g, e, tx, ty, hov);
			}
			x += pw + 6;
		}
		y += rows * tileH() + 26 + 10;

		// ---- checklists
		int tx = 8, rowH = 0;
		int lineW = this.width - 16;
		for (Tracker t : trackers) {
			int w = trackerW(t), h = trackerH(t);
			if (tx + w > 8 + lineW && tx > 8) {
				tx = 8;
				y += rowH + 6;
				rowH = 0;
			}
			drawTracker(g, t, tx, y, w, h);
			tx += w + 6;
			rowH = Math.max(rowH, h);
		}
		y += rowH + 6;
		contentH = y + scroll - top;
		g.disableScissor();
		int view = bottom - top;
		if (contentH > view) {
			int sh = Math.max(16, view * view / contentH);
			int sy = top + Math.round((view - sh) * (scroll / (float) (contentH - view)));
			g.fill(this.width - 5, top, this.width - 3, bottom, 0x40000000);
			g.fill(this.width - 5, sy, this.width - 3, sy + sh, 0xFFC0C0C0);
		}
		if (hovered != null) g.setComponentTooltipForNextFrame(this.font, tooltip(hovered), mouseX, mouseY);
	}

	private void drawTile(GuiGraphicsExtractor g, Entry e, int x, int y, boolean hov) {
		boolean d = done(e), r = ready(e);
		AdvancementType type = e.display().type();
		String kind = type == AdvancementType.CHALLENGE ? "challenge" : type == AdvancementType.GOAL ? "goal" : "task";
		int fx = x + (tileW() - 26) / 2, fy = y + 1;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("advancements/" + kind + "_frame_" + (d ? "obtained" : "unobtained")), fx, fy, 26, 26);
		g.item(icon(e.display()), fx + 5, fy + 5);
		if (!d && !r) g.fill(fx + 2, fy + 2, fx + 24, fy + 24, 0x80101010);
		if (hov) g.fill(fx + 2, fy + 2, fx + 24, fy + 24, 0x40FFFFFF);
		int n = e.holder().value().criteria().size();
		if (!d && n > 1 && e.progress() != null) {
			int left = 0;
			for (String ignored : e.progress().getRemainingCriteria()) left++;
			float f = (n - left) / (float) n;
			g.fill(fx + 4, fy + 22, fx + 22, fy + 24, 0xFF000000);
			g.fill(fx + 4, fy + 22, fx + 4 + Math.round(18 * f), fy + 24, 0xFF80FF20);
		}
		Long fl = flash.get(e.holder().id().toString());
		if (fl != null) {
			float t = (System.currentTimeMillis() - fl) / 1500f;
			if (t < 1f) g.fill(fx, fy, fx + 26, fy + 26, ((int) ((1f - t) * 160) << 24) | 0xFFFFFF);
		}
		if (names) {
			List<String> lines = wrap(e.display().title().getString(), (int) ((tileW() - 2) / 0.6f), 2);
			for (int i = 0; i < lines.size(); i++) {
				String l = lines.get(i);
				float w = this.font.width(l) * 0.6f;
				small(g, l, x + (tileW() - w) / 2f, fy + 27 + i * 6, d ? 0xFFFFFF55 : r ? 0xFFFFFFFF : 0xFF808080, 0.6f);
			}
		}
	}

	private List<Component> tooltip(Entry e) {
		List<Component> out = new ArrayList<>();
		AdvancementType type = e.display().type();
		int col = type == AdvancementType.CHALLENGE ? 0xFFAA00AA : 0xFF55FF55;
		out.add(e.display().title().copy().withColor(col & 0xFFFFFF));
		for (String l : wrap(e.display().description().getString(), 200, 4)) out.add(Component.literal(l).withColor(0xAAAAAA));
		boolean d = done(e);
		out.add(Component.literal(d ? "✔ Done" : ready(e) ? "Ready to get" : "Get the one before first").withColor(d ? 0x55FF55 : ready(e) ? 0xFFFFFF : 0x777777));
		String hint = HINTS.get(e.holder().id().getPath());
		if (hint != null && !d) for (String l : wrap("➜ " + hint, 200, 3)) out.add(Component.literal(l).withColor(0xFFFF55));
		int n = e.holder().value().criteria().size();
		if (n > 1 && !d) {
			List<String> rem = remaining(e);
			out.add(Component.literal((n - rem.size()) + "/" + n + " · missing:").withColor(0xFFFF55));
			int show = Math.min(rem.size(), 10);
			for (int i = 0; i < show; i++) out.add(Component.literal("• " + rem.get(i)).withColor(0xFFFFFF));
			if (rem.size() > show) out.add(Component.literal("+ " + (rem.size() - show) + " more below").withColor(0x777777));
		}
		return out;
	}

	private List<String> wrap(String text, int width, int maxLines) {
		List<String> out = new ArrayList<>();
		String rest = text;
		while (!rest.isEmpty() && out.size() < maxLines) {
			if (this.font.width(rest) <= width) {
				out.add(rest);
				break;
			}
			int cut = this.font.plainSubstrByWidth(rest, width).length();
			int sp = rest.lastIndexOf(' ', cut);
			if (out.size() == maxLines - 1) {
				out.add(this.font.plainSubstrByWidth(rest, width - this.font.width("…")) + "…");
				break;
			}
			if (sp > 0) cut = sp;
			out.add(rest.substring(0, cut).trim());
			rest = rest.substring(cut).trim();
		}
		return out;
	}

	private void drawTracker(GuiGraphicsExtractor g, Tracker t, int x, int y, int w, int h) {
		int d = 0;
		for (Crit c : t.crits()) if (c.done()) d++;
		int n = t.crits().size();
		panel(g, x, y, w, h, Icons.stack(t.icon()), t.title(), d + "/" + n, d == n);
		for (int i = 0; i < n; i++) {
			Crit c = t.crits().get(i);
			int cx = x + 4 + (i / TRACK_LINES) * COL_W, cy = y + 23 + (i % TRACK_LINES) * 9;
			int tx = cx;
			if (!c.icon().isEmpty()) {
				Icons.scaled(g, c.icon(), cx, cy, 0.5f);
				tx += 10;
			}
			String label = this.font.plainSubstrByWidth(c.name(), COL_W - (tx - cx) - 4);
			g.text(this.font, label, tx, cy, c.done() ? 0xFF55FF55 : 0xFFFFFFFF, false);
			if (c.done()) g.fill(tx, cy + 4, tx + this.font.width(label), cy + 5, 0xFF55FF55);
		}
	}

	private String playTime() {
		var p = this.minecraft.player;
		if (p == null) return null;
		int ticks = p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
		if (ticks <= 0) return null;
		long s = ticks / 20L;
		return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		int max = Math.max(0, contentH - (bottom() - top()));
		scroll = (int) Math.max(0, Math.min(max, scroll - v * 24));
		return true;
	}

	// ------------------------------------------------------------ hints

	private static String hint(Entry e) {
		return HINTS.get(e.holder().id().getPath());
	}

	private static final Map<String, String> HINTS = new HashMap<>();

	static {
		String[][] h = {
				{"story/follow_ender_eye", "Throw Eyes of Ender, follow them, dig down into the stronghold"},
				{"story/enter_the_end", "Fill the portal frame in the stronghold with 12 Eyes of Ender"},
				{"story/cure_zombie_villager", "Weakness splash potion + golden apple on a zombie villager"},
				{"nether/return_to_sender", "Hit a ghast fireball back so it kills the ghast"},
				{"nether/fast_travel", "Travel 875 blocks in the Nether (= 7 km in the Overworld)"},
				{"nether/uneasy_alliance", "Lure a ghast through a portal and kill it in the Overworld"},
				{"nether/create_full_beacon", "Beacon on a 4-layer pyramid (164 iron/gold/diamond/emerald blocks)"},
				{"nether/all_potions", "Have every potion effect at once – drink them right after each other"},
				{"nether/all_effects", "Every effect at once (beacon, conduit, suspicious stew, bad omen …)"},
				{"nether/ride_strider_in_overworld_lava", "Bring a strider + warped fungus on a stick, ride 50 blocks on Overworld lava"},
				{"nether/explore_nether", "Visit all 5 Nether biomes"},
				{"nether/distract_piglin", "Give a piglin gold while it's angry"},
				{"nether/charge_respawn_anchor", "Fill a respawn anchor completely with glowstone"},
				{"nether/use_lodestone", "Use a compass on a lodestone"},
				{"nether/get_wither_skull", "Kill wither skeletons in a fortress (Looting helps)"},
				{"nether/netherite_armor", "4 netherite ingots + smithing templates on full diamond armor"},
				{"end/dragon_egg", "Push the egg off with a piston or break the block under it and put a torch"},
				{"end/enter_end_gateway", "Throw an ender pearl into the small gateway after the dragon"},
				{"end/respawn_dragon", "Place 4 end crystals on the exit portal"},
				{"end/dragon_breath", "Use an empty bottle in the dragon's purple breath"},
				{"end/levitate", "Get hit by a shulker bullet and float up 50 blocks"},
				{"end/find_end_city", "Fly / bridge through an outer-island gateway to an End city"},
				{"adventure/adventuring_time", "Visit every Overworld biome – click to see which are missing"},
				{"adventure/kill_all_mobs", "Kill one of every hostile mob – click to see which are missing"},
				{"adventure/sniper_duel", "Kill a skeleton from at least 50 blocks away"},
				{"adventure/very_very_frightening", "Channeling trident on a villager during a thunderstorm"},
				{"adventure/two_birds_one_arrow", "Kill two phantoms with one Piercing crossbow arrow"},
				{"adventure/arbalistic", "Kill 5 different mobs with one Piercing IV crossbow shot"},
				{"adventure/hero_of_the_village", "Get Bad Omen (ominous bottle), enter a village, win the raid"},
				{"adventure/bullseye", "Hit the center of a target block from 30+ blocks"},
				{"adventure/trade_at_world_height", "Trade with a villager at the build height limit"},
				{"adventure/fall_from_world_height", "Fall from the build limit down to the bottom of the world (water at the end!)"},
				{"adventure/play_jukebox_in_meadows", "Play a music disc in a jukebox in a Meadow"},
				{"adventure/walk_on_powder_snow_with_leather_boots", "Walk on powder snow wearing leather boots"},
				{"adventure/lightning_rod_with_villager_no_fire", "Lightning rod protects a villager from lightning without fire"},
				{"adventure/spyglass_at_parrot", "Look at a parrot through a spyglass"},
				{"adventure/spyglass_at_ghast", "Look at a ghast through a spyglass"},
				{"adventure/spyglass_at_dragon", "Look at the Ender Dragon through a spyglass"},
				{"adventure/avoid_vibration", "Sneak next to a sculk sensor or warden without triggering it"},
				{"adventure/kill_mob_near_sculk_catalyst", "Kill a mob next to a sculk catalyst"},
				{"adventure/salvage_sherd", "Brush suspicious sand/gravel to get a pottery sherd"},
				{"adventure/craft_decorated_pot_using_only_sherds", "Craft a decorated pot from 4 sherds"},
				{"adventure/trim_with_all_exclusive_armor_patterns", "Use Snout, Rib, Ward, Silence, Vex, Tide, Spire and Wayfinder trims"},
				{"adventure/read_power_of_chiseled_bookshelf", "Read a chiseled bookshelf's signal with a comparator"},
				{"adventure/under_lock_and_key", "Open a trial chamber vault with a trial key"},
				{"adventure/revaulting", "Open an ominous vault with an ominous trial key"},
				{"adventure/blowback", "Kill a breeze with its own deflected wind charge"},
				{"adventure/who_needs_rockets", "Launch yourself 8 blocks up with a wind charge"},
				{"adventure/overoverkill", "Deal 50 hearts in one mace smash – fall from very high"},
				{"adventure/lighten_up", "Scrape a copper bulb with an axe to make it brighter"},
				{"adventure/crafters_crafting_crafters", "Let a crafter craft a crafter"},
				{"adventure/summon_iron_golem", "4 iron blocks in a T + carved pumpkin on top"},
				{"husbandry/balanced_diet", "Eat every food once – click to see which are missing"},
				{"husbandry/bred_all_animals", "Breed every animal – click to see which are missing"},
				{"husbandry/complete_catalogue", "Tame every cat variant (villages + swamp huts)"},
				{"husbandry/whole_pack", "Tame every wolf variant (different biomes)"},
				{"husbandry/froglights", "Get all 3 froglights: frogs from each climate eat small magma cubes"},
				{"husbandry/leash_all_frog_variants", "Leash a temperate, warm and cold frog at the same time"},
				{"husbandry/silk_touch_nest", "Move a bee nest with 3 bees inside using Silk Touch"},
				{"husbandry/safely_harvest_honey", "Campfire under the hive, then use a bottle"},
				{"husbandry/obtain_netherite_hoe", "Upgrade a diamond hoe with netherite"},
				{"husbandry/allay_deliver_cake_to_note_block", "Give an allay a cake, let it drop it at a note block"},
				{"husbandry/kill_axolotl_target", "Team up with an axolotl and win a fight"},
				{"husbandry/tadpole_in_a_bucket", "Catch a tadpole with a water bucket"},
				{"husbandry/feed_snifflet", "Feed a baby sniffer (torchflower seeds)"},
				{"husbandry/plant_any_sniffer_seed", "Plant a torchflower seed or pitcher pod"},
				{"husbandry/wax_off", "Scrape wax off a copper block with an axe"},
				{"husbandry/wax_on", "Use honeycomb on a copper block"},
				{"husbandry/remove_wolf_armor", "Remove wolf armor with shears"},
				{"husbandry/repair_wolf_armor", "Repair damaged wolf armor with armadillo scutes"},
				{"husbandry/fishy_business", "Catch a fish with a fishing rod"},
				{"husbandry/tactical_fishing", "Catch a fish with a water bucket"},
		};
		for (String[] e : h) HINTS.put(e[0], e[1]);
	}

}
