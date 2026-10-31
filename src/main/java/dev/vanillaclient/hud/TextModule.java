package dev.vanillaclient.hud;

import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** One-line HUD element: "[label] value[suffix]" with its own label color. */
public abstract class TextModule extends HudModule {
	protected TextModule(String id, String name) {
		super(id, name);
	}

	@Override
	public abstract Config.TextHud settings();

	/** Current value; editing = show a sample if there is nothing real. */
	protected abstract String value(boolean editing);

	protected String labelText() {
		Config.TextHud s = settings();
		if (!s.showLabel || s.label == null || s.label.isBlank()) return "";
		return s.label + " ";
	}

	private String valueText(boolean editing) {
		String v = value(editing);
		String suf = settings().suffix;
		return v + (suf == null ? "" : suf);
	}

	@Override
	public boolean hasContent() {
		return value(false) != null;
	}

	@Override
	public int width(boolean editing) {
		Font f = Minecraft.getInstance().font;
		String v = value(editing);
		return f.width(labelText()) + f.width(v == null ? "-" : valueText(editing));
	}

	@Override
	public int height(boolean editing) {
		return 8;
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		Config.TextHud s = settings();
		Font f = Minecraft.getInstance().font;
		String v = value(editing);
		String l = labelText();
		background(g, s, width(editing), 8);
		g.text(f, l, 0, 0, s.labelColor, s.shadow);
		g.text(f, v == null ? "-" : valueText(editing), f.width(l), 0, s.color, s.shadow);
	}
}
