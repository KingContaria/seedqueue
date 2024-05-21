package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.bawnorton.mixinsquared.TargetHandler;
import me.contaria.seedqueue.compat.WorldPreviewFrame;
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

import java.util.Optional;

@Mixin(value = LevelLoadingScreen.class, priority = 1500)
public abstract class LevelLoadingScreenMixin extends Screen {

    protected LevelLoadingScreenMixin(Text title) {
        super(title);
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.client.render.LevelLoadingScreenMixin",
            name = "renderWorldPreview"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    private void beginFrame(CallbackInfo ci) {
        this.getAsSeedQueuePreview().ifPresent(preview -> {
            WorldPreviewFrame frame = preview.getWorldPreviewProperties().getFrame();

            String renderData = preview.getWorldRenderer().getChunksDebugString() + "\n" + preview.getWorldRenderer().getEntitiesDebugString();
            if (frame.isEmpty() || (frame.isDirty(renderData) && preview.shouldRenderPreview())) {
                frame.beginWrite(renderData);
                return;
            }

            WorldPreview.runAsPreview(() -> {
                WorldPreview.tickPackets();
                WorldPreview.tickEntities();
                preview.getWorldPreviewProperties().buildChunks();
            });
            frame.draw(this.width, this.height);
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
            at = @At(
                    value = "INVOKE",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;runAsPreview(Ljava/lang/Runnable;)V",
                    remap = false
            )
    )
    private void drawClearBackground(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ignored, CallbackInfo ci) {
        this.getAsSeedQueuePreview().ifPresent(preview -> {
            // related to WorldRendererMixin#doNotClearOnWallScreen
            // the suppressed call usually renders a light blue overlay over the entire screen,
            // instead we draw it onto the preview ourselves
            DrawableHelper.fill(matrices, 0, 0, preview.wallScreen.width, preview.wallScreen.height, -5323025);
        });
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.client.render.LevelLoadingScreenMixin",
            name = "worldpreview$renderPauseMenu"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    private void bufferPauseMenuOnWall(CallbackInfo ci) {
        this.getAsSeedQueuePreview().ifPresent(preview -> {
            if (preview.wallScreen.shouldUseInGameHudBuffer() && !preview.wallScreen.isInGameHudBuffering()) {
                ci.cancel();
            }
        });
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.client.render.LevelLoadingScreenMixin",
            name = "renderWorldPreview"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("RETURN")
    )
    private void endFrame(CallbackInfo ci) {
        this.getAsSeedQueuePreview().ifPresent(preview -> {
            WorldPreviewFrame frame = preview.getWorldPreviewProperties().getFrame();

            if (preview.wallScreen.isInGameHudBuffering()) {
                preview.wallScreen.endBufferingInGameHud();
                frame.continueWrite();
            }
            if (preview.wallScreen.shouldUseInGameHudBuffer() && preview.wallScreen.isInGameHudBuffered()) {
                preview.wallScreen.drawBufferedInGameHud(this.width, this.height);
            }

            frame.endWrite();
            preview.updateLastRenderFrame();

            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
            preview.wallScreen.refreshViewport();
            frame.draw(this.width, this.height);
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
