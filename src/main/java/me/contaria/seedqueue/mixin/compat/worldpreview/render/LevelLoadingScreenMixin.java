package me.contaria.seedqueue.mixin.compat.worldpreview.render;

import com.bawnorton.mixinsquared.TargetHandler;
import me.contaria.seedqueue.compat.WorldPreviewFrameBuffer;
import me.contaria.seedqueue.gui.wall.SeedQueuePreview;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;

@Mixin(value = LevelLoadingScreen.class, priority = 1500)
public abstract class LevelLoadingScreenMixin {

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
    private void beginFrame(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ignored, CallbackInfo ci) {
        this.getAsSeedQueuePreview().ifPresent(preview -> {
            boolean hasPreviewProperties = preview.getWorldPreviewProperties() != null;

            WorldPreviewFrameBuffer frameBuffer = preview.getSeedQueueEntry().getFrameBuffer(hasPreviewProperties);
            if (frameBuffer == null) {
                return;
            }

            String renderData;
            if (preview.shouldRedrawPreview() && frameBuffer.isDirty(renderData = preview.getWorldRenderer().getChunksDebugString() + "\n" + preview.getWorldRenderer().getEntitiesDebugString())) {
                if (!hasPreviewProperties) {
                    throw new IllegalStateException("Tried to draw preview but there is no preview properties!");
                }
                frameBuffer.beginWrite(renderData);

                // related to WorldRendererMixin#doNotClearOnWallScreen
                // the suppressed call usually renders a light blue overlay over the entire screen,
                // instead we draw it onto the preview ourselves
                DrawableHelper.fill(matrices, 0, 0, preview.wall.width, preview.wall.height, -5323025);
                return;
            }

            if (frameBuffer.isEmpty()) {
                throw new IllegalStateException("Tried to draw preview framebuffer but framebuffer is empty!");
            }

            // this can not be SeedQueuePreview#build because that updates and resets WorldPreviewProperties
            if (hasPreviewProperties) {
                WorldPreview.runAsPreview(() -> {
                    WorldPreview.tickPackets();
                    WorldPreview.tickEntities();
                    Objects.requireNonNull(preview.getWorldPreviewProperties()).buildChunks();
                });
            }
            frameBuffer.draw(preview.width, preview.height);
            preview.onPreviewRender(false);
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
            WorldPreviewFrameBuffer frameBuffer = preview.getSeedQueueEntry().getFrameBuffer();
            frameBuffer.endWrite();
            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
            preview.wall.refreshViewport();
            frameBuffer.draw(preview.width, preview.height);
            preview.onPreviewRender(true);
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
