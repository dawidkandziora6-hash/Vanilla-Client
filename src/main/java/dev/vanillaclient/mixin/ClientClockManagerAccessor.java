package dev.vanillaclient.mixin;

import java.util.Map;

import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only: the client's world clocks (for the day counter). */
@Mixin(ClientClockManager.class)
public interface ClientClockManagerAccessor {
	@Accessor("clocks")
	@SuppressWarnings("rawtypes")
	Map vc$getClocks();
}
