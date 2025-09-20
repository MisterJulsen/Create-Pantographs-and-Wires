package de.mrjulsen.paw.block;

import java.util.Arrays;

import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.data.MapCache;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class RegistrationArmBlock extends Block {

    public static enum State implements StringRepresentable {
        NORMAL((byte)0, "normal", false),
        NORMAL_CENTERED((byte)1, "centered", false),
        ABOVE((byte)2, "above", true),
        ABOVE_CENTERED((byte)3, "above_centered", true);

        private final byte id;
        private final String name;
        private final boolean above;

        private static final MapCache<State, Byte, Byte> getter = new MapCache<>((a) -> Arrays.stream(values()).filter(x -> x.getId() == a).findFirst().orElse(NORMAL), Object::hashCode, ECachingPriority.LOW);

        private State(byte id, String name, boolean above) {
            this.id = id;
            this.name = name;
            this.above = above;
        }

        public byte getId() {
            return id;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
        
        public static State getById(int id) {
            return getter.get((byte)id, (byte)id);
        }

        public boolean isAbove() {
            return above;
        }
    }

    public static final EnumProperty<State> REGISTRATION_ARM = EnumProperty.create("registration_arm", State.class);
    public static final BooleanProperty MIRRORED = BooleanProperty.create("mirrored");
    
    public RegistrationArmBlock(Properties p) {
        super(p);

        this.registerDefaultState(defaultBlockState()
            .setValue(REGISTRATION_ARM, State.NORMAL)
            .setValue(MIRRORED, false)
        );
    }
    
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(REGISTRATION_ARM, MIRRORED);
    }
}
