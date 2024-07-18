package me.contaria.seedqueue.gui.wall;

import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class SeedQueueBenchmarkToast implements Toast {
    private final SeedQueueWallScreen wall;
    private final Text title;

    private boolean finished;
    private boolean fadeOut;
    private long fadeOutStart;

    public SeedQueueBenchmarkToast(SeedQueueWallScreen wall) {
        this.wall = wall;
        this.title = new TranslatableText("seedqueue.menu.benchmark.title");
    }

    @Override
    public Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        manager.getGame().getTextureManager().bindTexture(TOASTS_TEX);
        manager.drawTexture(matrices, 0, 0, 0, 0, this.getWidth(), this.getHeight());
        manager.getGame().textRenderer.draw(matrices, this.title, 7.0f, 7.0f, 0xFFFF00 | 0xFF000000);

        this.finished |= !this.wall.isBenchmarking();

        if (this.finished && !this.fadeOut && manager.getGame().isWindowFocused()) {
            this.fadeOutStart = startTime;
            this.fadeOut = true;
        }

        double time = (this.finished ? this.wall.benchmarkFinish : System.currentTimeMillis()) - this.wall.benchmarkStart;
        double rps = Math.round(this.wall.benchmarkedSeeds / (time / 10000.0)) / 10.0;
        manager.getGame().textRenderer.draw(matrices, new TranslatableText("seedqueue.menu.benchmark.result", this.wall.benchmarkedSeeds, Math.round(time / 1000.0), rps), 7.0f, 18.0f, -1);

        return (this.fadeOut && startTime - this.fadeOutStart > 5000) ? Visibility.HIDE : Visibility.SHOW;
    }
}
