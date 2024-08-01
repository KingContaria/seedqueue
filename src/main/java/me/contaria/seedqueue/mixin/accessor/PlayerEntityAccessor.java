package me.contaria.seedqueue.mixin.accessor;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface PlayerEntityAccessor {
    @Accessor("PLAYER_MODEL_PARTS")
    static TrackedData<Byte> seedQueue$getPLAYER_MODEL_PARTS() {
        throw new UnsupportedOperationException();
    }
}
