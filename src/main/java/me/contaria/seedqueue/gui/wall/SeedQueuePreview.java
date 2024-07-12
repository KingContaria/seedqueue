package me.contaria.seedqueue.gui.wall;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.WorldPreviewCompat;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.mixin.accessor.WorldRendererAccessor;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;
import org.mcsr.speedrunapi.config.SpeedrunConfigAPI;

import java.util.Arrays;
import java.util.Objects;

public class SeedQueuePreview extends LevelLoadingScreen {
    public final SeedQueueWallScreen wall;
    private final SeedQueueEntry seedQueueEntry;
    @Nullable
    private WorldPreviewProperties worldPreviewProperties;
    private WorldRenderer worldRenderer;

    private SeedQueueWallScreen.LockTexture lockTexture;

    private boolean previewRendered;
    private int lastPreviewFrame;
    private long cooldownStart = Long.MAX_VALUE;

    public SeedQueuePreview(SeedQueueWallScreen wall, SeedQueueEntry seedQueueEntry) {
        super(seedQueueEntry.getWorldGenerationProgressTracker());
        this.wall = wall;
        this.seedQueueEntry = seedQueueEntry;

        this.updateWorldPreviewProperties();
        this.initScreen();
    }

    private void updateWorldPreviewProperties() {
        if (this.worldPreviewProperties == (this.worldPreviewProperties = this.seedQueueEntry.getWorldPreviewProperties())) {
            return;
        }
        if (this.worldPreviewProperties != null) {
            this.worldRenderer = SeedQueueWallScreen.getOrCreateWorldRenderer(this.worldPreviewProperties.getWorld());
            if (this.seedQueueEntry.getSettingsCache() == null) {
                this.seedQueueEntry.setSettingsCache(this.wall.settingsCache);
            }
        } else {
            this.worldRenderer = null;
        }
    }

