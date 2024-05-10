package me.contaria.seedqueue;

import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.gui.wall.SeedQueueWallScreen;
import me.contaria.seedqueue.mixin.accessor.MinecraftClientAccessor;
import me.contaria.seedqueue.sounds.SeedQueueSounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

public class SeedQueue implements ClientModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final Version VERSION = FabricLoader.getInstance().getModContainer("seedqueue").orElseThrow(IllegalStateException::new).getMetadata().getVersion();
    public static final Object LOCK = new Object();
    public static final Queue<SeedQueueEntry> SEED_QUEUE = new LinkedBlockingQueue<>();
    public static SeedQueueEntry currentEntry;
    public static SeedQueueEntry selectedEntry;
    public static SeedQueueConfig config;
    public static SeedQueueThread thread;

    @Override
    public void onInitializeClient() {
        SeedQueueSounds.init();

        if (config.useWatchdog) {
            Thread watchDog = new Thread(() -> {
                try {
                    while (true) {
                        Thread mainThread = MinecraftClient.getInstance() != null ? ((MinecraftClientAccessor) MinecraftClient.getInstance()).seedQueue$getThread() : null;
                        if (mainThread != null) {
                            LOGGER.info("WATCHDOG | " + Arrays.toString(mainThread.getStackTrace()));
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
        ping();
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
        ping();
    }

    public static boolean remove(SeedQueueEntry seedQueueEntry) {
        boolean result;
        synchronized (LOCK) {
            result = SEED_QUEUE.remove(seedQueueEntry);
        }
        ping();
        return result;
    }

    public static boolean shouldGenerate() {
        synchronized (LOCK) {
            return getGeneratingCount() < getMaxGeneratingCount() && SEED_QUEUE.size() < config.maxCapacity;
        }
    }

    public static boolean shouldResumeGenerating() {
        synchronized (LOCK) {
            return getGeneratingCount() < getMaxGeneratingCount();
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

        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;
        client.setScreenAndRender(new SaveLevelScreen(new TranslatableText("seedqueue.menu.clearing")));

        currentEntry = null;
        selectedEntry = null;

        SEED_QUEUE.forEach(SeedQueueEntry::discard);

        while (!SEED_QUEUE.isEmpty()) {
            ((MinecraftClientAccessor) client).seedQueue$render(false);
            SEED_QUEUE.removeIf(entry -> entry.getServer().isStopping());
        }

        SeedQueueExecutorWrapper.shutdownExecutors();
        SeedQueueWallScreen.clearWorldRenderers();
        ModCompat.sodium$clearShaderCache();
        ModCompat.sodium$clearBuildBufferPool();
        System.gc();

        client.openScreen(screen);
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
        if (MinecraftClient.getInstance().getServer() == server) {
            return null;
        }
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
        MinecraftServer server = MinecraftClient.getInstance().getServer();
        if (server != null && server.getThread() == serverThread) {
            return null;
        }
        synchronized (LOCK) {
            for (SeedQueueEntry entry : SEED_QUEUE) {
                if (serverThread == entry.getServer().getThread()) {
                    return entry;
                }
            }
            return null;
        }
    }

    public static void ping() {
        if (isActive()) {
            thread.ping();
        }
    }

    public static List<String> getDebugText() {
        List<String> debugText = new ArrayList<>();
        debugText.add("");
        debugText.add("SeedQueue v" + VERSION.getFriendlyString());
        debugText.add(String.join(", ",
                "E: " + SEED_QUEUE.size() + "/" + SeedQueue.config.maxCapacity,
                "C: " + SeedQueue.config.maxConcurrently + (SeedQueue.config.shouldUseWall() ? " | " + SeedQueue.config.maxConcurrently_onWall : ""),
                "W: " + SeedQueue.config.backgroundExecutorThreads + (SeedQueue.config.shouldUseWall() ? " | " + SeedQueue.config.wallExecutorThreads : ""),
                "M: " + SeedQueue.config.maxWorldGenerationPercentage + "%"
        ));
        return debugText;
    }
}
