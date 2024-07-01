package me.contaria.seedqueue;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.compat.WorldPreviewFrameBuffer;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import me.contaria.seedqueue.mixin.accessor.MinecraftServerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.UserCache;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;

public class SeedQueueEntry {
    private final MinecraftServer server;

    private final LevelStorage.Session session;
    private final MinecraftClient.IntegratedResourceManager resourceManager;
    private final YggdrasilAuthenticationService yggdrasilAuthenticationService;
    private final MinecraftSessionService minecraftSessionService;
    private final GameProfileRepository gameProfileRepository;
    @Nullable // UserCache will be null when using wall, see also MinecraftClientMixin#loadUserCache
    private final UserCache userCache;

    @Nullable
    private WorldGenerationProgressTracker worldGenerationProgressTracker;
    @Nullable
    private WorldPreviewProperties worldPreviewProperties;
    @Nullable
    private WorldPreviewFrameBuffer frameBuffer;

    private boolean locked;
    private boolean discarded;

    public SeedQueueEntry(MinecraftServer server, LevelStorage.Session session, MinecraftClient.IntegratedResourceManager resourceManager, YggdrasilAuthenticationService yggdrasilAuthenticationService, MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository, @Nullable UserCache userCache) {
        this.server = server;
        this.session = session;
        this.resourceManager = resourceManager;
        this.yggdrasilAuthenticationService = yggdrasilAuthenticationService;
        this.minecraftSessionService = minecraftSessionService;
        this.gameProfileRepository = gameProfileRepository;
        this.userCache = userCache;
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public LevelStorage.Session getSession() {
        return this.session;
    }

    public MinecraftClient.IntegratedResourceManager getResourceManager() {
        return this.resourceManager;
    }

    public YggdrasilAuthenticationService getYggdrasilAuthenticationService() {
        return this.yggdrasilAuthenticationService;
    }

    public MinecraftSessionService getMinecraftSessionService() {
        return this.minecraftSessionService;
    }

    public GameProfileRepository getGameProfileRepository() {
        return this.gameProfileRepository;
    }

    public @Nullable UserCache getUserCache() {
        return this.userCache;
    }

    public @Nullable WorldGenerationProgressTracker getWorldGenerationProgressTracker() {
        return this.worldGenerationProgressTracker;
    }

    public void setWorldGenerationProgressTracker(@Nullable WorldGenerationProgressTracker worldGenerationProgressTracker) {
        this.worldGenerationProgressTracker = worldGenerationProgressTracker;
    }

    public @Nullable WorldPreviewProperties getWorldPreviewProperties() {
        return this.worldPreviewProperties;
    }

    public synchronized void setWorldPreviewProperties(@Nullable WorldPreviewProperties worldPreviewProperties) {
        this.worldPreviewProperties = worldPreviewProperties;
    }

    public WorldPreviewFrameBuffer getFrameBuffer() {
        return this.getFrameBuffer(false);
    }

    public WorldPreviewFrameBuffer getFrameBuffer(boolean create) {
        if (!MinecraftClient.getInstance().isOnThread()) {
            throw new IllegalStateException("Tried to get WorldPreviewFrameBuffer off-thread!");
        }
        if (create && this.frameBuffer == null) {
            Profiler profiler = MinecraftClient.getInstance().getProfiler();
            profiler.push("create_framebuffer");
            this.frameBuffer = new WorldPreviewFrameBuffer(MinecraftClient.getInstance().getWindow().getFramebufferWidth(), MinecraftClient.getInstance().getWindow().getFramebufferHeight());
            profiler.pop();
        }
        return this.frameBuffer;
    }

    public void discardFrameBuffer() {
        if (!MinecraftClient.getInstance().isOnThread()) {
            throw new IllegalStateException("Tried to discard WorldPreviewFrameBuffer off-thread!");
        }
        if (this.frameBuffer != null) {
            this.frameBuffer.delete();
            this.frameBuffer = null;
        }
    }

    public boolean shouldPause() {
        return ((SQMinecraftServer) this.server).seedQueue$shouldPause();
    }

    public boolean isPaused() {
        return ((SQMinecraftServer) this.server).seedQueue$isPaused();
    }

    public boolean isScheduledToPause() {
        return ((SQMinecraftServer) this.server).seedQueue$isScheduledToPause();
    }

    public void schedulePause() {
        ((SQMinecraftServer) this.server).seedQueue$schedulePause();
    }

    public boolean canUnpause() {
        return this.isScheduledToPause() || (this.isPaused() && !this.shouldPause());
    }

    public void unpause() {
        ((SQMinecraftServer) this.server).seedQueue$unpause();
    }

    public void tryToUnpause() {
        // to avoid a race condition within SeedQueueThread#pauseSeedQueueEntry
        // where the server would be scheduled to pause but then pause because it finishes loading,
        // we synchronize on the server object and recheck pause state before trying to unpause
        synchronized (this.server) {
            if (this.isPaused() && this.shouldPause()) {
                return;
            }
            ((SQMinecraftServer) this.server).seedQueue$unpause();
        }
    }

    public boolean isReady() {
        return this.server.isLoading();
    }

    public boolean isLocked() {
        return this.locked;
    }

    public boolean lock() {
        if (!this.locked) {
            this.locked = true;
            SeedQueue.ping();
            return true;
        }
        return false;
    }

    public boolean isDiscarded() {
        return this.discarded;
    }

    public synchronized void discard() {
        synchronized (this.server) {
            if (this.discarded) {
                return;
            }

            Profiler profiler = MinecraftClient.getInstance().getProfiler();

            SeedQueue.LOGGER.info("Discarding \"{}\"...", this.server.getSaveProperties().getLevelName());

            this.discarded = true;

            profiler.push("discard_framebuffer");
            this.discardFrameBuffer();

            profiler.swap("stop_server");
            if (!ModCompat.worldpreview$kill(this.server)) {
                ModCompat.fastReset$fastReset(this.server);
                ((MinecraftServerAccessor) this.server).seedQueue$setRunning(false);
            }
            profiler.swap("unpause");
            this.unpause();
            profiler.pop();
        }
    }
}
