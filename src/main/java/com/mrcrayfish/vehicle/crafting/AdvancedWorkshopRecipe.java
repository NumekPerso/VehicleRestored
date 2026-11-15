package com.mrcrayfish.vehicle.crafting;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.vehicle.init.ModRecipeSerializers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * A recipe of the advanced workshop: the whole vehicle (or a batch of items) made in one go from its
 * ingredients, in named phases that take time and energy (FE per tick). A vehicle recipe gives the
 * vehicle's crate, and the workshop builds the vehicle as a miniature while it works.
 */
public class AdvancedWorkshopRecipe implements Recipe<Container>
{
    /** One step of the work, shown in the screen: its name (translation key part) and its length */
    public record Phase(String name, int ticks)
    {
        public Component getDisplayName()
        {
            return Component.translatable("gui.vehicle_restored.advanced_workshop.phase." + this.name);
        }
    }

    private final ResourceLocation id;
    private final String category;
    @Nullable
    private final EntityType<?> vehicle;
    private final ItemStack result;
    private final ImmutableList<WorkstationIngredient> ingredients;
    private final int energyPerTick;
    private final ImmutableList<Phase> phases;
    private final int totalTicks;

    public AdvancedWorkshopRecipe(ResourceLocation id, String category, @Nullable EntityType<?> vehicle, ItemStack result, ImmutableList<WorkstationIngredient> ingredients, int energyPerTick, ImmutableList<Phase> phases)
    {
        this.id = id;
        this.category = category;
        this.vehicle = vehicle;
        this.result = result;
        this.ingredients = ingredients;
        this.energyPerTick = energyPerTick;
        this.phases = phases;
        this.totalTicks = Math.max(1, phases.stream().mapToInt(Phase::ticks).sum());
    }

    /** All the recipes of the workshop, in the order of the screen (category, then id): the same on both sides */
    public static List<AdvancedWorkshopRecipe> list(RecipeManager manager)
    {
        List<AdvancedWorkshopRecipe> recipes = new ArrayList<>(manager.getAllRecipesFor(VehicleRecipeTypes.ADVANCED_WORKSHOP.get()));
        recipes.sort(Comparator.comparing(AdvancedWorkshopRecipe::getCategory).thenComparing(r -> r.getId().toString()));
        return recipes;
    }

    @Nullable
    public static AdvancedWorkshopRecipe get(Level level, @Nullable ResourceLocation id)
    {
        if(id == null)
            return null;
        return level.getRecipeManager().byKey(id).filter(r -> r instanceof AdvancedWorkshopRecipe).map(r -> (AdvancedWorkshopRecipe) r).orElse(null);
    }

    public String getCategory()
    {
        return this.category;
    }

    /** The vehicle this recipe builds (its crate is the result), or null for items */
    @Nullable
    public EntityType<?> getVehicle()
    {
        return this.vehicle;
    }

    public ItemStack getResult()
    {
        return this.result.copy();
    }

    /** The ingredients, with their counts */
    public ImmutableList<WorkstationIngredient> getMaterials()
    {
        return this.ingredients;
    }

    public int getEnergyPerTick()
    {
        return this.energyPerTick;
    }

    public ImmutableList<Phase> getPhases()
    {
        return this.phases;
    }

    public int getTotalTicks()
    {
        return this.totalTicks;
    }

    /** Name shown in the workshop: the vehicle, or the result with its count */
    public Component getDisplayName()
    {
        if(this.vehicle != null)
        {
            return this.vehicle.getDescription();
        }
        Component name = this.result.getHoverName();
        return this.result.getCount() > 1 ? Component.translatable("gui.vehicle_restored.advanced_workshop.count", name, this.result.getCount()) : name;
    }

    /** Index of the phase at the given progress (ticks) */
    public int getPhaseIndex(int progress)
    {
        int end = 0;
        for(int i = 0; i < this.phases.size(); i++)
        {
            end += this.phases.get(i).ticks();
            if(progress < end)
                return i;
        }
        return Math.max(0, this.phases.size() - 1);
    }

    /** Ticks done in the phase at the given progress, and its length */
    public int[] getPhaseProgress(int progress)
    {
        int start = 0;
        for(Phase phase : this.phases)
        {
            if(progress < start + phase.ticks())
                return new int[]{progress - start, phase.ticks()};
            start += phase.ticks();
        }
        Phase last = this.phases.isEmpty() ? new Phase("", 1) : this.phases.get(this.phases.size() - 1);
        return new int[]{last.ticks(), last.ticks()};
    }

    /** How many items matching the ingredient are in the slots [from, to) */
    public static int count(IItemHandler items, int from, int to, WorkstationIngredient ingredient)
    {
        int count = 0;
        for(int slot = from; slot < to; slot++)
        {
            ItemStack stack = items.getStackInSlot(slot);
            if(!stack.isEmpty() && ingredient.test(stack))
            {
                count += stack.getCount();
            }
        }
        return count;
    }

    public boolean hasIngredients(IItemHandler items, int from, int to)
    {
        return this.ingredients.stream().allMatch(ingredient -> count(items, from, to, ingredient) >= ingredient.getCount());
    }

    /**
     * Takes the ingredients out of the slots [from, to) and returns what was taken (given back if the
     * work is cancelled). Check {@link #hasIngredients} first.
     */
    public List<ItemStack> consume(IItemHandlerModifiable items, int from, int to)
    {
        List<ItemStack> taken = new ArrayList<>();
        for(WorkstationIngredient ingredient : this.ingredients)
        {
            int needed = ingredient.getCount();
            for(int slot = from; slot < to && needed > 0; slot++)
            {
                ItemStack stack = items.getStackInSlot(slot);
                if(stack.isEmpty() || !ingredient.test(stack))
                    continue;
                int amount = Math.min(needed, stack.getCount());
                taken.add(stack.copyWithCount(amount));
                ItemStack rest = stack.copy();
                rest.shrink(amount);
                items.setStackInSlot(slot, rest);
                needed -= amount;
            }
        }
        return taken;
    }

    /* Recipe: the workshop does its own matching */

    @Override
    public boolean matches(Container container, Level level)
    {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access)
    {
        return this.getResult();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access)
    {
        return this.result;
    }

    @Override
    public boolean isSpecial()
    {
        // not in the recipe book
        return true;
    }

    @Override
    public ResourceLocation getId()
    {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return ModRecipeSerializers.ADVANCED_WORKSHOP.get();
    }

    @Override
    public RecipeType<?> getType()
    {
        return VehicleRecipeTypes.ADVANCED_WORKSHOP.get();
    }
}
