package me.contaria.seedqueue.mixin.compat.worldpreview;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.gui.wall.SeedQueueWallScreen;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.network.Packet;
import org.mcsr.speedrunapi.config.SpeedrunConfigAPI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(WorldPreview.class)
public abstract class WorldPreviewMixin {

    @Inject(
            method = "configure",
            at = @At("HEAD")
    )
    private static void getSeedQueueEntry(CallbackInfo ci, @Share("seedQueueEntry") LocalRef<SeedQueueEntry> entry) {
        entry.set(SeedQueue.getEntry(Thread.currentThread()));
    }

    @WrapOperation(
            method = "configure",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/voidxwalker/worldpreview/WorldPreview;set(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/client/network/ClientPlayerInteractionManager;Lnet/minecraft/client/render/Camera;Ljava/util/Queue;)V"
            )
    )
    private static void doNotConfigureWorldPreview_inQueue(ClientWorld world, ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager, Camera camera, Queue<Packet<?>> packetQueue, Operation<Void> original, @Share("seedQueueEntry") LocalRef<SeedQueueEntry> entry) {
        if (entry.get() != null) {
            entry.get().setWorldPreviewProperties(new WorldPreviewProperties(world, player, interactionManager, camera, packetQueue));
            return;
        }
        original.call(world, player, interactionManager, camera, packetQueue);
    }

    @WrapWithCondition(
            method = "configure",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/data/DataTracker;set(Lnet/minecraft/entity/data/TrackedData;Ljava/lang/Object;)V"
            )
    )
    private static boolean doNotSetPlayerModelParts_inQueue(DataTracker tracker, TrackedData<?> key, Object object, @Share("seedQueueEntry") LocalRef<SeedQueueEntry> entry) {
        return entry.get() == null;
    }

    @ModifyExpressionValue(
            method = "configure",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/options/GameOptions;perspective:I"
            )
    )
    private static int modifyPerspective_inQueue(int perspective, @Share("seedQueueEntry") LocalRef<SeedQueueEntry> entry) {
        if (entry.get() != null) {
            return (int) SpeedrunConfigAPI.getConfigValueOptionally("standardsettings", "perspective").orElse(0);
        }
        return perspective;
    }

    // can be replaced by an expression of WorldPreviewConfig#dataLimit <= 100 when MixinExtras adds expressions
    @ModifyExpressionValue(
            method = "shouldStopAtPacket",
            at = @At(
                    value = "CONSTANT",
                    args = "intValue=100"
            )
    )
    private static int doNotAllowUnlimitedPackets_onWall(int unlimitedPackets) {
        if (SeedQueue.isOnWall()) {
            return Integer.MAX_VALUE;
        }
        return unlimitedPackets;
    }

    @WrapWithCondition(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/util/math/MatrixStack;F)V"
            )
    )
    private static boolean bufferInGameHudOnWall(InGameHud instance, MatrixStack matrixStack, float f) {
        if (!(MinecraftClient.getInstance().currentScreen instanceof SeedQueueWallScreen)) {
            return true;
        }
        SeedQueueWallScreen wall = (SeedQueueWallScreen) MinecraftClient.getInstance().currentScreen;
        if (wall.shouldUseInGameHudBuffer()) {
            if (wall.isInGameHudBuffered()) {
                return false;
            }
            wall.beginBufferingInGameHud();
        }
        return true;
    }
}
