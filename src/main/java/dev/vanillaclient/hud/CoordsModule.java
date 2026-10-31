package dev.vanillaclient.hud;

import java.util.ArrayList;
import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** X / Y / Z, facing direction and the matching Nether/Overworld position – every label is your own text. */
public final class CoordsModule extends HudModule {
	private static final String[] DIRS = {"South", "South-West", "West", "North-West", "North", "North-East", "East", "South-East"};
	private static final String[] SHORT = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	private static final String[] AXIS = {"+Z", "", "-X", "", "-Z", "", "+X", ""};
	private static final String[] ARROWS = {"↓", "↙", "←", "↖", "↑", "↗", "→", "↘"};

	/** Piece of a line: label pieces use the label color. */
	private record Seg(String text, boolean label, int color) {}

	public CoordsModule() {
		super("coords", "Coordinates");
	}

	@Override
	public Config.Coords settings() {
		return Config.get().coords;
	}

	@Override
	public ItemStack icon() {
		return Icons.stack(Items.COMPASS);
	}

	private static String lbl(String custom, String fallback) {
		return custom == null || custom.isBlank() ? fallback : custom;
	}

	private long cachedFrame = -1;
	private boolean cachedEditing;
	private List<List<Seg>> cached;

	private List<List<Seg>> lines(boolean editing) {
		if (cached != null && cachedFrame == HudManager.frame && cachedEditing == editing && !editing) return cached;
		cached = compute(editing);
		cachedFrame = HudManager.frame;
		cachedEditing = editing;
		return cached;
	}

	private List<List<Seg>> compute(boolean editing) {
		Config.Coords s = settings();
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		int x = p == null ? 128 : p.getBlockX();
		int y = p == null ? 64 : p.getBlockY();
		int z = p == null ? -256 : p.getBlockZ();
		float yaw = p == null ? 180f : p.getYRot();
		int dir = Math.floorMod(Math.round(yaw / 45f), 8);
		List<List<Seg>> out = new ArrayList<>();
		if (s.compact) {
			List<Seg> l = new ArrayList<>();
			l.add(new Seg(lbl(s.labelX, "X") + lbl(s.labelY, "Y") + lbl(s.labelZ, "Z") + " ", true, 0));
			l.add(new Seg(x + " " + y + " " + z, false, 0));
			if (s.showDirection) l.add(new Seg(" " + ARROWS[dir] + " " + (s.labelFacing.isBlank() ? "" : s.labelFacing + " ") + SHORT[dir], true, 0));
			out.add(l);
		} else {
			out.add(List.of(new Seg(lbl(s.labelX, "X") + ": ", true, 0), new Seg(String.valueOf(x), false, 0)));
			out.add(List.of(new Seg(lbl(s.labelY, "Y") + ": ", true, 0), new Seg(String.valueOf(y), false, 0)));
			out.add(List.of(new Seg(lbl(s.labelZ, "Z") + ": ", true, 0), new Seg(String.valueOf(z), false, 0)));
			if (s.showDirection) {
				List<Seg> l = new ArrayList<>();
				l.add(new Seg(ARROWS[dir] + " " + (s.labelFacing.isBlank() ? "" : s.labelFacing + ": "), true, 0));
				l.add(new Seg(DIRS[dir], false, 0));
				if (!AXIS[dir].isEmpty()) l.add(new Seg(" (" + AXIS[dir] + ")", true, 0));
				out.add(l);
			}
		}
		boolean nether = mc.level != null && mc.level.dimension() == Level.NETHER;
		boolean over = mc.level == null || mc.level.dimension() == Level.OVERWORLD;
		if (s.showOtherDimension && (over || nether || editing)) {
			if (!nether) {
				out.add(List.of(new Seg(lbl(s.labelOther, "Nether") + ": ", true, 0),
						new Seg(Math.floorDiv(x, 8) + " " + Math.floorDiv(z, 8), false, 0xFFFF6A5A)));
			} else {
				out.add(List.of(new Seg(lbl(s.labelOther, "Overworld") + ": ", true, 0),
						new Seg((x * 8) + " " + (z * 8), false, 0xFF6AE070)));
			}
		}
		if (s.showChunk) {
			out.add(List.of(new Seg(lbl(s.labelChunk, "Chunk") + ": ", true, 0),
					new Seg(Math.floorDiv(x, 16) + " " + Math.floorDiv(z, 16), false, 0),
					new Seg(" in ", true, 0), new Seg(Math.floorMod(x, 16) + " " + Math.floorMod(z, 16), false, 0)));
		}
		return out;
	}

	private static int lineW(Font font, List<Seg> line) {
		int w = 0;
		for (Seg sg : line) w += font.width(sg.text());
		return w;
	}

	@Override
	public int width(boolean editing) {
		Font font = Minecraft.getInstance().font;
		int w = 0;
		for (List<Seg> l : lines(editing)) w = Math.max(w, lineW(font, l));
		return w;
	}

	@Override
	public int height(boolean editing) {
		return lines(editing).size() * 10 - 2;
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		Config.Coords s = settings();
		Font font = Minecraft.getInstance().font;
		background(g, s, width(editing), height(editing));
		int y = 0;
		for (List<Seg> line : lines(editing)) {
			int x = 0;
			for (Seg sg : line) {
				int col = sg.color() != 0 ? sg.color() : sg.label() && s.coloredLabels ? s.labelColor : s.color;
				g.text(font, sg.text(), x, y, col, s.shadow);
				x += font.width(sg.text());
			}
			y += 10;
		}
	}
}
