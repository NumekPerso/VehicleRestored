package com.mrcrayfish.vehicle.item;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * The item of the vehicle crate. When shift is held over it, its tooltip shows a 3D preview of
 * the vehicle inside, with its color, engine and wheels.
 */
public class VehicleCrateItem extends BlockItem
{
    public VehicleCrateItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack)
    {
        if(!Screen.hasShiftDown())
            return Optional.empty();

        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if(tag == null || !tag.contains("Vehicle", Tag.TAG_STRING))
            return Optional.empty();

        ResourceLocation vehicle = ResourceLocation.tryParse(tag.getString("Vehicle"));
        if(vehicle == null)
            return Optional.empty();

        Integer color = tag.contains("Color", Tag.TAG_INT) ? tag.getInt("Color") : null;
        ItemStack engine = tag.contains("EngineStack", Tag.TAG_COMPOUND) ? ItemStack.of(tag.getCompound("EngineStack")) : ItemStack.EMPTY;
        ItemStack wheel = tag.contains("WheelStack", Tag.TAG_COMPOUND) ? ItemStack.of(tag.getCompound("WheelStack")) : ItemStack.EMPTY;
        return Optional.of(new Tooltip(vehicle, color, engine, wheel, tag.getBoolean("Creative")));
    }

    /**
     * The data of the vehicle shown in the tooltip
     *
     * @param color null when the crate does not set a color
     * @param creative true for the crates of the creative tab, which get an iron engine and standard wheels
     */
    public record Tooltip(ResourceLocation vehicle, Integer color, ItemStack engine, ItemStack wheel, boolean creative) implements TooltipComponent {}
}
