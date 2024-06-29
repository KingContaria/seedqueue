package me.contaria.seedqueue.gui.wall;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
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

    protected SeedQueueWallScreen.LockTexture lock;

    protected int firstRenderFrame;
    protected int lastRenderFrame = Integer.MIN_VALUE;
    protected Long firstRenderTime;

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
            if (this.worldPreviewProperties.getSettingsCache() == null) {
                this.worldPreviewProperties.setSettingsCache(this.wall.settingsCache);
            }
        } else {
            this.worldRenderer = null;
        }
    }

    private void initScreen() {
        try {
            WorldPreview.inPreview = true;

            // forceUnicodeFont is not being loaded from the settings cache because it is not included in SeedQueueSettingsCache.PREVIEW_SETTINGS
            int scale = SeedQueue.config.calculateSimulatedScaleFactor(
                    this.worldPreviewProperties != null ? (int) this.worldPreviewProperties.getSettingsCache().getValue("guiScale") : MinecraftClient.getInstance().options.guiScale,
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
        } finally {
            WorldPreview.inPreview = false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        assert this.client != null;

        this.updateWorldPreviewProperties();

        if (this.worldPreviewProperties != null && this.shouldRender()) {
            this.runAsPreview(() -> super.render(matrices, mouseX, mouseY, delta));
            if (!this.hasBeenRendered()) {
                this.firstRenderFrame = this.wall.frame;
            }
        } else {
            this.build();
            // run as preview to set WorldPreview#renderingPreview
            // this ensures simulated window size is used in WindowMixin
            WorldPreview.runAsPreview(() -> {
                this.wall.setOrtho(this.width, this.height);
                super.render(matrices, mouseX, mouseY, delta);
            });
        }
    }

    public void build() {
        this.updateWorldPreviewProperties();
        if (this.worldPreviewProperties != null) {
            this.runAsPreview(() -> WorldPreview.runAsPreview(() -> {
                WorldPreview.tickPackets();
                WorldPreview.tickEntities();
                this.worldPreviewProperties.buildChunks();
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

    public boolean shouldRender() {
        if (this.worldPreviewProperties == null) {
            return false;
        }
        if (this.hasBeenRendered()) {
            return true;
        }
        if (((WorldRendererAccessor) this.worldRenderer).seedQueue$getCompletedChunkCount() == 0) {
            // this checks for instances that are ready to be loaded but do not have any chunks built, to avoid keeping them invisible forever we have to flush them through the system
            return this.seedQueueEntry.isPaused() && this.worldRenderer.isTerrainRenderComplete() && this.worldPreviewProperties.getPacketQueue().isEmpty();
        }
        return true;
    }

    public boolean hasBeenRendered() {
        return this.firstRenderTime != null;
    }

    public void updateLastRenderFrame() {
        this.lastRenderFrame = this.wall.frame;
    }

    public boolean shouldRenderPreview() {
        if (this.seedQueueEntry.isLocked() && SeedQueue.config.freezeLockedPreviews) {
            return false;
        }
        return this.wall.frame - this.lastRenderFrame >= SeedQueue.config.wallFPS / SeedQueue.config.previewFPS;
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
}
