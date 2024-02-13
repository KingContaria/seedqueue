package me.contaria.seedqueue;

import me.contaria.seedqueue.mixin.accessor.UtilAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class SeedQueueExecutorWrapper implements Executor {

    private static Executor BACKGROUND_EXECUTOR;
    private static Executor BEFORE_PREVIEW_EXECUTOR;
    private static Executor AFTER_PREVIEW_EXECUTOR;
    private static Executor LOCKED_EXECUTOR;

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

    public static Executor getBackgroundExecutor() {
        if (BACKGROUND_EXECUTOR == null) {
            BACKGROUND_EXECUTOR = UtilAccessor.seedQueue$createWorkerExecutor("SeedQueue Background");
        }
        return BACKGROUND_EXECUTOR;
    }

    public static Executor getBeforePreviewExecutor() {
        if (BEFORE_PREVIEW_EXECUTOR == null) {
            BEFORE_PREVIEW_EXECUTOR = UtilAccessor.seedQueue$createWorkerExecutor("SeedQueue Before Preview");
        }
        return BEFORE_PREVIEW_EXECUTOR;
    }

    public static Executor getAfterPreviewExecutor() {
        if (AFTER_PREVIEW_EXECUTOR == null) {
            AFTER_PREVIEW_EXECUTOR = UtilAccessor.seedQueue$createWorkerExecutor("SeedQueue After Preview");
        }
        return AFTER_PREVIEW_EXECUTOR;
    }

    public static Executor getLockedExecutor() {
        if (LOCKED_EXECUTOR == null) {
            LOCKED_EXECUTOR = UtilAccessor.seedQueue$createWorkerExecutor("SeedQueue Locked");
        }
        return LOCKED_EXECUTOR;
    }
}
