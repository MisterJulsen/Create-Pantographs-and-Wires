package de.mrjulsen.paw.blockentity;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class TickableBlockEntity<E extends TickableBlockEntity<E>> extends SyncedBlockEntity {

    protected TickableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }    
    
    public static <T extends BlockEntity> void globalTick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof TickableBlockEntity<?> instance) {
            instance.tick(level, pos, state);
        }
    }

    public abstract void tick(Level level, BlockPos pos, BlockState state);
}
