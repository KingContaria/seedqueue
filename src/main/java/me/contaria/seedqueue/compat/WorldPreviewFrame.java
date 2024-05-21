package me.contaria.seedqueue.compat;

import me.contaria.seedqueue.util.FrameBufferUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
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

    public void continueWrite() {
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

    public void draw(int width, int height) {
        FrameBufferUtils.draw(this.framebuffer, width, height);
    }

    public void delete() {
        this.framebuffer.delete();
    }
}
