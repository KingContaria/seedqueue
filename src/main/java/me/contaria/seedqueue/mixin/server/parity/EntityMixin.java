package me.contaria.seedqueue.mixin.server.parity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.contaria.seedqueue.interfaces.SQMinecraftServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Entity.class, priority = 500)
public abstract class EntityMixin {

    @ModifyExpressionValue(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/atomic/AtomicInteger;incrementAndGet()I"
            )
    )
    private int incrementEntityIdPerServer(int id, EntityType<?> type, World world) {
        // fallback for worldpreview entities
        // technically if the counter overflows and wraps back all the way around,
        // it could naturally reach -1 but that is imo within the realm of "whatever, shit happens"
        if (id == -1) {
            return id;
        }
        MinecraftServer server = world.getServer();
        if (server == null) {
            // for entities created clientside, use the entity id counter of the currently active server
            // this preserves the behaviour of clientside entities affecting the serverside id counter
            server = MinecraftClient.getInstance().getServer();
        }
        if (server == null) {
            // for entities created clientside while no server is active just use the id
            return id;
        }
        // by storing the max entity id counter per server, we ensure entity ID's will be in order
        // we initialize them with the current max id to preserve parity when seedqueue isn't active
        // and also avoid entity ID's starting at 0 on every server when using seedqueue
        return ((SQMinecraftServer) server).seedQueue$incrementAndGetEntityID(id);
    }
}
