package me.contaria.seedqueue.mixin.client.debug;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {

    @ModifyReturnValue(
            method = "getRightText",
            at = @At("RETURN")
    )
    private List<String> modifyRightText(List<String> debugText) {
        if (SeedQueue.isActive()) {
            debugText.addAll(SeedQueue.getDebugText());
        }
        return debugText;
    }
}
