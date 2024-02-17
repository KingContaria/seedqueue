package me.contaria.seedqueue.gui.wall;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.systems.RenderSystem;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.interfaces.SQWorldRenderer;
import me.contaria.seedqueue.mixin.accessor.WorldRendererAccessor;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.widget.AbstractPressableButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;

import java.util.Objects;

public class SeedQueueLevelLoadingScreen extends LevelLoadingScreen {

    private static final Identifier LOCK = new Identifier("textures/item/barrier.png");

    private final SeedQueueWallScreen wallScreen;
    private final SeedQueueEntry seedQueueEntry;
    private final WorldPreviewProperties worldPreviewProperties;
    private WorldRenderer worldRenderer;

    private int firstRenderFrame = Integer.MAX_VALUE;
    public int lastRenderFrame;

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
                this.client.getTextureManager().bindTexture(LOCK);
                drawTexture(matrices, 5, 5, 0.0F, 0.0F, 128, 128, 128, 128);
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

    @SuppressWarnings("deprecation")
    public void buildChunks() {
        assert this.client != null;

        WorldRenderer worldPreviewRenderer = WorldPreview.worldRenderer;

        WorldPreview.worldRenderer = this.getWorldRenderer();
        this.worldPreviewProperties.apply();
        WorldPreview.inPreview = true;

        try {
            WorldRenderer worldRenderer = this.client.worldRenderer;
            ClientPlayerEntity player = this.client.player;
            ClientWorld world = this.client.world;
            Entity cameraEntity = this.client.cameraEntity;
            ClientPlayerInteractionManager interactionManager = this.client.interactionManager;

            try {
                ((MinecraftClientAccessor) this.client).setWorldRenderer(WorldPreview.worldRenderer);
                this.client.player = WorldPreview.player;
                this.client.world = WorldPreview.world;
                this.client.cameraEntity = WorldPreview.player;
                this.client.interactionManager = WorldPreview.interactionManager;

                Window window = this.client.getWindow();
                RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                RenderSystem.loadIdentity();
                RenderSystem.ortho(0.0, window.getFramebufferWidth(), window.getFramebufferHeight(), 0.0, 1000.0, 3000.0);
                RenderSystem.loadIdentity();
                RenderSystem.translatef(0.0F, 0.0F, 0.0F);
                DiffuseLighting.disableGuiDepthLighting();

                int appliedPackets = 0;
                for (Packet<?> packet : ImmutableSet.copyOf(WorldPreview.packetQueue)) {
                    if (appliedPackets >= 50 && (packet instanceof ChunkDataS2CPacket || packet instanceof MobSpawnS2CPacket || packet instanceof EntitySpawnS2CPacket)) {
                        break;
                    }
                    //noinspection unchecked
                    ((Packet<ClientPlayPacketListener>) packet).apply(WorldPreview.player.networkHandler);
                    appliedPackets++;
                    WorldPreview.packetQueue.remove(packet);
                }

                MatrixStack matrices = new MatrixStack();
                Matrix4f projectionMatrix = new Matrix4f();
                this.client.gameRenderer.loadProjectionMatrix(projectionMatrix);
                WorldPreview.camera.update(WorldPreview.world, WorldPreview.player, this.client.options.perspective > 0, this.client.options.perspective == 2, 0);
                matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(WorldPreview.camera.getPitch()));
                matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(WorldPreview.camera.getYaw() + 180.0f));
                ((SQWorldRenderer) WorldPreview.worldRenderer).seedQueue$buildChunks(matrices, WorldPreview.camera, projectionMatrix);

                RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                RenderSystem.matrixMode(5889);
                RenderSystem.loadIdentity();
                RenderSystem.ortho(0.0, window.getFramebufferWidth() / window.getScaleFactor(), window.getFramebufferHeight() / window.getScaleFactor(), 0.0, 1000.0, 3000.0);
                RenderSystem.matrixMode(5888);
                RenderSystem.loadIdentity();
                RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
                DiffuseLighting.enableGuiDepthLighting();
                RenderSystem.defaultAlphaFunc();
                RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
            } finally {
                ((MinecraftClientAccessor) this.client).setWorldRenderer(worldRenderer);
                this.client.player = player;
                this.client.world = world;
                this.client.cameraEntity = cameraEntity;
                this.client.interactionManager = interactionManager;
            }
        } finally {
            WorldPreview.worldRenderer = worldPreviewRenderer;
            WorldPreview.clear();
            WorldPreview.inPreview = false;
        }
    }

    public void printDebug() {
        WorldRenderer worldRenderer = this.getWorldRenderer();
        SeedQueue.LOGGER.info("SeedQueue-DEBUG | " + "Instance: " + this.seedQueueEntry.getSession().getDirectoryName() + ", Seed: " + this.seedQueueEntry.getServer().getOverworld().getSeed() + ", Chunks: " + worldRenderer.getChunksDebugString() + " (" + worldRenderer.isTerrainRenderComplete() + ")");
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
            this.worldRenderer = SeedQueueWallScreen.getWorldRenderer(this.worldPreviewProperties.getWorld());
        }
        return this.worldRenderer;
    }
}
