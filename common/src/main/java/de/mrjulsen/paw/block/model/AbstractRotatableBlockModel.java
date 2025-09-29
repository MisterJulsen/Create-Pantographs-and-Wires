package de.mrjulsen.paw.block.model;

import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.DLModel;
import de.mrjulsen.mcdragonlib.client.model.mesh.Face;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import de.mrjulsen.paw.block.abstractions.IConicalShape;
import de.mrjulsen.paw.block.abstractions.IRotatableBlock;
import de.mrjulsen.paw.client.model.ETransformationType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

public abstract class AbstractRotatableBlockModel extends DLModel {

    protected abstract Mesh getBaseMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context);

    @Override
    protected Mesh getMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context) {
        Mesh mesh = getBaseMesh(type, originalModel, state, random, context);

        if (state.getBlock() instanceof IRotatableBlock rot) {
            Vec2 pivot2D = rot.getRotationPivotPoint(state);
            Vector3f pivot = new Vector3f(pivot2D.x + 0.5f, 0, pivot2D.y + 0.5f);

            // Stretch
            Axis axis = rot.transformOnAxis(state);
            float scaleFactor = (float)rot.getScaleForRotation(state);
            ETransformationType typeFlag = ETransformationType.NONE;
            for (Face face : mesh.getFaces()) {
                typeFlag = ETransformationType.getByIndex(face.getTintIndex());
                if (axis != null && typeFlag.isScale()) {
                    Vector3f scaleVec = switch (axis) {
                        case X -> new Vector3f(scaleFactor, 1, 1);
                        case Z -> new Vector3f(1, 1, scaleFactor);
                        default -> new Vector3f(1, 1, 1);
                    };
                    face.scale(scaleVec, pivot);
                }
            }            

            // Translation
            float angleRadians = (float) Math.toRadians(rot.getRelativeYRotation(state));
            float h = 1f / (float) Math.cos(angleRadians);
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            for (Face face : mesh.getFaces()) {
                typeFlag = ETransformationType.getByIndex(face.getTintIndex());
                if (axis != null && typeFlag == ETransformationType.TRANSLATE) {
                    switch (axis) {
                        case X -> face.translate(facing.getAxisDirection().getStep() * (1f - h), 0, 0);
                        case Z -> face.translate(0, 0, facing.getAxisDirection().getStep() * (1f - h));
                        default -> {}
                    }
                }
            }

            // Conical Shape
            if (state.getBlock() instanceof IConicalShape cone) {
                Vec2 coneTarget = cone.coneTarget(state);
                Vec2 coneOffset = cone.coneOffset(state);

                mesh.getTransformableElements().forEach(v -> {
                    Vector3f pos = v.getPos();
                    double sX = -Math.signum(pos.x - coneTarget.x);
                    double sZ = -Math.signum(pos.z - coneTarget.y);
                    pos.add((float) (sX * pos.y * coneOffset.x), 0, (float) (sZ * pos.y * coneOffset.y));
                });
            }
            
            // Rotation
            mesh.rotate(com.mojang.math.Axis.YP.rotationDegrees(rot.getRelativeYRotation(state)), pivot);

            // Offset
            Vec2 offset = rot.getOffset(state);
            mesh.translate(offset.x, 0, offset.y);
        }

        mesh.cleanUp();
        return mesh;
    }
    
}
