package me.contaria.seedqueue.mixin.compat.standardsettings;

import com.bawnorton.mixinsquared.TargetHandler;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    @Final
    public GameOptions options;

    @Dynamic
    @TargetHandler(
            mixin = "me.contaria.standardsettings.mixin.MinecraftClientMixin",
            name = "reset"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    private void doNotTriggerStandardSettings_reset_inQueue(CallbackInfo ci) {
        if (SeedQueue.inQueue()) {
            ci.cancel();
            return;
        }
        if (SeedQueue.currentEntry != null) {
            WorldPreviewProperties wpProperties = SeedQueue.currentEntry.getWorldPreviewProperties();
            if (wpProperties != null && wpProperties.getSettingsCache() != null) {
                wpProperties.getSettingsCache().load();
                this.options.perspective = wpProperties.getPerspective();
                ci.cancel();
            }
        }
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.contaria.standardsettings.mixin.MinecraftClientMixin",
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

    @Dynamic
    @TargetHandler(
            mixin = "me.contaria.standardsettings.mixin.MinecraftClientMixin",
            name = "resetPendingActions"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    private void doNotTriggerStandardSettings_resetPendingActions_inQueue(CallbackInfo ci) {
        if (SeedQueue.inQueue()) {
            ci.cancel();
        }
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.contaria.standardsettings.mixin.MinecraftClientMixin",
            name = "setLastWorld"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    private void doNotTriggerStandardSettings_setLastWorld_inQueue(CallbackInfo ci) {
        if (SeedQueue.inQueue()) {
            ci.cancel();
        }
    }
}
