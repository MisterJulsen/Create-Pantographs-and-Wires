package de.mrjulsen.paw.block.property;

import java.util.Arrays;
import com.tterrag.registrate.util.entry.BlockEntry;

import de.mrjulsen.paw.block.InsulatorBlock;
import de.mrjulsen.paw.registry.ModBlocks;
import net.minecraft.util.StringRepresentable;

public enum EInsulatorType implements StringRepresentable {
    GREEN("green", 0, ModBlocks.INSULATOR_GREEN),
    BROWN("brown", 1, ModBlocks.INSULATOR_BROWN);

    final String name;
    final int id;
    final BlockEntry<InsulatorBlock> insulatorBlock;

    EInsulatorType(String name, int id, BlockEntry<InsulatorBlock> insulatorBlock) {
        this.name = name;
        this.id = id;
        this.insulatorBlock = insulatorBlock;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public InsulatorBlock getInsulatorBlock() {
        return insulatorBlock.get();
    }

    public static EInsulatorType getByName(String name) {
        return Arrays.stream(values()).filter(x -> x.getName().equals(name)).findFirst().orElse(BROWN);
    }

    public static EInsulatorType getById(int id) {
        return Arrays.stream(values()).filter(x -> x.getId() == id).findFirst().orElse(BROWN);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}