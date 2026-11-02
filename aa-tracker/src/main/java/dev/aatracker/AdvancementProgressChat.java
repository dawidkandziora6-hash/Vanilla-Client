package dev.aatracker;

import java.util.Map;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** After you get an advancement: "✦ 38/126 advancements (30%) · 88 left" in the chat. */
public final class AdvancementProgressChat {
	private AdvancementProgressChat() {}

	private static String world = "";
	private static int lastDone = -1;
	private static long joinedAt;
	private static int ticks;

	/** {done, total} or null. Counts every advancement with a display (like the tracker). */
	public static int[] count(Minecraft mc) {
		AdvancementTree tree = null;
		java.util.function.Function<AdvancementHolder, AdvancementProgress> prog = h -> null;
		try {
			var srv = mc.getSingleplayerServer();
			if (srv != null && mc.player != null) {
				var sp = srv.getPlayerList().getPlayer(mc.player.getUUID());
				if (sp != null) {
					tree = srv.getAdvancements().tree();
					prog = sp.getAdvancements()::getOrStartProgress;
				}
			}
		} catch (RuntimeException e) {
			tree = null;
		}
		if (tree == null && mc.getConnection() != null) {
			var ca = mc.getConnection().getAdvancements();
			tree = ca.tree();
			Map<AdvancementHolder, AdvancementProgress> map = ca.progress();
			prog = map::get;
		}
		if (tree == null) return null;
		int[] out = new int[2];
		for (AdvancementNode root : tree.roots()) walk(root, prog, out);
		return out;
	}

	private static void walk(AdvancementNode n, java.util.function.Function<AdvancementHolder, AdvancementProgress> prog, int[] out) {
		if (n.advancement().display().isPresent()) {
			out[1]++;
			AdvancementProgress p = prog.apply(n.holder());
			if (p != null && p.isDone()) out[0]++;
		}
		for (AdvancementNode c : n.children()) walk(c, prog, out);
	}

	private static String worldKey(Minecraft mc) {
		if (mc.getSingleplayerServer() != null) return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		var s = mc.getCurrentServer();
		return s == null ? "?" : "mp:" + s.ip;
	}

	/** Every client tick. */
	public static void tick(Minecraft mc, boolean enabled, String command) {
		if (mc.player == null) {
			world = "";
			lastDone = -1;
			return;
		}
		if (++ticks % 10 != 0) return;
		String w = worldKey(mc);
		if (!w.equals(world)) {
			world = w;
			lastDone = -1;
			joinedAt = System.currentTimeMillis();
		}
		int[] c = count(mc);
		if (c == null || c[1] == 0) return;
		int before = lastDone;
		lastDone = c[0];
		// no message for the first sync after joining or for big jumps (commands, server sync)
		if (!enabled || before < 0 || c[0] <= before || c[0] - before > 5 || System.currentTimeMillis() - joinedAt < 5000) return;
		int pct = Math.round(c[0] * 100f / c[1]);
		MutableComponent msg = Component.literal("✦ ").withColor(0xFFD34D)
				.append(Component.literal(c[0] + "/" + c[1]).withColor(c[0] == c[1] ? 0x55FF55 : 0xFFFFFF))
				.append(Component.literal(" advancements ").withColor(0xAAAAAA))
				.append(Component.literal("(" + pct + "%)").withColor(0xFFD34D));
		if (c[0] < c[1]) msg.append(Component.literal(" · " + (c[1] - c[0]) + " left").withColor(0x888888));
		else msg.append(Component.literal(" · ALL DONE!").withColor(0x55FF55));
		if (command != null) {
			msg.append(Component.literal("  "));
			msg.append(Component.literal("[Tracker]").withStyle(Style.EMPTY.withColor(0x7FD8FF)
					.withClickEvent(new ClickEvent.RunCommand("/" + command))
					.withHoverEvent(new HoverEvent.ShowText(Component.literal("Open the advancements tracker")))));
		}
		mc.gui.hud.getChat().addClientSystemMessage(msg);
	}
}
