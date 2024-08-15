package me.contaria.seedqueue.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for Minecrafts {@link Framebuffer} storing a previews last drawn image.
 * <p>
 * Stores {@link WorldPreviewFrameBuffer#lastRenderData} as to only redraw the preview if it has changed.
 */
public class WorldPreviewFrameBuffer {
    private final Framebuffer framebuffer;

    // stores a string unique to the current state of world rendering when writing to the framebuffer
    @Nullable
    private String lastRenderData;

    public WorldPreviewFrameBuffer(int width, int height) {
        this.framebuffer = new Framebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
    }

    /**
     * @param renderData A string unique to the previews current state, sensitive to changes in entities and chunks. See LevelLoadingScreenMixin#beginFrame.
     */
    public void beginWrite(String renderData) {
        this.lastRenderData = renderData;
        this.framebuffer.beginWrite(true);
    }

    public void endWrite() {
        this.framebuffer.endWrite();
    }

    /**
     * @return True if this buffer hasn't been drawn to before.
     */
    public boolean isEmpty() {
        return this.lastRenderData == null;
    }

    /**
     * @param newRenderData A string unique to the previews current state, sensitive to changes in entities and chunks. See LevelLoadingScreenMixin#beginFrame.
     * @return True if the given newRenderData is different from the stored {@link WorldPreviewFrameBuffer#lastRenderData}.
     */
    public boolean isDirty(String newRenderData) {
        return !newRenderData.equals(this.lastRenderData);
    }

    /**
     * Draws the internal {@link Framebuffer} to the specified width and height.
     *
     * @see WorldPreviewFrameBuffer#draw(int, int, int, int)
     */
    public void draw(int width, int height) {
        this.draw(0, 0, width, height);
    }

    /**
     * Draws the internal {@link Framebuffer} without setting {@link RenderSystem#ortho} and {@link RenderSystem#viewport}.
     */
    @SuppressWarnings("deprecation")
    public void draw(int x1, int y1, int x2, int y2) {
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
        bufferBuilder.vertex(x1, y2, 0.0).texture(0.0f, 0.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(x2, y2, 0.0).texture(1.0f, 0.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(x2, y1, 0.0).texture(1.0f, 1.0f).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(x1, y1, 0.0F).texture(0.0f, 1.0f).color(255, 255, 255, 255).next();
        tessellator.draw();
        this.framebuffer.endRead();

        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
    }

    public void delete() {
        this.framebuffer.delete();
    }
}
