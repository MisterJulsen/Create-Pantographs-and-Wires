package de.mrjulsen.paw.network.packets;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.data.CantileverSettingsData;
import de.mrjulsen.paw.item.CantileverBlockItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class UpdateCantileverSettingsPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";

    private CantileverSettingsData data;

    public UpdateCantileverSettingsPacketData(DLStatus status) {
        super(status);
    }

    public UpdateCantileverSettingsPacketData(CantileverSettingsData data) {
        super(DLStatus.OK);
        this.data = data;

    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.put(NBT_DATA, data.toNbt());
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.data = CantileverSettingsData.fromNbt(nbt.getCompound(NBT_DATA));
    }
    
    public static void handle(UpdateCantileverSettingsPacketData packet, NetworkPacketContext contextSupplier) {
        contextSupplier.queue(() -> {
            Player player = contextSupplier.getPlayer();
            if (!CantileverBlockItem.setNbt(player.getMainHandItem(), packet.data)) {
                if (!CantileverBlockItem.setNbt(player.getOffhandItem(), packet.data)) {
                    PantographsAndWires.LOGGER.warn("Could not set NBT for 'mainhand=" + player.getMainHandItem() + ",offhand=" + player.getOffhandItem() + "'' because this item is not a CantileverBlockItem.");
                }
            }
        });
    }
    
}
