package me.contaria.seedqueue.mixin.client.render;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueConfig;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.gui.SeedQueueClearScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    @Final
    private Window window;
    @Shadow
    @Nullable
    public Screen currentScreen;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(
                            value = "CONSTANT",
                            args = "stringValue=blit"
                    )
            )
    )
    private void drawSeedQueueChunkMaps(CallbackInfo ci) {
        if (this.currentScreen instanceof SeedQueueClearScreen || SeedQueue.isOnWall() || SeedQueue.config.chunkMapVisibility == SeedQueueConfig.ChunkMapVisibility.FALSE) {
            return;
        }

        int x = 3;
        int y = 3;
        int scale = SeedQueue.config.chunkMapScale;
        for (SeedQueueEntry seedQueueEntry : SeedQueue.SEED_QUEUE) {
            if (seedQueueEntry.isPaused()) {
                continue;
            }
            WorldGenerationProgressTracker worldGenerationProgressTracker = seedQueueEntry.getWorldGenerationProgressTracker();
            if (worldGenerationProgressTracker != null) {
                if (x + worldGenerationProgressTracker.getSize() * scale > this.window.getWidth() - 3) {
                    x = 3;
                    y += worldGenerationProgressTracker.getSize() * scale + 3;
                }
                MatrixStack matrixStack = new MatrixStack();
                LevelLoadingScreen.drawChunkMap(matrixStack, worldGenerationProgressTracker, x + worldGenerationProgressTracker.getSize() * scale / 2, y + worldGenerationProgressTracker.getSize() * scale / 2, scale, 0);
                x += worldGenerationProgressTracker.getSize() * scale + 3;
            }
        }
    }
}
