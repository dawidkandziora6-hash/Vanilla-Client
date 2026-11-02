package dev.aatracker;

/** Tiny color helpers. */
final class Theme {
	static final int ACCENT = 0xFFFFD34D;

	private Theme() {}

	static float easeOut(float t) {
		t = Math.max(0f, Math.min(1f, t));
		return 1f - (1f - t) * (1f - t) * (1f - t);
	}

	static int alpha(int argb, float a) {
		int base = (argb >>> 24) & 0xFF;
		return ((int) (base * Math.max(0f, Math.min(1f, a))) << 24) | (argb & 0x00FFFFFF);
	}

	static int lerpColor(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
				| ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
	}
}
