package de.mrjulsen.paw.block.abstractions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.mrjulsen.paw.util.Const;
import de.mrjulsen.paw.util.ModMath;
import de.mrjulsen.mcdragonlib.util.MathUtils;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractRotatableBlock extends Block implements IRotatableBlock {

    private static final record ShapeCacheEntry(VoxelShape shape, Vec2[] corners) {}

    protected static final float EPSILON = 1e-6f;
    public static final int ROTATIONS = 2;
    
    public static final int ROTATION_OFFSET = ROTATIONS - 1;
    public static final int MAX_ROTATION_INDEX = ROTATIONS;
    public static final int MIN_ROTATION_INDEX = -ROTATIONS;
    public static final int ROTATION_STEPS_PER_SIDE = ROTATIONS * 2;
    public static final int TOTAL_ROTATION_STEPS = ROTATION_STEPS_PER_SIDE * 4;
    public static final int PROPERTY_MAX_ROTATION_INDEX = ROTATIONS + ROTATION_OFFSET;
    public static final int PROPERTY_BASE_ROTATION_INDEX = ROTATION_OFFSET;
    
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, PROPERTY_MAX_ROTATION_INDEX);

    private final Map<Integer, ShapeCacheEntry> shapes = new ConcurrentHashMap<>(); // Cache

    public AbstractRotatableBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ROTATION, PROPERTY_BASE_ROTATION_INDEX)
        );
    }

    @Environment(EnvType.CLIENT)
    @Override
    public BlockHitResult checkClickedFace(Level level, Player player, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Direction targetDir = hit.getDirection();
        if (targetDir.getAxis() != Axis.Y) {
            Vec2[] points = getCubeCorners(state, level, pos, null);
            for (int i = 0; i < points.length; i++) {
                Vec2 pointA = points[i];
                Vec2 pointB = points[(i + 1) % points.length];

                Direction dir = switch (i) {
                    case 1 -> Direction.EAST;
                    case 2 -> Direction.SOUTH;
                    case 3 -> Direction.WEST;
                    default -> Direction.NORTH;
                };
                
                float minX = Math.min(pointA.x, pointB.x);
                float minZ = Math.min(pointA.y, pointB.y);
                float maxX = Math.max(pointA.x, pointB.x);
                float maxZ = Math.max(pointA.y, pointB.y);

                Vec3 clicked = hit.getLocation().subtract(MathUtils.blockPosToVec3(pos));
                if (clicked.x >= minX && clicked.x <= maxX && clicked.z >= minZ && clicked.z <= maxZ) {
                    targetDir = dir;
                    break;
                }
            }
        }
        return hit.withDirection(targetDir);
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }    

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }
    
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(FACING, ROTATION);
    }
    
    @Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getClickedFace();
        BlockPos clickPos = context.getClickedPos().relative(direction.getOpposite());
        Level level = context.getLevel();
        BlockState clickedState = level.getBlockState(clickPos);

        BlockState state = super.defaultBlockState();

        if (clickedState.getBlock() instanceof AbstractRotatableBlock && direction.getAxis() == Axis.Y) {
            state = state
                .setValue(FACING, clickedState.getValue(FACING))
                .setValue(ROTATION, clickedState.getValue(ROTATION))
            ;
        } else {
            int rot = (Mth.floor((double)((180.0F + context.getRotation()) * (float)TOTAL_ROTATION_STEPS / 360.0F) + 0.5) & (TOTAL_ROTATION_STEPS - 1));
            final int steps = TOTAL_ROTATION_STEPS / 4;
            Direction dir = Direction.from2DDataValue((rot + (steps / 2)) / 4);
            state = state
                .setValue(FACING, dir)
                .setValue(ROTATION, steps - 1 - ((rot + (steps / 2)) % 4))
            ;
        }
        return state;
	}

    private ShapeCacheEntry getShapeData(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(shapeHash(level, pos, state), x -> calcShape(state, level, pos, context));
    }

    protected int shapeHash(BlockGetter level, BlockPos pos, BlockState state) {
        return state.hashCode();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {        
        return getShapeData(state, level, pos, context).shape();
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    public Vec2[] getCubeCorners(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {    
        return getShapeData(state, level, pos, context).corners();
    }

    @Override
    public float getRelativeYRotation(BlockState state) {
        return rotationOf(propertyIndexToRotIndex(state.getValue(ROTATION)));
    }

    @Override
    public float getYRotation(BlockState state) {
        return rotationOfFacingDirection(state) + getRelativeYRotation(state);
    }

    public float rotationOfFacingDirection(BlockState state) {
        return switch (state.getValue(FACING)) {
            case EAST  -> 270;
            case SOUTH -> 180;
            case WEST  -> 90;
            default    -> 0;
        };
    }

    @Override
    public Vec2 rotatedPivotPoint(BlockState state) {
        return ModMath.rotateY(getRotationPivotPoint(state), rotationOfFacingDirection(state)).add(0.5f);
    }



    private ShapeCacheEntry calcShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape base = getBaseShape(state, level, pos, context);
        float angle = -getRelativeYRotation(state);
        Vec2 pivot = rotatedPivotPoint(state);

        Vec2 offset = getOffset(state);
        List<AABB> aabbs = base.toAabbs();
        List<VoxelShape> shapes = new ArrayList<>();
        Vec2[] finalCorners = new Vec2[0];
        for (AABB aabb : aabbs) {
            Vec2[] corners = rotateCorners(angle, Const.PIXEL, pivot, aabb, offset);
            if (finalCorners.length <= 0) finalCorners = corners;

            List<Vec2[]> rectangles = approximateSquare(corners, angle, Const.PIXEL);

            for (Vec2[] rect : rectangles) {
                shapes.add(Block.box(rect[0].x * 16f, aabb.minY * 16f, rect[0].y * 16f, (rect[1].x + Const.PIXEL) * 16f, aabb.maxY * 16f, rect[1].y * 16f));
            }
        }

        return new ShapeCacheEntry(Shapes.or(Shapes.empty(), shapes.toArray(VoxelShape[]::new)).optimize(), finalCorners);
    }

    public static Vec2[] rotateCorners(float angle, float minSize, Vec2 pivotPoint, AABB src, Vec2 offset) {        
        Vec2[] square = new Vec2[] {
            new Vec2((float)src.minX, (float)src.minZ),
            new Vec2((float)src.maxX, (float)src.minZ),
            new Vec2((float)src.maxX, (float)src.maxZ),
            new Vec2((float)src.minX, (float)src.maxZ)
        };

        float radians = (float)Math.toRadians(angle);
        Vec2[] rotatedSquare = new Vec2[4];
        for (int i = 0; i < 4; i++) {
            Vec2 v = square[i];
            if (Math.abs(angle) > EPSILON) {
                v = rotatePointAroundPivot(v, pivotPoint, radians);
            }
            v = v.add(offset);
            rotatedSquare[i] = v;
        }

        return rotatedSquare;
    }
    
    public static List<Vec2[]> approximateSquare(Vec2[] rotatedCorners, float angle, float minSize) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

        for (Vec2 point : rotatedCorners) {
            if (point.x < minX) minX = point.x;
            if (point.y < minY) minY = point.y;
            if (point.x > maxX) maxX = point.x;
            if (point.y > maxY) maxY = point.y;
        }


        List<Vec2[]> rectangles = new ArrayList<>();
        
        if (Math.abs(angle) < EPSILON) {
            rectangles.add(new Vec2[] { new Vec2(minX, minY), new Vec2(maxX, maxY) });
            return rectangles;
        }

        for (float x = minX; x < maxX; x += minSize) {
            for (float y = minY; y < maxY; y += minSize) {
                Vec2 rectBottomLeft = new Vec2(x, y);
                Vec2 rectTopRight = new Vec2(x + minSize, y + minSize);

                if (isRectangleIntersectingPolygon(rotatedCorners, rectBottomLeft, rectTopRight)) {
                    rectangles.add(new Vec2[] {
                        rectBottomLeft,
                        new Vec2(rectBottomLeft.x, rectTopRight.y),
                        rectTopRight,
                        new Vec2(rectTopRight.x, rectBottomLeft.y)
                    });
                }
            }
        }

        return rectangles;
    }

    private static Vec2 rotatePointAroundPivot(Vec2 point, Vec2 pivot, float radians) {
        float translatedX = point.x - pivot.x;
        float translatedY = point.y - pivot.y;

        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        float rotatedX = translatedX * cos - translatedY * sin;
        float rotatedY = translatedX * sin + translatedY * cos;
        
        return new Vec2(rotatedX + pivot.x, rotatedY + pivot.y);
    }

    private static boolean isRectangleIntersectingPolygon(Vec2[] polygon, Vec2 rectBottomLeft, Vec2 rectTopRight) {
        Vec2[] rectPoints = new Vec2[] {
            rectBottomLeft,
            new Vec2(rectBottomLeft.x, rectTopRight.y),
            rectTopRight,
            new Vec2(rectTopRight.x, rectBottomLeft.y)
        };

        for (Vec2 point : rectPoints) {
            if (isPointInPolygon(polygon, point)) {
                return true;
            }
        }

        for (int i = 0; i < polygon.length; i++) {
            Vec2 p1 = polygon[i];
            Vec2 p2 = polygon[(i + 1) % polygon.length];

            for (int j = 0; j < rectPoints.length; j++) {
                Vec2 r1 = rectPoints[j];
                Vec2 r2 = rectPoints[(j + 1) % rectPoints.length];

                if (doLinesIntersect(p1, p2, r1, r2)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean doLinesIntersect(Vec2 p1, Vec2 p2, Vec2 q1, Vec2 q2) {
        float o1 = orientation(p1, p2, q1);
        float o2 = orientation(p1, p2, q2);
        float o3 = orientation(q1, q2, p1);
        float o4 = orientation(q1, q2, p2);

        if (o1 != o2 && o3 != o4) {
            return true;
        }

        return false;
    }

    private static float orientation(Vec2 p, Vec2 q, Vec2 r) {
        float val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
        if (Math.abs(val) < EPSILON) return 0;
        return (val > 0) ? 1 : 2;
    }

    private static boolean isPointInPolygon(Vec2[] polygon, Vec2 point) {
        int crossings = 0;
        for (int i = 0; i < polygon.length; i++) {
            Vec2 v1 = polygon[i];
            Vec2 v2 = polygon[(i + 1) % polygon.length];

            boolean cond1 = (v1.y <= point.y + EPSILON && point.y + EPSILON < v2.y);
            boolean cond2 = (v2.y <= point.y + EPSILON && point.y + EPSILON < v1.y);
            if (cond1 || cond2) {
                float t = (point.y - v1.y) / (v2.y - v1.y);
                if (point.x < v1.x + t * (v2.x - v1.x)) {
                    crossings++;
                }
            }
        }

        return (crossings % 2 != 0);
    }

    public static Vec2[] getRotatedSquare(float angle, Vec2 pivotPoint, AABB src) {
        Vec2[] square = new Vec2[]{
            new Vec2((float)src.minX, (float)src.minZ),
            new Vec2((float)src.maxX, (float)src.minZ),
            new Vec2((float)src.maxX, (float)src.maxZ),
            new Vec2((float)src.minX, (float)src.maxZ)
        };

        float radians = (float) Math.toRadians(angle);

        Vec2[] rotatedSquare = new Vec2[4];
        for (int i = 0; i < 4; i++) {
            rotatedSquare[i] = rotatePointAroundPivot(square[i], pivotPoint, radians);
        }

        return rotatedSquare;
    }

    protected static final float m(int rotIdx) {
        return (float)rotIdx / ROTATIONS;
    }

    protected static final int propertyIndexToRotIndex(int prop) {
        return prop - ROTATION_OFFSET;
    }

    protected static final float rotationOf(int rotationIndex) {
        return (float)Math.toDegrees(Math.atan(m(rotationIndex)));
    }

    protected static final int normalizedPropertyRotationIndex(BlockState state) {
        int prop = state.getValue(ROTATION);
        return prop - ROTATION_OFFSET;
    }

    protected BlockPos relativeTo(BlockAndTintGetter level, BlockState state, BlockPos pos, Direction direction) {
        BlockPos result = pos.relative(direction);
        if (level.getBlockState(pos).is(state.getBlock()) && normalizedPropertyRotationIndex(state) >= ROTATIONS) {
            result = result.relative(direction.getCounterClockWise());
        }
        return result;
    }

    protected BlockPos getSupportBlockPos(BlockGetter level, BlockPos pos, BlockState state) {        
        Direction direction = state.getValue(FACING);
        BlockPos relativePos = pos.relative(direction.getOpposite());
        if (level.getBlockState(pos).getBlock() instanceof AbstractRotatableBlock && getRelativeYRotation(state) > 30) {
            relativePos = relativePos.relative(direction.getClockWise());
        }
        return relativePos;
    }
}
