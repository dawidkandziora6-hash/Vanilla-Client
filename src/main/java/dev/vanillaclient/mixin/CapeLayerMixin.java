package dev.vanillaclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.vanillaclient.cosmetics.Cosmetics;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla draws capes fully solid. Our capes have shaped edges (V-cut, rags,
 * swallowtail…), so they are drawn with cutout transparency instead.
 */
@Mixin(CapeLayer.class)
public class CapeLayerMixin {
	@WrapOperation(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entitySolid(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
	private RenderType vc$shapedCape(Identifier tex, Operation<RenderType> original) {
		return Cosmetics.isOurs(tex) ? RenderTypes.entityCutout(tex) : original.call(tex);
	}
}
