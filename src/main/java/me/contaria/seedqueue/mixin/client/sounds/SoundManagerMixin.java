package me.contaria.seedqueue.mixin.client.sounds;

import me.contaria.seedqueue.interfaces.SQSoundManager;
import me.contaria.seedqueue.interfaces.SQSoundSystem;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin implements SQSoundManager {
    @Shadow
    @Final
    private SoundSystem soundSystem;

    @Override
    public void seedQueue$stopAllExceptSeedQueueSounds() {
        ((SQSoundSystem) this.soundSystem).seedQueue$stopAllExceptSeedQueueSounds();
    }
}
