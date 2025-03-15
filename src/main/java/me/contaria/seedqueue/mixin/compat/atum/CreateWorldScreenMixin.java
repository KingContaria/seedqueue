package me.contaria.seedqueue.mixin.compat.atum;

import com.bawnorton.mixinsquared.TargetHandler;
import me.contaria.seedqueue.SeedQueue;
import me.voidxwalker.autoreset.AttemptTracker;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = CreateWorldScreen.class, priority = 1500)
public abstract class CreateWorldScreenMixin {

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.autoreset.mixin.config.CreateWorldScreenMixin",
            name = "createWorld"
    )
    @ModifyArg(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/voidxwalker/autoreset/AttemptTracker;incrementAndGetWorldName(Lme/voidxwalker/autoreset/AttemptTracker$Type;)Ljava/lang/String;",
                    remap = false
            )
    )
    private AttemptTracker.Type useBenchmarkResetCounter(AttemptTracker.Type type) {
        if (SeedQueue.isBenchmarking()) {
            return SeedQueue.BENCHMARK_RESETS;
        }
        return type;
    }
}
