package me.contaria.seedqueue.interfaces;

import net.minecraft.util.math.ChunkPos;

public interface SQProgressLogger {
    int seedqueue$getProgressPercentage(ChunkPos center);
}
