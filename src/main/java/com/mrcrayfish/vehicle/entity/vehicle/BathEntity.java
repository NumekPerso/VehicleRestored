package com.mrcrayfish.vehicle.entity.vehicle;

import com.mrcrayfish.vehicle.entity.EngineTier;
import com.mrcrayfish.vehicle.entity.IEngineTier;
import com.mrcrayfish.vehicle.entity.PlaneEntity;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Author: MrCrayfish
 */
public class BathEntity extends PlaneEntity
{
    public BathEntity(EntityType<? extends BathEntity> type, Level worldIn)
    {
        super(type, worldIn);
        this.setFuelConsumption(0.0F);
    }

    @Override
    public SoundEvent getEngineSound()
    {
        return null;
    }

    @Override
    public void updateVehicle()
    {
        if(this.isFlying() && this.getControllingPassenger() != null)
        {
            for(int i = 0; i < 4; i++)
            {
                this.level().addParticle(ParticleTypes.DRIPPING_WATER, this.getX() - 0.25 + 0.5 * random.nextGaussian(), this.getY() + 0.5 * random.nextGaussian(), this.getZ() - 0.25 + 0.5 * random.nextGaussian(), 0, 0, 0);
            }
        }
    }

    @Override
    public boolean canBeColored()
    {
        return false;
    }

    @Override
    public boolean isLockable()
    {
        return false;
    }

    /**
     * The bath has no engine type, so no engine can be installed, but the plane controls only
     * accelerate when an engine tier is present. The bath flies as if it had an iron engine
     * (no acceleration or speed bonus), as in the 1.12.2 version where it had no engine
     */
    @Override
    public Optional<IEngineTier> getEngineTier()
    {
        return Optional.of(EngineTier.IRON);
    }

    /**
     * The bath has no fuel port and does not use fuel (its fuel consumption is 0, as in the
     * 1.12.2 version), so it never needs to be refueled
     */
    @Override
    public boolean requiresFuel()
    {
        return false;
    }
}
