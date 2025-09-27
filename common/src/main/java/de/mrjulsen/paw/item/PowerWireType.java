package de.mrjulsen.paw.item;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.registry.InsulatorWireDecoration;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.paw.util.Const;
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.util.GraphId;
import de.mrjulsen.wires.SegmentControl;
import de.mrjulsen.wires.Wire;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireBuilder;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.SegmentControl.Config;
import de.mrjulsen.wires.WireBuilder.CableType;
import net.minecraft.nbt.CompoundTag;
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

public class PowerWireType extends AbstractWireType {

	private static final float HANG_FAC = 0.025f;
	private static final float THICKNESS = Const.PIXEL;	

	public PowerWireType(ResourceLocation location) {
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
	public void onBreak(Level level, Vector3f breakPosition, Optional<Player> player, IWireGraph graph, WireEdge edge) {
		if (!player.isPresent() || (!player.get().isCreative() && !player.get().isSpectator())) {
			ItemEntity itementity = new ItemEntity(level, breakPosition.x(), breakPosition.y(), breakPosition.z(), ModItems.WIRE.asStack());
            itementity.setDefaultPickUpDelay();
            level.addFreshEntity(itementity);
		}
	}
	
	@Override
	public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData customData, WireEdge edge, WireNode nodeA, WireNode nodeB) {
		BasicConnectorDataProvider dataA = customData.connectorA().getAsTypeIfMatching(BasicConnectorDataProvider.class).orElse(null);
		BasicConnectorDataProvider dataB = customData.connectorB().getAsTypeIfMatching(BasicConnectorDataProvider.class).orElse(null);
		if (dataA == null || dataB == null) {
			return WireBatch.of();
		}

		Vector3f a = new Vector3f(nodeA.getPos()).add(dataA.getAttachOffset());
		Vector3f b = new Vector3f(nodeB.getPos()).add(dataB.getAttachOffset());

		float length = a.distance(b);
		Wire wire = WireBuilder.createWire("main", context, a, b, CableType.HANGING, THICKNESS, HANG_FAC * length, SegmentControl.create(Config.auto(), Config.fixed(1)));
		WireBatch batch = WireBatch.of(wire);
		return batch;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand, WireHitResult hitResult) {
		if (!level.isClientSide) {
			WireGraph network = WireGraphManager.get(level, getGraphId(null));
			WireEdge a = network.getEdge(hitResult.getWireId().id());

			if (player.getItemInHand(hand).is(Items.SHEARS)) {
				network.removeEdge(hitResult.getWireId().id(), hitResult.getLocation().toVector3f(), Optional.of(player));
			} else if (player.getItemInHand(hand).getItem() != Items.AIR && player.getItemInHand(hand).getItem() instanceof BlockItem) {
				ItemStack stack = player.getItemInHand(hand);
				InsulatorWireDecoration element = new InsulatorWireDecoration(stack.copyWithCount(1));
				if (a.addDecoration(hitResult.getLocation().toVector3f(), "main", element)) {
					stack.shrink(1);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
}
