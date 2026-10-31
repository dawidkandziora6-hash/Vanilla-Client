package dev.logbuch.hud;

import dev.logbuch.config.LogbuchConfig;
import dev.logbuch.images.ImageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Ein Bild vom PC als HUD-Element (z. B. Bauplan oder Farm-Skizze). */
public final class ImageModule extends HudModule {
	private final LogbuchConfig.Image image;

	public ImageModule(LogbuchConfig.Image image) {
		super("image:" + image.file, image.file);
		this.image = image;
	}

	public LogbuchConfig.Image image() {
		return image;
	}

	@Override
	public LogbuchConfig.Image settings() {
		return image;
	}

	private ImageManager.Tex tex() {
		ImageManager.Tex t = ImageManager.texture(image.file);
		if (t != null && image.scale <= 0f) {
			// Startgröße: etwa 120 GUI-Pixel an der längsten Seite.
			image.scale = 120f / Math.max(t.width(), t.height());
		}
		return t;
	}

	@Override
	public boolean hasContent() {
		return tex() != null;
	}

	@Override
	public int width(boolean editing) {
		ImageManager.Tex t = tex();
		return t == null ? 32 : t.width();
	}

	@Override
	public int height(boolean editing) {
		ImageManager.Tex t = tex();
		return t == null ? 32 : t.height();
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		ImageManager.Tex t = tex();
		if (t == null) {
			g.fill(0, 0, 32, 32, 0x80FF0000);
			g.text(Minecraft.getInstance().font, "?", 13, 12, 0xFFFFFFFF, true);
			return;
		}
		int w = t.width();
		int h = t.height();
		g.blit(RenderPipelines.GUI_TEXTURED, t.id(), 0, 0, 0f, 0f, w, h, w, h, w, h, image.tint);
	}
}
