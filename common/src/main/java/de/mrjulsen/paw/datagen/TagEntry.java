package de.mrjulsen.paw.datagen;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;
import java.util.function.Supplier;

public record TagEntry<T>(TagKey<T> key, Consumer<ITagAppender<T>> populator) {}
