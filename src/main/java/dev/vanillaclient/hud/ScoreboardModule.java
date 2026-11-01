package dev.vanillaclient.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Icons;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** The sidebar scoreboard as a movable, styled HUD module. */
public final class ScoreboardModule extends HudModule {
	private static final Comparator<PlayerScoreEntry> ORDER = Comparator.comparingInt(PlayerScoreEntry::value).reversed()
			.thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

	private record Line(Component name, Component score) {}

	private Component title;
	private final List<Line> lines = new ArrayList<>();
	private long builtFrame = -1;
	private boolean builtEditing;

	public ScoreboardModule() {
		super("scoreboard", "Scoreboard");
	}

	/** Replaces Minecraft's sidebar: when the mod draws it, the vanilla one is skipped. */
	public static HudElement wrap(HudElement vanilla) {
		return (g, delta) -> {
			if (!Config.get().scoreboard.custom) vanilla.extractRenderState(g, delta);
		};
	}

	@Override
	public Config.Scoreboard settings() {
		return Config.get().scoreboard;
	}

	private static Objective objective() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return null;
		Scoreboard sb = mc.level.getScoreboard();
		PlayerTeam team = sb.getPlayersTeam(mc.player.getScoreboardName());
		if (team != null) {
			var c = team.getColor();
			if (c.isPresent()) {
				Objective o = sb.getDisplayObjective(c.get().displaySlot());
				if (o != null) return o;
			}
		}
		return sb.getDisplayObjective(DisplaySlot.SIDEBAR);
	}

	private void build(boolean editing) {
		if (builtFrame == HudManager.frame && builtEditing == editing) return;
		builtFrame = HudManager.frame;
		builtEditing = editing;
		lines.clear();
		title = null;
		Objective o = objective();
		Config.Scoreboard s = settings();
		if (o == null) {
			if (!editing) return;
			title = Component.literal("§e§lVANILLA CLIENT");
			lines.add(new Line(Component.literal("§7Day §f12"), Component.literal("§c3")));
			lines.add(new Line(Component.literal("§7Kills §f4"), Component.literal("§c2")));
			lines.add(new Line(Component.literal("§aplay.example.net"), Component.literal("§c1")));
			return;
		}
		Scoreboard sb = o.getScoreboard();
		NumberFormat nf = o.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
		title = o.getDisplayName();
		sb.listPlayerScores(o).stream().filter(e -> !e.isHidden()).sorted(ORDER).limit(Math.max(1, Math.min(15, s.maxLines)))
				.forEach(e -> lines.add(new Line(PlayerTeam.formatNameForTeam(sb.getPlayersTeam(e.owner()), e.ownerName()), e.formatValue(nf))));
	}

	@Override
	public boolean hasContent() {
		Config.Scoreboard s = settings();
		if (!s.custom) return false;
		build(false);
		return title != null && (!lines.isEmpty() || !s.hideWhenEmpty);
	}

	@Override
	public int width(boolean editing) {
		build(editing);
		Font f = Minecraft.getInstance().font;
		int w = title == null ? 60 : f.width(title);
		boolean nums = settings().numbers;
		for (Line l : lines) w = Math.max(w, f.width(l.name()) + (nums ? f.width(": ") + f.width(l.score()) : 0));
		return w + 4;
	}

	@Override
	public int height(boolean editing) {
		build(editing);
		return (lines.size() + 1) * 9 + 1;
	}

	@Override
	public ItemStack icon() {
		return Icons.stack(Items.OAK_SIGN);
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		build(editing);
		if (title == null) return;
		Config.Scoreboard s = settings();
		Font f = Minecraft.getInstance().font;
		int w = width(editing), h = height(editing);
		g.fill(0, 0, w, 10, s.titleBackground);
		if (s.background) g.fill(0, 10, w, h, s.backgroundColor);
		g.centeredText(f, title, w / 2, 1, s.color);
		int y = 10;
		for (Line l : lines) {
			g.text(f, l.name(), 2, y, s.color, s.shadow);
			if (s.numbers) {
				// own number color replaces the default red (servers' own styled numbers stay as they are)
				Component sc = s.numberColor != 0xFFFF5555 ? Component.literal(l.score().getString()) : l.score();
				g.text(f, sc, w - 2 - f.width(sc), y, s.numberColor, s.shadow);
			}
			y += 9;
		}
	}
}
