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

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverInsulatorsPlacement;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import de.mrjulsen.paw.block.property.ECantileverConnectionType;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.datafix.fixes.References;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.LongStream;

public class CantileverDataFix extends DataFix {

    private static final String CANTILEVER_REGEX = "pantographsandwires:cantilever_(double_)?[3-7]_(green|brown)";
    private static final Pattern CANTILEVER_REGEX_PATTERN = Pattern.compile(CANTILEVER_REGEX);
    private static final Function<String, Matcher> CANTILEVER_REGEX_MATCHER = (i) -> CANTILEVER_REGEX_PATTERN.matcher(i);

    private final String name;

    public CantileverDataFix(Schema outputSchema, String name) {
        super(outputSchema, false);
        this.name = name;
    }

    @Override
    public TypeRewriteRule makeRule() {
        return writeFixAndRead(name, this.getInputSchema().getType(References.CHUNK), this.getInputSchema().getType(References.CHUNK), dynamic -> {
            try {
                Map<BlockPos, Dynamic<?>> foundBlocks = new HashMap<>();
                List<? extends Dynamic<?>> sections = dynamic.get("sections").asList(Function.identity());

                int chunkX = dynamic.get("xPos").asInt(0);
                int chunkZ = dynamic.get("zPos").asInt(0);

                for (Dynamic<?> section : sections) {
                    int sectionY = section.get("Y").asInt(0);
                    OptionalDynamic<?> blockStates = section.get("block_states");
                    List<? extends Dynamic<?>> palette = blockStates.get("palette").asList(Function.identity());

                    boolean found = true;
                    for (Dynamic<?> state : palette) {
                        String name = state.get("Name").asString("");
                        if (CANTILEVER_REGEX_MATCHER.apply(name).matches()) {
                            found = true;
                        }
                        if (found) break;
                    }
                    if (!found) continue;

                    Optional<long[]> rawData = blockStates.get("data").asLongStreamOpt().map(LongStream::toArray).result();
                    
                    if (palette.isEmpty() || rawData.isEmpty()) {
                        continue;
                    }

                    long[] data = rawData.get();
                    int bitsPerEntry = Math.max(4, (int)Math.ceil(Math.log(palette.size()) / Math.log(2)));
                    int blocksPerLong = 64 / bitsPerEntry;
                    int mask = (1 << bitsPerEntry) - 1;

                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            for (int x = 0; x < 16; x++) {
                                int index = y * 16 * 16 + z * 16 + x;
                                int longIndex = index / blocksPerLong;
                                int bitIndex = (index % blocksPerLong) * bitsPerEntry;

                                if (longIndex >= data.length) continue;

                                long blockData = data[longIndex];
                                int paletteIndex = (int)(blockData >> bitIndex) & mask;

                                if (paletteIndex >= palette.size()) continue;

                                Dynamic<?> state = palette.get(paletteIndex);
                                String blockName = state.get("Name").asString("");

                                if (CANTILEVER_REGEX_MATCHER.apply(blockName).matches()) {
                                    int globalX = chunkX * 16 + x;
                                    int globalY = sectionY * 16 + y;
                                    int globalZ = chunkZ * 16 + z;
                                    foundBlocks.put(new BlockPos(globalX, globalY, globalZ), state);
                                }
                            }
                        }
                    }

                }

                if (foundBlocks.isEmpty()) {
                    return dynamic;
                }

                List<? extends Dynamic<?>> blockEntities = dynamic.get("block_entities").asList(Function.identity());
                Map<BlockPos, Dynamic<?>> blockEntitiesByPos = new HashMap<>();
                for (Dynamic<?> blockEntity : blockEntities) {
                    int x = blockEntity.get("x").asInt(Integer.MAX_VALUE);
                    int y = blockEntity.get("y").asInt(Integer.MAX_VALUE);
                    int z = blockEntity.get("z").asInt(Integer.MAX_VALUE);
                    if (x >= Integer.MAX_VALUE || y >= Integer.MAX_VALUE || z >= Integer.MAX_VALUE) {
                        continue;
                    }
                    blockEntitiesByPos.put(new BlockPos(x, y, z), blockEntity);
                }

                for (Map.Entry<BlockPos, Dynamic<?>> entry : foundBlocks.entrySet()) {
                    try {
                        BlockPos pos = entry.getKey();
                        Dynamic<?> state = entry.getValue();

                        String name = state.get("Name").asString("");
                        int rawWidth = Integer.parseInt(name.replaceAll("pantographsandwires:cantilever_(double_)?", "").replaceAll("_(brown|green)", ""));
                        float width = (float)rawWidth - 0.5f;
                        String rawInsulatorType = name.replaceAll("pantographsandwires:cantilever_(double_)?[0-9]_", "");
                        int insulatorType = EInsulatorType.getByName(rawInsulatorType).getId();
                        byte cantileverCount = (byte)(name.contains("double") ? 2 : 1);
                        
                        int insulatorPlacement = ECantileverInsulatorsPlacement.getByName(state.get("Properties").get("insulator_placement").asString("").replace("\"", "")).ordinal();
                        int registrationArmType = ECantileverRegistrationArmType.getByName(state.get("Properties").get("registration_arm").asString("").replace("\"", "")).ordinal();
                        int postConnectionOffset = ECantileverConnectionType.getByName(state.get("Properties").get("connection").asString("").replace("\"", "")).getIndex();
                        float height = 0.5f * rawWidth - DragonLib.PIXEL * 2;
                        float catenaryHeight = width < 6 ? 1 : 2;

                        Dynamic<?> existing = blockEntitiesByPos.get(pos);
                        if (existing != null) {
                            Dynamic<?> updated = existing
                                .set(CantileverBlockEntity.NBT_WIDTH, existing.createFloat(width))
                                .set(CantileverBlockEntity.NBT_INSULATOR_PLACEMENT, existing.createInt(insulatorPlacement))
                                .set(CantileverBlockEntity.NBT_REGISTRATION_ARM_TYPE, existing.createInt(registrationArmType))
                                .set(CantileverBlockEntity.NBT_HEIGHT, existing.createFloat(height))
                                .set(CantileverBlockEntity.NBT_CATENARY_HEIGHT, existing.createFloat(catenaryHeight))
                                .set(CantileverBlockEntity.NBT_POST_CONNECTION_OFFSET, existing.createFloat(postConnectionOffset))
                                .set(CantileverBlockEntity.NBT_INSULATOR_TYPE, existing.createInt(insulatorType))
                                .set(CantileverBlockEntity.NBT_CANTILEVERS_COUNT, existing.createByte(cantileverCount))
                            ;
                            blockEntitiesByPos.put(pos, updated);
                        } else {
                            Dynamic<?> newEntity = dynamic.createMap(Map.ofEntries(
                                Map.entry(dynamic.createString("x"), dynamic.createInt(pos.getX())),
                                Map.entry(dynamic.createString("y"), dynamic.createInt(pos.getY())),
                                Map.entry(dynamic.createString("z"), dynamic.createInt(pos.getZ())),
                                Map.entry(dynamic.createString("id"), dynamic.createString(PantographsAndWires.MOD_ID + ":" + "cantilever_block_entity")),
                                Map.entry(dynamic.createString(CantileverBlockEntity.NBT_WIDTH), dynamic.createFloat(width)),
                                Map.entry(dynamic.createString(CantileverBlockEntity.NBT_INSULATOR_PLACEMENT), dynamic.createInt(insulatorPlacement)),
                                Map.entry(dynamic.createString(CantileverBlockEntity.NBT_REGISTRATION_ARM_TYPE), dynamic.createInt(registrationArmType)),
                                Map.entry(dynamic.createString(CantileverBlockEntity.NBT_HEIGHT), dynamic.createFloat(height)),
                                Map.entry(dynamic.createString(CantileverBlockEntity.NBT_CATENARY_HEIGHT), dynamic.createFloat(catenaryHeight)),
                                Map.entry(dynamic.createString(CantileverBlockEntity.NBT_POST_CONNECTION_OFFSET), dynamic.createFloat(postConnectionOffset)),
                                Map.entry(dynamic.createString(CantileverBlockEntity.NBT_INSULATOR_TYPE), dynamic.createInt(insulatorType)),
                                Map.entry(dynamic.createString(CantileverBlockEntity.NBT_CANTILEVERS_COUNT), dynamic.createByte(cantileverCount))
                            ));
                            blockEntitiesByPos.put(pos, newEntity);
                        }
                    } catch (Exception e) {
                        PantographsAndWires.LOGGER.error("Unable to convert cantilever.", e);
                    }
                }
                Dynamic<?> result = dynamic.set("block_entities", dynamic.createList(blockEntitiesByPos.values().stream()));
                return result;
            } catch (Exception e) {
                PantographsAndWires.LOGGER.error("Error while running datafixer: " + name, e);
                return dynamic;
            }
        });
    }


    public static int[] unpackBlockIndices(long[] data, int paletteSize) {
        int bitsPerEntry = Math.max(4, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
        int valuesPerLong = 64 / bitsPerEntry;
        int[] result = new int[4096];

        int longIndex = 0;

        for (int i = 0; i < 4096; i++) {
            if (longIndex >= data.length) break;
            long value = data[longIndex];

            int startBit = (i % valuesPerLong) * bitsPerEntry;
            if (startBit + bitsPerEntry > 64) {
                long low = value >>> startBit;
                long high = data[longIndex + 1] & ((1L << (bitsPerEntry - (64 - startBit))) - 1);
                result[i] = (int) ((high << (64 - startBit)) | low);
                longIndex++;
            } else {
                result[i] = (int) ((value >>> startBit) & ((1L << bitsPerEntry) - 1));
                if ((i + 1) % valuesPerLong == 0) longIndex++;
            }
        }
        return result;
    }

}
