package de.mrjulsen.paw.item;

import java.util.Optional;
import java.util.stream.Stream;

import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.block.CantileverBlock;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity.CantileverData;
import de.mrjulsen.paw.util.collision.LineShape;
import de.mrjulsen.paw.util.collision.RaycastHitResult;
import de.mrjulsen.paw.util.collision.RaycastUtils;
import de.mrjulsen.wires.WireNetwork;
import de.mrjulsen.wires.WireCollision.WireBlockCollision;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CutterItem extends Item {

    public CutterItem(Properties properties) {
        super(properties);
    }

    /*
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {

        if (level.isClientSide)
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));

        Optional<RaycastHitResult> result = RaycastUtils.rayTrace(
            player.getEyePosition().toVector3f(),
            player.getEyePosition().toVector3f().add(player.getLookAngle().toVector3f().normalize().mul(10)),
            level,
            DragonLib.PIXEL * 2,
            DragonLib.PIXEL * 2,
            (lvl, pos, rayOrigin, rayDirection) -> {   
                return WireNetwork.get(level).getCollisionsTroughBlock(pos)
                    .stream()
                    .flatMap(x -> {
                        Stream<WireBlockCollision> v = x.collisionsInBlock(pos).stream();
                        return v;
                    })
                    .map(x -> {
                        Vector3f a = new Vector3f(x.entryPointA()).add(pos.getX(), pos.getY(), pos.getZ());
                        Vector3f b = new Vector3f(x.entryPointB()).add(pos.getX(), pos.getY(), pos.getZ());
                        LineShape wire = new LineShape(a, b,  DragonLib.PIXEL);
                        return wire.intersects(rayOrigin, rayDirection)
                                .map(hit -> new RaycastHitResult(hit, pos, new Vector3f(hit).sub(rayOrigin).length(), x));
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .min((a, b) -> {
                        return Float.compare(a.distance(), b.distance());
                    });
            }
        );

        result.ifPresent(x -> {
            if (x.o() instanceof WireBlockCollision wire) {
                WireNetwork.get(level).removeConnection(level, wire.getId());
            }
        });

        return super.use(level, player, usedHand);
    }
        */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {

        if (level.isClientSide)
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));

        Optional<RaycastHitResult> result = RaycastUtils.rayTrace(
            player.getEyePosition().toVector3f(),
            player.getEyePosition().toVector3f().add(player.getLookAngle().toVector3f().normalize().mul(10)),
            level,
            7,
            DragonLib.PIXEL * 2,
            (lvl, pos, rayOrigin, rayDirection) -> {   
                
                if (!(lvl.getBlockEntity(pos) instanceof CantileverBlockEntity be)) 
                    return Optional.empty();

                CantileverData cantilever = be.getCantileverData()[0];
                
                LineShape[] shapes = new LineShape[] {
                    new LineShape(new Vector3f(0, cantilever.y(), cantilever.z()).add(pos.getX(), pos.getY(), pos.getZ()), new Vector3f(cantilever.width(), cantilever.y() + cantilever.frontYOffset(), cantilever.z()).add(pos.getX(), pos.getY(), pos.getZ()), DragonLib.PIXEL * 2),
                    new LineShape(new Vector3f(0, -cantilever.height(), cantilever.z()).add(pos.getX(), pos.getY(), pos.getZ()), new Vector3f(cantilever.width(), cantilever.y() + cantilever.frontYOffset(), cantilever.z()).add(pos.getX(), pos.getY(), pos.getZ()), DragonLib.PIXEL * 2)
                };

                for (LineShape shape : shapes) {
                    Optional<Vector3f> oHit = shape.intersects(rayOrigin, rayDirection);
                    if (oHit.isPresent()) {
                        Vector3f hit = oHit.get();
                        return Optional.of(new RaycastHitResult(hit, pos, new Vector3f(hit).sub(rayOrigin).length(), cantilever));
                    }
                }
                return Optional.empty();
            }
        );

        result.ifPresent(x -> {
            System.out.println("HALLO " + System.nanoTime());
        });

        return super.use(level, player, usedHand);
    }
    
}
