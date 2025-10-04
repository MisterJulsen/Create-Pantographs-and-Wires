package de.mrjulsen.wires.network.packets.cts;

import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import de.mrjulsen.wires.item.IWireInteractableItem;
import de.mrjulsen.wires.network.WireInteractionData;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public class WireInteractionPacket implements IPacketBase<WireInteractionPacket> {

    private WireInteractionData data;

    public WireInteractionPacket() {}

    public WireInteractionPacket(WireInteractionData data) {
        this.data = data;

    }

    @Override
    public void encode(WireInteractionPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data.toNbt());
    }

    @Override
    public WireInteractionPacket decode(FriendlyByteBuf buf) {
        return new WireInteractionPacket(WireInteractionData.fromNbt(buf.readNbt()).orElse(null));
    }

    @Override
    public void handle(WireInteractionPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {            
            if (packet.data == null)
                return;            

            DragonLib.getCurrentServer().ifPresent(x -> {
                x.execute(() -> {
                    Player player = contextSupplier.get().getPlayer();
                    InteractionResult interactionresult = null;
                    if (player.getItemInHand(packet.data.hand()).getItem() instanceof IWireInteractableItem i) {
                        interactionresult = i.interactWithWire(player.level(), player, packet.data.hand(), packet.data.hit());
                    }
                    if (interactionresult == null || !interactionresult.consumesAction()) {
                        interactionresult = packet.data.hit().getWireId().type().use(player.level(), player, packet.data.hand(), packet.data.hit());
                    }
                });
            });
        });
    }
    
}
