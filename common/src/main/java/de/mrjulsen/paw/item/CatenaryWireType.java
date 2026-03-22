package de.mrjulsen.paw.item;

import java.util.Optional;

import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.registry.InsulatorWireDecoration;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.render.WireRenderData;
import de.mrjulsen.wires.render.WireRenderPoint.VertexCorner;
import de.mrjulsen.wires.SegmentControl;
import de.mrjulsen.wires.Wire;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireBuilder;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.SegmentControl.Config;
import de.mrjulsen.wires.WireBuilder.CableType;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;

public class CatenaryWireType extends PAWWireType {

	private static final String KEY_NOT_ENOUGH_INSULATORS = "item." + PantographsAndWires.MOD_ID + ".wire.not_enough_insulators";
	private static final String KEY_ONE_INVALID_DECORATION_POSITION = "item." + PantographsAndWires.MOD_ID + ".wire.one_invalid_decoration_position";
    public static final String NBT_SUPER_TIGHTENED = "SuperTightened";
    public static final String WIRE_TENSION = "tension";
    public static final String WIRE_CONTACT = "contact";
    public static final String WIRE_DROPPER = "dropper";

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
	public double getWireConsumptionMultiplier(int connectionLength) {
		return 2.0;
	}
	
	@Override
	public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData customData, WireEdge edge, WireNode nodeA, WireNode nodeB) {
		CantileverConnectorDataProvider dataA = customData.connectorA().getAsTypeIfMatching(CantileverConnectorDataProvider.class).orElse(null);
		CantileverConnectorDataProvider dataB = customData.connectorB().getAsTypeIfMatching(CantileverConnectorDataProvider.class).orElse(null);
		if (dataA == null || dataB == null) {
			return WireBatch.of();
		}

		Vector3d start = nodeA.getPos();
		Vector3d end = nodeB.getPos();
		Vector3d contactWireAttachPointA = dataA.getAttachOffset();
		Vector3d contactWireAttachPointB = dataB.getAttachOffset();
		Vector3d tensionWireAttachPointA = dataA.getTensionWireAttachOffset();
		Vector3d tensionWireAttachPointB = dataB.getTensionWireAttachOffset();
		double maxHang = Math.min(tensionWireAttachPointA.y() - contactWireAttachPointA.y(), tensionWireAttachPointB.y() - contactWireAttachPointB.y());
		double length = Math.abs(new Vector3d(end).sub(start).length());
		double hang = Math.min(customData.customData().getCommonData().getBoolean(NBT_SUPER_TIGHTENED) ? 0.5f : HANG_FAC * length, maxHang - 0.25f);
		
		Wire tensionWire = WireBuilder.createWire(WIRE_TENSION, context, new Vector3d(start).add(tensionWireAttachPointA), new Vector3d(end).add(tensionWireAttachPointB), CableType.TENSION, THICKNESS * 0.75f, hang, SegmentControl.create(Config.fixed((int)(length / 5f)), Config.fixed(2)));
		Wire contactWire = WireBuilder.createWire(WIRE_CONTACT, context, new Vector3d(start).add(contactWireAttachPointA), new Vector3d(end).add(contactWireAttachPointB), CableType.TIGHT, THICKNESS, 0, SegmentControl.create(Config.fixed((int)(length / 5f)), Config.fixed(2)));
		WireBatch batch = WireBatch.of(contactWire, tensionWire);

		if (context.renderingRequired() && tensionWire.getRenderData().isPresent() && contactWire.getRenderData().isPresent()) {
			WireRenderData tensionRenderData = tensionWire.renderData();
			WireRenderData contactRenderData = contactWire.renderData();
			for (int i = 2, c = 0; i < tensionRenderData.count() - 1 && i < contactRenderData.count() - 1; i += 2, c++) {
				batch.addSubWire(WireBuilder.createWire(WIRE_DROPPER + c, WireCreationContext.RENDERING, contactRenderData.getPoint(i).vertex(VertexCorner.CENTER), tensionRenderData.getPoint(i).vertex(VertexCorner.CENTER), CableType.TIGHT, THICKNESS * 0.4f, 0, SegmentControl.single()));
			}
		}
		return batch;
	}

	

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand, WireHitResult hitResult) {
		if (!level.isClientSide) {
			WireGraph network = WireGraphManager.get(level, getGraphId(null));
			WireEdge a = network.getEdge(hitResult.getWireId().id());
			Vector3d hitPos = new Vector3d(hitResult.getLocation().x(), hitResult.getLocation().y(), hitResult.getLocation().z());

			if (player.getItemInHand(hand).is(Items.SHEARS)) {
				network.removeEdge(hitResult.getWireId().id(), hitPos, Optional.of(player));
			} else if (player.getItemInHand(hand).is(ModItems.TAG_INSULATORS) && player.getItemInHand(hand).getItem() instanceof BlockItem) {
				ItemStack stack = player.getItemInHand(hand);
				InsulatorWireDecoration element = new InsulatorWireDecoration(stack.copyWithCount(1));
				
				if (stack.getCount() < 2) {
					player.displayClientMessage(TextUtils.translate(KEY_NOT_ENOUGH_INSULATORS, 2).withStyle(ChatFormatting.RED), true);
					return InteractionResult.FAIL;
				}
				if (a.canPlaceDecoration(hitResult.getPosOnWire(), WIRE_TENSION, element) && a.canPlaceDecoration(hitResult.getPosOnWire(), WIRE_CONTACT, element)) { 
					a.addDecoration(hitPos, WIRE_TENSION, element);
					a.addDecoration(hitPos, WIRE_CONTACT, element);
					if (player == null || (!player.isCreative() && !player.isSpectator())) {
						stack.shrink(2);
					}
				} else {							
					player.displayClientMessage(TextUtils.translate(KEY_ONE_INVALID_DECORATION_POSITION, 2).withStyle(ChatFormatting.RED), true);
					return InteractionResult.FAIL;
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
}
