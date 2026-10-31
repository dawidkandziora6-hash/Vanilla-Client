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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Every advancement in one list – also hidden ones and ones you haven't unlocked
 * the parent of yet (singleplayer). Shows what is still missing for multi-part
 * advancements (biomes, foods, mobs …) and a short hint what to do.
 */
public final class AdvancementsChecklistScreen extends BaseScreen {
	private record Entry(AdvancementHolder holder, DisplayInfo display, AdvancementProgress progress, int depth, String parent) {}

	private record Tab(AdvancementNode root, DisplayInfo display, List<Entry> entries) {}

	private static int tabIndex;
	private static int filter; // 0 all, 1 to do, 2 done
	private final List<Tab> tabs = new ArrayList<>();
	private final Set<String> expanded = new HashSet<>();
	private String source = "";
	private int scroll, contentH;
	private long loadedAt;

	public AdvancementsChecklistScreen(Screen parent) {
		super(Component.literal("Advancements"), parent);
	}

	private int left() {
		return Math.max(6, this.width / 2 - 230);
	}

	private int tabsW() {
		return 112;
	}

	private int listX() {
		return left() + tabsW() + 6;
	}

	private int listW() {
		return Math.min(this.width - listX() - 8, 340);
	}

	private int top() {
		return 44;
	}

	private int bottom() {
		return this.height - 32;
	}

	@Override
	protected void init() {
		super.init();
		load();
		int y = this.height - 26;
		action(this.width / 2 - 154, y, 100, switch (filter) {
			case 1 -> "Show: §eTo do";
			case 2 -> "Show: §aDone";
			default -> "Show: All";
		}, () -> {
			filter = (filter + 1) % 3;
			scroll = 0;
			rebuildWidgets();
		});
		action(this.width / 2 - 50, y, 100, "Vanilla screen", () -> {
			if (this.minecraft.getConnection() != null) {
				this.minecraft.setScreenAndShow(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(this.minecraft.getConnection().getAdvancements()));
			}
		});
		action(this.width / 2 + 54, y, 100, "✔ Done", this::onClose);
	}

