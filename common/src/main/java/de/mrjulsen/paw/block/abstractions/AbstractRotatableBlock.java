package de.mrjulsen.paw.block.abstractions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.mrjulsen.paw.util.VoxelShapeRotator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractRotatableBlock extends Block implements IRotatableBlock {

    public static final int ROTATIONS = 2;
    public static final int ROTATION_OFFSET = ROTATIONS - 1;
    public static final int MAX_ROTATION_INDEX = ROTATIONS;
    public static final int MIN_ROTATION_INDEX = -ROTATIONS;
    public static final int ROTATION_STEPS_PER_SIDE = ROTATIONS * 2;
    public static final int TOTAL_ROTATION_STEPS = ROTATION_STEPS_PER_SIDE * 4;
    public static final int PROPERTY_MAX_ROTATION_INDEX  = ROTATIONS + ROTATION_OFFSET;
    public static final int PROPERTY_BASE_ROTATION_INDEX = ROTATION_OFFSET;

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, PROPERTY_MAX_ROTATION_INDEX);

    private final Map<Integer, VoxelShape> shapeCache = new ConcurrentHashMap<>();

    public AbstractRotatableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ROTATION, PROPERTY_BASE_ROTATION_INDEX));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, ROTATION);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockPos clickedOnPos = context.getClickedPos().relative(clickedFace.getOpposite());
        Level level = context.getLevel();
        BlockState clickedOnState = level.getBlockState(clickedOnPos);

        if (clickedOnState.getBlock() instanceof AbstractRotatableBlock && clickedFace.getAxis() == Axis.Y) {
            return defaultBlockState()
                    .setValue(FACING, clickedOnState.getValue(FACING))
                    .setValue(ROTATION, clickedOnState.getValue(ROTATION));
        }

        int rot = Mth.floor((180.0f + context.getRotation()) * (float) TOTAL_ROTATION_STEPS / 360.0f + 0.5f) & (TOTAL_ROTATION_STEPS - 1);
        final int stepsPerSide = TOTAL_ROTATION_STEPS / 4;
        Direction dir = Direction.from2DDataValue((rot + stepsPerSide / 2) / stepsPerSide);
        int fineIndex = stepsPerSide - 1 - ((rot + stepsPerSide / 2) % stepsPerSide);

        return defaultBlockState()
                .setValue(FACING, dir)
                .setValue(ROTATION, fineIndex);
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeCache.computeIfAbsent(
                shapeHash(level, pos, state),
                k -> buildRotatedShape(state, level, pos, context));
    }

    protected int shapeHash(BlockGetter level, BlockPos pos, BlockState state) {
        return state.hashCode();
    }

    public static int signedRotationIndex(BlockState state) {
        return state.getValue(ROTATION) - ROTATION_OFFSET;
    }

    @Deprecated
    public static int normalizedPropertyRotationIndex(BlockState state) {
        return signedRotationIndex(state);
    }

    @Override
    public float getRelativeYRotation(BlockState state) {
        return fineAngle(signedRotationIndex(state));
    }

    public static float getRelativeYRotation(BlockState state, boolean unused) {
        return fineAngle(signedRotationIndex(state));
    }

    @Override
    public float getYRotation(BlockState state) {
        return facingAngle(state.getValue(FACING)) + getRelativeYRotation(state);
    }

    public float rotationOfFacingDirection(BlockState state) {
        return facingAngle(state.getValue(FACING));
    }

    @Override
    public Vec2 rotatedPivotPoint(BlockState state) {
        Vec2 local = getRotationPivotPoint(state);
        Vec2 rotated = rotateVec2(local, facingAngle(state.getValue(FACING)));
        return new Vec2(rotated.x + 0.5f, rotated.y + 0.5f);
    }

    @Override
    public BlockHitResult checkClickedFace(Level level, Player player, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Direction targetDir = hit.getDirection();

        if (targetDir.getAxis() != Axis.Y) {
            Vec3 clicked = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            Vec2 pivot = rotatedPivotPoint(state);
            float angle = getRelativeYRotation(state);
            Vec2 offset = getOffset(state);

            float cx = (float)clicked.x - offset.x;
            float cz = (float)clicked.z - offset.y;
            float radians = (float) Math.toRadians(angle);
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);
            float tx = cx - pivot.x;
            float tz = cz - pivot.y;
            float unrotX = tx * cos - tz * sin + pivot.x;
            float unrotZ = tx * sin + tz * cos + pivot.y;

            VoxelShape base = getBaseShape(state, level, pos, null);
            AABB aabb = base.bounds();
            targetDir = nearestFaceOfAABB(aabb, unrotX, unrotZ);
        }
        return hit.withDirection(targetDir);
    }

    private Direction nearestFaceOfAABB(AABB aabb, float x, float z) {
        float dNorth = Math.abs(z - (float)aabb.minZ);
        float dSouth = Math.abs(z - (float)aabb.maxZ);
        float dWest = Math.abs(x - (float)aabb.minX);
        float dEast = Math.abs(x - (float)aabb.maxX);

        float min = Math.min(Math.min(dNorth, dSouth), Math.min(dWest, dEast));

        if (min == dNorth) return Direction.NORTH;
        if (min == dSouth) return Direction.SOUTH;
        if (min == dWest) return Direction.WEST;
        return Direction.EAST;
    }

    protected BlockPos relativeTo(BlockAndTintGetter level, BlockState state, BlockPos pos, Direction direction) {
        BlockPos result = pos.relative(direction);
        if (level.getBlockState(pos).is(state.getBlock()) && signedRotationIndex(state) >= ROTATIONS) {
            result = result.relative(direction.getCounterClockWise());
        }
        return result;
    }

    protected BlockPos getSupportBlockPos(BlockGetter level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos relativePos = pos.relative(facing.getOpposite());
        if (level.getBlockState(pos).getBlock() instanceof AbstractRotatableBlock && getRelativeYRotation(state) > 30f) {
            relativePos = relativePos.relative(facing.getClockWise());
        }
        return relativePos;
    }


    private VoxelShape buildRotatedShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape base = getBaseShape(state, level, pos, context);
        float yaw = getRelativeYRotation(state);
        Vec2 pivot = rotatedPivotPoint(state);
        Vec2 offset = getOffset(state);
        VoxelShape rotated = VoxelShapeRotator.rotateY(base, yaw, new Vec3(pivot.x, 0.5, pivot.y));
        if (offset.x == 0f && offset.y == 0f) {
            return rotated;
        }
        List<AABB> shifted = new ArrayList<>();
        for (AABB aabb : rotated.toAabbs()) {
            shifted.add(aabb.move(offset.x, 0, offset.y));
        }

        VoxelShape result = Shapes.empty();
        for (AABB aabb : shifted) {
            result = Shapes.joinUnoptimized(result, Shapes.create(aabb), BooleanOp.OR);
        }
        return result.optimize();
    }

    private static float facingAngle(Direction facing) {
        return switch (facing) {
            case EAST -> 270f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            default -> 0f;
        };
    }

    private static float fineAngle(int rotationIndex) {
        return (float) Math.toDegrees(Math.atan((double) rotationIndex / ROTATIONS));
    }

    private static Vec2 rotateVec2(Vec2 v, float angleDeg) {
        float rad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        return new Vec2(v.x * cos + v.y * sin, -v.x * sin + v.y * cos);
    }

    private static Direction nearestFace(VoxelShape baseShape, Vec3 point) {
        AABB outer = null;
        for (AABB box : baseShape.toAabbs()) {
            outer = (outer == null) ? box : outer.minmax(box);
        }
        if (outer == null) {
            outer = new AABB(0, 0, 0, 1, 1, 1);
        }

        double dNorth = Math.abs(point.z - outer.minZ);
        double dSouth = Math.abs(point.z - outer.maxZ);
        double dWest = Math.abs(point.x - outer.minX);
        double dEast = Math.abs(point.x - outer.maxX);
        double dDown = Math.abs(point.y - outer.minY);
        double dUp = Math.abs(point.y - outer.maxY);

        double min = Math.min(Math.min(Math.min(dNorth, dSouth), Math.min(dWest, dEast)), Math.min(dDown, dUp));

        if (min == dNorth) return Direction.NORTH;
        if (min == dSouth) return Direction.SOUTH;
        if (min == dWest) return Direction.WEST;
        if (min == dEast) return Direction.EAST;
        if (min == dDown) return Direction.DOWN;
        return Direction.UP;
    }

    private static Direction rotateDirection(Direction dir, float[][] matrix) {
        Vec3 normal  = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        Vec3 rotated = VoxelShapeRotator.applyMatrix(normal, matrix, Vec3.ZERO);

        Direction best   = dir;
        double    bestDot = -Double.MAX_VALUE;
        for (Direction candidate : Direction.values()) {
            double dot = rotated.dot(new Vec3(candidate.getStepX(), candidate.getStepY(), candidate.getStepZ()));
            if (dot > bestDot) {
                bestDot = dot;
                best    = candidate;
            }
        }
        return best;
    }
}