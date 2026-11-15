package com.mrcrayfish.vehicle.tileentity;

import com.mrcrayfish.vehicle.block.AdvancedWorkshopBlock;
import com.mrcrayfish.vehicle.client.audio.AdvancedWorkshopSounds;
import com.mrcrayfish.vehicle.crafting.AdvancedWorkshopRecipe;
import com.mrcrayfish.vehicle.init.ModSounds;
import com.mrcrayfish.vehicle.init.ModTileEntities;
import com.mrcrayfish.vehicle.inventory.container.AdvancedWorkshopContainer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

/**
 * The advanced workshop (main cell of AdvancedWorkshopBlock). The player chooses a recipe in its screen
 * and puts the ingredients in its 12 input slots; when started, the ingredients are taken and the
 * work goes through the phases of the recipe, using its energy each tick (FE, received from cables
 * of other mods on any cell). Without enough energy the work waits. At the end the result goes to
 * the output slot (it waits if the slot is full). Cancelling gives the ingredients back.
 * The client ticks the work too, so the miniature grows smoothly between two updates.
 */
public class AdvancedWorkshopTileEntity extends TileEntitySynced implements MenuProvider
{
    public static final int INPUT_SLOTS = 12;
    public static final int OUTPUT_SLOT = 12;
    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int MAX_RECEIVE = 50_000;

