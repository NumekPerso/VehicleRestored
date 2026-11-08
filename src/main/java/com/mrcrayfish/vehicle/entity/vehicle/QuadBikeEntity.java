package com.mrcrayfish.vehicle.entity.vehicle;

import com.mrcrayfish.vehicle.entity.LandVehicleEntity;
import com.mrcrayfish.vehicle.init.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The quad bike of the unreleased 1.16 development version of the Vehicle Mod, adapted to the
 * vehicle system of the released version.
 * Author: MrCrayfish
 */
public class QuadBikeEntity extends LandVehicleEntity
{
    public QuadBikeEntity(EntityType<? extends QuadBikeEntity> type, Level level)
    {
        super(type, level);
        this.setMaxSpeed(16F);
        this.setFuelCapacity(20000F);
    }

    @Override
    public FuelPortType getFuelPortType()
    {
        return FuelPortType.SMALL;
    }

    @Override
    public SoundEvent getEngineSound()
    {
        return ModSounds.ENTITY_ATV_ENGINE.get();
    }

    @Override
    public boolean canBeColored()
    {
        return true;
    }

    @Override
    public boolean canTowTrailer()
    {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderEngine()
    {
        return true;
    }
}
