package com.mrcrayfish.vehicle.compat.jei;

import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.client.screen.AdvancedWorkshopScreen;
import com.mrcrayfish.vehicle.client.screen.FluidExtractorScreen;
import com.mrcrayfish.vehicle.client.screen.FluidMixerScreen;
import com.mrcrayfish.vehicle.crafting.AdvancedWorkshopRecipe;
import com.mrcrayfish.vehicle.crafting.FluidExtractorRecipe;
import com.mrcrayfish.vehicle.crafting.FluidMixerRecipe;
import com.mrcrayfish.vehicle.crafting.VehicleRecipeTypes;
import com.mrcrayfish.vehicle.crafting.WorkstationRecipe;
import com.mrcrayfish.vehicle.init.ModBlocks;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * JEI integration (optional: JEI only loads this class when it is installed). Shows the recipes of
 * the mod's own machines, which JEI can't read by itself: the advanced workshop, the workstation, the
 * fluid extractor and the fluid mixer, each with its machine as the catalyst. The crafting table
 * recipes of the mod are vanilla ones and already show without this.
 */
@JeiPlugin
public class VehicleJeiPlugin implements IModPlugin
{
    private static final ResourceLocation UID = new ResourceLocation(Reference.MOD_ID, "jei_plugin");

    public static final RecipeType<AdvancedWorkshopRecipe> ADVANCED_WORKSHOP = RecipeType.create(Reference.MOD_ID, "advanced_workshop", AdvancedWorkshopRecipe.class);
    public static final RecipeType<WorkstationRecipe> WORKSTATION = RecipeType.create(Reference.MOD_ID, "workstation", WorkstationRecipe.class);
    public static final RecipeType<FluidExtractorRecipe> FLUID_EXTRACTOR = RecipeType.create(Reference.MOD_ID, "fluid_extractor", FluidExtractorRecipe.class);
    public static final RecipeType<FluidMixerRecipe> FLUID_MIXER = RecipeType.create(Reference.MOD_ID, "fluid_mixer", FluidMixerRecipe.class);

    @Override
    public ResourceLocation getPluginUid()
    {
        return UID;
    }

    /** A crate is one entry per vehicle inside (the vehicle's crate is what the machines make) */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration)
    {
        registration.registerSubtypeInterpreter(ModBlocks.VEHICLE_CRATE.get().asItem(), CrateVehicleInterpreter.INSTANCE);
        registration.registerSubtypeInterpreter(ModBlocks.MILITARY_CRATE.get().asItem(), CrateVehicleInterpreter.INSTANCE);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration)
    {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new AdvancedWorkshopCategory(guiHelper),
                new WorkstationCategory(guiHelper),
                new FluidExtractorCategory(guiHelper),
                new FluidMixerCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if(level == null)
            return;
        RecipeManager manager = level.getRecipeManager();
        registration.addRecipes(ADVANCED_WORKSHOP, AdvancedWorkshopRecipe.list(manager));
        registration.addRecipes(WORKSTATION, workstationRecipes(manager));
        registration.addRecipes(FLUID_EXTRACTOR, recipes(manager, VehicleRecipeTypes.FLUID_EXTRACTOR.get(), FluidExtractorRecipe.class));
        registration.addRecipes(FLUID_MIXER, recipes(manager, VehicleRecipeTypes.FLUID_MIXER.get(), FluidMixerRecipe.class));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
    {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ADVANCED_WORKSHOP.get()), ADVANCED_WORKSHOP);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.WORKSTATION.get()), WORKSTATION);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FLUID_EXTRACTOR.get()), FLUID_EXTRACTOR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FLUID_MIXER.get()), FLUID_MIXER);
    }

    /** Clicking the arrow (or the pipe) of a machine's screen shows its recipes */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration)
    {
        registration.addRecipeClickArea(AdvancedWorkshopScreen.class, 224, 124, 18, 13, ADVANCED_WORKSHOP);
        registration.addRecipeClickArea(FluidExtractorScreen.class, 93, 34, 23, 16, FLUID_EXTRACTOR);
        registration.addRecipeClickArea(FluidMixerScreen.class, 123, 45, 27, 8, FLUID_MIXER);
    }

    /** The vehicles of the workstation, as its screen lists them (without the ones the server disabled) */
    private static List<WorkstationRecipe> workstationRecipes(RecipeManager manager)
    {
        List<? extends String> disabled;
        try
        {
            disabled = Config.SERVER.disabledVehicles.get();
        }
        catch(RuntimeException e)
        {
            disabled = List.of();
        }
        List<? extends String> hidden = disabled;
        return recipes(manager, VehicleRecipeTypes.WORKSTATION.get(), WorkstationRecipe.class).stream()
                .filter(recipe -> recipe.getVehicle() != null)
                .filter(recipe -> !hidden.contains(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(recipe.getVehicle()))))
                .sorted(Comparator.comparing(recipe -> Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(recipe.getVehicle())).getPath()))
                .toList();
    }

    private static <T> List<T> recipes(RecipeManager manager, net.minecraft.world.item.crafting.RecipeType<?> type, Class<T> recipeClass)
    {
        return manager.getRecipes().stream()
                .filter(recipe -> recipe.getType() == type)
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
                .filter(recipeClass::isInstance)
                .map(recipeClass::cast)
                .toList();
    }

    /** The vehicle in a crate, from its block entity data: a crate per vehicle in JEI's lists */
    private static class CrateVehicleInterpreter implements ISubtypeInterpreter<ItemStack>
    {
        private static final CrateVehicleInterpreter INSTANCE = new CrateVehicleInterpreter();

        @Nullable
        @Override
        public Object getSubtypeData(ItemStack stack, UidContext context)
        {
            CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
            if(blockEntityTag == null)
                return null;
            String vehicle = blockEntityTag.getString("Vehicle");
            return vehicle.isEmpty() ? null : vehicle;
        }

        @Override
        public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context)
        {
            Object vehicle = this.getSubtypeData(stack, context);
            return vehicle != null ? vehicle.toString() : "";
        }
    }
}
