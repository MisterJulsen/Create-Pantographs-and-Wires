package de.mrjulsen.paw.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record WireConnectionDataComponent(List<CompoundTag> customPointData, CompoundTag customData) {

    public static final Codec<WireConnectionDataComponent> CODEC = RecordCodecBuilder.create(builder -> {
        return builder.group(
                Codec.list(CompoundTag.CODEC).optionalFieldOf("point_data", new ArrayList<>()).forGetter(WireConnectionDataComponent::customPointData),
                CompoundTag.CODEC.optionalFieldOf("custom_data", new CompoundTag()).forGetter(WireConnectionDataComponent::customData)
        ).apply(builder, (a, b) -> new WireConnectionDataComponent(new ArrayList<>(a), b));
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, WireConnectionDataComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG.apply(ByteBufCodecs.list()), WireConnectionDataComponent::customPointData,
            ByteBufCodecs.COMPOUND_TAG, WireConnectionDataComponent::customData,
            WireConnectionDataComponent::new
    );

    public static WireConnectionDataComponent empty() {
        return new WireConnectionDataComponent(
                List.of(),
                new CompoundTag()
        );
    }
}
