package dev.logbuch.hud;

import dev.logbuch.config.LogbuchConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Ein verschiebbares, skalierbares HUD-Element. */
public abstract class HudModule {
	public final String id;
	public final String name;

	/** Zuletzt gezeichnete Fläche in GUI-Pixeln (für den HUD-Editor). */
	public int bx, by, bw, bh;
	public boolean drawnLastFrame;

	protected HudModule(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public abstract LogbuchConfig.Hud settings();

	/** Unskalierte Breite/Höhe. */
	public abstract int width(boolean editing);

	public abstract int height(boolean editing);

	/** Zeichnet bei (0,0); Verschiebung und Skalierung macht der HudManager. */
	public abstract void draw(GuiGraphicsExtractor g, boolean editing);

	/** Ob es gerade etwas zu zeigen gibt (im Editor wird immer eine Vorschau gezeigt). */
	public boolean hasContent() {
		return true;
	}

	/** true = Position wird vom Modul selbst bestimmt (z. B. an der Hotbar angedockt). */
	public boolean fixedPosition() {
		return false;
	}

	/** Nur für fixedPosition(): obere linke Ecke in GUI-Pixeln. */
	public int[] fixedOrigin(int screenW, int screenH) {
		return new int[] {0, 0};
	}

	public boolean canScale() {
		return !fixedPosition();
	}

	public boolean canRecolor() {
		return true;
	}

	/** Hintergrundbox wie im Vanilla-Chat. */
	protected static void background(GuiGraphicsExtractor g, LogbuchConfig.Hud s, int w, int h) {
		if (s.background) {
			g.fill(-2, -2, w + 2, h + 2, s.backgroundColor);
		}
	}
}
