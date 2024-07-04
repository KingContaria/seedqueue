package me.contaria.seedqueue.mixin.accessor;

import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.server.WorldGenerationProgressLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldGenerationProgressTracker.class)
public interface WorldGenerationProgressTrackerAccessor {
    @Accessor
    WorldGenerationProgressLogger getProgressLogger();
}
