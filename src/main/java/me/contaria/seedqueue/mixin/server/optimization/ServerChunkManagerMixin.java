package me.contaria.seedqueue.mixin.server.optimization;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;

@Mixin(ServerChunkManager.class)
public class ServerChunkManagerMixin {

    @Shadow @Final private ServerWorld world;

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;mkdirs()Z"
            )
    )
    private boolean delayDirectoryCreate(File instance, Operation<Boolean> original) {
        SQMinecraftServer server = (SQMinecraftServer) this.world.getServer();
        if(server.seedQueue$inQueue()){
            return true;
        } else {
            return original.call(instance);
        }
    }

}
