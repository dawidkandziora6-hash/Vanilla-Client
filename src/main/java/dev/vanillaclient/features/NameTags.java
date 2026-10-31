package dev.vanillaclient.features;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.net.ClientSync;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

/** Name tags with client rank ("♛ OWNER ┃ Name") and play-time rank symbol. */
public final class NameTags {
	private NameTags() {}

	public static Component decorate(Player p, Component original) {
		Config.Ranks cfg = Config.get().ranks;
		if (!cfg.enabled) return original;
		boolean self = p == Minecraft.getInstance().player;
		String name = p.getName().getString();
		ClientRanks.Rank cr = ClientRanks.rank(name, p.getUUID());
		if (cr != null) {
			MutableComponent out = ClientRanks.prefix(cr);
			out.append(Component.literal(name).withColor(cr.nameColor()));
			return out;
		}
		Ranks.Rank r = null;
		if (self) {
			if (cfg.symbolInName) r = Ranks.current();
		} else {
			ClientSync.Remote rem = ClientSync.of(p);
			if (rem != null && rem.rank() != null) {
				for (Ranks.Rank x : Ranks.ALL) if (x.id().equals(rem.rank())) r = x;
			}
		}
		if (r == null) return original;
		MutableComponent out = Component.literal(r.symbol() + " ").withColor(r.color() & 0xFFFFFF);
		return out.append(original);
	}
}
