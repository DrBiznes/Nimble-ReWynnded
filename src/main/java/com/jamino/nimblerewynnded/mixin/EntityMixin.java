package com.jamino.nimblerewynnded.mixin;

import com.jamino.nimblerewynnded.perspective.FreeThirdPersonPerspective;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    /** In free third person the mouse turns the camera, not the player. */
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void nimble$turnCamera(double dx, double dy, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer && FreeThirdPersonPerspective.onMouseTurn(dx, dy)) {
            ci.cancel();
        }
    }
}
