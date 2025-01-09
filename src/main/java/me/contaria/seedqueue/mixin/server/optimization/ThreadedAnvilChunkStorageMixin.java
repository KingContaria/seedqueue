package me.contaria.seedqueue.mixin.server.optimization;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin {

    @Shadow
    @Final
    private ServerWorld world;

    // careful with this, chunkcacher injects shortly after
    @WrapOperation(
            method = "getUpdatedChunkTag",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;getNbt(Lnet/minecraft/util/math/ChunkPos;)Lnet/minecraft/nbt/CompoundTag;"
            )
    )
    private CompoundTag skipGettingNbtInQueue(ThreadedAnvilChunkStorage storage, ChunkPos pos, Operation<CompoundTag> original) {
        // we can skip checking storage for chunk nbt while we're in queue since no chunks have been saved yet anyway
        if (((SQMinecraftServer) this.world.getServer()).seedQueue$inQueue()) {
            return null;
        }
        return original.call(storage, pos);
    }
}
