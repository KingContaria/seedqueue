package me.contaria.seedqueue.mixin.server.synchronization;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.collection.WeightedList;
import net.minecraft.world.gen.stateprovider.WeightedBlockStateProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Random;

@Mixin(WeightedBlockStateProvider.class)
public abstract class WeightedBlockStateProviderMixin {

    @WrapOperation(
            method = "addState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/collection/WeightedList;add(Ljava/lang/Object;I)Lnet/minecraft/util/collection/WeightedList;"
            )
    )
    private synchronized WeightedList<?> synchronizeAddState(WeightedList<?> list, Object item, int weight, Operation<WeightedList<?>> original) {
        return original.call(list, item, weight);
    }

    @WrapOperation(
            method = "getBlockState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/collection/WeightedList;pickRandom(Ljava/util/Random;)Ljava/lang/Object;"
            )
    )
    private synchronized Object synchronizeGetBlockState(WeightedList<?> list, Random random, Operation<Object> original) {
        return original.call(list, random);
    }
}
