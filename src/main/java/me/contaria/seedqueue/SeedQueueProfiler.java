package me.contaria.seedqueue;

import net.minecraft.client.MinecraftClient;

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
