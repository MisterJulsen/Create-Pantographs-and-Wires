package de.mrjulsen.paw.item;

import org.joml.Vector3f;

import de.mrjulsen.paw.block.TensioningDeviceBlock;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.wires.util.Utils;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.network.WireConnectionSyncData;
import de.mrjulsen.wires.render.WireRenderData;
import de.mrjulsen.wires.render.WireRenderPoint.VertexCorner;
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

public class CatenaryWireType extends AbstractWireType {

	private static final float HANG_FAC = 0.025f;
	private static final float THICKNESS = 0.75f / 16f;

	public CatenaryWireType(ResourceLocation location) {
		super(location);
	}

	@Override
	public boolean isValidConnector(BlockAndTintGetter level, BlockPos pos, IWireConnector connector) {
		return connector instanceof ICatenaryWireConnector;
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
		Vector3f contactWireAttachPointA = data.getWireAttachPointA();
		Vector3f contactWireAttachPointB = data.getWireAttachPointB();
		Vector3f tensionWireAttachPointA = Utils.getNbtVector3f(data.getConnectorAData(), ICatenaryWireConnector.NBT_TENSION_WIRE_ATTACH_POINT);
		Vector3f tensionWireAttachPointB = Utils.getNbtVector3f(data.getConnectorBData(), ICatenaryWireConnector.NBT_TENSION_WIRE_ATTACH_POINT);

		float length = (float)Math.abs(new Vector3f(end).sub(start).length());
		float hang = data.getConnectorAData().contains(TensioningDeviceBlock.NBT_TENSION) || data.getConnectorBData().contains(TensioningDeviceBlock.NBT_TENSION) ? 0.5f : HANG_FAC * length;
		
		Wire tensionWire = WireBuilder.createWire(context, new Vector3f(start).add(tensionWireAttachPointA), new Vector3f(end).add(tensionWireAttachPointB), CableType.TENSION, THICKNESS * 0.75f, hang, SegmentControl.create(Config.fixed((int)(length / 5f)), Config.fixed(2)));
		Wire contactWire = WireBuilder.createWire(context, new Vector3f(start).add(contactWireAttachPointA), new Vector3f(end).add(contactWireAttachPointB), CableType.TIGHT, THICKNESS, 0, SegmentControl.create(Config.fixed((int)(length / 5f)), Config.fixed(2)));
		WireBatch batch = WireBatch.of(contactWire, tensionWire);

		if (context.renderingRequired() && tensionWire.getRenderData().isPresent() && contactWire.getRenderData().isPresent()) {
			WireRenderData tensionRenderData = tensionWire.renderData();
			WireRenderData contactRenderData = contactWire.renderData();
			for (int i = 2; i < tensionRenderData.count() - 1 && i < contactRenderData.count() - 1; i += 2) {
				batch.addSubWire(WireBuilder.createWire(WireCreationContext.RENDERING, contactRenderData.getPoint(i).vertex(VertexCorner.CENTER), tensionRenderData.getPoint(i).vertex(VertexCorner.CENTER), CableType.TIGHT, THICKNESS * 0.4f, 0, SegmentControl.single()));
			}
		}
		return batch;
	}
}
