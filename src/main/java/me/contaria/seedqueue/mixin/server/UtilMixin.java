package me.contaria.seedqueue.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@Mixin(Util.class)
public abstract class UtilMixin {

    @ModifyExpressionValue(
            method = "createWorker",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;clamp(III)I"
            )
    )
    private static int setSeedQueueWorkerThreads(int threads, @Local(argsOnly = true) String name) {
        if (name.equals("SeedQueue")) {
            return SeedQueue.config.getBackgroundExecutorThreads();
        } else if (name.equals("SeedQueue Wall")) {
            return SeedQueue.config.getWallExecutorThreads();
        }
        return threads;
    }

    @ModifyReturnValue(
            method = "method_28123",
            at = @At("RETURN")
    )
    private static ForkJoinWorkerThread setSeedQueueWorkerThreadPriority(ForkJoinWorkerThread thread, String name, ForkJoinPool pool) {
        if (name.equals("SeedQueue")) {
            thread.setPriority(SeedQueue.config.backgroundExecutorThreadPriority);
        } else if (name.equals("SeedQueue Wall")) {
            thread.setPriority(SeedQueue.config.wallExecutorThreadPriority);
        }
        return thread;
    }
}
