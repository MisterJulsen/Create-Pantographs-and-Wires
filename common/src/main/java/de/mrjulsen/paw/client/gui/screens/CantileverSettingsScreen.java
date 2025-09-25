package de.mrjulsen.paw.client.gui.screens;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllIcons;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.DLModel;
import de.mrjulsen.mcdragonlib.client.model.mesh.DLModel.ModelType;
import de.mrjulsen.mcdragonlib.client.newgui.events.DLGuiCommonEvents;
import de.mrjulsen.mcdragonlib.client.newgui.events.DLGuiStandardEvents;
import de.mrjulsen.mcdragonlib.client.newgui.widgets.base.DLGuiComponent;
import de.mrjulsen.mcdragonlib.client.newgui.widgets.base.DLWindow;
import de.mrjulsen.mcdragonlib.client.newgui.widgets.base.DLWindowManager;
import de.mrjulsen.mcdragonlib.client.newgui.widgets.components.DLCheckBox;
import de.mrjulsen.mcdragonlib.client.newgui.widgets.util.CursorType;
import de.mrjulsen.mcdragonlib.client.util.Graphics;
import de.mrjulsen.mcdragonlib.client.util.GuiUtils;
import de.mrjulsen.mcdragonlib.core.EAlignment;
import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import de.mrjulsen.mcdragonlib.util.math.Rectangle;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.Constraints;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverInsulatorsPlacement;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.client.gui.widgets.CreateButton;
import de.mrjulsen.paw.client.gui.widgets.CreateEnumSlider;
import de.mrjulsen.paw.client.gui.widgets.CreateSlider;
import de.mrjulsen.paw.client.gui.widgets.IIconEnum;
import de.mrjulsen.paw.item.CantileverBlockItem;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.paw.registry.ModNetworkAccessor;
import de.mrjulsen.paw.registry.ModNetworkAccessor.CantileverSettingsData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class CantileverSettingsScreen extends DLWindow {

    private static boolean showAdvanced = false;

    private static enum SupportTubeEnum implements ITranslatableEnum, IIconEnum {
        OFF(false, ModGuiIcons.CANTILEVER_INSULATOR_NONE),
        ON(true, ModGuiIcons.CANTILEVER_SUPPORT_TUBE);

        private final boolean b;
        private final ModGuiIcons icon;

        private SupportTubeEnum(boolean b, ModGuiIcons icon) {
            this.b = b;
            this.icon = icon;
        }

        public boolean get() {
            return b;
        }

        @Override
        public String getEnumName() {
            return "show_support_tube";
        }

        @Override
        public String getEnumValueName() {
            return String.valueOf(b);
        }

        @Override
        public ModGuiIcons getIcon() {
            return icon;
        }
    }
    
    private static final ResourceLocation TEXTURE = new ResourceLocation(PantographsAndWires.MOD_ID, "textures/gui/cantilever_settings.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int GUI_WIDTH = 251;
    private static final int GUI_HEIGHT = 231;
    private static final int AREA1_Y = 155;
    private static final int AREA2_Y = 177;

    private final Component title = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".cantilever_settings.title");
    private final Component txtAdvancedOptions = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".cantilever_settings.advanced_settings_option");
    private final MutableComponent txtWidth = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".cantilever_settings.width");
    private final MutableComponent txtHeight = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".cantilever_settings.height");
    private final MutableComponent txtCatenaryHeight = TextUtils.translate("gui." + PantographsAndWires.MOD_ID + ".cantilever_settings.catenary_height");
    
    private float width = 2.5f;
    private float height = 1.5f;
    private ECantileverRegistrationArmType registrationArmType = ECantileverRegistrationArmType.INNER;
    private ECantileverInsulatorsPlacement insulatorPlacement = ECantileverInsulatorsPlacement.BACK;
    private float catenaryHeight = 1;
    private boolean showBracing = false;

    private final Cache<BlockState> stateCache;
    private ModelContext context;

    public CantileverSettingsScreen(DLWindowManager manager, ItemStack stack) {
        super(manager);        
        if (!(stack.getItem() instanceof CantileverBlockItem)) {
            throw new IllegalArgumentException(stack.getItem() + " is not a CantileverBlockItem.");
        }

        setSize(GUI_WIDTH, GUI_HEIGHT);
        setPosition(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - width() / 2, Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 - height() / 2);

        this.width = CantileverBlockItem.getWidth(stack);
        this.height = CantileverBlockItem.getHeight(stack);
        this.catenaryHeight = CantileverBlockItem.getCatenaryHeight(stack);
        this.registrationArmType = CantileverBlockItem.getCantileverType(stack);
        this.insulatorPlacement = CantileverBlockItem.getInsulatorPlacement(stack);
        this.showBracing = CantileverBlockItem.shouldUseSupportTube(stack);

        addEventListener(DLGuiStandardEvents.LayoutUpdateEvent.class, (s, e) -> {
            setPosition(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - width() / 2, Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2 - height() / 2);
            return false;
        });
        addEventListener(DLGuiStandardEvents.CloseEvent.class, (s, e) -> {            
            CantileverSettingsData data = new CantileverSettingsData(width, height, catenaryHeight, registrationArmType, insulatorPlacement, showBracing);
            CantileverBlockItem.setNbt(stack, data);
            DataAccessor.getFromServer(data, ModNetworkAccessor.UPDATE_CANTILEVER_SETTINGS, $ -> {});
            return false;
        });

        this.stateCache = new Cache<>(() -> {
            return ModBlocks.getCantilever(((CantileverBlockItem<?>)stack.getItem()).getInsulatorType()).get().defaultBlockState();
        });
        updateModelContext();


        addEventListener(DLGuiStandardEvents.KeyPressEvent.class, (s, e) -> {
            if (e.keyCode() == GLFW.GLFW_KEY_ESCAPE) {
                getWindowManager().closeWindow(this);
            }
            return false;
        });

        
        CreateEnumSlider<ECantileverInsulatorsPlacement> insulatorPlacementSlider = new CreateEnumSlider<>(0, AREA2_Y, 50, 20, ECantileverInsulatorsPlacement.class);
        CreateEnumSlider<ECantileverRegistrationArmType> registrationArmSlider = new CreateEnumSlider<>(0, AREA2_Y, 50, 20, ECantileverRegistrationArmType.class);
        CreateEnumSlider<SupportTubeEnum> bracingSlider = new CreateEnumSlider<>(0, AREA2_Y, 50, 20, SupportTubeEnum.class);

        CreateSlider cantileverSizeSlider = new CreateSlider(0, AREA1_Y, 60, 14, txtWidth);
        CreateSlider cantileverHeightSlider = new CreateSlider(0, AREA1_Y, 60, 14, txtHeight);
        CreateSlider regstrationArmHeightSlider = new CreateSlider(0, AREA1_Y, 60, 14, txtCatenaryHeight);
        
        int cbTxtW = Minecraft.getInstance().font.width(txtAdvancedOptions);
        int cbW = 16 + cbTxtW;
        int cbH = Minecraft.getInstance().font.lineHeight;
        DLCheckBox advancedOptionCb = new DLCheckBox(220 - cbW, 148 - cbH, cbW, cbH) {
            @Override
            public void renderMainLayer(Graphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {
                if (checked.get()) {
                    GuiUtils.drawTexture(TEXTURE, graphics, 0, height() / 2 - 4, 12, 7, 0, TEXTURE_HEIGHT - 15, TEXTURE_WIDTH, TEXTURE_HEIGHT);
                } else {
                    GuiUtils.drawTexture(TEXTURE, graphics, 0, height() / 2 - 4, 12, 7, 0, TEXTURE_HEIGHT - 7, TEXTURE_WIDTH, TEXTURE_HEIGHT);
                }                
                GuiUtils.drawString(graphics, Minecraft.getInstance().font, 16, height() / 2 - Minecraft.getInstance().font.lineHeight / 2, text.get(), DragonLib.NATIVE_UI_FONT_COLOR, EAlignment.LEFT, false);
            }
        };

        CreateButton doneBtn = new CreateButton(width() - 7 - CreateButton.WIDTH, height() - 6 - CreateButton.HEIGHT, AllIcons.I_CONFIRM);
        doneBtn.cursor.set(CursorType.HAND);
        doneBtn.addEventListener(DLGuiStandardEvents.ClickEvent.class, (s, e) -> {
            getWindowManager().closeWindow(this);
            return false;
        });
        addComponent(doneBtn);
        

        final Runnable relayoutFunc = () -> {
            bracingSlider.visible.set(advancedOptionCb.checked.get() && width > 1.5f);
            regstrationArmHeightSlider.visible.set(advancedOptionCb.checked.get());
            cantileverHeightSlider.visible.set(advancedOptionCb.checked.get());
            DLGuiComponent[] area1 = { cantileverSizeSlider, cantileverHeightSlider, regstrationArmHeightSlider };
            DLGuiComponent[] area2 = { insulatorPlacementSlider, registrationArmSlider, bracingSlider };
            distributeHorizontally(area1, width() / 2);
            distributeHorizontally(area2, width() / 2);
        };

        final MutableBoolean updatingValues = new MutableBoolean();
        final Runnable updateFunc = () -> {
            updatingValues.setTrue();
            Constraints c = AbstractCantileverBlock.calculate(width, height, catenaryHeight);
            cantileverSizeSlider.min.set((double)c.width().min());
            cantileverSizeSlider.max.set((double)c.width().max());

            cantileverHeightSlider.min.set((double)c.height().min());
            cantileverHeightSlider.max.set((double)c.height().max());
            cantileverHeightSlider.step.set((double)c.height().step());

            regstrationArmHeightSlider.min.set((double)c.armHeight().min());
            regstrationArmHeightSlider.max.set((double)c.armHeight().max());
            regstrationArmHeightSlider.step.set((double)c.armHeight().step());

            registrationArmSlider.visible.set(c.registrationArmsAllowed());
            if (!c.registrationArmsAllowed()) {
                registrationArmSlider.value.set((double)ECantileverRegistrationArmType.CENTER.ordinal());
            }            

            if (!showAdvanced) {
                float w = (width - 0.5f);
                cantileverHeightSlider.value.set((double)(w / 2 + 0.5d));
                regstrationArmHeightSlider.value.set((double)(w <= 1 ? 0.5d : Math.floor((w + 1) / 3.0d)));
            }

            relayoutFunc.run();
            updatingValues.setFalse();
        };

        cantileverSizeSlider.min.set((double)AbstractCantileverBlock.MIN_WIDTH);
        cantileverSizeSlider.max.set((double)AbstractCantileverBlock.MAX_WIDTH);
        cantileverSizeSlider.value.set((double)this.width);
        cantileverSizeSlider.cursor.set(CursorType.HRESIZE);
        cantileverSizeSlider.addEventListener(DLGuiCommonEvents.ValueChangedEvent.class, (s, e) -> {
            this.width = (float)e.value();

            if (updatingValues.isFalse()) updateFunc.run();
            
            stateCache.clear();
            updateModelContext();
            return false;
        });
        addComponent(cantileverSizeSlider);

        cantileverHeightSlider.step.set(0.5d);
        cantileverHeightSlider.min.set((double)AbstractCantileverBlock.MIN_HEIGHT);
        cantileverHeightSlider.max.set((double)AbstractCantileverBlock.MAX_HEIGHT);
        cantileverHeightSlider.value.set((double)this.height);
        cantileverHeightSlider.cursor.set(CursorType.HRESIZE);
        cantileverHeightSlider.addEventListener(DLGuiCommonEvents.ValueChangedEvent.class, (s, e) -> {
            this.height = (float)e.value();
            if (updatingValues.isFalse()) updateFunc.run();

            stateCache.clear();
            updateModelContext();
            return false;
        });
        addComponent(cantileverHeightSlider);
        
        regstrationArmHeightSlider.step.set(0.5d);
        regstrationArmHeightSlider.min.set((double)AbstractCantileverBlock.MIN_HEIGHT);
        regstrationArmHeightSlider.max.set((double)AbstractCantileverBlock.MAX_HEIGHT);
        regstrationArmHeightSlider.value.set((double)this.catenaryHeight);
        regstrationArmHeightSlider.cursor.set(CursorType.HRESIZE);
        regstrationArmHeightSlider.addEventListener(DLGuiCommonEvents.ValueChangedEvent.class, (s, e) -> {
            this.catenaryHeight = (float)e.value();
            if (updatingValues.isFalse()) updateFunc.run();

            stateCache.clear();
            updateModelContext();
            return false;
        });
        addComponent(regstrationArmHeightSlider);


        
        insulatorPlacementSlider.value.set((double)this.insulatorPlacement.ordinal());
        insulatorPlacementSlider.cursor.set(CursorType.HRESIZE);
        insulatorPlacementSlider.addEventListener(DLGuiCommonEvents.ValueChangedEvent.class, (s, e) -> {
            this.insulatorPlacement = ECantileverInsulatorsPlacement.values()[(int)e.value()];
            stateCache.clear();
            updateModelContext();
            return false;
        });
        addComponent(insulatorPlacementSlider);

        registrationArmSlider.value.set((double)this.registrationArmType.ordinal());
        registrationArmSlider.cursor.set(CursorType.HRESIZE);
        registrationArmSlider.addEventListener(DLGuiCommonEvents.ValueChangedEvent.class, (s, e) -> {
            this.registrationArmType = ECantileverRegistrationArmType.values()[(int)e.value()];
            stateCache.clear();
            updateModelContext();
            return false;
        });
        addComponent(registrationArmSlider);
        
        bracingSlider.value.set(showBracing ? 1D : 0D);
        bracingSlider.cursor.set(CursorType.HRESIZE);
        bracingSlider.addEventListener(DLGuiCommonEvents.ValueChangedEvent.class, (s, e) -> {            
            this.showBracing = SupportTubeEnum.values()[(int)e.value()].get();
            if (updatingValues.isFalse()) updateFunc.run();

            stateCache.clear();
            updateModelContext();
            return false;
        });
        addComponent(bracingSlider);
        updateFunc.run();

        advancedOptionCb.text.set(txtAdvancedOptions);
        advancedOptionCb.checked.set(showAdvanced);
        advancedOptionCb.cursor.set(CursorType.HAND);
        advancedOptionCb.addEventListener(DLGuiCommonEvents.CheckedChangedEvent.class, (s, e) -> {
            showAdvanced = e.checked();
            relayoutFunc.run();
            return false;
        });
        addComponent(advancedOptionCb);
        
        relayoutFunc.run();
    }

    private static void distributeHorizontally(DLGuiComponent[] components, int centerX) {
        if (components == null || components.length <= 0) {
            return;
        }

        List<DLGuiComponent> visible = new ArrayList<>();
        for (DLGuiComponent c : components) {
            if (c != null && c.visible.get()) {
                visible.add(c);
            }
        }

        int count = visible.size();
        if (count == 0) {
            return;
        }

        int w = visible.get(0).width();
        int gap = 4;
        int blockWidth = count * w + (count - 1) * gap;
        double startX = centerX - blockWidth / 2.0;

        double currentX = startX;
        for (DLGuiComponent o : visible) {
            o.setX((int) Math.round(currentX));
            currentX += w + gap;
        }
    }


    
    private void updateModelContext() {
        this.context = ModelContext.builder()
            .with(CantileverBlockEntity.PROPERTY_WIDTH, width)
            .with(CantileverBlockEntity.PROPERTY_HEIGHT, height)
            .with(CantileverBlockEntity.PROPERTY_INSULATOR_PLACEMENT, insulatorPlacement)
            .with(CantileverBlockEntity.PROPERTY_REGISTRATION_ARM, registrationArmType)
            .with(CantileverBlockEntity.PROPERTY_CATENARY_HEIGHT, catenaryHeight)
            .with(CantileverBlockEntity.PROPERTY_USE_SUPPORT_TUBE, showBracing)
            .build();
    }
            
    @Override
    public void renderMainLayer(Graphics graphics, double mouseX, double mouseY, Rectangle renderBounds) {        
        GuiUtils.drawTexture(TEXTURE, graphics, 0, 0, GUI_WIDTH, GUI_HEIGHT, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        Lighting.setupForFlatItems();
        graphics.poseStack().pushPose();
        graphics.poseStack().setIdentity();
        graphics.poseStack().translate((x() + 30) + 24 * 4, y() + 54, 200);
        graphics.poseStack().pushPose();
        graphics.poseStack().scale(24, 24, -24);
        graphics.poseStack().mulPose(Axis.ZP.rotationDegrees(180));
        graphics.poseStack().mulPose(Axis.YP.rotationDegrees(90));
        graphics.poseStack().mulPose(Axis.YP.rotationDegrees((float)System.nanoTime() / (50000000f / 2)));
        graphics.poseStack().pushPose();
        graphics.poseStack().translate(0, 0, ((float)width - 1.5f) / 2f + ((width - 0.5f) % 2 == 0 ? 0.5f : 0));
        MultiBufferSource.BufferSource buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
        DLModel.renderModel(graphics.poseStack().last(), buffersource.getBuffer(RenderType.solid()), ModelType.BLOCK, stateCache.get(), context, 1, 1, 1, LightTexture.FULL_BRIGHT, 0);
        buffersource.endBatch();
        graphics.poseStack().popPose();
        graphics.poseStack().popPose();
        graphics.poseStack().popPose();
        Lighting.setupFor3DItems();

        graphics.poseStack().pushPose();
        graphics.poseStack().translate(0, 0, 500);
        GuiUtils.drawString(graphics, Minecraft.getInstance().font, width() / 2, 4, title, DragonLib.NATIVE_UI_FONT_COLOR, EAlignment.CENTER, false);
        graphics.poseStack().popPose();
    }
    
}
