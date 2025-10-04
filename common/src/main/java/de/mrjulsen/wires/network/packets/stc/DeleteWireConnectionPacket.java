package de.mrjulsen.wires.network.packets.stc;

import java.util.UUID;
import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.net.IPacketBase;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.network.DeleteWireSyncData;
import de.mrjulsen.wires.util.ClientUtils;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.FriendlyByteBuf;

public class DeleteWireConnectionPacket implements IPacketBase<DeleteWireConnectionPacket> {

    private DeleteWireSyncData data;

    public DeleteWireConnectionPacket() {}

    public DeleteWireConnectionPacket(DeleteWireSyncData data) {
        this.data = data;

    }

    @Override
    public void encode(DeleteWireConnectionPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data.toNbt());
    }

    @Override
    public DeleteWireConnectionPacket decode(FriendlyByteBuf buf) {
        return new DeleteWireConnectionPacket(DeleteWireSyncData.fromNbt(buf.readNbt()));
    }

    @Override
    public void handle(DeleteWireConnectionPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {            
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                WireGraphClient graph = WireGraphManager.getClient(ClientUtils.level(), packet.data.id());
                for (UUID id : packet.data.wireEdgeIds()) {
                    graph.removeEdge(id);
                }
            });
        });
    }
    
}
