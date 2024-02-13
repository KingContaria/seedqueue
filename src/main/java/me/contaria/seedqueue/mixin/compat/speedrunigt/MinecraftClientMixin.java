package me.contaria.seedqueue.mixin.compat.speedrunigt;

import com.bawnorton.mixinsquared.TargetHandler;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftClient.class, priority = 1500)
public abstract class MinecraftClientMixin {

    @Dynamic
    @TargetHandler(
            mixin = "com.redlimerl.speedrunigt.mixins.MinecraftClientMixin",
            name = "onCreate"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    private void doNotTriggerSpeedRunIGT_onCreate_inQueue(CallbackInfo ci) {
        if (SeedQueue.inQueue()) {
            ci.cancel();
        }
    }
}
