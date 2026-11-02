package dev.vanillaclient.gui;

import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.cosmetics.Cosmetics;
import dev.vanillaclient.cosmetics.Cosmetics.Collection;
import dev.vanillaclient.cosmetics.Cosmetics.Design;
import dev.vanillaclient.cosmetics.Cosmetics.Kind;
import dev.vanillaclient.features.Shop;
import dev.vanillaclient.gui.ui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Wardrobe & shop: sets and single capes, wings and shields. Hover to try on,
 * click to wear what you own – or buy it with coins.
 */
public final class CosmeticsScreen extends BaseScreen {
	private static final int SETS = -1;
	private static int tab = SETS;
	private static final String[] TABS = {"Sets", "Capes", "Wings", "Shields"};

	private float yaw = 200f, tabX = -1, tabW;
	private boolean spinning = true, draggingPlayer;
	private double lastMouseX;
	private long lastFrame = System.nanoTime();
	private Object hovered;
	private final float[] hover = new float[16];
	private long flashAt;
	private Object flashed;
	/** Purchase window: the thing to buy, or null. */
	private Object buying;
	private long buyingAt;
	private String status = "";
	private long statusAt;
	private float coinsShown = -1;

	public CosmeticsScreen(Screen parent) {
		super(Component.literal("Wardrobe"), parent);
	}

	public static CosmeticsScreen onTab(Screen parent, Kind k) {
		tab = k.ordinal();
		return new CosmeticsScreen(parent);
	}

	// ------------------------------------------------------------ layout

	private int previewW() {
		return Math.min(160, this.width / 3);
	}

	private int left() {
		return Math.max(8, this.width / 2 - (previewW() + 10 + gridW()) / 2);
	}

	private int gridW() {
		return Math.min(420, this.width - 26 - previewW());
	}

	private int gridX() {
		return left() + previewW() + 10;
	}

	private int top() {
		return 30;
	}

	private int gridTop() {
		return top() + 22 + (tab == Kind.WINGS.ordinal() ? 16 : 0);
	}

	private int bottom() {
		return this.height - 30;
	}

	private int cols() {
		return tab == SETS ? 1 : items().size() > 4 ? 3 : 2;
	}

	private int cardW() {
		return (gridW() - (cols() - 1) * 6) / cols();
	}

	private int cardH() {
		int avail = bottom() - gridTop() - 4;
		if (tab == SETS) {
			int n = Cosmetics.COLLECTIONS.size();
			return Math.max(30, Math.min(70, (avail - (n - 1) * 6) / n));
		}
		return Math.max(44, Math.min(88, (avail - 6) / 2));
	}

	private List<?> items() {
		return tab == SETS ? Cosmetics.COLLECTIONS : Cosmetics.designs(Kind.values()[tab]);
	}

	private int[] tabRect(int i) {
		int w = (gridW() - 6) / 4;
		return new int[] {gridX() + i * (w + 2), top(), w, 16};
	}

	// ------------------------------------------------------------ widgets

	@Override
	protected void init() {
		super.init();
		int by = this.height - 24;
		action(this.width / 2 - 154, by, 100, "Your images", () -> Platform.open(Config.cosmeticsDir()))
				.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Put cape.png / shield.png here for your own design")));
		action(this.width / 2 - 50, by, 100, "Take all off", () -> {
			for (Kind k : Kind.values()) Cosmetics.select(k, Cosmetics.NONE);
		});
		action(this.width / 2 + 54, by, 100, "✔ Done", this::onClose);
	}

	// ------------------------------------------------------------ logic

