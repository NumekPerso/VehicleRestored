package com.mrcrayfish.vehicle.compat.jei;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.block.VehicleCrateBlock;
import com.mrcrayfish.vehicle.crafting.WorkstationIngredient;
import com.mrcrayfish.vehicle.crafting.WorkstationRecipe;
import com.mrcrayfish.vehicle.entity.EngineType;
import com.mrcrayfish.vehicle.entity.IEngineType;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import com.mrcrayfish.vehicle.entity.VehicleProperties;
import com.mrcrayfish.vehicle.init.ModBlocks;
import com.mrcrayfish.vehicle.item.EngineItem;
import com.mrcrayfish.vehicle.item.WheelItem;
import java.util.Arrays;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The vehicles of the workstation: the materials with their counts (taken from the inventory), the
 * parts put in the workstation's slots (paint, engine of the right type, wheels) and the crate.
 */
public class WorkstationCategory implements IRecipeCategory<WorkstationRecipe>
{
    private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/workstation.png");

    private static final int COLUMNS = 4, ROWS = 3;
    private static final int GRID_Y = 11;
    private static final int PARTS_X = COLUMNS * 18 + 6;
    private static final int ARROW_X = PARTS_X + 18 + 5, ARROW_Y = GRID_Y + ROWS * 9 - 8;
    private static final int OUTPUT_X = ARROW_X + 24 + 5, OUTPUT_Y = GRID_Y + ROWS * 9 - 13;

    private static final int WIDTH = OUTPUT_X + 26;
    private static final int HEIGHT = GRID_Y + ROWS * 18;

    /* The part slots, in the order of the workstation: paint, engine, wheels */
    private static final int PAINT = 0, ENGINE = 1, WHEELS = 2;

    private final IDrawable partSlot;
    private final IDrawable notApplicable;
    private final IDrawable arrow;
    private final IDrawable icon;
    private final Component title;

