package dev.logbuch.gui;

import java.nio.file.Path;
import java.util.List;

import dev.logbuch.config.LogbuchConfig;
import dev.logbuch.cosmetics.Cosmetics;
import dev.logbuch.cosmetics.ShieldPack;
import dev.logbuch.crosshair.CrosshairRenderer;
import dev.logbuch.images.ImageManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Das Hauptmenü der Mod: links die Bereiche, rechts die Einstellungen. */
public final class LogbuchScreen extends LbScreen {
	private enum Tab {
		MODULES("Module"), DAYS("Tage"), TODO("Aufgaben"), ARMOR("Rüstung"), CROSSHAIR("Fadenkreuz"),
		ZOOM("Zoom"), IMAGES("Bilder"), COSMETICS("Kosmetik");

		final String label;

		Tab(String label) {
			this.label = label;
		}
	}

	private static Tab tab = Tab.MODULES;
	private static int imagePage;

	private static final int SIDEBAR = 96;
	private static final int CONTENT = 220;
	private static final int TOP = 40;
	private static final int ROW = 24;

	private String status = "";
	private long statusUntil;

	public LogbuchScreen(Screen parent) {
		super(Component.literal("Logbuch"), parent);
	}

	private int left() {
		return this.width / 2 - (SIDEBAR + 10 + CONTENT) / 2;
	}

	private int cx() {
		return left() + SIDEBAR + 10;
	}

	private int rowY(int i) {
		return TOP + i * ROW;
	}

	/** Linke/rechte Hälfte einer Zeile. */
	private int halfW() {
		return (CONTENT - 4) / 2;
	}

	private int rightHalfX() {
		return cx() + halfW() + 4;
	}

	private void say(String msg) {
		status = msg;
		statusUntil = System.currentTimeMillis() + 4000;
	}

	@Override
	protected void init() {
		super.init();
		int y = TOP;
		for (Tab t : Tab.values()) {
			Button b = action(left(), y, SIDEBAR, t.label, () -> {
				tab = t;
				rebuildWidgets();
			});
			b.active = t != tab;
			y += 22;
		}
		switch (tab) {
			case MODULES -> modulesTab();
			case DAYS -> daysTab();
			case TODO -> todoTab();
			case ARMOR -> armorTab();
			case CROSSHAIR -> crosshairTab();
			case ZOOM -> zoomTab();
			case IMAGES -> imagesTab();
			case COSMETICS -> cosmeticsTab();
		}
		action(this.width / 2 - 154, this.height - 28, 150, "HUD bearbeiten …", () -> this.minecraft.setScreenAndShow(new HudEditorScreen(this)));
		action(this.width / 2 + 4, this.height - 28, 150, "Fertig", this::onClose);
	}

	private LogbuchConfig cfg() {
		return LogbuchConfig.get();
	}

	// ------------------------------------------------------------ Bereiche

	private void modulesTab() {
		LogbuchConfig c = cfg();
		int x = cx();
		int w = halfW();
		toggle(x, rowY(0), w, "Tage", () -> c.days.enabled, v -> c.days.enabled = v);
		toggle(rightHalfX(), rowY(0), w, "Aufgaben", () -> c.todo.enabled, v -> c.todo.enabled = v);
		toggle(x, rowY(1), w, "Rüstung", () -> c.armor.enabled, v -> c.armor.enabled = v);
		toggle(rightHalfX(), rowY(1), w, "Fadenkreuz", () -> c.crosshair.enabled, v -> c.crosshair.enabled = v);
		toggle(x, rowY(2), w, "Zoom", () -> c.zoom.enabled, v -> c.zoom.enabled = v);
		long shown = c.images.stream().filter(i -> i.enabled).count();
		action(rightHalfX(), rowY(2), w, "Bilder: " + shown + "/" + c.images.size(), () -> {
			tab = Tab.IMAGES;
			rebuildWidgets();
		});
		toggle(x, rowY(3), CONTENT, "Knopf im Pausemenü", () -> c.pauseMenuButton, v -> c.pauseMenuButton = v);
		action(x, rowY(4), CONTENT, "Aufgaben bearbeiten …", () -> this.minecraft.setScreenAndShow(new TodoScreen(this)));
	}

