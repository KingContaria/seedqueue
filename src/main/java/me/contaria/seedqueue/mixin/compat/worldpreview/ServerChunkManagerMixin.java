package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.Option;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

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
        if (!this.isActiveServer()) {
            return SeedQueue.config.simulatedWindowSize.width();
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
        if (!this.isActiveServer()) {
            return SeedQueue.config.simulatedWindowSize.height();
        }
        return height;
    }

    @Unique
    private Optional<WorldPreviewProperties> getWorldPreviewProperties() {
        return SeedQueue.getEntryOrCurrentEntry(this.world.getServer()).filter(entry -> !(SeedQueue.config.freezeLockedPreviews && entry.isLocked())).map(SeedQueueEntry::getWorldPreviewProperties);
    }

    @Unique
    private boolean isActiveServer() {
        return this.world.getServer() == MinecraftClient.getInstance().getServer();
    }
}
