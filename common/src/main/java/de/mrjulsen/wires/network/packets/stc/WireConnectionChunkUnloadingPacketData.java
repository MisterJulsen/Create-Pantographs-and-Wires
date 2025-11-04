package de.mrjulsen.wires.network.packets.stc;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.network.WireChunkUnloadingData;
import de.mrjulsen.wires.util.ClientUtils;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.nbt.CompoundTag;

public class WireConnectionChunkUnloadingPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";

    private WireChunkUnloadingData data;

    public WireConnectionChunkUnloadingPacketData(DLStatus status) {
        super(status);
    }

    public WireConnectionChunkUnloadingPacketData(WireChunkUnloadingData data) {
        super(DLStatus.OK);
        this.data = data;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.put(NBT_DATA, data.toNbt());
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.data = WireChunkUnloadingData.fromNbt(nbt.getCompound(NBT_DATA));
    }

    public static void handle(WireConnectionChunkUnloadingPacketData packet, NetworkPacketContext contextSupplier) {
        contextSupplier.queue(() -> {            
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
                try {
                    WireGraphManager.getClient(ClientUtils.level(), packet.data.id()).onClientChunkUnloading(packet.data);                
                } catch (Exception e) {
                    PantographsAndWires.LOGGER.error("Unable to process WireConnectionChunkUnloadingPacket:", e);
                }
            });
        });
    }
    
}
