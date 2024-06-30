package me.contaria.seedqueue.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin extends MinecraftServerMixin {

    public IntegratedServerMixin(String string) {
        super(string);
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;getNetworkHandler()Lnet/minecraft/client/network/ClientPlayNetworkHandler;"
            )
    )
    private ClientPlayNetworkHandler doNotPauseBackgroundWorlds(ClientPlayNetworkHandler networkHandler) {
        if (this.getEntry() != null) {
            return null;
        }
        return networkHandler;
    }
}
