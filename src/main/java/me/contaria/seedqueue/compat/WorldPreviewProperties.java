package me.contaria.seedqueue.compat;

import me.contaria.seedqueue.mixin.accessor.CameraAccessor;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;

import java.util.Objects;
import java.util.Queue;

/**
 * Stores all objects defining a world preview to be loaded when needed using {@link WorldPreviewProperties#apply}.
 */
public class WorldPreviewProperties {
    private final ClientWorld world;
    private final ClientPlayerEntity player;
    private final ClientPlayerInteractionManager interactionManager;
    private final Camera camera;
    private final Queue<Packet<?>> packetQueue;

    public WorldPreviewProperties(ClientWorld world, ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager, Camera camera, Queue<Packet<?>> packetQueue) {
        this.world = Objects.requireNonNull(world);
        this.player = Objects.requireNonNull(player);
        this.interactionManager = Objects.requireNonNull(interactionManager);
        this.camera = Objects.requireNonNull(camera);
        this.packetQueue = Objects.requireNonNull(packetQueue);
    }

    public ClientWorld getWorld() {
        return this.world;
    }

    public ClientPlayerEntity getPlayer() {
        return this.player;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public Queue<Packet<?>> getPacketQueue() {
        return this.packetQueue;
    }

    /**
     * @return The perspective of this previews {@link WorldPreviewProperties#camera}.
     */
    // perspective is set earlier than the settingsCache because it is important for chunk data culling
    // the same does not go for FOV because we simply use 110 (Quake Pro) for culling because most people will/should just use it on the wall screen anyway
    // see also WorldPreviewMixin#modifyPerspective_inQueue, ServerChunkManagerMixin#modifyCullingFov_inQueue
    public int getPerspective() {
        return this.camera.isThirdPerson() ? ((CameraAccessor) this.camera).seedQueue$isInverseView() ? 2 : 1 : 0;
    }

    /**
     * Sets {@link WorldPreview} properties to the values stored in this {@link WorldPreviewProperties}.
     *
     * @see WorldPreview#set
     */
    public void apply() {
        WorldPreview.set(this.world, this.player, this.interactionManager, this.camera, this.packetQueue);
    }
}
