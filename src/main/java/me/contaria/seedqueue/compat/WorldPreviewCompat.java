package me.contaria.seedqueue.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import me.contaria.seedqueue.SeedQueueProfiler;
import me.contaria.seedqueue.interfaces.worldpreview.SQWorldRenderer;
import me.contaria.seedqueue.mixin.accessor.CameraAccessor;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.interfaces.WPMinecraftServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Matrix4f;

public class WorldPreviewCompat {

    static boolean kill(MinecraftServer server) {
        return ((WPMinecraftServer) server).worldpreview$kill();
    }

    /**
     * Builds chunks for the current {@link WorldPreview#worldRenderer} without rendering the preview.
     *
     * @see WorldPreview#render
     */
    @SuppressWarnings({"deprecation", "SynchronizeOnNonFinalField"})
    public static void buildChunks() {
        MinecraftClient client = MinecraftClient.getInstance();
        Window window = client.getWindow();

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
        rotationMatrix.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(WorldPreview.camera.getPitch()));
        rotationMatrix.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(WorldPreview.camera.getYaw() + 180.0f));

        Matrix4f projectionMatrix = new Matrix4f();
        client.gameRenderer.loadProjectionMatrix(projectionMatrix);

        SeedQueueProfiler.swap("camera_update");
        synchronized (WorldPreview.camera) {
            WorldPreview.camera.update(WorldPreview.world, WorldPreview.player, WorldPreview.camera.isThirdPerson(), ((CameraAccessor) WorldPreview.camera).seedQueue$isInverseView(), 0);
        }
        SeedQueueProfiler.swap("build_chunks");
        ((SQWorldRenderer) WorldPreview.worldRenderer).seedQueue$buildChunks(rotationMatrix, WorldPreview.camera, projectionMatrix);
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
}
