package de.mrjulsen.paw.block.property;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import de.mrjulsen.paw.registry.ModBlocks;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum ECantileverConnectionType implements StringRepresentable {
    PX16(16, "16px", ModBlocks.TAG_CANTILEVER_CONNECTABLE_16PX),
    PX12(12, "12px", ModBlocks.TAG_CANTILEVER_CONNECTABLE_12PX),
    PX8(8, "8px", ModBlocks.TAG_CANTILEVER_CONNECTABLE_8PX),
    PX5(5, "5px", ModBlocks.TAG_CANTILEVER_CONNECTABLE_5PX),
    PX4(4, "4px", ModBlocks.TAG_CANTILEVER_CONNECTABLE_4PX);

    final String name;
    final int index;
    final TagKey<Block> tag;

    ECantileverConnectionType(int index, String name, TagKey<Block> tag) {
        this.index = index;
        this.name = name;
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public TagKey<Block> getTag() {
        return tag;
    }

    public static ECantileverConnectionType getByIndex(int index) {
        return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(PX16);
    }

    public static ECantileverConnectionType getByName(String name) {
        return Arrays.stream(values()).filter(x -> x.getSerializedName().equals(name)).findFirst().orElse(PX16);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static Stream<TagKey<Block>> tags() {
        return Arrays.stream(values()).map(x -> x.getTag());
    }

    public static boolean hasTag(TagKey<Block> tag) {
        return tags().anyMatch(x -> x.equals(tag));
    }
    
    public static Optional<ECantileverConnectionType> getFirstForTag(TagKey<Block> tag) {
        return Arrays.stream(values()).filter(x -> x.getTag().equals(tag)).findFirst();
    }
    
    public static Optional<ECantileverConnectionType> getFirstForState(BlockState state) {
        return Arrays.stream(values()).filter(x -> state.getTags().anyMatch(y -> y.equals(x.getTag()))).findFirst();
    }
}
