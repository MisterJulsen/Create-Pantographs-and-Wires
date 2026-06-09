package de.mrjulsen.paw.network.packets;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.data.WireSettingsData;
import de.mrjulsen.wires.item.MultiWireItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class UpdateWireSettingsPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";

    private WireSettingsData data;

    public UpdateWireSettingsPacketData(DLStatus status) {
        super(status);
    }

    public UpdateWireSettingsPacketData(WireSettingsData data) {
        super(DLStatus.OK);
        this.data = data;

    }

    @Override
    protected void write(CompoundTag nbt) {
        CompoundTag compound = new CompoundTag();
        data.toNbt(compound);
        nbt.put(NBT_DATA, compound);
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.data = WireSettingsData.fromNbt(nbt.getCompound(NBT_DATA));
    }

    public static void handle(UpdateWireSettingsPacketData packet, NetworkPacketContext contextSupplier) {
        contextSupplier.queue(() -> {            
            Player player = contextSupplier.getPlayer();
<<<<<<< HEAD
            if (!MultiWireItem.setNbt(player.getMainHandItem(), packet.data)) {
                if (!MultiWireItem.setNbt(player.getOffhandItem(), packet.data)) {
=======
            if (!MultiWireItem.setSettings(player.getMainHandItem(), packet.data)) {
                if (!MultiWireItem.setSettings(player.getOffhandItem(), packet.data)) {
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
                    PantographsAndWires.LOGGER.warn("Could not set NBT for 'mainhand=" + player.getMainHandItem() + ",offhand=" + player.getOffhandItem() + "'' because this item is not a MultiWireItem.");
                }
            }
        });
    }
    
}
