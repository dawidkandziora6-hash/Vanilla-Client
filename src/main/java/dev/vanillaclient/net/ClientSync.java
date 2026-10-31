package dev.vanillaclient.net;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.cosmetics.Cosmetics;
import dev.vanillaclient.features.ClientRanks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Sends your cosmetics to the server (if it has the mod) and keeps everyone else's. */
public final class ClientSync {
	/** What another player wears. Null fields = nothing. */
	public record Remote(String cape, String wings, String shield, String rank, String style) {}

	private static final Map<UUID, Remote> REMOTE = new ConcurrentHashMap<>();
	private static String lastSent = "";
	private static int ticks;

	/** Entity currently being drawn (set while a player model is submitted), or -1. */
	public static final ThreadLocal<Integer> RENDERING = ThreadLocal.withInitial(() -> -1);

	private ClientSync() {}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, ctx) -> receive(payload.data()));
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			REMOTE.clear();
			lastSent = "";
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> REMOTE.clear());
	}

	private static void receive(String data) {
		String[] p = data.split("\\|", -1);
		try {
			UUID id = UUID.fromString(p[0]);
			if (p.length < 5) {
				REMOTE.remove(id);
				return;
			}
			REMOTE.put(id, new Remote(nz(p[1]), nz(p[2]), nz(p[3]), nz(p[4]), p.length > 5 ? p[5] : null));
		} catch (IllegalArgumentException ignored) {
			// bad data
		}
	}

	private static String nz(String s) {
		return s == null || s.isEmpty() || s.equals("none") || s.equals("custom") ? null : s;
	}

	/** Every second: send your state when it changed (and when the server can take it). */
	public static void tick(Minecraft mc) {
		if (mc.player == null || ++ticks % 20 != 0) return;
		if (ticks % 12000 == 0) ClientRanks.refresh();
		if (!Config.get().ranks.shareCosmetics) return;
		Config.Cosmetics c = Config.get().cosmetics;
		String data = owned(Cosmetics.Kind.CAPE, c.cape) + "|" + owned(Cosmetics.Kind.WINGS, c.wings) + "|" + owned(Cosmetics.Kind.SHIELD, c.shield)
				+ "|" + Config.get().ranks.display + "|" + dev.vanillaclient.features.NameStyle.mine().encode();
		if (data.equals(lastSent)) return;
		try {
			if (!ClientPlayNetworking.canSend(SyncPayload.TYPE)) return;
			ClientPlayNetworking.send(new SyncPayload(data));
			lastSent = data;
		} catch (RuntimeException ignored) {
			// not connected yet
		}
	}

	private static String owned(Cosmetics.Kind k, String id) {
		return dev.vanillaclient.features.Shop.owns(k, id) ? id : "none";
	}

	/** Cosmetics of another player: from the server sync, otherwise from ranks.json. */
	public static Remote of(Player p) {
		if (p == null) return null;
		Remote r = REMOTE.get(p.getUUID());
		if (r != null) return r;
		ClientRanks.Entry e = ClientRanks.entry(p.getName().getString(), p.getUUID());
		if (e != null && (e.cape() != null || e.wings() != null || e.shield() != null)) return new Remote(e.cape(), e.wings(), e.shield(), null, null);
		return null;
	}

	/** The player being drawn right now (for shields in other players' hands). */
	public static Player renderingPlayer() {
		int id = RENDERING.get();
		Minecraft mc = Minecraft.getInstance();
		if (id < 0 || mc.level == null) return null;
		Entity e = mc.level.getEntity(id);
		return e instanceof Player p ? p : null;
	}
}
