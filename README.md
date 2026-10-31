# Logbuch – Fabric-Mod für Minecraft 26.3

Rein **clientseitiger** Helfer für deine Hardcore-/UHC-Welt. Die Mod schickt nichts an den Server
und ändert keine Spielmechanik: Sie zeigt nur etwas an. Kosmetik siehst nur du.

## Funktionen

| Modul | Was es macht |
|---|---|
| **Tage** | „Tag 37 · 14:20“ als Anzeige auf dem Bildschirm (liest die Überwelt-Uhr nur aus) |
| **Aufgaben** | To-do-Liste auf dem Bildschirm mit Kategorien (Farm, Bau, …), Priorität, Koordinaten („Hier“-Knopf) und Unteraufgaben/Materiallisten |
| **Rüstung** | Rüstung + Haltbarkeit im echten Hotbar-Look, direkt neben der Hotbar (oder frei verschiebbar) |
| **Fadenkreuz** | Eigenes Pixel-Fadenkreuz mit Malprogramm (malen, radieren, spiegeln, Vorlagen), Farbe, Größe, Umrandung |
| **Zoom** | Taste **C** halten. Zoomstufe, weicher Übergang, Mausrad zum Nachjustieren, Kinokamera |
| **Bilder** | PNG/JPG vom PC als Overlay (z. B. Baupläne). Einfach ins Menü ziehen, im HUD-Editor platzieren, Deckkraft einstellen |
| **Kosmetik** | Umhänge, Flügel (nur sichtbar, wenn du eine Elytra trägst), Schild-Designs (nur auf dem Schild). Alle auch mit eigenen Bildern |

**Menü:** Im Pausemenü links neben „Optionen…“ ist ein neuer Knopf mit Buch-Symbol. Dort kannst du alle Module an- und
ausschalten, Farben und Größen einstellen, und über **„HUD bearbeiten“** alles mit der Maus verschieben (Mausrad = Größe,
Rechtsklick = Größe zurücksetzen, rastet an Rändern und an der Mitte ein).

Tastenbelegungen unter *Optionen → Steuerung → Logbuch*: Zoom (C), Menü öffnen, Aufgaben-Anzeige an/aus.

## Bauen

Benötigt: **JDK 25**.

```bash
./gradlew build        # Windows: gradlew.bat build
```

Die fertige Mod liegt danach in `build/libs/logbuch-1.0.0+26.3.jar`.
Zusammen mit **Fabric Loader ≥ 0.19.5** und **Fabric API (0.161.0+26.3 oder neuer)** in den `mods`-Ordner legen.

Ohne eigenes Java: Projekt in ein GitHub-Repository hochladen. Der mitgelieferte Workflow
(`.github/workflows/build.yml`) baut die Jar automatisch und legt sie als Download unter *Actions* ab.

## Ordner

Alles liegt in `.minecraft/config/logbuch/`:

- `config.json` – alle Einstellungen und Aufgaben
- `bilder/` – deine Overlay-Bilder
- `kosmetik/` – eigene Kosmetik: `umhang.png` / `fluegel.png` (64×32, Minecraft-Umhang-Layout), `schild.png` (64×64, Schild-Layout)

**Schild-Design:** Schilde werden über ein normales Ressourcenpaket umgefärbt (der Vanilla-Weg). Nach „Übernehmen“
einmal unter *Optionen → Ressourcenpakete* **„Logbuch Kosmetik“** aktivieren. Spätere Änderungen lädst du mit **F3+T** neu.

## Hinweise

- **C** ist in Vanilla auch die Taste „Schnellleiste speichern“ (nur im Kreativmodus). Falls das stört, unter *Steuerung* umbelegen.
- Zoom und eigenes Fadenkreuz sind rein optisch. Auf fremden Servern oder bei UHC-Events trotzdem kurz die Regeln prüfen.
