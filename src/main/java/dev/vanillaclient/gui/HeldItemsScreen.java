package dev.vanillaclient.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.ui.Ui;
import dev.vanillaclient.held.HeldItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Every item listed: set the size it has in your hand, one by one. Search and filter by category. */
public final class HeldItemsScreen extends BaseScreen {
	private static String search = "";
	private static int category = -1; // -1 = all, -2 = only changed
	private final Ui.List_ list = new Ui.List_();
	private EditBox searchBox;
	private List<Item> all;

	public HeldItemsScreen(Screen parent) {
		super(Component.literal("Held Items – every item"), parent);
	}

	private int lw() {
		return Math.min(320, this.width - 40);
	}

	@Override
	protected void init() {
		super.init();
		if (all == null) {
			all = new ArrayList<>();
			for (Item it : BuiltInRegistries.ITEM) if (it != Items.AIR) all.add(it);
		}
		int x = this.width / 2 - lw() / 2;
		searchBox = new EditBox(this.font, x, 32, lw() - 124, 18, Component.literal("Search"));
		searchBox.setHint(Component.literal("§7Search items …"));
		searchBox.setValue(search);
		searchBox.setResponder(v -> {
			search = v;
			build();
		});
		addRenderableWidget(searchBox);
		action(x + lw() - 120, 31, 120, filterName(), () -> {
			category++;
			if (category >= HeldItems.CATEGORIES.size()) category = -2;
			rebuildWidgets();
		});
		list.x = x;
		list.w = lw();
		list.top = 56;
		list.bottom = this.height - 34;
		build();
		action(this.width / 2 - 154, this.height - 26, 150, "↺ Reset all items", () -> {
			Config.get().heldItems.items.clear();
			Config.save();
			build();
		});
		action(this.width / 2 + 4, this.height - 26, 150, "✔ Done", this::onClose);
	}

	private String filterName() {
		if (category == -1) return "All categories";
		if (category == -2) return "§eOnly changed";
		return HeldItems.CATEGORIES.get(category).name();
	}

	private void build() {
		list.reset();
		String q = search.toLowerCase(Locale.ROOT).trim();
		var items = Config.get().heldItems.items;
		int shown = 0;
		for (Item it : all) {
			String id = HeldItems.id(it);
			ItemStack st = Icons.stack(it);
			String name = Icons.name(it);
			if (!q.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(q) && !id.contains(q)) continue;
			if (category == -2 && !items.containsKey(id)) continue;
			if (category >= 0 && !st.isEmpty() && HeldItems.categoryOf(st) != HeldItems.CATEGORIES.get(category)) continue;
			if (++shown > 400) {
				list.add(new Ui.Info("… more items – type to search"));
				break;
			}
			list.add(new Ui.Slider(name, 0.2, 1.6, 0.05,
					() -> items.containsKey(id) ? items.get(id) : (st.isEmpty() ? 1f : HeldItems.category(HeldItems.categoryOf(st).id())),
					v -> items.put(id, (float) v),
					v -> (items.containsKey(id) ? "§e" : "§7") + Math.round(v * 100) + "%")
					.icon(st).onReset(() -> items.remove(id)));
		}
		if (shown == 0) list.add(new Ui.Info("Nothing found."));
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.centeredText(this.font, "§l✋ Held Items", this.width / 2, 8, Theme.alpha(Theme.ACCENT, intro()));
		g.centeredText(this.font, "§7Gray = from its category · yellow = own size · right-click a slider to reset", this.width / 2, 19, 0xFFFFFFFF);
		list.render(g, this.font, mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		return list.mouseDown(event.x(), event.y(), event.button());
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		list.mouseUp();
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		if (list.scroll(mx, my, v)) return true;
		return super.mouseScrolled(mx, my, h, v);
	}
}
