package me.contaria.seedqueue.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.contaria.seedqueue.interfaces.SQWorldGenerationProgressLogger;
import net.minecraft.server.WorldGenerationProgressLogger;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldGenerationProgressLogger.class)
public abstract class WorldGenerationProgressLoggerMixin implements SQWorldGenerationProgressLogger {

    @Unique
    private boolean muted;

    @WrapWithCondition(
            method = "setChunkStatus",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;)V",
                    remap = false
            )
    )
    private boolean muteWorldGenProgress_inQueue(Logger logger, String s) {
        return !this.muted;
    }

    @WrapWithCondition(
            method = "stop",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V",
                    remap = false
            )
    )
    private boolean muteWorldGenProgress_inQueue(Logger logger, String s, Object o) {
        return !this.muted;
    }

    @Override
    public void seedQueue$mute() {
        this.muted = true;
    }

    @Override
    public void seedQueue$unmute() {
        this.muted = false;
    }
}
