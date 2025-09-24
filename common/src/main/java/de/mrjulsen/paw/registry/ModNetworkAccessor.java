package de.mrjulsen.paw.registry;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverInsulatorsPlacement;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import de.mrjulsen.paw.item.CantileverBlockItem;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessorType;
import net.minecraft.resources.ResourceLocation;

public final class ModNetworkAccessor {

    public static void init() {}
    
    public static record CantileverSettingsData(float width, float height, float catenaryHeight, ECantileverRegistrationArmType cantileverType, ECantileverInsulatorsPlacement insulatorPlacement, boolean showBracing) {}
    public static final DataAccessorType<CantileverSettingsData, Void, Void> UPDATE_CANTILEVER_SETTINGS = DataAccessorType.register(new ResourceLocation(PantographsAndWires.MOD_ID, "update_cantilever_settings"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.putFloat("Width", in.width());
            nbt.putFloat("Height", in.height());
            nbt.putFloat("CatenaryHeight", in.catenaryHeight());
            nbt.putInt("Type", in.cantileverType().ordinal());
            nbt.putInt("InsulatorPlacement", in.insulatorPlacement().ordinal());
            nbt.putBoolean("ShowBracing", in.showBracing());
        }, (nbt) -> {
            return new CantileverSettingsData(
                nbt.getFloat("Width"),
                nbt.getFloat("Height"),
                nbt.getFloat("CatenaryHeight"),
                ECantileverRegistrationArmType.values()[nbt.getInt("Type")],
                ECantileverInsulatorsPlacement.values()[nbt.getInt("InsulatorPlacement")],
                nbt.getBoolean("ShowBracing")
            );
        }, (player, in, temp, nbt, iteration) -> {
            if (!CantileverBlockItem.setNbt(player.getMainHandItem(), in)) {
                if (!CantileverBlockItem.setNbt(player.getOffhandItem(), in)) {
                    PantographsAndWires.LOGGER.warn("Could not set NBT for 'mainhand=" + player.getMainHandItem() + ",offhand=" + player.getOffhandItem() + "'' because this item is not a CantileverBlockItem.");
                }
            }
            return false;
        }
    ));
    
}
