package me.contaria.seedqueue.interfaces;

import java.util.concurrent.Executor;

public interface SQMinecraftServer {

    void seedQueue$tryPausingServer();

    boolean seedQueue$isPaused();

    void seedQueue$setExecutor(Executor executor);

    void seedQueue$resetExecutor();
}
