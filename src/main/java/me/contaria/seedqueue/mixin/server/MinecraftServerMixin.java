package me.contaria.seedqueue.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.SeedQueueException;
import me.contaria.seedqueue.SeedQueueExecutorWrapper;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.ServerWorldProperties;
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
public abstract class MinecraftServerMixin extends ReentrantThreadExecutor<ServerTask> implements SQMinecraftServer {

    @Shadow
    private volatile boolean loading;

    @Shadow
    @Final
    protected SaveProperties saveProperties;

    @Shadow
    @Final
    private Executor workerExecutor;

    @Unique
    private boolean paused;

    public MinecraftServerMixin(String string) {
        super(string);
    }

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
            method = "setupSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/SpawnLocating;findServerSpawnPoint(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/ChunkPos;Z)Lnet/minecraft/util/math/BlockPos;"
            )
    )
    private static void pauseServerDuringWorldSetup(ServerWorld serverWorld, ServerWorldProperties serverWorldProperties, boolean bl, boolean bl2, boolean bl3, CallbackInfo ci) {
        ((SQMinecraftServer) serverWorld.getServer()).seedQueue$tryPausingServer();
    }

    @Inject(
            method = "prepareStartRegion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;method_16208()V",
                    shift = At.Shift.AFTER
            )
    )
    private void pauseServerDuringWorldGen(CallbackInfo ci) {
        this.seedQueue$tryPausingServer();
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
            this.seedQueue$tryPausingServer();
        }
    }

    @Override
    public boolean seedQueue$shouldPause() {
        SeedQueueEntry seedQueueEntry = SeedQueue.getEntry((MinecraftServer) (Object) this);
        if (seedQueueEntry == null || seedQueueEntry.isDiscarded()) {
            return false;
        }
        if (seedQueueEntry.isReady()) {
            return true;
        }
        if (seedQueueEntry.getWorldPreviewProperties() == null) {
            return false;
        }
        if (!seedQueueEntry.isLocked() && SeedQueue.config.maxWorldGenerationPercentage < 100) {
            WorldGenerationProgressTracker tracker = seedQueueEntry.getWorldGenerationProgressTracker();
            return tracker != null && tracker.getProgressPercentage() >= SeedQueue.config.maxWorldGenerationPercentage;
        }
        return false;
    }

    @Override
    public synchronized void seedQueue$tryPausingServer() {
        if (!this.isOnThread()) {
            throw new IllegalStateException("Tried to pause the server from another thread!");
        }

        if (!this.seedQueue$shouldPause()) {
            return;
        }

        try {
            this.paused = true;
            SeedQueue.thread.ping();
            this.wait();
        } catch (InterruptedException e) {
            throw new SeedQueueException();
        } finally {
            this.paused = false;
        }
    }

    @Override
    public boolean seedQueue$isPaused() {
        return this.paused;
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
