package me.contaria.seedqueue.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import me.contaria.seedqueue.interfaces.worldpreview.SQWorldRenderer;
import me.contaria.seedqueue.mixin.accessor.CameraAccessor;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.profiler.DummyProfiler;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;

public class WorldPreviewProperties {
    private final ClientWorld world;
    private final ClientPlayerEntity player;
    private final ClientPlayerInteractionManager interactionManager;
    private final Camera camera;
    private final Queue<Packet<?>> packetQueue;

    private SeedQueueSettingsCache settingsCache;

    @Nullable
    private WorldPreviewFrameBuffer frameBuffer;

    public WorldPreviewProperties(ClientWorld world, ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager, Camera camera, Queue<Packet<?>> packetQueue) {
        this.world = world;
        this.player = player;
        this.interactionManager = interactionManager;
        this.camera = camera;
        this.packetQueue = packetQueue;
    }

    public ClientWorld getWorld() {
        return this.world;
    }

    public ClientPlayerEntity getPlayer() {
        return this.player;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public Queue<Packet<?>> getPacketQueue() {
        return this.packetQueue;
    }

    public SeedQueueSettingsCache getSettingsCache() {
        return this.settingsCache;
    }

    public void setSettingsCache(SeedQueueSettingsCache settingsCache) {
        this.settingsCache = settingsCache;
        this.settingsCache.loadPlayerModelParts(this.player);
    }

    // perspective is set earlier than the settingsCache because it is important for chunk data culling
    // the same does not go for FOV because we simply use 110 (Quake Pro) for culling because most people will/should just use it on the wall screen anyway
    // see also WorldPreviewMixin#modifyPerspective_inQueue, ServerChunkManagerMixin#modifyCullingFov_inQueue
    public int getPerspective() {
        return this.camera.isThirdPerson() ? ((CameraAccessor) this.camera).seedQueue$isInverseView() ? 2 : 1 : 0;
    }

    public void apply() {
        WorldPreview.set(this.world, this.player, this.interactionManager, this.camera, this.packetQueue);
    }

    // see WorldPreview#render
    @SuppressWarnings("deprecation")
    public void buildChunks() {
        MinecraftClient client = MinecraftClient.getInstance();
        Profiler profiler = client.getProfiler();
        Window window = client.getWindow();

        profiler.swap("build_preview");

        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0, window.getFramebufferWidth(), window.getFramebufferHeight(), 0.0, 1000.0, 3000.0);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, 0.0F);
        DiffuseLighting.disableGuiDepthLighting();

        profiler.push("matrix");
        // see GameRenderer#renderWorld
        MatrixStack rotationMatrix = new MatrixStack();
        rotationMatrix.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(this.camera.getPitch()));
        rotationMatrix.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(this.camera.getYaw() + 180.0f));

        Matrix4f projectionMatrix = new Matrix4f();
        client.gameRenderer.loadProjectionMatrix(projectionMatrix);

        profiler.swap("camera_update");
        synchronized (this.camera) {
            this.camera.update(this.world, this.player, this.camera.isThirdPerson(), ((CameraAccessor) this.camera).seedQueue$isInverseView(), 0);
        }
        profiler.swap("build_chunks");
        ((SQWorldRenderer) WorldPreview.worldRenderer).seedQueue$buildChunks(rotationMatrix, this.camera, projectionMatrix);
        profiler.pop();

        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, (double) window.getFramebufferWidth() / window.getScaleFactor(), (double) window.getFramebufferHeight() / window.getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
    }

    public WorldPreviewFrameBuffer getOrCreateFrameBuffer() {
        if (this.frameBuffer == null) {
            Profiler profiler = MinecraftClient.getInstance().getProfiler();
            profiler.push("create_framebuffer");
            this.frameBuffer = new WorldPreviewFrameBuffer(MinecraftClient.getInstance().getWindow().getFramebufferWidth(), MinecraftClient.getInstance().getWindow().getFramebufferHeight());
            profiler.pop();
        }
        return this.frameBuffer;
    }

    public synchronized void discard() {
        Profiler profiler = MinecraftClient.getInstance().isOnThread() ? MinecraftClient.getInstance().getProfiler() : DummyProfiler.INSTANCE;

        profiler.push("clear_packet_queue");
        this.packetQueue.clear();
        profiler.swap("delete_framebuffer");
        if (this.frameBuffer != null) {
            if (!MinecraftClient.getInstance().isOnThread()) {
                // WorldPreviewProperties#discard should only be called off-thread from MinecraftServerMixin#discardWorldPreviewPropertiesOnLoad
                // which only triggers when not using wall, meaning no framebuffer will be created
                throw new IllegalStateException("Tried to discard WorldPreviewProperties with a framebuffer off-thread!");
            }
            this.frameBuffer.delete();
            this.frameBuffer = null;
        }
        profiler.pop();
    }
}
