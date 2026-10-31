package dev.vanillaclient.hud;

import java.util.ArrayList;
import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Icons;
import dev.vanillaclient.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Task list at the screen edge. */
public final class TodoModule extends HudModule {
	private static final int LINE = 10;

	private record Line(int indent, boolean checkbox, boolean done, String text, ItemStack icon) {}

	public TodoModule() {
		super("todo", "Tasks");
	}

	@Override
	public Config.Todo settings() {
		return Config.get().todo;
	}

	@Override
	public ItemStack icon() {
		return Icons.stack(Items.WRITABLE_BOOK);
	}

	public static List<Config.TodoItem> visibleItems() {
		Config cfg = Config.get();
		List<Config.TodoItem> out = new ArrayList<>();
		for (Config.TodoItem t : cfg.todos) {
			if (cfg.todo.hideDone && t.done) continue;
			if (!cfg.todo.categoryFilter.isEmpty() && !cfg.todo.categoryFilter.equals(t.category)) continue;
			out.add(t);
		}
		// Urgent first, done last – otherwise your own order stays.
		out.sort((a, b) -> {
			if (a.done != b.done) return a.done ? 1 : -1;
			return Integer.compare(b.priority, a.priority);
		});
		return out;
	}

	private List<Line> lines(boolean editing) {
		Config cfg = Config.get();
		Config.Todo s = settings();
		List<Config.TodoItem> items = visibleItems();
		List<Line> out = new ArrayList<>();
		if (s.showTitle) {
			long done = cfg.todos.stream().filter(t -> t.done).count();
			String filter = s.categoryFilter.isEmpty() ? "" : " §7[" + s.categoryFilter + "]§r";
			out.add(new Line(0, false, false, "§l" + s.title + "§r" + filter + " §7" + done + "/" + cfg.todos.size(), null));
		}
		if (items.isEmpty() && editing) {
			out.add(new Line(0, true, false, "Build iron farm", Icons.categoryIcon(0)));
			out.add(new Line(0, true, true, "§7§mCollect beds", Icons.categoryIcon(2)));
			return out;
		}
		int budget = Math.max(1, s.maxLines);
		int shownItems = 0;
		for (Config.TodoItem t : items) {
			if (budget <= 0) break;
			StringBuilder text = new StringBuilder();
			if (t.priority == 2) text.append("§c! ");
			else if (t.priority == 1) text.append("§6! ");
			text.append(t.done ? "§7§m" : "§r").append(t.text).append("§r");
			if (!t.subtasks.isEmpty()) {
				long d = t.subtasks.stream().filter(st -> st.done).count();
				text.append(" §7(").append(d).append("/").append(t.subtasks.size()).append(")");
			}
			if (s.showCoords && !t.coords.isBlank()) {
				text.append(" §8⌖ ").append(t.coords.trim());
			}
			Config.Category cat = cfg.category(t.category);
			out.add(new Line(0, true, t.done, text.toString(), cat == null ? null : Icons.categoryIcon(cat.icon)));
			budget--;
			shownItems++;
			if (s.showSubtasks && !t.done) {
				for (Config.SubTask st : t.subtasks) {
					if (budget <= 0) break;
					if (s.hideDone && st.done) continue;
					out.add(new Line(12, true, st.done, (st.done ? "§7§m" : "§7") + st.text, null));
					budget--;
				}
			}
		}
		int hidden = items.size() - shownItems;
		if (hidden > 0) {
			out.add(new Line(0, false, false, "§7+" + hidden + " more …", null));
		}
		return out;
	}

	@Override
	public boolean hasContent() {
		return !visibleItems().isEmpty();
	}

	@Override
	public int width(boolean editing) {
		Font font = Minecraft.getInstance().font;
		int w = 0;
		for (Line l : lines(editing)) {
			w = Math.max(w, l.indent + (l.checkbox ? 11 : 0) + (l.icon != null ? 10 : 0) + font.width(l.text));
		}
		return Math.max(w, 40);
	}

	@Override
	public int height(boolean editing) {
		return Math.max(LINE, lines(editing).size() * LINE - 2);
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		Config.Todo s = settings();
		Font font = Minecraft.getInstance().font;
		background(g, s, width(editing), height(editing));
		int y = 0;
		for (Line l : lines(editing)) {
			int x = l.indent;
			if (l.checkbox) {
				Theme.checkbox(g, x, y, l.done, s.color);
				x += 11;
			}
			if (l.icon != null) {
				Icons.small(g, l.icon, x, y);
				x += 10;
			}
			g.text(font, l.text, x, y, s.color, s.shadow);
			y += LINE;
		}
	}
}
