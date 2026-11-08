package com.mrcrayfish.vehicle.common;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;
import com.mrcrayfish.vehicle.entity.IEngineTier;
import com.mrcrayfish.vehicle.entity.IEngineType;
import com.mrcrayfish.vehicle.entity.VehicleEntity;
import com.mrcrayfish.vehicle.item.EngineItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Author: MrCrayfish
 */
public class VehicleRegistry
{
    private static final List<Supplier<? extends EntityType<? extends VehicleEntity>>> REGISTERED_VEHICLES = new ArrayList<>();
    private static final Map<ResourceLocation, IEngineType> ID_TO_ENGINE_TYPE = new HashMap<>();
    private static final Map<Pair<IEngineType, IEngineTier>, EngineItem> PAIR_TO_ENGINE_ITEM = new HashMap<>();

    public static synchronized void registerVehicleType(Supplier<? extends EntityType<? extends VehicleEntity>> entityType)
    {
        REGISTERED_VEHICLES.add(entityType);
    }

    public static Set<EntityType<? extends VehicleEntity>> getRegisteredVehicleTypes()
    {
        Set<EntityType<? extends VehicleEntity>> types = new LinkedHashSet<>();
        REGISTERED_VEHICLES.forEach(supplier -> types.add(supplier.get()));
        return types;
    }

    public static synchronized void registerEngine(IEngineType type, IEngineTier tier, EngineItem item)
    {
        Pair<IEngineType, IEngineTier> pair = Pair.of(type, tier);
        if(PAIR_TO_ENGINE_ITEM.containsKey(pair))
        {
            throw new IllegalArgumentException("The given engine type/tier pair is already registered!");
        }
        PAIR_TO_ENGINE_ITEM.put(pair, item);
        ID_TO_ENGINE_TYPE.put(type.getId(), type);
    }

    @Nullable
    public static IEngineType getEngineTypeFromId(ResourceLocation id)
    {
        return ID_TO_ENGINE_TYPE.get(id);
    }

    @Nullable
    public static EngineItem getEngineItem(IEngineType type, IEngineTier tier)
    {
        return PAIR_TO_ENGINE_ITEM.get(Pair.of(type, tier));
    }
}
