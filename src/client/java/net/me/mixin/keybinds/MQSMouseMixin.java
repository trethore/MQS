package net.me.mixin.keybinds;

import net.me.Main;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MQSMouseMixin {
    @Inject(at = @At("HEAD"), method = "onMouseButton")
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        Main.getKeybindManager().onMouseClick(button, action);
    }
}
