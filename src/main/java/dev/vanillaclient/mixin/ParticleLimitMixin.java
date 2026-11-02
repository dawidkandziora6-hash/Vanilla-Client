package dev.vanillaclient.mixin;

import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Performance: fewer particles (far away, explosions, your own potion swirls). */
@Mixin(ParticleEngine.class)
public class ParticleLimitMixin {
	@Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
	private void vc$limit(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
		Config.Performance p = Config.get().performance;
		var type = options.getType();
		if (p.noExplosionParticles && (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER)) {
			cir.setReturnValue(null);
			return;
		}
		var pl = Minecraft.getInstance().player;
		if (pl == null) return;
		double d2 = pl.distanceToSqr(x, y, z);
		if (p.hideOwnPotionParticles && Minecraft.getInstance().options.getCameraType().isFirstPerson()
				&& (type == ParticleTypes.ENTITY_EFFECT || type == ParticleTypes.EFFECT || type == ParticleTypes.INSTANT_EFFECT) && d2 < 2.5) {
			cir.setReturnValue(null);
			return;
		}
		if (p.particleDistance > 0 && d2 > (double) p.particleDistance * p.particleDistance) cir.setReturnValue(null);
	}
}
