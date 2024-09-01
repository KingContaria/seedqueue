package me.contaria.seedqueue.interfaces;

import net.minecraft.client.gui.WorldGenerationProgressTracker;

import java.util.Optional;

public interface SQWorldGenerationProgressTracker {
    /**
     * @return a copy of the WorldGenerationProgressTracker which was made after the milliseconds provided by seedQueue$makeCopyAfter
     */
    Optional<WorldGenerationProgressTracker> seedQueue$getFrozenCopy();
}
