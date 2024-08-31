package me.contaria.seedqueue.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.util.Function4;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.SeedQueueException;
import me.contaria.seedqueue.SeedQueueExecutorWrapper;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.gui.wall.SeedQueueWallScreen;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import me.contaria.seedqueue.interfaces.SQSoundManager;
import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressLogger;
import me.contaria.seedqueue.mixin.accessor.MinecraftServerAccessor;
import me.contaria.seedqueue.mixin.accessor.PlayerEntityAccessor;
import me.contaria.seedqueue.mixin.accessor.WorldGenerationProgressTrackerAccessor;
import me.voidxwalker.autoreset.Atum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.net.Proxy;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

@Mixin(value = MinecraftClient.class, priority = 500)
public abstract class MinecraftClientMixin {

    @Shadow
    @Final
    private AtomicReference<WorldGenerationProgressTracker> worldGenProgressTracker;
    @Shadow
    @Nullable
    public Screen currentScreen;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void LogSystemInformation(CallbackInfo ci) {
        if (Boolean.parseBoolean(System.getProperty("seedqueue.logSystemInfo", "true"))) {
            SeedQueue.logSystemInformation();
        }
    }

    @Inject(
            method = "createWorld",
            at = @At("TAIL")
    )
    private void startSeedQueue(CallbackInfo ci) {
        // SeedQueue is started after one world has been created so AntiResourceReload is guaranteed to have populated its cache
        // this means we don't have to worry about synchronizing in that area
        if (Atum.isRunning() && !SeedQueue.isActive()) {
            SeedQueue.start();
        }
    }

    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/LevelStorage;createSession(Ljava/lang/String;)Lnet/minecraft/world/level/storage/LevelStorage$Session;"
            )
    )
    private LevelStorage.Session loadSession(LevelStorage levelStorage, String directoryName, Operation<LevelStorage.Session> original) {
        if (!SeedQueue.inQueue() && SeedQueue.currentEntry != null) {
            return SeedQueue.currentEntry.getSession();
        }
        return original.call(levelStorage, directoryName);
    }

    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;createIntegratedResourceManager(Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/world/level/storage/LevelStorage$Session;)Lnet/minecraft/client/MinecraftClient$IntegratedResourceManager;"
            )
    )
    private MinecraftClient.IntegratedResourceManager loadIntegratedResourceManager(MinecraftClient client, RegistryTracker.Modifiable modifiable, Function<LevelStorage.Session, DataPackSettings> function, Function4<LevelStorage.Session, RegistryTracker.Modifiable, ResourceManager, DataPackSettings, SaveProperties> function4, boolean bl, LevelStorage.Session session, Operation<MinecraftClient.IntegratedResourceManager> original) {
        if (!SeedQueue.inQueue() && SeedQueue.currentEntry != null) {
            return SeedQueue.currentEntry.getResourceManager();
        }
        return original.call(client, modifiable, function, function4, bl, session);
    }

    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "NEW",
                    target = "(Ljava/net/Proxy;Ljava/lang/String;)Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;",
                    remap = false
            )
    )
    private YggdrasilAuthenticationService loadYggdrasilAuthenticationService(Proxy proxy, String clientToken, Operation<YggdrasilAuthenticationService> original) {
        if (!SeedQueue.inQueue() && SeedQueue.currentEntry != null) {
            return SeedQueue.currentEntry.getYggdrasilAuthenticationService();
        }
        return original.call(proxy, clientToken);
    }

    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;createMinecraftSessionService()Lcom/mojang/authlib/minecraft/MinecraftSessionService;",
                    remap = false
            )
    )
    private MinecraftSessionService loadMinecraftSessionService(YggdrasilAuthenticationService service, Operation<MinecraftSessionService> original) {
        if (!SeedQueue.inQueue() && SeedQueue.currentEntry != null) {
            return SeedQueue.currentEntry.getMinecraftSessionService();
        }
        return original.call(service);
    }

    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;createProfileRepository()Lcom/mojang/authlib/GameProfileRepository;",
                    remap = false
            )
    )
    private GameProfileRepository loadGameProfileRepository(YggdrasilAuthenticationService service, Operation<GameProfileRepository> original) {
        if (!SeedQueue.inQueue() && SeedQueue.currentEntry != null) {
            return SeedQueue.currentEntry.getGameProfileRepository();
        }
        return original.call(service);
    }

    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "NEW",
                    target = "(Lcom/mojang/authlib/GameProfileRepository;Ljava/io/File;)Lnet/minecraft/util/UserCache;"
            )
    )
    private UserCache loadUserCache(GameProfileRepository profileRepository, File cacheFile, Operation<UserCache> original) {
        if (!SeedQueue.inQueue() && SeedQueue.currentEntry != null) {
            UserCache userCache = SeedQueue.currentEntry.getUserCache();
            if (userCache != null) {
                return userCache;
            }
        }
        if (SeedQueue.inQueue() && SeedQueue.config.shouldUseWall()) {
            // creating the UserCache is quite expensive compared to the rest of the server creation, so we do it lazily (see "loadServer")
            return null;
        }
        return original.call(profileRepository, cacheFile);
    }

    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;startServer(Ljava/util/function/Function;)Lnet/minecraft/server/MinecraftServer;"
            )
    )
    private MinecraftServer loadServer(Function<Thread, MinecraftServer> serverFactory, Operation<MinecraftServer> original, @Local UserCache userCache) {
        if (!SeedQueue.inQueue() && SeedQueue.currentEntry != null) {
            // see "loadUserCache"
            MinecraftServer server = SeedQueue.currentEntry.getServer();
            if (SeedQueue.currentEntry.getUserCache() == null) {
                ((MinecraftServerAccessor) server).seedQueue$setUserCache(userCache);
            }
            server.getThread().setPriority(Thread.NORM_PRIORITY);
            return server;
        }
        return original.call(serverFactory);
    }

    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/MinecraftClient;server:Lnet/minecraft/server/integrated/IntegratedServer;",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void queueServer(MinecraftClient client, IntegratedServer server, Operation<Void> original, @Local LevelStorage.Session session, @Local MinecraftClient.IntegratedResourceManager resourceManager, @Local YggdrasilAuthenticationService yggdrasilAuthenticationService, @Local MinecraftSessionService minecraftSessionService, @Local GameProfileRepository gameProfileRepository, @Local UserCache userCache) {
        if (SeedQueue.inQueue()) {
            ((SQMinecraftServer) server).seedQueue$setExecutor(SeedQueueExecutorWrapper.SEEDQUEUE_EXECUTOR);
            SeedQueue.add(new SeedQueueEntry(server, session, resourceManager, yggdrasilAuthenticationService, minecraftSessionService, gameProfileRepository, userCache));
            return;
        }
        original.call(client, server);
        if (SeedQueue.currentEntry != null) {
            ((SQMinecraftServer) server).seedQueue$resetExecutor();
            SeedQueue.currentEntry.load();
        }
    }

    @WrapOperation(
            method = "method_17533",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/atomic/AtomicReference;set(Ljava/lang/Object;)V",
                    remap = false
            )
    )
    private void saveWorldGenerationProgressTracker(AtomicReference<?> instance, Object tracker, Operation<Void> original) {
        Optional<SeedQueueEntry> entry = SeedQueue.getThreadLocalEntry();
        if (entry.isPresent()) {
            ((SQWorldGenerationProgressLogger) ((WorldGenerationProgressTrackerAccessor) tracker).getProgressLogger()).seedQueue$mute();
            entry.get().setWorldGenerationProgressTracker((WorldGenerationProgressTracker) tracker);
            return;
        }
        original.call(instance, tracker);
    }

    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/atomic/AtomicReference;get()Ljava/lang/Object;",
                    ordinal = 0,
                    remap = false
            )
    )
    private void loadWorldGenerationProgressTracker(CallbackInfo ci) {
        if (!SeedQueue.inQueue() && SeedQueue.currentEntry != null) {
            WorldGenerationProgressTracker tracker = SeedQueue.currentEntry.getWorldGenerationProgressTracker();
            // tracker could be null if the SeedQueueEntry is loaded before the server creates the tracker,
            // in that case the vanilla logic will loop and wait for the tracker to be created
            if (tracker != null) {
                ((SQWorldGenerationProgressLogger) ((WorldGenerationProgressTrackerAccessor) tracker).getProgressLogger()).seedQueue$unmute();
                this.worldGenProgressTracker.set(tracker);
            }
        }
    }

    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
                    ordinal = 0,
                    remap = false
            )
    )
    private void throwSeedQueueException_createSessionFailure(Logger logger, String message, Object p0, Object p1, Operation<Void> original) {
        if (SeedQueue.inQueue()) {
            throw new SeedQueueException("Failed to read level data!", (Throwable) p1);
        }
        original.call(logger, message, p0, p1);
    }

    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Throwable;)V",
                    remap = false
            )
    )
    private void throwSeedQueueException_loadDataPacksFailure(Logger logger, String message, Throwable t, Operation<Void> original) {
        if (SeedQueue.inQueue()) {
            throw new SeedQueueException("Failed to load datapacks!", t);
        }
        original.call(logger, message, t);
    }

    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;showExperimentalWarning(Lnet/minecraft/client/MinecraftClient$WorldLoadAction;Ljava/lang/String;ZLjava/lang/Runnable;)V"
            )
    )
    private void throwSeedQueueException_legacyWorldLoadFailure(CallbackInfo ci) {
        if (SeedQueue.inQueue()) {
            throw new SeedQueueException("Failed to load legacy world!");
        }
    }

    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/MinecraftClient;isIntegratedServerRunning:Z",
                    opcode = Opcodes.PUTFIELD
            ),
            cancellable = true
    )
    private void cancelJoiningWorld(CallbackInfo ci) {
        if (SeedQueue.inQueue()) {
            ci.cancel();
        }
    }

    @WrapWithCondition(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;disconnect()V"
            )
    )
    private boolean cancelDisconnect(MinecraftClient client) {
        return !SeedQueue.inQueue();
    }

    @WrapWithCondition(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/atomic/AtomicReference;set(Ljava/lang/Object;)V",
                    remap = false
            )
    )
    private boolean cancelWorldGenTrackerSetNull(AtomicReference<?> instance, Object value) {
        return !SeedQueue.inQueue();
    }

    @WrapWithCondition(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/LevelStorage$Session;backupLevelDataFile(Lnet/minecraft/util/registry/RegistryTracker;Lnet/minecraft/world/SaveProperties;)V"
            )
    )
    private boolean cancelSessionLevelDatInit(LevelStorage.Session instance, RegistryTracker registryTracker, SaveProperties saveProperties) {
        return SeedQueue.inQueue() || SeedQueue.currentEntry == null;
    }

    @WrapWithCondition(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resource/ServerResourceManager;loadRegistryTags()V"
            )
    )
    private boolean cancelLoadingRegistryTags(ServerResourceManager manager) {
        return !SeedQueue.inQueue();
    }

    @WrapWithCondition(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/SkullBlockEntity;setUserCache(Lnet/minecraft/util/UserCache;)V"
            )
    )
    private boolean cancelSetUserCache(UserCache value) {
        return !SeedQueue.inQueue();
    }

    @WrapWithCondition(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/SkullBlockEntity;setSessionService(Lcom/mojang/authlib/minecraft/MinecraftSessionService;)V"
            )
    )
    private boolean cancelSetSessionService(MinecraftSessionService value) {
        return !SeedQueue.inQueue();
    }

    @WrapWithCondition(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/UserCache;setUseRemote(Z)V"
            )
    )
    private boolean cancelSetUseRemote(boolean value) {
        return !SeedQueue.inQueue();
    }

    @WrapWithCondition(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;openScreen(Lnet/minecraft/client/gui/screen/Screen;)V",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;<init>(Lnet/minecraft/client/gui/WorldGenerationProgressTracker;)V"
                    )
            )
    )
    private boolean smoothTransition(MinecraftClient client, Screen screen) {
        if (SeedQueue.inQueue()) {
            return false;
        }
        if (SeedQueue.currentEntry == null) {
            return true;
        }
        return !SeedQueue.currentEntry.isReady();
    }

    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;render(Z)V"
            )
    )
    private void applyWorldPreviewProperties(CallbackInfo ci) {
        WorldPreviewProperties wpProperties;
        if (!ModCompat.worldpreview$inPreview() && SeedQueue.currentEntry != null && (wpProperties = SeedQueue.currentEntry.getWorldPreviewProperties()) != null) {
            wpProperties.apply();
            // player model configuration is suppressed in WorldPreviewMixin#doNotSetPlayerModelParts_inQueue for SeedQueue worlds
            // when using wall, player model will be configured in SeedQueueEntry#setSettingsCache
            if (SeedQueue.currentEntry.getSettingsCache() == null) {
                // see WorldPreview#configure
                int playerModelPartsBitMask = 0;
                for (PlayerModelPart playerModelPart : MinecraftClient.getInstance().options.getEnabledPlayerModelParts()) {
                    playerModelPartsBitMask |= playerModelPart.getBitFlag();
                }
                wpProperties.getPlayer().getDataTracker().set(PlayerEntityAccessor.seedQueue$getPLAYER_MODEL_PARTS(), (byte) playerModelPartsBitMask);
            }
        }
    }

    @ModifyExpressionValue(
            method = "createIntegratedResourceManager",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Util;getServerWorkerExecutor()Ljava/util/concurrent/Executor;"
            )
    )
    private Executor useSeedQueueExecutorForCreatingResourcesInQueue(Executor serverWorkerExecutor) {
        if (SeedQueue.inQueue()) {
            return SeedQueueExecutorWrapper.SEEDQUEUE_EXECUTOR;
        }
        return serverWorkerExecutor;
    }

    @ModifyArg(
            method = "createIntegratedResourceManager",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resource/ServerResourceManager;reload(Ljava/util/List;Lnet/minecraft/server/command/CommandManager$RegistrationEnvironment;ILjava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
            ),
            index = 4
    )
    private Executor useSeedQueueExecutorForCreatingResourcesInQueue2(Executor executor) {
        if (SeedQueue.inQueue()) {
            return SeedQueueExecutorWrapper.SEEDQUEUE_EXECUTOR;
        }
        return executor;
    }

    @WrapWithCondition(
            method = "createIntegratedResourceManager",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;runTasks(Ljava/util/function/BooleanSupplier;)V"
            )
    )
    private boolean doNotRunTasksOnSeedQueueThread(MinecraftClient client, BooleanSupplier booleanSupplier) {
        return !SeedQueue.inQueue();
    }

    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At("TAIL")
    )
    private void clearCurrentSeedQueueEntry(CallbackInfo ci) {
        if (!SeedQueue.inQueue()) {
            SeedQueue.clearCurrentEntry();
        }
    }

    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At("TAIL")
    )
    private void pingSeedQueueThreadOnLoadingWorld(CallbackInfo ci) {
        if (!SeedQueue.inQueue()) {
            SeedQueue.ping();
        }
    }

    @Inject(
            method = "openScreen",
            at = @At("RETURN")
    )
    private void pingSeedQueueThreadOnOpeningWall(Screen screen, CallbackInfo ci) {
        if (screen instanceof SeedQueueWallScreen) {
            SeedQueue.ping();
        }
    }

    @WrapWithCondition(
            method = {
                    "reset",
                    "setScreenAndRender"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;render(Z)V"
            )
    )
    private boolean skipIntermissionScreens(MinecraftClient instance, boolean tick) {
        return !SeedQueue.isActive();
    }

    @WrapOperation(
            method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/integrated/IntegratedServer;isStopping()Z"
            )
    )
    private boolean fastQuit(IntegratedServer server, Operation<Boolean> original) {
        return original.call(server) || (SeedQueue.isActive() && !ModCompat.fastReset$shouldSave(server));
    }

    @Inject(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;render(Z)V"
            )
    )
    private void runClientTasks(CallbackInfo ci) {
        SeedQueue.runClientThreadTasks();
    }

    @Inject(
            method = "getFramerateLimit",
            at = @At("HEAD"),
            cancellable = true
    )
    private void modifyFPSOnWall(CallbackInfoReturnable<Integer> cir) {
        if (SeedQueue.isOnWall()) {
            cir.setReturnValue(SeedQueue.config.wallFPS);
        }
    }

    @ModifyReturnValue(
            method = "shouldMonitorTickDuration",
            at = @At("RETURN")
    )
    private boolean showDebugMenuOnWall(boolean shouldMonitorTickDuration) {
        return shouldMonitorTickDuration || (SeedQueue.isOnWall() && SeedQueue.config.showDebugMenu);
    }

    @WrapWithCondition(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Thread;yield()V"
            )
    )
    private boolean doNotYieldRenderThreadOnWall() {
        // because of the increased amount of threads when using SeedQueue,
        // not yielding the render thread results in a much smoother experience on the Wall Screen
        return !SeedQueue.isOnWall();
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/Window;swapBuffers()V",
                    shift = At.Shift.AFTER
            )
    )
    private void finishRenderingWall(CallbackInfo ci) {
        if (this.currentScreen instanceof SeedQueueWallScreen) {
            SeedQueueWallScreen wall = (SeedQueueWallScreen) this.currentScreen;
            wall.joinScheduledInstance();
            wall.populateResetCooldowns();
            wall.tickBenchmark();
        }
    }

    @WrapWithCondition(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/MusicTracker;tick()V"
            )
    )
    private boolean doNotPlayMusicOnWall(MusicTracker musicTracker) {
        return !SeedQueue.isOnWall();
    }

    @WrapOperation(
            method = "reset",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/SoundManager;stopAll()V"
            )
    )
    private void keepSeedQueueSounds(SoundManager soundManager, Operation<Void> original) {
        if (SeedQueue.isActive()) {
            ((SQSoundManager) soundManager).seedQueue$stopAllExceptSeedQueueSounds();
            return;
        }
        original.call(soundManager);
    }

    @ModifyReturnValue(
            method = "isFabulousGraphicsOrBetter",
            at = @At("RETURN")
    )
    private static boolean doNotAllowFabulousGraphicsOnWall(boolean isFabulousGraphicsOrBetter) {
        return isFabulousGraphicsOrBetter && !SeedQueue.isOnWall();
    }

    @Inject(
            method = "stop",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;disconnect()V",
                    shift = At.Shift.AFTER
            )
    )
    private void shutdownQueue(CallbackInfo ci) {
        SeedQueue.stop();
    }

    @Inject(
            method = "printCrashReport",
            at = @At("HEAD")
    )
    private static void shutdownQueueOnCrash(CallbackInfo ci) {
        // don't try to stop SeedQueue if Minecraft crashes before the client is initialized
        // if Minecraft crashes in MinecraftClient#<init>, MinecraftClient#thread can be null
        if (MinecraftClient.getInstance() == null || !MinecraftClient.getInstance().isOnThread()) {
            SeedQueue.stop();
        }
    }
}
