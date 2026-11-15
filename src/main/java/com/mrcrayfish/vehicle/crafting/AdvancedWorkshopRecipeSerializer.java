package com.mrcrayfish.vehicle.crafting;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mrcrayfish.vehicle.block.VehicleCrateBlock;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Reads the recipes of the advanced workshop:
 * <pre>
 * {
 *   "type": "vehicle_restored:advanced_workshop",
 *   "category": "military",
 *   "vehicle": "vehicle_restored:tank",            (the result is the vehicle's crate)
 *   "result": { "item": "...", "count": 4 },       (or an item result)
 *   "energy_per_tick": 200,
 *   "ingredients": [ { "item": "minecraft:iron_block", "count": 8 }, { "tag": "...", "count": 2 } ],
 *   "phases": [ { "name": "structure", "ticks": 600 }, ... ]
 * }
 * </pre>
 */
public class AdvancedWorkshopRecipeSerializer implements RecipeSerializer<AdvancedWorkshopRecipe>
{
    @Override
    public AdvancedWorkshopRecipe fromJson(ResourceLocation id, JsonObject json)
    {
        String category = GsonHelper.getAsString(json, "category", "misc");
        EntityType<?> vehicle = null;
        ItemStack result;
        if(json.has("vehicle"))
        {
            ResourceLocation vehicleId = new ResourceLocation(GsonHelper.getAsString(json, "vehicle"));
            vehicle = EntityType.byString(vehicleId.toString()).orElseThrow(() -> new JsonSyntaxException("Invalid vehicle entity: " + vehicleId));
            result = crate(vehicleId);
        }
        else
        {
            result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true);
        }
        ImmutableList.Builder<WorkstationIngredient> ingredients = ImmutableList.builder();
        for(var element : GsonHelper.getAsJsonArray(json, "ingredients"))
        {
            ingredients.add(WorkstationIngredient.fromJson(element.getAsJsonObject()));
        }
        ImmutableList.Builder<AdvancedWorkshopRecipe.Phase> phases = ImmutableList.builder();
        JsonArray phaseArray = GsonHelper.getAsJsonArray(json, "phases");
        if(phaseArray.isEmpty())
            throw new JsonSyntaxException("A workshop recipe needs at least one phase");
        for(var element : phaseArray)
        {
            JsonObject object = element.getAsJsonObject();
            phases.add(new AdvancedWorkshopRecipe.Phase(GsonHelper.getAsString(object, "name"), Math.max(1, GsonHelper.getAsInt(object, "ticks"))));
        }
        int energy = Math.max(0, GsonHelper.getAsInt(json, "energy_per_tick", 0));
        return new AdvancedWorkshopRecipe(id, category, vehicle, result, ingredients.build(), energy, phases.build());
    }

    /** The crate of a vehicle, as the workstation makes it (default colour, no engine or wheels given) */
    private static ItemStack crate(ResourceLocation vehicle)
    {
        return VehicleCrateBlock.create(vehicle, VehicleEntity.DYE_TO_COLOR[0], ItemStack.EMPTY, ItemStack.EMPTY);
    }

    @Nullable
    @Override
    public AdvancedWorkshopRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer)
    {
        String category = buffer.readUtf();
        EntityType<?> vehicle = buffer.readBoolean() ? ForgeRegistries.ENTITY_TYPES.getValue(buffer.readResourceLocation()) : null;
        ItemStack result = buffer.readItem();
        ImmutableList.Builder<WorkstationIngredient> ingredients = ImmutableList.builder();
        int count = buffer.readVarInt();
        for(int i = 0; i < count; i++)
        {
            ingredients.add((WorkstationIngredient) Ingredient.fromNetwork(buffer));
        }
        ImmutableList.Builder<AdvancedWorkshopRecipe.Phase> phases = ImmutableList.builder();
        int phaseCount = buffer.readVarInt();
        for(int i = 0; i < phaseCount; i++)
        {
            phases.add(new AdvancedWorkshopRecipe.Phase(buffer.readUtf(), buffer.readVarInt()));
        }
        int energy = buffer.readVarInt();
        return new AdvancedWorkshopRecipe(id, category, vehicle, result, ingredients.build(), energy, phases.build());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, AdvancedWorkshopRecipe recipe)
    {
        buffer.writeUtf(recipe.getCategory());
        buffer.writeBoolean(recipe.getVehicle() != null);
        if(recipe.getVehicle() != null)
        {
            buffer.writeResourceLocation(ForgeRegistries.ENTITY_TYPES.getKey(recipe.getVehicle()));
        }
        buffer.writeItem(recipe.getResult());
        buffer.writeVarInt(recipe.getMaterials().size());
        for(WorkstationIngredient ingredient : recipe.getMaterials())
        {
            ingredient.toNetwork(buffer);
        }
        buffer.writeVarInt(recipe.getPhases().size());
        for(AdvancedWorkshopRecipe.Phase phase : recipe.getPhases())
        {
            buffer.writeUtf(phase.name());
            buffer.writeVarInt(phase.ticks());
        }
        buffer.writeVarInt(recipe.getEnergyPerTick());
    }
}
