package com.mrcrayfish.vehicle.tileentity;

import com.mrcrayfish.vehicle.init.ModTileEntities;
import com.mrcrayfish.vehicle.inventory.IStorageBlock;
import com.mrcrayfish.vehicle.inventory.container.WorkstationContainer;
import com.mrcrayfish.vehicle.item.EngineItem;
import com.mrcrayfish.vehicle.item.WheelItem;
import net.minecraft.world.Container;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Author: MrCrayfish
 */
public class WorkstationTileEntity extends TileEntitySynced implements IStorageBlock
{
    private NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);

    public WorkstationTileEntity(BlockPos pos, BlockState state)
    {
        super(ModTileEntities.WORKSTATION.get(), pos, state);
    }

    @Override
    public NonNullList<ItemStack> getInventory()
    {
        return this.inventory;
    }

    @Override
    public void load(CompoundTag compound)
    {
        super.load(compound);
        ContainerHelper.loadAllItems(compound, this.inventory);
    }

    @Override
    protected void saveAdditional(CompoundTag compound)
    {
        ContainerHelper.saveAllItems(compound, this.inventory);
        super.saveAdditional(compound);
    }

    /**
     * Same rule as the slots of the menu, also for hoppers and pipes: one dye, one engine and one
     * wheel. Crafting uses one of each, a whole stack would be lost in the crate.
     */
    @Override
    public boolean canPlaceItem(int index, ItemStack stack)
    {
        if(index < 0 || index >= this.inventory.size() || !this.inventory.get(index).isEmpty())
            return false;
        return switch(index)
        {
            case 0 -> stack.getItem() instanceof DyeItem;
            case 1 -> stack.getItem() instanceof EngineItem;
            default -> stack.getItem() instanceof WheelItem;
        };
    }

    @Override
    public int getMaxStackSize()
    {
        return 1;
    }

    /** IStorageBlock returns false, which would close the menu: the vanilla 8 block rule instead. */
    @Override
    public boolean stillValid(Player player)
    {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("container.vehicle_restored.workstation");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player playerEntity)
    {
        return new WorkstationContainer(windowId, playerInventory, this);
    }
}
