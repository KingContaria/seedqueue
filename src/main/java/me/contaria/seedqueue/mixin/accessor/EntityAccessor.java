package me.contaria.seedqueue.mixin.accessor;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("MAX_ENTITY_ID")
    static AtomicInteger seedQueue$getMAX_ENTITY_ID() {
        throw new UnsupportedOperationException();
    }
}
