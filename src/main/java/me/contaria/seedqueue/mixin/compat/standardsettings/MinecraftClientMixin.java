package me.contaria.seedqueue.mixin.compat.standardsettings;

import com.bawnorton.mixinsquared.TargetHandler;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

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
    private void loadSettingsCache(CallbackInfo ci) {
        if (!SeedQueue.inQueue() && SeedQueue.currentEntry != null && SeedQueue.currentEntry.loadSettingsCache()) {
            ci.cancel();
        }
    }
}
