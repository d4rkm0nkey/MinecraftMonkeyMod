package me.teajay.cheeky.monkeys.client.network;

import java.util.Optional;

import me.teajay.cheeky.monkeys.common.networking.Packets;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;

public class EntityDispatcher {
    private EntityDispatcher() {}

    public static void spawnFrom(final MinecraftClient client, final ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        Packets.readFrom(buf, EntitySpawnS2CPacket::new).ifPresent(packet -> {
            Optional.ofNullable(packet.getEntityTypeId().create(client.world)).ifPresent(entity -> {
                entity.updateTrackedPosition(packet.getX(), packet.getY(), packet.getZ());
                entity.setVelocity(packet.getVelocityX(), packet.getVelocityY(), packet.getVelocityZ());
                entity.pitch = (float) (packet.getPitch() * 360) / 256.0F;
                entity.yaw = (float) (packet.getYaw() * 360) / 256.0F;
                entity.setEntityId(packet.getId());
                entity.setUuid(packet.getUuid());
                entity.setPos(packet.getX(), packet.getY(), packet.getZ());
                client.world.addEntity(packet.getId(), entity);
            });
        });
    }
}
