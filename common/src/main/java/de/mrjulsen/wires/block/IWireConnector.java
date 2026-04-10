package de.mrjulsen.wires.block;

import java.util.Optional;

import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
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

    ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex);

    default boolean canConnectWire(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }
}
