package com.mrcrayfish.vehicle.entity;

import com.mrcrayfish.vehicle.item.EngineItem;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

/**
 * Author: MrCrayfish
 */
public interface IEngineTier
{
    default float getAccelerationMultiplier()
    {
        return 1.0F;
    }

    default float getAdditionalMaxSpeed()
    {
        return 0.0F;
    }

    static Optional<IEngineTier> fromStack(ItemStack stack)
    {
        if(stack.getItem() instanceof EngineItem)
        {
            return Optional.of(((EngineItem) stack.getItem()).getEngineTier());
        }
        return Optional.empty();
    }
}
