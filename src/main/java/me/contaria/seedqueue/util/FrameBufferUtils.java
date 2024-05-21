package me.contaria.seedqueue.util;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;

@SuppressWarnings("deprecation")
public class FrameBufferUtils {

    /**
     * {@link Framebuffer#draw} without {@link RenderSystem#viewport} and {@link RenderSystem#ortho}
     */
    public static void draw(Framebuffer framebuffer, int width, int height) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);

        RenderSystem.colorMask(true, true, true, false);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableTexture();
        RenderSystem.disableLighting();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        framebuffer.beginRead();
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(0.0, height, 0.0).texture(0.0f, 0.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width, height, 0.0).texture(1.0f, 0.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width, 0.0, 0.0).texture(1.0f, 1.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(0.0, 0.0, 0.0).texture(0.0f, 1.0f).color(255, 255, 255, 255).next();
        tessellator.draw();
        framebuffer.endRead();

        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
    }

    /**
     * {@link Framebuffer#draw} without {@link RenderSystem#viewport} and made to work with translucent FrameBuffers
     */
    public static void drawTranslucent(Framebuffer framebuffer, int width, int height) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);

        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        framebuffer.beginRead();
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(0.0, height, 0.0).texture(0.0f, 0.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width, height, 0.0).texture(1.0f, 0.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(width, 0.0, 0.0).texture(1.0f, 1.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(0.0, 0.0, 0.0).texture(0.0f, 1.0f).color(255, 255, 255, 255).next();
        tessellator.draw();
        framebuffer.endRead();

        RenderSystem.depthMask(true);
    }
}
