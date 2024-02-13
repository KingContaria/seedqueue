package me.contaria.seedqueue.mixin.accessor;

import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.ExecutorService;

@Mixin(Util.class)
public interface UtilAccessor {
    @Invoker("method_28122")
    static ExecutorService seedQueue$createWorkerExecutor(String string) {
        throw new UnsupportedOperationException();
    }
}
