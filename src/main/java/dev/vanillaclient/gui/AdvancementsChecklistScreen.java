package dev.vanillaclient.gui;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * All advancements at one glance (like an "all advancements" speedrun tracker):
 * every category as a grid of icons that tick off by themselves, plus checklists
 * for the multi-part ones (biomes, mobs, foods, animals, cats, wolves, nether biomes).
 */
public final class AdvancementsChecklistScreen extends BaseScreen {
	private record Entry(AdvancementHolder holder, DisplayInfo display, AdvancementProgress progress, String parentId) {}

	private record Tab(DisplayInfo display, List<Entry> entries) {}

	private record Crit(String name, boolean done, ItemStack icon) {}

	private record Tracker(String id, String title, Item icon, List<Crit> crits) {}

	/** Multi-part advancements that get their own checklist panel. */
	private static final String[][] TRACKED = {
			{"adventure/adventuring_time", "Adventuring Time", "biome"},
			{"adventure/kill_all_mobs", "Monsters Hunted", "mob"},
			{"husbandry/balanced_diet", "A Balanced Diet", "item"},
			{"husbandry/bred_all_animals", "Two by Two", "mob"},
			{"husbandry/complete_catalogue", "A Complete Catalogue", "none"},
			{"husbandry/whole_pack", "The Whole Pack", "none"},
			{"nether/explore_nether", "Hot Tourist Destinations", "biome"}};

	public static boolean names;
	/** Set when you ask for Minecraft's own screen, so L doesn't send you back here. */
	public static boolean vanillaWanted;
	private final List<Tab> tabs = new ArrayList<>();
	private final List<Tracker> trackers = new ArrayList<>();
	private final Map<String, Boolean> doneById = new HashMap<>();
	private final Map<String, Long> flash = new HashMap<>();
	private Set<String> lastDone;
	private String source = "";
	private int scroll, contentH;
	private long loadedAt;
	private Entry hovered;
	private Tracker hoveredTracker;

	public AdvancementsChecklistScreen(Screen parent) {
		super(Component.literal("Advancements"), parent);
	}

	private int top() {
		return 34;
	}

	private int bottom() {
		return this.height - 30;
	}

	private int contentW() {
		return this.width - 16;
	}

	private int tileW() {
		return names ? 46 : 24;
	}

	private int tileH() {
		return names ? 40 : 24;
	}

