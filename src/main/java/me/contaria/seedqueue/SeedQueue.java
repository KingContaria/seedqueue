package me.contaria.seedqueue;

import me.contaria.seedqueue.gui.SeedQueueClearScreen;
import me.contaria.seedqueue.gui.wall.SeedQueueWallScreen;
import me.contaria.seedqueue.mixin.accessor.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

public class SeedQueue {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final Object LOCK = new Object();
    public static final Queue<SeedQueueEntry> SEED_QUEUE = new LinkedBlockingQueue<>();
    public static SeedQueueEntry currentEntry;
    public static SeedQueueEntry selectedEntry;
    public static SeedQueueConfig config;
    public static SeedQueueThread thread;

    public static boolean loadEntry() {
        synchronized (LOCK) {
            if (selectedEntry != null) {
                currentEntry = selectedEntry;
                remove(selectedEntry);
                selectedEntry = null;
            } else {
                currentEntry = SEED_QUEUE.poll();
            }
        }
        if (thread != null) {
            thread.ping();
        }
        return currentEntry != null;
    }

    public static Optional<SeedQueueEntry> getEntryMatching(Predicate<SeedQueueEntry> predicate) {
        synchronized (LOCK) {
            return SEED_QUEUE.stream().filter(predicate).findFirst();
        }
    }

    public static void add(SeedQueueEntry seedQueueEntry) {
        synchronized (LOCK) {
            SEED_QUEUE.add(seedQueueEntry);
        }
        thread.ping();
    }

    public static boolean remove(SeedQueueEntry seedQueueEntry) {
        boolean result;
        synchronized (LOCK) {
            result = SEED_QUEUE.remove(seedQueueEntry);
        }
        thread.ping();
        return result;
    }

    public static boolean shouldGenerate() {
        synchronized (LOCK) {
            return getGeneratingCount() < getMaxGeneratingCount() && SEED_QUEUE.size() < config.maxCapacity;
        }
    }

    public static boolean shouldStopGenerating() {
        synchronized (LOCK) {
            return getGeneratingCount() > getMaxGeneratingCount();
        }
    }

    private static long getGeneratingCount() {
        long count = SEED_QUEUE.stream().filter(entry -> !entry.isPaused()).count();

        // add 1 when not using wall and the main world is currently generating
        MinecraftServer currentServer = MinecraftClient.getInstance().getServer();
        if (!SeedQueue.config.shouldUseWall() && (currentServer == null || !currentServer.isLoading())) {
            count++;
        }

        return count;
    }

    private static int getMaxGeneratingCount() {
        return isOnWall() ? config.maxConcurrently_onWall : config.maxConcurrently;
    }

    public static void start() {
        synchronized (LOCK) {
            if (thread != null) {
                throw new IllegalStateException("Tried to start SeedQueue but a queue is already active!");
            }

            if (config.maxCapacity == 0) {
                return;
            }

            LOGGER.info("Starting SeedQueue...");

            thread = new SeedQueueThread();
            thread.start();
        }
    }

    public static void stop() {
        if (thread == null) {
            return;
        }

        LOGGER.info("Stopping SeedQueue...");

        thread.stopQueue();
        thread.ping();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new SeedQueueException();
        }
        thread = null;

        clear();
    }

    private static void clear() {
        LOGGER.info("Clearing SeedQueue...");

        SeedQueueClearScreen seedQueueClearScreen = new SeedQueueClearScreen(MinecraftClient.getInstance().currentScreen);
        MinecraftClient.getInstance().setScreenAndRender(seedQueueClearScreen);

        currentEntry = null;
        selectedEntry = null;

        SEED_QUEUE.forEach(SeedQueueEntry::discard);

        while (!SEED_QUEUE.isEmpty()) {
            ((MinecraftClientAccessor) MinecraftClient.getInstance()).seedQueue$render(false);
            SEED_QUEUE.removeIf(entry -> entry.getServer().isStopping());
        }

        SeedQueueExecutorWrapper.shutdownExecutors();

        SeedQueueWallScreen.clearWorldRenderers();

        System.gc();

        seedQueueClearScreen.onClose();
    }

    public static boolean isActive() {
        return thread != null;
    }

    public static boolean inQueue() {
        return Thread.currentThread() instanceof SeedQueueThread;
    }

    public static boolean isOnWall() {
        return MinecraftClient.getInstance().currentScreen instanceof SeedQueueWallScreen;
    }

    public static @Nullable SeedQueueEntry getEntry(MinecraftServer server) {
        synchronized (LOCK) {
            for (SeedQueueEntry entry : SEED_QUEUE) {
                if (server == entry.getServer()) {
                    return entry;
                }
            }
            return null;
        }
    }

    public static @Nullable SeedQueueEntry getEntry(Thread serverThread) {
        synchronized (LOCK) {
            for (SeedQueueEntry entry : SEED_QUEUE) {
                if (serverThread == entry.getServer().getThread()) {
                    return entry;
                }
            }
            return null;
        }
    }
}
