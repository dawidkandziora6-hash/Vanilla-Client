package dev.vanillaclient.gui;

import java.util.ArrayList;
import java.util.List;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.config.Config.Category;
import dev.vanillaclient.config.Config.SubTask;
import dev.vanillaclient.config.Config.TodoItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * Task planner in three columns:
 * categories (with icons) | tasks of the category | details of the selected task.
 */
public final class TasksScreen extends BaseScreen {
	private enum Show {
		ALL("All"), OPEN("Open"), DONE("Done");

		final String label;

		Show(String label) {
			this.label = label;
		}
	}

	private enum CatMode { NONE, ADD, RENAME }

	private static final int M = 8, GAP = 6, TOP = 30;
	private static final int ROW = 20, CAT_ROW = 17, SUB_ROW = 14;

	/** null = all categories */
	private static String category;
	private static TodoItem selected;
	private static Show show = Show.ALL;
	private static int taskScroll, subScroll;

	private EditBox addBox, subBox, catBox;
	private CatMode catMode = CatMode.NONE;
	private float shownProgress = -1f;
	private final java.util.Map<Object, Long> flash = new java.util.HashMap<>();

	public TasksScreen(Screen parent) {
		super(Component.literal("Task Planner"), parent);
	}

	private Config cfg() {
		return Config.get();
	}

	// ------------------------------------------------------------ layout

	private int bottom() {
		return this.height - 34;
	}

	private int lw() {
		return 112;
	}

	private int rw() {
		return 176;
	}

	private int mw() {
		return Math.max(140, this.width - 2 * M - lw() - rw() - 2 * GAP);
	}

	private int xl() {
		return M;
	}

	private int xm() {
		return xl() + lw() + GAP;
	}

	private int xr() {
		return xm() + mw() + GAP;
	}

	private int listTop() {
		return TOP + 48;
	}

	private int subTop() {
		return TOP + 116;
	}

	private int subBottom() {
		return bottom() - 48;
	}

	// ------------------------------------------------------------ data helpers

	private List<TodoItem> tasks() {
		List<TodoItem> out = new ArrayList<>();
		for (TodoItem t : cfg().todos) {
			if (category != null && !category.equals(t.category)) continue;
			if (show == Show.OPEN && t.done) continue;
			if (show == Show.DONE && !t.done) continue;
			out.add(t);
		}
		return out;
	}

	private List<TodoItem> inCategory(String cat) {
		List<TodoItem> out = new ArrayList<>();
		for (TodoItem t : cfg().todos) {
			if (cat == null || cat.equals(t.category)) out.add(t);
		}
		return out;
	}

	private static int done(List<TodoItem> l) {
		int n = 0;
		for (TodoItem t : l) if (t.done) n++;
		return n;
	}

	// ------------------------------------------------------------ widgets

