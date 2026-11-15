package com.mrcrayfish.vehicle.compat.jei;

import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.crafting.FluidEntry;
import com.mrcrayfish.vehicle.crafting.FluidExtractorRecipe;
import com.mrcrayfish.vehicle.init.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
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
 * The fluids of the fluid extractor: the item put in the drop, burning fuel, gives the fluid.
 * Drawn with the extractor's own screen (its fuel slot, drop, arrow and tank).
 */
public class FluidExtractorCategory implements IRecipeCategory<FluidExtractorRecipe>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/fluid_extractor.png");

    /* The part of the screen shown, and the positions in it (screen position minus its corner) */
    private static final int PANEL_U = 26, PANEL_V = 10, WIDTH = 124, HEIGHT = 66;
    private static final int FUEL_X = 32 - PANEL_U, FUEL_Y = 33 - PANEL_V;
    private static final int SOURCE_X = 64 - PANEL_U, SOURCE_Y = 33 - PANEL_V;
    private static final int FLAME_X = 64 - PANEL_U, FLAME_Y = 53 - PANEL_V;
    private static final int ARROW_X = 93 - PANEL_U, ARROW_Y = 34 - PANEL_V;
    private static final int TANK_X = 127 - PANEL_U, TANK_Y = 14 - PANEL_V, TANK_HEIGHT = 59;

    private final IDrawable background;
    private final IDrawableAnimated flame;
    private final IDrawable tankOverlay;
    private final IDrawable icon;
    private final Component title;

    public FluidExtractorCategory(IGuiHelper guiHelper)
    {
        this.background = guiHelper.createDrawable(TEXTURE, PANEL_U, PANEL_V, WIDTH, HEIGHT);
        this.flame = guiHelper.drawableBuilder(TEXTURE, 176, 0, 14, 14).buildAnimated(300, IDrawableAnimated.StartDirection.TOP, true);
        this.tankOverlay = guiHelper.createDrawable(TEXTURE, 176, 44, 16, TANK_HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.FLUID_EXTRACTOR.get()));
        this.title = Component.translatable(ModBlocks.FLUID_EXTRACTOR.get().getDescriptionId());
    }

    @Override
    public RecipeType<FluidExtractorRecipe> getRecipeType()
    {
        return VehicleJeiPlugin.FLUID_EXTRACTOR;
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
    public void setRecipe(IRecipeLayoutBuilder builder, FluidExtractorRecipe recipe, IFocusGroup focuses)
    {
        builder.addSlot(RecipeIngredientRole.INPUT, SOURCE_X, SOURCE_Y).addItemStack(recipe.getIngredient());
        FluidEntry result = recipe.getResult();
        builder.addSlot(RecipeIngredientRole.OUTPUT, TANK_X, TANK_Y)
                .setFluidRenderer(result.getAmount(), false, 16, TANK_HEIGHT)
                .setOverlay(this.tankOverlay, 0, 0)
                .addFluidStack(result.getFluid(), result.getAmount());
    }

    @Override
    public void draw(FluidExtractorRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY)
    {
        this.background.draw(graphics);
        this.flame.draw(graphics, FLAME_X, FLAME_Y);
        int time = JeiHelper.config(Config.SERVER.extractorExtractTime::get, 600);
        JeiHelper.drawText(graphics, Component.translatable("jei.vehicle_restored.time", JeiHelper.seconds(time)), ARROW_X + 12, ARROW_Y + 19, 40, true);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, FluidExtractorRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY)
    {
        if(JeiHelper.inside(mouseX, mouseY, FUEL_X, FUEL_Y, 18, 18) || JeiHelper.inside(mouseX, mouseY, FLAME_X, FLAME_Y, 14, 14))
        {
            tooltip.add(Component.translatable("jei.vehicle_restored.fuel"));
            tooltip.add(Component.translatable("jei.vehicle_restored.fuel.info").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getRegistryName(FluidExtractorRecipe recipe)
    {
        return recipe.getId();
    }
}
