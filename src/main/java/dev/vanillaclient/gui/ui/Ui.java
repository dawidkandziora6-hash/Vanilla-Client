package dev.vanillaclient.gui.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Icons;
import dev.vanillaclient.gui.Mouse;
import dev.vanillaclient.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Small, animated controls drawn in a vanilla style (all custom, so they can
 * slide, fade and react to hover).
 */
public final class Ui {
	private Ui() {}

	static final Identifier SLIDER = Identifier.withDefaultNamespace("widget/slider");
	static final Identifier SLIDER_HL = Identifier.withDefaultNamespace("widget/slider_highlighted");
	static final Identifier HANDLE = Identifier.withDefaultNamespace("widget/slider_handle");
	static final Identifier HANDLE_HL = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");
	public static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
	public static final Identifier BUTTON_HL = Identifier.withDefaultNamespace("widget/button_highlighted");
	public static final Identifier BUTTON_OFF = Identifier.withDefaultNamespace("widget/button_disabled");

	public static void click() {
		try {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		} catch (RuntimeException ignored) {
			// sound is optional
		}
	}

	static float approach(float v, float target, float speed) {
		return v + (target - v) * Math.min(1f, speed);
	}

	// ================================================================ base

	public abstract static class Control {
		public int x, y, w;
		/** 1 = full width, 2 = half width (two per row). */
		public int span = 1;
		protected float hover;
		protected boolean hovered;

		public int height() {
			return 22;
		}

		/** alpha: 0..1 fade-in, used for the slide/fade animation. */
		public abstract void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha);

		public boolean mouseDown(double mx, double my, int button) {
			return false;
		}

		/** Called every frame while this control holds the mouse. */
		public void mouseHeld(double mx, double my) {}

		public void mouseUp() {}

		boolean inside(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + height() - 2;
		}

		void updateHover(int mx, int my) {
			hovered = inside(mx, my);
			hover = approach(hover, hovered ? 1f : 0f, 0.25f);
		}

