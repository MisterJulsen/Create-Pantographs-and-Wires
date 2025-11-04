package de.mrjulsen.wires.network.packets.stc;

import java.util.UUID;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.network.DeleteWireSyncData;
import de.mrjulsen.wires.util.ClientUtils;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.nbt.CompoundTag;

public class DeleteWireConnectionPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";

    private DeleteWireSyncData data;

    public DeleteWireConnectionPacketData(DLStatus status) {
        super(status);
    }
    
    public DeleteWireConnectionPacketData(DeleteWireSyncData data) {
        super(DLStatus.OK);
        this.data = data;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.put(NBT_DATA, data.toNbt());
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.data = DeleteWireSyncData.fromNbt(nbt.getCompound(NBT_DATA));
    }    

    public static void handle(DeleteWireConnectionPacketData packet, NetworkPacketContext contextSupplier) {
        contextSupplier.queue(() -> {            
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                try {
                    WireGraphClient graph = WireGraphManager.getClient(ClientUtils.level(), packet.data.id());
                    for (UUID id : packet.data.wireEdgeIds()) {
                        graph.removeEdge(id);
                    }         
                } catch (Exception e) {
                    PantographsAndWires.LOGGER.error("Unable to process DeleteWireConnectionPacket:", e);
                }
                
            });
        });
    }
    
}
