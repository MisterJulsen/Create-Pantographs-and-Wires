package de.mrjulsen.paw.registry;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverInsulatorsPlacement;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import de.mrjulsen.paw.item.CantileverBlockItem;
import de.mrjulsen.wires.item.IPawWireItemBase;
import de.mrjulsen.wires.item.MultiWireItem;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessorType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class ModNetworkAccessor {

    public static void init() {}
    
    public static record CantileverSettingsData(float width, float height, float catenaryHeight, ECantileverRegistrationArmType cantileverType, ECantileverInsulatorsPlacement insulatorPlacement, boolean showBracing) {
        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putFloat("Width", width);
            nbt.putFloat("Height", height);
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
                nbt.getFloat("CatenaryHeight"),
                ECantileverRegistrationArmType.values()[nbt.getInt("Type")],
                ECantileverInsulatorsPlacement.values()[nbt.getInt("InsulatorPlacement")],
                nbt.getBoolean("ShowBracing")
            );
        }
    }    
    public static final DataAccessorType<CantileverSettingsData, Void, Void> UPDATE_CANTILEVER_SETTINGS = DataAccessorType.register(new ResourceLocation(PantographsAndWires.MOD_ID, "update_cantilever_settings"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return CantileverSettingsData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            if (!CantileverBlockItem.setNbt(player.getMainHandItem(), in)) {
                if (!CantileverBlockItem.setNbt(player.getOffhandItem(), in)) {
                    PantographsAndWires.LOGGER.warn("Could not set NBT for 'mainhand=" + player.getMainHandItem() + ",offhand=" + player.getOffhandItem() + "'' because this item is not a CantileverBlockItem.");
                }
            }
            return false;
        }
    ));

    public static record WireSettingsData(IPawWireItemBase selectedType) {
        public void toNbt(CompoundTag nbt) {
            nbt.put("SelectedSubtype", selectedType.getRegistryType().wrap(selectedType));
        }

        public static WireSettingsData fromNbt(CompoundTag nbt) {
            return new WireSettingsData(
                ModWireRegistry.WIRE_SUBTYPES_REGISTRY.load(nbt.getCompound("SelectedSubtype"))
            );
        }
    }
    public static final DataAccessorType<WireSettingsData, Void, Void> UPDATE_WIRE_SETTINGS = DataAccessorType.register(new ResourceLocation(PantographsAndWires.MOD_ID, "update_wire_settings"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            in.toNbt(nbt);
        }, (nbt) -> {
            return WireSettingsData.fromNbt(nbt);
        }, (player, in, temp, nbt, iteration) -> {
            if (!MultiWireItem.setNbt(player.getMainHandItem(), in)) {
                if (!MultiWireItem.setNbt(player.getOffhandItem(), in)) {
                    PantographsAndWires.LOGGER.warn("Could not set NBT for 'mainhand=" + player.getMainHandItem() + ",offhand=" + player.getOffhandItem() + "'' because this item is not a MultiWireItem.");
                }
            }
            return false;
        }
    ));
    
}
