package com.mrcrayfish.vehicle.inventory.container;

import com.mrcrayfish.vehicle.entity.vehicle.TankAmmo;
import com.mrcrayfish.vehicle.entity.vehicle.TankArmorFace;
import com.mrcrayfish.vehicle.entity.vehicle.TankEntity;
import com.mrcrayfish.vehicle.init.ModContainers;
import com.mrcrayfish.vehicle.item.ArmorPlateItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The tank's store, opened by a crew member with the inventory key (like the inventory of a donkey): one row of
 * 4 slots per ammunition (120 mm shells, packs of light shells, belts of cartridges), each slot only taking its
 * own ammunition; the armour plates, one slot per face of the tank (TankArmorFace); the player's inventory below.
 * The "load all" button (clickMenuButton, on the server) moves the ammunition and the plates of the player's
 * inventory into the tank.
 */
public class TankAmmoContainer extends AbstractContainerMenu
{
    /**
     * Layout of the screen (see TankAmmoScreen and its background texture, made by tools/concepts/tank/items.js):
     * one module per ammunition, in the TankAmmo order, with its 4 slots; the player's inventory below; the
     * armour module on the right, the tank seen from above with a socket for each face
     */
    public static final int MODULE_X = 7;
    public static final int MODULE_Y = 21;
    public static final int MODULE_SPACING = 27;
    public static final int AMMO_X = MODULE_X + 28;
    public static final int AMMO_Y = MODULE_Y + 4;
    public static final int INVENTORY_X = 27;
    public static final int INVENTORY_Y = 119;
    public static final int HOTBAR_Y = 177;
    /** Position of the socket of each face (TankArmorFace order): front, left, top, right, rear */
    public static final int[][] ARMOR_SLOTS = {{248, 43}, {217, 121}, {248, 121}, {279, 121}, {248, 174}};
    /** Id of the "load all" button (clickMenuButton) */
    public static final int BUTTON_LOAD_ALL = 0;

    private final Container store;
    private final Container armor;
    private final int ammoSlots;
    private final int storeSlots;
    @Nullable
    private final TankEntity tank;

