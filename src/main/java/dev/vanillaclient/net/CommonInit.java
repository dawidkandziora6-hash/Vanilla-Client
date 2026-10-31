package dev.vanillaclient.net;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Runs on servers and in singleplayer/LAN worlds: relays every player's cosmetics
 * to everyone else who has Vanilla Client. Players without the mod get nothing.
 */
public final class CommonInit implements ModInitializer {
	private static final Map<UUID, String> STATES = new ConcurrentHashMap<>();
	private static final int MAX = 200;

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncPayload.TYPE, SyncPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, ctx) -> {
			ServerPlayer from = ctx.player();
			String data = payload.data();
			if (data == null || data.length() > MAX) return;
			UUID id = from.getUUID();
			STATES.put(id, data);
			broadcast(ctx.server(), id + "|" + data);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.getPlayer();
			if (!ServerPlayNetworking.canSend(p, SyncPayload.TYPE)) return;
			for (Map.Entry<UUID, String> e : STATES.entrySet()) {
				ServerPlayNetworking.send(p, new SyncPayload(e.getKey() + "|" + e.getValue()));
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			if (STATES.remove(id) != null) broadcast(server, id + "|");
		});
	}

	private static void broadcast(MinecraftServer server, String data) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(p, SyncPayload.TYPE)) ServerPlayNetworking.send(p, new SyncPayload(data));
		}
	}
}
