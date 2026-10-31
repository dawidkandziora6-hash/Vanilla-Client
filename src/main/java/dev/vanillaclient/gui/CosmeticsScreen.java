package dev.vanillaclient.gui;

import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.cosmetics.Cosmetics;
import dev.vanillaclient.cosmetics.Cosmetics.Collection;
import dev.vanillaclient.cosmetics.Cosmetics.Design;
import dev.vanillaclient.cosmetics.Cosmetics.Kind;
import dev.vanillaclient.gui.ui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Wardrobe in store style: Collections (full sets) and single capes, wings and shields.
 * Hover a card to try it on, click to wear it.
 */
public final class CosmeticsScreen extends BaseScreen {
	private static final int COLLECTIONS = -1;
	/** -1 = collections, else Kind ordinal. */
	private static int tab = COLLECTIONS;

	private float yaw = 200f;
	private boolean spinning = true, draggingPlayer;
	private double lastMouseX;
	private long lastFrame = System.nanoTime();
	private Object hovered;
	private final float[] hover = new float[16];
	private long flashAt;
	private Object flashed;

	public CosmeticsScreen(Screen parent) {
		super(Component.literal("Wardrobe"), parent);
	}

	public static CosmeticsScreen onTab(Screen parent, Kind k) {
		tab = k.ordinal();
		return new CosmeticsScreen(parent);
	}

	// ------------------------------------------------------------ layout

	private int previewW() {
		return Math.min(170, this.width / 3);
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
		return 34;
	}

	private int gridTop() {
		return top() + 26;
	}

	private int bottom() {
		return this.height - 32;
	}

	private int cols() {
		return tab == COLLECTIONS ? 1 : 2;
	}

	private int cardW() {
		return (gridW() - (cols() - 1) * 6) / cols();
	}

	private int cardH() {
		if (tab == COLLECTIONS) return Math.max(52, Math.min(74, (bottom() - gridTop() - 12) / 3));
		return Math.max(54, Math.min(90, (bottom() - gridTop() - 8) / 2 - 2));
	}

	private List<?> items() {
		return tab == COLLECTIONS ? Cosmetics.COLLECTIONS : Cosmetics.designs(Kind.values()[tab]);
	}

	// ------------------------------------------------------------ widgets

	@Override
	protected void init() {
		super.init();
		String[] names = {"Collections", "Capes", "Wings", "Shields"};
		ItemStack[] icons = {Icons.stack(Items.NETHER_STAR), Icons.stack(Items.LEATHER), Icons.stack(Items.FEATHER), Icons.stack(Items.SHIELD)};
		int tw = (gridW() - 12) / 4;
		for (int i = 0; i < 4; i++) {
			final int t = i - 1;
			iconAction(gridX() + i * (tw + 4), top(), tw, icons[i], "    " + names[i], () -> {
				tab = t;
				rebuildWidgets();
			}).active = tab != t;
		}
		int px = left(), pw = previewW();
		action(px, bottom() - 22, pw / 2 - 2, spinning ? "⏸ Spin" : "▶ Spin", () -> {
			spinning = !spinning;
			rebuildWidgets();
		});
		action(px + pw / 2 + 2, bottom() - 22, pw / 2 - 2, "↻ Turn", () -> yaw = (yaw + 180f) % 360f);
		int by = this.height - 26;
		action(this.width / 2 - 154, by, 100, "Your images", () -> Platform.open(Config.cosmeticsDir()))
				.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Put cape.png / shield.png here for your own design")));
		action(this.width / 2 - 50, by, 100, "Take all off", () -> {
			for (Kind k : Kind.values()) Cosmetics.select(k, Cosmetics.NONE);
		});
		action(this.width / 2 + 54, by, 100, "✔ Done", this::onClose);
	}

