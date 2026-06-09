package de.mrjulsen.paw.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import de.mrjulsen.paw.block.abstractions.IConicalShape;
import de.mrjulsen.paw.block.abstractions.IRotatableBlock;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class RotatedBlockModel extends BakedModelExtension<BakedModel> {

	public RotatedBlockModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {

		if (!(state.getBlock() instanceof IRotatableBlock rot)) {
			return originalModel.getQuads(state, side, rand);
		}

		List<BakedQuad> templateQuads = originalModel.getQuads(state, side, rand);
		if (templateQuads.isEmpty())
			return templateQuads;

		double hAngle = rot.getRelativeYRotation(state);
		Vec2 pivot = rot.rotatedPivotPoint(state);

		Vec2 offset = (rot.getOffset(state));
		Vec3 verticalOffset = new Vec3(0.5f, 0.25f, 0.5f).subtract(pivot.x, 0, pivot.y);		

		int size = templateQuads.size();
		List<BakedQuad> quads = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			BakedQuad quad = clone(templateQuads.get(i));
			int[] vertexData = quad.getVertices();
			for (int j = 0; j < 4; j++) {
				Vec3 vec = getXYZ(vertexData, j);

				if (state.getBlock() instanceof IConicalShape cone) {
					Vec2 coneTarget = cone.coneTarget(state);
					Vec2 coneOffset = cone.coneOffset(state);
					double sX = -Math.signum(vec.x - coneTarget.x);
					double sZ = -Math.signum(vec.z - coneTarget.y);
	
					vec = vec.add(sX * vec.y * coneOffset.x, 0, sZ * vec.y * coneOffset.y);
				}

				setXYZ(vertexData, j, transformVector(state, side, rand, rot, vec, hAngle, verticalOffset, quad.getTintIndex(), offset, pivot));
			}
			quads.add(quad);
		}

		return quads;
	}

	private Vec3 transformVector(BlockState state, Direction side, RandomSource rand, IRotatableBlock rot, Vec3 v, double rotationAngle, Vec3 verticalOffset, int flags, Vec2 offset, Vec2 pivot) {
		ETransformationType transformationType = ETransformationType.getByIndex(flags);

		Axis axis = rot.transformOnAxis(state);
		if (axis != null) {
			if (transformationType.isScale()) {
				switch (axis) {
					case X -> {
						double distance = v.get(axis) - pivot.x;
						if (
							(transformationType == ETransformationType.SCALE_POSITIVE || transformationType == ETransformationType.SCALE_NEGATIVE) &&
							((distance > 0 && transformationType != ETransformationType.SCALE_POSITIVE) || (distance < 0 && transformationType != ETransformationType.SCALE_NEGATIVE))
						) break;
						double scaledDistance = distance * rot.getScaleForRotation(state);
						v = new Vec3(pivot.x + scaledDistance, v.y, v.z);
					}
					case Z -> {
						double distance = v.get(axis) - pivot.y;
						if (
							(transformationType == ETransformationType.SCALE_POSITIVE || transformationType == ETransformationType.SCALE_NEGATIVE) &&
							((distance > 0 && transformationType != ETransformationType.SCALE_POSITIVE) || (distance < 0 && transformationType != ETransformationType.SCALE_NEGATIVE))
						) break;
						double scaledDistance = distance * rot.getScaleForRotation(state);
						v = new Vec3(v.x, v.y, pivot.y + scaledDistance);
					}
					default -> throw new IllegalArgumentException("The scaling axis for block " + state.getBlock() + " must be horizontal. " + axis + " is not allowed!");
				}
			} else if (transformationType == ETransformationType.TRANSLATE) {
				switch (axis) {
					case X -> {
						double angleRadians = Math.toRadians(rot.getRelativeYRotation(state));
						float h = (float)1 / (float)Math.cos(angleRadians);
						v = new Vec3(v.x + state.getValue(HorizontalDirectionalBlock.FACING).getAxisDirection().getStep() * (1f - h), v.y, v.z);
					}
					case Z -> {
						double angleRadians = Math.toRadians(rot.getRelativeYRotation(state));
						float h = (float)1 / (float)Math.cos(angleRadians);
						v = new Vec3(v.x, v.y, v.z + state.getValue(HorizontalDirectionalBlock.FACING).getAxisDirection().getStep() * (1f - h));
					}
					default -> throw new IllegalArgumentException("The scaling axis for block " + state.getBlock() + " must be horizontal. " + axis + " is not allowed!");
				}
			}
		}
		
		v = v.add(verticalOffset);
		v = VecHelper.rotateCentered(v, rotationAngle, Axis.Y);
		v = v.subtract(verticalOffset);
		v = v.add(offset.x, 0, offset.y);
		return v;
	}

	/* Copy from: BakedQuadHelper */
	public static final VertexFormat FORMAT = DefaultVertexFormat.BLOCK;
	public static final int VERTEX_STRIDE = FORMAT.getVertexSize() / 4;

	public static final int X_OFFSET = 0;
	public static final int Y_OFFSET = 1;
	public static final int Z_OFFSET = 2;

	public static BakedQuad clone(BakedQuad quad) {
		return new BakedQuad(Arrays.copyOf(quad.getVertices(), quad.getVertices().length),
			quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
	}

	public static Vec3 getXYZ(int[] vertexData, int vertex) {
		float x = Float.intBitsToFloat(vertexData[vertex * VERTEX_STRIDE + X_OFFSET]);
        float y = Float.intBitsToFloat(vertexData[vertex * VERTEX_STRIDE + Y_OFFSET]);
        float z = Float.intBitsToFloat(vertexData[vertex * VERTEX_STRIDE + Z_OFFSET]);
        return new Vec3(x, y, z);
	}

	public static void setXYZ(int[] vertexData, int vertex, Vec3 xyz) {
		vertexData[vertex * VERTEX_STRIDE + X_OFFSET] = Float.floatToRawIntBits((float) xyz.x);
		vertexData[vertex * VERTEX_STRIDE + Y_OFFSET] = Float.floatToRawIntBits((float) xyz.y);
		vertexData[vertex * VERTEX_STRIDE + Z_OFFSET] = Float.floatToRawIntBits((float) xyz.z);
	}
}

