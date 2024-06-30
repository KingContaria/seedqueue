package me.contaria.seedqueue;

import me.contaria.seedqueue.mixin.accessor.UtilAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class SeedQueueExecutorWrapper implements Executor {
    public static final Executor SEEDQUEUE_EXECUTOR = command -> getSeedqueueExecutor().execute(command);

    private static ExecutorService SEEDQUEUE_BACKGROUND_EXECUTOR;
    private static ExecutorService SEEDQUEUE_WALL_EXECUTOR;

    private final Executor originalExecutor;
    private Executor executor;

    public SeedQueueExecutorWrapper(Executor originalExecutor) {
        this.executor = this.originalExecutor = originalExecutor;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        this.executor.execute(command);
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void resetExecutor() {
        this.setExecutor(this.originalExecutor);
    }

    private static Executor getSeedqueueExecutor() {
        if (SeedQueue.isOnWall() || SeedQueue.config.maxConcurrently == 0) {
            return getWallExecutor();
        }
        return getBackgroundExecutor();
    }

    private static Executor getBackgroundExecutor() {
        if (SEEDQUEUE_BACKGROUND_EXECUTOR == null) {
            SEEDQUEUE_BACKGROUND_EXECUTOR = createBackgroundExecutor();
        }
        return SEEDQUEUE_BACKGROUND_EXECUTOR;
    }

    private static synchronized ExecutorService createBackgroundExecutor() {
        return SEEDQUEUE_BACKGROUND_EXECUTOR != null ? SEEDQUEUE_BACKGROUND_EXECUTOR : UtilAccessor.seedQueue$createWorkerExecutor("SeedQueue");
    }

    private static Executor getWallExecutor() {
        if (SEEDQUEUE_WALL_EXECUTOR == null) {
            SEEDQUEUE_WALL_EXECUTOR = createWallExecutor();
        }
        return SEEDQUEUE_WALL_EXECUTOR;
    }

    private static synchronized ExecutorService createWallExecutor() {
        return SEEDQUEUE_WALL_EXECUTOR != null ? SEEDQUEUE_WALL_EXECUTOR : UtilAccessor.seedQueue$createWorkerExecutor("SeedQueue Wall");
    }

    public static void shutdownExecutors() {
        if (SEEDQUEUE_BACKGROUND_EXECUTOR != null) {
            UtilAccessor.seedQueue$shutdownWorkerExecutor(SEEDQUEUE_BACKGROUND_EXECUTOR);
            SEEDQUEUE_BACKGROUND_EXECUTOR = null;
        }
        if (SEEDQUEUE_WALL_EXECUTOR != null) {
            UtilAccessor.seedQueue$shutdownWorkerExecutor(SEEDQUEUE_WALL_EXECUTOR);
            SEEDQUEUE_WALL_EXECUTOR = null;
        }
    }
}
