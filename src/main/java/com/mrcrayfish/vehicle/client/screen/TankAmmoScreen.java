package com.mrcrayfish.vehicle.client.screen;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.entity.vehicle.TankAmmo;
import com.mrcrayfish.vehicle.entity.vehicle.TankEntity;
import com.mrcrayfish.vehicle.inventory.container.TankAmmoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Screen of the tank's store (TankAmmoContainer), in the look of the tank: a panel painted in the tank's
 * camouflage with dark steel modules (tools/concepts/tank/items.js draws the background, 309 x 204 in a
 * 512 x 256 texture). On the left one module per ammunition (the icon of its weapon on a raised plate, its 4
 * slots with the outline of the ammunition when empty, its name, the rounds the tank has and a gauge leaning
 * like the bars of the HUD), the "load all" button and the player's inventory; on the right the armour: the
 * tank seen from above with a socket for the plate of each face, and the armour left in percent.
 */
@OnlyIn(Dist.CLIENT)
public class TankAmmoScreen extends AbstractContainerScreen<TankAmmoContainer>
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation(Reference.MOD_ID, "textures/gui/container/tank.png");
    private static final int TEXTURE_WIDTH = 512, TEXTURE_HEIGHT = 256;
    private static final ResourceLocation[] EMPTY_SLOT_ICONS = {
        new ResourceLocation(Reference.MOD_ID, "textures/gui/container/tank_slot_shell.png"),
        new ResourceLocation(Reference.MOD_ID, "textures/gui/container/tank_slot_light_shells.png"),
        new ResourceLocation(Reference.MOD_ID, "textures/gui/container/tank_slot_belt.png")
    };
    private static final ResourceLocation EMPTY_PLATE_ICON = new ResourceLocation(Reference.MOD_ID, "textures/gui/container/tank_slot_armor_plate.png");
    /** Icon of the weapon that fires each ammunition, on the plate of its module */
    private static final ResourceLocation[] WEAPON_ICONS = {
        new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/weapon_cannon.png"),
        new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/weapon_light_shells.png"),
        new ResourceLocation(Reference.MOD_ID, "textures/gui/tank/weapon_coax.png")
    };
    private static final int TITLE_COLOR = 0xE3C04A;
    private static final int LABEL_COLOR = 0xD6CFA6;
    private static final int COUNT_COLOR = 0xF2D35B;
    private static final int EMPTY_COLOR = 0xE0533F;
    /**
     * Gauge of each module: 12 segments of 6 pixels, 1 apart, on 4 rows each one pixel further right than the
     * row below (its outline and track are in the background)
     */
    private static final int GAUGE_SEGMENTS = 12, GAUGE_SEGMENT = 6;
    private static final int GAUGE_LIT = 0xFFD9B12E, GAUGE_LIT_TOP = 0xFFF2D676, GAUGE_LIT_BOTTOM = 0xFFA8892A, GAUGE_OFF = 0xFF2C2D2A;
    /** Armour module: its label and the armour left (percent) right-aligned to ARMOR_RIGHT */
    private static final int ARMOR_LABEL_X = 215, ARMOR_LABEL_Y = 25, ARMOR_RIGHT = 298;
    /** "Load all" button: 84 x 13, its 3 states (normal, mouse over, nothing to load) in the texture at (320, 0) */
    private static final int BUTTON_X = 123, BUTTON_Y = 100, BUTTON_WIDTH = 84, BUTTON_HEIGHT = 13, BUTTON_U = 320;
    private static final int[] BUTTON_TEXT_COLORS = {0xD6CFA6, 0xFFF4C8, 0x7A7C70};

    public TankAmmoScreen(TankAmmoContainer menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 309;
        this.imageHeight = 204;
        this.titleLabelX = 10;
        this.titleLabelY = 5;
        this.inventoryLabelX = 10;
        this.inventoryLabelY = 103;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
        if(this.isOverButton(mouseX, mouseY))
        {
            graphics.renderTooltip(this.font, Component.translatable("gui.vehicle_restored.tank.load_all.info"), mouseX, mouseY);
        }
        else if(this.hoveredSlot instanceof TankAmmoContainer.ArmorSlot armorSlot && !armorSlot.hasItem() && this.menu.getCarried().isEmpty())
        {
            graphics.renderTooltip(this.font, Component.translatable("gui.vehicle_restored.tank.armor.empty", Component.translatable(armorSlot.getFace().getTranslationKey())), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
    {
        int left = this.leftPos, top = this.topPos;
        graphics.blit(BACKGROUND, left, top, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        TankEntity tank = this.menu.getTank();
        for(TankAmmo ammo : TankAmmo.values())
        {
            int x = left + TankAmmoContainer.MODULE_X, y = top + TankAmmoContainer.MODULE_Y + ammo.ordinal() * TankAmmoContainer.MODULE_SPACING;
            graphics.blit(WEAPON_ICONS[ammo.ordinal()], x + 5, y + 4, 16, 16, 0, 0, 32, 32, 32, 32);
            // gauge: how full the row is (the belt or pack in use included)
            int rounds = tank != null ? tank.getAmmoCount(ammo) : 0;
            float full = Mth.clamp(rounds / (float) Math.max(1, ammo.getCapacity()), 0F, 1F);
            int lit = rounds > 0 ? Math.max(1, Math.round(full * GAUGE_SEGMENTS)) : 0;
            for(int row = 1; row <= 4; row++)
            {
                int sy = y + 14 + row, rowLeft = x + 105 + (5 - row);
                for(int s = 0; s < GAUGE_SEGMENTS; s++)
                {
                    int sx = rowLeft + 2 + s * (GAUGE_SEGMENT + 1);
                    int color = s >= lit ? GAUGE_OFF : row == 1 ? GAUGE_LIT_TOP : row == 4 ? GAUGE_LIT_BOTTOM : GAUGE_LIT;
                    graphics.fill(sx, sy, sx + GAUGE_SEGMENT, sy + 1, color);
                }
            }
        }
        for(Slot slot : this.menu.slots)
        {
            if(slot.hasItem())
                continue;
            if(slot instanceof TankAmmoContainer.AmmoSlot ammoSlot)
            {
                graphics.blit(EMPTY_SLOT_ICONS[ammoSlot.getAmmo().ordinal()], left + slot.x, top + slot.y, 0, 0, 16, 16, 16, 16);
            }
            else if(slot instanceof TankAmmoContainer.ArmorSlot)
            {
                graphics.blit(EMPTY_PLATE_ICON, left + slot.x, top + slot.y, 0, 0, 16, 16, 16, 16);
            }
        }
        int state = !this.menu.canLoadAll() ? 2 : this.isOverButton(mouseX, mouseY) ? 1 : 0;
        graphics.blit(BACKGROUND, left + BUTTON_X, top + BUTTON_Y, BUTTON_U, state * BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TITLE_COLOR, true);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, true);
        TankEntity tank = this.menu.getTank();
        for(TankAmmo ammo : TankAmmo.values())
        {
            int x = TankAmmoContainer.MODULE_X, y = TankAmmoContainer.MODULE_Y + ammo.ordinal() * TankAmmoContainer.MODULE_SPACING;
            graphics.drawString(this.font, Component.translatable(ammo.getTranslationKey()), x + 106, y + 5, LABEL_COLOR, true);
            int rounds = tank != null ? tank.getAmmoCount(ammo) : 0;
            String count = String.valueOf(rounds);
            graphics.drawString(this.font, count, x + 196 - this.font.width(count), y + 5, rounds > 0 ? COUNT_COLOR : EMPTY_COLOR, true);
        }
        // armour: what the plates have left, out of 5 new plates
        graphics.drawString(this.font, Component.translatable("gui.vehicle_restored.tank.armor"), ARMOR_LABEL_X, ARMOR_LABEL_Y, LABEL_COLOR, true);
        float armor = tank != null && tank.getMaxArmor() > 0 ? tank.getArmor() / tank.getMaxArmor() : 0F;
        String percent = Component.translatable("gui.vehicle_restored.tank.percent", Math.round(armor * 100F)).getString();
        graphics.drawString(this.font, percent, ARMOR_RIGHT - this.font.width(percent), ARMOR_LABEL_Y, armor > 0F ? COUNT_COLOR : EMPTY_COLOR, true);
        // the text of the button
        int state = !this.menu.canLoadAll() ? 2 : this.isOverButton(mouseX, mouseY) ? 1 : 0;
        graphics.drawString(this.font, Component.translatable("gui.vehicle_restored.tank.load_all"), BUTTON_X + 13, BUTTON_Y + 3, BUTTON_TEXT_COLORS[state], state != 2);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if(button == 0 && this.isOverButton(mouseX, mouseY) && this.menu.canLoadAll() && this.minecraft != null && this.minecraft.gameMode != null)
        {
            // the server moves the items (TankAmmoContainer.clickMenuButton) and sends the slots back
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, TankAmmoContainer.BUTTON_LOAD_ALL);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOverButton(double mouseX, double mouseY)
    {
        double x = mouseX - this.leftPos, y = mouseY - this.topPos;
        return x >= BUTTON_X && x < BUTTON_X + BUTTON_WIDTH && y >= BUTTON_Y && y < BUTTON_Y + BUTTON_HEIGHT;
    }
}
