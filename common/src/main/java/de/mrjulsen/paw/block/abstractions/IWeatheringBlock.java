package de.mrjulsen.paw.block.abstractions;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.state.BlockState;

public interface IWeatheringBlock<T extends Block & IWeatheringBlock<T>> extends ChangeOverTimeBlock<IWeatheringBlock.WeatherState> {

    public enum WeatherState {
        UNAFFECTED("", true),
        EXPOSED("exposed", true),
        WEATHERED("weathered", true),
        OXIDIZED("oxidized", true),
        TREATED("treated", false);

        final String name;
        final boolean canOxidize;

        final static WeatherState[] oxidationStates = Arrays.stream(values()).filter(x -> x.canOxidize).toArray(WeatherState[]::new);

        private WeatherState(String name, boolean canOxidize) {
            this.name = name;
            this.canOxidize = canOxidize;
        }

        public String getname() {
            return name;
        }

        public static WeatherState[] oxidationStates() {
            return oxidationStates;
        }
    }
    
    Supplier<T> getNextState();

    default Optional<T> getNext(Block block) {        
        return Optional.ofNullable(getNextState() == null ? null : getNextState().get());
    }

    default Optional<BlockState> getNext(BlockState state) {
        return getNext(state.getBlock()).map((arg2) -> {
            return arg2.withPropertiesOf(state);
        });
    }

    default float getChanceModifier() {
        return this.getAge() == WeatherState.UNAFFECTED ? 0.75F : 1.0F;
    }
}
