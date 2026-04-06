package de.mrjulsen.paw.network.packets;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.wires.item.IWireItemBase;
import de.mrjulsen.wires.item.MultiWireItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class ClearWireConnectionPacketData extends NetworkPacketData {

    public ClearWireConnectionPacketData(DLStatus status) {
        super(status);
    }

    public ClearWireConnectionPacketData() {
        super(DLStatus.OK);
    }

    @Override
    protected void write(CompoundTag nbt) {
    }

    @Override
    protected void read(CompoundTag nbt) {
    }

    public static void handle(ClearWireConnectionPacketData packet, NetworkPacketContext contextSupplier) {
        contextSupplier.queue(() -> {
            Player player = contextSupplier.getPlayer();
            if (player.getMainHandItem().getItem() instanceof MultiWireItem) {
                IWireItemBase.clear(player, player.getMainHandItem());
            } else if (player.getOffhandItem().getItem() instanceof MultiWireItem) {
                IWireItemBase.clear(player, player.getOffhandItem());
            }
        });
    }
    
}
