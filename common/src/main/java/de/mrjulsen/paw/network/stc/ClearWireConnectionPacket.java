package de.mrjulsen.paw.network.stc;

import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.net.IPacketBase;
import de.mrjulsen.wires.item.IWireItemBase;
import de.mrjulsen.wires.item.MultiWireItem;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class ClearWireConnectionPacket implements IPacketBase<ClearWireConnectionPacket> {

    public ClearWireConnectionPacket() {}

    @Override
    public void encode(ClearWireConnectionPacket packet, FriendlyByteBuf buf) {
    }

    @Override
    public ClearWireConnectionPacket decode(FriendlyByteBuf buf) {
        return new ClearWireConnectionPacket();
    }

    @Override
    public void handle(ClearWireConnectionPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {            
            Player player = contextSupplier.get().getPlayer();
            if (player.getMainHandItem().getItem() instanceof MultiWireItem) {
                IWireItemBase.clear(player.getMainHandItem());
            } else if (player.getOffhandItem().getItem() instanceof MultiWireItem) {
                IWireItemBase.clear(player.getOffhandItem());
            }
        });
    }
    
}
