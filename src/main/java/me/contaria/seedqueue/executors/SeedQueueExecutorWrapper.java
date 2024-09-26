package me.contaria.seedqueue.executors;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueConfig;
import me.contaria.seedqueue.mixin.accessor.UtilAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps the server executor to allow for using seperate executors while a server is generating in queue.
 * This allows for modifying thread priorities and parallelism of executors used while in queue to help manage performance.
 * <p>
 * The backing {@link ExecutorService}'s are created lazily and should be shut down after a SeedQueue session.
 * This should happen AFTER all servers have been shut down!
 *
 * @see SeedQueueConfig#backgroundExecutorThreadPriority
 * @see SeedQueueConfig#getBackgroundExecutorThreads
 * @see SeedQueueConfig#wallExecutorThreadPriority
 * @see SeedQueueConfig#getWallExecutorThreads
 */
public class SeedQueueExecutorWrapper implements Executor {
    /**
     * Executor used by servers while they are in queue.
     * Redirects to the backing executors depending on the current state.
     */
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
        // if Max Generating Seeds is set to 0 while not on wall,
        // this will ensure the background executor is never created
        if (SeedQueue.isOnWall() || SeedQueue.config.maxConcurrently == 0) {
            return getOrCreateWallExecutor();
        }
        return getOrCreateBackgroundExecutor();
    }

    private synchronized static Executor getOrCreateBackgroundExecutor() {
        if (SEEDQUEUE_BACKGROUND_EXECUTOR == null) {
            SEEDQUEUE_BACKGROUND_EXECUTOR = createExecutor("SeedQueue", SeedQueue.config.getBackgroundExecutorThreads(), SeedQueue.config.backgroundExecutorThreadPriority);
        }
        return SEEDQUEUE_BACKGROUND_EXECUTOR;
    }

    private synchronized static Executor getOrCreateWallExecutor() {
        if (SEEDQUEUE_WALL_EXECUTOR == null) {
            SEEDQUEUE_WALL_EXECUTOR = createExecutor("SeedQueue Wall", SeedQueue.config.getWallExecutorThreads(), SeedQueue.config.wallExecutorThreadPriority);
        }
        return SEEDQUEUE_WALL_EXECUTOR;
    }

    // see Util#createWorker
    private static ExecutorService createExecutor(String name, int threads, int priority) {
        AtomicInteger threadCount = new AtomicInteger();
        if (SeedQueue.config.usePriorityExecutors) {
            return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.SECONDS, new PriorityQueue(), runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("Worker-" + name + "-" + threadCount.getAndIncrement());
                thread.setPriority(priority);
                return thread;
            });
        }
        return new ForkJoinPool(threads, pool -> {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setName("Worker-" + name + "-" + threadCount.getAndIncrement());
            thread.setPriority(priority);
            return thread;
        }, UtilAccessor::seedQueue$uncaughtExceptionHandler, true);
    }

    /**
     * Shuts down and removes the SeedQueue specific {@link ExecutorService}s.
     */
    public synchronized static void shutdownExecutors() {
        if (SEEDQUEUE_BACKGROUND_EXECUTOR != null) {
            UtilAccessor.seedQueue$attemptShutdown(SEEDQUEUE_BACKGROUND_EXECUTOR);
            SEEDQUEUE_BACKGROUND_EXECUTOR = null;
        }
        if (SEEDQUEUE_WALL_EXECUTOR != null) {
            UtilAccessor.seedQueue$attemptShutdown(SEEDQUEUE_WALL_EXECUTOR);
            SEEDQUEUE_WALL_EXECUTOR = null;
        }
    }
}
