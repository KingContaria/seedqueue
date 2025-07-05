package me.contaria.seedqueue.customization;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.speedrunapi.util.IdentifierUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LockTexture extends AnimatedTexture {

    public final int width;
    public final int height;
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

            Identifier metadataId = new Identifier(id.getNamespace(), id.getPath() + ".mcmeta");
            try {
                Resource metadataResource = MinecraftClient.getInstance().getResourceManager().getResource(metadataId);
                LockPosition position = metadataResource.getMetadata(new LockMetaDataReader());
                if (position != null) {
                    this.posX = position.getX();
                    this.posY = position.getY();
                    this.specifiedWidth = position.getWidth();
                    this.specifiedHeight = position.getHeight();
                }
            } catch (IOException e) {
                SeedQueue.LOGGER.warn("Metadata file not found for {}.", id);
            }
        }
    }

    public double getAspectRatio() {
        return (double) this.width / this.height;
    }

    public static List<LockTexture> createLockTextures() {
        List<LockTexture> lockTextures = new ArrayList<>();
        Identifier lock = IdentifierUtil.of("seedqueue", "textures/gui/wall/lock.png");
        do {
            try {
                lockTextures.add(new LockTexture(lock));
            } catch (IOException e) {
                SeedQueue.LOGGER.warn("Failed to read lock image texture: {}", lock, e);
            }
        } while (MinecraftClient.getInstance().getResourceManager().containsResource(lock = IdentifierUtil.of("seedqueue", "textures/gui/wall/lock-" + lockTextures.size() + ".png")));
        return lockTextures;
    }
}
