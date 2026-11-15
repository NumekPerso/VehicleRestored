package com.mrcrayfish.vehicle.compat.jei;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.crafting.AdvancedWorkshopRecipe;
import com.mrcrayfish.vehicle.crafting.WorkstationIngredient;
import com.mrcrayfish.vehicle.init.ModBlocks;
import com.mrcrayfish.vehicle.inventory.container.AdvancedWorkshopContainer;
import com.mrcrayfish.vehicle.tileentity.AdvancedWorkshopTileEntity;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The recipes of the advanced workshop: the ingredients with their counts in the machine's input
 * slots, its output slot with the crate (or the items), and below the energy per tick, the time
 * (all the phases) and the energy of the whole work. The phases are in the tooltip of the arrow.
 */
public class AdvancedWorkshopCategory implements IRecipeCategory<AdvancedWorkshopRecipe>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/container/advanced_workshop.png");
    private static final int TEXTURE_SIZE = 512;

    /* The part of the workshop screen shown: input slots, arrow, output slot and energy gauge */
    private static final int PANEL_U = 112, PANEL_V = 111, PANEL_WIDTH = 168, PANEL_HEIGHT = 42;
    private static final int PANEL_Y = 11;

    private static final int INPUT_X = AdvancedWorkshopContainer.INPUT_X - PANEL_U;
    private static final int INPUT_Y = AdvancedWorkshopContainer.INPUT_Y - PANEL_V + PANEL_Y;
    private static final int OUTPUT_X = AdvancedWorkshopContainer.OUTPUT_X - PANEL_U;
    private static final int OUTPUT_Y = AdvancedWorkshopContainer.OUTPUT_Y - PANEL_V + PANEL_Y;
    private static final int ARROW_X = 224 - PANEL_U, ARROW_Y = 124 - PANEL_V + PANEL_Y, ARROW_WIDTH = 18, ARROW_HEIGHT = 13;
    private static final int GAUGE_X = 267 - PANEL_U, GAUGE_Y = 115 - PANEL_V + PANEL_Y, GAUGE_WIDTH = 9, GAUGE_HEIGHT = 34;
    private static final int INFO_Y = PANEL_Y + PANEL_HEIGHT + 2;

    private static final int WIDTH = PANEL_WIDTH;
    private static final int HEIGHT = INFO_Y + 19;

    private final IDrawable panel;
    private final IDrawable icon;
    private final Component title;

    public AdvancedWorkshopCategory(IGuiHelper guiHelper)
    {
        this.panel = guiHelper.drawableBuilder(TEXTURE, PANEL_U, PANEL_V, PANEL_WIDTH, PANEL_HEIGHT).setTextureSize(TEXTURE_SIZE, TEXTURE_SIZE).build();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.ADVANCED_WORKSHOP.get()));
        this.title = Component.translatable(ModBlocks.ADVANCED_WORKSHOP.get().getDescriptionId());
    }

    @Override
    public RecipeType<AdvancedWorkshopRecipe> getRecipeType()
    {
        return VehicleJeiPlugin.ADVANCED_WORKSHOP;
    }

    @Override
    public Component getTitle()
    {
        return this.title;
    }

    @Override
    public int getWidth()
    {
        return WIDTH;
    }

    @Override
    public int getHeight()
    {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon()
    {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AdvancedWorkshopRecipe recipe, IFocusGroup focuses)
    {
        List<WorkstationIngredient> materials = recipe.getMaterials();
        for(int i = 0; i < materials.size(); i++)
        {
            List<ItemStack> stacks = JeiHelper.withCount(materials.get(i));
            if(i < AdvancedWorkshopTileEntity.INPUT_SLOTS)
            {
                int column = i % AdvancedWorkshopContainer.INPUT_COLUMNS;
                int row = i / AdvancedWorkshopContainer.INPUT_COLUMNS;
                builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X + column * 18, INPUT_Y + row * 18).addItemStacks(stacks);
            }
            else
            {
                // more ingredients than the machine has slots: still found by the lookups
                builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(stacks);
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y).addItemStack(recipe.getResult());
    }

    @Override
    public void draw(AdvancedWorkshopRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY)
    {
        this.panel.draw(graphics, 0, PANEL_Y);

        // the gauge full, as the sign of the energy the work takes (details in its tooltip)
        graphics.fill(GAUGE_X, GAUGE_Y, GAUGE_X + GAUGE_WIDTH, GAUGE_Y + GAUGE_HEIGHT, 0xFF5FCF5A);
        graphics.fill(GAUGE_X, GAUGE_Y, GAUGE_X + 1, GAUGE_Y + GAUGE_HEIGHT, 0xFF8EE58A);
        graphics.fill(GAUGE_X, GAUGE_Y, GAUGE_X + GAUGE_WIDTH, GAUGE_Y + 1, 0xFF8EE58A);

        JeiHelper.drawText(graphics, recipe.getDisplayName(), 1, 1, WIDTH - 2, false);
        JeiHelper.drawText(graphics, info(recipe), 1, INFO_Y, WIDTH - 2, false);
        JeiHelper.drawText(graphics, Component.translatable("jei.vehicle_restored.advanced_workshop.total_energy", totalEnergy(recipe)), 1, INFO_Y + 10, WIDTH - 2, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, AdvancedWorkshopRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY)
    {
        if(JeiHelper.inside(mouseX, mouseY, ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT) || JeiHelper.inside(mouseX, mouseY, 0, INFO_Y - 1, WIDTH, 10))
        {
            tooltip.add(Component.translatable("jei.vehicle_restored.advanced_workshop.phases"));
            for(AdvancedWorkshopRecipe.Phase phase : recipe.getPhases())
            {
                tooltip.add(Component.translatable("jei.vehicle_restored.advanced_workshop.phase", phase.getDisplayName(), JeiHelper.seconds(phase.ticks())).withStyle(ChatFormatting.GRAY));
            }
        }
        else if(JeiHelper.inside(mouseX, mouseY, GAUGE_X - 1, GAUGE_Y - 1, GAUGE_WIDTH + 2, GAUGE_HEIGHT + 2) || JeiHelper.inside(mouseX, mouseY, 0, INFO_Y + 9, WIDTH, 10))
        {
            tooltip.add(Component.translatable("jei.vehicle_restored.advanced_workshop.energy", recipe.getEnergyPerTick(), totalEnergy(recipe)));
            tooltip.add(Component.translatable("jei.vehicle_restored.advanced_workshop.energy.info").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getRegistryName(AdvancedWorkshopRecipe recipe)
    {
        return recipe.getId();
    }

    private static Component info(AdvancedWorkshopRecipe recipe)
    {
        return Component.translatable("gui.vehicle_restored.advanced_workshop.info", recipe.getEnergyPerTick(), JeiHelper.seconds(recipe.getTotalTicks()));
    }

    /** All the energy the work takes, formatted as the workshop screen does */
    private static String totalEnergy(AdvancedWorkshopRecipe recipe)
    {
        return String.format("%,d", (long) recipe.getEnergyPerTick() * recipe.getTotalTicks());
    }
}
