package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = MinecraftServer.class, priority = 1500)
public abstract class MinecraftServerMixin {

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.MinecraftServerMixin",
            name = "configureWorldPreview"
    )
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;configure(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/client/network/ClientPlayerInteractionManager;Lnet/minecraft/client/render/Camera;Ljava/util/Set;)V"
            )
    )
    private void doNotConfigureWorldPreview_inQueue(ClientWorld world, ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager, Camera camera, Set<Packet<?>> packetQueue, Operation<Void> original) {
        SeedQueueEntry entry = SeedQueue.getEntry((MinecraftServer) (Object) this);
        if (entry != null) {
            entry.setWorldPreviewProperties(new WorldPreviewProperties(world, player, interactionManager, camera, packetQueue));
            return;
        }
        original.call(world, player, interactionManager, camera, packetQueue);
    }

    @Inject(
            method = "loadWorld",
            at = @At("TAIL")
    )
    private void tooLateToConfigureWorldPreview(CallbackInfo ci) {
        SeedQueueEntry entry = SeedQueue.getEntry((MinecraftServer) (Object) this);
        if (entry != null && !SeedQueue.config.shouldUseWall()) {
            entry.setWorldPreviewProperties(null);
        }
    }
}
