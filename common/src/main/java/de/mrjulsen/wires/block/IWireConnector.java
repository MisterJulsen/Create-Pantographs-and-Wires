package de.mrjulsen.wires.block;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface containing all methods for a wire connector.
 */
public interface IWireConnector {

    public static final String NBT_WIRE_ATTACH_POINT = "WireAttachPoint";

    /**
     * Generates the data to create the wire renderer.
     * @param level The level the wire is in.
     * @param pos The pos of the current connector.
     * @param state The {@code BlockState} of the current connector
     * @param itemData Additional item data
     * @param firstPoint Whether this is the first connection point
     * @return A {@code CompoundTag} containing all data from this connector, e.g. the attachment point. This data is then passed to the respective wire type where the data can be read to create the wire.
     */
    CompoundTag wireRenderData(Level level, BlockPos pos, BlockState state, CompoundTag itemData, int index);
    
    default boolean onAttachWireTo(Level level, BlockPos pos, BlockState state, Player player, Optional<UseOnContext> hit, CompoundTag pointData, int index) {
        return true;
    }

    default boolean beforeCreateWireConnection(Level level, BlockPos pos, BlockState state, Player player, Optional<UseOnContext> hit, CompoundTag itemData, int connectionIndex) {
        return true;
    }
    
    default boolean afterCreateWireConnection(Level level, BlockPos pos, BlockState state, Player player, Optional<UseOnContext> hit, CompoundTag itemData, int connectionIndex) {
        return true;
    }

    default boolean canConnectWire(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }
}
