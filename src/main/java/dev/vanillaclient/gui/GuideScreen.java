package dev.vanillaclient.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.vanillaclient.gui.ui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** UHC guide: exact villager trades, best enchantments and a brewing table. */
public final class GuideScreen extends BaseScreen {
	private enum Tab {
		VILLAGERS("Trades"), ENCHANTS("Enchants"), BREWING("Brewing");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private static final int[] LEVEL_COLORS = {0xFF9A9A9A, 0xFFD9D9D9, 0xFFF5C846, 0xFF3CC864, 0xFF6EE6E6};
	private static Tab tab = Tab.VILLAGERS;
	private static int profession;

	private final List<GuideData.Profession> villagers = GuideData.villagers();
	private float scroll, scrollTarget;
	private long tabAt = System.currentTimeMillis();
	private EditBox search;
	private String filter = "";
	private int contentH;

	public GuideScreen(Screen parent) {
		super(Component.literal("UHC Guide"), parent);
	}

	private int panelW() {
		return Math.min(420, this.width - 20);
	}

	private int px() {
		return (this.width - panelW()) / 2;
	}

	private int top() {
		return 58;
	}

	private int bottom() {
		return this.height - 34;
	}

	private int listW() {
		return tab == Tab.VILLAGERS ? 96 : 0;
	}

	@Override
	protected void init() {
		super.init();
		int x = px();
		int tw = (panelW() - 8) / 3;
		Item[] icons = {Items.EMERALD, Items.ENCHANTED_BOOK, Items.BREWING_STAND};
		for (Tab t : Tab.values()) {
			Button b = iconAction(x + t.ordinal() * (tw + 4), 30, tw, Icons.stack(icons[t.ordinal()]), "    " + t.label, () -> {
				tab = t;
				scroll = scrollTarget = 0;
				tabAt = System.currentTimeMillis();
				rebuildWidgets();
			});
			b.active = t != tab;
		}
		if (tab != Tab.VILLAGERS) {
			String old = search == null ? "" : search.getValue();
			search = new EditBox(this.font, x + 1, this.height - 27, panelW() - 108, 18, Component.literal("Search"));
			search.setHint(Component.literal("Search …"));
			search.setValue(old);
			search.setResponder(v -> {
				filter = v.trim().toLowerCase(Locale.ROOT);
				scroll = scrollTarget = 0;
			});
			addRenderableWidget(search);
		} else {
			search = null;
			filter = "";
		}
		action(x + panelW() - 100, this.height - 28, 100, "✔ Done", this::onClose);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		int max = Math.max(0, contentH - (bottom() - top()));
		scrollTarget = (float) Math.max(0, Math.min(max, scrollTarget - v * 30));
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		if (tab == Tab.VILLAGERS && event.x() >= px() && event.x() < px() + listW()) {
			int i = (int) ((event.y() - top()) / 15);
			if (i >= 0 && i < villagers.size()) {
				profession = i;
				scroll = scrollTarget = 0;
				tabAt = System.currentTimeMillis();
				Ui.click();
				return true;
			}
		}
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		Theme.panel(g, px() - 4, top() - 4, panelW() + 8, bottom() - top() + 8);
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		float in = intro();
		g.centeredText(this.font, "§l✦ UHC Guide ✦", this.width / 2, 8, Theme.alpha(Theme.ACCENT, in));
		String sub = switch (tab) {
			case VILLAGERS -> "§7Exact trades per level – villagers get 2 random ones per level";
			case ENCHANTS -> "§7The best enchantments for every item";
			case BREWING -> "§7Every potion: what you need and where it comes from";
		};
		g.centeredText(this.font, sub, this.width / 2, 19, 0xFFFFFFFF);
		scroll += (scrollTarget - scroll) * 0.3f;
		switch (tab) {
			case VILLAGERS -> drawVillagers(g, mouseX, mouseY);
			case ENCHANTS -> drawEnchants(g, mouseX, mouseY);
			case BREWING -> drawBrewing(g, mouseX, mouseY);
		}
	}

	private float appear(int i) {
		return Theme.easeOut((System.currentTimeMillis() - tabAt - i * 30L) / 240f);
	}

	// ------------------------------------------------------------ trades

