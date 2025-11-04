package de.mrjulsen.wires.network.packets.cts;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import de.mrjulsen.mcdragonlib.network.NetworkPacketData;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.wires.item.IWireInteractableItem;
import de.mrjulsen.wires.network.WireInteractionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public class WireInteractionPacketData extends NetworkPacketData {

    private static final String NBT_DATA = "Data";

    private WireInteractionData data;

    public WireInteractionPacketData(DLStatus status) {
        super(status);
    }

    public WireInteractionPacketData(WireInteractionData data) {
        super(DLStatus.OK);
        this.data = data;
    }

    @Override
    protected void write(CompoundTag nbt) {
        nbt.put(NBT_DATA, data.toNbt());
    }

    @Override
    protected void read(CompoundTag nbt) {
        this.data = WireInteractionData.fromNbt(nbt.getCompound(NBT_DATA)).orElse(null);
    }

    public static void handle(WireInteractionPacketData packet, NetworkPacketContext contextSupplier) {
        contextSupplier.queue(() -> {
            if (packet.data == null)
                return;            

            DragonLib.getCurrentServer().ifPresent(x -> {
                x.execute(() -> {
                    try {
                        Player player = contextSupplier.getPlayer();
                        InteractionResult interactionresult = null;
                        if (player.getItemInHand(packet.data.hand()).getItem() instanceof IWireInteractableItem i) {
                            interactionresult = i.interactWithWire(player.level(), player, packet.data.hand(), packet.data.hit());
                        }
                        if (interactionresult == null || !interactionresult.consumesAction()) {
                            interactionresult = packet.data.hit().getWireId().type().use(player.level(), player, packet.data.hand(), packet.data.hit());
                        }
                    } catch (Exception e) {
                        PantographsAndWires.LOGGER.error("Unable to process WireInteractionPacket:", e);
                    }                    
                });
            });
        });
    }
    
}
