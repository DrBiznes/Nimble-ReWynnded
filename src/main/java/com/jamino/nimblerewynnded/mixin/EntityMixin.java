package com.jamino.nimblerewynnded.mixin;

import com.jamino.nimblerewynnded.NimbleRewynnded;
import net.minecraft.entity.Entity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At("RETURN"))
    private void onStartRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ClientPlayerEntity && ((Entity) (Object) this).getWorld().isClient()) {
            NimbleRewynnded.getInstance().mountEvent((Entity) (Object) this, true);
        }
    }

    @Inject(method = "stopRiding", at = @At("HEAD"))
    private void onStopRiding(CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity && ((Entity) (Object) this).getWorld().isClient()) {
            NimbleRewynnded.getInstance().mountEvent((Entity) (Object) this, false);
        }
    }
}
