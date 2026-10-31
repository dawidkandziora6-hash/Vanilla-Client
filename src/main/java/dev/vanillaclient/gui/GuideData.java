package dev.vanillaclient.gui;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Reference data for the UHC guide (vanilla Java Edition defaults). */
final class GuideData {
	record Line(Item icon, String text) {}

	record Entry(Item icon, String title, String sub, List<Line> lines) {}

	private GuideData() {}

	static Line l(Item i, String t) {
		return new Line(i, t);
	}

	static Line t(String text) {
		return new Line(null, text);
	}

	static List<Entry> villagers() {
		return List.of(
				new Entry(Items.LECTERN, "Librarian", "Workstation: Lectern", List.of(
						l(Items.ENCHANTED_BOOK, "§fSells enchanted books §7– any normal enchant + §bMending"),
						l(Items.BOOKSHELF, "§fBookshelves, lanterns, glass, clock, compass, name tag"),
						l(Items.PAPER, "§7Buys paper, books, ink sacs, book & quill"),
						t("§eTip: §7break & replace the lectern to reroll – before your first trade"))),
				new Entry(Items.BLAST_FURNACE, "Armorer", "Workstation: Blast Furnace", List.of(
						l(Items.IRON_CHESTPLATE, "§fIron & chainmail armor, bell, shield"),
						l(Items.DIAMOND_CHESTPLATE, "§fMaster: §benchanted diamond armor"),
						l(Items.COAL, "§7Buys coal, iron, lava bucket, diamonds"))),
				new Entry(Items.SMITHING_TABLE, "Toolsmith", "Workstation: Smithing Table", List.of(
						l(Items.DIAMOND_PICKAXE, "§fEnchanted iron & §bdiamond tools"),
						l(Items.STONE_PICKAXE, "§fStone tools, bell"),
						l(Items.IRON_INGOT, "§7Buys coal, iron, flint, diamonds"))),
				new Entry(Items.GRINDSTONE, "Weaponsmith", "Workstation: Grindstone", List.of(
						l(Items.DIAMOND_SWORD, "§fEnchanted iron & §bdiamond swords / axes"),
						l(Items.IRON_AXE, "§fIron axe, bell"),
						l(Items.IRON_INGOT, "§7Buys coal, iron, flint, diamonds"))),
				new Entry(Items.BREWING_STAND, "Cleric", "Workstation: Brewing Stand", List.of(
						l(Items.ENDER_PEARL, "§fEnder pearls, glowstone, lapis, redstone"),
						l(Items.EXPERIENCE_BOTTLE, "§fBottles o' Enchanting"),
						l(Items.ROTTEN_FLESH, "§7Buys rotten flesh, gold, rabbit feet, nether wart"))),
				new Entry(Items.COMPOSTER, "Farmer", "Workstation: Composter", List.of(
						l(Items.GOLDEN_CARROT, "§fGolden carrots §7(best food), glistering melon"),
						l(Items.BREAD, "§fBread, pie, apples, cookies, cake, stew"),
						l(Items.WHEAT, "§7Buys wheat, potatoes, carrots, beetroot, pumpkin, melon"))),
				new Entry(Items.FLETCHING_TABLE, "Fletcher", "Workstation: Fletching Table", List.of(
						l(Items.BOW, "§fBows, crossbows (also enchanted)"),
						l(Items.ARROW, "§fArrows, flint, tipped arrows"),
						l(Items.STICK, "§7Buys sticks, flint, string, feathers, tripwire hooks"))),
				new Entry(Items.CARTOGRAPHY_TABLE, "Cartographer", "Workstation: Cartography Table", List.of(
						l(Items.FILLED_MAP, "§fExplorer maps §7(ocean monument, woodland mansion, trial chambers)"),
						l(Items.MAP, "§fEmpty maps, banners, item frames"),
						l(Items.PAPER, "§7Buys paper, glass panes, compasses"))),
				new Entry(Items.BARREL, "Fisherman", "Workstation: Barrel", List.of(
						l(Items.FISHING_ROD, "§fEnchanted fishing rods, cooked fish, campfire"),
						l(Items.COD, "§7Buys string, coal, fish, boats"))),
				new Entry(Items.SMOKER, "Butcher", "Workstation: Smoker", List.of(
						l(Items.COOKED_PORKCHOP, "§fCooked porkchop & chicken, rabbit stew"),
						l(Items.SWEET_BERRIES, "§7Buys raw meat, coal, kelp blocks, sweet berries"))),
				new Entry(Items.CAULDRON, "Leatherworker", "Workstation: Cauldron", List.of(
						l(Items.SADDLE, "§fSaddles, leather armor, leather horse armor"),
						l(Items.LEATHER, "§7Buys leather, flint, rabbit hide, scutes"))),
				new Entry(Items.STONECUTTER, "Mason", "Workstation: Stonecutter", List.of(
						l(Items.QUARTZ_BLOCK, "§fBricks, terracotta, quartz blocks, polished stone"),
						l(Items.CLAY_BALL, "§7Buys clay, stone, granite, diorite, andesite, quartz"))),
				new Entry(Items.LOOM, "Shepherd", "Workstation: Loom", List.of(
						l(Items.SHEARS, "§fShears, wool, carpets, beds, banners, paintings"),
						l(Items.STRING, "§7Buys wool and dyes"))),
				new Entry(Items.EMERALD, "Trading tips", "Cheaper trades", List.of(
						l(Items.GOLDEN_APPLE, "§fCure a zombie villager §7(weakness + golden apple) = big discounts"),
						l(Items.TOTEM_OF_UNDYING, "§fHero of the Village §7after a raid = lower prices"),
						t("§7Villagers restock up to 2× a day at their workstation"),
						t("§7Since 26.1 trades are data-driven; the \"Trade Rebalance\" is still an experiment"))));
	}

