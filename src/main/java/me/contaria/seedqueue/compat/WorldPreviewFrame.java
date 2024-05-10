package me.contaria.seedqueue.compat;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
        // DrawableHelper#drawTexture enables alpha test when rendering the lock image,
        // which causes artifacts to appear when drawing the buffer, to prevent this we just make sure its disabled
        RenderSystem.disableAlphaTest();

        GlStateManager.bindTexture(this.framebuffer.colorAttachment);

        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(7, VertexFormats.POSITION_TEXTURE);
        bufferbuilder.vertex(0.0, height, -90.0).texture(0.0F, 0.0F).next();
        bufferbuilder.vertex(width, height, -90.0).texture(1.0F, 0.0F).next();
        bufferbuilder.vertex(width, 0.0, -90.0).texture(1.0F, 1.0F).next();
        bufferbuilder.vertex(0.0, 0.0, -90.0).texture(0.0F, 1.0F).next();
        bufferbuilder.end();
        BufferRenderer.draw(bufferbuilder);
    }

    public void delete() {
        this.framebuffer.delete();
    }
}
