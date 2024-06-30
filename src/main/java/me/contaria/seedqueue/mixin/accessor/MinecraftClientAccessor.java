package me.contaria.seedqueue.mixin.accessor;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Invoker("render")
    void seedQueue$render(boolean tick);

    @Accessor("thread")
    Thread seedQueue$getThread();

    @Invoker("handleProfilerKeyPress")
    void seedQueue$handleProfilerKeyPress(int digit);
}
