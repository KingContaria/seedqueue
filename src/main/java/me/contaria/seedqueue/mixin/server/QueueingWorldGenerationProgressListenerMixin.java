package me.contaria.seedqueue.mixin.server;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.server.QueueingWorldGenerationProgressListener;
import net.minecraft.util.thread.TaskExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(QueueingWorldGenerationProgressListener.class)
public abstract class QueueingWorldGenerationProgressListenerMixin {

    @WrapWithCondition(
            method = {
                    "start",
                    "setChunkStatus",
                    "stop"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/thread/TaskExecutor;send(Ljava/lang/Object;)V"
            )
    )
    private boolean muteWorldGenTrackerLogging_inQueue(TaskExecutor<?> executor, Object message) {
        return SeedQueue.getEntry(Thread.currentThread()) == null;
    }
}
