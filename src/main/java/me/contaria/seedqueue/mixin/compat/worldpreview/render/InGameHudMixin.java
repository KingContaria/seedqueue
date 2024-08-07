package me.contaria.seedqueue.mixin.compat.worldpreview.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin extends DrawableHelper {

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

    @ModifyExpressionValue(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;overlayMessage:Lnet/minecraft/text/Text;",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0
            )
    )
    private Text doNotRenderOverlayMessageOnWall(Text overlayMessage) {
        if (SeedQueue.isOnWall()) {
            return null;
        }
        return overlayMessage;
    }

    @ModifyExpressionValue(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;title:Lnet/minecraft/text/Text;",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0
            )
    )
    private Text doNotRenderTitleMessageOnWall(Text title) {
        if (SeedQueue.isOnWall()) {
            return null;
        }
        return title;
    }

    @ModifyVariable(
            method = "renderStatusBars",
            at = @At("STORE")
    )
    private boolean doNotRenderBlinkingHeartsOnWall(boolean blinking) {
        return blinking && !SeedQueue.isOnWall();
    }
}
