package dev.logbuch.mixin;

import java.util.Map;

import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Nur lesend: liefert die Welt-Uhren des Clients (für den Tageszähler). */
@Mixin(ClientClockManager.class)
public interface ClientClockManagerAccessor {
	@Accessor("clocks")
	@SuppressWarnings("rawtypes")
	Map logbuch$getClocks();
}
