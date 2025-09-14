package de.mrjulsen.paw.item;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.paw.block.TensioningDeviceBlock;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.registry.InsulatorWireDecoration;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.util.GraphId;
import de.mrjulsen.wires.util.Utils;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.network.WireConnectionSyncData;
import de.mrjulsen.wires.render.WireRenderData;
import de.mrjulsen.wires.render.WireRenderPoint.VertexCorner;
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

public class CatenaryWireType extends AbstractWireType {

	private static final float HANG_FAC = 0.025f;
	private static final float THICKNESS = 0.75f / 16f;

	public CatenaryWireType(ResourceLocation location) {
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

	public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionSyncData data) {
		Vector3f start = data.getStartPos();
		Vector3f end = data.getEndPos();
		Vector3f contactWireAttachPointA = data.getWireAttachPointA();
		Vector3f contactWireAttachPointB = data.getWireAttachPointB();
		Vector3f tensionWireAttachPointA = Utils.getNbtVector3f(data.getConnectorAData(), ICatenaryWireConnector.NBT_TENSION_WIRE_ATTACH_POINT);
		Vector3f tensionWireAttachPointB = Utils.getNbtVector3f(data.getConnectorBData(), ICatenaryWireConnector.NBT_TENSION_WIRE_ATTACH_POINT);

		float length = (float)Math.abs(new Vector3f(end).sub(start).length());
		float hang = data.getConnectorAData().contains(TensioningDeviceBlock.NBT_TENSION) || data.getConnectorBData().contains(TensioningDeviceBlock.NBT_TENSION) ? 0.5f : HANG_FAC * length;
		
		Wire tensionWire = WireBuilder.createWire("tension", context, new Vector3f(start).add(tensionWireAttachPointA), new Vector3f(end).add(tensionWireAttachPointB), CableType.TENSION, THICKNESS * 0.75f, hang, SegmentControl.create(Config.fixed((int)(length / 5f)), Config.fixed(2)));
		Wire contactWire = WireBuilder.createWire("contact", context, new Vector3f(start).add(contactWireAttachPointA), new Vector3f(end).add(contactWireAttachPointB), CableType.TIGHT, THICKNESS, 0, SegmentControl.create(Config.fixed((int)(length / 5f)), Config.fixed(2)));
		WireBatch batch = WireBatch.of(contactWire, tensionWire);

		if (context.renderingRequired() && tensionWire.getRenderData().isPresent() && contactWire.getRenderData().isPresent()) {
			WireRenderData tensionRenderData = tensionWire.renderData();
			WireRenderData contactRenderData = contactWire.renderData();
			for (int i = 2, c = 0; i < tensionRenderData.count() - 1 && i < contactRenderData.count() - 1; i += 2, c++) {
				batch.addSubWire(WireBuilder.createWire("dropper" + c, WireCreationContext.RENDERING, contactRenderData.getPoint(i).vertex(VertexCorner.CENTER), tensionRenderData.getPoint(i).vertex(VertexCorner.CENTER), CableType.TIGHT, THICKNESS * 0.4f, 0, SegmentControl.single()));
			}
		}
		return batch;
	}
	
	@Override
	public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData customData, WireNode nodeA, WireNode nodeB) {
		CantileverConnectorDataProvider dataA = customData.connectorA().getAsTypeIfMatching(CantileverConnectorDataProvider.class).orElse(null);
		CantileverConnectorDataProvider dataB = customData.connectorB().getAsTypeIfMatching(CantileverConnectorDataProvider.class).orElse(null);
		if (dataA == null || dataB == null) {
			return WireBatch.of();
		}

		Vector3f start = nodeA.getPos();
		Vector3f end = nodeB.getPos();
		Vector3f contactWireAttachPointA = dataA.getAttachOffset();
		Vector3f contactWireAttachPointB = dataB.getAttachOffset();
		Vector3f tensionWireAttachPointA = dataA.getTensionWireAttachOffset();
		Vector3f tensionWireAttachPointB = dataB.getTensionWireAttachOffset();

		float length = (float)Math.abs(new Vector3f(end).sub(start).length());
		float hang = HANG_FAC * length; // TODO Tensioning device: data.getConnectorAData().contains(TensioningDeviceBlock.NBT_TENSION) || data.getConnectorBData().contains(TensioningDeviceBlock.NBT_TENSION) ? 0.5f : HANG_FAC * length;
		
		Wire tensionWire = WireBuilder.createWire("tension", context, new Vector3f(start).add(tensionWireAttachPointA), new Vector3f(end).add(tensionWireAttachPointB), CableType.TENSION, THICKNESS * 0.75f, hang, SegmentControl.create(Config.fixed((int)(length / 5f)), Config.fixed(2)));
		Wire contactWire = WireBuilder.createWire("contact", context, new Vector3f(start).add(contactWireAttachPointA), new Vector3f(end).add(contactWireAttachPointB), CableType.TIGHT, THICKNESS, 0, SegmentControl.create(Config.fixed((int)(length / 5f)), Config.fixed(2)));
		WireBatch batch = WireBatch.of(contactWire, tensionWire);

		if (context.renderingRequired() && tensionWire.getRenderData().isPresent() && contactWire.getRenderData().isPresent()) {
			WireRenderData tensionRenderData = tensionWire.renderData();
			WireRenderData contactRenderData = contactWire.renderData();
			for (int i = 2, c = 0; i < tensionRenderData.count() - 1 && i < contactRenderData.count() - 1; i += 2, c++) {
				batch.addSubWire(WireBuilder.createWire("dropper" + c, WireCreationContext.RENDERING, contactRenderData.getPoint(i).vertex(VertexCorner.CENTER), tensionRenderData.getPoint(i).vertex(VertexCorner.CENTER), CableType.TIGHT, THICKNESS * 0.4f, 0, SegmentControl.single()));
			}
		}
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
				InsulatorWireDecoration element = ModWireRegistry.BROWN_INSULATOR_DECORATION.get();
				ItemStack stack = player.getItemInHand(hand);
				element.setItem(stack.copyWithCount(1));
				if (a.addDecoration(hitResult.getLocation().toVector3f(), "tension", element)) {
					stack.shrink(1);
				}
				if (a.addDecoration(hitResult.getLocation().toVector3f(), "contact", element)) {
					stack.shrink(1);
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
}
