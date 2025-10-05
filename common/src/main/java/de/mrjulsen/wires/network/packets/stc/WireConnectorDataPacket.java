package de.mrjulsen.wires.network.packets.stc;

import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.net.IPacketBase;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.network.WiresSyncData;
import de.mrjulsen.wires.util.ClientUtils;
import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.FriendlyByteBuf;

public class WireConnectorDataPacket implements IPacketBase<WireConnectorDataPacket> {

    private WiresSyncData.Wrapper data;

    public WireConnectorDataPacket() {}

    public WireConnectorDataPacket(WiresSyncData.Wrapper data) {
        this.data = data;

    }

    @Override
    public void encode(WireConnectorDataPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data.toNbt());
    }

    @Override
    public WireConnectorDataPacket decode(FriendlyByteBuf buf) {
        return new WireConnectorDataPacket(WiresSyncData.Wrapper.fromNbt(buf.readNbt()));
    }

    @Override
    public void handle(WireConnectorDataPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {            
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                try {
                    WiresSyncData d = packet.data.unwrap(ClientUtils.level());
                    WireGraphClient graph = WireGraphManager.getClient(ClientUtils.level(), d.id());
                    for (WireNode node : d.nodes().get()) {
                        if (node == null) continue;
                        graph.addNode(node);
                    }
                    for (WireEdge edge : d.edges().get()) {
                        if (edge == null) continue;
                        graph.addEdge(edge, d.forceUpdate());
                    }
                } catch (Exception e) {
                    PantographsAndWires.LOGGER.error("Unable to process WireConnectorDataPacket:", e);
                }
            });
        });
    }
    
}
