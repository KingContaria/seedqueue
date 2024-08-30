package me.contaria.seedqueue.customization;

import com.google.gson.*;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.gui.wall.SeedQueueWallScreen;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class Layout {
    @NotNull
    public final Group main;
    @Nullable
    public final Group locked;
    public final Group[] preparing;
    public final boolean replaceLockedInstances;

    private Layout(@NotNull Group main) {
        this(main, null, new Group[0], true);
    }

    private Layout(@NotNull Group main, @Nullable Group locked, Group[] preparing, boolean replaceLockedInstances) {
        this.main = main;
        this.locked = locked;
        this.preparing = preparing;
        this.replaceLockedInstances = replaceLockedInstances;

        if (this.main.cosmetic) {
            throw new IllegalArgumentException("Main Group may not be cosmetic!");
        }
    }

    private static int getX(JsonObject jsonObject) {
        return getAsInt(jsonObject, "x", MinecraftClient.getInstance().getWindow().getFramebufferWidth());
    }

    private static int getY(JsonObject jsonObject) {
        return getAsInt(jsonObject, "y", MinecraftClient.getInstance().getWindow().getFramebufferHeight());
    }

    private static int getWidth(JsonObject jsonObject) {
        return getAsInt(jsonObject, "width", MinecraftClient.getInstance().getWindow().getFramebufferWidth());
    }

    private static int getHeight(JsonObject jsonObject) {
        return getAsInt(jsonObject, "height", MinecraftClient.getInstance().getWindow().getFramebufferHeight());
    }

    private static int getAsInt(JsonObject jsonObject, String name, int windowSize) {
        JsonPrimitive jsonPrimitive = jsonObject.getAsJsonPrimitive(name);
        if (jsonPrimitive.isNumber() && jsonPrimitive.toString().contains(".")) {
            return (int) (windowSize * jsonPrimitive.getAsDouble());
        }
        return jsonPrimitive.getAsInt();
    }

    private static Layout grid(int rows, int columns, int width, int height) {
        return new Layout(Group.grid(rows, columns, 0, 0, width, height, 0, false, true));
    }

    private static Layout fromJson(JsonObject jsonObject) throws JsonParseException {
        return new Layout(
                Group.fromJson(jsonObject.getAsJsonObject("main"), SeedQueue.config.rows, SeedQueue.config.columns),
                jsonObject.has("locked") ? Group.fromJson(jsonObject.getAsJsonObject("locked")) : null,
                jsonObject.has("preparing") ? Group.fromJson(jsonObject.getAsJsonArray("preparing")) : new Group[0],
                jsonObject.has("replaceLockedInstances") && jsonObject.get("replaceLockedInstances").getAsBoolean()
        );
    }

    public static Layout createLayout() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getResourceManager().containsResource(SeedQueueWallScreen.CUSTOM_LAYOUT)) {
            try (Reader reader = new InputStreamReader(client.getResourceManager().getResource(SeedQueueWallScreen.CUSTOM_LAYOUT).getInputStream(), StandardCharsets.UTF_8)) {
                return Layout.fromJson(new JsonParser().parse(reader).getAsJsonObject());
            } catch (Exception e) {
                SeedQueue.LOGGER.warn("Failed to parse custom wall layout!", e);
            }
        }
        return Layout.grid(SeedQueue.config.rows, SeedQueue.config.columns, client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
    }

    public static class Group {
        private final Pos[] positions;
        public final boolean cosmetic;
        public final boolean instance_background;

        private Group(Pos[] positions, boolean cosmetic, boolean instance_background) {
            this.positions = positions;
            this.cosmetic = cosmetic;
            this.instance_background = instance_background;
        }

        public Pos getPos(int index) {
            if (index < 0 || index >= this.positions.length) {
                return null;
            }
            return this.positions[index];
        }

        public int size() {
            return this.positions.length;
        }

        public static int totalSize(Group[] groups) {
            int sum = 0;
            for (Group group : groups) {
                sum += group.size();
            }
            return sum;
        }

        private static Group grid(int rows, int columns, int x, int y, int width, int height, int padding, boolean cosmetic, boolean instance_background) {
            Pos[] positions = new Pos[rows * columns];
            int columnWidth = (width - padding * (columns - 1)) / columns;
            int rowHeight = (height - padding * (rows - 1)) / rows;
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    positions[row * columns + column] = new Pos(
                            x + column * columnWidth + padding * column,
                            y + row * rowHeight + padding * row,
                            columnWidth,
                            rowHeight
                    );
                }
            }
            return new Group(positions, cosmetic, instance_background);
        }

        private static Group[] fromJson(JsonArray jsonArray) throws JsonParseException {
            Group[] groups = new Group[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                groups[i] = Group.fromJson(jsonArray.get(i).getAsJsonObject());
            }
            return groups;
        }

        private static Group fromJson(JsonObject jsonObject) throws JsonParseException {
            return fromJson(jsonObject, null, null);
        }

        private static Group fromJson(JsonObject jsonObject, Integer defaultRows, Integer defaultColumns) throws JsonParseException {
            boolean cosmetic = jsonObject.has("cosmetic") && jsonObject.get("cosmetic").getAsBoolean();
            boolean instance_background = !jsonObject.has("instance_background") || jsonObject.get("instance_background").getAsBoolean();
            if (jsonObject.has("positions")) {
                JsonArray positionsArray = jsonObject.get("positions").getAsJsonArray();
                Pos[] positions = new Pos[positionsArray.size()];
                for (int i = 0; i < positionsArray.size(); i++) {
                    positions[i] = Pos.fromJson(positionsArray.get(i).getAsJsonObject());
                }
                return new Group(positions, cosmetic, instance_background);
            }
            return Group.grid(
                    jsonObject.has("rows") || defaultRows == null ? jsonObject.get("rows").getAsInt() : defaultRows,
                    jsonObject.has("columns") || defaultColumns == null ? jsonObject.get("columns").getAsInt() : defaultColumns,
                    getX(jsonObject),
                    getY(jsonObject),
                    getWidth(jsonObject),
                    getHeight(jsonObject),
                    jsonObject.has("padding") ? jsonObject.get("padding").getAsInt() : 0, cosmetic, instance_background
            );
        }
    }

    public static class Pos {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        Pos(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private static Pos fromJson(JsonObject jsonObject) throws JsonParseException {
            return new Pos(
                    getX(jsonObject),
                    getY(jsonObject),
                    getWidth(jsonObject),
                    getHeight(jsonObject)
            );
        }
    }
}
