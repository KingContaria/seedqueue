package me.contaria.seedqueue.mixin.compat.sodium.profiling;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.VertexData;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw.MultidrawChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw.MultidrawGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.ChunkRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkRenderShaderBackend;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;

/**
 * Profiling mixins add more usage of the profiler to hot paths during wall rendering.
 * Because of the amount of injections this would require, @Overwrites are used where possible instead.
 * These Mixins will be removed in later versions of SeedQueue anyway.
 */
@Mixin(value = MultidrawChunkRenderBackend.class, remap = false)
public abstract class MultidrawChunkRenderBackendMixin extends ChunkRenderShaderBackend<MultidrawGraphicsState> {

    @Shadow @Final private GlMutableBuffer uploadBuffer;

    @Shadow @Final private ObjectArrayFIFOQueue<ChunkRegion<MultidrawGraphicsState>> pendingUploads;

    @Shadow
    private static int getUploadQueuePayloadSize(List<ChunkBuildResult<MultidrawGraphicsState>> queue) {
        return 0;
    }

    @Shadow protected abstract GlTessellation createRegionTessellation(CommandList commandList, GlBuffer buffer);

    @Shadow @Final private ChunkRegionManager<MultidrawGraphicsState> bufferManager;

    public MultidrawChunkRenderBackendMixin(ChunkVertexType vertexType) {
        super(vertexType);
    }

    /**
     * @author contaria
     * @reason see JavaDocs on this mixin class
     */
    @Overwrite
    public void upload(CommandList commandList, Iterator<ChunkBuildResult<MultidrawGraphicsState>> queue) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();

        profiler.push("setup_upload_batches");
        this.setupUploadBatches(queue);

        profiler.swap("bind_buffer");
        commandList.bindBuffer(GlBufferTarget.ARRAY_BUFFER, this.uploadBuffer);

        profiler.swap("iterate_queue");
        while (!this.pendingUploads.isEmpty()) {
            profiler.push("dequeue_upload");
            ChunkRegion<MultidrawGraphicsState> region = this.pendingUploads.dequeue();

            GlBufferArena arena = region.getBufferArena();
            GlBuffer buffer = arena.getBuffer();

            ObjectArrayList<ChunkBuildResult<MultidrawGraphicsState>> uploadQueue = region.getUploadQueue();
            profiler.swap("prepare_buffer");
            arena.prepareBuffer(commandList, getUploadQueuePayloadSize(uploadQueue));

            profiler.swap("iterate_results");
            for (ChunkBuildResult<MultidrawGraphicsState> result : uploadQueue) {
                ChunkRenderContainer<MultidrawGraphicsState> render = result.render;
                ChunkRenderData data = result.data;

                for (BlockRenderPass pass : BlockRenderPass.VALUES) {
                    profiler.push("get_graphics_state");
                    MultidrawGraphicsState graphics = render.getGraphicsState(pass);

                    profiler.swap("deallocate_buffer");
                    // De-allocate the existing buffer arena for this render
                    // This will allow it to be cheaply re-allocated just below
                    if (graphics != null) {
                        graphics.delete(commandList);
                    }

                    profiler.swap("get_mesh");
                    ChunkMeshData meshData = data.getMesh(pass);

                    if (meshData.hasVertexData()) {
                        profiler.swap("take_vertex_data");
                        VertexData upload = meshData.takeVertexData();

                        profiler.swap("upload_data");
                        commandList.uploadData(this.uploadBuffer, upload.buffer);

                        profiler.swap("upload_buffer");
                        GlBufferSegment segment = arena.uploadBuffer(commandList, this.uploadBuffer, 0, upload.buffer.capacity());

                        profiler.swap("set_graphics_state");
                        render.setGraphicsState(pass, new MultidrawGraphicsState(render, region, segment, meshData, this.vertexFormat));
                    } else {
                        profiler.swap("set_graphics_state");
                        render.setGraphicsState(pass, null);
                    }
                    profiler.pop();
                }

                render.setData(data);
            }

            profiler.swap("update_tesselation");
            // Check if the tessellation needs to be updated
            // This happens whenever the backing buffer object for the arena changes, or if it hasn't already been created
            if (region.getTessellation() == null || buffer != arena.getBuffer()) {
                if (region.getTessellation() != null) {
                    commandList.deleteTessellation(region.getTessellation());
                }

                region.setTessellation(this.createRegionTessellation(commandList, arena.getBuffer()));
            }

            profiler.swap("clear_queue");
            uploadQueue.clear();
            profiler.pop();
        }

        profiler.swap("invalidate_buffer");
        commandList.invalidateBuffer(this.uploadBuffer);
        profiler.pop();
    }

    /**
     * @author contaria
     * @reason see JavaDocs on this mixin class
     */
    @Overwrite
    private void setupUploadBatches(Iterator<ChunkBuildResult<MultidrawGraphicsState>> renders) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();

        while (renders.hasNext()) {
            ChunkBuildResult<MultidrawGraphicsState> result = renders.next();
            ChunkRenderContainer<MultidrawGraphicsState> render = result.render;

            profiler.push("get_region");
            ChunkRegion<MultidrawGraphicsState> region = this.bufferManager.getRegion(render.getChunkX(), render.getChunkY(), render.getChunkZ());

            if (region == null) {
                if (result.data.getMeshSize() <= 0) {
                    render.setData(result.data);
                    profiler.pop();
                    continue;
                }

                profiler.swap("create_region");
                region = this.bufferManager.getOrCreateRegion(render.getChunkX(), render.getChunkY(), render.getChunkZ());
            }

            profiler.swap("add_to_queue");
            ObjectArrayList<ChunkBuildResult<MultidrawGraphicsState>> uploadQueue = region.getUploadQueue();

            if (uploadQueue.isEmpty()) {
                this.pendingUploads.enqueue(region);
            }

            uploadQueue.add(result);
            profiler.pop();
        }
    }
}
