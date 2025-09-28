package de.mrjulsen.paw.data;

import net.minecraft.util.StringRepresentable;

public enum HashingType implements StringRepresentable {
    RAW("raw"),
    GZIP_COMPRESSED("gzip_compressed");

    private final String name;

    private HashingType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