	private void hudCommon(LogbuchConfig.Hud s, int firstRow, boolean colors) {
		int x = cx();
		if (colors) {
			color(x, rowY(firstRow), halfW(), "Textfarbe", () -> s.color, v -> s.color = v);
			toggle(rightHalfX(), rowY(firstRow), halfW(), "Schatten", () -> s.shadow, v -> s.shadow = v);
			firstRow++;
		}
		toggle(x, rowY(firstRow), halfW(), "Hintergrund", () -> s.background, v -> s.background = v);
		color(rightHalfX(), rowY(firstRow), halfW(), "Hintergr.-Farbe", () -> s.backgroundColor, v -> s.backgroundColor = v);
		slider(x, rowY(firstRow + 1), CONTENT, 0.5, 3.0, 0.05, () -> s.scale, v -> s.scale = (float) v,
				v -> "Größe: " + Math.round(v * 100) + "%");
	}

	private void daysTab() {
		LogbuchConfig.Days s = cfg().days;
		int x = cx();
		toggle(x, rowY(0), halfW(), "Anzeigen", () -> s.enabled, v -> s.enabled = v);
		toggle(rightHalfX(), rowY(0), halfW(), "Uhrzeit", () -> s.showTime, v -> s.showTime = v);
		toggle(x, rowY(1), halfW(), "Ab Tag 1", () -> s.countFromOne, v -> s.countFromOne = v);
		EditBox label = new EditBox(this.font, rightHalfX() + 1, rowY(1) + 1, halfW() - 2, 18, Component.literal("Bezeichnung"));
		label.setMaxLength(16);
		label.setValue(s.label);
		label.setHint(Component.literal("Bezeichnung"));
		label.setResponder(t -> {
			s.label = t;
			changed();
		});
		addRenderableWidget(label);
		hudCommon(s, 2, true);
	}

	private void todoTab() {
		LogbuchConfig.Todo s = cfg().todo;
		int x = cx();
		action(x, rowY(0), CONTENT, "Aufgaben bearbeiten …", () -> this.minecraft.setScreenAndShow(new TodoScreen(this)));
		toggle(x, rowY(1), halfW(), "Anzeigen", () -> s.enabled, v -> s.enabled = v);
		toggle(rightHalfX(), rowY(1), halfW(), "Erledigte weg", () -> s.hideDone, v -> s.hideDone = v);
		toggle(x, rowY(2), halfW(), "Unteraufg.", () -> s.showSubtasks, v -> s.showSubtasks = v);
		toggle(rightHalfX(), rowY(2), halfW(), "Koordinaten", () -> s.showCoords, v -> s.showCoords = v);
		List<String> filters = new java.util.ArrayList<>();
		filters.add("");
		filters.addAll(cfg().categories);
		cycle(x, rowY(3), halfW(), "Filter", filters, () -> filters.contains(s.categoryFilter) ? s.categoryFilter : "",
				v -> s.categoryFilter = v, v -> v.isEmpty() ? "Alle" : v);
		slider(rightHalfX(), rowY(3), halfW(), 1, 20, 1, () -> s.maxLines, v -> s.maxLines = (int) v, v -> "Zeilen: " + (int) v);
		hudCommon(s, 4, true);
	}

	private void armorTab() {
		LogbuchConfig.Armor s = cfg().armor;
		int x = cx();
		toggle(x, rowY(0), halfW(), "Anzeigen", () -> s.enabled, v -> s.enabled = v);
		toggle(rightHalfX(), rowY(0), halfW(), "An Hotbar", () -> s.attachToHotbar, v -> s.attachToHotbar = v);
		cycle(x, rowY(1), halfW(), "Seite", List.of(true, false), () -> s.rightSide, v -> s.rightSide = v, v -> v ? "Rechts" : "Links");
		cycle(rightHalfX(), rowY(1), halfW(), "Haltbarkeit", List.of(0, 1, 2), () -> s.durabilityText, v -> s.durabilityText = v,
				v -> switch (v) {
					case 1 -> "%";
					case 2 -> "Zahl";
					default -> "Balken";
				});
		toggle(x, rowY(2), halfW(), "Hand-Item", () -> s.showHeldItem, v -> s.showHeldItem = v);
		toggle(rightHalfX(), rowY(2), halfW(), "Leer verstecken", () -> s.hideWhenEmpty, v -> s.hideWhenEmpty = v);
		slider(x, rowY(3), CONTENT, 0.5, 3.0, 0.05, () -> s.scale, v -> s.scale = (float) v,
				v -> "Größe (nur frei): " + Math.round(v * 100) + "%");
	}

