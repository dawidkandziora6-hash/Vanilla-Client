package dev.logbuch.hud;

import dev.logbuch.config.LogbuchConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Rüstung im Vanilla-Hotbar-Look: dieselbe Slot-Leiste wie die Hotbar, direkt daneben.
 */
public final class ArmorModule extends HudModule {
	private static final Identifier HOTBAR = Identifier.withDefaultNamespace("hud/hotbar");
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static ItemStack[] preview;

	private static ItemStack[] preview() {
		if (preview == null) {
			preview = new ItemStack[] {
					new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE),
					new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_PICKAXE)};
		}
		return preview;
	}

	public ArmorModule() {
		super("armor", "Rüstung");
	}

	@Override
	public LogbuchConfig.Armor settings() {
		return LogbuchConfig.get().armor;
	}

	private int slotCount() {
		return settings().showHeldItem ? 5 : 4;
	}

	private ItemStack stack(int i, boolean editing) {
		Player p = Minecraft.getInstance().player;
		ItemStack s = ItemStack.EMPTY;
		if (p != null) {
			s = i < 4 ? p.getItemBySlot(ARMOR[i]) : p.getItemBySlot(EquipmentSlot.MAINHAND);
		}
		if (editing && !anyArmor()) {
			return preview()[i];
		}
		return s;
	}

	private boolean anyArmor() {
		Player p = Minecraft.getInstance().player;
		if (p == null) return false;
		for (EquipmentSlot slot : ARMOR) {
			if (!p.getItemBySlot(slot).isEmpty()) return true;
		}
		return false;
	}

	@Override
	public boolean hasContent() {
		return !settings().hideWhenEmpty || anyArmor();
	}

	@Override
	public boolean fixedPosition() {
		return settings().attachToHotbar;
	}

	@Override
	public boolean canRecolor() {
		return false;
	}

	@Override
	public int[] fixedOrigin(int screenW, int screenH) {
		LogbuchConfig.Armor s = settings();
		Minecraft mc = Minecraft.getInstance();
		int center = screenW / 2;
		int w = width(false);
		boolean offhandLeft = false;
		boolean offhandRight = false;
		if (mc.player != null && !mc.player.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
			// Vanilla zeichnet die Zweithand auf der Seite gegenüber der Haupthand.
			boolean mainRight = mc.player.getMainArm() == HumanoidArm.RIGHT;
			offhandLeft = mainRight;
			offhandRight = !mainRight;
		}
		int x = s.rightSide
				? center + 91 + 6 + (offhandRight ? 29 : 0)
				: center - 91 - 6 - w - (offhandLeft ? 29 : 0);
		return new int[] {x, screenH - 22};
	}

	@Override
	public int width(boolean editing) {
		return 2 + 20 * slotCount();
	}

	@Override
	public int height(boolean editing) {
		return 22;
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		LogbuchConfig.Armor s = settings();
		int n = slotCount();
		// Linker Rand + n Slots aus der echten Hotbar-Textur, dann der rechte Rand.
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 0, 0, 0, 0, 1 + 20 * n, 22);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, 182, 22, 181, 0, 1 + 20 * n, 0, 1, 22);

		Font font = Minecraft.getInstance().font;
		for (int i = 0; i < n; i++) {
			ItemStack st = stack(i, editing);
			if (st.isEmpty()) continue;
			int ix = 3 + 20 * i;
			int iy = 3;
			g.item(st, ix, iy);
			g.itemDecorations(font, st, ix, iy);
			if (s.durabilityText != 0 && st.isDamageableItem()) {
				int max = st.getMaxDamage();
				int left = max - st.getDamageValue();
				String t = s.durabilityText == 1 ? (left * 100 / Math.max(1, max)) + "%" : String.valueOf(left);
				float f = left / (float) Math.max(1, max);
				int color = f > 0.5f ? 0xFF55FF55 : (f > 0.2f ? 0xFFFFFF55 : 0xFFFF5555);
				g.pose().pushMatrix();
				g.pose().translate(ix + 8, -6);
				g.pose().scale(0.75f, 0.75f);
				g.text(font, t, -font.width(t) / 2, 0, color, true);
				g.pose().popMatrix();
			}
		}
	}
}
