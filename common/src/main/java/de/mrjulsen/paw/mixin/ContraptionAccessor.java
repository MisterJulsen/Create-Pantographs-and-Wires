package de.mrjulsen.paw.mixin;

import java.util.concurrent.atomic.AtomicReference;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ClientContraption;

@Mixin(Contraption.class)
public interface ContraptionAccessor {

    @Accessor(value = "clientContraption", remap = false)
    AtomicReference<ClientContraption> paw$clientContraption();
}
