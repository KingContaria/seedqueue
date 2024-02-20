package me.contaria.seedqueue.mixin.server.synchronization;

import net.minecraft.world.ScheduledTick;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(ScheduledTick.class)
public abstract class ScheduledTickMixin {

    @Unique
    private static final AtomicLong atomicIdCounter = new AtomicLong();

    @Mutable
    @Shadow
    @Final
    private long id;

    @Redirect(
            method = "<init>(Lnet/minecraft/util/math/BlockPos;Ljava/lang/Object;JLnet/minecraft/world/TickPriority;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/ScheduledTick;id:J",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void atomicIdCounter(ScheduledTick<?> tick, long l) {
        this.id = atomicIdCounter.incrementAndGet();
    }
}
