package me.contaria.seedqueue.compat;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.Packet;

import java.util.Set;

public class WorldPreviewProperties {

    private final ClientWorld world;
    private final ClientPlayerEntity player;
    private final ClientPlayerInteractionManager interactionManager;
    private final Camera camera;
    private final Set<Packet<?>> packetQueue;

    public WorldPreviewProperties(ClientWorld world, ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager, Camera camera, Set<Packet<?>> packetQueue) {
        this.world = world;
        this.player = player;
        this.interactionManager = interactionManager;
        this.camera = camera;
        this.packetQueue = packetQueue;
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

    public Set<Packet<?>> getPacketQueue() {
        return this.packetQueue;
    }

    public void apply() {
        WorldPreview.set(this.world, this.player, this.interactionManager, this.camera, this.packetQueue);
    }
}
