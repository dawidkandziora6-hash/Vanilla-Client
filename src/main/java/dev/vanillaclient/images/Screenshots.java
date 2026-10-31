package dev.vanillaclient.images;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.gui.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Nicer "screenshot saved" message, a little popup with the picture, and the list of screenshots. */
public final class Screenshots {
	private static Path last;
	private static long lastAt;

	private Screenshots() {}

	/** Called instead of the vanilla chat line. */
	public static void saved(String fileName) {
		Path file = Thumbs.screenshotsDir().resolve(fileName);
		last = file;
		lastAt = System.currentTimeMillis();
		Minecraft mc = Minecraft.getInstance();
		if (!Config.get().screenshotMessage) return;
		MutableComponent msg = Component.literal("✦ ").withColor(0xFFD34D)
				.append(Component.literal("Screenshot saved  ").withColor(0xFFFFFF));
		msg.append(button("[View]", 0x7FD8FF, new ClickEvent.RunCommand("/vcgallery " + fileName), "Open it in the in-game gallery"));
		msg.append(Component.literal(" "));
		msg.append(button("[Open]", 0x8AF08A, new ClickEvent.OpenFile(file.toAbsolutePath().toString()), "Open the file"));
		msg.append(Component.literal(" "));
		msg.append(button("[Folder]", 0xC0C0C0, new ClickEvent.OpenFile(file.getParent().toAbsolutePath().toString()), "Open the screenshots folder"));
		mc.gui.getChat().addClientSystemMessage(msg);
	}

	private static Component button(String text, int color, ClickEvent click, String hover) {
		return Component.literal(text).withStyle(Style.EMPTY.withColor(color).withUnderlined(false)
				.withClickEvent(click).withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
	}

	/** Newest first. */
	public static List<Path> all() {
		List<Path> out = new ArrayList<>();
		try (Stream<Path> s = Files.list(Thumbs.screenshotsDir())) {
			s.filter(p -> {
				String n = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
				return n.endsWith(".png") || n.endsWith(".jpg");
			}).forEach(out::add);
		} catch (Exception ignored) {
			// no screenshots yet
		}
		out.sort(Comparator.comparingLong((Path p) -> {
			try {
				return Files.getLastModifiedTime(p).toMillis();
			} catch (Exception e) {
				return 0L;
			}
		}).reversed());
		return out;
	}

	/** HUD popup in the top-right corner for a few seconds. */
	public static void renderPopup(GuiGraphicsExtractor g, DeltaTracker delta) {
		if (last == null || !Config.get().screenshotPopup) return;
		long age = System.currentTimeMillis() - lastAt;
		if (age > 4500) return;
		Thumbs.Thumb t = Thumbs.get(last, 160);
		if (t == null) return;
		float in = Theme.easeOut(age / 250f);
		float out = age > 4000 ? 1f - (age - 4000) / 500f : 1f;
		int w = 120, h = Math.round(w * t.h() / (float) t.w());
		int x = g.guiWidth() - w - 8 + Math.round((1f - in) * (w + 12));
		int y = 8;
		Font font = Minecraft.getInstance().font;
		g.fill(x - 3, y - 3, x + w + 3, y + h + 15, Theme.alpha(0xD0101010, out));
		g.outline(x - 3, y - 3, w + 6, h + 18, Theme.alpha(Theme.ACCENT, out));
		g.blit(RenderPipelines.GUI_TEXTURED, t.id(), x, y, 0f, 0f, w, h, t.w(), t.h(), t.w(), t.h(), Theme.alpha(0xFFFFFFFF, out));
		g.text(font, "✦ Screenshot saved", x, y + h + 4, Theme.alpha(0xFFFFD34D, out), false);
		// flash
		if (age < 200) g.fill(0, 0, g.guiWidth(), g.guiHeight(), Theme.alpha(0xFFFFFFFF, 0.35f * (1f - age / 200f)));
	}
}
