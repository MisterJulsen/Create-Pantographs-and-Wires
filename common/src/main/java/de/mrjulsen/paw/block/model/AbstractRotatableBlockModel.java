package de.mrjulsen.paw.block.model;

import org.joml.Vector3f;

import com.mojang.math.Axis;

import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.DLModel;
import de.mrjulsen.mcdragonlib.client.model.mesh.DLModel.ModelType;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import de.mrjulsen.paw.block.abstractions.IRotatableBlock;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

public abstract class AbstractRotatableBlockModel extends DLModel {

    protected abstract Mesh getBaseMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context);

    @Override
    protected Mesh getMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context) {
        Mesh mesh = getBaseMesh(type, originalModel, state, random, context);
        if (state.getBlock() instanceof IRotatableBlock rot) {
            Vec2 pivot = rot.getRotationPivotPoint(state);
            mesh.rotate(Axis.YP.rotationDegrees(rot.getRelativeYRotation(state)), new Vector3f(pivot.x + 0.5f, 0, pivot.y + 0.5f));
        }
        return mesh;
    }
    
}
