package de.mrjulsen.paw.client.gui;

import java.util.Arrays;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.mcdragonlib.client.util.DLGuiGraphics;
import de.mrjulsen.mcdragonlib.client.util.DLSprite;
import de.mrjulsen.mcdragonlib.client.util.DLTexture;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.util.DLUtils;

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
    DECORATION_WIRE("decoration_wire", 11, 0);


    private String id;
    private int u;
    private int v;

    public static final int ICON_SIZE = 16;
    public static final DLTexture ICONS = new DLTexture(DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "textures/gui/icons.png"), 256, 256);

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

    public void render(DLGuiGraphics graphics, int x, int y) {
        GuiUtils.drawTexture(ModGuiIcons.ICONS, graphics, x, y, ICON_SIZE, ICON_SIZE, getU(), getV());
    }
    
    public DLSprite getAsSprite(int renderWidth, int renderHeight) {
        return new DLSprite(ICONS, ICON_SIZE, ICON_SIZE, getU(), getV(), renderWidth, renderHeight);
    }

    public static class ModAllIcons extends AllIcons {

        public ModAllIcons(int x, int y) {
            super(x, y);
        }

        @Override
        public void bind() {
            RenderSystem.setShaderTexture(0, ICONS.getTexture().get());
        }
        
    }
}
