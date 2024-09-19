package me.contaria.seedqueue.customization;

import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import net.minecraft.resource.Resource;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LockTexture extends AnimatedTexture {
    private final int width;
    private final int height;
    public double posX;
    public double posY;
    public double specifiedWidth;
    public double specifiedHeight;

    public LockTexture(Identifier id) throws IOException {
        super(id);
        try (NativeImage image = NativeImage.read(MinecraftClient.getInstance().getResourceManager().getResource(id).getInputStream())) {
            this.width = image.getWidth();
            this.height = image.getHeight() / (this.animation != null ? this.animation.getFrameIndexSet().size() : 1);

            this.posX = 0;
            this.posY = 0;
            this.specifiedWidth = this.width;
            this.specifiedHeight = this.height;

            Identifier metadataId = new Identifier(id.getNamespace(), id.getPath().replace(".png", ".json"));
            try {
                Resource metadataResource = MinecraftClient.getInstance().getResourceManager().getResource(metadataId);
                JsonObject metadata = parseMetadata(metadataResource);
                if (metadata != null && metadata.has("position")) {
                    JsonObject position = metadata.getAsJsonObject("position");
                    this.posX = position.has("x") ? position.get("x").getAsDouble() : this.posX;
                    this.posY = position.has("y") ? position.get("y").getAsDouble() : this.posY;
                    this.specifiedWidth = position.has("width") ? position.get("width").getAsDouble() : this.specifiedWidth;
                    this.specifiedHeight = position.has("height") ? position.get("height").getAsDouble() : this.specifiedHeight;
                }
            } catch (IOException e) {
                SeedQueue.LOGGER.warn("Metadata file not found for {}: Using default values.", id);
            }
        }
    }

    private JsonObject parseMetadata(Resource resource) {
        try (InputStream inputStream = resource.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        } catch (IOException e) {
            SeedQueue.LOGGER.warn("Failed to read metadata for {}", resource.getId(), e);
            return null;
        }
    }

    public double getAspectRatio() {
        return (double) this.width / this.height;
    }

    public static List<LockTexture> createLockTextures() {
        List<LockTexture> lockTextures = new ArrayList<>();
        Identifier lock = new Identifier("seedqueue", "textures/gui/wall/lock.png");
        do {
            try {
                lockTextures.add(new LockTexture(lock));
            } catch (IOException e) {
                SeedQueue.LOGGER.warn("Failed to read lock image texture: {}", lock, e);
            }
        } while (MinecraftClient.getInstance().getResourceManager().containsResource(lock = new Identifier("seedqueue", "textures/gui/wall/lock-" + lockTextures.size() + ".png")));
        return lockTextures;
    }
}
