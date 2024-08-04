package me.contaria.seedqueue.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
        if (this.seedQueue$inQueue()) {
            return null;
        }
        return networkHandler;
    }

    @ModifyVariable(
            method = "tick",
            at = @At("STORE")
    )
    private int doNotChangeViewDistanceInQueue(int viewDistance) {
        if (this.seedQueue$inQueue()) {
            return this.getPlayerManager().getViewDistance();
        }
        return viewDistance;
    }
}
