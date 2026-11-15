package com.mrcrayfish.vehicle.inventory.container;

import com.mrcrayfish.vehicle.crafting.AdvancedWorkshopRecipe;
import com.mrcrayfish.vehicle.init.ModContainers;
import com.mrcrayfish.vehicle.tileentity.AdvancedWorkshopTileEntity;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Menu of the advanced workshop: its 12 input slots and its output slot, the player's inventory, and
 * the buttons of the screen (choose a recipe, start, cancel) sent with the vanilla menu button packet.
 */
public class AdvancedWorkshopContainer extends AbstractContainerMenu
{
    /* Layout of the screen (see AdvancedWorkshopScreen and its background texture) */
    public static final int INPUT_X = 116, INPUT_Y = 115, INPUT_COLUMNS = 6;
    public static final int OUTPUT_X = 244, OUTPUT_Y = 124;
    public static final int INVENTORY_X = 116, INVENTORY_Y = 166, HOTBAR_Y = 224;

    public static final int BUTTON_START = 1000;
    public static final int BUTTON_CANCEL = 1001;

    private final AdvancedWorkshopTileEntity workshop;
    private final ContainerData data;

    public AdvancedWorkshopContainer(int windowId, Inventory inventory, AdvancedWorkshopTileEntity workshop, ContainerData data)
    {
        super(ModContainers.ADVANCED_WORKSHOP.get(), windowId);
        checkContainerDataCount(data, AdvancedWorkshopTileEntity.DATA_COUNT);
        this.workshop = workshop;
        this.data = data;

        for(int slot = 0; slot < AdvancedWorkshopTileEntity.INPUT_SLOTS; slot++)
        {
            this.addSlot(new SlotItemHandler(workshop.getItems(), slot, INPUT_X + (slot % INPUT_COLUMNS) * 18, INPUT_Y + (slot / INPUT_COLUMNS) * 18));
        }
        this.addSlot(new SlotItemHandler(workshop.getItems(), AdvancedWorkshopTileEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y)
        {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack)
            {
                return false;
            }
        });
        for(int row = 0; row < 3; row++)
        {
            for(int column = 0; column < 9; column++)
            {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for(int column = 0; column < 9; column++)
        {
            this.addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
        this.addDataSlots(data);
    }

    public AdvancedWorkshopTileEntity getWorkshop()
    {
        return this.workshop;
    }

    public int getEnergy()
    {
        return (this.data.get(0) & 0xFFFF) | (this.data.get(1) << 16);
    }

    public int getProgress()
    {
        return this.data.get(2);
    }

    public int getTotal()
    {
        return this.data.get(3);
    }

    public boolean isCrafting()
    {
        return (this.data.get(4) & 1) != 0;
    }

    public boolean isPowered()
    {
        return (this.data.get(4) & 2) != 0;
    }

    public boolean isBlocked()
    {
        return (this.data.get(4) & 4) != 0;
    }

    /** Buttons of the screen: a recipe of the list (its index), start or cancel */
    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if(id == BUTTON_START)
        {
            return this.workshop.start();
        }
        if(id == BUTTON_CANCEL)
        {
            this.workshop.cancel();
            return true;
        }
        List<AdvancedWorkshopRecipe> recipes = AdvancedWorkshopRecipe.list(player.level().getRecipeManager());
        if(id >= 0 && id < recipes.size())
        {
            this.workshop.select(recipes.get(id).getId());
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return !this.workshop.isRemoved() && player.distanceToSqr(this.workshop.getBlockPos().getCenter()) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = this.slots.get(index);
        if(!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machineSlots = AdvancedWorkshopTileEntity.INPUT_SLOTS + 1;
        if(index < machineSlots)
        {
            if(!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true))
                return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        }
        else if(!this.moveItemStackTo(stack, 0, AdvancedWorkshopTileEntity.INPUT_SLOTS, false))
        {
            return ItemStack.EMPTY;
        }
        if(stack.isEmpty())
            slot.set(ItemStack.EMPTY);
        else
            slot.setChanged();
        if(stack.getCount() == copy.getCount())
            return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }
}
