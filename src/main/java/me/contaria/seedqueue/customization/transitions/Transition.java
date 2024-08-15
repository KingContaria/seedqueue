package me.contaria.seedqueue.customization.transitions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.gui.wall.SeedQueueWallScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Transition {
    private final TransitionType type;
    private final int duration;

    private Transition(TransitionType type, int duration) {
        this.type = type;
        this.duration = duration;
    }

    public int getDuration() {
        return this.duration;
    }

    public int transform(int start, int end, double progress) {
        return (int) Math.round(MathHelper.clampedLerp(start, end, TransitionType.get(this.type, progress)));
    }

    public static Map<String, Transition> createTransitions() {
        ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
        if (manager.containsResource(SeedQueueWallScreen.TRANSITIONS)) {
            try (Reader reader = new InputStreamReader(manager.getResource(SeedQueueWallScreen.TRANSITIONS).getInputStream(), StandardCharsets.UTF_8)) {
                return createTransitions(new JsonParser().parse(reader).getAsJsonObject());
            } catch (Exception e) {
                SeedQueue.LOGGER.warn("Failed to parse wall transitions!", e);
            }
        }
        return new HashMap<>();
    }

    private static Map<String, Transition> createTransitions(JsonObject jsonObject) {
        Map<String, Transition> transitions = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            transitions.put(entry.getKey(), fromJson(entry.getValue().getAsJsonObject()));
        }
        return transitions;
    }

    private static Transition fromJson(JsonObject jsonObject) {
        return new Transition(jsonObject.has("type") ? TransitionType.valueOf(jsonObject.get("type").getAsString()) : TransitionType.LINEAR, jsonObject.get("duration").getAsInt());
    }

    public static @Nullable Transition createTransition(String id) {
        ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
        if (manager.containsResource(SeedQueueWallScreen.TRANSITIONS)) {
            try (Reader reader = new InputStreamReader(manager.getResource(SeedQueueWallScreen.TRANSITIONS).getInputStream(), StandardCharsets.UTF_8)) {
                return fromJson(new JsonParser().parse(reader).getAsJsonObject(), id);
            } catch (Exception e) {
                SeedQueue.LOGGER.warn("Failed to parse wall transitions!", e);
            }
        }
        return null;
    }

    private static @Nullable Transition fromJson(JsonObject jsonObject, String id) {
        if (!jsonObject.has(id)) {
            return null;
        }
        JsonObject transition = jsonObject.getAsJsonObject(id);
        return new Transition(transition.has("type") ? TransitionType.valueOf(transition.get("type").getAsString()) : TransitionType.LINEAR, transition.get("duration").getAsInt());
    }
}
