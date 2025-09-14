package de.mrjulsen.paw.data;

import java.util.Arrays;

import net.minecraft.world.phys.HitResult;

public enum CustomHitResultTypes {
    WIRE;

    private CustomHitResultTypes() {
    }  
    
    
    public HitResult.Type getType() {
        return Arrays.stream(HitResult.Type.values()).filter(x -> x.name().toUpperCase().equals(name().toUpperCase())).findFirst().orElse(HitResult.Type.MISS);
    }
}
