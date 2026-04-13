package de.mrjulsen.paw.item;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverInsulatorsPlacement;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity.SubCantileverSetting;
import de.mrjulsen.paw.event.ClientWrapper;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CantileverBlockItem<T extends AbstractCantileverBlock> extends BlockItem {

    private static final String KEY_TOO_SMALL_FOR_ADDITIONAL_CANTILEVERS = "block." + PantographsAndWires.MOD_ID + ".cantilever.too_small_for_multiple_cantilevers";

    private final EInsulatorType insulatorType;

    public CantileverBlockItem(T block, EInsulatorType insulatorType, Item.Properties properties) {
		super(block, properties);
        this.insulatorType = insulatorType;
	}

    @SuppressWarnings("unchecked")
    public AbstractCantileverBlock getCantilever() {
        return (T)getBlock();
    }

    public EInsulatorType getInsulatorType() {
        return insulatorType;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CantileverBlockEntity be) {
            if (be.shouldNotUpdate()) {
                be.setDoNotUpdate(false);
                return false;
            }
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();

        if (level.getBlockEntity(context.getClickedPos()) instanceof CantileverBlockEntity be) {
            byte allowedCount = AbstractCantileverBlock.additionalCantileversCheck(be.getWidth(), be.getHeight(), be.getCatenaryHeight());
            int currentCount = be.getSubCantileverSettings().size();
            if (currentCount < allowedCount) {
                be.getSubCantileverSettings().add(getSubSettings(context.getItemInHand()));
                be.update();
                be.setDoNotUpdate(true);
                return true;
            } else if (allowedCount < AbstractCantileverBlock.MAX_CANTILEVERS) {
                context.getPlayer().displayClientMessage(TextUtils.translate(KEY_TOO_SMALL_FOR_ADDITIONAL_CANTILEVERS).withStyle(ChatFormatting.RED), true);
            }
        }
        return super.placeBlock(context, state);
    }

    public static CompoundTag getNbt(ItemStack stack) {        
        CompoundTag itemNbt = stack.getOrCreateTag();
        CompoundTag nbt;
        if (itemNbt.contains(BLOCK_ENTITY_TAG)) {
            nbt = itemNbt.getCompound(BLOCK_ENTITY_TAG);
        } else {
            nbt = new CompoundTag();
            itemNbt.put(BLOCK_ENTITY_TAG, nbt);
        }

        // Validate and init values
        if (!nbt.contains(CantileverBlockEntity.NBT_WIDTH)) {
            nbt.putFloat(CantileverBlockEntity.NBT_WIDTH, CantileverBlockEntity.DEFAULT_WIDTH);
        } else {            
            nbt.putFloat(CantileverBlockEntity.NBT_WIDTH, MathUtils.clamp(nbt.getFloat(CantileverBlockEntity.NBT_WIDTH), AbstractCantileverBlock.MIN_WIDTH, AbstractCantileverBlock.MAX_WIDTH));
        }

        if (!nbt.contains(CantileverBlockEntity.NBT_HEIGHT)) {
            nbt.putFloat(CantileverBlockEntity.NBT_HEIGHT, CantileverBlockEntity.DEFAULT_HEIGHT);
        } else {            
            nbt.putFloat(CantileverBlockEntity.NBT_HEIGHT, MathUtils.clamp(nbt.getFloat(CantileverBlockEntity.NBT_HEIGHT), AbstractCantileverBlock.MIN_HEIGHT, AbstractCantileverBlock.MAX_HEIGHT));
        }

        if (!nbt.contains(CantileverBlockEntity.NBT_CATENARY_HEIGHT)) {
            nbt.putFloat(CantileverBlockEntity.NBT_CATENARY_HEIGHT, CantileverBlockEntity.DEFAULT_CATENARY_HEIGHT);
        } else {            
            nbt.putFloat(CantileverBlockEntity.NBT_CATENARY_HEIGHT, MathUtils.clamp(nbt.getFloat(CantileverBlockEntity.NBT_CATENARY_HEIGHT), AbstractCantileverBlock.MIN_HEIGHT, AbstractCantileverBlock.MAX_HEIGHT));
        }

        /*
        if (!nbt.contains(CantileverBlockEntity.NBT_REGISTRATION_ARM_TYPE)) {
            nbt.putInt(CantileverBlockEntity.NBT_REGISTRATION_ARM_TYPE, CantileverBlockEntity.DEFAULT_REGISTRATION_ARM_TYPE.ordinal());
        } else {
            nbt.putInt(CantileverBlockEntity.NBT_REGISTRATION_ARM_TYPE, MathUtils.clamp(nbt.getInt(CantileverBlockEntity.NBT_REGISTRATION_ARM_TYPE), 0, ECantileverRegistrationArmType.values().length - 1));
        }

        if (!nbt.contains(CantileverBlockEntity.NBT_INSULATOR_PLACEMENT)) {
            nbt.putInt(CantileverBlockEntity.NBT_INSULATOR_PLACEMENT, CantileverBlockEntity.DEFAULT_INSULATOR_PLACEMENT.ordinal());
        } else {
            nbt.putInt(CantileverBlockEntity.NBT_INSULATOR_PLACEMENT, MathUtils.clamp(nbt.getInt(CantileverBlockEntity.NBT_INSULATOR_PLACEMENT), 0, ECantileverInsulatorsPlacement.values().length - 1));
        }

         */

        return nbt;
    }

    public static boolean setNbt(ItemStack stack, de.mrjulsen.paw.data.CantileverSettingsData data) {
        if (stack.getItem() instanceof CantileverBlockItem) {
            CompoundTag nbt = getNbt(stack);
            nbt.putFloat(CantileverBlockEntity.NBT_WIDTH, data.width());
            nbt.putFloat(CantileverBlockEntity.NBT_WIDTH, data.width());
            nbt.putFloat(CantileverBlockEntity.NBT_Y_OFFSET, data.yOffset());
            nbt.putFloat(CantileverBlockEntity.NBT_HEIGHT, data.height());
            nbt.putFloat(CantileverBlockEntity.NBT_CATENARY_HEIGHT, data.catenaryHeight());

            ListTag list = new ListTag();
            list.add(new SubCantileverSetting(
                    data.cantileverType(),
                    data.insulatorPlacement(),
                    data.showBracing()
            ).toNbt());
            nbt.put(CantileverBlockEntity.NBT_SUB_CANTILEVER_SETTINGS, list);
            return true;
        } 
        return false;
    }

    private static SubCantileverSetting getSubSettings(ItemStack stack) {
        CompoundTag nbt = getNbt(stack);
        if (nbt.contains(CantileverBlockEntity.NBT_SUB_CANTILEVER_SETTINGS) && nbt.getTagType(CantileverBlockEntity.NBT_SUB_CANTILEVER_SETTINGS) == Tag.TAG_LIST) {
            return nbt.getList(CantileverBlockEntity.NBT_SUB_CANTILEVER_SETTINGS, Tag.TAG_COMPOUND)
                    .stream()
                    .map(c -> SubCantileverSetting.fromNbt((CompoundTag)c))
                    .findFirst()
                    .orElse(SubCantileverSetting.EMPTY);
        }
        return SubCantileverSetting.EMPTY;
    }

    public static float getWidth(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof CantileverBlockItem) {
            return getNbt(stack).getFloat(CantileverBlockEntity.NBT_WIDTH);
        }
        return CantileverBlockEntity.DEFAULT_WIDTH;
    }

    public static float getHeight(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof CantileverBlockItem) {
            return getNbt(stack).getFloat(CantileverBlockEntity.NBT_HEIGHT);
        }
        return CantileverBlockEntity.DEFAULT_HEIGHT;
    }

    public static float getYOffset(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof CantileverBlockItem) {
            return getNbt(stack).getFloat(CantileverBlockEntity.NBT_Y_OFFSET);
        }
        return CantileverBlockEntity.DEFAULT_Y_OFFSET;
    }

    public static boolean getShowBracing(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof CantileverBlockItem) {
            return getSubSettings(stack).showBracing();
        }
        return CantileverBlockEntity.DEFAULT_SHOW_BRACING;
    }

    public static float getCatenaryHeight(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof CantileverBlockItem) {
            return getNbt(stack).getFloat(CantileverBlockEntity.NBT_CATENARY_HEIGHT);
        }
        return CantileverBlockEntity.DEFAULT_CATENARY_HEIGHT;
    }

    public static ECantileverRegistrationArmType getRegistrationArm(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof CantileverBlockItem) {
            return getSubSettings(stack).registrationArm();
        }
        return CantileverBlockEntity.DEFAULT_REGISTRATION_ARM_TYPE;
    }

    public static ECantileverInsulatorsPlacement getInsulatorPlacement(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof CantileverBlockItem) {
            return getSubSettings(stack).insulatorPlacement();
        }
        return CantileverBlockEntity.DEFAULT_INSULATOR_PLACEMENT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            ClientWrapper.showCantileverSettingsScreen(stack);
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
    }
}
