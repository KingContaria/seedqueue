package me.contaria.seedqueue.mixin.client.profiling;

import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.Set;

/**
 * Profiling mixins add more usage of the profiler to hot paths during wall rendering.
 * Because of the amount of injections this would require, @Overwrites are used where possible instead.
 * These Mixins will be removed in later versions of SeedQueue anyway.
 */
@Mixin(value = WorldRenderer.class, priority = 500)
public abstract class WorldRendererMixin {

    @Shadow @Final private MinecraftClient client;

    @Shadow private ClientWorld world;

    @Shadow private double lastCameraChunkUpdateX;

    @Shadow private double lastCameraChunkUpdateY;

    @Shadow private double lastCameraChunkUpdateZ;

    @Shadow private int cameraChunkX;

    @Shadow private int cameraChunkY;

    @Shadow private int cameraChunkZ;

    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow private Set<ChunkBuilder.BuiltChunk> chunksToRebuild;

    @Shadow @Final private ObjectList<?> visibleChunks;

    @Shadow private BuiltChunkStorage chunks;

    @Shadow private ChunkBuilder chunkBuilder;

    @Shadow @Final private Set<BlockEntity> noCullingBlockEntities;

    @Shadow protected abstract void loadTransparencyShader();

    @Shadow protected abstract void resetTransparencyShader();

    @Shadow private boolean needsTerrainUpdate;

    @Shadow private boolean cloudsDirty;

    @Shadow private int renderDistance;

    @Shadow protected abstract void clearChunkRenderers();

    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    /**
     * @author contaria
     * @reason see JavaDocs on this mixin class
     */
    @Overwrite
    public void setWorld(@Nullable ClientWorld clientWorld) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();

        profiler.push("vanilla");
        profiler.push("reset_values");
        this.lastCameraChunkUpdateX = Double.MIN_VALUE;
        this.lastCameraChunkUpdateY = Double.MIN_VALUE;
        this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
        this.cameraChunkX = Integer.MIN_VALUE;
        this.cameraChunkY = Integer.MIN_VALUE;
        this.cameraChunkZ = Integer.MIN_VALUE;
        this.entityRenderDispatcher.setWorld(clientWorld);
        this.world = clientWorld;
        if (clientWorld != null) {
            profiler.swap("reload");
            this.reload();
        } else {
            profiler.swap("clear_chunks");
            this.chunksToRebuild.clear();
            this.visibleChunks.clear();
            if (this.chunks != null) {
                this.chunks.clear();
                this.chunks = null;
            }

            profiler.swap("stop_builder");
            if (this.chunkBuilder != null) {
                this.chunkBuilder.stop();
            }

            this.chunkBuilder = null;
            profiler.swap("clear_no_culling_block_entities");
            this.noCullingBlockEntities.clear();
        }
        profiler.pop();
        profiler.pop();
    }

    /**
     * @author contaria
     * @reason see JavaDocs on this mixin class
     */
    @Overwrite
    public void reload() {
        if (this.world != null) {
            Profiler profiler = MinecraftClient.getInstance().getProfiler();
            profiler.push("transparency_shader");
            if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                this.loadTransparencyShader();
            } else {
                this.resetTransparencyShader();
            }

            profiler.swap("reload_color");
            this.world.reloadColor();
            profiler.swap("chunk_builder");
            if (this.chunkBuilder == null) {
                this.chunkBuilder = new ChunkBuilder(this.world, this.getThis(), Util.getServerWorkerExecutor(), this.client.is64Bit(), this.bufferBuilders.getBlockBufferBuilders());
            } else {
                this.chunkBuilder.setWorld(this.world);
            }

            profiler.swap("set_dirty");
            this.needsTerrainUpdate = true;
            this.cloudsDirty = true;
            RenderLayers.setFancyGraphicsOrBetter(MinecraftClient.isFancyGraphicsOrBetter());
            this.renderDistance = this.client.options.viewDistance;
            profiler.swap("clear_chunks");
            if (this.chunks != null) {
                this.chunks.clear();
            }

            profiler.swap("clear_chunk_renderers");
            this.clearChunkRenderers();
            profiler.swap("clear_no_culling_block_entities");
            synchronized(this.noCullingBlockEntities) {
                this.noCullingBlockEntities.clear();
            }

            profiler.swap("built_chunk_storage");
            this.chunks = new BuiltChunkStorage(this.chunkBuilder, this.world, this.client.options.viewDistance, this.getThis());
            if (this.world != null) {
                Entity entity = this.client.getCameraEntity();
                if (entity != null) {
                    profiler.swap("update_camera_pos");
                    this.chunks.updateCameraPosition(entity.getX(), entity.getZ());
                }
            }
            profiler.pop();
        }
    }

    @Unique
    private WorldRenderer getThis() {
        return (WorldRenderer) (Object) this;
    }
}
