package me.contaria.seedqueue.customization.textures;

import me.contaria.seedqueue.SeedQueue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class AnimatedTexture {
    private final Identifier id;
    @Nullable
    protected final AnimationResourceMetadata animation;

    protected AnimatedTexture(Identifier id) {
        this.id = id;
        AnimationResourceMetadata animation = null;
        try {
            animation = MinecraftClient.getInstance().getResourceManager().getResource(id).getMetadata(AnimationResourceMetadata.READER);
        } catch (IOException e) {
            SeedQueue.LOGGER.warn("Failed to read animation data for {}!", id, e);
        }
        this.animation = animation;
    }

    public Identifier getId() {
        return this.id;
    }

    public int getFrameIndex(int tick) {
        // does not currently support setting frametime for individual frames
        // see AnimationFrameResourceMetadata#usesDefaultFrameTime
        return this.animation != null ? this.animation.getFrameIndex((tick / this.animation.getDefaultFrameTime()) % this.animation.getFrameCount()) : 0;
    }

    public int getIndividualFrameCount() {
        return this.animation != null ? this.animation.getFrameIndexSet().size() : 1;
    }

    public static @Nullable AnimatedTexture of(Identifier id) {
        if (MinecraftClient.getInstance().getResourceManager().containsResource(id)) {
            return new AnimatedTexture(id);
        }
        return null;
    }
}
