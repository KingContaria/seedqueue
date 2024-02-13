package me.contaria.seedqueue.mixin.debug;

import me.contaria.seedqueue.SeedQueue;
import net.minecraft.server.world.ServerTickScheduler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.ScheduledTick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;

@Mixin(ServerTickScheduler.class)
public abstract class ServerTickSchedulerMixin {

    @Shadow @Final private ServerWorld world;

    //@Inject(method = "getScheduledTicks", at = @At("HEAD"))
    private void printOnOffThreadTickSchedule(CallbackInfoReturnable<List<ScheduledTick<?>>> cir) {
        this.printOnOffThreadTickSchedule(cir);
    }

    //@Inject(method = "addScheduledTick", at = @At("HEAD"))
    private void printOnOffThreadTickSchedule(CallbackInfo ci) {
        if (!this.world.getServer().isOnThread()) {
            SeedQueue.LOGGER.warn("Off-thread tick schedule: " + Thread.currentThread().getName() + " tried to schedule a tick for " + this.world.getServer().getSaveProperties().getLevelName() + ". " + Arrays.toString(Thread.currentThread().getStackTrace()));
        }
    }
}
