package me.contaria.seedqueue;

import com.google.gson.JsonParseException;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.debug.SeedQueueSystemInfo;
import me.contaria.seedqueue.debug.SeedQueueWatchdog;
import me.contaria.seedqueue.gui.wall.SeedQueueWallScreen;
import me.contaria.seedqueue.mixin.accessor.MinecraftClientAccessor;
import me.contaria.seedqueue.sounds.SeedQueueSounds;
import me.voidxwalker.autoreset.AttemptTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

public class SeedQueue implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    private static final Version VERSION = FabricLoader.getInstance().getModContainer("seedqueue").orElseThrow(IllegalStateException::new).getMetadata().getVersion();
    private static final Object LOCK = new Object();

    public static SeedQueueConfig config;

    public static AttemptTracker.Type BENCHMARK_RESETS = new AttemptTracker.Type("Benchmark Reset #", "benchmark-resets.txt");

    private static final Queue<Runnable> CLIENT_TASKS = new LinkedBlockingQueue<>();

    private static final Queue<SeedQueueEntry> SEED_QUEUE = new LinkedBlockingQueue<>();
    private static SeedQueueThread thread;

    public static final ThreadLocal<SeedQueueEntry> LOCAL_ENTRY = new ThreadLocal<>();
    public static SeedQueueEntry currentEntry;
    public static SeedQueueEntry selectedEntry;

    @Override
    public void onInitializeClient() {
        SeedQueueSounds.init();
    }

    /**
     * Polls a new {@link SeedQueueEntry} from the queue.
     * If {@link SeedQueue#selectedEntry} is not null, it will pull that entry from the queue.
     *
     * @return True if a new {@link SeedQueueEntry} was successfully loaded.
     */
    public static boolean loadEntry() {
        if (!MinecraftClient.getInstance().isOnThread()) {
            throw new RuntimeException("Tried to load a SeedQueueEntry off-thread!");
        }
        synchronized (LOCK) {
            if (selectedEntry != null) {
                currentEntry = selectedEntry;
                if (!SEED_QUEUE.remove(selectedEntry)) {
                    throw new IllegalStateException("SeedQueue selectedEntry is not part of the queue!");
                }
                selectedEntry = null;
            } else {
                currentEntry = SEED_QUEUE.poll();
            }
        }
        ping();
        return currentEntry != null;
    }

    /**
     * Clears {@link SeedQueue#currentEntry}.
     */
    public static void clearCurrentEntry() {
        synchronized (LOCK) {
            currentEntry = null;
        }
    }

    /**
     * Traverses the queue in order and returns the first {@link SeedQueueEntry} matching the {@link Predicate}.
     * This method will return the first match and will not test any further entries.
     *
     * @return A {@link SeedQueueEntry} from the queue matching the given predicate.
     */
    public static Optional<SeedQueueEntry> getEntryMatching(Predicate<SeedQueueEntry> predicate) {
        for (SeedQueueEntry entry : SEED_QUEUE) {
            if (predicate.test(entry)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Adds the given {@link SeedQueueEntry} to the queue.
     */
    public static void add(SeedQueueEntry entry) {
        synchronized (LOCK) {
            if (SEED_QUEUE.contains(entry)) {
                throw new IllegalArgumentException("Tried to add a SeedQueueEntry that is already in queue!");
            }
            SEED_QUEUE.add(Objects.requireNonNull(entry));
        }
        ping();
    }

    /**
     * Discards the given {@link SeedQueueEntry} and removes it from the queue.
     */
    public static void discard(SeedQueueEntry entry) {
        entry.discard();
        synchronized (LOCK) {
            if (!SEED_QUEUE.remove(entry)) {
                throw new IllegalArgumentException("Tried to discard a SeedQueueEntry that is not currently in queue!");
            }
        }
        ping();
    }

    /**
     * @return If the {@link SeedQueueThread} should launch another {@link SeedQueueEntry}.
     */
    public static boolean shouldGenerate() {
        synchronized (LOCK) {
            return getGeneratingCount() < getMaxGeneratingCount() && !isFull();
        }
    }

    /**
     * @return If the queue is filled to capacity.
     * @see SeedQueueConfig#maxCapacity
     */
    public static boolean isFull() {
        return SEED_QUEUE.size() >= config.maxCapacity;
    }

    /**
     * @return If all {@link SeedQueueEntry} have reached the {@link SeedQueueConfig#maxWorldGenerationPercentage}.
     */
    public static boolean allMaxWorldGenerationReached() {
        for (SeedQueueEntry entry: SEED_QUEUE) {
            if (!entry.isMaxWorldGenerationReached() && !entry.isLocked()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return If all currently generating {@link SeedQueueEntry} are not locked.
     */
    public static boolean noLockedRemaining() {
        for (SeedQueueEntry entry: SEED_QUEUE) {
            if (entry.isLocked() && !entry.isReady()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return If the {@link SeedQueueThread} should unpause a {@link SeedQueueEntry} that was previously scheduled to pause.
     */
    public static boolean shouldResumeGenerating() {
        synchronized (LOCK) {
            return getGeneratingCount() < getMaxGeneratingCount();
        }
    }

    /**
     * @return If the {@link SeedQueueThread} should unpause a {@link SeedQueueEntry} that was previously scheduled to pause
     * (after the queue is filled).
     * @see SeedQueue#isFull()
     * @see SeedQueue#allMaxWorldGenerationReached()
     */
    public static boolean shouldResumeAfterQueueFull() {
       synchronized (LOCK) {
           return config.resumeOnFilledQueue && isFull() && allMaxWorldGenerationReached();
       }
    }


    /**
     * @return If the {@link SeedQueueThread} should actively schedule a {@link SeedQueueEntry} to be paused.
     */
    public static boolean shouldPauseGenerating() {
        synchronized (LOCK) {
            return getGeneratingCount(true) > getMaxGeneratingCount();
        }
    }

    /**
     * @return The amount of currently generating seeds in the queue.
     */
    private static long getGeneratingCount() {
        return getGeneratingCount(false);
    }

    /**
     * Counts the amount of {@link SeedQueueEntry}'s in the queue that are currently unpaused.
     * If the Wall Screen is disabled it will also count the main server if it is still generating.
     *
     * @param treatScheduledAsPaused If {@link SeedQueueEntry}'s that are scheduled to pause but haven't been paused yet should be added to the count.
     * @return The amount of currently generating / unpaused {@link SeedQueueEntry}'s in queue.
     *
     * @see SeedQueueConfig#shouldUseWall
     */
    private static long getGeneratingCount(boolean treatScheduledAsPaused) {
        long count = 0;
        for (SeedQueueEntry entry: SEED_QUEUE) {
            if (!(entry.isPaused() || (treatScheduledAsPaused && entry.isScheduledToPause()))) {
                count ++;
            }
        }

        // add 1 when not using wall and the main world is currently generating
        MinecraftServer currentServer = MinecraftClient.getInstance().getServer();
        if (!SeedQueue.config.shouldUseWall() && (currentServer == null || !currentServer.isLoading())) {
            count++;
        }
        return count;
    }

    /**
     * @return The maximum number of {@link SeedQueueEntry}'s that should be generating concurrently.
     *
     * @see SeedQueueConfig#maxConcurrently
     * @see SeedQueueConfig#maxConcurrently_onWall
     */
    private static int getMaxGeneratingCount() {
        return isOnWall() ? config.maxConcurrently_onWall : config.maxConcurrently;
    }

    /**
     * Starts a new SeedQueue session, launching a new {@link SeedQueueThread}.
     * <p>
     * This method may only be called from the Render Thread and when SeedQueue is not currently active!
     */
    public static void start() {
        if (!MinecraftClient.getInstance().isOnThread()) {
            throw new RuntimeException("Tried to start SeedQueue off-thread!");
        }

        synchronized (LOCK) {
            if (thread != null) {
                throw new IllegalStateException("Tried to start SeedQueue but a queue is already active!");
            }

            if (!shouldStart()) {
                return;
            }

            LOGGER.info("Reloading SeedQueue Config...");
            try {
                SeedQueue.config.reload();
            } catch (IOException | JsonParseException e) {
                LOGGER.error("Failed to reload SeedQueue Config!", e);
            }
            SeedQueue.config.simulatedWindowSize.init();
            SeedQueueSystemInfo.logConfigSettings();

            LOGGER.info("Starting SeedQueue...");
            thread = new SeedQueueThread();
            thread.start();

            SeedQueueWatchdog.start();
        }
    }

    private static boolean shouldStart() {
        return config.maxCapacity > 0 && (config.maxConcurrently > 0 || config.shouldUseWall());
    }

    /**
     * If SeedQueue is active, stops the current SeedQueue session.
     * This stops the active {@link SeedQueueThread} and clears the queue and any caches.
     * <p>
     * This method may only be called from the Render Thread!
     */
    public static void stop() {
        if (!MinecraftClient.getInstance().isOnThread()) {
            throw new RuntimeException("Tried to stop SeedQueue off-thread!");
        }

        if (thread == null) {
            return;
        }

        LOGGER.info("Stopping SeedQueue...");

        thread.stopQueue();
        thread.ping();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to stop SeedQueue Thread!", e);
        }
        thread = null;

        SeedQueueWatchdog.stop();

        clear();
    }

    /**
     * Clears the queue and discards all the active {@link SeedQueueEntry}'s.
     * Also clears any other resources like caches and the Wall Screen WorldRenderers.
     */
    private static void clear() {
        LOGGER.info("Clearing SeedQueue...");

        Screen screen = MinecraftClient.getInstance().currentScreen;
        MinecraftClient.getInstance().setScreenAndRender(new SaveLevelScreen(new TranslatableText("seedqueue.menu.clearing")));

        synchronized (LOCK) {
            if (currentEntry != null && !currentEntry.isLoaded()) {
                currentEntry.discard();
            }
            currentEntry = null;
            selectedEntry = null;
        }

        SEED_QUEUE.forEach(SeedQueueEntry::discard);

        while (!SEED_QUEUE.isEmpty()) {
            ((MinecraftClientAccessor) MinecraftClient.getInstance()).seedQueue$render(false);
            SEED_QUEUE.removeIf(entry -> entry.getServer().isStopping());
        }

        SeedQueueExecutorWrapper.shutdownExecutors();
        SeedQueueWallScreen.clearWorldRenderers();
        ModCompat.worldpreview$clearFramebufferPool();
        ModCompat.sodium$clearShaderCache();
        ModCompat.sodium$clearBuildBufferPool();
        System.gc();

        MinecraftClient.getInstance().openScreen(screen);
    }

    /**
     * @return True if there is a running {@link SeedQueueThread}.
     */
    public static boolean isActive() {
        return thread != null;
    }

    /**
     * @return True if currently on the {@link SeedQueueThread}.
     */
    public static boolean inQueue() {
        return Thread.currentThread() instanceof SeedQueueThread;
    }

    /**
     * @return True if currently on the Wall Screen.
     */
    public static boolean isOnWall() {
        return MinecraftClient.getInstance().currentScreen instanceof SeedQueueWallScreen;
    }

    /**
     * @return True if currently running a benchmark on the Wall Screen.
     */
    public static boolean isBenchmarking() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return screen instanceof SeedQueueWallScreen && ((SeedQueueWallScreen) screen).isBenchmarking();
    }

    /**
     * @return The {@link SeedQueueEntry} corresponding to the calling server thread. Returns {@link Optional#empty()} if called before the server has created it's {@link WorldGenerationProgressTracker} because that's when the field is set!
     */
    public static Optional<SeedQueueEntry> getThreadLocalEntry() {
        return Optional.ofNullable(LOCAL_ENTRY.get());
    }

    /**
     * Pings the currently active {@link SeedQueueThread}.
     */
    public static void ping() {
        SeedQueueThread thread = SeedQueue.thread;
        if (thread != null) {
            thread.ping();
        }
    }

    /**
     * @return A mutable copy of the queue.
     */
    public static List<SeedQueueEntry> getEntries() {
        return new ArrayList<>(SEED_QUEUE);
    }

    /**
     * @return The currently active {@link SeedQueueThread}.
     */
    public static SeedQueueThread getThread() {
        return thread;
    }

    /**
     * Schedules the given {@link Runnable} to be run on the client thread.
     * Should be used in favor of {@link net.minecraft.util.thread.ThreadExecutor#execute} on the {@link MinecraftClient} because tasks submitted that way may be cancelled.
     */
    public static void scheduleTaskOnClientThread(Runnable runnable) {
        CLIENT_TASKS.add(runnable);
    }

    public static void runClientThreadTasks() {
        Runnable runnable;
        while ((runnable = CLIENT_TASKS.poll()) != null) {
            runnable.run();
        }
    }

    /**
     * @return A {@link List} of debug information to add to the F3 screen when SeedQueue is active.
     */
    public static List<String> getDebugText() {
        List<String> debugText = new ArrayList<>();
        debugText.add("");
        debugText.add("SeedQueue v" + VERSION.getFriendlyString());
        debugText.add(String.join(", ",
                "E: " + SEED_QUEUE.size() + "/" + SeedQueue.config.maxCapacity,
                "C: " + SeedQueue.config.maxConcurrently
                        + (SeedQueue.config.shouldUseWall() ? " | " + SeedQueue.config.maxConcurrently_onWall : ""),
                "W: " + (SeedQueue.config.backgroundExecutorThreads == SeedQueueConfig.AUTO ? "Auto" : SeedQueue.config.backgroundExecutorThreads)
                        + (SeedQueue.config.shouldUseWall() ? " | " + (SeedQueue.config.wallExecutorThreads == SeedQueueConfig.AUTO ? "Auto" : SeedQueue.config.wallExecutorThreads) : ""),
                "M: " + SeedQueue.config.maxWorldGenerationPercentage + "%"
        ));
        return debugText;
    }
}
