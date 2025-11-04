package de.mrjulsen.wires.network.packets.stc;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.network.WiresSyncData;
import de.mrjulsen.wires.util.ClientUtils;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.nbt.CompoundTag;

public class WireConnectorDataPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";

    private WiresSyncData.Wrapper data;

    public WireConnectorDataPacketData(DLStatus status) {
        super(status);
    }

    public WireConnectorDataPacketData(WiresSyncData.Wrapper data) {
        super(DLStatus.OK);
        this.data = data;

    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.put(NBT_DATA, data.toNbt());
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.data = WiresSyncData.Wrapper.fromNbt(nbt.getCompound(NBT_DATA));
    }
    
    public static void handle(WireConnectorDataPacketData packet, NetworkPacketContext contextSupplier) {
        contextSupplier.queue(() -> {            
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
