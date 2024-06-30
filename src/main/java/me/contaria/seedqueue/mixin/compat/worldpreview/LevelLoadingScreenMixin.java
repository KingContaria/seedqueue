package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.bawnorton.mixinsquared.TargetHandler;
import me.contaria.seedqueue.compat.WorldPreviewFrameBuffer;
import me.contaria.seedqueue.gui.wall.SeedQueuePreview;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;

@Mixin(value = LevelLoadingScreen.class, priority = 1500)
public abstract class LevelLoadingScreenMixin extends Screen {

    protected LevelLoadingScreenMixin(Text title) {
        super(title);
    }

    @SuppressWarnings("CancellableInjectionUsage") // it seems MCDev gets confused because there is two CallbackInfos
    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.client.render.LevelLoadingScreenMixin",
            name = "renderWorldPreview"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;runAsPreview(Ljava/lang/Runnable;)V",
                    remap = false
            ),
            cancellable = true
    )
    private void drawClearBackground(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ignored, CallbackInfo ci) {
        this.getAsSeedQueuePreview().ifPresent(preview -> {
            WorldPreviewFrameBuffer frameBuffer = Objects.requireNonNull(preview.getWorldPreviewProperties()).getOrCreateFrameBuffer();

            String renderData = preview.getWorldRenderer().getChunksDebugString() + "\n" + preview.getWorldRenderer().getEntitiesDebugString();
            if (frameBuffer.isEmpty() || (frameBuffer.isDirty(renderData) && preview.shouldRenderPreview())) {
                frameBuffer.beginWrite(renderData);

                // related to WorldRendererMixin#doNotClearOnWallScreen
                // the suppressed call usually renders a light blue overlay over the entire screen,
                // instead we draw it onto the preview ourselves
                DrawableHelper.fill(matrices, 0, 0, preview.wall.width, preview.wall.height, -5323025);
                return;
            }

            // this can not be SeedQueuePreview#build because that updates and resets WorldPreviewProperties
            WorldPreview.runAsPreview(() -> {
                WorldPreview.tickPackets();
                WorldPreview.tickEntities();
                preview.getWorldPreviewProperties().buildChunks();
            });
            frameBuffer.draw(this.width, this.height);
            ci.cancel();
        });
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.client.render.LevelLoadingScreenMixin",
            name = "renderWorldPreview"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("TAIL")
    )
    private void endFrame(CallbackInfo ci) {
        this.getAsSeedQueuePreview().ifPresent(preview -> {
            WorldPreviewFrameBuffer frameBuffer = Objects.requireNonNull(preview.getWorldPreviewProperties()).getOrCreateFrameBuffer();

            frameBuffer.endWrite();
            preview.updateLastRenderFrame();

            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
            preview.wall.refreshViewport();
            frameBuffer.draw(this.width, this.height);
        });
    }

    @Unique
    private Optional<SeedQueuePreview> getAsSeedQueuePreview() {
        //noinspection ConstantValue
        if ((Object) this instanceof SeedQueuePreview) {
            return Optional.of((SeedQueuePreview) (Object) this);
        }
        return Optional.empty();
    }
}
