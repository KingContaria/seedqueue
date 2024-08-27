package me.contaria.seedqueue.mixin.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressTracker;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.server.WorldGenerationProgressLogger;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(WorldGenerationProgressTracker.class)
public abstract class WorldGenerationProgressTrackerMixin implements SQWorldGenerationProgressTracker {
    @Shadow @Final
    private Long2ObjectOpenHashMap<ChunkStatus> chunkStatuses;
    @Shadow
    private ChunkPos spawnPos;
    @Shadow @Final
    private int radius;

    @Shadow @Final private WorldGenerationProgressLogger progressLogger;
    @Unique
    private long freezeTime = -1;
    @Unique
    @Nullable
    WorldGenerationProgressTracker frozenCopy = null;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void markFreezeTimeStart(int radius, CallbackInfo ci) {
        long chunkMapFreezingTime = SeedQueue.config.chunkMapFreezing;
        if (chunkMapFreezingTime != -1 && this.freezeTime == -1) {
            this.makeFrozenCopyAfter(chunkMapFreezingTime);
        }
    }

    @Inject(
            method = "setChunkStatus",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/WorldGenerationProgressLogger;setChunkStatus(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/ChunkStatus;)V"
            )
    )
    private void onSetChunkStatus(ChunkPos pos, ChunkStatus status, CallbackInfo ci) {
        if (this.frozenCopy == null && this.isPastFreezingTime()) {
            this.makeFrozenCopy();
        }
    }

    @Unique
    private void makeFrozenCopy() {
        WorldGenerationProgressTracker frozenCopy = new WorldGenerationProgressTracker(this.radius - ChunkStatus.getMaxTargetGenerationRadius()); // This will trigger a makeFrozenCopyAfter inside the frozen copy itself which could lead to further recursion, but as long as setChunkStatus isn't called, this will never be an issue.
        ((WorldGenerationProgressTrackerMixin) (Object) this.frozenCopy).setAsFrozenCopy(this.chunkStatuses, this.spawnPos, this.progressLogger.getProgressPercentage());
        this.frozenCopy = frozenCopy;
    }

    @Unique
    private boolean isPastFreezingTime() {
        return this.freezeTime != -1 && Util.getMeasuringTimeMs() > this.freezeTime;
    }

    @Unique
    private void makeFrozenCopyAfter(long millis) {
        this.freezeTime = Util.getMeasuringTimeMs() + millis;
    }

    @Unique
    private void setAsFrozenCopy(Long2ObjectOpenHashMap<ChunkStatus> chunkStatuses, ChunkPos spawnPos, int progressPercentage) {
        this.chunkStatuses.putAll(chunkStatuses);
        this.spawnPos = spawnPos;
    }

    @Override
    public Optional<WorldGenerationProgressTracker> seedQueue$getFrozenCopy() {
        return Optional.ofNullable(this.frozenCopy);
    }
}
