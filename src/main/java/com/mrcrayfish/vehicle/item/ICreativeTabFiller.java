package com.mrcrayfish.vehicle.item;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * Implemented by items and blocks that add their own stacks to the creative tab (1.20 removed
 * Item#fillItemCategory)
 */
public interface ICreativeTabFiller
{
    void fillItemCategory(NonNullList<ItemStack> items);
}
