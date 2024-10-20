package me.contaria.seedqueue.mixin.server.synchronization;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.surfacebuilder.ConfiguredSurfaceBuilder;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
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
    private boolean synchronizedAccess;

    @Inject(
            method = "<init>*",
            at = @At("TAIL")
    )
    private void shouldSynchronizeAccess(CallbackInfo ci) {
        Class<?> clas = this.surfaceBuilder.surfaceBuilder.getClass();
        while (clas != SurfaceBuilder.class) {
            try {
                clas.getDeclaredMethod(initSeed, long.class);
                this.synchronizedAccess = true;
                break;
            } catch (NoSuchMethodException e) {
                clas = clas.getSuperclass();
            }
        }
    }

    /**
     * @author contaria
     * @reason Synchronize calls if necessary, WrapMethod is not used because it causes a lot of Object arrays to be allocated in hot code.
     */
    @Overwrite
    public void buildSurface(Random random, Chunk chunk, int x, int z, int worldHeight, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel, long seed) {
        if (this.synchronizedAccess) {
            synchronized (this.surfaceBuilder.surfaceBuilder) {
                this.surfaceBuilder.initSeed(seed);
                this.surfaceBuilder.generate(random, chunk, (Biome) (Object) this, x, z, worldHeight, noise, defaultBlock, defaultFluid, seaLevel, seed);
            }
        } else {
            this.surfaceBuilder.initSeed(seed);
            this.surfaceBuilder.generate(random, chunk, (Biome) (Object) this, x, z, worldHeight, noise, defaultBlock, defaultFluid, seaLevel, seed);
        }
    }
}
