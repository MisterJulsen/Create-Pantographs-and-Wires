package de.mrjulsen.wires.decoration;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;

public class WireDecorationRegistry {
    private static final Map<ResourceLocation, Supplier<IWireDecoration<?>>> registeredTypes = new HashMap<>();

    public static <T extends IWireDecoration<T>> Supplier<T> register(String modid, String name, Function<ResourceLocation, T> type) {
        ResourceLocation location = new ResourceLocation(modid, name);
        if (registeredTypes.containsKey(location)) {
            throw new IllegalArgumentException("A wire with ID '" + location.toString() + "' is already registered.");
        }
        Supplier<IWireDecoration<?>> factory = () -> type.apply(location);
        registeredTypes.put(location, factory);
        return (Supplier<T>)factory;
    }

    public static Supplier<IWireDecoration<?>> get(ResourceLocation id) {
        if (!has(id)) {
            throw new NoSuchElementException("There is no wire decoration with id '" + id + "' registered.");
        }
        return registeredTypes.get(id);
    }

    public static boolean has(ResourceLocation id) {
        return registeredTypes.containsKey(id);
    }
}
