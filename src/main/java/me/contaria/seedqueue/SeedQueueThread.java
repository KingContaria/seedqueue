package me.contaria.seedqueue;

import me.voidxwalker.autoreset.AtumCreateWorldScreen;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The thread responsible for launching new server threads and creating and queueing the corresponding {@link SeedQueueEntry}'s.
 * <p>
 * This thread is to be launched at the start of a SeedQueue session and to be closed by calling {@link SeedQueueThread#stopQueue}.
 */
public class SeedQueueThread extends Thread {
    private final Object lock = new Object();
    private final AtomicBoolean pinged = new AtomicBoolean();
    private volatile boolean running = true;

    SeedQueueThread() {
        super("SeedQueue Thread");
        this.setPriority(SeedQueue.config.seedQueueThreadPriority);
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                // clear pinged state when starting a new check
                this.pinged.set(false);

                if (SeedQueue.shouldPauseGenerating()) {
                    this.pauseSeedQueueEntry();
                    continue;
                }
                if (!SeedQueue.shouldGenerate()) {
                    if (SeedQueue.shouldResumeGenerating() && this.unpauseSeedQueueEntry()) {
                        continue;
                    }
                    synchronized (this.lock) {
                        // don't freeze thread if it's been pinged at any point during the check
                        if (this.pinged.get()) {
                            continue;
                        }
                        this.lock.wait();
                    }
                    continue;
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

    /**
     * Tries to find a currently unpaused {@link SeedQueueEntry} and schedules it to be paused.
     */
    private void pauseSeedQueueEntry() {
        // try to pause not locked entries first
        Optional<SeedQueueEntry> entry = SeedQueue.getEntryMatching(e -> !e.isLocked() && e.canPause());
        if (!entry.isPresent()) {
            entry = SeedQueue.getEntryMatching(SeedQueueEntry::canPause);
        }
        entry.ifPresent(SeedQueueEntry::schedulePause);
    }

    /**
     * Tries to find a currently paused {@link SeedQueueEntry} and schedules it to be unpaused.
     *
     * @return False if there is no entries to unpause.
     */
    private boolean unpauseSeedQueueEntry() {
        List<SeedQueueEntry> entries = SeedQueue.getEntries();
        // try to unpause locked entries first
        for (SeedQueueEntry entry : entries) {
            if (entry.isLocked() && entry.tryToUnpause()) {
                return true;
            }
        }
        for (SeedQueueEntry entry : entries) {
            if (entry.tryToUnpause()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new {@link SeedQueueEntry} and adds it to the queue.
     */
    private void createSeedQueueEntry() {
        new AtumCreateWorldScreen(null).init(MinecraftClient.getInstance(), 0, 0);
    }

    public void ping() {
        synchronized (this.lock) {
            this.pinged.set(true);
            this.lock.notify();
        }
    }

    /**
     * Stops this thread.
     * <p>
     * If this method is called from another thread, {@link SeedQueueThread#ping} has to be called AFTER.
     */
    public void stopQueue() {
        this.running = false;
    }
}
