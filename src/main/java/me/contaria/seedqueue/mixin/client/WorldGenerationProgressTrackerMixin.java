package me.contaria.seedqueue.mixin.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressTracker;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
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
    @Shadow
    @Final
    private Long2ObjectOpenHashMap<ChunkStatus> chunkStatuses;

    @Shadow
    private ChunkPos spawnPos;

    @Shadow
    @Final
    private int radius;

    @Shadow
    public abstract @Nullable ChunkStatus getChunkStatus(int x, int z);

    @Shadow
    @Final
    private int centerSize;

    @Unique
    private long freezeTime = -1;

    @Unique
    @Nullable
    private WorldGenerationProgressTracker frozenCopy;

    @Unique
    private volatile int progressLevel;
    @Unique
    private volatile int progressPercentage;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void markFreezeTimeStart(CallbackInfo ci) {
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
    private void onSetChunkStatus(CallbackInfo ci) {
        if (this.frozenCopy == null && this.isPastFreezingTime()) {
            this.makeFrozenCopy();
        }
    }

    @Inject(
            method = "start(Lnet/minecraft/util/math/ChunkPos;)V",
            at = @At("TAIL")
    )
    private void recalculateProgressCount(CallbackInfo ci) {
        this.progressLevel = 0;
        this.calculateProgressCount();
    }

    @Inject(
            method = "setChunkStatus",
            at = @At("TAIL")
    )
    private void recalculateProgressCount(ChunkPos pos, ChunkStatus status, CallbackInfo ci) {
        if (status == ChunkStatus.FULL) {
            this.calculateProgressCount();
        }
    }

    @Unique
    private void makeFrozenCopy() {
        WorldGenerationProgressTracker frozenCopy = new WorldGenerationProgressTracker(this.radius - ChunkStatus.getMaxTargetGenerationRadius()); // This will trigger a makeFrozenCopyAfter inside the frozen copy itself which could lead to further recursion, but as long as setChunkStatus isn't called, this will never be an issue.
        ((WorldGenerationProgressTrackerMixin) (Object) frozenCopy).setAsFrozenCopy(this.chunkStatuses, this.spawnPos);
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
    private void setAsFrozenCopy(Long2ObjectOpenHashMap<ChunkStatus> chunkStatuses, ChunkPos spawnPos) {
        this.chunkStatuses.putAll(chunkStatuses);
        this.spawnPos = spawnPos;
    }

    @Override
    public Optional<WorldGenerationProgressTracker> seedQueue$getFrozenCopy() {
        return Optional.ofNullable(this.frozenCopy);
    }

    @Unique
    private void calculateProgressCount() {
        // load cached level to avoid checking levels we already know are full
        int level = this.progressLevel;
        int count = (level * 2 - 1) * (level * 2 - 1);
        boolean end = false;

        for (; level < this.radius; level++) {
            // travel left to right on the x-axis
            // when on the right and leftmost bounds of the current level,
            // check all the y values of that ring,
            // otherwise, just check the top and bottom of the ring.
            // if any chunks are missing in the ring,
            // no future rings are searched but the current ring is completed.
            //
            //
            // ↑ then ->
            //
            // - - - - -
            // - + + + -
            // - + = + -
            // - + + + -
            // - - - - -

            // adding radius as WorldGenerationProgressTracker#getChunkStatus subtracts it
            int leftX = -level + this.radius;
            int rightX = level + this.radius;
            int bottomZ = -level + this.radius;
            int topZ = level + this.radius;

            for (int x = leftX; x <= rightX; ++x) {
                boolean onBounds = x == leftX || x == rightX;
                for (int z = bottomZ; z <= topZ; z += onBounds ? 1 : level * 2) {
                    if (this.getChunkStatus(x, z) == ChunkStatus.FULL) {
                        count++;
                    } else {
                        end = true;
                    }
                }
            }

            if (end) {
                break;
            }
        }

        this.progressLevel = level;
        this.progressPercentage = MathHelper.clamp(count * 100 / (this.centerSize * this.centerSize), 0, 100);
    }

    @Override
    public int seedQueue$getProgressPercentage() {
        return this.progressPercentage;
    }
}
