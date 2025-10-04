package de.mrjulsen.paw.network.stc;

import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.net.IPacketBase;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.data.WireSettingsData;
import de.mrjulsen.wires.item.MultiWireItem;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class UpdateWireSettingsPacket implements IPacketBase<UpdateWireSettingsPacket> {

    private WireSettingsData data;

    public UpdateWireSettingsPacket() {}

    public UpdateWireSettingsPacket(WireSettingsData data) {
        this.data = data;

    }

    @Override
    public void encode(UpdateWireSettingsPacket packet, FriendlyByteBuf buf) {
        CompoundTag nbt = new CompoundTag();
        packet.data.toNbt(nbt);
        buf.writeNbt(nbt);
    }

    @Override
    public UpdateWireSettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdateWireSettingsPacket(WireSettingsData.fromNbt(buf.readNbt()));
    }

    @Override
    public void handle(UpdateWireSettingsPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {            
            Player player = contextSupplier.get().getPlayer();
            if (!MultiWireItem.setNbt(player.getMainHandItem(), packet.data)) {
                if (!MultiWireItem.setNbt(player.getOffhandItem(), packet.data)) {
                    PantographsAndWires.LOGGER.warn("Could not set NBT for 'mainhand=" + player.getMainHandItem() + ",offhand=" + player.getOffhandItem() + "'' because this item is not a MultiWireItem.");
                }
            }
        });
    }
    
}
