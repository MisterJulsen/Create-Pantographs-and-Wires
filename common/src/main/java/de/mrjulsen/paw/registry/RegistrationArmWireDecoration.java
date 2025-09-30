package de.mrjulsen.paw.registry;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.joml.Vector3f;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.BasicMesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.DLModel;
import de.mrjulsen.mcdragonlib.client.model.mesh.DLModel.ModelType;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.data.DataCache;
import de.mrjulsen.paw.block.RegistrationArmBlock;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.wires.decoration.IWireDecoration;
import de.mrjulsen.wires.decoration.WireDecorationRenderer;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RegistrationArmWireDecoration implements IWireDecoration<RegistrationArmWireDecoration> {

    public static final float RADIUS = 0.5f;
    public static final String NBT_ITEM = "Item";
    public static final String NBT_DROPPER_ID = "DropperId";
    public static final String NBT_MIRRORED = "Mirrored";
    public static final String NBT_VARIANT = "Variant";

    private final Renderer renderer;

    private ItemStack stack;
    private boolean mirrored;
    private RegistrationArmBlock.State variant;
    private UUID dropperId;

    public RegistrationArmWireDecoration() {
        this.renderer = new Renderer(this);
    }
    
    public RegistrationArmWireDecoration(ItemStack stack, boolean mirrored, RegistrationArmBlock.State variant, UUID dropperId) {
        this();
        this.stack = stack;
        this.mirrored = mirrored;
        this.variant = variant;
        this.dropperId = dropperId;
    }

    @Override
    public DLRegistryObject<IWireDecoration<?>> getRegistryType() {        
        return (DLRegistryObject<IWireDecoration<?>>)(Object)ModWireRegistry.CATENARY_HEADSPAN_REGISTRATION_ARM;
    }

    @Override
    public WireDecorationRenderer<RegistrationArmWireDecoration> getRenderer() {
        return renderer;
    }   

    @Override
    public void onBreak(Level level, Vector3f position, Optional<Player> player) {
        if (!player.isPresent() || (!player.get().isCreative() && !player.get().isSpectator()) || ModServerConfig.DROP_WIRE_ITEMS_IN_CREATIVE.get()) {
            ItemEntity itementity = new ItemEntity(level, position.x(), position.y(), position.z(), stack);
            itementity.setDefaultPickUpDelay();
            level.addFreshEntity(itementity);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand, WireHitResult hit) {
        return InteractionResult.PASS;
    }
    
    public boolean isMirrored() {
        return mirrored;
    }
    
    public RegistrationArmBlock.State getVariant() {
        return variant;
    }

    public ItemStack getItem() {
        return stack;
    }

    public UUID getDropperId() {
        return dropperId;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_ITEM, stack.save(new CompoundTag()));
        nbt.putBoolean(NBT_MIRRORED, mirrored);
        nbt.putByte(NBT_VARIANT, variant == null ? 0 : variant.getId());
        nbt.putUUID(NBT_DROPPER_ID, dropperId);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        this.stack = ItemStack.of(nbt.getCompound(NBT_ITEM));
        this.mirrored = nbt.getBoolean(NBT_MIRRORED);
        this.variant = RegistrationArmBlock.State.getById(nbt.getByte(NBT_VARIANT));
        this.dropperId = nbt.getUUID(NBT_DROPPER_ID);
    }

    @Override
    public float getRadius() {
        return RADIUS;
    }



    private static class Renderer extends WireDecorationRenderer<RegistrationArmWireDecoration> {

        private final Supplier<DLModel> model = Suppliers.memoize(() -> new DLModel() {
			@Override
			protected Mesh getMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context) {
				Mesh mesh = BasicMesh.fromBlock(ModBlocks.REGISTRATION_ARM.getDefaultState()
                    .setValue(RegistrationArmBlock.MIRRORED, decoration.mirrored)
                    .setValue(RegistrationArmBlock.REGISTRATION_ARM, decoration.variant)
                , random);
                mesh.getFaces().forEach(x -> {
                    x.setRenderType(RenderType.cutout());
                });
				mesh.translate(new Vector3f(-0.5f));
				mesh.rotate(Axis.YP.rotationDegrees(90), new Vector3f(0));
				return mesh;
			}
		});

        public Renderer(RegistrationArmWireDecoration decoratin) {
            super(decoratin);
        }

        @Override
        public void render(PoseStack poseStack, VertexConsumer consumer, Vector3f pos, Vector3f direction, int light) {
            model.get().render(poseStack.last(), consumer, ModelType.BLOCK, ModBlocks.REGISTRATION_ARM.getDefaultState(), ModelContext.EMPTY, 1, 1, 1, light, OverlayTexture.NO_OVERLAY);
        }
        
    }
}
