package dev.vanillaclient.gui;

import java.util.List;
import java.util.Random;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** The "VANILLA CLIENT" logo with a moving shine and a vanilla-style splash text. */
public final class Logo {
	public static final int W = 160, H = 18;
	private static final Identifier LOGO = Identifier.fromNamespaceAndPath("vanillaclient", "textures/gui/logo.png");
	private static final Identifier MASK = Identifier.fromNamespaceAndPath("vanillaclient", "textures/gui/logo_mask.png");
	private static final List<String> SPLASHES = List.of(
			"Hardcore ready!", "100% vanilla!", "Don't dig straight down!", "Bring a totem!",
			"Mending first!", "Plan your farms!", "Eat your carrots!", "UHC time!",
			"Craft golden apples!", "Stay frosty!", "Also try Hardcore!", "Never mine at night!");

	private final String splash = SPLASHES.get(new Random().nextInt(SPLASHES.size()));

	/** Draws the logo centered at cx with its top at y. */
	public void draw(GuiGraphicsExtractor g, Font font, int cx, int y, float scale, float intro) {
		float drop = (1f - intro) * -10f;
		g.pose().pushMatrix();
		g.pose().translate(cx - W * scale / 2f, y + drop);
		g.pose().scale(scale, scale);
		int alpha = Theme.alpha(0xFFFFFFFF, intro);
		g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0f, 0f, W, H, W, H, W, H, alpha);
		// shine: a soft white band that sweeps across every 4 seconds
		float t = (Theme.time() % 4f) / 1.2f; // 0..3.3, sweep during 0..1
		if (t <= 1f) {
			float center = -20f + t * (W + 40f);
			for (int sx = 0; sx < W; sx += 2) {
				float d = Math.abs(sx + 1 - center + (H / 2f));
				if (d > 14f) continue;
				float a = (1f - d / 14f) * 0.55f * intro;
				g.blit(RenderPipelines.GUI_TEXTURED, MASK, sx, 0, sx, 0f, 2, H, 2, H, W, H, Theme.alpha(0xFFFFFFFF, a));
			}
		}
		g.pose().popMatrix();

		// splash (yellow, tilted, pulsing – like the title screen)
		float pulse = 1.0f + 0.06f * (float) Math.abs(Math.sin(Theme.time() * 4.0));
		g.pose().pushMatrix();
		g.pose().translate(cx + W * scale / 2f - 6, y + H * scale - 2 + drop);
		g.pose().rotate((float) Math.toRadians(-14));
		g.pose().scale(0.75f * pulse, 0.75f * pulse);
		g.centeredText(font, splash, 0, -4, Theme.alpha(0xFFFFFF00, intro));
		g.pose().popMatrix();
	}
}
