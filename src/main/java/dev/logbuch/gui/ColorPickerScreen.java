package dev.logbuch.gui;

import java.util.Locale;
import java.util.function.IntConsumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Farbwähler: Minecraft-Farben, RGB-/Deckkraft-Regler und Hex-Eingabe. */
public final class ColorPickerScreen extends LbScreen {
	/** Die 16 Chat-Farben von Minecraft. */
	private static final int[] PALETTE = {
			0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
			0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

	private final IntConsumer onDone;
	private int argb;
	private EditBox hex;
	private boolean updatingHex;

	public ColorPickerScreen(Screen parent, String what, int initial, IntConsumer onDone) {
		super(Component.literal(what), parent);
		this.argb = initial;
		this.onDone = onDone;
	}

	private int left() {
		return this.width / 2 - 100;
	}

	@Override
	protected void init() {
		super.init();
		int x = left();
		int y = 70;
		slider(x, y, 200, 0, 255, 1, () -> (argb >> 16) & 0xFF, v -> set(16, v), v -> "Rot: " + (int) v);
		slider(x, y + 24, 200, 0, 255, 1, () -> (argb >> 8) & 0xFF, v -> set(8, v), v -> "Grün: " + (int) v);
		slider(x, y + 48, 200, 0, 255, 1, () -> argb & 0xFF, v -> set(0, v), v -> "Blau: " + (int) v);
		slider(x, y + 72, 200, 0, 255, 1, () -> (argb >>> 24) & 0xFF, v -> set(24, v),
				v -> "Deckkraft: " + Math.round(v / 2.55) + "%");

		hex = new EditBox(this.font, x + 1, y + 98, 98, 18, Component.literal("Hex"));
		hex.setMaxLength(9);
		hex.setValue(hexString());
		hex.setResponder(t -> {
			if (updatingHex) return;
			String s = t.trim().replace("#", "");
			try {
				if (s.length() == 6) argb = (argb & 0xFF000000) | Integer.parseInt(s, 16);
				else if (s.length() == 8) argb = (int) Long.parseLong(s, 16);
			} catch (NumberFormatException ignored) {
				// Erst übernehmen, wenn die Eingabe gültig ist.
			}
		});
		addRenderableWidget(hex);

		action(this.width / 2 - 100, this.height - 28, 98, "Abbrechen", this::onClose);
		action(this.width / 2 + 2, this.height - 28, 98, "Fertig", () -> {
			onDone.accept(argb);
			onClose();
		});
	}

	private void set(int shift, double v) {
		int c = (int) v & 0xFF;
		argb = (argb & ~(0xFF << shift)) | (c << shift);
		if (hex != null) {
			updatingHex = true;
			hex.setValue(hexString());
			updatingHex = false;
		}
	}

	private String hexString() {
		return String.format(Locale.ROOT, "#%08X", argb);
	}

	@Override
	protected void changed() {
		// Erst mit "Fertig" speichern.
	}

	private int paletteX(int i) {
		return left() + (i % 8) * 25 + 1;
	}

	private int paletteY(int i) {
		return 30 + (i / 8) * 18;
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		for (int i = 0; i < PALETTE.length; i++) {
			int px = paletteX(i);
			int py = paletteY(i);
			boolean hover = mouseX >= px && mouseX < px + 22 && mouseY >= py && mouseY < py + 14;
			g.fill(px - 1, py - 1, px + 23, py + 15, hover ? 0xFFFFFFFF : 0xFF000000);
			g.fill(px, py, px + 22, py + 14, 0xFF000000 | PALETTE[i]);
		}
		// Vorschau (Schachbrett zeigt die Deckkraft).
		int vx = left() + 104;
		int vy = 168;
		for (int yy = 0; yy < 18; yy += 6) {
			for (int xx = 0; xx < 96; xx += 6) {
				g.fill(vx + xx, vy + yy, vx + xx + 6, vy + yy + 6, ((xx + yy) / 6) % 2 == 0 ? 0xFF999999 : 0xFF666666);
			}
		}
		g.fill(vx, vy, vx + 96, vy + 18, argb);
		g.text(this.font, "Shift-Klick auf Knöpfe = rückwärts", left(), this.height - 44, 0xFF808080, false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int i = 0; i < PALETTE.length; i++) {
			int px = paletteX(i);
			int py = paletteY(i);
			if (event.x() >= px && event.x() < px + 22 && event.y() >= py && event.y() < py + 14) {
				argb = (argb & 0xFF000000) | PALETTE[i];
				if ((argb >>> 24) == 0) argb |= 0xFF000000;
				updatingHex = true;
				hex.setValue(hexString());
				updatingHex = false;
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
}
