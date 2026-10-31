package dev.vanillaclient.features;

import java.util.Locale;

import dev.vanillaclient.config.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** How a name looks: color, gradient, animation, bold/italic, font and badge – limited by the rank's perks. */
public record NameStyle(int color, int color2, boolean animated, boolean bold, boolean italic, String font, String badge, boolean title) {
	public static NameStyle mine() {
		Config.Ranks r = Config.get().ranks;
		return new NameStyle(r.nameColor, r.nameColor2, r.animatedName, r.boldName, r.italicName, r.nameFont, r.badge, r.showRankTitle);
	}

	/** Compact text for the sync packet. */
	public String encode() {
		return Integer.toHexString(color) + "," + Integer.toHexString(color2) + "," + (animated ? 1 : 0) + "," + (bold ? 1 : 0) + ","
				+ (italic ? 1 : 0) + "," + font + "," + badge + "," + (title ? 1 : 0);
	}

	public static NameStyle decode(String s) {
		if (s == null || s.isEmpty()) return null;
		try {
			String[] p = s.split(",", -1);
			return new NameStyle((int) Long.parseLong(p[0], 16), (int) Long.parseLong(p[1], 16), p[2].equals("1"), p[3].equals("1"),
					p[4].equals("1"), p[5], p[6].length() > 2 ? "" : p[6], p[7].equals("1"));
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static FontDescription font(String f) {
		if (f == null || f.equals("default")) return null;
		String id = switch (f.toLowerCase(Locale.ROOT)) {
			case "uniform", "alt", "illageralt" -> f.toLowerCase(Locale.ROOT);
			default -> null;
		};
		return id == null ? null : new FontDescription.Resource(Identifier.withDefaultNamespace(id));
	}

	/** Builds the full name tag for a player with this rank. */
	public MutableComponent build(String name, ClientRanks.Rank r, float time) {
		boolean gradient = ClientRanks.has(r, ClientRanks.Perk.GRADIENT) && color2 != 0;
		int c1 = ClientRanks.has(r, ClientRanks.Perk.COLOR) && color != 0 ? color & 0xFFFFFF : r.nameColor() & 0xFFFFFF;
		int c2 = gradient ? color2 & 0xFFFFFF : c1;
		boolean anim = gradient && animated && ClientRanks.has(r, ClientRanks.Perk.ANIMATED);
		boolean b = bold && ClientRanks.has(r, ClientRanks.Perk.BOLD);
		boolean it = italic && ClientRanks.has(r, ClientRanks.Perk.BOLD);
		FontDescription fd = ClientRanks.has(r, ClientRanks.Perk.FONT) ? font(font) : null;
		String bd = ClientRanks.has(r, ClientRanks.Perk.BADGE) && badge != null && !badge.isEmpty() ? badge : r.symbol();

		MutableComponent out = Component.literal(bd + " ").withStyle(Style.EMPTY.withColor(r.from()));
		if (title) {
			out.append(ClientRanks.gradient(r.tag(), r.from(), r.to(), true));
			out.append(Component.literal(" "));
		}
		int n = Math.max(1, name.length() - 1);
		for (int i = 0; i < name.length(); i++) {
			float t = gradient ? i / (float) n : 0f;
			if (anim) t = (float) (0.5 + 0.5 * Math.sin(time * 3.0 - i * 0.6));
			int col = lerp(c1, c2, t);
			Style st = Style.EMPTY.withColor(col).withBold(b).withItalic(it);
			if (fd != null) st = st.withFont(fd);
			out.append(Component.literal(String.valueOf(name.charAt(i))).withStyle(st));
		}
		return out;
	}

	private static int lerp(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (g << 8) | bl;
	}
}
