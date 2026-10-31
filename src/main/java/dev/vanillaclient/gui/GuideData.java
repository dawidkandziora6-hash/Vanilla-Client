package dev.vanillaclient.gui;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Reference data for the UHC guide (vanilla Java Edition default trades). */
final class GuideData {
	/** One trade: up to two inputs → one output. Counts are text so ranges like "5-64" work. */
	record Trade(Item in1, String c1, Item in2, String c2, Item out, String cOut, String note) {}

	record Level(String name, List<Trade> trades) {}

	record Profession(String name, Item workstation, String station, List<Level> levels) {}

	record Enchant(Item icon, String item, String best, String alt) {}

	record Potion(Item ingredient, String effect, int color, String base, String source, String time, String redstone, String glowstone) {}

	private GuideData() {}

	static final Item E = Items.EMERALD;

	/** "24 paper → 1 emerald" */
	static Trade buy(Item in, String c) {
		return new Trade(in, c, null, null, E, "1", null);
	}

	/** "5 emeralds → item" */
	static Trade sell(String price, Item out, String c) {
		return new Trade(E, price, null, null, out, c, null);
	}

	static Trade sell(String price, Item out, String c, String note) {
		return new Trade(E, price, null, null, out, c, note);
	}

	static Trade sell2(String price, Item in2, String c2, Item out, String c) {
		return new Trade(E, price, in2, c2, out, c, null);
	}

	static Level lv(String name, Trade... trades) {
		return new Level(name, List.of(trades));
	}

