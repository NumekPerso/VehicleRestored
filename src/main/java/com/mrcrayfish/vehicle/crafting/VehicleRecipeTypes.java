package com.mrcrayfish.vehicle.crafting;

import com.mrcrayfish.vehicle.Reference;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Author: MrCrayfish
 */
public class VehicleRecipeTypes
{
    public static final DeferredRegister<RecipeType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Reference.MOD_ID);

    public static final RegistryObject<RecipeType<FluidExtractorRecipe>> FLUID_EXTRACTOR = register("fluid_extractor");
    public static final RegistryObject<RecipeType<FluidMixerRecipe>> FLUID_MIXER = register("fluid_mixer");
    public static final RegistryObject<RecipeType<WorkstationRecipe>> WORKSTATION = register("workstation");

    private static <T extends Recipe<?>> RegistryObject<RecipeType<T>> register(String name)
    {
        return REGISTER.register(name, () -> RecipeType.simple(new net.minecraft.resources.ResourceLocation(Reference.MOD_ID, name)));
    }
}
