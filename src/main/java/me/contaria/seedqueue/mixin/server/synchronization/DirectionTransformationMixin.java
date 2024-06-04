package me.contaria.seedqueue.mixin.server.synchronization;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.DirectionTransformation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DirectionTransformation.class)
public abstract class DirectionTransformationMixin {

    @WrapMethod(
            method = "map"
    )
    private Direction synchronizeMapDirections(Direction direction, Operation<Direction> original) {
        synchronized (this) {
            return original.call(direction);
        }
    }
}
