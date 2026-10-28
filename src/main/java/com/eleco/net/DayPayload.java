package com.eleco.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> cliente: día actual (1..5). */
public record DayPayload(int day) implements CustomPayload {
    public static final Id<DayPayload> ID = new Id<>(Identifier.of("eleco", "day"));
    public static final PacketCodec<RegistryByteBuf, DayPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, DayPayload::day, DayPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
