package com.mrcrayfish.vehicle.client.model.geometry;

import com.mrcrayfish.vehicle.Reference;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers the model loaders of the mod
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GeometryLoaders
{
    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event)
    {
        event.register("open_model", OpenModelLoader.INSTANCE);
    }
}
