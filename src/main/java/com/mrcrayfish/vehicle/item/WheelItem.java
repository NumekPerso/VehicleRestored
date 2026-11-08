package com.mrcrayfish.vehicle.item;

import com.mrcrayfish.vehicle.entity.IWheelType;
import com.mrcrayfish.vehicle.entity.WheelType;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Author: MrCrayfish
 */
public class WheelItem extends PartItem implements IDyeable
{
    private IWheelType wheelType;

    public WheelItem(IWheelType wheelType, Item.Properties properties)
    {
        super(properties);
        this.wheelType = wheelType;
    }

    public IWheelType getWheelType()
    {
        return this.wheelType;
    }
}