	static List<Entry> enchants() {
		return List.of(
				new Entry(Items.DIAMOND_HELMET, "Helmet", "", List.of(
						t("§7Protection IV, Unbreaking III, Mending, Respiration III, Aqua Affinity"))),
				new Entry(Items.DIAMOND_CHESTPLATE, "Chestplate", "", List.of(
						t("§7Protection IV, Unbreaking III, Mending §8(Thorns III optional)"))),
				new Entry(Items.DIAMOND_LEGGINGS, "Leggings", "", List.of(
						t("§7Protection IV, Unbreaking III, Mending, Swift Sneak III"))),
				new Entry(Items.DIAMOND_BOOTS, "Boots", "", List.of(
						t("§7Protection IV, Feather Falling IV, Depth Strider III, Soul Speed III,"),
						t("§7Unbreaking III, Mending §8(Frost Walker instead of Depth Strider)"))),
				new Entry(Items.DIAMOND_SWORD, "Sword", "", List.of(
						t("§7Sharpness V, Looting III, Sweeping Edge III, Fire Aspect II,"),
						t("§7Unbreaking III, Mending §8(Knockback II optional)"))),
				new Entry(Items.DIAMOND_AXE, "Axe", "", List.of(
						t("§7Sharpness V, Efficiency V, Unbreaking III, Mending"))),
				new Entry(Items.DIAMOND_PICKAXE, "Pickaxe / Shovel", "", List.of(
						t("§7Efficiency V, Fortune III §8or§7 Silk Touch, Unbreaking III, Mending"))),
				new Entry(Items.BOW, "Bow", "", List.of(
						t("§7Power V, Punch II, Flame, Unbreaking III, Infinity §8or§7 Mending"))),
				new Entry(Items.CROSSBOW, "Crossbow", "", List.of(
						t("§7Quick Charge III, Multishot §8or§7 Piercing IV, Unbreaking III, Mending"))),
				new Entry(Items.TRIDENT, "Trident", "", List.of(
						t("§7Loyalty III + Channeling §8or§7 Riptide III, Impaling V,"),
						t("§7Unbreaking III, Mending"))),
				new Entry(Items.MACE, "Mace", "", List.of(
						t("§7Density V §8or§7 Breach IV, Wind Burst III, Unbreaking III, Mending"))),
				new Entry(Items.FISHING_ROD, "Fishing rod", "", List.of(
						t("§7Luck of the Sea III, Lure III, Unbreaking III, Mending"))),
				new Entry(Items.ELYTRA, "Elytra / Shield", "", List.of(
						t("§7Unbreaking III, Mending"))),
				new Entry(Items.ENCHANTING_TABLE, "Can't combine", "Pick one of each", List.of(
						t("§7Protection types · Sharpness / Smite / Bane"),
						t("§7Fortune / Silk Touch · Infinity / Mending · Multishot / Piercing"),
						t("§7Depth Strider / Frost Walker · Riptide / Loyalty & Channeling"),
						l(Items.BOOKSHELF, "§715 bookshelves for level 30 enchants"))));
	}