	private int indexAt(double mx, double my) {
		List<?> l = items();
		for (int i = 0; i < l.size(); i++) {
			int[] r = cardRect(i);
			if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) return i;
		}
		return -1;
	}

	private int[] cardRect(int i) {
		int cw = cardW(), ch = cardH();
		return new int[] {gridX() + (i % cols()) * (cw + 6), gridTop() + (i / cols()) * (ch + 6), cw, ch};
	}

	private void setPreview(Object o) {
		Cosmetics.previewCape = Cosmetics.previewWings = Cosmetics.previewShield = null;
		if (o instanceof Collection c) {
			Cosmetics.previewCape = Cosmetics.previewWings = Cosmetics.previewShield = c.id();
		} else if (o instanceof Design d && tab >= 0) {
			switch (Kind.values()[tab]) {
				case CAPE -> Cosmetics.previewCape = d.id();
				case WINGS -> Cosmetics.previewWings = d.id();
				case SHIELD -> Cosmetics.previewShield = d.id();
			}
		}
	}

	private boolean worn(Object o) {
		if (o instanceof Collection c) return Cosmetics.wearing(c) && Shop.ownsSet(c);
		Design d = (Design) o;
		Kind k = Kind.values()[tab];
		return d.id().equals(Cosmetics.selected(k)) && !d.id().equals(Cosmetics.NONE) && Shop.owns(k, d.id());
	}

	private boolean owned(Object o) {
		if (o instanceof Collection c) return Shop.ownsSet(c);
		return Shop.owns(Kind.values()[tab], ((Design) o).id());
	}

	private int price(Object o) {
		return o instanceof Collection c ? Shop.setPrice(c) : Shop.price(Kind.values()[tab]);
	}

	private String name(Object o) {
		return o instanceof Collection c ? c.name() + " Set" : ((Design) o).name();
	}

	private void say(String s) {
		status = s;
		statusAt = System.currentTimeMillis();
	}

	private void use(Object o) {
		if (o instanceof Collection c) {
			if (Cosmetics.wearing(c)) for (Kind k : Kind.values()) Cosmetics.select(k, Cosmetics.NONE);
			else Cosmetics.equip(c);
		} else {
			Design d = (Design) o;
			Kind k = Kind.values()[tab];
			Cosmetics.select(k, d.id().equals(Cosmetics.selected(k)) && !d.id().equals(Cosmetics.NONE) ? Cosmetics.NONE : d.id());
		}
		flashed = o;
		flashAt = System.currentTimeMillis();
	}

	private void confirmBuy() {
		Object o = buying;
		buying = null;
		String err = o instanceof Collection c ? Shop.buySet(c) : Shop.buy(Kind.values()[tab], ((Design) o).id());
		if (err != null) {
			say("§c" + err);
			return;
		}
		say("§a✔ Bought " + name(o) + "!");
		use(o);
	}

	// ------------------------------------------------------------ input

	private int[] modalRect() {
		int w = 200, h = 96;
		return new int[] {this.width / 2 - w / 2, this.height / 2 - h / 2, w, h};
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		if (buying != null) {
			int[] m = modalRect();
			int by = m[1] + m[3] - 24;
			if (my >= by && my < by + 18) {
				if (mx >= m[0] + 10 && mx < m[0] + m[2] / 2 - 4) {
					Ui.click();
					confirmBuy();
					return true;
				}
				if (mx >= m[0] + m[2] / 2 + 4 && mx < m[0] + m[2] - 10) {
					Ui.click();
					buying = null;
					return true;
				}
			}
			if (mx < m[0] || mx >= m[0] + m[2] || my < m[1] || my >= m[1] + m[3]) buying = null;
			return true;
		}
		if (super.mouseClicked(event, doubleClick)) return true;
		for (int i = 0; i < 4; i++) {
			int[] r = tabRect(i);
			if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
				Ui.click();
				tab = i - 1;
				rebuildWidgets();
				return true;
			}
		}
		if (tab == Kind.WINGS.ordinal() && my >= top() + 20 && my < top() + 32) {
			Config.Cosmetics c = Config.get().cosmetics;
			if (mx >= gridX() && mx < gridX() + gridW() / 2) c.wingsOnlyWithElytra = !c.wingsOnlyWithElytra;
			else if (mx >= gridX() + gridW() / 2 && mx < gridX() + gridW()) c.wingsReplaceElytra = !c.wingsReplaceElytra;
			Config.save();
			Ui.click();
			return true;
		}
		int i = indexAt(mx, my);
		if (i >= 0) {
			Object o = items().get(i);
			Ui.click();
			if (owned(o)) {
				use(o);
			} else {
				buying = o;
				buyingAt = System.currentTimeMillis();
			}
			return true;
		}
		if (mx >= left() && mx < left() + previewW() && my >= top() && my < bottom() && event.button() == Mouse.LEFT) {
			draggingPlayer = true;
			lastMouseX = mx;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		draggingPlayer = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (buying != null) {
			if (event.isConfirmation()) {
				confirmBuy();
				return true;
			}
			if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
				buying = null;
				return true;
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		setPreview(null);
		super.onClose();
	}

	// ------------------------------------------------------------ drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int x0 = left(), y0 = top(), w = previewW(), h = bottom() - top();
		int col = 0xFF3A4660;
		Object o = hovered != null ? hovered : null;
		if (o instanceof Collection c) col = c.color();
		else if (o instanceof Design d && !d.id().equals(Cosmetics.NONE)) col = d.color();
		g.fillGradient(x0, y0, x0 + w, y0 + h, Theme.alpha(col, 0.45f), 0xF00C0C10);
		g.outline(x0, y0, w, h, 0x30FFFFFF);
		// floor shadow under the player
		int cx = x0 + w / 2, fy = y0 + h - 30;
		for (int k = 0; k < 6; k++) g.fill(cx - 30 + k * 4, fy + k, cx + 30 - k * 4, fy + k + 1, 0x28000000);
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		if (draggingPlayer) {
			yaw += (float) (mouseX - lastMouseX) * 1.5f;
			lastMouseX = mouseX;
		} else if (spinning && buying == null) {
			yaw += dt * 25f;
		}
		yaw = ((yaw % 360f) + 360f) % 360f;
		float in = intro();

		// header: title left, coins right
		g.text(this.font, "§l✦ WARDROBE", left(), 10, Theme.alpha(Theme.ACCENT, in), true);
		g.text(this.font, "§8try on · wear · buy", left() + this.font.width("✦ WARDROBE") + 14, 10, 0xFFFFFFFF, false);
		long coins = Shop.wallet().coins;
		if (coinsShown < 0) coinsShown = coins;
		coinsShown += (coins - coinsShown) * Math.min(1f, dt * 6f);
		String cs = Shop.format(Math.round(coinsShown));
		int pw = this.font.width(cs) + 26;
		int px = gridX() + gridW() - pw;
		g.fill(px, 6, px + pw, 20, 0xC0201A08);
		g.outline(px, 6, pw, 14, 0x80F2C440);
		coin(g, px + 5, 9);
		g.text(this.font, cs, px + 17, 9, 0xFFFFD34D, true);

		drawTabs(g, mouseX, mouseY, dt);
		if (tab == Kind.WINGS.ordinal()) drawWingToggles(g, mouseX, mouseY);

		int idx = buying == null ? indexAt(mouseX, mouseY) : -1;
		hovered = buying != null ? buying : idx >= 0 ? items().get(idx) : null;
		setPreview(hovered);
		drawPreview(g);
		List<?> l = items();
		for (int i = 0; i < l.size(); i++) drawCard(g, i, l.get(i), dt);
		if (tab == SETS) {
			int[] last = cardRect(l.size() - 1);
			if (last[1] + last[3] + 12 < bottom()) g.text(this.font, "§8More sets coming soon …", gridX() + 2, last[1] + last[3] + 5, 0xFFFFFFFF, false);
		}
		if (System.currentTimeMillis() - statusAt < 3000) g.centeredText(this.font, status, gridX() + gridW() / 2, bottom() - 9, 0xFFFFFFFF);
		if (buying != null) drawModal(g, mouseX, mouseY);
	}

	private void drawTabs(GuiGraphicsExtractor g, int mx, int my, float dt) {
		int[] cur = tabRect(tab + 1);
		if (tabX < 0) {
			tabX = cur[0];
			tabW = cur[2];
		}
		tabX += (cur[0] - tabX) * Math.min(1f, dt * 14f);
		tabW += (cur[2] - tabW) * Math.min(1f, dt * 14f);
		g.fill(gridX(), top(), gridX() + gridW(), top() + 16, 0x60000000);
		g.fill(Math.round(tabX), top(), Math.round(tabX + tabW), top() + 16, 0x30FFFFFF);
		g.fill(Math.round(tabX), top() + 15, Math.round(tabX + tabW), top() + 16, Theme.ACCENT);
		for (int i = 0; i < 4; i++) {
			int[] r = tabRect(i);
			boolean sel = i - 1 == tab;
			boolean hov = mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
			int c = sel ? 0xFFFFFFFF : hov ? 0xFFE0E0E0 : 0xFF909090;
			g.centeredText(this.font, TABS[i], r[0] + r[2] / 2, r[1] + 4, c);
		}
	}

	private void drawWingToggles(GuiGraphicsExtractor g, int mx, int my) {
		Config.Cosmetics c = Config.get().cosmetics;
		int y = top() + 20, half = gridW() / 2;
		pill(g, gridX(), y, half - 3, "Only with elytra", c.wingsOnlyWithElytra, mx, my);
		pill(g, gridX() + half + 3, y, half - 3, "Replace elytra look", c.wingsReplaceElytra, mx, my);
	}

	private void pill(GuiGraphicsExtractor g, int x, int y, int w, String text, boolean on, int mx, int my) {
		boolean hov = mx >= x && mx < x + w && my >= y && my < y + 12;
		g.fill(x, y, x + w, y + 12, hov ? 0x50FFFFFF : 0x30000000);
		g.fill(x + 3, y + 3, x + 9, y + 9, on ? 0xFF5AD06A : 0xFF4A4A4A);
		g.text(this.font, (on ? "§f" : "§7") + this.font.plainSubstrByWidth(text, w - 16), x + 13, y + 2, 0xFFFFFFFF, false);
	}

	private void drawPreview(GuiGraphicsExtractor g) {
		int x0 = left(), y0 = top(), x1 = x0 + previewW(), y1 = bottom();
		var player = this.minecraft.player;
		Object o = hovered;
		String title = o == null ? "You" : name(o);
		g.centeredText(this.font, "§f" + this.font.plainSubstrByWidth(title, previewW() - 8), (x0 + x1) / 2, y0 + 5, 0xFFFFFFFF);
		if (player == null) {
			g.centeredText(this.font, "§7Join a world to see", (x0 + x1) / 2, (y0 + y1) / 2 - 8, 0xFFFFFFFF);
			g.centeredText(this.font, "§7yourself here", (x0 + x1) / 2, (y0 + y1) / 2 + 4, 0xFFFFFFFF);
			return;
		}
		int size = Math.max(20, Math.min((y1 - y0) / 3, 54));
		try {
			PlayerPreview.draw(g, x0 + 2, y0 + 16, x1 - 2, y1 - 14, size, yaw, -0.1f, player, PlayerPreview.Gear.WINGS);
		} catch (RuntimeException e) {
			g.centeredText(this.font, "§7Preview unavailable", (x0 + x1) / 2, (y0 + y1) / 2, 0xFFFFFFFF);
		}
		String shield = Cosmetics.effectiveId(Kind.SHIELD);
		if (shield != null && !shield.equals(Cosmetics.NONE)) {
			float bob = (float) Math.sin(Theme.time() * 2.0) * 2f;
			Icons.scaled(g, Icons.stack(Items.SHIELD), x1 - 36, y0 + 18 + bob, 1.8f);
		}
		g.centeredText(this.font, "§8drag to turn", (x0 + x1) / 2, y1 - 11, 0xFFFFFFFF);
	}

	private void badge(GuiGraphicsExtractor g, Object o, int x, int y, int w, int h) {
		String text;
		int bg;
		if (worn(o)) {
			text = "§fEQUIPPED";
			bg = 0xE02A8A3A;
		} else if (owned(o)) {
			text = "§7OWNED";
			bg = 0xA0303030;
		} else {
			text = "§e" + Shop.format(price(o));
			bg = 0xD0201A08;
		}
		boolean coin = !owned(o);
		int tw = this.font.width(text) + 6 + (coin ? 10 : 0);
		int bx = x + w - tw - 3, by = y + h - 13;
		g.fill(bx, by, bx + tw, by + 11, bg);
		if (coin) coin(g, bx + 2, by + 1);
		g.text(this.font, text, bx + 3 + (coin ? 10 : 0), by + 2, 0xFFFFFFFF, false);
	}

	private void drawCard(GuiGraphicsExtractor g, int i, Object o, float dt) {
		int[] r = cardRect(i);
		int x = r[0], w = r[2], h = r[3];
		boolean hov = o == hovered && buying == null;
		hover[i] += ((hov ? 1f : 0f) - hover[i]) * Math.min(1f, dt * 14f);
		float appear = Theme.easeOut((System.currentTimeMillis() - openedAt - i * 50L) / 280f);
		int y = r[1] + Math.round((1f - appear) * 10f) - Math.round(hover[i] * 2f);
		int color = o instanceof Collection c ? c.color() : ((Design) o).color();
		boolean none = o instanceof Design d && d.id().equals(Cosmetics.NONE);
		// clean dark card with a soft colored glow at the top
		g.fill(x, y, x + w, y + h, Theme.alpha(0xE0141418, appear));
		if (!none) g.fillGradient(x, y, x + w, y + h * 2 / 3, Theme.alpha(color, (0.28f + hover[i] * 0.2f) * appear), 0x00000000);
		g.fill(x, y, x + w, y + 1, Theme.alpha(none ? 0xFF404040 : color, appear));
		g.outline(x, y, w, h, Theme.alpha(worn(o) ? 0xFF5AD06A : Theme.lerpColor(0x22FFFFFF, 0x90FFFFFF, hover[i]), appear));
		if (o instanceof Collection c) {
			int th = h - 8;
			thumb(g, Kind.CAPE, c.id(), x + 6, y + 4, th * 20 / 32, th);
			int ww = Math.min(w / 3, th * 104 / 40);
			thumb(g, Kind.WINGS, c.id(), x + 10 + th * 20 / 32, y + 4, ww, th);
			thumb(g, Kind.SHIELD, c.id(), x + 14 + th * 20 / 32 + ww, y + 4 + th / 6, th * 2 / 3, th * 2 / 3);
			int tx = x + 22 + th * 20 / 32 + ww + th * 2 / 3;
			g.text(this.font, "§l" + c.name(), tx, y + 5, Theme.alpha(c.accent(), appear), true);
			if (h >= 46) g.text(this.font, "§7" + this.font.plainSubstrByWidth(c.desc(), x + w - tx - 4), tx, y + 17, Theme.alpha(0xFFFFFFFF, appear), false);
			if (h >= 60 && !Shop.ownsSet(c)) g.text(this.font, "§8Set price −20%", tx, y + 29, Theme.alpha(0xFFFFFFFF, appear), false);
		} else {
			Design d = (Design) o;
			Kind k = Kind.values()[tab];
			int nameH = 24;
			if (none) {
				g.centeredText(this.font, "§8✖", x + w / 2, y + (h - nameH) / 2 - 4, 0xFFFFFFFF);
			} else {
				int th = h - nameH - 4;
				int tw = k == Kind.WINGS ? Math.min(w - 12, th * 104 / 40) : k == Kind.CAPE ? th * 20 / 32 : th;
				float scale = 1f + hover[i] * 0.06f;
				g.pose().pushMatrix();
				g.pose().translate(x + w / 2f, y + 3 + th / 2f);
				g.pose().scale(scale, scale);
				thumb(g, k, d.id(), -tw / 2, -th / 2, tw, th);
				g.pose().popMatrix();
			}
			g.text(this.font, this.font.plainSubstrByWidth(d.name(), w - 8), x + 5, y + h - nameH + 1, Theme.alpha(0xFFFFFFFF, appear), true);
			if (none || d.id().equals(Cosmetics.CUSTOM)) {
				g.text(this.font, "§7" + this.font.plainSubstrByWidth(d.desc(), w - 8), x + 5, y + h - 11, Theme.alpha(0xFFFFFFFF, appear), false);
			}
		}
		if (!none && !(o instanceof Design dd && dd.id().equals(Cosmetics.CUSTOM))) badge(g, o, x, y, w, h);
		if (o == flashed && System.currentTimeMillis() - flashAt < 450) {
			g.fill(x, y, x + w, y + h, Theme.alpha(0xFFFFFFFF, 0.3f * (1f - (System.currentTimeMillis() - flashAt) / 450f)));
		}
	}

	private void drawModal(GuiGraphicsExtractor g, int mx, int my) {
		float t = Theme.easeOut((System.currentTimeMillis() - buyingAt) / 180f);
		g.fill(0, 0, this.width, this.height, Theme.alpha(0xA0000000, t));
		int[] m = modalRect();
		int x = m[0], y = m[1] + Math.round((1f - t) * 10), w = m[2], h = m[3];
		int color = buying instanceof Collection c ? c.color() : ((Design) buying).color();
		g.fill(x, y, x + w, y + h, 0xF0141418);
		g.fillGradient(x, y, x + w, y + 40, Theme.alpha(color, 0.35f), 0x00000000);
		g.outline(x, y, w, h, Theme.alpha(color, 1f));
		g.centeredText(this.font, "§lBuy " + name(buying) + "?", x + w / 2, y + 10, 0xFFFFFFFF);
		long have = Shop.wallet().coins;
		int price = price(buying);
		boolean ok = have >= price;
		coin(g, x + w / 2 - 28, y + 28);
		g.text(this.font, "§e" + Shop.format(price), x + w / 2 - 16, y + 28, 0xFFFFFFFF, true);
		g.centeredText(this.font, ok ? "§7After: " + Shop.format(have - price) + " coins" : "§cYou have " + Shop.format(have) + " coins", x + w / 2, y + 44, 0xFFFFFFFF);
		int by = y + h - 24;
		boolean hb = my >= by && my < by + 18 && mx >= x + 10 && mx < x + w / 2 - 4;
		boolean hc = my >= by && my < by + 18 && mx >= x + w / 2 + 4 && mx < x + w - 10;
		g.fill(x + 10, by, x + w / 2 - 4, by + 18, ok ? (hb ? 0xFF4CC85C : 0xFF3A9A48) : 0xFF444444);
		g.centeredText(this.font, ok ? "§lBuy" : "§7Buy", x + 10 + (w / 2 - 14) / 2, by + 5, 0xFFFFFFFF);
		g.fill(x + w / 2 + 4, by, x + w - 10, by + 18, hc ? 0xFF555555 : 0xFF3A3A3A);
		g.centeredText(this.font, "Cancel", x + w / 2 + 4 + (w / 2 - 14) / 2, by + 5, 0xFFFFFFFF);
	}

	/** A small gold coin. */
	static void coin(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x + 2, y, x + 6, y + 8, 0xFFB8860B);
		g.fill(x, y + 2, x + 8, y + 6, 0xFFB8860B);
		g.fill(x + 1, y + 1, x + 7, y + 7, 0xFFB8860B);
		g.fill(x + 2, y + 1, x + 6, y + 7, 0xFFFFD34D);
		g.fill(x + 1, y + 2, x + 7, y + 6, 0xFFFFD34D);
		g.fill(x + 3, y + 2, x + 5, y + 6, 0xFFE0A520);
		g.fill(x + 2, y + 2, x + 3, y + 3, 0xFFFFF4B0);
	}

	/** Visible face of a design, scaled into the box (animated designs animate here too). */
	private void thumb(GuiGraphicsExtractor g, Kind k, String id, int x, int y, int w, int h) {
		Cosmetics.Tex t = Cosmetics.texture(k, k == Kind.WINGS ? id + "_preview" : id);
		if (k == Kind.WINGS) Cosmetics.texture(k, id);
		if (t == null) return;
		float u, v, rw, rh;
		float s = t.width() / 128f;
		boolean round = k == Kind.SHIELD && !id.equals(Cosmetics.CUSTOM);
		switch (k) {
			case CAPE -> { u = 2; v = 2; rw = 20; rh = 32; }
			case SHIELD -> {
				u = 2;
				v = 2;
				rw = round ? 36 : 24;
				rh = round ? 36 : 44;
			}
			default -> { u = 0; v = 0; rw = t.width(); rh = t.height(); s = 1f; }
		}
		float scale = Math.min(w / rw, h / rh);
		int dw = Math.round(rw * scale), dh = Math.round(rh * scale);
		int dx = x + (w - dw) / 2, dy = y + (h - dh) / 2;
		g.blit(RenderPipelines.GUI_TEXTURED, t.id(), dx, dy, u * s, v * s, dw, dh, Math.round(rw * s), Math.round(rh * s), t.width(), t.height());
	}

	@SuppressWarnings("unused")
	private static ItemStack unused() {
		return ItemStack.EMPTY;
	}
}