	private void drawVillagers(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = px(), lw = listW();
		// profession list
		for (int i = 0; i < villagers.size(); i++) {
			GuideData.Profession p = villagers.get(i);
			int y = top() + i * 15;
			if (y + 14 > bottom()) break;
			boolean sel = i == profession;
			boolean hover = mouseX >= x && mouseX < x + lw && mouseY >= y && mouseY < y + 15;
			if (sel) {
				g.fill(x, y, x + lw - 4, y + 14, 0x40FFFFFF);
				g.fill(x, y, x + 2, y + 14, Theme.ACCENT);
			} else if (hover) {
				g.fill(x, y, x + lw - 4, y + 14, Theme.ROW_HOVER);
			}
			Icons.small(g, Icons.stack(p.workstation()), x + 4, y + 3);
			g.text(this.font, p.name(), x + 15 + (hover ? 1 : 0), y + 3, sel ? Theme.ACCENT : 0xFFD0D0D0, false);
		}
		g.fill(x + lw - 2, top(), x + lw - 1, bottom(), 0x30FFFFFF);

		// trades of the selected profession
		GuideData.Profession p = villagers.get(Math.min(profession, villagers.size() - 1));
		int cx = x + lw + 6, cw = panelW() - lw - 6;
		g.enableScissor(cx - 2, top(), cx + cw + 2, bottom());
		int y = top() - Math.round(scroll);
		int start = y;
		Icons.scaled(g, Icons.stack(p.workstation()), cx, y, 1.25f);
		g.text(this.font, "§l" + p.name(), cx + 24, y + 2, 0xFFFFFFFF, false);
		g.text(this.font, "§7Workstation: " + p.station(), cx + 24, y + 12, 0xFFFFFFFF, false);
		y += 26;
		int idx = 0;
		int colW = cw >= 300 ? (cw - 6) / 2 : cw;
		for (int li = 0; li < p.levels().size(); li++) {
			GuideData.Level lv = p.levels().get(li);
			float t = appear(idx++);
			int off = Math.round((1f - t) * 12f);
			int col = LEVEL_COLORS[li];
			g.fill(cx + off, y + 4, cx + off + 6, y + 10, col);
			g.text(this.font, lv.name(), cx + 10 + off, y + 3, Theme.alpha(col, t), false);
			g.fill(cx + 12 + this.font.width(lv.name()) + off, y + 7, cx + cw, y + 8, 0x30FFFFFF);
			y += 14;
			int c = 0;
			for (GuideData.Trade tr : lv.trades()) {
				int tx = cx + (c % 2 == 1 && colW < cw ? colW + 6 : 0) + off;
				drawTrade(g, tr, tx, y, colW, mouseX, mouseY, t);
				c++;
				if (colW >= cw || c % 2 == 0) y += 20;
			}
			if (colW < cw && c % 2 == 1) y += 20;
			y += 4;
		}
		if (p.name().equals("Librarian")) {
			y += 2;
			g.text(this.font, "§eEnchanted book prices", cx, y, 0xFFFFFFFF, false);
			y += 11;
			g.text(this.font, "§7I 5-19 · II 8-32 · III 11-45 · IV 14-58 · V 17-64", cx, y, 0xFFFFFFFF, false);
			y += 10;
			g.text(this.font, "§7Mending (treasure): §b10-38 §7· price ×2 for treasure", cx, y, 0xFFFFFFFF, false);
			y += 10;
			g.text(this.font, "§7Reroll: break the lectern before you trade", cx, y, 0xFFFFFFFF, false);
			y += 12;
		}
		g.text(this.font, "§8Cheaper: cure zombie villagers · Hero of the Village", cx, y + 2, 0xFFFFFFFF, false);
		y += 14;
		contentH = y - start;
		g.disableScissor();
	}

	private void slot(GuiGraphicsExtractor g, Item item, String count, int x, int y) {
		g.fill(x, y, x + 18, y + 18, 0x40000000);
		g.outline(x, y, 18, 18, 0x30FFFFFF);
		if (item != null) g.item(Icons.stack(item), x + 1, y + 1);
		if (count != null && !count.equals("1")) {
			g.pose().pushMatrix();
			g.pose().translate(x + 18, y + 11);
			float s = count.length() > 2 ? 0.75f : 1f;
			g.pose().scale(s, s);
			g.text(this.font, count, -this.font.width(count), 0, 0xFFFFFFFF, true);
			g.pose().popMatrix();
		}
	}

