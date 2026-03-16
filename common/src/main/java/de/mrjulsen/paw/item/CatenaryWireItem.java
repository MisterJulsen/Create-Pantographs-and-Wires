package de.mrjulsen.paw.item;

import java.util.Optional;
import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.CantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity.CantileverShapeData;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.paw.util.collision.LineShape;
import de.mrjulsen.paw.util.collision.RaycastHitResult;
import de.mrjulsen.paw.util.collision.RaycastUtils;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.registry.DLStaticRegistryObject;
import de.mrjulsen.wires.graph.data.node.CatenaryHeadspanConnectionNodeData;
import de.mrjulsen.wires.item.IPawWireItemBase;
import de.mrjulsen.wires.network.WireId;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CatenaryWireItem implements IPawWireItemBase {

    public static final String NBT_CANTILEVER_INDEX = "CantileverIndex";

    private final Component txtNoRegistrationArm = TextUtils.translate("item." + PantographsAndWires.MOD_ID + ".catenary_headspan.dropper_missing_registration_arm").withStyle(ChatFormatting.RED);

    @Override
    public IWireType getWireType(ItemStack stack) {
        return ModWireRegistry.CATENARY_WIRE;
    }

    @Override
    public DLStaticRegistryObject<IPawWireItemBase> getRegistryType() {
        return (DLStaticRegistryObject<IPawWireItemBase>)(Object)ModWireRegistry.CATENARY_WIRE_ITEM_SUBTYPE;
    }

    @Override
    public String getTranslationKey() {
        return "wire." + PantographsAndWires.MOD_ID + ".catenary_wire";
    }

    @Override
    public ModGuiIcons getIcon() {
        return ModGuiIcons.CATENARY_WIRE;
    }

    @Override
    public InteractionResult useWireOn(UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            InteractionResult result = useWire(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
            if (result == InteractionResult.FAIL) {
                return IPawWireItemBase.super.useWireOn(context);
            }
        }
        return InteractionResult.CONSUME;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> useWire(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide) {
            return InteractionResultHolder.consume(player.getItemInHand(usedHand));
        }

        Optional<RaycastHitResult> result = RaycastUtils.rayTrace(
            player.getEyePosition().toVector3f(),
            player.getEyePosition().toVector3f().add(player.getLookAngle().toVector3f().normalize().mul(5)),
            level,
            AbstractCantileverBlock.MAX_WIDTH,
                (lvl, pos, rayOrigin, rayDirection) -> {
                    if (!(lvl.getBlockState(pos).getBlock() instanceof CantileverBlock && lvl.getBlockEntity(pos) instanceof CantileverBlockEntity be))
                        return Optional.empty();

                    RaycastHitResult closest = null;

                    for (int i = 0; i < be.getCantileversCount(); i++) {
                        CantileverShapeData shapeData = be.getCantileverInteractionShape(i);
                        LineShape[] shapes = new LineShape[] {
                                new LineShape(shapeData.stayTubeRoot(), shapeData.front(), DragonLib.BLOCK_PIXEL * 2),
                                new LineShape(shapeData.bracketTubeRoot(), shapeData.front(), DragonLib.BLOCK_PIXEL * 2)
                        };

                        for (LineShape shape : shapes) {
                            Optional<Vector3f> oHit = shape.intersects(rayOrigin, rayDirection);
                            if (oHit.isPresent()) {
                                Vector3f hit = oHit.get();
                                float dist = new Vector3f(hit).sub(rayOrigin).length();
                                if (closest == null || dist < closest.getDistance()) {
                                    closest = new RaycastHitResult(new Vec3(hit), pos, dist, i);
                                }
                            }
                        }
                    }

                    return Optional.ofNullable(closest);
                }
        );

        result.ifPresent(x -> {
            placeWire(level, player, usedHand, x, (metaNbt, pointMeta) -> {
                pointMeta.putInt(NBT_CANTILEVER_INDEX, (Integer)x.getHitData());
            });
        });

        return result.isPresent() ? InteractionResultHolder.success(player.getItemInHand(usedHand)) : InteractionResultHolder.fail(player.getItemInHand(usedHand));
    }

    @Override
    public InteractionResult interactWithWire(Level level, Player player, InteractionHand hand, WireHitResult hit) {
        return placeWire(level, player, hand, hit, (a, b) -> {});
    }
    

    @Override
    public NodeData createNodeData(Level level, Player player, InteractionHand hand, HitResult hit) {
        if (hit instanceof WireHitResult h) {
            Pair<Boolean, WireId> result = CatenaryHeadspanWireType.canConnectCatenary(WireGraphManager.get(level, h.getGraphId()).getEdge(h.getWireId().id()), h.getWireId());
            if (result.getFirst()) {
                return new CatenaryHeadspanConnectionNodeData(result.getSecond());
            } else {
                player.displayClientMessage(txtNoRegistrationArm, true);
                return null;
            }
        }

        BlockPos pos = null;
        if (hit instanceof BlockHitResult h) {
            pos = h.getBlockPos();
        } else if (hit instanceof RaycastHitResult h) {
            pos = h.getBlockPos();
        }
        if (pos != null && level.getBlockEntity(pos) instanceof WireConnectorBlockEntity && level.getBlockState(pos).getBlock() instanceof ICatenaryWireConnector) {
            return new BlockConnectorNodeData(pos);
        }
        return null;
    }
}
