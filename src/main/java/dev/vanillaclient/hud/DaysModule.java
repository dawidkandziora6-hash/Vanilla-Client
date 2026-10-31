package dev.vanillaclient.hud;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Icons;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** "Day 37" – always counted from day 1, with your own label. */
public final class DaysModule extends TextModule {
	public DaysModule() {
		super("days", "Days");
	}

	@Override
	public Config.Days settings() {
		return Config.get().days;
	}

	@Override
	public ItemStack icon() {
		return Icons.stack(Items.CLOCK);
	}

	@Override
	protected String value(boolean editing) {
		return String.valueOf(WorldTime.day(true));
	}
}
