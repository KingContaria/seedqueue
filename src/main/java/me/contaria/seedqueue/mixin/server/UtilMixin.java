package me.contaria.seedqueue.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@Mixin(Util.class)
public abstract class UtilMixin {

    @ModifyArg(
            method = "method_28122",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;clamp(III)I"
            ),
            index = 2
    )
    private static int maxServerExecutorThreads(int max) {
        return SeedQueue.config.maxServerExecutorThreads;
    }

    @ModifyReturnValue(
            method = "method_28123",
            at = @At("RETURN")
    )
    private static ForkJoinWorkerThread setSeedQueueWorkerThreadPriority(ForkJoinWorkerThread thread, String string, ForkJoinPool pool) {
        if (string.startsWith("SeedQueue")) {
            switch (string) {
                case "SeedQueue Background":
                    thread.setPriority(SeedQueue.config.executorPriority_background);
                    break;
                case "SeedQueue Before Preview":
                    thread.setPriority(SeedQueue.config.executorPriority_beforePreview);
                    break;
                case "SeedQueue After Preview":
                    thread.setPriority(SeedQueue.config.executorPriority_afterPreview);
                    break;
                case "SeedQueue Locked":
                    thread.setPriority(SeedQueue.config.executorPriority_locked);
                    break;
            }
        }
        return thread;
    }
}
