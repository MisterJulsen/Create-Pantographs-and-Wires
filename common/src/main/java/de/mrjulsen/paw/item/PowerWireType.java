package de.mrjulsen.paw.item;

import org.joml.Vector3f;

import de.mrjulsen.paw.block.AbstractPlaceableInsulatorBlock;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.util.Const;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.network.WireConnectionSyncData;
import de.mrjulsen.wires.SegmentControl;
import de.mrjulsen.wires.Wire;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireBuilder;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.SegmentControl.Config;
import de.mrjulsen.wires.WireBuilder.CableType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;

public class PowerWireType extends AbstractWireType {

	private static final float HANG_FAC = 0.025f;
	private static final float THICKNESS = Const.PIXEL;	

	public PowerWireType(ResourceLocation location) {
		super(location);
	}

	@Override
	public boolean isValidConnector(BlockAndTintGetter level, BlockPos pos, IWireConnector connector) {
		return connector instanceof AbstractPlaceableInsulatorBlock;
	}

	@Override
	public boolean allowMultiConnections() {
		return false;
	}

	@Override
	public int getMaxLength() {
		return ModServerConfig.CATENARY_WIRE_MAX_LENGTH.get();
	}

	@Override
	public WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionSyncData data) {
		Vector3f start = data.getStartPos();
		Vector3f end = data.getEndPos();
		Vector3f wireAttachPointA = data.getWireAttachPointA();
		Vector3f wireAttachPointB = data.getWireAttachPointB();
	
		float length = (float)Math.abs(new Vector3f(end).sub(start).length());
		Wire wire = WireBuilder.createWire(context, new Vector3f(start).add(wireAttachPointA), new Vector3f(end).add(wireAttachPointB), CableType.HANGING, THICKNESS, HANG_FAC * length, SegmentControl.create(Config.auto(), Config.fixed(1)));
		WireBatch batch = WireBatch.of(wire);
		return batch;
	}
}