    public WorkstationCategory(IGuiHelper guiHelper)
    {
        this.partSlot = guiHelper.createDrawable(TEXTURE, 198, 20, 18, 18);
        this.notApplicable = guiHelper.createDrawable(TEXTURE, 164, 232, 16, 16);
        this.arrow = guiHelper.getRecipeArrow();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.WORKSTATION.get()));
        this.title = Component.translatable(ModBlocks.WORKSTATION.get().getDescriptionId());
    }

    @Override
    public RecipeType<WorkstationRecipe> getRecipeType()
    {
        return VehicleJeiPlugin.WORKSTATION;
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
    public void setRecipe(IRecipeLayoutBuilder builder, WorkstationRecipe recipe, IFocusGroup focuses)
    {
        List<WorkstationIngredient> materials = recipe.getMaterials();
        for(int i = 0; i < materials.size(); i++)
        {
            List<ItemStack> stacks = JeiHelper.withCount(materials.get(i));
            if(i < COLUMNS * ROWS)
            {
                builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % COLUMNS) * 18, GRID_Y + 1 + (i / COLUMNS) * 18)
                        .setStandardSlotBackground()
                        .addItemStacks(stacks);
            }
            else
            {
                builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(stacks);
            }
        }

        Parts parts = Parts.of(recipe.getVehicle());
        if(parts.colored())
        {
            // optional, and only changes the colour: shown, but not a use of the dyes
            List<ItemStack> dyes = Arrays.stream(DyeColor.values()).map(color -> new ItemStack(DyeItem.byColor(color))).toList();
            this.addPartSlot(builder, RecipeIngredientRole.RENDER_ONLY, PAINT, dyes,
                    Component.translatable("vehicle_restored.tooltip.optional").withStyle(ChatFormatting.AQUA),
                    Component.translatable("vehicle_restored.tooltip.paint_color").withStyle(ChatFormatting.GRAY));
        }
        if(parts.engine() != EngineType.NONE)
        {
            List<ItemStack> engines = ForgeRegistries.ITEMS.getValues().stream()
                    .filter(item -> item instanceof EngineItem engine && engine.getEngineType() == parts.engine())
                    .map(ItemStack::new).toList();
            this.addPartSlot(builder, RecipeIngredientRole.INPUT, ENGINE, engines,
                    Component.translatable("vehicle_restored.tooltip.required").withStyle(ChatFormatting.RED),
                    parts.engine().getEngineName().copy().withStyle(ChatFormatting.GRAY));
        }
        if(parts.wheels())
        {
            List<ItemStack> wheels = ForgeRegistries.ITEMS.getValues().stream()
                    .filter(item -> item instanceof WheelItem)
                    .map(ItemStack::new).toList();
            this.addPartSlot(builder, RecipeIngredientRole.INPUT, WHEELS, wheels,
                    Component.translatable("vehicle_restored.tooltip.required").withStyle(ChatFormatting.RED),
                    Component.translatable("vehicle_restored.tooltip.wheels").withStyle(ChatFormatting.GRAY));
        }

        // the crate as the workstation makes it (default colour; the engine and wheels go inside)
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(recipe.getVehicle());
        if(id != null)
        {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 5, OUTPUT_Y + 5)
                    .setOutputSlotBackground()
                    .addItemStack(VehicleCrateBlock.create(id, VehicleEntity.DYE_TO_COLOR[0], ItemStack.EMPTY, ItemStack.EMPTY));
        }
    }

    private void addPartSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int part, List<ItemStack> stacks, Component state, Component name)
    {
        builder.addSlot(role, PARTS_X + 1, GRID_Y + 1 + part * 18)
                .setBackground(this.partSlot, -1, -1)
                .addItemStacks(stacks)
                .addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(state);
                    tooltip.add(name);
                });
    }

    @Override
    public void draw(WorkstationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY)
    {
        JeiHelper.drawText(graphics, recipe.getVehicle().getDescription(), 1, 1, WIDTH - 2, false);
        this.arrow.draw(graphics, ARROW_X, ARROW_Y);

        // the part slots the vehicle doesn't use, crossed as in the workstation
        Parts parts = Parts.of(recipe.getVehicle());
        for(int part = PAINT; part <= WHEELS; part++)
        {
            if(!parts.uses(part))
            {
                int y = GRID_Y + part * 18;
                this.partSlot.draw(graphics, PARTS_X, y);
                this.notApplicable.draw(graphics, PARTS_X + 1, y + 1);
            }
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, WorkstationRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY)
    {
        Parts parts = Parts.of(recipe.getVehicle());
        for(int part = PAINT; part <= WHEELS; part++)
        {
            if(!parts.uses(part) && JeiHelper.inside(mouseX, mouseY, PARTS_X, GRID_Y + part * 18, 18, 18))
            {
                String name = part == PAINT ? "paint_color" : part == ENGINE ? "engine" : "wheels";
                tooltip.add(Component.translatable("vehicle_restored.tooltip." + name));
                tooltip.add(Component.translatable("vehicle_restored.tooltip.not_applicable").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public ResourceLocation getRegistryName(WorkstationRecipe recipe)
    {
        return recipe.getId();
    }

    /** The parts a vehicle takes in the workstation, from its properties (none if it has none) */
    private record Parts(boolean colored, IEngineType engine, boolean wheels)
    {
        private static final Parts NONE = new Parts(false, EngineType.NONE, false);

        static Parts of(EntityType<?> vehicle)
        {
            try
            {
                VehicleProperties properties = VehicleProperties.get(vehicle);
                IEngineType engine = properties.getEngineType() != null ? properties.getEngineType() : EngineType.NONE;
                return new Parts(properties.isColored(), engine, properties.canChangeWheels());
            }
            catch(RuntimeException e)
            {
                return NONE;
            }
        }

        boolean uses(int part)
        {
            return switch(part)
            {
                case PAINT -> this.colored;
                case ENGINE -> this.engine != EngineType.NONE;
                default -> this.wheels;
            };
        }
    }
}
