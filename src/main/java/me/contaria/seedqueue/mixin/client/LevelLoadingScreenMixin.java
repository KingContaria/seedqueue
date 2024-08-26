package me.contaria.seedqueue.mixin.client;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressTracker;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {
    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void setTrackerFreezeTime(WorldGenerationProgressTracker progressProvider, CallbackInfo ci) {
        if (SeedQueue.config.chunkMapFreezing != -1) {
            ((SQWorldGenerationProgressTracker) progressProvider).seedQueue$makeFrozenCopyAfter(SeedQueue.config.chunkMapFreezing);
        }
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;drawChunkMap(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/gui/WorldGenerationProgressTracker;IIII)V"
            ),
            index = 1
    )
    private WorldGenerationProgressTracker replaceChunkMap(WorldGenerationProgressTracker progress) {
        WorldGenerationProgressTracker frozenCopy = ((SQWorldGenerationProgressTracker) progress).seedQueue$getFrozenCopy();
        if (frozenCopy != null) {
            return frozenCopy;
        }
        return progress;
    }
}
