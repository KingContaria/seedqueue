package me.contaria.seedqueue.mixin.compat.worldpreview;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Inject(
            method = "loadWorld",
            at = @At("TAIL")
    )
    private void tooLateToConfigureWorldPreview(CallbackInfo ci) {
        SeedQueueEntry entry = SeedQueue.getEntry((MinecraftServer) (Object) this);
        if (entry != null && !SeedQueue.config.shouldUseWall()) {
            entry.setWorldPreviewProperties(null);
        }
    }
}
