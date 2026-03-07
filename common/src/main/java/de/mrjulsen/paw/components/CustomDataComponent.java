package de.mrjulsen.paw.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record CustomDataComponent(List<CompoundTag> customPointData, CompoundTag customData) {

    public static final Codec<CustomDataComponent> CODEC = RecordCodecBuilder.create(builder -> {
        return builder.group(
                Codec.list(CompoundTag.CODEC).optionalFieldOf("custom_point_data", new ArrayList<>()).forGetter(CustomDataComponent::customPointData),
                CompoundTag.CODEC.optionalFieldOf("custom_data", new CompoundTag()).forGetter(CustomDataComponent::customData)
        ).apply(builder, (a, b) -> new CustomDataComponent(new ArrayList<>(a), b));
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomDataComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG.apply(ByteBufCodecs.list()), CustomDataComponent::customPointData,
            ByteBufCodecs.COMPOUND_TAG, CustomDataComponent::customData,
            CustomDataComponent::new
    );

    public static CustomDataComponent empty() {
        return new CustomDataComponent(
                List.of(),
                new CompoundTag()
        );
    }
}
