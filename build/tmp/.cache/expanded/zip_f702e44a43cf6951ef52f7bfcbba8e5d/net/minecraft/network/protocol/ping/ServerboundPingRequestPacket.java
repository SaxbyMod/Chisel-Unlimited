package net.minecraft.network.protocol.ping;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPingRequestPacket implements Packet<ServerPingPacketListener> {
    public static final StreamCodec<ByteBuf, ServerboundPingRequestPacket> STREAM_CODEC = Packet.codec(
        ServerboundPingRequestPacket::write, ServerboundPingRequestPacket::new
    );
    private final long time;

    public ServerboundPingRequestPacket(long pTime) {
        this.time = pTime;
    }

    private ServerboundPingRequestPacket(ByteBuf pBuffer) {
        this.time = pBuffer.readLong();
    }

    private void write(ByteBuf pBuffer) {
        pBuffer.writeLong(this.time);
    }

    @Override
    public PacketType<ServerboundPingRequestPacket> type() {
        return PingPacketTypes.SERVERBOUND_PING_REQUEST;
    }

    public void handle(ServerPingPacketListener p_336205_) {
        p_336205_.handlePingRequest(this);
    }

    public long getTime() {
        return this.time;
    }
}