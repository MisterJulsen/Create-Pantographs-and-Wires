package de.mrjulsen.paw.registry;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.Tag;
import org.joml.Vector3f;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.BasicMesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.DLModel;
import de.mrjulsen.mcdragonlib.client.model.mesh.DLModel.ModelType;
import de.mrjulsen.mcdragonlib.util.DLColor;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.wires.decoration.IWireDecoration;
import de.mrjulsen.wires.decoration.WireDecorationRenderer;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class InsulatorWireDecoration implements IWireDecoration<InsulatorWireDecoration> {

    public static final float RADIUS = 0.35f;
    private static final String NBT_ITEM = "Item";

    private final Renderer renderer;

    private ItemStack stack;

    public InsulatorWireDecoration() {
        this.renderer = new Renderer(this);
    }
    
    public InsulatorWireDecoration(ItemStack stack) {
        this();
        this.stack = Objects.requireNonNull(stack);
    }

    @Override
    public DLRegistryObject<IWireDecoration<?>> getRegistryType() {
        return (DLRegistryObject<IWireDecoration<?>>)(Object)ModWireRegistry.INSULATOR_DECORATION;
    }

    @Override
    public WireDecorationRenderer<InsulatorWireDecoration> getRenderer() {
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
    
    public ItemStack getItem() {
        return stack;
    }



    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        if (stack != null && stack != ItemStack.EMPTY) {
            Tag tag = stack.save(RegistryAccess.EMPTY);
            nbt.put(NBT_ITEM, tag);
        }
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        this.stack = ItemStack.parseOptional(RegistryAccess.EMPTY, nbt.getCompound(NBT_ITEM));
    }
    
    @Override
    public float getRadius(IWireDecoration<?> element) {
        return RADIUS;
    }

    private static class Renderer extends WireDecorationRenderer<InsulatorWireDecoration> {

        private final Supplier<DLModel> model = Suppliers.memoize(() -> new DLModel() {
			@Override
			protected Mesh getMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context) {
				Mesh mesh = BasicMesh.fromBlock(((BlockItem)decoration.stack.getItem()).getBlock().defaultBlockState(), RandomSource.create());
                mesh.getFaces().forEach(x -> {
                    x.setRenderType(RenderType.cutout());
                });
				mesh.centerTo(new Vector3f(0));
				mesh.rotate(Axis.XP.rotationDegrees(90), new Vector3f(0));
				return mesh;
			}
		});


        public Renderer(InsulatorWireDecoration decoratin) {
            super(decoratin);
        }

        @Override
        public void render(PoseStack poseStack, VertexConsumer consumer, Vector3f pos, Vector3f directio, int light) {
            if (!(decoration.stack.getItem() instanceof BlockItem blockitem)) {
                return;
            }
            model.get().render(poseStack.last(), consumer, ModelType.BLOCK, blockitem.getBlock().defaultBlockState(), ModelContext.EMPTY, DLColor.WHITE, light, OverlayTexture.NO_OVERLAY);
        }
        
    }
    
}
