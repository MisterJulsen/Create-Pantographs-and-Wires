package de.mrjulsen.paw.block.property;

import java.util.Arrays;

import de.mrjulsen.paw.registry.ModBlocks;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum ECantileverMastConnection implements StringRepresentable {
    NONE((byte)0, "none", null),
    BRACKET((byte)1, "bracket", ModBlocks.TAG_CANTILEVER_MAST_BRACKET_FITTING),
    HINGE((byte)2, "hinge", ModBlocks.TAG_CANTILEVER_MAST_HINGE);

    final String name;
    final byte index;
    final TagKey<Block> tag;

    private static final ECantileverMastConnection[] nonEmptyValues = Arrays.stream(values()).filter(x -> x != NONE).toArray(ECantileverMastConnection[]::new);

    ECantileverMastConnection(byte index, String name, TagKey<Block> tag) {
        this.index = index;
        this.name = name;
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public byte getIndex() {
        return index;
    }

    public TagKey<Block> getTag() {
        return tag;
    }

    public static ECantileverMastConnection getByIndex(int index) {
        return Arrays.stream(values()).filter(x -> x.getIndex() == index).findFirst().orElse(NONE);
    }

    public static ECantileverMastConnection getByName(String name) {
        return Arrays.stream(values()).filter(x -> x.getSerializedName().equals(name)).findFirst().orElse(NONE);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static ECantileverMastConnection getFirstForState(BlockState state) {
        return Arrays.stream(nonEmptyValues).filter(x -> state.getTags().anyMatch(y -> y.equals(x.getTag()))).findFirst().orElse(NONE);
    }
}
