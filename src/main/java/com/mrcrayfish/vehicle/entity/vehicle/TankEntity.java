package com.mrcrayfish.vehicle.entity.vehicle;

import com.mrcrayfish.vehicle.Config;
import com.mrcrayfish.vehicle.client.particle.TankEffects;
import com.mrcrayfish.vehicle.item.ArmorPlateItem;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import com.mrcrayfish.vehicle.entity.EngineTier;
import com.mrcrayfish.vehicle.entity.IEngineTier;
import com.mrcrayfish.vehicle.entity.PoweredVehicleEntity;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import com.mrcrayfish.vehicle.entity.TankBulletEntity;
import com.mrcrayfish.vehicle.entity.TankShellEntity;
import com.mrcrayfish.vehicle.init.ModSounds;
import com.mrcrayfish.vehicle.inventory.container.TankAmmoContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Main battle tank. Driven like the other vehicles with the keyboard, but on tracks: it turns on the
 * spot. The turret turns towards where the driver looks and the gun follows the driver's pitch, both
 * at a limited speed. The crew sits inside (4 seats); the driver (seat 0) drives, aims and fires with
 * the attack key (see TankHandler and MessageFireTank).
 */
public class TankEntity extends PoweredVehicleEntity implements HasCustomInventoryScreen
{
    private static final EntityDataAccessor<Float> TURRET_YAW = SynchedEntityData.defineId(TankEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GUN_PITCH = SynchedEntityData.defineId(TankEntity.class, EntityDataSerializers.FLOAT);
    /** Rounds of each ammunition (TankAmmo order): in the store and in the belt or pack in use */
    private static final List<EntityDataAccessor<Integer>> AMMO_ROUNDS = Arrays.stream(TankAmmo.values())
            .map(ammo -> SynchedEntityData.defineId(TankEntity.class, EntityDataSerializers.INT)).toList();
    /** Armour points left on the plate of each face (TankArmorFace order), -1 without a plate (synced for the HUD) */
    private static final List<EntityDataAccessor<Integer>> ARMOR_POINTS = Arrays.stream(TankArmorFace.values())
            .map(face -> SynchedEntityData.defineId(TankEntity.class, EntityDataSerializers.INT)).toList();
    /**
     * The hull, in blocks from the tank's ground origin (its middle), to tell which face a hit comes from: front
     * and rear ends, half width over the skirts, height of the turret roof
     */
    private static final double HULL_FRONT = 4.1, HULL_REAR = -3.75, HULL_HALF_WIDTH = 1.81, ROOF_HEIGHT = 2.5;

    public static final byte EVENT_FIRE = 60;
    public static final byte EVENT_COAX = 61;
    public static final byte EVENT_HEAVY_MG = 62;
    /** Light shell fired from launch tube n: event EVENT_LIGHT_SHELL + n (n from 0 to 11) */
    public static final byte EVENT_LIGHT_SHELL = 64;

    /** Gun elevation limits, in degrees (positive is up) */
    public static final float MIN_GUN_PITCH = -8F;
    public static final float MAX_GUN_PITCH = 18F;
    /** Maximum rotation speeds, in degrees per tick */
    public static final float TURRET_SPEED = 3.0F;
    public static final float GUN_SPEED = 1.5F;
    /** Rotation of the hull when turning, in degrees per tick (on the spot / at full speed) */
    private static final float PIVOT_SPEED = 2.4F;
    private static final float PIVOT_SPEED_AT_MAX_SPEED = 1.2F;
    /** Shell speed in blocks per tick (its drop is TankShellEntity.GRAVITY) */
    public static final float SHELL_SPEED = 3.0F;
    /** Machine gun bullets: speed in blocks per tick and damage */
    public static final float COAX_SPEED = 8.0F, COAX_DAMAGE = 5F;
    public static final float HEAVY_MG_SPEED = 7.0F, HEAVY_MG_DAMAGE = 10F;
    /** Light shells (rapid fire): speed in blocks per tick */
    public static final float LIGHT_SHELL_SPEED = 4.0F;
    /** Push back of the hull when the 120 mm gun fires (blocks per tick, against the gun) */
    private static final double RECOIL_PUSH = 0.12;
    /**
     * Health of the tank (other vehicles: 100). A 120 mm shell takes up to 99 (explosion of power 7 on
     * the hull), a light shell up to 50, a bullet 5 or 10: 5 to 6 shells, about 12 light shells
     */
    public static final float MAX_HEALTH = 500F;

    /* Positions on the models, in model pixels; the hull model is centred on x 8 and z 8 */
    private static final double MODEL_SCALE = 3.0 / 16.0;
    /** Turret ring centre, relative to the centre of the hull */
    public static final Vec3 TURRET_PIVOT = new Vec3(0, 8.5, -1);
    /** Gun trunnions, relative to the turret ring centre */
    public static final Vec3 GUN_PIVOT = new Vec3(0, 2.9, 7.5);
    /** Distance from the trunnions to the muzzle */
    public static final double GUN_LENGTH = 25.5;
    /** Coaxial machine gun port, relative to the trunnions in the gun frame (z along the bore) */
    public static final Vec3 COAX_PORT = new Vec3(1.7, 0.95, 1.9);
    /** Muzzle of the .50 cal on the cupola, relative to the turret ring centre (y above the turret bottom) */
    public static final Vec3 HEAVY_MG_MUZZLE = new Vec3(4.3, 7.98, 1.65);
    /**
     * Mouths of the 12 launch tubes on the turret sides (2 rows of 3 on each side), relative to the
     * turret ring centre, y above the turret bottom; they fire the light shells, left and right in turn
     */
    public static final Vec3[] LAUNCH_TUBES = new Vec3[12];
    /** Centre of all the tubes (aim of the light shells) */
    public static final Vec3 LAUNCH_TUBES_CENTRE = new Vec3(0, 2.74, -1.66);
    /** Furthest the light shells turn from the turret direction to meet at the target, in degrees */
    private static final float LAUNCH_TUBE_CONVERGENCE = 10F;

    static
    {
        double[] xs = {-9.05, -8.35, -7.65}, ys = {2.19, 3.29};
        int n = 0;
        for(int k = 0; k < 6; k++)
        {
            for(int side = -1; side <= 1; side += 2)
            {
                // left (model -x) and right in turn, through the 6 tubes of each side
                LAUNCH_TUBES[n++] = new Vec3(xs[k % 3] * -side, ys[k / 3], -1.66);
            }
        }
    }
    /**
     * Eye position of each seat, in model pixels, y above the ground. The whole crew is inside with
     * the hatches closed: each one looks out through his vision device. The driver (seat 0, the
     * first player in, who drives, aims and fires) is in the hull at the front, at the height of
     * his periscopes, x and z relative to the centre of the hull. The other seats turn with the
     * turret, x and z relative to the turret ring centre: the gunner in his sight, the commander in
     * his cupola, the loader under his hatch.
     */
    private static final Vec3[] CREW_EYES = {
        new Vec3(0.0, 8.5, 14.6),    // driver, just above his periscopes on the glacis
        new Vec3(3.8, 14.7, 0.6),    // gunner, in the sight housing (doghouse)
        new Vec3(4.3, 14.5, -6.2),   // commander, in the cupola
        new Vec3(-4.2, 13.8, -6.0)   // loader, under his hatch
    };
    private static final int DRIVER_SEAT = 0;
    /**
     * Point the third person camera turns around, in model pixels: 2 blocks above the turret roof
     * (13.5) over the turret ring, so the camera looks over the turret and the gun, at the target.
     */
    private static final Vec3 CAMERA_ORIGIN = new Vec3(0, 13.5 + 2.0 / MODEL_SCALE, -1);
    /** Half the distance between the tracks, in blocks */
    private static final double TRACK_HALF_GAUGE = 7.4 * MODEL_SCALE;

    public float turretYaw;
    public float prevTurretYaw;
    public float gunPitch;
    public float prevGunPitch;
    /** Where the driver aims (turret yaw relative to the hull, gun elevation), see MessageTankAim */
    private float aimYaw;
    private float aimPitch;
    private boolean hasAim;
    /** Point under the driver's crosshair (server: aim of the light shells) */
    @Nullable
    private Vec3 aimTarget;
    /** Next launch tube for a light shell */
    private int launchTube;
    /** Ticks before each weapon can fire again (server) */
    private final int[] weaponTimers = new int[TankWeapon.values().length];
    /**
     * Ammunition store, loaded by the crew from the tank's screen (inventory key): one row of slots
     * per ammunition (see TankAmmo). Server side; the counts are synced (AMMO_ROUNDS).
     */
    private final SimpleContainer ammoStore = new SimpleContainer(TankAmmo.values().length * TankAmmo.SLOTS_PER_TYPE);
    /** Rounds left in the belt or pack in use of each ammunition (taken out of the store) */
    private final int[] loadedRounds = new int[TankAmmo.values().length];
    /**
     * Armour: one plate (ArmorPlateItem) per face (TankArmorFace order), put on by the crew from the tank's screen.
     * A crafted tank has none. Server side; the points left are synced (ARMOR_POINTS).
     */
    private final SimpleContainer armorStore = new SimpleContainer(TankArmorFace.values().length);

    /* Client side animation */
    public float recoil;
    public float prevRecoil;
    /** Rocking of the hull after a shot (degrees, negative lifts the side the gun points to) */
    public float rock;
    public float prevRock;
    private float rockVelocity;
    /** Turret yaw at the shot: the hull rocks around the axis across the gun */
    public float rockYaw;
    /** Distance travelled by each track (model pixels); the side with the low model x first */
    public float trackDistanceA;
    public float prevTrackDistanceA;
    public float trackDistanceB;
    public float prevTrackDistanceB;

    public TankEntity(EntityType<? extends TankEntity> type, Level level)
    {
        super(type, level);
        this.setMaxSpeed(11F);
        this.setAccelerationSpeed(0.35F);
        this.setFuelCapacity(60000F);
        this.setMaxHealth(MAX_HEALTH);
        this.setHealth(MAX_HEALTH);
        this.setMaxUpStep(1.25F);
        this.ammoStore.addListener(container -> this.updateAmmoCounts());
        this.armorStore.addListener(container -> this.updateArmorPoints());
    }

    @Override
    public void defineSynchedData()
    {
        super.defineSynchedData();
        this.entityData.define(TURRET_YAW, 0F);
        this.entityData.define(GUN_PITCH, 0F);
        AMMO_ROUNDS.forEach(key -> this.entityData.define(key, 0));
        ARMOR_POINTS.forEach(key -> this.entityData.define(key, -1));
    }

    @Override
    public SoundEvent getEngineSound()
    {
        return ModSounds.ENTITY_TRACTOR_ENGINE.get();
    }

    @Override
    public float getMinEnginePitch()
    {
        return 0.5F;
    }

    @Override
    public float getMaxEnginePitch()
    {
        return 0.95F;
    }

    @Override
    public boolean canBeColored()
    {
        return true;
    }

    /**
     * The tank has its own engine (no engine item to install). Without an engine tier the vehicle
     * never accelerates, like the bath does.
     */
    @Override
    public Optional<IEngineTier> getEngineTier()
    {
        return Optional.of(EngineTier.IRON);
    }

    @Override
    public boolean canChangeWheels()
    {
        return false;
    }

    /**
     * The tank runs on tracks and takes no wheel item. The vehicle crate gives every vehicle a
     * standard wheel by default: kept, it made the base vehicle look for the wheels of tank.json
     * (there are none), decide the tank was in the air and never accelerate.
     */
    @Override
    public void setWheelStack(ItemStack wheels)
    {
        super.setWheelStack(ItemStack.EMPTY);
    }

    /**
     * On the ground when the hull rests on something (the tracks are under the hull)
     */
    @Override
    protected void updateGroundState()
    {
        this.wheelsOnGround = this.onGround() || !this.level().noCollision(this, this.getBoundingBox().move(0, -0.2, 0));
    }

    @Override
    public boolean canMountTrailer()
    {
        return false;
    }

    @Override
    public double getCameraDistance()
    {
        return 7.0;
    }

    @Override
    public Vec3 getThirdPersonCameraOrigin(float partialTicks)
    {
        float yaw = Mth.lerp(partialTicks, this.yRotO, this.getYRot());
        Vec3 offset = new Vec3(CAMERA_ORIGIN.x, 0, CAMERA_ORIGIN.z).scale(MODEL_SCALE).yRot(-yaw * Mth.DEG_TO_RAD);
        return this.getPosition(partialTicks).add(offset.x, CAMERA_ORIGIN.y * MODEL_SCALE, offset.z);
    }

    @Override
    public boolean shouldShowEngineSmoke()
    {
        return true;
    }

    @Override
    public Vec3 getEngineSmokePosition()
    {
        return new Vec3(0, 6 * MODEL_SCALE, -20.3 * MODEL_SCALE);
    }

    /* ------------------------------------------------------------------ driving */

    @Override
    protected void updateTurning()
    {
        super.updateTurning();
        // Tracks: the hull turns at a rate that does not depend on the speed, so it can turn on the spot
        float input = this.getMaxTurnAngle() != 0 ? Mth.clamp(this.turnAngle / this.getMaxTurnAngle(), -1F, 1F) : 0F;
        float speedFactor = Mth.clamp(Math.abs(this.currentSpeed) / Math.max(1F, this.getMaxSpeed()), 0F, 1F);
        float rate = Mth.lerp(speedFactor, PIVOT_SPEED, PIVOT_SPEED_AT_MAX_SPEED);
        this.deltaYaw = this.canDrive() && this.getControllingPassenger() != null ? input * rate : 0F;
        this.wheelAngle = 0F;
    }

    @Override
    public void updateVehicleMotion()
    {
        float f1 = Mth.sin(this.getYRot() * Mth.DEG_TO_RAD) / 20F;
        float f2 = Mth.cos(this.getYRot() * Mth.DEG_TO_RAD) / 20F;
        this.vehicleMotionX = -this.currentSpeed * f1;
        this.vehicleMotionZ = this.currentSpeed * f2;
        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.08, 0));
    }

    @Override
    public void onUpdateVehicle()
    {
        super.onUpdateVehicle();
        this.updateTurret();

        for(int i = 0; i < this.weaponTimers.length; i++)
        {
            if(this.weaponTimers[i] > 0)
            {
                this.weaponTimers[i]--;
            }
        }

        if(this.level().isClientSide())
        {
            this.prevRecoil = this.recoil;
            this.recoil *= 0.82F;
            // the hull rocks on its suspension and settles (damped spring)
            this.prevRock = this.rock;
            this.rockVelocity += -this.rock * 0.3F;
            this.rockVelocity *= 0.72F;
            this.rock += this.rockVelocity;
            if(Math.abs(this.rock) < 0.01F && Math.abs(this.rockVelocity) < 0.01F)
            {
                this.rock = this.rockVelocity = 0F;
            }

            // Each track moves with the hull and in opposite directions when it turns
            this.prevTrackDistanceA = this.trackDistanceA;
            this.prevTrackDistanceB = this.trackDistanceB;
            double forward = this.currentSpeed / 20.0;
            double turn = this.deltaYaw * Mth.DEG_TO_RAD * TRACK_HALF_GAUGE;
            this.trackDistanceA += (float) ((forward + turn) / MODEL_SCALE);
            this.trackDistanceB += (float) ((forward - turn) / MODEL_SCALE);
        }
    }

    /**
     * Turns the turret and raises the gun towards the aim of the driver (worked out by his client
     * from what is under the crosshair, with the drop of the shell; the driver's view direction until
     * the first aim arrives). The server is the reference and syncs the angles; the driver's client
     * predicts them so aiming feels direct.
     */
    private void updateTurret()
    {
        this.prevTurretYaw = this.turretYaw;
        this.prevGunPitch = this.gunPitch;

        LivingEntity driver = this.getControllingPassenger();
        boolean predict = !this.level().isClientSide() || (driver != null && driver.isControlledByLocalInstance());
        if(driver == null)
        {
            this.hasAim = false;
        }
        if(driver != null && predict)
        {
            float targetYaw = this.hasAim ? this.aimYaw : Mth.wrapDegrees(driver.getYRot() - this.getYRot());
            this.turretYaw += Mth.clamp(Mth.wrapDegrees(targetYaw - this.turretYaw), -TURRET_SPEED, TURRET_SPEED);
            float targetPitch = Mth.clamp(this.hasAim ? this.aimPitch : -driver.getXRot(), MIN_GUN_PITCH, MAX_GUN_PITCH);
            this.gunPitch += Mth.clamp(targetPitch - this.gunPitch, -GUN_SPEED, GUN_SPEED);
        }
        else if(this.level().isClientSide())
        {
            // Other players see the angles synced by the server, smoothed
            this.turretYaw += Mth.clamp(Mth.wrapDegrees(this.entityData.get(TURRET_YAW) - this.turretYaw), -TURRET_SPEED * 2, TURRET_SPEED * 2);
            this.gunPitch += Mth.clamp(this.entityData.get(GUN_PITCH) - this.gunPitch, -GUN_SPEED * 2, GUN_SPEED * 2);
        }

        // Keeps the angle small without breaking the interpolation between two ticks
        if(this.turretYaw > 180F || this.turretYaw < -180F)
        {
            float wrapped = Mth.wrapDegrees(this.turretYaw);
            this.prevTurretYaw += wrapped - this.turretYaw;
            this.turretYaw = wrapped;
        }

        if(!this.level().isClientSide())
        {
            this.entityData.set(TURRET_YAW, this.turretYaw);
            this.entityData.set(GUN_PITCH, this.gunPitch);
        }
    }

    /**
     * Sets where the turret and the gun must point: turret yaw relative to the hull and gun
     * elevation, in degrees, and the point under the crosshair (from MessageTankAim on the server,
     * from TankHandler on the driver's client)
     */
    public void setAim(float turretYaw, float gunPitch, Vec3 target)
    {
        if(!Float.isFinite(turretYaw) || !Float.isFinite(gunPitch) || !Double.isFinite(target.x) || !Double.isFinite(target.y) || !Double.isFinite(target.z))
            return;
        this.aimYaw = Mth.wrapDegrees(turretYaw);
        this.aimPitch = Mth.clamp(gunPitch, MIN_GUN_PITCH, MAX_GUN_PITCH);
        // a sane point: not further than the aim range from the tank
        this.aimTarget = target.distanceToSqr(this.position()) < 512 * 512 ? target : null;
        this.hasAim = true;
    }

    /* ------------------------------------------------------------------ ballistics */

    /**
     * Elevation (degrees, between min and max) that drops a projectile on the target, found by
     * flying it tick by tick like the projectiles fly (bisection). start gives the launch point for an
     * elevation (the muzzle moves with the gun).
     */
    public static float solveElevation(Function<Float, Vec3> start, Vec3 target, double speed, double gravity, float min, float max)
    {
        if(heightError(start.apply(max), max, target, speed, gravity) < 0)
            return max;
        if(heightError(start.apply(min), min, target, speed, gravity) > 0)
            return min;
        float lo = min, hi = max;
        for(int i = 0; i < 18; i++)
        {
            float mid = (lo + hi) / 2F;
            if(heightError(start.apply(mid), mid, target, speed, gravity) > 0) hi = mid;
            else lo = mid;
        }
        return (lo + hi) / 2F;
    }

    /** Height of the projectile above the target when it is over it (negative: too low) */
    private static double heightError(Vec3 start, float pitch, Vec3 target, double speed, double gravity)
    {
        double range = Math.sqrt(Mth.square(target.x - start.x) + Mth.square(target.z - start.z));
        double x = 0, y = start.y, vx = Mth.cos(pitch * Mth.DEG_TO_RAD) * speed, vy = Mth.sin(pitch * Mth.DEG_TO_RAD) * speed;
        for(int i = 0; i < 1000; i++)
        {
            if(x + vx >= range)
            {
                double t = vx > 1.0E-6 ? (range - x) / vx : 0;
                return y + vy * t - target.y;
            }
            x += vx;
            y += vy;
            vy -= gravity;
            if(y < target.y - 64)
                break;
        }
        return -1000;
    }

    /**
     * Direction of a light shell fired from the given launch tube: along the turret, turned a little
     * towards the target so the shells of both sides meet on it, with the elevation that drops them
     * on it
     */
    public Vec3 getLaunchDirection(Vec3 tube, @Nullable Vec3 target, float partialTicks)
    {
        float hull = Mth.lerp(partialTicks, this.yRotO, this.getYRot());
        float turretWorldYaw = hull + this.getTurretYaw(partialTicks);
        if(target == null)
        {
            return this.getTurretDirection(new Vec3(0, 0, 1).xRot(8F * Mth.DEG_TO_RAD), partialTicks);
        }
        double dx = target.x - tube.x, dz = target.z - tube.z;
        float yaw = turretWorldYaw;
        if(dx * dx + dz * dz > 1.0)
        {
            float toTarget = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
            yaw = turretWorldYaw + Mth.clamp(Mth.wrapDegrees(toTarget - turretWorldYaw), -LAUNCH_TUBE_CONVERGENCE, LAUNCH_TUBE_CONVERGENCE);
        }
        // the target as seen along this direction (the shell flies in the vertical plane of its yaw)
        double range = Math.sqrt(dx * dx + dz * dz);
        Vec3 along = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
        Vec3 aimPoint = tube.add(along.scale(range)).with(net.minecraft.core.Direction.Axis.Y, target.y);
        float pitch = solveElevation(p -> tube, aimPoint, LIGHT_SHELL_SPEED, TankShellEntity.GRAVITY, -10F, 45F);
        return new Vec3(0, 0, 1).xRot(pitch * Mth.DEG_TO_RAD).yRot(-yaw * Mth.DEG_TO_RAD);
    }

    public float getAimYaw()
    {
        return this.aimYaw;
    }

    public float getAimPitch()
    {
        return this.aimPitch;
    }

    public float getTurretYaw(float partialTicks)
    {
        return Mth.lerp(partialTicks, this.prevTurretYaw, this.turretYaw);
    }

    public float getGunPitch(float partialTicks)
    {
        return Mth.lerp(partialTicks, this.prevGunPitch, this.gunPitch);
    }

    /* ------------------------------------------------------------------ crew */

    /**
     * The crew is inside the tank (the riders are not drawn, see TankHandler). Each seat places the
     * eyes of the player where that crew member looks out (see CREW_EYES). The view is not turned
     * with the hull: the turret stays aimed where the driver looks.
     */
    @Override
    public void updatePassengerPosition(Entity passenger)
    {
        if(!this.hasPassenger(passenger))
            return;
        int seat = Math.max(0, this.getSeatTracker().getSeatIndex(passenger.getUUID()));
        Vec3 eye = seat < CREW_EYES.length ? CREW_EYES[seat] : CREW_EYES[0];
        Vec3 local = seat == DRIVER_SEAT ? eye : TURRET_PIVOT.add(new Vec3(eye.x, 0, eye.z).yRot(-this.turretYaw * Mth.DEG_TO_RAD)).add(0, eye.y, 0);
        Vec3 offset = new Vec3(local.x, 0, local.z).scale(MODEL_SCALE).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
        double eyeY = local.y * MODEL_SCALE;
        passenger.setPos(this.getX() + offset.x, this.getY() + eyeY - passenger.getEyeHeight(), this.getZ() + offset.z);
        this.applyYawToEntity(passenger);
    }

    @Override
    protected void applyYawToEntity(Entity passenger)
    {
        // No limit on how far the player can turn the head: the turret turns all the way round
        if(passenger instanceof LivingEntity living)
        {
            living.setYBodyRot(this.getYRot() + this.turretYaw);
        }
    }

    /* ------------------------------------------------------------------ gun */

    /**
     * A point fixed to the gun, in the world: local is in model pixels from the trunnions, z along
     * the bore. The angles are given (degrees) so the aim can try other ones.
     */
    public Vec3 getGunPoint(Vec3 local, float hullYaw, float turretYaw, float gunPitch, Vec3 position)
    {
        Vec3 onTurret = GUN_PIVOT.add(local.xRot(gunPitch * Mth.DEG_TO_RAD)).yRot(-turretYaw * Mth.DEG_TO_RAD);
        return position.add(TURRET_PIVOT.add(onTurret).scale(MODEL_SCALE).yRot(-hullYaw * Mth.DEG_TO_RAD));
    }

    public Vec3 getGunPoint(Vec3 local, float partialTicks)
    {
        return this.getGunPoint(local, Mth.lerp(partialTicks, this.yRotO, this.getYRot()), this.getTurretYaw(partialTicks), this.getGunPitch(partialTicks), this.getPosition(partialTicks));
    }

    /**
     * A point fixed to the turret, in the world: local is in model pixels from the turret ring centre,
     * y above the bottom of the turret
     */
    public Vec3 getTurretPoint(Vec3 local, float partialTicks)
    {
        float hull = Mth.lerp(partialTicks, this.yRotO, this.getYRot());
        Vec3 onTurret = new Vec3(local.x, 0, local.z).yRot(-this.getTurretYaw(partialTicks) * Mth.DEG_TO_RAD).add(0, local.y, 0);
        return this.getPosition(partialTicks).add(TURRET_PIVOT.add(onTurret).scale(MODEL_SCALE).yRot(-hull * Mth.DEG_TO_RAD));
    }

    /** Direction in the world of a direction given in the turret frame (z forwards) */
    public Vec3 getTurretDirection(Vec3 local, float partialTicks)
    {
        float hull = Mth.lerp(partialTicks, this.yRotO, this.getYRot());
        return local.yRot(-this.getTurretYaw(partialTicks) * Mth.DEG_TO_RAD).yRot(-hull * Mth.DEG_TO_RAD);
    }

    /**
     * Position of the muzzle in the world, and direction of the gun
     */
    public Vec3 getMuzzlePosition(float partialTicks)
    {
        return this.getGunPoint(new Vec3(0, 0, GUN_LENGTH), partialTicks);
    }

    public Vec3 getGunDirection(float partialTicks)
    {
        return this.getTurretDirection(new Vec3(0, 0, 1).xRot(this.getGunPitch(partialTicks) * Mth.DEG_TO_RAD), partialTicks);
    }

    /**
     * Fires the given weapon if it is ready and has ammunition (a player in creative mode needs none
     * and uses none, like the fuel). Called on the server when the driver fires; each weapon waits its
     * interval between two shots (the gun: its reload time of 5 seconds; the server allows one tick less
     * than the client waits, for the lag between them).
     */
    public void fire(Player player, TankWeapon weapon)
    {
        // ALL is only a mode of the client, which sends the shots of each weapon
        if(this.level().isClientSide() || weapon == TankWeapon.ALL || this.getControllingPassenger() != player || !this.canDrive())
            return;
        int index = weapon.ordinal();
        if(this.weaponTimers[index] > 0)
            return;
        TankAmmo ammo = TankAmmo.of(weapon);
        if(ammo != null && !player.isCreative() && !this.takeRound(ammo))
            return;
        this.weaponTimers[index] = Math.max(0, weapon.getInterval() - 1);

        switch(weapon)
        {
            case CANNON ->
            {
                Vec3 muzzle = this.getMuzzlePosition(1F);
                Vec3 direction = this.getGunDirection(1F);
                this.level().addFreshEntity(new TankShellEntity(this.level(), player, this, muzzle, direction.scale(SHELL_SPEED).add(this.getDeltaMovement()), false));
                this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z, ModSounds.ENTITY_TANK_FIRE.get(), SoundSource.NEUTRAL, 6.0F, 0.95F + this.random.nextFloat() * 0.1F);
                this.level().broadcastEntityEvent(this, EVENT_FIRE);
            }
            case COAX -> this.fireBullet(player, this.getGunPoint(COAX_PORT, 1F), COAX_SPEED, COAX_DAMAGE, false, 0.004);
            case HEAVY_MG -> this.fireBullet(player, this.getTurretPoint(HEAVY_MG_MUZZLE, 1F), HEAVY_MG_SPEED, HEAVY_MG_DAMAGE, true, 0.006);
            case LIGHT_SHELLS ->
            {
                // from the launch tubes on the turret sides, left and right in turn, aimed at the target
                int tube = this.launchTube;
                this.launchTube = (this.launchTube + 1) % LAUNCH_TUBES.length;
                Vec3 mouth = this.getTurretPoint(LAUNCH_TUBES[tube], 1F);
                Vec3 direction = this.getLaunchDirection(mouth, this.aimTarget, 1F).add(this.random.nextGaussian() * 0.003, this.random.nextGaussian() * 0.003, this.random.nextGaussian() * 0.003).normalize();
                this.level().addFreshEntity(new TankShellEntity(this.level(), player, this, mouth, direction.scale(LIGHT_SHELL_SPEED).add(this.getDeltaMovement()), true));
                this.level().playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ENTITY_TANK_LIGHT_CANNON.get(), SoundSource.NEUTRAL, 4.0F, 0.95F + this.random.nextFloat() * 0.1F);
                this.level().broadcastEntityEvent(this, (byte) (EVENT_LIGHT_SHELL + tube));
            }
            case ALL -> {}
        }
    }

    private void fireBullet(Player player, Vec3 muzzle, float speed, float damage, boolean heavy, double spread)
    {
        Vec3 direction = this.getGunDirection(1F).add(this.random.nextGaussian() * spread, this.random.nextGaussian() * spread, this.random.nextGaussian() * spread).normalize();
        this.level().addFreshEntity(new TankBulletEntity(this.level(), player, this, muzzle, direction.scale(speed).add(this.getDeltaMovement()), damage, heavy));
        SoundEvent sound = heavy ? ModSounds.ENTITY_TANK_HEAVY_MG.get() : ModSounds.ENTITY_TANK_COAX.get();
        this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z, sound, SoundSource.NEUTRAL, heavy ? 4.0F : 3.0F, 0.94F + this.random.nextFloat() * 0.12F);
        this.level().broadcastEntityEvent(this, heavy ? EVENT_HEAVY_MG : EVENT_COAX);
    }

    /**
     * The 120 mm gun kicks the tank back (client): the hull rocks, the side of the gun going up, and
     * the tank slides back a little (on the driver's client, which moves the vehicle).
     */
    private void kickBack()
    {
        this.rockYaw = this.turretYaw;
        this.rockVelocity -= 2.6F;
        LivingEntity driver = this.getControllingPassenger();
        if(driver != null && driver.isControlledByLocalInstance())
        {
            Vec3 dir = this.getGunDirection(1F);
            Vec3 back = new Vec3(-dir.x, 0, -dir.z);
            if(back.lengthSqr() > 1.0E-4)
            {
                this.setDeltaMovement(this.getDeltaMovement().add(back.normalize().scale(RECOIL_PUSH)));
            }
        }
    }

    public float getRock(float partialTicks)
    {
        return Mth.lerp(partialTicks, this.prevRock, this.rock);
    }

    @Override
    public void handleEntityEvent(byte id)
    {
        switch(id)
        {
            case EVENT_FIRE ->
            {
                this.recoil = 1.0F;
                this.kickBack();
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TankEffects.muzzleBlast(this));
            }
            case EVENT_COAX -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TankEffects.machineGunFlash(this.level(), this.getGunPoint(COAX_PORT, 1F), this.getGunDirection(1F), false));
            case EVENT_HEAVY_MG -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TankEffects.machineGunFlash(this.level(), this.getTurretPoint(HEAVY_MG_MUZZLE, 1F), this.getGunDirection(1F), true));
            default ->
            {
                int tube = id - EVENT_LIGHT_SHELL;
                if(tube >= 0 && tube < LAUNCH_TUBES.length)
                {
                    // flash at the mouth of the tube, which points forwards and up
                    Vec3 mouth = this.getTurretPoint(LAUNCH_TUBES[tube], 1F);
                    Vec3 dir = this.getTurretDirection(new Vec3(0, 0, 1).xRot(20F * Mth.DEG_TO_RAD), 1F);
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TankEffects.launchTubeBlast(this.level(), mouth, dir));
                    return;
                }
                super.handleEntityEvent(id);
            }
        }
    }

    /* ------------------------------------------------------------------ ammunition and armour */

    /** The ammunition store (server: the real one; client: an empty copy, see TankAmmoContainer) */
    public SimpleContainer getAmmoStore()
    {
        return this.ammoStore;
    }

    /** Rounds of an ammunition left in the tank (synced: also right on the clients) */
    public int getAmmoCount(TankAmmo ammo)
    {
        return this.entityData.get(AMMO_ROUNDS.get(ammo.ordinal()));
    }

    /**
     * Takes one round: from the belt or pack in use, else the next item of the store is opened (the
     * last slot of the row first). False when there is none left.
     */
    private boolean takeRound(TankAmmo ammo)
    {
        int index = ammo.ordinal();
        if(this.loadedRounds[index] <= 0)
        {
            for(int slot = ammo.getFirstSlot() + TankAmmo.SLOTS_PER_TYPE - 1; slot >= ammo.getFirstSlot() && this.loadedRounds[index] <= 0; slot--)
            {
                if(ammo.isAmmo(this.ammoStore.getItem(slot)))
                {
                    this.ammoStore.removeItem(slot, 1);
                    this.loadedRounds[index] = ammo.getRoundsPerItem();
                }
            }
            if(this.loadedRounds[index] <= 0)
                return false;
        }
        this.loadedRounds[index]--;
        this.updateAmmoCounts();
        return true;
    }

    private void updateAmmoCounts()
    {
        if(this.level().isClientSide())
            return;
        for(TankAmmo ammo : TankAmmo.values())
        {
            int items = 0;
            for(int i = 0; i < TankAmmo.SLOTS_PER_TYPE; i++)
            {
                ItemStack stack = this.ammoStore.getItem(ammo.getFirstSlot() + i);
                if(ammo.isAmmo(stack))
                {
                    items += stack.getCount();
                }
            }
            this.entityData.set(AMMO_ROUNDS.get(ammo.ordinal()), this.loadedRounds[ammo.ordinal()] + items * ammo.getRoundsPerItem());
        }
    }

    /**
     * The inventory key opens the ammunition store for the crew (like the inventory of a donkey),
     * with the player's inventory
     */
    @Override
    public void openCustomInventoryScreen(Player player)
    {
        if(player instanceof ServerPlayer serverPlayer && this.hasPassenger(player) && this.isAlive())
        {
            MenuProvider provider = new SimpleMenuProvider((windowId, inventory, p) -> new TankAmmoContainer(windowId, inventory, this.ammoStore, this.armorStore, this), Component.translatable("container.vehicle_restored.tank"));
            NetworkHooks.openScreen(serverPlayer, provider, buffer -> buffer.writeVarInt(this.getId()));
        }
    }

    /** The fuel filler on the rear plate */
    @Override
    public FuelPortType getFuelPortType()
    {
        return FuelPortType.TANK;
    }

    @Override
    public void onClientInit()
    {
        super.onClientInit();
        this.setFuelPortType(FuelPortType.TANK);
    }

    @Override
    protected void onVehicleDestroyed(LivingEntity entity)
    {
        super.onVehicleDestroyed(entity);
        Containers.dropContents(this.level(), this, this.ammoStore);
        Containers.dropContents(this.level(), this, this.armorStore);
    }

    /** The health of the tank is lost for good: no repair with the hammer */
    @Override
    public boolean canRepairWithHammer()
    {
        return false;
    }

    /** The armour store, one plate per face (server: the real one; client: an empty copy, see TankAmmoContainer) */
    public SimpleContainer getArmorStore()
    {
        return this.armorStore;
    }

    /** Armour points left on the plate of a face, -1 without a plate (synced: also right on the clients) */
    public int getArmorPoints(TankArmorFace face)
    {
        return this.entityData.get(ARMOR_POINTS.get(face.ordinal()));
    }

    /** Armour points left on all the plates */
    public float getArmor()
    {
        int points = 0;
        for(TankArmorFace face : TankArmorFace.values())
        {
            points += Math.max(0, this.getArmorPoints(face));
        }
        return points;
    }

    /** Armour points of a full set of new plates (a missing plate counts as armour missing) */
    public float getMaxArmor()
    {
        return TankArmorFace.values().length * ArmorPlateItem.POINTS;
    }

    private void updateArmorPoints()
    {
        if(this.level().isClientSide())
            return;
        for(TankArmorFace face : TankArmorFace.values())
        {
            ItemStack stack = this.armorStore.getItem(face.ordinal());
            this.entityData.set(ARMOR_POINTS.get(face.ordinal()), stack.getItem() instanceof ArmorPlateItem ? ArmorPlateItem.getPoints(stack) : -1);
        }
    }

    /**
     * The armour takes the hits first: the plate of the face a hit comes from is worn by it (plates are not
     * repaired: the wear is for good), and only what that plate cannot take reaches the health. A hit with no
     * direction is shared by the plates. Damage that goes through armour in vanilla (fall, burning, drowning, the
     * void...) goes straight to the health. A creative player's own blow and the crew's own shots are left to
     * VehicleEntity (the first breaks the tank at once, the others do nothing).
     */
    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if(this.level().isClientSide() || !this.isAlive() || this.isInvulnerableTo(source) || !Config.SERVER.vehicleDamage.get() || source.is(DamageTypeTags.BYPASSES_ARMOR))
            return super.hurt(source, amount);
        Entity trueSource = source.getEntity();
        if(trueSource != null && source.getDirectEntity() != trueSource && this.hasPassenger(trueSource))
            return super.hurt(source, amount);
        if(trueSource instanceof Player player && player.isCreative() && source.getDirectEntity() == trueSource)
            return super.hurt(source, amount);
        float left = this.wearArmor(source, amount);
        if(left <= 0F)
        {
            this.setTimeSinceHit(10);
            return true;
        }
        return super.hurt(source, left);
    }

    /** Wears the plates by a hit; returns the damage they could not take */
    private float wearArmor(DamageSource source, float amount)
    {
        TankArmorFace face = this.getFaceHitBy(source);
        if(face != null)
            return this.wearPlate(face, source, amount);
        List<TankArmorFace> faces = Arrays.stream(TankArmorFace.values()).filter(f -> this.armorStore.getItem(f.ordinal()).getItem() instanceof ArmorPlateItem).toList();
        if(faces.isEmpty())
            return amount;
        float left = 0F;
        for(TankArmorFace f : faces)
        {
            left += this.wearPlate(f, source, amount / faces.size());
        }
        return left;
    }

    /**
     * Wears the plate of a face by a hit (its wear factor for that kind of damage), breaking it when it is worn
     * through; returns the damage it could not take (all of it without a plate)
     */
    private float wearPlate(TankArmorFace face, DamageSource source, float amount)
    {
        ItemStack stack = this.armorStore.getItem(face.ordinal());
        if(!(stack.getItem() instanceof ArmorPlateItem plate))
            return amount;
        float factor = plate.getWearFactor(source);
        if(factor <= 0F)
            return 0F;
        float wear = amount * factor;
        int points = ArmorPlateItem.getPoints(stack);
        if(wear < points)
        {
            stack.setDamageValue(stack.getDamageValue() + Math.max(1, Mth.ceil(wear)));
            if(ArmorPlateItem.getPoints(stack) > 0)
            {
                this.armorStore.setChanged();
                return 0F;
            }
        }
        this.breakPlate(face, stack);
        return Math.max(0F, wear - points) / factor;
    }

    /** A plate worn through breaks: it is gone, with the sound of a breaking shield and its pieces */
    private void breakPlate(TankArmorFace face, ItemStack stack)
    {
        ItemStack pieces = stack.copy();
        this.armorStore.setItem(face.ordinal(), ItemStack.EMPTY);
        Vec3 at = this.position().add(new Vec3(face.getX(), face.getY(), face.getZ()).yRot(-this.getYRot() * Mth.DEG_TO_RAD));
        this.level().playSound(null, at.x, at.y, at.z, SoundEvents.SHIELD_BREAK, SoundSource.NEUTRAL, 1.2F, 0.6F);
        if(this.level() instanceof ServerLevel server)
        {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, pieces), at.x, at.y, at.z, 24, 0.4, 0.3, 0.4, 0.12);
        }
    }

    /**
     * The face a hit comes from: from where it comes (the explosion, the projectile, the attacker) seen from the
     * hull; from above when it comes down steeply or from over the turret roof, else the face it is nearest to
     * for the size of the hull. Null when the hit has no position (it is then shared by the plates).
     */
    @Nullable
    private TankArmorFace getFaceHitBy(DamageSource source)
    {
        Vec3 from = source.getSourcePosition();
        if(from == null)
            return null;
        // in the frame of the hull: x to the tank's left, z to its front
        Vec3 local = from.subtract(this.position()).yRot(this.getYRot() * Mth.DEG_TO_RAD);
        boolean over = Math.abs(local.x) < HULL_HALF_WIDTH && local.z > HULL_REAR && local.z < HULL_FRONT;
        Entity direct = source.getDirectEntity();
        Vec3 motion = direct != null && direct != source.getEntity() ? direct.getDeltaMovement() : Vec3.ZERO;
        boolean fromAbove = local.y > ROOF_HEIGHT || (motion.lengthSqr() > 1.0E-4 && motion.y < -0.7 * motion.length());
        if(over && fromAbove)
            return TankArmorFace.TOP;
        double middle = (HULL_FRONT + HULL_REAR) / 2, halfLength = (HULL_FRONT - HULL_REAR) / 2;
        if(Math.abs(local.z - middle) / halfLength >= Math.abs(local.x) / HULL_HALF_WIDTH)
            return local.z > middle ? TankArmorFace.FRONT : TankArmorFace.REAR;
        return local.x > 0 ? TankArmorFace.LEFT : TankArmorFace.RIGHT;
    }

    /* ------------------------------------------------------------------ save */

    @Override
    protected void readAdditionalSaveData(CompoundTag compound)
    {
        super.readAdditionalSaveData(compound);
        // tanks saved with the old health (100) get the new one, damage kept in proportion
        if(this.getMaxHealth() != MAX_HEALTH)
        {
            float part = this.getMaxHealth() > 0F ? this.getHealth() / this.getMaxHealth() : 1F;
            this.setMaxHealth(MAX_HEALTH);
            this.setHealth(MAX_HEALTH * part);
        }
        this.turretYaw = this.prevTurretYaw = compound.getFloat("TurretYaw");
        this.gunPitch = this.prevGunPitch = compound.getFloat("GunPitch");
        this.entityData.set(TURRET_YAW, this.turretYaw);
        this.entityData.set(GUN_PITCH, this.gunPitch);
        int[] loaded = compound.getIntArray("LoadedRounds");
        for(int i = 0; i < this.loadedRounds.length; i++)
        {
            this.loadedRounds[i] = i < loaded.length ? Math.max(0, loaded[i]) : 0;
        }
        // each item at its own slot (its row is its ammunition)
        this.ammoStore.clearContent();
        ListTag list = compound.getList("Ammo", Tag.TAG_COMPOUND);
        for(int i = 0; i < list.size(); i++)
        {
            CompoundTag tag = list.getCompound(i);
            int slot = tag.getByte("Slot") & 255;
            if(slot < this.ammoStore.getContainerSize())
            {
                this.ammoStore.setItem(slot, ItemStack.of(tag));
            }
        }
        this.updateAmmoCounts();
        // the armour plates, each at the slot of its face
        this.armorStore.clearContent();
        ListTag plates = compound.getList("Armor", Tag.TAG_COMPOUND);
        for(int i = 0; i < plates.size(); i++)
        {
            CompoundTag tag = plates.getCompound(i);
            int slot = tag.getByte("Slot") & 255;
            if(slot < this.armorStore.getContainerSize())
            {
                this.armorStore.setItem(slot, ItemStack.of(tag));
            }
        }
        this.updateArmorPoints();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound)
    {
        super.addAdditionalSaveData(compound);
        compound.putFloat("TurretYaw", this.turretYaw);
        compound.putFloat("GunPitch", this.gunPitch);
        compound.putIntArray("LoadedRounds", this.loadedRounds);
        ListTag list = new ListTag();
        for(int slot = 0; slot < this.ammoStore.getContainerSize(); slot++)
        {
            ItemStack stack = this.ammoStore.getItem(slot);
            if(!stack.isEmpty())
            {
                CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte) slot);
                stack.save(tag);
                list.add(tag);
            }
        }
        compound.put("Ammo", list);
        ListTag plates = new ListTag();
        for(int slot = 0; slot < this.armorStore.getContainerSize(); slot++)
        {
            ItemStack stack = this.armorStore.getItem(slot);
            if(!stack.isEmpty())
            {
                CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte) slot);
                stack.save(tag);
                plates.add(tag);
            }
        }
        compound.put("Armor", plates);
    }

}
