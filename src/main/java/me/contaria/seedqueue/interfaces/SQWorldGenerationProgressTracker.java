package me.contaria.seedqueue.interfaces;

import net.minecraft.client.gui.WorldGenerationProgressTracker;

import java.util.Optional;

public interface SQWorldGenerationProgressTracker {
    /**
     * @return a copy of the WorldGenerationProgressTracker which was made after the milliseconds provided by seedQueue$makeCopyAfter
     */
    Optional<WorldGenerationProgressTracker> seedQueue$getFrozenCopy();

    /**
     * Calculates a more accurate progress percentage by counting chunks in rings and only including outer layers if inner layers are complete. This tries to address the effect ocean spawns have on inflating the progress percentage in vanilla's {@link net.minecraft.server.WorldGenerationProgressLogger}
     *
     * @return The percent of spawn chunks that are generated, in the range of 0 to 100.
     */
    int seedQueue$getProgressPercentage();
}
