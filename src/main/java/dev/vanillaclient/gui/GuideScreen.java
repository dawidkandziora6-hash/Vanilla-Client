package dev.vanillaclient.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/** UHC guide: villagers, best enchantments, brewing and survival tips. */
public final class GuideScreen extends BaseScreen {
	private enum Tab {
		VILLAGERS("Villagers"), ENCHANTS("Enchants"), BREWING("Brewing"), TIPS("Tips");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private static Tab tab = Tab.VILLAGERS;
	private float scroll, scrollTarget;
	private long tabAt = System.currentTimeMillis();
	private EditBox search;
	private String filter = "";
	private int contentH;

	public GuideScreen(Screen parent) {
		super(Component.literal("UHC Guide"), parent);
	}

	private List<GuideData.Entry> entries() {
		return switch (tab) {
			case VILLAGERS -> GuideData.villagers();
			case ENCHANTS -> GuideData.enchants();
			case BREWING -> GuideData.brewing();
			case TIPS -> GuideData.tips();
		};
	}

	private int panelW() {
		return Math.min(340, this.width - 20);
	}

	private int px() {
		return (this.width - panelW()) / 2;
	}

	private int top() {
		return 60;
	}

	private int bottom() {
		return this.height - 34;
	}

	@Override
	protected void init() {
		super.init();
		int x = px();
		int tw = (panelW() - 6 * 3) / 4;
		var icons = new net.minecraft.world.item.Item[] {Items.EMERALD, Items.ENCHANTED_BOOK, Items.BREWING_STAND, Items.GOLDEN_APPLE};
		for (Tab t : Tab.values()) {
			Button b = iconAction(x + t.ordinal() * (tw + 6), 30, tw, Icons.stack(icons[t.ordinal()]), "    " + t.label, () -> {
				tab = t;
				scroll = scrollTarget = 0;
				tabAt = System.currentTimeMillis();
				rebuildWidgets();
			});
			b.active = t != tab;
		}
		String old = search == null ? "" : search.getValue();
		search = new EditBox(this.font, x + 1, this.height - 27, panelW() - 108, 18, Component.literal("Search"));
		search.setHint(Component.literal("Search …"));
		search.setValue(old);
		search.setResponder(v -> {
			filter = v.trim().toLowerCase(java.util.Locale.ROOT);
			scroll = scrollTarget = 0;
		});
		addRenderableWidget(search);
		action(x + panelW() - 100, this.height - 28, 100, "✔ Done", this::onClose);
	}

	private boolean matches(GuideData.Entry e) {
		if (filter.isEmpty()) return true;
		StringBuilder b = new StringBuilder(e.title()).append(' ').append(e.sub());
		for (GuideData.Line l : e.lines()) b.append(' ').append(l.text());
		return b.toString().replaceAll("§.", "").toLowerCase(java.util.Locale.ROOT).contains(filter);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		int max = Math.max(0, contentH - (bottom() - top()));
		scrollTarget = (float) Math.max(0, Math.min(max, scrollTarget - v * 30));
		return true;
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
		g.centeredText(this.font, "§7Everything you never remember – right when you need it", this.width / 2, 19, 0xFFFFFFFF);

		scroll += (scrollTarget - scroll) * 0.3f;
		int x = px(), w = panelW();
		int y = top() - Math.round(scroll);
		long now = System.currentTimeMillis();
		g.enableScissor(x - 2, top(), x + w + 2, bottom());
		int i = 0;
		int start = y;
		for (GuideData.Entry e : entries()) {
			if (!matches(e)) continue;
			int h = 22 + e.lines().size() * 11;
			float t = Theme.easeOut((now - tabAt - i * 35L) / 260f);
			int off = Math.round((1f - t) * 16f);
			i++;
			if (y + h >= top() && y <= bottom()) {
				boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h - 3 && mouseY >= top() && mouseY < bottom();
				g.pose().pushMatrix();
				g.pose().translate(off, 0);
				g.fill(x, y, x + w, y + h - 3, Theme.alpha(hover ? 0x40FFFFFF : 0x20FFFFFF, t));
				g.fill(x, y, x + 2, y + h - 3, Theme.alpha(Theme.ACCENT, t));
				float bob = hover ? (float) Math.sin(Theme.time() * 6) * 1.2f : 0f;
				Icons.scaled(g, Icons.stack(e.icon()), x + 6, y + 3 + bob, 1f);
				g.text(this.font, "§l" + e.title(), x + 26, y + 4, Theme.alpha(0xFFFFFFFF, t), false);
				if (!e.sub().isEmpty()) {
					String sub = "§7" + e.sub();
					g.text(this.font, sub, x + w - 6 - this.font.width(sub), y + 4, Theme.alpha(0xFFFFFFFF, t), false);
				}
				int ly = y + 18;
				for (GuideData.Line l : e.lines()) {
					int lx = x + 26;
					if (l.icon() != null) {
						Icons.small(g, Icons.stack(l.icon()), lx, ly);
						lx += 11;
					}
					String text = this.font.plainSubstrByWidth(l.text(), x + w - 6 - lx);
					g.text(this.font, text, lx, ly, Theme.alpha(0xFFFFFFFF, t), false);
					ly += 11;
				}
				g.pose().popMatrix();
			}
			y += h;
		}
		contentH = y - start;
		g.disableScissor();
		if (i == 0) g.centeredText(this.font, "§7Nothing found", this.width / 2, top() + 20, 0xFFFFFFFF);
	}
}
