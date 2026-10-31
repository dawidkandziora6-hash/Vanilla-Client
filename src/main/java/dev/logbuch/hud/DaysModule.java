package dev.logbuch.hud;

import dev.logbuch.config.LogbuchConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** "Tag 37 · 14:20" oben in der Ecke. */
public final class DaysModule extends HudModule {
	public DaysModule() {
		super("days", "Tage");
	}

	@Override
	public LogbuchConfig.Days settings() {
		return LogbuchConfig.get().days;
	}

	private String text() {
		LogbuchConfig.Days s = settings();
		String label = s.label == null || s.label.isBlank() ? "Tag" : s.label.trim();
		String out = label + " " + WorldTime.day(s.countFromOne);
		if (s.showTime) {
			out += " §7·§r " + WorldTime.clock();
		}
		return out;
	}

	@Override
	public int width(boolean editing) {
		return Minecraft.getInstance().font.width(text());
	}

	@Override
	public int height(boolean editing) {
		return 8;
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		LogbuchConfig.Days s = settings();
		Font font = Minecraft.getInstance().font;
		String t = text();
		background(g, s, font.width(t), 8);
		g.text(font, t, 0, 0, s.color, s.shadow);
	}
}
