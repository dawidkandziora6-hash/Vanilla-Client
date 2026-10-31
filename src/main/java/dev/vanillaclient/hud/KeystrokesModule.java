package dev.vanillaclient.hud;

import java.util.ArrayDeque;
import java.util.Deque;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Icons;
import dev.vanillaclient.gui.Theme;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** W A S D, mouse buttons with CPS, space, sneak/sprint – lights up when pressed. Everything adjustable. */
public final class KeystrokesModule extends HudModule {
	private final float[] glow = new float[10];
	private final Deque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();
	private boolean wasLeft, wasRight;
	private long lastFrame = System.nanoTime();

	public KeystrokesModule() {
		super("keystrokes", "Keystrokes");
	}

	@Override
	public Config.Keystrokes settings() {
		return Config.get().keystrokes;
	}

	@Override
	public ItemStack icon() {
		return Icons.stack(Items.STONE_BUTTON);
	}

	private int k() {
		return Math.max(12, Math.min(32, settings().keySize));
	}

	private int gap() {
		return Math.max(0, Math.min(8, settings().gap));
	}

	@Override
	public int width(boolean editing) {
		return 3 * k() + 2 * gap();
	}

	@Override
	public int height(boolean editing) {
		Config.Keystrokes s = settings();
		int h = 2 * k() + gap();
		if (s.showMouse) h += k() + gap();
		if (s.showSpace) h += Math.max(8, k() / 2) + gap();
		if (s.showSneakSprint) h += Math.max(10, k() * 2 / 3) + gap();
		return h;
	}

	private String label(KeyMapping key, String arrow) {
		if (settings().labelStyle == 1 && arrow != null) return arrow;
		String s = key.getTranslatedKeyMessage().getString();
		return s.length() > 4 ? s.substring(0, 4) : s;
	}

	private static int cps(Deque<Long> q, long now) {
		while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
		return q.size();
	}

	private void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int col) {
		if (settings().rounded && w > 4 && h > 4) {
			g.fill(x + 1, y, x + w - 1, y + h, col);
			g.fill(x, y + 1, x + 1, y + h - 1, col);
			g.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
		} else {
			g.fill(x, y, x + w, y + h, col);
		}
	}

	private void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int col) {
		if (settings().rounded) {
			g.fill(x + 1, y, x + w - 1, y + 1, col);
			g.fill(x + 1, y + h - 1, x + w - 1, y + h, col);
			g.fill(x, y + 1, x + 1, y + h - 1, col);
			g.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
		} else {
			g.outline(x, y, w, h, col);
		}
	}

	private float fade(int i, boolean down, float dt) {
		float speed = Math.max(0.2f, settings().fadeSpeed);
		glow[i] += ((down ? 1f : 0f) - glow[i]) * Math.min(1f, dt * (down ? 35f : 8f * speed));
		return glow[i];
	}

	private void key(GuiGraphicsExtractor g, Font f, int i, int x, int y, int w, int h, String text, String sub, boolean down, float dt) {
		Config.Keystrokes s = settings();
		float gl = fade(i, down, dt);
		rect(g, x, y, w, h, Theme.lerpColor(s.keyColor, s.pressedColor, gl));
		if (s.outline) border(g, x, y, w, h, Theme.lerpColor(s.borderColor, 0xFFFFFFFF, gl * 0.6f));
		int tc = Theme.lerpColor(s.color, s.pressedTextColor, gl);
		if (sub != null) {
			if (!text.isEmpty()) g.text(f, text, x + (w - f.width(text)) / 2, y + h / 2 - 8, tc, s.textShadow);
			g.pose().pushMatrix();
			g.pose().translate(x + w / 2f, y + h / 2f + (text.isEmpty() ? -3 : 1));
			g.pose().scale(0.75f, 0.75f);
			g.text(f, sub, -f.width(sub) / 2, 0, tc, s.textShadow);
			g.pose().popMatrix();
		} else if (!text.isEmpty()) {
			g.text(f, text, x + (w - f.width(text)) / 2, y + (h - 8) / 2 + 1, tc, s.textShadow);
		}
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		Minecraft mc = Minecraft.getInstance();
		Options o = mc.options;
		Font f = mc.font;
		Config.Keystrokes s = settings();
		long nowN = System.nanoTime();
		float dt = Math.min(0.1f, (nowN - lastFrame) / 1e9f);
		lastFrame = nowN;
		long now = System.currentTimeMillis();
		boolean l = o.keyAttack.isDown(), r = o.keyUse.isDown();
		if (l && !wasLeft) left.addLast(now);
		if (r && !wasRight) right.addLast(now);
		wasLeft = l;
		wasRight = r;
		int K = k(), G = gap(), full = 3 * K + 2 * G;
		int y = 0;
		key(g, f, 0, K + G, y, K, K, label(o.keyUp, "▲"), null, o.keyUp.isDown(), dt);
		y += K + G;
		key(g, f, 1, 0, y, K, K, label(o.keyLeft, "◀"), null, o.keyLeft.isDown(), dt);
		key(g, f, 2, K + G, y, K, K, label(o.keyDown, "▼"), null, o.keyDown.isDown(), dt);
		key(g, f, 3, 2 * (K + G), y, K, K, label(o.keyRight, "▶"), null, o.keyRight.isDown(), dt);
		y += K + G;
		if (s.showMouse) {
			int half = (full - G) / 2;
			String lt = s.showLabelsOnMouse ? "LMB" : "", rt = s.showLabelsOnMouse ? "RMB" : "";
			key(g, f, 4, 0, y, half, K, lt, s.showCps ? cps(left, now) + " CPS" : null, l, dt);
			key(g, f, 5, half + G, y, full - half - G, K, rt, s.showCps ? cps(right, now) + " CPS" : null, r, dt);
			y += K + G;
		}
		if (s.showSneakSprint) {
			int h = Math.max(10, K * 2 / 3);
			int half = (full - G) / 2;
			key(g, f, 7, 0, y, half, h, "", "Sneak", o.keyShift.isDown(), dt);
			key(g, f, 8, half + G, y, full - half - G, h, "", "Sprint", o.keySprint.isDown(), dt);
			y += h + G;
		}
		if (s.showSpace) {
			int h = Math.max(8, K / 2);
			float gl = fade(6, o.keyJump.isDown(), dt);
			rect(g, 0, y, full, h, Theme.lerpColor(s.keyColor, s.pressedColor, gl));
			if (s.outline) border(g, 0, y, full, h, Theme.lerpColor(s.borderColor, 0xFFFFFFFF, gl * 0.6f));
			int bw = full / 3;
			g.fill(full / 2 - bw / 2, y + h / 2 - 1, full / 2 + bw / 2, y + h / 2 + 1, Theme.lerpColor(s.color, s.pressedTextColor, gl));
		}
	}
}
