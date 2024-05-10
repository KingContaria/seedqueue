package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.compat.WorldPreviewCompat;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    @ModifyExpressionValue(
            method = "*",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/world/ClientWorld;worldRenderer:Lnet/minecraft/client/render/WorldRenderer;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private WorldRenderer modifyWorldRenderer(WorldRenderer worldRenderer) {
        if (this.isWorldPreview()) {
            return WorldPreview.worldRenderer;
        }
        return worldRenderer;
    }

    @Inject(
            method = "addEntity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void captureEntitiesFromServer(int id, Entity entity, CallbackInfo ci) {
        WorldPreviewProperties wpProperties = WorldPreviewCompat.SERVER_WP_PROPERTIES.get();
        if (wpProperties != null) {
            wpProperties.addEntity(entity);
            ci.cancel();
        }
    }

    @Unique
    private boolean isWorldPreview() {
        return (Object) this == WorldPreview.world;
    }
}
