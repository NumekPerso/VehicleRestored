package com.mrcrayfish.vehicle.entity.vehicle;

import com.mrcrayfish.vehicle.init.ModItems;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Ammunition of the tank. Each type has its row of slots in the tank's ammunition store (see
 * TankEntity, TankAmmoContainer); one item holds a number of rounds (a shell, a pack of light
 * shells, a belt of cartridges). The machine guns share the belts.
 */
public enum TankAmmo
{
    /** 120 mm shells for the gun, one per item */
    SHELLS("shells", ModItems.TANK_SHELL, 1),
    /** Light shells for the launch tubes, in packs of 4 */
    LIGHT_SHELLS("light_shells", ModItems.LIGHT_SHELL_PACK, 4),
    /** Cartridges for the coaxial and the heavy machine guns, in belts of 100 */
    BULLETS("bullets", ModItems.BULLET_BELT, 100);

    /** Slots of each type in the ammunition store (one row in the screen) */
    public static final int SLOTS_PER_TYPE = 4;

    private final String id;
    private final Supplier<? extends Item> item;
    private final int roundsPerItem;

    TankAmmo(String id, Supplier<? extends Item> item, int roundsPerItem)
    {
        this.id = id;
        this.item = item;
        this.roundsPerItem = roundsPerItem;
    }

    public String getId()
    {
        return this.id;
    }

    public Item getItem()
    {
        return this.item.get();
    }

    public int getRoundsPerItem()
    {
        return this.roundsPerItem;
    }

    /** Rounds of a full row of slots */
    public int getCapacity()
    {
        return SLOTS_PER_TYPE * new ItemStack(this.getItem()).getMaxStackSize() * this.roundsPerItem;
    }

    public boolean isAmmo(ItemStack stack)
    {
        return stack.is(this.getItem());
    }

    /** First slot of this type in the ammunition store */
    public int getFirstSlot()
    {
        return this.ordinal() * SLOTS_PER_TYPE;
    }

    public String getTranslationKey()
    {
        return "gui.vehicle_restored.tank.ammo." + this.id;
    }

    /** Ammunition fired by a weapon, or null (all the weapons at once) */
    public static TankAmmo of(TankWeapon weapon)
    {
        return switch(weapon)
        {
            case CANNON -> SHELLS;
            case COAX, HEAVY_MG -> BULLETS;
            case LIGHT_SHELLS -> LIGHT_SHELLS;
            case ALL -> null;
        };
    }

    /** The ammunition an item is, or null */
    @javax.annotation.Nullable
    public static TankAmmo of(ItemStack stack)
    {
        for(TankAmmo ammo : values())
        {
            if(ammo.isAmmo(stack))
                return ammo;
        }
        return null;
    }
}
