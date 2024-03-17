package me.contaria.seedqueue.gui.wall;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.mixin.accessor.WorldRendererAccessor;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.widget.AbstractPressableButtonWidget;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.Objects;

public class SeedQueueLevelLoadingScreen extends LevelLoadingScreen {

    private static final Identifier LOCK = new Identifier("textures/item/barrier.png");

    private final SeedQueueWallScreen wallScreen;
    private final SeedQueueEntry seedQueueEntry;
    private final WorldPreviewProperties worldPreviewProperties;
    private WorldRenderer worldRenderer;

    private int firstRenderFrame = Integer.MAX_VALUE;
    public int lastRenderFrame;

    private boolean renderedLock;

    public SeedQueueLevelLoadingScreen(SeedQueueWallScreen wallScreen, SeedQueueEntry seedQueueEntry) {
        super(seedQueueEntry.getWorldGenerationProgressTracker());
        this.wallScreen = wallScreen;
        this.seedQueueEntry = seedQueueEntry;
        this.worldPreviewProperties = Objects.requireNonNull(seedQueueEntry.getWorldPreviewProperties());
        this.init(MinecraftClient.getInstance(), wallScreen.width, wallScreen.height);
        ((AbstractPressableButtonWidget) this.children.get(0)).onPress();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        assert this.client != null;

        WorldRenderer worldPreviewRenderer = WorldPreview.worldRenderer;

        WorldPreview.worldRenderer = this.getWorldRenderer();
        this.worldPreviewProperties.apply();
        WorldPreview.inPreview = true;


        try {
            // related to WorldRendererMixin#doNotClearOnWallScreen
            // the suppressed call usually renders a light blue overlay over the entire screen,
            // instead we draw it onto the preview ourselves
            DrawableHelper.fill(matrices, 0, 0, this.width, this.height, -5323025);

            super.render(matrices, mouseX, mouseY, delta);

            if (this.seedQueueEntry.isLocked()) {
                this.renderLock(matrices);
            }
        } finally {
            WorldPreview.worldRenderer = worldPreviewRenderer;
            WorldPreview.clear();
            WorldPreview.inPreview = false;
        }

        if (!this.hasBeenRendered()) {
            this.firstRenderFrame = this.wallScreen.frame;
        }
        this.lastRenderFrame = this.wallScreen.frame;
    }

    public void renderLock(MatrixStack matrices) {
        assert this.client != null;
        this.client.getTextureManager().bindTexture(LOCK);
        drawTexture(matrices, 5, 5, 0.0F, 0.0F, 128, 128, 128, 128);
        this.renderedLock = true;
    }

    public boolean hasRenderedLock() {
        return this.renderedLock;
    }

    public void renderChunkMap(MatrixStack matrices) {
        if (this.seedQueueEntry.isReady()) {
            return;
        }
        int i = 45;
        int j = this.height - 75;
        WorldGenerationProgressTracker progressProvider = Objects.requireNonNull(this.seedQueueEntry.getWorldGenerationProgressTracker());
        LevelLoadingScreen.drawChunkMap(matrices, progressProvider, i, j + 30, 2, 0);
        this.drawCenteredString(matrices, this.textRenderer, MathHelper.clamp(progressProvider.getProgressPercentage(), 0, 100) + "%", i, j - this.textRenderer.fontHeight / 2 - 30, 0xFFFFFF);
    }

    public void buildChunks() {
        assert this.client != null;

        WorldRenderer worldPreviewRenderer = WorldPreview.worldRenderer;

        WorldPreview.worldRenderer = this.getWorldRenderer();
        this.worldPreviewProperties.apply();
        WorldPreview.inPreview = true;

        try {
            WorldPreview.runAsPreview(() -> {
                WorldPreview.tickPackets();
                WorldPreview.tickEntities();
                this.worldPreviewProperties.buildChunks();
            });
        } finally {
            WorldPreview.worldRenderer = worldPreviewRenderer;
            WorldPreview.clear();
            WorldPreview.inPreview = false;
        }
    }

    public void printDebug() {
        WorldRenderer worldRenderer = this.getWorldRenderer();
        SeedQueue.LOGGER.info("SeedQueue-DEBUG | " + "Instance: " + this.seedQueueEntry.getSession().getDirectoryName() + ", Seed: " + this.seedQueueEntry.getServer().getOverworld().getSeed() + ", Chunks: " + worldRenderer.getChunksDebugString() + " (" + worldRenderer.isTerrainRenderComplete() + "), locked: " + this.seedQueueEntry.isLocked() + ", paused: " + this.seedQueueEntry.isPaused() + ", ready: " + this.seedQueueEntry.isReady());
    }

    public void printStacktrace() {
        SeedQueue.LOGGER.info("SeedQueue-DEBUG | " + "Instance: " + this.seedQueueEntry.getSession().getDirectoryName() + ", Stacktrace: " + Arrays.toString(this.seedQueueEntry.getServer().getThread().getStackTrace()));
    }

    public boolean shouldRender() {
        if (SeedQueue.config.doNotWaitForChunksToBuild) {
            return true;
        }
        WorldRenderer worldRenderer = this.getWorldRenderer();
        if (((WorldRendererAccessor) worldRenderer).seedQueue$getCompletedChunkCount() == 0) {
            // this checks for instances that are ready to be loaded but do not have any chunks built, to avoid keeping them invisible forever we have to flush them through the system
            return this.seedQueueEntry.isPaused() && worldRenderer.isTerrainRenderComplete() && this.worldPreviewProperties.getPacketQueue().isEmpty();
        }
        return true;
    }

    public boolean hasBeenRendered() {
        return this.firstRenderFrame < this.wallScreen.frame;
    }

    public SeedQueueEntry getSeedQueueEntry() {
        return this.seedQueueEntry;
    }

    public WorldPreviewProperties getWorldPreviewProperties() {
        return this.worldPreviewProperties;
    }

    public WorldRenderer getWorldRenderer() {
        if (this.worldRenderer == null) {
            this.worldRenderer = SeedQueueWallScreen.getOrCreateWorldRenderer(this.worldPreviewProperties.getWorld());
        }
        return this.worldRenderer;
    }
}
