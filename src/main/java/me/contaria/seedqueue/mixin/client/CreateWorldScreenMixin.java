package me.contaria.seedqueue.mixin.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @WrapWithCondition(
            method = "createLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;setScreenAndRender(Lnet/minecraft/client/gui/screen/Screen;)V"
            )
    )
    private boolean cancelRenderingScreen(MinecraftClient client, Screen screen) {
        return !SeedQueue.inQueue();
    }

    @WrapOperation(
            method = "createLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;setScreenAndRender(Lnet/minecraft/client/gui/screen/Screen;)V"
            )
    )
    private void skipIntermissionScreen(MinecraftClient client, Screen screen, Operation<Void> original) {
        if (SeedQueue.currentEntry != null) {
            client.openScreen(screen);
        } else {
            original.call(client, screen);
        }
    }

    @Inject(
            method = "createLevel",
            at = @At(
                    value = "RETURN",
                    ordinal = 0
            )
    )
    private void throwSeedQueueException(CallbackInfo ci) throws SeedQueueException {
        if (SeedQueue.inQueue()) {
            throw new SeedQueueException();
        }
    }
}
