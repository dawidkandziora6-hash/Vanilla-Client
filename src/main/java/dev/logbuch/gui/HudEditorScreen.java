package dev.logbuch.gui;

import java.util.List;

import dev.logbuch.config.LogbuchConfig;
import dev.logbuch.hud.HudManager;
import dev.logbuch.hud.HudModule;
import dev.logbuch.hud.ImageModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Durchsichtiger Editor über dem echten HUD: ziehen = verschieben,
 * Mausrad = Größe, Rechtsklick = Größe zurücksetzen.
 */
public final class HudEditorScreen extends LbScreen {
	private static final int SNAP = 4;

	private HudModule dragged;
	private HudModule selected;
	private int grabX, grabY;
	private int guideX = -1, guideY = -1;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("HUD bearbeiten"), parent);
	}

	@Override
	protected void init() {
		super.init();
		action(this.width / 2 - 50, this.height / 2 - 10 + 60, 100, "Fertig", this::onClose);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// Kein Abdunkeln: man soll das HUD so sehen, wie es im Spiel aussieht.
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
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		// Mittellinien beim Einrasten.
		if (guideX >= 0) g.fill(guideX, 0, guideX + 1, this.height, 0x80FFFF55);
		if (guideY >= 0) g.fill(0, guideY, this.width, guideY + 1, 0x80FFFF55);

		HudModule hover = dragged != null ? dragged : hit(mouseX, mouseY);
		for (HudModule m : HudManager.modules()) {
			if (!m.drawnLastFrame) continue;
			int col = m == hover ? 0xFFFFFFFF : (m == selected ? 0xFFFFFF55 : 0x60FFFFFF);
			outline(g, m.bx - 2, m.by - 2, m.bw + 4, m.bh + 4, col);
		}
		if (hover != null) {
			String info = hover.name;
			if (hover.fixedPosition()) info += " §7(an Hotbar angedockt)";
			else if (hover.canScale()) info += " §7" + Math.round(hover.settings().scale * 100) + "%";
			int ty = hover.by - 13 < 2 ? hover.by + hover.bh + 4 : hover.by - 13;
			int tw = this.font.width(info);
			int tx = Math.max(2, Math.min(this.width - tw - 2, hover.bx));
			g.fill(tx - 2, ty - 2, tx + tw + 2, ty + 10, 0xC0000000);
			g.text(this.font, info, tx, ty, 0xFFFFFFFF, false);
		}
		int cy = this.height / 2 + 28;
		g.centeredText(this.font, "Ziehen = verschieben · Mausrad = Größe · Rechtsklick = Größe zurücksetzen", this.width / 2, cy, 0xFFFFFFFF);
		g.centeredText(this.font, "§7Ausgeblendete Elemente im Logbuch-Menü anschalten", this.width / 2, cy + 12, 0xFFFFFFFF);
	}

	private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
		g.fill(x, y, x + w, y + 1, c);
		g.fill(x, y + h - 1, x + w, y + h, c);
		g.fill(x, y, x + 1, y + h, c);
		g.fill(x + w - 1, y, x + w, y + h, c);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		HudModule m = hit(event.x(), event.y());
		if (m == null) {
			selected = null;
			return false;
		}
		selected = m;
		if (event.button() == 1) {
			if (m.canScale()) {
				m.settings().scale = m instanceof ImageModule ? -1f : 1f;
				LogbuchConfig.save();
			}
			return true;
		}
		if (event.button() == 0 && !m.fixedPosition()) {
			dragged = m;
			grabX = (int) event.x() - m.bx;
			grabY = (int) event.y() - m.by;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragged == null) return super.mouseDragged(event, dx, dy);
		int x = (int) event.x() - grabX;
		int y = (int) event.y() - grabY;
		guideX = -1;
		guideY = -1;
		int w = dragged.bw;
		int h = dragged.bh;
		// An Rändern und Bildschirmmitte einrasten.
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
		HudManager.setPixelPosition(dragged, x, y, this.width, this.height);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragged != null) {
			dragged = null;
			guideX = -1;
			guideY = -1;
			LogbuchConfig.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
		HudModule m = hit(mx, my);
		if (m == null) m = selected;
		if (m == null || !m.canScale() || vertical == 0) return false;
		LogbuchConfig.Hud s = m.settings();
		boolean image = m instanceof ImageModule;
		float step = image ? s.scale * 0.1f : 0.05f;
		float min = image ? 0.02f : 0.5f;
		float max = image ? 8f : 3f;
		s.scale = LogbuchConfig.clamp(s.scale + (float) Math.signum(vertical) * Math.max(step, 0.005f), min, max);
		LogbuchConfig.save();
		return true;
	}
}
