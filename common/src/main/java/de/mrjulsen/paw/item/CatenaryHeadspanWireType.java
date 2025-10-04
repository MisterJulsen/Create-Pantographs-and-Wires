package de.mrjulsen.paw.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import org.joml.Vector3f;

import com.eliotlash.mclib.utils.MathUtils;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.RegistrationArmBlock;
import de.mrjulsen.paw.block.RegistrationArmBlock.State;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.registry.InsulatorWireDecoration;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.paw.registry.RegistrationArmWireDecoration;
import de.mrjulsen.paw.util.ModMath;
import de.mrjulsen.wires.graph.NewWireCollision;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.accessor.GenericWireNodeAccessor;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.network.WireId;
import de.mrjulsen.wires.SegmentControl;
import de.mrjulsen.wires.Wire;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireBuilder;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.WirePoints;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.SegmentControl.Config;
import de.mrjulsen.wires.WireBuilder.CableType;
import de.mrjulsen.wires.decoration.WireDecorationData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;

public class CatenaryHeadspanWireType extends PAWWireType {

	private static final String KEY_NOT_ENOUGH_INSULATORS = "item." + PantographsAndWires.MOD_ID + ".wire.not_enough_insulators";
	private static final String KEY_INVALID_DECORATION_POSITION = "item." + PantographsAndWires.MOD_ID + ".wire.invalid_decoration_position";
	private static final String KEY_ONE_INVALID_DECORATION_POSITION = "item." + PantographsAndWires.MOD_ID + ".wire.one_invalid_decoration_position";
	private static final String KEY_DROPPER_HAS_REGISTRATION_ARM = "item." + PantographsAndWires.MOD_ID + ".catenary_headspan.dropper_has_registration_arm";
	private static final String KEY_INVALID_DROPPER_LOCATION = "item." + PantographsAndWires.MOD_ID + ".catenary_headspan.invalid_dropper_position";
	private static final String KEY_DROPPER_EXISTS = "item." + PantographsAndWires.MOD_ID + ".catenary_headspan.dropper_exists";

	public static final String WIRE_UPPER_TENSION = "upper_tension_wire";
	public static final String WIRE_LOWER_TENSION = "lower_tension_wire";
	public static final String WIRE_TOP_SUPPORT_WIRE = "headspan_wire";
	public static final String WIRE_DROPPER_L = "dropper_l_";
	public static final String WIRE_DROPPER_U = "dropper_u_";
	public static final String WIRE_CROSS_CONNECTION = "cross_connection";

	public static final String NBT_DROPPERS = "Droppers";

	private static final float THICKNESS = 0.75f / 16f;
	private static final float DECORATION_GRID_SIZE = 0.5f;

	public CatenaryHeadspanWireType(ResourceLocation location) {
		super(location);
	}

	@Override
	public int getMaxLength() {
		return ModServerConfig.CATENARY_WIRE_MAX_LENGTH.get();
	}
	
	@Override
	public int getWireLength(int connectionLength) {
		return connectionLength * 4;
	}

