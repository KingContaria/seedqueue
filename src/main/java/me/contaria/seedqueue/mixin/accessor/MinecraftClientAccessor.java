package me.contaria.seedqueue.mixin.accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("worldGenProgressTracker")
    AtomicReference<WorldGenerationProgressTracker> seedQueue$getWorldGenProgressTracker();

    @Invoker("render")
    void seedQueue$render(boolean tick);

    @Accessor("thread")
    Thread seedQueue$getThread();
}
