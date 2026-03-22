package de.mrjulsen.paw.item;

import java.util.Optional;

import org.joml.Vector3d;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.item.IPawWireItemBase;
import de.mrjulsen.wires.util.GraphId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class PAWWireType extends AbstractWireType {    

    public PAWWireType(ResourceLocation location) {
        super(location);
    }
    
    @Override
    public void onBreak(Level level, Vector3d breakPosition, Optional<Player> player, IWireGraph graph, WireEdge edge) {
		int length = (int)((double)edge.length() * getWireConsumptionMultiplier(edge.length()));
		boolean enableDrops = !player.isPresent() || (!player.get().isCreative() && !player.get().isSpectator());

		if (player.map(p -> {
			if (!p.isCreative() && !player.get().isSpectator()) {
				return !IPawWireItemBase.creditWireToInventory(p, length);
			}
			return enableDrops;
		}).orElse(enableDrops)) {
			float scaleFactor = (float)IPawWireItemBase.WIRE_LENGTH / 8;
			int x = (int)Math.floor(length / scaleFactor);
			float chance = 1.0f;
			if (x < 1) {
				x = 1;
				chance = Math.min((float)length / scaleFactor, 1.0f);
			}
			float rand = DragonLib.RANDOM.nextFloat();
			if (rand < chance) {				
				ItemEntity itementity = new ItemEntity(level, breakPosition.x(), breakPosition.y(), breakPosition.z(), ModItems.COPPER_WIRE.asStack(x));
				itementity.setDefaultPickUpDelay();
				level.addFreshEntity(itementity);
			}
		}
    }	

	@Override
	public GraphId getGraphId(CompoundTag itemData) {
		return WiresApi.PAW_CATENARY_WIRES;
	}

    public abstract double getWireConsumptionMultiplier(int connectionLength);
}
