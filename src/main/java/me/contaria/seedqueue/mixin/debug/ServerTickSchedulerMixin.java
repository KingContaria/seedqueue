package me.contaria.seedqueue.mixin.debug;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.debug.CallbackSet;
import me.contaria.seedqueue.debug.CallbackTreeSet;
import net.minecraft.server.world.ServerTickScheduler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.ScheduledTick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(ServerTickScheduler.class)
public abstract class ServerTickSchedulerMixin {

    @Shadow
    @Final
    private ServerWorld world;

    @Shadow @Final private Set<ScheduledTick<?>> scheduledTickActions;
    @Shadow @Final private TreeSet<ScheduledTick<?>> scheduledTickActionsInOrder;
    @Unique
    private final Set<Object> addedToTicks = new HashSet<>();

    @Unique
    private final Set<Object> addedToOrderedTicks = new HashSet<>();

    @Unique
    private final Set<Object> removedFromTicks = new HashSet<>();

    @Unique
    private final Set<Object> removedFromOrderedTicks = new HashSet<>();

    @Unique
    private final Map<Object, StackTraceElement[]> stacktraces = new HashMap<>();

    //@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newHashSet()Ljava/util/HashSet;"))
    private HashSet<?> test1() {
        return new CallbackSet<>(t -> {
            if (!this.addedToOrderedTicks.remove(t)) {
                this.addedToTicks.add(t);
            }
        }, t -> {
            if (!this.removedFromOrderedTicks.remove(t)) {
                this.removedFromTicks.add(t);
            }
        }, () -> {
            throw new IllegalStateException("ooops");
        });
    }

    //@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newTreeSet(Ljava/util/Comparator;)Ljava/util/TreeSet;"))
    private TreeSet<?> test(Comparator<? super Object> comparator) {
        return new CallbackTreeSet<>(comparator, t -> {
            if (!this.addedToTicks.remove(t)) {
                this.addedToOrderedTicks.add(t);
            }
        }, t -> {
            if (!this.removedFromTicks.remove(t)) {
                this.removedFromOrderedTicks.add(t);
            }
        }, () -> {
            throw new IllegalStateException("ooops");
        });
    }

    private boolean ohno;

    //@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;shouldTickBlock(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean test(boolean original) {
        if (!this.world.getServer().isLoading() && !ohno) {
            this.ohno = true;
            System.out.println("oh nononononono");
        }
        return original;
    }

    //@Inject(method = "tick", at = @At("HEAD"))
    private void testing(CallbackInfo ci) {
        if (!this.scheduledTickActions.equals(this.scheduledTickActionsInOrder)) {
            System.out.println("oh no.......");
            Set<?> diff1 = new HashSet<>(this.scheduledTickActions);
            diff1.removeAll(this.scheduledTickActionsInOrder);
            Set<?> diff2 = new HashSet<>(this.scheduledTickActionsInOrder);
            diff2.removeAll(this.scheduledTickActions);
            System.out.println(diff1);
            /*
            for (Object o : diff1) {
                System.out.println(o);
                System.out.println(Arrays.toString(this.stacktraces.get(o)));
            }

             */
            System.out.println(diff2);
            /*
            for (Object o : diff2) {
                System.out.println(o);
                System.out.println(Arrays.toString(this.stacktraces.get(o)));
            }

             */
        }
        /*
        if (!this.addedToTicks.isEmpty() || !this.addedToOrderedTicks.isEmpty() || !this.removedFromTicks.isEmpty() || !this.removedFromOrderedTicks.isEmpty()) {
            System.out.println("oh no.....");
        }

         */
    }

    //@Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/lang/IllegalStateException;<init>(Ljava/lang/String;)V"))
    private void test(CallbackInfo ci) {
        /*
        System.out.println(this.addedToTicks);
        System.out.println(this.addedToOrderedTicks);
        System.out.println(this.removedFromTicks);
        System.out.println(this.removedFromOrderedTicks);

         */
    }

    //@Inject(method = "getScheduledTicks", at = @At("HEAD"))
    private void test(CallbackInfoReturnable<List<ScheduledTick<?>>> cir) {
        this.printOnOffThreadTickSchedule(cir);
    }

    //@Inject(method = "addScheduledTick", at = @At("HEAD"))
    private void printOnOffThreadTickSchedule(CallbackInfo ci) {
        //StackTraceElement[] stackTrace = this.stacktraces.put(scheduledTick, Thread.currentThread().getStackTrace());
        /*
        if (stackTrace != null && (this.scheduledTickActions.contains(scheduledTick) != this.scheduledTickActionsInOrder.contains(scheduledTick))) {
            System.out.println(Arrays.toString(stackTrace));
        }

         */
        if (!this.world.getServer().isOnThread()) {
            SeedQueue.LOGGER.warn("Off-thread tick schedule: " + Thread.currentThread().getName() + " tried to schedule a tick for " + this.world.getServer().getSaveProperties().getLevelName() + ". " + Arrays.toString(Thread.currentThread().getStackTrace()));
        }
    }
}
