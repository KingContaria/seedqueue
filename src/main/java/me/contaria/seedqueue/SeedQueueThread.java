package me.contaria.seedqueue;

import me.voidxwalker.autoreset.AtumCreateWorldScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class SeedQueueThread extends Thread {

    private final Object lock = new Object();
    private volatile boolean running = true;

    public SeedQueueThread() {
        super("SeedQueue Thread");
        this.setPriority(SeedQueue.config.seedQueueThreadPriority);
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                if (SeedQueue.shouldStopGenerating()) {
                    this.pauseSeedQueueEntry();
                    continue;
                }
                synchronized (this.lock) {
                    if (!SeedQueue.shouldGenerate()) {
                        if (SeedQueue.shouldResumeGenerating() && this.unpauseSeedQueueEntry()) {
                            continue;
                        }
                        this.lock.wait();
                        continue;
                    }
                }

                if (this.unpauseSeedQueueEntry()) {
                    continue;
                }

                this.createSeedQueueEntry();
            } catch (Exception e) {
                SeedQueue.LOGGER.error("Shutting down SeedQueue Thread...", e);
                this.stopQueue();
            }
        }
    }

    private void pauseSeedQueueEntry() {
        SeedQueue.getEntryMatching(entry -> !entry.isScheduledToPause() && !entry.isPaused()).ifPresent(SeedQueueEntry::schedulePause);
    }

    private boolean unpauseSeedQueueEntry() {
        return SeedQueue.getEntryMatching(entry -> entry.isScheduledToPause() || (entry.isPaused() && !entry.shouldPause())).map(entry -> {
            entry.tryToUnpause();
            return true;
        }).orElse(false);
    }

    private void createSeedQueueEntry() {
        Screen atumCreateWorldScreen = new AtumCreateWorldScreen(null);
        atumCreateWorldScreen.init(MinecraftClient.getInstance(), 0, 0);
    }

    public void ping() {
        synchronized (this.lock) {
            this.lock.notify();
        }
    }

    public void stopQueue() {
        this.running = false;
    }
}
