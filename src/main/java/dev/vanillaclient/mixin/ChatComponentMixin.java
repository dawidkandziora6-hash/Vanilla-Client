package dev.vanillaclient.mixin;

import dev.vanillaclient.config.Config;
import dev.vanillaclient.images.Screenshots;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces "Saved screenshot as …" with a nicer message + popup. */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
	@Inject(method = "addClientSystemMessage", at = @At("HEAD"), cancellable = true)
	private void vc$screenshot(Component message, CallbackInfo ci) {
		if (!Config.get().screenshotMessage && !Config.get().screenshotPopup) return;
		if (message.getContents() instanceof TranslatableContents tc && tc.getKey().equals("screenshot.success") && tc.getArgs().length > 0) {
			Object a = tc.getArgs()[0];
			String file = a instanceof Component c ? c.getString() : String.valueOf(a);
			ci.cancel();
			Screenshots.saved(file, (ChatComponent) (Object) this);
		}
	}
}
