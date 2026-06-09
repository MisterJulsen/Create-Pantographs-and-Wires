package de.mrjulsen.paw.mixin;

import de.mrjulsen.paw.block.abstractions.weathering.IAgingBlock;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
<<<<<<< HEAD
=======
import net.minecraft.world.entity.LivingEntity;
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AxeItem.class)
public class AxeItemMixin {

    @Inject(method = "useOn", at = @At("RETURN"), cancellable = true)
    private void onUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() != InteractionResult.PASS) return;

        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        BlockState blockState = level.getBlockState(blockPos);

        if (!(blockState.getBlock() instanceof IAgingBlock<?, ?> agingBlock)) return;

        Optional<BlockState> result;

        if (agingBlock.isWaxed()) {
            result = agingBlock.getWaxToggled(blockState);
            result.ifPresent($ -> {
                level.playSound(context.getPlayer(), blockPos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.levelEvent(context.getPlayer(), LevelEvent.PARTICLES_WAX_OFF, blockPos, 0);
            });
        } else {
            result = agingBlock.getPrevious(blockState);
            result.ifPresent($ -> {
                level.playSound(context.getPlayer(), blockPos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.levelEvent(context.getPlayer(), LevelEvent.PARTICLES_SCRAPE, blockPos, 0);
            });
        }

        if (result.isEmpty()) return;

        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        BlockState newState = result.get();

        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, blockPos, itemStack);
        }

        level.setBlock(blockPos, newState, 11);
        level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(player, newState));

        if (player != null) {
<<<<<<< HEAD
            itemStack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));
=======
            itemStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
        }

        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }
}