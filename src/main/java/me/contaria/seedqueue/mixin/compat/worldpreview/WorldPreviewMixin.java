package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.SeedQueuePreviewProperties;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import me.contaria.speedrunapi.config.SpeedrunConfigAPI;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.Queue;

@Mixin(WorldPreview.class)
public abstract class WorldPreviewMixin {

    @WrapWithCondition(
            method = "configure",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/data/DataTracker;set(Lnet/minecraft/entity/data/TrackedData;Ljava/lang/Object;)V"
            )
    )
    private static boolean doNotSetPlayerModelParts_inQueue(DataTracker tracker, TrackedData<?> key, Object object, ServerWorld serverWorld) {
        return !((SQMinecraftServer) serverWorld.getServer()).seedQueue$inQueue();
    }

    @ModifyExpressionValue(
            method = "configure",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/options/GameOptions;perspective:I"
            )
    )
    private static int modifyPerspective_inQueue(int perspective, ServerWorld serverWorld) {
        if (((SQMinecraftServer) serverWorld.getServer()).seedQueue$inQueue()) {
            return (int) SpeedrunConfigAPI.getConfigValueOptionally("standardsettings", "perspective").orElse(0);
        }
        return perspective;
    }

    @WrapOperation(
            method = "configure",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;set(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/client/network/ClientPlayerInteractionManager;Lnet/minecraft/client/render/Camera;Ljava/util/Queue;)V"
            )
    )
    private static void doNotConfigureWorldPreview_inQueue(ClientWorld world, ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager, Camera camera, Queue<Packet<?>> packetQueue, Operation<Void> original, ServerWorld serverWorld) {
        Optional<SeedQueueEntry> entry = ((SQMinecraftServer) serverWorld.getServer()).seedQueue$getEntry();
        if (entry.isPresent()) {
            entry.get().setPreviewProperties(new SeedQueuePreviewProperties(world, player, interactionManager, camera, packetQueue));
            return;
        }
        original.call(world, player, interactionManager, camera, packetQueue);
    }
}
