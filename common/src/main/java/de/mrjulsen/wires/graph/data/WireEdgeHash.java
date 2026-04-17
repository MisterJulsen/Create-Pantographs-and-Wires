package de.mrjulsen.wires.graph.data;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.config.ModCommonConfig;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.nbt.CompoundTag;

public class WireEdgeHash {
    private final byte[] data;
    private final int hash;

    public WireEdgeHash(CustomData customData, NodeData nodeA, NodeData nodeB) {
        CompoundTag pointA = new CompoundTag();
        pointA.put("Data", nodeA.serializeNbt());
        pointA.put("CustomData", customData.getCustomDataForPoint(0));

        CompoundTag pointB = new CompoundTag();
        pointB.put("Data", nodeB.serializeNbt());
        pointB.put("CustomData", customData.getCustomDataForPoint(1));

        this.data = compressStringToGzipBytes(customData.getCommonData(), pointA, pointB);
        this.hash = Arrays.hashCode(data);
    }

    public WireEdgeHash(CustomData customData, WireNode nodeA, WireNode nodeB) {
        this(customData, nodeA.getData(), nodeB.getData());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WireEdgeHash h)) return false;
        return Arrays.equals(data, h.data);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    private static byte[] encode(CompoundTag... objects) {
        List<String> parts = new ArrayList<>();
        for (CompoundTag obj : objects) {
            parts.add(obj.toString());
        }
        Collections.sort(parts);
        String s = String.join("", parts);
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] compressStringToGzipBytes(CompoundTag... objects) {
        byte[] encodedData = encode(objects);
        if (!ModCommonConfig.COMPRESS_HASH_VALUES.get()) {
            return encodedData;
        }
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOS = new GZIPOutputStream(bos)) {
            gzipOS.write(encodedData);
        } catch (Exception e) {
            PantographsAndWires.LOGGER.error("Could not compress hash for wire edge data.", e);
            return encodedData;
        }
        return bos.toByteArray();
    }
}