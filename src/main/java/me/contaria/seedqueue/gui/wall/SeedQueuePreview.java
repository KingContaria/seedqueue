package me.contaria.seedqueue.gui.wall;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.SeedQueuePreviewFrameBuffer;
import me.contaria.seedqueue.compat.SeedQueuePreviewProperties;
import me.contaria.seedqueue.customization.LockTexture;
import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressTracker;
import me.contaria.seedqueue.mixin.accessor.WorldRendererAccessor;
import me.contaria.speedrunapi.config.SpeedrunConfigAPI;
import me.voidxwalker.autoreset.Atum;
import me.voidxwalker.autoreset.interfaces.ISeedStringHolder;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.WorldPreviewProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class SeedQueuePreview extends DrawableHelper {
    private final SeedQueueWallScreen wall;
    private final SeedQueueEntry seedQueueEntry;
    private final WorldGenerationProgressTracker tracker;
    private SeedQueuePreviewProperties previewProperties;
    private WorldRenderer worldRenderer;

    private final MinecraftClient client;

    private final int width;
    private final int height;

    private final List<ButtonWidget> buttons;
    private final boolean showMenu;
    private final String seedString;
    private final LockTexture lockTexture;

    private long cooldownStart;
    private boolean previewRendered;
    private int lastPreviewFrame;

    public SeedQueuePreview(SeedQueueWallScreen wall, SeedQueueEntry seedQueueEntry) {
        this.wall = wall;
        this.seedQueueEntry = seedQueueEntry;
        this.tracker = Objects.requireNonNull(seedQueueEntry.getWorldGenerationProgressTracker());

        this.client = MinecraftClient.getInstance();

        // forceUnicodeFont is not being loaded from the settings cache because it is not included in SeedQueueSettingsCache.PREVIEW_SETTINGS
        int scale = SeedQueue.config.calculateSimulatedScaleFactor(
                this.seedQueueEntry.getSettingsCache() != null ? (int) this.seedQueueEntry.getSettingsCache().getValue("guiScale") : MinecraftClient.getInstance().options.guiScale,
                MinecraftClient.getInstance().options.forceUnicodeFont
        );
        this.width = SeedQueue.config.simulatedWindowSize.width() / scale;
        this.height = SeedQueue.config.simulatedWindowSize.height() / scale;

        this.buttons = WorldPreviewProperties.createMenu(this.width, this.height, () -> {}, () -> {});
        this.showMenu = !Boolean.TRUE.equals(SpeedrunConfigAPI.getConfigValue("standardsettings", "autoF3Esc"));

        if (Atum.inDemoMode()) {
            this.seedString = "North Carolina";
        } else if (Atum.getSeedProvider().shouldShowSeed()) {
            this.seedString = ((ISeedStringHolder) this.seedQueueEntry.getServer().getSaveProperties().getGeneratorOptions()).atum$getSeedString();
        } else {
            this.seedString = "Set Seed";
        }

        this.lockTexture = wall.getRandomLockTexture();

        this.updatePreviewProperties();
    }

    private void updatePreviewProperties() {
        if (this.previewProperties == (this.previewProperties = this.seedQueueEntry.getPreviewProperties())) {
            return;
        }
        if (this.previewProperties != null) {
            this.worldRenderer = SeedQueueWallScreen.getOrCreateWorldRenderer(this.previewProperties.world);
            if (this.seedQueueEntry.getSettingsCache() == null) {
                this.seedQueueEntry.setSettingsCache(this.wall.settingsCache);
            }
        } else {
            this.worldRenderer = null;
        }
    }

    public void render(MatrixStack matrices) {
        this.updatePreviewProperties();

        if (!this.isPreviewReady()) {
            if (this.previewProperties != null) {
                this.run(p -> this.buildChunks());
            }
            this.wall.renderBackground(matrices);
        } else {
            this.renderPreview(matrices);
        }

        if (!this.seedQueueEntry.isReady()) {
            this.renderLoading(matrices);
        } else if (SeedQueue.config.chunkMapFreezing != -1 && !this.seedQueueEntry.isLocked()) {
            this.renderChunkmap(matrices);
        }
    }

    private void renderPreview(MatrixStack matrices) {
        SeedQueuePreviewFrameBuffer frameBuffer = this.seedQueueEntry.getFrameBuffer();
        if (this.previewProperties != null) {
            if (this.shouldRedrawPreview() && frameBuffer.updateRenderData(this.worldRenderer.getChunksDebugString() + "\n" + this.worldRenderer.getEntitiesDebugString())) {
                this.redrawPreview(matrices, frameBuffer);
            } else {
                this.run(p -> this.buildChunks());
            }
        }
        frameBuffer.draw(this.width, this.height);
        this.previewRendered = true;
    }

    private void redrawPreview(MatrixStack matrices, SeedQueuePreviewFrameBuffer frameBuffer) {
        frameBuffer.beginWrite();
        // related to WorldRendererMixin#doNotClearOnWallScreen
        // the suppressed call usually renders a light blue overlay over the entire screen,
        // instead we draw it onto the preview ourselves
        DrawableHelper.fill(matrices, 0, 0, this.wall.width, this.wall.height, -5323025);
        this.run(properties -> properties.render(matrices, 0, 0, 0.0f, this.buttons, this.width, this.height, this.showMenu));
        frameBuffer.endWrite();

        this.client.getFramebuffer().beginWrite(true);
        this.wall.refreshViewport();
        this.lastPreviewFrame = this.wall.frame;
    }

    private void renderLoading(MatrixStack matrices) {
        // see LevelLoadingScreen#render
        this.renderChunkmap(matrices);
        this.drawCenteredString(matrices, this.client.textRenderer, MathHelper.clamp(this.tracker.getProgressPercentage(), 0, 100) + "%", 45, this.height - 75 - 9 / 2 - 30, 16777215);
        this.drawCenteredString(matrices, this.client.textRenderer, this.seedString, 45, this.height - 75 - 9 / 2 - 50, 16777215);
    }

    private void renderChunkmap(MatrixStack matrices) {
        WorldGenerationProgressTracker tracker = this.seedQueueEntry.isLocked() ? this.tracker : ((SQWorldGenerationProgressTracker) this.tracker).seedQueue$getFrozenCopy().orElse(this.tracker);
        LevelLoadingScreen.drawChunkMap(matrices, tracker, 45, this.height - 75 + 30, 2, 0);
    }

    public void build() {
        this.updatePreviewProperties();
        if (this.previewProperties != null) {
            this.run(p -> this.buildChunks());
        }
    }

    private void buildChunks() {
        this.previewProperties.tickPackets();
        this.previewProperties.tickEntities();
        this.previewProperties.buildChunks();
    }

    private void run(Consumer<WorldPreviewProperties> consumer) {
        WorldRenderer worldRenderer = WorldPreview.worldRenderer;
        WorldPreviewProperties properties = WorldPreview.properties;
        try {
            WorldPreview.worldRenderer = this.worldRenderer;
            WorldPreview.properties = this.previewProperties;
            WorldPreview.properties.run(consumer);
        } finally {
            WorldPreview.worldRenderer = worldRenderer;
            WorldPreview.properties = properties;
        }
    }

    private boolean shouldRedrawPreview() {
        return this.lastPreviewFrame == 0 || this.wall.frame - this.lastPreviewFrame >= SeedQueue.config.wallFPS / SeedQueue.config.previewFPS;
    }

    public boolean isPreviewReady() {
        return this.seedQueueEntry.hasFrameBuffer() || (this.worldRenderer != null && ((WorldRendererAccessor) this.worldRenderer).seedQueue$getCompletedChunkCount() > 0);
    }

    public boolean hasPreviewRendered() {
        return this.previewRendered;
    }

    public boolean canReset(boolean ignoreLock, boolean ignoreResetCooldown) {
        return this.previewRendered && (!this.seedQueueEntry.isLocked() || ignoreLock) && (this.isCooldownReady() || ignoreResetCooldown) && SeedQueue.selectedEntry != this.seedQueueEntry;
    }

    protected void resetCooldown() {
        this.cooldownStart = Long.MAX_VALUE;
    }

    protected void populateCooldownStart(long cooldownStart) {
        if (this.previewRendered && this.cooldownStart == Long.MAX_VALUE) {
            this.cooldownStart = cooldownStart;
        }
    }

    private boolean isCooldownReady() {
        return System.currentTimeMillis() - this.cooldownStart >= SeedQueue.config.resetCooldown;
    }

    public void printDebug() {
        if (this.worldRenderer != null) {
            SeedQueue.LOGGER.info("SeedQueue-DEBUG | Instance: {}, Seed: {}, World Gen %: {}, Chunks: {} ({}), locked: {}, paused: {}, ready: {}", this.seedQueueEntry.getSession().getDirectoryName(), this.seedQueueEntry.getServer().getSaveProperties().getGeneratorOptions().getSeed(), this.seedQueueEntry.getProgressPercentage(), this.worldRenderer.getChunksDebugString(), this.worldRenderer.isTerrainRenderComplete(), this.seedQueueEntry.isLocked(), this.seedQueueEntry.isPaused(), this.seedQueueEntry.isReady());
        } else {
            SeedQueue.LOGGER.info("SeedQueue-DEBUG | Instance: {}, Seed: {}, World Gen %: {}", this.seedQueueEntry.getSession().getDirectoryName(), this.seedQueueEntry.getServer().getSaveProperties().getGeneratorOptions().getSeed(), this.seedQueueEntry.getProgressPercentage());
        }
    }

    public void printStacktrace() {
        SeedQueue.LOGGER.info("SeedQueue-DEBUG | Instance: {}, Stacktrace: {}", this.seedQueueEntry.getSession().getDirectoryName(), Arrays.toString(this.seedQueueEntry.getServer().getThread().getStackTrace()));
    }

    public SeedQueueEntry getSeedQueueEntry() {
        return this.seedQueueEntry;
    }

    public LockTexture getLockTexture() {
        return this.lockTexture;
    }
}
