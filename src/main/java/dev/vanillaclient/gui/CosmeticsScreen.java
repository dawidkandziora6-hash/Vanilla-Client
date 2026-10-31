package dev.vanillaclient.gui;

import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.cosmetics.Cosmetics;
import dev.vanillaclient.cosmetics.Cosmetics.Design;
import dev.vanillaclient.cosmetics.Cosmetics.Kind;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Wardrobe: live player preview on the left (spins, drag to turn), design cards
 * on the right. Hovering a card shows it on your player right away.
 */
public final class CosmeticsScreen extends BaseScreen {
	private static Kind kind = Kind.CAPE;
	private static int page;

	private static final int CARD_W = 42, CARD_H = 66, CARD_GAP = 4;

	private float yaw = 160f;
	private boolean spinning = true;
	private boolean draggingPlayer;
	private double lastMouseX;
	private long lastFrame = System.nanoTime();
	private String hovered;
	private String flashId;
	private long flashAt;
	private final String totemAtOpen = Cosmetics.selected(Kind.TOTEM);
	private boolean totemDirty;
	private String status = "";
	private long statusAt;

	/** Opens the wardrobe on a given tab. */
	public static CosmeticsScreen onTab(Screen parent, Kind tab) {
		kind = tab;
		return new CosmeticsScreen(parent);
	}

	public CosmeticsScreen(Screen parent) {
		super(Component.literal("Wardrobe"), parent);
	}

	// ------------------------------------------------------------ layout

	private int previewW() {
		return Math.min(150, this.width / 3);
	}

	private int left() {
		return Math.max(8, this.width / 2 - (previewW() + 8 + gridW()) / 2);
	}

	private int gridX() {
		return left() + previewW() + 8;
	}

	private int gridW() {
		int avail = this.width - 16 - previewW() - 8;
		int cols = Math.max(2, Math.min(6, (avail + CARD_GAP) / (CARD_W + CARD_GAP)));
		return cols * (CARD_W + CARD_GAP) - CARD_GAP;
	}

	private int cols() {
		return (gridW() + CARD_GAP) / (CARD_W + CARD_GAP);
	}

	private int top() {
		return 32;
	}

	private int gridTop() {
		return top() + 26;
	}

	private int bottom() {
		return this.height - 34;
	}

	private int rowsFit() {
		return Math.max(1, (bottom() - 26 - gridTop() + CARD_GAP) / (CARD_H + CARD_GAP));
	}

	private int perPage() {
		return cols() * rowsFit();
	}

	// ------------------------------------------------------------ widgets

	@Override
	protected void init() {
		super.init();
		int x = gridX();
		int tw = (gridW() - 12) / 4;
		ItemStack[] icons = {Icons.stack(Items.LEATHER), Icons.stack(Items.ELYTRA), Icons.stack(Items.SHIELD), Icons.stack(Items.TOTEM_OF_UNDYING)};
		for (Kind k : Kind.values()) {
			Button b = iconAction(x + k.ordinal() * (tw + 4), top(), tw, icons[k.ordinal()], "    " + k.title, () -> {
				kind = k;
				page = 0;
				yaw = k == Kind.SHIELD ? 200f : 160f;
				rebuildWidgets();
			});
			b.active = k != kind;
		}
		List<Design> list = Cosmetics.designs(kind);
		int pages = Math.max(1, (list.size() + perPage() - 1) / perPage());
		page = Math.max(0, Math.min(page, pages - 1));
		if (pages > 1) {
			action(x, bottom() - 22, 20, "◀", () -> {
				page--;
				rebuildWidgets();
			}).active = page > 0;
			action(x + gridW() - 20, bottom() - 22, 20, "▶", () -> {
				page++;
				rebuildWidgets();
			}).active = page < pages - 1;
		}

		int px = left();
		int pw = previewW();
		action(px, bottom() - 22, pw / 2 - 2, spinning ? "⏸ Spin" : "▶ Spin", () -> {
			spinning = !spinning;
			rebuildWidgets();
		});
		action(px + pw / 2 + 2, bottom() - 22, pw / 2 - 2, "↻ Front/Back", () -> yaw = (yaw + 180f) % 360f);

		int by = this.height - 26;
		action(this.width / 2 - 154, by, 100, "Open folder", () -> Platform.open(Config.cosmeticsDir()));
		action(this.width / 2 - 50, by, 100, "↻ Reload own", Cosmetics::reloadCustom);
		action(this.width / 2 + 54, by, 100, "✔ Done", this::onClose);
	}

