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

	public record Entry(String rank, String cape, String wings, String shield) {}

	public static final List<Rank> ALL = List.of(
			new Rank("owner", "OWNER", "♛", 0xFF3B3B, 0xFFC21A, 0xFFD966),
			new Rank("admin", "ADMIN", "✪", 0xFF4F6A, 0xFF6AD5, 0xFF9AC8),
			new Rank("dev", "DEV", "⚙", 0x48E0FF, 0x5A7CFF, 0x9FE8FF),
			new Rank("mod", "MOD", "⚔", 0x5CFF7A, 0x1FB58F, 0xA8FFB8),
			new Rank("partner", "PARTNER", "❖", 0xFF9A1F, 0xFFE066, 0xFFE3A0),
			new Rank("vip", "VIP", "★", 0xFFD700, 0xFFF7A8, 0xFFF0B0),
			new Rank("friend", "FRIEND", "❤", 0xFF7AC8, 0xB48CFF, 0xFFC8E8),
			new Rank("tester", "TESTER", "✎", 0xB0B0B0, 0xFFFFFF, 0xE0E0E0));

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
			next.put(e.getKey().toLowerCase(Locale.ROOT), new Entry(str(o, "rank"), str(o, "cape"), str(o, "wings"), str(o, "shield")));
		}
		PLAYERS.clear();
		PLAYERS.putAll(next);
	}

	private static String str(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
	}

	/** Entry by name or UUID (as text). */
	public static Entry entry(String name, java.util.UUID uuid) {
		Entry e = name == null ? null : PLAYERS.get(name.toLowerCase(Locale.ROOT));
		if (e == null && uuid != null) e = PLAYERS.get(uuid.toString().toLowerCase(Locale.ROOT));
		return e;
	}

	public static Rank rank(String name, java.util.UUID uuid) {
		if (!Config.get().ranks.showClientRanks) return null;
		Entry e = entry(name, uuid);
		if (e == null || e.rank() == null) return null;
		for (Rank r : ALL) if (r.id().equalsIgnoreCase(e.rank())) return r;
		return null;
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

	/** "♛ OWNER ┃ " prefix for a name tag. */
	public static MutableComponent prefix(Rank r) {
		MutableComponent out = Component.literal(r.symbol() + " ").withColor(r.from());
		out.append(gradient(r.tag(), r.from(), r.to(), true));
		out.append(Component.literal(" ┃ ").withColor(0x808080));
		return out;
	}
}
