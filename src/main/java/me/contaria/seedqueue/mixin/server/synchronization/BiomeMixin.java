package me.contaria.seedqueue.mixin.server.synchronization;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.surfacebuilder.ConfiguredSurfaceBuilder;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @WrapMethod(
            method = "buildSurface"
    )
    private void synchronizeBuildSurface(Random random, Chunk chunk, int x, int z, int worldHeight, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel, long seed, Operation<Void> original) {
        if (this.synchronizedAccess) {
            synchronized (this.surfaceBuilder.surfaceBuilder) {
                original.call(random, chunk, x, z, worldHeight, noise, defaultBlock, defaultFluid, seaLevel, seed);
            }
        } else {
            original.call(random, chunk, x, z, worldHeight, noise, defaultBlock, defaultFluid, seaLevel, seed);
        }
    }
}
