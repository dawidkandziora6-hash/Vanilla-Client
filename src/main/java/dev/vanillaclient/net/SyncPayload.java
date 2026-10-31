package dev.vanillaclient.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Cosmetics / rank of one player. Client → server: "cape|wings|shield|rank".
 * Server → clients: "uuid|cape|wings|shield|rank" ("uuid|" alone = player left).
 */
public record SyncPayload(String data) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SyncPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("vanillaclient", "sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC =
			StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SyncPayload::data, SyncPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
