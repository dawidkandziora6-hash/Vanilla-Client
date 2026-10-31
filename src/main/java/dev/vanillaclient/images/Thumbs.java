package dev.vanillaclient.images;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mojang.blaze3d.platform.NativeImage;
import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Small preview textures for pictures and screenshots. Files are read and shrunk
 * on a background thread; only the small result is uploaded. Least recently used
 * previews are freed.
 */
public final class Thumbs {
	public record Thumb(Identifier id, int w, int h, int fullW, int fullH) {}

	private record Pending(Path file, int[] argb, int w, int h, int fullW, int fullH, int max) {}

	private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "VanillaClient-Thumbs");
		t.setDaemon(true);
		return t;
	});
	private static final Map<String, Thumb> CACHE = new LinkedHashMap<>(64, 0.75f, true);
	private static final Map<String, Pending> READY = new ConcurrentHashMap<>();
	private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
	private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();
	private static int counter;

	private Thumbs() {}

	private static String key(Path file, int max) {
		return max + ":" + file.toAbsolutePath();
	}

	/** Preview at most max pixels wide/high, or null while it is still loading. Render thread only. */
	public static Thumb get(Path file, int max) {
		String k = key(file, max);
		Thumb t = CACHE.get(k);
		if (t != null) return t;
		Pending p = READY.remove(k);
		if (p != null) return upload(k, p);
		if (FAILED.contains(k) || !LOADING.add(k)) return null;
		IO.submit(() -> load(k, file, max));
		return null;
	}

	public static boolean failed(Path file, int max) {
		return FAILED.contains(key(file, max));
	}

	public static void forget(Path file) {
		String suffix = ":" + file.toAbsolutePath();
		CACHE.entrySet().removeIf(e -> {
			if (e.getKey().endsWith(suffix)) {
				Minecraft.getInstance().getTextureManager().release(e.getValue().id());
				return true;
			}
			return false;
		});
		FAILED.removeIf(k -> k.endsWith(suffix));
	}

	private static void load(String k, Path file, int max) {
		try (InputStream in = Files.newInputStream(file); NativeImage img = NativeImage.read(in)) {
			int fw = img.getWidth(), fh = img.getHeight();
			float s = Math.min(1f, max / (float) Math.max(fw, fh));
			int w = Math.max(1, Math.round(fw * s)), h = Math.max(1, Math.round(fh * s));
			int[] out = new int[w * h];
			// box filter: average the source pixels behind every preview pixel
			for (int y = 0; y < h; y++) {
				int y0 = (int) (y / s), y1 = Math.max(y0 + 1, Math.min(fh, (int) ((y + 1) / s)));
				for (int x = 0; x < w; x++) {
					int x0 = (int) (x / s), x1 = Math.max(x0 + 1, Math.min(fw, (int) ((x + 1) / s)));
					long a = 0, r = 0, g = 0, b = 0;
					int n = 0;
					int stepY = Math.max(1, (y1 - y0) / 3), stepX = Math.max(1, (x1 - x0) / 3);
					for (int yy = y0; yy < y1; yy += stepY) {
						for (int xx = x0; xx < x1; xx += stepX) {
							int c = img.getPixel(xx, yy);
							a += (c >>> 24) & 0xFF;
							r += (c >> 16) & 0xFF;
							g += (c >> 8) & 0xFF;
							b += c & 0xFF;
							n++;
						}
					}
					out[y * w + x] = (int) ((a / n) << 24 | (r / n) << 16 | (g / n) << 8 | (b / n));
				}
			}
			READY.put(k, new Pending(file, out, w, h, fw, fh, max));
		} catch (Exception e) {
			FAILED.add(k);
		} finally {
			LOADING.remove(k);
		}
	}

	private static Thumb upload(String k, Pending p) {
		NativeImage img = new NativeImage(p.w(), p.h(), false);
		for (int y = 0; y < p.h(); y++) {
			for (int x = 0; x < p.w(); x++) img.setPixel(x, y, p.argb()[y * p.w() + x]);
		}
		Identifier id = Identifier.fromNamespaceAndPath("vanillaclient", "thumb/" + (counter++));
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "vc thumb", img));
		Thumb t = new Thumb(id, p.w(), p.h(), p.fullW(), p.fullH());
		CACHE.put(k, t);
		while (CACHE.size() > 80) {
			var it = CACHE.entrySet().iterator();
			var e = it.next();
			Minecraft.getInstance().getTextureManager().release(e.getValue().id());
			it.remove();
		}
		return t;
	}

	public static Path screenshotsDir() {
		return Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
	}

	@SuppressWarnings("unused")
	private static void log(String s) {
		Config.LOG.debug(s);
	}
}
