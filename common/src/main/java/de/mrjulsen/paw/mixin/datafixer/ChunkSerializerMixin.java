package de.mrjulsen.paw.mixin.datafixer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.Pair;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverInsulatorsPlacement;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import de.mrjulsen.paw.block.property.ECantileverConnectionType;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity.SubCantileverSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;

@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
    
    @Unique
    private static final String CANTILEVER_REGEX = "pantographsandwires:cantilever_(double_)?[3-7]_(green|brown)";
    @Unique
    private static final Pattern CANTILEVER_REGEX_PATTERN = Pattern.compile(CANTILEVER_REGEX);
    @Unique
    private static final Function<String, Matcher> CANTILEVER_REGEX_MATCHER = (i) -> CANTILEVER_REGEX_PATTERN.matcher(i);

    @Inject(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putInt(Ljava/lang/String;I)V", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void paw$write(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir, ChunkPos pos, CompoundTag tag) {
        tag.putInt(PantographsAndWires.NBT_DATA_FIXER, PantographsAndWires.DATA_FIXER_VERSION);
    }

    @Inject(method = "read", at = @At(value = "HEAD"))
    private static void paw$read(ServerLevel level, PoiManager poiManager, RegionStorageInfo regionStorageInfo, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
        if (tag.getInt(PantographsAndWires.NBT_DATA_FIXER) >= PantographsAndWires.DATA_FIXER_VERSION) {
            return;
        }

        try {
            Map<BlockPos, CompoundTag> foundBlocks = new HashMap<>();
            List<CompoundTag> sections = tag.getList("sections", Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList();

            int chunkX = tag.getInt("xPos");
            int chunkZ = tag.getInt("zPos");

            for (CompoundTag section : sections) {
                int sectionY = section.getInt("Y");
                CompoundTag blockStates = section.getCompound("block_states");
                ListTag blockStatesList = blockStates.getList("palette", Tag.TAG_COMPOUND);
                List<Pair<CompoundTag, Boolean>> palette = new ArrayList<>(blockStatesList.size());
                for (Tag bst : blockStatesList) {
                    palette.add(new Pair<>((CompoundTag)bst, CANTILEVER_REGEX_MATCHER.apply(((CompoundTag)bst).getString("Name")).matches()));
                }
                if (palette.isEmpty()) {
                    continue;
                }

                boolean found = false;
                for (Pair<CompoundTag, Boolean> state : palette) {
                    if (state.getSecond()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    continue;
                }

                long[] data = blockStates.getLongArray("data");                
                if (data == null || data.length <= 0) {
                    continue;
                }

                int bitsPerEntry = Math.max(4, (int)Math.ceil(Math.log(palette.size()) / Math.log(2)));
                int blocksPerLong = 64 / bitsPerEntry;
                int mask = (1 << bitsPerEntry) - 1;

                // Collect block positions of all cantilevers
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

                            Pair<CompoundTag, Boolean> state = palette.get(paletteIndex);
                            if (state.getSecond()) {
                                int globalX = chunkX * 16 + x;
                                int globalY = sectionY * 16 + y;
                                int globalZ = chunkZ * 16 + z;
                                foundBlocks.put(new BlockPos(globalX, globalY, globalZ), state.getFirst());
                            }
                        }
                    }
                }

                // Rename cantilevers
                ListTag bsl = new ListTag();
                for (int i = 0; i < palette.size(); i++) {
                    Pair<CompoundTag, Boolean> pair = palette.get(i);
                    if (!pair.getSecond()) {
                        bsl.add(pair.getFirst());
                        continue;
                    }
                    
                    CompoundTag state = pair.getFirst().copy();
                    String name = state.getString("Name");
                    String insulatorType = name.replaceAll("pantographsandwires:cantilever_(double_)?[3-7]_", "");
                    state.putString("Name", PantographsAndWires.MOD_ID + ":cantilever_" + insulatorType);
                    bsl.add(state);
                }
                blockStates.put("palette", bsl);
            }

            if (foundBlocks.isEmpty()) {
                return;
            }

            List<CompoundTag> blockEntities = tag.getList("block_entities", Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList();
            Map<BlockPos, CompoundTag> blockEntitiesByPos = new HashMap<>();
            for (CompoundTag blockEntity : blockEntities) {
                int x = blockEntity.getInt("x");
                int y = blockEntity.getInt("y");
                int z = blockEntity.getInt("z");
                if (x >= Integer.MAX_VALUE || y >= Integer.MAX_VALUE || z >= Integer.MAX_VALUE) {
                    continue;
                }
                blockEntitiesByPos.put(new BlockPos(x, y, z), blockEntity);
            }

            for (Map.Entry<BlockPos, CompoundTag> entry : foundBlocks.entrySet()) {
                try {
                    BlockPos ppos = entry.getKey();
                    CompoundTag state = entry.getValue();

                    String name = state.getString("Name");
                    int rawWidth = Integer.parseInt(name.replaceAll("pantographsandwires:cantilever_(double_)?", "").replaceAll("_(brown|green)", ""));
                    float width = (float)rawWidth - 0.5f;
                    String rawInsulatorType = name.replaceAll("pantographsandwires:cantilever_(double_)?[0-9]_", "");
                    int insulatorType = EInsulatorType.getByName(rawInsulatorType).getId();
                    byte cantileverCount = (byte)(name.contains("double") ? 2 : 1);

                    ECantileverInsulatorsPlacement insulatorPlacement = ECantileverInsulatorsPlacement.getByName(((CompoundTag)state.get("Properties")).getString("insulator_placement").replace("\"", ""));
                    int registrationArmType = ECantileverRegistrationArmType.getByName(((CompoundTag)state.get("Properties")).getString("registration_arm").replace("\"", "")).ordinal();
                    int postConnectionOffset = ECantileverConnectionType.getByName(((CompoundTag)state.get("Properties")).getString("connection").replace("\"", "")).getIndex();
                    float height = 0.5f * rawWidth - DragonLib.BLOCK_PIXEL * 2;
                    float catenaryHeight = width < 5 ? 1 : 2;
                    boolean showBracing = width >= 4;

                    CompoundTag existing = blockEntitiesByPos.get(ppos);
                    if (existing != null) {
                        existing.putFloat(CantileverBlockEntity.NBT_WIDTH, width);
                        //existing.putInt(CantileverBlockEntity.NBT_INSULATOR_PLACEMENT, insulatorPlacement);
                        existing.putFloat(CantileverBlockEntity.NBT_HEIGHT, height);
                        existing.putFloat(CantileverBlockEntity.NBT_CATENARY_HEIGHT, catenaryHeight);
                        existing.putInt(CantileverBlockEntity.NBT_POST_CONNECTION_OFFSET, postConnectionOffset);
                        existing.putInt(CantileverBlockEntity.NBT_INSULATOR_TYPE, insulatorType);
                        if (cantileverCount > 1 && registrationArmType == ECantileverRegistrationArmType.CENTER.ordinal()) {
                            //existing.putInt(CantileverBlockEntity.NBT_REGISTRATION_ARM_TYPE, ECantileverRegistrationArmType.INNER.ordinal());
                            ListTag list = new ListTag();
                            list.add(new SubCantileverSetting(ECantileverRegistrationArmType.OUTER, insulatorPlacement, showBracing).toNbt());
                            existing.put(CantileverBlockEntity.NBT_SUB_CANTILEVER_SETTINGS, list);
                        } else {
                            //existing.putInt(CantileverBlockEntity.NBT_REGISTRATION_ARM_TYPE, registrationArmType);
                        }
                        blockEntitiesByPos.put(ppos, existing);
                    } else {
                        CompoundTag newEntity = new CompoundTag();
                        newEntity.putInt("x", ppos.getX());
                        newEntity.putInt("y", ppos.getY());
                        newEntity.putInt("z", ppos.getZ());
                        newEntity.putString("id", PantographsAndWires.MOD_ID + ":" + "cantilever_block_entity");
                        newEntity.putFloat(CantileverBlockEntity.NBT_WIDTH, width);
                        newEntity.putFloat(CantileverBlockEntity.NBT_HEIGHT, height);
                        newEntity.putFloat(CantileverBlockEntity.NBT_CATENARY_HEIGHT, catenaryHeight);
                        newEntity.putInt(CantileverBlockEntity.NBT_POST_CONNECTION_OFFSET, postConnectionOffset);
                        newEntity.putInt(CantileverBlockEntity.NBT_INSULATOR_TYPE, insulatorType);
                        if (cantileverCount > 1 && registrationArmType == ECantileverRegistrationArmType.CENTER.ordinal()) {
                            //newEntity.putInt(CantileverBlockEntity.NBT_REGISTRATION_ARM_TYPE, ECantileverRegistrationArmType.INNER.ordinal());
                            ListTag list = new ListTag();
                            list.add(new SubCantileverSetting(ECantileverRegistrationArmType.OUTER, insulatorPlacement, showBracing).toNbt());
                            newEntity.put(CantileverBlockEntity.NBT_SUB_CANTILEVER_SETTINGS, list);
                        } else {
                            //newEntity.putInt(CantileverBlockEntity.NBT_REGISTRATION_ARM_TYPE, registrationArmType);
                        }
                        blockEntitiesByPos.put(ppos, newEntity);
                    }
                } catch (Exception e) {
                    PantographsAndWires.LOGGER.error("Unable to convert cantilever.", e);
                }
            }
            ListTag lst = new ListTag();
            for (CompoundTag c : blockEntitiesByPos.values()) {
                lst.add(c);
            }
            tag.put("block_entities", lst);
        } catch (Exception e) {
            PantographsAndWires.LOGGER.error("Error while running datafixer: " + "", e);
        }
    }
}
