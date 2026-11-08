package com.mrcrayfish.vehicle.entity.vehicle;

import com.mrcrayfish.vehicle.entity.LandVehicleEntity;
import com.mrcrayfish.vehicle.init.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The sports car of the unreleased 1.16 development version of the Vehicle Mod, adapted to the
 * vehicle system of the released version.
 * Author: MrCrayfish
 */
public class SportsCarEntity extends LandVehicleEntity
{
    public SportsCarEntity(EntityType<? extends SportsCarEntity> type, Level level)
    {
        super(type, level);
        this.setMaxSpeed(22F);
        this.setFuelCapacity(20000F);
        this.setMaxTurnAngle(35);
    }

    @Override
    public SoundEvent getEngineSound()
    {
        return ModSounds.ENTITY_SPORTS_CAR_ENGINE.get();
    }

    @Override
    public float getMinEnginePitch()
    {
        return 0.9F;
    }

    @Override
    public float getMaxEnginePitch()
    {
        return 1.5F;
    }

    @Override
    public boolean canBeColored()
    {
        return true;
    }

    @Override
    public boolean canMountTrailer()
    {
        return false;
    }

    @Override
    public double getCameraDistance()
    {
        return 5.0;
    }
}
