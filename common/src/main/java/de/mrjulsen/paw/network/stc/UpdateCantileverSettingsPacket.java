package de.mrjulsen.paw.network.stc;

import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.net.IPacketBase;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.data.CantileverSettingsData;
import de.mrjulsen.paw.item.CantileverBlockItem;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class UpdateCantileverSettingsPacket implements IPacketBase<UpdateCantileverSettingsPacket> {

    private CantileverSettingsData data;

    public UpdateCantileverSettingsPacket() {}

    public UpdateCantileverSettingsPacket(CantileverSettingsData data) {
        this.data = data;

    }

    @Override
    public void encode(UpdateCantileverSettingsPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data.toNbt());
    }

    @Override
    public UpdateCantileverSettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdateCantileverSettingsPacket(CantileverSettingsData.fromNbt(buf.readNbt()));
    }

    @Override
    public void handle(UpdateCantileverSettingsPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {            
            Player player = contextSupplier.get().getPlayer();
            if (!CantileverBlockItem.setNbt(player.getMainHandItem(), packet.data)) {
                if (!CantileverBlockItem.setNbt(player.getOffhandItem(), packet.data)) {
                    PantographsAndWires.LOGGER.warn("Could not set NBT for 'mainhand=" + player.getMainHandItem() + ",offhand=" + player.getOffhandItem() + "'' because this item is not a CantileverBlockItem.");
                }
            }
        });
    }
    
}