	// ------------------------------------------------------------ input

	private int cardIndexAt(double mx, double my) {
		int cols = cols();
		for (int i = 0; i < perPage(); i++) {
			int cx = gridX() + (i % cols) * (CARD_W + CARD_GAP);
			int cy = gridTop() + (i / cols) * (CARD_H + CARD_GAP);
			if (mx >= cx && mx < cx + CARD_W && my >= cy && my < cy + CARD_H) {
				int idx = page * perPage() + i;
				return idx < Cosmetics.designs(kind).size() ? idx : -1;
			}
		}
		return -1;
	}

	private boolean overPreview(double mx, double my) {
		return mx >= left() && mx < left() + previewW() && my >= top() && my < bottom() - 26;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		int idx = cardIndexAt(event.x(), event.y());
		if (idx >= 0) {
			Design d = Cosmetics.designs(kind).get(idx);
			Cosmetics.select(kind, d.id());
			flashId = d.id();
			flashAt = System.currentTimeMillis();
			return true;
		}
		if (overPreview(event.x(), event.y()) && event.button() == Mouse.LEFT) {
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
		Cosmetics.previewCape = null;
		Cosmetics.previewWings = null;
		Cosmetics.previewShield = null;
		String totem = Cosmetics.selected(Kind.TOTEM);
		super.onClose();
		if (totemDirty || !java.util.Objects.equals(totem, totemAtOpen)) {
			// swaps the totem item texture (short resource reload)
			TexturePackApply.run(totem);
		}
	}

	/** Drop a PNG on the wardrobe to use it as your own design for the open tab. */
	@Override
	public void onFilesDrop(java.util.List<java.nio.file.Path> paths) {
		for (java.nio.file.Path p : paths) {
			String n = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
			if (!n.endsWith(".png")) continue;
			try {
				java.nio.file.Files.createDirectories(Config.cosmeticsDir());
				java.nio.file.Files.copy(p, Config.cosmeticsDir().resolve(kind.customFile), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				Cosmetics.reloadCustom();
				Cosmetics.select(kind, Cosmetics.CUSTOM);
				if (kind == Kind.TOTEM) totemDirty = true;
				flashId = Cosmetics.CUSTOM;
				flashAt = System.currentTimeMillis();
				status = "✔ Your image is now your " + kind.title.toLowerCase(java.util.Locale.ROOT).replaceAll("s$", "");
			} catch (java.io.IOException e) {
				status = "§cCould not copy the file";
			}
			statusAt = System.currentTimeMillis();
			return;
		}
		status = "§cPlease drop a PNG file";
		statusAt = System.currentTimeMillis();
	}

	// ------------------------------------------------------------ drawing

	private void setPreview(String id) {
		Cosmetics.previewCape = kind == Kind.CAPE ? id : null;
		Cosmetics.previewWings = kind == Kind.WINGS ? id : null;
		Cosmetics.previewShield = kind == Kind.SHIELD ? id : null;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		Theme.panel(g, left(), top(), previewW(), bottom() - 26 - top());
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
		lastFrame = now;
		if (draggingPlayer) {
			yaw += (float) (mouseX - lastMouseX) * 1.5f;
			lastMouseX = mouseX;
		} else if (spinning) {
			yaw += dt * 35f;
		}
		yaw = ((yaw % 360f) + 360f) % 360f;

		float in = intro();
		g.centeredText(this.font, "§l✦ Wardrobe ✦", this.width / 2, 8, Theme.alpha(Theme.ACCENT, in));
		g.centeredText(this.font, "§7Only you can see your cosmetics", this.width / 2, 19, 0xFFFFFFFF);

		int idx = cardIndexAt(mouseX, mouseY);
		List<Design> list = Cosmetics.designs(kind);
		hovered = idx >= 0 ? list.get(idx).id() : null;
		setPreview(hovered);

		drawPreview(g);
		drawCards(g, mouseX, mouseY, list);

		// info line
		String showId = hovered != null ? hovered : Cosmetics.selected(kind);
		Design d = list.stream().filter(e -> e.id().equals(showId)).findFirst().orElse(list.get(0));
		String info = "§f" + d.name() + " §7· " + d.desc();
		if (d.id().equals(Cosmetics.CUSTOM) && !Cosmetics.customExists(kind)) {
			info = "§eYour image: §7put §f" + kind.customFile + "§7 into the folder";
		}
		g.centeredText(this.font, info, gridX() + gridW() / 2, bottom() - 16, 0xFFFFFFFF);
		if (System.currentTimeMillis() - statusAt < 4000) {
			g.centeredText(this.font, status, this.width / 2, 19 + 11, Theme.ACCENT);
		} else {
			g.centeredText(this.font, "§8Tip: drop a PNG here to use your own image", gridX() + gridW() / 2, bottom() - 5, 0xFFFFFFFF);
		}
	}

	private void drawPreview(GuiGraphicsExtractor g) {
		int x0 = left();
		int y0 = top();
		int x1 = x0 + previewW();
		int y1 = bottom() - 26;
		var player = this.minecraft.player;
		if (kind == Kind.TOTEM) {
			String id = hovered != null ? hovered : Cosmetics.selected(Kind.TOTEM);
			float t = Theme.time();
			float spin = (float) Math.cos(t * 1.6);
			float bob = (float) Math.sin(t * 2.0) * 3f;
			float size = Math.min(80f, (x1 - x0) * 0.6f);
			int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2 - 8;
			g.pose().pushMatrix();
			g.pose().translate(cx, cy + bob);
			if (!"statue".equals(id)) g.pose().scale(Math.max(0.08f, Math.abs(spin)) * (spin < 0 ? -1f : 1f), 1f);
			Cosmetics.Tex tex = Cosmetics.texture(Kind.TOTEM, id);
			if (tex != null) {
				int sz = Math.round(size);
				g.blit(RenderPipelines.GUI_TEXTURED, tex.id(), -sz / 2, -sz / 2, 0f, 0f, sz, sz, tex.width(), tex.height(), tex.width(), tex.height());
			} else {
				float s = size / 16f;
				g.pose().scale(s, s);
				g.item(Icons.stack(Items.TOTEM_OF_UNDYING), -8, -8);
			}
			g.pose().popMatrix();
			g.centeredText(this.font, "§7Changes the totem item", (x0 + x1) / 2, y1 - 22, 0xFFFFFFFF);
			g.centeredText(this.font, "§7(applied when you close)", (x0 + x1) / 2, y1 - 12, 0xFFFFFFFF);
			return;
		}
		if (kind == Kind.SHIELD) {
			// big shield that gently floats
			float bob = (float) Math.sin(Theme.time() * 2.0) * 3f;
			float s = Math.min(6f, (x1 - x0) / 22f);
			Icons.scaled(g, Icons.stack(Items.SHIELD), (x0 + x1) / 2f - 8 * s, (y0 + y1) / 2f - 8 * s + bob - 6, s);
			g.centeredText(this.font, "§7Plain shields only –", (x0 + x1) / 2, y1 - 22, 0xFFFFFFFF);
			g.centeredText(this.font, "§7banners stay vanilla", (x0 + x1) / 2, y1 - 12, 0xFFFFFFFF);
			return;
		}
		if (player == null) {
			g.centeredText(this.font, "§7Join a world", (x0 + x1) / 2, (y0 + y1) / 2 - 4, 0xFFFFFFFF);
			return;
		}
		int size = Math.max(20, Math.min((y1 - y0) / 3, 60));
		PlayerPreview.Gear gear = kind == Kind.WINGS ? PlayerPreview.Gear.WINGS : PlayerPreview.Gear.CAPE;
		try {
			PlayerPreview.draw(g, x0 + 2, y0 + 2, x1 - 2, y1 - 14, size, yaw, -0.15f, player, gear);
		} catch (RuntimeException e) {
			g.centeredText(this.font, "§7Preview unavailable", (x0 + x1) / 2, (y0 + y1) / 2, 0xFFFFFFFF);
		}
		g.centeredText(this.font, "§8drag to turn", (x0 + x1) / 2, y1 - 11, 0xFFFFFFFF);
	}

	private void drawCards(GuiGraphicsExtractor g, int mouseX, int mouseY, List<Design> list) {
		int cols = cols();
		String sel = Cosmetics.selected(kind);
		long nowMs = System.currentTimeMillis();
		for (int i = 0; i < perPage(); i++) {
			int idx = page * perPage() + i;
			if (idx >= list.size()) break;
			Design d = list.get(idx);
			int cx = gridX() + (i % cols) * (CARD_W + CARD_GAP);
			int cy = gridTop() + (i / cols) * (CARD_H + CARD_GAP);
			boolean hover = d.id().equals(hovered);
			boolean chosen = d.id().equals(sel);
			if (hover) cy -= 2; // lift
			// staggered fade-in
			float appear = Theme.easeOut((nowMs - openedAt - i * 35L) / 220f);
			cy += Math.round((1f - appear) * 8f);

			g.fill(cx, cy, cx + CARD_W, cy + CARD_H, hover ? 0xC0303030 : 0xA0181818);
			g.fill(cx, cy, cx + CARD_W, cy + 2, d.color() | 0xFF000000);
			drawThumb(g, d, cx + 1, cy + 4, CARD_W - 2, 48);
			// long names shrink to fit instead of being cut off
			int nameW = this.font.width(d.name());
			float ns = Math.max(0.6f, Math.min(1f, (CARD_W - 4) / (float) Math.max(1, nameW)));
			String name = ns > 0.6f ? d.name() : this.font.plainSubstrByWidth(d.name(), Math.round((CARD_W - 4) / ns));
			g.pose().pushMatrix();
			g.pose().translate(cx + CARD_W / 2f, cy + CARD_H - 11 + (1f - ns) * 4f);
			g.pose().scale(ns, ns);
			g.centeredText(this.font, name, 0, 0, chosen ? Theme.ACCENT : 0xFFE0E0E0);
			g.pose().popMatrix();

			int outline = chosen ? Theme.ACCENT : hover ? 0xFFFFFFFF : 0x40FFFFFF;
			g.outline(cx, cy, CARD_W, CARD_H, outline);
			if (chosen) {
				g.fill(cx + CARD_W - 11, cy + 3, cx + CARD_W - 2, cy + 12, 0xFF2A7A2A);
				g.text(this.font, "✔", cx + CARD_W - 10, cy + 4, 0xFFFFFFFF, false);
			}
			if (d.id().equals(flashId) && nowMs - flashAt < 500) {
				float f = 1f - (nowMs - flashAt) / 500f;
				g.fill(cx, cy, cx + CARD_W, cy + CARD_H, Theme.alpha(0xFFFFFFFF, 0.35f * f));
			}
		}
	}

	/** Thumbnail of the visible face of a design, centered in the box. */
	private void drawThumb(GuiGraphicsExtractor g, Design d, int x, int y, int w, int h) {
		if (d.id().equals(Cosmetics.NONE)) {
			g.centeredText(this.font, "§8✖", x + w / 2, y + h / 2 - 4, 0xFFFFFFFF);
			return;
		}
		Cosmetics.Tex t = Cosmetics.texture(kind, d.id());
		if (t == null) {
			g.centeredText(this.font, "§7?", x + w / 2, y + h / 2 - 4, 0xFFFFFFFF);
			return;
		}
		// regions in the 128-px-wide layout
		float u, v, rw, rh;
		float k = t.width() / 128f;
		switch (kind) {
			case CAPE -> { u = 2; v = 2; rw = 20; rh = 32; }
			case WINGS -> { u = 48; v = 4; rw = 20; rh = 40; }
			case TOTEM -> { u = 0; v = 0; rw = 16; rh = 16; k = t.width() / 16f; }
			default -> { u = 2; v = 2; rw = 24; rh = 44; }
		}
		int ru = Math.round(u * k), rv = Math.round(v * k), rwp = Math.round(rw * k), rhp = Math.round(rh * k);
		float scale = Math.min(w / rw, h / rh);
		int dw = Math.round(rw * scale);
		int dh = Math.round(rh * scale);
		int dx = x + (w - dw) / 2;
		int dy = y + (h - dh) / 2;
		g.fill(dx - 1, dy - 1, dx + dw + 1, dy + dh + 1, 0x30000000); // soft backdrop – shaped designs keep their outline
		g.blit(RenderPipelines.GUI_TEXTURED, t.id(), dx, dy, ru, rv, dw, dh, rwp, rhp, t.width(), t.height());
	}
}
