package me.contaria.seedqueue.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.SeedQueueException;
import me.contaria.seedqueue.SeedQueueExecutorWrapper;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import me.contaria.seedqueue.mixin.accessor.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.world.SaveProperties;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements SQMinecraftServer {

    @Shadow
    private volatile boolean loading;

    @Shadow
    @Final
    protected SaveProperties saveProperties;

    @Shadow
    @Final
    private Executor workerExecutor;

    @ModifyExpressionValue(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Util;getServerWorkerExecutor()Ljava/util/concurrent/Executor;"
            )
    )
    private Executor wrapExecutor(Executor executor) {
        return new SeedQueueExecutorWrapper(executor);
    }

    @ModifyVariable(
            method = "<init>",
            at = @At("TAIL"),
            argsOnly = true
    )
    private Thread modifyServerThreadName(Thread thread) {
        thread.setName(thread.getName() + " - " + this.saveProperties.getLevelName());
        return thread;
    }

    @Inject(
            method = "prepareStartRegion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerChunkManager;getTotalChunksLoadedCount()I"
            )
    )
    private void test(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci) throws InterruptedException {
    }

    @WrapOperation(
            method = "runServer",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/server/MinecraftServer;loading:Z",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void pauseServer(MinecraftServer server, boolean value, Operation<Void> original) {
        boolean firstTick = !this.loading;

        original.call(server, value);

        if (firstTick) {
            this.tryPausingServer();
        }
    }

    @Unique
    private synchronized void tryPausingServer() {
        SeedQueueEntry seedQueueEntry = SeedQueue.getEntry((MinecraftServer) (Object) this);
        if (seedQueueEntry == null || seedQueueEntry.isDiscarded()) {
            return;
        }

        try {
            SeedQueue.thread.ping();
            this.wait();
        } catch (InterruptedException e) {
            throw new SeedQueueException();
        }
    }

    @Override
    public void seedQueue$setExecutor(Executor executor) {
        ((SeedQueueExecutorWrapper) this.workerExecutor).setExecutor(executor);
    }

    @Override
    public void seedQueue$resetExecutor() {
        ((SeedQueueExecutorWrapper) this.workerExecutor).resetExecutor();
    }
}
