package dev.vanillaclient.hud;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** "☀ Day 37 · 14:20" in a corner. The sun turns into a moon at night. */
public final class DaysModule extends HudModule {
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

	private String text() {
		Config.Days s = settings();
		String label = s.label == null || s.label.isBlank() ? "Day" : s.label.trim();
		String out = label + " " + WorldTime.day(s.countFromOne);
		if (s.showTime) {
			out += " §7·§r " + WorldTime.clock();
		}
		return out;
	}

	private int iconW() {
		return settings().showIcon ? 11 : 0;
	}

	@Override
	public int width(boolean editing) {
		return iconW() + Minecraft.getInstance().font.width(text());
	}

	@Override
	public int height(boolean editing) {
		return 8;
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		Config.Days s = settings();
		Font font = Minecraft.getInstance().font;
		String t = text();
		background(g, s, width(editing), 8);
		if (s.showIcon) {
			String sym = WorldTime.isNight() ? "☽" : "☀";
			int col = WorldTime.isNight() ? 0xFFB8C4FF : 0xFFFFD34D;
			g.text(font, sym, 0, 0, col, s.shadow);
		}
		g.text(font, t, iconW(), 0, s.color, s.shadow);
	}
}
