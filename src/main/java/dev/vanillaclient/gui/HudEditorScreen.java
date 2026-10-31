package dev.vanillaclient.gui;

import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.hud.HudManager;
import dev.vanillaclient.hud.HudModule;
import dev.vanillaclient.hud.ImageModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * HUD editor. It draws every element itself, so dragging always works:
 * click and hold to move (follows the mouse every frame), scroll to resize,
 * right-click to reset the size. Snaps to edges and the screen center.
 */
public final class HudEditorScreen extends BaseScreen {
	private static final int SNAP = 5;

	private HudModule dragged;
	private HudModule selected;
	private int grabX, grabY;
	private int guideX = -1, guideY = -1;
	private boolean showHelp = true;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("Edit HUD"), parent);
	}

	@Override
	protected void init() {
		super.init();
		int y = this.height - 28;
		action(this.width / 2 - 154, y, 100, "↺ Reset sizes", () -> {
			for (HudModule m : HudManager.modules()) {
				if (m.canScale()) m.settings().scale = m instanceof ImageModule ? -1f : 1f;
			}
			Config.save();
		});
		action(this.width / 2 - 50, y, 100, showHelp ? "Hide help" : "Show help", () -> {
			showHelp = !showHelp;
			rebuildWidgets();
		});
		action(this.width / 2 + 54, y, 100, "✔ Done", this::onClose);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// Light grid instead of darkening, so you see the HUD like in game.
		int c = 0x10FFFFFF;
		for (int x = 0; x < this.width; x += 20) g.fill(x, 0, x + 1, this.height, c);
		for (int y = 0; y < this.height; y += 20) g.fill(0, y, this.width, y + 1, c);
	}

	private HudModule hit(double mx, double my) {
		List<HudModule> mods = HudManager.modules();
		for (int i = mods.size() - 1; i >= 0; i--) {
			HudModule m = mods.get(i);
			if (!m.drawnLastFrame) continue;
			if (mx >= m.bx - 2 && mx < m.bx + m.bw + 2 && my >= m.by - 2 && my < m.by + m.bh + 2) return m;
		}
		return null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// 1) Move the grabbed element to the mouse – every frame.
		if (dragged != null) moveTo(dragged, mouseX - grabX, mouseY - grabY);

		// 2) Draw all enabled elements (as preview) and remember where they are.
		for (HudModule m : HudManager.modules()) {
			m.drawnLastFrame = false;
			if (!m.settings().enabled) continue;
			try {
				HudManager.drawModule(g, m, this.width, this.height, true);
			} catch (RuntimeException e) {
				Config.LOG.debug("Preview of {} failed", m.id, e);
			}
		}
		super.extractRenderState(g, mouseX, mouseY, delta);
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		if (guideX >= 0) g.fill(guideX, 0, guideX + 1, this.height, 0xA0FFD34D);
		if (guideY >= 0) g.fill(0, guideY, this.width, guideY + 1, 0xA0FFD34D);

		HudModule hover = dragged != null ? dragged : hit(mouseX, mouseY);
		float pulse = Theme.pulse(5f);
		for (HudModule m : HudManager.modules()) {
			if (!m.drawnLastFrame) continue;
			int col;
			if (m == dragged) col = Theme.ACCENT;
			else if (m == hover) col = 0xFFFFFFFF;
			else if (m == selected) col = Theme.lerpColor(0x80FFD34D, 0xFFFFD34D, pulse);
			else col = 0x50FFFFFF;
			if (m == hover || m == dragged) g.fill(m.bx - 2, m.by - 2, m.bx + m.bw + 2, m.by + m.bh + 2, 0x20FFFFFF);
			g.outline(m.bx - 2, m.by - 2, m.bw + 4, m.bh + 4, col);
		}
		if (hover != null) {
			String info = hover.name;
			if (hover.fixedPosition()) info += " §7· docked to hotbar";
			else if (hover.canScale()) info += " §7· " + Math.round(hover.settings().scale * 100) + "%";
			int tw = this.font.width(info) + 12;
			int ty = hover.by - 15 < 2 ? hover.by + hover.bh + 5 : hover.by - 15;
			int tx = Math.max(2, Math.min(this.width - tw - 2, hover.bx));
			g.fill(tx - 2, ty - 2, tx + tw + 2, ty + 11, 0xE0101010);
			g.outline(tx - 2, ty - 2, tw + 4, 13, 0x60FFFFFF);
			Icons.small(g, hover.icon(), tx, ty);
			g.text(this.font, info, tx + 11, ty, 0xFFFFFFFF, false);
		}
		if (showHelp) {
			int w = 230;
			int x = this.width / 2 - w / 2;
			int y = this.height - 76;
			Theme.panel(g, x, y, w, 40);
			g.centeredText(this.font, "§e✥ Drag§r to move  ·  §e⇕ Scroll§r to resize", this.width / 2, y + 6, 0xFFFFFFFF);
			g.centeredText(this.font, "§eRight-click§r resets size  ·  §eEsc§r to finish", this.width / 2, y + 17, 0xFFFFFFFF);
			g.centeredText(this.font, "§7Hidden elements: turn them on in the menu", this.width / 2, y + 28, 0xFFFFFFFF);
		}
	}

	private void moveTo(HudModule m, int x, int y) {
		guideX = -1;
		guideY = -1;
		int w = m.bw;
		int h = m.bh;
		if (Math.abs(x - 2) < SNAP) x = 2;
		if (Math.abs(x + w - (this.width - 2)) < SNAP) x = this.width - 2 - w;
		if (Math.abs(x + w / 2 - this.width / 2) < SNAP) {
			x = this.width / 2 - w / 2;
			guideX = this.width / 2;
		}
		if (Math.abs(y - 2) < SNAP) y = 2;
		if (Math.abs(y + h - (this.height - 2)) < SNAP) y = this.height - 2 - h;
		if (Math.abs(y + h / 2 - this.height / 2) < SNAP) {
			y = this.height / 2 - h / 2;
			guideY = this.height / 2;
		}
		HudManager.setPixelPosition(m, x, y, this.width, this.height);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		HudModule m = hit(event.x(), event.y());
		if (m == null) {
			selected = null;
			return super.mouseClicked(event, doubleClick);
		}
		selected = m;
		if (event.button() == Mouse.RIGHT) {
			if (m.canScale()) {
				m.settings().scale = m instanceof ImageModule ? -1f : 1f;
				Config.save();
			}
			return true;
		}
		if (event.button() == Mouse.LEFT && !m.fixedPosition()) {
			dragged = m;
			grabX = (int) event.x() - m.bx;
			grabY = (int) event.y() - m.by;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragged != null) {
			moveTo(dragged, (int) event.x() - grabX, (int) event.y() - grabY);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragged != null) {
			dragged = null;
			guideX = -1;
			guideY = -1;
			Config.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
		HudModule m = hit(mx, my);
		if (m == null) m = selected;
		if (m == null || !m.canScale() || vertical == 0) return false;
		Config.Hud s = m.settings();
		boolean image = m instanceof ImageModule;
		float step = image ? Math.max(0.005f, s.scale * 0.1f) : 0.05f;
		float min = image ? 0.02f : 0.5f;
		float max = image ? 8f : 3f;
		s.scale = Config.clamp(s.scale + (float) Math.signum(vertical) * step, min, max);
		Config.save();
		return true;
	}
}
