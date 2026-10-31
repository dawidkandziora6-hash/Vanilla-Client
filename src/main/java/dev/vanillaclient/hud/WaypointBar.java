package dev.vanillaclient.hud;

import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Theme;
import dev.vanillaclient.map.Waypoints;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * Waypoints like the vanilla locator bar: colored markers on the XP bar
 * (or a compass bar at the top) that show where your waypoints are.
 */
public final class WaypointBar {
	private static final String[] CARDINAL = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	private WaypointBar() {}

	private static float wrap(float deg) {
		deg %= 360f;
		if (deg >= 180f) deg -= 360f;
		if (deg < -180f) deg += 360f;
		return deg;
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		Config.WaypointHud cfg = Config.get().waypointHud;
		if (p == null || cfg.mode == 0 || mc.gui.screen() instanceof dev.vanillaclient.gui.HudEditorScreen) return;
		List<Config.Waypoint> list = Waypoints.here();
		if (cfg.mode == 1 && list.isEmpty()) return;
		Font font = mc.font;
		int sw = g.guiWidth(), sh = g.guiHeight();
		int cx = sw / 2;
		float yaw = p.getYRot();
		boolean top = cfg.mode == 2;
		int barY = top ? 6 : sh - 27;
		float fov = top ? 90f : 60f;
		int half = top ? 91 : 88;

		if (top) {
			g.fill(cx - 93, barY - 5, cx + 93, barY + 6, 0x80000000);
			g.outline(cx - 93, barY - 5, 186, 11, 0x40FFFFFF);
			for (int i = 0; i < 8; i++) {
				float rel = wrap(i * 45f - yaw);
				if (Math.abs(rel) > fov) continue;
				int x = cx + Math.round(rel / fov * half);
				boolean main = i % 2 == 0;
				String s = CARDINAL[i];
				int col = s.equals("N") ? 0xFFFF5555 : main ? 0xFFFFFFFF : 0xFFA0A0A0;
				g.centeredText(font, s, x, barY - 3, col);
			}
			for (int a = 0; a < 360; a += 15) {
				float rel = wrap(a - yaw);
				if (Math.abs(rel) > fov || a % 45 == 0) continue;
				int x = cx + Math.round(rel / fov * half);
				g.fill(x, barY + 2, x + 1, barY + 4, 0x60FFFFFF);
			}
			g.fill(cx, barY - 6, cx + 1, barY - 4, Theme.ACCENT);
		}

		Config.Waypoint focused = null;
		float best = 999f;
		for (Config.Waypoint w : list) {
			if (!w.visible) continue;
			double dx = w.x + 0.5 - p.getX();
			double dz = w.z + 0.5 - p.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (cfg.maxDistance > 0 && dist > cfg.maxDistance) continue;
			float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float rel = wrap(target - yaw);
			int col = w.color | 0xFF000000;
			if (Math.abs(rel) <= fov) {
				int x = cx + Math.round(rel / fov * half);
				int y = top ? barY + 1 : barY;
				// diamond marker with outline, a bit smaller when far away
				int r = dist < 64 ? 3 : 2;
				diamond(g, x, y, r + 1, 0xFF000000);
				diamond(g, x, y, r, col);
				double dy = w.y - p.getY();
				if (dy > 6) g.text(font, "▴", x - 2, y - r - 8, col, true);
				else if (dy < -6) g.text(font, "▾", x - 2, y + r, col, true);
				if (Math.abs(rel) < best) {
					best = Math.abs(rel);
					focused = w;
				}
			} else {
				// off to the side: small arrow at the end of the bar
				boolean right = rel > 0;
				int x = right ? cx + half + 3 : cx - half - 7;
				g.text(font, right ? "▸" : "◂", x, (top ? barY + 1 : barY) - 4, Theme.alpha(col, 0.8f), true);
			}
		}
		if (focused != null && best < 7f) {
			double dx = focused.x + 0.5 - p.getX(), dz = focused.z + 0.5 - p.getZ();
			int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
			String label = focused.name + (cfg.showDistance ? " §7" + dist + "m" : "");
			int ly = top ? barY + 9 : barY - 15;
			int w = font.width(label) + 6;
			g.fill(cx - w / 2, ly - 2, cx + w / 2, ly + 9, 0x90000000);
			g.centeredText(font, label, cx, ly, focused.color | 0xFF000000);
		}
	}

	private static void diamond(GuiGraphicsExtractor g, int x, int y, int r, int color) {
		for (int i = -r; i <= r; i++) {
			int w = r - Math.abs(i);
			g.fill(x - w, y + i, x + w + 1, y + i + 1, color);
		}
	}
}
