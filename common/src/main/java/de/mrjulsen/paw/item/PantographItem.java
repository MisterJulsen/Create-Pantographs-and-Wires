package de.mrjulsen.paw.item;

import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class PantographItem extends BlockItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private static final RawAnimation ANIM_WIRE_CONTACT = RawAnimation.begin().thenPlayAndHold("wire_contact");

	private final boolean expanded;

	protected PantographItem(Block block, Properties properties, boolean expanded) {
		super(block, properties
			.stacksTo(1)
		);
		this.expanded = expanded;
	}

	@ExpectPlatform
	public static PantographItem create(Block block, Properties properties, boolean expaned) {
		throw new AssertionError();
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<GeoAnimatable>(this, "popup_controller", 0, state -> {
			MathParser.setVariable("query.height_percentage", () -> {
				return expanded ? 1D / PantographBlockEntity.DELTA_HEIGHT * 2 : 0;
			});
			MathParser.setVariable("query.func", () -> {
				double p = MathParser.getVariableFor("query.height_percentage").get();
				return PantographBlockEntity.getArmAngle(p);
			});
			MathParser.setVariable("query.head_rotation", () -> {
				return 0;
			});
			state.setAnimation(ANIM_WIRE_CONTACT);
			return expanded ? PlayState.CONTINUE : PlayState.STOP;
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