	static List<Profession> villagers() {
		return List.of(
				new Profession("Librarian", Items.LECTERN, "Lectern", List.of(
						lv("Novice", buy(Items.PAPER, "24"), sell("9", Items.BOOKSHELF, "1"), sell2("5-64", Items.BOOK, "1", Items.ENCHANTED_BOOK, "1")),
						lv("Apprentice", buy(Items.BOOK, "4"), sell("1", Items.LANTERN, "1"), sell2("5-64", Items.BOOK, "1", Items.ENCHANTED_BOOK, "1")),
						lv("Journeyman", buy(Items.INK_SAC, "5"), sell("1", Items.GLASS, "4"), sell2("5-64", Items.BOOK, "1", Items.ENCHANTED_BOOK, "1")),
						lv("Expert", buy(Items.WRITABLE_BOOK, "2"), sell("5", Items.CLOCK, "1"), sell("4", Items.COMPASS, "1"), sell2("5-64", Items.BOOK, "1", Items.ENCHANTED_BOOK, "1")),
						lv("Master", sell("20", Items.NAME_TAG, "1")))),
				new Profession("Armorer", Items.BLAST_FURNACE, "Blast Furnace", List.of(
						lv("Novice", buy(Items.COAL, "15"), sell("5", Items.IRON_HELMET, "1"), sell("9", Items.IRON_CHESTPLATE, "1"), sell("7", Items.IRON_LEGGINGS, "1"), sell("4", Items.IRON_BOOTS, "1")),
						lv("Apprentice", buy(Items.IRON_INGOT, "4"), sell("36", Items.BELL, "1"), sell("3", Items.CHAINMAIL_LEGGINGS, "1"), sell("1", Items.CHAINMAIL_BOOTS, "1")),
						lv("Journeyman", buy(Items.LAVA_BUCKET, "1"), buy(Items.DIAMOND, "1"), sell("1", Items.CHAINMAIL_HELMET, "1"), sell("4", Items.CHAINMAIL_CHESTPLATE, "1"), sell("5", Items.SHIELD, "1")),
						lv("Expert", sell("19-33", Items.DIAMOND_LEGGINGS, "1", "enchanted"), sell("13-27", Items.DIAMOND_BOOTS, "1", "enchanted")),
						lv("Master", sell("13-27", Items.DIAMOND_HELMET, "1", "enchanted"), sell("21-35", Items.DIAMOND_CHESTPLATE, "1", "enchanted")))),
				new Profession("Toolsmith", Items.SMITHING_TABLE, "Smithing Table", List.of(
						lv("Novice", buy(Items.COAL, "15"), sell("1", Items.STONE_AXE, "1"), sell("1", Items.STONE_SHOVEL, "1"), sell("1", Items.STONE_PICKAXE, "1"), sell("1", Items.STONE_HOE, "1")),
						lv("Apprentice", buy(Items.IRON_INGOT, "4"), sell("36", Items.BELL, "1")),
						lv("Journeyman", buy(Items.FLINT, "30"), sell("1-15", Items.IRON_PICKAXE, "1", "enchanted"), sell("4", Items.DIAMOND_HOE, "1")),
						lv("Expert", buy(Items.DIAMOND, "1"), sell("17-31", Items.DIAMOND_AXE, "1", "enchanted"), sell("10-24", Items.DIAMOND_SHOVEL, "1", "enchanted")),
						lv("Master", sell("18-32", Items.DIAMOND_PICKAXE, "1", "enchanted")))),
				new Profession("Weaponsmith", Items.GRINDSTONE, "Grindstone", List.of(
						lv("Novice", buy(Items.COAL, "15"), sell("3", Items.IRON_AXE, "1"), sell("2-16", Items.IRON_SWORD, "1", "enchanted")),
						lv("Apprentice", buy(Items.IRON_INGOT, "4"), sell("36", Items.BELL, "1")),
						lv("Journeyman", buy(Items.FLINT, "24")),
						lv("Expert", buy(Items.DIAMOND, "1"), sell("17-31", Items.DIAMOND_AXE, "1", "enchanted")),
						lv("Master", sell("13-27", Items.DIAMOND_SWORD, "1", "enchanted")))),
				new Profession("Cleric", Items.BREWING_STAND, "Brewing Stand", List.of(
						lv("Novice", buy(Items.ROTTEN_FLESH, "32"), sell("1", Items.REDSTONE, "2")),
						lv("Apprentice", buy(Items.GOLD_INGOT, "3"), sell("1", Items.LAPIS_LAZULI, "1")),
						lv("Journeyman", buy(Items.RABBIT_FOOT, "2"), sell("4", Items.GLOWSTONE, "1")),
						lv("Expert", buy(Items.TURTLE_SCUTE, "4"), buy(Items.GLASS_BOTTLE, "9"), sell("5", Items.ENDER_PEARL, "1")),
						lv("Master", buy(Items.NETHER_WART, "22"), sell("3", Items.EXPERIENCE_BOTTLE, "1")))),
				new Profession("Farmer", Items.COMPOSTER, "Composter", List.of(
						lv("Novice", buy(Items.WHEAT, "20"), buy(Items.POTATO, "26"), buy(Items.CARROT, "22"), buy(Items.BEETROOT, "15"), sell("1", Items.BREAD, "6")),
						lv("Apprentice", buy(Items.PUMPKIN, "6"), sell("1", Items.PUMPKIN_PIE, "4"), sell("1", Items.APPLE, "4")),
						lv("Journeyman", buy(Items.MELON, "4"), sell("3", Items.COOKIE, "18")),
						lv("Expert", sell("1", Items.CAKE, "1"), sell("1", Items.SUSPICIOUS_STEW, "1")),
						lv("Master", sell("3", Items.GOLDEN_CARROT, "3"), sell("4", Items.GLISTERING_MELON_SLICE, "3")))),
				new Profession("Fletcher", Items.FLETCHING_TABLE, "Fletching Table", List.of(
						lv("Novice", buy(Items.STICK, "32"), sell("1", Items.ARROW, "16"), sell2("1", Items.GRAVEL, "10", Items.FLINT, "10")),
						lv("Apprentice", buy(Items.FLINT, "26"), sell("2", Items.BOW, "1")),
						lv("Journeyman", buy(Items.STRING, "14"), sell("3", Items.CROSSBOW, "1")),
						lv("Expert", buy(Items.FEATHER, "24"), sell("7-21", Items.BOW, "1", "enchanted")),
						lv("Master", buy(Items.TRIPWIRE_HOOK, "8"), sell("8-22", Items.CROSSBOW, "1", "enchanted"), sell2("2", Items.ARROW, "5", Items.TIPPED_ARROW, "5")))),
				new Profession("Cartographer", Items.CARTOGRAPHY_TABLE, "Cartography Table", List.of(
						lv("Novice", buy(Items.PAPER, "24"), sell("7", Items.MAP, "1")),
						lv("Apprentice", buy(Items.GLASS_PANE, "11"), sell2("13", Items.COMPASS, "1", Items.FILLED_MAP, "1")),
						lv("Journeyman", buy(Items.COMPASS, "1"), sell2("14", Items.COMPASS, "1", Items.FILLED_MAP, "1")),
						lv("Expert", sell("7", Items.ITEM_FRAME, "1")),
						lv("Master", sell("8", Items.GLOBE_BANNER_PATTERN, "1")))),
				new Profession("Fisherman", Items.BARREL, "Barrel", List.of(
						lv("Novice", buy(Items.STRING, "20"), buy(Items.COAL, "10"), sell2("1", Items.COD, "6", Items.COOKED_COD, "6"), sell("3", Items.COD_BUCKET, "1")),
						lv("Apprentice", buy(Items.COD, "15"), sell2("1", Items.SALMON, "6", Items.COOKED_SALMON, "6"), sell("2", Items.CAMPFIRE, "1")),
						lv("Journeyman", buy(Items.SALMON, "13"), sell("7-22", Items.FISHING_ROD, "1", "enchanted")),
						lv("Expert", buy(Items.TROPICAL_FISH, "6")),
						lv("Master", buy(Items.PUFFERFISH, "4"), buy(Items.OAK_BOAT, "1")))),
				new Profession("Butcher", Items.SMOKER, "Smoker", List.of(
						lv("Novice", buy(Items.CHICKEN, "14"), buy(Items.PORKCHOP, "7"), buy(Items.RABBIT, "4"), sell("1", Items.RABBIT_STEW, "1")),
						lv("Apprentice", buy(Items.COAL, "15"), sell("1", Items.COOKED_PORKCHOP, "5"), sell("1", Items.COOKED_CHICKEN, "8")),
						lv("Journeyman", buy(Items.MUTTON, "7"), buy(Items.BEEF, "10")),
						lv("Expert", buy(Items.DRIED_KELP_BLOCK, "10")),
						lv("Master", buy(Items.SWEET_BERRIES, "10")))),
				new Profession("Leatherworker", Items.CAULDRON, "Cauldron", List.of(
						lv("Novice", buy(Items.LEATHER, "6"), sell("3", Items.LEATHER_LEGGINGS, "1"), sell("7", Items.LEATHER_CHESTPLATE, "1")),
						lv("Apprentice", buy(Items.FLINT, "26"), sell("5", Items.LEATHER_HELMET, "1"), sell("4", Items.LEATHER_BOOTS, "1")),
						lv("Journeyman", buy(Items.RABBIT_HIDE, "9"), sell("7", Items.LEATHER_CHESTPLATE, "1")),
						lv("Expert", buy(Items.TURTLE_SCUTE, "4"), sell("6", Items.LEATHER_HORSE_ARMOR, "1")),
						lv("Master", sell("6", Items.SADDLE, "1"), sell("5", Items.LEATHER_HELMET, "1")))),
				new Profession("Mason", Items.STONECUTTER, "Stonecutter", List.of(
						lv("Novice", buy(Items.CLAY_BALL, "10"), sell("1", Items.BRICK, "10")),
						lv("Apprentice", buy(Items.STONE, "20"), sell("1", Items.CHISELED_STONE_BRICKS, "4")),
						lv("Journeyman", buy(Items.GRANITE, "16"), buy(Items.ANDESITE, "16"), buy(Items.DIORITE, "16"), sell("1", Items.POLISHED_ANDESITE, "4")),
						lv("Expert", buy(Items.QUARTZ, "12"), sell("1", Items.TERRACOTTA, "1")),
						lv("Master", sell("1", Items.QUARTZ_PILLAR, "1"), sell("1", Items.QUARTZ_BLOCK, "1")))),
				new Profession("Shepherd", Items.LOOM, "Loom", List.of(
						lv("Novice", sell("2", Items.SHEARS, "1", "buys 18 wool → 1 emerald")),
						lv("Apprentice", new Trade(null, null, null, null, E, "1", "buys 12 dye; sells wool & carpets")),
						lv("Journeyman", new Trade(null, null, null, null, E, "3", "sells beds (any color)")),
						lv("Expert", new Trade(null, null, null, null, E, "3", "sells banners")),
						lv("Master", sell("2", Items.PAINTING, "3")))));
	}

