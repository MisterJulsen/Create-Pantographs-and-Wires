package de.mrjulsen.paw.item;

import java.util.Optional;
import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.block.CantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity.CantileverShapeData;
import de.mrjulsen.paw.util.collision.LineShape;
import de.mrjulsen.paw.util.collision.RaycastHitResult;
import de.mrjulsen.paw.util.collision.RaycastUtils;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.item.WireBaseItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class CatenaryWireItem extends WireBaseItem {

    public static final String NBT_CANTILEVER_INDEX = "CantileverIndex";

    public CatenaryWireItem(Properties properties, IWireType wireType) {
        super(properties, wireType);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        
        if (level.isClientSide)
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));

        Optional<RaycastHitResult> result = RaycastUtils.rayTrace(
            player.getEyePosition().toVector3f(),
            player.getEyePosition().toVector3f().add(player.getLookAngle().toVector3f().normalize().mul(10)),
            level,
            AbstractCantileverBlock.MAX_WIDTH,
            DragonLib.PIXEL * 2,
            (lvl, pos, rayOrigin, rayDirection) -> {
                
                if (!(lvl.getBlockState(pos).getBlock() instanceof CantileverBlock && lvl.getBlockEntity(pos) instanceof CantileverBlockEntity be)) 
                    return Optional.empty();

                for (int i = 0; i < be.getCantileversCount(); i++) {
                    final int k = i;                    
                    CantileverShapeData shapeData = be.getCantileverInteractionShape(k);
                    LineShape[] shapes = new LineShape[] {
                        new LineShape(shapeData.stayTubeRoot(), shapeData.front(), DragonLib.PIXEL * 2),
                        new LineShape(shapeData.bracketTubeRoot(), shapeData.front(), DragonLib.PIXEL * 2)
                    };

                    for (LineShape shape : shapes) {
                        Optional<Vector3f> oHit = shape.intersects(rayOrigin, rayDirection);
                        if (oHit.isPresent()) {
                            Vector3f hit = oHit.get();
                            return Optional.of(new RaycastHitResult(hit, pos, new Vector3f(hit).sub(rayOrigin).length(), k));
                        }
                    }
                }
                
                return Optional.empty();
            }
        );

        result.ifPresent(x -> {
            placeWire(level, x.blockPos(), level.getBlockState(x.blockPos()), player, player.getItemInHand(usedHand), Optional.empty(), (metaNbt) -> {
                metaNbt.putInt(NBT_CANTILEVER_INDEX, (Integer)x.metadata());
            });
        });

        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }
}
