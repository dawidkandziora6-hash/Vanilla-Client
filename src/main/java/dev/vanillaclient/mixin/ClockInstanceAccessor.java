package dev.vanillaclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only: total ticks of a world clock (overworld clock = day/night). */
@Mixin(targets = "net.minecraft.client.ClientClockManager$ClientClockInstance")
public interface ClockInstanceAccessor {
	@Accessor("totalTicks")
	long vc$getTotalTicks();
}
