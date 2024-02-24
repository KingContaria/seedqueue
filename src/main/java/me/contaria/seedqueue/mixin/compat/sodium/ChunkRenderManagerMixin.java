package me.contaria.seedqueue.mixin.compat.sodium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.SeedQueue;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ChunkRenderManager.class, remap = false)
public abstract class ChunkRenderManagerMixin {

    @ModifyExpressionValue(
            method = "isChunkPrioritized",
            at = @At(
                    value = "FIELD",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/ChunkRenderManager;NEARBY_CHUNK_DISTANCE:D"
            )
    )
    private double decreaseNearbyChunkDistanceOnWall(double nearbyChunkDistance) {
        if (SeedQueue.isOnWall()) {
            return 16 * 16;
        }
        return nearbyChunkDistance;
    }
}
