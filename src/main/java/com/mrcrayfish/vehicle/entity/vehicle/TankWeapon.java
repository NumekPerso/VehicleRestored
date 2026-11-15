package com.mrcrayfish.vehicle.entity.vehicle;

/**
 * Weapons of the tank, chosen by the driver in the weapon bar (keys 1 to 4).
 */
public enum TankWeapon
{
    /** 120 mm gun, explosive shell; reloads in 5 seconds (100 ticks) after each shot */
    CANNON("cannon", 100, false),
    /** 7.62 mm machine gun beside the gun, fires where the gun points */
    COAX("coax", 2, true),
    /** .50 cal machine gun on the commander's cupola */
    HEAVY_MG("heavy_mg", 3, true),
    /** Light explosive shells fired from the launch tubes at a high rate while the attack key is held */
    LIGHT_SHELLS("light_shells", 5, true),
    /**
     * Every weapon at once (chosen on the client only): a click fires the gun, holding the key fires
     * the machine guns and the light shells, each at its own rate. Never sent to the server.
     */
    ALL("all", 2, true);

    private final String id;
    private final int interval;
    private final boolean automatic;

    TankWeapon(String id, int interval, boolean automatic)
    {
        this.id = id;
        this.interval = interval;
        this.automatic = automatic;
    }

    public String getId()
    {
        return this.id;
    }

    /** Minimum number of ticks between two shots */
    public int getInterval()
    {
        return this.interval;
    }

    /** Keeps firing while the attack key is held */
    public boolean isAutomatic()
    {
        return this.automatic;
    }

    public String getTranslationKey()
    {
        return "vehicle_restored.tank.weapon." + this.id;
    }

    public static TankWeapon byId(int ordinal)
    {
        TankWeapon[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CANNON;
    }
}
