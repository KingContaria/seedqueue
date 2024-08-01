package me.contaria.seedqueue.interfaces;

import java.util.concurrent.Executor;

public interface SQMinecraftServer {

    void seedQueue$tryPausingServer();

    boolean seedQueue$shouldPause();

    boolean seedQueue$isPaused();

    boolean seedQueue$isScheduledToPause();

    void seedQueue$schedulePause();

    void seedQueue$unpause();

    void seedQueue$setExecutor(Executor executor);

    void seedQueue$resetExecutor();

    int seedQueue$incrementAndGetEntityID();
}
