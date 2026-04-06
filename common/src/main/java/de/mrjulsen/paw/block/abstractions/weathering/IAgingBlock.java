package de.mrjulsen.paw.block.abstractions.weathering;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public interface IAgingBlock<T extends Block & IAgingBlock<T, E>, E extends Enum<E>> extends ChangeOverTimeBlock<E> {

    public static class AgeData<T extends Block & IAgingBlock<T, E>, E extends Enum<E>> {
        private final E ageState;
        private final BlockTransform<T, E> transform;
        private final boolean isWaxed;

        public AgeData(E ageState, BlockTransform<T, E> transform, boolean isWaxed) {
            this.ageState = ageState;
            this.transform = transform;
            this.isWaxed = isWaxed;
        }

        public E ageState() {
            return this.ageState;
        }

        public BlockTransform<T, E> transform() {
            return this.transform;
        }

        public boolean isWaxed() {
            return isWaxed;
        }
    }

    public static class BlockTransform<T extends Block & IAgingBlock<T, E>, E extends Enum<E>> {
        private final Supplier<T> previous;
        private final Supplier<T> next;
        private final Supplier<T> waxOrUnwax;

        public BlockTransform(Supplier<T> previous, Supplier<T> next, Supplier<T> waxOrUnwax) {
            this.previous = previous;
            this.next = next;
            this.waxOrUnwax = waxOrUnwax;
        }

        public Supplier<T> previous() {
            return this.previous;
        }

        public Supplier<T> next() {
            return this.next;
        }

        public Supplier<T> waxOrUnwax() {
            return this.waxOrUnwax;
        }
    }


    @NotNull IAgingBlock.AgeData<T, E> getAgeData();


    @Override
    default @NotNull E getAge() {
        return getAgeData().ageState();
    }

    @Override
    default @NotNull Optional<BlockState> getNext(@NotNull BlockState state) {
        return getNext().map((block) -> block.withPropertiesOf(state));
    }

    default Optional<Block> getNext() {
        Supplier<T> nextState = getAgeData().transform().next();
        return Optional.ofNullable(nextState == null ? null : nextState.get());
    }

    default @NotNull Optional<BlockState> getPrevious(@NotNull BlockState state) {
        return getPrevious().map((block) -> block.withPropertiesOf(state));
    }

    default Optional<Block> getPrevious() {
        Supplier<T> prevState = getAgeData().transform().previous();
        return Optional.ofNullable(prevState == null ? null : prevState.get());
    }

    default Optional<BlockState> getWaxToggled(@NotNull BlockState state) {
        Supplier<T> waxToggle = getAgeData().transform().waxOrUnwax();
        return Optional.ofNullable(waxToggle == null ? null : waxToggle.get()).map(block -> block.withPropertiesOf(state));
    }

    default boolean isWaxed() {
        return getAgeData().isWaxed();
    }
}
