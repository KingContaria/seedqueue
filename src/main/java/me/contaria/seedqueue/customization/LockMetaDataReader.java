package me.contaria.seedqueue.customization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resource.metadata.ResourceMetadataReader;

public class LockMetaDataReader implements ResourceMetadataReader<LockPosition> {

    @Override
    public String getKey() {
        return "seedqueue$position";
    }

    @Override
    public LockPosition fromJson(JsonObject json) {
        double x = getDouble(json, "x");
        double y = getDouble(json, "y");
        double width = getDouble(json, "width");
        double height = getDouble(json, "height");
        return new LockPosition(x, y, width, height);
    }

    private double getDouble(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("Missing required key: " + key);
        }
        return element.getAsDouble();
    }
}
