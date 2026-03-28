package de.mrjulsen.paw.block.abstractions;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.config.ModServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface IWeatheringBlock<T extends Block & IWeatheringBlock<T>> extends ChangeOverTimeBlock<IWeatheringBlock.WeatherState> {

    record WeatheringData<T extends Block & IWeatheringBlock<T>>(WeatherState weatherState, Supplier<T> nextState, boolean isWaxed) {}

    enum WeatherState {
        UNAFFECTED("", true),
        EXPOSED("exposed", true),
        WEATHERED("weathered", true),
        OXIDIZED("oxidized", true),
        GALVANIZED("galvanized", false);

        final String name;
        final boolean canOxidize;

        final static WeatherState[] oxidationStates = Arrays.stream(values()).filter(x -> x.canOxidize).toArray(WeatherState[]::new);

        WeatherState(String name, boolean canOxidize) {
            this.name = name;
            this.canOxidize = canOxidize;
        }

        public String getName() {
            return name;
        }

        public static WeatherState[] oxidationStates() {
            return oxidationStates;
        }
    }

    @Override
    default @NotNull WeatherState getAge() {
        return getWeatheringData().weatherState();
    }

    @NotNull WeatheringData<T> getWeatheringData();

    default Optional<Block> getNext() {
        Supplier<T> nextState = getWeatheringData().nextState();
        return Optional.ofNullable(nextState == null ? null : nextState.get());
    }

    default @NotNull Optional<BlockState> getNext(@NotNull BlockState state) {
        return getNext().map((block) -> block.withPropertiesOf(state));
    }

    default float getChanceModifier() {
        if (!ModServerConfig.USE_OXIDATION.get()) {
            return 0;
        }
        return this.getAge() == WeatherState.UNAFFECTED ? 0.3F : 0.4F;
    }
}
