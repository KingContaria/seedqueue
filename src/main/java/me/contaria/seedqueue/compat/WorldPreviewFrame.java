package me.contaria.seedqueue.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.jetbrains.annotations.Nullable;

public class WorldPreviewFrame {

    private final Framebuffer framebuffer;

    @Nullable
    private String lastRenderData;

    public WorldPreviewFrame(int width, int height) {
        this.framebuffer = new Framebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
    }

    public void beginWrite(String renderData) {
        this.lastRenderData = renderData;
        this.framebuffer.beginWrite(true);
    }

    public void endWrite() {
        this.framebuffer.endWrite();
    }

    public boolean isEmpty() {
        return this.lastRenderData == null;
    }

    public boolean isDirty(String newRenderData) {
        return !newRenderData.equals(this.lastRenderData);
    }

    @SuppressWarnings("deprecation")
    public void draw(int width, int height) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);

        RenderSystem.colorMask(true, true, true, false);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableTexture();
        RenderSystem.disableLighting();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        this.framebuffer.beginRead();
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(0.0, height, 0.0).texture(0.0f, 0.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width, height, 0.0).texture(1.0f, 0.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width, 0.0, 0.0).texture(1.0f, 1.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(0.0, 0.0, 0.0).texture(0.0f, 1.0f).color(255, 255, 255, 255).next();
        tessellator.draw();
        this.framebuffer.endRead();

        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
    }

    public void delete() {
        this.framebuffer.delete();
    }
}
