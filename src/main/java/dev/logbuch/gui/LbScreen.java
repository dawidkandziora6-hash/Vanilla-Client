package dev.logbuch.gui;

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

import dev.logbuch.config.LogbuchConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Basis für alle Logbuch-Menüs: Vanilla-Knöpfe plus Schieberegler, die mit den
 * echten Vanilla-Slider-Texturen gezeichnet werden.
 */
public abstract class LbScreen extends Screen {
	private static final Identifier SLIDER = Identifier.withDefaultNamespace("widget/slider");
	private static final Identifier SLIDER_HL = Identifier.withDefaultNamespace("widget/slider_highlighted");
	private static final Identifier HANDLE = Identifier.withDefaultNamespace("widget/slider_handle");
	private static final Identifier HANDLE_HL = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");

	protected final Screen parent;
	private final List<Slider> sliders = new ArrayList<>();
	private final List<Swatch> swatches = new ArrayList<>();
	private Slider dragging;

	protected LbScreen(Component title, Screen parent) {
		super(title);
		this.parent = parent;
	}

	// ------------------------------------------------------------ Schieberegler

	protected static final class Slider {
		final int x, y, w, h;
		final double min, max, step;
		final DoubleSupplier get;
		final DoubleConsumer set;
		final DoubleFunction<String> text;

		Slider(int x, int y, int w, int h, double min, double max, double step, DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> text) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			this.min = min;
			this.max = max;
			this.step = step;
			this.get = get;
			this.set = set;
			this.text = text;
		}

		boolean hit(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}

		void apply(double mx) {
			double f = Math.max(0, Math.min(1, (mx - (x + 4)) / (double) (w - 8)));
			double v = min + f * (max - min);
			if (step > 0) v = Math.round(v / step) * step;
			set.accept(Math.max(min, Math.min(max, v)));
		}
	}

	private record Swatch(Button button, IntSupplier color) {}

	/** Unterklassen rufen super.init() als Erstes auf (auch bei rebuildWidgets()). */
	@Override
	protected void init() {
		sliders.clear();
		swatches.clear();
		dragging = null;
	}

	protected void slider(int x, int y, int w, double min, double max, double step,
			DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> text) {
		sliders.add(new Slider(x, y, w, 20, min, max, step, get, v -> {
			set.accept(v);
			changed();
		}, text));
	}

	protected Button toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
		Button b = Button.builder(Component.literal(onOff(label, get.getAsBoolean())), btn -> {
			set.accept(!get.getAsBoolean());
			btn.setMessage(Component.literal(onOff(label, get.getAsBoolean())));
			changed();
		}).bounds(x, y, w, 20).build();
		return addRenderableWidget(b);
	}

	protected <T> Button cycle(int x, int y, int w, String label, List<T> values, Supplier<T> get, Consumer<T> set, Function<T, String> fmt) {
		Button b = Button.builder(Component.literal(label + ": " + fmt.apply(get.get())), btn -> {
			int i = values.indexOf(get.get());
			int dir = hasShift() ? -1 : 1;
			T next = values.get(Math.floorMod(i + dir, values.size()));
			set.accept(next);
			btn.setMessage(Component.literal(label + ": " + fmt.apply(get.get())));
			changed();
		}).bounds(x, y, w, 20).build();
		return addRenderableWidget(b);
	}

	protected Button action(int x, int y, int w, String label, Runnable run) {
		return addRenderableWidget(Button.builder(Component.literal(label), b -> run.run()).bounds(x, y, w, 20).build());
	}

	protected Button color(int x, int y, int w, String label, IntSupplier get, IntConsumer set) {
		Button b = action(x, y, w, label, () -> this.minecraft.setScreenAndShow(new ColorPickerScreen(this, label, get.getAsInt(), c -> {
			set.accept(c);
			changed();
		})));
		swatches.add(new Swatch(b, get));
		return b;
	}

	protected static String onOff(String label, boolean on) {
		return label + ": " + (on ? "An" : "Aus");
	}

	protected static boolean hasShift() {
		return net.minecraft.client.Minecraft.getInstance().hasShiftDown();
	}

	/** Nach jeder Änderung speichern. */
	protected void changed() {
		LogbuchConfig.save();
	}

	// ------------------------------------------------------------ Zeichnen / Eingabe

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		for (Slider s : sliders) {
			boolean hover = s.hit(mouseX, mouseY) || s == dragging;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? SLIDER_HL : SLIDER, s.x, s.y, s.w, s.h);
			double v = s.get.getAsDouble();
			double f = s.max == s.min ? 0 : (v - s.min) / (s.max - s.min);
			int hx = s.x + (int) Math.round(f * (s.w - 8));
			g.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? HANDLE_HL : HANDLE, hx, s.y, 8, s.h);
			String t = s.text.apply(v);
			g.centeredText(this.font, t, s.x + s.w / 2, s.y + (s.h - 8) / 2, 0xFFFFFFFF);
		}
		for (Swatch sw : swatches) {
			Button b = sw.button();
			int sx = b.getX() + b.getWidth() - 16;
			int sy = b.getY() + 4;
			g.fill(sx - 1, sy - 1, sx + 13, sy + 13, 0xFF000000);
			g.fill(sx, sy, sx + 12, sy + 12, sw.color().getAsInt() | 0xFF000000);
		}
		extractForeground(g, mouseX, mouseY, delta);
	}

	/** Für Unterklassen: alles, was über den Widgets liegen soll. */
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0) {
			for (Slider s : sliders) {
				if (s.hit(event.x(), event.y())) {
					dragging = s;
					s.apply(event.x());
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			dragging.apply(event.x());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		LogbuchConfig.save();
		this.minecraft.setScreenAndShow(parent);
	}
}
