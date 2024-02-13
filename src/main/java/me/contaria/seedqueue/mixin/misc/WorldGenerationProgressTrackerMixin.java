package me.contaria.seedqueue.mixin.misc;

import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressListener;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.server.WorldGenerationProgressLogger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldGenerationProgressTracker.class)
public abstract class WorldGenerationProgressTrackerMixin implements WorldGenerationProgressListenerMixin {

    @Shadow
    @Final
    private WorldGenerationProgressLogger progressLogger;

    @Override
    public void seedQueue$mute() {
        ((SQWorldGenerationProgressListener) this.progressLogger).seedQueue$mute();
    }

    @Override
    public void seedQueue$unmute() {
        ((SQWorldGenerationProgressListener) this.progressLogger).seedQueue$unmute();
    }
}
