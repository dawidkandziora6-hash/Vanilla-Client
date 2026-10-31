package dev.logbuch.gui;

import java.util.ArrayList;
import java.util.List;

import dev.logbuch.config.LogbuchConfig;
import dev.logbuch.config.LogbuchConfig.SubTask;
import dev.logbuch.config.LogbuchConfig.TodoItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * Aufgaben planen: Kategorien (Farm, Bau, …), Priorität, Koordinaten und
 * Unteraufgaben (z. B. Materialliste für eine Farm).
 */
public final class TodoScreen extends LbScreen {
	private static final int PER_PAGE = 7;
	private static final List<String> PRIORITIES = List.of("Normal", "§6Wichtig", "§cDringend");

	private static String filter = "";
	private static int page;
	private static TodoItem selected;

	private EditBox addBox;
	private EditBox subBox;
	private EditBox categoryBox;

	public TodoScreen(Screen parent) {
		super(Component.literal("Aufgaben"), parent);
	}

	private LogbuchConfig cfg() {
		return LogbuchConfig.get();
	}

	private int leftX() {
		return this.width / 2 - 205;
	}

	private int rightX() {
		return this.width / 2 + 5;
	}

	private List<TodoItem> list() {
		List<TodoItem> out = new ArrayList<>();
		for (TodoItem t : cfg().todos) {
			if (filter.isEmpty() || filter.equals(t.category)) out.add(t);
		}
		return out;
	}

	@Override
	protected void init() {
		super.init();
		if (selected != null && !cfg().todos.contains(selected)) selected = null;
		int lx = leftX();
		int y = 30;

		// Kategorie-Filter
		List<String> filters = new ArrayList<>();
		filters.add("");
		filters.addAll(cfg().categories);
		if (!filters.contains(filter)) filter = "";
		cycle(lx, y, 120, "Kategorie", filters, () -> filter, v -> {
			filter = v;
			page = 0;
			rebuildWidgets();
		}, v -> v.isEmpty() ? "Alle" : v);
		action(lx + 124, y, 76, "Erledigte −", () -> {
			cfg().todos.removeIf(t -> t.done);
			changed();
			rebuildWidgets();
		});
		y += 24;

		// Neue Aufgabe
		addBox = new EditBox(this.font, lx + 1, y + 1, 174, 18, Component.literal("Neue Aufgabe"));
		addBox.setMaxLength(64);
		addBox.setHint(Component.literal("Neue Aufgabe … (Enter)"));
		addRenderableWidget(addBox);
		action(lx + 180, y, 20, "+", this::addTask);
		y += 24;

		// Liste
		List<TodoItem> items = list();
		int pages = Math.max(1, (items.size() + PER_PAGE - 1) / PER_PAGE);
		page = Math.max(0, Math.min(page, pages - 1));
		for (int i = 0; i < PER_PAGE; i++) {
			int idx = page * PER_PAGE + i;
			if (idx >= items.size()) break;
			TodoItem t = items.get(idx);
			action(lx, y, 20, t.done ? "§a✔" : "", () -> {
				t.done = !t.done;
				changed();
				rebuildWidgets();
			});
			String prefix = t.priority == 2 ? "§c! " : t.priority == 1 ? "§6! " : "";
			String label = prefix + (t.done ? "§7§m" : "") + shorten(t.text, 150);
			if (t == selected) label = "§e» §r" + label;
			Button b = action(lx + 22, y, 178, label, () -> {
				selected = selected == t ? null : t;
				rebuildWidgets();
			});
			b.active = true;
			y += 22;
		}
		if (pages > 1) {
			int ny = 78 + PER_PAGE * 22 + 2;
			action(lx, ny, 30, "◀", () -> {
				page--;
				rebuildWidgets();
			}).active = page > 0;
			action(lx + 170, ny, 30, "▶", () -> {
				page++;
				rebuildWidgets();
			}).active = page < pages - 1;
		}

		if (selected != null) {
			detailPanel(selected);
		} else {
			categoryPanel();
		}
		action(this.width / 2 - 100, this.height - 28, 200, "Fertig", this::onClose);
	}

