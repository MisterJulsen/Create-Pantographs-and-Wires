package de.mrjulsen.paw.registry;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.components.WireAmountComponent;
import de.mrjulsen.paw.components.WireConnectionDataComponent;
import de.mrjulsen.paw.components.WireSubtypeComponent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(PantographsAndWires.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<WireConnectionDataComponent>> WIRE_CONNECTION_DATA = COMPONENTS.register("wire_connection_data", () ->
            new DataComponentType.Builder<WireConnectionDataComponent>()
                    .persistent(WireConnectionDataComponent.CODEC)
                    .networkSynchronized(WireConnectionDataComponent.STREAM_CODEC)
                    .build()
    );

    public static final RegistrySupplier<DataComponentType<WireAmountComponent>> WIRE_AMOUNT = COMPONENTS.register("wire_amount", () ->
            new DataComponentType.Builder<WireAmountComponent>()
                    .persistent(WireAmountComponent.CODEC)
                    .networkSynchronized(WireAmountComponent.STREAM_CODEC)
                    .build()
    );

    public static final RegistrySupplier<DataComponentType<WireSubtypeComponent>> WIRE_SUBTYPE = COMPONENTS.register("wire_subtype", () ->
            new DataComponentType.Builder<WireSubtypeComponent>()
                    .persistent(WireSubtypeComponent.CODEC)
                    .networkSynchronized(WireSubtypeComponent.STREAM_CODEC)
                    .build()
    );


    public static <T> boolean hasComponent(ItemStack stack, RegistrySupplier<DataComponentType<T>> type) {
        return hasComponent(stack, type.get());
    }

    public static <T> boolean hasComponent(ItemStack stack, DataComponentType<T> type) {
        return stack.has(type);
    }

    public static <T> T getComponent(ItemStack stack, RegistrySupplier<DataComponentType<T>> type, Supplier<T> defaultProvider) {
        return getComponent(stack, type.get(), defaultProvider);
    }

    public static <T> T getComponent(ItemStack stack, DataComponentType<T> type, Supplier<T> defaultProvider) {
        if (hasComponent(stack, type)) {
            return stack.get(type);
        }
        return defaultProvider.get();
    }

    public static <T> T getOrSetComponent(ItemStack stack, RegistrySupplier<DataComponentType<T>> type, Supplier<T> defaultProvider) {
        return getOrSetComponent(stack, type.get(), defaultProvider);
    }

    public static <T> T getOrSetComponent(ItemStack stack, DataComponentType<T> type, Supplier<T> defaultProvider) {
        if (!hasComponent(stack, type)) {
            setComponent(stack, type, defaultProvider.get());
        }
        return getComponent(stack, type, defaultProvider);
    }

    public static <T> void setComponent(ItemStack stack, RegistrySupplier<DataComponentType<T>> type, T value) {
        setComponent(stack, type.get(), value);
    }

    public static <T> void setComponent(ItemStack stack, DataComponentType<T> type, T value) {
        stack.set(type, value);
    }

    public static void init() {
        COMPONENTS.register();
    }
}
