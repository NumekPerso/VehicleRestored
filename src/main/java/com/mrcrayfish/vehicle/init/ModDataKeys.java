package com.mrcrayfish.vehicle.init;

import com.mrcrayfish.vehicle.Reference;
import com.mrcrayfish.vehicle.common.data.Serializers;
import com.mrcrayfish.vehicle.common.data.SyncedDataKey;
import com.mrcrayfish.vehicle.common.data.SyncedPlayerData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class ModDataKeys
{
    public static final SyncedDataKey<Integer> TRAILER = SyncedDataKey.builder(Serializers.INTEGER)
            .id(new ResourceLocation(Reference.MOD_ID, "trailer"))
            .defaultValueSupplier(() -> -1)
            .resetOnDeath()
            .build();

    public static final SyncedDataKey<Optional<BlockPos>> GAS_PUMP = SyncedDataKey.builder(com.mrcrayfish.vehicle.common.data.Serializers.OPTIONAL_BLOCK_POS)
            .id(new ResourceLocation(Reference.MOD_ID, "gas_pump"))
            .defaultValueSupplier(Optional::empty)
            .resetOnDeath()
            .build();

    public static void register()
    {
        SyncedPlayerData.instance().registerKey(TRAILER);
        SyncedPlayerData.instance().registerKey(GAS_PUMP);
    }
}
