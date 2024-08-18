package me.contaria.seedqueue.mixin.client.sounds;

import me.contaria.seedqueue.interfaces.SQSoundSystem;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin implements SQSoundSystem {
    @Shadow
    @Final
    private Map<SoundInstance, Channel.SourceManager> sources;

    @Shadow
    public abstract void stop(SoundInstance soundInstance);

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
