package de.mrjulsen.paw.data;

import java.util.Arrays;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.client.gui.widgets.IIconRepresentable;
import de.mrjulsen.paw.item.CatenaryHeadspanWireItem;
import de.mrjulsen.paw.item.CatenaryWireItem;
import de.mrjulsen.paw.item.FeederWireItem;
import de.mrjulsen.wires.item.IPawWireItemBase;

public enum WireTypes implements IIconRepresentable, ITranslatableEnum {
    ENERGY(0, "energy", ModGuiIcons.CANTILEVER_CENTER, Suppliers.memoize(FeederWireItem::new)),
    TENSION(1, "tension", ModGuiIcons.CANTILEVER_CENTER, Suppliers.memoize(FeederWireItem::new)),
    CATENARY(2, "catenary", ModGuiIcons.CANTILEVER_CENTER, Suppliers.memoize(CatenaryWireItem::new)),
    HEADSPAN(3, "headspan", ModGuiIcons.CANTILEVER_CENTER, Suppliers.memoize(CatenaryHeadspanWireItem::new));

    private final int id;
    private final String name;
    private final ModGuiIcons icon;    
    private final Supplier<IPawWireItemBase> itemImpl;    

    private WireTypes(int id, String name, ModGuiIcons icon, Supplier<IPawWireItemBase> itemImpl) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.itemImpl = itemImpl;
    }

    public int getId() {
        return id;
    }

    public static WireTypes getById(int id) {
        return Arrays.stream(values()).filter(x -> x.getId() == id).findFirst().orElse(ENERGY);
    }

    public IPawWireItemBase getItemImpl() {
        return itemImpl.get();
    }

    @Override
    public String getEnumName() {
        return "wire_types";
    }

    @Override
    public String getEnumValueName() {
        return name;
    }

    @Override
    public ModGuiIcons getIcon() {
        return icon;
    }
    
}