    private void initScreen() {
        // forceUnicodeFont is not being loaded from the settings cache because it is not included in SeedQueueSettingsCache.PREVIEW_SETTINGS
        int scale = SeedQueue.config.calculateSimulatedScaleFactor(
                this.seedQueueEntry.getSettingsCache() != null ? (int) this.seedQueueEntry.getSettingsCache().getValue("guiScale") : MinecraftClient.getInstance().options.guiScale,
                MinecraftClient.getInstance().options.forceUnicodeFont
        );
        this.init(
                MinecraftClient.getInstance(),
                SeedQueue.config.simulatedWindowSize.width() / scale,
                SeedQueue.config.simulatedWindowSize.height() / scale
        );

        if (Boolean.TRUE.equals(SpeedrunConfigAPI.getConfigValue("standardsettings", "autoF3Esc"))) {
            Text backToGame = new TranslatableText("menu.returnToGame");
            for (Element e : this.children()) {
                if (!(e instanceof ButtonWidget)) {
                    continue;
                }
                ButtonWidget button = (ButtonWidget) e;
                if (backToGame.equals(button.getMessage())) {
                    button.onPress();
                    break;
                }
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        assert this.client != null;

        this.updateWorldPreviewProperties();

        if (this.worldPreviewProperties != null && this.isPreviewReady()) {
            this.runAsPreview(() -> super.render(matrices, mouseX, mouseY, delta));
        } else {
            this.build();
            // run as preview to set WorldPreview#renderingPreview
            // this ensures simulated window size is used in WindowMixin
            WorldPreview.runAsPreview(() -> {
                WorldPreview.inPreview = this.seedQueueEntry.hasFrameBuffer();
                try {
                    this.wall.setOrtho(this.width, this.height);
                    super.render(matrices, mouseX, mouseY, delta);
                } finally {
                    WorldPreview.inPreview = false;
                }
            });
        }
    }

    public void build() {
        this.updateWorldPreviewProperties();
        if (this.worldPreviewProperties != null) {
            this.runAsPreview(() -> WorldPreview.runAsPreview(() -> {
                WorldPreview.tickPackets();
                WorldPreview.tickEntities();
                WorldPreviewCompat.buildChunks();
            }));
        }
    }

    private void runAsPreview(Runnable runnable) {
        if (this.worldPreviewProperties == null) {
            throw new IllegalStateException("Tried to run as preview but preview is null!");
        }

        WorldRenderer worldPreviewRenderer = WorldPreview.worldRenderer;
        WorldPreview.worldRenderer = this.worldRenderer;
        this.worldPreviewProperties.apply();
        WorldPreview.inPreview = true;

        try {
            runnable.run();
        } finally {
            WorldPreview.worldRenderer = worldPreviewRenderer;
            WorldPreview.clear();
            WorldPreview.inPreview = false;
        }
    }

    public void printDebug() {
        if (this.worldRenderer != null) {
            SeedQueue.LOGGER.info("SeedQueue-DEBUG | Instance: {}, Seed: {}, World Gen %: {}, Chunks: {} ({}), locked: {}, paused: {}, ready: {}", this.seedQueueEntry.getSession().getDirectoryName(), this.seedQueueEntry.getServer().getSaveProperties().getGeneratorOptions().getSeed(), Objects.requireNonNull(this.seedQueueEntry.getWorldGenerationProgressTracker()).getProgressPercentage(), this.worldRenderer.getChunksDebugString(), this.worldRenderer.isTerrainRenderComplete(), this.seedQueueEntry.isLocked(), this.seedQueueEntry.isPaused(), this.seedQueueEntry.isReady());
        } else {
            SeedQueue.LOGGER.info("SeedQueue-DEBUG | Instance: {}, Seed: {}, World Gen %: {}", this.seedQueueEntry.getSession().getDirectoryName(), this.seedQueueEntry.getServer().getSaveProperties().getGeneratorOptions().getSeed(), Objects.requireNonNull(this.seedQueueEntry.getWorldGenerationProgressTracker()).getProgressPercentage());
        }
    }

    public void printStacktrace() {
        SeedQueue.LOGGER.info("SeedQueue-DEBUG | Instance: {}, Stacktrace: {}", this.seedQueueEntry.getSession().getDirectoryName(), Arrays.toString(this.seedQueueEntry.getServer().getThread().getStackTrace()));
    }

    public boolean isPreviewReady() {
        if (this.previewRendered || this.seedQueueEntry.hasFrameBuffer()) {
            return true;
        }
        if (this.worldPreviewProperties == null || this.worldRenderer == null) {
            return false;
        }
        if (((WorldRendererAccessor) this.worldRenderer).seedQueue$getCompletedChunkCount() > 0) {
            return true;
        }
        // this checks for instances that are ready to be loaded but do not have any chunks built, to avoid keeping them invisible forever we have to flush them through the system
        // this should not happen anymore but to avoid disaster I will leave it in with a log message
        if (this.seedQueueEntry.isPaused() && this.worldRenderer.isTerrainRenderComplete() && this.worldPreviewProperties.getPacketQueue().isEmpty()) {
            SeedQueue.LOGGER.warn("\"{}\" failed to build any chunks on the wall screen!", this.seedQueueEntry.getSession().getDirectoryName());
            return true;
        }
        return false;
    }

    public boolean hasPreviewRendered() {
        return this.previewRendered;
    }

    public void onPreviewRender(boolean redrawn) {
        this.previewRendered = true;
        if (redrawn) {
            this.lastPreviewFrame = this.wall.frame;
        }
    }

    public boolean shouldRedrawPreview() {
        return this.worldPreviewProperties != null && (this.lastPreviewFrame == 0 || this.wall.frame - this.lastPreviewFrame >= SeedQueue.config.wallFPS / SeedQueue.config.previewFPS);
    }

    protected void populateCooldownStart(long cooldownStart) {
        if (this.previewRendered && this.cooldownStart == Long.MAX_VALUE) {
            this.cooldownStart = cooldownStart;
        }
    }

    public boolean isCooldownReady() {
        return System.currentTimeMillis() - this.cooldownStart >= SeedQueue.config.resetCooldown;
    }

    public boolean canReset(boolean ignoreLock, boolean ignoreResetCooldown) {
        return this.hasPreviewRendered() && (!this.seedQueueEntry.isLocked() || ignoreLock) && (this.isCooldownReady() || ignoreResetCooldown) && SeedQueue.selectedEntry != this.seedQueueEntry;
    }

    public SeedQueueEntry getSeedQueueEntry() {
        return this.seedQueueEntry;
    }

    public @Nullable WorldPreviewProperties getWorldPreviewProperties() {
        return this.worldPreviewProperties;
    }

    public WorldRenderer getWorldRenderer() {
        return this.worldRenderer;
    }

    public SeedQueueWallScreen.LockTexture getLockTexture() {
        if (this.lockTexture == null) {
            this.lockTexture = this.wall.getLockTexture();
        }
        return this.lockTexture;
    }
}
