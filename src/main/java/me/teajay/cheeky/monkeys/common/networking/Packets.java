package me.teajay.cheeky.monkeys.common.networking;

import io.netty.buffer.Unpooled;
import me.teajay.cheeky.monkeys.common.CheekyMonkeys;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class Packets {
    public static final Identifier SPAWN = new Identifier(CheekyMonkeys.MODID, "spawn");

    private Packets() {
    }
    
    public static Packet<?> newSpawnPacket(final Entity entity) {
        final PacketByteBuf bytes = new PacketByteBuf(Unpooled.buffer());
        try {
            new EntitySpawnS2CPacket(entity).write(bytes);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Failed to write bytes for " + entity, e);
        }
        return new CustomPayloadS2CPacket(SPAWN, bytes);
    }

    public static <T extends Packet<?>> Optional<T> readFrom(final PacketByteBuf bytes, final Supplier<T> packet) {
        final T deserializedPacket = packet.get();
        try {
            deserializedPacket.read(bytes);
        } catch (final IOException e) {
            if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
                throw new IllegalStateException("Reading " + deserializedPacket + " from " + bytes, e);
            }
            return Optional.empty();
        }
        return Optional.of(deserializedPacket);
    }

    public static <T extends Entity> void dispatchToAllWatching(final T entity, final Function<T, Packet<?>> packet) {
        PlayerLookup.tracking(entity).stream()
            .map(player -> ((ServerPlayerEntity) player).networkHandler)
            .forEach(handler -> handler.sendPacket(packet.apply(entity)));
    }
}
