package me.contaria.seedqueue.mixin.compat.starlight;

import ca.spottedleaf.starlight.common.light.StarLightEngine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.compat.WorldPreviewCompat;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StarLightEngine.class)
public abstract class StarLightEngineMixin {

    @WrapOperation(
            method = "blocksChangedInChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lca/spottedleaf/starlight/common/light/StarLightEngine;handleEmptySectionChanges(Lnet/minecraft/world/chunk/ChunkProvider;Lnet/minecraft/world/chunk/Chunk;[Ljava/lang/Boolean;Z)[Z"
            )
    )
    private boolean[] forceHandleEmptySectionChangesOnServer(StarLightEngine lightEngine, ChunkProvider sectionIndex, Chunk dy, Boolean[] extrude, boolean dx, Operation<boolean[]> original) {
        if (ModCompat.HAS_WORLDPREVIEW && WorldPreviewCompat.SERVER_WP_PROPERTIES.get() != null) {
            lightEngine.forceHandleEmptySectionChanges(sectionIndex, dy, extrude);
        }
        return original.call(lightEngine, sectionIndex, dy, extrude, dx);
    }
}
