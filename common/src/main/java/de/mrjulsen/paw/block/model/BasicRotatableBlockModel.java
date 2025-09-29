package de.mrjulsen.paw.block.model;

import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.BasicMesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.Face;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class BasicRotatableBlockModel extends AbstractRotatableBlockModel {

    @Override
    protected Mesh getBaseMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context) {
        return fromBakedModel(state, originalModel, random);
    }

    public static BasicMesh fromBakedModel(BlockState state, BakedModel srcModel, RandomSource random) {
        BasicMesh mesh = new BasicMesh();
        Direction[] directions = new Direction[Direction.values().length + 1];
        System.arraycopy(Direction.values(), 0, directions, 0, Direction.values().length);
        for (Direction side : directions) {
            for (BakedQuad quad : srcModel.getQuads(state, side, random)) {
                mesh.addFace(new Face(quad, side));
            }
        }
        return mesh;
    }
    
}
