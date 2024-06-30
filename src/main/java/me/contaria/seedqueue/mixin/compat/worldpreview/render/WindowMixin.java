package me.contaria.seedqueue.mixin.compat.worldpreview.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.contaria.seedqueue.SeedQueue;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Window.class)
public abstract class WindowMixin {

    @ModifyReturnValue(
            method = {
                    "getWidth",
                    "getFramebufferWidth"
            },
            at = @At("RETURN")
    )
    private int modifyWidthOnWall(int width) {
        if (this.shouldModifyWindowSize()) {
            return SeedQueue.config.simulatedWindowSize.width();
        }
        return width;
    }

    @ModifyReturnValue(
            method = "getScaledWidth",
            at = @At("RETURN")
    )
    private int modifyScaledWidthOnWall(int width) {
        if (this.shouldModifyWindowSize()) {
            return SeedQueue.config.simulatedWindowSize.width() / this.modifiedScaleFactor();
        }
        return width;
    }

    @ModifyReturnValue(
            method = {
                    "getHeight",
                    "getFramebufferHeight"
            },
            at = @At("RETURN")
    )
    private int modifyHeightOnWall(int height) {
        if (this.shouldModifyWindowSize()) {
            return SeedQueue.config.simulatedWindowSize.height();
        }
        return height;
    }

    @ModifyReturnValue(
            method = "getScaledHeight",
            at = @At("RETURN")
    )
    private int modifyScaledHeightOnWall(int height) {
        if (this.shouldModifyWindowSize()) {
            return SeedQueue.config.simulatedWindowSize.height() / this.modifiedScaleFactor();
        }
        return height;
    }

    @ModifyReturnValue(
            method = "getScaleFactor",
            at = @At("RETURN")
    )
    private double modifyScaleFactorOnWall(double scaleFactor) {
        if (this.shouldModifyWindowSize()) {
            return this.modifiedScaleFactor();
        }
        return scaleFactor;
    }

    @Unique
    private boolean shouldModifyWindowSize() {
        return MinecraftClient.getInstance().isOnThread() && SeedQueue.isOnWall() && WorldPreview.renderingPreview;
    }

    @Unique
    private int modifiedScaleFactor() {
        return SeedQueue.config.calculateSimulatedScaleFactor(MinecraftClient.getInstance().options.guiScale, MinecraftClient.getInstance().options.forceUnicodeFont);
    }
}
