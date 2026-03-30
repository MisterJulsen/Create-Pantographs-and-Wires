package de.mrjulsen.paw.datagen.fabric;

import de.mrjulsen.paw.datagen.ITagAppender;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class TagAppender<T> implements ITagAppender<T> {

    private final FabricTagProvider<T>.FabricTagBuilder builder;

    public TagAppender(FabricTagProvider<T>.FabricTagBuilder builder) {
        this.builder = builder;
    }

    @Override
    public ITagAppender<T> add(T t) {
        builder.add(t);
        return this;
    }

    @Override
    public ITagAppender<T> add(T... ts) {
        builder.add(ts);
        return this;
    }

    @Override
    public ITagAppender<T> add(TagKey<T> tag) {
        builder.forceAddTag(tag);
        return this;
    }

    @Override
    public ITagAppender<T> add(TagKey<T>... tags) {
        builder.addTags(tags);
        return this;
    }

    @Override
    public ITagAppender<T> add(ResourceLocation id) {
        builder.add(id);
        return this;
    }

    @Override
    public ITagAppender<T> add(ResourceLocation... ids) {
        builder.add(ids);
        return this;
    }

    @Override
    public ITagAppender<T> add(ResourceKey<T> key) {
        builder.add(key);
        return this;
    }

    @Override
    public ITagAppender<T> add(ResourceKey<T>... keys) {
        builder.add(keys);
        return this;
    }

    @Override
    public ITagAppender<T> addOptional(ResourceLocation id) {
        builder.addOptional(id);
        return this;
    }

    @Override
    public ITagAppender<T> addOptional(ResourceKey<T> key) {
        builder.addOptional(key);
        return this;
    }

    @Override
    public ITagAppender<T> addOptional(TagKey<T> tag) {
        builder.addOptionalTag(tag);
        return this;
    }

    @Override
    public ITagAppender<T> addOptionalTag(ResourceLocation id) {
        builder.addOptionalTag(id);
        return this;
    }
}
