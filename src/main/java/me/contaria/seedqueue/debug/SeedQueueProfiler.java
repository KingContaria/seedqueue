package me.contaria.seedqueue.debug;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueConfig;
import net.minecraft.client.MinecraftClient;

/**
 * Provides methods to profile rendering on the Wall Screen without affecting the profiler during vanilla gameplay.
 * It will only profile when called from the Render Thread while being on the Wall Screen with {@link SeedQueueConfig#showDebugMenu} enabled.
 */
public class SeedQueueProfiler {
    public static void push(String location) {
        if (shouldProfile()) {
            MinecraftClient.getInstance().getProfiler().push(location);
        }
    }

    public static void swap(String location) {
        if (shouldProfile()) {
            MinecraftClient.getInstance().getProfiler().swap(location);
        }
    }

    public static void pop() {
        if (shouldProfile()) {
            MinecraftClient.getInstance().getProfiler().pop();
        }
    }

    private static boolean shouldProfile() {
        return MinecraftClient.getInstance().isOnThread() && SeedQueue.isOnWall() && SeedQueue.config.showDebugMenu;
    }
}
