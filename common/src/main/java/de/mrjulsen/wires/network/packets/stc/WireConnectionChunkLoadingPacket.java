package de.mrjulsen.wires.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.net.IPacketBase;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.network.WireChunkLoadingData;
import de.mrjulsen.wires.util.ClientUtils;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.FriendlyByteBuf;

public class WireConnectionChunkLoadingPacket implements IPacketBase<WireConnectionChunkLoadingPacket> {

    private WireChunkLoadingData data;

    public WireConnectionChunkLoadingPacket() {}

    public WireConnectionChunkLoadingPacket(WireChunkLoadingData data) {
        this.data = data;

    }

    @Override
    public void encode(WireConnectionChunkLoadingPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data.toNbt());
    }

    @Override
    public WireConnectionChunkLoadingPacket decode(FriendlyByteBuf buf) {
        return new WireConnectionChunkLoadingPacket(WireChunkLoadingData.fromNbt(buf.readNbt()));
    }

    @Override
    public void handle(WireConnectionChunkLoadingPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {            
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                WireGraphManager.getClient(ClientUtils.level(), packet.data.id()).onClientChunkLoading(packet.data);
            });
        });
    }
    
}
