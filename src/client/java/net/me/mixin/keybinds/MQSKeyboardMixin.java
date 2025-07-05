package net.me.mixin.keybinds;

import net.me.Main;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MQSKeyboardMixin {
    @Inject(at = @At("HEAD"), method = "onKey(JIIII)V")
    private void onOnKey(long windowHandle, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        Main.getInstance().getKeybindManager().onKey(key, action);
    }
}
