package me.contaria.seedqueue.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.server.WorldGenerationProgressLogger;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldGenerationProgressTracker.class)
public abstract class WorldGenerationProgressTrackerMixin {

    @WrapWithCondition(
            method = "start(Lnet/minecraft/util/math/ChunkPos;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/WorldGenerationProgressLogger;start(Lnet/minecraft/util/math/ChunkPos;)V"
            )
    )
    private boolean muteWorldGenTracker_inQueue(WorldGenerationProgressLogger logger, ChunkPos spawnPos) {
        return SeedQueue.getEntry(Thread.currentThread()) == null;
    }

    @WrapWithCondition(
            method = "setChunkStatus",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/WorldGenerationProgressLogger;setChunkStatus(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/ChunkStatus;)V"
            )
    )
    private boolean muteWorldGenTracker_inQueue(WorldGenerationProgressLogger logger, ChunkPos pos, ChunkStatus status) {
        return SeedQueue.getEntry(Thread.currentThread()) == null;
    }

    @WrapWithCondition(
            method = "stop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/WorldGenerationProgressLogger;stop()V"
            )
    )
    private boolean muteWorldGenTracker_inQueue(WorldGenerationProgressLogger logger) {
        return SeedQueue.getEntry(Thread.currentThread()) == null;
    }
}
