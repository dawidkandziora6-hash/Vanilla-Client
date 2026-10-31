package dev.vanillaclient.mixin;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Gives the cosmetics preview access to the vanilla player render state. */
@Mixin(InventoryScreen.class)
public interface InventoryScreenInvoker {
	@Invoker("extractRenderState")
	static EntityRenderState vc$extractRenderState(LivingEntity entity) {
		throw new AssertionError();
	}
}
