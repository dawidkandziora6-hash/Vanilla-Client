package dev.vanillaclient.features;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.net.ClientSync;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

/** Name tags with the client rank: "♛ OWNER ┃ Name". */
public final class NameTags {
	private NameTags() {}

	public static Component decorate(Player p, Component original) {
		Config.Ranks cfg = Config.get().ranks;
		if (!cfg.enabled) return original;
		boolean self = p == Minecraft.getInstance().player;
		String name = p.getName().getString();
		String picked = null;
		if (self) {
			picked = cfg.display;
		} else {
			ClientSync.Remote rem = ClientSync.of(p);
			if (rem != null) picked = rem.rank();
		}
		ClientRanks.Rank cr = ClientRanks.shown(name, p.getUUID(), picked);
		if (cr == null || (self && !cfg.symbolInName)) return original;
		MutableComponent out = ClientRanks.prefix(cr);
		out.append(Component.literal(name).withColor(cr.nameColor()));
		return out;
	}
}