	@Override
	public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData customData, WireEdge edge, WireNode nodeA, WireNode nodeB) {
		Vector3f start = nodeA.getPos();
		Vector3f end = nodeB.getPos();

		float upperWireHeight = customData.customData().getCommonData().getFloat(CatenaryHeadspanWireItem.NBT_UPPER_WIRE_HEIGHT);
		float topWireHeight = upperWireHeight + customData.customData().getCommonData().getFloat(CatenaryHeadspanWireItem.NBT_TOP_WIRE_HEIGHT) + 0.4f;
		float wireLength = new Vector3f(end).sub(start).length();
		
		int subSegments = (int)(wireLength / 2);
		List<Dropper> droppers = customData.customData().getCommonData().getList(NBT_DROPPERS, Tag.TAG_COMPOUND).stream().map(x -> Dropper.fromNbt((CompoundTag)x)).sorted(Dropper::compareTo).toList();
		float[] dropperDistances = new float[droppers.size()];
		float lastDropperDistance = 0;
		for (int i = 0; i < droppers.size(); i++) {
			dropperDistances[i] = wireLength * (droppers.get(i).pos() - lastDropperDistance);
			lastDropperDistance = droppers.get(i).pos();
		}

		Vector3f direction = new Vector3f(end).sub(start);
		direction = new Vector3f(direction.x(), 0, direction.z());
		Vector3f rightVec = new Vector3f(direction.z(), 0, -direction.x()).normalize();
		direction.absolute().normalize();
        Vector3f offsetA = new Vector3f(rightVec).mul(DragonLib.PIXEL * 2);
        Vector3f offsetB = new Vector3f(rightVec).mul(DragonLib.PIXEL * -2);

		Wire topWire1 = WireBuilder.createWire(WIRE_TOP_SUPPORT_WIRE + 1, context, new Vector3f(start).add(offsetA.x(), topWireHeight, offsetA.z()), new Vector3f(end).add(offsetA.x(), topWireHeight, offsetA.z()), CableType.HANGING, THICKNESS, topWireHeight - upperWireHeight - 1, SegmentControl.create(dropperDistances.length <= 0 ? Config.auto() : Config.custom(dropperDistances, false), Config.maxLength(3)));
		Wire topWire2 = WireBuilder.createWire(WIRE_TOP_SUPPORT_WIRE + 2, context, new Vector3f(start).add(offsetB.x(), topWireHeight, offsetB.z()), new Vector3f(end).add(offsetB.x(), topWireHeight, offsetB.z()), CableType.TENSION, THICKNESS, topWireHeight - upperWireHeight - 1, SegmentControl.create(dropperDistances.length <= 0 ? Config.auto() : Config.custom(dropperDistances, false), Config.maxLength(3)));
		Wire upperWire = WireBuilder.createWire(WIRE_UPPER_TENSION, context, new Vector3f(start).add(0, upperWireHeight, 0), new Vector3f(end).add(0, upperWireHeight, 0), CableType.TIGHT, THICKNESS, 0, SegmentControl.create(Config.custom(dropperDistances, false), Config.maxLength(3)));
		Wire lowerWire = WireBuilder.createWire(WIRE_LOWER_TENSION, context, new Vector3f(start).add(0, DragonLib.PIXEL * -2, 0), new Vector3f(end).add(0, DragonLib.PIXEL * -2, 0), CableType.TIGHT, THICKNESS, 0, SegmentControl.create(Config.custom(dropperDistances, false), Config.maxLength(3)));
		WireBatch batch = WireBatch.of(lowerWire, upperWire, topWire1, topWire2);

		if (dropperDistances.length > 0 && upperWire.getCollisionData().isPresent() && lowerWire.getCollisionData().isPresent() && topWire1.getCollisionData().isPresent() && topWire2.getCollisionData().isPresent()) {			
			WirePoints topRenderData1 = topWire1.collisionData();
			WirePoints topRenderData2 = topWire2.collisionData();
			List<Wire> crossConnectionWires = new ArrayList<>(topRenderData1.vertices().length);
			for (int i = 1, c = 0; i < topRenderData1.vertices().length && i < topRenderData2.vertices().length - 1; i++, c++) {
				crossConnectionWires.add(batch.addSubWire(WireBuilder.createWire(
					WIRE_CROSS_CONNECTION + c,
					context,
					topRenderData1.vertices()[i],
					topRenderData2.vertices()[i],
					CableType.TIGHT,
					THICKNESS,
					0,
					SegmentControl.single())
				));
			}

			WirePoints lowerRenderData = lowerWire.collisionData();
			WirePoints upperRenderData = upperWire.collisionData();
			for (int i = 1, c = 0; i < lowerRenderData.vertices().length && i < upperRenderData.vertices().length && c < crossConnectionWires.size(); i++, c++) {
				UUID dropperId = droppers.get(c).id();
				WirePoints crossConnectionWire = crossConnectionWires.get(c).collisionData();
				
				boolean noDropperL = false;
				for (WireDecorationData decoData : edge.getDecorations()) {
					if (decoData.getDecoration() instanceof RegistrationArmWireDecoration deco) {
						if (deco.getDropperId().equals(dropperId)) {
							noDropperL = deco.getVariant().isAbove();
							break;
						}
					}
				}

				batch.addSubWire(WireBuilder.createWire(
					WIRE_DROPPER_L + dropperId,
					noDropperL ? WireCreationContext.COLLISION : context,
					lowerRenderData.vertices()[i],
					upperRenderData.vertices()[i],
					CableType.TIGHT,
					THICKNESS,
					0,
					SegmentControl.single()
				));
				batch.addSubWire(WireBuilder.createWire(
					WIRE_DROPPER_U + dropperId,
					context,
					upperRenderData.vertices()[i],
					ModMath.centerOf(crossConnectionWire.vertices()[0], crossConnectionWire.vertices()[1]),
					CableType.TIGHT,
					THICKNESS,
					0,
					SegmentControl.single()
				));
			}
			
		}
		return batch;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand, WireHitResult hitResult) {
		if (!level.isClientSide()) {
			WireGraph network = WireGraphManager.get(level, hitResult.getGraphId());
			WireEdge edge = network.getEdge(hitResult.getWireId().id());

			if (edge == null) {
				return InteractionResult.FAIL;
			}

			// --- REMOVE DROPPERS ---
			if ((player.getItemInHand(hand).is(Items.SHEARS) || player.getItemInHand(hand).is(ModItems.TAG_WRENCH)) && (hitResult.getWireId().name().startsWith(WIRE_DROPPER_L) || hitResult.getWireId().name().startsWith(WIRE_DROPPER_U))) {

				UUID dropperId;
				try {
					dropperId = UUID.fromString(hitResult.getWireId().name().replace(WIRE_DROPPER_L, "").replace(WIRE_DROPPER_U, ""));
				} catch (Exception e) {
					return InteractionResult.FAIL;
				}

				CompoundTag nbt = edge.getWireConnectionData().customData().getCommonData();
				List<Dropper> points = nbt.getList(NBT_DROPPERS, Tag.TAG_COMPOUND).stream().map(x -> Dropper.fromNbt((CompoundTag)x)).filter(x -> !x.id().equals(dropperId)).toList();
				ListTag li = new ListTag();
				for (Dropper f : points) {
					li.add(f.toNbt());
				}
				nbt.put(NBT_DROPPERS, li);

				Collection<WireDecorationData> decorations = edge.getDecorations((deco) -> {
					return deco.getDecoration() instanceof RegistrationArmWireDecoration d && d.getDropperId().equals(dropperId);
				});
				for (WireDecorationData decoration : decorations) {
					edge.removeDecorations(level, Optional.of(player), WIRE_LOWER_TENSION, List.of(decoration));
				}

				for (NodeDataRegistryObject<? extends NodeData, ? extends NodeAccessor<? extends NodeData>> type : WiresApi.NODE_DATA_REGISTRY.getRegisteredTypes()) {
					Optional<? extends NodeAccessor<?>> accessor = type.getAccessor(network);
					if (accessor.isPresent() && accessor.get() instanceof GenericWireNodeAccessor a) {
						Collection<WireNode> nodes = new ArrayList<>(a.get(hitResult.getWireId()));
						for (WireNode node : nodes) {
							network.removeNode(node.getId(), node.getPos(), Optional.of(player));
							a.remove(node);
						}
					}
				}

				network.setEdge(edge, true);
				network.sendEdgeToClient(edge);
				network.setDirty();
			
			// --- REMOVE WIRES ---
			} else if (player.getItemInHand(hand).is(Items.SHEARS)) {
				network.removeEdge(hitResult.getWireId().id(), hitResult.getLocation().toVector3f(), Optional.of(player));

			// --- CREATE DROPPERS ---
			} else if (player.getItemInHand(hand).is(ModItems.TAG_WRENCH)) {
				if (edge != null) {
					Optional<NewWireCollision> collisionOpt = hitResult.getCollision(level);
					if (!collisionOpt.isPresent()) {
						return InteractionResult.FAIL;
					}

					NewWireCollision collision = collisionOpt.get();
					float pos = (float)ModMath.snapNearest(hitResult.getPosOnWire(), DECORATION_GRID_SIZE);
					float posPercentage = MathUtils.clamp(1F / collision.length(hitResult.getWireId().name()) * pos, 0F, 1F);

					CompoundTag nbt = edge.getWireConnectionData().customData().getCommonData();
					Map<UUID, Dropper> pointsById = new HashMap<>();
					TreeSet<Float> pointsByLocation = new TreeSet<>();
					for (Tag tag : nbt.getList(NBT_DROPPERS, Tag.TAG_COMPOUND)) {
						Dropper dropper = Dropper.fromNbt((CompoundTag)tag);
						pointsById.put(dropper.id(), dropper);
						pointsByLocation.add(collision.length(WIRE_LOWER_TENSION) * dropper.pos());
					}

					if (edge.isOccupied(pos, WIRE_LOWER_TENSION, DragonLib.PIXEL / 2) || edge.isOccupied(pos, WIRE_UPPER_TENSION, DragonLib.PIXEL / 2)) {
						player.displayClientMessage(TextUtils.translate(KEY_INVALID_DROPPER_LOCATION).withStyle(ChatFormatting.RED), true);
						return InteractionResult.FAIL;
					}
					
					Float ceil = pointsByLocation.ceiling(pos);
					Float floor = pointsByLocation.floor(pos);
					if ((ceil != null && Math.abs(ceil - pos) < DECORATION_GRID_SIZE / 2) || (floor != null && Math.abs(pos - floor) < DECORATION_GRID_SIZE / 2)) {
						player.displayClientMessage(TextUtils.translate(KEY_DROPPER_EXISTS).withStyle(ChatFormatting.RED), true);
						return InteractionResult.FAIL;
					}

					UUID dropperId;
					do {
						dropperId = UUID.randomUUID();
					} while (pointsById.containsKey(dropperId));

					pointsById.put(dropperId, new Dropper(dropperId, posPercentage));
					ListTag li = new ListTag();
					for (Dropper f : pointsById.values()) {
						li.add(f.toNbt());
					}
					nbt.put(NBT_DROPPERS, li);
					
					network.setEdge(edge, true);
					network.sendEdgeToClient(edge);
					network.setDirty();
				}


			// --- PLACE REGISTRATION ARMS ---
			} else if (player.getItemInHand(hand).is(ModItems.TAG_CANTILEVERS) && (hitResult.getWireId().name().startsWith(WIRE_DROPPER_L) || hitResult.getWireId().name().startsWith(WIRE_DROPPER_U))) {
				ItemStack stack = player.getItemInHand(hand);
				RegistrationArmWireDecoration element;
				
				Optional<NewWireCollision> collisionOpt = network.getCollisionById(edge.getId());
				if (!collisionOpt.isPresent()) {
					return InteractionResult.FAIL;
				}

				NewWireCollision collision = collisionOpt.get();
				UUID dropperId;
				try {
					dropperId = UUID.fromString(hitResult.getWireId().name().replace(WIRE_DROPPER_L, "").replace(WIRE_DROPPER_U, ""));
				} catch (Exception e) {
					return InteractionResult.FAIL;
				}

				// Check whether registration arm is set for this dropper
				for (WireDecorationData decoration : edge.getDecorations()) {
					if (decoration.getDecoration() instanceof RegistrationArmWireDecoration deco) {
						if (deco.getDropperId().equals(dropperId)) {
							player.displayClientMessage(TextUtils.translate(KEY_DROPPER_HAS_REGISTRATION_ARM).withStyle(ChatFormatting.RED), true);
							return InteractionResult.FAIL;
						}
					}
				}

				boolean centered = CantileverBlockItem.getCantileverType(stack) == ECantileverRegistrationArmType.CENTER;
				float pos = collision.worldPosToWirePos(WIRE_LOWER_TENSION, collision.getWirePointsOf(WIRE_DROPPER_L + dropperId).vertices()[0]);
				boolean front = isFront(network.getNode(edge.getNodeAId()).getPos(), network.getNode(edge.getNodeBId()).getPos(), player.getViewVector(0).toVector3f()) ^ CantileverBlockItem.getCantileverType(stack) == ECantileverRegistrationArmType.OUTER;
				RegistrationArmBlock.State state = centered ? State.NORMAL_CENTERED : State.NORMAL;
				float offset = front ? 1 : 0;
				offset += (centered ? DragonLib.PIXEL * -0.5f : DragonLib.PIXEL * 3.5f) * (front ? 1 : -1);

				element = new RegistrationArmWireDecoration(stack.copyWithCount(1), front, state, dropperId);
				if (edge.canPlaceDecoration(pos - element.getRadius(element) + offset, WIRE_LOWER_TENSION, element)) {
					edge.addDecoration(pos - element.getRadius(element) + offset, WIRE_LOWER_TENSION, element);
					stack.shrink(1);
				} else if (!state.isAbove()) {
					element = new RegistrationArmWireDecoration(stack.copyWithCount(1), front, centered ? State.ABOVE_CENTERED : State.ABOVE, dropperId);
					if (edge.addDecoration(pos - element.getRadius(element) + offset, WIRE_LOWER_TENSION, element)) {
						stack.shrink(1);
					}
				}


			// --- PLACE INSULATORS ---
			} else if (player.getItemInHand(hand).is(ModItems.TAG_INSULATORS)) {
				InsulatorWireDecoration element;
				ItemStack stack = player.getItemInHand(hand);
				Optional<NewWireCollision> collisionOpt = network.getCollisionById(edge.getId());
				if (!collisionOpt.isPresent()) {
					return InteractionResult.FAIL;
				}
				NewWireCollision collision = collisionOpt.get();
				boolean isLarge = edge.getWireConnectionData().customData().getCommonData().getFloat(CatenaryHeadspanWireItem.NBT_UPPER_WIRE_HEIGHT) > 1.5f;
				
				if (hitResult.getWireId().name().startsWith(WIRE_DROPPER_L) || hitResult.getWireId().name().startsWith(WIRE_DROPPER_U)) {
					element = new InsulatorWireDecoration(stack.copyWithCount(1));
					String wn = hitResult.getWireId().name().replace(WIRE_DROPPER_L, "").replace(WIRE_DROPPER_U, "");
					
					if (isLarge) {
						wn = WIRE_DROPPER_L + wn;
						if (edge.addDecoration(collision.length(wn) - element.getRadius(element), wn, element)) {
							stack.shrink(1);
						}
					} else {
						wn = WIRE_DROPPER_U + wn;
						if (edge.addDecoration(element.getRadius(element), wn, element)) {
							stack.shrink(1);
						}
					}					
				} else if (hitResult.getWireId().name().equals(WIRE_LOWER_TENSION) || hitResult.getWireId().name().equals(WIRE_UPPER_TENSION)) {
					float pos = (float)ModMath.snapNearest(hitResult.getPosOnWire(), 0.5f);
					if (isLarge) {
						element = new InsulatorWireDecoration(stack.copyWithCount(1));
						if (edge.addDecoration(pos, WIRE_LOWER_TENSION, element)) {
							stack.shrink(1);
						}
					} else {
						if (stack.getCount() < 2) {
							player.displayClientMessage(TextUtils.translate(KEY_NOT_ENOUGH_INSULATORS, 2).withStyle(ChatFormatting.RED), true);
							return InteractionResult.FAIL;
						}
						element = new InsulatorWireDecoration(stack.copyWithCount(1));
						if (edge.canPlaceDecoration(pos, WIRE_LOWER_TENSION, element) && edge.canPlaceDecoration(pos, WIRE_UPPER_TENSION, element)) {
							edge.addDecoration(pos, WIRE_LOWER_TENSION, element);
							edge.addDecoration(pos, WIRE_UPPER_TENSION, element);
							stack.shrink(2);
						} else {							
							player.displayClientMessage(TextUtils.translate(KEY_ONE_INVALID_DECORATION_POSITION, 2).withStyle(ChatFormatting.RED), true);
							return InteractionResult.FAIL;
						}
					}					
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	public static boolean isFront(Vector3f A, Vector3f B, Vector3f D) {		
        float abx = B.x - A.x;
        float abz = B.z - A.z;
        float dx = D.x;
        float dz = D.z;
        float cross = abx * dz - abz * dx;
        return cross > 0;
    }

	public static boolean canConnectCatenary(WireEdge edge, WireId id) {
		if (id.type() != ModWireRegistry.CATENARY_HEADSPAN || !id.name().startsWith(WIRE_DROPPER_L)) {
			return false;
		}

		return toDropperId(id.name()).map(dropperId -> {
			for (WireDecorationData decoration : edge.getDecorations()) {
				if (decoration.getDecoration() instanceof RegistrationArmWireDecoration deco) {
					if (deco.getDropperId().equals(dropperId)) {
						return true;
					}
				}
			}
			return false;
		}).orElse(false);
	}

	public static Optional<UUID> toDropperId(String wireName) {
		try {
			return Optional.of(UUID.fromString(wireName.replace(WIRE_DROPPER_L, "").replace(WIRE_DROPPER_U, "")));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public static Optional<RegistrationArmWireDecoration> getRegistrationArmForDropper(WireEdge edge, UUID dropperId) {
		for (WireDecorationData decoration : edge.getDecorations()) {
			if (decoration.getDecoration() instanceof RegistrationArmWireDecoration deco) {
				if (deco.getDropperId().equals(dropperId)) {
					return Optional.of(deco);
				}
			}
		}
		return Optional.empty();
	}

	public static record Dropper(UUID id, float pos) implements Comparable<Dropper> {

		private static final String NBT_ID = "Id";
		private static final String NBT_POS = "Pos";

		public CompoundTag toNbt() {
			CompoundTag nbt = new CompoundTag();
			nbt.putUUID(NBT_ID, id);
			nbt.putFloat(NBT_POS, pos);
			return nbt;
		}

		public static Dropper fromNbt(CompoundTag nbt) {
			return new Dropper(nbt.getUUID(NBT_ID), nbt.getFloat(NBT_POS));
		}

		@Override
		public int compareTo(Dropper o) {
			return Float.compare(pos(), o.pos());
		}
	}
}