	private void load() {
		tabs.clear();
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
					source = "§aAll advancements (singleplayer)";
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
			source = "§7Only advancements the server has shown you";
		}
		if (tree == null) {
			source = "§7Join a world first";
			return;
		}
		for (AdvancementNode root : tree.roots()) {
			if (root.advancement().display().isEmpty()) continue;
			List<Entry> list = new ArrayList<>();
			collect(root, 0, null, list, prog);
			tabs.add(new Tab(root, root.advancement().display().get(), list));
		}
		tabIndex = Math.max(0, Math.min(tabIndex, tabs.size() - 1));
	}

	private void collect(AdvancementNode node, int depth, String parent, List<Entry> out, Function<AdvancementHolder, AdvancementProgress> prog) {
		var d = node.advancement().display();
		String title = parent;
		if (d.isPresent()) {
			out.add(new Entry(node.holder(), d.get(), prog.apply(node.holder()), depth, parent));
			title = d.get().title().getString();
		}
		for (AdvancementNode c : node.children()) collect(c, depth + 1, title, out, prog);
	}

	private static boolean done(Entry e) {
		return e.progress() != null && e.progress().isDone();
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

	private List<String> remaining(Entry e) {
		List<String> out = new ArrayList<>();
		if (e.progress() == null) return out;
		for (String s : e.progress().getRemainingCriteria()) out.add(pretty(s));
		out.sort(String::compareTo);
		return out;
	}

	private int criteriaCount(Entry e) {
		return e.holder().value().criteria().size();
	}

	private List<Entry> visible() {
		if (tabs.isEmpty()) return List.of();
		List<Entry> out = new ArrayList<>();
		for (Entry e : tabs.get(tabIndex).entries()) {
			boolean d = done(e);
			if (filter == 1 && d) continue;
			if (filter == 2 && !d) continue;
			out.add(e);
		}
		return out;
	}

	// ------------------------------------------------------------ drawing

	private int rowH(Entry e) {
		int h = 30;
		if (hint(e) != null) h += 10;
		if (expanded.contains(e.holder().id().toString())) {
			int n = remaining(e).size();
			int perLine = Math.max(1, (listW() - 16) / 92);
			h += ((n + perLine - 1) / perLine) * 10 + 4;
		}
		return h;
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		if (System.currentTimeMillis() - loadedAt > 2000) load();
		float in = intro();
		g.centeredText(this.font, "§l✦ Advancements", this.width / 2, 8, Theme.alpha(Theme.ACCENT, in));
		int doneAll = 0, total = 0;
		for (Tab t : tabs) for (Entry e : t.entries()) {
			total++;
			if (done(e)) doneAll++;
		}
		g.centeredText(this.font, source + " §8· §f" + doneAll + "§7/" + total + " done", this.width / 2, 19, 0xFFFFFFFF);
		int bw = 200, bx = this.width / 2 - bw / 2;
		g.fill(bx, 31, bx + bw, 34, 0x60000000);
		if (total > 0) g.fill(bx, 31, bx + Math.round(bw * doneAll / (float) total), 34, 0xFF5AD06A);

		// tabs
		int tx = left(), ty = top();
		Theme.panel(g, tx - 2, ty - 2, tabsW() + 4, bottom() - ty + 4);
		for (int i = 0; i < tabs.size(); i++) {
			Tab t = tabs.get(i);
			int y = ty + i * 26;
			int d = 0;
			for (Entry e : t.entries()) if (done(e)) d++;
			boolean sel = i == tabIndex;
			boolean hov = mouseX >= tx && mouseX < tx + tabsW() && mouseY >= y && mouseY < y + 24;
			g.fill(tx, y, tx + tabsW(), y + 24, sel ? 0x50FFFFFF : hov ? 0x30FFFFFF : 0x18000000);
			if (sel) g.fill(tx, y, tx + 2, y + 24, Theme.ACCENT);
			g.item(icon(t.display()), tx + 4, y + 4);
			g.text(this.font, this.font.plainSubstrByWidth(t.display().title().getString(), tabsW() - 26), tx + 23, y + 3, sel ? Theme.ACCENT : 0xFFFFFFFF, false);
			g.text(this.font, (d == t.entries().size() ? "§a" : "§7") + d + "/" + t.entries().size(), tx + 23, y + 13, 0xFFFFFFFF, false);
		}

		// list
		int lx = listX(), lw = listW();
		Theme.panel(g, lx - 2, ty - 2, lw + 4, bottom() - ty + 4);
		g.enableScissor(lx, ty, lx + lw, bottom());
		int y = ty + 2 - scroll;
		for (Entry e : visible()) {
			int h = rowH(e);
			if (y + h >= ty && y < bottom()) drawRow(g, e, lx, y, lw, h, mouseX, mouseY);
			y += h + 2;
		}
		contentH = y + scroll - ty;
		if (visible().isEmpty()) g.centeredText(this.font, filter == 1 ? "§aEverything done here!" : "§7Nothing here", lx + lw / 2, ty + 20, 0xFFFFFFFF);
		g.disableScissor();
	}

	private static ItemStack icon(DisplayInfo d) {
		try {
			return d.icon().create();
		} catch (RuntimeException e) {
			return ItemStack.EMPTY;
		}
	}

	private void drawRow(GuiGraphicsExtractor g, Entry e, int x, int y, int w, int h, int mx, int my) {
		boolean d = done(e);
		boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
		AdvancementType type = e.display().type();
		int typeCol = type == AdvancementType.CHALLENGE ? 0xFFB46CFF : type == AdvancementType.GOAL ? 0xFF55D8FF : 0xFF8AE07A;
		g.fill(x, y, x + w, y + h, d ? (hov ? 0x40306030 : 0x30204020) : (hov ? 0x40FFFFFF : 0x28000000));
		g.fill(x, y, x + 2, y + h, typeCol);
		int indent = Math.min(3, e.depth()) * 3;
		g.item(icon(e.display()), x + 5 + indent, y + 6);
		int tx = x + 25 + indent;
		String title = e.display().title().getString();
		String tag = type == AdvancementType.CHALLENGE ? " §5challenge" : type == AdvancementType.GOAL ? " §3goal" : "";
		g.text(this.font, (d ? "§a✔ " : "§f") + this.font.plainSubstrByWidth(title, w - (tx - x) - 60) + tag, tx, y + 4, 0xFFFFFFFF, false);
		String desc = e.display().description().getString();
		g.text(this.font, "§7" + this.font.plainSubstrByWidth(desc, w - (tx - x) - 6), tx, y + 15, 0xFFFFFFFF, false);
		// progress
		int n = criteriaCount(e);
		if (n > 1 && e.progress() != null) {
			int left = remaining(e).size();
			String ps = (n - left) + "/" + n;
			g.text(this.font, (d ? "§a" : "§e") + ps, x + w - 6 - this.font.width(ps), y + 4, 0xFFFFFFFF, false);
			if (!d) {
				float f = (n - left) / (float) n;
				g.fill(x + w - 46, y + 25, x + w - 6, y + 27, 0x60000000);
				g.fill(x + w - 46, y + 25, x + w - 46 + Math.round(40 * f), y + 27, 0xFFFFD34D);
				String more = expanded.contains(e.holder().id().toString()) ? "§8▴ missing" : "§8▾ missing";
				g.text(this.font, more, x + w - 50 - this.font.width(more), y + 22, 0xFFFFFFFF, false);
			}
		}
		int yy = y + 28;
		String hint = hint(e);
		if (hint != null) {
			g.text(this.font, "§6➜ §e" + this.font.plainSubstrByWidth(hint, w - (tx - x) - 14), tx, yy, 0xFFFFFFFF, false);
			yy += 10;
		}
		if (expanded.contains(e.holder().id().toString())) {
			List<String> rem = remaining(e);
			int perLine = Math.max(1, (w - 16) / 92);
			for (int i = 0; i < rem.size(); i++) {
				int cx = x + 10 + (i % perLine) * 92;
				int cy = yy + 2 + (i / perLine) * 10;
				g.text(this.font, "§c• §f" + this.font.plainSubstrByWidth(rem.get(i), 84), cx, cy, 0xFFFFFFFF, false);
			}
		}
	}

	// ------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		double mx = event.x(), my = event.y();
		if (mx >= left() && mx < left() + tabsW()) {
			int i = (int) ((my - top()) / 26);
			if (my >= top() && i >= 0 && i < tabs.size()) {
				dev.vanillaclient.gui.ui.Ui.click();
				tabIndex = i;
				scroll = 0;
				return true;
			}
		}
		if (mx >= listX() && mx < listX() + listW() && my >= top() && my < bottom()) {
			int y = top() + 2 - scroll;
			for (Entry e : visible()) {
				int h = rowH(e);
				if (my >= y && my < y + h) {
					if (criteriaCount(e) > 1 && !done(e)) {
						dev.vanillaclient.gui.ui.Ui.click();
						String id = e.holder().id().toString();
						if (!expanded.remove(id)) expanded.add(id);
					}
					return true;
				}
				y += h + 2;
			}
		}
		return false;
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

	@SuppressWarnings("unused")
	private static String lower(String s) {
		return s.toLowerCase(Locale.ROOT);
	}
}
