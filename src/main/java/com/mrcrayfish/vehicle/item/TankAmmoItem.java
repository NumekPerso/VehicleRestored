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
 * Ammunition of the tank (no recipe yet: from the creative tab). It is loaded in the tank's
 * ammunition store, opened with the inventory key from inside the tank. The tooltip says how many
 * rounds the item holds and what fires them.
 */
public class TankAmmoItem extends Item
{
    private final int rounds;

    public TankAmmoItem(int rounds, Properties properties)
    {
        super(properties);
        this.rounds = rounds;
    }

    public int getRounds()
    {
        return this.rounds;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable(this.getDescriptionId() + ".info", this.rounds).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.vehicle_restored.tank_ammo.load").withStyle(ChatFormatting.DARK_GRAY));
    }
}
