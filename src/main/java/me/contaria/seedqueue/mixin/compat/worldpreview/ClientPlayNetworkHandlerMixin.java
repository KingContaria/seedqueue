package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.contaria.seedqueue.compat.WorldPreviewCompat;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightingProvider;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Optional;

@Mixin(value = ClientPlayNetworkHandler.class, priority = 1500)
public abstract class ClientPlayNetworkHandlerMixin {

    @WrapWithCondition(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V"
            )
    )
    private boolean getWorldPreviewPropertiesFromServerThread(Packet<?> packet, PacketListener listener, ThreadExecutor<?> engine) {
        return WorldPreviewCompat.SERVER_WP_PROPERTIES.get() == null;
    }

    @ModifyExpressionValue(
            method = "*",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private ClientWorld modifyWorldOnServerThread(ClientWorld world) {
        return Optional.ofNullable(WorldPreviewCompat.SERVER_WP_PROPERTIES.get()).map(WorldPreviewProperties::getWorld).orElse(world);
    }

    @ModifyExpressionValue(
            method = "*",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private ClientPlayerEntity modifyPlayerOnServerThread(ClientPlayerEntity player) {
        return Optional.ofNullable(WorldPreviewCompat.SERVER_WP_PROPERTIES.get()).map(WorldPreviewProperties::getPlayer).orElse(player);
    }

    @WrapOperation(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;"
            )
    )
    private Entity getBufferedAddedEntities(ClientWorld world, int id, Operation<Entity> original) {
        return Optional.ofNullable(WorldPreviewCompat.SERVER_WP_PROPERTIES.get()).map(wpProperties -> wpProperties.getAddedEntity(id)).orElseGet(() -> original.call(world, id));
    }

    @Inject(
            method = "updateLighting",
            at = @At("TAIL")
    )
    private void doLightUpdatesOnServerThread(int chunkX, int chunkZ, LightingProvider provider, LightType type, int mask, int filledMask, Iterator<byte[]> updates, boolean bl, CallbackInfo ci) {
        provider.doLightUpdates(Integer.MAX_VALUE, true, true);
    }

    // see WorldPreview's ClientPlayNetworkHandlerMixin
    @WrapWithCondition(
            method = "onEntitySpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/SoundManager;play(Lnet/minecraft/client/sound/SoundInstance;)V"
            )
    )
    private boolean suppressSoundsOnServerThread(SoundManager manager, SoundInstance sound) {
        return WorldPreviewCompat.SERVER_WP_PROPERTIES.get() == null;
    }

    // see WorldPreview's ClientPlayNetworkHandlerMixin
    @WrapWithCondition(
            method = "onMobSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/SoundManager;playNextTick(Lnet/minecraft/client/sound/TickableSoundInstance;)V"
            )
    )
    private boolean suppressSoundsOnServerThread(SoundManager manager, TickableSoundInstance sound) {
        return WorldPreviewCompat.SERVER_WP_PROPERTIES.get() == null;
    }
}
