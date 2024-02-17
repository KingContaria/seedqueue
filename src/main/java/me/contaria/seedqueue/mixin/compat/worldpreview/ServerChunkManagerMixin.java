package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.Set;

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
    private ClientWorld sendChunksToCorrectWorldPreviewWorld_inQueue(ClientWorld world) {
        return this.getWorldPreviewProperties().map(WorldPreviewProperties::getWorld).orElse(this.world.getServer() == MinecraftClient.getInstance().getServer() ? world : null);
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
    private ClientPlayerEntity sendChunksToCorrectWorldPreviewWorld_inQueue(ClientPlayerEntity player) {
        return this.getWorldPreviewProperties().map(WorldPreviewProperties::getPlayer).orElse(this.world.getServer() == MinecraftClient.getInstance().getServer() ? player : null);
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
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;packetQueue:Ljava/util/Set;"
            ),
            remap = false
    )
    private Set<Packet<?>> sendChunksToCorrectWorldPreviewWorld_inQueue(Set<Packet<?>> packetQueue) {
        return this.getWorldPreviewProperties().map(WorldPreviewProperties::getPacketQueue).orElse(this.world.getServer() == MinecraftClient.getInstance().getServer() ? packetQueue : null);
    }

    @Unique
    private Optional<WorldPreviewProperties> getWorldPreviewProperties() {
        SeedQueueEntry entry = SeedQueue.getEntry(this.world.getServer());
        if (entry == null) {
            return Optional.empty();
        }
        WorldPreviewProperties worldPreviewProperties = entry.getWorldPreviewProperties();
        if (worldPreviewProperties == null) {
            return Optional.empty();
        }
        return Optional.of(worldPreviewProperties);
    }
}
