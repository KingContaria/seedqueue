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
                try {
                    while (true) {
                        Thread mainThread = MinecraftClient.getInstance() != null ? ((MinecraftClientAccessor) MinecraftClient.getInstance()).seedQueue$getThread() : null;
                        if (mainThread != null) {
                            SeedQueue.LOGGER.info("WATCHDOG | " + Arrays.toString(mainThread.getStackTrace()));
                        }
                        //noinspection BusyWait
                        Thread.sleep(10000);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            watchDog.setDaemon(true);
            watchDog.setPriority(3);
            watchDog.setName("SeedQueue WatchDog");
            watchDog.start();
        }
    }
}
