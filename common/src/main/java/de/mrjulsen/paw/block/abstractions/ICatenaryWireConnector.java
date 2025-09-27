package de.mrjulsen.paw.block.abstractions;

import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface ICatenaryWireConnector extends IWireConnector {
    
    public static final String NBT_TENSION_WIRE_ATTACH_POINT = "TensionWireAttachPoint";

    Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData itemData, int index);
}
