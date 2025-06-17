package me.contaria.seedqueue.mixin.server.optimization;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.interfaces.SQLevelStorageSession;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.SessionLock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Path;

@Mixin(LevelStorage.Session.class)
public class LevelStorageSessionMixin  implements SQLevelStorageSession {
    @Shadow @Mutable
    private SessionLock lock;

    @Shadow @Final private Path directory;

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/SessionLock;create(Ljava/nio/file/Path;)Lnet/minecraft/world/level/storage/SessionLock;"
            )
    )
    private SessionLock skipSessionLockCreation(Path path, Operation<SessionLock> original) throws IOException {
        return SeedQueue.inQueue()? null : original.call(path);
    }

    public void seedQueue$createLock() throws IOException {
        this.lock = SessionLock.create(this.directory);
    }

    @Inject(method = "checkValid", at = @At("HEAD"),cancellable = true)
    private void dontRequireLockWhileInQueue(CallbackInfo ci) {
        if (SeedQueue.inQueue() || SeedQueue.getThreadLocalEntry().isPresent()) {
            ci.cancel();
        }
    }

}
