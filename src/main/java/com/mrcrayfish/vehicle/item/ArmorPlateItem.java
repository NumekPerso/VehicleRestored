package com.mrcrayfish.vehicle.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Armour plate of the tank (composite armour: steel, ceramic, steel), made on a crafting table. The tank
 * carries 5 of them, one on each face (front, left, top, right, rear: see TankArmorFace), put on from the
 * tank's screen. A plate takes the hits on its face until it is worn through, then breaks; its durability
 * is its armour points. Nothing repairs a plate (worn armour is lost for good, like the tank's health), but a
 * worn plate can be changed for a new one.
 *
 * This general plate is worn alike by every kind of damage; plates made against one kind of damage
 * (explosions, bullets, fire) will be variants with their own {@link #getWearFactor}.
 */
public class ArmorPlateItem extends Item
{
    /** Armour points of a new plate */
    public static final int POINTS = 100;

    public ArmorPlateItem(Properties properties)
    {
        super(properties.durability(POINTS));
    }

    /**
     * Points of wear for one point of damage of this kind (1: the plate takes it all; less: the plate resists
     * it better, a plate against explosions for example)
     */
    public float getWearFactor(DamageSource source)
    {
        return 1F;
    }

    /** Armour points left on a plate */
    public static int getPoints(ItemStack stack)
    {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    @Override
    public boolean isRepairable(ItemStack stack)
    {
        return false; // no repair in the crafting grid nor at the anvil
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairItem)
    {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack)
    {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book)
    {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("item.vehicle_restored.armor_plate.points", getPoints(stack), POINTS).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.vehicle_restored.armor_plate.info").withStyle(ChatFormatting.DARK_GRAY));
    }
}
