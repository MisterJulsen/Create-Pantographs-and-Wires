package de.mrjulsen.wires;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.resources.ResourceLocation;

public class WireTypeRegistry {
    private static final Map<ResourceLocation, IWireType> registeredTypes = new HashMap<>();

    public static <T extends IWireType> T register(String modid, String name, Function<ResourceLocation, T> type) {
        ResourceLocation location = DLUtils.resourceLocation(modid, name);
        if (registeredTypes.containsKey(location)) {
            throw new IllegalArgumentException("A wire with ID '" + location.toString() + "' is already registered.");
        }
        T wire = type.apply(location);
        registeredTypes.put(location, wire);
        return wire;
    }

    public static IWireType get(ResourceLocation id) {
        if (!has(id)) {
            throw new NoSuchElementException("There is no wire type with id '" + id + "' registered.");
        }
        return registeredTypes.get(id);
    }

    public static boolean has(ResourceLocation id) {
        return registeredTypes.containsKey(id);
    }
}
