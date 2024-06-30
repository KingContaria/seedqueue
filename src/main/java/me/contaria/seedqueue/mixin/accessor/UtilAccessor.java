package me.contaria.seedqueue.mixin.accessor;

import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.ExecutorService;

@Mixin(Util.class)
public interface UtilAccessor {
    @Invoker("attemptShutdown")
    static void seedQueue$attemptShutdown(ExecutorService executorService) {
        throw new UnsupportedOperationException();
    }

    @Invoker("uncaughtExceptionHandler")
    static void seedQueue$uncaughtExceptionHandler(Thread thread, Throwable throwable) {
        throw new UnsupportedOperationException();
    }
}
