package dev.logbuch.mixin;

import dev.logbuch.zoom.Zoom;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Zoom: skaliert nur die Projektionsmatrix (reine Darstellung). Spieler-FOV,
 * Optionen und Spielmechanik bleiben unberührt.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@ModifyVariable(method = "renderLevel", at = @At("STORE"), ordinal = 0)
	private Matrix4f logbuch$zoomProjection(Matrix4f projection) {
		float factor = Zoom.frameFactor();
		if (factor > 1.0001f) {
			float s = factor;
			projection.scale(s, s, 1.0f);
		}
		return projection;
	}
}
