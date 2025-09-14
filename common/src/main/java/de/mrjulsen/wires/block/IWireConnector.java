package de.mrjulsen.wires.block;

import java.util.Optional;

import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.WireBaseItem.CustomData;
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

    @Deprecated
    public static final String NBT_WIRE_ATTACH_POINT = "WireAttachPoint";

    ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex);
    
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
