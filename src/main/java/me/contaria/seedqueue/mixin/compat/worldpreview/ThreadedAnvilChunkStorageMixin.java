package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import me.voidxwalker.worldpreview.WorldPreviewProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.Option;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(value = ThreadedAnvilChunkStorage.class, priority = 1500)
public abstract class ThreadedAnvilChunkStorageMixin {

    @Shadow
    @Final
    private ServerWorld world;

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ThreadedAnvilChunkStorageMixin",
            name = "worldpreview$sendData"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "FIELD",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;properties:Lme/voidxwalker/worldpreview/WorldPreviewProperties;",
                    remap = false
            )
    )
    private WorldPreviewProperties sendChunksToCorrectWorldPreview_inQueue(WorldPreviewProperties properties) {
        return this.getWorldPreviewProperties().orElse(this.isActiveServer() ? properties : null);
    }

    @Dynamic
    @TargetHandler(
            mixin = "me.voidxwalker.worldpreview.mixin.server.ThreadedAnvilChunkStorageMixin",
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
            mixin = "me.voidxwalker.worldpreview.mixin.server.ThreadedAnvilChunkStorageMixin",
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
            mixin = "me.voidxwalker.worldpreview.mixin.server.ThreadedAnvilChunkStorageMixin",
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
        return ((SQMinecraftServer) this.world.getServer()).seedQueue$getEntry().filter(entry -> !(SeedQueue.config.freezeLockedPreviews && entry.isLocked())).map(SeedQueueEntry::getPreviewProperties);
    }

    @Unique
    private boolean isActiveServer() {
        return this.world.getServer() == MinecraftClient.getInstance().getServer();
    }
}
