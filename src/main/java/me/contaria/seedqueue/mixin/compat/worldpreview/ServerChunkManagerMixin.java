package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.WorldPreviewCompat;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.Option;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Queue;

@Mixin(value = ServerChunkManager.class, priority = 1500)
public abstract class ServerChunkManagerMixin {

    @Shadow
    @Final
    private ServerWorld world;

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ServerChunkManagerMixin",
            name = "getChunks"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "FIELD",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;world:Lnet/minecraft/client/world/ClientWorld;"
            )
    )
    private ClientWorld sendChunksToCorrectWorldPreview_inQueue(ClientWorld world) {
        return this.getWorldPreviewProperties().map(WorldPreviewProperties::getWorld).orElse(this.isActiveServer() ? world : null);
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ServerChunkManagerMixin",
            name = "getChunks"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "FIELD",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;player:Lnet/minecraft/client/network/ClientPlayerEntity;"
            )
    )
    private ClientPlayerEntity sendChunksToCorrectWorldPreview_inQueue(ClientPlayerEntity player) {
        return this.getWorldPreviewProperties().map(WorldPreviewProperties::getPlayer).orElse(this.isActiveServer() ? player : null);
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ServerChunkManagerMixin",
            name = "getChunks"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "FIELD",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;camera:Lnet/minecraft/client/render/Camera;"
            )
    )
    private Camera sendChunksToCorrectWorldPreview_inQueue(Camera camera) {
        return this.getWorldPreviewProperties().map(WorldPreviewProperties::getCamera).orElse(this.isActiveServer() ? camera : null);
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ServerChunkManagerMixin",
            name = "getChunks"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "FIELD",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;packetQueue:Ljava/util/Queue;"
            ),
            remap = false
    )
    private Queue<Packet<?>> sendChunksToCorrectWorldPreview_inQueue(Queue<Packet<?>> packetQueue) {
        return this.getWorldPreviewProperties().map(WorldPreviewProperties::getPacketQueue).orElse(this.isActiveServer() ? packetQueue : null);
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ServerChunkManagerMixin",
            name = "updateFrustum"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/options/GameOptions;fov:D"
            )
    )
    private double modifyCullingFov_inQueue(double fov) {
        if (!this.isActiveServer()) {
            // trying to keep track of the FOV in the settings cache / standardsettings is unnecessarily complicated
            // the majority of people will use quake pro with their personal FOV as fovOnWorldJoin anyway
            return Option.FOV.getMax();
        }
        return fov;
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ServerChunkManagerMixin",
            name = "updateFrustum"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/Window;getFramebufferWidth()I"
            )
    )
    private int modifyCullingWindowWidth(int width) {
        if (!this.isActiveServer() && SeedQueue.config.hasSimulatedWindowSize()) {
            return SeedQueue.config.simulatedWindowWidth;
        }
        return width;
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ServerChunkManagerMixin",
            name = "updateFrustum"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/Window;getFramebufferHeight()I"
            )
    )
    private int modifyCullingWindowHeight(int height) {
        if (!this.isActiveServer() && SeedQueue.config.hasSimulatedWindowSize()) {
            return SeedQueue.config.simulatedWindowHeight;
        }
        return height;
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ServerChunkManagerMixin",
            name = "processChunk"
    )
    @ModifyArg(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "java/util/Queue.addAll(Ljava/util/Collection;)Z"
            )
    )
    private Collection<Packet<?>> evaluateChunkDataServerSide(Collection<Packet<?>> packets) {
        return this.evaluateDataServerSide(packets);
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ServerChunkManagerMixin",
            name = "processEntity"
    )
    @ModifyArg(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "java/util/Queue.addAll(Ljava/util/Collection;)Z"
            )
    )
    private Collection<Packet<?>> evaluateEntityDataServerSide(Collection<Packet<?>> packets) {
        return this.evaluateDataServerSide(packets);
    }

    @Unique
    private Collection<Packet<?>> evaluateDataServerSide(Collection<Packet<?>> packets) {
        if (!SeedQueue.config.evaluatePacketsServerSide) {
            return packets;
        }
        return this.getWorldPreviewProperties().map(wpProperties -> {
            ClientPlayNetworkHandler networkHandler = wpProperties.getPlayer().networkHandler;
            Collection<Packet<?>> missedPackets = new ArrayList<>();
            try {
                WorldPreviewCompat.SERVER_WP_PROPERTIES.set(wpProperties);
                for (Packet<?> packet : packets) {
                    try {
                        //noinspection unchecked
                        ((Packet<ClientPlayNetworkHandler>) packet).apply(networkHandler);
                    } catch (OffThreadException e) {
                        missedPackets.add(packet);
                    }
                }
            } finally {
                WorldPreviewCompat.SERVER_WP_PROPERTIES.remove();
            }
            if (!missedPackets.isEmpty()) {
                SeedQueue.LOGGER.warn("Failed to evaluate {} packets serverside", missedPackets.size());
            }
            return missedPackets;
        }).orElse(packets);
    }

    @Unique
    private Optional<WorldPreviewProperties> getWorldPreviewProperties() {
        return Optional.ofNullable(SeedQueue.getEntry(this.world.getServer())).filter(entry -> !(SeedQueue.config.freezeLockedPreviews && entry.isLocked())).map(SeedQueueEntry::getWorldPreviewProperties);
    }

    @Unique
    private boolean isActiveServer() {
        return this.world.getServer() == MinecraftClient.getInstance().getServer();
    }
}
