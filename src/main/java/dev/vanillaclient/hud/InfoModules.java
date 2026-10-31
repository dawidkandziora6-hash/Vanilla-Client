package dev.vanillaclient.hud;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.features.Ranks;
import dev.vanillaclient.gui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;

/** Small info modules: FPS, ping, biome, clock, speed, light, session, rank, effects, item counter. */
public final class InfoModules {
	private InfoModules() {}

	private static Minecraft mc() {
		return Minecraft.getInstance();
	}

	// ------------------------------------------------------------ speed tracking

	private static double lastX, lastZ, speed;
	private static boolean hasLast;

	public static void tick(Minecraft mc) {
		if (mc.player == null) {
			hasLast = false;
			return;
		}
		double x = mc.player.getX(), z = mc.player.getZ();
		if (hasLast) {
			double d = Math.hypot(x - lastX, z - lastZ) * 20.0;
			speed = speed * 0.7 + d * 0.3;
		}
		lastX = x;
		lastZ = z;
		hasLast = true;
	}

	public static String pretty(String path) {
		StringBuilder b = new StringBuilder();
		for (String part : path.split("_")) {
			if (part.isEmpty()) continue;
			if (b.length() > 0) b.append(' ');
			b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return b.toString();
	}

	// ------------------------------------------------------------ modules

	public static final class Fps extends TextModule {
		public Fps() {
			super("fps", "FPS");
		}

		@Override
		public Config.TextHud settings() {
			return Config.get().fps;
		}

		@Override
		public ItemStack icon() {
			return Icons.stack(Items.REDSTONE);
		}

		@Override
		protected String value(boolean editing) {
			return String.valueOf(mc().getFps());
		}
	}

	public static final class Ping extends TextModule {
		public Ping() {
			super("ping", "Ping");
		}

		@Override
		public Config.TextHud settings() {
			return Config.get().ping;
		}

		@Override
		public ItemStack icon() {
			return Icons.stack(Items.ENDER_PEARL);
		}

		@Override
		protected String value(boolean editing) {
			Minecraft mc = mc();
			if (mc.player == null || mc.getConnection() == null) return editing ? "24 ms" : null;
			if (mc.hasSingleplayerServer()) return "0 ms";
			PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			return info == null ? "? ms" : info.getLatency() + " ms";
		}
	}

	public static final class Biome extends TextModule {
		public Biome() {
			super("biome", "Biome");
		}

		@Override
		public Config.TextHud settings() {
			return Config.get().biome;
		}

		@Override
		public ItemStack icon() {
			return Icons.stack(Items.OAK_SAPLING);
		}

		@Override
		protected String value(boolean editing) {
			Minecraft mc = mc();
			if (mc.player == null || mc.level == null) return editing ? "Plains" : null;
			return mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
					.map(k -> pretty(k.identifier().getPath())).orElse("?");
		}
	}

	public static final class Clock extends TextModule {
		public Clock() {
			super("clock", "Real Time");
		}

		@Override
		public Config.TextHud settings() {
			return Config.get().clock;
		}

		@Override
		public ItemStack icon() {
			return Icons.stack(Items.CLOCK);
		}

		@Override
		protected String value(boolean editing) {
			LocalTime t = LocalTime.now();
			return String.format(Locale.ROOT, "%02d:%02d", t.getHour(), t.getMinute());
		}
	}

	public static final class Speed extends TextModule {
		public Speed() {
			super("speed", "Speed");
		}

		@Override
		public Config.TextHud settings() {
			return Config.get().speed;
		}

		@Override
		public ItemStack icon() {
			return Icons.stack(Items.FEATHER);
		}

		@Override
		protected String value(boolean editing) {
			return String.format(Locale.ROOT, "%.1f m/s", speed);
		}
	}

	public static final class Light extends TextModule {
		public Light() {
			super("light", "Light Level");
		}

		@Override
		public Config.TextHud settings() {
			return Config.get().light;
		}

		@Override
		public ItemStack icon() {
			return Icons.stack(Items.TORCH);
		}

		@Override
		protected String value(boolean editing) {
			Minecraft mc = mc();
			if (mc.player == null || mc.level == null) return editing ? "§a12" : null;
			BlockPos p = mc.player.blockPosition();
			int block = mc.level.getBrightness(LightLayer.BLOCK, p);
			int sky = mc.level.getBrightness(LightLayer.SKY, p);
			return (block == 0 ? "§c" : "§a") + block + " §7(sky " + sky + ")";
		}
	}

	public static final class Session extends TextModule {
		public Session() {
			super("session", "Session Time");
		}

		@Override
		public Config.TextHud settings() {
			return Config.get().session;
		}

		@Override
		public ItemStack icon() {
			return Icons.stack(Items.COMPASS);
		}

		@Override
		protected String value(boolean editing) {
			long s = Ranks.sessionMillis() / 1000;
			return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
		}
	}

	public static final class RankLine extends TextModule {
		public RankLine() {
			super("rank", "Rank");
		}

		@Override
		public Config.TextHud settings() {
			return Config.get().rankHud;
		}

		@Override
		public ItemStack icon() {
			return Icons.stack(Items.NETHER_STAR);
		}

		@Override
		protected String value(boolean editing) {
			Ranks.Rank r = Ranks.current();
			return r.plain() + " §7· " + Ranks.formatHours(Ranks.hours());
		}

		@Override
		public void draw(GuiGraphicsExtractor g, boolean editing) {
			Config.TextHud s = settings();
			Font f = mc().font;
			Ranks.Rank r = Ranks.current();
			String l = labelText();
			background(g, s, width(editing), 8);
			g.text(f, l, 0, 0, s.labelColor, s.shadow);
			g.text(f, r.plain(), f.width(l), 0, r.color(), s.shadow);
			g.text(f, " §7· " + Ranks.formatHours(Ranks.hours()), f.width(l) + f.width(r.plain()), 0, s.color, s.shadow);
		}
	}

	/** Active potion effects with remaining time. */
	public static final class Effects extends HudModule {
		public Effects() {
			super("effects", "Effects");
		}

		@Override
		public Config.TextHud settings() {
			return Config.get().effects;
		}

		@Override
		public ItemStack icon() {
			return Icons.stack(Items.POTION);
		}

		private List<String[]> lines(boolean editing) {
			List<String[]> out = new ArrayList<>();
			Minecraft mc = mc();
			if (mc.player != null) {
				for (MobEffectInstance e : mc.player.getActiveEffects()) {
					String name = e.getEffect().value().getDisplayName().getString();
					int amp = e.getAmplifier();
					if (amp > 0) name += " " + roman(amp + 1);
					String time;
					if (e.isInfiniteDuration()) {
						time = "∞";
					} else {
						int sec = e.getDuration() / 20;
						time = String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
					}
					out.add(new String[] {name, time, String.valueOf(e.getEffect().value().getColor())});
				}
			}
			if (out.isEmpty() && editing) {
				out.add(new String[] {"Speed II", "1:30", String.valueOf(0x33EBFF)});
				out.add(new String[] {"Regeneration", "0:22", String.valueOf(0xCD5CAB)});
			}
			return out;
		}

		private static String roman(int n) {
			String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
			return n < r.length ? r[n] : String.valueOf(n);
		}

		@Override
		public boolean hasContent() {
			return !lines(false).isEmpty();
		}

		@Override
		public int width(boolean editing) {
			Font f = mc().font;
			int w = 0;
			for (String[] l : lines(editing)) w = Math.max(w, f.width(l[0] + "  " + l[1]) + 6);
			return Math.max(w, 40);
		}

		@Override
		public int height(boolean editing) {
			return Math.max(8, lines(editing).size() * 10 - 2);
		}

		@Override
		public void draw(GuiGraphicsExtractor g, boolean editing) {
			Config.TextHud s = settings();
			Font f = mc().font;
			int w = width(editing);
			background(g, s, w, height(editing));
			int y = 0;
			for (String[] l : lines(editing)) {
				int col = 0xFF000000 | Integer.parseInt(l[2]);
				g.fill(0, y, 2, y + 8, col);
				g.text(f, l[0], 5, y, s.color, s.shadow);
				g.text(f, l[1], w - f.width(l[1]), y, s.labelColor, s.shadow);
				y += 10;
			}
		}
	}

	/** How many totems, gapples, pearls, arrows … you carry. */
	public static final class Counter extends HudModule {
		public Counter() {
			super("counter", "Item Counter");
		}

		@Override
		public Config.Counter settings() {
			return Config.get().counter;
		}

		@Override
		public ItemStack icon() {
			return Icons.stack(Items.TOTEM_OF_UNDYING);
		}

		public static Item item(String id) {
			try {
				return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(id));
			} catch (RuntimeException e) {
				return Items.AIR;
			}
		}

		private static int count(Item item) {
			Minecraft mc = mc();
			if (mc.player == null) return 0;
			var inv = mc.player.getInventory();
			int n = 0;
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack st = inv.getItem(i);
				if (st.is(item)) n += st.getCount();
			}
			return n;
		}

