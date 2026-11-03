package dev.aatracker.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** CI only: tracker screen, chat line and L key in a real world. */
public class TrackerTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			var s = sp.getServer();
			ctx.waitTicks(140);
			for (String a : new String[] {"story/root", "story/mine_stone", "story/upgrade_tools", "nether/root", "adventure/root", "husbandry/root"})
				s.runCommand("advancement grant @a only minecraft:" + a);
			s.runCommand("advancement grant @a only minecraft:adventure/adventuring_time minecraft:plains");
			ctx.waitTicks(20);
			s.runCommand("advancement grant @a only minecraft:story/smelt_iron");
			ctx.waitTicks(40);
			ctx.takeScreenshot("aa1_chat");
			ctx.getInput().pressKey(o -> o.keyAdvancements);
			ctx.waitTicks(30);
			ctx.takeScreenshot("aa2_tracker");
			ctx.runOnClient(mc -> dev.aatracker.TrackerScreen.names = true);
			ctx.setScreen(() -> new dev.aatracker.TrackerScreen(null));
			ctx.waitTicks(30);
			ctx.takeScreenshot("aa3_names");
			ctx.setScreen(() -> null);
		}
	}
}
