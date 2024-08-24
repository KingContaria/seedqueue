package me.contaria.seedqueue.mixin.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressTracker;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.server.WorldGenerationProgressLogger;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenerationProgressTracker.class)
public abstract class WorldGenerationProgressTrackerMixin implements SQWorldGenerationProgressTracker {
    @Shadow @Final private Long2ObjectOpenHashMap<ChunkStatus> chunkStatuses;
    @Shadow @Final private WorldGenerationProgressLogger progressLogger;
    @Shadow private ChunkPos spawnPos;
    @Shadow @Final private int radius;

    @Unique private long creationTime = -1;

    @Unique private boolean frozenStatesGathered = false;
    @Unique private Long2ObjectOpenHashMap<ChunkStatus> frozenChunkStatuses;
    @Unique private int frozenProgressPercentage;
    @Unique private ChunkPos frozenSpawnPos;

    @Unique private boolean unfrozen = false;

    @Inject(method = "start()V", at = @At("TAIL"))
    private void onStart(CallbackInfo ci) {
        this.creationTime = Util.getMeasuringTimeMs();
    }

    @Inject(method = "setChunkStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/WorldGenerationProgressLogger;setChunkStatus(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/ChunkStatus;)V", shift = At.Shift.BEFORE))
    private void onSetChunkStatus(ChunkPos pos, ChunkStatus status, CallbackInfo ci) {
        if (SeedQueue.config.chunkMapShouldFreeze && !isFrozen() && isPastFreezingTime()) {
            this.freezeHere();
        }
    }


    @Inject(method = "getChunkStatus", at = @At("HEAD"), cancellable = true)
    private void giveFrozenChunkStatus(int x, int z, CallbackInfoReturnable<ChunkStatus> cir) {
        if (this.isFrozen()) {
            cir.setReturnValue(frozenChunkStatuses.get(ChunkPos.toLong(x + this.frozenSpawnPos.x - this.radius, z + this.frozenSpawnPos.z - this.radius)));
        }
    }

    @Inject(method = "getProgressPercentage", at = @At("HEAD"), cancellable = true)
    private void giveFrozenProgressPercentage(CallbackInfoReturnable<Integer> cir) {
        if (this.isFrozen()) {
            cir.setReturnValue(frozenProgressPercentage);
        }
    }

    @Unique
    private boolean isPastFreezingTime() {
        return creationTime != -1 && Util.getMeasuringTimeMs() - this.creationTime > SeedQueue.config.chunkMapFreezeTime;
    }

    @Unique
    private boolean isFrozen() {
        return !unfrozen && frozenStatesGathered;
    }

    @Unique
    private void freezeHere() {
        frozenChunkStatuses = new Long2ObjectOpenHashMap<>(chunkStatuses);
        frozenProgressPercentage = this.progressLogger.getProgressPercentage();
        frozenSpawnPos = spawnPos;
        frozenStatesGathered = true;
    }

    @Override
    public void seedQueue$unfreeze() {
        unfrozen = true;
    }
}
