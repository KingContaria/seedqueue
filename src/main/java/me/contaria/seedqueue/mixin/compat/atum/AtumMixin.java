package me.contaria.seedqueue.mixin.compat.atum;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.gui.wall.SeedQueueWallScreen;
import me.contaria.seedqueue.sounds.SeedQueueSounds;
import me.voidxwalker.autoreset.Atum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = Atum.class, remap = false)
public abstract class AtumMixin {

    @WrapOperation(
            method = "createNewWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;openScreen(Lnet/minecraft/client/gui/screen/Screen;)V",
                    remap = true
            )
    )
    private static void openSeedQueueWallScreen(MinecraftClient client, Screen screen, Operation<Void> original) {
        if (!SeedQueue.isActive()) {
            original.call(client, screen);
            return;
        }
        ModCompat.standardsettings$cache();
        if (SeedQueue.config.shouldUseWall()) {
            if (SeedQueue.config.bypassWall) {
                Optional<SeedQueueEntry> nextSeedQueueEntry = SeedQueue.getEntryMatching(entry -> entry.isReady() && entry.isLocked());
                if (nextSeedQueueEntry.isPresent()) {
                    SeedQueueSounds.play(SeedQueueSounds.BYPASS_WALL);
                    SeedQueue.playEntry(nextSeedQueueEntry.get());
                    return;
                }
            }
            // standardsettings can cause the current screen to be re-initialized,
            // so we open an intermission screen to avoid atum reset logic being called twice
            client.openScreen(new ProgressScreen());
            ModCompat.standardsettings$reset();
            ModCompat.stateoutput$setWallState();
            SeedQueueSounds.play(SeedQueueSounds.OPEN_WALL);
            client.openScreen(new SeedQueueWallScreen());
            return;
        }
        if (!SeedQueue.playEntry()) {
            original.call(client, screen);
        }
    }

    @Inject(
            method = "stopRunning",
            at = @At("TAIL")
    )
    private static void stopSeedQueueOnAtumStop(CallbackInfo ci) {
        SeedQueue.stop();
    }
}
