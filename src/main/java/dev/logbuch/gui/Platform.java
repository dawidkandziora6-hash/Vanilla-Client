package dev.logbuch.gui;

import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.blaze3d.Blaze3D;
import dev.logbuch.config.LogbuchConfig;

/** Öffnet einen Ordner im Datei-Explorer des Systems. */
final class Platform {
	private Platform() {}

	static void open(Path dir) {
		try {
			Files.createDirectories(dir);
			Blaze3D.openUri(dir.toUri());
		} catch (Throwable t) {
			LogbuchConfig.LOG.warn("Ordner konnte nicht geöffnet werden: {}", dir, t);
		}
	}
}