	@Override
	protected void init() {
		super.init();
		load();
		int y = this.height - 25;
		action(this.width / 2 - 154, y, 100, names ? "Names: §aOn" : "Names: §7Off", () -> {
			names = !names;
			rebuildWidgets();
		});
		action(this.width / 2 - 50, y, 100, "Vanilla screen", () -> {
			if (this.minecraft.getConnection() != null) {
				vanillaWanted = true;
				this.minecraft.setScreenAndShow(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(this.minecraft.getConnection().getAdvancements()));
			}
		});
		action(this.width / 2 + 54, y, 100, "✔ Done", this::onClose);
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

	/** Rows so that all category panels fit next to each other. */
	private int rows() {
		int w = contentW();
		for (int r = 1; r < 200; r++) {
			int sum = 0;
			for (Tab t : tabs) sum += ((t.entries().size() + r - 1) / r) * tileW() + 8;
			sum += (tabs.size() - 1) * 4;
			if (sum <= w) return r;
		}
		return 200;
	}

	private static final int TRACK_LINES = 12, COL_W = 92;

	private int trackerW(Tracker t) {
		int cols = Math.max(1, (t.crits().size() + TRACK_LINES - 1) / TRACK_LINES);
		return Math.max(110, cols * COL_W + 8);
	}

	private int trackerH(Tracker t) {
		return 18 + Math.min(TRACK_LINES, t.crits().size()) * 9 + 4;
	}

	// ------------------------------------------------------------ drawing

	private void small(GuiGraphicsExtractor g, String text, float x, float y, int color, float scale) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.text(this.font, text, 0, 0, color, false);
		g.pose().popMatrix();
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		if (System.currentTimeMillis() - loadedAt > 1000) load();
		float in = intro();
		int doneAll = 0, total = 0;
		for (Tab t : tabs) for (Entry e : t.entries()) {
			total++;
			if (done(e)) doneAll++;
		}
		// header: title · progress bar · time
		int x0 = 8, x1 = this.width - 8;
		g.text(this.font, "§l✦ ALL ADVANCEMENTS", x0, 7, Theme.alpha(Theme.ACCENT, in), true);
		int pct = total == 0 ? 0 : Math.round(doneAll * 100f / total);
		String ps = doneAll + " / " + total + " (" + pct + "%)";
		int bw = Math.min(220, this.width / 3), bx = this.width / 2 - bw / 2;
		g.fill(bx, 6, bx + bw, 17, 0x80000000);
		if (total > 0) g.fill(bx, 6, bx + Math.round(bw * doneAll / (float) total), 17, 0xFF3FA34D);
		g.outline(bx, 6, bw, 11, 0x40FFFFFF);
		g.centeredText(this.font, ps, this.width / 2, 8, 0xFFFFFFFF);
		String igt = playTime();
		if (igt != null) g.text(this.font, "§7IGT §f" + igt, x1 - this.font.width("IGT " + igt), 7, 0xFFFFFFFF, true);
		small(g, source + "   §a■ §7done   §f■ §7ready now   §8■ §7do the one before first   §8hover = details", x0, 21, 0xFFFFFFFF, 0.75f);

		hovered = null;
		hoveredTracker = null;
		int top = top(), bottom = bottom();
		g.enableScissor(0, top, this.width, bottom);
		int y = top + 2 - scroll;
		boolean inside = mouseY >= top && mouseY < bottom;

		// ---- category panels
		int rows = rows();
		int x = x0;
		for (Tab t : tabs) {
			int n = t.entries().size();
			int cols = (n + rows - 1) / rows;
			int pw = cols * tileW() + 8, ph = rows * tileH() + 20;
			int d = 0;
			for (Entry e : t.entries()) if (done(e)) d++;
			g.fill(x, y, x + pw, y + ph, 0x90101014);
			g.outline(x, y, pw, ph, d == n ? 0xFF3FA34D : 0x30FFFFFF);
			Icons.scaled(g, icon(t.display()), x + 3, y + 2, 0.75f);
			String name = t.display().title().getString();
			String cnt = (d == n ? "§a" : "§7") + d + "/" + n;
			int room = pw - 22 - this.font.width(cnt);
			if (this.font.width(name) <= room) g.text(this.font, "§f" + name, x + 17, y + 4, 0xFFFFFFFF, false);
			else small(g, "§f" + this.font.plainSubstrByWidth(name, (int) (room / 0.7f)), x + 17, y + 5, 0xFFFFFFFF, 0.7f);
			g.text(this.font, cnt, x + pw - 4 - this.font.width(cnt), y + 4, 0xFFFFFFFF, false);
			for (int i = 0; i < n; i++) {
				Entry e = t.entries().get(i);
				int tx = x + 4 + (i / rows) * tileW(), ty = y + 16 + (i % rows) * tileH();
				boolean hov = inside && mouseX >= tx && mouseX < tx + tileW() && mouseY >= ty && mouseY < ty + tileH();
				if (hov) hovered = e;
				drawTile(g, e, tx, ty, hov);
			}
			x += pw + 4;
		}
		y += rows * tileH() + 20 + 8;

		// ---- multi-part checklists
		if (!trackers.isEmpty()) {
			g.text(this.font, "§lCHECKLISTS §8· multi-part advancements", x0, y, Theme.ACCENT, false);
			y += 12;
			int tx = x0, rowH = 0;
			for (Tracker t : trackers) {
				int w = trackerW(t), h = trackerH(t);
				if (tx + w > x1 && tx > x0) {
					tx = x0;
					y += rowH + 4;
					rowH = 0;
				}
				boolean hov = inside && mouseX >= tx && mouseX < tx + w && mouseY >= y && mouseY < y + h;
				if (hov) hoveredTracker = t;
				drawTracker(g, t, tx, y, w, h);
				tx += w + 4;
				rowH = Math.max(rowH, h);
			}
			y += rowH + 6;
		}
		contentH = y + scroll - top;
		g.disableScissor();
		if (tabs.isEmpty()) g.centeredText(this.font, source, this.width / 2, this.height / 2, 0xFFFFFFFF);
		// scrollbar
		int view = bottom - top;
		if (contentH > view) {
			int sh = Math.max(16, view * view / contentH);
			int sy = top + Math.round((view - sh) * (scroll / (float) (contentH - view)));
			g.fill(this.width - 4, top, this.width - 2, bottom, 0x30FFFFFF);
			g.fill(this.width - 4, sy, this.width - 2, sy + sh, 0xA0FFFFFF);
		}
		if (hovered != null) tooltip(g, hovered, mouseX, mouseY);
	}

