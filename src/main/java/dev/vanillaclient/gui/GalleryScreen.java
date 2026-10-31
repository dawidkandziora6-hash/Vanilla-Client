package dev.vanillaclient.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.ui.Ui;
import dev.vanillaclient.images.ImageManager;
import dev.vanillaclient.images.Screenshots;
import dev.vanillaclient.images.Thumbs;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * Your pictures with real previews: screenshots (newest first) and the overlay
 * pictures for the HUD. Click a picture to see it big.
 */
public final class GalleryScreen extends BaseScreen {
	private static boolean shotsTab = true;
	private List<Path> files = new ArrayList<>();
	private int scroll, contentH;
	private int viewing = -1;
	private long confirmDeleteAt;
	private String status = "";
	private long statusAt;

	public GalleryScreen(Screen parent) {
		super(Component.literal("Gallery"), parent);
	}

	/** Opens the gallery showing one screenshot. */
	public static GalleryScreen showing(Screen parent, String fileName) {
		shotsTab = true;
		GalleryScreen s = new GalleryScreen(parent);
		s.reload();
		for (int i = 0; i < s.files.size(); i++) {
			if (s.files.get(i).getFileName().toString().equals(fileName)) s.viewing = i;
		}
		return s;
	}

	public static GalleryScreen overlays(Screen parent) {
		shotsTab = false;
		return new GalleryScreen(parent);
	}

	private void reload() {
		if (shotsTab) {
			files = Screenshots.all();
		} else {
			files = new ArrayList<>();
			for (Config.Image img : Config.get().images) files.add(Config.imagesDir().resolve(img.file));
		}
	}

	private int gx() {
		return Math.max(8, this.width / 2 - gw() / 2);
	}

	private int gw() {
		return Math.min(this.width - 16, 520);
	}

	private int top() {
		return 54;
	}

	private int bottom() {
		return this.height - 34;
	}

	private int cellW() {
		return shotsTab ? 120 : 76;
	}

	private int cellH() {
		return shotsTab ? 80 : 88;
	}

	private int cols() {
		return Math.max(1, (gw() + 6) / (cellW() + 6));
	}

	@Override
	protected void init() {
		super.init();
		if (files.isEmpty() || viewing < 0) reload();
		int tw = 120;
		iconAction(this.width / 2 - tw - 2, 28, tw, Icons.stack(net.minecraft.world.item.Items.PAINTING), "    Screenshots", () -> switchTab(true)).active = !shotsTab;
		iconAction(this.width / 2 + 2, 28, tw, Icons.stack(net.minecraft.world.item.Items.ITEM_FRAME), "    HUD pictures", () -> switchTab(false)).active = shotsTab;
		int y = this.height - 26;
		if (viewing >= 0) {
			int bw = 80;
			int x = this.width / 2 - (bw * 5 + 16) / 2;
			action(x, y, bw, "◀", () -> step(-1));
			action(x + (bw + 4), y, bw, "Open file", () -> Platform.open(files.get(viewing)));
			if (shotsTab) {
				action(x + 2 * (bw + 4), y, bw, "→ HUD picture", this::useAsOverlay)
						.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Copy it to your HUD pictures")));
				action(x + 3 * (bw + 4), y, bw, "§cDelete", this::delete);
			} else {
				Config.Image img = image(viewing);
				action(x + 2 * (bw + 4), y, bw, img != null && img.enabled ? "§aOn screen" : "§7Hidden", () -> {
					Config.Image i = image(viewing);
					if (i != null) {
						i.enabled = !i.enabled;
						Config.save();
					}
					rebuildWidgets();
				});
				action(x + 3 * (bw + 4), y, bw, "Place it", () -> this.minecraft.setScreenAndShow(new HudEditorScreen(this)));
			}
			action(x + 4 * (bw + 4), y, bw, "▶", () -> step(1));
		} else {
			action(this.width / 2 - 154, y, 100, "Open folder", () -> Platform.open(shotsTab ? Thumbs.screenshotsDir() : Config.imagesDir()));
			action(this.width / 2 - 50, y, 100, "↻ Refresh", () -> {
				ImageManager.scan();
				reload();
			});
			action(this.width / 2 + 54, y, 100, "✔ Done", this::onClose);
		}
	}

