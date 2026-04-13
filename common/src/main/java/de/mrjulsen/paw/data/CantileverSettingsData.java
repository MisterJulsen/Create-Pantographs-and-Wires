package de.mrjulsen.paw.data;

import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverInsulatorsPlacement;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import net.minecraft.nbt.CompoundTag;

public record CantileverSettingsData(float width, float height, float yOffset, float catenaryHeight, ECantileverRegistrationArmType cantileverType, ECantileverInsulatorsPlacement insulatorPlacement, boolean showBracing) {
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("Width", width);
        nbt.putFloat("Height", height);
        nbt.putFloat("YOffset", yOffset);
        nbt.putFloat("CatenaryHeight", catenaryHeight);
        nbt.putInt("Type", cantileverType.ordinal());
        nbt.putInt("InsulatorPlacement", insulatorPlacement.ordinal());
        nbt.putBoolean("ShowBracing", showBracing);
        return nbt;
    }

    public static CantileverSettingsData fromNbt(CompoundTag nbt) {
        return new CantileverSettingsData(
            nbt.getFloat("Width"),
            nbt.getFloat("Height"),
            nbt.getFloat("YOffset"),
            nbt.getFloat("CatenaryHeight"),
            ECantileverRegistrationArmType.values()[nbt.getInt("Type")],
            ECantileverInsulatorsPlacement.values()[nbt.getInt("InsulatorPlacement")],
            nbt.getBoolean("ShowBracing")
        );
    }
}