package me.contaria.seedqueue.compat;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;

import java.util.*;

public class SodiumCompat {
    public static final Map<ChunkFogMode, ChunkProgram> WALL_SHADER_CACHE = new EnumMap<>(ChunkFogMode.class);
    public static final List<ChunkBuildBuffers> WALL_BUILD_BUFFERS_POOL = Collections.synchronizedList(new ArrayList<>());

    static void clearShaderCache() {
        for (ChunkProgram program : WALL_SHADER_CACHE.values()) {
            program.delete();
        }
        WALL_SHADER_CACHE.clear();
    }

    static void clearBuildBufferPool() {
        WALL_BUILD_BUFFERS_POOL.clear();
    }
}
