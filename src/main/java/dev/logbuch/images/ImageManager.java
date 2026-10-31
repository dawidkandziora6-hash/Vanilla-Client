package dev.logbuch.images;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.NativeImage;
import dev.logbuch.config.LogbuchConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Bilder vom PC (config/logbuch/bilder) als GUI-Texturen. PNG und JPG werden
 * unterstützt; sehr große Bilder werden auf max. 1024 px verkleinert.
 */
public final class ImageManager {
	public static final int MAX_SIDE = 1024;

	public record Tex(Identifier id, int width, int height) {}

	private static final Map<String, Tex> CACHE = new HashMap<>();
	private static final Map<String, Boolean> FAILED = new HashMap<>();
	private static int generation;

	private ImageManager() {}

	public static boolean isImage(Path p) {
		String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
		return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
	}

	/** Gleicht die Bilderliste der Einstellungen mit dem Ordner ab. */
	public static void scan() {
		LogbuchConfig cfg = LogbuchConfig.get();
		List<String> files = new ArrayList<>();
		try {
			Files.createDirectories(LogbuchConfig.imagesDir());
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(LogbuchConfig.imagesDir())) {
				for (Path p : ds) {
					if (Files.isRegularFile(p) && isImage(p)) files.add(p.getFileName().toString());
				}
			}
		} catch (IOException e) {
			LogbuchConfig.LOG.warn("Bilder-Ordner nicht lesbar", e);
		}
		files.sort(String.CASE_INSENSITIVE_ORDER);
		cfg.images.removeIf(img -> !files.contains(img.file));
		int i = 0;
		for (String f : files) {
			boolean known = cfg.images.stream().anyMatch(img -> img.file.equals(f));
			if (!known) {
				LogbuchConfig.Image img = new LogbuchConfig.Image();
				img.file = f;
				img.enabled = false;
				img.scale = -1f;
				img.x = 0.5f;
				img.y = 0.3f + 0.05f * (i++ % 6);
				cfg.images.add(img);
			}
		}
		LogbuchConfig.save();
	}

	/** Alle Texturen beim nächsten Zeichnen neu laden. */
	public static void reload() {
		CACHE.clear();
		FAILED.clear();
		generation++;
		scan();
	}

	/** Kopiert Dateien (z. B. per Drag & Drop) in den Bilder-Ordner. */
	public static int importFiles(List<Path> paths) {
		int n = 0;
		for (Path p : paths) {
			if (!Files.isRegularFile(p) || !isImage(p)) continue;
			try {
				Files.copy(p, LogbuchConfig.imagesDir().resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
				CACHE.remove(p.getFileName().toString());
				FAILED.remove(p.getFileName().toString());
				n++;
			} catch (IOException e) {
				LogbuchConfig.LOG.warn("Konnte {} nicht kopieren", p, e);
			}
		}
		if (n > 0) {
			generation++;
			scan();
		}
		return n;
	}

	/** Muss auf dem Render-Thread aufgerufen werden. */
	public static Tex texture(String file) {
		Tex t = CACHE.get(file);
		if (t != null || FAILED.containsKey(file)) return t;
		try {
			BufferedImage src = ImageIO.read(LogbuchConfig.imagesDir().resolve(file).toFile());
			if (src == null) throw new IOException("Unbekanntes Bildformat");
			BufferedImage img = shrink(src);
			ByteArrayOutputStream png = new ByteArrayOutputStream();
			ImageIO.write(img, "png", png);
			NativeImage nat = NativeImage.read(new ByteArrayInputStream(png.toByteArray()));
			Identifier id = Identifier.fromNamespaceAndPath("logbuch", "image/g" + generation + "_" + Integer.toHexString(file.hashCode()));
			DynamicTexture tex = new DynamicTexture(() -> "logbuch:" + file, nat);
			Minecraft.getInstance().getTextureManager().register(id, tex);
			t = new Tex(id, img.getWidth(), img.getHeight());
			CACHE.put(file, t);
		} catch (Exception e) {
			LogbuchConfig.LOG.warn("Bild {} konnte nicht geladen werden", file, e);
			FAILED.put(file, true);
		}
		return t;
	}

	private static BufferedImage shrink(BufferedImage src) {
		int w = src.getWidth();
		int h = src.getHeight();
		double f = Math.min(1.0, MAX_SIDE / (double) Math.max(w, h));
		int nw = Math.max(1, (int) Math.round(w * f));
		int nh = Math.max(1, (int) Math.round(h * f));
		BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(src, 0, 0, nw, nh, null);
		g.dispose();
		return out;
	}
}
