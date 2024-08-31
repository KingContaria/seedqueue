package me.contaria.seedqueue.mixin.server;

import me.contaria.seedqueue.interfaces.SQProgressLogger;
import net.minecraft.server.WorldGenerationProgressLogger;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(WorldGenerationProgressLogger.class)
public abstract class WorldGenerationProgressLoggerMixin implements SQProgressLogger {
    @Shadow
    @Final
    private int totalCount;

    @Unique
    private final HashMap<Long, Boolean> map = new HashMap<>(this.totalCount);

    @Inject(method = "setChunkStatus", at = @At("TAIL"))
    private void addToMap(ChunkPos pos, ChunkStatus status, CallbackInfo ci) {
        if (status == ChunkStatus.FULL) {
            map.put(pos.toLong(), true);
        }
    }

    public int seedqueue$getProgressPercentage(ChunkPos center) {
        int count = 1;
        int level = 0;
        boolean end = false;
        while (!end) {
            level++;
            for (int x = -level; x <= level; ++x) {
                if (Math.abs(x) == level) {
                    for (int z = -level; z <= level; ++z) {
                        if (this.getChunk(center.x + x, center.z + z)) count++;
                        else end = true;
                    }
                } else {
                    if (this.getChunk(center.x + x, center.z - level)) count++;
                    else end = true;
                    if (this.getChunk(center.x + x, center.z + level)) count++;
                    else end = true;
                }
            }
        }
        // kinda unsure where this constant came from
        // return (int) ((double) count / 241 * 100);
        return count * 100 / this.totalCount;

    }

    @Unique
    private boolean getChunk(int x, int z) {
        return map.containsKey(new ChunkPos(x, z).toLong());
    }
}
