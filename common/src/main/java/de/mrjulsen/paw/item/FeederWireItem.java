package de.mrjulsen.paw.item;

import de.mrjulsen.mcdragonlib.util.MathUtils;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.util.ModMath;
import de.mrjulsen.paw.util.collision.RaycastHitResult;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.data.node.CatenaryWireConnectorNodeData;
import de.mrjulsen.wires.item.WireBaseItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class FeederWireItem extends WireBaseItem {

    public FeederWireItem(Properties properties, IWireType wireType) {
        super(properties, wireType);
    }


    @Override
    public InteractionResult interactWithWire(Level level, Player player, InteractionHand hand, WireHitResult hit) {
        return placeWire(level, player, hand, hit, EWireConnectorType.WIRE, (a, b) -> {});
    }
    

    @Override
    protected NodeData createNodeData(Level level, Player player, InteractionHand hand, HitResult hit, EWireConnectorType type) {
        if (hit instanceof WireHitResult h) {
            float posOnWire = (float)ModMath.snap(h.getPosOnWire(), 0.5f);
            float p = h.getCollision(level).map(x -> MathUtils.clamp(1F / x.length(h.getWireId().name()) * posOnWire, 0F, 1F)).orElse(0F);
            return new CatenaryWireConnectorNodeData(h.getWireId(), p);
        }

        BlockPos pos = null;
        if (hit instanceof BlockHitResult h) {
            pos = h.getBlockPos();
        } else if (hit instanceof RaycastHitResult h) {
            pos = h.getBlockPos();
        }
        if (level.getBlockEntity(pos) instanceof WireConnectorBlockEntity) {
            return new BlockConnectorNodeData(pos);
        }
        return null;
    }
}