    private final ItemStackHandler items = new ItemStackHandler(INPUT_SLOTS + 1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            AdvancedWorkshopTileEntity.this.setChanged();
        }
    };
    private final Energy energy = new Energy();
    private final LazyOptional<IItemHandler> automation = LazyOptional.of(() -> new Automation(this.items));
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> this.energy);

    /** Recipe chosen in the screen */
    @Nullable
    private ResourceLocation selected;
    /** Recipe being made, or null */
    @Nullable
    private ResourceLocation crafting;
    private int progress;
    private int total;
    /** The work went on in the last tick (enough energy) */
    private boolean powered;
    /** The work is done but the output slot is full */
    private boolean blocked;
    /** Ingredients taken for the work in progress (given back if it is cancelled) */
    private final List<ItemStack> consumed = new ArrayList<>();
    private int syncTimer;

    /** Values for the screen (ContainerData sends 16 bit values: the energy goes in two halves) */
    private final ContainerData data = new ContainerData()
    {
        @Override
        public int get(int index)
        {
            AdvancedWorkshopTileEntity self = AdvancedWorkshopTileEntity.this;
            return switch(index)
            {
                case 0 -> self.energy.getEnergyStored() & 0xFFFF;
                case 1 -> self.energy.getEnergyStored() >>> 16;
                case 2 -> self.progress;
                case 3 -> self.total;
                case 4 -> (self.crafting != null ? 1 : 0) | (self.powered ? 2 : 0) | (self.blocked ? 4 : 0);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount()
        {
            return DATA_COUNT;
        }
    };
    public static final int DATA_COUNT = 5;

    public AdvancedWorkshopTileEntity(BlockPos pos, BlockState state)
    {
        super(ModTileEntities.ADVANCED_WORKSHOP.get(), pos, state);
    }

    public ItemStackHandler getItems()
    {
        return this.items;
    }

    @Nullable
    public ResourceLocation getSelected()
    {
        return this.selected;
    }

    @Nullable
    public AdvancedWorkshopRecipe getSelectedRecipe()
    {
        return this.level != null ? AdvancedWorkshopRecipe.get(this.level, this.selected) : null;
    }

    @Nullable
    public AdvancedWorkshopRecipe getCraftingRecipe()
    {
        return this.level != null ? AdvancedWorkshopRecipe.get(this.level, this.crafting) : null;
    }

    public boolean isCrafting()
    {
        return this.crafting != null;
    }

    public boolean isPowered()
    {
        return this.powered;
    }

    public boolean isBlocked()
    {
        return this.blocked;
    }

    public int getProgress()
    {
        return this.progress;
    }

    public int getTotal()
    {
        return this.total;
    }

    public int getEnergy()
    {
        return this.energy.getEnergyStored();
    }

    public ContainerData getData()
    {
        return this.data;
    }

    /* ------------------------------------------------------------------ work */

    public void tick()
    {
        if(this.level == null)
            return;
        if(this.level.isClientSide())
        {
            // carries on like the server between two updates
            if(this.crafting != null && this.powered && !this.blocked && this.progress < this.total)
            {
                this.progress++;
            }
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AdvancedWorkshopSounds.tick(this));
            return;
        }

        boolean wasPowered = this.powered, wasBlocked = this.blocked;
        if(this.crafting != null)
        {
            AdvancedWorkshopRecipe recipe = this.getCraftingRecipe();
            if(recipe == null)
            {
                // the recipe is gone (data pack changed): give the ingredients back
                this.cancel();
                return;
            }
            if(this.progress >= this.total)
            {
                this.powered = false;
                this.blocked = !this.deliver(recipe.getResult());
            }
            else if(this.energy.getEnergyStored() >= recipe.getEnergyPerTick())
            {
                this.energy.consume(recipe.getEnergyPerTick());
                this.progress++;
                this.powered = true;
            }
            else
            {
                this.powered = false;
            }
            this.setChanged();
        }
        if(wasPowered != this.powered || wasBlocked != this.blocked)
        {
            this.syncToClient();
        }
        else if(this.crafting != null && ++this.syncTimer >= 20)
        {
            this.syncTimer = 0;
            this.syncToClient();
        }
    }

    /** Puts the result in the output slot; false if it does not fit (the work waits) */
    private boolean deliver(ItemStack result)
    {
        ItemStack output = this.items.getStackInSlot(OUTPUT_SLOT);
        if(!output.isEmpty() && (!ItemHandlerHelper.canItemStacksStack(output, result) || output.getCount() + result.getCount() > output.getMaxStackSize()))
        {
            return false;
        }
        this.items.setStackInSlot(OUTPUT_SLOT, output.isEmpty() ? result : output.copyWithCount(output.getCount() + result.getCount()));
        this.playSound(ModSounds.BLOCK_ADVANCED_WORKSHOP_FINISH.get());
        this.crafting = null;
        this.progress = 0;
        this.total = 0;
        this.consumed.clear();
        this.syncToClient();
        return true;
    }

    /** Chooses the recipe shown in the screen (not while working) */
    public void select(@Nullable ResourceLocation recipe)
    {
        if(this.crafting == null && (recipe == null || AdvancedWorkshopRecipe.get(this.level, recipe) != null))
        {
            this.selected = recipe;
            this.syncToClient();
        }
    }

    /** Starts the chosen recipe if the ingredients are in the input slots */
    public boolean start()
    {
        AdvancedWorkshopRecipe recipe = this.getSelectedRecipe();
        if(this.crafting != null || recipe == null || !recipe.hasIngredients(this.items, 0, INPUT_SLOTS))
            return false;
        this.consumed.clear();
        this.consumed.addAll(recipe.consume(this.items, 0, INPUT_SLOTS));
        this.crafting = recipe.getId();
        this.progress = 0;
        this.total = recipe.getTotalTicks();
        this.powered = false;
        this.blocked = false;
        this.playSound(ModSounds.BLOCK_ADVANCED_WORKSHOP_START.get());
        this.syncToClient();
        return true;
    }

    /** A sound of the machine, from its middle, for everyone around */
    private void playSound(SoundEvent sound)
    {
        if(this.level != null && !this.level.isClientSide())
        {
            Vec3 centre = AdvancedWorkshopBlock.getCentre(this.worldPosition, this.getBlockState().getValue(AdvancedWorkshopBlock.FACING));
            this.level.playSound(null, centre.x, centre.y, centre.z, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** Stops the work and gives the ingredients back (in the input slots, the rest on the ground) */
    public void cancel()
    {
        if(this.crafting == null)
            return;
        for(ItemStack stack : this.consumed)
        {
            ItemStack rest = stack.copy();
            for(int slot = 0; slot < INPUT_SLOTS && !rest.isEmpty(); slot++)
            {
                rest = this.items.insertItem(slot, rest, false);
            }
            if(!rest.isEmpty() && this.level != null)
            {
                Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5, rest);
            }
        }
        this.consumed.clear();
        this.crafting = null;
        this.progress = 0;
        this.total = 0;
        this.powered = false;
        this.blocked = false;
        this.playSound(ModSounds.BLOCK_ADVANCED_WORKSHOP_CANCEL.get());
        this.syncToClient();
    }

    /** The machine is broken: its items and the ingredients of the work in progress drop */
    public void dropContents()
    {
        if(this.level == null)
            return;
        List<ItemStack> drops = new ArrayList<>(this.consumed);
        for(int slot = 0; slot < this.items.getSlots(); slot++)
        {
            drops.add(this.items.getStackInSlot(slot));
        }
        for(ItemStack stack : drops)
        {
            if(!stack.isEmpty())
            {
                Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, stack);
            }
        }
        this.consumed.clear();
    }

    /* ------------------------------------------------------------------ screen, capabilities, save */

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("container.vehicle_restored.advanced_workshop");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player)
    {
        return new AdvancedWorkshopContainer(windowId, inventory, this, this.data);
    }

    /** Capabilities of the machine, also given by the other cells (cables, hoppers, pipes) */
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side)
    {
        if(capability == ForgeCapabilities.ENERGY)
            return this.energyCapability.cast();
        if(capability == ForgeCapabilities.ITEM_HANDLER)
            return this.automation.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        this.automation.invalidate();
        this.energyCapability.invalidate();
    }

    @Override
    public AABB getRenderBoundingBox()
    {
        // the whole machine, whatever its direction
        BlockPos p = this.worldPosition;
        return new AABB(p.getX() - 3, p.getY(), p.getZ() - 3, p.getX() + 4, p.getY() + 3, p.getZ() + 4);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.items.deserializeNBT(tag.getCompound("Items"));
        this.energy.set(tag.getInt("Energy"));
        this.selected = tag.contains("Selected", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("Selected")) : null;
        this.crafting = tag.contains("Crafting", Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString("Crafting")) : null;
        this.progress = tag.getInt("Progress");
        this.total = tag.getInt("Total");
        this.powered = tag.getBoolean("Powered");
        this.blocked = tag.getBoolean("Blocked");
        this.consumed.clear();
        ListTag list = tag.getList("Consumed", Tag.TAG_COMPOUND);
        for(int i = 0; i < list.size(); i++)
        {
            this.consumed.add(ItemStack.of(list.getCompound(i)));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("Items", this.items.serializeNBT());
        tag.putInt("Energy", this.energy.getEnergyStored());
        if(this.selected != null)
            tag.putString("Selected", this.selected.toString());
        if(this.crafting != null)
            tag.putString("Crafting", this.crafting.toString());
        tag.putInt("Progress", this.progress);
        tag.putInt("Total", this.total);
        tag.putBoolean("Powered", this.powered);
        tag.putBoolean("Blocked", this.blocked);
        ListTag list = new ListTag();
        for(ItemStack stack : this.consumed)
        {
            list.add(stack.save(new CompoundTag()));
        }
        tag.put("Consumed", list);
    }

    /** Energy of the machine: it only receives (FE from cables or the creative battery) */
    private class Energy extends EnergyStorage
    {
        Energy()
        {
            super(ENERGY_CAPACITY, MAX_RECEIVE, 0);
        }

        void set(int value)
        {
            this.energy = Math.max(0, Math.min(this.capacity, value));
        }

        void consume(int amount)
        {
            this.energy = Math.max(0, this.energy - amount);
            AdvancedWorkshopTileEntity.this.setChanged();
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate)
        {
            int received = super.receiveEnergy(maxReceive, simulate);
            if(received > 0 && !simulate)
            {
                AdvancedWorkshopTileEntity.this.setChanged();
            }
            return received;
        }
    }

    /** What hoppers and pipes see: they fill the input slots and take from the output slot */
    private record Automation(ItemStackHandler items) implements IItemHandler
    {
        @Override
        public int getSlots()
        {
            return this.items.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return this.items.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate)
        {
            return slot < INPUT_SLOTS ? this.items.insertItem(slot, stack, simulate) : stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return slot == OUTPUT_SLOT ? this.items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return this.items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack)
        {
            return slot < INPUT_SLOTS;
        }
    }
}
