package dev.aatracker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvents;

/**
 * Plays a sound when you get an advancement and adds the count to Minecraft's own
 * chat line: "Steve has made the advancement [Diamonds!] 38/126".
 */
public final class AdvancementProgressChat {
	private AdvancementProgressChat() {}

	private record Count(int done, int total, Set<String> doneIds, Map<String, AdvancementType> types, Map<String, String> titles) {}

	private static String world = "";
	private static Set<String> lastDone;
	private static long joinedAt;
	private static int ticks;
	private static long appendedAt;
	private static long pendingSince;
	private static int pendingDone, pendingTotal;

	static Count count(Minecraft mc) {
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
		Count c = new Count(0, 0, new HashSet<>(), new java.util.HashMap<>(), new java.util.HashMap<>());
		int[] n = new int[2];
		for (AdvancementNode root : tree.roots()) walk(root, prog, n, c);
		return new Count(n[0], n[1], c.doneIds(), c.types(), c.titles());
	}

	private static void walk(AdvancementNode node, java.util.function.Function<AdvancementHolder, AdvancementProgress> prog, int[] n, Count c) {
		var d = node.advancement().display();
		if (d.isPresent()) {
			n[1]++;
			String id = node.holder().id().toString();
			c.types().put(id, d.get().type());
			c.titles().put(d.get().title().getString(), id);
			AdvancementProgress p = prog.apply(node.holder());
			if (p != null && p.isDone()) {
				n[0]++;
				c.doneIds().add(id);
			}
		}
		for (AdvancementNode ch : node.children()) walk(ch, prog, n, c);
	}

	private static String worldKey(Minecraft mc) {
		if (mc.getSingleplayerServer() != null) return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		var s = mc.getCurrentServer();
		return s == null ? "?" : "mp:" + s.ip;
	}

	private static Component number(int done, int total) {
		return Component.literal(" " + done + "/" + total).withColor(done == total ? 0x55FF55 : 0xAAAAAA);
	}

	/** Minecraft's "X has made the advancement [Y]" line: add " 38/126" for your own. */
	public static Component modify(Component message) {
		if (!(message.getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith("chat.type.advancement.")) return message;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || tc.getArgs().length < 2) return message;
		Object who = tc.getArgs()[0];
		String name = who instanceof Component c ? c.getString() : String.valueOf(who);
		if (!name.equals(mc.player.getName().getString())) return message;
		Count c = count(mc);
		if (c == null) return message;
		int done = c.done();
		Object adv = tc.getArgs()[1];
		String title = (adv instanceof Component ac ? ac.getString() : String.valueOf(adv)).replace("[", "").replace("]", "");
		String id = c.titles().get(title);
		if (id != null && !c.doneIds().contains(id)) done++; // the server tells the chat a moment before the progress
		appendedAt = System.currentTimeMillis();
		pendingSince = 0;
		return message.copy().append(number(Math.min(done, c.total()), c.total()));
	}

	/** Every client tick: sound right away; a short line if the server doesn't announce advancements. */
	public static void tick(Minecraft mc) {
		if (mc.player == null) {
			world = "";
			lastDone = null;
			return;
		}
		if (++ticks % 2 != 0) return;
		if (pendingSince > 0 && System.currentTimeMillis() - pendingSince > 1500) {
			pendingSince = 0;
			if (System.currentTimeMillis() - appendedAt > 2500) mc.gui.hud.getChat().addClientSystemMessage(number(pendingDone, pendingTotal).copy());
		}
		String w = worldKey(mc);
		if (!w.equals(world)) {
			world = w;
			lastDone = null;
			joinedAt = System.currentTimeMillis();
		}
		Count c = count(mc);
		if (c == null || c.total() == 0) return;
		Set<String> before = lastDone;
		lastDone = c.doneIds();
		if (before == null || System.currentTimeMillis() - joinedAt < 5000) return;
		Set<String> fresh = new HashSet<>(c.doneIds());
		fresh.removeAll(before);
		if (fresh.isEmpty() || fresh.size() > 5) return;
		boolean challenge = false;
		for (String id : fresh) challenge |= c.types().get(id) == AdvancementType.CHALLENGE;
		// challenges already have Minecraft's fanfare
		if (!challenge) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.5f, 0.55f));
		pendingSince = System.currentTimeMillis();
		pendingDone = c.done();
		pendingTotal = c.total();
	}
}
