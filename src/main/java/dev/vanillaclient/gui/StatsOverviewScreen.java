package dev.vanillaclient.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** Your world in numbers: big stat cards and top lists (mined, killed, killed by, crafted). */
public final class StatsOverviewScreen extends BaseScreen {
	private record Card(Item icon, String label, Identifier stat, int kind) {}

	private static final int TIME = 0, DIST = 1, NUM = 2, DMG = 3;
	private static final List<Card> CARDS = List.of(
			new Card(Items.CLOCK, "Time played", Stats.PLAY_TIME, TIME),
			new Card(Items.TOTEM_OF_UNDYING, "Since last death", Stats.TIME_SINCE_DEATH, TIME),
			new Card(Items.SKELETON_SKULL, "Deaths", Stats.DEATHS, NUM),
			new Card(Items.IRON_SWORD, "Mob kills", Stats.MOB_KILLS, NUM),
			new Card(Items.DIAMOND_SWORD, "Player kills", Stats.PLAYER_KILLS, NUM),
			new Card(Items.NETHERITE_SWORD, "Damage dealt", Stats.DAMAGE_DEALT, DMG),
			new Card(Items.SHIELD, "Damage taken", Stats.DAMAGE_TAKEN, DMG),
			new Card(Items.LEATHER_BOOTS, "Walked", Stats.WALK_ONE_CM, DIST),
			new Card(Items.IRON_BOOTS, "Sprinted", Stats.SPRINT_ONE_CM, DIST),
			new Card(Items.ELYTRA, "Flown (elytra)", Stats.AVIATE_ONE_CM, DIST),
			new Card(Items.WATER_BUCKET, "Swum", Stats.SWIM_ONE_CM, DIST),
			new Card(Items.OAK_BOAT, "By boat", Stats.BOAT_ONE_CM, DIST),
			new Card(Items.SADDLE, "By horse", Stats.HORSE_ONE_CM, DIST),
			new Card(Items.FEATHER, "Fallen", Stats.FALL_ONE_CM, DIST),
			new Card(Items.RABBIT_FOOT, "Jumps", Stats.JUMP, NUM),
			new Card(Items.EMERALD, "Villager trades", Stats.TRADED_WITH_VILLAGER, NUM),
			new Card(Items.ENCHANTING_TABLE, "Items enchanted", Stats.ENCHANT_ITEM, NUM),
			new Card(Items.WHEAT, "Animals bred", Stats.ANIMALS_BRED, NUM),
			new Card(Items.FISHING_ROD, "Fish caught", Stats.FISH_CAUGHT, NUM),
			new Card(Items.STRAW_BED, "Nights slept", Stats.SLEEP_IN_BED, NUM));

	private record Top(ItemStack icon, String name, int value) {}

	private int scroll;
	private int contentH;

	public StatsOverviewScreen(Screen parent) {
		super(Component.literal("Statistics"), parent);
	}

	@Override
	protected void init() {
		super.init();
		var p = this.minecraft.player;
		if (p != null) {
			try {
				p.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
			} catch (RuntimeException ignored) {
				// offline
			}
		}
		action(this.width / 2 - 154, this.height - 26, 150, "Vanilla statistics", () -> {
			if (p != null) this.minecraft.setScreenAndShow(new net.minecraft.client.gui.screens.achievement.StatsScreen(this, p.getStats()));
		});
		action(this.width / 2 + 4, this.height - 26, 150, "✔ Done", this::onClose);
	}

	private static String fmt(int v, int kind) {
		switch (kind) {
			case TIME -> {
				long s = v / 20L;
				long d = s / 86400, h = (s / 3600) % 24, m = (s / 60) % 60;
				return d > 0 ? d + "d " + h + "h" : h > 0 ? h + "h " + m + "m" : m + "m " + (s % 60) + "s";
			}
			case DIST -> {
				double m = v / 100.0;
				return m >= 1000 ? String.format(Locale.ROOT, "%.2f km", m / 1000) : Math.round(m) + " m";
			}
			case DMG -> {
				return String.format(Locale.ROOT, "%.1f ♥", v / 20.0);
			}
			default -> {
				return String.format(Locale.ROOT, "%,d", v);
			}
		}
	}

	private List<Top> topBlocks(StatsCounter c) {
		List<Top> out = new ArrayList<>();
		for (Stat<Block> s : Stats.BLOCK_MINED) {
			int v = c.getValue(s);
			if (v > 0) out.add(new Top(Icons.stack(s.getValue().asItem()), s.getValue().getName().getString(), v));
		}
		out.sort(Comparator.comparingInt(t -> -t.value()));
		return out;
	}

	private List<Top> topEntities(StatsCounter c, net.minecraft.stats.StatType<EntityType<?>> type) {
		List<Top> out = new ArrayList<>();
		for (Stat<EntityType<?>> s : type) {
			int v = c.getValue(s);
			if (v > 0) out.add(new Top(Icons.stack(Items.ZOMBIE_HEAD), s.getValue().getDescription().getString(), v));
		}
		out.sort(Comparator.comparingInt(t -> -t.value()));
		return out;
	}

