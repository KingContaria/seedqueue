package me.contaria.seedqueue.mixin.compat.atum;

import me.contaria.seedqueue.SeedQueue;
import me.voidxwalker.autoreset.AttemptTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(AttemptTracker.class)
public abstract class AttemptTrackerMixin {

    @Shadow
    public abstract void register(AttemptTracker.Type type) throws IOException;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void registerBenchmarkResetCounter(CallbackInfo ci) throws IOException {
        this.register(SeedQueue.BENCHMARK_RESETS);
    }
}
