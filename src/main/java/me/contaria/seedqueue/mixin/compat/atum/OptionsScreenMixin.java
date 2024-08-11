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
public abstract class OptionsScreenMixin extends Screen {
    @Unique
    private ButtonWidget atumButton;

    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private boolean shouldClearQueue() {
        return Screen.hasShiftDown() && SeedQueue.isActive();
    }

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
        this.atumButton = new ButtonWidget(x, y, width, height, text, button -> {
            if (shouldClearQueue()) {
                SeedQueue.stop();
            } else {
                action.onPress(button);
            }
        });

        return this.atumButton;
    }

    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V",
        at = @At("HEAD")
    )
    private void setAtumButtonText(CallbackInfo ci) {
        if (atumButton == null) {
            return;
        }

        if (shouldClearQueue()) {
            atumButton.setMessage(new TranslatableText("seedqueue.menu.clearQueue"));
        } else {
            atumButton.setMessage(new TranslatableText("atum.menu.stop_resets"));
        }
    }
}