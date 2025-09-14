package de.mrjulsen.paw.registry;

import java.util.Optional;
import java.util.function.Supplier;

import org.joml.Vector3f;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.AbstractModel;
import de.mrjulsen.mcdragonlib.client.model.mesh.BasicMesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import de.mrjulsen.wires.decoration.WireDecorationElement;
import de.mrjulsen.wires.decoration.WireDecorationRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class InsulatorWireDecoration extends WireDecorationElement<InsulatorWireDecoration> {

    private final Renderer renderer;

    private ItemStack stack;

    public InsulatorWireDecoration(ResourceLocation id) {
        super(id);
        this.renderer = new Renderer(this);
    }

    @Override
    public WireDecorationRenderer<InsulatorWireDecoration> getRenderer() {
        return renderer;
    }   

    @Override
    public void onBreak(Level level, Vector3f position, Optional<Player> player) {
        if (!player.isPresent() || (!player.get().isCreative() && !player.get().isSpectator())) {
            ItemEntity itementity = new ItemEntity(level, position.x(), position.y(), position.z(), stack);
            itementity.setDefaultPickUpDelay();
            level.addFreshEntity(itementity);
        }
    }

    public void setItem(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void writeNbt(CompoundTag nbt) {
        stack.save(nbt);
    }

    @Override
    public void readNbt(CompoundTag nbt) {
        this.stack = ItemStack.of(nbt);
    }

    @Override
    public float getRadius() {
        return 0.5f;
    }

    private static class Renderer extends WireDecorationRenderer<InsulatorWireDecoration> {

        private final Supplier<AbstractModel> model = Suppliers.memoize(() -> new AbstractModel() {
			@Override
			protected Mesh getMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context) {
				Mesh mesh = BasicMesh.fromBlock(((BlockItem)decoration.stack.getItem()).getBlock().defaultBlockState(), RandomSource.create());
                mesh.getFaces().forEach(x -> x.setRenderType(RenderType.cutout()));
				mesh.centerTo(new Vector3f(0));
				mesh.rotate(Axis.XP.rotationDegrees(90), new Vector3f(0));

				return mesh;
			}
		});

        public Renderer(InsulatorWireDecoration decoratin) {
            super(decoratin);
        }

        @Override
        public void render(PoseStack poseStack, VertexConsumer consumer, Vector3f pos, Vector3f direction) {
            if (!(decoration.stack.getItem() instanceof BlockItem)) {
                return;
            }
            model.get().render(poseStack.last(), consumer, ((BlockItem)decoration.stack.getItem()).getBlock().defaultBlockState(), ModelContext.EMPTY);
        }
        
    }
    
}
