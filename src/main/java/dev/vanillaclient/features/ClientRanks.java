package dev.vanillaclient.features;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.vanillaclient.config.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Client ranks (Owner, Admin …) given by nickname. The list lives in ranks.json in the
 * GitHub repo – edit it there and every Vanilla Client picks it up within minutes.
 * A copy is built into the mod, so it also works offline.
 */
public final class ClientRanks {
	public record Rank(String id, String tag, String symbol, int from, int to, int nameColor) {}

	public record Entry(String rank, String cape, String wings, String shield, long coins) {}

	/** Lowest to highest. Not obtainable yet – only the owner account can wear them. */
	public static final List<Rank> ALL = List.of(
			new Rank("vip", "PLUS", "★", 0x55FF55, 0x2FD67A, 0xB8FFC8),
			new Rank("vip+", "PRO", "★", 0x3CFF9A, 0x00D4C8, 0xA8FFE8),
			new Rank("mvp", "ULTRA", "✦", 0x55FFFF, 0x3C9CFF, 0xB0F0FF),
			new Rank("mvp+", "ULTRA+", "✦", 0x6AA8FF, 0xA05CFF, 0xC8C0FF),
			new Rank("elite", "ELITE", "❖", 0xC86CFF, 0xFF5CD6, 0xF0C0FF),
			new Rank("legend", "LEGEND", "♚", 0xFFC21A, 0xFF7A1A, 0xFFE3A0),
			new Rank("media", "CREATOR", "▶", 0xFF5555, 0xFFFFFF, 0xFFC8C8),
			new Rank("partner", "PARTNER", "❤", 0xFF7AC8, 0xB48CFF, 0xFFC8E8),
			new Rank("mod", "STAFF", "⚔", 0x5CFF7A, 0x1FB58F, 0xA8FFB8),
			new Rank("admin", "DEV", "✪", 0xFF4F6A, 0xFF6AD5, 0xFF9AC8),
			new Rank("owner", "OWNER", "♛", 0xFF3B3B, 0xFFC21A, 0xFFD966));

	/** Perks in the order they unlock (VIP gets the first, staff ranks get all). */
	public enum Perk {
		COLOR("Name color", 0), BOLD("Bold / italic name", 1), GRADIENT("Two-color gradient name", 2),
		ANIMATED("Animated gradient", 3), FONT("Name font", 4), BADGE("Own badge symbol", 5);

		public final String label;
		public final int tier;

		Perk(String label, int tier) {
			this.label = label;
			this.tier = tier;
		}
	}

	public static int perkCount(Rank r) {
		int n = 0;
		for (Perk p : Perk.values()) if (has(r, p)) n++;
		return n;
	}

	public static boolean has(Rank r, Perk p) {
		if (r == null) return false;
		int i = ALL.indexOf(r);
		return i >= 6 || i >= p.tier; // media, partner, mod, admin, owner: everything
	}

	public static final String[] BADGES = {"✦", "★", "♛", "❖", "❤", "⚡", "☀", "✿", "♦", "☯", "⚔", "☾", "♪", "✪"};
	public static final String[] FONTS = {"default", "uniform", "alt", "illageralt"};

	public static String fontName(String f) {
		return switch (f) {
			case "uniform" -> "Smooth";
			case "alt" -> "Galactic";
			case "illageralt" -> "Illager";
			default -> "Minecraft";
		};
	}

	public static Rank byId(String id) {
		if (id == null) return null;
		for (Rank r : ALL) if (r.id().equalsIgnoreCase(id)) return r;
		return null;
	}

	/** Owner accounts may wear any rank. */
	public static boolean isOwner(String name, java.util.UUID uuid) {
		Entry e = entry(name, uuid);
		return e != null && "owner".equalsIgnoreCase(e.rank());
	}

	/** The rank a player shows: their granted rank, or (owners only) the one they picked. */
	public static Rank shown(String name, java.util.UUID uuid, String picked) {
		if (!Config.get().ranks.showClientRanks) return null;
		Entry e = entry(name, uuid);
		if (e == null || e.rank() == null) return null;
		if ("owner".equalsIgnoreCase(e.rank()) && picked != null && !picked.isEmpty()) {
			Rank p = byId(picked);
			if (p != null) return p;
		}
		return byId(e.rank());
	}

