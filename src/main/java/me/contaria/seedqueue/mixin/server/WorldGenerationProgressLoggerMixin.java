package me.contaria.seedqueue.mixin.server;

import me.contaria.seedqueue.interfaces.SQProgressLogger;
import net.minecraft.server.WorldGenerationProgressLogger;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkStatus;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Inject(method = "setChunkStatus", at = @At(value = "FIELD", target = "Lnet/minecraft/server/WorldGenerationProgressLogger;generatedCount:I", opcode = Opcodes.PUTFIELD))
    private void addToMap(ChunkPos pos, ChunkStatus status, CallbackInfo ci) {
        map.put(pos.toLong(), true);
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

        return MathHelper.clamp(count * 100 / this.totalCount, 0, 100);
    }

    @Unique
    private boolean getChunk(int x, int z) {
        return map.containsKey(ChunkPos.toLong(x, z));
    }
}