	static List<Enchant> enchants() {
		return List.of(
				new Enchant(Items.DIAMOND_HELMET, "Helmet", "Protection IV · Unbreaking III · Mending · Respiration III · Aqua Affinity", "Thorns III"),
				new Enchant(Items.DIAMOND_CHESTPLATE, "Chestplate", "Protection IV · Unbreaking III · Mending", "Thorns III"),
				new Enchant(Items.DIAMOND_LEGGINGS, "Leggings", "Protection IV · Unbreaking III · Mending · Swift Sneak III", ""),
				new Enchant(Items.DIAMOND_BOOTS, "Boots", "Protection IV · Feather Falling IV · Depth Strider III · Soul Speed III · Unbreaking III · Mending", "Frost Walker II instead of Depth Strider"),
				new Enchant(Items.DIAMOND_SWORD, "Sword", "Sharpness V · Looting III · Sweeping Edge III · Fire Aspect II · Unbreaking III · Mending", "Knockback II"),
				new Enchant(Items.DIAMOND_AXE, "Axe", "Sharpness V · Efficiency V · Unbreaking III · Mending", "Silk Touch or Fortune III"),
				new Enchant(Items.DIAMOND_PICKAXE, "Pickaxe", "Efficiency V · Fortune III · Unbreaking III · Mending", "Silk Touch instead of Fortune"),
				new Enchant(Items.DIAMOND_SHOVEL, "Shovel", "Efficiency V · Unbreaking III · Mending", "Silk Touch or Fortune III"),
				new Enchant(Items.BOW, "Bow", "Power V · Punch II · Flame · Unbreaking III · Infinity", "Mending instead of Infinity"),
				new Enchant(Items.CROSSBOW, "Crossbow", "Quick Charge III · Multishot · Unbreaking III · Mending", "Piercing IV instead of Multishot"),
				new Enchant(Items.TRIDENT, "Trident", "Loyalty III · Channeling · Impaling V · Unbreaking III · Mending", "Riptide III instead of Loyalty + Channeling"),
				new Enchant(Items.MACE, "Mace", "Density V · Wind Burst III · Unbreaking III · Mending", "Breach IV instead of Density"),
				new Enchant(Items.FISHING_ROD, "Fishing Rod", "Luck of the Sea III · Lure III · Unbreaking III · Mending", ""),
				new Enchant(Items.ELYTRA, "Elytra", "Unbreaking III · Mending", ""),
				new Enchant(Items.SHIELD, "Shield", "Unbreaking III · Mending", ""));
	}

