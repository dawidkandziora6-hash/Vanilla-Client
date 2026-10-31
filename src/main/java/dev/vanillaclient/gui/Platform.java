package dev.vanillaclient.gui;

import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.blaze3d.Blaze3D;
import dev.vanillaclient.config.Config;

/** Opens a folder in the system file explorer. */
final class Platform {
	private Platform() {}

	static void open(Path dir) {
		try {
			Files.createDirectories(dir);
			Blaze3D.openUri(dir.toUri());
		} catch (Throwable t) {
			Config.LOG.warn("Could not open folder: {}", dir, t);
		}
	}
}
