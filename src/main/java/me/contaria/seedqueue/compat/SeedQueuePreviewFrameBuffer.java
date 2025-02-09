package me.contaria.seedqueue.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper for Minecrafts {@link Framebuffer} storing a previews last drawn image.
 * <p>
 * Stores {@link SeedQueuePreviewFrameBuffer#lastRenderData} as to only redraw the preview if it has changed.
 */
public class SeedQueuePreviewFrameBuffer {
    private static final List<Framebuffer> FRAMEBUFFER_POOL = new ArrayList<>();

    private final Framebuffer framebuffer;

    // stores a string unique to the current state of world rendering when writing to the framebuffer
    @Nullable
    private String lastRenderData;

    public SeedQueuePreviewFrameBuffer() {
        if (FRAMEBUFFER_POOL.isEmpty()) {
            this.framebuffer = new Framebuffer(
                    SeedQueue.config.simulatedWindowSize.width(),
                    SeedQueue.config.simulatedWindowSize.height(),
                    true,
                    MinecraftClient.IS_SYSTEM_MAC
            );
        } else {
            this.framebuffer = FRAMEBUFFER_POOL.remove(0);
        }
    }

    public void beginWrite() {
        this.framebuffer.beginWrite(true);
    }

    public void endWrite() {
        this.framebuffer.endWrite();
    }

    /**
     * @param renderData A string unique to the previews current state, sensitive to changes in entities and chunks. See LevelLoadingScreenMixin#beginFrame.
     * @return True if the given newRenderData is different from the stored {@link SeedQueuePreviewFrameBuffer#lastRenderData}.
     */
    public boolean updateRenderData(String renderData) {
        return !Objects.equals(this.lastRenderData, this.lastRenderData = renderData);
    }

    /**
     * Draws the internal {@link Framebuffer} without setting {@link RenderSystem#ortho} and {@link RenderSystem#viewport}.
     */
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

    public void discard() {
        FRAMEBUFFER_POOL.add(this.framebuffer);
    }

    static void clearFramebufferPool() {
        for (Framebuffer framebuffer : FRAMEBUFFER_POOL) {
            framebuffer.delete();
        }
        FRAMEBUFFER_POOL.clear();
    }
}
