package dev.vanillaclient.features;

import java.util.Locale;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.cosmetics.Cosmetics;
import net.minecraft.client.Minecraft;

/**
 * Coins & owned cosmetics, per Minecraft account. Coins are given by ranks.json
 * (right now only to the owner account) – there is no way to buy them.
 */
public final class Shop {
	public static final int CAPE = 1200, WINGS = 1800, SHIELD = 900;

	private Shop() {}

	private static String account() {
		return Minecraft.getInstance().getUser().getName().toLowerCase(Locale.ROOT);
	}

	public static Config.Wallet wallet() {
		Config.Wallet w = Config.get().wallets.computeIfAbsent(account(), k -> new Config.Wallet());
		// coins granted in ranks.json
		var u = Minecraft.getInstance().getUser();
		ClientRanks.Entry e = ClientRanks.entry(u.getName(), u.getProfileId());
		if (e != null && e.coins() > w.granted) {
			w.coins += e.coins() - w.granted;
			w.granted = e.coins();
			Config.save();
		}
		return w;
	}

	public static int price(Cosmetics.Kind k) {
		return switch (k) {
			case CAPE -> CAPE;
			case WINGS -> WINGS;
			case SHIELD -> SHIELD;
		};
	}

	/** A set costs 20% less than its parts. */
	public static int setPrice(Cosmetics.Collection c) {
		int sum = 0;
		for (Cosmetics.Kind k : Cosmetics.Kind.values()) if (!owns(k, c.id())) sum += price(k);
		return Math.round(sum * 0.8f / 10f) * 10;
	}

	private static String key(Cosmetics.Kind k, String id) {
		return k.prefix + ":" + id;
	}

	public static boolean owns(Cosmetics.Kind k, String id) {
		if (id == null || id.equals(Cosmetics.NONE) || id.equals(Cosmetics.CUSTOM)) return true;
		return wallet().owned.contains(key(k, id));
	}

	public static boolean ownsSet(Cosmetics.Collection c) {
		for (Cosmetics.Kind k : Cosmetics.Kind.values()) if (!owns(k, c.id())) return false;
		return true;
	}

	/** Returns null on success, else why it didn't work. */
	public static String buy(Cosmetics.Kind k, String id) {
		if (owns(k, id)) return null;
		Config.Wallet w = wallet();
		int p = price(k);
		if (w.coins < p) return "Not enough coins";
		w.coins -= p;
		w.owned.add(key(k, id));
		Config.save();
		return null;
	}

	public static String buySet(Cosmetics.Collection c) {
		if (ownsSet(c)) return null;
		Config.Wallet w = wallet();
		int p = setPrice(c);
		if (w.coins < p) return "Not enough coins";
		w.coins -= p;
		for (Cosmetics.Kind k : Cosmetics.Kind.values()) if (!owns(k, c.id())) w.owned.add(key(k, c.id()));
		Config.save();
		return null;
	}

	public static String format(long coins) {
		return String.format(Locale.ROOT, "%,d", coins);
	}
}
