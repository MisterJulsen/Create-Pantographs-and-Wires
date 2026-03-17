package de.mrjulsen.wires;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;

import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.wires.decoration.WireDecorationData;
import de.mrjulsen.wires.render.WireRenderData;
import de.mrjulsen.wires.render.WireSegmentRenderData;
import de.mrjulsen.wires.render.WireSegmentRenderDataBatch;
import net.minecraft.core.SectionPos;

/** A collection of several individual wires combined to one cable connection. */
public class WireBatch {
    private final Map<String, Wire> subWires = new HashMap<>();

    private final Cache<ImmutableMap<String, WirePoints>> collisionCache = new Cache<>(() -> {
        ImmutableMap.Builder<String, WirePoints> points = ImmutableMap.builder();
        for (Map.Entry<String, Wire> wire : subWires.entrySet()) {
            if (!wire.getValue().getCollisionData().isPresent()) {
                continue;
            }
            points.put(wire.getKey(), wire.getValue().collisionData());
        }
        return points.build();
    });
    private final Cache<ImmutableMap<String, WireRenderData>> renderCache = new Cache<>(() -> {
        ImmutableMap.Builder<String, WireRenderData> points = ImmutableMap.builder();
        for (Map.Entry<String, Wire> wire : subWires.entrySet()) {
            if (!wire.getValue().getRenderData().isPresent()) {
                continue;
            }
            points.put(wire.getKey(), wire.getValue().renderData());
        }
        return points.build();
    });

    
    private WireBatch() {}

    /**
     * Create a new collection of wires with initial values.
     * @param mainWire The first wire
     */
    public WireBatch(Wire mainWire) {
        this.subWires.put(mainWire.name(), mainWire);
    }

    /**
     * Create a new collection of wires with initial values.
     * @param wires The first wires
     * @return
     */
    public static WireBatch of(Wire... wires) {
        if (wires.length <= 0) {
            return new WireBatch();
        }
        WireBatch batch = new WireBatch(wires[0]);
        for (int i = 1; i < wires.length; i++) {
            batch.addSubWire(wires[i]);
        }
        return batch;
    }

    /**
     * Add additional wire to this collection.
     * @param subWire The new wire
     */
    public Wire addSubWire(Wire subWire) {
        this.subWires.put(subWire.name(), subWire);
        return subWire;
    }

    public int count() {
        return subWires.size();
    }

    public boolean isEmpty() {
        return count() <= 0;
    }

    public Map<String, Wire> getWires() {
        return Collections.unmodifiableMap(subWires);
    }

    public ImmutableMap<String, WirePoints> getCollisions() {
        return collisionCache.get();
    }

    public ImmutableMap<String, WireRenderData> getRenderData() {
        return renderCache.get();
    }

    public Map<SectionPos, WireSegmentRenderDataBatch> splitRenderDataInChunkSections(UUID id, Collection<WireDecorationData> decorations) {
        if (isEmpty()) {
            return Map.of();
        }

        Map<SectionPos, WireSegmentRenderDataBatch> result = new HashMap<>();

        // Cache
        Map<String, TreeMap<Double, WireDecorationData>> decorationsMapped = new HashMap<>();
        for (WireDecorationData deco : decorations) {
            decorationsMapped.computeIfAbsent(deco.getWireName(), name -> new TreeMap<>()).put(deco.getPos(), deco);
        }
        
        for (Map.Entry<String, Wire> wireData : subWires.entrySet()) {
            Wire wire = wireData.getValue();
            String wireName = wireData.getKey();
            TreeMap<Double, WireDecorationData> wireDecor = decorationsMapped.get(wireName);

            Optional<WireRenderData> data = wire.getRenderData();
            if (!data.isPresent()) continue;
            Map<SectionPos, WireSegmentRenderData> segments = data.get().splitInChunkSections(wireDecor);
            for (Map.Entry<SectionPos, WireSegmentRenderData> segment : segments.entrySet()) {
                result.computeIfAbsent(segment.getKey(), x -> new WireSegmentRenderDataBatch(id, segment.getKey())).addSegment(segment.getValue());
            }
        }

        return result;
    }
}
