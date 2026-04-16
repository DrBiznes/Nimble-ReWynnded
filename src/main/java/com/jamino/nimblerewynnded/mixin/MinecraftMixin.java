package com.jamino.nimblerewynnded.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    /**
     * Drains all pending clicks from vanilla's keyTogglePerspective before the vanilla
     * handleKeybinds loop can process them. This prevents vanilla from cycling through
     * THIRD_PERSON_FRONT (which our mod intentionally skips), while our custom
     * togglePerspectiveKey handles all perspective switching logic instead.
     */
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void suppressVanillaPerspectiveCycle(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        while (mc.options.keyTogglePerspective.consumeClick()) {
            // Intentionally drain vanilla perspective cycling clicks
        }
    }
}
