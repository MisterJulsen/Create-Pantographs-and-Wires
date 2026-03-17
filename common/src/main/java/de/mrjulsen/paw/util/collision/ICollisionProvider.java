package de.mrjulsen.paw.util.collision;

import java.util.Optional;

import org.joml.Vector3d;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface ICollisionProvider {
    Optional<RaycastHitResult> tryHit(Level level, BlockPos pos, Vector3d rayOrigin, Vector3d rayDirection);
}

