package me.contaria.seedqueue.mixin.client;

import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressTracker;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;drawChunkMap(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/gui/WorldGenerationProgressTracker;IIII)V"
            ),
            index = 1
    )
    private WorldGenerationProgressTracker replaceChunkMap(WorldGenerationProgressTracker progress) {
        return ((SQWorldGenerationProgressTracker) progress).seedQueue$getFrozenCopy().orElse(progress);
    }
}
