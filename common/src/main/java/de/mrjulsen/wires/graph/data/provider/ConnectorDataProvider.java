package de.mrjulsen.wires.graph.data.provider;

import java.util.Optional;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import de.mrjulsen.wires.graph.registry.IRegisterable;
import net.minecraft.nbt.CompoundTag;

/**
 * A data wrapper that cleanly and securely decodes and provides the raw data of a connector or another connection point.
 * While the {@link CompoundTag} could also be used directly, this wrapper allows safe access to the data, guarantees
 * its validity, and can also be used to compare types via {@code instanceof} to more easily perform context-based operations
 * without having to store an ID in the NBT data. All instances that allow wire connections can thus provide their own
 * object with data that can be uniformly understood by every wire builder.
 * 
 * @apiNote This class already provides a few methods for standard data that should be provided by most connection points.
 * If you create a subclass of this for a specialized use case, you may want to override these methods if the data
 * cannot be provided or is provided in a non-standardized form.
 */
public abstract class ConnectorDataProvider implements IRegisterable<ConnectorDataProvider> {    

    /**
     * A convenient and secure way to receive this data as a specific type.
     * @param <T> The desired data type.
     * @param clazz The class reference for this type.
     * @return The data in the specified type or nothing (if this data is no instance of this type).
     */
    public final <T extends ConnectorDataProvider> Optional<T> getAsTypeIfMatching(Class<T> clazz) {
        if (clazz.isInstance(this)) {
            try {
                return Optional.of(clazz.cast(this));
            } catch (ClassCastException e) {}
        }
        return Optional.empty();
    }

    public static final class Empty extends ConnectorDataProvider {

        @Override
        public CompoundTag serializeNbt() {
            return new CompoundTag();
        }

        @Override
        public void deserializeNbt(CompoundTag nbt) {
        }

        @Override
        public DLRegistryObject<ConnectorDataProvider> getRegistryType() {
            return (DLRegistryObject<ConnectorDataProvider>)(Object)WiresApi.EMPTY_WIRE_CONNECTOR;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Empty;
        }
        
    }
}