	private void crosshairTab() {
		LogbuchConfig.Crosshair s = cfg().crosshair;
		int x = cx();
		toggle(x, rowY(0), halfW(), "Eigenes", () -> s.enabled, v -> s.enabled = v);
		action(rightHalfX(), rowY(0), halfW(), "Malen …", () -> this.minecraft.setScreenAndShow(new CrosshairEditorScreen(this)));
		color(x, rowY(1), halfW(), "Farbe", () -> s.color, v -> s.color = v);
		toggle(rightHalfX(), rowY(1), halfW(), "Angriffsanz.", () -> s.attackIndicator, v -> s.attackIndicator = v);
		toggle(x, rowY(2), halfW(), "Umrandung", () -> s.outline, v -> s.outline = v);
		color(rightHalfX(), rowY(2), halfW(), "Rand-Farbe", () -> s.outlineColor, v -> s.outlineColor = v);
		slider(x, rowY(3), CONTENT, 0.5, 4.0, 0.25, () -> s.scale, v -> s.scale = (float) v, v -> "Größe: " + v + "×");
	}

	private void zoomTab() {
		LogbuchConfig.Zoom z = cfg().zoom;
		int x = cx();
		toggle(x, rowY(0), halfW(), "Zoom", () -> z.enabled, v -> z.enabled = v);
		toggle(rightHalfX(), rowY(0), halfW(), "Weich", () -> z.smoothTransition, v -> z.smoothTransition = v);
		slider(x, rowY(1), CONTENT, 1.5, 30, 0.5, () -> z.level, v -> z.level = (float) v, v -> "Zoomstufe: " + v + "×");
		toggle(x, rowY(2), halfW(), "Mausrad", () -> z.scrollToAdjust, v -> z.scrollToAdjust = v);
		toggle(rightHalfX(), rowY(2), halfW(), "Kinokamera", () -> z.cinematicCamera, v -> z.cinematicCamera = v);
		toggle(x, rowY(3), CONTENT, "Mausrad-Stufe beim Loslassen zurücksetzen", () -> z.resetLevelOnRelease, v -> z.resetLevelOnRelease = v);
	}

	private void imagesTab() {
		LogbuchConfig c = cfg();
		int x = cx();
		action(x, rowY(0), halfW(), "Ordner öffnen", () -> Platform.open(LogbuchConfig.imagesDir()));
		action(rightHalfX(), rowY(0), halfW(), "Neu laden", () -> {
			ImageManager.reload();
			say(c.images.size() + " Bild(er) gefunden");
			rebuildWidgets();
		});
		int perPage = 4;
		int pages = Math.max(1, (c.images.size() + perPage - 1) / perPage);
		imagePage = Math.max(0, Math.min(imagePage, pages - 1));
		for (int i = 0; i < perPage; i++) {
			int idx = imagePage * perPage + i;
			if (idx >= c.images.size()) break;
			LogbuchConfig.Image img = c.images.get(idx);
			String name = img.file.length() > 14 ? img.file.substring(0, 13) + "…" : img.file;
			toggle(x, rowY(1 + i), 110, name, () -> img.enabled, v -> img.enabled = v);
			slider(x + 114, rowY(1 + i), CONTENT - 114, 0.05, 1.0, 0.05, () -> ((img.tint >>> 24) & 0xFF) / 255.0,
					v -> img.tint = (img.tint & 0x00FFFFFF) | ((int) Math.round(v * 255) << 24),
					v -> "Deckkraft " + Math.round(v * 100) + "%");
		}
		if (pages > 1) {
			action(x, rowY(5), 30, "◀", () -> {
				imagePage--;
				rebuildWidgets();
			}).active = imagePage > 0;
			action(x + CONTENT - 30, rowY(5), 30, "▶", () -> {
				imagePage++;
				rebuildWidgets();
			}).active = imagePage < pages - 1;
		}
	}

