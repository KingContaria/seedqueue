package me.contaria.seedqueue.mixin.compat.atum;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.gui.config.SeedQueueKeybindingsScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Keyboard.class, priority = 1500)
public abstract class KeyboardMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.autoreset.mixin.hotkey.KeyboardMixin",
            name = "onKey"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/options/KeyBinding;matchesKey(II)Z"
            )
    )
    private boolean doNotActivateAtumHotKey_onWall(boolean matchesKey) {
        return matchesKey && !(SeedQueue.isOnWall() || this.client.currentScreen instanceof SeedQueueKeybindingsScreen);
    }
}
