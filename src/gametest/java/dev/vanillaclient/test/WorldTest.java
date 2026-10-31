package dev.vanillaclient.test;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.features.ChestTools;
import dev.vanillaclient.map.Waypoints;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;

/** CI only: joins a world, turns features on and takes screenshots of everything in-game. */
public class WorldTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		ctx.runOnClient(mc -> {
			Config c = Config.get();
			c.cosmetics.cape = "inferno";
			c.cosmetics.shield = "netherite";
			c.cosmetics.wings = "phoenix";
			c.fps.enabled = true;
			c.biome.enabled = true;
			c.rankHud.enabled = true;
			c.counter.enabled = true;
			c.effects.enabled = true;
			c.light.enabled = true;
			c.ranks.ownNameTag = true;
			c.waypointHud.mode = 1;
			c.crosshair.enabled = true;
			c.crosshair.outline = true;
			c.crosshair.outlineThickness = 2;
		});
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			ctx.waitTicks(120);
			TestServerContext s = sp.getServer();
			s.runCommand("gamemode survival @a");
			s.runCommand("time set noon");
			s.runCommand("weather clear");
			s.runCommand("give @a totem_of_undying 2");
			s.runCommand("give @a golden_apple 5");
			s.runCommand("give @a ender_pearl 16");
			s.runCommand("item replace entity @a weapon.offhand with shield");
			s.runCommand("item replace entity @a weapon.mainhand with diamond_sword");
			s.runCommand("effect give @a speed 300 1");
			s.runCommand("effect give @a regeneration 300 0");
			ctx.waitTicks(20);
			ctx.runOnClient(mc -> {
				var p = mc.player;
				var w = Waypoints.add("Base", p.getBlockX() + 3, p.getBlockY(), p.getBlockZ() + 30);
				if (w != null) w.icon = "campfire";
				Waypoints.add("Mine", p.getBlockX() - 40, p.getBlockY() - 20, p.getBlockZ() - 10);
				p.setYRot(0f);
				p.setXRot(10f);
			});
			ctx.waitTicks(30);
			ctx.takeScreenshot("w01_hud");

			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			ctx.waitTicks(20);
			ctx.takeScreenshot("w02_third_person_cape");
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			ctx.waitTicks(20);
			ctx.takeScreenshot("w03_front_nametag_shield");

			s.runCommand("item replace entity @a armor.chest with elytra");
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			ctx.waitTicks(20);
			ctx.takeScreenshot("w04_wings_phoenix");
			ctx.runOnClient(mc -> Config.get().cosmetics.wings = "none");
			ctx.waitTicks(10);
			ctx.takeScreenshot("w05_wings_vanilla_with_cape");
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

			ctx.setScreen(() -> new net.minecraft.client.gui.screens.PauseScreen(true));
			ctx.waitTicks(10);
			ctx.takeScreenshot("w06_pause");
			ctx.setScreen(() -> null);

			// chest next to the player, filled with a mess
			int[] pos = ctx.computeOnClient(mc -> new int[] {mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ() + 2});
			String at = pos[0] + " " + pos[1] + " " + pos[2];
			s.runCommand("setblock " + at + " chest");
			String[] items = {"dirt 12", "cobblestone 30", "dirt 40", "oak_planks 5", "cobblestone 64", "iron_ingot 3", "dirt 7", "torch 9", "iron_ingot 20"};
			for (int i = 0; i < items.length; i++) {
				s.runCommand("item replace block " + at + " container." + (i * 3) + " with " + items[i]);
			}
			s.runCommand("give @a dirt 20");
			ctx.waitTicks(10);
			ctx.runOnClient(mc -> {
				net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(pos[0], pos[1], pos[2]);
				mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
						new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(bp), net.minecraft.core.Direction.UP, bp, false));
			});
			ctx.waitTicks(20);
			ctx.takeScreenshot("w07_chest");
			ctx.runOnClient(mc -> ChestTools.sortOpen());
			ctx.waitTicks(10);
			ctx.takeScreenshot("w08_chest_sorted");
			ctx.runOnClient(mc -> {
				if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> cs) ChestTools.store(cs.getMenu());
			});
			ctx.waitTicks(10);
			ctx.takeScreenshot("w08b_chest_stored");
			ctx.setScreen(() -> null);

			ctx.setScreen(() -> new dev.vanillaclient.gui.AdvancementsChecklistScreen(null));
			ctx.waitTicks(20);
			ctx.takeScreenshot("w09_advancements");
			ctx.setScreen(() -> new dev.vanillaclient.gui.SettingsScreen(null));
			ctx.waitTicks(5);
			ctx.setScreen(() -> new dev.vanillaclient.gui.StatsOverviewScreen(null));
			ctx.waitTicks(40);
			ctx.takeScreenshot("w10_stats");
			ctx.setScreen(() -> new dev.vanillaclient.gui.WorldMapScreen(null));
			ctx.waitTicks(30);
			ctx.takeScreenshot("w11_map");
			ctx.setScreen(() -> new dev.vanillaclient.gui.SettingsScreen(null));
			ctx.waitTicks(20);
			ctx.takeScreenshot("w12_settings");
			ctx.setScreen(() -> dev.vanillaclient.gui.CosmeticsScreen.onTab(null, dev.vanillaclient.cosmetics.Cosmetics.Kind.SHIELD));
			ctx.waitTicks(20);
			ctx.takeScreenshot("w13_wardrobe_shield");
			ctx.setScreen(() -> new dev.vanillaclient.gui.TasksScreen(null));
			ctx.waitTicks(10);
			ctx.takeScreenshot("w14_tasks");
			ctx.setScreen(() -> null);
		}
	}
}