		private record Entry(Item item, int count) {}

		private List<Entry> entries(boolean editing) {
			List<Entry> out = new ArrayList<>();
			Config.Counter s = settings();
			for (String id : s.items) {
				Item it = item(id);
				if (it == Items.AIR) continue;
				int n = editing && mc().player == null ? 3 : count(it);
				if (n == 0 && s.hideZero && !editing) continue;
				out.add(new Entry(it, n));
			}
			return out;
		}

		@Override
		public boolean hasContent() {
			return !entries(false).isEmpty();
		}

		private int cell(Entry e) {
			return 18 + mc().font.width(String.valueOf(e.count())) + 4;
		}

		@Override
		public int width(boolean editing) {
			int w = 0;
			for (Entry e : entries(editing)) w = settings().vertical ? Math.max(w, cell(e)) : w + cell(e);
			return Math.max(16, w);
		}

		@Override
		public int height(boolean editing) {
			return settings().vertical ? Math.max(16, entries(editing).size() * 17) : 16;
		}

		@Override
		public void draw(GuiGraphicsExtractor g, boolean editing) {
			Config.Counter s = settings();
			Font f = mc().font;
			background(g, s, width(editing), height(editing));
			int x = 0, y = 0;
			for (Entry e : entries(editing)) {
				ItemStack st = Icons.stack(e.item());
				if (!st.isEmpty()) g.item(st, x, y);
				g.text(f, String.valueOf(e.count()), x + 18, y + 4, e.count() == 0 ? 0xFFFF5555 : s.color, s.shadow);
				if (s.vertical) y += 17;
				else x += cell(e);
			}
		}
	}
}
