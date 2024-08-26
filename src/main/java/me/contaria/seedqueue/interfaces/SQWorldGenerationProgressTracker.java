package me.contaria.seedqueue.interfaces;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public interface SQWorldGenerationProgressTracker {
    void seedQueue$makeFrozenCopyAfter(long millis);

    /**
     * Should only be used on a new WorldGenerationProgressTracker without calling any of its other methods.
     */
    void seedQueue$setAsFrozenCopy(Long2ObjectOpenHashMap<ChunkStatus> chunkStatuses, ChunkPos spawnPos, int progressPercentage);

    /**
     * Gets a copy of the SQWorldGenerationProgressTracker which was made after the milliseconds provided by seedQueue$makeCopyAfter.
     */
    @Nullable
    WorldGenerationProgressTracker seedQueue$getFrozenCopy();
}
