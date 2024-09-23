package me.contaria.seedqueue.customization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resource.metadata.ResourceMetadataReader;

public class LockMetaDataReader implements ResourceMetadataReader<LockPosition> {

    @Override
    public String getKey() {
        // Key that should be used in the metadata
        return "position";
    }

    @Override
    public LockPosition fromJson(JsonObject json) {
        double x = JsonHelper.getDouble(json, "x", 0.0);
        double y = JsonHelper.getDouble(json, "y", 0.0);
        double width = JsonHelper.getDouble(json, "width", 0.0);
        double height = JsonHelper.getDouble(json, "height", 0.0);
        return new LockPosition(x, y, width, height);
    }

    public static class JsonHelper {
        public static double getDouble(JsonObject object, String key, double fallback) {
            JsonElement element = object.get(key);
            return element != null && !element.isJsonNull() ? element.getAsDouble() : fallback;
        }
    }
}