	static List<Entry> brewing() {
		return List.of(
				new Entry(Items.NETHER_WART, "Start: Awkward Potion", "Water bottle + Nether Wart", List.of(
						l(Items.BLAZE_POWDER, "§7Brewing stand fuel: blaze powder"))),
				new Entry(Items.GLISTERING_MELON_SLICE, "Healing", "Glistering Melon", List.of(t("§7Instant health · §eglowstone§7 = II"))),
				new Entry(Items.GHAST_TEAR, "Regeneration", "Ghast Tear", List.of(t("§7§credstone§7 = longer · §eglowstone§7 = II"))),
				new Entry(Items.BLAZE_POWDER, "Strength", "Blaze Powder", List.of(t("§7§credstone§7 = longer · §eglowstone§7 = II"))),
				new Entry(Items.SUGAR, "Swiftness", "Sugar", List.of(t("§7§credstone§7 = longer · §eglowstone§7 = II"))),
				new Entry(Items.MAGMA_CREAM, "Fire Resistance", "Magma Cream", List.of(t("§7Nether essential · §credstone§7 = 8 min"))),
				new Entry(Items.GOLDEN_CARROT, "Night Vision", "Golden Carrot", List.of(
						l(Items.FERMENTED_SPIDER_EYE, "§7+ Fermented Spider Eye = §fInvisibility"))),
				new Entry(Items.PUFFERFISH, "Water Breathing", "Pufferfish", List.of(t("§7§credstone§7 = longer"))),
				new Entry(Items.RABBIT_FOOT, "Leaping", "Rabbit's Foot", List.of(t("§7§credstone§7 = longer · §eglowstone§7 = II"))),
				new Entry(Items.PHANTOM_MEMBRANE, "Slow Falling", "Phantom Membrane", List.of(t("§7§credstone§7 = longer"))),
				new Entry(Items.TURTLE_HELMET, "Turtle Master", "Turtle Shell", List.of(t("§7Resistance + slowness"))),
				new Entry(Items.SPIDER_EYE, "Poison", "Spider Eye", List.of(
						l(Items.FERMENTED_SPIDER_EYE, "§7Poison/Healing + Fermented Eye = §fHarming"))),
				new Entry(Items.FERMENTED_SPIDER_EYE, "Corrupting", "Fermented Spider Eye", List.of(
						t("§7Swiftness/Leaping → §fSlowness§7 · Water → §fWeakness"))),
				new Entry(Items.GUNPOWDER, "Modifiers", "Add after brewing", List.of(
						l(Items.REDSTONE, "§cRedstone§7: longer"),
						l(Items.GLOWSTONE_DUST, "§eGlowstone§7: stronger"),
						l(Items.GUNPOWDER, "§7Gunpowder: splash"),
						l(Items.DRAGON_BREATH, "§7Dragon's Breath: lingering"))));
	}

	static List<Entry> tips() {
		return List.of(
				new Entry(Items.GOLDEN_APPLE, "Golden Apple", "8 gold ingots around an apple", List.of(
						t("§7Absorption + Regeneration II – your best heal"))),
				new Entry(Items.GOLDEN_CARROT, "Best food", "Golden carrots & steak", List.of(
						t("§7Highest saturation – keeps natural regen going"))),
				new Entry(Items.TOTEM_OF_UNDYING, "Totem of Undying", "Evokers in mansions & raids", List.of(
						t("§7Hold it in the offhand – it saves you once"))),
				new Entry(Items.SHIELD, "Shield", "1 iron + 6 planks", List.of(
						t("§7Blocks arrows and melee; axes disable it for 5 s"))),
				new Entry(Items.WATER_BUCKET, "Water bucket", "Always carry one", List.of(
						t("§7Stops fall damage (MLG), puts out fire, blocks lava"))),
				new Entry(Items.DIAMOND, "Diamonds", "Best at Y -59", List.of(
						t("§7Branch-mine with 2 blocks between tunnels"))),
				new Entry(Items.ANCIENT_DEBRIS, "Ancient Debris", "Best at Y 15 in the Nether", List.of(
						t("§7Beds explode in the Nether – great for mining, deadly for you"))),
				new Entry(Items.ENDER_EYE, "Strongholds", "Throw eyes of ender", List.of(
						t("§7Nether travel: 1 block in the Nether = 8 in the Overworld"))));
	}
}
