package me.contaria.seedqueue.mixin.compat.atum;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.options.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OptionsScreen.class, priority = 1500)
public abstract class OptionsScreenMixin {
    @Unique
    private ButtonWidget atumButton;

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.autoreset.mixin.gui.OptionsScreenMixin",
            name = "addStopResetsButton"
    )
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "NEW",
                    target = "(IIIILnet/minecraft/text/Text;Lnet/minecraft/client/gui/widget/ButtonWidget$PressAction;)Lnet/minecraft/client/gui/widget/ButtonWidget;"
            )
    )
    private ButtonWidget stopQueueWhenShiftClicked(int x, int y, int width, int height, Text text, ButtonWidget.PressAction action, Operation<ButtonWidget> original) {
        return this.atumButton = original.call(x, y, width, height, text, (ButtonWidget.PressAction)button -> {
            if (this.shouldClearQueue()) {
                SeedQueue.stop();
            } else {
                action.onPress(button);
            }
        });
    }

    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V",
        at = @At("HEAD")
    )
    private void setAtumButtonText(CallbackInfo ci) {
        if (this.atumButton == null) {
            return;
        }

        if (this.shouldClearQueue() && this.atumButton.isHovered()) {
            this.atumButton.setMessage(new TranslatableText("seedqueue.menu.clearQueue"));
        } else {
            this.atumButton.setMessage(new TranslatableText("atum.menu.stop_resets"));
        }
    }

    @Unique
    private boolean shouldClearQueue() {
        return Screen.hasShiftDown() && SeedQueue.isActive();
    }
}