package de.mrjulsen.paw.client.gui;

import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.mcdragonlib.client.render.Sprite;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import net.minecraft.resources.ResourceLocation;

public enum ModGuiIcons {
    EMPTY("empty", 0, 0),
    CANTILEVER_CENTER("cantilever_center", 1, 0),
    CANTILEVER_OUTER("cantilever_outer", 2, 0),
    CANTILEVER_INNER("cantilever_inner", 3, 0),
    CANTILEVER_INSULATOR_NONE("cantilever_insulator_none", 4, 0),
    CANTILEVER_INSULATOR_BACK("cantilever_insulator_back", 5, 0),
    CANTILEVER_INSULATOR_FRONT("cantilever_insulator_front", 6, 0),
    CANTILEVER_SUPPORT_TUBE("cantilever_support_tube", 7, 0),
    CATENARY_WIRE("catenary_wire", 8, 0),
    ENERGY_WIRE("energy_wire", 9, 0),
    CATENARY_HEADSPAN_WIRE("catenary_headspan_wire", 10, 0),
    TENSION_WIRE("tension_wire", 11, 0);


    private String id;
    private int u;
    private int v;

    public static final int ICON_SIZE = 16;
    public static final ResourceLocation ICON_LOCATION = new ResourceLocation(PantographsAndWires.MOD_ID, "textures/gui/icons.png");

    ModGuiIcons(String id, int u, int v) {
        this.id = id;
        this.u = u;
        this.v = v;
    }

    public String getId() {
        return id;
    }

    public int getUMultiplier() {
        return u;
    }

    public int getVMultiplier() {
        return v;
    }

    public int getU() {
        return u * ICON_SIZE;
    }

    public int getV() {
        return v * ICON_SIZE;
    }

    public static ModGuiIcons getByStringId(String id) {
        return Arrays.stream(values()).filter(x -> x.getId().equals(id)).findFirst().orElse(ModGuiIcons.EMPTY);
    }

    public AllIcons getAsCreateIcon() {
        return new ModAllIcons(u, v);
    }

    public void render(Graphics graphics, int x, int y) {
        GuiUtils.drawTexture(ModGuiIcons.ICON_LOCATION, graphics, x, y, getU(), getV(), ICON_SIZE, ICON_SIZE);
    }
    
    public Sprite getAsSprite(int renderWidth, int renderHeight) {
        return new Sprite(ICON_LOCATION, 256, 256, getU(), getV(), ICON_SIZE, ICON_SIZE, renderWidth, renderHeight);
    }

    public static class ModAllIcons extends AllIcons {

        public ModAllIcons(int x, int y) {
            super(x, y);
        }

        @Override
        public void bind() {
            RenderSystem.setShaderTexture(0, ICON_LOCATION);
        }
        
    }
}
