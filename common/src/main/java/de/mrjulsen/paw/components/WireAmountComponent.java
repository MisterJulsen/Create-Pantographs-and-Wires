package de.mrjulsen.paw.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record WireAmountComponent(int wireAmount) {

    public static final int MAX_WIRE = 400;

    public static final Codec<WireAmountComponent> CODEC = RecordCodecBuilder.create(builder -> {
        return builder.group(
                Codec.INT.optionalFieldOf("wire_amount", 0).forGetter(WireAmountComponent::wireAmount)
        ).apply(builder, WireAmountComponent::new);
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, WireAmountComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, WireAmountComponent::wireAmount,
            WireAmountComponent::new
    );

    public static WireAmountComponent empty() {
        return new WireAmountComponent(
                MAX_WIRE
        );
    }
}