	private void detailPanel(TodoItem t) {
		int rx = rightX();
		int y = 30;
		EditBox text = new EditBox(this.font, rx + 1, y + 1, 198, 18, Component.literal("Text"));
		text.setMaxLength(64);
		text.setValue(t.text);
		text.setResponder(v -> {
			t.text = v;
			changed();
		});
		addRenderableWidget(text);
		y += 24;
		cycle(rx, y, 98, "Kat.", cfg().categories, () -> cfg().categories.contains(t.category) ? t.category : cfg().categories.get(0),
				v -> t.category = v, v -> v);
		cycle(rx + 102, y, 98, "Prio", List.of(0, 1, 2), () -> t.priority, v -> t.priority = v, PRIORITIES::get);
		y += 24;
		EditBox coords = new EditBox(this.font, rx + 1, y + 1, 146, 18, Component.literal("Koordinaten"));
		coords.setMaxLength(40);
		coords.setValue(t.coords);
		coords.setHint(Component.literal("Koordinaten (optional)"));
		coords.setResponder(v -> {
			t.coords = v;
			changed();
		});
		addRenderableWidget(coords);
		action(rx + 150, y, 50, "Hier", () -> {
			var p = Minecraft.getInstance().player;
			if (p != null) {
				var pos = p.blockPosition();
				t.coords = pos.getX() + " " + pos.getY() + " " + pos.getZ();
				changed();
				rebuildWidgets();
			}
		});
		y += 24;
		List<TodoItem> all = cfg().todos;
		action(rx, y, 48, "▲", () -> move(t, -1)).active = all.indexOf(t) > 0;
		action(rx + 50, y, 48, "▼", () -> move(t, 1)).active = all.indexOf(t) < all.size() - 1;
		action(rx + 102, y, 98, "§cLöschen", () -> {
			all.remove(t);
			selected = null;
			changed();
			rebuildWidgets();
		});
		y += 28;

		// Unteraufgaben
		int shown = 0;
		for (SubTask st : t.subtasks) {
			if (shown >= 4) break;
			action(rx, y, 20, st.done ? "§a✔" : "", () -> {
				st.done = !st.done;
				changed();
				rebuildWidgets();
			});
			action(rx + 22, y, 156, (st.done ? "§7§m" : "") + shorten(st.text, 140), () -> {
				st.done = !st.done;
				changed();
				rebuildWidgets();
			});
			action(rx + 180, y, 20, "§c×", () -> {
				t.subtasks.remove(st);
				changed();
				rebuildWidgets();
			});
			y += 22;
			shown++;
		}
		if (t.subtasks.size() > 4) {
			y += 2;
		}
		subBox = new EditBox(this.font, rx + 1, y + 1, 174, 18, Component.literal("Unteraufgabe"));
		subBox.setMaxLength(48);
		subBox.setHint(Component.literal("Unteraufgabe / Material …"));
		addRenderableWidget(subBox);
		action(rx + 180, y, 20, "+", this::addSubtask);
	}

	private void categoryPanel() {
		int rx = rightX();
		int y = 54;
		categoryBox = new EditBox(this.font, rx + 1, y + 1, 174, 18, Component.literal("Kategorie"));
		categoryBox.setMaxLength(16);
		categoryBox.setHint(Component.literal("Neue Kategorie …"));
		addRenderableWidget(categoryBox);
		action(rx + 180, y, 20, "+", () -> {
			String c = categoryBox.getValue().trim();
			if (!c.isEmpty() && !cfg().categories.contains(c)) {
				cfg().categories.add(c);
				changed();
				rebuildWidgets();
			}
		});
		y += 24;
		for (String c : cfg().categories) {
			if (y > this.height - 60) break;
			long n = cfg().todos.stream().filter(t -> c.equals(t.category)).count();
			action(rx, y, 178, c + " §7(" + n + ")", () -> {
				filter = c;
				page = 0;
				rebuildWidgets();
			});
			action(rx + 180, y, 20, "§c×", () -> {
				if (cfg().categories.size() <= 1) return;
				cfg().categories.remove(c);
				String fallback = cfg().categories.get(cfg().categories.size() - 1);
				for (TodoItem t : cfg().todos) {
					if (c.equals(t.category)) t.category = fallback;
				}
				if (c.equals(filter)) filter = "";
				changed();
				rebuildWidgets();
			}).active = cfg().categories.size() > 1;
			y += 22;
		}
	}

	private void move(TodoItem t, int dir) {
		List<TodoItem> all = cfg().todos;
		int i = all.indexOf(t);
		int j = i + dir;
		if (i < 0 || j < 0 || j >= all.size()) return;
		all.set(i, all.get(j));
		all.set(j, t);
		changed();
		rebuildWidgets();
	}

	private void addTask() {
		String text = addBox.getValue().trim();
		if (text.isEmpty()) return;
		String cat = filter.isEmpty() ? cfg().categories.get(cfg().categories.size() - 1) : filter;
		TodoItem t = new TodoItem(text, cat);
		cfg().todos.add(t);
		selected = t;
		changed();
		rebuildWidgets();
		if (addBox != null) setFocused(addBox);
	}

	private void addSubtask() {
		if (selected == null || subBox == null) return;
		String text = subBox.getValue().trim();
		if (text.isEmpty()) return;
		selected.subtasks.add(new SubTask(text));
		changed();
		rebuildWidgets();
		if (subBox != null) setFocused(subBox);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isConfirmation()) {
			if (addBox != null && addBox.isFocused()) {
				addTask();
				return true;
			}
			if (subBox != null && subBox.isFocused()) {
				addSubtask();
				return true;
			}
		}
		return super.keyPressed(event);
	}

	private String shorten(String s, int maxWidth) {
		if (this.font.width(s) <= maxWidth) return s;
		String out = s;
		while (out.length() > 1 && this.font.width(out + "…") > maxWidth) out = out.substring(0, out.length() - 1);
		return out + "…";
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.centeredText(this.font, "§lAufgaben", this.width / 2, 12, 0xFFFFFFFF);
		if (list().isEmpty()) {
			g.text(this.font, "§7Noch nichts geplant.", leftX() + 4, 84, 0xFFFFFFFF, false);
		}
		if (selected == null) {
			g.text(this.font, "§7Kategorien · Aufgabe anklicken zum Bearbeiten", rightX(), 36, 0xFFFFFFFF, false);
		} else {
			long d = selected.subtasks.stream().filter(s -> s.done).count();
			g.text(this.font, "§7Unteraufgaben " + d + "/" + selected.subtasks.size(), rightX(), 121, 0xFFFFFFFF, false);
		}
	}
}
