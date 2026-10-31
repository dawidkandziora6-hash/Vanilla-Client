package dev.vanillaclient.hud;

import dev.vanillaclient.config.Config;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/** A movable, scalable HUD element. */
public abstract class HudModule {
	public final String id;
	public final String name;

	/** Last drawn area in GUI pixels (used by the HUD editor). */
	public int bx, by, bw, bh;
	public boolean drawnLastFrame;

	protected HudModule(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public abstract Config.Hud settings();

	/** Unscaled width/height. */
	public abstract int width(boolean editing);

	public abstract int height(boolean editing);

	/** Draws at (0,0); moving and scaling is done by the HudManager. */
	public abstract void draw(GuiGraphicsExtractor g, boolean editing);

	/** Icon shown in the HUD editor. */
	public abstract ItemStack icon();

	/** Whether there is something to show right now (the editor always shows a preview). */
	public boolean hasContent() {
		return true;
	}

	/** true = the module decides its own position (e.g. docked to the hotbar). */
	public boolean fixedPosition() {
		return false;
	}

	/** Only for fixedPosition(): top-left corner in GUI pixels. */
	public int[] fixedOrigin(int screenW, int screenH) {
		return new int[] {0, 0};
	}

	public boolean canScale() {
		return !fixedPosition();
	}

	/** Background box like the vanilla chat. */
	protected static void background(GuiGraphicsExtractor g, Config.Hud s, int w, int h) {
		if (s.background) {
			g.fill(-2, -2, w + 2, h + 2, s.backgroundColor);
		}
	}
}
