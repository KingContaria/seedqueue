package me.contaria.seedqueue;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.compat.SeedQueueSettingsCache;
import me.contaria.seedqueue.compat.WorldPreviewFrameBuffer;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressTracker;
import me.contaria.seedqueue.mixin.accessor.MinecraftServerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.UserCache;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the {@link MinecraftServer} and any other resources related to a seed in the queue.
 */
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

    @Nullable
    private SeedQueueSettingsCache settingsCache;
    private int perspective;

    private volatile boolean locked;
    private volatile boolean loaded;
    private volatile boolean discarded;
    private volatile boolean maxWorldGenerationReached;

    /**
     * Stores the position (index) of the queue entry in the wall screen's main group.
     * A value of -1 indicates that this entry is not in the main group.
     */
    public int mainPosition = -1;

    public SeedQueueEntry(MinecraftServer server, LevelStorage.Session session, MinecraftClient.IntegratedResourceManager resourceManager, YggdrasilAuthenticationService yggdrasilAuthenticationService, MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository, @Nullable UserCache userCache) {
        this.server = server;
        this.session = session;
        this.resourceManager = resourceManager;
        this.yggdrasilAuthenticationService = yggdrasilAuthenticationService;
        this.minecraftSessionService = minecraftSessionService;
        this.gameProfileRepository = gameProfileRepository;
        this.userCache = userCache;

        ((SQMinecraftServer) server).seedQueue$setEntry(this);
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

    /**
     * @param create Whether the {@link WorldPreviewFrameBuffer} should be created if it's null.
     */
    public WorldPreviewFrameBuffer getFrameBuffer(boolean create) {
        if (!MinecraftClient.getInstance().isOnThread()) {
            throw new IllegalStateException("Tried to get WorldPreviewFrameBuffer off-thread!");
        }
        if (create && this.frameBuffer == null) {
            SeedQueueProfiler.push("create_framebuffer");
            this.frameBuffer = new WorldPreviewFrameBuffer(SeedQueue.config.simulatedWindowSize.width(), SeedQueue.config.simulatedWindowSize.height());
            SeedQueueProfiler.pop();
        }
        return this.frameBuffer;
    }

    public boolean hasFrameBuffer() {
        return this.frameBuffer != null;
    }

    /**
     * Deletes and removes this entry's framebuffer.
     *
     * @see WorldPreviewFrameBuffer#delete
     */
    public void discardFrameBuffer() {
        if (!MinecraftClient.getInstance().isOnThread()) {
            throw new RuntimeException("Tried to discard WorldPreviewFrameBuffer off-thread!");
        }
        if (this.frameBuffer != null) {
            this.frameBuffer.delete();
            this.frameBuffer = null;
        }
    }

    /**
     * @return True if this entry has either {@link WorldPreviewProperties} or a {@link WorldPreviewFrameBuffer}.
     */
    public boolean hasWorldPreview() {
        return this.worldPreviewProperties != null || this.frameBuffer != null;
    }

    public @Nullable SeedQueueSettingsCache getSettingsCache() {
        return this.settingsCache;
    }

    /**
     * Sets the settings cache to be loaded when loading this entry.
     *
     * @throws IllegalStateException If this method is called but {@link SeedQueueEntry#worldPreviewProperties} is null.
     */
    public void setSettingsCache(SeedQueueSettingsCache settingsCache) {
        if (this.worldPreviewProperties == null) {
            throw new IllegalStateException("Tried to set SettingsCache but WorldPreviewProperties is null!");
        }
        this.settingsCache = settingsCache;
        this.settingsCache.loadPlayerModelParts(this.worldPreviewProperties.getPlayer());
        this.perspective = this.worldPreviewProperties.getPerspective();
    }

    /**
     * Loads this entry's {@link SeedQueueEntry#settingsCache} and {@link SeedQueueEntry#perspective}.
     *
     * @return True if this entry has a settings cache which was loaded.
     */
    public boolean loadSettingsCache() {
        if (this.settingsCache != null) {
            this.settingsCache.load();
            MinecraftClient.getInstance().options.perspective = perspective;
            return true;
        }
        return false;
    }

    /**
     * @return The perspective used in the preview of this entry.
     */
    public int getPerspective() {
        return this.perspective;
    }

    /**
     * Checks if this entry should pause.
     * <p>
     * Returns true if:
     * <p>
     *  - the entry has finished world generation
     * <p>
     *  - the entry has reached the {@link SeedQueueConfig#maxWorldGenerationPercentage} and is not locked
     * <p>
     *  - the entry has been scheduled to pause by the {@link SeedQueueThread}
     *
     * @return If this entry's {@link MinecraftServer} should pause in its current state.
     *
     * @see SQMinecraftServer#seedQueue$shouldPause
     */
    public boolean shouldPause() {
        return ((SQMinecraftServer) this.server).seedQueue$shouldPause();
    }

    /**
     * @return If the entry is currently paused.
     *
     * @see SQMinecraftServer#seedQueue$isPaused
     * @see SeedQueueEntry#shouldPause
     */
    public boolean isPaused() {
        return ((SQMinecraftServer) this.server).seedQueue$isPaused();
    }

    /**
     * @return If the entry has been scheduled to pause by the {@link SeedQueueThread} but hasn't been paused yet.
     *
     * @see SQMinecraftServer#seedQueue$isScheduledToPause
     * @see SeedQueueEntry#shouldPause
     */
    public boolean isScheduledToPause() {
        return ((SQMinecraftServer) this.server).seedQueue$isScheduledToPause();
    }

    /**
     * Schedules this entry to be paused.
     *
     * @see SQMinecraftServer#seedQueue$schedulePause
     */
    public void schedulePause() {
        ((SQMinecraftServer) this.server).seedQueue$schedulePause();
    }

    /**
     * @return True if the entry is not currently paused or scheduled to pause.
     */
    public boolean canPause() {
        return !this.isScheduledToPause() && !this.isPaused();
    }

    /**
     * Unpauses this entry.
     *
     * @see SQMinecraftServer#seedQueue$unpause
     */
    public void unpause() {
        ((SQMinecraftServer) this.server).seedQueue$unpause();
    }

    /**
     * An entry can be unpaused if:
     * <p>
     * - it was paused by reaching the {@link SeedQueueConfig#maxWorldGenerationPercentage} but has been locked since
     * <p>
     * - it was scheduled to be paused by the {@link SeedQueueThread}
     *
     * @return True if this entry is currently paused or scheduled to be paused and is allowed to be unpaused.
     */
    public boolean canUnpause() {
        return this.isScheduledToPause() || (this.isPaused() && !this.shouldPause());
    }

    /**
     * @return True if the entry was paused and has now been successfully unpaused.
     *
     * @see SeedQueueEntry#unpause
     * @see SeedQueueEntry#canUnpause
     */
    public boolean tryToUnpause() {
        synchronized (this.server) {
            if (this.canUnpause()) {
                this.unpause();
                return true;
            }
            return false;
        }
    }

    /**
     * @return True if the {@link MinecraftServer} has fully finished generation and is ready to be joined by the player.
     */
    public boolean isReady() {
        return this.server.isLoading();
    }

    /**
     * @see SeedQueueEntry#lock
     */
    public boolean isLocked() {
        return this.locked;
    }

    /**
     * @return True if the {@link MinecraftServer} has not reached {@link SeedQueueConfig#maxWorldGenerationPercentage}.
     */
    public boolean isMaxWorldGenerationReached() {
        return this.maxWorldGenerationReached;
    }

    /**
     * Marks this entry as having reached {@link SeedQueueConfig#maxWorldGenerationPercentage}.
     */
    public void setMaxWorldGenerationReached() {
        this.maxWorldGenerationReached = true;
    }

    /**
     * Locks this entry from being mass-reset on the Wall Screen.
     * Mass Resets include Reset All, Focus Reset, Reset Row, Reset Column.
     *
     * @return True if the entry was not locked before.
     */
    public boolean lock() {
        if (!this.locked) {
            this.locked = true;
            SeedQueue.ping();
            return true;
        }
        return false;
    }

    /**
     * @see SeedQueueEntry#load
     */
    public boolean isLoaded() {
        return this.loaded;
    }

    /**
     * Marks this entry as loaded and discards its framebuffer.
     */
    public synchronized void load() {
        synchronized (this.server) {
            if (this.discarded) {
                throw new IllegalStateException("Tried to load \"" + this.session.getDirectoryName() + "\" but it has already been discarded!");
            }

            this.loaded = true;

            SeedQueueProfiler.push("discard_framebuffer");
            this.discardFrameBuffer();

            SeedQueueProfiler.swap("unpause");
            this.unpause();
            SeedQueueProfiler.pop();
        }
    }

    /**
     * @see SeedQueueEntry#discard
     */
    public boolean isDiscarded() {
        return this.discarded;
    }

    /**
     * Discards this entry and all the resources attached to it, including shutting down the {@link MinecraftServer}.
     */
    public synchronized void discard() {
        synchronized (this.server) {
            if (this.discarded) {
                SeedQueue.LOGGER.warn("Tried to discard \"{}\" but it has already been discarded!", this.session.getDirectoryName());
                return;
            }

            SeedQueue.LOGGER.info("Discarding \"{}\"...", this.session.getDirectoryName());

            this.discarded = true;

            SeedQueueProfiler.push("discard_framebuffer");
            this.discardFrameBuffer();

            SeedQueueProfiler.swap("stop_server");
            if (!ModCompat.worldpreview$kill(this.server)) {
                ModCompat.fastReset$fastReset(this.server);
                ((MinecraftServerAccessor) this.server).seedQueue$setRunning(false);
            }
            SeedQueueProfiler.swap("unpause");
            this.unpause();
            SeedQueueProfiler.pop();
        }
    }

    public int getProgressPercentage() {
        // doubtful this will happen, but the method is @Nullable
        WorldGenerationProgressTracker tracker = this.getWorldGenerationProgressTracker();
        if (tracker == null) {
            return 0;
        }

        return ((SQWorldGenerationProgressTracker) tracker).seedQueue$getProgressPercentage();
    }
}