		protected void rowBackground(GuiGraphicsExtractor g, float alpha) {
			int bg = Theme.lerpColor(0x18FFFFFF, 0x30FFFFFF, hover);
			g.fill(x, y, x + w, y + height() - 2, Theme.alpha(bg, alpha));
		}
	}

	// ================================================================ header / info

	public static final class Header extends Control {
		final String text;

		public Header(String text) {
			this.text = text;
		}

		@Override
		public int height() {
			return 16;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha) {
			g.text(font, text, x + 1, y + 5, Theme.alpha(Theme.ACCENT, alpha), false);
			int tw = font.width(text) + 6;
			g.fill(x + tw, y + 9, x + w, y + 10, Theme.alpha(0x40FFFFFF, alpha));
		}
	}

	public static final class Info extends Control {
		final String[] lines;

		public Info(String... lines) {
			this.lines = lines;
		}

		@Override
		public int height() {
			return 4 + lines.length * 10;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha) {
			int yy = y + 2;
			for (String l : lines) {
				g.text(font, "§7" + l, x + 2, yy, Theme.alpha(0xFFFFFFFF, alpha), false);
				yy += 10;
			}
		}
	}

	/** Free drawing area (previews). */
	public static final class Custom extends Control {
		final int h;
		final Painter painter;

		public interface Painter {
			void paint(GuiGraphicsExtractor g, int x, int y, int w, int h, int mx, int my, float alpha);
		}

		public Custom(int h, Painter painter) {
			this.h = h;
			this.painter = painter;
		}

		@Override
		public int height() {
			return h;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha) {
			painter.paint(g, x, y, w, h, mx, my, alpha);
		}
	}

	// ================================================================ toggle

	public static final class Toggle extends Control {
		final String label;
		final ItemStack icon;
		final BooleanSupplier get;
		final Consumer<Boolean> set;
		float knob = -1f;

		public Toggle(String label, ItemStack icon, BooleanSupplier get, Consumer<Boolean> set) {
			this.label = label;
			this.icon = icon;
			this.get = get;
			this.set = set;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha) {
			updateHover(mx, my);
			boolean on = get.getAsBoolean();
			if (knob < 0) knob = on ? 1f : 0f;
			knob = approach(knob, on ? 1f : 0f, 0.3f);
			rowBackground(g, alpha);
			int tx = x + 6;
			if (icon != null) {
				Icons.small(g, icon, x + 5, y + 6);
				tx += 11;
			}
			g.text(font, label, tx, y + 6, Theme.alpha(0xFFFFFFFF, alpha), false);
			// switch
			int sw = 22, sh = 10;
			int sx = x + w - sw - 6;
			int sy = y + 5;
			int track = Theme.lerpColor(0xFF4A4A4A, 0xFF3FA34D, knob);
			g.fill(sx, sy, sx + sw, sy + sh, Theme.alpha(track, alpha));
			g.outline(sx, sy, sw, sh, Theme.alpha(0x60000000, alpha));
			int kx = sx + 1 + Math.round(knob * (sw - 10));
			g.fill(kx, sy + 1, kx + 8, sy + sh - 1, Theme.alpha(Theme.lerpColor(0xFFC8C8C8, 0xFFFFFFFF, hover), alpha));
			g.fill(kx, sy + sh - 2, kx + 8, sy + sh - 1, Theme.alpha(0x40000000, alpha));
		}

		@Override
		public boolean mouseDown(double mx, double my, int button) {
			if (!inside(mx, my) || button != Mouse.LEFT) return false;
			set.accept(!get.getAsBoolean());
			Config.save();
			click();
			return true;
		}
	}

	// ================================================================ slider

	public static final class Slider extends Control {
		final String label;
		final double min, max, step;
		final DoubleSupplier get;
		final DoubleConsumer set;
		final DoubleFunction<String> fmt;
		boolean held;

		public Slider(String label, double min, double max, double step, DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> fmt) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.step = step;
			this.get = get;
			this.set = set;
			this.fmt = fmt;
		}

		private int sx() {
			return label.isEmpty() ? x : x + Math.max(60, w * 2 / 5);
		}

		private int sw() {
			return x + w - sx();
		}

		@Override
		public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha) {
			updateHover(mx, my);
			if (!label.isEmpty()) {
				rowBackground(g, alpha);
				g.text(font, label, x + 6, y + 6, Theme.alpha(0xFFFFFFFF, alpha), false);
			}
			int sx = sx(), sw = sw();
			boolean hl = held || (mx >= sx && mx < sx + sw && my >= y && my < y + 20);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, hl ? SLIDER_HL : SLIDER, sx, y, sw, 20);
			double v = get.getAsDouble();
			double f = max == min ? 0 : (v - min) / (max - min);
			int hx = sx + (int) Math.round(f * (sw - 8));
			g.blitSprite(RenderPipelines.GUI_TEXTURED, hl ? HANDLE_HL : HANDLE, hx, y, 8, 20);
			g.centeredText(font, fmt.apply(v), sx + sw / 2, y + 6, Theme.alpha(0xFFFFFFFF, alpha));
		}

		@Override
		public boolean mouseDown(double mx, double my, int button) {
			if (button != Mouse.LEFT || !(mx >= sx() && mx < sx() + sw() && my >= y && my < y + 20)) return false;
			held = true;
			apply(mx);
			return true;
		}

		@Override
		public void mouseHeld(double mx, double my) {
			if (held) apply(mx);
		}

		@Override
		public void mouseUp() {
			if (held) {
				held = false;
				Config.save();
			}
		}

		private void apply(double mx) {
			double f = Math.max(0, Math.min(1, (mx - (sx() + 4)) / (double) (sw() - 8)));
			double v = min + f * (max - min);
			if (step > 0) v = Math.round(v / step) * step;
			set.accept(Math.max(min, Math.min(max, v)));
		}
	}

	// ================================================================ cycle

	public static final class Cycle<T> extends Control {
		final String label;
		final List<T> values;
		final Supplier<T> get;
		final Consumer<T> set;
		final Function<T, String> fmt;
		float bump;

		public Cycle(String label, List<T> values, Supplier<T> get, Consumer<T> set, Function<T, String> fmt) {
			this.label = label;
			this.values = values;
			this.get = get;
			this.set = set;
			this.fmt = fmt;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha) {
			updateHover(mx, my);
			bump = approach(bump, 0f, 0.2f);
			rowBackground(g, alpha);
			g.text(font, label, x + 6, y + 6, Theme.alpha(0xFFFFFFFF, alpha), false);
			String v = fmt.apply(get.get());
			int vw = font.width(v);
			int right = x + w - 6;
			int arrows = Theme.alpha(hovered ? Theme.ACCENT : 0xFF808080, alpha);
			g.text(font, "▶", right - 6, y + 6, arrows, false);
			int vx = right - 10 - vw;
			g.text(font, v, vx + Math.round(bump * 4), y + 6, Theme.alpha(0xFFFFFFFF, alpha), false);
			g.text(font, "◀", vx - 10, y + 6, arrows, false);
		}

		@Override
		public boolean mouseDown(double mx, double my, int button) {
			if (!inside(mx, my)) return false;
			int i = values.indexOf(get.get());
			int dir = button == Mouse.RIGHT ? -1 : 1;
			set.accept(values.get(Math.floorMod(i + dir, values.size())));
			bump = dir;
			Config.save();
			click();
			return true;
		}
	}

	// ================================================================ color

	public static final class ColorPick extends Control {
		final String label;
		final IntSupplier get;
		final IntConsumer set;
		final Consumer<ColorPick> open;

		public ColorPick(String label, IntSupplier get, IntConsumer set, Consumer<ColorPick> open) {
			this.label = label;
			this.get = get;
			this.set = set;
			this.open = open;
		}

		public int value() {
			return get.getAsInt();
		}

		public void apply(int c) {
			set.accept(c);
			Config.save();
		}

		public String label() {
			return label;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha) {
			updateHover(mx, my);
			rowBackground(g, alpha);
			g.text(font, label, x + 6, y + 6, Theme.alpha(0xFFFFFFFF, alpha), false);
			int sx = x + w - 30, sy = y + 4;
			for (int i = 0; i < 4; i++) {
				g.fill(sx + i * 6, sy, sx + i * 6 + 6, sy + 6, Theme.alpha(i % 2 == 0 ? 0xFF999999 : 0xFF666666, alpha));
				g.fill(sx + i * 6, sy + 6, sx + i * 6 + 6, sy + 12, Theme.alpha(i % 2 == 0 ? 0xFF666666 : 0xFF999999, alpha));
			}
			g.fill(sx, sy, sx + 24, sy + 12, get.getAsInt());
			g.outline(sx - 1, sy - 1, 26, 14, Theme.alpha(hovered ? 0xFFFFFFFF : 0xFF000000, alpha));
		}

		@Override
		public boolean mouseDown(double mx, double my, int button) {
			if (!inside(mx, my) || button != Mouse.LEFT) return false;
			click();
			open.accept(this);
			return true;
		}
	}

	// ================================================================ button

	public static final class Button extends Control {
		final String label;
		final ItemStack icon;
		final Runnable run;
		final int h;
		long pressedAt;

		public Button(String label, ItemStack icon, Runnable run) {
			this(label, icon, run, 20);
		}

		public Button(String label, ItemStack icon, Runnable run, int h) {
			this.label = label;
			this.icon = icon;
			this.run = run;
			this.h = h;
		}

		@Override
		public int height() {
			return h + 2;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha) {
			updateHover(mx, my);
			int lift = Math.round(hover);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, hovered ? BUTTON_HL : BUTTON, x, y - lift, w, h);
			long since = System.currentTimeMillis() - pressedAt;
			if (since < 300) g.fill(x, y - lift, x + w, y - lift + h, Theme.alpha(0x60FFFFFF, 1f - since / 300f));
			int tw = font.width(label) + (icon != null ? 20 : 0);
			int tx = x + (w - tw) / 2;
			if (icon != null) {
				g.item(icon, tx, y - lift + (h - 16) / 2);
				tx += 20;
			}
			g.text(font, label, tx, y - lift + (h - 8) / 2, Theme.alpha(hovered ? 0xFFFFFFA0 : 0xFFFFFFFF, alpha), true);
		}

		@Override
		public boolean mouseDown(double mx, double my, int button) {
			if (!inside(mx, my) || button != Mouse.LEFT) return false;
			pressedAt = System.currentTimeMillis();
			click();
			run.run();
			return true;
		}
	}

	// ================================================================ module card

	public static final class ModuleCard extends Control {
		final String name;
		final String sub;
		final ItemStack icon;
		final BooleanSupplier get;
		final Consumer<Boolean> set;
		final Runnable open;
		float knob = -1f;

		public ModuleCard(String name, String sub, ItemStack icon, BooleanSupplier get, Consumer<Boolean> set, Runnable open) {
			this.name = name;
			this.sub = sub;
			this.icon = icon;
			this.get = get;
			this.set = set;
			this.open = open;
			this.span = 2;
		}

		@Override
		public int height() {
			return 32;
		}

		private boolean onSwitch(double mx, double my) {
			return mx >= x + w - 30 && my >= y && my < y + 30 && mx < x + w;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha) {
			updateHover(mx, my);
			boolean on = get.getAsBoolean();
			if (knob < 0) knob = on ? 1f : 0f;
			knob = approach(knob, on ? 1f : 0f, 0.3f);
			int bg = Theme.lerpColor(0x30000000, 0x50FFFFFF, hover * 0.5f);
			g.fill(x, y, x + w, y + 30, Theme.alpha(bg, alpha));
			g.fill(x, y, x + 2, y + 30, Theme.alpha(Theme.lerpColor(0xFF555555, 0xFF3FA34D, knob), alpha));
			g.outline(x, y, w, 30, Theme.alpha(Theme.lerpColor(0x30FFFFFF, 0x90FFFFFF, hover), alpha));
			float bob = hovered ? (float) Math.sin(Theme.time() * 6) * 1.5f : 0f;
			Icons.scaled(g, icon, x + 6, y + 7 + bob, 1f);
			int tw = w - 26 - 30;
			g.text(font, font.plainSubstrByWidth(name, tw), x + 26, y + 6, Theme.alpha(0xFFFFFFFF, alpha), false);
			g.text(font, on ? "§a" + font.plainSubstrByWidth(sub, tw) : "§8off", x + 26, y + 17, Theme.alpha(0xFFFFFFFF, alpha), false);
			int sw = 18, sh = 8;
			int sx = x + w - sw - 6, sy = y + 11;
			g.fill(sx, sy, sx + sw, sy + sh, Theme.alpha(Theme.lerpColor(0xFF4A4A4A, 0xFF3FA34D, knob), alpha));
			int kx = sx + 1 + Math.round(knob * (sw - 8));
			g.fill(kx, sy + 1, kx + 6, sy + sh - 1, Theme.alpha(0xFFFFFFFF, alpha));
		}

		@Override
		public boolean mouseDown(double mx, double my, int button) {
			if (!inside(mx, my) || button != Mouse.LEFT) return false;
			click();
			if (onSwitch(mx, my) || open == null) {
				set.accept(!get.getAsBoolean());
				Config.save();
			} else {
				open.run();
			}
			return true;
		}
	}

	// ================================================================ tool tile

	public static final class Tile extends Control {
		final String name;
		final String sub;
		final ItemStack icon;
		final Runnable run;

		public Tile(String name, String sub, ItemStack icon, Runnable run) {
			this.name = name;
			this.sub = sub;
			this.icon = icon;
			this.run = run;
			this.span = 2;
		}

		@Override
		public int height() {
			return 40;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float alpha) {
			updateHover(mx, my);
			int lift = Math.round(hover * 2);
			int yy = y - lift;
			g.fill(x, yy, x + w, yy + 38, Theme.alpha(Theme.lerpColor(0x40000000, 0x60202020, hover), alpha));
			g.outline(x, yy, w, 38, Theme.alpha(Theme.lerpColor(0x40FFFFFF, Theme.ACCENT, hover), alpha));
			float s = 1.25f + hover * 0.15f;
			Icons.scaled(g, icon, x + 8 - (s - 1.25f) * 8, yy + 9 - (s - 1.25f) * 8, s);
			int tw = w - 34 - 12;
			g.text(font, "§l" + font.plainSubstrByWidth(name, tw - 6), x + 34, yy + 9, Theme.alpha(0xFFFFFFFF, alpha), false);
			g.text(font, "§7" + font.plainSubstrByWidth(sub, tw), x + 34, yy + 21, Theme.alpha(0xFFFFFFFF, alpha), false);
			g.text(font, "›", x + w - 10 + Math.round(hover * 3), yy + 15, Theme.alpha(hovered ? Theme.ACCENT : 0xFF707070, alpha), false);
		}

		@Override
		public boolean mouseDown(double mx, double my, int button) {
			if (!inside(mx, my) || button != Mouse.LEFT) return false;
			click();
			run.run();
			return true;
		}
	}

	// ================================================================ list

	/** Vertical list with two-column rows, scrolling, clipping and a staggered intro. */
	public static final class List_ {
		public final List<Control> controls = new ArrayList<>();
		private Control held;
		public int x, top, w, bottom;
		private float scroll, scrollTarget;
		private long builtAt = System.currentTimeMillis();
		private int contentH;

		public <T extends Control> T add(T c) {
			controls.add(c);
			return c;
		}

		public void reset() {
			controls.clear();
			held = null;
			scroll = scrollTarget = 0;
			builtAt = System.currentTimeMillis();
		}

		private void layout() {
			int yy = top + 2 - Math.round(scroll);
			int col = 0;
			int rowH = 0;
			int half = (w - 6) / 2;
			for (Control c : controls) {
				if (c.span == 2) {
					c.w = half;
					c.x = x + (col == 0 ? 0 : half + 6);
					c.y = yy;
					rowH = Math.max(rowH, c.height());
					if (col == 1) {
						yy += rowH + 2;
						col = 0;
						rowH = 0;
					} else {
						col = 1;
					}
				} else {
					if (col == 1) {
						yy += rowH + 2;
						col = 0;
						rowH = 0;
					}
					c.w = w;
					c.x = x;
					c.y = yy;
					yy += c.height() + 2;
				}
			}
			if (col == 1) yy += rowH + 2;
			contentH = yy + Math.round(scroll) - top;
		}

		public void render(GuiGraphicsExtractor g, Font font, int mx, int my) {
			scroll = approach(scroll, scrollTarget, 0.3f);
			layout();
			if (held != null) held.mouseHeld(mx, my);
			g.enableScissor(x - 4, top, x + w + 4, bottom);
			long now = System.currentTimeMillis();
			int i = 0;
			for (Control c : controls) {
				float t = Theme.easeOut((now - builtAt - i * 28L) / 260f);
				i++;
				if (c.y + c.height() < top || c.y > bottom) continue;
				int off = Math.round((1f - t) * 14f);
				g.pose().pushMatrix();
				g.pose().translate(off, 0);
				boolean inView = my >= top && my < bottom;
				c.render(g, font, inView ? mx - off : -1000, inView ? my : -1000, Math.max(0.02f, t));
				g.pose().popMatrix();
			}
			g.disableScissor();
			// scrollbar
			int viewH = bottom - top;
			if (contentH > viewH) {
				int barH = Math.max(12, viewH * viewH / contentH);
				int barY = top + Math.round((viewH - barH) * (scroll / Math.max(1, contentH - viewH)));
				g.fill(x + w + 3, top, x + w + 4, bottom, 0x30FFFFFF);
				g.fill(x + w + 3, barY, x + w + 4, barY + barH, 0xC0FFFFFF);
			}
		}

		public boolean mouseDown(double mx, double my, int button) {
			if (my < top || my >= bottom || mx < x || mx >= x + w) return false;
			for (Control c : controls) {
				if (c.mouseDown(mx, my, button)) {
					held = c;
					return true;
				}
			}
			return false;
		}

		public void mouseUp() {
			if (held != null) held.mouseUp();
			held = null;
		}

		public boolean scroll(double mx, double my, double v) {
			if (mx < x || mx >= x + w + 6 || my < top || my >= bottom) return false;
			int max = Math.max(0, contentH - (bottom - top));
			scrollTarget = (float) Math.max(0, Math.min(max, scrollTarget - v * 24));
			return true;
		}
	}
}
