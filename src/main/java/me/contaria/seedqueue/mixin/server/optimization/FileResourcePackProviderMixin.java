package me.contaria.seedqueue.mixin.server.optimization;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import net.minecraft.resource.FileResourcePackProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;

@Mixin(FileResourcePackProvider.class)
public class FileResourcePackProviderMixin {

    @WrapOperation(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;mkdirs()Z"
            )
    )
    private boolean delayDirectoryCreate(File instance, Operation<Boolean> original) {
        return SeedQueue.cancelDirectoryCreate(instance, original);
    }
}
