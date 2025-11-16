package de.mrjulsen.paw.blockentity;

import org.apache.commons.lang3.tuple.MutablePair;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import de.mrjulsen.paw.CrossPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class PantographInteractionBehaviour extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        Contraption contraption = contraptionEntity.getContraption();        
        MutablePair<StructureBlockInfo, MovementContext> actor = contraptionEntity.getContraption().getActorAt(localPos);
        if (actor == null || actor.right == null)
            return false;
    
        MovementContext ctx = actor.right;
        boolean state = !ctx.blockEntityData.getBoolean(PantographBlockEntity.NBT_EXPANDABLE);
        ctx.blockEntityData.putBoolean(PantographBlockEntity.NBT_EXPANDABLE, state);

        if (CrossPlatform.getClientContraptionBlockEntity(contraption, localPos) instanceof PantographBlockEntity be) {
		    be.setExpandable(state);
        }

        return true;
    }
}
