package de.mrjulsen.paw.item;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.registry.InsulatorWireDecoration;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.wires.util.GraphId;
import de.mrjulsen.wires.graph.IWireGraph;
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
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.SegmentControl.Config;
import de.mrjulsen.wires.WireBuilder.CableType;
import net.minecraft.ChatFormatting;
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
		float maxHang = Math.min(tensionWireAttachPointA.y() - contactWireAttachPointA.y(), tensionWireAttachPointB.y() - contactWireAttachPointB.y());
		float length = (float)Math.abs(new Vector3f(end).sub(start).length());
		float hang = Math.min(customData.customData().getCommonData().getBoolean(NBT_SUPER_TIGHTENED) ? 0.5f : HANG_FAC * length, maxHang - 0.25f);
		
		Wire tensionWire = WireBuilder.createWire(WIRE_TENSION, context, new Vector3f(start).add(tensionWireAttachPointA), new Vector3f(end).add(tensionWireAttachPointB), CableType.TENSION, THICKNESS * 0.75f, hang, SegmentControl.create(Config.fixed((int)(length / 5f)), Config.fixed(2)));
		Wire contactWire = WireBuilder.createWire(WIRE_CONTACT, context, new Vector3f(start).add(contactWireAttachPointA), new Vector3f(end).add(contactWireAttachPointB), CableType.TIGHT, THICKNESS, 0, SegmentControl.create(Config.fixed((int)(length / 5f)), Config.fixed(2)));
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

			if (player.getItemInHand(hand).is(Items.SHEARS)) {
				network.removeEdge(hitResult.getWireId().id(), hitResult.getLocation().toVector3f(), Optional.of(player));
			} else if (player.getItemInHand(hand).is(ModItems.TAG_INSULATORS) && player.getItemInHand(hand).getItem() instanceof BlockItem) {
				ItemStack stack = player.getItemInHand(hand);
				InsulatorWireDecoration element = new InsulatorWireDecoration(stack.copyWithCount(2));
				
				if (stack.getCount() < 2) {
					player.displayClientMessage(TextUtils.translate(KEY_NOT_ENOUGH_INSULATORS, 2).withStyle(ChatFormatting.RED), true);
					return InteractionResult.FAIL;
				}
				if (a.canPlaceDecoration(hitResult.getPosOnWire(), WIRE_TENSION, element) && a.canPlaceDecoration(hitResult.getPosOnWire(), WIRE_CONTACT, element)) { 
					a.addDecoration(hitResult.getLocation().toVector3f(), WIRE_TENSION, element);
					a.addDecoration(hitResult.getLocation().toVector3f(), WIRE_CONTACT, element);
					stack.shrink(2);
				} else {							
					player.displayClientMessage(TextUtils.translate(KEY_ONE_INVALID_DECORATION_POSITION, 2).withStyle(ChatFormatting.RED), true);
					return InteractionResult.FAIL;
				}
			}
		}
		return InteractionResult.SUCCESS;
	}
}
