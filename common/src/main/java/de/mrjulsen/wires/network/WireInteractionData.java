package de.mrjulsen.wires.network;

import java.util.Optional;

import de.mrjulsen.paw.data.WireHitResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;

public record WireInteractionData(InteractionHand hand, WireHitResult hit) {

    private static final String NBT_HAND = "Hand";
    private static final String NBT_HIT = "Hit";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt(NBT_HAND, hand.ordinal());
        nbt.put(NBT_HIT, hit.toNbt());
        return nbt;
    }

    public static Optional<WireInteractionData> fromNbt(CompoundTag nbt) {
        return WireHitResult.fromNbt(nbt.getCompound(NBT_HIT)).map(x -> new WireInteractionData(
            InteractionHand.values()[nbt.getInt(NBT_HAND)],
            x
        ));
    }
}
