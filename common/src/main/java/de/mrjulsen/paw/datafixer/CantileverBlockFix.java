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

package de.mrjulsen.paw.datafixer;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import de.mrjulsen.paw.PantographsAndWires;
import net.minecraft.util.datafix.fixes.References;

import java.util.Optional;

public class CantileverBlockFix extends DataFix {

    private static final String CANTILEVER_REGEX = "pantographsandwires:cantilever_(double_)?[3-7]_(brown|green)";

    private final String name;

    public CantileverBlockFix(Schema outputSchema, String name) {
        super(outputSchema, false);
        this.name = name;
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(name, this.getInputSchema().getType(References.BLOCK_STATE), typed -> typed.update(DSL.remainderFinder(), dynamic -> {
            Optional<String> optional = dynamic.get("Name").asString().result();
            if (optional.isPresent() && optional.get().matches(CANTILEVER_REGEX)) {
                String insulatorType = optional.get().replaceAll("pantographsandwires:cantilever_(double_)?[3-7]_", "");
                dynamic = dynamic.set("Name", dynamic.createString(PantographsAndWires.MOD_ID + ":cantilever_" + insulatorType));
                return dynamic;
            }

            return dynamic;
        }));
    }

}
