package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundCustomQueryPacket(int transactionId, CustomQueryPayload payload) implements Packet<ClientLoginPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundCustomQueryPacket> STREAM_CODEC = Packet.codec(
        ClientboundCustomQueryPacket::write, ClientboundCustomQueryPacket::new
    );
    private static final int MAX_PAYLOAD_SIZE = 1048576;

    private ClientboundCustomQueryPacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readVarInt(), readPayload(pBuffer.readResourceLocation(), pBuffer));
    }

    private static CustomQueryPayload readPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
        return readUnknownPayload(pId, pBuffer);
    }

    private static DiscardedQueryPayload readUnknownPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
        int i = pBuffer.readableBytes();
        if (i >= 0 && i <= 1048576) {
            pBuffer.skipBytes(i);
            return new DiscardedQueryPayload(pId);
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
        }
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.transactionId);
        pBuffer.writeResourceLocation(this.payload.id());
        this.payload.write(pBuffer);
    }

    @Override
    public PacketType<ClientboundCustomQueryPacket> type() {
        return LoginPacketTypes.CLIENTBOUND_CUSTOM_QUERY;
    }

    public void handle(ClientLoginPacketListener pHandler) {
        pHandler.handleCustomQuery(this);
    }
}