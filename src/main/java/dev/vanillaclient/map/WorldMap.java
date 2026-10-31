package dev.vanillaclient.map;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.NativeImage;
import dev.vanillaclient.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;

/**
 * Explored-area map. Only reads chunks your client already has loaded, colors
 * them like a vanilla map and saves 256×256-block tiles per world and dimension
 * (config/vanillaclient/maps). Nothing is sent to the server.
 */
public final class WorldMap {
	public static final int TILE = 256;

	public static final class Tile {
		public final int tx, tz;
		final int[] px = new int[TILE * TILE];
		DynamicTexture tex;
		Identifier id;
		boolean gpuDirty = true, diskDirty;

		Tile(int tx, int tz) {
			this.tx = tx;
			this.tz = tz;
		}

		/** Texture for drawing (uploads changes first). Render thread only. */
		public Identifier texture() {
			if (tex == null) {
				NativeImage img = new NativeImage(TILE, TILE, false);
				tex = new DynamicTexture(() -> "vanillaclient map tile", img);
				id = Identifier.fromNamespaceAndPath("vanillaclient", "map/" + (++counter) + "_" + tx + "_" + tz);
				Minecraft.getInstance().getTextureManager().register(id, tex);
				gpuDirty = true;
			}
			if (gpuDirty) {
				NativeImage img = tex.getPixels();
				for (int z = 0; z < TILE; z++) {
					for (int x = 0; x < TILE; x++) img.setPixel(x, z, px[z * TILE + x]);
				}
				tex.upload();
				gpuDirty = false;
			}
			return id;
		}
	}

	private static int counter;
	private static final Map<Long, Tile> TILES = new HashMap<>();
	private static final Map<Long, Long> SCANNED = new HashMap<>();
	private static String worldKey;
	private static long lastSave = System.currentTimeMillis();
	private static int tick;

	private WorldMap() {}

	private static long key(int a, int b) {
		return ((long) a << 32) ^ (b & 0xFFFFFFFFL);
	}

	public static Collection<Tile> tiles() {
		return TILES.values();
	}

	public static String currentKey() {
		return worldKey;
	}

	// ------------------------------------------------------------ world identity

	private static String sanitize(String s) {
		return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
	}

	private static String computeKey(Minecraft mc, ClientLevel level) {
		String world = "unknown";
		try {
			if (mc.getSingleplayerServer() != null) {
				world = "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName();
			} else if (mc.getCurrentServer() != null) {
				world = "mp_" + mc.getCurrentServer().name;
			}
		} catch (RuntimeException ignored) {
			// keep "unknown"
		}
		String dim = level.dimension().identifier().getPath();
		return sanitize(world) + "/" + sanitize(dim);
	}

	private static Path dir() {
		return Config.dir().resolve("maps").resolve(worldKey);
	}

