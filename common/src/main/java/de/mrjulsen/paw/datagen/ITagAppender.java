package de.mrjulsen.paw.datagen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public interface ITagAppender<T> {
    ITagAppender<T> add(T t);
    ITagAppender<T> add(T... ts);
    ITagAppender<T> add(TagKey<T> tag);
    ITagAppender<T> add(TagKey<T>... tags);
    ITagAppender<T> add(ResourceLocation id);
    ITagAppender<T> add(ResourceLocation... ids);
    ITagAppender<T> add(ResourceKey<T> key);
    ITagAppender<T> add(ResourceKey<T>... keys);
    ITagAppender<T> addOptional(ResourceLocation id);
    ITagAppender<T> addOptional(ResourceKey<T> key);
    ITagAppender<T> addOptional(TagKey<T> tag);
    ITagAppender<T> addOptionalTag(ResourceLocation id);
}