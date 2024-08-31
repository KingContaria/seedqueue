package me.contaria.seedqueue.mixin.compat.worldpreview.render;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.compat.WorldPreviewCompat;
import me.contaria.seedqueue.compat.WorldPreviewFrameBuffer;
import me.contaria.seedqueue.gui.wall.SeedQueuePreview;
import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressTracker;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
                    WorldPreviewCompat.buildChunks();
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
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;updateState()Z",
                    remap = false
            )
    )
    private boolean doNotUpdateWorldPreviewStateOnWall(Operation<Boolean> original) {
        //noinspection ConstantValue
        if ((Object) this instanceof SeedQueuePreview) {
            return false;
        }
        return original.call();
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

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;drawChunkMap(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/gui/WorldGenerationProgressTracker;IIII)V"
            ),
            index = 1
    )
    private WorldGenerationProgressTracker replaceChunkMap(WorldGenerationProgressTracker progress) {
        Optional<SeedQueuePreview> seedQueuePreview = this.getAsSeedQueuePreview();
        if (!seedQueuePreview.isPresent() || seedQueuePreview.get().getSeedQueueEntry().isLocked()) return progress;
        return ((SQWorldGenerationProgressTracker) progress).seedQueue$getFrozenCopy().orElse(progress);
    }
}
