package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerWorld;
import org.mcsr.speedrunapi.config.SpeedrunConfigAPI;
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
            entry.get().setWorldPreviewProperties(new WorldPreviewProperties(world, player, interactionManager, camera, packetQueue));
            return;
        }
        original.call(world, player, interactionManager, camera, packetQueue);
    }

    // can be replaced by an expression of WorldPreviewConfig#dataLimit <= 100 when MixinExtras adds expressions
    @ModifyExpressionValue(
            method = "shouldStopAtPacket",
            at = @At(
                    value = "CONSTANT",
                    args = "intValue=100"
            )
    )
    private static int doNotAllowUnlimitedPackets_onWall(int unlimitedPackets) {
        if (SeedQueue.isOnWall()) {
            // ensures there is always a packet limit enforced when on the wall screen
            // not having a limit causes freezes when opening the wall screen
            // since new SeedQueueEntry's would load all their stored packets at once
            return Integer.MAX_VALUE;
        }
        return unlimitedPackets;
    }
}
