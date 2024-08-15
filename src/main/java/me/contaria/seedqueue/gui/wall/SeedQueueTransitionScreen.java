package me.contaria.seedqueue.gui.wall;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.compat.WorldPreviewFrameBuffer;
import me.contaria.seedqueue.customization.Layout;
import me.contaria.seedqueue.customization.transitions.Transition;
import me.contaria.seedqueue.mixin.accessor.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public class SeedQueueTransitionScreen extends Screen {
    private final WorldPreviewFrameBuffer transitionTo;
    private final Transition transition;
    private final long start;
    private final int startX;
    private final int startY;
    private final int startX2;
    private final int startY2;

    private SeedQueueTransitionScreen(WorldPreviewFrameBuffer transitionTo, Transition transition, long start, int startX, int startY, int startX2, int startY2) {
        super(LiteralText.EMPTY);
        this.transitionTo = transitionTo;
        this.transition = transition;
        this.start = start;
        this.startX = startX;
        this.startY = startY;
        this.startX2 = startX2;
        this.startY2 = startY2;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        double progress = (double) (System.currentTimeMillis() - this.start) / this.transition.getDuration();
        int x = this.transition.transform(this.startX, 0, progress);
        int y = this.transition.transform(this.startY, 0, progress);
        int x2 = this.transition.transform(this.startX2, this.width, progress);
        int y2 = this.transition.transform(this.startY2, this.height, progress);
        this.transitionTo.draw(x, y, x2, y2);
    }

    @Override
    public void removed() {
        this.transitionTo.delete();
    }

    public static void transition(Screen atumCreateWorldScreen, Layout.Pos startPos) {
        if (startPos == null) {
            MinecraftClient.getInstance().openScreen(atumCreateWorldScreen);
            return;
        }
        Transition transition = Transition.createTransition("wall_to_world");
        if (transition != null) {
            long start = System.currentTimeMillis();
            MinecraftClient.getInstance().openScreen(new SeedQueueTransitionScreen(
                    SeedQueue.selectedEntry.removeFrameBuffer(),
                    transition,
                    start,
                    scale(startPos.x),
                    scale(startPos.y),
                    scale(startPos.x + startPos.width),
                    scale(startPos.y + startPos.height)
            ));
            while (System.currentTimeMillis() - start < transition.getDuration()) {
                ((MinecraftClientAccessor) MinecraftClient.getInstance()).seedQueue$render(false);
            }
            atumCreateWorldScreen.init(MinecraftClient.getInstance(), 0, 0);
            return;
        }
        MinecraftClient.getInstance().openScreen(atumCreateWorldScreen);
    }

    private static int scale(int value) {
        return (int) Math.round(value / MinecraftClient.getInstance().getWindow().getScaleFactor());
    }
}
