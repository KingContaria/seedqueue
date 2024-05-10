package me.contaria.seedqueue.mixin.compat.sodium.profiling;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import me.jellysquid.mods.sodium.client.render.chunk.format.DefaultModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheShared;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListenerManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.*;

import java.util.Set;

/**
 * Profiling mixins add more usage of the profiler to hot paths during wall rendering.
 * Because of the amount of injections this would require, @Overwrites are used where possible instead.
 * These Mixins will be removed in later versions of SeedQueue anyway.
 */
@Mixin(value = SodiumWorldRenderer.class, remap = false, priority = 500)
public abstract class SodiumWorldRendererMixin implements ChunkStatusListener {

    @Shadow(remap = true)
    private ClientWorld world;

    @Shadow
    private ChunkRenderManager<?> chunkRenderManager;

    @Shadow
    private ChunkRenderBackend<?> chunkRenderBackend;

    @Shadow
    @Final
    private LongSet loadedChunkPositions;

    @Shadow(remap = true)
    @Final
    private Set<BlockEntity> globalBlockEntities;

    @Shadow
    private int renderDistance;

    @Shadow(remap = true)
    @Final
    private MinecraftClient client;

    @Shadow
    private BlockRenderPassManager renderPassManager;

    @Shadow
    private static ChunkRenderBackend<?> createChunkRenderBackend(RenderDevice device, SodiumGameOptions options, ChunkVertexType vertexFormat) {
        return null;
    }

    /**
     * @author contaria
     * @reason see JavaDocs on this mixin class
     */
    @Overwrite(remap = true)
    public void setWorld(ClientWorld world) {
        // Check that the world is actually changing
        if (this.world == world) {
            return;
        }

        Profiler profiler = MinecraftClient.getInstance().getProfiler();

        profiler.push("sodium");
        // If we have a world is already loaded, unload the renderer
        if (this.world != null) {
            profiler.push("unload_world");
            this.unloadWorld();
            profiler.pop();
        }

        // If we're loading a new world, load the renderer
        if (world != null) {
            profiler.push("load_world");
            this.loadWorld(world);
            profiler.pop();
        }
        profiler.pop();
    }

    /**
     * @author contaria
     * @reason see JavaDocs on this mixin class
     */
    @Overwrite(remap = true)
    private void loadWorld(ClientWorld world) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();

        this.world = world;

        profiler.push("create_render_context");
        ChunkRenderCacheShared.createRenderContext(this.world);

        profiler.swap("init_renderer");
        this.initRenderer();

        profiler.swap("set_listener");
        ((ChunkStatusListenerManager) world.getChunkManager()).setListener(this);
        profiler.pop();
    }

    /**
     * @author contaria
     * @reason see JavaDocs on this mixin class
     */
    @Overwrite
    private void unloadWorld() {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();

        profiler.push("destroy_render_context");
        ChunkRenderCacheShared.destroyRenderContext(this.world);

        profiler.swap("destroy_render_manager");
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.destroy();
            this.chunkRenderManager = null;
        }

        profiler.swap("delete_render_backend");
        if (this.chunkRenderBackend != null) {
            this.chunkRenderBackend.delete();
            this.chunkRenderBackend = null;
        }

        profiler.swap("clear_loaded_chunks");
        this.loadedChunkPositions.clear();
        profiler.swap("clear_block_entities");
        this.globalBlockEntities.clear();

        this.world = null;
        profiler.pop();
    }

    /**
     * @author contaria
     * @reason see JavaDocs on this mixin class
     */
    @Overwrite
    public void reload() {
        if (this.world == null) {
            return;
        }
        Profiler profiler = MinecraftClient.getInstance().getProfiler();

        profiler.push("sodium");
        this.initRenderer();
        profiler.pop();
    }

    /**
     * @author contaria
     * @reason see JavaDocs on this mixin class
     */
    @Overwrite
    private void initRenderer() {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();

        profiler.push("destroy_render_manager");
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.destroy();
            this.chunkRenderManager = null;
        }

        profiler.swap("delete_render_backend");
        if (this.chunkRenderBackend != null) {
            this.chunkRenderBackend.delete();
            this.chunkRenderBackend = null;
        }

        profiler.swap("set_values");
        RenderDevice device = RenderDevice.INSTANCE;

        this.renderDistance = this.client.options.viewDistance;

        SodiumGameOptions opts = SodiumClientMod.options();

        profiler.swap("create_default_mappings");
        this.renderPassManager = BlockRenderPassManager.createDefaultMappings();

        final ChunkVertexType vertexFormat;

        if (opts.advanced.useCompactVertexFormat) {
            vertexFormat = DefaultModelVertexFormats.MODEL_VERTEX_HFP;
        } else {
            vertexFormat = DefaultModelVertexFormats.MODEL_VERTEX_SFP;
        }

        profiler.swap("create_render_backend");
        this.chunkRenderBackend = createChunkRenderBackend(device, opts, vertexFormat);
        profiler.swap("create_shaders");
        this.chunkRenderBackend.createShaders(device);

        profiler.swap("create_render_manager");
        this.chunkRenderManager = new ChunkRenderManager<>(this.getThis(), this.chunkRenderBackend, this.renderPassManager, this.world, this.renderDistance);
        profiler.swap("restore_chunks");
        this.chunkRenderManager.restoreChunks(this.loadedChunkPositions);
        profiler.pop();
    }

    @Unique
    private SodiumWorldRenderer getThis() {
        return (SodiumWorldRenderer) (Object) this;
    }
}
