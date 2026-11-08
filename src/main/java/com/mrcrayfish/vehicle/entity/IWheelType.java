package com.mrcrayfish.vehicle.entity;

import com.mrcrayfish.vehicle.item.WheelItem;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

/**
 * Author: MrCrayfish
 */
public interface IWheelType
{
    default float getRoadMultiplier()
    {
        return 1.0F;
    }

    default float getDirtMultiplier()
    {
        return 1.0F;
    }

    default float getSnowMultiplier()
    {
        return 1.0F;
    }

    default void applyPhysics(PoweredVehicleEntity vehicle) {}

    static Optional<IWheelType> fromStack(ItemStack stack)
    {
        if(stack.getItem() instanceof WheelItem)
        {
            return Optional.of(((WheelItem) stack.getItem()).getWheelType());
        }
        return Optional.empty();
    }
}