	private void drawTrade(GuiGraphicsExtractor g, GuideData.Trade tr, int x, int y, int w, int mouseX, int mouseY, float a) {
		boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 19 && mouseY >= top() && mouseY < bottom();
		if (hover) g.fill(x - 2, y - 1, x + w, y + 19, Theme.ROW_HOVER);
		int xx = x;
		if (tr.in1() != null) {
			slot(g, tr.in1(), tr.c1(), xx, y);
			xx += 20;
		}
		if (tr.in2() != null) {
			g.text(this.font, "§7+", xx + 1, y + 5, 0xFFFFFFFF, false);
			xx += 8;
			slot(g, tr.in2(), tr.c2(), xx, y);
			xx += 20;
		}
		if (tr.in1() != null) {
			g.text(this.font, "§e→", xx + 2, y + 5, 0xFFFFFFFF, false);
			xx += 14;
		}
		slot(g, tr.out(), tr.cOut(), xx, y);
		xx += 22;
		String name = tr.note() != null && tr.in1() == null ? tr.note()
				: Icons.name(tr.out()) + (tr.note() != null ? " §b(" + tr.note() + ")" : "");
		g.text(this.font, this.font.plainSubstrByWidth(name, x + w - xx - 2), xx, y + 5, Theme.alpha(0xFFE0E0E0, a), false);
	}

	// ------------------------------------------------------------ enchants

	private List<String> wrap(String text, int width) {
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String test = line.length() == 0 ? word : line + " " + word;
			if (this.font.width(test) > width && line.length() > 0) {
				out.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(test);
			}
		}
		if (line.length() > 0) out.add(line.toString());
		return out;
	}

	private void drawEnchants(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = px(), w = panelW();
		g.enableScissor(x - 2, top(), x + w + 2, bottom());
		int y = top() - Math.round(scroll);
		int start = y;
		int i = 0;
		for (GuideData.Enchant e : GuideData.enchants()) {
			String all = (e.item() + " " + e.best() + " " + e.alt()).toLowerCase(Locale.ROOT);
			if (!filter.isEmpty() && !all.contains(filter)) continue;
			List<String> lines = wrap(e.best(), w - 40);
			if (!e.alt().isEmpty()) lines.addAll(wrap("§8or: " + e.alt(), w - 40));
			int h = 16 + lines.size() * 10;
			float t = appear(i++);
			int off = Math.round((1f - t) * 14f);
			boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h - 2;
			g.fill(x + off, y, x + w, y + h - 3, Theme.alpha(hover ? 0x38FFFFFF : 0x1CFFFFFF, t));
			float bob = hover ? (float) Math.sin(Theme.time() * 6) * 1.2f : 0f;
			Icons.scaled(g, Icons.stack(e.icon()), x + 6 + off, y + 3 + bob, 1f);
			g.text(this.font, "§l" + e.item(), x + 28 + off, y + 3, Theme.alpha(0xFFFFFFFF, t), false);
			int ly = y + 14;
			for (String l : lines) {
				g.text(this.font, l.startsWith("§8") ? l : "§b" + l, x + 28 + off, ly, Theme.alpha(0xFFFFFFFF, t), false);
				ly += 10;
			}
			y += h;
		}
		y += 4;
		g.text(this.font, "§eCan't be combined", x, y, 0xFFFFFFFF, false);
		y += 11;
		for (String l : wrap("§7Protection types · Sharpness/Smite/Bane · Fortune/Silk Touch · Infinity/Mending · Multishot/Piercing · Depth Strider/Frost Walker · Riptide/Loyalty+Channeling", w)) {
			g.text(this.font, l, x, y, 0xFFFFFFFF, false);
			y += 10;
		}
		g.text(this.font, "§715 bookshelves = level 30 enchanting", x, y, 0xFFFFFFFF, false);
		y += 14;
		contentH = y - start;
		g.disableScissor();
	}

	// ------------------------------------------------------------ brewing

