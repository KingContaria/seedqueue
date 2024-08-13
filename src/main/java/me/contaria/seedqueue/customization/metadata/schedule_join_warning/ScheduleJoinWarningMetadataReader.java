package me.contaria.seedqueue.customization.metadata.schedule_join_warning;

import com.google.gson.JsonObject;
import net.minecraft.resource.metadata.ResourceMetadataReader;

public class ScheduleJoinWarningMetadataReader implements ResourceMetadataReader<ScheduleJoinWarningMetadata> {
    @Override
    public String getKey() {
        return "schedule_join_warning";
    }

    @Override
    public ScheduleJoinWarningMetadata fromJson(JsonObject json) {
        return new ScheduleJoinWarningMetadata(json.get("world_gen_percentage").getAsInt());
    }
}
