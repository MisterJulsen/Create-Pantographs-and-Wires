package de.mrjulsen.wires;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import de.mrjulsen.wires.util.Utils;
import net.minecraft.resources.ResourceLocation;

public class WireConnectorContextRegistry {
    private static final Map<ResourceLocation, IWireType> registeredTypes = new HashMap<>();

    public static IWireType register(String modid, String name, Function<ResourceLocation, IWireType> type) {
        ResourceLocation location = Utils.resLoc(modid, name);
        if (registeredTypes.containsKey(location)) {
            throw new IllegalArgumentException("A wire with ID '" + location.toString() + "' is already registered.");
        }
        IWireType wire = type.apply(location);
        registeredTypes.put(Utils.resLoc(name), wire);
        return wire;
    }

    public static IWireType get(ResourceLocation id) {
        if (!registeredTypes.containsKey(id)) {
            throw new NoSuchElementException("There is no wire type with id '" + id + "' registered.");
        }
        return registeredTypes.get(id);
    }
}
