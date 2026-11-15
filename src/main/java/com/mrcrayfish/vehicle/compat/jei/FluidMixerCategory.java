package com.mrcrayfish.vehicle.compat.jei;

import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.crafting.FluidEntry;
import com.mrcrayfish.vehicle.crafting.FluidMixerRecipe;
import com.mrcrayfish.vehicle.init.ModBlocks;
import com.mrcrayfish.vehicle.init.ModFluids;
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
 * The fluids of the fluid mixer: two fluids and an item, burning fuel, give a third fluid (fuelium).
 * Drawn with the mixer's own screen (its two input tanks, the pipes, the item slot and the tank).
 */
public class FluidMixerCategory implements IRecipeCategory<FluidMixerRecipe>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/fluid_mixer.png");

    /* The part of the screen shown, and the positions in it (screen position minus its corner) */
    private static final int PANEL_U = 4, PANEL_V = 12, WIDTH = 168, HEIGHT = 74;
    private static final int FUEL_X = 8 - PANEL_U, FUEL_Y = 49 - PANEL_V;
    private static final int FLAME_X = 9 - PANEL_U, FLAME_Y = 31 - PANEL_V;
    private static final int INPUT_X = 33 - PANEL_U, INPUT_TOP_Y = 17 - PANEL_V, INPUT_BOTTOM_Y = 52 - PANEL_V, INPUT_HEIGHT = 29;
    private static final int INGREDIENT_X = 103 - PANEL_U, INGREDIENT_Y = 41 - PANEL_V;
    private static final int OUTPUT_X = 151 - PANEL_U, OUTPUT_Y = 20 - PANEL_V, OUTPUT_HEIGHT = 59;
    private static final int TIME_Y = 64 - PANEL_V;

    private final IDrawable background;
    private final IDrawableAnimated flame;
    private final IDrawable smallTankOverlay;
    private final IDrawable tankOverlay;
    private final IDrawable icon;
    private final Component title;

    public FluidMixerCategory(IGuiHelper guiHelper)
    {
        this.background = guiHelper.createDrawable(TEXTURE, PANEL_U, PANEL_V, WIDTH, HEIGHT);
        this.flame = guiHelper.drawableBuilder(TEXTURE, 176, 0, 14, 14).buildAnimated(300, IDrawableAnimated.StartDirection.TOP, true);
        this.smallTankOverlay = guiHelper.createDrawable(TEXTURE, 176, 44, 16, INPUT_HEIGHT);
        this.tankOverlay = guiHelper.createDrawable(TEXTURE, 176, 44, 16, OUTPUT_HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.FLUID_MIXER.get()));
        this.title = Component.translatable(ModBlocks.FLUID_MIXER.get().getDescriptionId());
    }

    @Override
    public RecipeType<FluidMixerRecipe> getRecipeType()
    {
        return VehicleJeiPlugin.FLUID_MIXER;
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
    public void setRecipe(IRecipeLayoutBuilder builder, FluidMixerRecipe recipe, IFocusGroup focuses)
    {
        FluidEntry[] inputs = recipe.getInputs();
        FluidEntry result = recipe.getResult();
        // blaze juice in the top tank, as in the mixer's screen (its "blaze" tank)
        boolean swap = inputs[1].getFluid() == ModFluids.BLAZE_JUICE.get();
        FluidEntry top = swap ? inputs[1] : inputs[0];
        FluidEntry bottom = swap ? inputs[0] : inputs[1];
        // the tanks on the same scale, so the amounts compare at a glance
        long capacity = Math.max(result.getAmount(), Math.max(top.getAmount(), bottom.getAmount()));
        this.addTank(builder, RecipeIngredientRole.INPUT, INPUT_X, INPUT_TOP_Y, INPUT_HEIGHT, this.smallTankOverlay, capacity, top);
        this.addTank(builder, RecipeIngredientRole.INPUT, INPUT_X, INPUT_BOTTOM_Y, INPUT_HEIGHT, this.smallTankOverlay, capacity, bottom);
        builder.addSlot(RecipeIngredientRole.INPUT, INGREDIENT_X, INGREDIENT_Y).addItemStack(recipe.getIngredient());
        this.addTank(builder, RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y, OUTPUT_HEIGHT, this.tankOverlay, capacity, result);
    }

    private void addTank(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, int height, IDrawable overlay, long capacity, FluidEntry fluid)
    {
        builder.addSlot(role, x, y)
                .setFluidRenderer(capacity, false, 16, height)
                .setOverlay(overlay, 0, 0)
                .addFluidStack(fluid.getFluid(), fluid.getAmount());
    }

    @Override
    public void draw(FluidMixerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY)
    {
        this.background.draw(graphics);
        this.flame.draw(graphics, FLAME_X, FLAME_Y);
        int time = JeiHelper.config(Config.SERVER.mixerMixTime::get, 100);
        JeiHelper.drawText(graphics, Component.translatable("jei.vehicle_restored.time", JeiHelper.seconds(time)), INGREDIENT_X + 8, TIME_Y, 40, true);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, FluidMixerRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY)
    {
        if(JeiHelper.inside(mouseX, mouseY, FUEL_X, FUEL_Y, 18, 18) || JeiHelper.inside(mouseX, mouseY, FLAME_X, FLAME_Y, 14, 14))
        {
            tooltip.add(Component.translatable("jei.vehicle_restored.fuel"));
            tooltip.add(Component.translatable("jei.vehicle_restored.fuel.info").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getRegistryName(FluidMixerRecipe recipe)
    {
        return recipe.getId();
    }
}
