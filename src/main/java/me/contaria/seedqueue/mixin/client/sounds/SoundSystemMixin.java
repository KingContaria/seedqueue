package me.contaria.seedqueue.mixin.client.sounds;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.interfaces.SQSoundSystem;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin implements SQSoundSystem {
    @Shadow
    @Final
    private Map<SoundInstance, Channel.SourceManager> sources;

    @Shadow
    public abstract void stop(SoundInstance soundInstance);

    @ModifyExpressionValue(
            method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/sound/SoundLoader;loadStatic(Lnet/minecraft/util/Identifier;)Ljava/util/concurrent/CompletableFuture;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/sound/SoundLoader;loadStreamed(Lnet/minecraft/util/Identifier;Z)Ljava/util/concurrent/CompletableFuture;"
                    )
            }
    )
    private CompletableFuture<AudioStream> crashBrokenSeedQueueSoundFiles(CompletableFuture<AudioStream> audio, SoundInstance soundInstance) {
        if (soundInstance.getId().getNamespace().equals("seedqueue") && audio.isCompletedExceptionally()) {
            throw new RuntimeException("Tried to play a broken sound file from a SeedQueue customization pack! If you are using empty sound files to mute sounds on the wall screen, use short silent ones instead.");
        }
        return audio;
    }

    // see SoundSystem#closeSounds
    @Override
    public void seedQueue$stopAllExceptSeedQueueSounds() {
        for (SoundInstance sound : this.sources.keySet()) {
            if (!sound.getId().getNamespace().equals("seedqueue")) {
                this.stop(sound);
            }
        }
    }
}
