package me.contaria.seedqueue.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringRenderable;
import net.minecraft.text.Text;

import java.util.List;

public class SeedQueueCrashToast implements Toast {
    private final Text title;
    private final List<StringRenderable> description;
    private boolean hasStarted;
    private long startTime = Long.MAX_VALUE;

    public SeedQueueCrashToast(Text title, Text description) {
        this.title = title;
        this.description = MinecraftClient.getInstance().textRenderer.wrapLines(description, this.getWidth() - 7);
    }

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        if (!this.hasStarted && manager.getGame().isWindowFocused()) {
            this.startTime = startTime;
            this.hasStarted = true;
        }

        manager.getGame().getTextureManager().bindTexture(TOASTS_TEX);
        if (this.description.size() < 2) {
            manager.drawTexture(matrices, 0, 0, 0, 0, this.getWidth(), this.getHeight());
        } else {
            manager.drawTexture(matrices, 0, 0, 0, 0, this.getWidth(), 11);
            int y = 8;
            for (int i = 0; i < this.description.size(); i++) {
                manager.drawTexture(matrices, 0, y, 0, 11, this.getWidth(), 10);
                y += 10;
            }
            manager.drawTexture(matrices, 0, y, 0, 21, this.getWidth(), 11);
        }

        manager.getGame().textRenderer.draw(matrices, this.title, 7.0f, 7.0f, 0xFFFF00 | 0xFF000000);

        float y = 18.0f;
        for (StringRenderable description : this.description) {
            manager.getGame().textRenderer.draw(matrices, description, 7.0f, y, -1);
            y += 10.0f;
        }

        return startTime - this.startTime < 5000L ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    @Override
    public int getHeight() {
        return Toast.super.getHeight() + Math.max(1, this.description.size() - 1) * 10;
    }
}
