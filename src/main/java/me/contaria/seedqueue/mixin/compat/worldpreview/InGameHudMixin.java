package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @WrapWithCondition(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/SubtitlesHud;render(Lnet/minecraft/client/util/math/MatrixStack;)V"
            )
    )
    private boolean doNotRenderSubtitlesOnWall(SubtitlesHud subtitlesHud, MatrixStack matrices) {
        return !SeedQueue.isOnWall();
    }

    @WrapWithCondition(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/util/math/MatrixStack;I)V"
            )
    )
    private boolean doNotRenderChatOnWall(ChatHud chatHud, MatrixStack matrices, int i) {
        return !SeedQueue.isOnWall();
    }
}
