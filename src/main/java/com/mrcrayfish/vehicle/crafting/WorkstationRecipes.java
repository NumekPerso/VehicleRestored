package com.mrcrayfish.vehicle.crafting;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Author: MrCrayfish
 */
public class WorkstationRecipes
{
    @Nullable
    public static WorkstationRecipe getRecipe(EntityType<?> entityType, Level world)
    {
        List<WorkstationRecipe> recipes = world.getRecipeManager().getRecipes().stream().filter(recipe -> recipe.getType() == VehicleRecipeTypes.WORKSTATION.get()).map(recipe -> (WorkstationRecipe) recipe).collect(Collectors.toList());
        return recipes.stream().filter(recipe -> recipe.getVehicle() == entityType).findFirst().orElse(null);
    }
}
