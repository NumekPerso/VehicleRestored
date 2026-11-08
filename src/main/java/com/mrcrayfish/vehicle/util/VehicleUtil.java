package com.mrcrayfish.vehicle.util;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.block.VehicleCrateBlock;
import com.mrcrayfish.vehicle.common.VehicleRegistry;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * Author: MrCrayfish
 */
public class VehicleUtil
{
    public static <T extends VehicleEntity> RegistryObject<EntityType<T>> createEntityType(DeferredRegister<EntityType<?>> deferredRegister, String name, BiFunction<EntityType<T>, Level, T> function, float width, float height)
    {
        return createEntityType(deferredRegister, name, function, width, height, true);
    }

    public static <T extends VehicleEntity> RegistryObject<EntityType<T>> createEntityType(DeferredRegister<EntityType<?>> deferredRegister, String name, BiFunction<EntityType<T>, Level, T> function, float width, float height, boolean includeCrate)
    {
        ResourceLocation id = new ResourceLocation(Reference.MOD_ID, name);
        RegistryObject<EntityType<T>> object = deferredRegister.register(name, () -> VehicleUtil.buildVehicleType(id, function, width, height));
        VehicleRegistry.registerVehicleType(object);
        if(includeCrate) VehicleCrateBlock.registerVehicle(id);
        return object;
    }

    /**
     * Registers a vehicle only if the given mod is installed. Used for the vehicles made from
     * furniture of Furniture Restored.
     */
    @Nullable
    public static <T extends VehicleEntity> RegistryObject<EntityType<T>> createModDependentEntityType(DeferredRegister<EntityType<?>> deferredRegister, String modId, String id, BiFunction<EntityType<T>, Level, T> function, float width, float height, boolean registerCrate)
    {
        if(ModList.get().isLoaded(modId))
        {
            return createEntityType(deferredRegister, id, function, width, height, registerCrate);
        }
        return null;
    }

    private static <T extends VehicleEntity> EntityType<T> buildVehicleType(ResourceLocation id, BiFunction<EntityType<T>, Level, T> function, float width, float height)
    {
        return EntityType.Builder.of(function::apply, MobCategory.MISC).sized(width, height).setTrackingRange(256).setUpdateInterval(1).fireImmune().setShouldReceiveVelocityUpdates(true).build(id.toString());
    }
}
