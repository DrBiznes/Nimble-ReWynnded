package com.jamino.nimblerewynnded.mixin;

import com.jamino.nimblerewynnded.perspective.FreeThirdPersonPerspective;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    /** In free third person the scroll wheel zooms the camera instead of changing the hotbar slot. */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void nimble$zoomCamera(long handle, double xOffset, double yOffset, CallbackInfo ci) {
        if (yOffset != 0 && FreeThirdPersonPerspective.onMouseScroll(yOffset)) {
            ci.cancel();
        }
    }
}
