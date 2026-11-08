package com.mrcrayfish.vehicle.util;

import net.minecraft.world.item.DyeColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Author: MrCrayfish
 */
public class CommonUtils
{
    /**
     * Gets the color of a dye as an RGB integer. Replaces DyeColor#getColorValue, removed after 1.16.
     */
    public static int getDyeColor(DyeColor color)
    {
        float[] rgb = color.getTextureDiffuseColors();
        return (Math.round(rgb[0] * 255.0F) << 16) | (Math.round(rgb[1] * 255.0F) << 8) | Math.round(rgb[2] * 255.0F);
    }

    /**
     * Server side check for messages sent by a client about an entity: the entity must be in the
     * level of the player and within interaction reach. The client ray traces the parts of a
     * vehicle with the block reach, and some parts stick out of the bounding box, hence the margin.
     */
    public static boolean isWithinInteractionReach(Player player, Entity target)
    {
        if(target == null || !target.isAlive() || target.level() != player.level())
            return false;
        double reach = Math.max(player.getBlockReach(), player.getEntityReach()) + 2.0;
        return target.getBoundingBox().distanceToSqr(player.getEyePosition()) <= reach * reach;
    }

    public static CompoundTag getOrCreateStackTag(ItemStack stack)
    {
        if(stack.getTag() == null)
        {
            stack.setTag(new CompoundTag());
        }
        return stack.getTag();
    }

    public static void writeItemStackToTag(CompoundTag compound, String key, ItemStack stack)
    {
        if(!stack.isEmpty())
        {
            compound.put(key, stack.save(new CompoundTag()));
        }
    }

    public static ItemStack readItemStackFromTag(CompoundTag compound, String key)
    {
        if(compound.contains(key, Tag.TAG_COMPOUND))
        {
            return ItemStack.of(compound.getCompound(key));
        }
        return ItemStack.EMPTY;
    }

    public static void sendInfoMessage(Player player, String message)
    {
        if(player instanceof ServerPlayer)
        {
            player.displayClientMessage(Component.translatable(message), true);
        }
    }

    public static boolean isMouseWithin(int mouseX, int mouseY, int x, int y, int width, int height)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
