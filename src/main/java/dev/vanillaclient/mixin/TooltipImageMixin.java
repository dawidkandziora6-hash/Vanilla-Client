package dev.vanillaclient.mixin;

import java.util.Optional;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shulker box / ender chest contents as a picture in the tooltip. */
@Mixin(ItemStack.class)
public class TooltipImageMixin {
	@Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
	private void vc$preview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
		if (!net.minecraft.client.Minecraft.getInstance().isSameThread()) return;
		Optional<TooltipComponent> o = dev.vanillaclient.features.ContainerPreview.image((ItemStack) (Object) this);
		if (o.isPresent()) cir.setReturnValue(o);
	}
}
