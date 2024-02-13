package me.contaria.seedqueue;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.gui.wall.SeedQueueWallScreen;
import me.contaria.seedqueue.mixin.accessor.MinecraftServerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.UserCache;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class SeedQueueEntry {

    private final MinecraftServer server;

    private final LevelStorage.Session session;
    private final MinecraftClient.IntegratedResourceManager resourceManager;
    private final YggdrasilAuthenticationService yggdrasilAuthenticationService;
    private final MinecraftSessionService minecraftSessionService;
    private final GameProfileRepository gameProfileRepository;
    @Nullable
    private final UserCache userCache;

    @Nullable
    private WorldGenerationProgressTracker worldGenerationProgressTracker;
    @Nullable
    private WorldPreviewProperties worldPreviewProperties;

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

    public synchronized @Nullable WorldGenerationProgressTracker getWorldGenerationProgressTracker() {
        return this.worldGenerationProgressTracker;
    }

    public synchronized void setWorldGenerationProgressTracker(@Nullable WorldGenerationProgressTracker worldGenerationProgressTracker) {
        this.worldGenerationProgressTracker = worldGenerationProgressTracker;
    }

    public synchronized @Nullable WorldPreviewProperties getWorldPreviewProperties() {
        return this.worldPreviewProperties;
    }

    public synchronized void setWorldPreviewProperties(@Nullable WorldPreviewProperties worldPreviewProperties) {
        this.worldPreviewProperties = worldPreviewProperties;
    }

    public boolean isReady() {
        return this.server.isLoading();
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void lock() {
        this.locked = true;
    }

    public boolean isDiscarded() {
        return this.discarded;
    }

    public void discard() {
        synchronized (this.server) {
            if (this.discarded) {
                return;
            }

            SeedQueue.LOGGER.info("Discarding \"{}\"...", this.server.getSaveProperties().getLevelName());

            ModCompat.fastReset$fastReset(this.server);
            if (!ModCompat.worldpreview$kill(this.server)) {
                ((MinecraftServerAccessor) this.server).seedQueue$setRunning(false);
            }
            this.server.notify();
            WorldPreviewProperties worldPreviewProperties = this.getWorldPreviewProperties();
            if (worldPreviewProperties != null && !(SeedQueue.config.lazilyClearWorldRenderers && SeedQueue.isActive())) {
                SeedQueueWallScreen.clearWorldRenderer(worldPreviewProperties.getWorld());
            }

            this.discarded = true;
        }
    }
}
