package com.mrcrayfish.vehicle.compat.jei;

import com.mrcrayfish.vehicle.crafting.WorkstationIngredient;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Small helpers shared by the categories of the JEI integration
 */
final class JeiHelper
{
    /** Colour of the texts drawn in the categories, the grey JEI uses */
    static final int TEXT_COLOR = 0xFF404040;

    private JeiHelper() {}

    /** The items an ingredient accepts, each with the count the recipe asks for */
    static List<ItemStack> withCount(WorkstationIngredient ingredient)
    {
        List<ItemStack> stacks = new ArrayList<>();
        for(ItemStack stack : ingredient.getItems())
        {
            if(!stack.isEmpty())
            {
                stacks.add(stack.copyWithCount(Math.max(1, ingredient.getCount())));
            }
        }
        return stacks;
    }

    /** Ticks as whole seconds, as the advanced workshop shows them */
    static String seconds(int ticks)
    {
        return String.valueOf(Math.round(ticks / 20F));
    }

    /** A value of the server config, or the default when it isn't loaded (not connected yet) */
    static int config(Supplier<Integer> value, int fallback)
    {
        try
        {
            return value.get();
        }
        catch(RuntimeException e)
        {
            return fallback;
        }
    }

    /** Draws a text cut to the given width, from x (or centred on x) */
    static void drawText(GuiGraphics graphics, Component text, int x, int y, int maxWidth, boolean centred)
    {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        if(lines.isEmpty())
            return;
        FormattedCharSequence line = lines.get(0);
        int drawX = centred ? x - font.width(line) / 2 : x;
        graphics.drawString(font, line, drawX, y, TEXT_COLOR, false);
    }

    static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
