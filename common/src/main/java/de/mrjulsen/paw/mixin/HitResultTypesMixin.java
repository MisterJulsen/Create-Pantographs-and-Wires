package de.mrjulsen.paw.mixin;

import java.util.ArrayList;
import java.util.Arrays;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;

import de.mrjulsen.paw.data.CustomHitResultTypes;
import net.minecraft.world.phys.HitResult;

@Mixin(HitResult.Type.class)
@Unique
public class HitResultTypesMixin {

	@Shadow
	@Final
	@Mutable
	private static HitResult.Type[] $VALUES;

	static {
		for (CustomHitResultTypes src : CustomHitResultTypes.values()) {
			paw$addVariant(src.name().toUpperCase());
		}
	}

	@Invoker("<init>")
	public static HitResult.Type paw$invokeInit(String internalName, int internalId) {
		throw new AssertionError();
	}

	private static HitResult.Type paw$addVariant(String internalName) {
		ArrayList<HitResult.Type> variants = new ArrayList<HitResult.Type>(Arrays.asList(HitResultTypesMixin.$VALUES));
		HitResult.Type type = paw$invokeInit(internalName, variants.get(variants.size() - 1).ordinal() + 1);
		variants.add(type);
		HitResultTypesMixin.$VALUES = variants.toArray(HitResult.Type[]::new);
		return type;
	}
}
