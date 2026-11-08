package com.mrcrayfish.vehicle.init;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.world.storage.loot.functions.CopyFluidTanks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Author: MrCrayfish
 */
public class ModLootFunctions
{
    public static final DeferredRegister<LootItemFunctionType> REGISTER = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Reference.MOD_ID);

    public static final RegistryObject<LootItemFunctionType> COPY_FLUID_TANKS = REGISTER.register("copy_fluid_tanks", () -> new LootItemFunctionType(new CopyFluidTanks.Serializer()));
}