	private void drawTile(GuiGraphicsExtractor g, Entry e, int x, int y, boolean hov) {
		boolean d = done(e);
		boolean r = ready(e);
		AdvancementType type = e.display().type();
		int typeCol = type == AdvancementType.CHALLENGE ? 0xFFB46CFF : type == AdvancementType.GOAL ? 0xFF55D8FF : 0xFFB0B0B0;
		int fx = x + (tileW() - 22) / 2, fy = y + 1;
		int bg = d ? 0xFF2E5A2E : hov ? 0xFF4A4A52 : r ? 0xFF2C2C32 : 0xFF1A1A1E;
		g.fill(fx, fy, fx + 22, fy + 22, bg);
		g.outline(fx, fy, 22, 22, d ? 0xFF5AD06A : r ? typeCol : Theme.lerpColor(typeCol, 0xFF202024, 0.6f));
		g.item(icon(e.display()), fx + 3, fy + 3);
		if (!d && !r) g.fill(fx + 1, fy + 1, fx + 21, fy + 21, 0x60101014);
		if (d) g.text(this.font, "§a✔", fx + 15, fy + 13, 0xFFFFFFFF, true);
		// partial progress
		int n = e.holder().value().criteria().size();
		if (!d && n > 1 && e.progress() != null) {
			int left = e.progress().getRemainingCriteria() == null ? n : countRemaining(e);
			float f = (n - left) / (float) n;
			g.fill(fx + 2, fy + 19, fx + 20, fy + 21, 0xA0000000);
			g.fill(fx + 2, fy + 19, fx + 2 + Math.round(18 * f), fy + 21, 0xFFFFD34D);
		}
		Long fl = flash.get(e.holder().id().toString());
		if (fl != null) {
			float t = (System.currentTimeMillis() - fl) / 1500f;
			if (t < 1f) g.fill(fx - 2, fy - 2, fx + 24, fy + 24, Theme.alpha(0xFF8CFF8C, (1f - t) * 0.7f));
		}
		if (names) {
			String title = e.display().title().getString();
			List<String> lines = wrap(title, (int) ((tileW() - 2) / 0.6f), 2);
			for (int i = 0; i < lines.size(); i++) {
				String l = lines.get(i);
				float w = this.font.width(l) * 0.6f;
				small(g, (d ? "§a" : r ? "§f" : "§8") + l, x + (tileW() - w) / 2f, fy + 24 + i * 6, 0xFFFFFFFF, 0.6f);
			}
		}
	}

