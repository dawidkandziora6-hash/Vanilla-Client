package dev.vanillaclient.config;

import java.util.List;

/** Presets for the pixel crosshair. Each returns size*size chars ('1'/'0'). */
public final class CrosshairPresets {
	public static final List<String> NAMES = List.of("Vanilla", "Dot", "Small plus", "Circle", "X", "Gap", "Chevron");

	private CrosshairPresets() {}

	public static String build(String name, int size) {
		return switch (name) {
			case "Dot" -> dot(size);
			case "Small plus" -> smallPlus(size);
			case "Circle" -> circle(size);
			case "X" -> cross(size);
			case "Gap" -> gap(size);
			case "Chevron" -> chevron(size);
			default -> vanilla(size);
		};
	}

	public static String empty(int size) {
		return "0".repeat(size * size);
	}

	/** The classic "+" (15×15 like in game). */
	public static String vanilla(int size) {
		char[] p = empty(size).toCharArray();
		int c = size / 2;
		for (int i = 0; i < size; i++) {
			p[c * size + i] = '1';
			p[i * size + c] = '1';
		}
		return new String(p);
	}

	private static String dot(int size) {
		char[] p = empty(size).toCharArray();
		int c = size / 2;
		for (int y = c - 1; y <= c + 1; y++) {
			for (int x = c - 1; x <= c + 1; x++) {
				if (inside(x, y, size)) p[y * size + x] = '1';
			}
		}
		return new String(p);
	}

	private static String smallPlus(int size) {
		char[] p = empty(size).toCharArray();
		int c = size / 2;
		int r = Math.max(1, size / 5);
		for (int i = -r; i <= r; i++) {
			set(p, size, c + i, c);
			set(p, size, c, c + i);
		}
		return new String(p);
	}

	private static String circle(int size) {
		char[] p = empty(size).toCharArray();
		double c = (size - 1) / 2.0;
		double r = size / 2.0 - 1.5;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double d = Math.hypot(x - c, y - c);
				if (Math.abs(d - r) < 0.6) p[y * size + x] = '1';
			}
		}
		set(p, size, size / 2, size / 2);
		return new String(p);
	}

	private static String cross(int size) {
		char[] p = empty(size).toCharArray();
		for (int i = 0; i < size; i++) {
			set(p, size, i, i);
			set(p, size, size - 1 - i, i);
		}
		return new String(p);
	}

	private static String gap(int size) {
		char[] p = empty(size).toCharArray();
		int c = size / 2;
		int g = Math.max(1, size / 6);
		for (int i = 0; i < size; i++) {
			if (Math.abs(i - c) > g) {
				set(p, size, i, c);
				set(p, size, c, i);
			}
		}
		set(p, size, c, c);
		return new String(p);
	}

	private static String chevron(int size) {
		char[] p = empty(size).toCharArray();
		int c = size / 2;
		for (int i = 0; i <= c; i++) {
			set(p, size, c - i, c + i);
			set(p, size, c + i, c + i);
		}
		return new String(p);
	}

	private static boolean inside(int x, int y, int size) {
		return x >= 0 && y >= 0 && x < size && y < size;
	}

	private static void set(char[] p, int size, int x, int y) {
		if (inside(x, y, size)) p[y * size + x] = '1';
	}
}