	// ------------------------------------------------------------ helpers

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
		int x = gridX() + (i % cols()) * (cw + 6);
		int y = gridTop() + (i / cols()) * (ch + 6);
		return new int[] {x, y, cw, ch};
	}

	private void setPreview(Object o) {
		Cosmetics.previewCape = Cosmetics.previewWings = Cosmetics.previewShield = null;
		if (o instanceof Collection c) {
			Cosmetics.previewCape = Cosmetics.previewWings = Cosmetics.previewShield = c.id();
		} else if (o instanceof Design d) {
			switch (Kind.values()[tab]) {
				case CAPE -> Cosmetics.previewCape = d.id();
				case WINGS -> Cosmetics.previewWings = d.id();
				case SHIELD -> Cosmetics.previewShield = d.id();
			}
		}
	}

	private boolean worn(Object o) {
		if (o instanceof Collection c) return Cosmetics.wearing(c);
		Design d = (Design) o;
		return d.id().equals(Cosmetics.selected(Kind.values()[tab]));
	}

	// ------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		int i = indexAt(event.x(), event.y());
		if (i >= 0) {
			Object o = items().get(i);
			Ui.click();
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
			return true;
		}
		if (event.x() >= left() && event.x() < left() + previewW() && event.y() >= top() && event.y() < bottom() - 26 && event.button() == Mouse.LEFT) {
			draggingPlayer = true;
			lastMouseX = event.x();
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
	public void onClose() {
		setPreview(null);
		super.onClose();
	}

	@Override
	public void onFilesDrop(java.util.List<java.nio.file.Path> paths) {
		Kind k = tab == Kind.SHIELD.ordinal() ? Kind.SHIELD : Kind.CAPE;
		for (java.nio.file.Path p : paths) {
			if (!p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png")) continue;
			try {
				java.nio.file.Files.createDirectories(Config.cosmeticsDir());
				java.nio.file.Files.copy(p, Config.cosmeticsDir().resolve(k.customFile), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				Cosmetics.reloadCustom();
				Cosmetics.select(k, Cosmetics.CUSTOM);
			} catch (java.io.IOException ignored) {
				// ignore
			}
			return;
		}
	}

	// ------------------------------------------------------------ drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		int x0 = left(), y0 = top(), w = previewW(), h = bottom() - 26 - top();
		// colored spotlight behind the player in the color of what you look at
		int col = 0xFF3A4660;
		Object o = hovered;
		if (o instanceof Collection c) col = c.color();
		else if (o instanceof Design d && !d.id().equals(Cosmetics.NONE)) col = d.color();
		g.fillGradient(x0, y0, x0 + w, y0 + h, Theme.alpha(col, 0.55f), 0xE0101010);
		g.outline(x0, y0, w, h, 0x50FFFFFF);
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
		lastFrame = now;
		if (draggingPlayer) {
			yaw += (float) (mouseX - lastMouseX) * 1.5f;
			lastMouseX = mouseX;
		} else if (spinning) {
			yaw += dt * 30f;
		}
		yaw = ((yaw % 360f) + 360f) % 360f;
		float in = intro();
		g.centeredText(this.font, "§l✦ Wardrobe ✦", this.width / 2, 8, Theme.alpha(Theme.ACCENT, in));
		g.centeredText(this.font, "§7Hover to try on · click to wear · other Vanilla Client players see it too", this.width / 2, 19, 0xFFFFFFFF);

		int idx = indexAt(mouseX, mouseY);
		hovered = idx >= 0 ? items().get(idx) : null;
		setPreview(hovered);
		drawPreview(g);
		List<?> l = items();
		for (int i = 0; i < l.size(); i++) drawCard(g, i, l.get(i), mouseX, mouseY, dt);
		if (tab == COLLECTIONS) {
			g.text(this.font, "§8More collections coming soon", gridX(), bottom() - 8, 0xFFFFFFFF, false);
		}
	}

	private void drawPreview(GuiGraphicsExtractor g) {
		int x0 = left(), y0 = top(), x1 = x0 + previewW(), y1 = bottom() - 26;
		var player = this.minecraft.player;
		if (player == null) {
			g.centeredText(this.font, "§7Join a world to see", (x0 + x1) / 2, (y0 + y1) / 2 - 8, 0xFFFFFFFF);
			g.centeredText(this.font, "§7yourself here", (x0 + x1) / 2, (y0 + y1) / 2 + 4, 0xFFFFFFFF);
			return;
		}
		int size = Math.max(20, Math.min((y1 - y0) / 3, 56));
		try {
			PlayerPreview.draw(g, x0 + 2, y0 + 14, x1 - 2, y1 - 12, size, yaw, -0.1f, player, PlayerPreview.Gear.WINGS);
		} catch (RuntimeException e) {
			g.centeredText(this.font, "§7Preview unavailable", (x0 + x1) / 2, (y0 + y1) / 2, 0xFFFFFFFF);
		}
		// shield floating next to you
		String shield = Cosmetics.effectiveId(Kind.SHIELD);
		if (shield != null && !shield.equals(Cosmetics.NONE)) {
			float bob = (float) Math.sin(Theme.time() * 2.0) * 2f;
			Icons.scaled(g, Icons.stack(Items.SHIELD), x1 - 34, y0 + 6 + bob, 1.8f);
		}
		g.centeredText(this.font, "§8drag to turn", (x0 + x1) / 2, y1 - 10, 0xFFFFFFFF);
	}

	private void drawCard(GuiGraphicsExtractor g, int i, Object o, int mx, int my, float dt) {
		int[] r = cardRect(i);
		int x = r[0], w = r[2], h = r[3];
		boolean hov = o == hovered;
		hover[i] += ((hov ? 1f : 0f) - hover[i]) * Math.min(1f, dt * 14f);
		long nowMs = System.currentTimeMillis();
		float appear = Theme.easeOut((nowMs - openedAt - i * 60L) / 300f);
		int y = r[1] + Math.round((1f - appear) * 12f) - Math.round(hover[i] * 2f);
		int color = o instanceof Collection c ? c.color() : ((Design) o).color();
		boolean worn = worn(o);
		// card: colored gradient like a store tile
		g.fillGradient(x, y, x + w, y + h, Theme.alpha(Theme.lerpColor(color, 0xFF202020, 0.45f - hover[i] * 0.2f), appear),
				Theme.alpha(0xF0101010, appear));
		g.outline(x, y, w, h, Theme.alpha(worn ? Theme.ACCENT : Theme.lerpColor(0x40FFFFFF, 0xFFFFFFFF, hover[i]), appear));
		if (o instanceof Collection c) {
			int th = h - 8;
			thumb(g, Kind.CAPE, c.id(), x + 6, y + 4, th * 20 / 32, th);
			int ww = Math.min(w / 3, th * 104 / 40 + 10);
			thumb(g, Kind.WINGS, c.id(), x + 12 + th * 20 / 32, y + 4, ww, th);
			thumb(g, Kind.SHIELD, c.id(), x + 18 + th * 20 / 32 + ww, y + 4, th * 24 / 44, th);
			int tx = x + 26 + th * 20 / 32 + ww + th * 24 / 44;
			g.text(this.font, "§l" + c.name(), tx, y + 8, Theme.alpha(c.accent(), appear), true);
			g.text(this.font, "§7" + this.font.plainSubstrByWidth(c.desc(), x + w - tx - 4), tx, y + 20, Theme.alpha(0xFFFFFFFF, appear), false);
			g.text(this.font, "§8Cape · Wings · Shield", tx, y + 31, Theme.alpha(0xFFFFFFFF, appear), false);
			String btn = worn ? "§a✔ Wearing" : hov ? "§e» Wear set" : "§7Wear set";
			g.text(this.font, btn, tx, y + h - 13, Theme.alpha(0xFFFFFFFF, appear), false);
		} else {
			Design d = (Design) o;
			Kind k = Kind.values()[tab];
			int nameH = 22;
			if (d.id().equals(Cosmetics.NONE)) {
				g.centeredText(this.font, "§8✖", x + w / 2, y + (h - nameH) / 2 - 4, 0xFFFFFFFF);
			} else {
				int th = h - nameH - 6;
				int tw = k == Kind.WINGS ? Math.min(w - 12, th * 104 / 40) : k == Kind.CAPE ? th * 20 / 32 : th * 24 / 44;
				float scale = 1f + hover[i] * 0.06f;
				g.pose().pushMatrix();
				g.pose().translate(x + w / 2f, y + 4 + th / 2f);
				g.pose().scale(scale, scale);
				thumb(g, k, d.id(), -tw / 2, -th / 2, tw, th);
				g.pose().popMatrix();
			}
			g.text(this.font, this.font.plainSubstrByWidth(d.name(), w - 8), x + 5, y + h - nameH + 2, Theme.alpha(worn ? Theme.ACCENT : 0xFFFFFFFF, appear), true);
			g.text(this.font, "§7" + this.font.plainSubstrByWidth(d.desc(), w - 8), x + 5, y + h - nameH + 12, Theme.alpha(0xFFFFFFFF, appear), false);
		}
		if (worn) {
			g.fill(x + w - 12, y + 3, x + w - 3, y + 12, 0xFF2A8A3A);
			g.text(this.font, "✔", x + w - 11, y + 4, 0xFFFFFFFF, false);
		}
		if (o == flashed && System.currentTimeMillis() - flashAt < 450) {
			g.fill(x, y, x + w, y + h, Theme.alpha(0xFFFFFFFF, 0.35f * (1f - (System.currentTimeMillis() - flashAt) / 450f)));
		}
	}

	/** Visible face of a design, scaled into the box (animated designs animate here too). */
	private void thumb(GuiGraphicsExtractor g, Kind k, String id, int x, int y, int w, int h) {
		Cosmetics.Tex t = Cosmetics.texture(k, k == Kind.WINGS ? id + "_preview" : id);
		if (k == Kind.WINGS) Cosmetics.texture(k, id); // keep the animation of the real texture alive
		if (t == null) return;
		float u, v, rw, rh;
		float s = t.width() / 128f;
		switch (k) {
			case CAPE -> { u = 2; v = 2; rw = 20; rh = 32; }
			case SHIELD -> { u = 2; v = 2; rw = 24; rh = 44; }
			default -> { u = 0; v = 0; rw = t.width(); rh = t.height(); s = 1f; }
		}
		float scale = Math.min(w / rw, h / rh);
		int dw = Math.round(rw * scale), dh = Math.round(rh * scale);
		int dx = x + (w - dw) / 2, dy = y + (h - dh) / 2;
		g.blit(RenderPipelines.GUI_TEXTURED, t.id(), dx, dy, u * s, v * s, dw, dh, Math.round(rw * s), Math.round(rh * s), t.width(), t.height());
	}
}
