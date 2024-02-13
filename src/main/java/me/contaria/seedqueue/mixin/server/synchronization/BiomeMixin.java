package me.contaria.seedqueue.mixin.server.synchronization;

import com.mojang.serialization.Codec;
import me.contaria.seedqueue.SeedQueue;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.surfacebuilder.ConfiguredSurfaceBuilder;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import net.minecraft.world.gen.surfacebuilder.SurfaceConfig;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Biome.class)
public abstract class BiomeMixin {

    @Unique
    private static final String initSeed = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_3523", "method_15306", "(J)V");

    @Shadow
    @Final
    protected ConfiguredSurfaceBuilder<?> surfaceBuilder;

    @Unique
    private ThreadLocal<ConfiguredSurfaceBuilder<?>> threadSafeSurfaceBuilder;

    @Unique
    private boolean synchronizedAccess;

    // TODO: logging doesn't actually log the biome id, but the surfacebuilder id
    @Inject(
            method = "<init>*",
            at = @At("TAIL")
    )
    private void shouldSynchronizeAccess(CallbackInfo ci) {
        Class<?> clas = this.surfaceBuilder.surfaceBuilder.getClass();
        while (clas != SurfaceBuilder.class) {
            try {
                clas.getDeclaredMethod(initSeed, long.class);
                if (SeedQueue.config.usePerThreadSurfaceBuilders) {
                    SeedQueue.LOGGER.info("Enabled per thread surface building for " + Registry.SURFACE_BUILDER.getId(this.surfaceBuilder.surfaceBuilder) + " using " + clas + ".");
                    this.threadSafeSurfaceBuilder = new ThreadLocal<>();
                } else {
                    SeedQueue.LOGGER.info("Enabled synchronized surface building for " + Registry.SURFACE_BUILDER.getId(this.surfaceBuilder.surfaceBuilder) + " using " + clas + ".");
                    this.synchronizedAccess = true;
                }
                break;
            } catch (NoSuchMethodException e) {
                clas = clas.getSuperclass();
            }
        }
    }

    /**
     * @author contaria
     * @reason Synchronize building surface to avoid corruption/crashes.
     */
    @Overwrite
    public void buildSurface(Random random, Chunk chunk, int x, int z, int worldHeight, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel, long seed) {
        ConfiguredSurfaceBuilder<?> surfaceBuilder = this.getThreadSafeSurfaceBuilder();
        if (this.synchronizedAccess) {
            synchronized (this.surfaceBuilder.surfaceBuilder) {
                surfaceBuilder.initSeed(seed);
                surfaceBuilder.generate(random, chunk, (Biome) (Object) this, x, z, worldHeight, noise, defaultBlock, defaultFluid, seaLevel, seed);
            }
        } else {
            surfaceBuilder.initSeed(seed);
            surfaceBuilder.generate(random, chunk, (Biome) (Object) this, x, z, worldHeight, noise, defaultBlock, defaultFluid, seaLevel, seed);
        }
    }

    @Unique
    private ConfiguredSurfaceBuilder<?> getThreadSafeSurfaceBuilder() {
        if (this.threadSafeSurfaceBuilder != null) {
            if (this.threadSafeSurfaceBuilder.get() == null) {
                try {
                    this.threadSafeSurfaceBuilder.set(ConfiguredSurfaceBuilder.class.getConstructor(SurfaceBuilder.class, SurfaceConfig.class).newInstance(this.surfaceBuilder.surfaceBuilder.getClass().getConstructor(Codec.class).newInstance(this.surfaceBuilder.surfaceBuilder.method_29003()), this.surfaceBuilder.config));
                } catch (ReflectiveOperationException e) {
                    SeedQueue.LOGGER.warn("Failed to construct per thread surface builder for " + Registry.SURFACE_BUILDER.getId(this.surfaceBuilder.surfaceBuilder) + ", falling back to synchronized building.");
                    this.threadSafeSurfaceBuilder = null;
                    this.synchronizedAccess = true;
                    return this.surfaceBuilder;
                }
            }
            return this.threadSafeSurfaceBuilder.get();
        }
        return this.surfaceBuilder;
    }
}
