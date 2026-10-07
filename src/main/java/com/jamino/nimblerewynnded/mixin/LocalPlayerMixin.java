package com.jamino.nimblerewynnded.mixin;

import com.jamino.nimblerewynnded.perspective.FreeThirdPersonPerspective;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Shadow
    public ClientInput input;

    /** Makes movement keys relative to the camera in free third person. */
    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER))
    private void nimble$cameraRelativeMovement(CallbackInfo ci) {
        FreeThirdPersonPerspective.onInputTick((LocalPlayer) (Object) this, input);
    }
}
