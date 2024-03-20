package me.contaria.seedqueue.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressListener;
import me.contaria.seedqueue.mixin.accessor.MinecraftServerAccessor;
import me.voidxwalker.autoreset.Atum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.net.Proxy;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

@Mixin(value = MinecraftClient.class, priority = 500)
public abstract class MinecraftClientMixin {

    @Shadow
    @Nullable
    private IntegratedServer server;
    @Shadow
    @Final
    private AtomicReference<WorldGenerationProgressTracker> worldGenProgressTracker;
    @Shadow
    @Final
    private Window window;

    @Shadow
    @Nullable
    public Screen currentScreen;

    @Inject(
            method = "createWorld",
            at = @At("TAIL")
    )
    private void startSeedQueue(CallbackInfo ci) {
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

    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference", "InvalidInjectorMethodSignature"}) // MCDev struggles with targeting constructors, this is correct
    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "NEW",
                    target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;",
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

    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference", "InvalidInjectorMethodSignature"}) // MCDev struggles with targeting constructors, this is correct
    @WrapOperation(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "NEW",
                    target = "Lnet/minecraft/util/UserCache;"
            )
    )
    private UserCache loadUserCache(GameProfileRepository profileRepository, File cacheFile, Operation<UserCache> original) {
        if (!SeedQueue.inQueue() && SeedQueue.currentEntry != null) {
            UserCache userCache = SeedQueue.currentEntry.getUserCache();
            if (userCache != null) {
                return userCache;
            }
        }
        if (SeedQueue.inQueue() && SeedQueue.config.lazyUserCache) {
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
            if (SeedQueue.currentEntry.getUserCache() == null) {
                ((MinecraftServerAccessor) SeedQueue.currentEntry.getServer()).seedQueue$setUserCache(userCache);
            }
            return SeedQueue.currentEntry.getServer();
        }
        return original.call(serverFactory);
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
            WorldGenerationProgressTracker tracker;
            do {
                tracker = SeedQueue.currentEntry.getWorldGenerationProgressTracker();
            } while (tracker == null);
            ((SQWorldGenerationProgressListener) tracker).seedQueue$unmute();
            this.worldGenProgressTracker.set(SeedQueue.currentEntry.getWorldGenerationProgressTracker());
        }
    }

    @Inject(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
                            ordinal = 0,
                            shift = At.Shift.AFTER,
                            remap = false
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Throwable;)V",
                            shift = At.Shift.AFTER,
                            remap = false
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/MinecraftClient;method_29601(Lnet/minecraft/client/MinecraftClient$WorldLoadAction;Ljava/lang/String;ZLjava/lang/Runnable;)V"
                    )
            }
    )
    private void throwSeedQueueException(CallbackInfo ci) throws SeedQueueException {
        if (SeedQueue.inQueue()) {
            throw new SeedQueueException();
        }
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
            SeedQueue.currentEntry.unpause();
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
    private void saveWorldGenerationProgressTracker(AtomicReference<?> instance, Object value, Operation<Void> original) {
        SeedQueueEntry seedQueueEntry;
        do {
            if (this.server != null && Thread.currentThread() == this.server.getThread()) {
                original.call(instance, value);
                return;
            }
            seedQueueEntry = SeedQueue.getEntry(Thread.currentThread());
        } while (seedQueueEntry == null);

        WorldGenerationProgressTracker tracker = (WorldGenerationProgressTracker) value;
        ((SQWorldGenerationProgressListener) tracker).seedQueue$mute();
        seedQueueEntry.setWorldGenerationProgressTracker(tracker);
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
                    target = "Lnet/minecraft/client/MinecraftClient;openScreen(Lnet/minecraft/client/gui/screen/Screen;)V"
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
        if (!SeedQueue.currentEntry.isReady()) {
            WorldPreviewProperties wpProperties = SeedQueue.currentEntry.getWorldPreviewProperties();
            if (wpProperties != null) {
                wpProperties.apply();
            }
            return true;
        }
        return false;
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

    @ModifyExpressionValue(
            method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/integrated/IntegratedServer;isStopping()Z"
            )
    )
    private boolean fastQuit(boolean value) {
        return value || (SeedQueue.isActive() && ModCompat.HAS_FASTRESET);
    }

    @Inject(
            method = "getFramerateLimit",
            at = @At("HEAD"),
            cancellable = true
    )
    private void unlimitedWallFPS(CallbackInfoReturnable<Integer> cir) {
        if (SeedQueue.isOnWall() && SeedQueue.config.unlimitedWallFPS) {
            cir.setReturnValue(this.window.getFramerateLimit());
        }
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/Window;swapBuffers()V",
                    shift = At.Shift.AFTER
            )
    )
    private void benchmarkResets(CallbackInfo ci) {
        if (this.currentScreen instanceof SeedQueueWallScreen) {
            ((SeedQueueWallScreen) this.currentScreen).tickBenchmark();
        }
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
        SeedQueue.stop();
    }
}
