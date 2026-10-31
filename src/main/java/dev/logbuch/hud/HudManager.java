package dev.logbuch.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.logbuch.config.LogbuchConfig;
import dev.logbuch.gui.HudEditorScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Zeichnet alle Logbuch-HUD-Elemente und merkt sich ihre Flächen für den Editor. */
public final class HudManager {
	public static final DaysModule DAYS = new DaysModule();
	public static final TodoModule TODO = new TodoModule();
	public static final ArmorModule ARMOR = new ArmorModule();
	private static final Map<String, ImageModule> IMAGES = new HashMap<>();

	private HudManager() {}

	public static List<HudModule> modules() {
		List<HudModule> out = new ArrayList<>();
		// Bilder zuerst, damit Text darüber liegt.
		for (LogbuchConfig.Image img : LogbuchConfig.get().images) {
			ImageModule m = IMAGES.get(img.file);
			if (m == null || m.image() != img) {
				m = new ImageModule(img);
				IMAGES.put(img.file, m);
			}
			out.add(m);
		}
		out.add(DAYS);
		out.add(TODO);
		out.add(ARMOR);
		return out;
	}

	public static boolean editing() {
		return Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		boolean editing = editing();
		int sw = g.guiWidth();
		int sh = g.guiHeight();
		for (HudModule m : modules()) {
			m.drawnLastFrame = false;
			LogbuchConfig.Hud s = m.settings();
			if (!s.enabled) continue;
			if (!editing && !m.hasContent()) continue;
			try {
				drawModule(g, m, s, sw, sh, editing);
			} catch (RuntimeException e) {
				LogbuchConfig.LOG.debug("HUD-Element {} konnte nicht gezeichnet werden", m.id, e);
			}
		}
	}

	private static void drawModule(GuiGraphicsExtractor g, HudModule m, LogbuchConfig.Hud s, int sw, int sh, boolean editing) {
		int w = m.width(editing);
		int h = m.height(editing);
		float scale = m.canScale() ? s.scale : 1f;
		if (scale <= 0f) scale = 1f;
		int[] pos = position(m, s, w, h, scale, sw, sh);
		g.pose().pushMatrix();
		g.pose().translate(pos[0], pos[1]);
		g.pose().scale(scale, scale);
		m.draw(g, editing);
		g.pose().popMatrix();
		m.bx = pos[0];
		m.by = pos[1];
		m.bw = Math.round(w * scale);
		m.bh = Math.round(h * scale);
		m.drawnLastFrame = true;
	}

	public static int[] position(HudModule m, LogbuchConfig.Hud s, int w, int h, float scale, int sw, int sh) {
		if (m.fixedPosition()) {
			return m.fixedOrigin(sw, sh);
		}
		int pw = Math.round(w * scale);
		int ph = Math.round(h * scale);
		int x = Math.round(LogbuchConfig.clamp(s.x, 0f, 1f) * Math.max(0, sw - pw - 4)) + 2;
		int y = Math.round(LogbuchConfig.clamp(s.y, 0f, 1f) * Math.max(0, sh - ph - 4)) + 2;
		return new int[] {x, y};
	}

	/** Setzt die Position aus GUI-Pixeln (obere linke Ecke). */
	public static void setPixelPosition(HudModule m, int x, int y, int sw, int sh) {
		LogbuchConfig.Hud s = m.settings();
		int pw = m.bw;
		int ph = m.bh;
		float fx = (x - 2) / (float) Math.max(1, sw - pw - 4);
		float fy = (y - 2) / (float) Math.max(1, sh - ph - 4);
		s.x = LogbuchConfig.clamp(fx, 0f, 1f);
		s.y = LogbuchConfig.clamp(fy, 0f, 1f);
	}
}
