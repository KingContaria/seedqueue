package me.contaria.seedqueue.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.SeedQueueExecutorWrapper;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
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
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantThreadExecutor<ServerTask> implements SQMinecraftServer {

    @Shadow
    private volatile boolean loading;

    @Shadow
    @Final
    protected LevelStorage.Session session;

    @Shadow
    @Final
    private Executor workerExecutor;

    @Shadow public abstract PlayerManager getPlayerManager();
    @Unique
    private volatile boolean pauseScheduled;

    @Unique
    private volatile boolean paused;

    @Unique
    private AtomicInteger maxEntityId;

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
        if (SeedQueue.inQueue()) {
            return new SeedQueueExecutorWrapper(executor);
        }
        return executor;
    }

    @ModifyVariable(
            method = "<init>",
            at = @At("TAIL"),
            argsOnly = true
    )
    private Thread modifyServerThreadProperties(Thread thread) {
        if (SeedQueue.inQueue()) {
            thread.setPriority(SeedQueue.config.serverThreadPriority);
        }
        thread.setName(thread.getName() + " - " + this.session.getDirectoryName());
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
                    target = "Lnet/minecraft/server/MinecraftServer;runTasksTillTickEnd()V",
                    shift = At.Shift.AFTER
            )
    )
    private void pauseServerDuringWorldGen(CallbackInfo ci) {
        this.seedQueue$tryPausingServer();
    }

    @Inject(
            method = "loadWorld",
            at = @At("TAIL")
    )
    private void discardWorldPreviewPropertiesOnLoad(CallbackInfo ci) {
        SeedQueueEntry entry = this.getEntry();
        if (entry != null && !SeedQueue.config.shouldUseWall()) {
            entry.setWorldPreviewProperties(null);
        }
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
        // "loading" is a bad mapping and actually means something more like "finishedLoading"
        if (this.loading || !this.inQueue()) {
            original.call(server, value);
            return;
        }

        original.call(server, value);

        SeedQueue.LOGGER.info("Finished loading \"{}\".", this.session.getDirectoryName());
        this.seedQueue$tryPausingServer();
    }

    @Unique
    protected SeedQueueEntry getEntry() {
        return SeedQueue.getEntry((MinecraftServer) (Object) this);
    }

    @Unique
    protected boolean inQueue() {
        return this.getEntry() != null;
    }

    @Override
    public boolean seedQueue$shouldPause() {
        SeedQueueEntry entry = this.getEntry();
        if (entry == null || entry.isDiscarded()) {
            return false;
        }
        if (this.pauseScheduled || entry.isReady()) {
            return true;
        }
        if (!entry.hasWorldPreview()) {
            return false;
        }
        if (!entry.isLocked() && SeedQueue.config.maxWorldGenerationPercentage < 100) {
            WorldGenerationProgressTracker tracker = entry.getWorldGenerationProgressTracker();
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
            this.pauseScheduled = false;
            SeedQueue.ping();
            SeedQueue.LOGGER.info("Pausing \"{}\"...", this.session.getDirectoryName());
            this.wait();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to pause server in SeedQueue!", e);
        } finally {
            this.paused = false;
        }
    }

    @Override
    public boolean seedQueue$isPaused() {
        return this.paused;
    }

    @Override
    public boolean seedQueue$isScheduledToPause() {
        return this.pauseScheduled;
    }

    @Override
    public synchronized void seedQueue$schedulePause() {
        if (!this.paused) {
            this.pauseScheduled = true;
        }
    }

    @Override
    public synchronized void seedQueue$unpause() {
        this.pauseScheduled = false;
        if (this.paused) {
            this.notify();
            this.paused = false;
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

    @Override
    public synchronized int seedQueue$incrementAndGetEntityID(int currentMaxID) {
        if (this.maxEntityId == null) {
            this.maxEntityId = new AtomicInteger(currentMaxID);
            // currentMaxID is the ID after the initial incrementAndGet
            // to retain full vanilla parity we return it instead of incrementing again and skipping one id
            return currentMaxID;
        }
        return this.maxEntityId.incrementAndGet();
    }
}
