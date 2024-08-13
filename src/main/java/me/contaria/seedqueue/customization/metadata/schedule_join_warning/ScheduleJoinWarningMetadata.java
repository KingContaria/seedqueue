package me.contaria.seedqueue.customization.metadata.schedule_join_warning;

import me.contaria.seedqueue.sounds.SeedQueueSounds;

public class ScheduleJoinWarningMetadata {
    private final int worldGenPercentage;

    ScheduleJoinWarningMetadata(int worldGenPercentage) {
        this.worldGenPercentage = worldGenPercentage;
    }

    public static int getWorldGenPercentage() {
        SeedQueueSounds.SCHEDULED_JOIN_WARNING
    }
}
