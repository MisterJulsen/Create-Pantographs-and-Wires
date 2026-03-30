package de.mrjulsen.paw.item;

import java.util.Optional;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.registry.ModBlockTags;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.graph.data.node.MastNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.registry.DLStaticRegistryObject;
import de.mrjulsen.wires.item.IPawWireItemBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SupportWireItem implements IPawWireItemBase {

    @Override
    public IWireType getWireType(ItemStack stack) {
        return ModWireRegistry.SUPPORT_WIRE;
    }

    @Override
    public DLStaticRegistryObject<IPawWireItemBase> getRegistryType() {
        return (DLStaticRegistryObject<IPawWireItemBase>)(Object)ModWireRegistry.SUPPORT_WIRE_ITEM_SUBTYPE;
    }

    @Override
    public String getTranslationKey() {
        return "wire." + PantographsAndWires.MOD_ID + ".support_wire";
    }

    @Override
    public ModGuiIcons getIcon() {
        return ModGuiIcons.DECORATION_WIRE;
    }

    @Override
    public NodeData createNodeData(Level level, Player player, InteractionHand hand, HitResult hit) {        
        if (hit instanceof BlockHitResult blockHit && level.getBlockState(blockHit.getBlockPos()).getTags().anyMatch(x -> x.equals(ModBlockTags.SUPPORT_WIRE_CONNECTABLE))) {
            //BlockPos pos = blockHit.getBlockPos();
            //BlockState state = level.getBlockState(blockHit.getBlockPos());
            //VoxelShape shape = state.getVisualShape(level, blockHit.getBlockPos(), CollisionContext.empty());
            //return clipFromSide(shape, pos, blockHit.getDirection()).map(x -> {
            //    return new GenericBlockNodeData(x.getBlockPos(), x.getLocation().toVector3f().sub(x.getBlockPos().getX(), x.getBlockPos().getY(), x.getBlockPos().getZ()));
            //}).orElse(null);
            return new MastNodeData(blockHit.getBlockPos());
        }
        return null;
    }

    public static Optional<BlockHitResult> clipFromSide(VoxelShape shape, BlockPos pos, Direction dir) {
        if (shape.isEmpty()) {
            return Optional.empty();
        }

        AABB bounds = shape.bounds();
        double minX = 0.5;
        double minY = 0.5;
        double minZ = 0.5;
        double maxX = 0.5;
        double maxY = 0.5;
        double maxZ = 0.5;

        switch (dir) {
            case DOWN  -> {
                minY = bounds.maxY;
                maxY = bounds.minY;
            }
            case UP    -> {
                minY = bounds.minY;
                maxY = bounds.maxY;
            }
            case NORTH -> {
                minZ = bounds.maxZ;
                maxZ = bounds.minZ;
            }
            case SOUTH -> {
                minZ = bounds.minZ;
                maxZ = bounds.maxZ;
            }
            case WEST  -> {
                minX = bounds.maxX;
                maxX = bounds.minX;
            }
            case EAST  -> {
                minX = bounds.minX;
                maxX = bounds.maxX;
            }
        }

        Vec3 block = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        Vec3 start = new Vec3(
                maxX + dir.getStepX() * 0.001,
                maxY + dir.getStepY() * 0.001,
                maxZ + dir.getStepZ() * 0.001
        ).add(block);
        Vec3 end = new Vec3(
                minX - dir.getStepX() * 0.001,
                minY - dir.getStepY() * 0.001,
                minZ - dir.getStepZ() * 0.001
        ).add(block);
        return Optional.ofNullable(shape.clip(start, end, pos));
    }
}
