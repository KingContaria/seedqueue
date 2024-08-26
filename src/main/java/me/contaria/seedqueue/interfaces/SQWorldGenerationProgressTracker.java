package me.contaria.seedqueue.interfaces;

public interface SQWorldGenerationProgressTracker {
    void seedQueue$freezeAfterMillis(long millis);

    void seedQueue$unfreeze();
}
