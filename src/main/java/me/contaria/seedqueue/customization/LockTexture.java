package me.contaria.seedqueue.customization;

import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LockTexture {
    private final Identifier id;
    private final int width;
    private final int height;

    @Nullable
    private final AnimationResourceMetadata animation;

    public LockTexture(Identifier id) throws IOException {
        this.id = id;
        Resource resource = MinecraftClient.getInstance().getResourceManager().getResource(id);
        this.animation = resource.getMetadata(AnimationResourceMetadata.READER);
        try (NativeImage image = NativeImage.read(resource.getInputStream())) {
            this.width = image.getWidth();
            this.height = image.getHeight() / (this.animation != null ? this.animation.getFrameIndexSet().size() : 1);
        }
    }

    public Identifier getId() {
        return this.id;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public double getAspectRatio() {
        return (double) this.width / this.height;
    }

    public int getFrameIndex(int tick) {
        // does not currently support setting frametime for individual frames
        // see AnimationFrameResourceMetadata#usesDefaultFrameTime
        return this.animation != null ? this.animation.getFrameIndex((tick / this.animation.getDefaultFrameTime()) % this.animation.getFrameCount()) : 0;
    }

    public int getIndividualFrameCount() {
        return this.animation != null ? this.animation.getFrameIndexSet().size() : 1;
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
