package dev.vanillaclient.gui;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Mouse button ids. Minecraft 26.3 uses a new input library, so the left button
 * is NOT 0 any more – always compare against these constants.
 */
public final class Mouse {
	public static final int LEFT = InputConstants.MOUSE_BUTTON_LEFT;
	public static final int RIGHT = InputConstants.MOUSE_BUTTON_RIGHT;
	public static final int MIDDLE = InputConstants.MOUSE_BUTTON_MIDDLE;

	private Mouse() {}
}