	private void switchTab(boolean shots) {
		shotsTab = shots;
		viewing = -1;
		scroll = 0;
		reload();
		rebuildWidgets();
	}

	private Config.Image image(int i) {
		if (i < 0 || i >= files.size()) return null;
		String name = files.get(i).getFileName().toString();
		for (Config.Image img : Config.get().images) if (img.file.equals(name)) return img;
		return null;
	}

	private void step(int d) {
		if (files.isEmpty()) return;
		viewing = Math.floorMod(viewing + d, files.size());
		confirmDeleteAt = 0;
		rebuildWidgets();
	}

	private void say(String s) {
		status = s;
		statusAt = System.currentTimeMillis();
	}

	private void useAsOverlay() {
		Path f = files.get(viewing);
		try {
			Files.createDirectories(Config.imagesDir());
			Files.copy(f, Config.imagesDir().resolve(f.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
			ImageManager.scan();
			say("✔ Added to your HUD pictures – turn it on in the HUD pictures tab");
		} catch (Exception e) {
			say("§cCould not copy it");
		}
	}

	private void delete() {
		long now = System.currentTimeMillis();
		if (now - confirmDeleteAt > 3000) {
			confirmDeleteAt = now;
			say("§eClick Delete again to delete this screenshot");
			return;
		}
		Path f = files.get(viewing);
		try {
			Files.deleteIfExists(f);
			Thumbs.forget(f);
			files.remove(viewing);
			say("✔ Deleted");
		} catch (Exception e) {
			say("§cCould not delete it");
		}
		if (files.isEmpty()) viewing = -1;
		else viewing = Math.min(viewing, files.size() - 1);
		confirmDeleteAt = 0;
		rebuildWidgets();
	}

	// ------------------------------------------------------------ drawing

	private void drawPic(GuiGraphicsExtractor g, Path file, int x, int y, int w, int h, int max) {
		Thumbs.Thumb t = Thumbs.get(file, max);
		if (t == null) {
			g.fill(x, y, x + w, y + h, 0xFF1A1A1A);
			String s = Thumbs.failed(file, max) ? "§8?" : "§8…";
			g.centeredText(this.font, s, x + w / 2, y + h / 2 - 4, 0xFFFFFFFF);
			return;
		}
		float k = Math.min(w / (float) t.w(), h / (float) t.h());
		int dw = Math.max(1, Math.round(t.w() * k)), dh = Math.max(1, Math.round(t.h() * k));
		int dx = x + (w - dw) / 2, dy = y + (h - dh) / 2;
		g.blit(RenderPipelines.GUI_TEXTURED, t.id(), dx, dy, 0f, 0f, dw, dh, t.w(), t.h(), t.w(), t.h());
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.centeredText(this.font, "§l✦ Gallery", this.width / 2, 8, Theme.alpha(Theme.ACCENT, intro()));
		g.centeredText(this.font, "§7" + files.size() + (shotsTab ? " screenshots · F2 takes one" : " HUD pictures · drop PNG/JPG files here"), this.width / 2, 18, 0xFFFFFFFF);
		if (viewing >= 0 && viewing < files.size()) {
			Path f = files.get(viewing);
			int x = 10, y = top(), w = this.width - 20, h = bottom() - top() - 14;
			g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xC0000000);
			drawPic(g, f, x, y, w, h, 1280);
			Thumbs.Thumb t = Thumbs.get(f, 1280);
			String info = f.getFileName() + (t == null ? "" : "  §8" + t.fullW() + "×" + t.fullH()) + "  §7(" + (viewing + 1) + "/" + files.size() + ")";
			g.centeredText(this.font, info, this.width / 2, bottom() - 10, 0xFFFFFFFF);
		} else {
			int x0 = gx(), cols = cols();
			int cw = cellW(), ch = cellH();
			g.enableScissor(x0 - 2, top(), x0 + gw() + 2, bottom());
			int y0 = top() + 2 - scroll;
			for (int i = 0; i < files.size(); i++) {
				int cx = x0 + (i % cols) * (cw + 6), cy = y0 + (i / cols) * (ch + 6);
				if (cy + ch < top() || cy > bottom()) continue;
				boolean hov = mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + ch && mouseY >= top() && mouseY < bottom();
				Config.Image img = shotsTab ? null : image(i);
				int border = img != null && img.enabled ? Theme.GOOD : hov ? Theme.ACCENT : 0x40FFFFFF;
				g.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, border);
				g.fill(cx, cy, cx + cw, cy + ch, 0xFF141414);
				int picH = ch - 12;
				drawPic(g, files.get(i), cx + 2, cy + 2, cw - 4, picH - 2, shotsTab ? 240 : 128);
				String name = files.get(i).getFileName().toString();
				if (shotsTab && name.length() > 10) name = name.substring(0, 10).replace('_', ' ') + " " + name.substring(Math.min(name.length(), 11), Math.min(name.length(), 16)).replace('.', ':');
				g.text(this.font, this.font.plainSubstrByWidth(name, cw - 4), cx + 2, cy + ch - 10, hov ? 0xFFFFFFFF : 0xFFB0B0B0, false);
				if (img != null && img.enabled) g.text(this.font, "§a●", cx + cw - 8, cy + ch - 10, 0xFFFFFFFF, false);
			}
			contentH = ((files.size() + cols - 1) / cols) * (ch + 6);
			if (files.isEmpty()) {
				g.centeredText(this.font, shotsTab ? "§7No screenshots yet – press F2 in game" : "§7No pictures – drop PNG/JPG files onto this window", this.width / 2, top() + 40, 0xFFFFFFFF);
			}
			g.disableScissor();
			if (!shotsTab && !files.isEmpty()) g.centeredText(this.font, "§8click = big view · right-click = show/hide on screen", this.width / 2, bottom() - 8, 0xFFFFFFFF);
		}
		if (System.currentTimeMillis() - statusAt < 3500) g.centeredText(this.font, status, this.width / 2, 42, Theme.ACCENT);
	}