	private List<Top> topItems(StatsCounter c, net.minecraft.stats.StatType<Item> type) {
		List<Top> out = new ArrayList<>();
		for (Stat<Item> s : type) {
			int v = c.getValue(s);
			if (v > 0) out.add(new Top(Icons.stack(s.getValue()), Icons.name(s.getValue()), v));
		}
		out.sort(Comparator.comparingInt(t -> -t.value()));
		return out;
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		float in = intro();
		g.centeredText(this.font, "§l✦ Statistics", this.width / 2, 8, Theme.alpha(Theme.ACCENT, in));
		var p = this.minecraft.player;
		if (p == null) {
			g.centeredText(this.font, "§7Join a world to see your statistics", this.width / 2, this.height / 2, 0xFFFFFFFF);
			return;
		}
		StatsCounter c = p.getStats();
		int top = 26, bottom = this.height - 34;
		int w = Math.min(420, this.width - 20);
		int x0 = this.width / 2 - w / 2;
		g.enableScissor(x0 - 2, top, x0 + w + 2, bottom);
		int y = top + 2 - scroll;
		int cols = w >= 4 * 120 + 12 ? 4 : 3;
		int cw = (w - (cols - 1) * 4) / cols;
		long now = System.currentTimeMillis();
		for (int i = 0; i < CARDS.size(); i++) {
			Card cd = CARDS.get(i);
			int cx = x0 + (i % cols) * (cw + 4);
			int cy = y + (i / cols) * 34;
			float t = Theme.easeOut((now - openedAt - i * 25L) / 260f);
			int off = Math.round((1f - t) * 8);
			boolean hov = mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + 31;
			g.fill(cx, cy + off, cx + cw, cy + 31 + off, Theme.alpha(hov ? 0x60303030 : 0x50000000, t));
			g.outline(cx, cy + off, cw, 31, Theme.alpha(hov ? Theme.ACCENT : 0x30FFFFFF, t));
			g.item(Icons.stack(cd.icon()), cx + 5, cy + 8 + off);
			g.text(this.font, "§7" + this.font.plainSubstrByWidth(cd.label(), cw - 30), cx + 25, cy + 5 + off, Theme.alpha(0xFFFFFFFF, t), false);
			int v;
			try {
				v = c.getValue(Stats.CUSTOM.get(cd.stat()));
			} catch (RuntimeException e) {
				v = 0;
			}
			g.text(this.font, "§l" + fmt(v, cd.kind()), cx + 25, cy + 17 + off, Theme.alpha(0xFFFFFFFF, t), false);
		}
		y += ((CARDS.size() + cols - 1) / cols) * 34 + 6;
		// top lists
		List<List<Top>> lists = List.of(topBlocks(c), topEntities(c, Stats.ENTITY_KILLED), topEntities(c, Stats.ENTITY_KILLED_BY), topItems(c, Stats.ITEM_CRAFTED));
		String[] titles = {"Most mined", "Most killed", "Killed you", "Most crafted"};
		int lc = cols >= 4 ? 4 : 2;
		int lw = (w - (lc - 1) * 4) / lc;
		int maxRows = 8;
		for (int k = 0; k < lists.size(); k++) {
			int lx = x0 + (k % lc) * (lw + 4);
			int ly = y + (k / lc) * (maxRows * 12 + 22);
			g.fill(lx, ly, lx + lw, ly + maxRows * 12 + 16, 0x40000000);
			g.text(this.font, "§e" + titles[k], lx + 4, ly + 4, 0xFFFFFFFF, false);
			List<Top> l = lists.get(k);
			if (l.isEmpty()) g.text(this.font, "§8nothing yet", lx + 4, ly + 16, 0xFFFFFFFF, false);
			for (int r = 0; r < Math.min(maxRows, l.size()); r++) {
				Top tp = l.get(r);
				int ry = ly + 15 + r * 12;
				if (!tp.icon().isEmpty() && k != 1 && k != 2) Icons.small(g, tp.icon(), lx + 3, ry + 1);
				String val = String.format(Locale.ROOT, "%,d", tp.value());
				int vw = this.font.width(val);
				String name = this.font.plainSubstrByWidth(tp.name(), lw - 18 - vw - 4);
				g.text(this.font, (r < 3 ? "§f" : "§7") + name, lx + 14, ry + 2, 0xFFFFFFFF, false);
				g.text(this.font, "§b" + val, lx + lw - 4 - vw, ry + 2, 0xFFFFFFFF, false);
			}
		}
		int rowsOfLists = (lists.size() + lc - 1) / lc;
		y += rowsOfLists * (maxRows * 12 + 22);
		contentH = y + scroll - top;
		g.disableScissor();
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		int max = Math.max(0, contentH - (this.height - 34 - 26));
		scroll = (int) Math.max(0, Math.min(max, scroll - v * 20));
		return true;
	}
}
