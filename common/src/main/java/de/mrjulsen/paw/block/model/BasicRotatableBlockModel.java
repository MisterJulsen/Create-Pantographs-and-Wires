package de.mrjulsen.paw.block.model;

import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.BasicMesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class BasicRotatableBlockModel extends AbstractRotatableBlockModel {

    @Override
    protected Mesh getBaseMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context) {
        return BasicMesh.fromBakedModel(state, originalModel, random);
    }
}
