package me.contaria.seedqueue.mixin.compat.sodium;

import me.contaria.seedqueue.compat.WorldPreviewCompat;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public abstract class SodiumWorldRendererMixin {

    @Inject(
            method = "scheduleRebuildForChunk",
            at = @At("HEAD"),
            cancellable = true
    )
    private void captureScheduledChunkRebuildsFromServer(int x, int y, int z, boolean important, CallbackInfo ci) {
        WorldPreviewProperties wpProperties = WorldPreviewCompat.SERVER_WP_PROPERTIES.get();
        if (wpProperties != null) {
            wpProperties.scheduleChunkRender(x, y, z, important);
            ci.cancel();
        }
    }
}