	/** Your own rank on your screen. */
	public static Rank mine() {
		var u = net.minecraft.client.Minecraft.getInstance().getUser();
		return shown(u.getName(), u.getProfileId(), Config.get().ranks.display);
	}

	private static final String URL = "https://raw.githubusercontent.com/dawidkandziora6-hash/Vanilla-Client/main/ranks.json";
	private static final Map<String, Entry> PLAYERS = new ConcurrentHashMap<>();
	private static long lastFetch;
	private static volatile boolean fetching;

	private ClientRanks() {}

	public static void init() {
		try (Reader r = new InputStreamReader(ClientRanks.class.getResourceAsStream("/assets/vanillaclient/ranks.json"), StandardCharsets.UTF_8)) {
			parse(JsonParser.parseReader(r));
		} catch (Exception e) {
			Config.LOG.warn("Built-in ranks could not be read", e);
		}
		refresh();
	}

	/** Downloads the newest list (at most every 10 minutes). */
	public static void refresh() {
		if (!Config.get().ranks.onlineRanks || fetching || System.currentTimeMillis() - lastFetch < 600_000) return;
		fetching = true;
		lastFetch = System.currentTimeMillis();
		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
		HttpRequest req = HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(10)).GET().build();
		client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((res, err) -> {
			fetching = false;
			if (err != null || res == null || res.statusCode() != 200) return;
			try {
				parse(JsonParser.parseString(res.body()));
			} catch (Exception e) {
				Config.LOG.warn("ranks.json from GitHub is broken", e);
			}
		});
	}

	private static void parse(JsonElement root) {
		JsonObject players = root.getAsJsonObject().getAsJsonObject("players");
		if (players == null) return;
		Map<String, Entry> next = new ConcurrentHashMap<>();
		for (var e : players.entrySet()) {
			JsonObject o = e.getValue().getAsJsonObject();
			long coins = o.has("coins") ? o.get("coins").getAsLong() : 0L;
			next.put(e.getKey().toLowerCase(Locale.ROOT), new Entry(str(o, "rank"), str(o, "cape"), str(o, "wings"), str(o, "shield"), coins));
		}
		PLAYERS.clear();
		PLAYERS.putAll(next);
	}

	private static String str(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
	}

	/** Adds a rank locally (used by the CI world test). */
	public static void put(String name, String rank) {
		PLAYERS.put(name.toLowerCase(Locale.ROOT), new Entry(rank, null, null, null, 0L));
	}

	/** Entry by name or UUID (as text). */
	public static Entry entry(String name, java.util.UUID uuid) {
		Entry e = name == null ? null : PLAYERS.get(name.toLowerCase(Locale.ROOT));
		if (e == null && uuid != null) e = PLAYERS.get(uuid.toString().toLowerCase(Locale.ROOT));
		return e;
	}

	public static Rank rank(String name, java.util.UUID uuid) {
		return shown(name, uuid, null);
	}

	/** Text with a color gradient from one color to another. */
	public static MutableComponent gradient(String text, int from, int to, boolean bold) {
		MutableComponent out = Component.empty();
		int n = Math.max(1, text.length() - 1);
		for (int i = 0; i < text.length(); i++) {
			float t = i / (float) n;
			int r = Math.round(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
			int g = Math.round(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
			int b = Math.round((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
			out.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(Style.EMPTY.withColor((r << 16) | (g << 8) | b).withBold(bold)));
		}
		return out;
	}

	/** "♛ OWNER" without separator (rank cards). */
	public static MutableComponent tag(Rank r) {
		MutableComponent out = Component.literal(r.symbol() + " ").withColor(r.from());
		out.append(gradient(r.tag(), r.from(), r.to(), true));
		return out;
	}

	/** Name tag prefix in client style: a small badge (and the title only if wanted). */
	public static MutableComponent prefix(Rank r) {
		MutableComponent out = Component.literal("✦ ").withColor(r.from());
		out.append(gradient(r.tag(), r.from(), r.to(), true));
		out.append(Component.literal(" ").withColor(0x808080));
		return out;
	}
}