	// ------------------------------------------------------------ ticking

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			if (worldKey != null) {
				saveAll();
				clear();
				worldKey = null;
			}
			return;
		}
		String k = computeKey(mc, level);
		if (!k.equals(worldKey)) {
			if (worldKey != null) saveAll();
			clear();
			worldKey = k;
			loadAround(mc.player.getBlockX(), mc.player.getBlockZ());
		}
		if (!Config.get().worldMap.enabled) return;
		if (++tick % 2 != 0) return;
		scanSome(level, mc.player.getBlockX() >> 4, mc.player.getBlockZ() >> 4, mc.player.getBlockY());
		if (System.currentTimeMillis() - lastSave > 60_000) {
			saveAll();
			lastSave = System.currentTimeMillis();
		}
	}

	private static void clear() {
		for (Tile t : TILES.values()) {
			if (t.id != null) Minecraft.getInstance().getTextureManager().release(t.id);
		}
		TILES.clear();
		SCANNED.clear();
	}

	private static int radius(Minecraft mc) {
		int r = Config.get().worldMap.radius;
		if (r <= 0) r = mc.options.renderDistance().get();
		return Math.max(2, Math.min(32, r));
	}

	private static void scanSome(ClientLevel level, int pcx, int pcz, int playerY) {
		int radius = radius(Minecraft.getInstance());
		boolean ceiling = level.dimensionType().hasCeiling();
		int budget = ceiling ? 4 : 24;
		long now = System.currentTimeMillis();
		for (int r = 0; r <= radius && budget > 0; r++) {
			for (int dx = -r; dx <= r && budget > 0; dx++) {
				for (int dz = -r; dz <= r && budget > 0; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					int cx = pcx + dx, cz = pcz + dz;
					long ck = key(cx, cz);
					Long last = SCANNED.get(ck);
					long age = r <= 1 ? 2_000 : r <= 4 ? 10_000 : 40_000;
					if (last != null && now - last < age) continue;
					LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
					if (chunk == null) continue;
					scanChunk(level, chunk, cx, cz, ceiling, playerY);
					SCANNED.put(ck, now);
					budget--;
				}
			}
		}
	}

	private static int shade(int argb, int factor) {
		int r = ((argb >> 16) & 0xFF) * factor / 255;
		int g = ((argb >> 8) & 0xFF) * factor / 255;
		int b = (argb & 0xFF) * factor / 255;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static int mix(int a, int b, float t) {
		int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
	}

	/** Base color of a block: biome tint for grass, leaves and water, else the vanilla map color. */
	private static int baseColor(ClientLevel level, BlockState state, BlockPos pos, MapColor mc) {
		if (mc == MapColor.GRASS || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
				|| state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.SUGAR_CANE)) {
			return shade(BiomeColors.getAverageGrassColor(level, pos), 235);
		}
		if (mc == MapColor.PLANT && (state.is(BlockTags.LEAVES) || state.is(Blocks.VINE))) {
			return shade(BiomeColors.getAverageFoliageColor(level, pos), 215);
		}
		return mc.calculateARGBColor(MapColor.Brightness.HIGH) | 0xFF000000;
	}

	private static void scanChunk(ClientLevel level, LevelChunk chunk, int cx, int cz, boolean ceiling, int playerY) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int minY = level.getMinY();
		int[] prevRow = new int[16];
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				int wx = cx * 16 + x, wz = cz * 16 + z;
				int y;
				if (ceiling) {
					y = findFloor(chunk, pos, wx, wz, Math.min(playerY + 12, level.getMaxY() - 1), minY);
				} else {
					y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
				}
				int color = 0;
				if (y > minY) {
					pos.set(wx, y, wz);
					BlockState state = chunk.getBlockState(pos);
					MapColor mc = state.getMapColor(level, pos);
					if (mc == MapColor.NONE && y - 1 > minY) {
						y--;
						pos.set(wx, y, wz);
						state = chunk.getBlockState(pos);
						mc = state.getMapColor(level, pos);
					}
					if (mc != MapColor.NONE) {
						if (mc == MapColor.WATER) {
							int water = BiomeColors.getAverageWaterColor(level, pos) | 0xFF000000;
							int depth = 0;
							int floor = water;
							for (int d = 1; d <= 16; d++) {
								pos.set(wx, y - d, wz);
								BlockState below = chunk.getBlockState(pos);
								MapColor bm = below.getMapColor(level, pos);
								if (bm != MapColor.WATER) {
									if (bm != MapColor.NONE) floor = baseColor(level, below, pos, bm);
									break;
								}
								depth++;
							}
							float see = depth <= 1 ? 0.45f : depth <= 3 ? 0.3f : depth <= 6 ? 0.15f : 0.04f;
							int f = depth <= 2 ? 245 : depth <= 5 ? 220 : depth <= 9 ? 195 : 170;
							// subtle ripple pattern so oceans aren't flat
							if (((wx + wz) & 3) == 0 && depth > 2) f -= 6;
							color = shade(mix(water, floor, see), f);
						} else {
							int north = z == 0 ? (ceiling ? y : level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz - 1) - 1) : prevRow[x];
							int diff = y - north;
							int f = diff > 1 ? 255 : diff == 1 ? 242 : diff == 0 ? 220 : diff == -1 ? 198 : 178;
							color = shade(baseColor(level, state, pos.set(wx, y, wz), mc), f);
						}
					}
				}
				prevRow[x] = y;
				put(wx, wz, color);
			}
		}
	}

	/** Nether: walk down from near the player to the first solid block below air. */
	private static int findFloor(LevelChunk chunk, BlockPos.MutableBlockPos pos, int wx, int wz, int startY, int minY) {
		boolean air = false;
		for (int y = startY; y > minY; y--) {
			pos.set(wx, y, wz);
			boolean isAir = chunk.getBlockState(pos).isAir();
			if (isAir) air = true;
			else if (air) return y;
		}
		return minY;
	}

	private static void put(int wx, int wz, int color) {
		if (color == 0) return;
		int tx = Math.floorDiv(wx, TILE), tz = Math.floorDiv(wz, TILE);
		Tile t = tile(tx, tz, true);
		int i = Math.floorMod(wz, TILE) * TILE + Math.floorMod(wx, TILE);
		if (t.px[i] != color) {
			t.px[i] = color;
			t.gpuDirty = true;
			t.diskDirty = true;
		}
	}

	public static Tile tile(int tx, int tz, boolean create) {
		long k = key(tx, tz);
		Tile t = TILES.get(k);
		if (t == null) {
			t = load(tx, tz);
			if (t == null && create) t = new Tile(tx, tz);
			if (t != null) TILES.put(k, t);
		}
		return t;
	}

	/** Color at a world position (0 = unknown). */
	public static int colorAt(int wx, int wz) {
		Tile t = TILES.get(key(Math.floorDiv(wx, TILE), Math.floorDiv(wz, TILE)));
		if (t == null) return 0;
		return t.px[Math.floorMod(wz, TILE) * TILE + Math.floorMod(wx, TILE)];
	}

	// ------------------------------------------------------------ disk

	/** Loads every saved tile of this world/dimension near a position (radius ~4 tiles). */
	public static void loadAround(int bx, int bz) {
		if (worldKey == null) return;
		int tx = Math.floorDiv(bx, TILE), tz = Math.floorDiv(bz, TILE);
		for (int x = tx - 4; x <= tx + 4; x++) {
			for (int z = tz - 4; z <= tz + 4; z++) tile(x, z, false);
		}
	}

	/** All saved tile coordinates of this world/dimension. */
	public static List<int[]> savedTiles() {
		List<int[]> out = new ArrayList<>();
		if (worldKey == null || !Files.isDirectory(dir())) return out;
		try (var s = Files.list(dir())) {
			for (Path p : s.toList()) {
				String n = p.getFileName().toString();
				if (!n.endsWith(".png")) continue;
				String[] parts = n.substring(0, n.length() - 4).split("_");
				if (parts.length != 2) continue;
				try {
					out.add(new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
				} catch (NumberFormatException ignored) {
					// not a tile
				}
			}
		} catch (IOException ignored) {
			// nothing saved yet
		}
		return out;
	}

	private static Tile load(int tx, int tz) {
		if (worldKey == null) return null;
		Path p = dir().resolve(tx + "_" + tz + ".png");
		if (!Files.exists(p)) return null;
		try {
			BufferedImage img = ImageIO.read(p.toFile());
			if (img == null || img.getWidth() != TILE || img.getHeight() != TILE) return null;
			Tile t = new Tile(tx, tz);
			img.getRGB(0, 0, TILE, TILE, t.px, 0, TILE);
			return t;
		} catch (IOException e) {
			Config.LOG.warn("Map tile {} could not be read", p, e);
			return null;
		}
	}

	public static void saveAll() {
		if (worldKey == null) return;
		for (Tile t : TILES.values()) {
			if (!t.diskDirty) continue;
			try {
				Files.createDirectories(dir());
				BufferedImage img = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
				img.setRGB(0, 0, TILE, TILE, t.px, 0, TILE);
				ImageIO.write(img, "png", dir().resolve(t.tx + "_" + t.tz + ".png").toFile());
				t.diskDirty = false;
			} catch (IOException e) {
				Config.LOG.warn("Map tile could not be saved", e);
			}
		}
	}
}
