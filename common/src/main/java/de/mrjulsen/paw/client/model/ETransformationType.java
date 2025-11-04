package de.mrjulsen.paw.client.model;

import java.util.Arrays;

import de.mrjulsen.mcdragonlib.util.MapCache;

public enum ETransformationType {
    NONE(0),
    SCALE(-1000),
    TRANSLATE(-1001),
    SCALE_POSITIVE(-1002),
    SCALE_NEGATIVE(-1003);

    static final MapCache<ETransformationType, Integer, Integer> cache = new MapCache<>((i) -> Arrays.stream(values()).filter(x -> x.getIndex() == i).findFirst().orElse(NONE), i -> i);
    int index;

    ETransformationType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public boolean isScale() {
        return this == SCALE || this == SCALE_NEGATIVE || this == SCALE_POSITIVE;
    }

    public static ETransformationType getByIndex(int index) {
        return cache.get(index, index);
    }
}
