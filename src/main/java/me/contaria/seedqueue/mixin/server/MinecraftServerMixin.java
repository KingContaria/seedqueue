package me.contaria.seedqueue.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.SeedQueueExecutorWrapper;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import me.contaria.seedqueue.mixin.accessor.EntityAccessor;
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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

    @Unique
    private CompletableFuture<SeedQueueEntry> seedQueueEntry;

    @Unique
    private volatile boolean pauseScheduled;

    @Unique
    private volatile boolean paused;

    @Unique
    private final AtomicInteger maxEntityId = new AtomicInteger(EntityAccessor.seedQueue$getMAX_ENTITY_ID().get());

    @Shadow
    public abstract PlayerManager getPlayerManager();

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
        String name = this.session.getDirectoryName();
        if (name.startsWith("Random Speedrun #") || name.startsWith("Set Speedrun #")) {
            thread.setName(thread.getName() + " " + name.substring(name.indexOf('#')));
        } else {
            thread.setName(thread.getName() + " - " + name);
        }
        return thread;
    }

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void setSeedQueueEntry(CallbackInfo ci) {
        if (SeedQueue.inQueue()) {
            this.seedQueueEntry = new CompletableFuture<>();
        }
    }

    @Inject(
            method = "loadWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/WorldGenerationProgressListenerFactory;create(I)Lnet/minecraft/server/WorldGenerationProgressListener;"
            )
    )
    private void setThreadLocalSeedQueueEntry(CallbackInfo ci) {
        this.seedQueue$getEntry().ifPresent(SeedQueue.LOCAL_ENTRY::set);
    }

    @Inject(
            method = "setupSpawn",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/biome/source/BiomeSource;locateBiome(IIIILjava/util/List;Ljava/util/Random;)Lnet/minecraft/util/math/BlockPos;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/server/network/SpawnLocating;findServerSpawnPoint(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/ChunkPos;Z)Lnet/minecraft/util/math/BlockPos;"
                    )
            },
            cancellable = true
    )
    private static void pauseOrResetServerDuringWorldSetup(ServerWorld serverWorld, ServerWorldProperties serverWorldProperties, boolean bl, boolean bl2, boolean bl3, CallbackInfo ci) {
        ((SQMinecraftServer) serverWorld.getServer()).seedQueue$tryPausingServer();
        if (((SQMinecraftServer) serverWorld.getServer()).seedQueue$isDiscarded()) {
            ci.cancel();
        }
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
    private void discardPreviewPropertiesOnLoad(CallbackInfo ci) {
        if (!SeedQueue.config.shouldUseWall()) {
            this.seedQueue$getEntry().ifPresent(entry -> entry.setPreviewProperties(null));
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
        if (this.loading || !this.seedQueue$inQueue()) {
            original.call(server, value);
            return;
        }

        original.call(server, value);

        SeedQueue.LOGGER.info("Finished loading \"{}\".", this.session.getDirectoryName());
        this.seedQueue$tryPausingServer();
        this.seedQueueEntry = null;
    }

    @Override
    public Optional<SeedQueueEntry> seedQueue$getEntry() {
        return Optional.ofNullable(this.seedQueueEntry).map(CompletableFuture::join);
    }

    @Override
    public boolean seedQueue$inQueue() {
        return this.seedQueueEntry != null;
    }

    @Override
    public void seedQueue$setEntry(SeedQueueEntry entry) {
        this.seedQueueEntry.complete(entry);
    }

    @Override
    public boolean seedQueue$shouldPause() {
        CompletableFuture<SeedQueueEntry> future = this.seedQueueEntry;
        if (future == null) {
            return false;
        }
        SeedQueueEntry entry = this.seedQueueEntry.join();
        if (entry.isLoaded() || entry.isDiscarded()) {
            return false;
        }
        if (this.pauseScheduled || entry.isReady()) {
            return true;
        }
        if (!entry.hasWorldPreview() || entry.isLocked()) {
            return false;
        }
        if (SeedQueue.config.resumeOnFilledQueue && entry.isMaxWorldGenerationReached() && SeedQueue.isFull()) {
            return false;
        }
        if (SeedQueue.config.maxWorldGenerationPercentage < 100 && entry.getProgressPercentage() >= SeedQueue.config.maxWorldGenerationPercentage) {
            entry.setMaxWorldGenerationReached();
            return true;
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
    public boolean seedQueue$isDiscarded() {
        return this.seedQueueEntry != null && this.seedQueueEntry.isDone() && this.seedQueueEntry.join().isDiscarded();
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
    public int seedQueue$incrementAndGetEntityID() {
        return this.maxEntityId.incrementAndGet();
    }
}
