package de.mrjulsen.wires.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.util.collision.LineShape;
import de.mrjulsen.paw.util.collision.RaycastHitResult;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.graph.NewWireCollision;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.NewWireCollision.WireBlockCollision;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.WireTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record WireId(UUID id, String name, IWireType type) {

    private static final String NBT_ID = "Id";
    private static final String NBT_NAME = "Name";
    private static final String NBT_TYPE = "Type";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(NBT_ID, id);
        nbt.putString(NBT_NAME, name);
        nbt.putString(NBT_TYPE, type.getRegistryId().toString());
        return nbt;
    }

    public static Optional<WireId> fromNbt(CompoundTag nbt) {
        ResourceLocation type = DLUtils.resourceLocation(nbt.getString(NBT_TYPE));
        if (!WireTypeRegistry.has(type)) {
            return Optional.empty();
        }
        return Optional.of(new WireId(
            nbt.getUUID(NBT_ID),
            nbt.getString(NBT_NAME),
            WireTypeRegistry.get(type)
        ));
    }

    public static Optional<RaycastHitResult> checkCollision(Level lvl, BlockPos pos, Vector3d rayOrigin, Vector3d rayDirection) {
        RaycastHitResult hit = null;
        for (WireGraphClient graph : WireGraphManager.getAllClient(lvl)) {            
            Collection<NewWireCollision> collisions = graph.getCollisionsInBlock(pos);
            Collection<WireBlockCollision> collisionsinBlock = new ArrayList<>();
            for (NewWireCollision collision : collisions) {
                collisionsinBlock.addAll(Collections.synchronizedCollection(collision.collisionsInBlock(pos)));
            }

            for (WireBlockCollision collision : collisionsinBlock) {
                Vector3d a = new Vector3d(collision.getInVector()).add(pos.getX(), pos.getY(), pos.getZ());
                Vector3d b = new Vector3d(collision.getOutVector()).add(pos.getX(), pos.getY(), pos.getZ());
                LineShape wire = new LineShape(a, b, DragonLib.BLOCK_PIXEL * 2);
                Optional<RaycastHitResult> res = wire.intersects(rayOrigin, rayDirection).map(h -> new RaycastHitResult(new Vec3(h.x(), h.y(), h.z()), pos, new Vector3d(h.x(), h.y(), h.z()).sub(rayOrigin).length(), collision));
                if (res.isPresent() && (hit == null || hit.getDistance() < res.get().getDistance())) {
                    hit = res.get();
                }
            }
        }
        return Optional.ofNullable(hit);
    }
}
