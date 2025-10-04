package de.mrjulsen.paw.blockentity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.joml.Vector3d;
import org.joml.Vector3f;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.util.Const;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.debug.WireDebugRenderer;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.NewWireCollision.WireBlockCollision;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class PantographBlockEntity extends SmartBlockEntity implements GeoBlockEntity  {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private static final RawAnimation ANIM_WIRE_CONTACT = RawAnimation.begin().thenPlayAndHold("wire_contact");
	private static final RawAnimation ANIM_EXPAND = RawAnimation.begin().thenPlay("expand").thenPlayAndHold("wire_contact");
	private static final RawAnimation ANIM_COLLAPSE = RawAnimation.begin().thenPlayAndHold("collapse");    

    public static final String NBT_EXPANDABLE = "IsExpandable";
    
    public static final double MAX_HEIGHT = 3.6D;
    public static final double MAX_HEIGHT_PIXELS = MAX_HEIGHT / Const.PIXEL;
    public static final double MIN_HEIGHT_PIXELS = 13D + Const.PIXEL;
    public static final double MIN_HEIGHT = Const.PIXEL * MIN_HEIGHT_PIXELS;
    public static final double FORWARD_OFFSET = Const.PIXEL * 4;
    public static final double MAX_WIDTH = 2.5D;
    public static final double DELTA_HEIGHT = MAX_HEIGHT - MIN_HEIGHT;
    public static final double DELTA_HEIGHT_PIXELS = MAX_HEIGHT_PIXELS - MIN_HEIGHT_PIXELS;
    public static final double ARM_LENGTH = 36;
    public static final double ARM_LENGTH_DOUBLE_POW = 2 * Math.pow(ARM_LENGTH, 2);
    public static final double BASE_ANGLE = Math.toDegrees(Math.acos((ARM_LENGTH_DOUBLE_POW - Math.pow(1.5, 2)) / ARM_LENGTH_DOUBLE_POW));
    public static final double START_ANGLE = Math.toDegrees(Math.acos((ARM_LENGTH_DOUBLE_POW - Math.pow(((0.04) * DELTA_HEIGHT_PIXELS), 2)) / ARM_LENGTH_DOUBLE_POW));
    public static final Vector3d BASE_UP_VECTOR = new Vector3d(0, 1, 0).normalize().mul(MAX_HEIGHT);
    public static final Vector3d BASE_RIGHT_VECTOR = new Vector3d(1, 0, 0).normalize().mul(MAX_WIDTH / 2d);
    public static final Vector3d BASE_FORWARD_VECTOR = new Vector3d(0, 0, 1).normalize().mul(FORWARD_OFFSET);

    // Client only, unsaved
    private Vector3d currentPos;
    private UnaryOperator<Vector3d> rotationFunc = v -> v;
    private double catenaryWireHeight = DELTA_HEIGHT;
    private final LerpedFloat animationTransition = LerpedFloat.linear().startWithValue(catenaryWireHeight);
 
    // state
    private boolean expanded = false;
    private boolean stateChanged = false;

    // properties
    private boolean expandable = false;

    // Debug
    public Vector3f debug_wireCollisionA = new Vector3f();
    public Vector3f debug_wireCollisionB = new Vector3f();
    public double debug_hitHeight = 0;

    public PantographBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Vector3d getCurrentPos() {
        return currentPos;
    }

    public Vector3d rotate(Vector3d vec) {
        return rotationFunc.apply(vec);
    }

    public void toggleExpandable() {
        setExpandable(!isExpandable());
    }

    public void setExpandable(boolean b) {
        this.expandable = b;
        notifyUpdate();
    }

    public boolean isExpandable() {
        return this.expandable;
    }

    protected void setExpanded(boolean b) {        
        this.stateChanged = this.expanded != b;
        this.expanded = b;
    }

    public boolean isExpanded() {
        return this.expanded;
    }


    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putBoolean(NBT_EXPANDABLE, isExpandable());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        setExpandable(tag.getBoolean(NBT_EXPANDABLE));
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state -> {
            if (state.getController().getCurrentRawAnimation() == null && isExpanded()) {
                state.setAnimation(ANIM_WIRE_CONTACT);
            }
            if (stateChanged) {
                if (isExpanded()) {
                    state.setAnimation(ANIM_EXPAND);
                } else {
                    state.setAnimation(ANIM_COLLAPSE);
                }
                stateChanged = false;
            }
            return PlayState.CONTINUE;
        })
            .setAnimationSpeed(0.25f)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


    @Override
    public AABB getRenderBoundingBox() {
        AABB aabb = new AABB(worldPosition.offset(-2, 0, -2));
        return aabb.expandTowards(4, 4, 4);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}


    @Override
    public void tick() {
        // As Block
        this.setExpanded(this.isExpandable());
        commonTick();
    }

    public void contraptionTick() {
        // As contraption
        commonTick();
    }

    protected void commonTick() {
        super.tick();
        animationTransition.tickChaser();
    }


    public void updateContraptionValues(Vector3d worldPos, UnaryOperator<Vector3d> rotationFunc) {
        this.currentPos = worldPos;
        this.rotationFunc = rotationFunc;
        
        final Vector3d currPos = this.currentPos == null ? new Vector3d(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()) : this.currentPos;
        final Vector3d upVec = this.rotationFunc == null ? BASE_UP_VECTOR : this.rotationFunc.apply(BASE_UP_VECTOR);
        final Vector3d rightVec = this.rotationFunc == null ? BASE_RIGHT_VECTOR : this.rotationFunc.apply(BASE_RIGHT_VECTOR);
        final Vector3d forwardVec = this.rotationFunc == null ? BASE_FORWARD_VECTOR : this.rotationFunc.apply(BASE_FORWARD_VECTOR);
        currPos.add(forwardVec);
        
        this.catenaryWireHeight = calculateWireContact(currPos, upVec, rightVec);
        this.setExpanded(this.expandable && this.catenaryWireHeight >= 0);
        this.catenaryWireHeight = this.catenaryWireHeight < 0 ? 0 : this.catenaryWireHeight;
        if (this.expanded) {            
            animationTransition.chase(this.catenaryWireHeight, 1, Chaser.LINEAR);
        }
    }
    
    public void applyMolangVariables() {
        MolangParser.INSTANCE.setValue("query.height_percentage", () -> {
            double p = 1D / DELTA_HEIGHT * animationTransition.getValue(AnimationTickHolder.getPartialTicks(level));
            return p;
        });
        MolangParser.INSTANCE.setValue("query.func", () -> {            
            double p = MolangParser.INSTANCE.getVariable("query.height_percentage").get();
            return getArmAngle(p);
        });
        MolangParser.INSTANCE.setMemoizedValue("query.head_rotation", () -> {
            return 0;//calculateWireContactSlope(currentPos, upVec, rightVec);
        });
    } 

    public static final double getArmAngle(double heightPercentage) {
        return Math.toDegrees(Math.acos((ARM_LENGTH_DOUBLE_POW - Math.pow(((heightPercentage + 0.04) * DELTA_HEIGHT_PIXELS), 2)) / ARM_LENGTH_DOUBLE_POW));
    }

    private double calculateWireContact(Vector3d worldPosition, Vector3d upVec, Vector3d rightVec) {        
        if (WireDebugRenderer.enabled()) {
            debug_hitHeight = 0;
            debug_wireCollisionA = new Vector3f();
            debug_wireCollisionB = new Vector3f();
        }
        
        Vector3d pA = new Vector3d(worldPosition).sub(rightVec);
        Vector3d pB = new Vector3d(worldPosition).add(rightVec);
        Iterator<BlockPos> poses = findIntersectingBlocks(pA, pB, upVec).iterator();
        double result = MAX_HEIGHT;
        boolean hasWire = false;
        WireGraphClient net = WireGraphManager.getClient(level, WiresApi.PAW_CATENARY_WIRES);
        while (poses.hasNext()) {
            BlockPos pos = poses.next();
            for (WireBlockCollision c : net.getCollisionsInBlock(pos).stream().flatMap(x -> x.collisionsInBlock(pos).stream()).toList()) {
                Vector3d d = checkWireIntersection(
                    new Vector3d(c.getAbsoluteInVector().x, c.getAbsoluteInVector().y, c.getAbsoluteInVector().z),
                    new Vector3d(c.getAbsoluteOutVector().x, c.getAbsoluteOutVector().y, c.getAbsoluteOutVector().z),
                    pA,
                    pB,
                    upVec
                );
                if (d != null) {
                    double rY = d.y - worldPosition.y;
                    Vector3d scaledUp = new Vector3d(upVec).normalize().mul(rY);
                    double f = new Vector3d(scaledUp.x(), 0, scaledUp.z()).length();
                    rY = Math.sqrt(Math.pow(f, 2) + Math.pow(rY, 2));
                    if (rY < result) {
                        result = rY;
                        if (WireDebugRenderer.enabled()) {
                            debug_hitHeight = rY;
                            debug_wireCollisionA = new Vector3f((float)c.getAbsoluteInVector().x, (float)c.getAbsoluteInVector().y, (float)c.getAbsoluteInVector().z);
                            debug_wireCollisionB = new Vector3f((float)c.getAbsoluteOutVector().x, (float)c.getAbsoluteOutVector().y, (float)c.getAbsoluteOutVector().z);
                        }
                    }
                    hasWire = true;
                }
            }
        }
        return hasWire ? result : -1;
    }
    
    private double calculateWireContactSlope(Vector3d worldPosition, Vector3d upVec, Vector3d rightVec) {
        Vector3d pA = new Vector3d(worldPosition).sub(rightVec);
        Vector3d pB = new Vector3d(worldPosition).add(rightVec);
        Iterator<BlockPos> poses = findIntersectingBlocks(pA, pB, upVec).iterator();
        WireGraphClient net = WireGraphManager.getClient(level, WiresApi.PAW_CATENARY_WIRES);
        while (poses.hasNext()) {
            BlockPos pos = poses.next();
            for (WireBlockCollision c : net.getCollisionsInBlock(pos).stream().flatMap(x -> x.collisionsInBlock(pos).stream()).toList()) {
                Vector3d d = checkWireIntersection(
                    new Vector3d(c.getAbsoluteInVector().x, c.getAbsoluteInVector().y, c.getAbsoluteInVector().z),
                    new Vector3d(c.getAbsoluteOutVector().x, c.getAbsoluteOutVector().y, c.getAbsoluteOutVector().z),
                    pA,
                    pB,
                    upVec
                );
                if (d != null) {
                    return slope(new Vector3d(c.getAbsoluteInVector().x, c.getAbsoluteInVector().y, c.getAbsoluteInVector().z), new Vector3d(c.getAbsoluteOutVector().x, c.getAbsoluteOutVector().y, c.getAbsoluteOutVector().z));
                }
            }
        }        
        return 0;
    }
    
    protected static Vector3d checkWireIntersection(Vector3d c, Vector3d d, Vector3d a, Vector3d b, Vector3d direction) {
        Vector3d AB = new Vector3d(b).sub(a);
        Vector3d vDir = new Vector3d(direction);
    
        Vector3d normal = new Vector3d();
        AB.cross(vDir, normal);
        Vector3d CD = new Vector3d(d).sub(c);
        double numerator = normal.dot(new Vector3d(a).sub(c));
        double denominator = normal.dot(CD);
        if (Math.abs(denominator) < 1e-8) {
            return null;
        }
    
        double t = numerator / denominator;
        if (t < 0 || t > 1) {
            return null;
        }
    
        Vector3d intersection = new Vector3d(c).add(CD.mul(t));
        Vector3d AP = new Vector3d(intersection).sub(a);
        double u = AP.dot(AB) / AB.lengthSquared();
        double v = AP.dot(vDir) / vDir.lengthSquared();
    
        if (u < 0 || u > 1 || v < 0 || v > 1) {
            return null;
        }
        
        return intersection;
    }
    
    private static double slope(Vector3d pointA, Vector3d pointB) {
        Vector3d direction = new Vector3d();
        pointB.sub(pointA, direction);
        double projectionXY = Math.sqrt(direction.x * direction.x + direction.y * direction.y);
        double slopeAngle = Math.atan2(direction.z, projectionXY);
        return Math.toDegrees(slopeAngle) + 90;
    }

    protected static Set<BlockPos> findIntersectingBlocks(Vector3d a, Vector3d b, Vector3d v) {
        Set<BlockPos> intersections = new HashSet<>();
        Vector3d ab = new Vector3d(b).sub(a);
        Vector3d normal = new Vector3d(ab).cross(v);
        Vector3d min = new Vector3d(a);
        Vector3d max = new Vector3d(a);

        min.min(b).min(new Vector3d(a).add(v)).min(new Vector3d(b).add(v));
        max.max(b).max(new Vector3d(a).add(v)).max(new Vector3d(b).add(v));

        for (int x = (int) Math.floor(min.x); x <= Math.ceil(max.x); x++) {
            for (int y = (int) Math.floor(min.y); y <= Math.ceil(max.y); y++) {
                for (int z = (int) Math.floor(min.z); z <= Math.ceil(max.z); z++) {
                    Vector3d blockCenter = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                    double distance = Math.abs(blockCenter.sub(a, new Vector3d()).dot(normal)) / normal.length();
                    if (distance <= Math.sqrt(3) / 2) {
                        intersections.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        return intersections;
    }
}
