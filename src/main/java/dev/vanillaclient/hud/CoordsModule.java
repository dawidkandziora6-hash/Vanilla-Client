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

/** X / Y / Z, facing direction and the matching Nether/Overworld position. */
public final class CoordsModule extends HudModule {
	private static final String[] DIRS = {"South", "South-West", "West", "North-West", "North", "North-East", "East", "South-East"};
	private static final String[] SHORT = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
	private static final String[] AXIS = {"+Z", "", "-X", "", "-Z", "", "+X", ""};
	private static final String[] ARROWS = {"↓", "↙", "←", "↖", "↑", "↗", "→", "↘"};

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

	private List<String> lines(boolean editing) {
		Config.Coords s = settings();
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		int x = p == null ? 128 : p.getBlockX();
		int y = p == null ? 64 : p.getBlockY();
		int z = p == null ? -256 : p.getBlockZ();
		float yaw = p == null ? 180f : p.getYRot();
		int dir = Math.floorMod(Math.round(yaw / 45f), 8);
		String l = s.coloredLabels ? "§7" : "";
		String r = "§r";
		List<String> out = new ArrayList<>();
		if (s.compact) {
			StringBuilder b = new StringBuilder();
			b.append(l).append("XYZ ").append(r).append(x).append(" ").append(y).append(" ").append(z);
			if (s.showDirection) b.append(" ").append(l).append(ARROWS[dir]).append(" ").append(SHORT[dir]).append(r);
			out.add(b.toString());
		} else {
			out.add(l + "X: " + r + x);
			out.add(l + "Y: " + r + y);
			out.add(l + "Z: " + r + z);
			if (s.showDirection) {
				String axis = AXIS[dir].isEmpty() ? "" : " " + l + "(" + AXIS[dir] + ")" + r;
				out.add(l + ARROWS[dir] + " " + r + DIRS[dir] + axis);
			}
		}
		if (s.showOtherDimension && mc.level != null) {
			if (mc.level.dimension() == Level.OVERWORLD) {
				out.add(l + "Nether: " + r + "§c" + Math.floorDiv(x, 8) + " " + Math.floorDiv(z, 8));
			} else if (mc.level.dimension() == Level.NETHER) {
				out.add(l + "Overworld: " + r + "§a" + (x * 8) + " " + (z * 8));
			}
		} else if (s.showOtherDimension && editing) {
			out.add(l + "Nether: " + r + "§c16 -32");
		}
		if (s.showChunk) {
			out.add(l + "Chunk: " + r + Math.floorDiv(x, 16) + " " + Math.floorDiv(z, 16)
					+ l + " in " + r + Math.floorMod(x, 16) + " " + Math.floorMod(z, 16));
		}
		return out;
	}

	@Override
	public int width(boolean editing) {
		Font font = Minecraft.getInstance().font;
		int w = 0;
		for (String s : lines(editing)) w = Math.max(w, font.width(s));
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
		for (String line : lines(editing)) {
			g.text(font, line, 0, y, s.color, s.shadow);
			y += 10;
		}
	}
}