	private void drawBrewing(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = px(), w = panelW();
		g.enableScissor(x - 2, top(), x + w + 2, bottom());
		int y = top() - Math.round(scroll);
		int start = y;
		// base recipe flow
		g.fill(x, y, x + w, y + 26, 0x24FFFFFF);
		int fx = x + 6;
		slot(g, Items.POTION, null, fx, y + 4);
		g.text(this.font, "§7Water", fx + 21, y + 9, 0xFFFFFFFF, false);
		fx += 21 + this.font.width("Water") + 6;
		g.text(this.font, "§7+", fx, y + 9, 0xFFFFFFFF, false);
		slot(g, Items.NETHER_WART, null, fx + 8, y + 4);
		fx += 30;
		g.text(this.font, "§e→", fx, y + 9, 0xFFFFFFFF, false);
		slot(g, Items.POTION, null, fx + 12, y + 4);
		g.text(this.font, "§fAwkward Potion", fx + 33, y + 9, 0xFFFFFFFF, false);
		Icons.small(g, Icons.stack(Items.BLAZE_POWDER), x + w - 64, y + 9);
		g.text(this.font, "§7= fuel", x + w - 54, y + 9, 0xFFFFFFFF, false);
		y += 32;

		// column headers
		int cIng = x + 4, cName = x + 26, cTime = x + w - 132, cRed = x + w - 88, cGlow = x + w - 44;
		g.text(this.font, "§7Potion", cName, y, 0xFFFFFFFF, false);
		g.text(this.font, "§7Time", cTime, y, 0xFFFFFFFF, false);
		Icons.small(g, Icons.stack(Items.REDSTONE), cRed, y);
		g.text(this.font, "§cLong", cRed + 10, y, 0xFFFFFFFF, false);
		Icons.small(g, Icons.stack(Items.GLOWSTONE_DUST), cGlow, y);
		g.text(this.font, "§eStrong", cGlow + 10, y, 0xFFFFFFFF, false);
		y += 11;
		int i = 0;
		for (GuideData.Potion p : GuideData.potions()) {
			String all = (p.effect() + " " + p.source() + " " + p.base() + " " + Icons.name(p.ingredient())).toLowerCase(Locale.ROOT);
			if (!filter.isEmpty() && !all.contains(filter)) continue;
			float t = appear(i);
			int off = Math.round((1f - t) * 14f);
			boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 24 && mouseY >= top();
			g.fill(x + off, y, x + w, y + 23, Theme.alpha(hover ? 0x38FFFFFF : (i % 2 == 0 ? 0x1CFFFFFF : 0x10FFFFFF), t));
			g.fill(x + off, y, x + 2 + off, y + 23, p.color());
			i++;
			slot(g, p.ingredient(), null, cIng + off, y + 2);
			g.text(this.font, p.effect(), cName + off, y + 3, p.color(), true);
			String ing = Icons.name(p.ingredient());
			String from = "§7" + ing + " + " + p.base() + " §8· " + p.source();
			g.text(this.font, this.font.plainSubstrByWidth(from, cTime - cName - 6), cName + off, y + 13, 0xFFFFFFFF, false);
			g.text(this.font, "§f" + p.time(), cTime, y + 8, 0xFFFFFFFF, false);
			g.text(this.font, p.redstone().equals("–") ? "§8–" : "§c" + p.redstone(), cRed, y + 8, 0xFFFFFFFF, false);
			g.text(this.font, p.glowstone().equals("–") ? "§8–" : "§e" + p.glowstone(), cGlow, y + 8, 0xFFFFFFFF, false);
			y += 25;
		}
		y += 6;
		g.text(this.font, "§eAdd to any potion", x, y, 0xFFFFFFFF, false);
		y += 12;
		int mx = x;
		Item[] mods = {Items.GUNPOWDER, Items.DRAGON_BREATH};
		String[] labels = {"§7Gunpowder → §fSplash", "§7Dragon's Breath → §fLingering"};
		for (int k = 0; k < 2; k++) {
			slot(g, mods[k], null, mx, y);
			g.text(this.font, labels[k], mx + 22, y + 5, 0xFFFFFFFF, false);
			mx += 24 + this.font.width(labels[k]) + 14;
		}
		y += 26;
		contentH = y - start;
		g.disableScissor();
	}
}
