package de.mrjulsen.paw.data;

import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.item.IPawWireItemBase;
import net.minecraft.nbt.CompoundTag;

public record WireSettingsData(IPawWireItemBase selectedType) {
    public void toNbt(CompoundTag nbt) {
        nbt.put("SelectedSubtype", selectedType.getRegistryType().wrap(selectedType));
    }

    public static WireSettingsData fromNbt(CompoundTag nbt) {
        return new WireSettingsData(
            ModWireRegistry.WIRE_SUBTYPES_REGISTRY.load(nbt.getCompound("SelectedSubtype"))
        );
    }
}
