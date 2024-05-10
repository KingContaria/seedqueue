package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.contaria.seedqueue.compat.WorldPreviewCompat;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.chunk.light.LightingProvider;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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

    @WrapWithCondition(
            method = "onChunkData",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/light/LightingProvider;doLightUpdates(IZZ)I"
            )
    )
    private boolean doNotDoLightUpdatesOnServerThread(LightingProvider instance, int maxUpdateCount, boolean doSkylight, boolean skipEdgeLightPropagation) {
        return WorldPreviewCompat.SERVER_WP_PROPERTIES.get() == null;
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