	private static int countRemaining(Entry e) {
		int n = 0;
		for (String ignored : e.progress().getRemainingCriteria()) n++;
		return n;
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
			if (sp > 0 && out.size() < maxLines - 1) cut = sp;
			if (out.size() == maxLines - 1 && cut < rest.length()) {
				out.add(this.font.plainSubstrByWidth(rest, width - this.font.width("…")) + "…");
				break;
			}
			out.add(rest.substring(0, cut).trim());
			rest = rest.substring(cut).trim();
		}
		return out;
	}

	private void drawTracker(GuiGraphicsExtractor g, Tracker t, int x, int y, int w, int h) {
		int d = 0;
		for (Crit c : t.crits()) if (c.done()) d++;
		int n = t.crits().size();
		boolean all = d == n;
		g.fill(x, y, x + w, y + h, 0x90101014);
		g.outline(x, y, w, h, all ? 0xFF3FA34D : 0x30FFFFFF);
		Icons.scaled(g, Icons.stack(t.icon()), x + 3, y + 2, 0.75f);
		String cnt = (all ? "§a" : "§e") + d + "/" + n;
		g.text(this.font, "§f" + this.font.plainSubstrByWidth(t.title(), w - 24 - this.font.width(cnt)), x + 17, y + 4, 0xFFFFFFFF, false);
		g.text(this.font, cnt, x + w - 4 - this.font.width(cnt), y + 4, 0xFFFFFFFF, false);
		g.fill(x + 3, y + 14, x + w - 3, y + 15, 0x60000000);
		if (n > 0) g.fill(x + 3, y + 14, x + 3 + Math.round((w - 6) * d / (float) n), y + 15, 0xFF5AD06A);
		for (int i = 0; i < n; i++) {
			Crit c = t.crits().get(i);
			int cx = x + 4 + (i / TRACK_LINES) * COL_W, cy = y + 18 + (i % TRACK_LINES) * 9;
			int tx = cx;
			if (!c.icon().isEmpty()) {
				Icons.scaled(g, c.icon(), cx, cy, 0.5f);
				tx += 10;
			}
			String label = this.font.plainSubstrByWidth(c.name(), COL_W - (tx - cx) - 10);
			g.text(this.font, c.done() ? "§2✔ §8" + label : "§f" + label, tx, cy, 0xFFFFFFFF, false);
		}
	}

	private void tooltip(GuiGraphicsExtractor g, Entry e, int mx, int my) {
		List<String> lines = new ArrayList<>();
		AdvancementType type = e.display().type();
		String tc = type == AdvancementType.CHALLENGE ? "§d" : type == AdvancementType.GOAL ? "§b" : "§a";
		lines.add(tc + "§l" + e.display().title().getString() + (done(e) ? "  §a✔" : ""));
		lines.addAll(wrap("§7" + e.display().description().getString().replace("\n", " "), 200, 3));
		String kind = type == AdvancementType.CHALLENGE ? "§5Challenge" : type == AdvancementType.GOAL ? "§3Goal" : "§2Advancement";
		lines.add(kind + (done(e) ? " §8· §adone" : ready(e) ? " §8· §fready now" : " §8· §7do the one before first"));
		String hint = HINTS.get(e.holder().id().getPath());
		if (hint != null && !done(e)) lines.addAll(wrap("§6➜ §e" + hint, 200, 2));
		int n = e.holder().value().criteria().size();
		if (n > 1 && !done(e)) {
			List<String> rem = remaining(e);
			lines.add("§e" + (n - rem.size()) + "/" + n + " §7· missing:");
			int show = Math.min(rem.size(), 10);
			for (int i = 0; i < show; i++) lines.add("§c• §f" + rem.get(i));
			if (rem.size() > show) lines.add("§8+ " + (rem.size() - show) + " more (see checklists below)");
		}
		int w = 0;
		for (String l : lines) w = Math.max(w, this.font.width(l));
		int h = lines.size() * 10 + 6;
		int x = mx + 12, y = my - 4;
		if (x + w + 8 > this.width) x = mx - w - 16;
		if (y + h > this.height - 4) y = this.height - 4 - h;
		g.nextStratum();
		g.fill(x, y, x + w + 8, y + h, 0xF0100010);
		g.outline(x, y, w + 8, h, type == AdvancementType.CHALLENGE ? 0xFFB46CFF : 0xFF5050A0);
		for (int i = 0; i < lines.size(); i++) g.text(this.font, lines.get(i), x + 4, y + 4 + i * 10, 0xFFFFFFFF, false);
	}

	private String playTime() {
		var p = this.minecraft.player;
		if (p == null) return null;
		int ticks = p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
		if (ticks <= 0) return null;
		long s = ticks / 20L;
		return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
	}

	// ------------------------------------------------------------ input

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		int max = Math.max(0, contentH - (bottom() - top()));
		scroll = (int) Math.max(0, Math.min(max, scroll - v * 24));
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return super.mouseClicked(event, doubleClick);
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
