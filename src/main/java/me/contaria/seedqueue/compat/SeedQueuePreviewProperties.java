package me.contaria.seedqueue.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import me.contaria.seedqueue.debug.SeedQueueProfiler;
import me.contaria.seedqueue.interfaces.worldpreview.SQWorldRenderer;
import me.contaria.seedqueue.mixin.accessor.CameraAccessor;
import me.contaria.seedqueue.mixin.accessor.PlayerEntityAccessor;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.WorldPreviewProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.util.math.Matrix4f;

import java.util.Queue;

public class SeedQueuePreviewProperties extends WorldPreviewProperties {
    public SeedQueuePreviewProperties(ClientWorld world, ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager, Camera camera, Queue<Packet<?>> packetQueue) {
        super(world, player, interactionManager, camera, packetQueue);
    }

    public int getPerspective() {
        return this.camera.isThirdPerson() ? ((CameraAccessor) this.camera).seedQueue$isInverseView() ? 2 : 1 : 0;
    }

    /**
     * Builds chunks for the current {@link WorldPreview#worldRenderer} without rendering the preview.
     *
     * @see WorldPreviewProperties#render
     */
    @SuppressWarnings("deprecation")
    public void buildChunks() {
        MinecraftClient client = MinecraftClient.getInstance();
        Window window = client.getWindow();
        Camera camera = client.gameRenderer.getCamera();

        SeedQueueProfiler.swap("build_preview");

        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0, window.getFramebufferWidth(), window.getFramebufferHeight(), 0.0, 1000.0, 3000.0);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, 0.0F);
        DiffuseLighting.disableGuiDepthLighting();

        SeedQueueProfiler.push("matrix");
        // see GameRenderer#renderWorld
        MatrixStack rotationMatrix = new MatrixStack();
        rotationMatrix.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(camera.getPitch()));
        rotationMatrix.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(camera.getYaw() + 180.0f));

        Matrix4f projectionMatrix = new Matrix4f();
        client.gameRenderer.loadProjectionMatrix(projectionMatrix);

        SeedQueueProfiler.swap("camera_update");
        camera.update(client.world, client.player, camera.isThirdPerson(), ((CameraAccessor) camera).seedQueue$isInverseView(), 1.0f);
        SeedQueueProfiler.swap("build_chunks");
        ((SQWorldRenderer) client.worldRenderer).seedQueue$buildChunks(rotationMatrix, camera, projectionMatrix);
        SeedQueueProfiler.pop();

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

    public void loadPlayerModelParts() {
        // see WorldPreview#configure
        int playerModelPartsBitMask = 0;
        for (PlayerModelPart playerModelPart : MinecraftClient.getInstance().options.getEnabledPlayerModelParts()) {
            playerModelPartsBitMask |= playerModelPart.getBitFlag();
        }
        this.player.getDataTracker().set(PlayerEntityAccessor.seedQueue$getPLAYER_MODEL_PARTS(), (byte) playerModelPartsBitMask);
    }

    public void load() {
        WorldPreview.set(this.world, this.player, this.interactionManager, this.camera, this.packetQueue);
    }

    @Override
    protected int getDataLimit() {
        // Don't allow Unlimited (=100) on wall
        return Math.min(99, super.getDataLimit());
    }
}
