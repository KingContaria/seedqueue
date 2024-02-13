package me.contaria.seedqueue.mixin.server.synchronization;

import com.google.common.collect.Maps;
import net.minecraft.util.math.AxisTransformation;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.DirectionTransformation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(DirectionTransformation.class)
public abstract class DirectionTransformationMixin {

    @Shadow
    @Nullable
    private Map<Direction, Direction> mappings;

    @Shadow
    @Final
    private AxisTransformation axisTransformation;

    @Shadow
    public abstract boolean shouldFlipDirection(Direction.Axis axis);

    /**
     * @author contaria
     * @reason Synchronize mappings creation, without it if another thread accesses this method while the mappings are still being evaluated it might return null unexpectedly.
     */
    @Overwrite
    public Direction map(Direction direction) {
        synchronized (this) {
            if (this.mappings == null) {
                this.mappings = Maps.newEnumMap(Direction.class);
                Direction[] var2 = Direction.values();
                int var3 = var2.length;

                for (int var4 = 0; var4 < var3; ++var4) {
                    Direction direction2 = var2[var4];
                    Direction.Axis axis = direction2.getAxis();
                    Direction.AxisDirection axisDirection = direction2.getDirection();
                    Direction.Axis axis2 = Direction.Axis.values()[this.axisTransformation.map(axis.ordinal())];
                    Direction.AxisDirection axisDirection2 = this.shouldFlipDirection(axis2) ? axisDirection.getOpposite() : axisDirection;
                    Direction direction3 = Direction.from(axis2, axisDirection2);
                    this.mappings.put(direction2, direction3);
                }
            }

            return this.mappings.get(direction);
        }
    }
}
