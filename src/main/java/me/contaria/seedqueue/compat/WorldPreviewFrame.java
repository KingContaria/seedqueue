package me.contaria.seedqueue.compat;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.jetbrains.annotations.Nullable;

public class WorldPreviewFrame {

    private final Framebuffer framebuffer;

    @Nullable
    private String lastRenderData;
    private long lastRenderTime;

    public WorldPreviewFrame(int width, int height) {
        this.framebuffer = new Framebuffer(width, height, true, MinecraftClient.IS_SYSTEM_MAC);
    }

    public void beginWrite(String renderData) {
        this.lastRenderTime = System.currentTimeMillis();
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

    public long getLastRenderTime() {
        return this.lastRenderTime;
    }

    public void draw(int width, int height) {
        GlStateManager.bindTexture(this.framebuffer.colorAttachment);

        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(7, VertexFormats.POSITION_TEXTURE);
        bufferbuilder.vertex(0.0D, height, -90.0D).texture(0.0F, 0.0F).next();
        bufferbuilder.vertex(width, height, -90.0D).texture(1.0F, 0.0F).next();
        bufferbuilder.vertex(width, 0.0D, -90.0D).texture(1.0F, 1.0F).next();
        bufferbuilder.vertex(0.0D, 0.0D, -90.0D).texture(0.0F, 1.0F).next();
        bufferbuilder.end();
        BufferRenderer.draw(bufferbuilder);
    }
}
