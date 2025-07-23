package de.mrjulsen.paw.registry;

/*
 * Steam 'n' Rails
 * Copyright (c) 2022-2024 The Railways Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;

import de.mrjulsen.paw.datafixer.CantileverBlockFix;
import de.mrjulsen.paw.datafixer.CantileverDataFix;
import de.mrjulsen.paw.datafixer.api.DataFixesInternals;
import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.config.ModCommonConfig;

public class ModDataFixers {
    private static final BiFunction<Integer, Schema, Schema> SAME = Schema::new;
    private static final BiFunction<Integer, Schema, Schema> SAME_NAMESPACED = NamespacedSchema::new;

    public static void init() {
        PantographsAndWires.LOGGER.info("Registering data fixers...");

        //if (ModCommonConfig.USE_DATA_FIXERS.get()) {
        //    PantographsAndWires.LOGGER.warn("Skipping Datafixer Registration due to it being disabled in the config.");
        //    return;
        //}

        DataFixesInternals api = DataFixesInternals.get();

        DataFixerBuilder builder = new DataFixerBuilder(PantographsAndWires.DATA_FIXER_VERSION);
        addFixers(builder);

        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("Pantographs And Wires Datafixer Bootstrap").setDaemon(true).setPriority(1).build());
        api.registerFixer(PantographsAndWires.DATA_FIXER_VERSION, builder.buildOptimized(SharedConstants.DATA_FIX_TYPES_TO_OPTIMIZE, executor));
    }

    private static void addFixers(DataFixerBuilder builder) {
        builder.addSchema(0, DataFixesInternals.BASE_SCHEMA);

        Schema schemaV2 = builder.addSchema(1, SAME_NAMESPACED);
        builder.addFixer(new CantileverDataFix(schemaV2, "Convert Cantilever BlockState Data to BlockEntity"));
        builder.addFixer(new CantileverBlockFix(schemaV2, "Convert Cantilever Block Name"));
    }
}
