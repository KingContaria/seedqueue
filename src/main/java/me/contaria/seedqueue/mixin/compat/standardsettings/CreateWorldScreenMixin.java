package me.contaria.seedqueue.mixin.compat.standardsettings;

import com.bawnorton.mixinsquared.TargetHandler;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreateWorldScreen.class, priority = 1500)
public abstract class CreateWorldScreenMixin {

    @Dynamic
    @TargetHandler(
            mixin = "com.kingcontaria.standardsettings.mixins.CreateWorldScreenMixin",
            name = "resetSettings"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    private void doNotTriggerStandardSettings_resetSettings_inQueue(CallbackInfo ci) {
        if (SeedQueue.inQueue()) {
            ci.cancel();
        }
    }

    @Dynamic
    @TargetHandler(
            mixin = "com.kingcontaria.standardsettings.mixins.CreateWorldScreenMixin",
            name = "onWorldJoin"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    private void doNotTriggerStandardSettings_onWorldJoin_inQueue(CallbackInfo ci) {
        if (SeedQueue.inQueue()) {
            ci.cancel();
        }
    }
}
