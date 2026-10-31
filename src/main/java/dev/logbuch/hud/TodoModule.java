package dev.logbuch.hud;

import java.util.ArrayList;
import java.util.List;

import dev.logbuch.config.LogbuchConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Aufgabenliste am Bildschirmrand. */
public final class TodoModule extends HudModule {
	private static final int LINE = 10;

	private record Line(int indent, boolean checkbox, boolean done, String text) {}

	public TodoModule() {
		super("todo", "Aufgaben");
	}

	@Override
	public LogbuchConfig.Todo settings() {
		return LogbuchConfig.get().todo;
	}

	public static List<LogbuchConfig.TodoItem> visibleItems() {
		LogbuchConfig cfg = LogbuchConfig.get();
		List<LogbuchConfig.TodoItem> out = new ArrayList<>();
		for (LogbuchConfig.TodoItem t : cfg.todos) {
			if (cfg.todo.hideDone && t.done) continue;
			if (!cfg.todo.categoryFilter.isEmpty() && !cfg.todo.categoryFilter.equals(t.category)) continue;
			out.add(t);
		}
		// Dringendes zuerst, Erledigtes ans Ende – die eigene Reihenfolge bleibt sonst erhalten.
		out.sort((a, b) -> {
			if (a.done != b.done) return a.done ? 1 : -1;
			return Integer.compare(b.priority, a.priority);
		});
		return out;
	}

	private List<Line> lines(boolean editing) {
		LogbuchConfig.Todo s = settings();
		List<LogbuchConfig.TodoItem> items = visibleItems();
		List<Line> out = new ArrayList<>();
		if (s.showTitle) {
			long done = LogbuchConfig.get().todos.stream().filter(t -> t.done).count();
			String filter = s.categoryFilter.isEmpty() ? "" : " §7[" + s.categoryFilter + "]§r";
			out.add(new Line(0, false, false, "§l" + s.title + "§r" + filter + " §7" + done + "/" + LogbuchConfig.get().todos.size()));
		}
		if (items.isEmpty() && editing) {
			out.add(new Line(0, true, false, "Eisenfarm bauen"));
			out.add(new Line(0, true, true, "§7§mBetten sammeln"));
			return out;
		}
		int budget = Math.max(1, s.maxLines);
		int shownItems = 0;
		for (LogbuchConfig.TodoItem t : items) {
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
				text.append(" §8").append(t.coords.trim());
			}
			out.add(new Line(0, true, t.done, text.toString()));
			budget--;
			shownItems++;
			if (s.showSubtasks && !t.done) {
				for (LogbuchConfig.SubTask st : t.subtasks) {
					if (budget <= 0) break;
					if (s.hideDone && st.done) continue;
					out.add(new Line(10, true, st.done, (st.done ? "§7§m" : "§7") + st.text));
					budget--;
				}
			}
		}
		int hidden = items.size() - shownItems;
		if (hidden > 0) {
			out.add(new Line(0, false, false, "§7+" + hidden + " weitere …"));
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
			w = Math.max(w, l.indent + (l.checkbox ? 11 : 0) + font.width(l.text));
		}
		return Math.max(w, 40);
	}

	@Override
	public int height(boolean editing) {
		return Math.max(LINE, lines(editing).size() * LINE - 2);
	}

	@Override
	public void draw(GuiGraphicsExtractor g, boolean editing) {
		LogbuchConfig.Todo s = settings();
		Font font = Minecraft.getInstance().font;
		background(g, s, width(editing), height(editing));
		int y = 0;
		for (Line l : lines(editing)) {
			int x = l.indent;
			if (l.checkbox) {
				drawCheckbox(g, x, y, l.done, s.color);
				x += 11;
			}
			g.text(font, l.text, x, y, s.color, s.shadow);
			y += LINE;
		}
	}

	/** Kleines 8×8-Kästchen im Pixel-Stil. */
	static void drawCheckbox(GuiGraphicsExtractor g, int x, int y, boolean done, int color) {
		int frame = (color & 0x00FFFFFF) | 0xC0000000;
		g.fill(x, y, x + 8, y + 1, frame);
		g.fill(x, y + 7, x + 8, y + 8, frame);
		g.fill(x, y, x + 1, y + 8, frame);
		g.fill(x + 7, y, x + 8, y + 8, frame);
		if (done) {
			int ok = 0xFF55FF55;
			g.fill(x + 2, y + 4, x + 3, y + 6, ok);
			g.fill(x + 3, y + 5, x + 4, y + 7, ok);
			g.fill(x + 4, y + 4, x + 5, y + 6, ok);
			g.fill(x + 5, y + 2, x + 6, y + 5, ok);
		}
	}
}