	// ------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		if (viewing >= 0) return false;
		double mx = event.x(), my = event.y();
		if (my < top() || my >= bottom()) return false;
		int cols = cols(), cw = cellW(), ch = cellH();
		int y0 = top() + 2 - scroll;
		for (int i = 0; i < files.size(); i++) {
			int cx = gx() + (i % cols) * (cw + 6), cy = y0 + (i / cols) * (ch + 6);
			if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ch) {
				Ui.click();
				if (!shotsTab && event.button() == Mouse.RIGHT) {
					Config.Image img = image(i);
					if (img != null) {
						img.enabled = !img.enabled;
						Config.save();
					}
				} else {
					viewing = i;
					rebuildWidgets();
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		if (viewing >= 0) {
			step(v > 0 ? -1 : 1);
			return true;
		}
		int max = Math.max(0, contentH - (bottom() - top()));
		scroll = (int) Math.max(0, Math.min(max, scroll - v * 30));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (viewing >= 0) {
			int k = event.key();
			if (k == com.mojang.blaze3d.platform.InputConstants.KEY_LEFT) {
				step(-1);
				return true;
			}
			if (k == com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT) {
				step(1);
				return true;
			}
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (viewing >= 0) {
			viewing = -1;
			rebuildWidgets();
			return;
		}
		super.onClose();
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		int n = ImageManager.importFiles(paths);
		if (n > 0) {
			switchTab(false);
			say("✔ " + n + " picture(s) added");
		}
	}
}
