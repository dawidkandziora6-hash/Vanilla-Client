package dev.vanillaclient.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.HudEditorScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Draws all HUD elements of the mod. */
public final class HudManager {
	public static final DaysModule DAYS = new DaysModule();
	public static final CoordsModule COORDS = new CoordsModule();
	public static final TodoModule TODO = new TodoModule();
	public static final ArmorModule ARMOR = new ArmorModule();
	public static final KeystrokesModule KEYSTROKES = new KeystrokesModule();
	public static final List<HudModule> INFO = List.of(new InfoModules.Fps(), new InfoModules.Ping(), new InfoModules.Biome(),
			new InfoModules.Clock(), new InfoModules.Speed(), new InfoModules.Light(), new InfoModules.Session(),
new InfoModules.Effects(), new InfoModules.Counter(), new InfoModules.Reach(), new InfoModules.Combo(), new InfoModules.Playtime());
	public static final TargetHudModule TARGET = new TargetHudModule();
	public static final ScoreboardModule SCOREBOARD = new ScoreboardModule();
	private static final Map<String, ImageModule> IMAGES = new HashMap<>();

	private HudManager() {}

	public static List<HudModule> modules() {
		List<HudModule> out = new ArrayList<>();
		// Images first so text is drawn on top.
		for (Config.Image img : Config.get().images) {
			ImageModule m = IMAGES.get(img.file);
			if (m == null || m.image() != img) {
				m = new ImageModule(img);
				IMAGES.put(img.file, m);
			}
			out.add(m);
		}
		out.add(DAYS);
		out.add(COORDS);
		out.add(TODO);
		out.add(ARMOR);
		out.add(KEYSTROKES);
		out.add(TARGET);
		out.add(SCOREBOARD);
		out.addAll(INFO);
		return out;
	}

	/** Counts HUD frames – modules use it to compute their text once per frame. */
	public static long frame;

	public static boolean editing() {
		return Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
	}

	/** Normal in-game HUD pass. The HUD editor draws everything itself. */
	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		frame++;
		if (mc.player == null || editing()) return;
		int sw = g.guiWidth();
		int sh = g.guiHeight();
		for (HudModule m : modules()) {
			m.drawnLastFrame = false;
			Config.Hud s = m.settings();
			if (!s.enabled || !m.hasContent()) continue;
			try {
				drawModule(g, m, sw, sh, false);
			} catch (RuntimeException e) {
				Config.LOG.debug("HUD element {} could not be drawn", m.id, e);
			}
		}
	}

	/** Draws one module at its configured place and records its bounds. */
	public static void drawModule(GuiGraphicsExtractor g, HudModule m, int sw, int sh, boolean editing) {
		Config.Hud s = m.settings();
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

	public static int[] position(HudModule m, Config.Hud s, int w, int h, float scale, int sw, int sh) {
		if (m.fixedPosition()) {
			return m.fixedOrigin(sw, sh);
		}
		int pw = Math.round(w * scale);
		int ph = Math.round(h * scale);
		int x = Math.round(Config.clamp(s.x, 0f, 1f) * Math.max(0, sw - pw - 4)) + 2;
		int y = Math.round(Config.clamp(s.y, 0f, 1f) * Math.max(0, sh - ph - 4)) + 2;
		return new int[] {x, y};
	}

	/** Sets the position from GUI pixels (top-left corner). */
	public static void setPixelPosition(HudModule m, int x, int y, int sw, int sh) {
		Config.Hud s = m.settings();
		float fx = (x - 2) / (float) Math.max(1, sw - m.bw - 4);
		float fy = (y - 2) / (float) Math.max(1, sh - m.bh - 4);
		s.x = Config.clamp(fx, 0f, 1f);
		s.y = Config.clamp(fy, 0f, 1f);
	}
}
