package de.mrjulsen.paw.item;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.util.Const;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.SegmentControl;
import de.mrjulsen.wires.Wire;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireBuilder;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.WirePoints;
import de.mrjulsen.wires.WireBuilder.CableType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;

public class SupportWireType extends PAWWireType {

	private static final float THICKNESS = Const.PIXEL;	

	public SupportWireType(ResourceLocation location) {
		super(location);
	}

	@Override
	public int getMaxLength() {
		return ModServerConfig.SUPPORT_WIRE_MAX_LENGTH.get();
	}

	@Override
	public int getWireLength(int connectionLength) {
		return connectionLength;
	}
	
	@Override
	public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData customData, WireEdge edge, WireNode nodeA, WireNode nodeB) {
		Vector3f a = new Vector3f(nodeA.getPos());
		Vector3f b = new Vector3f(nodeB.getPos());

		Vector3f direction = new Vector3f(b).sub(a);
		direction = new Vector3f(direction.x(), 0, direction.z());
		Vector3f rightVec = new Vector3f(direction.z(), 0, -direction.x()).normalize();
		direction.absolute().normalize();
        Vector3f offsetA = new Vector3f(rightVec).mul(DragonLib.BLOCK_PIXEL * 2);
        Vector3f offsetB = new Vector3f(rightVec).mul(DragonLib.BLOCK_PIXEL * -2);

		Wire wire1 = WireBuilder.createWire("main1", context, new Vector3f(a).add(offsetA.x(), 0, offsetA.z()), new Vector3f(b).add(offsetA.x(), 0, offsetA.z()), CableType.TIGHT, THICKNESS, 0, SegmentControl.createAuto());
		Wire wire2 = WireBuilder.createWire("main2", context, new Vector3f(a).add(offsetB.x(), 0, offsetB.z()), new Vector3f(b).add(offsetB.x(), 0, offsetB.z()), CableType.TIGHT, THICKNESS, 0, SegmentControl.createAuto());
		
		WirePoints topRenderData1 = wire1.collisionData();
		WirePoints topRenderData2 = wire2.collisionData();
		Wire crossConnection1 = WireBuilder.createWire("connection1", WireCreationContext.RENDERING, topRenderData1.vertices()[0], topRenderData2.vertices()[0], CableType.TIGHT, THICKNESS, 0, SegmentControl.single());
		Wire crossConnection2 = WireBuilder.createWire("connection2", WireCreationContext.RENDERING, topRenderData1.vertices()[topRenderData1.vertices().length - 1], topRenderData2.vertices()[topRenderData2.vertices().length - 1], CableType.TIGHT, THICKNESS, 0, SegmentControl.single());
		
		WireBatch batch = WireBatch.of(wire1, wire2, crossConnection1, crossConnection2);
		return batch;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand, WireHitResult hitResult) {
		if (!level.isClientSide) {
			WireGraph network = WireGraphManager.get(level, getGraphId(null));
			WireEdge a = network.getEdge(hitResult.getWireId().id());
			if (player.getItemInHand(hand).is(Items.SHEARS)) {
				network.removeEdge(hitResult.getWireId().id(), hitResult.getLocation().toVector3f(), Optional.of(player));
			}
		}
		return InteractionResult.SUCCESS;
	}
}
