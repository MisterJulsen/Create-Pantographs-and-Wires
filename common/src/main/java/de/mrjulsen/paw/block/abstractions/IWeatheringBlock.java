package de.mrjulsen.paw.block.abstractions;

import java.util.Arrays;
import de.mrjulsen.paw.block.abstractions.weathering.IAgingBlock;
import de.mrjulsen.paw.config.ModServerConfig;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public interface IWeatheringBlock<T extends Block & IWeatheringBlock<T>> extends IAgingBlock<T, IWeatheringBlock.WeatherState> {

    public static class WeatherData<T extends Block & IAgingBlock<T, WeatherState>> extends AgeData<T, WeatherState> {
        public WeatherData(WeatherState ageState, BlockTransform<T, WeatherState> transform, boolean isWaxed) {
            super(ageState, transform, isWaxed);
        }
    }

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

        public boolean canOxidize() {
            return canOxidize;
        }

        public static WeatherState[] oxidationStates() {
            return oxidationStates;
        }
    }

    @Override
    default float getChanceModifier() {
        if (!ModServerConfig.USE_OXIDATION.get()) {
            return 0;
        }
        return this.getAge() == WeatherState.UNAFFECTED ? 0.3F : 0.4F;
    }

    @Override
    default @NotNull AgeData<T, WeatherState> getAgeData() {
        return getWeatheringData();
    }

    WeatherData<T> getWeatheringData();
}
