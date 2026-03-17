package de.mrjulsen.wires;

import org.joml.Vector3d;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.data.accessor.BlockConnectorNodeAccessor;
import de.mrjulsen.wires.graph.data.accessor.GenericBlockNodeAccessor;
import de.mrjulsen.wires.graph.data.accessor.GenericWireNodeAccessor;
import de.mrjulsen.wires.graph.data.accessor.WireConnectorNodeAccessor;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.GenericBlockNodeData;
import de.mrjulsen.wires.graph.data.node.LatticeMastNodeData;
import de.mrjulsen.wires.graph.data.node.MastNodeData;
import de.mrjulsen.wires.graph.data.node.CatenaryHeadspanConnectionNodeData;
import de.mrjulsen.wires.graph.data.node.CatenaryWireConnectorNodeData;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.DLRegistry;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import de.mrjulsen.wires.graph.registry.NodeDataRegistry;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.network.ModNetworkManager;
import de.mrjulsen.wires.util.GraphId;
import de.mrjulsen.wires.util.SafeChunkUtils;
import net.minecraft.resources.ResourceLocation;

public class WiresApi {
    public static final String MOD_ID = "wiresapi";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DLRegistry<ConnectorDataProvider> CONNECTOR_DATA_PROVIDER_REGISTRY = new DLRegistry<>();
    public static final NodeDataRegistry NODE_DATA_REGISTRY = new NodeDataRegistry();
    
    public static final NodeDataRegistryObject<GenericBlockNodeData, GenericBlockNodeAccessor<GenericBlockNodeData>> GENERIC_BLOCK = NODE_DATA_REGISTRY.register(new ResourceLocation(WiresApi.MOD_ID, "generic_block"), GenericBlockNodeData::new, GenericBlockNodeAccessor::new);
    public static final NodeDataRegistryObject<BlockConnectorNodeData, BlockConnectorNodeAccessor> BLOCK_CONNECTOR = NODE_DATA_REGISTRY.register(new ResourceLocation(WiresApi.MOD_ID, "block_connector"), BlockConnectorNodeData::new, BlockConnectorNodeAccessor::new);
    public static final NodeDataRegistryObject<CatenaryWireConnectorNodeData, WireConnectorNodeAccessor> WIRE_CONNECTOR = NODE_DATA_REGISTRY.register(new ResourceLocation(WiresApi.MOD_ID, "wire_connector"), CatenaryWireConnectorNodeData::new, WireConnectorNodeAccessor::new);
    public static final NodeDataRegistryObject<LatticeMastNodeData, GenericBlockNodeAccessor<LatticeMastNodeData>> LATTICE_MAST = NODE_DATA_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "lattice_mast"), LatticeMastNodeData::new, GenericBlockNodeAccessor::new);
    public static final NodeDataRegistryObject<MastNodeData, GenericBlockNodeAccessor<MastNodeData>> MAST = NODE_DATA_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "mast"), MastNodeData::new, GenericBlockNodeAccessor::new);
    public static final NodeDataRegistryObject<CatenaryHeadspanConnectionNodeData, GenericWireNodeAccessor<CatenaryHeadspanConnectionNodeData>> CATENARY_HEADSPAN = NODE_DATA_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "catenary_headspan"), CatenaryHeadspanConnectionNodeData::new, GenericWireNodeAccessor::new);

    public static final DLRegistryObject<BasicConnectorDataProvider.Empty> EMPTY_WIRE_CONNECTOR = CONNECTOR_DATA_PROVIDER_REGISTRY.register(new ResourceLocation(WiresApi.MOD_ID, "empty_wire_connector"), ConnectorDataProvider.Empty::new);
    public static final DLRegistryObject<BasicConnectorDataProvider> BASIC_WIRE_CONNECTOR = CONNECTOR_DATA_PROVIDER_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "basic_wire_connector"), () -> new BasicConnectorDataProvider((Vector3d) null));
    public static final DLRegistryObject<CantileverConnectorDataProvider> CANTILEVER_WIRE_CONNECTOR = CONNECTOR_DATA_PROVIDER_REGISTRY.register(new ResourceLocation(PantographsAndWires.MOD_ID, "cantilever_wire_connector"), () -> new CantileverConnectorDataProvider((Vector3d)null, (Vector3d)null));

    public static final GraphId PAW_CATENARY_WIRES = WireGraphManager.register("paw_catenary", WireGraph::new, WireGraphClient::new);

    
    public static void init() {
        ModNetworkManager.init();

        dev.architectury.event.events.common.TickEvent.SERVER_LEVEL_PRE.register((level) -> {
            SafeChunkUtils.onTick(level);
        });
        
    }
}
