package me.contaria.seedqueue.mixin.compat.atum;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.gui.wall.SeedQueueWallScreen;
import me.voidxwalker.autoreset.Atum;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = Atum.class, remap = false)
public abstract class AtumMixin {

    @ModifyArg(
            method = "createNewWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;openScreen(Lnet/minecraft/client/gui/screen/Screen;)V",
                    remap = true
            )
    )
    private static Screen openSeedQueueWallScreen(Screen screen) {
        if (SeedQueue.isActive() && SeedQueue.config.shouldUseWall()) {
            if (SeedQueue.config.bypassWall) {
                Optional<SeedQueueEntry> nextSeedQueueEntry = SeedQueue.getEntryMatching(entry -> entry.isReady() && entry.isLocked());
                if (nextSeedQueueEntry.isPresent()) {
                    SeedQueue.selectedEntry = nextSeedQueueEntry.get();
                    return screen;
                }
            }
            ModCompat.standardsettings$cacheAndReset();
            return new SeedQueueWallScreen(screen, SeedQueue.config.rows, SeedQueue.config.columns);
        }
        return screen;
    }

    @Inject(
            method = "stopRunning",
            at = @At("HEAD")
    )
    private static void stopSeedQueueOnAtumStop(CallbackInfo ci) {
        SeedQueue.stop();
    }
}
