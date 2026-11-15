package com.mrcrayfish.vehicle.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A component of the advanced vehicles (circuit board, servo motor), made on a crafting table and used by
 * the recipes of the advanced workshop: the tank now, the next advanced vehicles later
 */
public class ComponentItem extends Item
{
    public ComponentItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("item.vehicle_restored.component.info").withStyle(ChatFormatting.GRAY));
    }
}
