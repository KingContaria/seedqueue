package me.contaria.seedqueue.mixin.client.levellist;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;

@Mixin(LevelStorage.class)
public abstract class LevelStorageMixin {

    @WrapOperation(
            method = "getLevelList",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;isDirectory()Z",
                    remap = false
            )
    )
    private boolean reduceLevelList(File file, Operation<Boolean> original) {
        String name = file.getName();
        if (SeedQueue.config.reduceLevelList && (name.startsWith("Benchmark Reset #") || (name.startsWith("Random Speedrun #") || name.startsWith("Set Speedrun #")) && !new File(file, "level.dat_old").exists())) {
            return false;
        }
        return original.call(file);
    }
}
