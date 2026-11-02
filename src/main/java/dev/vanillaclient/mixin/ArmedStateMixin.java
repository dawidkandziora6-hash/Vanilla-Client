package dev.vanillaclient.mixin;

import dev.vanillaclient.cosmetics.FakeShield;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Puts your shield design into the empty off hand (only for how it's drawn – nothing is in your inventory). */
@Mixin(ArmedEntityRenderState.class)
public class ArmedStateMixin {
	@Inject(method = "extractArmedEntityRenderState", at = @At("TAIL"))
	private static void vc$fakeShield(LivingEntity entity, ArmedEntityRenderState state, ItemModelResolver resolver, float partial, CallbackInfo ci) {
		HumanoidArm arm = FakeShield.armFor(entity);
		if (arm == null) return;
		if (arm == HumanoidArm.LEFT) {
			resolver.updateForLiving(state.leftHandItemState, FakeShield.stack(), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
			state.leftHandItemStack = FakeShield.stack();
		} else {
			resolver.updateForLiving(state.rightHandItemState, FakeShield.stack(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
			state.rightHandItemStack = FakeShield.stack();
		}
	}
}
