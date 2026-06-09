package de.mrjulsen.wires.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.mrjulsen.wires.util.GraphId;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public final class WireGraphManager {
    private WireGraphManager() {}
    
    public static interface GraphFactory {
        WireGraph build(GraphId id, Level level);
    }

    public static interface GraphClientFactory {
        WireGraphClient build(GraphId id, Level level);
    }

    // Registry
    private static final Map<GraphId, GraphFactory> graphFactories = new ConcurrentHashMap<>();
    private static final Map<GraphId, GraphClientFactory> graphClientFactories = new ConcurrentHashMap<>();

    private static final Map<ResourceLocation /* dimension */, Map<GraphId, WireGraph>> graphs = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation /* dimension */, Map<GraphId, WireGraphClient>> clientGraphs = new ConcurrentHashMap<>();


    public static GraphId register(String name, GraphFactory commonFactory, GraphClientFactory clientFactory) {
        GraphId id = new GraphId(name);
        if (graphFactories.containsKey(id) || graphClientFactories.containsKey(id)) {
            throw new IllegalStateException("A wire graph with the ID '" + id + "' is already registered!");
        }
        graphFactories.put(id, commonFactory);
        graphClientFactories.put(id, clientFactory);
        return id;
    }

    private static WireGraphClient createClientGraph(GraphId id, Level level) {
        if (!graphClientFactories.containsKey(id)) {
            throw new IllegalStateException("A wire graph with the ID '" + id + "' is not registered!");
        }
        return graphClientFactories.get(id).build(id, level);
    }

    public static void build(ServerLevel level) {
        for (Map.Entry<GraphId, GraphFactory> factory : graphFactories.entrySet()) {
            final GraphId id = factory.getKey();
            final WireGraph graph = factory.getValue().build(id, level);
            graphs.computeIfAbsent(level.dimension().location(), x -> new HashMap<>()).put(id, graph);
            level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(() -> graph, (nbt, provider) -> graph.load(level, nbt), DataFixTypes.SAVED_DATA_SCOREBOARD), graph.getFileId());
        }        
    }



    public static void clearServer() {
        graphs.clear();
    }

    public static void clearClient() {
        clientGraphs.clear();
    }

    public synchronized static WireGraph get(Level level, GraphId id) {
        return graphs.get(level.dimension().location()).get(id);
    }

    public synchronized static WireGraphClient getClient(Level level, GraphId id) {
        return clientGraphs.computeIfAbsent(level.dimension().location(), x -> new HashMap<>()).computeIfAbsent(id, x -> createClientGraph(x, level));
    }

    public synchronized static Collection<WireGraph> getAll(Level level) {
        if (!graphs.containsKey(level.dimension().location())) {
            return List.of();
        }
        return Collections.unmodifiableCollection(graphs.get(level.dimension().location()).values());
    }

    public synchronized static Collection<WireGraphClient> getAllClient(Level level) {
        if (!clientGraphs.containsKey(level.dimension().location())) {
            return List.of();
        }
        return Collections.unmodifiableCollection(clientGraphs.get(level.dimension().location()).values());
    }
    
}
