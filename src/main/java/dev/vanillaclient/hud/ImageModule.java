package dev.vanillaclient.hud;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.images.ImageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** A picture from your PC as a HUD element (e.g. a build plan or farm sketch). */
public final class ImageModule extends HudModule {
	private final Config.Image image;

	public ImageModule(Config.Image image) {
		super("image:" + image.file, image.file);
		this.image = image;
	}

	public Config.Image image() {
		return image;
	}

	@Override
	public Config.Image settings() {
		return image;
	}

	private ImageManager.Tex tex() {
		ImageManager.Tex t = ImageManager.texture(image.file);
		if (t != null && image.scale <= 0f) {
			// Start size: about 120 GUI pixels on the longest side.
			image.scale = 120f / Math.max(t.width(), t.height());
		}
		return t;
	}

	@Override
	public net.minecraft.world.item.ItemStack icon() {
		return dev.vanillaclient.gui.Icons.stack(net.minecraft.world.item.Items.PAINTING);
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
