package me.contaria.seedqueue.mixin.compat.antiresourcereload;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.MinecraftClient;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MinecraftClient.class, priority = 1500)
public abstract class MinecraftClientMixin {

    @Dynamic
    @TargetHandler(
            mixin = "me.wurgo.antiresourcereload.mixin.MinecraftClientMixin",
            name = "cachedReload"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "FIELD",
                    target = "Lme/wurgo/antiresourcereload/AntiResourceReload;hasSeenRecipes:Z",
                    opcode = Opcodes.GETSTATIC,
                    remap = false
            ),
            require = 0
    )
    private boolean doNotReloadAntiResourceReload_inQueue(boolean hasSeenRecipes) {
        return hasSeenRecipes && !SeedQueue.inQueue();
    }
}
