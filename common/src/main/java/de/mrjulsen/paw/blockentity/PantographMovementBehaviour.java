package de.mrjulsen.paw.blockentity;

import org.joml.Vector3d;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

public class PantographMovementBehaviour implements MovementBehaviour {

	@Override
	public void tick(MovementContext context) {       
        if (context.contraption.entity.level().isClientSide() &&
            context.contraption.presentBlockEntities.containsKey(context.localPos) &&
            context.contraption.presentBlockEntities.get(context.localPos) instanceof PantographBlockEntity be
        ) {
            Direction dir = context.state.getValue(HorizontalDirectionalBlock.FACING);
            if (dir.getAxis() == Axis.X) {
                dir = dir.getOpposite();
            }
            final double yRot = dir.toYRot();
            be.updateContraptionValues(new Vector3d(context.position.x(), context.position.y() - 0.5D + PantographBlockEntity.MIN_HEIGHT, context.position.z()), (v) -> {
                Vec3 r = VecHelper.rotate(new Vec3(v.x(), v.y(), v.z()), yRot, Axis.Y);
                r = context.rotation.apply(r);
                return new Vector3d(r.x(), r.y(), r.z());
            });
            be.contraptionTick();
        }
	}
}
