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

/** W A S D, mouse buttons with clicks per second and the space bar – lights up when pressed. */
public final class KeystrokesModule extends HudModule {
	private static final int K = 20, GAP = 2;
	private final float[] glow = new float[7];
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

	@Override
	public int width(boolean editing) {
		return 3 * K + 2 * GAP;
	}

	@Override
	public int height(boolean editing) {
		Config.Keystrokes s = settings();
		int h = 2 * K + GAP;
		if (s.showMouse) h += K + GAP;
		if (s.showSpace) h += 10 + GAP;
		return h;
	}

	private static String label(KeyMapping k) {
		String s = k.getTranslatedKeyMessage().getString();
		return s.length() > 3 ? s.substring(0, 3) : s;
	}

	private static int cps(Deque<Long> q, long now) {
		while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
		return q.size();
	}

	private void key(GuiGraphicsExtractor g, Font f, int i, int x, int y, int w, int h, String text, boolean down, float dt) {
		Config.Keystrokes s = settings();
		glow[i] += ((down ? 1f : 0f) - glow[i]) * Math.min(1f, dt * (down ? 30f : 8f));
		int bg = Theme.lerpColor(s.keyColor, s.pressedColor, glow[i]);
		g.fill(x, y, x + w, y + h, bg);
		if (s.outline) g.outline(x, y, w, h, Theme.lerpColor(0x40FFFFFF, 0xC0FFFFFF, glow[i]));
		int tc = Theme.lerpColor(s.color, s.pressedTextColor, glow[i]);
		if (text.contains("\n")) {
			String[] parts = text.split("\n");
			g.centeredText(f, parts[0], x + w / 2, y + h / 2 - 8, tc);
			g.pose().pushMatrix();
			g.pose().translate(x + w / 2f, y + h / 2f + 2);
			g.pose().scale(0.75f, 0.75f);
			g.centeredText(f, parts[1], 0, 0, tc);
			g.pose().popMatrix();
		} else {
			g.centeredText(f, text, x + w / 2, y + (h - 8) / 2 + 1, tc);
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
		int y = 0;
		key(g, f, 0, K + GAP, y, K, K, label(o.keyUp), o.keyUp.isDown(), dt);
		y += K + GAP;
		key(g, f, 1, 0, y, K, K, label(o.keyLeft), o.keyLeft.isDown(), dt);
		key(g, f, 2, K + GAP, y, K, K, label(o.keyDown), o.keyDown.isDown(), dt);
		key(g, f, 3, 2 * (K + GAP), y, K, K, label(o.keyRight), o.keyRight.isDown(), dt);
		y += K + GAP;
		int full = 3 * K + 2 * GAP;
		if (s.showMouse) {
			int half = (full - GAP) / 2;
			String lt = s.showCps ? "LMB\n" + cps(left, now) + " CPS" : "LMB";
			String rt = s.showCps ? "RMB\n" + cps(right, now) + " CPS" : "RMB";
			key(g, f, 4, 0, y, half, K, lt, l, dt);
			key(g, f, 5, half + GAP, y, full - half - GAP, K, rt, r, dt);
			y += K + GAP;
		}
		if (s.showSpace) {
			boolean sp = o.keyJump.isDown();
			glow[6] += ((sp ? 1f : 0f) - glow[6]) * Math.min(1f, dt * (sp ? 30f : 8f));
			g.fill(0, y, full, y + 10, Theme.lerpColor(s.keyColor, s.pressedColor, glow[6]));
			if (s.outline) g.outline(0, y, full, 10, Theme.lerpColor(0x40FFFFFF, 0xC0FFFFFF, glow[6]));
			int bw = full / 3;
			g.fill(full / 2 - bw / 2, y + 4, full / 2 + bw / 2, y + 6, Theme.lerpColor(s.color, s.pressedTextColor, glow[6]));
		}
	}
}