	@Override
	protected void init() {
		super.init();
		if (selected != null && !cfg().todos.contains(selected)) selected = null;
		if (category != null && cfg().category(category) == null) category = null;

		// left: categories
		int lx = xl() + 4;
		int lwi = lw() - 8;
		if (catMode != CatMode.NONE) {
			catBox = new EditBox(this.font, lx + 1, bottom() - 69, lwi - 2, 18, Component.literal("Category"));
			catBox.setMaxLength(18);
			catBox.setHint(Component.literal(catMode == CatMode.ADD ? "Name … (Enter)" : "New name … (Enter)"));
			if (catMode == CatMode.RENAME && category != null) catBox.setValue(category);
			addRenderableWidget(catBox);
			setFocused(catBox);
		}
		if (category != null) {
			Category cat = cfg().category(category);
			int bw = (lwi - 4) / 3;
			iconAction(lx, bottom() - 46, bw, Icons.categoryIcon(cat.icon), "", () -> {
				cat.icon = Math.floorMod(cat.icon + (hasShift() ? -1 : 1), Icons.categoryIcons().size());
				changed();
				rebuildWidgets();
			}).setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Change icon")));
			action(lx + bw + 2, bottom() - 46, bw, "✎", () -> {
				catMode = catMode == CatMode.RENAME ? CatMode.NONE : CatMode.RENAME;
				rebuildWidgets();
			}).setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Rename")));
			Button del = action(lx + 2 * (bw + 2), bottom() - 46, bw, "§c✖", this::deleteCategory);
			del.active = cfg().taskCategories.size() > 1;
			del.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Delete category (tasks move to another one)")));
		}
		action(lx, bottom() - 24, lwi, catMode == CatMode.ADD ? "Cancel" : "+ New category", () -> {
			catMode = catMode == CatMode.ADD ? CatMode.NONE : CatMode.ADD;
			rebuildWidgets();
		});

		// middle: add + filter
		int mx = xm() + 4;
		int mwi = mw() - 8;
		addBox = new EditBox(this.font, mx + 1, TOP + 25, mwi - 70, 18, Component.literal("New task"));
		addBox.setMaxLength(64);
		addBox.setHint(Component.literal("+ Add a task … (Enter)"));
		addRenderableWidget(addBox);
		action(mx + mwi - 66, TOP + 24, 20, "+", this::addTask);
		cycle(mx + mwi - 44, TOP + 24, 44, "", List.of(Show.values()), () -> show, v -> {
			show = v;
			taskScroll = 0;
			rebuildWidgets();
		}, v -> v.label);

		// right: details
		if (selected != null) detailWidgets(selected);

		action(this.width / 2 - 100, this.height - 26, 200, "✔ Done", this::onClose);
	}

	private void detailWidgets(TodoItem t) {
		int rx = xr() + 4;
		int rwi = rw() - 8;
		EditBox title = new EditBox(this.font, rx + 1, TOP + 15, rwi - 2, 18, Component.literal("Task"));
		title.setMaxLength(64);
		title.setValue(t.text);
		title.setResponder(v -> {
			t.text = v;
			changed();
		});
		addRenderableWidget(title);

		List<String> names = new ArrayList<>();
		for (Category c : cfg().taskCategories) names.add(c.name);
		cycle(rx, TOP + 37, rwi, "", names, () -> names.contains(t.category) ? t.category : names.get(0),
				v -> t.category = v, v -> "      " + v);

		int pw = (rwi - 4) / 3;
		String[] labels = {"Normal", "§6! High", "§c!! Urgent"};
		for (int p = 0; p < 3; p++) {
			final int prio = p;
			Button b = action(rx + p * (pw + 2), TOP + 59, pw, labels[p], () -> {
				t.priority = prio;
				changed();
				rebuildWidgets();
			});
			b.active = t.priority != p;
		}

		EditBox coords = new EditBox(this.font, rx + 1, TOP + 82, rwi - 44, 18, Component.literal("Coordinates"));
		coords.setMaxLength(40);
		coords.setValue(t.coords);
		coords.setHint(Component.literal("⌖ Coordinates"));
		coords.setResponder(v -> {
			t.coords = v;
			changed();
		});
		addRenderableWidget(coords);
		action(rx + rwi - 40, TOP + 81, 40, "Here", () -> {
			var p = Minecraft.getInstance().player;
			if (p != null) {
				var pos = p.blockPosition();
				t.coords = pos.getX() + " " + pos.getY() + " " + pos.getZ();
				changed();
				rebuildWidgets();
			}
		}).setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Use your current position")));

		subBox = new EditBox(this.font, rx + 1, bottom() - 45, rwi - 24, 18, Component.literal("Subtask"));
		subBox.setMaxLength(48);
		subBox.setHint(Component.literal("+ Subtask / material …"));
		addRenderableWidget(subBox);
		action(rx + rwi - 20, bottom() - 46, 20, "+", this::addSubtask);

		List<TodoItem> all = cfg().todos;
		action(rx, bottom() - 24, 24, "▲", () -> move(t, -1)).active = all.indexOf(t) > 0;
		action(rx + 26, bottom() - 24, 24, "▼", () -> move(t, 1)).active = all.indexOf(t) < all.size() - 1;
		action(rx + 52, bottom() - 24, rwi - 52, "§cDelete task", () -> {
			all.remove(t);
			selected = null;
			changed();
			rebuildWidgets();
		});
	}

