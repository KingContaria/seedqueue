package me.contaria.seedqueue;

import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import me.voidxwalker.autoreset.AtumCreateWorldScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.concurrent.Executor;

public class SeedQueueThread extends Thread {

    private volatile boolean running;

    public SeedQueueThread() {
        super("SeedQueue Thread");
    }

    @Override
    public void run() {
        this.running = true;
        while (this.running) {
            this.updateSeedQueueEntryExecutors();
            if (!SeedQueue.shouldGenerate()) {
                // TODO: implement a way to actually put the thread to sleep (preferably without race conditions that lock everything)
                // this is now harder to do because we update the executors on this thread :/
                Thread.yield();
                continue;
            }
            try {
                Screen atumCreateWorldScreen = new AtumCreateWorldScreen(null);
                atumCreateWorldScreen.init(MinecraftClient.getInstance(), 0, 0);
            } catch (Exception e) {
                SeedQueue.LOGGER.error("Shutting down SeedQueue Thread...", e);
                this.stopQueue();
            }
        }
    }

    private void updateSeedQueueEntryExecutors() {
        if (SeedQueue.config.backgroundExecutorMode == SeedQueueConfig.BackgroundExecutorMode.OFF) {
            return;
        }
        synchronized (SeedQueue.LOCK) {
            for (SeedQueueEntry entry : SeedQueue.SEED_QUEUE) {
                if (entry.isReady()) {
                    continue;
                }
                ((SQMinecraftServer) entry.getServer()).seedQueue$setExecutor(this.getExecutor(entry));
            }
        }
    }

    private Executor getExecutor(SeedQueueEntry entry) {
        if (SeedQueue.config.backgroundExecutorMode == SeedQueueConfig.BackgroundExecutorMode.SINGLE_EXECUTOR || !SeedQueue.isOnWall()) {
            return SeedQueueExecutorWrapper.getBackgroundExecutor();
        }
        if (entry.isLocked()) {
            return SeedQueueExecutorWrapper.getLockedExecutor();
        }
        if (entry.getWorldPreviewProperties() == null) {
            return SeedQueueExecutorWrapper.getBeforePreviewExecutor();
        }
        return SeedQueueExecutorWrapper.getAfterPreviewExecutor();
    }

    public void stopQueue() {
        this.running = false;
    }
}
