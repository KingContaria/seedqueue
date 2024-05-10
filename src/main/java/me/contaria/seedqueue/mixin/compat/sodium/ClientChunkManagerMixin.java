package me.contaria.seedqueue.mixin.compat.sodium;

import com.bawnorton.mixinsquared.TargetHandler;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.contaria.seedqueue.compat.WorldPreviewCompat;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.interfaces.SQClientChunkManager;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientChunkManager.class, priority = 1500)
public abstract class ClientChunkManagerMixin implements SQClientChunkManager {

    // I am unsure if shadowing other mods added fields is supported,
    // but since MixinSquared doesn't provide an alternative we roll with it
    // see: https://github.com/Bawnorton/MixinSquared/issues/4
    @SuppressWarnings("MixinAnnotationTarget")
    @Dynamic
    @Shadow
    private ChunkStatusListener listener;

    @SuppressWarnings("MixinAnnotationTarget")
    @Dynamic
    @Shadow
    @Final
    private LongOpenHashSet loadedChunks;

    @SuppressWarnings("CancellableInjectionUsage") // it seems MCDev gets confused because there is two CallbackInfos
    @Dynamic
    @TargetHandler(
            mixin = "me.jellysquid.mods.sodium.mixin.features.chunk_rendering.MixinClientChunkManager",
            name = "afterLoadChunkFromPacket"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    private synchronized void doNotUpdateSodiumChunksFromServer(int x, int z, BiomeArray biomes, PacketByteBuf buf, CompoundTag tag, int verticalStripBitmask, boolean complete, CallbackInfoReturnable<WorldChunk> cir, CallbackInfo ci) {
        WorldPreviewProperties wpProperties = WorldPreviewCompat.SERVER_WP_PROPERTIES.get();
        if (wpProperties != null) {
            wpProperties.addChunk(x, z);
            ci.cancel();
        }
    }

    // see MixinClientChunkManager#afterLoadChunkFromPacket
    // if MixinSquared provided a way to shadow handler methods, that could be used instead
    @Override
    public void seedQueue$addChunkToSodiumListener(int x, int z) {
        this.listener.onChunkAdded(x, z);
        this.loadedChunks.add(ChunkPos.toLong(x, z));
    }
}