	private void cosmeticsTab() {
		LogbuchConfig.Cosmetics s = cfg().cosmetics;
		int x = cx();
		cycle(x, rowY(0), CONTENT, "Umhang", Cosmetics.CAPES, () -> s.cape, v -> s.cape = v, Cosmetics::label);
		cycle(x, rowY(1), CONTENT, "Flügel (mit Elytra)", Cosmetics.WINGS, () -> s.wings, v -> s.wings = v, Cosmetics::label);
		cycle(x, rowY(2), halfW() + 30, "Schild", Cosmetics.SHIELDS, () -> s.shield, v -> s.shield = v, Cosmetics::label);
		action(x + halfW() + 34, rowY(2), CONTENT - halfW() - 34, "Übernehmen", () -> {
			String err = ShieldPack.write(s.shield);
			if (err != null) {
				say("§c" + err);
			} else if (ShieldPack.isActive()) {
				say("Gespeichert – F3+T drücken zum Neuladen");
			} else {
				say("Paket \"" + ShieldPack.FOLDER + "\" unter Ressourcenpakete aktivieren");
			}
		});
		action(x, rowY(3), halfW(), "Ordner öffnen", () -> Platform.open(LogbuchConfig.cosmeticsDir()));
		action(rightHalfX(), rowY(3), halfW(), "Eigene neu laden", () -> {
			Cosmetics.reloadCustom();
			say("Eigene Bilder neu geladen");
		});
	}

	// ------------------------------------------------------------ Zeichnen

	@Override
	protected void extractForeground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		g.centeredText(this.font, "§lLogbuch", this.width / 2, 12, 0xFFFFFFFF);
		g.centeredText(this.font, "§7" + tab.label + " · alles bleibt vanilla", this.width / 2, 24, 0xFFFFFFFF);
		int x = cx();
		int hintY = rowY(6);
		switch (tab) {
			case MODULES -> hint(g, x, hintY, "Tipp: Im HUD-Editor alles hinziehen,", "Größe mit dem Mausrad ändern.");
			case ARMOR -> hint(g, x, rowY(4), "\"An Hotbar\" aus = frei verschiebbar", "im HUD-Editor.");
			case CROSSHAIR -> {
				int px = x + CONTENT / 2;
				int py = rowY(4) + 30;
				g.fill(px - 30, py - 26, px + 30, py + 26, 0xFF6A9BD8);
				g.fill(px - 30, py + 6, px + 30, py + 26, 0xFF5E8C3A);
				CrosshairRenderer.draw(g, cfg().crosshair, px, py, cfg().crosshair.scale);
			}
			case ZOOM -> hint(g, x, rowY(5), "Taste: §eC§r (änderbar unter Steuerung).", "Beim Zoomen: Mausrad = näher/weiter.");
			case IMAGES -> hint(g, x, rowY(cfg().images.size() > 4 ? 6 : 1 + Math.max(1, cfg().images.size())),
					"PNG/JPG einfach ins Fenster ziehen.", "Platzieren im HUD-Editor.");
			case COSMETICS -> hint(g, x, rowY(4), "Nur für dich sichtbar. Flügel nur mit Elytra.",
					"Eigene: umhang.png / fluegel.png / schild.png");
			default -> {}
		}
		if (System.currentTimeMillis() < statusUntil) {
			g.centeredText(this.font, status, this.width / 2, this.height - 42, 0xFFFFFF55);
		}
	}

	private void hint(GuiGraphicsExtractor g, int x, int y, String a, String b) {
		g.text(this.font, "§7" + a, x, y + 4, 0xFFFFFFFF, false);
		if (!b.isEmpty()) g.text(this.font, "§7" + b, x, y + 15, 0xFFFFFFFF, false);
	}

	/** Bilder per Drag & Drop aus dem Explorer importieren. */
	public void onFilesDrop(List<Path> paths) {
		int n = ImageManager.importFiles(paths);
		if (n > 0) {
			tab = Tab.IMAGES;
			say(n + " Bild(er) importiert – im HUD-Editor platzieren");
			rebuildWidgets();
		} else {
			say("§cNur PNG- oder JPG-Dateien");
		}
	}
}
