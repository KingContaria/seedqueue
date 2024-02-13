package me.contaria.seedqueue.mixin.misc;

import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressListener;
import net.minecraft.server.WorldGenerationProgressListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldGenerationProgressListener.class)
public interface WorldGenerationProgressListenerMixin extends SQWorldGenerationProgressListener {
}