    public TankAmmoContainer(int windowId, Inventory playerInventory, Container store, Container armor, @Nullable TankEntity tank)
    {
        super(ModContainers.TANK_AMMO.get(), windowId);
        checkContainerSize(store, TankAmmo.values().length * TankAmmo.SLOTS_PER_TYPE);
        checkContainerSize(armor, TankArmorFace.values().length);
        this.store = store;
        this.armor = armor;
        this.tank = tank;
        store.startOpen(playerInventory.player);
        armor.startOpen(playerInventory.player);

        for(TankAmmo ammo : TankAmmo.values())
        {
            for(int i = 0; i < TankAmmo.SLOTS_PER_TYPE; i++)
            {
                this.addSlot(new AmmoSlot(store, ammo.getFirstSlot() + i, AMMO_X + i * 18, AMMO_Y + ammo.ordinal() * MODULE_SPACING, ammo));
            }
        }
        this.ammoSlots = this.slots.size();
        for(TankArmorFace face : TankArmorFace.values())
        {
            int[] at = ARMOR_SLOTS[face.ordinal()];
            this.addSlot(new ArmorSlot(armor, face.ordinal(), at[0], at[1], face));
        }
        this.storeSlots = this.slots.size();
        for(int row = 0; row < 3; row++)
        {
            for(int column = 0; column < 9; column++)
            {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for(int column = 0; column < 9; column++)
        {
            this.addSlot(new Slot(playerInventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }

    /** The tank (null if the client did not find it) */
    @Nullable
    public TankEntity getTank()
    {
        return this.tank;
    }

    @Override
    public boolean stillValid(Player player)
    {
        if(this.tank == null)
            return this.store.stillValid(player);
        // the crew, or a player standing next to the tank (8 blocks, like a chest)
        return this.tank.isAlive() && this.tank.level() == player.level()
                && (this.tank.hasPassenger(player) || this.tank.getBoundingBox().distanceToSqr(player.getEyePosition()) <= 64.0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = this.slots.get(index);
        if(!slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if(index < this.storeSlots)
        {
            // from the tank to the player's inventory
            if(!this.moveItemStackTo(stack, this.storeSlots, this.slots.size(), true))
                return ItemStack.EMPTY;
        }
        else if(!this.moveItemStackTo(stack, 0, this.storeSlots, false))
        {
            // into the row of its ammunition or an empty face (the slots refuse the other items)
            return ItemStack.EMPTY;
        }
        if(stack.isEmpty())
        {
            slot.set(ItemStack.EMPTY);
        }
        else
        {
            slot.setChanged();
        }
        return copy;
    }

    /**
     * "Load all" (the server only: the client sends the button's id and gets the result through the slots, so
     * nothing can be doubled): each stack of ammunition of the player's inventory goes into its row (filling the
     * stacks there first, as far as they take it), then the armour plates go on the faces without a plate, the
     * least worn first. Every move goes through the slots and their rules, like a shift click.
     */
    @Override
    public boolean clickMenuButton(Player player, int id)
    {
        if(id != BUTTON_LOAD_ALL)
            return false;
        boolean moved = false;
        for(int i = this.storeSlots; i < this.slots.size(); i++)
        {
            Slot slot = this.slots.get(i);
            ItemStack stack = slot.getItem();
            if(stack.isEmpty() || TankAmmo.of(stack) == null)
                continue;
            int count = stack.getCount();
            this.moveItemStackTo(stack, 0, this.ammoSlots, false);
            if(stack.getCount() != count)
            {
                moved = true;
                if(stack.isEmpty())
                {
                    slot.set(ItemStack.EMPTY);
                }
                else
                {
                    slot.setChanged();
                }
            }
        }
        List<Slot> plates = new ArrayList<>();
        for(int i = this.storeSlots; i < this.slots.size(); i++)
        {
            if(this.slots.get(i).getItem().getItem() instanceof ArmorPlateItem)
            {
                plates.add(this.slots.get(i));
            }
        }
        plates.sort(Comparator.comparingInt(slot -> slot.getItem().getDamageValue()));
        int next = 0;
        for(int i = this.ammoSlots; i < this.storeSlots && next < plates.size(); i++)
        {
            Slot face = this.slots.get(i);
            if(face.hasItem())
                continue;
            Slot from = plates.get(next++);
            face.set(from.getItem());
            from.set(ItemStack.EMPTY);
            moved = true;
        }
        return moved;
    }

    /**
     * Whether "load all" has something to do (for the look of the button, on the client): ammunition in the
     * player's inventory with room in its row, or a plate and a face without one
     */
    public boolean canLoadAll()
    {
        boolean plate = false, freeFace = false;
        for(int i = this.ammoSlots; i < this.storeSlots; i++)
        {
            freeFace |= !this.slots.get(i).hasItem();
        }
        for(int i = this.storeSlots; i < this.slots.size(); i++)
        {
            ItemStack stack = this.slots.get(i).getItem();
            plate |= stack.getItem() instanceof ArmorPlateItem;
            TankAmmo ammo = TankAmmo.of(stack);
            if(ammo == null)
                continue;
            for(int s = 0; s < TankAmmo.SLOTS_PER_TYPE; s++)
            {
                ItemStack in = this.slots.get(ammo.getFirstSlot() + s).getItem();
                if(in.isEmpty() || (ItemStack.isSameItemSameTags(in, stack) && in.getCount() < in.getMaxStackSize()))
                    return true;
            }
        }
        return plate && freeFace;
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        this.store.stopOpen(player);
        this.armor.stopOpen(player);
    }

    /** A slot of the store: only takes its own ammunition */
    public static class AmmoSlot extends Slot
    {
        private final TankAmmo ammo;

        public AmmoSlot(Container container, int index, int x, int y, TankAmmo ammo)
        {
            super(container, index, x, y);
            this.ammo = ammo;
        }

        public TankAmmo getAmmo()
        {
            return this.ammo;
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return this.ammo.isAmmo(stack);
        }
    }

    /** The slot of a face of the tank: one armour plate */
    public static class ArmorSlot extends Slot
    {
        private final TankArmorFace face;

        public ArmorSlot(Container container, int index, int x, int y, TankArmorFace face)
        {
            super(container, index, x, y);
            this.face = face;
        }

        public TankArmorFace getFace()
        {
            return this.face;
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return stack.getItem() instanceof ArmorPlateItem;
        }

        @Override
        public int getMaxStackSize()
        {
            return 1;
        }
    }
}
