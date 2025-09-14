package de.mrjulsen.paw.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.joml.Vector3f;

import com.eliotlash.mclib.utils.MathUtils;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.registry.InsulatorWireDecoration;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.paw.util.ModMath;
import de.mrjulsen.wires.util.GraphId;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;

public class CatenaryHeadspanWireType extends AbstractWireType {

	public static final String WIRE_UPPER_TENSION = "upper_tension_wire";
	public static final String WIRE_LOWER_TENSION = "lower_tension_wire";
	public static final String WIRE_TOP_SUPPORT_WIRE = "headspan_wire";
	public static final String WIRE_DROPPER = "dropper";
	public static final String WIRE_CROSS_CONNECTION = "cross_connection";

	public static final String NBT_DROPPER_POSITIONS = "DropperPositions";

	private static final float THICKNESS = 0.75f / 16f;

	public CatenaryHeadspanWireType(ResourceLocation location) {
		super(location);
	}

	@Override
	public int getMaxLength() {
		return ModServerConfig.CATENARY_WIRE_MAX_LENGTH.get();
	}	

	@Override
	public GraphId getGraphId(CompoundTag itemData) {
		return WiresApi.PAW_CATENARY_WIRES;
	}

	@Override
	public void onBreak(Level level, Vector3f breakPosition, Optional<Player> player) {
		if (!player.isPresent() || (!player.get().isCreative() && !player.get().isSpectator())) {
			ItemEntity itementity = new ItemEntity(level, breakPosition.x(), breakPosition.y(), breakPosition.z(), ModItems.CATENARY_WIRE_COIL.asStack());
            itementity.setDefaultPickUpDelay();
            level.addFreshEntity(itementity);
		}
	}

	@Override
	public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData customData, WireNode nodeA, WireNode nodeB) {
		Vector3f start = nodeA.getPos();
		Vector3f end = nodeB.getPos();

		float upperWireHeight = customData.customData().getCommonData().getFloat(CatenaryHeadspanWireItem.NBT_UPPER_WIRE_HEIGHT);
		float topWireHeight = customData.customData().getCommonData().getFloat(CatenaryHeadspanWireItem.NBT_TOP_WIRE_HEIGHT);

		Vector3f wireVec = new Vector3f(end).sub(start);
		Vector3f direction = new Vector3f(wireVec).normalize();
		float wireLength = wireVec.length();
		
		int subSegments = (int)(wireLength / 3);
		List<Float> f = customData.customData().getCommonData().getList(NBT_DROPPER_POSITIONS, Tag.TAG_FLOAT).stream().map(x -> ((FloatTag)x).getAsFloat()).sorted(Float::compare).toList();
		float[] fa = new float[f.size()];
		float pF = 0;
		for (int i = 0; i < f.size(); i++) {
			fa[i] = wireLength * (f.get(i) - pF);
			pF = f.get(i);
		}

		Wire topWire1 = WireBuilder.createWire(WIRE_TOP_SUPPORT_WIRE + 1, context, new Vector3f(start).add(0, topWireHeight, -DragonLib.PIXEL * 2), new Vector3f(end).add(0, topWireHeight, -DragonLib.PIXEL * 2), CableType.HANGING, THICKNESS, topWireHeight - upperWireHeight - 1, SegmentControl.create(Config.custom(fa, false), Config.fixed(subSegments)));
		Wire topWire2 = WireBuilder.createWire(WIRE_TOP_SUPPORT_WIRE + 2, context, new Vector3f(start).add(0, topWireHeight, DragonLib.PIXEL * 2), new Vector3f(end).add(0, topWireHeight, DragonLib.PIXEL * 2), CableType.TENSION, THICKNESS, topWireHeight - upperWireHeight - 1, SegmentControl.create(Config.custom(fa, false), Config.fixed(subSegments)));
		Wire upperWire = WireBuilder.createWire(WIRE_UPPER_TENSION, context, new Vector3f(start).add(0, upperWireHeight, 0), new Vector3f(end).add(0, upperWireHeight, 0), CableType.TIGHT, THICKNESS, 0, SegmentControl.create(Config.custom(fa, false), Config.fixed(subSegments)));
		Wire lowerWire = WireBuilder.createWire(WIRE_LOWER_TENSION, context, new Vector3f(start), new Vector3f(end), CableType.TIGHT, THICKNESS, 0, SegmentControl.create(Config.custom(fa, false), Config.fixed(subSegments)));
		WireBatch batch = WireBatch.of(lowerWire, upperWire, topWire1, topWire2);

		if (upperWire.getCollisionData().isPresent() && lowerWire.getCollisionData().isPresent() && topWire1.getCollisionData().isPresent() && topWire2.getCollisionData().isPresent()) {			
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
				WirePoints crossConnectionWire = crossConnectionWires.get(c).collisionData();
				batch.addSubWire(WireBuilder.createWire(
					WIRE_DROPPER + "_l_" + c,
					context,
					lowerRenderData.vertices()[i],
					upperRenderData.vertices()[i],
					CableType.TIGHT,
					THICKNESS,
					0,
					SegmentControl.single()
				));
				batch.addSubWire(WireBuilder.createWire(
					WIRE_DROPPER + "_u_" + c,
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
			WireGraph network = WireGraphManager.get(level, getGraphId(null));
			WireEdge a = network.getEdge(hitResult.getWireId().id());

			if (player.getItemInHand(hand).is(Items.SHEARS)) {
				network.removeEdge(hitResult.getWireId().id(), hitResult.getLocation().toVector3f(), Optional.of(player));
			} else if (player.getItemInHand(hand).is(Items.DIAMOND)) {
				WireGraph graph = WireGraphManager.get(level, hitResult.getGraphId());
				WireEdge edge = graph.getEdge(hitResult.getWireId().id());
				if (edge != null) {
					CompoundTag nbt = edge.getWireConnectionData().customData().getCommonData();
					Set<Float> points = nbt.getList(NBT_DROPPER_POSITIONS, Tag.TAG_FLOAT).stream().map(x -> ((FloatTag)x).getAsFloat()).collect(Collectors.toSet());					
					float posOnWire = (float)ModMath.snap(hitResult.getPosOnWire(), 0.5f);
					points.add(hitResult.getCollision(level).map(x -> MathUtils.clamp(1F / x.length(hitResult.getWireId().name()) * posOnWire, 0F, 1F)).orElse(0F));
					ListTag li = new ListTag();
					for (float f : points) {
						li.add(FloatTag.valueOf(f));
					}
					nbt.put(NBT_DROPPER_POSITIONS, li);
					
					graph.setEdge(edge);
					graph.sendEdgeToClient(edge);
					graph.setDirty();
				}
			} else if (player.getItemInHand(hand).getItem() != Items.AIR && player.getItemInHand(hand).getItem() instanceof BlockItem) {
				InsulatorWireDecoration element = ModWireRegistry.BROWN_INSULATOR_DECORATION.get();
				ItemStack stack = player.getItemInHand(hand);
				element.setItem(stack.copyWithCount(1));
				if (a.addDecoration(hitResult.getLocation().toVector3f(), hitResult.getWireId().name(), element)) {
					stack.shrink(1);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	public static boolean canConnectCatenary(WireId id) {
		return id.type() == ModWireRegistry.CATENARY_HEADSPAN && id.name().matches("^" + WIRE_DROPPER + "_l_\\d+$");
	}
}
