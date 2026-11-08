package com.mrcrayfish.vehicle.entity.vehicle;

import com.mrcrayfish.vehicle.entity.HelicopterEntity;
import com.mrcrayfish.vehicle.init.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The compact helicopter of the unreleased 1.16 development version of the Vehicle Mod, adapted
 * to the vehicle system of the released version.
 * Author: MrCrayfish
 */
public class CompactHelicopterEntity extends HelicopterEntity
{
    public CompactHelicopterEntity(EntityType<? extends CompactHelicopterEntity> type, Level level)
    {
        super(type, level);
        this.setMaxSpeed(20F);
        this.setFuelCapacity(40000F);
        this.setFuelConsumption(0.5F);
    }

    @Override
    public SoundEvent getEngineSound()
    {
        return ModSounds.ENTITY_VEHICLE_HELICOPTER_ROTOR.get();
    }

    @Override
    public float getMaxEnginePitch()
    {
        return 1.0F;
    }

    @Override
    public boolean canBeColored()
    {
        return true;
    }

    @Override
    public double getCameraDistance()
    {
        return 9.0;
    }

    @Override
    public double getCameraHeightOffset()
    {
        return 2.0;
    }
}
