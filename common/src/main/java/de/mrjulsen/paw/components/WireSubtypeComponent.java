package de.mrjulsen.paw.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record WireSubtypeComponent(Optional<ResourceLocation> id) {

    public static final Codec<WireSubtypeComponent> CODEC = RecordCodecBuilder.create(builder -> {
        return builder.group(
                ResourceLocation.CODEC.optionalFieldOf("id").forGetter(WireSubtypeComponent::id)
        ).apply(builder, WireSubtypeComponent::new);
    });

    public static final StreamCodec<RegistryFriendlyByteBuf, WireSubtypeComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(ResourceLocation.CODEC).apply(ByteBufCodecs::optional), WireSubtypeComponent::id,
            WireSubtypeComponent::new
    );

    public static WireSubtypeComponent empty() {
        return new WireSubtypeComponent(
                Optional.empty()
        );
    }
}
