package dev.logbuch.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Nur lesend: Gesamt-Ticks einer Welt-Uhr (Überwelt-Uhr = Tag/Nacht). */
@Mixin(targets = "net.minecraft.client.ClientClockManager$ClientClockInstance")
public interface ClockInstanceAccessor {
	@Accessor("totalTicks")
	long logbuch$getTotalTicks();
}
