package com.mrcrayfish.vehicle.entity.vehicle;

/**
 * The faces of the tank that carry an armour plate (ArmorPlateItem), in the order of their slots in the
 * tank's armour store (TankEntity) and of the parts of the armour bar of the HUD: the sockets of the tank's
 * screen read row by row (front; left, top, right; rear). Each face has the point the plate sits at, in
 * blocks in the frame of the hull: x to the tank's left, y up from the ground, z to its front.
 */
public enum TankArmorFace
{
    FRONT("front", 0, 1.2, 4.1),
    LEFT("left", 1.8, 1.0, 0.2),
    TOP("top", 0, 2.55, 0.0),
    RIGHT("right", -1.8, 1.0, 0.2),
    REAR("rear", 0, 1.2, -3.75);

    private final String id;
    private final double x, y, z;

    TankArmorFace(String id, double x, double y, double z)
    {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getId()
    {
        return this.id;
    }

    public double getX()
    {
        return this.x;
    }

    public double getY()
    {
        return this.y;
    }

    public double getZ()
    {
        return this.z;
    }

    public String getTranslationKey()
    {
        return "gui.vehicle_restored.tank.armor." + this.id;
    }
}