	static List<Potion> potions() {
		return List.of(
				new Potion(Items.GLISTERING_MELON_SLICE, "Healing", 0xFFF82423, "Awkward", "Melon slice + 8 gold nuggets", "instant", "–", "II"),
				new Potion(Items.GHAST_TEAR, "Regeneration", 0xFFCD5CAB, "Awkward", "Ghasts (Nether)", "0:45", "1:30", "II 0:22"),
				new Potion(Items.BLAZE_POWDER, "Strength", 0xFFFFC700, "Awkward", "Blaze rods (Nether fortress)", "3:00", "8:00", "II 1:30"),
				new Potion(Items.SUGAR, "Swiftness", 0xFF33EBFF, "Awkward", "Sugar cane", "3:00", "8:00", "II 1:30"),
				new Potion(Items.MAGMA_CREAM, "Fire Resistance", 0xFFFF9900, "Awkward", "Magma cubes / slime + blaze powder", "3:00", "8:00", "–"),
				new Potion(Items.GOLDEN_CARROT, "Night Vision", 0xFFC2FF66, "Awkward", "Carrot + 8 gold nuggets", "3:00", "8:00", "–"),
				new Potion(Items.FERMENTED_SPIDER_EYE, "Invisibility", 0xFFF6F6F6, "Night Vision", "Spider eye + sugar + brown mushroom", "3:00", "8:00", "–"),
				new Potion(Items.PUFFERFISH, "Water Breathing", 0xFF98DAC0, "Awkward", "Fishing / warm oceans", "3:00", "8:00", "–"),
				new Potion(Items.RABBIT_FOOT, "Leaping", 0xFFFDFF84, "Awkward", "Rabbits", "3:00", "8:00", "II 1:30"),
				new Potion(Items.PHANTOM_MEMBRANE, "Slow Falling", 0xFFF3CFB9, "Awkward", "Phantoms", "1:30", "4:00", "–"),
				new Potion(Items.TURTLE_HELMET, "Turtle Master", 0xFF8E6E9E, "Awkward", "Turtle scutes", "0:20", "0:40", "II 0:20"),
				new Potion(Items.BREEZE_ROD, "Wind Charging", 0xFFBDC9FF, "Awkward", "Breezes (trial chambers)", "3:00", "–", "–"),
				new Potion(Items.SPIDER_EYE, "Poison", 0xFF87A363, "Awkward", "Spiders", "0:45", "1:30", "II 0:21"),
				new Potion(Items.FERMENTED_SPIDER_EYE, "Weakness", 0xFF484D48, "Water", "Spider eye + sugar + brown mushroom", "1:30", "4:00", "–"),
				new Potion(Items.FERMENTED_SPIDER_EYE, "Slowness", 0xFF8BAFE0, "Swiftness/Leaping", "Corrupts the potion", "1:30", "4:00", "IV 0:20"),
				new Potion(Items.FERMENTED_SPIDER_EYE, "Harming", 0xFFA9656A, "Healing/Poison", "Corrupts the potion", "instant", "–", "II"));
	}
}
