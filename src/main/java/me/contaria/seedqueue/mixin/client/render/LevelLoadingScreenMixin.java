package me.contaria.seedqueue.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueConfig;
import me.contaria.seedqueue.gui.wall.SeedQueuePreview;
import me.contaria.seedqueue.mixin.accessor.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(value = LevelLoadingScreen.class, priority = 1500)
public abstract class LevelLoadingScreenMixin {

    @Unique
    private static final int NO_MODIFIER = new Color(255, 255, 255, 255).getRGB();

    @Unique
    private static final int TRANSPARENT_MODIFIER = new Color(255, 255, 255, 150).getRGB();

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;renderBackground(Lnet/minecraft/client/util/math/MatrixStack;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void doNotRenderChunkMapOnWallScreen(CallbackInfo ci) {
        //noinspection ConstantValue
        if ((Object) this instanceof SeedQueuePreview && ((SeedQueuePreview) (Object) this).getSeedQueueEntry().isReady()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "drawChunkMap",
            at = @At("HEAD")
    )
    private static void setColorModifier(MatrixStack matrixStack, WorldGenerationProgressTracker tracker, int i, int j, int k, int l, CallbackInfo ci, @Share("colorModifier") LocalIntRef colorModifier) {
        if (isSeedQueueChunkMap(tracker) && !SeedQueue.isOnWall() && SeedQueue.config.chunkMapVisibility == SeedQueueConfig.ChunkMapVisibility.TRANSPARENT) {
            colorModifier.set(TRANSPARENT_MODIFIER);
        } else {
            colorModifier.set(NO_MODIFIER);
        }
    }

    // This group is here for compatibility with sodium's MixinLevelLoadingScreen @Overwrite of drawChunkMap.
    @Group(name = "transparent")
    @ModifyArg(
            method = "drawChunkMap",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V"
            ),
            slice = @Slice(
                    from = @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;STATUS_TO_COLOR:Lit/unimi/dsi/fastutil/objects/Object2IntMap;"
                    )
            ),
            index = 5
    )
    private static int transparentSeedQueueChunkMap(int color, @Share("colorModifier") LocalIntRef colorModifier) {
        return color & colorModifier.get();
    }

    @Dynamic
    @Group(name = "transparent")
    @ModifyArg(
            method = "drawChunkMap",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;addRect(Lnet/minecraft/util/math/Matrix4f;Lme/jellysquid/mods/sodium/client/model/vertex/formats/screen_quad/BasicScreenQuadVertexSink;IIIII)V"
            ),
            index = 6
    )
    private static int transparentSeedQueueChunkMap_sodium(int color, @Share("colorModifier") LocalIntRef colorModifier) {
        return color & colorModifier.get();
    }

    @Unique
    private static boolean isSeedQueueChunkMap(WorldGenerationProgressTracker tracker) {
        return !tracker.equals(((MinecraftClientAccessor) MinecraftClient.getInstance()).seedQueue$getWorldGenProgressTracker().get());
    }
}
