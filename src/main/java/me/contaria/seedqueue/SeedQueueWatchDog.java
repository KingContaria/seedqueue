package me.contaria.seedqueue;

import me.contaria.seedqueue.mixin.accessor.MinecraftClientAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

import java.util.Arrays;

public class SeedQueueWatchDog implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        if (SeedQueue.config.useWatchdog) {
            Thread watchDog = new Thread(() -> {

                long last = 0;
                while (true) {
                    if (System.currentTimeMillis() > last + 10000) {
                        Thread mainThread = MinecraftClient.getInstance() != null ? ((MinecraftClientAccessor) MinecraftClient.getInstance()).seedQueue$getThread() : null;
                        if (mainThread != null) {
                            SeedQueue.LOGGER.info("WATCHDOG | " + Arrays.toString(mainThread.getStackTrace()));
                        }
                        last = System.currentTimeMillis();
                    }
                }
            });
            watchDog.setDaemon(true);
            watchDog.setName("SeedQueue WatchDog");
            watchDog.start();
        }
    }
}