	// ------------------------------------------------------------ actions

	private void addTask() {
		String text = addBox.getValue().trim();
		if (text.isEmpty()) return;
		String cat = category != null ? category : cfg().taskCategories.get(cfg().taskCategories.size() - 1).name;
		TodoItem t = new TodoItem(text, cat);
		cfg().todos.add(t);
		selected = t;
		if (show == Show.DONE) show = Show.ALL;
		flash.put(t, System.currentTimeMillis());
		changed();
		rebuildWidgets();
		setFocused(addBox);
		taskScroll = Math.max(0, tasks().size() - visibleRows());
	}

	private void addSubtask() {
		if (selected == null || subBox == null) return;
		String text = subBox.getValue().trim();
		if (text.isEmpty()) return;
		SubTask st = new SubTask(text);
		selected.subtasks.add(st);
		flash.put(st, System.currentTimeMillis());
		changed();
		rebuildWidgets();
		setFocused(subBox);
		subScroll = Math.max(0, selected.subtasks.size() - visibleSubRows());
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

	private void confirmCategory() {
		String name = catBox == null ? "" : catBox.getValue().trim();
		if (name.isEmpty()) {
			catMode = CatMode.NONE;
			rebuildWidgets();
			return;
		}
		if (catMode == CatMode.ADD && cfg().category(name) == null) {
			cfg().taskCategories.add(new Category(name, cfg().taskCategories.size()));
			category = name;
		} else if (catMode == CatMode.RENAME && category != null && cfg().category(name) == null) {
			Category c = cfg().category(category);
			for (TodoItem t : cfg().todos) if (category.equals(t.category)) t.category = name;
			if (category.equals(cfg().todo.categoryFilter)) cfg().todo.categoryFilter = name;
			c.name = name;
			category = name;
		}
		catMode = CatMode.NONE;
		changed();
		rebuildWidgets();
	}

	private void deleteCategory() {
		if (category == null || cfg().taskCategories.size() <= 1) return;
		Category c = cfg().category(category);
		cfg().taskCategories.remove(c);
		String fallback = cfg().taskCategories.get(cfg().taskCategories.size() - 1).name;
		for (TodoItem t : cfg().todos) if (category.equals(t.category)) t.category = fallback;
		if (category.equals(cfg().todo.categoryFilter)) cfg().todo.categoryFilter = "";
		category = null;
		changed();
		rebuildWidgets();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isConfirmation()) {
			if (catBox != null && catBox.isFocused()) {
				confirmCategory();
				return true;
			}
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

	// ------------------------------------------------------------ mouse

	private int visibleRows() {
		return Math.max(1, (bottom() - 4 - listTop()) / ROW);
	}

	private int visibleSubRows() {
		return Math.max(1, (subBottom() - subTop()) / SUB_ROW);
	}

	private List<String> catRows() {
		List<String> rows = new ArrayList<>();
		rows.add(null);
		for (Category c : cfg().taskCategories) rows.add(c.name);
		return rows;
	}

	private int catRowsBottom() {
		return bottom() - (category != null ? 50 : 28) - (catMode != CatMode.NONE ? 24 : 0);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		double mx = event.x();
		double my = event.y();
		// categories
		if (mx >= xl() && mx < xl() + lw() && my >= TOP + 16 && my < catRowsBottom()) {
			int i = (int) ((my - (TOP + 16)) / CAT_ROW);
			List<String> rows = catRows();
			if (i >= 0 && i < rows.size()) {
				category = rows.get(i);
				taskScroll = 0;
				catMode = CatMode.NONE;
				rebuildWidgets();
				return true;
			}
		}
		// tasks
		if (mx >= xm() && mx < xm() + mw() && my >= listTop() && my < bottom() - 4) {
			int i = (int) ((my - listTop()) / ROW) + taskScroll;
			List<TodoItem> l = tasks();
			if (i >= 0 && i < l.size()) {
				TodoItem t = l.get(i);
				if (mx < xm() + 20) {
					t.done = !t.done;
					flash.put(t, System.currentTimeMillis());
					changed();
				} else {
					selected = selected == t ? null : t;
					subScroll = 0;
				}
				rebuildWidgets();
				return true;
			}
		}
		// subtasks
		if (selected != null && mx >= xr() && mx < xr() + rw() && my >= subTop() && my < subBottom()) {
			int i = (int) ((my - subTop()) / SUB_ROW) + subScroll;
			if (i >= 0 && i < selected.subtasks.size()) {
				SubTask st = selected.subtasks.get(i);
				if (mx >= xr() + rw() - 16) {
					selected.subtasks.remove(st);
				} else {
					st.done = !st.done;
					flash.put(st, System.currentTimeMillis());
				}
				changed();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double h, double v) {
		int d = v > 0 ? -1 : 1;
		if (mx >= xm() && mx < xm() + mw()) {
			taskScroll = Math.max(0, Math.min(Math.max(0, tasks().size() - visibleRows()), taskScroll + d));
			return true;
		}
		if (selected != null && mx >= xr() && mx < xr() + rw()) {
			subScroll = Math.max(0, Math.min(Math.max(0, selected.subtasks.size() - visibleSubRows()), subScroll + d));
			return true;
		}
		return super.mouseScrolled(mx, my, h, v);
	}

	// ------------------------------------------------------------ drawing

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractBackground(g, mouseX, mouseY, delta);
		Theme.panel(g, xl(), TOP, lw(), bottom() - TOP);
		Theme.panel(g, xm(), TOP, mw(), bottom() - TOP);
		Theme.panel(g, xr(), TOP, rw(), bottom() - TOP);
	}

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		List<TodoItem> all = cfg().todos;
		int doneAll = done(all);
		float in = intro();
		g.centeredText(this.font, "§l✎ Task Planner", this.width / 2, 8, Theme.alpha(Theme.ACCENT, in));
		String sum = all.isEmpty() ? "§7Plan farms, builds and trips" : "§7" + doneAll + " of " + all.size() + " done";
		g.centeredText(this.font, sum, this.width / 2, 19, 0xFFFFFFFF);

		drawCategories(g, mouseX, mouseY);
		drawTasks(g, mouseX, mouseY);
		drawDetails(g, mouseX, mouseY);
	}

	private void drawCategories(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = xl();
		g.text(this.font, "§7CATEGORIES", x + 5, TOP + 5, 0xFFFFFFFF, false);
		int y = TOP + 16;
		int maxY = catRowsBottom();
		for (String name : catRows()) {
			if (y + CAT_ROW > maxY) break;
			boolean sel = name == null ? category == null : name.equals(category);
			boolean hover = mouseX >= x && mouseX < x + lw() && mouseY >= y && mouseY < y + CAT_ROW;
			if (sel) {
				g.fill(x + 2, y, x + lw() - 2, y + CAT_ROW - 1, Theme.ROW_SELECTED);
				g.fill(x + 2, y, x + 4, y + CAT_ROW - 1, Theme.ACCENT);
			} else if (hover) {
				g.fill(x + 2, y, x + lw() - 2, y + CAT_ROW - 1, Theme.ROW_HOVER);
			}
			List<TodoItem> l = inCategory(name);
			Category c = name == null ? null : cfg().category(name);
			g.item(c == null ? Icons.stack(Items.BOOK) : Icons.categoryIcon(c.icon), x + 6, y);
			String label = this.font.plainSubstrByWidth(name == null ? "All tasks" : name, lw() - 52);
			g.text(this.font, label, x + 25, y + 4, sel ? 0xFFFFFFFF : 0xFFD0D0D0, false);
			String cnt = done(l) + "/" + l.size();
			int cw = this.font.width(cnt);
			boolean complete = !l.isEmpty() && done(l) == l.size();
			g.text(this.font, cnt, x + lw() - 6 - cw, y + 4, complete ? Theme.GOOD : Theme.DIM, false);
			y += CAT_ROW;
		}
	}

	private void drawTasks(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = xm();
		int w = mw();
		List<TodoItem> catTasks = inCategory(category);
		Category c = category == null ? null : cfg().category(category);
		g.item(c == null ? Icons.stack(Items.BOOK) : Icons.categoryIcon(c.icon), x + 4, TOP + 2);
		g.text(this.font, "§l" + (category == null ? "All tasks" : category), x + 23, TOP + 4, 0xFFFFFFFF, false);
		int d = done(catTasks);
		String cnt = d + "/" + catTasks.size() + " done";
		g.text(this.font, "§7" + cnt, x + w - 5 - this.font.width(cnt), TOP + 4, 0xFFFFFFFF, false);
		float target = catTasks.isEmpty() ? 0f : d / (float) catTasks.size();
		if (shownProgress < 0) shownProgress = target;
		shownProgress += (target - shownProgress) * 0.15f;
		Theme.progressBar(g, x + 23, TOP + 15, w - 28, 3, shownProgress, target >= 1f && !catTasks.isEmpty() ? Theme.GOOD : Theme.ACCENT);

		List<TodoItem> l = tasks();
		int rows = visibleRows();
		taskScroll = Math.max(0, Math.min(taskScroll, Math.max(0, l.size() - rows)));
		if (l.isEmpty()) {
			g.centeredText(this.font, "§7" + (show == Show.DONE ? "Nothing done yet" : "No tasks here yet"), x + w / 2, listTop() + 14, 0xFFFFFFFF);
			g.centeredText(this.font, "§8Type above and press Enter", x + w / 2, listTop() + 26, 0xFFFFFFFF);
			return;
		}
		long now = System.currentTimeMillis();
		for (int i = 0; i < rows && i + taskScroll < l.size(); i++) {
			TodoItem t = l.get(i + taskScroll);
			int y = listTop() + i * ROW;
			boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW;
			boolean sel = t == selected;
			int bg = sel ? Theme.ROW_SELECTED : hover ? Theme.ROW_HOVER : 0x10FFFFFF;
			Long f = flash.get(t);
			if (f != null && now - f < 600) bg = Theme.lerpColor(0x8055FF55, bg, (now - f) / 600f);
			g.fill(x + 2, y, x + w - 2, y + ROW - 2, bg);
			g.fill(x + 2, y, x + 4, y + ROW - 2, Theme.priorityColor(t.priority));
			if (sel) g.outline(x + 2, y, w - 4, ROW - 2, 0xA0FFFFFF);

			Theme.checkbox(g, x + 8, y + 5, t.done, 0xFFFFFFFF);
			Category tc = cfg().category(t.category);
			int tx = x + 21;
			if (category == null && tc != null) {
				Icons.small(g, Icons.categoryIcon(tc.icon), tx, y + 5);
				tx += 11;
			}
			int right = x + w - 6;
			if (!t.subtasks.isEmpty()) {
				int sd = 0;
				for (SubTask st : t.subtasks) if (st.done) sd++;
				String s = sd + "/" + t.subtasks.size();
				int sw = this.font.width(s);
				g.text(this.font, s, right - sw, y + 3, sd == t.subtasks.size() ? Theme.GOOD : Theme.MUTED, false);
				Theme.progressBar(g, right - Math.max(sw, 18), y + 13, Math.max(sw, 18), 2, sd / (float) t.subtasks.size(), Theme.GOOD);
				right -= Math.max(sw, 18) + 4;
			}
			if (!t.coords.isBlank()) {
				g.text(this.font, "⌖", right - 6, y + 5, Theme.MUTED, false);
				right -= 10;
			}
			String text = this.font.plainSubstrByWidth(t.text, right - tx - 2);
			if (!text.equals(t.text)) text = this.font.plainSubstrByWidth(t.text, right - tx - 8) + "…";
			g.text(this.font, (t.done ? "§7§m" : "") + text, tx, y + 5, 0xFFFFFFFF, false);
		}
		// scrollbar
		if (l.size() > rows) {
			int trackH = rows * ROW - 2;
			int barH = Math.max(8, trackH * rows / l.size());
			int barY = listTop() + (trackH - barH) * taskScroll / Math.max(1, l.size() - rows);
			g.fill(x + w - 2, listTop(), x + w - 1, listTop() + trackH, 0x30FFFFFF);
			g.fill(x + w - 2, barY, x + w - 1, barY + barH, 0xC0FFFFFF);
		}
	}

	private void drawDetails(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = xr();
		int w = rw();
		if (selected == null) {
			g.text(this.font, "§7DETAILS", x + 5, TOP + 5, 0xFFFFFFFF, false);
			int y = TOP + 22;
			g.text(this.font, "§fClick a task to edit it.", x + 6, y, 0xFFFFFFFF, false);
			y += 16;
			g.text(this.font, "§7OVERVIEW", x + 5, y, 0xFFFFFFFF, false);
			y += 12;
			for (Category c : cfg().taskCategories) {
				if (y > bottom() - 16) break;
				List<TodoItem> l = inCategory(c.name);
				Icons.small(g, Icons.categoryIcon(c.icon), x + 6, y);
				g.text(this.font, this.font.plainSubstrByWidth(c.name, 70), x + 17, y, 0xFFD0D0D0, false);
				float f = l.isEmpty() ? 0f : done(l) / (float) l.size();
				Theme.progressBar(g, x + 92, y + 3, w - 100, 3, f, f >= 1f ? Theme.GOOD : Theme.ACCENT);
				y += 12;
			}
			return;
		}
		g.text(this.font, "§7TASK", x + 5, TOP + 5, 0xFFFFFFFF, false);
		Category tc = cfg().category(selected.category);
		if (tc != null) g.item(Icons.categoryIcon(tc.icon), x + 8, TOP + 39);

		int sd = 0;
		for (SubTask st : selected.subtasks) if (st.done) sd++;
		g.text(this.font, "§7SUBTASKS §f" + sd + "/" + selected.subtasks.size(), x + 5, TOP + 104, 0xFFFFFFFF, false);
		if (!selected.subtasks.isEmpty()) {
			Theme.progressBar(g, x + 90, TOP + 107, w - 96, 3, sd / (float) selected.subtasks.size(), Theme.GOOD);
		}
		int rows = visibleSubRows();
		subScroll = Math.max(0, Math.min(subScroll, Math.max(0, selected.subtasks.size() - rows)));
		if (selected.subtasks.isEmpty()) {
			g.text(this.font, "§8e.g. \"64 iron blocks\"", x + 8, subTop() + 3, 0xFFFFFFFF, false);
		}
		long now = System.currentTimeMillis();
		for (int i = 0; i < rows && i + subScroll < selected.subtasks.size(); i++) {
			SubTask st = selected.subtasks.get(i + subScroll);
			int y = subTop() + i * SUB_ROW;
			boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + SUB_ROW;
			int bg = hover ? Theme.ROW_HOVER : 0;
			Long f = flash.get(st);
			if (f != null && now - f < 600) bg = Theme.lerpColor(0x8055FF55, bg, (now - f) / 600f);
			if (bg != 0) g.fill(x + 2, y, x + w - 2, y + SUB_ROW - 1, bg);
			Theme.checkbox(g, x + 6, y + 3, st.done, 0xFFFFFFFF);
			String text = this.font.plainSubstrByWidth(st.text, w - 40);
			g.text(this.font, (st.done ? "§7§m" : "§f") + text, x + 18, y + 3, 0xFFFFFFFF, false);
			if (hover) {
				boolean overX = mouseX >= x + w - 16;
				g.text(this.font, "✖", x + w - 12, y + 3, overX ? Theme.BAD : Theme.DIM, false);
			}
		}
	}
}
