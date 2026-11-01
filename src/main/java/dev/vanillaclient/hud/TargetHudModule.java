package dev.vanillaclient.hud;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Icons;
import dev.vanillaclient.pvp.Combat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shows the name, health and armor of whatever you hit last. */
public final class TargetHudModule extends HudModule {
	private static final int W = 128, H = 36;
	private float shownHealth = -1;
	private int shownId = -1;

	public TargetHudModule() {
		super("target", "Target HUD");
	}

	@Override
	public Config.TargetHud settings() {
		return Config.get().targetHud;
	}

	private LivingEntity target(boolean editing) {
		Config.TargetHud s = settings();
		LivingEntity t = Combat.target(s.seconds);
		if (t != null && s.playersOnly && !(t instanceof Player)) t = null;
		if (t == null && editing) t = Minecraft.getInstance().player;
		return t;
	}

	@Override
	public boolean hasContent() {
		return target(false) != null;
	}

	@Override
	public int width(boolean editing) {
		return W;
	}

	@Override
	public int height(boolean editing) {
		return H;
	}

	@Override
	public ItemStack icon() {
		return Icons.stack(Items.PLAYER_HEAD);
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		Config.TargetHud s = settings();
		LivingEntity t = target(editing);
		Font f = Minecraft.getInstance().font;
		if (t == null) return;
		if (s.background) g.fill(0, 0, W, H, s.backgroundColor);
		// head (players) or a spawn-egg-like icon
		int tx = 4;
		if (t instanceof AbstractClientPlayer p) {
			var tex = p.getSkin().body().texturePath();
			g.blit(RenderPipelines.GUI_TEXTURED, tex, 4, 4, 8f, 8f, 28, 28, 8, 8, 64, 64);
			g.blit(RenderPipelines.GUI_TEXTURED, tex, 4, 4, 40f, 8f, 28, 28, 8, 8, 64, 64);
			tx = 36;
		} else {
			Icons.scaled(g, Icons.stack(Items.IRON_SWORD), 8, 8, 1.25f);
			tx = 32;
		}
		g.text(f, f.plainSubstrByWidth(t.getName().getString(), W - tx - 4), tx, 4, s.color, s.shadow);
		float hp = t.getHealth(), max = Math.max(1f, t.getMaxHealth()), abs = t.getAbsorptionAmount();
		if (shownId != t.getId() || shownHealth < 0) {
			shownId = t.getId();
			shownHealth = hp;
		}
		shownHealth += (hp - shownHealth) * 0.15f;
		int bw = W - tx - 4, by = 15;
		g.fill(tx, by, tx + bw, by + 5, 0xFF2A2A2A);
		int lag = Math.round(bw * Math.min(1f, shownHealth / max));
		int now = Math.round(bw * Math.min(1f, hp / max));
		g.fill(tx, by, tx + lag, by + 5, 0xFFB03030);
		int col = hp / max > 0.5f ? s.barColor : hp / max > 0.25f ? 0xFFE0B030 : 0xFFE04040;
		g.fill(tx, by, tx + now, by + 5, col);
		if (abs > 0) g.fill(tx, by + 5, tx + Math.round(bw * Math.min(1f, abs / max)), by + 6, 0xFFFFD34D);
		String info = String.format(java.util.Locale.ROOT, "❤ %.1f", (hp + abs) / 2f);
		if (s.showDistance && Minecraft.getInstance().player != null && t != Minecraft.getInstance().player) {
			info += String.format(java.util.Locale.ROOT, "  §7%.1fm", Math.sqrt(t.distanceToSqr(Minecraft.getInstance().player)));
		}
		g.text(f, info, tx, 24, 0xFFFF7070, s.shadow);
		if (s.showArmor) {
			EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
			int ax = W - 4;
			for (int i = slots.length - 1; i >= 0; i--) {
				ItemStack st = t.getItemBySlot(slots[i]);
				if (st.isEmpty()) continue;
				ax -= 10;
				Icons.scaled(g, st, ax, 23, 0.6f);
			}
		}
	}
}
