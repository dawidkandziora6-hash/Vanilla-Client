package dev.vanillaclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A tiny square button that only shows an item icon (tooltip explains it). */
public final class IconButton extends AbstractButton {
	private final ItemStack icon;
	private final Runnable action;

	public IconButton(int x, int y, int size, ItemStack icon, String tooltip, Runnable action) {
		super(x, y, size, size, Component.literal(tooltip));
		this.icon = icon;
		this.action = action;
		setTooltip(Tooltip.create(Component.literal(tooltip)));
	}

	@Override
	public void onPress(InputWithModifiers input) {
		action.run();
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		int x = getX(), y = getY(), s = getWidth();
		g.fill(x, y, x + s, y + s, this.isHovered ? 0xFF6A6A6A : 0xFF8B8B8B);
		g.fill(x, y, x + s, y + 1, 0xFF373737);
		g.fill(x, y, x + 1, y + s, 0xFF373737);
		g.fill(x + 1, y + s - 1, x + s, y + s, 0xFFFFFFFF);
		g.fill(x + s - 1, y + 1, x + s, y + s, 0xFFFFFFFF);
		float scale = (s - 2) / 16f;
		Icons.scaled(g, icon, x + 1, y + 1, scale);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput out) {
		defaultButtonNarrationText(out);
	}
}
