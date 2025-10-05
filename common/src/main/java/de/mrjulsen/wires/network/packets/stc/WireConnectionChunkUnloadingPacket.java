package de.mrjulsen.wires.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.net.IPacketBase;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.network.WireChunkUnloadingData;
import de.mrjulsen.wires.util.ClientUtils;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.FriendlyByteBuf;

public class WireConnectionChunkUnloadingPacket implements IPacketBase<WireConnectionChunkUnloadingPacket> {

    private WireChunkUnloadingData data;

    public WireConnectionChunkUnloadingPacket() {}

    public WireConnectionChunkUnloadingPacket(WireChunkUnloadingData data) {
        this.data = data;

    }

    @Override
    public void encode(WireConnectionChunkUnloadingPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data.toNbt());
    }

    @Override
    public WireConnectionChunkUnloadingPacket decode(FriendlyByteBuf buf) {
        return new WireConnectionChunkUnloadingPacket(WireChunkUnloadingData.fromNbt(buf.readNbt()));
    }

    @Override
    public void handle(WireConnectionChunkUnloadingPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {            
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                WireGraphManager.getClient(ClientUtils.level(), packet.data.id()).onClientChunkUnloading(packet.data);
            });
        });
    }
    
}
