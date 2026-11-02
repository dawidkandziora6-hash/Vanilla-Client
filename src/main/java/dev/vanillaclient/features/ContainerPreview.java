package dev.vanillaclient.features;

import java.util.List;
import java.util.Optional;

import dev.vanillaclient.config.Config;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

/** Shulker box and ender chest preview in the item tooltip. */
public final class ContainerPreview {
	private ContainerPreview() {}

	public record Data(List<ItemStack> items, int color, String note) implements TooltipComponent {}

	private static final String[][] DYES = {{"white", "F9FFFE"}, {"orange", "F9801D"}, {"magenta", "C74EBD"}, {"light_blue", "3AB3DA"},
			{"yellow", "FED83D"}, {"lime", "80C71F"}, {"pink", "F38BAA"}, {"gray", "474F52"}, {"light_gray", "9D9D97"}, {"cyan", "169C9C"},
			{"purple", "8932B8"}, {"blue", "3C44AA"}, {"brown", "835432"}, {"green", "5E7C16"}, {"red", "B02E26"}, {"black", "1D1D21"}};

	public static Optional<TooltipComponent> image(ItemStack stack) {
		Config.Preview p = Config.get().preview;
		if (!p.enabled || stack.isEmpty()) return Optional.empty();
		if (p.shiftOnly && !net.minecraft.client.Minecraft.getInstance().hasShiftDown()) return Optional.empty();
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		if (path.endsWith("shulker_box")) {
			ItemContainerContents c = stack.get(DataComponents.CONTAINER);
			if (c == null || c.nonEmptyItemCopyStream().findAny().isEmpty()) return Optional.empty();
			NonNullList<ItemStack> list = NonNullList.withSize(27, ItemStack.EMPTY);
			c.copyInto(list);
			int col = 0xFF8E5F8E;
			for (String[] d : DYES) if (path.equals(d[0] + "_shulker_box")) col = 0xFF000000 | Integer.parseInt(d[1], 16);
			return Optional.of(new Data(list, col, null));
		}
		if (stack.is(Items.ENDER_CHEST) && p.enderChest) {
			List<ItemStack> items = EnderChestCache.items();
			if (items == null) return Optional.of(new Data(List.of(), 0xFF1E3A3A, "Open your ender chest once to see it here"));
			return Optional.of(new Data(items, 0xFF1E3A3A, null));
		}
		return Optional.empty();
	}

	public static ClientTooltipComponent client(TooltipComponent data) {
		return data instanceof Data d ? new Client(d) : null;
	}

	private static final class Client implements ClientTooltipComponent {
		private final Data d;

		Client(Data d) {
			this.d = d;
		}

		private int rows() {
			return d.note() != null ? 0 : Math.max(1, (d.items().size() + 8) / 9);
		}

		@Override
		public int getHeight(Font font) {
			return d.note() != null ? 12 : rows() * 18 + 6;
		}

		@Override
		public int getWidth(Font font) {
			return d.note() != null ? font.width(d.note()) + 4 : 9 * 18 + 4;
		}

		@Override
		public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor g) {
			if (d.note() != null) {
				g.text(font, "§7" + d.note(), x, y + 2, 0xFFFFFFFF, false);
				return;
			}
			int c = d.color();
			int bg = 0xE0000000 | (((c >> 16) & 0xFF) / 3 << 16) | (((c >> 8) & 0xFF) / 3 << 8) | ((c & 0xFF) / 3);
			g.fill(x, y + 1, x + 9 * 18 + 4, y + rows() * 18 + 5, bg);
			g.outline(x, y + 1, 9 * 18 + 4, rows() * 18 + 4, 0xFF000000 | c);
			for (int i = 0; i < d.items().size(); i++) {
				int sx = x + 2 + (i % 9) * 18, sy = y + 3 + (i / 9) * 18;
				g.fill(sx, sy, sx + 18, sy + 18, 0x40000000);
				g.fill(sx, sy, sx + 18, sy + 1, 0x30000000);
				ItemStack st = d.items().get(i);
				if (st.isEmpty()) continue;
				g.item(st, sx + 1, sy + 1);
				g.itemDecorations(font, st, sx + 1, sy + 1);
			}
		}
	}
}
